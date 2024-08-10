package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.service.UploadFileService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
@Author zhc
@Create 2024/7/23 14:32
*/
@Api(tags = "文件上传")
@RestController
@RequestMapping("/admin/product")
public class FileUploadController {
    @Autowired
    private UploadFileService uploadFileService;
    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/fileUpload")
    public Result fileUpload(MultipartFile file){
        // 调用服务层方法：ctrl+alt+M 提取方法
        String url = uploadFileService.fileUpload(file);
        // 返回数据
        return Result.ok(url);
    }


}
