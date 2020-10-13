package com.cs.selenium.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: CS
 * @Date: 2020/9/23 10:04
 * @Description: 企业信息处理状态
 */
@Getter
@AllArgsConstructor
public enum ProcessState {

    UNPROCESSED(-1, "待处理"),
    SUCCESS(2, "处理成功"),
    FAILURE(1, "处理失败"),
    ;

    /**
     * 处理状态码
     */
    private Integer code;

    /**
     * 描述信息
     */
    private String description;


}
