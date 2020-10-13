package com.cs.selenium.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 公共三方开票公司明细信息表
 * </p>
 *
 * @author lic
 * @date 2020-07-15
 */
@Data
public class KpPublicInvoiceCustomerInfoDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;

    /**
     * 公共三方开票公司表id
     */
    private Long kpPublicInvoiceCustomerInfoId;

    /**
     * 地址、电话
     */
    private String dzdh;

    /**
     * 开户银行及账户
     */
    private String openBankAndAccount;

    /**
     * 手机号码
     */
    private String phoneNumber;

    /**
     * 邮箱地址
     */
    private String email;

}
