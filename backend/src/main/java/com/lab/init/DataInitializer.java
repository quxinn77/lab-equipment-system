package com.lab.init;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.entity.SysUser;
import com.lab.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 初始账号：sys_user 为空时创建三个种子账号
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Resource
    private SysUserMapper userMapper;

    @Override
    public void run(String... args) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<>());
        if (count != null && count > 0) {
            log.info("已存在 {} 个用户，跳过初始账号创建", count);
            return;
        }
        createUser("admin", "admin123", "系统管理员", 3L);
        createUser("labadmin", "lab123", "实验室管理员", 2L);
        createUser("student", "123456", "张同学", 1L);
        log.info("初始账号创建完成：admin/admin123、labadmin/lab123、student/123456");
    }

    private void createUser(String username, String password, String realName, Long roleId) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password));
        user.setRealName(realName);
        user.setRoleId(roleId);
        user.setStatus(1);
        user.setOverdueCount(0);
        userMapper.insert(user);
    }
}
