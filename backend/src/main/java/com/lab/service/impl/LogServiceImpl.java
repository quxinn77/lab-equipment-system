package com.lab.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.PageResult;
import com.lab.entity.LoginLog;
import com.lab.entity.OperationLog;
import com.lab.mapper.LoginLogMapper;
import com.lab.mapper.OperationLogMapper;
import com.lab.service.LogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class LogServiceImpl implements LogService {

    @Resource
    private OperationLogMapper operationLogMapper;
    @Resource
    private LoginLogMapper loginLogMapper;

    @Override
    public PageResult<OperationLog> operationLogs(String keyword, String module, int pageNum, int pageSize) {
        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(OperationLog::getUsername, keyword)
                    .or().like(OperationLog::getOperation, keyword)
                    .or().like(OperationLog::getDetail, keyword));
        }
        if (StrUtil.isNotBlank(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        wrapper.orderByDesc(OperationLog::getId);
        Page<OperationLog> result = operationLogMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Override
    public PageResult<LoginLog> loginLogs(int pageNum, int pageSize) {
        Page<LoginLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<LoginLog>()
                .orderByDesc(LoginLog::getId);
        Page<LoginLog> result = loginLogMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }
}
