package com.lab.service;

import com.lab.common.PageResult;
import com.lab.dto.ApproveDTO;
import com.lab.dto.BorrowApplyDTO;
import com.lab.dto.ReturnDTO;
import com.lab.vo.BorrowVO;

import javax.servlet.http.HttpServletResponse;

public interface BorrowService {

    /** 学生提交借用申请 */
    Long apply(BorrowApplyDTO dto);

    /** 我的借用记录 */
    PageResult<BorrowVO> myRecords(String status, int pageNum, int pageSize);

    /** 全部借用记录（管理员） */
    PageResult<BorrowVO> page(String keyword, String status, Long deviceId, Long labId, int pageNum, int pageSize);

    /** 审批 */
    void approve(Long id, ApproveDTO dto);

    /** 确认取件 */
    void pickup(Long id);

    /** 归还登记 */
    void returnDevice(Long id, ReturnDTO dto);

    /** 导出 */
    void export(String keyword, String status, Long deviceId, Long labId, HttpServletResponse response) throws Exception;

    /** 单条详情 */
    BorrowVO detail(Long id);
}
