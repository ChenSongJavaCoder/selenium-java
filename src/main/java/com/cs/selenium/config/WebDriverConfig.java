package com.cs.selenium.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: CS
 * @Date: 2020/9/28 13:51
 * @Description:
 */
@Data
@Configuration
public class WebDriverConfig {

    @Value("${webdriver.headless:false}")
    private Boolean headless;

    @Value("${webdriver.login.refresh.threshold:5}")
    private Integer refreshThreshold;

    @Value("${qcc.task.size:400}")
    private Integer handleThreshold;

    @Value("${qcc.task.maxperhandle:100}")
    private Integer maxPerHandle;

}
