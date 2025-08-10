package com.code.minio.controller;

import com.code.minio.service.impl.AwsS3MultipartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/multipart")
@Slf4j
public class AwsS3MultipartController {

    @Autowired
    private AwsS3MultipartService multipartService;

    /**
     * 初始化分片上传
     */
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiateMultipartUpload(
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "contentType", required = false) String contentType) {
        
        try {
            Map<String, Object> result = multipartService.initiateMultipartUpload(fileName, contentType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "初始化分片上传成功");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("初始化分片上传失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "初始化分片上传失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 上传分片
     */
    @PostMapping("/upload-part")
    public ResponseEntity<Map<String, Object>> uploadPart(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("partNumber") int partNumber,
            @RequestParam("file") MultipartFile file) {
        
        try {
            // 验证分片号
            if (partNumber < 1 || partNumber > 10000) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("code", 400);
                errorResponse.put("message", "分片号必须在1-10000之间");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 验证文件大小（AWS S3要求除最后一个分片外，每个分片至少5MB）
            if (file.getSize() < 5 * 1024 * 1024) {
                log.warn("分片大小小于5MB: {} bytes", file.getSize());
            }
            
            Map<String, Object> result = multipartService.uploadPart(uploadId, partNumber, file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "分片上传成功");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("分片上传失败: uploadId={}, partNumber={}, error={}", 
                    uploadId, partNumber, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "分片上传失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 完成分片上传
     */
    @PostMapping("/complete")
    public ResponseEntity<Map<String, Object>> completeMultipartUpload(
            @RequestParam("uploadId") String uploadId) {
        
        try {
            Map<String, Object> result = multipartService.completeMultipartUpload(uploadId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "分片上传完成");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("完成分片上传失败: uploadId={}, error={}", uploadId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "完成分片上传失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 取消分片上传
     */
    @PostMapping("/abort")
    public ResponseEntity<Map<String, Object>> abortMultipartUpload(
            @RequestParam("uploadId") String uploadId) {
        
        try {
            multipartService.abortMultipartUpload(uploadId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "分片上传已取消");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("取消分片上传失败: uploadId={}, error={}", uploadId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "取消分片上传失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 列出已上传的分片
     */
    @GetMapping("/list-parts")
    public ResponseEntity<Map<String, Object>> listUploadedParts(
            @RequestParam("uploadId") String uploadId) {
        
        try {
            List<Map<String, Object>> parts = multipartService.listUploadedParts(uploadId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("parts", parts);
            result.put("totalParts", parts.size());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取分片列表成功");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取分片列表失败: uploadId={}, error={}", uploadId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取分片列表失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取上传会话状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getUploadStatus(
            @RequestParam("uploadId") String uploadId) {
        
        try {
            List<Map<String, Object>> parts = multipartService.listUploadedParts(uploadId);
            
            Map<String, Object> status = new HashMap<>();
            status.put("uploadId", uploadId);
            status.put("uploadedParts", parts.size());
            status.put("parts", parts);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取上传状态成功");
            response.put("data", status);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取上传状态失败: uploadId={}, error={}", uploadId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取上传状态失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}