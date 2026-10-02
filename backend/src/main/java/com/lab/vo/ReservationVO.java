package com.lab.vo;

import com.lab.entity.Reservation;
import lombok.Data;

import java.util.Date;

@Data
public class ReservationVO {

    private Long id;
    private Long userId;
    private String userName;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Date startTime;
    private Date endTime;
    private String status;
    private String remark;
    private Long approverId;
    private String approver;
    private Date approveTime;
    private String approveRemark;
    private Date createTime;

    public static ReservationVO from(Reservation r) {
        ReservationVO vo = new ReservationVO();
        vo.setId(r.getId());
        vo.setUserId(r.getUserId());
        vo.setDeviceId(r.getDeviceId());
        vo.setStartTime(r.getStartTime());
        vo.setEndTime(r.getEndTime());
        vo.setStatus(r.getStatus());
        vo.setRemark(r.getRemark());
        vo.setApproverId(r.getApproverId());
        vo.setApproveTime(r.getApproveTime());
        vo.setApproveRemark(r.getApproveRemark());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}
