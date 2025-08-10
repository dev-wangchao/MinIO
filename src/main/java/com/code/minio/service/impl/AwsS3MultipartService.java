package com.code.minio.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.code.minio.config.MinioProperties;
import com.code.minio.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class AwsS3MultipartService {

    @Autowired
    private AmazonS3 amazonS3;
    
    @Autowired
    private MinioProperties minioProperties;
    
    @Autowired
    private RedisUtil redisUtil;

    /**
     * 初始化分片上传
     */
    public Map<String, Object> initiateMultipartUpload(String fileName, String contentType) {
        try {
            // 生成对象名称
            String objectKey = generateObjectKey(fileName);
            
            // 创建初始化分片上传请求
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(
                    minioProperties.getDefaultBucket(), objectKey);
            
            if (contentType != null) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(contentType);
                request.setObjectMetadata(metadata);
            }
            
            // 初始化分片上传
            InitiateMultipartUploadResult result = amazonS3.initiateMultipartUpload(request);
            String uploadId = result.getUploadId();
            
            // 在Redis中保存上传会话信息
            Map<String, Object> uploadSession = new HashMap<>();
            uploadSession.put("objectKey", objectKey);
            uploadSession.put("fileName", fileName);
            uploadSession.put("contentType", contentType);
            uploadSession.put("createTime", System.currentTimeMillis());
            
            redisUtil.set("upload_session:" + uploadId, uploadSession, 24 * 3600); // 24小时过期
            
            // 返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("uploadId", uploadId);
            response.put("objectKey", objectKey);
            response.put("bucketName", minioProperties.getDefaultBucket());
            
            log.info("初始化分片上传成功: uploadId={}, objectKey={}", uploadId, objectKey);
            return response;
            
        } catch (Exception e) {
            log.error("初始化分片上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("初始化分片上传失败", e);
        }
    }

    /**
     * 上传单个分片
     */
    public Map<String, Object> uploadPart(String uploadId, int partNumber, MultipartFile partFile) {
        try {
            // 从Redis获取上传会话信息
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadSession = (Map<String, Object>) redisUtil.get("upload_session:" + uploadId);
            if (uploadSession == null) {
                throw new RuntimeException("上传会话不存在或已过期");
            }
            
            String objectKey = (String) uploadSession.get("objectKey");
            
            // 创建上传分片请求
            UploadPartRequest request = new UploadPartRequest()
                    .withBucketName(minioProperties.getDefaultBucket())
                    .withKey(objectKey)
                    .withUploadId(uploadId)
                    .withPartNumber(partNumber)
                    .withPartSize(partFile.getSize())
                    .withInputStream(partFile.getInputStream());
            
            // 上传分片
            UploadPartResult result = amazonS3.uploadPart(request);
            PartETag partETag = result.getPartETag();
            
            // 在Redis中保存分片信息
            Map<String, Object> partInfo = new HashMap<>();
            partInfo.put("partNumber", partNumber);
            partInfo.put("etag", partETag.getETag());
            partInfo.put("partSize", partFile.getSize());
            partInfo.put("uploadTime", System.currentTimeMillis());
            
            String partKey = "upload_part:" + uploadId + ":" + partNumber;
            redisUtil.set(partKey, partInfo, 24 * 3600); // 24小时过期
            
            // 返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("partNumber", partNumber);
            response.put("etag", partETag.getETag());
            response.put("partSize", partFile.getSize());
            
            log.info("分片上传成功: uploadId={}, partNumber={}, etag={}", 
                    uploadId, partNumber, partETag.getETag());
            return response;
            
        } catch (Exception e) {
            log.error("分片上传失败: uploadId={}, partNumber={}, error={}", 
                    uploadId, partNumber, e.getMessage(), e);
            throw new RuntimeException("分片上传失败", e);
        }
    }

    /**
     * 完成分片上传
     */
    public Map<String, Object> completeMultipartUpload(String uploadId) {
        try {
            // 从Redis获取上传会话信息
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadSession = (Map<String, Object>) redisUtil.get("upload_session:" + uploadId);
            if (uploadSession == null) {
                throw new RuntimeException("上传会话不存在或已过期");
            }
            
            String objectKey = (String) uploadSession.get("objectKey");
            
            // 获取所有已上传的分片
            List<PartETag> partETags = getUploadedParts(uploadId);
            if (partETags.isEmpty()) {
                throw new RuntimeException("没有找到已上传的分片");
            }
            
            // 按分片号排序
            partETags.sort(Comparator.comparing(PartETag::getPartNumber));
            
            // 创建完成分片上传请求
            CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(
                    minioProperties.getDefaultBucket(), objectKey, uploadId, partETags);
            
            // 完成分片上传
            CompleteMultipartUploadResult result = amazonS3.completeMultipartUpload(request);
            
            // 清理Redis中的临时数据
            cleanupUploadSession(uploadId);
            
            // 返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("objectKey", objectKey);
            response.put("etag", result.getETag());
            response.put("location", result.getLocation());
            response.put("bucketName", result.getBucketName());
            
            log.info("分片上传完成: uploadId={}, objectKey={}, etag={}", 
                    uploadId, objectKey, result.getETag());
            return response;
            
        } catch (Exception e) {
            log.error("完成分片上传失败: uploadId={}, error={}", uploadId, e.getMessage(), e);
            throw new RuntimeException("完成分片上传失败", e);
        }
    }

    /**
     * 取消分片上传
     */
    public void abortMultipartUpload(String uploadId) {
        try {
            // 从Redis获取上传会话信息
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadSession = (Map<String, Object>) redisUtil.get("upload_session:" + uploadId);
            if (uploadSession == null) {
                log.warn("上传会话不存在或已过期: uploadId={}", uploadId);
                return;
            }
            
            String objectKey = (String) uploadSession.get("objectKey");
            
            // 取消分片上传
            AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(
                    minioProperties.getDefaultBucket(), objectKey, uploadId);
            amazonS3.abortMultipartUpload(request);
            
            // 清理Redis中的临时数据
            cleanupUploadSession(uploadId);
            
            log.info("分片上传已取消: uploadId={}, objectKey={}", uploadId, objectKey);
            
        } catch (Exception e) {
            log.error("取消分片上传失败: uploadId={}, error={}", uploadId, e.getMessage(), e);
            throw new RuntimeException("取消分片上传失败", e);
        }
    }

    /**
     * 列出已上传的分片
     */
    public List<Map<String, Object>> listUploadedParts(String uploadId) {
        try {
            // 从Redis获取上传会话信息
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadSession = (Map<String, Object>) redisUtil.get("upload_session:" + uploadId);
            if (uploadSession == null) {
                throw new RuntimeException("上传会话不存在或已过期");
            }
            
            String objectKey = (String) uploadSession.get("objectKey");
            
            // 列出分片
            ListPartsRequest request = new ListPartsRequest(
                    minioProperties.getDefaultBucket(), objectKey, uploadId);
            PartListing result = amazonS3.listParts(request);
            
            List<Map<String, Object>> parts = new ArrayList<>();
            for (PartSummary part : result.getParts()) {
                Map<String, Object> partInfo = new HashMap<>();
                partInfo.put("partNumber", part.getPartNumber());
                partInfo.put("etag", part.getETag());
                partInfo.put("size", part.getSize());
                partInfo.put("lastModified", part.getLastModified());
                parts.add(partInfo);
            }
            
            return parts;
            
        } catch (Exception e) {
            log.error("列出已上传分片失败: uploadId={}, error={}", uploadId, e.getMessage(), e);
            throw new RuntimeException("列出已上传分片失败", e);
        }
    }

    /**
     * 获取所有已上传的分片ETags
     */
    private List<PartETag> getUploadedParts(String uploadId) {
        // 这里可以通过两种方式获取：
        // 1. 从Redis获取（更快）
        // 2. 从MinIO服务器获取（更可靠）
        
        // 方式1：从Redis获取
        List<PartETag> partETags = new ArrayList<>();
        
        // 扫描Redis中的分片信息
        Set<String> partKeys = redisUtil.keys("upload_part:" + uploadId + ":*");
        for (String partKey : partKeys) {
            @SuppressWarnings("unchecked")
            Map<String, Object> partInfo = (Map<String, Object>) redisUtil.get(partKey);
            if (partInfo != null) {
                Integer partNumber = (Integer) partInfo.get("partNumber");
                String etag = (String) partInfo.get("etag");
                partETags.add(new PartETag(partNumber, etag));
            }
        }
        
        // 如果Redis中没有数据，从MinIO服务器获取
        if (partETags.isEmpty()) {
            List<Map<String, Object>> serverParts = listUploadedParts(uploadId);
            for (Map<String, Object> part : serverParts) {
                Integer partNumber = (Integer) part.get("partNumber");
                String etag = (String) part.get("etag");
                partETags.add(new PartETag(partNumber, etag));
            }
        }
        
        return partETags;
    }

    /**
     * 清理上传会话的临时数据
     */
    private void cleanupUploadSession(String uploadId) {
        // 删除会话信息
        redisUtil.del("upload_session:" + uploadId);
        
        // 删除所有分片信息
        Set<String> partKeys = redisUtil.keys("upload_part:" + uploadId + ":*");
        for (String partKey : partKeys) {
            redisUtil.del(partKey);
        }
    }

    /**
     * 生成对象键名
     */
    private String generateObjectKey(String fileName) {
        return String.format("%s/%s",
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), fileName);
    }

    // 需要在RedisUtil中添加keys方法
    // 或者可以直接注入StringRedisTemplate来实现keys功能
}