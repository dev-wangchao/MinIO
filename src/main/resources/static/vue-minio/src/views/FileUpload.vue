<template>
  <div class="file-upload">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>用户头像上传</span>
            </div>
          </template>
          
          <el-upload
            class="avatar-uploader"
            :action="avatarUploadUrl"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
            :headers="uploadHeaders"
          >
            <img v-if="avatarUrl" :src="avatarUrl" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          
          <div class="upload-tip">
            <p>支持 JPG、PNG 格式，大小不超过 2MB</p>
            <p>推荐尺寸：200x200 像素</p>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>合同文件上传</span>
            </div>
          </template>
          
          <el-upload
            ref="contractUploadRef"
            class="upload-demo"
            drag
            :action="contractUploadUrl"
            :headers="uploadHeaders"
            :data="contractUploadData"
            :before-upload="beforeContractUpload"
            :on-success="handleContractSuccess"
            :on-error="handleContractError"
            multiple
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF、Word、Excel 等格式，单个文件不超过 10MB
              </div>
            </template>
          </el-upload>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-4">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>批量文件上传</span>
            </div>
          </template>
          
          <el-upload
            ref="batchUploadRef"
            class="upload-demo"
            drag
            :action="batchUploadUrl"
            :headers="uploadHeaders"
            :before-upload="beforeBatchUpload"
            :on-success="handleBatchSuccess"
            :on-error="handleBatchError"
            :file-list="fileList"
            multiple
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持多种文件格式，每个文件大小不超过 50MB
              </div>
            </template>
          </el-upload>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-4">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>上传历史</span>
            </div>
          </template>
          
          <el-table :data="uploadHistory" style="width: 100%" v-loading="historyLoading">
            <el-table-column prop="fileName" label="文件名" />
            <el-table-column prop="fileSize" label="文件大小" width="120">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="uploadTime" label="上传时间" width="180" />
            <el-table-column prop="uploadType" label="上传类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getUploadTypeTag(row.uploadType)">
                  {{ getUploadTypeText(row.uploadType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleDownload(row)">下载</el-button>
                <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, UploadFilled } from '@element-plus/icons-vue'
import { fileApi } from '@/mock/mockApi'

export default {
  name: 'FileUpload',
  components: {
    Plus,
    UploadFilled
  },
  setup() {
    const avatarUrl = ref('')
    const fileList = ref([])
    const uploadHistory = ref([])
    const historyLoading = ref(false)

    // 上传配置
    const uploadHeaders = {
      Authorization: `Bearer ${localStorage.getItem('token')}`
    }

    const avatarUploadUrl = '/api/files/upload/avatar'
    const contractUploadUrl = '/api/files/upload/contract'
    const batchUploadUrl = '/api/files/upload/batch'
    const contractUploadData = { type: 'contract' }

    // 头像上传
    const beforeAvatarUpload = (file) => {
      const isJPGorPNG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPGorPNG) {
        ElMessage.error('头像只能是 JPG 或 PNG 格式!')
      }
      if (!isLt2M) {
        ElMessage.error('头像大小不能超过 2MB!')
      }
      return isJPGorPNG && isLt2M
    }

    const handleAvatarSuccess = (response) => {
      if (response.code === 200) {
        avatarUrl.value = response.data.url
        ElMessage.success('头像上传成功')
      } else {
        ElMessage.error(response.message || '头像上传失败')
      }
    }

    // 合同上传
    const beforeContractUpload = (file) => {
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        ElMessage.error('文件大小不能超过 10MB!')
      }
      return isLt10M
    }

    const handleContractSuccess = (response) => {
      if (response.code === 200) {
        ElMessage.success('合同上传成功')
        loadUploadHistory()
      } else {
        ElMessage.error(response.message || '合同上传失败')
      }
    }

    const handleContractError = () => {
      ElMessage.error('合同上传失败')
    }

    // 批量上传
    const beforeBatchUpload = (file) => {
      const isLt50M = file.size / 1024 / 1024 < 50
      if (!isLt50M) {
        ElMessage.error('文件大小不能超过 50MB!')
      }
      return isLt50M
    }

    const handleBatchSuccess = (response, file) => {
      if (response.code === 200) {
        ElMessage.success(`${file.name} 上传成功`)
        loadUploadHistory()
      } else {
        ElMessage.error(`${file.name} 上传失败`)
      }
    }

    const handleBatchError = (error, file) => {
      ElMessage.error(`${file.name} 上传失败`)
    }

    // 加载上传历史
    const loadUploadHistory = async () => {
      historyLoading.value = true
      try {
        const res = await fileApi.getUploadHistory()
        if (res.code === 200) {
          uploadHistory.value = res.data
        }
      } catch (error) {
        ElMessage.error('获取上传历史失败')
      } finally {
        historyLoading.value = false
      }
    }

    // 文件操作
    const handleDownload = async (row) => {
      try {
        const res = await fileApi.downloadFile(row.id)
        if (res.code === 200) {
          const link = document.createElement('a')
          link.href = res.data.url
          link.download = row.fileName
          link.click()
          ElMessage.success('下载成功')
        }
      } catch (error) {
        ElMessage.error('下载失败')
      }
    }

    const handleDelete = async (row) => {
      try {
        const res = await fileApi.deleteFile(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          loadUploadHistory()
        }
      } catch (error) {
        ElMessage.error('删除失败')
      }
    }

    const formatFileSize = (bytes) => {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    const getUploadTypeTag = (type) => {
      const map = {
        avatar: 'success',
        contract: 'primary',
        batch: 'info'
      }
      return map[type] || 'info'
    }

    const getUploadTypeText = (type) => {
      const map = {
        avatar: '头像',
        contract: '合同',
        batch: '批量文件'
      }
      return map[type] || type
    }

    onMounted(() => {
      loadUploadHistory()
    })

    return {
      avatarUrl,
      fileList,
      uploadHistory,
      historyLoading,
      uploadHeaders,
      avatarUploadUrl,
      contractUploadUrl,
      batchUploadUrl,
      contractUploadData,
      beforeAvatarUpload,
      handleAvatarSuccess,
      beforeContractUpload,
      handleContractSuccess,
      handleContractError,
      beforeBatchUpload,
      handleBatchSuccess,
      handleBatchError,
      handleDownload,
      handleDelete,
      formatFileSize,
      getUploadTypeTag,
      getUploadTypeText
    }
  }
}
</script>

<style scoped>
.file-upload {
  padding: 20px;
}

.avatar-uploader {
  text-align: center;
  padding: 20px;
}

.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
}

.avatar {
  width: 178px;
  height: 178px;
  display: block;
}

.upload-tip {
  margin-top: 10px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.upload-demo {
  text-align: center;
}
</style>