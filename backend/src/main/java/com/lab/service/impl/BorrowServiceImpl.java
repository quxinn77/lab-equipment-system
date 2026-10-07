package com.lab.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.BusinessException;
import com.lab.common.PageResult;
import com.lab.dto.ApproveDTO;
import com.lab.dto.BorrowApplyDTO;
import com.lab.dto.ReturnDTO;
import com.lab.dto.excel.BorrowExportData;
import com.lab.entity.BorrowRecord;
import com.lab.entity.Device;
import com.lab.entity.RepairRecord;
import com.lab.entity.SysConfig;
import com.lab.entity.SysUser;
import com.lab.interceptor.UserContext;
import com.lab.mapper.BorrowRecordMapper;
import com.lab.mapper.DeviceMapper;
import com.lab.mapper.RepairRecordMapper;
import com.lab.mapper.SysConfigMapper;
import com.lab.mapper.SysUserMapper;
import com.lab.service.BorrowService;
import com.lab.service.DeviceService;
import com.lab.service.MessageService;
import com.lab.vo.BorrowVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BorrowServiceImpl implements BorrowService {

    @Resource
    private BorrowRecordMapper borrowMapper;
    @Resource
    private DeviceMapper deviceMapper;
    @Resource
    private SysUserMapper userMapper;
    @Resource
    private SysConfigMapper configMapper;
    @Resource
    private RepairRecordMapper repairMapper;
    @Resource
    private MessageService messageService;
    @Resource
    private DeviceService deviceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(BorrowApplyDTO dto) {
        Long userId = UserContext.userId();
        Device device = deviceMapper.selectById(dto.getDeviceId());
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if ("REPAIRING".equals(device.getStatus()) || "SCRAPPED".equals(device.getStatus())) {
            throw new BusinessException("设备当前状态为 " + device.getStatus() + "，不可提交借用申请");
        }
        // B3：可借数 = 总库存 - (PENDING + APPROVED 的借用单数)
        int totalQty = device.getTotalQty() == null ? 1 : device.getTotalQty();
        Long occupied = borrowMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getDeviceId, device.getId())
                .in(BorrowRecord::getStatus, java.util.Arrays.asList("PENDING", "APPROVED")));
        int availableQty = totalQty - (occupied == null ? 0 : occupied.intValue());
        if (availableQty <= 0) {
            throw new BusinessException("该设备库存不足：总库存 " + totalQty + "，已全部被预约或借出");
        }
        if (dto.getStartTime() == null || dto.getDueTime() == null
                || !dto.getStartTime().before(dto.getDueTime())) {
            throw new BusinessException("借用时间不合法：开始时间须早于截止时间");
        }
        if (dto.getStartTime().before(new Date(System.currentTimeMillis() - 60_000L))) {
            throw new BusinessException("开始时间不能早于当前时间");
        }
        // 逾期未还禁止申请
        String limitApply = getConfigValue("overdue.limit.apply", "1");
        if ("1".equals(limitApply)) {
            Long overdueCount = borrowMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                    .eq(BorrowRecord::getUserId, userId)
                    .eq(BorrowRecord::getStatus, "OVERDUE"));
            if (overdueCount > 0) {
                throw new BusinessException("您存在 " + overdueCount + " 笔逾期未还记录，禁止提交新的借用申请");
            }
        }
        // 逾期次数超上限禁止申请
        SysUser user = userMapper.selectById(userId);
        String maxTimesStr = getConfigValue("overdue.max.times", "3");
        int maxTimes;
        try {
            maxTimes = Integer.parseInt(maxTimesStr.trim());
        } catch (NumberFormatException e) {
            maxTimes = 3;
        }
        if (user != null && user.getOverdueCount() != null && user.getOverdueCount() >= maxTimes) {
            throw new BusinessException("您的累计逾期次数（" + user.getOverdueCount() + "）已达上限 " + maxTimes + "，禁止借用");
        }

        BorrowRecord record = new BorrowRecord();
        record.setRecordNo(generateRecordNo());
        record.setUserId(userId);
        record.setDeviceId(dto.getDeviceId());
        record.setStartTime(dto.getStartTime());
        record.setDueTime(dto.getDueTime());
        record.setPurpose(dto.getPurpose());
        record.setStatus("PENDING");
        record.setCreateTime(new Date());
        borrowMapper.insert(record);

        messageService.send(userId, "借用申请已提交",
                "您的借用申请 " + record.getRecordNo() + "（" + device.getName() + "）已提交，等待管理员审批。", "BORROW");
        return record.getId();
    }

    @Override
    public PageResult<BorrowVO> myRecords(String status, int pageNum, int pageSize) {
        Long userId = UserContext.userId();
        return doPage(userId, null, status, null, null, null, pageNum, pageSize);
    }

    @Override
    public PageResult<BorrowVO> page(String keyword, String status, Long deviceId, Long labId, int pageNum, int pageSize) {
        return doPage(null, keyword, status, deviceId, labId, null, pageNum, pageSize);
    }

    @Override
    public BorrowVO detail(Long id) {
        BorrowRecord record = borrowMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("借用记录不存在");
        }
        return toVos(new ArrayList<BorrowRecord>() {{ add(record); }}).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, ApproveDTO dto) {
        UserContext.LoginUser approver = UserContext.get();
        BorrowRecord record = borrowMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("借用记录不存在");
        }
        if (!"PENDING".equals(record.getStatus())) {
            throw new BusinessException("该申请已处理，当前状态：" + record.getStatus());
        }
        Device device = deviceMapper.selectById(record.getDeviceId());
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        Date now = new Date();
        if (Boolean.TRUE.equals(dto.getApproved())) {
            // 一台设备同一时间只能被一人借用：审批时设备必须仍为 IDLE
            if (!"IDLE".equals(device.getStatus())) {
                throw new BusinessException("设备状态已变化（当前 " + device.getStatus() + "），无法通过该申请");
            }
            record.setStatus("APPROVED");
            device.setStatus("RESERVED");
            deviceMapper.updateById(device);
            deviceService.saveDeviceLog(device, "STATUS_CHANGE", "IDLE", "RESERVED",
                    "借用审批通过，单据 " + record.getRecordNo() + "（预留）");
        } else {
            record.setStatus("REJECTED");
        }
        record.setApproverId(approver.getUserId());
        record.setApprover(approver.getRealName() != null ? approver.getRealName() : approver.getUsername());
        record.setApproveTime(now);
        record.setApproveRemark(dto.getRemark());
        borrowMapper.updateById(record);

        String resultText = Boolean.TRUE.equals(dto.getApproved()) ? "已通过" : "被驳回";
        String remarkText = StrUtil.isBlank(dto.getRemark()) ? "" : "，理由：" + dto.getRemark();
        messageService.send(record.getUserId(), "借用申请" + resultText,
                "您的借用申请 " + record.getRecordNo() + "（" + device.getName() + "）" + resultText + remarkText, "BORROW");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickup(Long id) {
        BorrowRecord record = borrowMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("借用记录不存在");
        }
        if (!"APPROVED".equals(record.getStatus())) {
            throw new BusinessException("仅审批通过的申请可确认取件，当前状态：" + record.getStatus());
        }
        Device device = deviceMapper.selectById(record.getDeviceId());
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if (!"RESERVED".equals(device.getStatus())) {
            throw new BusinessException("设备状态已变化（当前 " + device.getStatus() + "），无法取件");
        }
        record.setStatus("BORROWED");
        record.setUpdateTime(new Date());
        borrowMapper.updateById(record);

        device.setStatus("BORROWED");
        deviceMapper.updateById(device);
        deviceService.saveDeviceLog(device, "STATUS_CHANGE", "RESERVED", "BORROWED",
                "借用取件，单据 " + record.getRecordNo());
        messageService.send(record.getUserId(), "设备已借出",
                "您申请的设备已取件借出，单据 " + record.getRecordNo() + "，请按时归还。", "BORROW");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnDevice(Long id, ReturnDTO dto) {
        UserContext.LoginUser operator = UserContext.get();
        BorrowRecord record = borrowMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("借用记录不存在");
        }
        // B1：仅申请人本人或管理员可归还
        boolean isAdmin = "LAB_ADMIN".equals(operator.getRoleCode())
                || "SUPER_ADMIN".equals(operator.getRoleCode());
        if (!isAdmin && !record.getUserId().equals(operator.getUserId())) {
            throw new BusinessException("只能归还本人的借用记录");
        }
        // B2：允许 APPROVED 直接归还（配合去取件环节），兼容 BORROWED/OVERDUE
        String recordStatus = record.getStatus();
        if (!"APPROVED".equals(recordStatus)
                && !"BORROWED".equals(recordStatus)
                && !"OVERDUE".equals(recordStatus)) {
            throw new BusinessException("该借用单当前状态不可归还：" + recordStatus);
        }
        String condition = dto.getCondition();
        if (!"INTACT".equals(condition) && !"DAMAGED".equals(condition)) {
            throw new BusinessException("归还状况必须为 INTACT 或 DAMAGED");
        }
        Device device = deviceMapper.selectById(record.getDeviceId());
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        Date now = new Date();
        String oldDeviceStatus = device.getStatus();
        record.setActualReturnTime(now);
        record.setReturnCondition(condition);
        record.setReturnRemark(dto.getRemark());
        record.setCompensation(dto.getCompensation());
        record.setStatus("RETURNED");
        record.setUpdateTime(now);
        borrowMapper.updateById(record);

        if ("DAMAGED".equals(condition)) {
            // 归还损坏：设备保持维修中并自动生成报修单
            RepairRecord repair = new RepairRecord();
            repair.setDeviceId(device.getId());
            repair.setReporterId(record.getUserId());
            repair.setFaultDesc("归还时发现设备损坏（借用单 " + record.getRecordNo() + "）"
                    + (StrUtil.isBlank(dto.getRemark()) ? "" : "：" + dto.getRemark()));
            repair.setStatus("REPAIRING");
            repair.setHandlerId(operator.getUserId());
            repair.setHandler(operator.getRealName() != null ? operator.getRealName() : operator.getUsername());
            repair.setHandleRemark("归还登记时自动创建报修单，赔偿金额：" + dto.getCompensation());
            repair.setCreateTime(now);
            repairMapper.insert(repair);
            device.setStatus("REPAIRING");
            deviceMapper.updateById(device);
            deviceService.saveDeviceLog(device, "STATUS_CHANGE", oldDeviceStatus, "REPAIRING",
                    "归还损坏，自动报修 #" + repair.getId());
        } else {
            device.setStatus("IDLE");
            deviceMapper.updateById(device);
            deviceService.saveDeviceLog(device, "STATUS_CHANGE", oldDeviceStatus, "IDLE",
                    "归还完好，单据 " + record.getRecordNo());
        }

        String damageText = "DAMAGED".equals(condition) ? "（设备损坏，已生成报修单，赔偿金额 " + dto.getCompensation() + "）" : "（设备完好）";
        messageService.send(record.getUserId(), "设备归还登记完成",
                "您的借用单 " + record.getRecordNo() + " 归还登记完成" + damageText, "BORROW");
    }

    @Override
    public void export(String keyword, String status, Long deviceId, Long labId, HttpServletResponse response) throws Exception {
        PageResult<BorrowVO> result = doPage(null, keyword, status, deviceId, labId, null, 1, 100000);
        List<BorrowExportData> rows = new ArrayList<>();
        for (BorrowVO vo : result.getRecords()) {
            BorrowExportData row = new BorrowExportData();
            row.setRecordNo(vo.getRecordNo());
            row.setUserName(vo.getUserName());
            row.setDeviceCode(vo.getDeviceCode());
            row.setDeviceName(vo.getDeviceName());
            row.setStartTime(vo.getStartTime());
            row.setDueTime(vo.getDueTime());
            row.setActualReturnTime(vo.getActualReturnTime());
            row.setPurpose(vo.getPurpose());
            row.setStatus(vo.getStatus());
            row.setApprover(vo.getApprover());
            row.setApproveRemark(vo.getApproveRemark());
            row.setReturnCondition(vo.getReturnCondition());
            row.setCompensation(vo.getCompensation());
            row.setCreateTime(vo.getCreateTime());
            rows.add(row);
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("借用记录", "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), BorrowExportData.class).sheet("借用记录").doWrite(rows);
    }

    private PageResult<BorrowVO> doPage(Long userId, String keyword, String status, Long deviceId,
                                        Long labId, Long excludeUserId, int pageNum, int pageSize) {
        Page<BorrowRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(BorrowRecord::getUserId, userId);
        }
        if (StrUtil.isNotBlank(keyword)) {
            // 编号模糊 或 申请人姓名模糊
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, keyword))
                    .stream().map(SysUser::getId).collect(java.util.stream.Collectors.toList());
            wrapper.and(w -> {
                w.like(BorrowRecord::getRecordNo, keyword);
                if (!userIds.isEmpty()) {
                    w.or().in(BorrowRecord::getUserId, userIds);
                }
            });
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(BorrowRecord::getStatus, status);
        }
        if (deviceId != null) {
            wrapper.eq(BorrowRecord::getDeviceId, deviceId);
        }
        if (labId != null) {
            List<Long> deviceIds = deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                            .eq(Device::getLabId, labId))
                    .stream().map(Device::getId).collect(java.util.stream.Collectors.toList());
            if (deviceIds.isEmpty()) {
                return PageResult.of(0, new ArrayList<>());
            }
            wrapper.in(BorrowRecord::getDeviceId, deviceIds);
        }
        wrapper.orderByDesc(BorrowRecord::getId);
        Page<BorrowRecord> result = borrowMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), toVos(result.getRecords()));
    }

    private List<BorrowVO> toVos(List<BorrowRecord> records) {
        Map<Long, Device> deviceMap = new HashMap<>();
        Map<Long, SysUser> userMap = new HashMap<>();
        for (BorrowRecord record : records) {
            if (record.getDeviceId() != null && !deviceMap.containsKey(record.getDeviceId())) {
                Device device = deviceMapper.selectById(record.getDeviceId());
                deviceMap.put(record.getDeviceId(), device);
            }
            if (record.getUserId() != null && !userMap.containsKey(record.getUserId())) {
                userMap.put(record.getUserId(), userMapper.selectById(record.getUserId()));
            }
        }
        List<BorrowVO> vos = new ArrayList<>();
        for (BorrowRecord record : records) {
            BorrowVO vo = BorrowVO.from(record);
            Device device = deviceMap.get(record.getDeviceId());
            if (device != null) {
                vo.setDeviceCode(device.getCode());
                vo.setDeviceName(device.getName());
            }
            SysUser user = userMap.get(record.getUserId());
            if (user != null) {
                vo.setUserName(user.getRealName());
            }
            vos.add(vo);
        }
        return vos;
    }

    private String getConfigValue(String key, String defaultValue) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));
        return config == null || StrUtil.isBlank(config.getConfigValue()) ? defaultValue : config.getConfigValue();
    }

    private String generateRecordNo() {
        return "BR" + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%04d", (int) (Math.random() * 10000));
    }
}
