package com.lab.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.BusinessException;
import com.lab.common.PageResult;
import com.lab.dto.RepairReportDTO;
import com.lab.dto.RepairUpdateDTO;
import com.lab.entity.Device;
import com.lab.entity.RepairRecord;
import com.lab.entity.SysUser;
import com.lab.interceptor.UserContext;
import com.lab.mapper.DeviceMapper;
import com.lab.mapper.RepairRecordMapper;
import com.lab.mapper.SysUserMapper;
import com.lab.service.DeviceService;
import com.lab.service.MessageService;
import com.lab.service.RepairService;
import com.lab.vo.RepairVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class RepairServiceImpl implements RepairService {

    @Resource
    private RepairRecordMapper repairMapper;
    @Resource
    private DeviceMapper deviceMapper;
    @Resource
    private SysUserMapper userMapper;
    @Resource
    private MessageService messageService;
    @Resource
    private DeviceService deviceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long report(RepairReportDTO dto) {
        Long userId = UserContext.userId();
        Device device = deviceMapper.selectById(dto.getDeviceId());
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if ("SCRAPPED".equals(device.getStatus())) {
            throw new BusinessException("设备已报废，无需报修");
        }
        RepairRecord repair = new RepairRecord();
        repair.setDeviceId(dto.getDeviceId());
        repair.setReporterId(userId);
        repair.setFaultDesc(dto.getFaultDesc());
        repair.setImageUrl(dto.getImageUrl());
        repair.setStatus("PENDING");
        repair.setCreateTime(new Date());
        repairMapper.insert(repair);

        String oldStatus = device.getStatus();
        device.setStatus("REPAIRING");
        deviceMapper.updateById(device);
        deviceService.saveDeviceLog(device, "STATUS_CHANGE", oldStatus, "REPAIRING",
                "故障报修 #" + repair.getId() + "：" + StrUtil.maxLength(dto.getFaultDesc(), 100));
        return repair.getId();
    }

    @Override
    public PageResult<RepairVO> myRecords(String status, int pageNum, int pageSize) {
        return doPage(UserContext.userId(), null, status, pageNum, pageSize);
    }

    @Override
    public PageResult<RepairVO> page(String status, Long deviceId, int pageNum, int pageSize) {
        return doPage(null, deviceId, status, pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RepairUpdateDTO dto) {
        UserContext.LoginUser handler = UserContext.get();
        RepairRecord repair = repairMapper.selectById(id);
        if (repair == null) {
            throw new BusinessException("报修记录不存在");
        }
        String newStatus = dto.getStatus();
        if (!"REPAIRING".equals(newStatus) && !"FINISHED".equals(newStatus) && !"SCRAPPED".equals(newStatus)) {
            throw new BusinessException("报修状态仅允许 REPAIRING / FINISHED / SCRAPPED");
        }
        Device device = deviceMapper.selectById(repair.getDeviceId());
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        String oldDeviceStatus = device.getStatus();
        repair.setStatus(newStatus);
        repair.setHandlerId(handler.getUserId());
        repair.setHandler(handler.getRealName() != null ? handler.getRealName() : handler.getUsername());
        repair.setHandleRemark(dto.getHandleRemark());
        if ("FINISHED".equals(newStatus)) {
            repair.setFinishTime(new Date());
            device.setStatus("IDLE");
        } else if ("SCRAPPED".equals(newStatus)) {
            device.setStatus("SCRAPPED");
        } else {
            device.setStatus("REPAIRING");
        }
        repairMapper.updateById(repair);
        deviceMapper.updateById(device);
        deviceService.saveDeviceLog(device, "STATUS_CHANGE", oldDeviceStatus, device.getStatus(),
                "报修处理 #" + repair.getId() + " → " + newStatus);

        String text = "FINISHED".equals(newStatus) ? "已维修完成，设备恢复可用" :
                ("SCRAPPED".equals(newStatus) ? "已判定报废" : "维修中");
        messageService.send(repair.getReporterId(), "报修进度更新",
                "您提交的报修单 #" + repair.getId() + "（" + device.getName() + "）" + text
                        + (StrUtil.isBlank(dto.getHandleRemark()) ? "" : "：" + dto.getHandleRemark()), "REPAIR");
    }

    private PageResult<RepairVO> doPage(Long reporterId, Long deviceId, String status, int pageNum, int pageSize) {
        Page<RepairRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RepairRecord> wrapper = new LambdaQueryWrapper<>();
        if (reporterId != null) {
            wrapper.eq(RepairRecord::getReporterId, reporterId);
        }
        if (deviceId != null) {
            wrapper.eq(RepairRecord::getDeviceId, deviceId);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(RepairRecord::getStatus, status);
        }
        wrapper.orderByDesc(RepairRecord::getId);
        Page<RepairRecord> result = repairMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), toVos(result.getRecords()));
    }

    private List<RepairVO> toVos(List<RepairRecord> records) {
        List<RepairVO> vos = new ArrayList<>();
        for (RepairRecord record : records) {
            RepairVO vo = RepairVO.from(record);
            Device device = record.getDeviceId() == null ? null : deviceMapper.selectById(record.getDeviceId());
            if (device != null) {
                vo.setDeviceCode(device.getCode());
                vo.setDeviceName(device.getName());
            }
            SysUser reporter = record.getReporterId() == null ? null : userMapper.selectById(record.getReporterId());
            if (reporter != null) {
                vo.setReporter(reporter.getRealName());
            }
            vos.add(vo);
        }
        return vos;
    }
}
