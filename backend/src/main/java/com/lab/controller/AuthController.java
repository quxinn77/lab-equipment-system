package com.lab.controller;

import com.lab.annotation.OpLog;
import com.lab.common.Result;
import com.lab.dto.LoginDTO;
import com.lab.dto.PasswordDTO;
import com.lab.dto.RegisterDTO;
import com.lab.service.AuthService;
import com.lab.vo.LoginVO;
import com.lab.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @OpLog(module = "auth", operation = "登录", detail = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.ok(authService.info());
    }

    @OpLog(module = "auth", operation = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordDTO dto) {
        authService.updatePassword(dto);
        return Result.ok();
    }
}
