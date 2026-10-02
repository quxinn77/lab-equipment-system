package com.lab.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
public class ReturnDTO {

    @NotBlank(message = "归还状况不能为空")
    private String condition;

    private String remark;

    private BigDecimal compensation;
}
