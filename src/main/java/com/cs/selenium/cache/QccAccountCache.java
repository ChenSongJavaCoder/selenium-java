package com.cs.selenium.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.cs.selenium.enums.QccAccount;

import java.util.Arrays;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: CS
 * @Date: 2020/9/30 10:15
 * @Description: 企查查账户缓存
 */
public class QccAccountCache {

    /**
     * 最多用户使用数配置
     * 此配置可以用来动态控制使用用户数的上限，配合失效账号使得可以平滑保证可用账号的使用
     */
    private static final Integer MAX_USER = 6;

    /**
     * 全局下的失效账号缓存
     */
    private static Cache<String, QccAccount> CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .initialCapacity(6)
            .maximumSize(6)
            .build();


    /**
     * 获取不可用的企查查账户
     *
     * @return
     */
    private static Set<QccAccount> unAvailableAccount() {
        return CACHE.asMap().values().stream().collect(Collectors.toSet());
    }

    /**
     * 添加不可用的账户
     *
     * @param account 登陆账户
     */
    public static void addUnAvailableAccount(QccAccount account) {
        CACHE.put(account.getUsername(), account);
    }

    /**
     * 获取可用集合
     */
    public static Stack<QccAccount> availableAccount() {
        Stack<QccAccount> availableAccount = new Stack<>();
        Arrays.stream(QccAccount.values())
                .filter(f -> !unAvailableAccount().contains(f))
                .limit(MAX_USER)
                .forEach(e -> availableAccount.push(e));
        return availableAccount;
    }

}
