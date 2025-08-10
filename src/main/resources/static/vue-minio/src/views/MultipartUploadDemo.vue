<template>
  <div class="multipart-upload-demo">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>AWS S3 分片上传演示</span>
        </div>
      </template>

      <div class="upload-section">
        <el-upload
          ref="uploadRef"
          class="upload-demo"
          :auto-upload="false"
          :on-change="handleFileChange"
          :file-list="fileList"
          :show-file-list="false"
          drag
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或 <em>点击选择文件</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持大文件上传，自动分片处理
            </div>
          </template>
        </el-upload>

        <div class="file-info" v-if="selectedFile">
          <h4>选择的文件：</h4>
          <p><strong>文件名：</strong>{{ selectedFile.name }}</p>
          <p><strong>文件大小：</strong>{{ formatFileSize(selectedFile.size) }}</p>
          <p><strong>文件类型：</strong>{{ selectedFile.type || '未知' }}</p>
          <p><strong>预计分片数：</strong>{{ estimatedChunks }}</p>
        </div>

        <div class="upload-controls" v-if="selectedFile">
          <el-button type="primary" @click="startUpload" :loading="uploading">
            开始上传
          </el-button>
          <el-button @click="clearFile">清除文件</el-button>
          <el-button 
            v-if="currentUploadId" 
            type="danger" 
            @click="abortUpload"
            :loading="aborting"
          >
            取消上传
          </el-button>
        </div>

        <div class="upload-progress" v-if="uploading || uploadProgress.total > 0">
          <h4>上传进度：</h4>
          <el-progress 
            :percentage="uploadPercentage" 
            :status="uploadStatus"
            :stroke-width="20"
          />
          <p>
            已上传：{{ uploadProgress.completed }} / {{ uploadProgress.total }} 分片
          </p>
          <div v-if="uploadSpeed > 0" class="upload-stats">
            <p>上传速度：{{ formatFileSize(uploadSpeed) }}/s</p>
            <p>剩余时间：{{ estimatedTime }}</p>
          </div>
        </div>

        <div class="upload-result" v-if="uploadResult">
          <el-alert 
            title="上传成功" 
            type="success" 
            :closable="false"
            show-icon
          >
            <p><strong>上传ID：</strong>{{ uploadResult.uploadId }}</p>
            <p><strong>文件名：</strong>{{ uploadResult.fileName }}</p>
            <p><strong>文件大小：</strong>{{ formatFileSize(uploadResult.fileSize) }}</p>
            <p><strong>分片数量：</strong>{{ uploadResult.chunkCount }}</p>
            <p><strong>状态：</strong>{{ uploadResult.message }}</p>
          </el-alert>
        </div>

        <div class="error-message" v-if="errorMessage">
          <el-alert 
            :title="errorMessage" 
            type="error" 
            :closable="true"
            @close="clearError"
            show-icon
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import MultipartUpload from '@/utils/MultipartUpload'

