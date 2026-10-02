package com.lab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("borrow_record")
public class BorrowRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordNo;
    private Long userId;
    private Long deviceId;
    private Date startTime;
    private Date dueTime;
    private Date actualReturnTime;
    private String purpose;
    private String status;
    private Long approverId;
    private String approver;
    private Date approveTime;
    private String approveRemark;
    private String returnCondition;
    private String returnRemark;
    private BigDecimal compensation;
    private Date createTime;
    private Date updateTime;
}
