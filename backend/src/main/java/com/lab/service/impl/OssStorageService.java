package com.lab.service.impl;

import com.lab.common.BusinessException;
import com.lab.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 云端对象存储骨架（阿里云 OSS / 腾讯云 COS）
 * 通过配置 lab.storage-type=oss 启用，连接参数使用环境变量占位：
 * OSS_ENDPOINT / OSS_ACCESS_KEY / OSS_SECRET_KEY / OSS_BUCKET
 */
@Service
@ConditionalOnProperty(name = "lab.storage-type", havingValue = "oss")
public class OssStorageService implements StorageService {

    @Value("${OSS_ENDPOINT:}")
    private String endpoint;

    @Value("${OSS_ACCESS_KEY:}")
    private String accessKey;

    @Value("${OSS_SECRET_KEY:}")
    private String secretKey;

    @Value("${OSS_BUCKET:}")
    private String bucket;

    @Override
    public String store(MultipartFile file) throws Exception {
        // TODO: 引入 OSS/COS SDK 后实现上传逻辑：
        //  1. 使用 accessKey/secretKey 构建客户端
        //  2. file.getInputStream() 上传到 bucket
        //  3. 返回公网访问 URL（如 https://{bucket}.{endpoint}/{filename}）
        throw new BusinessException("OSS云存储暂未接入，请将 lab.storage-type 配置为 local");
    }
}
