package com.lab.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 借用记录导出
 */
@Data
public class BorrowExportData {

    @ExcelProperty("申请编号")
    private String recordNo;

    @ExcelProperty("申请人")
    private String userName;

    @ExcelProperty("设备编号")
    private String deviceCode;

    @ExcelProperty("设备名称")
    private String deviceName;

    @ExcelProperty("开始时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @ExcelProperty("截止时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date dueTime;

    @ExcelProperty("实际归还时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date actualReturnTime;

    @ExcelProperty("用途")
    private String purpose;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("审批人")
    private String approver;

    @ExcelProperty("审批备注")
    private String approveRemark;

    @ExcelProperty("归还状况")
    private String returnCondition;

    @ExcelProperty("赔偿金额")
    private BigDecimal compensation;

    @ExcelProperty("申请时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
