package com.lab.controller;

import com.lab.annotation.OpLog;
import com.lab.annotation.RequireRole;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.dto.RepairReportDTO;
import com.lab.dto.RepairUpdateDTO;
import com.lab.enums.RoleEnum;
import com.lab.service.RepairService;
import com.lab.vo.RepairVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/repairs")
public class RepairController {

    @Resource
    private RepairService repairService;

    @OpLog(module = "repair", operation = "提交报修")
    @PostMapping
    public Result<Long> report(@Valid @RequestBody RepairReportDTO dto) {
        return Result.ok(repairService.report(dto));
    }

    @GetMapping("/my")
    public Result<PageResult<RepairVO>> my(@RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(repairService.myRecords(status, pageNum, pageSize));
    }

    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping
    public Result<PageResult<RepairVO>> page(@RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long deviceId,
                                             @RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(repairService.page(status, deviceId, pageNum, pageSize));
    }

    @OpLog(module = "repair", operation = "处理报修")
    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RepairUpdateDTO dto) {
        repairService.update(id, dto);
        return Result.ok();
    }
}
