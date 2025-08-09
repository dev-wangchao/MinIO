// 图书管理系统模拟数据

// 用户数据
export const users = [
  {
    id: 1,
    username: 'admin',
    password: 'admin123',
    name: '管理员',
    email: 'admin@library.com',
    phone: '13800138000',
    avatar: '', // 头像URL，预留用于Minio上传
    role: 'admin',
    status: 'active',
    createTime: '2024-01-01 00:00:00'
  },
  {
    id: 2,
    username: 'librarian',
    password: 'lib123',
    name: '图书管理员',
    email: 'librarian@library.com',
    phone: '13800138001',
    avatar: '',
    role: 'librarian',
    status: 'active',
    createTime: '2024-01-02 00:00:00'
  },
  {
    id: 3,
    username: 'reader1',
    password: 'reader123',
    name: '张三',
    email: 'reader1@library.com',
    phone: '13800138002',
    avatar: '',
    role: 'reader',
    status: 'active',
    createTime: '2024-01-03 00:00:00'
  }
];

// 图书分类数据
export const categories = [
  { id: 1, name: '文学', description: '文学作品分类' },
  { id: 2, name: '科技', description: '科学技术类图书' },
  { id: 3, name: '历史', description: '历史相关书籍' },
  { id: 4, name: '教育', description: '教育学习类图书' },
  { id: 5, name: '艺术', description: '艺术设计类图书' }
];

// 图书数据
export const books = [
  {
    id: 1,
    isbn: '978-7-111-12345-6',
    title: 'Java编程思想',
    author: 'Bruce Eckel',
    publisher: '机械工业出版社',
    categoryId: 2,
    price: 108.00,
    stock: 10,
    total: 15,
    location: 'A1-01',
    status: 'available',
    cover: '', // 封面图片URL，预留用于Minio上传
    description: 'Java经典入门书籍',
    publishDate: '2020-01-01',
    createTime: '2024-01-01 10:00:00'
  },
  {
    id: 2,
    isbn: '978-7-302-54321-0',
    title: '三体',
    author: '刘慈欣',
    publisher: '重庆出版社',
    categoryId: 1,
    price: 68.00,
    stock: 5,
    total: 8,
    location: 'B2-03',
    status: 'available',
    cover: '',
    description: '中国科幻小说代表作',
    publishDate: '2008-01-01',
    createTime: '2024-01-02 10:00:00'
  },
  {
    id: 3,
    isbn: '978-7-508-61234-5',
    title: '明朝那些事儿',
    author: '当年明月',
    publisher: '中国友谊出版公司',
    categoryId: 3,
    price: 45.00,
    stock: 8,
    total: 12,
    location: 'C3-05',
    status: 'available',
    cover: '',
    description: '通俗易懂的明史读物',
    publishDate: '2009-04-01',
    createTime: '2024-01-03 10:00:00'
  }
];

// 借阅记录数据
export const borrowRecords = [
  {
    id: 1,
    userId: 3,
    bookId: 1,
    borrowDate: '2024-01-15',
    returnDate: '2024-02-15',
    actualReturnDate: null,
    status: 'borrowing',
    fine: 0
  },
  {
    id: 2,
    userId: 3,
    bookId: 2,
    borrowDate: '2024-01-10',
    returnDate: '2024-02-10',
    actualReturnDate: '2024-02-08',
    status: 'returned',
    fine: 0
  }
];

// 合同数据（预留用于Minio文件上传功能）
export const contracts = [
  {
    id: 1,
    userId: 1,
    contractNumber: 'HT2024001',
    title: '图书采购合同',
    fileUrl: '', // 合同文件URL，预留用于Minio上传
    fileName: '采购合同.pdf',
    fileSize: 1024000,
    uploadTime: '2024-01-05 14:00:00',
    status: 'active'
  }
];

// 模拟API延迟
export const mockDelay = (ms = 500) => {
  return new Promise(resolve => setTimeout(resolve, ms));
};

// 工具函数：生成ID
export const generateId = () => {
  return Date.now() + Math.floor(Math.random() * 1000);
};

// 工具函数：格式化日期
export const formatDate = (date) => {
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};