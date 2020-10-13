//package com.cs.selenium.controller;
//
//import cn.snowheart.dingtalk.robot.starter.client.DingTalkRobotClient;
//import com.cs.selenium.cache.QccHeartbeat;
//import com.cs.selenium.service.IQccService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import javax.annotation.Resource;
//
///**
// * 企查查爬虫
// *
// * @author hll
// * @version 1.0
// * @date 2020/9/22 13:41
// */
//@Slf4j
//@RestController
//public class QccController {
//
//    @Resource
//    private IQccService qccService;
//
//    @Resource
//    DingTalkRobotClient dingTalkRobotClient;
//
//    /**
//     * 提供手动触发拉取的接口
//     * 当然目的还是想配合计划任务检测服务是否在正常启动，类似心跳机制
//     * 30min进行一次检验
//     */
////    @Scheduled(cron = "0 0 0/1 * * ?")
//    @Scheduled(initialDelay = 200,fixedRate = 1000 * 60)
////    @GetMapping("/pull")
//    public void pull() {
//        if (!QccHeartbeat.isAlive()) {
//            dingTalkRobotClient.sendTextMessage("【企查查】当前服务未收到反馈，任务重新拉取.....");
//            qccService.qcc();
//        } else {
//            dingTalkRobotClient.sendTextMessage("【企查查】当前服务正常运行......");
//        }
//    }
//
//}
