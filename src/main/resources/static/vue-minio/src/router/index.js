import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    redirect: '/dashboard',
    component: MainLayout,
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: '/users',
        name: 'UserManage',
        component: () => import('@/views/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: '/books',
        name: 'BookManage',
        component: () => import('@/views/BookManage.vue'),
        meta: { title: '图书管理' }
      },
      {
        path: '/categories',
        name: 'CategoryManage',
        component: () => import('@/views/CategoryManage.vue'),
        meta: { title: '图书分类' }
      },
      {
        path: '/borrow-records',
        name: 'BorrowManage',
        component: () => import('@/views/BorrowManage.vue'),
        meta: { title: '借阅记录' }
      },
      {
        path: '/contracts',
        name: 'ContractManage',
        component: () => import('@/views/ContractManage.vue'),
        meta: { title: '合同管理' }
      },
      {
        path: '/upload',
        name: 'FileUpload',
        component: () => import('@/views/FileUpload.vue'),
        meta: { title: '文件上传' }
      },
      {
        path: '/multipart-upload',
        name: 'MultipartUpload',
        component: () => import('@/views/MultipartUploadDemo.vue'),
        meta: { title: '分片上传演示' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router