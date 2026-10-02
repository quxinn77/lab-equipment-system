package com.lab.controller;

import com.lab.annotation.OpLog;
import com.lab.annotation.RequireRole;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.dto.ApproveDTO;
import com.lab.dto.BorrowApplyDTO;
import com.lab.dto.ReturnDTO;
import com.lab.enums.RoleEnum;
import com.lab.service.BorrowService;
import com.lab.vo.BorrowVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/borrows")
public class BorrowController {

    @Resource
    private BorrowService borrowService;

    @OpLog(module = "borrow", operation = "提交借用申请")
    @PostMapping
    public Result<Long> apply(@Valid @RequestBody BorrowApplyDTO dto) {
        return Result.ok(borrowService.apply(dto));
    }

    @GetMapping("/my")
    public Result<PageResult<BorrowVO>> my(@RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(borrowService.myRecords(status, pageNum, pageSize));
    }

    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping
    public Result<PageResult<BorrowVO>> page(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long deviceId,
                                             @RequestParam(required = false) Long labId,
                                             @RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(borrowService.page(keyword, status, deviceId, labId, pageNum, pageSize));
    }

    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/{id}")
    public Result<BorrowVO> detail(@PathVariable Long id) {
        return Result.ok(borrowService.detail(id));
    }

    @OpLog(module = "borrow", operation = "借用审批")
    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @Valid @RequestBody ApproveDTO dto) {
        borrowService.approve(id, dto);
        return Result.ok();
    }

    @OpLog(module = "borrow", operation = "确认取件")
    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/{id}/pickup")
    public Result<Void> pickup(@PathVariable Long id) {
        borrowService.pickup(id);
        return Result.ok();
    }

    @OpLog(module = "borrow", operation = "归还登记")
    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/{id}/return")
    public Result<Void> returnDevice(@PathVariable Long id, @Valid @RequestBody ReturnDTO dto) {
        borrowService.returnDevice(id, dto);
        return Result.ok();
    }

    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) Long deviceId,
                       @RequestParam(required = false) Long labId,
                       HttpServletResponse response) throws Exception {
        borrowService.export(keyword, status, deviceId, labId, response);
    }
}
