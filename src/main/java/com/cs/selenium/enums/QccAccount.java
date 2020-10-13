package com.cs.selenium.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: CS
 * @Date: 2020/9/27 9:58
 * @Description: 企查查账户
 */
@Getter
@AllArgsConstructor
public enum QccAccount {
    wq("15257106712", "A12345678"),
    u1("18370608678", "A12345678"),
    u2("18368815603", "A12345678"),
    hll("15868102541", "1q2w3e4r"),
    srj("18257172325", "A12345678"),
    cs("13349257109", "cs123456"),
    ;

    private String username;
    private String password;
}
