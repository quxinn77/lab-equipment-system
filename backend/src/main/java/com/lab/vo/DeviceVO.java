package com.lab.vo;

import com.lab.entity.Device;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class DeviceVO {

    private Long id;
    private String code;
    private String name;
    private String model;
    private String spec;
    private String brand;
    private Long categoryId;
    private String categoryName;
    private Long labId;
    private String labName;
    private Date purchaseDate;
    private BigDecimal originalValue;
    private String imageUrl;
    private String status;
    private String remark;
    private Date createTime;
    private Date updateTime;

    public static DeviceVO from(Device device) {
        DeviceVO vo = new DeviceVO();
        vo.setId(device.getId());
        vo.setCode(device.getCode());
        vo.setName(device.getName());
        vo.setModel(device.getModel());
        vo.setSpec(device.getSpec());
        vo.setBrand(device.getBrand());
        vo.setCategoryId(device.getCategoryId());
        vo.setLabId(device.getLabId());
        vo.setPurchaseDate(device.getPurchaseDate());
        vo.setOriginalValue(device.getOriginalValue());
        vo.setImageUrl(device.getImageUrl());
        vo.setStatus(device.getStatus());
        vo.setRemark(device.getRemark());
        vo.setCreateTime(device.getCreateTime());
        vo.setUpdateTime(device.getUpdateTime());
        return vo;
    }
}
