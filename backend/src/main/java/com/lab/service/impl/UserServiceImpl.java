package com.lab.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.BusinessException;
import com.lab.common.PageResult;
import com.lab.dto.ResetPasswordDTO;
import com.lab.dto.StatusDTO;
import com.lab.dto.UserDTO;
import com.lab.entity.Role;
import com.lab.entity.SysUser;
import com.lab.enums.RoleEnum;
import com.lab.interceptor.UserContext;
import com.lab.mapper.RoleMapper;
import com.lab.mapper.SysUserMapper;
import com.lab.service.UserService;
import com.lab.vo.UserVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private SysUserMapper userMapper;
    @Resource
    private RoleMapper roleMapper;

    @Override
    public PageResult<UserVO> page(String keyword, Long roleId, Integer status, int pageNum, int pageSize) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
        }
        if (roleId != null) {
            wrapper.eq(SysUser::getRoleId, roleId);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getId);
        Page<SysUser> result = userMapper.selectPage(page, wrapper);

        Map<Long, Role> roleMap = loadRoleMap();
        List<UserVO> vos = new java.util.ArrayList<>();
        for (SysUser user : result.getRecords()) {
            Role role = roleMap.get(user.getRoleId());
            vos.add(UserVO.from(user, role == null ? "" : role.getCode(), role == null ? "" : role.getName()));
        }
        return PageResult.of(result.getTotal(), vos);
    }

    @Override
    public void create(UserDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("学号/工号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(StrUtil.isBlank(dto.getPassword()) ? "123456" : dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setCollege(dto.getCollege());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRoleId(dto.getRoleId());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setOverdueCount(0);
        userMapper.insert(user);
    }

    @Override
    public void update(Long id, UserDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!user.getUsername().equals(dto.getUsername())) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, dto.getUsername()));
            if (count > 0) {
                throw new BusinessException("学号/工号已存在");
            }
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setUsername(dto.getUsername());
        update.setRealName(dto.getRealName());
        update.setCollege(dto.getCollege());
        update.setPhone(dto.getPhone());
        update.setEmail(dto.getEmail());
        update.setRoleId(dto.getRoleId());
        update.setStatus(dto.getStatus());
        userMapper.updateById(update);
    }

    @Override
    public void delete(Long id) {
        if (id != null && id.equals(UserContext.userId())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.deleteById(id);
    }

    @Override
    public void resetPassword(Long id, ResetPasswordDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userMapper.updateById(update);
    }

    @Override
    public void updateStatus(Long id, StatusDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(dto.getStatus());
        userMapper.updateById(update);
    }

    private Map<Long, Role> loadRoleMap() {
        Map<Long, Role> map = new HashMap<>();
        List<Role> roles = roleMapper.selectList(null);
        for (Role role : roles) {
            map.put(role.getId(), role);
        }
        return map;
    }
}