export default {
  name: 'MultipartUploadDemo',
  components: {
    UploadFilled
  },
  setup() {
    const selectedFile = ref(null)
    const fileList = ref([])
    const uploading = ref(false)
    const aborting = ref(false)
    const uploadProgress = ref({ completed: 0, total: 0 })
    const uploadResult = ref(null)
    const errorMessage = ref('')
    const currentUploadId = ref('')
    const uploadStartTime = ref(0)
    const uploadedBytes = ref(0)
    const uploadSpeed = ref(0)

    // 分片大小 (10MB)
    const chunkSize = 10 * 1024 * 1024

    // 计算预计分片数
    const estimatedChunks = computed(() => {
      if (!selectedFile.value) return 0
      return Math.ceil(selectedFile.value.size / chunkSize)
    })

    // 计算上传百分比
    const uploadPercentage = computed(() => {
      if (uploadProgress.value.total === 0) return 0
      return Math.round((uploadProgress.value.completed / uploadProgress.value.total) * 100)
    })

    // 上传状态
    const uploadStatus = computed(() => {
      if (errorMessage.value) return 'exception'
      if (uploadPercentage.value === 100) return 'success'
      return undefined
    })

    // 预计剩余时间
    const estimatedTime = computed(() => {
      if (uploadSpeed.value === 0 || !selectedFile.value) return '计算中...'
      
      const remainingBytes = selectedFile.value.size - uploadedBytes.value
      const remainingSeconds = remainingBytes / uploadSpeed.value
      
      if (remainingSeconds < 60) {
        return `${Math.round(remainingSeconds)}秒`
      } else if (remainingSeconds < 3600) {
        return `${Math.round(remainingSeconds / 60)}分钟`
      } else {
        return `${Math.round(remainingSeconds / 3600)}小时`
      }
    })

    // 文件选择处理
    const handleFileChange = (file) => {
      selectedFile.value = file.raw
      clearResult()
    }

    // 开始上传
    const startUpload = async () => {
      if (!selectedFile.value) {
        ElMessage.warning('请先选择文件')
        return
      }

      uploading.value = true
      uploadProgress.value = { completed: 0, total: estimatedChunks.value }
      uploadStartTime.value = Date.now()
      uploadedBytes.value = 0
      clearError()
      clearResult()

      try {
        const multipartUpload = new MultipartUpload({
          chunkSize: chunkSize,
          onProgress: (progress) => {
            uploadProgress.value.completed = progress.partNumber
            
            // 计算上传速度
            const currentTime = Date.now()
            const elapsedTime = (currentTime - uploadStartTime.value) / 1000 // 秒
            uploadedBytes.value = progress.partNumber * chunkSize
            uploadSpeed.value = uploadedBytes.value / elapsedTime
          },
          onError: (error) => {
            errorMessage.value = error.message || '上传失败'
            uploading.value = false
          },
          onSuccess: (result) => {
            uploadResult.value = result
            ElMessage.success('文件上传成功')
            uploading.value = false
          }
        })

        const result = await multipartUpload.upload(selectedFile.value)
        console.log('上传完成:', result)
        
        // 保存uploadId用于可能的取消操作
        currentUploadId.value = result.uploadId

      } catch (error) {
        console.error('上传失败:', error)
        errorMessage.value = error.message || '上传失败'
        ElMessage.error('文件上传失败')
      } finally {
        uploading.value = false
      }
    }

    // 取消上传
    const abortUpload = async () => {
      if (!currentUploadId.value) return

      aborting.value = true
      try {
        // 由于MinioChunkUploadController没有abort方法，这里只能在前端取消
        // 实际上已经上传的分片仍然会保留在Minio中
        ElMessage.info('上传已取消（注意：已上传的分片仍保留在服务器中）')
        uploading.value = false
        currentUploadId.value = ''
        uploadProgress.value = { completed: 0, total: 0 }
        
      } catch (error) {
        console.error('取消上传失败:', error)
        ElMessage.error('取消上传失败')
      } finally {
        aborting.value = false
      }
    }

    // 清除文件
    const clearFile = () => {
      selectedFile.value = null
      fileList.value = []
      clearResult()
      clearError()
      uploadProgress.value = { completed: 0, total: 0 }
    }

    // 清除结果
    const clearResult = () => {
      uploadResult.value = null
    }

    // 清除错误
    const clearError = () => {
      errorMessage.value = ''
    }

    // 格式化文件大小
    const formatFileSize = (bytes) => {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    return {
      selectedFile,
      fileList,
      uploading,
      aborting,
      uploadProgress,
      uploadResult,
      errorMessage,
      currentUploadId,
      uploadSpeed,
      estimatedChunks,
      uploadPercentage,
      uploadStatus,
      estimatedTime,
      handleFileChange,
      startUpload,
      abortUpload,
      clearFile,
      clearError,
      formatFileSize
    }
  }
}
</script>

<style scoped>
.multipart-upload-demo {
  padding: 20px;
}

.upload-section {
  max-width: 800px;
  margin: 0 auto;
}

.upload-demo {
  margin-bottom: 20px;
}

.file-info {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin: 20px 0;
}

.file-info h4 {
  margin-top: 0;
  color: #303133;
}

.file-info p {
  margin: 8px 0;
  color: #606266;
}

.upload-controls {
  margin: 20px 0;
  text-align: center;
}

.upload-controls .el-button {
  margin: 0 10px;
}

.upload-progress {
  margin: 20px 0;
}

.upload-progress h4 {
  color: #303133;
  margin-bottom: 15px;
}

.upload-stats {
  margin-top: 10px;
  font-size: 14px;
  color: #909399;
}

.upload-result {
  margin: 20px 0;
}

.error-message {
  margin: 20px 0;
}
</style>