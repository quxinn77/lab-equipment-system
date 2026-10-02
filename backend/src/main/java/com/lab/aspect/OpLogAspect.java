package com.lab.aspect;

import com.lab.annotation.OpLog;
import com.lab.common.IpUtil;
import com.lab.entity.OperationLog;
import com.lab.interceptor.UserContext;
import com.lab.mapper.OperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 操作日志切面：拦截 @OpLog 注解方法，成功执行后写入 operation_log
 */
@Slf4j
@Aspect
@Component
public class OpLogAspect {

    @Resource
    private OperationLogMapper operationLogMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint pjp, OpLog opLog) throws Throwable {
        Object result = pjp.proceed();
        try {
            OperationLog entity = new OperationLog();
            UserContext.LoginUser user = UserContext.get();
            if (user != null) {
                entity.setUserId(user.getUserId());
                entity.setUsername(user.getUsername());
            }
            entity.setModule(opLog.module());
            entity.setOperation(opLog.operation());
            entity.setDetail(opLog.detail());
            entity.setIp(IpUtil.getIp());
            operationLogMapper.insert(entity);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
        return result;
    }
}
