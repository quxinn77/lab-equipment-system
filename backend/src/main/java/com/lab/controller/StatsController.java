package com.lab.controller;

import com.lab.annotation.RequireRole;
import com.lab.common.Result;
import com.lab.enums.RoleEnum;
import com.lab.service.StatsService;
import com.lab.vo.MonthCountVO;
import com.lab.vo.NameCountVO;
import com.lab.vo.NameValueVO;
import com.lab.vo.OverviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Resource
    private StatsService statsService;

    @GetMapping("/overview")
    public Result<OverviewVO> overview() {
        return Result.ok(statsService.overview());
    }

    @GetMapping("/device-by-status")
    public Result<List<NameValueVO>> deviceByStatus() {
        return Result.ok(statsService.deviceByStatus());
    }

    @GetMapping("/device-by-lab")
    public Result<List<NameValueVO>> deviceByLab() {
        return Result.ok(statsService.deviceByLab());
    }

    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/borrow-monthly")
    public Result<List<MonthCountVO>> borrowMonthly(@RequestParam(defaultValue = "6") int months) {
        return Result.ok(statsService.borrowMonthly(months));
    }

    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/top-devices")
    public Result<List<NameCountVO>> topDevices(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statsService.topDevices(limit));
    }

    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/overdue")
    public Result<Map<String, Object>> overdue() {
        return Result.ok(statsService.overdueStats());
    }

    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/repair-summary")
    public Result<Map<String, Object>> repairSummary() {
        return Result.ok(statsService.repairSummary());
    }

    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/user-rank")
    public Result<List<NameCountVO>> userRank(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statsService.userRank(limit));
    }
}
