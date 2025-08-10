/**
 * 分片上传工具类 - 调用MinioChunkUploadController
 */
class MultipartUpload {
    constructor(options = {}) {
        this.baseURL = options.baseURL || '/minio'; // 直接指定后端地址
        this.chunkSize = options.chunkSize || 10 * 1024 * 1024; // 默认10MB
        this.maxRetries = options.maxRetries || 3;
        this.onProgress = options.onProgress || (() => {});
        this.onError = options.onError || (() => {});
        this.onSuccess = options.onSuccess || (() => {});
    }

    /**
     * 开始上传文件
     */
    async upload(file) {
        try {
            console.log('开始分片上传:', file.name, '文件大小:', this.formatFileSize(file.size));
            
            // 1. 初始化分片上传
            const uploadId = await this.initiateUpload(file);
            
            console.log('初始化成功, uploadId:', uploadId);
            
            // 2. 计算分片信息
            const chunks = this.createChunks(file);
            console.log('分片数量:', chunks.length, '每片大小:', this.formatFileSize(this.chunkSize));
            
            // 3. 上传所有分片
            const uploadPromises = chunks.map((chunk, index) => 
                this.uploadChunk(uploadId, index + 1, chunk)
            );
            
            // 使用 Promise.allSettled 来处理部分失败的情况
            const results = await Promise.allSettled(uploadPromises);
            
            // 检查失败的分片
            const failedChunks = results
                .map((result, index) => ({ result, index }))
                .filter(({ result }) => result.status === 'rejected');
            
            if (failedChunks.length > 0) {
                console.error('部分分片上传失败:', failedChunks);
                throw new Error(`${failedChunks.length} 个分片上传失败`);
            }
            
            // 4. 完成分片上传
            await this.completeUpload(uploadId);
            
            console.log('分片上传完成');
            
            // 构建成功结果
            const result = {
                uploadId: uploadId,
                fileName: file.name,
                fileSize: file.size,
                chunkCount: chunks.length,
                message: '文件上传成功'
            };
            
            this.onSuccess(result);
            
            return result;
            
        } catch (error) {
            console.error('分片上传失败:', error);
            this.onError(error);
            throw error;
        }
    }

    /**
     * 初始化分片上传 - 调用MinioChunkUploadController.init
     */
    async initiateUpload(file) {
        const formData = new FormData();
        formData.append('fileName', file.name);
        formData.append('contentType', file.type || 'application/octet-stream');
        
        const response = await fetch(`${this.baseURL}/init`, {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) {
            throw new Error(`初始化分片上传失败: ${response.statusText}`);
        }
        
        // MinioChunkUploadController.init直接返回uploadId字符串
        return await response.text();
    }

    /**
     * 创建分片
     */
    createChunks(file) {
        const chunks = [];
        const totalChunks = Math.ceil(file.size / this.chunkSize);
        
        for (let i = 0; i < totalChunks; i++) {
            const start = i * this.chunkSize;
            const end = Math.min(start + this.chunkSize, file.size);
            chunks.push(file.slice(start, end));
        }
        
        return chunks;
    }

    /**
     * 上传单个分片 - 调用MinioChunkUploadController.upload
     */
    async uploadChunk(uploadId, partNumber, chunk, retryCount = 0) {
        try {
            const formData = new FormData();
            formData.append('uploadId', uploadId);
            formData.append('partNumber', partNumber);
            formData.append('partFile', chunk); // 注意：参数名改为partFile，与Controller一致
            
            const response = await fetch(`${this.baseURL}/upload`, {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) {
                throw new Error(`分片 ${partNumber} 上传失败: ${response.statusText}`);
            }
            
            // 更新进度
            this.onProgress({
                partNumber,
                completed: true,
                total: Math.ceil(chunk.size / this.chunkSize)
            });
            
            return { success: true, partNumber };
            
        } catch (error) {
            if (retryCount < this.maxRetries) {
                console.warn(`分片 ${partNumber} 上传失败，进行第 ${retryCount + 1} 次重试:`, error.message);
                await this.delay(1000 * (retryCount + 1)); // 指数退避
                return this.uploadChunk(uploadId, partNumber, chunk, retryCount + 1);
            }
            throw error;
        }
    }

    /**
     * 完成分片上传 - 调用MinioChunkUploadController.marge
     */
    async completeUpload(uploadId) {
        const formData = new FormData();
        formData.append('uploadId', uploadId);
        
        const response = await fetch(`${this.baseURL}/marge`, {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) {
            throw new Error(`完成分片上传失败: ${response.statusText}`);
        }
        
        return { success: true };
    }

    /**
     * 取消分片上传 - 注意：MinioChunkUploadController没有abort方法，这里保留接口但不实现
     */
    async abortUpload(uploadId) {
        console.warn('MinioChunkUploadController暂不支持取消上传功能');
        // 由于Controller没有abort方法，这里只是记录日志
        return { success: false, message: '暂不支持取消上传' };
    }

    /**
     * 获取上传状态 - 注意：MinioChunkUploadController没有status方法，这里保留接口但不实现
     */
    async getUploadStatus(uploadId) {
        console.warn('MinioChunkUploadController暂不支持获取上传状态功能');
        // 由于Controller没有status方法，这里只是记录日志
        return { success: false, message: '暂不支持获取上传状态' };
    }

    /**
     * 延迟函数
     */
    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    /**
     * 格式化文件大小
     */
    formatFileSize(bytes) {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
}

export default MultipartUpload;