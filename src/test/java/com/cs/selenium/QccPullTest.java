package com.cs.selenium;

import com.cs.selenium.service.IQccService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @Author: CS
 * @Date: 2020/9/23 10:22
 * @Description:
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class QccPullTest {

    @Autowired
    IQccService iQccService;


    @Test
    public void pull(){
        iQccService.qcc();
    }
}
