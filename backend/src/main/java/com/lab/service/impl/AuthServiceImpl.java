package com.lab.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.common.BusinessException;
import com.lab.common.IpUtil;
import com.lab.dto.LoginDTO;
import com.lab.dto.PasswordDTO;
import com.lab.dto.RegisterDTO;
import com.lab.entity.LoginLog;
import com.lab.entity.Role;
import com.lab.entity.SysUser;
import com.lab.enums.RoleEnum;
import com.lab.interceptor.UserContext;
import com.lab.mapper.LoginLogMapper;
import com.lab.mapper.RoleMapper;
import com.lab.mapper.SysUserMapper;
import com.lab.service.AuthService;
import com.lab.util.JwtUtil;
import com.lab.vo.LoginVO;
import com.lab.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private SysUserMapper userMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private LoginLogMapper loginLogMapper;
    @Resource
    private JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        String ip = IpUtil.getIp();
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            LoginLog log = new LoginLog();
            log.setUserId(user == null ? null : user.getId());
            log.setUsername(dto.getUsername());
            log.setIp(ip);
            log.setStatus(0);
            log.setCreateTime(new Date());
            loginLogMapper.insert(log);
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        LoginLog log = new LoginLog();
        log.setUserId(user.getId());
        log.setUsername(user.getUsername());
        log.setIp(ip);
        log.setStatus(1);
        log.setCreateTime(new Date());
        loginLogMapper.insert(log);

        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.create(user.getId(), user.getUsername(), user.getRoleId()));
        vo.setUser(buildUserVO(user));
        return vo;
    }

    @Override
    public void register(RegisterDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("学号/工号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setCollege(dto.getCollege());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRoleId(RoleEnum.STUDENT.getId());
        user.setStatus(1);
        user.setOverdueCount(0);
        userMapper.insert(user);
    }

    @Override
    public UserVO info() {
        Long userId = UserContext.userId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return buildUserVO(user);
    }

    @Override
    public void updatePassword(PasswordDTO dto) {
        Long userId = UserContext.userId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userMapper.updateById(update);
    }

    @Override
    public void logout() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        String auth = attrs.getRequest().getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            // B4：当前 token 加入黑名单，后续请求被拦截器拒绝
            jwtUtil.blacklist(auth.substring(7).trim());
        }
    }

    private UserVO buildUserVO(SysUser user) {
        Role role = roleMapper.selectById(user.getRoleId());
        String roleCode = role == null ? "" : role.getCode();
        String roleName = role == null ? "" : role.getName();
        return UserVO.from(user, roleCode, roleName);
    }
}
