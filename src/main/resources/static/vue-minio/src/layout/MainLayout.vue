<template>
  <div class="main-layout">
    <el-container style="height: 100vh">
      <!-- 侧边栏 -->
      <el-aside width="200px" class="sidebar">
        <div class="logo">
          <h3>图书管理系统</h3>
        </div>
        <el-menu
          :default-active="$route.path"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          unique-opened
        >
          <el-menu-item index="/dashboard">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          
          <el-sub-menu index="user">
            <template #title>
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </template>
            <el-menu-item index="/users">用户列表</el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="book">
            <template #title>
              <el-icon><Reading /></el-icon>
              <span>图书管理</span>
            </template>
            <el-menu-item index="/books">图书列表</el-menu-item>
            <el-menu-item index="/categories">图书分类</el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="borrow">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>借阅管理</span>
            </template>
            <el-menu-item index="/borrow-records">借阅记录</el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="file">
            <template #title>
              <el-icon><Folder /></el-icon>
              <span>文件管理</span>
            </template>
            <el-menu-item index="/contracts">合同文件</el-menu-item>
            <el-menu-item index="/upload">文件上传</el-menu-item>
            <el-menu-item index="/multipart-upload">分片上传</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-container>
        <!-- 头部 -->
        <el-header class="header">
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="32" :src="userAvatar" />
                <span class="username">{{ userInfo.name }}</span>
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 主内容 -->
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  House,
  User,
  Reading,
  Document,
  Folder,
  ArrowDown
} from '@element-plus/icons-vue'

export default {
  name: 'MainLayout',
  components: {
    House,
    User,
    Reading,
    Document,
    Folder,
    ArrowDown
  },
  setup() {
    const router = useRouter()
    
    const userInfo = ref({
      name: '管理员',
      avatar: ''
    })

    const userAvatar = computed(() => {
      return userInfo.value.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
    })

    const handleCommand = (command) => {
      switch (command) {
        case 'profile':
          router.push('/profile')
          break
        case 'logout':
          handleLogout()
          break
      }
    }

    const handleLogout = () => {
      ElMessageBox.confirm('确认退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem('token')
        router.push('/login')
      }).catch(() => {})
    }

    return {
      userInfo,
      userAvatar,
      handleCommand
    }
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #434a50;
}

.logo h3 {
  color: #fff;
  margin: 0;
  font-size: 18px;
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username {
  margin-left: 8px;
  margin-right: 4px;
}

.main-content {
  background-color: #f5f5f5;
  padding: 20px;
}
</style>