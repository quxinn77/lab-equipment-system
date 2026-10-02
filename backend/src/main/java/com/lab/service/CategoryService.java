package com.lab.service;

import com.lab.dto.CategoryDTO;
import com.lab.entity.DeviceCategory;

import java.util.List;

public interface CategoryService {

    List<DeviceCategory> list();

    void create(CategoryDTO dto);

    void update(Long id, CategoryDTO dto);

    void delete(Long id);
}
