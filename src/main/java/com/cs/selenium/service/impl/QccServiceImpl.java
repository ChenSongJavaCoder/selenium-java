package com.cs.selenium.service.impl;

import cn.snowheart.dingtalk.robot.starter.client.DingTalkRobotClient;
import com.alibaba.fastjson.JSON;
import com.cs.selenium.entity.KpPublicInvoiceCustomerInfo;
import com.cs.selenium.entity.KpPublicInvoiceCustomerInfoDetails;
import com.google.common.collect.Lists;
import com.cs.selenium.cache.QccAccountCache;
import com.cs.selenium.cache.QccHeartbeat;
import com.cs.selenium.config.WebDriverConfig;
import com.cs.selenium.enums.QccAccount;
import com.cs.selenium.service.IQccService;
import com.cs.selenium.util.ListUtil;
import com.cs.selenium.util.QccThreadExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

/**
 * 企查查爬虫
 *
 * @author hll
 * @version 1.0
 * @date 2020/9/22 13:41
 */
@Slf4j
@Service
public class QccServiceImpl implements IQccService {

    DingTalkRobotClient dingTalkRobotClient;

    @Resource
    private WebDriverConfig webDriverConfig;

    private static final String WEBDRIVER_LOCATION = getWebdriverLocation();

    private static final String ZW = "暂无";

    private static final String WINDOWS = "windows";

    private static final String LINUX = "linux";

    private static final String QCC_SEARCH_URL = "https://www.qcc.com/tax";

    private static Stack<QccAccount> availableAccount;

    @PostConstruct
    public void init() {
        qcc();
    }


