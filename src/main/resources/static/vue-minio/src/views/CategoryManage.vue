<template>
  <div class="category-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>图书分类管理</span>
          <el-button type="primary" @click="handleAdd">新增分类</el-button>
        </div>
      </template>

      <el-table :data="categoryList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="bookCount" label="图书数量" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      @close="handleClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入分类描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bookApi } from '@/mock/mockApi'

export default {
  name: 'CategoryManage',
  setup() {
    const categoryList = ref([])
    const loading = ref(false)
    const dialogVisible = ref(false)
    const dialogTitle = ref('')
    const submitLoading = ref(false)
    const formRef = ref()

    const form = ref({
      id: null,
      name: '',
      description: ''
    })

    const rules = {
      name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
    }

    const loadCategories = async () => {
      loading.value = true
      try {
        const res = await bookApi.getCategories()
        if (res.code === 200) {
          categoryList.value = res.data
        }
      } catch (error) {
        ElMessage.error('获取分类列表失败')
      } finally {
        loading.value = false
      }
    }

    const handleAdd = () => {
      dialogTitle.value = '新增分类'
      form.value = {
        id: null,
        name: '',
        description: ''
      }
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑分类'
      form.value = { ...row }
      dialogVisible.value = true
    }

    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm('确定要删除该分类吗？', '提示', {
          type: 'warning'
        })
        const res = await bookApi.deleteCategory(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          loadCategories()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    const handleSubmit = async () => {
      if (!formRef.value) return

      await formRef.value.validate(async (valid) => {
        if (valid) {
          submitLoading.value = true
          try {
            const res = form.value.id
              ? await bookApi.updateCategory(form.value.id, form.value)
              : await bookApi.addCategory(form.value)

            if (res.code === 200) {
              ElMessage.success(form.value.id ? '编辑成功' : '新增成功')
              handleClose()
              loadCategories()
            }
          } catch (error) {
            ElMessage.error('操作失败')
          } finally {
            submitLoading.value = false
          }
        }
      })
    }

    const handleClose = () => {
      dialogVisible.value = false
      formRef.value?.resetFields()
    }

    onMounted(() => {
      loadCategories()
    })

    return {
      categoryList,
      loading,
      dialogVisible,
      dialogTitle,
      submitLoading,
      form,
      formRef,
      rules,
      handleAdd,
      handleEdit,
      handleDelete,
      handleSubmit,
      handleClose
    }
  }
}
</script>

<style scoped>
.category-manage {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>