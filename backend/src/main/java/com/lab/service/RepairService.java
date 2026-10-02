package com.lab.service;

import com.lab.common.PageResult;
import com.lab.dto.RepairReportDTO;
import com.lab.dto.RepairUpdateDTO;
import com.lab.vo.RepairVO;

public interface RepairService {

    Long report(RepairReportDTO dto);

    PageResult<RepairVO> myRecords(String status, int pageNum, int pageSize);

    PageResult<RepairVO> page(String status, Long deviceId, int pageNum, int pageSize);

    void update(Long id, RepairUpdateDTO dto);
}
