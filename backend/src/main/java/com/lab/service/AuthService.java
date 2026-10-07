package com.lab.service;

import com.lab.dto.LoginDTO;
import com.lab.dto.PasswordDTO;
import com.lab.dto.RegisterDTO;
import com.lab.vo.LoginVO;
import com.lab.vo.UserVO;

public interface AuthService {

    LoginVO login(LoginDTO dto);

    void register(RegisterDTO dto);

    UserVO info();

    void updatePassword(PasswordDTO dto);

    /** 退出登录：当前 token 加入黑名单失效 */
    void logout();
}
