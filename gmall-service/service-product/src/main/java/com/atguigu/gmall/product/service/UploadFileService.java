package com.atguigu.gmall.product.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * @Author zhc
 * @Create 2024/7/23 15:50
 */
public interface UploadFileService {
    /**
     * 文件上传
     * @param file
     * @return
     */
    String fileUpload(MultipartFile file);
}
