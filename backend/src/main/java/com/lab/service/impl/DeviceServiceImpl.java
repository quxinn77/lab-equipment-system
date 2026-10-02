package com.lab.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.lab.common.BusinessException;
import com.lab.common.PageResult;
import com.lab.dto.DeviceDTO;
import com.lab.dto.excel.DeviceExportData;
import com.lab.dto.excel.DeviceImportData;
import com.lab.entity.Device;
import com.lab.entity.DeviceCategory;
import com.lab.entity.DeviceLog;
import com.lab.entity.Lab;
import com.lab.interceptor.UserContext;
import com.lab.mapper.DeviceCategoryMapper;
import com.lab.mapper.DeviceLogMapper;
import com.lab.mapper.DeviceMapper;
import com.lab.mapper.LabMapper;
import com.lab.service.DeviceService;
import com.lab.vo.DeviceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeviceServiceImpl implements DeviceService {

    @Resource
    private DeviceMapper deviceMapper;
    @Resource
    private DeviceLogMapper deviceLogMapper;
    @Resource
    private DeviceCategoryMapper categoryMapper;
    @Resource
    private LabMapper labMapper;

    @Override
    public PageResult<DeviceVO> page(String keyword, Long labId, Long categoryId, String status, int pageNum, int pageSize) {
        Page<Device> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Device::getCode, keyword).or().like(Device::getName, keyword));
        }
        if (labId != null) {
            wrapper.eq(Device::getLabId, labId);
        }
        if (categoryId != null) {
            wrapper.eq(Device::getCategoryId, categoryId);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Device::getStatus, status);
        }
        wrapper.orderByDesc(Device::getId);
        Page<Device> result = deviceMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), toVos(result.getRecords()));
    }

    @Override
    public DeviceVO detail(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        List<DeviceVO> vos = toVos(java.util.Collections.singletonList(device));
        return vos.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(DeviceDTO dto) {
        checkCodeUnique(dto.getCode(), null);
        Device device = new Device();
        copyDto(dto, device);
        device.setStatus("IDLE");
        deviceMapper.insert(device);
        saveDeviceLog(device, "CREATE", null, "IDLE", "新增设备");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DeviceDTO dto) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        checkCodeUnique(dto.getCode(), id);
        Device update = new Device();
        update.setId(id);
        copyDto(dto, update);
        deviceMapper.updateById(update);
        Device latest = deviceMapper.selectById(id);
        saveDeviceLog(latest, "UPDATE", device.getStatus(), device.getStatus(), "编辑设备信息");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if ("BORROWED".equals(device.getStatus())) {
            throw new BusinessException("设备处于借出状态，禁止删除");
        }
        deviceMapper.deleteById(id);
        saveDeviceLog(device, "DELETE", device.getStatus(), null, "删除设备（逻辑删除）");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importExcel(MultipartFile file) throws Exception {
        List<DeviceImportData> rows = EasyExcel.read(file.getInputStream())
                .head(DeviceImportData.class).sheet().doReadSync();
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("导入文件为空或格式不正确");
        }
        Map<String, Long> categoryIdMap = new HashMap<>();
        for (DeviceCategory category : categoryMapper.selectList(null)) {
            categoryIdMap.put(category.getName(), category.getId());
        }
        Map<String, Long> labIdMap = new HashMap<>();
        for (Lab lab : labMapper.selectList(null)) {
            labIdMap.put(lab.getCode(), lab.getId());
        }
        int success = 0;
        int fail = 0;
        for (DeviceImportData row : rows) {
            if (StrUtil.isBlank(row.getCode()) || StrUtil.isBlank(row.getName())) {
                fail++;
                continue;
            }
            Long exist = deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                    .eq(Device::getCode, row.getCode()));
            if (exist > 0) {
                fail++;
                continue;
            }
            try {
                Device device = new Device();
                device.setCode(row.getCode().trim());
                device.setName(row.getName().trim());
                device.setModel(row.getModel());
                device.setSpec(row.getSpec());
                device.setBrand(row.getBrand());
                device.setCategoryId(categoryIdMap.get(StrUtil.trim(row.getCategoryName())));
                device.setLabId(labIdMap.get(StrUtil.trim(row.getLabCode())));
                if (StrUtil.isNotBlank(row.getPurchaseDate())) {
                    String dateStr = row.getPurchaseDate().trim();
                    if (dateStr.length() > 10) {
                        dateStr = dateStr.substring(0, 10);
                    }
                    device.setPurchaseDate(new SimpleDateFormat("yyyy-MM-dd").parse(dateStr));
                }
                if (StrUtil.isNotBlank(row.getOriginalValue())) {
                    device.setOriginalValue(new BigDecimal(row.getOriginalValue().trim()));
                }
                device.setStatus("IDLE");
                deviceMapper.insert(device);
                saveDeviceLog(device, "CREATE", null, "IDLE", "Excel批量导入设备");
                success++;
            } catch (Exception e) {
                fail++;
            }
        }
        return "导入完成：成功 " + success + " 条，失败 " + fail + " 条";
    }

    @Override
    public void export(String keyword, Long labId, Long categoryId, String status, HttpServletResponse response) throws Exception {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Device::getCode, keyword).or().like(Device::getName, keyword));
        }
        if (labId != null) {
            wrapper.eq(Device::getLabId, labId);
        }
        if (categoryId != null) {
            wrapper.eq(Device::getCategoryId, categoryId);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Device::getStatus, status);
        }
        wrapper.orderByAsc(Device::getCode);
        List<Device> devices = deviceMapper.selectList(wrapper);

        Map<Long, String> categoryNames = new HashMap<>();
        for (DeviceCategory category : categoryMapper.selectList(null)) {
            categoryNames.put(category.getId(), category.getName());
        }
        Map<Long, String> labNames = new HashMap<>();
        for (Lab lab : labMapper.selectList(null)) {
            labNames.put(lab.getId(), lab.getName());
        }
        List<DeviceExportData> rows = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (Device device : devices) {
            DeviceExportData row = new DeviceExportData();
            row.setCode(device.getCode());
            row.setName(device.getName());
            row.setModel(device.getModel());
            row.setSpec(device.getSpec());
            row.setBrand(device.getBrand());
            row.setCategoryName(device.getCategoryId() == null ? "" : categoryNames.getOrDefault(device.getCategoryId(), ""));
            row.setLabName(device.getLabId() == null ? "" : labNames.getOrDefault(device.getLabId(), ""));
            row.setPurchaseDate(device.getPurchaseDate() == null ? "" : sdf.format(device.getPurchaseDate()));
            row.setOriginalValue(device.getOriginalValue() == null ? "" : device.getOriginalValue().toPlainString());
            row.setStatus(device.getStatus());
            row.setRemark(device.getRemark());
            rows.add(row);
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("设备台账", "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), DeviceExportData.class).sheet("设备台账").doWrite(rows);
    }

    @Override
    public List<DeviceLog> logs(Long deviceId) {
        return deviceLogMapper.selectList(new LambdaQueryWrapper<DeviceLog>()
                .eq(DeviceLog::getDeviceId, deviceId)
                .orderByDesc(DeviceLog::getId));
    }

    @Override
    public void qrcode(Long id, HttpServletResponse response) throws Exception {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        String content = "设备编号:" + device.getCode() + "\n设备名称:" + device.getName()
                + (StrUtil.isNotBlank(device.getModel()) ? "\n型号:" + device.getModel() : "");
        BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 300, 300);
        response.setContentType("image/png");
        OutputStream os = response.getOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", os);
        os.flush();
    }

    private List<DeviceVO> toVos(List<Device> devices) {
        Map<Long, String> categoryNames = new HashMap<>();
        for (DeviceCategory category : categoryMapper.selectList(null)) {
            categoryNames.put(category.getId(), category.getName());
        }
        Map<Long, String> labNames = new HashMap<>();
        for (Lab lab : labMapper.selectList(null)) {
            labNames.put(lab.getId(), lab.getName());
        }
        List<DeviceVO> vos = new ArrayList<>();
        for (Device device : devices) {
            DeviceVO vo = DeviceVO.from(device);
            vo.setCategoryName(device.getCategoryId() == null ? null : categoryNames.get(device.getCategoryId()));
            vo.setLabName(device.getLabId() == null ? null : labNames.get(device.getLabId()));
            vos.add(vo);
        }
        return vos;
    }

    private void copyDto(DeviceDTO dto, Device device) {
        device.setCode(dto.getCode());
        device.setName(dto.getName());
        device.setModel(dto.getModel());
        device.setSpec(dto.getSpec());
        device.setBrand(dto.getBrand());
        device.setCategoryId(dto.getCategoryId());
        device.setLabId(dto.getLabId());
        device.setPurchaseDate(dto.getPurchaseDate());
        device.setOriginalValue(dto.getOriginalValue());
        device.setImageUrl(dto.getImageUrl());
        device.setRemark(dto.getRemark());
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>().eq(Device::getCode, code);
        if (excludeId != null) {
            wrapper.ne(Device::getId, excludeId);
        }
        if (deviceMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("设备编号已存在");
        }
    }

    /** 写设备变更日志 */
    public void saveDeviceLog(Device device, String action, String oldStatus, String newStatus, String detail) {
        DeviceLog log = new DeviceLog();
        log.setDeviceId(device.getId());
        log.setDeviceCode(device.getCode());
        UserContext.LoginUser user = UserContext.get();
        if (user != null) {
            log.setOperatorId(user.getUserId());
            log.setOperator(user.getRealName() != null ? user.getRealName() : user.getUsername());
        } else {
            log.setOperator("系统定时任务");
        }
        log.setAction(action);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setDetail(detail + (StrUtil.isBlank(device.getName()) ? "" : "：" + device.getName()));
        log.setCreateTime(new Date());
        deviceLogMapper.insert(log);
    }
}
