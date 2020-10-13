package com.cs.selenium.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 公共三方开票公司信息表
 * </p>
 *
 * @author jackhu
 * @date 2020-06-24
 */
@Data
public class KpPublicInvoiceCustomerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 纳税人识别号
     */
    private String nsrsbh;

    /**
     * 开票代码
     */
    private String kpdm;

    /**
     * 客户号
     */
    private Integer customId;

    /**
     * 客户号
     */
    private Integer version;
}
