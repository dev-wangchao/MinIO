<template>
  <div class="dashboard">
    <el-row :gutter="20" class="mb-4">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="40" color="#409EFF"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.userCount }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="40" color="#67C23A"><Reading /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.bookCount }}</div>
              <div class="stat-label">图书总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="40" color="#E6A23C"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.borrowCount }}</div>
              <div class="stat-label">借阅总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon size="40" color="#F56C6C"><Folder /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.fileCount }}</div>
              <div class="stat-label">文件总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb-4">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近借阅</span>
              <el-button type="text" @click="$router.push('/borrow-records')">更多</el-button>
            </div>
          </template>
          <el-table :data="recentBorrows" style="width: 100%">
            <el-table-column prop="bookTitle" label="书名" />
            <el-table-column prop="userName" label="借阅人" />
            <el-table-column prop="borrowDate" label="借阅日期" />
            <el-table-column prop="returnDate" label="应还日期" />
          </el-table>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>热门图书</span>
              <el-button type="text" @click="$router.push('/books')">更多</el-button>
            </div>
          </template>
          <el-table :data="popularBooks" style="width: 100%">
            <el-table-column prop="title" label="书名" />
            <el-table-column prop="author" label="作者" />
            <el-table-column prop="borrowCount" label="借阅次数" />
            <el-table-column prop="stock" label="库存">
              <template #default="{ row }">
                {{ row.stock }}/{{ row.total }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>系统公告</span>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="notice in notices"
              :key="notice.id"
              :timestamp="notice.createTime"
              :type="notice.type"
            >
              {{ notice.content }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { User, Reading, Document, Folder } from '@element-plus/icons-vue'
import { bookApi, userApi } from '@/mock/mockApi'

export default {
  name: 'Dashboard',
  components: {
    User,
    Reading,
    Document,
    Folder
  },
  setup() {
    const stats = ref({
      userCount: 0,
      bookCount: 0,
      borrowCount: 0,
      fileCount: 0
    })

    const recentBorrows = ref([
      {
        bookTitle: 'Java编程思想',
        userName: '张三',
        borrowDate: '2024-01-15',
        returnDate: '2024-02-15'
      },
      {
        bookTitle: '三体',
        userName: '李四',
        borrowDate: '2024-01-14',
        returnDate: '2024-02-14'
      }
    ])

    const popularBooks = ref([
      {
        title: 'Java编程思想',
        author: 'Bruce Eckel',
        borrowCount: 15,
        stock: 10,
        total: 15
      },
      {
        title: '三体',
        author: '刘慈欣',
        borrowCount: 12,
        stock: 5,
        total: 8
      }
    ])

    const notices = ref([
      {
        id: 1,
        content: '系统升级完成，新增文件上传功能，欢迎使用！',
        type: 'primary',
        createTime: '2024-01-20 10:00:00'
      },
      {
        id: 2,
        content: '图书馆新增100本新书，欢迎大家借阅！',
        type: 'success',
        createTime: '2024-01-19 09:00:00'
      },
      {
        id: 3,
        content: '借阅规则更新，请注意查看最新规定。',
        type: 'warning',
        createTime: '2024-01-18 14:00:00'
      }
    ])

    const loadStats = async () => {
      try {
        const [userRes, bookRes, borrowRes] = await Promise.all([
          userApi.getUserList({ page: 1, pageSize: 1 }),
          bookApi.getBookList({ page: 1, pageSize: 1 }),
          bookApi.getBorrowRecords({ page: 1, pageSize: 1 })
        ])

        if (userRes.code === 200) stats.value.userCount = userRes.data.total
        if (bookRes.code === 200) stats.value.bookCount = bookRes.data.total
        if (borrowRes.code === 200) stats.value.borrowCount = borrowRes.data.total
        stats.value.fileCount = 12 // 模拟文件数量
      } catch (error) {
        console.error('获取统计数据失败', error)
      }
    }

    onMounted(() => {
      loadStats()
    })

    return {
      stats,
      recentBorrows,
      popularBooks,
      notices
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stat-card {
  text-align: center;
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.stat-icon {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>