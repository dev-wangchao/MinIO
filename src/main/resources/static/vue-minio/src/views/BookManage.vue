<template>
  <div class="book-manage">
    <el-card class="mb-4">
      <template #header>
        <div class="card-header">
          <span>图书管理</span>
          <el-button type="primary" @click="handleAdd">新增图书</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="mb-4">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="书名/作者/ISBN" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部" clearable>
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="可借" value="available" />
            <el-option label="已借完" value="borrowed" />
            <el-option label="下架" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 图书列表 -->
      <el-table :data="bookList" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="isbn" label="ISBN" width="150" />
        <el-table-column prop="title" label="书名" min-width="200" />
        <el-table-column prop="author" label="作者" />
        <el-table-column prop="publisher" label="出版社" />
        <el-table-column prop="categoryName" label="分类" />
        <el-table-column prop="price" label="价格" width="80">
          <template #default="{ row }">
            ¥{{ row.price.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80">
          <template #default="{ row }">
            <el-tag :type="row.stock > 0 ? 'success' : 'danger'">
              {{ row.stock }}/{{ row.total }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            <el-button type="warning" size="small" @click="handleUploadCover(row)">上传封面</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="mt-4"
      />
    </el-card>

    <!-- 图书表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增图书' : '编辑图书'"
      width="600px"
    >
      <el-form
        ref="bookFormRef"
        :model="bookForm"
        :rules="bookRules"
        label-width="100px"
      >
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="bookForm.isbn" placeholder="请输入ISBN" />
        </el-form-item>
        <el-form-item label="书名" prop="title">
          <el-input v-model="bookForm.title" placeholder="请输入书名" />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="bookForm.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="出版社" prop="publisher">
          <el-input v-model="bookForm.publisher" placeholder="请输入出版社" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="bookForm.categoryId" placeholder="请选择分类">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="bookForm.price" :min="0" :precision="2" :step="0.1" />
        </el-form-item>
        <el-form-item label="总数量" prop="total">
          <el-input-number v-model="bookForm.total" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="bookForm.location" placeholder="如：A1-01" />
        </el-form-item>
        <el-form-item label="出版日期" prop="publishDate">
          <el-date-picker
            v-model="bookForm.publishDate"
            type="date"
            placeholder="选择出版日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="bookForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入图书描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 封面上传对话框 -->
    <el-dialog
      v-model="coverDialogVisible"
      title="上传封面"
      width="400px"
    >
      <el-upload
        class="cover-uploader"
        :show-file-list="false"
        :before-upload="beforeCoverUpload"
        :http-request="handleCoverUpload"
        drag
      >
        <img v-if="coverPreview" :src="coverPreview" class="cover-preview" />
        <div v-else>
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            拖拽图片到此处或 <em>点击上传</em>
          </div>
        </div>
      </el-upload>
      <template #footer>
        <el-button @click="coverDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCoverUpload" :loading="uploadLoading">
          确认上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { bookApi, fileApi } from '@/mock/mockApi'

export default {
  name: 'BookManage',
  components: { UploadFilled },
  setup() {
    const loading = ref(false)
    const bookList = ref([])
    const categories = ref([])
    const dialogVisible = ref(false)
    const dialogType = ref('add')
    const coverDialogVisible = ref(false)
    const uploadLoading = ref(false)
    const currentBook = ref(null)
    const coverPreview = ref('')
    const selectedCover = ref(null)

    const searchForm = reactive({
      keyword: '',
      categoryId: '',
      status: ''
    })

    const pagination = reactive({
      page: 1,
      pageSize: 10,
      total: 0
    })

    const bookForm = reactive({
      isbn: '',
      title: '',
      author: '',
      publisher: '',
      categoryId: '',
      price: 0,
      total: 1,
      location: '',
      publishDate: '',
      description: ''
    })

    const bookFormRef = ref()

    const bookRules = {
      isbn: [
        { required: true, message: '请输入ISBN', trigger: 'blur' },
        { pattern: /^[\d-]+$/, message: 'ISBN格式不正确', trigger: 'blur' }
      ],
      title: [
        { required: true, message: '请输入书名', trigger: 'blur' }
      ],
      author: [
        { required: true, message: '请输入作者', trigger: 'blur' }
      ],
      publisher: [
        { required: true, message: '请输入出版社', trigger: 'blur' }
      ],
      categoryId: [
        { required: true, message: '请选择分类', trigger: 'change' }
      ],
      price: [
        { required: true, message: '请输入价格', trigger: 'blur' }
      ],
      total: [
        { required: true, message: '请输入总数量', trigger: 'blur' }
      ],
      location: [
        { required: true, message: '请输入位置', trigger: 'blur' }
      ]
    }

    const getStatusLabel = (status) => {
      const statusMap = {
        available: '可借',
        borrowed: '已借完',
        disabled: '下架'
      }
      return statusMap[status] || status
    }

    const getStatusType = (status) => {
      const typeMap = {
        available: 'success',
        borrowed: 'warning',
        disabled: 'info'
      }
      return typeMap[status] || ''
    }

    const loadBookList = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.page,
          pageSize: pagination.pageSize,
          ...searchForm
        }
        const res = await bookApi.getBookList(params)
        if (res.code === 200) {
          bookList.value = res.data.list.map(book => ({
            ...book,
            categoryName: categories.value.find(c => c.id === book.categoryId)?.name || ''
          }))
          pagination.total = res.data.total
        }
      } catch (error) {
        ElMessage.error('获取图书列表失败')
      } finally {
        loading.value = false
      }
    }

    const loadCategories = async () => {
      try {
        const res = await bookApi.getCategories()
        if (res.code === 200) {
          categories.value = res.data
        }
      } catch (error) {
        ElMessage.error('获取分类列表失败')
      }
    }

    const handleSearch = () => {
      pagination.page = 1
      loadBookList()
    }

    const handleReset = () => {
      searchForm.keyword = ''
      searchForm.categoryId = ''
      searchForm.status = ''
      handleSearch()
    }

    const handleAdd = () => {
      dialogType.value = 'add'
      Object.assign(bookForm, {
        isbn: '',
        title: '',
        author: '',
        publisher: '',
        categoryId: '',
        price: 0,
        total: 1,
        location: '',
        publishDate: '',
        description: ''
      })
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogType.value = 'edit'
      Object.assign(bookForm, {
        isbn: row.isbn,
        title: row.title,
        author: row.author,
        publisher: row.publisher,
        categoryId: row.categoryId,
        price: row.price,
        total: row.total,
        location: row.location,
        publishDate: row.publishDate,
        description: row.description
      })
      dialogVisible.value = true
    }

    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm('确认删除该图书吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const res = await bookApi.deleteBook(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          loadBookList()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    const handleSubmit = async () => {
      bookFormRef.value.validate(async (valid) => {
        if (!valid) return
        
        try {
          let res
          if (dialogType.value === 'add') {
            res = await bookApi.addBook(bookForm)
          } else {
            res = await bookApi.updateBook(currentBook.value.id, bookForm)
          }
          
          if (res.code === 200) {
            ElMessage.success(dialogType.value === 'add' ? '添加成功' : '更新成功')
            dialogVisible.value = false
            loadBookList()
          }
        } catch (error) {
          ElMessage.error(dialogType.value === 'add' ? '添加失败' : '更新失败')
        }
      })
    }

    const handleUploadCover = (row) => {
      currentBook.value = row
      coverDialogVisible.value = true
      coverPreview.value = row.cover || ''
    }

    const beforeCoverUpload = (file) => {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt5M = file.size / 1024 / 1024 < 5

      if (!isJPG) {
        ElMessage.error('封面只能是 JPG/PNG 格式!')
        return false
      }
      if (!isLt5M) {
        ElMessage.error('封面大小不能超过 5MB!')
        return false
      }
      return true
    }

    const handleCoverUpload = async ({ file }) => {
      selectedCover.value = file
      const reader = new FileReader()
      reader.onload = (e) => {
        coverPreview.value = e.target.result
      }
      reader.readAsDataURL(file)
    }

    const confirmCoverUpload = async () => {
      if (!selectedCover.value) {
        ElMessage.warning('请选择封面图片')
        return
      }

      uploadLoading.value = true
      try {
        const res = await fileApi.uploadContract(selectedCover.value, {
          userId: 1, // 当前用户ID
          contractNumber: `BOOK_COVER_${currentBook.value.id}`
        })
        if (res.code === 200) {
          const book = books.find(b => b.id === currentBook.value.id)
          if (book) {
            book.cover = res.data.fileUrl
            ElMessage.success('封面上传成功')
            coverDialogVisible.value = false
            loadBookList()
          }
        }
      } catch (error) {
        ElMessage.error('封面上传失败')
      } finally {
        uploadLoading.value = false
      }
    }

    const handleSizeChange = (val) => {
      pagination.pageSize = val
      loadBookList()
    }

    const handleCurrentChange = (val) => {
      pagination.page = val
      loadBookList()
    }

    onMounted(() => {
      loadCategories()
      loadBookList()
    })

    return {
      loading,
      bookList,
      categories,
      searchForm,
      pagination,
      bookForm,
      bookFormRef,
      bookRules,
      dialogVisible,
      dialogType,
      coverDialogVisible,
      uploadLoading,
      coverPreview,
      getStatusLabel,
      getStatusType,
      handleSearch,
      handleReset,
      handleAdd,
      handleEdit,
      handleDelete,
      handleSubmit,
      handleUploadCover,
      beforeCoverUpload,
      handleCoverUpload,
      confirmCoverUpload,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.book-manage {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cover-uploader {
  text-align: center;
}

.cover-preview {
  width: 200px;
  height: 300px;
  object-fit: cover;
  border-radius: 8px;
}

:deep(.el-upload-dragger) {
  padding: 20px;
}
</style>