package com.lab.service;

import com.lab.common.PageResult;
import com.lab.dto.ApproveDTO;
import com.lab.dto.ReservationApplyDTO;
import com.lab.vo.ReservationVO;

public interface ReservationService {

    Long apply(ReservationApplyDTO dto);

    PageResult<ReservationVO> myRecords(String status, int pageNum, int pageSize);

    PageResult<ReservationVO> page(String status, int pageNum, int pageSize);

    void approve(Long id, ApproveDTO dto);

    void cancel(Long id);
}
