package com.lab.controller;

import com.lab.annotation.OpLog;
import com.lab.annotation.RequireRole;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.dto.DeviceDTO;
import com.lab.entity.DeviceLog;
import com.lab.enums.RoleEnum;
import com.lab.service.DeviceService;
import com.lab.vo.DeviceVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Resource
    private DeviceService deviceService;

    @GetMapping
    public Result<PageResult<DeviceVO>> page(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long labId,
                                             @RequestParam(required = false) Long categoryId,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(deviceService.page(keyword, labId, categoryId, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<DeviceVO> detail(@PathVariable Long id) {
        return Result.ok(deviceService.detail(id));
    }

    @OpLog(module = "device", operation = "新增设备")
    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @PostMapping
    public Result<Void> create(@Valid @RequestBody DeviceDTO dto) {
        deviceService.create(dto);
        return Result.ok();
    }

    @OpLog(module = "device", operation = "编辑设备")
    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DeviceDTO dto) {
        deviceService.update(id, dto);
        return Result.ok();
    }

    @OpLog(module = "device", operation = "删除设备")
    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return Result.ok();
    }

    @OpLog(module = "device", operation = "Excel导入设备")
    @RequireRole({RoleEnum.LAB_ADMIN, RoleEnum.SUPER_ADMIN})
    @PostMapping("/import")
    public Result<String> importExcel(@RequestParam("file") MultipartFile file) throws Exception {
        return Result.ok(deviceService.importExcel(file));
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long labId,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String status,
                       HttpServletResponse response) throws Exception {
        deviceService.export(keyword, labId, categoryId, status, response);
    }

    @GetMapping("/{id}/logs")
    public Result<List<DeviceLog>> logs(@PathVariable Long id) {
        return Result.ok(deviceService.logs(id));
    }

    @GetMapping("/qrcode/{id}")
    public void qrcode(@PathVariable Long id, HttpServletResponse response) throws Exception {
        deviceService.qrcode(id, response);
    }
}
