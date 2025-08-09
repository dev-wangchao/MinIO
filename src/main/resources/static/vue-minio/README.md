# 图书管理系统 (Vue + MinIO 实战案例)

这是一个基于 Vue 3 + Element Plus 的图书管理系统前端项目，专为 MinIO 文件存储实战案例而设计。

## 功能特性

### 📚 图书管理
- 图书信息管理（增删改查）
- 图书分类管理
- 图书封面上传
- 图书库存管理

### 👥 用户管理
- 用户注册与登录
- 用户权限管理
- 用户头像上传
- 用户信息编辑

### 📖 借阅管理
- 图书借阅记录
- 借阅状态跟踪
- 逾期提醒
- 归还处理

### 📁 文件管理
- 用户头像上传（MinIO集成）
- 合同文件上传（MinIO集成）
- 批量文件上传
- 文件下载与删除

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Element Plus** - Vue 3 组件库
- **Vue Router** - 官方路由管理器
- **Axios** - HTTP 客户端
- **MinIO** - 对象存储服务
- **Vite** - 构建工具

## 快速开始

### 安装依赖
```bash
npm install
```

### 开发环境运行
```bash
npm run dev
```

### 构建生产版本
```bash
npm run build
```

## MinIO 集成说明

本项目预留了 MinIO 文件上传功能接口，后端开发者需要实现以下 API：

### 文件上传接口
- `POST /api/files/upload/avatar` - 用户头像上传
- `POST /api/files/upload/contract` - 合同文件上传
- `POST /api/files/upload/batch` - 批量文件上传

### 文件管理接口
- `GET /api/files/history` - 获取上传历史
- `GET /api/files/download/:id` - 下载文件
- `DELETE /api/files/:id` - 删除文件

## 默认账号

系统提供测试账号：
- 用户名：admin
- 密码：123456

## 项目结构

```
src/
├── api/                    # API 接口定义
├── assets/                 # 静态资源
├── components/             # 公共组件
├── layout/                 # 布局组件
├── mock/                   # 模拟数据
├── router/                 # 路由配置
├── utils/                  # 工具函数
├── views/                  # 页面组件
└── main.js                # 应用入口
```
