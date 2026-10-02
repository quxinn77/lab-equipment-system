package com.lab.controller;

import com.lab.annotation.OpLog;
import com.lab.annotation.RequireRole;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.dto.ResetPasswordDTO;
import com.lab.dto.StatusDTO;
import com.lab.dto.UserDTO;
import com.lab.enums.RoleEnum;
import com.lab.service.UserService;
import com.lab.vo.UserVO;
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

/**
 * 用户管理：LAB_ADMIN 可查看（分页），增删改仅 SUPER_ADMIN
 */
@RestController
@RequestMapping("/api/users")
@RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping
    public Result<PageResult<UserVO>> page(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long roleId,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(userService.page(keyword, roleId, status, pageNum, pageSize));
    }

    @OpLog(module = "user", operation = "新增用户")
    @RequireRole(RoleEnum.SUPER_ADMIN)
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserDTO dto) {
        userService.create(dto);
        return Result.ok();
    }

    @OpLog(module = "user", operation = "编辑用户")
    @RequireRole(RoleEnum.SUPER_ADMIN)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserDTO dto) {
        userService.update(id, dto);
        return Result.ok();
    }

    @OpLog(module = "user", operation = "删除用户")
    @RequireRole(RoleEnum.SUPER_ADMIN)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @OpLog(module = "user", operation = "重置密码")
    @RequireRole(RoleEnum.SUPER_ADMIN)
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(id, dto);
        return Result.ok();
    }

    @OpLog(module = "user", operation = "启用/禁用用户")
    @RequireRole(RoleEnum.SUPER_ADMIN)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        userService.updateStatus(id, dto);
        return Result.ok();
    }
}
