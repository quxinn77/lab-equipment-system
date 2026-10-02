package com.lab.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储层抽象：本地磁盘 / 云端对象存储
 */
public interface StorageService {

    /**
     * 保存文件，返回访问 URL
     */
    String store(MultipartFile file) throws Exception;
}
