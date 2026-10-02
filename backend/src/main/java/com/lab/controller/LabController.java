package com.lab.controller;

import com.lab.annotation.OpLog;
import com.lab.annotation.RequireRole;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.dto.LabDTO;
import com.lab.entity.Lab;
import com.lab.enums.RoleEnum;
import com.lab.service.LabService;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.util.List;

@RestController
@RequestMapping("/api/labs")
@RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
public class LabController {

    @Resource
    private LabService labService;

    @GetMapping
    public Result<PageResult<Lab>> page(@RequestParam(required = false) String keyword,
                                        @RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(labService.page(keyword, pageNum, pageSize));
    }

    @GetMapping("/all")
    public Result<List<Lab>> listAll() {
        return Result.ok(labService.listAll());
    }

    @OpLog(module = "lab", operation = "新增实验室")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody LabDTO dto) {
        labService.create(dto);
        return Result.ok();
    }

    @OpLog(module = "lab", operation = "编辑实验室")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody LabDTO dto) {
        labService.update(id, dto);
        return Result.ok();
    }

    @OpLog(module = "lab", operation = "删除实验室")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        labService.delete(id);
        return Result.ok();
    }
}
