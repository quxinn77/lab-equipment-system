package com.lab.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
public class BorrowApplyDTO {

    @NotNull(message = "设备不能为空")
    private Long deviceId;

    @NotNull(message = "开始时间不能为空")
    private Date startTime;

    @NotNull(message = "截止时间不能为空")
    private Date dueTime;

    private String purpose;
}
