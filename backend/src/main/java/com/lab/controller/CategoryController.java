package com.lab.controller;

import com.lab.annotation.OpLog;
import com.lab.annotation.RequireRole;
import com.lab.common.Result;
import com.lab.dto.CategoryDTO;
import com.lab.entity.DeviceCategory;
import com.lab.enums.RoleEnum;
import com.lab.service.CategoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 分类管理：查看需登录，增删改限 LAB_ADMIN / SUPER_ADMIN
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @GetMapping
    public Result<List<DeviceCategory>> list() {
        return Result.ok(categoryService.list());
    }

    @OpLog(module = "category", operation = "新增分类")
    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CategoryDTO dto) {
        categoryService.create(dto);
        return Result.ok();
    }

    @OpLog(module = "category", operation = "编辑分类")
    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        categoryService.update(id, dto);
        return Result.ok();
    }

    @OpLog(module = "category", operation = "删除分类")
    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
