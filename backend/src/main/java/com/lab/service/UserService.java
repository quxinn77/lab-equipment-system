package com.lab.service;

import com.lab.common.PageResult;
import com.lab.dto.ResetPasswordDTO;
import com.lab.dto.StatusDTO;
import com.lab.dto.UserDTO;
import com.lab.vo.UserVO;

public interface UserService {

    PageResult<UserVO> page(String keyword, Long roleId, Integer status, int pageNum, int pageSize);

    void create(UserDTO dto);

    void update(Long id, UserDTO dto);

    void delete(Long id);

    void resetPassword(Long id, ResetPasswordDTO dto);

    void updateStatus(Long id, StatusDTO dto);
}
