package com.cs.selenium.util;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: CS
 * @Date: 2020/9/23 10:57
 * @Description:
 */
public class QccThreadExecutor {

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(
            6,
            10,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            r -> new Thread(r, "qcc-thread-pool-" + r.hashCode()));

    public static void execute(List<Runnable> runnable) {
        runnable.forEach(r -> executor.execute(r));
    }

    public static void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    public static ThreadPoolExecutor getExecutor() {
        return executor;
    }
}
