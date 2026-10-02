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
}
