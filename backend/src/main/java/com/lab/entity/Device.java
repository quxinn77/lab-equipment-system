package com.lab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("device")
public class Device implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String model;
    private String spec;
    private String brand;
    private Long categoryId;
    private Long labId;
    private Date purchaseDate;
    private BigDecimal originalValue;
    private String imageUrl;
    private String status;
    private String remark;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    private Date createTime;
    private Date updateTime;
}
