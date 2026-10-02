package com.lab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.common.BusinessException;
import com.lab.dto.CategoryDTO;
import com.lab.entity.Device;
import com.lab.entity.DeviceCategory;
import com.lab.mapper.DeviceCategoryMapper;
import com.lab.mapper.DeviceMapper;
import com.lab.service.CategoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Resource
    private DeviceCategoryMapper categoryMapper;
    @Resource
    private DeviceMapper deviceMapper;

    @Override
    public List<DeviceCategory> list() {
        return categoryMapper.selectList(new LambdaQueryWrapper<DeviceCategory>()
                .orderByAsc(DeviceCategory::getId));
    }

    @Override
    public void create(CategoryDTO dto) {
        DeviceCategory category = new DeviceCategory();
        category.setName(dto.getName());
        category.setRemark(dto.getRemark());
        categoryMapper.insert(category);
    }

    @Override
    public void update(Long id, CategoryDTO dto) {
        if (categoryMapper.selectById(id) == null) {
            throw new BusinessException("分类不存在");
        }
        DeviceCategory category = new DeviceCategory();
        category.setId(id);
        category.setName(dto.getName());
        category.setRemark(dto.getRemark());
        categoryMapper.updateById(category);
    }

    @Override
    public void delete(Long id) {
        if (categoryMapper.selectById(id) == null) {
            throw new BusinessException("分类不存在");
        }
        Long count = deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .eq(Device::getCategoryId, id));
        if (count > 0) {
            throw new BusinessException("该分类下存在 " + count + " 台设备，无法删除");
        }
        categoryMapper.deleteById(id);
    }
}
