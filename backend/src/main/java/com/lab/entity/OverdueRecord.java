package com.lab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("overdue_record")
public class OverdueRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long borrowId;
    private Long userId;
    private Long deviceId;
    private Date dueTime;
    private Integer overdueDays;
    private Integer handled;
    private Date createTime;
}
