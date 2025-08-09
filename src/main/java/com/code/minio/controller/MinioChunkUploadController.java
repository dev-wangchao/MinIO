package com.code.minio.controller;

import com.code.minio.service.impl.MinioChunkUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/minio")
public class MinioChunkUploadController {

    @Autowired
    private MinioChunkUploadService minioChunkUploadService;

    /**
     * 初始化分片上传
     */
    @PostMapping("init")
    public String initiateMultipartUpload(MultipartFile file) throws Exception {
        return minioChunkUploadService.multipartUpload(file);
    }


    /**
     * 合并分片文件
     */
    @PostMapping("merge")
    public void mergeMultipartUpload(List<String> partFileNames) throws Exception {
        minioChunkUploadService.multipartMerge(partFileNames);
    }


}
