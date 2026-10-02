package com.lab.service;

import com.lab.common.PageResult;
import com.lab.dto.DeviceDTO;
import com.lab.entity.Device;
import com.lab.entity.DeviceLog;
import com.lab.vo.DeviceVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface DeviceService {

    PageResult<DeviceVO> page(String keyword, Long labId, Long categoryId, String status, int pageNum, int pageSize);

    DeviceVO detail(Long id);

    void create(DeviceDTO dto);

    void update(Long id, DeviceDTO dto);

    void delete(Long id);

    String importExcel(MultipartFile file) throws Exception;

    void export(String keyword, Long labId, Long categoryId, String status, HttpServletResponse response) throws Exception;

    List<DeviceLog> logs(Long deviceId);

    void qrcode(Long id, HttpServletResponse response) throws Exception;

    /** 写设备变更日志（供其他模块复用） */
    void saveDeviceLog(Device device, String action, String oldStatus, String newStatus, String detail);
}
