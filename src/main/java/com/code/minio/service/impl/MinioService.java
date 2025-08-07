package com.code.minio.service.impl;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;
}
