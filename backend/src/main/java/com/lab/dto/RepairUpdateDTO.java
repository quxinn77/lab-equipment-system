package com.lab.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RepairUpdateDTO {

    @NotBlank(message = "报修状态不能为空")
    private String status;

    private String handleRemark;
}
