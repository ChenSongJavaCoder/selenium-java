package com.cs.selenium.cache;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayDeque;

/**
 * @Author: CS
 * @Date: 2020/10/9 14:08
 * @Description:
 */
@Slf4j
public class QccHeartbeat {

    private static boolean state;

    private static ArrayDeque<LocalDateTime> queue = new ArrayDeque(4);


    public static void alive() {
        queue.addFirst(LocalDateTime.now());
        if (queue.size() > 3) {
            queue.pollLast();
        }
        log.warn("queue.size = {}", queue.size());
    }

    public static boolean isAlive() {
        if (queue.isEmpty() || queue.poll().isBefore(LocalDateTime.now().minusMinutes(30))) {
            state = false;
        } else {
            state = true;
        }
        log.warn("检查服务是否正常啦！是否正常：{}", state);
        return state;
    }
}