    @Override
    public void qcc() {
        long start = System.currentTimeMillis();
        // 手动初始化
        init0();

        // todo: 要查询的数据可从数据库获取
        List<KpPublicInvoiceCustomerInfo> customerInfoList = new ArrayList<>(webDriverConfig.getHandleThreshold());
        if (CollectionUtils.isEmpty(customerInfoList)) {
            return;
        }
        // 自适应处理拉取条数和账户之间的关系
        List<List<KpPublicInvoiceCustomerInfo>> splitList = ListUtil.averageAssign(customerInfoList, (int) Math.ceil(Double.valueOf(customerInfoList.size()) / availableAccount.size()));
        CountDownLatch countDownLatch = new CountDownLatch(splitList.size());

        for (List<KpPublicInvoiceCustomerInfo> list : splitList) {
            WebDriver driver = getWebDriver();
            QccThreadExecutor.execute(() -> {
                try {
                    List<KpPublicInvoiceCustomerInfoDetails> detailsList = Lists.newArrayList();
                    List<Long> nonCheck = Lists.newArrayList();
                    for (KpPublicInvoiceCustomerInfo customerInfo : list) {
                        String companyName = customerInfo.getCompanyName();
                        Long id = customerInfo.getId();
                        log.info("开始抓取企业名称为={}的信息", companyName);
                        searchCompany(driver, companyName, id, detailsList, nonCheck);
                        log.info("成功抓取企业名称为={}的信息", companyName);
                    }
                    storage(detailsList);
                    nonCheck(nonCheck);
                } catch (Exception e) {
                    log.error("【企查查】 同步出错！！ 原因：{}", e.getMessage());
                    dingTalkRobotClient.sendTextMessage("【企查查】 同步出错！！ 原因：" + e.getMessage());
                } finally {
                    driver.quit();
                    countDownLatch.countDown();
                }
            });
        }

        try {
            // 为了锁定待处理的数据资源
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.warn("此次任务同步完成！！！ 耗时：{} 毫秒", System.currentTimeMillis() - start);
        dingTalkRobotClient.sendTextMessage("【企查查】此次任务同步完成！ 耗时：" + (System.currentTimeMillis() - start) / 1000 + " 秒");

        qcc();
    }

    /**
     * 接口数据初始化
     */
    private void init0() {
        // 需要保证有足够可用的账号
        while (true) {
            availableAccount = QccAccountCache.availableAccount();
            QccHeartbeat.alive();
            try {
                if (availableAccount.isEmpty()) {
                    dingTalkRobotClient.sendTextMessage("【企查查】可用账号已用完！！！ 稍等30分钟再试试。");
                    sleep(1000 * 60 * 30);
                } else if (webDriverConfig.getHandleThreshold() / availableAccount.size() <= webDriverConfig.getMaxPerHandle()) {
                    log.warn("此次任务可用账号个数：{}，账号信息：{}", availableAccount.size(), JSON.toJSONString(availableAccount));
                    break;
                } else {
                    log.warn("当前可用账号：{}, 尝试获取足够可用账号......", JSON.toJSONString(availableAccount));
                    dingTalkRobotClient.sendTextMessage("【企查查】 当前可用账号：" + JSON.toJSONString(availableAccount) + ", 尝试获取足够可用账号......");
                    sleep(1000 * 60 * 3);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 打开企查查网址 查询企业信息
     * 每次都打开查询初始界面，可以使得webdriver不需要关闭操作
     *
     * @param driver driver
     */
    private void searchCompany(WebDriver driver, String queryName, Long id, List<KpPublicInvoiceCustomerInfoDetails> detailsList, List<Long> unCheck) throws Exception {
        // 打开企查查的网站
        driver.get(QCC_SEARCH_URL);

        // 获取输入框元素并且输入查询内容
        driver.findElement(By.xpath("//*[@id=\"company-name\"]")).sendKeys(queryName);

        // 点击搜索按钮
        driver.findElement(By.xpath("//*[@id=\"V3_Brands_Search\"]/div/span/input")).click();

        // 登陆相关逻辑
        doLogin(driver);

        // 操作频繁的验证处理
        indexVerify(driver);

        // 是否获取到公司详情页元素 根据匹配到公司的条数做判断
        String count = driver.findElement(By.cssSelector("body > div.container.m-t-lg > div > div.col-sm-9 > section > div.panel-heading.b-b > span > span")).getText();

        boolean contentAppeared = !"0".equals(count);
        if (contentAppeared) {
            // 获取企业信息网页地址
            String href = driver.findElement(By.xpath(".//div[@class='col-md-6']//a")).getAttribute("href");
            log.info("公司详情页url={}", href);

            // 点击进入详情页
            driver.get(href);
            driver.navigate().refresh();

            // 获取企业信息
            getCompanyDetails(driver, id, detailsList);
        } else {
            unCheck.add(id);
        }
    }

    /**
     * 获取企业信息
     *
     * @param driver driver
     */
    private void getCompanyDetails(WebDriver driver, Long id, List<KpPublicInvoiceCustomerInfoDetails> detailsList) {
        log.info("开始解析企业信息");
        // 企业地址
        String dz = driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/section/div[2]/form/div[3]/div/p")).getText();
        // 电话号码
        String dh = driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/section/div[2]/form/div[4]/div/p")).getText();
        // 开户银行
        String khyh = driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/section/div[2]/form/div[5]/div/p")).getText();
        // 银行账户
        String yhzh = driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/section/div[2]/form/div[6]/div/p")).getText();

        // 新增企业信息子表
        KpPublicInvoiceCustomerInfoDetails details = new KpPublicInvoiceCustomerInfoDetails();
        details.setKpPublicInvoiceCustomerInfoId(id);
        details.setDzdh(ZW.equals(dz) ? StringUtils.EMPTY : dz);
        details.setPhoneNumber(ZW.equals(dh) ? StringUtils.EMPTY : dh);
        details.setOpenBankAndAccount(!ZW.equals(khyh) && !ZW.equals(yhzh) ? khyh + yhzh : StringUtils.EMPTY);
        log.info("企业税务信息详情: {}", JSON.toJSONString(details));

        // 加入入库集合
        detailsList.add(details);
    }

    /**
     * 登陆
     *
     * @param driver driver
     */
    private void doLogin(WebDriver driver) throws IllegalAccessException {
        // 获取选择登陆元素 如果没有 则不需要登陆
        String currentUrl = driver.getCurrentUrl();
        log.info("当前url：{}", currentUrl);

        boolean contentAppeared = currentUrl.contains("user_login");
        if (contentAppeared) {
            if (availableAccount.isEmpty()) {
                throw new IllegalAccessException("账号已使用完！");
            }
            QccAccount qccAccount = availableAccount.pop();

            log.info("登录人信息：username = {} password = {}", qccAccount.getUsername(), qccAccount.getPassword());

            // 选择账户密码登陆模式
            driver.findElement(By.xpath("//*[@id=\"normalLogin\"]")).click();

            // 输入账户名称
            driver.findElement(By.xpath("//*[@id=\"nameNormal\"]")).sendKeys(qccAccount.getUsername());

            // 输入密码
            driver.findElement(By.xpath("//*[@id=\"pwdNormal\"]")).sendKeys(qccAccount.getPassword());

            // 滑块验证码
            loginVerify(driver, qccAccount);

            // 点击登陆
            driver.findElement(By.xpath("//*[@id=\"user_login_normal\"]/button")).click();

            // 睡一会儿
            try {
                Thread.sleep(300L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 登陆滑块验证
     * 多次刷新验证，导致账号登陆拒绝
     *
     * @param driver driver
     */
    private void loginVerify(WebDriver driver, QccAccount account) throws IllegalAccessException {
        String success = "验证通过";
        AtomicInteger distance = new AtomicInteger(307);
        // 失败次数，用来标记账号登陆异常
        AtomicInteger failCount = new AtomicInteger(0);

        String text;
        do {
            if (failCount.get() > webDriverConfig.getRefreshThreshold()) {
                QccAccountCache.addUnAvailableAccount(account);
                throw new IllegalAccessException("用户：" + account.getUsername() + " 尝试刷新登陆" + webDriverConfig.getRefreshThreshold() + "次不成功，列为暂不可用账号，30分钟后释放重试登陆！");
            }
            WebElement element = driver.findElement(By.id("nc_1_n1z"));
            try {
                // 这里的刷新操作需要延时
                doRefresh(driver, distance, failCount);
                sleep(1000);
                move(driver, element, distance.incrementAndGet());
                sleep(1000);
                doRefresh(driver, distance, failCount);
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            text = driver.findElement(By.className("nc-lang-cnt")).getText();
        }
        while (!success.equals(text));

        log.info("distance is {}", distance);
    }

    /**
     * 查询操作频繁的滑动验证
     *
     * @param driver driver
     */
    private void indexVerify(WebDriver driver) {
        String url = driver.getCurrentUrl();
        String verifyKeyword = "https://www.qcc.com/index_verify";
        // 如果进入了操作频繁验证页面
        if (url.startsWith(verifyKeyword)) {
            log.error("进入登录验证页面！ {}", url);
            WebElement element = driver.findElement(By.xpath("//*[@id=\"nc_1_n1z\"]"));

            String success = "验证通过";
            int distance = 307;

            String text;
            do {
                distance++;
                move(driver, element, distance);
                text = driver.findElement(By.xpath("//*[@id=\"nc_1__scale_text\"]")).getText();
            }
            while (!success.equals(text));

            // 提交验证
            driver.findElement(By.cssSelector("#verify")).click();
        }
    }

    /**
     * 处理滑动块人工手动刷新操作
     *
     * @param driver driver
     */
    private void doRefresh(WebDriver driver, AtomicInteger distance, AtomicInteger failCount) {

        WebElement element = driver.findElement(By.xpath("//*[@id=\"dom_id_one\"]/div"));
        String att = element.getAttribute("class");
        log.warn("登陆验证刷新模块 ： {}", att);
        String err = "errloading";
        // 有错误信息，需要手动刷新
        if (err.equals(att)) {
            driver.findElement(By.xpath("//*[@id=\"dom_id_one\"]/div/span/a")).click();
            distance.decrementAndGet();
            failCount.incrementAndGet();
        }

    }

    /**
     * 获取浏览器驱动
     *
     * @return driver driver
     */
    private WebDriver getWebDriver() {
        // chromedriver服务地址 本地下载
        System.setProperty("webdriver.chrome.driver", WEBDRIVER_LOCATION);

        // 新建一个WebDriver 的对象，使用chromedriver
        ChromeOptions options = new ChromeOptions();

        // 是否隐藏窗口
        options.setHeadless(webDriverConfig.getHeadless());
        // 防止检测Selenium
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "window-size=1920x3000", "--disable-gpu", "--hide-scrollbars", "blink-settings=imagesEnabled=false");
        options.setCapability("networkConnectionEnabled", true);
        options.setExperimentalOption("w3c", false);
        options.setExperimentalOption("excludeSwitches", Lists.newArrayList("enable-automation"));
        WebDriver driver = new ChromeDriver(options);

        // 休眠
        driver.manage().timeouts().implicitlyWait(300, TimeUnit.SECONDS);
        return driver;
    }

    /**
     * 处理拉取到的企业信息
     *
     * @param detailsList 企业税务详细信息集合
     */
    public void storage(List<KpPublicInvoiceCustomerInfoDetails> detailsList) {
        if (CollectionUtils.isEmpty(detailsList)) {
            return;
        }
        // todo 获取到的数据入库
    }

    /**
     * 处理未拉取到税务信息的企业
     *
     * @param ids 未拉取到税务信息的企业数据集合
     */
    public void nonCheck(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        // todo 未获取到的数据标记
    }

    /**
     * 移动选中的元素位置
     *
     * @param driver   浏览器驱动
     * @param element  选定的页面元素
     * @param distance 位移的距离
     */
    private static void move(WebDriver driver, WebElement element, int distance) {
        Actions actions = new Actions(driver);
        actions.clickAndHold(element).perform();
        actions.moveByOffset(distance, 0).perform();
        actions.release(element).perform();
    }

    /**
     * 自适应获取浏览器驱动位置
     *
     * @return 浏览器驱动地址
     */
    private static String getWebdriverLocation() {
        // 区分运行环境
        String osName = System.getProperty("os.name").toLowerCase();
        String fileName;
        if (osName.equals(LINUX)) {
            fileName = "chromedriver-linux";
        } else if (osName.contains(WINDOWS)) {
            fileName = "chromedriver.exe";
        } else {
            fileName = "chromedriver-mac";
        }
        return Objects.requireNonNull(QccServiceImpl.class.getClassLoader().getResource(fileName)).getPath();
    }

}
