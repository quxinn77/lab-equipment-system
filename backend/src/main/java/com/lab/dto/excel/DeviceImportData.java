package com.lab.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 设备导入模板：设备编号、设备名称、型号、规格、品牌、分类名称、实验室编号、购置日期、原值
 */
@Data
public class DeviceImportData {

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

    @ExcelProperty("分类名称")
    private String categoryName;

    @ExcelProperty("实验室编号")
    private String labCode;

    @ExcelProperty("购置日期")
    private String purchaseDate;

    @ExcelProperty("原值")
    private String originalValue;
}
