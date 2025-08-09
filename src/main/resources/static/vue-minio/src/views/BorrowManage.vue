<template>
  <div class="borrow-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>借阅记录管理</span>
        </div>
      </template>

      <!-- 搜索区域 -->
      <el-form :inline="true" :model="searchForm" class="mb-4">
        <el-form-item label="书名">
          <el-input v-model="searchForm.bookTitle" placeholder="请输入书名" />
        </el-form-item>
        <el-form-item label="借阅人">
          <el-input v-model="searchForm.userName" placeholder="请输入借阅人" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="借阅中" value="borrowed" />
            <el-option label="已归还" value="returned" />
            <el-option label="逾期" value="overdue" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格区域 -->
      <el-table :data="borrowList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="bookTitle" label="书名" />
        <el-table-column prop="userName" label="借阅人" />
        <el-table-column prop="borrowDate" label="借阅日期" width="120" />
        <el-table-column prop="returnDate" label="应还日期" width="120" />
        <el-table-column prop="actualReturnDate" label="实际归还日期" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 'borrowed'" 
              type="success" 
              link 
              @click="handleReturn(row)"
            >
              归还
            </el-button>
            <el-button 
              v-if="row.status === 'overdue'" 
              type="warning" 
              link 
              @click="handleOverdue(row)"
            >
              处理
            </el-button>
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
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bookApi } from '@/mock/mockApi'

export default {
  name: 'BorrowManage',
  setup() {
    const borrowList = ref([])
    const loading = ref(false)
    const searchForm = ref({
      bookTitle: '',
      userName: '',
      status: ''
    })

    const pagination = ref({
      current: 1,
      pageSize: 10,
      total: 0
    })

    const loadBorrowList = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.value.current,
          pageSize: pagination.value.pageSize,
          ...searchForm.value
        }
        const res = await bookApi.getBorrowRecords(params)
        if (res.code === 200) {
          borrowList.value = res.data.list
          pagination.value.total = res.data.total
        }
      } catch (error) {
        ElMessage.error('获取借阅记录失败')
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      pagination.value.current = 1
      loadBorrowList()
    }

    const handleReset = () => {
      searchForm.value = {
        bookTitle: '',
        userName: '',
        status: ''
      }
      handleSearch()
    }

    const handleSizeChange = (val) => {
      pagination.value.pageSize = val
      loadBorrowList()
    }

    const handleCurrentChange = (val) => {
      pagination.value.current = val
      loadBorrowList()
    }

    const handleReturn = async (row) => {
      try {
        await ElMessageBox.confirm('确认归还该图书？', '提示', {
          type: 'info'
        })
        const res = await bookApi.returnBook(row.id)
        if (res.code === 200) {
          ElMessage.success('归还成功')
          loadBorrowList()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('归还失败')
        }
      }
    }

    const handleOverdue = async (row) => {
      try {
        await ElMessageBox.confirm('确认处理逾期记录？', '提示', {
          type: 'warning'
        })
        ElMessage.info('逾期处理功能待实现')
      } catch (error) {
        // 取消操作
      }
    }

    const getStatusType = (status) => {
      const map = {
        borrowed: 'primary',
        returned: 'success',
        overdue: 'danger'
      }
      return map[status] || 'info'
    }

    const getStatusText = (status) => {
      const map = {
        borrowed: '借阅中',
        returned: '已归还',
        overdue: '逾期'
      }
      return map[status] || status
    }

    onMounted(() => {
      loadBorrowList()
    })

    return {
      borrowList,
      loading,
      searchForm,
      pagination,
      loadBorrowList,
      handleSearch,
      handleReset,
      handleSizeChange,
      handleCurrentChange,
      handleReturn,
      handleOverdue,
      getStatusType,
      getStatusText
    }
  }
}
</script>

<style scoped>
.borrow-manage {
  padding: 20px;
}
</style>