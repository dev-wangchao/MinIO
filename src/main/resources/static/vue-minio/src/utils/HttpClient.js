// utils/http.js
import axios from 'axios';

// 创建 axios 实例
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // 从环境变量获取基础URL
  timeout: 15000, // 请求超时时间
  headers: { 'Content-Type': 'application/json' }
});

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 这里可以添加全局请求逻辑，例如添加 token
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 这里处理全局响应逻辑
    // 如果后端有统一响应格式，可以在这里处理
    return response.data;
  },
  error => {
    // 这里处理全局错误
    if (error.response) {
      switch (error.response.status) {
        case 401:
          console.error('未授权，请重新登录');
          break;
        case 403:
          console.error('拒绝访问');
          break;
        case 404:
          console.error('资源不存在');
          break;
        case 500:
          console.error('服务器错误');
          break;
        default:
          console.error('请求错误', error.message);
      }
    } else {
      console.error('网络错误，请检查网络连接');
    }
    return Promise.reject(error);
  }
);

// 封装 GET 请求
export function get(url, params = {}, config = {}) {
  return service({
    method: 'get',
    url,
    params,
    ...config
  });
}

// 封装 POST 请求
export function post(url, data = {}, config = {}) {
  return service({
    method: 'post',
    url,
    data,
    ...config
  });
}

// 按需导出其他方法（可选）
export default {
  get,
  post
};