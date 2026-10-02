package com.lab.service;

import com.lab.common.PageResult;
import com.lab.dto.LabDTO;
import com.lab.entity.Lab;

import java.util.List;

public interface LabService {

    PageResult<Lab> page(String keyword, int pageNum, int pageSize);

    List<Lab> listAll();

    void create(LabDTO dto);

    void update(Long id, LabDTO dto);

    void delete(Long id);
}
