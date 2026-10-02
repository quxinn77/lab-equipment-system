package com.lab.controller;

import com.lab.annotation.RequireRole;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.entity.LoginLog;
import com.lab.entity.OperationLog;
import com.lab.enums.RoleEnum;
import com.lab.service.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/logs")
@RequireRole(RoleEnum.SUPER_ADMIN)
public class LogController {

    @Resource
    private LogService logService;

    @GetMapping("/operations")
    public Result<PageResult<OperationLog>> operations(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String module,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(logService.operationLogs(keyword, module, pageNum, pageSize));
    }

    @GetMapping("/logins")
    public Result<PageResult<LoginLog>> logins(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(logService.loginLogs(pageNum, pageSize));
    }
}
