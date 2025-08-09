<template>
  <div class="user-manage">
    <el-card class="mb-4">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAdd">新增用户</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="mb-4">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="用户名/姓名/邮箱" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="全部" clearable>
            <el-option label="管理员" value="admin" />
            <el-option label="图书管理员" value="librarian" />
            <el-option label="读者" value="reader" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 用户列表 -->
      <el-table :data="userList" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="role" label="角色">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">
              {{ getRoleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
              {{ row.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            <el-button type="warning" size="small" @click="handleUploadAvatar(row)">上传头像</el-button>
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

    <!-- 用户表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增用户' : '编辑用户'"
      width="500px"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="dialogType === 'edit'" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="userForm.name" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="dialogType === 'add'">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色">
            <el-option label="管理员" value="admin" />
            <el-option label="图书管理员" value="librarian" />
            <el-option label="读者" value="reader" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio value="active">正常</el-radio>
            <el-radio value="inactive">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 头像上传对话框 -->
    <el-dialog
      v-model="avatarDialogVisible"
      title="上传头像"
      width="400px"
    >
      <el-upload
        class="avatar-uploader"
        :show-file-list="false"
        :before-upload="beforeAvatarUpload"
        :http-request="handleAvatarUpload"
      >
        <img v-if="avatarPreview" :src="avatarPreview" class="avatar" />
        <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
      </el-upload>
      <template #footer>
        <el-button @click="avatarDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAvatarUpload" :loading="uploadLoading">
          确认上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { userApi, fileApi } from '@/mock/mockApi'

export default {
  name: 'UserManage',
  components: { Plus },
  setup() {
    const loading = ref(false)
    const userList = ref([])
    const dialogVisible = ref(false)
    const dialogType = ref('add')
    const avatarDialogVisible = ref(false)
    const uploadLoading = ref(false)
    const currentUser = ref(null)
    const avatarPreview = ref('')
    const selectedAvatar = ref(null)

    const searchForm = reactive({
      keyword: '',
      role: ''
    })

    const pagination = reactive({
      page: 1,
      pageSize: 10,
      total: 0
    })

    const userForm = reactive({
      username: '',
      name: '',
      email: '',
      phone: '',
      password: '',
      role: 'reader',
      status: 'active'
    })

    const userFormRef = ref()

    const userRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
      ],
      name: [
        { required: true, message: '请输入姓名', trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
      ],
      phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
      ],
      role: [
        { required: true, message: '请选择角色', trigger: 'change' }
      ]
    }

    const getRoleLabel = (role) => {
      const roleMap = {
        admin: '管理员',
        librarian: '图书管理员',
        reader: '读者'
      }
      return roleMap[role] || role
    }

    const getRoleType = (role) => {
      const typeMap = {
        admin: 'danger',
        librarian: 'warning',
        reader: 'info'
      }
      return typeMap[role] || ''
    }

    const loadUserList = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.page,
          pageSize: pagination.pageSize,
          ...searchForm
        }
        const res = await userApi.getUserList(params)
        if (res.code === 200) {
          userList.value = res.data.list
          pagination.total = res.data.total
        }
      } catch (error) {
        ElMessage.error('获取用户列表失败')
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      pagination.page = 1
      loadUserList()
    }

    const handleReset = () => {
      searchForm.keyword = ''
      searchForm.role = ''
      handleSearch()
    }

    const handleAdd = () => {
      dialogType.value = 'add'
      Object.assign(userForm, {
        username: '',
        name: '',
        email: '',
        phone: '',
        password: '',
        role: 'reader',
        status: 'active'
      })
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogType.value = 'edit'
      Object.assign(userForm, {
        username: row.username,
        name: row.name,
        email: row.email,
        phone: row.phone,
        role: row.role,
        status: row.status
      })
      dialogVisible.value = true
    }

    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm('确认删除该用户吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const res = await userApi.deleteUser(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          loadUserList()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    const handleSubmit = async () => {
      userFormRef.value.validate(async (valid) => {
        if (!valid) return
        
        try {
          let res
          if (dialogType.value === 'add') {
            res = await userApi.addUser(userForm)
          } else {
            const user = users.find(u => u.username === userForm.username)
            if (user) {
              res = await userApi.updateUser(user.id, userForm)
            }
          }
          
          if (res.code === 200) {
            ElMessage.success(dialogType.value === 'add' ? '添加成功' : '更新成功')
            dialogVisible.value = false
            loadUserList()
          }
        } catch (error) {
          ElMessage.error(dialogType.value === 'add' ? '添加失败' : '更新失败')
        }
      })
    }

    const handleUploadAvatar = (row) => {
      currentUser.value = row
      avatarDialogVisible.value = true
      avatarPreview.value = row.avatar || ''
    }

    const beforeAvatarUpload = (file) => {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPG) {
        ElMessage.error('头像只能是 JPG/PNG 格式!')
        return false
      }
      if (!isLt2M) {
        ElMessage.error('头像大小不能超过 2MB!')
        return false
      }
      return true
    }

    const handleAvatarUpload = async ({ file }) => {
      selectedAvatar.value = file
      const reader = new FileReader()
      reader.onload = (e) => {
        avatarPreview.value = e.target.result
      }
      reader.readAsDataURL(file)
    }

    const confirmAvatarUpload = async () => {
      if (!selectedAvatar.value) {
        ElMessage.warning('请选择头像')
        return
      }

      uploadLoading.value = true
      try {
        const res = await fileApi.uploadAvatar(selectedAvatar.value)
        if (res.code === 200) {
          const user = users.find(u => u.id === currentUser.value.id)
          if (user) {
            user.avatar = res.data.url
            ElMessage.success('头像上传成功')
            avatarDialogVisible.value = false
            loadUserList()
          }
        }
      } catch (error) {
        ElMessage.error('头像上传失败')
      } finally {
        uploadLoading.value = false
      }
    }

    const handleSizeChange = (val) => {
      pagination.pageSize = val
      loadUserList()
    }

    const handleCurrentChange = (val) => {
      pagination.page = val
      loadUserList()
    }

    onMounted(() => {
      loadUserList()
    })

    return {
      loading,
      userList,
      searchForm,
      pagination,
      userForm,
      userFormRef,
      userRules,
      dialogVisible,
      dialogType,
      avatarDialogVisible,
      uploadLoading,
      avatarPreview,
      getRoleLabel,
      getRoleType,
      handleSearch,
      handleReset,
      handleAdd,
      handleEdit,
      handleDelete,
      handleSubmit,
      handleUploadAvatar,
      beforeAvatarUpload,
      handleAvatarUpload,
      confirmAvatarUpload,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.user-manage {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.avatar-uploader {
  text-align: center;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader-icon:hover {
  border-color: var(--el-color-primary);
}

.avatar {
  width: 178px;
  height: 178px;
  display: block;
}
</style>