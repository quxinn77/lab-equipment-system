package com.lab.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.BusinessException;
import com.lab.common.PageResult;
import com.lab.dto.LabDTO;
import com.lab.entity.Device;
import com.lab.entity.Lab;
import com.lab.mapper.DeviceMapper;
import com.lab.mapper.LabMapper;
import com.lab.service.LabService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class LabServiceImpl implements LabService {

    @Resource
    private LabMapper labMapper;
    @Resource
    private DeviceMapper deviceMapper;

    @Override
    public PageResult<Lab> page(String keyword, int pageNum, int pageSize) {
        Page<Lab> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Lab> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Lab::getCode, keyword).or().like(Lab::getName, keyword));
        }
        wrapper.orderByAsc(Lab::getId);
        Page<Lab> result = labMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Override
    public List<Lab> listAll() {
        return labMapper.selectList(new LambdaQueryWrapper<Lab>()
                .eq(Lab::getStatus, 1)
                .orderByAsc(Lab::getId));
    }

    @Override
    public void create(LabDTO dto) {
        checkCodeUnique(dto.getCode(), null);
        Lab lab = new Lab();
        lab.setCode(dto.getCode());
        lab.setName(dto.getName());
        lab.setLocation(dto.getLocation());
        lab.setManager(dto.getManager());
        lab.setDescription(dto.getDescription());
        lab.setStatus(1);
        labMapper.insert(lab);
    }

    @Override
    public void update(Long id, LabDTO dto) {
        if (labMapper.selectById(id) == null) {
            throw new BusinessException("实验室不存在");
        }
        checkCodeUnique(dto.getCode(), id);
        Lab lab = new Lab();
        lab.setId(id);
        lab.setCode(dto.getCode());
        lab.setName(dto.getName());
        lab.setLocation(dto.getLocation());
        lab.setManager(dto.getManager());
        lab.setDescription(dto.getDescription());
        labMapper.updateById(lab);
    }

    @Override
    public void delete(Long id) {
        if (labMapper.selectById(id) == null) {
            throw new BusinessException("实验室不存在");
        }
        Long count = deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .eq(Device::getLabId, id));
        if (count > 0) {
            throw new BusinessException("该实验室下存在 " + count + " 台设备，请先转移设备后再删除");
        }
        labMapper.deleteById(id);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<Lab> wrapper = new LambdaQueryWrapper<Lab>().eq(Lab::getCode, code);
        if (excludeId != null) {
            wrapper.ne(Lab::getId, excludeId);
        }
        if (labMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("实验室编号已存在");
        }
    }
}
