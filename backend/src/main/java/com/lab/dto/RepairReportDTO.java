package com.lab.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class RepairReportDTO {

    @NotNull(message = "设备不能为空")
    private Long deviceId;

    @NotBlank(message = "故障描述不能为空")
    private String faultDesc;

    private String imageUrl;
}
