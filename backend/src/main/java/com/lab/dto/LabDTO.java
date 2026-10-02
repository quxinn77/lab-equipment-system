package com.lab.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LabDTO {

    private Long id;

    @NotBlank(message = "实验室编号不能为空")
    private String code;

    @NotBlank(message = "实验室名称不能为空")
    private String name;

    private String location;
    private String manager;
    private String description;
}
