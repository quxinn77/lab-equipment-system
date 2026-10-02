package com.lab.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ResetPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
