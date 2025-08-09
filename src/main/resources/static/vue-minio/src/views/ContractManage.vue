<template>
  <div class="contract-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>合同管理</span>
          <el-button type="primary" @click="handleUpload">上传合同</el-button>
        </div>
      </template>

      <!-- 搜索区域 -->
      <el-form :inline="true" :model="searchForm" class="mb-4">
        <el-form-item label="合同名称">
          <el-input v-model="searchForm.name" placeholder="请输入合同名称" />
        </el-form-item>
        <el-form-item label="上传人">
          <el-input v-model="searchForm.uploader" placeholder="请输入上传人" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格区域 -->
      <el-table :data="contractList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="合同名称" />
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="fileSize" label="文件大小" width="100">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="uploader" label="上传人" width="120" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDownload(row)">下载</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="mt-4"
      />
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传合同"
      width="500px"
      @close="handleUploadClose"
    >
      <el-upload
        ref="uploadRef"
        class="upload-demo"
        drag
        :action="uploadUrl"
        :headers="uploadHeaders"
        :data="uploadData"
        :limit="1"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word、Excel 等格式，单个文件不超过 10MB
          </div>
        </template>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fileApi } from '@/mock/mockApi'

export default {
  name: 'ContractManage',
  setup() {
    const contractList = ref([])
    const loading = ref(false)
    const uploadDialogVisible = ref(false)
    const searchForm = ref({
      name: '',
      uploader: ''
    })

    const pagination = ref({
      current: 1,
      pageSize: 10,
      total: 0
    })

    // 上传配置
    const uploadUrl = '/api/files/upload/contract'
    const uploadHeaders = {
      Authorization: `Bearer ${localStorage.getItem('token')}`
    }
    const uploadData = {
      type: 'contract'
    }

    const loadContracts = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.value.current,
          pageSize: pagination.value.pageSize,
          ...searchForm.value
        }
        const res = await fileApi.getContracts(params)
        if (res.code === 200) {
          contractList.value = res.data.list
          pagination.value.total = res.data.total
        }
      } catch (error) {
        ElMessage.error('获取合同列表失败')
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      pagination.value.current = 1
      loadContracts()
    }

    const handleReset = () => {
      searchForm.value = {
        name: '',
        uploader: ''
      }
      handleSearch()
    }

    const handleSizeChange = (val) => {
      pagination.value.pageSize = val
      loadContracts()
    }

    const handleCurrentChange = (val) => {
      pagination.value.current = val
      loadContracts()
    }

    const handleUpload = () => {
      uploadDialogVisible.value = true
    }

    const handleUploadClose = () => {
      uploadDialogVisible.value = false
    }

    const beforeUpload = (file) => {
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        ElMessage.error('文件大小不能超过 10MB!')
      }
      return isLt10M
    }

    const handleUploadSuccess = (response) => {
      if (response.code === 200) {
        ElMessage.success('上传成功')
        uploadDialogVisible.value = false
        loadContracts()
      } else {
        ElMessage.error(response.message || '上传失败')
      }
    }

    const handleUploadError = () => {
      ElMessage.error('上传失败')
    }

    const handleDownload = async (row) => {
      try {
        const res = await fileApi.downloadFile(row.id)
        if (res.code === 200) {
          // 模拟下载
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
        await ElMessageBox.confirm('确定要删除该合同吗？', '提示', {
          type: 'warning'
        })
        const res = await fileApi.deleteFile(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          loadContracts()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    const formatFileSize = (bytes) => {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    onMounted(() => {
      loadContracts()
    })

    return {
      contractList,
      loading,
      uploadDialogVisible,
      searchForm,
      pagination,
      uploadUrl,
      uploadHeaders,
      uploadData,
      loadContracts,
      handleSearch,
      handleReset,
      handleSizeChange,
      handleCurrentChange,
      handleUpload,
      handleUploadClose,
      beforeUpload,
      handleUploadSuccess,
      handleUploadError,
      handleDownload,
      handleDelete,
      formatFileSize
    }
  }
}
</script>

<style scoped>
.contract-manage {
  padding: 20px;
}

.upload-demo {
  text-align: center;
}
</style>