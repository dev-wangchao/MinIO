package com.code.minio.service.impl;

import com.code.minio.config.MinioProperties;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MinioChunkUploadService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MinioAsyncClient minioAsyncClient;
    @Autowired
    private MinioProperties minioProperties;


    public String multipartUpload(MultipartFile file) throws Exception{
        // 计算分片大小
        int partSize = calculateOptimalChunkSize(file.getSize());
        String newFileName = String.format("%s/%s",
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), file.getOriginalFilename());
        log.info("构建分片文件名: {}", newFileName);
        // 分片上传
        try(InputStream inputStream = file.getInputStream()){
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getDefaultBucket())
                    .object(newFileName)
                    .stream(inputStream, file.getSize(), partSize)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("分片上传失败");
        }
        // 获取上传的预签名URL
        String presignedObjectUrl = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(minioProperties.getDefaultBucket())
                .object(newFileName)
                .method(Method.GET)
                .expiry(minioProperties.getPresignedUrlExpiry(), TimeUnit.MINUTES)
                .build());
        log.info("获取上传的预签名URL: {}", presignedObjectUrl);
        return newFileName;
    }






    /**
     * 动态计算最优分片大小
     *
     * @param fileSize 文件大小（字节）
     * @return 最优分片大小（字节）
     */
    public int calculateOptimalChunkSize(long fileSize) {
        // 基础分片大小配置（单位：字节）
        final int MIN_CHUNK_SIZE = 5 * 1024 * 1024;   // 5MB（MinIO要求的最小分片）
        final int BASE_CHUNK_SIZE = 10 * 1024 * 1024; // 10MB（标准分片）
        final int LARGE_CHUNK_SIZE = 50 * 1024 * 1024; // 50MB（大文件分片）
        final int MAX_CHUNK_SIZE = 100 * 1024 * 1024; // 100MB（最大分片）

        // 目标分片数量范围（基于性能优化）
        final int MIN_CHUNKS = 10;  // 最小分片数
        final int MAX_CHUNKS = 200; // 最大分片数
        final int IDEAL_CHUNKS = 50; // 理想分片数

        // 基于文件大小的动态计算
        if (fileSize <= 10 * 1024 * 1024) {         // <10MB
            // 小文件不分片
            return (int) fileSize;
        } else if (fileSize <= 100 * 1024 * 1024) { // 10-100MB
            return BASE_CHUNK_SIZE;
        } else if (fileSize <= 1024 * 1024 * 1024) { // 100MB-1GB
            return LARGE_CHUNK_SIZE;
        } else {                                   // >1GB
            // 3. 基于目标分片数量计算
            long calculatedByChunkCount = fileSize / IDEAL_CHUNKS;

            // 确保在合理范围内
            int chunkSize = (int) Math.max(
                    MIN_CHUNK_SIZE,
                    Math.min(MAX_CHUNK_SIZE, calculatedByChunkCount)
            );

            // 4. 边界检查（确保分片数量在合理范围）
            int actualChunks = (int) Math.ceil((double) fileSize / chunkSize);

            if (actualChunks < MIN_CHUNKS) {
                // 分片太少，减小分片大小
                return (int) Math.max(MIN_CHUNK_SIZE, fileSize / MIN_CHUNKS);
            } else if (actualChunks > MAX_CHUNKS) {
                // 分片太多，增大分片大小
                return (int) Math.min(MAX_CHUNK_SIZE, fileSize / MAX_CHUNKS);
            }

            return chunkSize;
        }
    }


    public void multipartMerge(List<String> partFileNames) throws Exception{
        final String mergeFileName = "约会大作战.mp4";
        // 构建合并集合
        List<ComposeSource> composeSources = partFileNames.stream().map(partName -> ComposeSource.builder()
                        .bucket(minioProperties.getDefaultBucket())
                        .object(partName)
                        .build())
                .toList();
        // 下面采用了直接合并；也可以分小批合并，再合并中间文件
        minioClient.composeObject(ComposeObjectArgs.builder()
                .bucket(minioProperties.getDefaultBucket())
                .object(mergeFileName)
                .sources(composeSources)
                .build());
        log.info("合并完成");
        // 清理分片文件
        List<DeleteObject> deleteObjects = partFileNames.stream().map(DeleteObject::new).toList();
        minioClient.removeObjects(RemoveObjectsArgs.builder()
                .bucket(minioProperties.getDefaultBucket())
                .objects(deleteObjects)
                .build());
        // 合并后的文件URL
        String presignedObjectUrl = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(minioProperties.getDefaultBucket())
                .object(mergeFileName)
                .method(Method.GET)
                .expiry(minioProperties.getPresignedUrlExpiry(), TimeUnit.MINUTES)
                .build());
        log.info("获取合并后的文件URL: {}", presignedObjectUrl);
    }




}
