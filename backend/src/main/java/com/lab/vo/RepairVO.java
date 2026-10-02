package com.lab.vo;

import com.lab.entity.RepairRecord;
import lombok.Data;

import java.util.Date;

@Data
public class RepairVO {

    private Long id;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Long reporterId;
    private String reporter;
    private String faultDesc;
    private String imageUrl;
    private String status;
    private Long handlerId;
    private String handler;
    private String handleRemark;
    private Date finishTime;
    private Date createTime;

    public static RepairVO from(RepairRecord r) {
        RepairVO vo = new RepairVO();
        vo.setId(r.getId());
        vo.setDeviceId(r.getDeviceId());
        vo.setReporterId(r.getReporterId());
        vo.setFaultDesc(r.getFaultDesc());
        vo.setImageUrl(r.getImageUrl());
        vo.setStatus(r.getStatus());
        vo.setHandlerId(r.getHandlerId());
        vo.setHandler(r.getHandler());
        vo.setHandleRemark(r.getHandleRemark());
        vo.setFinishTime(r.getFinishTime());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}
