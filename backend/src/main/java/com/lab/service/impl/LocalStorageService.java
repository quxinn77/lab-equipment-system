package com.lab.service.impl;

import cn.hutool.core.util.StrUtil;
import com.lab.common.BusinessException;
import com.lab.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 本地磁盘存储（默认）
 */
@Service
@ConditionalOnProperty(name = "lab.storage-type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final List<String> ALLOWED_EXTENSIONS =
            Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp", "xlsx", "xls", "pdf", "doc", "docx");

    private static final long MAX_SIZE = 10L * 1024 * 1024;

    @Value("${lab.storage-path:./upload-files}")
    private String storagePath;

    @Override
    public String store(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("不支持的文件类型：" + ext + "，仅支持 " + ALLOWED_EXTENSIONS);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("文件大小不能超过10MB");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File dir = new File(storagePath).getAbsoluteFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("存储目录创建失败：" + dir.getAbsolutePath());
        }
        File dest = new File(dir, filename);
        file.transferTo(dest);
        return "/api/files/" + filename;
    }

    public String getStoragePath() {
        return storagePath;
    }
}
