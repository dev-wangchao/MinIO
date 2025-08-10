package com.code.minio.controller;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.code.minio.config.MinioProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/minio")
public class MinioChunkUploadController {

    private static final Logger log = LoggerFactory.getLogger(MinioChunkUploadController.class);
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private AmazonS3 amazonS3;
    @Autowired
    private MinioProperties minioProperties;

    /**
     * 初始化分片上传
     */
    @PostMapping("/init")
    public String initMultipartUpload(@RequestParam String fileName,
                                      @RequestParam String contentType) {
        String newFileName = String.format("%s/%s",
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), fileName);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        InitiateMultipartUploadRequest uploadRequest = new InitiateMultipartUploadRequest(
                minioProperties.getDefaultBucket(), newFileName, metadata
        );
        InitiateMultipartUploadResult uploadResult = amazonS3.initiateMultipartUpload(uploadRequest);
        String uploadId = uploadResult.getUploadId();
        log.info("初始化分片上传成功，uploadId: {}", uploadId);
        String redisKey = String.format("minio:multipart:upload:%s", uploadId);
        Map<String, Object> partInfo = Map.of("bucket", minioProperties.getDefaultBucket(),
                "originFileName", fileName, "newFileName", newFileName, "uploadId", uploadId);
        stringRedisTemplate.opsForHash().putAll(redisKey, partInfo);
        stringRedisTemplate.expire(redisKey, Duration.ofMinutes(10));
        return uploadId;
    }



    /**
     * 分片上传文件
     */
    @PostMapping("/upload")
    public void uploadMultipartFile(@RequestParam MultipartFile partFile,
                                      @RequestParam String uploadId,
                                      @RequestParam Integer partNumber) throws Exception {
        Assert.isTrue(1 <= partNumber && partNumber < 10000, "分片号必须在1-10000之间");
        String redisKey = String.format("minio:multipart:upload:%s", uploadId);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(redisKey);
        UploadPartRequest uploadPartRequest = new UploadPartRequest()
                    .withInputStream(partFile.getInputStream())
                    .withPartSize(partFile.getSize())
                    .withUploadId(uploadId)
                    .withPartNumber(partNumber)
                    .withBucketName((String) entries.get("bucket"))
                    // 确保所有分片上传到同一个对象
                    .withKey((String) entries.get("newFileName"));
        UploadPartResult result = amazonS3.uploadPart(uploadPartRequest);
        log.info("分片上传成功: uploadId={}, partNumber={}, etag={}",
                uploadId, partNumber, result.getPartETag().getETag());
        String redisPartKey = String.format("minio:multipart:%s:%s", uploadId, partNumber);
        Map<String, Object> partInfo = Map.of("partNumber", partNumber.toString(), "etag", result.getPartETag().getETag());
        stringRedisTemplate.opsForHash().putAll(redisPartKey, partInfo);
    }


    /**
     * 合并分片
     */
    @PostMapping("/marge")
    public void margeMultipartUpload(@RequestParam String uploadId) {
        String redisKey = String.format("minio:multipart:upload:%s", uploadId);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(redisKey);
        // 获取所有分片信息（排除上传信息本身）
        String partKeys = String.format("minio:multipart:%s:*", uploadId);
        Set<String> keys = stringRedisTemplate.keys(partKeys);
        Assert.isTrue(keys != null && !keys.isEmpty(), "请先上传分片");
        
        // 过滤掉上传信息本身，只保留分片信息
        Set<String> partKeysOnly = keys.stream()
                .filter(key -> !key.equals(String.format("minio:multipart:upload:%s", uploadId)))
                .collect(java.util.stream.Collectors.toSet());
        
        Assert.isTrue(!partKeysOnly.isEmpty(), "没有找到有效的分片信息");
        
        // 构建分片ETag
        List<Map<Object, Object>> partInfoMap = partKeysOnly.stream()
                .map(key -> stringRedisTemplate.opsForHash().entries(key))
                .sorted(Comparator.comparingInt(p -> Integer.parseInt(p.get("partNumber").toString())))
                .toList();
        List<PartETag> partETags = partInfoMap.stream().map(partInfo ->
                new PartETag(
                        Integer.parseInt(partInfo.get("partNumber").toString()) , partInfo.get("etag").toString()
                )
        ).toList();
        CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest()
                .withUploadId(uploadId)
                .withBucketName(entries.get("bucket").toString())
                .withKey(entries.get("newFileName").toString())
                .withPartETags(partETags);
        CompleteMultipartUploadResult result = amazonS3.completeMultipartUpload(completeMultipartUploadRequest);
        // 清理Redis数据
        stringRedisTemplate.delete(redisKey);
        log.info("合并分片成功: uploadId={}, objectKey={}, etag={}", uploadId, result.getKey(), result.getETag());
    }



}
