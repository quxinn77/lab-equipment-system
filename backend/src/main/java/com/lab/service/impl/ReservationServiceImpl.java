package com.lab.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.BusinessException;
import com.lab.common.PageResult;
import com.lab.dto.ApproveDTO;
import com.lab.dto.ReservationApplyDTO;
import com.lab.entity.Device;
import com.lab.entity.Reservation;
import com.lab.entity.SysUser;
import com.lab.interceptor.UserContext;
import com.lab.mapper.DeviceMapper;
import com.lab.mapper.ReservationMapper;
import com.lab.mapper.SysUserMapper;
import com.lab.service.MessageService;
import com.lab.service.ReservationService;
import com.lab.vo.ReservationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class ReservationServiceImpl implements ReservationService {

    @Resource
    private ReservationMapper reservationMapper;
    @Resource
    private DeviceMapper deviceMapper;
    @Resource
    private SysUserMapper userMapper;
    @Resource
    private MessageService messageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(ReservationApplyDTO dto) {
        Long userId = UserContext.userId();
        Device device = deviceMapper.selectById(dto.getDeviceId());
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if (!"IDLE".equals(device.getStatus()) && !"RESERVED".equals(device.getStatus())) {
            throw new BusinessException("设备当前状态为 " + device.getStatus() + "，不可预约");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null
                || !dto.getStartTime().before(dto.getEndTime())) {
            throw new BusinessException("预约时间不合法：开始时间须早于结束时间");
        }
        if (dto.getStartTime().before(new Date(System.currentTimeMillis() - 60_000L))) {
            throw new BusinessException("预约开始时间不能早于当前时间");
        }
        // 同设备时段冲突校验：与任何 PENDING/APPROVED 且时间区间相交的预约均拒绝
        Long conflict = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getDeviceId, dto.getDeviceId())
                .in(Reservation::getStatus, Arrays.asList("PENDING", "APPROVED"))
                .lt(Reservation::getStartTime, dto.getEndTime())
                .gt(Reservation::getEndTime, dto.getStartTime()));
        if (conflict > 0) {
            throw new BusinessException("该设备在所选时段已存在待审核或已通过的预约，请更换时段");
        }
        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setDeviceId(dto.getDeviceId());
        reservation.setStartTime(dto.getStartTime());
        reservation.setEndTime(dto.getEndTime());
        reservation.setRemark(dto.getRemark());
        reservation.setStatus("PENDING");
        reservation.setCreateTime(new Date());
        reservationMapper.insert(reservation);
        return reservation.getId();
    }

    @Override
    public PageResult<ReservationVO> myRecords(String status, int pageNum, int pageSize) {
        return doPage(UserContext.userId(), status, pageNum, pageSize);
    }

    @Override
    public PageResult<ReservationVO> page(String status, int pageNum, int pageSize) {
        return doPage(null, status, pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, ApproveDTO dto) {
        UserContext.LoginUser approver = UserContext.get();
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new BusinessException("预约不存在");
        }
        if (!"PENDING".equals(reservation.getStatus())) {
            throw new BusinessException("该预约已处理，当前状态：" + reservation.getStatus());
        }
        if (Boolean.TRUE.equals(dto.getApproved())) {
            reservation.setStatus("APPROVED");
        } else {
            reservation.setStatus("REJECTED");
        }
        reservation.setApproverId(approver.getUserId());
        reservation.setApproveTime(new Date());
        reservation.setApproveRemark(dto.getRemark());
        reservationMapper.updateById(reservation);

        Device device = deviceMapper.selectById(reservation.getDeviceId());
        String deviceName = device == null ? "" : device.getName();
        String resultText = Boolean.TRUE.equals(dto.getApproved()) ? "已通过" : "被驳回";
        messageService.send(reservation.getUserId(), "预约" + resultText,
                "您对设备 " + deviceName + " 的预约申请" + resultText
                        + (StrUtil.isBlank(dto.getRemark()) ? "" : "，理由：" + dto.getRemark()), "SYSTEM");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Long userId = UserContext.userId();
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new BusinessException("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException("只能取消自己的预约");
        }
        if (!"PENDING".equals(reservation.getStatus()) && !"APPROVED".equals(reservation.getStatus())) {
            throw new BusinessException("该预约当前状态不可取消：" + reservation.getStatus());
        }
        reservation.setStatus("CANCELLED");
        reservation.setUpdateTime(new Date());
        reservationMapper.updateById(reservation);
    }

    private PageResult<ReservationVO> doPage(Long userId, String status, int pageNum, int pageSize) {
        Page<Reservation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Reservation::getUserId, userId);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Reservation::getStatus, status);
        }
        wrapper.orderByDesc(Reservation::getId);
        Page<Reservation> result = reservationMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), toVos(result.getRecords()));
    }

    private List<ReservationVO> toVos(List<Reservation> reservations) {
        List<ReservationVO> vos = new ArrayList<>();
        for (Reservation reservation : reservations) {
            ReservationVO vo = ReservationVO.from(reservation);
            Device device = reservation.getDeviceId() == null ? null : deviceMapper.selectById(reservation.getDeviceId());
            if (device != null) {
                vo.setDeviceCode(device.getCode());
                vo.setDeviceName(device.getName());
            }
            SysUser user = reservation.getUserId() == null ? null : userMapper.selectById(reservation.getUserId());
            if (user != null) {
                vo.setUserName(user.getRealName());
            }
            vos.add(vo);
        }
        return vos;
    }
}
