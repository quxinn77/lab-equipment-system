package com.lab.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.entity.BorrowRecord;
import com.lab.entity.Device;
import com.lab.entity.Message;
import com.lab.entity.OverdueRecord;
import com.lab.entity.Reservation;
import com.lab.entity.SysUser;
import com.lab.mapper.BorrowRecordMapper;
import com.lab.mapper.DeviceMapper;
import com.lab.mapper.MessageMapper;
import com.lab.mapper.OverdueRecordMapper;
import com.lab.mapper.ReservationMapper;
import com.lab.mapper.SysUserMapper;
import com.lab.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 定时任务：
 * 1. 借用超期：BORROWED 且 now > due_time → OVERDUE + overdue_record + 用户 overdue_count+1 + 站内信
 * 2. 预约过期：APPROVED 预约到期未取 → EXPIRED，设备 RESERVED→IDLE
 */
@Slf4j
@Component
public class OverdueTask {

    @Resource
    private BorrowRecordMapper borrowMapper;
    @Resource
    private OverdueRecordMapper overdueMapper;
    @Resource
    private SysUserMapper userMapper;
    @Resource
    private MessageMapper messageMapper;
    @Resource
    private DeviceMapper deviceMapper;
    @Resource
    private ReservationMapper reservationMapper;
    @Resource
    private DeviceService deviceService;

    @Scheduled(fixedRate = 30 * 60 * 1000, initialDelay = 15 * 1000)
    @Transactional(rollbackFor = Exception.class)
    public void scanOverdue() {
        Date now = new Date();
        List<BorrowRecord> overdueList = borrowMapper.selectList(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getStatus, "BORROWED")
                .lt(BorrowRecord::getDueTime, now));
        if (overdueList.isEmpty()) {
            log.debug("逾期扫描：无新增逾期借用单");
        }
        for (BorrowRecord record : overdueList) {
            record.setStatus("OVERDUE");
            record.setUpdateTime(now);
            borrowMapper.updateById(record);

            long days = (now.getTime() - record.getDueTime().getTime()) / (24 * 3600 * 1000L);
            OverdueRecord overdueRecord = new OverdueRecord();
            overdueRecord.setBorrowId(record.getId());
            overdueRecord.setUserId(record.getUserId());
            overdueRecord.setDeviceId(record.getDeviceId());
            overdueRecord.setDueTime(record.getDueTime());
            overdueRecord.setOverdueDays((int) days);
            overdueRecord.setHandled(0);
            overdueRecord.setCreateTime(now);
            overdueMapper.insert(overdueRecord);

            SysUser user = userMapper.selectById(record.getUserId());
            if (user != null) {
                SysUser update = new SysUser();
                update.setId(user.getId());
                update.setOverdueCount((user.getOverdueCount() == null ? 0 : user.getOverdueCount()) + 1);
                userMapper.updateById(update);
            }

            Message message = new Message();
            message.setUserId(record.getUserId());
            message.setTitle("借用已逾期提醒");
            message.setContent("您的借用单 " + record.getRecordNo() + " 已超过归还截止时间（逾期 "
                    + days + " 天），请尽快归还设备。");
            message.setType("OVERDUE");
            message.setIsRead(0);
            message.setCreateTime(now);
            messageMapper.insert(message);
            log.info("借用单 {} 标记逾期", record.getRecordNo());
        }
    }

    @Scheduled(fixedRate = 30 * 60 * 1000, initialDelay = 30 * 1000)
    @Transactional(rollbackFor = Exception.class)
    public void expireReservations() {
        Date now = new Date();
        // APPROVED 预约到期未取 → EXPIRED，释放设备
        List<Reservation> expired = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, "APPROVED")
                .lt(Reservation::getEndTime, now));
        for (Reservation reservation : expired) {
            reservation.setStatus("EXPIRED");
            reservation.setUpdateTime(now);
            reservationMapper.updateById(reservation);
            releaseDeviceIfNeeded(reservation.getDeviceId(), now);
            Message message = new Message();
            message.setUserId(reservation.getUserId());
            message.setTitle("预约已过期");
            message.setContent("您的预约申请（#" + reservation.getId() + "）已超过结束时间，系统已自动过期。");
            message.setType("SYSTEM");
            message.setIsRead(0);
            message.setCreateTime(now);
            messageMapper.insert(message);
            log.info("预约 #{} 已过期", reservation.getId());
        }
        // PENDING 预约过期同样置 EXPIRED
        List<Reservation> pendingExpired = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, "PENDING")
                .lt(Reservation::getEndTime, now));
        for (Reservation reservation : pendingExpired) {
            reservation.setStatus("EXPIRED");
            reservation.setUpdateTime(now);
            reservationMapper.updateById(reservation);
        }
    }

    private void releaseDeviceIfNeeded(Long deviceId, Date now) {
        if (deviceId == null) {
            return;
        }
        Device device = deviceMapper.selectById(deviceId);
        if (device != null && "RESERVED".equals(device.getStatus())) {
            device.setStatus("IDLE");
            deviceMapper.updateById(device);
            deviceService.saveDeviceLog(device, "STATUS_CHANGE", "RESERVED", "IDLE", "预约过期自动释放设备");
        }
    }
}
