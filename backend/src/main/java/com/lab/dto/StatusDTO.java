package com.lab.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class StatusDTO {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
