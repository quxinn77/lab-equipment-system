package com.lab.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UserDTO {

    private Long id;

    @NotBlank(message = "学号/工号不能为空")
    private String username;

    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private String college;
    private String phone;
    private String email;

    @NotNull(message = "角色不能为空")
    private Long roleId;

    private Integer status;
}
