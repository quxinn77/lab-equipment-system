package com.lab.vo;

import com.lab.entity.BorrowRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class BorrowVO {

    private Long id;
    private String recordNo;
    private Long userId;
    private String userName;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
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

    public static BorrowVO from(BorrowRecord record) {
        BorrowVO vo = new BorrowVO();
        vo.setId(record.getId());
        vo.setRecordNo(record.getRecordNo());
        vo.setUserId(record.getUserId());
        vo.setDeviceId(record.getDeviceId());
        vo.setStartTime(record.getStartTime());
        vo.setDueTime(record.getDueTime());
        vo.setActualReturnTime(record.getActualReturnTime());
        vo.setPurpose(record.getPurpose());
        vo.setStatus(record.getStatus());
        vo.setApproverId(record.getApproverId());
        vo.setApprover(record.getApprover());
        vo.setApproveTime(record.getApproveTime());
        vo.setApproveRemark(record.getApproveRemark());
        vo.setReturnCondition(record.getReturnCondition());
        vo.setReturnRemark(record.getReturnRemark());
        vo.setCompensation(record.getCompensation());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }
}
