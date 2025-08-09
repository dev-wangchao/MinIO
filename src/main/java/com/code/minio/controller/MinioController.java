package com.code.minio.controller;

import com.code.minio.config.MinioProperties;
import com.code.minio.service.impl.MinioService;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("minio")
@Slf4j
public class MinioController {
    @Autowired
    private MinioService minioService;
    @Autowired
    MinioProperties minioProperties;
    @Autowired
    MinioClient minioClient;
    public final String ImageType = "image";
    public final String PdfType = "pdf";
    public final String BigType = "big";

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getDefaultBucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getDefaultBucket()).build());
                log.info("创建bucket成功：{}" , minioProperties.getDefaultBucket());
            } else {
                log.info("bucket已存在：{}" , minioProperties.getDefaultBucket());
            }
        } catch (Exception e) {
            log.error("初始化bucket失败：{}" , e.getMessage());
        }
    }

    /**
     * 用户头像上传
     * @return
     */
    @PostMapping("image")
    public String uploadImage(MultipartFile file) throws Exception{
        String fileName = getObjName(file.getOriginalFilename(), ImageType);
        try(InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getDefaultBucket())
                    .object(fileName)
                    // 文件流；文件的总大小；每个分片大小，-1表示默认分片5或者64MB
                    .stream(inputStream, file.getSize(), -1)
                    .build());
        }
        String imageUrl = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(minioProperties.getDefaultBucket())
                .object(fileName)
                .method(Method.GET)
                .expiry(minioProperties.getPresignedUrlExpiry(), TimeUnit.DAYS)
                .build());
        return imageUrl;
    }



    /**
     * 用户合同上传
     * @return
     */
    @PostMapping("pdf")
    public String uploadPdf(MultipartFile file) throws Exception{
        String fileName = getObjName(file.getOriginalFilename(), PdfType);
        try(InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getDefaultBucket())
                    .object(fileName)
                    // 文件流；文件的总大小；每个分片的大小，-1表示默认分片5或者64MB
                    .stream(inputStream, file.getSize(), -1)
                    .build());
        }
        // 获取文件对象进行验证
        GetObjectResponse object = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioProperties.getDefaultBucket())
                .object(fileName)
                .build());
        log.debug("文件对象已验证：{}");
        return getObjectUrl(fileName);
    }






    public String getObjName(String fileName, String bizType) {
        return String.format("%s/%s/%s",
                bizType, LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), fileName);
    }


    private String getObjectUrl(String fileName) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(minioProperties.getDefaultBucket())
                .object(fileName)
                .method(Method.GET)
                .expiry(minioProperties.getPresignedUrlExpiry(), TimeUnit.DAYS)
                .build());
    }


}
