package com.lab.controller;

import com.lab.annotation.OpLog;
import com.lab.annotation.RequireRole;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.dto.ApproveDTO;
import com.lab.dto.ReservationApplyDTO;
import com.lab.enums.RoleEnum;
import com.lab.service.ReservationService;
import com.lab.vo.ReservationVO;
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
@RequestMapping("/api/reservations")
public class ReservationController {

    @Resource
    private ReservationService reservationService;

    @OpLog(module = "reservation", operation = "提交预约申请")
    @PostMapping
    public Result<Long> apply(@Valid @RequestBody ReservationApplyDTO dto) {
        return Result.ok(reservationService.apply(dto));
    }

    @GetMapping("/my")
    public Result<PageResult<ReservationVO>> my(@RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(reservationService.myRecords(status, pageNum, pageSize));
    }

    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping
    public Result<PageResult<ReservationVO>> page(@RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(reservationService.page(status, pageNum, pageSize));
    }

    @OpLog(module = "reservation", operation = "预约审核")
    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @Valid @RequestBody ApproveDTO dto) {
        reservationService.approve(id, dto);
        return Result.ok();
    }

    @OpLog(module = "reservation", operation = "取消预约")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        return Result.ok();
    }
}
