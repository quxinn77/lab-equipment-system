package com.lab.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 设备台账导出
 */
@Data
public class DeviceExportData {

    @ExcelProperty("设备编号")
    private String code;

    @ExcelProperty("设备名称")
    private String name;

    @ExcelProperty("型号")
    private String model;

    @ExcelProperty("规格")
    private String spec;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("分类")
    private String categoryName;

    @ExcelProperty("实验室")
    private String labName;

    @ExcelProperty("购置日期")
    private String purchaseDate;

    @ExcelProperty("原值")
    private String originalValue;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("备注")
    private String remark;
}
