package com.lab.controller;

import com.lab.common.Result;
import com.lab.entity.Role;
import com.lab.mapper.RoleMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 角色全量列表（下拉用）
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Resource
    private RoleMapper roleMapper;

    @GetMapping
    public Result<List<Role>> list() {
        return Result.ok(roleMapper.selectList(null));
    }
}
