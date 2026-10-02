package com.lab.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class DeviceDTO {

    private Long id;

    @NotBlank(message = "设备编号不能为空")
    private String code;

    @NotBlank(message = "设备名称不能为空")
    private String name;

    private String model;
    private String spec;
    private String brand;
    private Long categoryId;
    private Long labId;
    private Date purchaseDate;
    private BigDecimal originalValue;
    private String imageUrl;
    private String remark;
}
