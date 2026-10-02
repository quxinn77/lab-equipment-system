package com.lab.service;

import com.lab.common.PageResult;
import com.lab.entity.LoginLog;
import com.lab.entity.OperationLog;

public interface LogService {

    PageResult<OperationLog> operationLogs(String keyword, String module, int pageNum, int pageSize);

    PageResult<LoginLog> loginLogs(int pageNum, int pageSize);
}
