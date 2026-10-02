package com.lab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.entity.BorrowRecord;
import com.lab.entity.Device;
import com.lab.entity.OverdueRecord;
import com.lab.entity.RepairRecord;
import com.lab.mapper.BorrowRecordMapper;
import com.lab.mapper.DeviceMapper;
import com.lab.mapper.OverdueRecordMapper;
import com.lab.mapper.RepairRecordMapper;
import com.lab.mapper.StatsMapper;
import com.lab.service.StatsService;
import com.lab.vo.MonthCountVO;
import com.lab.vo.NameCountVO;
import com.lab.vo.NameValueVO;
import com.lab.vo.OverviewVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsServiceImpl implements StatsService {

    private static final Map<String, String> STATUS_NAMES = new HashMap<>();

    static {
        STATUS_NAMES.put("IDLE", "空闲");
        STATUS_NAMES.put("BORROWED", "借出");
        STATUS_NAMES.put("REPAIRING", "维修中");
        STATUS_NAMES.put("SCRAPPED", "报废");
        STATUS_NAMES.put("RESERVED", "预留");
    }

    @Resource
    private StatsMapper statsMapper;
    @Resource
    private DeviceMapper deviceMapper;
    @Resource
    private BorrowRecordMapper borrowMapper;
    @Resource
    private RepairRecordMapper repairMapper;
    @Resource
    private OverdueRecordMapper overdueMapper;

    @Override
    public OverviewVO overview() {
        OverviewVO vo = new OverviewVO();
        vo.setDeviceTotal(deviceMapper.selectCount(new LambdaQueryWrapper<Device>()));
        for (NameValueVO item : statsMapper.countDeviceByStatus()) {
            long value = item.getValue() == null ? 0 : item.getValue();
            if ("IDLE".equals(item.getName())) {
                vo.setIdleCount(value);
            } else if ("BORROWED".equals(item.getName())) {
                vo.setBorrowedCount(value);
            } else if ("REPAIRING".equals(item.getName())) {
                vo.setRepairingCount(value);
            } else if ("SCRAPPED".equals(item.getName())) {
                vo.setScrappedCount(value);
            } else if ("RESERVED".equals(item.getName())) {
                vo.setReservedCount(value);
            }
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date monthStart = calendar.getTime();
        vo.setMonthBorrowCount(borrowMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .ge(BorrowRecord::getCreateTime, monthStart)));
        vo.setPendingBorrowCount(borrowMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getStatus, "PENDING")));
        vo.setOverdueCount(borrowMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getStatus, "OVERDUE")));
        vo.setPendingRepairCount(repairMapper.selectCount(new LambdaQueryWrapper<RepairRecord>()
                .eq(RepairRecord::getStatus, "PENDING")));
        return vo;
    }

    @Override
    public List<NameValueVO> deviceByStatus() {
        List<NameValueVO> result = new ArrayList<>();
        for (NameValueVO item : statsMapper.countDeviceByStatus()) {
            NameValueVO vo = new NameValueVO();
            vo.setName(STATUS_NAMES.getOrDefault(item.getName(), item.getName()));
            vo.setValue(item.getValue());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<NameValueVO> deviceByLab() {
        return statsMapper.countDeviceByLab();
    }

    @Override
    public List<MonthCountVO> borrowMonthly(int months) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -(months - 1));
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return statsMapper.countBorrowMonthly(calendar.getTime());
    }

    @Override
    public List<NameCountVO> topDevices(int limit) {
        return statsMapper.topDevices(limit);
    }

    @Override
    public Map<String, Object> overdueStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", overdueMapper.selectCount(null));
        result.put("unhandled", overdueMapper.selectCount(new LambdaQueryWrapper<OverdueRecord>()
                .eq(OverdueRecord::getHandled, 0)));
        result.put("handled", overdueMapper.selectCount(new LambdaQueryWrapper<OverdueRecord>()
                .eq(OverdueRecord::getHandled, 1)));
        result.put("currentOverdueBorrows", borrowMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getStatus, "OVERDUE")));
        return result;
    }

    @Override
    public Map<String, Object> repairSummary() {
        Map<String, Object> result = new HashMap<>();
        List<NameValueVO> byStatus = new ArrayList<>();
        Map<String, String> repairStatusNames = new HashMap<>();
        repairStatusNames.put("PENDING", "待处理");
        repairStatusNames.put("REPAIRING", "维修中");
        repairStatusNames.put("FINISHED", "维修完成");
        repairStatusNames.put("SCRAPPED", "报废");
        for (NameValueVO item : statsMapper.countRepairByStatus()) {
            NameValueVO vo = new NameValueVO();
            vo.setName(repairStatusNames.getOrDefault(item.getName(), item.getName()));
            vo.setValue(item.getValue());
            byStatus.add(vo);
        }
        result.put("byStatus", byStatus);
        result.put("topFaultDevices", statsMapper.topFaultDevices(10));
        return result;
    }

    @Override
    public List<NameCountVO> userRank(int limit) {
        return statsMapper.userRank(limit);
    }
}
