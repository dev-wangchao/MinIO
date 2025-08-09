// 模拟API服务
import { users, books, categories, borrowRecords, contracts, mockDelay, generateId, formatDate } from './mockData.js';

// 用户管理相关API
export const userApi = {
  // 登录
  login: async (username, password) => {
    await mockDelay(800);
    const user = users.find(u => u.username === username && u.password === password);
    if (user) {
      const { password: _, ...userInfo } = user;
      return {
        code: 200,
        message: '登录成功',
        data: {
          user: userInfo,
          token: `token_${user.id}_${Date.now()}`
        }
      };
    }
    return { code: 401, message: '用户名或密码错误' };
  },

  // 获取用户列表
  getUserList: async (params = {}) => {
    await mockDelay(600);
    const { page = 1, pageSize = 10, keyword = '', role = '' } = params;
    
    let filteredUsers = users.map(user => {
      const { password: _, ...userInfo } = user;
      return userInfo;
    });

    if (keyword) {
      filteredUsers = filteredUsers.filter(user => 
        user.username.includes(keyword) || 
        user.name.includes(keyword) || 
        user.email.includes(keyword)
      );
    }

    if (role) {
      filteredUsers = filteredUsers.filter(user => user.role === role);
    }

    const startIndex = (page - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    
    return {
      code: 200,
      message: '获取成功',
      data: {
        list: filteredUsers.slice(startIndex, endIndex),
        total: filteredUsers.length,
        page,
        pageSize
      }
    };
  },

  // 添加用户
  addUser: async (userData) => {
    await mockDelay(500);
    const newUser = {
      id: generateId(),
      ...userData,
      createTime: formatDate(new Date()),
      status: 'active'
    };
    users.push(newUser);
    const { password: _, ...userInfo } = newUser;
    return { code: 200, message: '添加成功', data: userInfo };
  },

  // 更新用户
  updateUser: async (id, userData) => {
    await mockDelay(500);
    const index = users.findIndex(u => u.id === id);
    if (index !== -1) {
      users[index] = { ...users[index], ...userData };
      const { password: _, ...userInfo } = users[index];
      return { code: 200, message: '更新成功', data: userInfo };
    }
    return { code: 404, message: '用户不存在' };
  },

  // 删除用户
  deleteUser: async (id) => {
    await mockDelay(500);
    const index = users.findIndex(u => u.id === id);
    if (index !== -1) {
      users.splice(index, 1);
      return { code: 200, message: '删除成功' };
    }
    return { code: 404, message: '用户不存在' };
  }
};

// 图书管理相关API
export const bookApi = {
  // 获取图书分类
  getCategories: async () => {
    await mockDelay(300);
    return { code: 200, message: '获取成功', data: categories };
  },

  // 获取图书列表
  getBookList: async (params = {}) => {
    await mockDelay(600);
    const { page = 1, pageSize = 10, keyword = '', categoryId = '', status = '' } = params;
    
    let filteredBooks = [...books];

    if (keyword) {
      filteredBooks = filteredBooks.filter(book => 
        book.title.includes(keyword) || 
        book.author.includes(keyword) || 
        book.isbn.includes(keyword)
      );
    }

    if (categoryId) {
      filteredBooks = filteredBooks.filter(book => book.categoryId === parseInt(categoryId));
    }

    if (status) {
      filteredBooks = filteredBooks.filter(book => book.status === status);
    }

    const startIndex = (page - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    
    return {
      code: 200,
      message: '获取成功',
      data: {
        list: filteredBooks.slice(startIndex, endIndex),
        total: filteredBooks.length,
        page,
        pageSize
      }
    };
  },

  // 添加图书
  addBook: async (bookData) => {
    await mockDelay(500);
    const newBook = {
      id: generateId(),
      ...bookData,
      createTime: formatDate(new Date()),
      stock: bookData.total || 0
    };
    books.push(newBook);
    return { code: 200, message: '添加成功', data: newBook };
  },

  // 更新图书
  updateBook: async (id, bookData) => {
    await mockDelay(500);
    const index = books.findIndex(b => b.id === id);
    if (index !== -1) {
      books[index] = { ...books[index], ...bookData };
      return { code: 200, message: '更新成功', data: books[index] };
    }
    return { code: 404, message: '图书不存在' };
  },

  // 删除图书
  deleteBook: async (id) => {
    await mockDelay(500);
    const index = books.findIndex(b => b.id === id);
    if (index !== -1) {
      books.splice(index, 1);
      return { code: 200, message: '删除成功' };
    }
    return { code: 404, message: '图书不存在' };
  },

  // 借书
  borrowBook: async (userId, bookId) => {
    await mockDelay(800);
    const book = books.find(b => b.id === bookId);
    const user = users.find(u => u.id === userId);
    
    if (!book) return { code: 404, message: '图书不存在' };
    if (!user) return { code: 404, message: '用户不存在' };
    if (book.stock <= 0) return { code: 400, message: '图书库存不足' };
    
    const existingRecord = borrowRecords.find(r => 
      r.userId === userId && r.bookId === bookId && r.status === 'borrowing'
    );
    
    if (existingRecord) {
      return { code: 400, message: '您已借阅此书，请先归还' };
    }

    const borrowDate = new Date();
    const returnDate = new Date(borrowDate.getTime() + 30 * 24 * 60 * 60 * 1000); // 30天后
    
    const newRecord = {
      id: generateId(),
      userId,
      bookId,
      borrowDate: formatDate(borrowDate),
      returnDate: formatDate(returnDate),
      actualReturnDate: null,
      status: 'borrowing',
      fine: 0
    };
    
    borrowRecords.push(newRecord);
    book.stock--;
    
    return { code: 200, message: '借阅成功', data: newRecord };
  },

  // 还书
  returnBook: async (recordId) => {
    await mockDelay(800);
    const record = borrowRecords.find(r => r.id === recordId);
    if (!record) return { code: 404, message: '借阅记录不存在' };
    
    const book = books.find(b => b.id === record.bookId);
    if (!book) return { code: 404, message: '图书不存在' };
    
    record.actualReturnDate = formatDate(new Date());
    record.status = 'returned';
    book.stock++;
    
    // 计算逾期罚款
    const returnDate = new Date(record.returnDate);
    const actualReturnDate = new Date(record.actualReturnDate);
    const diffDays = Math.ceil((actualReturnDate - returnDate) / (1000 * 60 * 60 * 24));
    
    if (diffDays > 0) {
      record.fine = diffDays * 1; // 每天1元
    }
    
    return { code: 200, message: '归还成功', data: record };
  },

  // 获取借阅记录
  getBorrowRecords: async (params = {}) => {
    await mockDelay(600);
    const { page = 1, pageSize = 10, userId = '', status = '' } = params;
    
    let filteredRecords = [...borrowRecords];

    if (userId) {
      filteredRecords = filteredRecords.filter(r => r.userId === parseInt(userId));
    }

    if (status) {
      filteredRecords = filteredRecords.filter(r => r.status === status);
    }

    const startIndex = (page - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    
    return {
      code: 200,
      message: '获取成功',
      data: {
        list: filteredRecords.slice(startIndex, endIndex),
        total: filteredRecords.length,
        page,
        pageSize
      }
    };
  }
};

// 文件上传相关API（预留用于Minio实战）
export const fileApi = {
  // 上传头像
  uploadAvatar: async (file) => {
    await mockDelay(1000);
    // 模拟文件上传，实际使用时替换为真实上传逻辑
    const fileUrl = `https://minio.example.com/avatars/${Date.now()}_${file.name}`;
    return { code: 200, message: '上传成功', data: { url: fileUrl } };
  },

  // 上传合同
  uploadContract: async (file, contractInfo) => {
    await mockDelay(1500);
    // 模拟文件上传，实际使用时替换为真实上传逻辑
    const fileUrl = `https://minio.example.com/contracts/${Date.now()}_${file.name}`;
    const newContract = {
      id: generateId(),
      ...contractInfo,
      fileUrl,
      fileName: file.name,
      fileSize: file.size,
      uploadTime: formatDate(new Date()),
      status: 'active'
    };
    contracts.push(newContract);
    return { code: 200, message: '上传成功', data: newContract };
  },

  // 获取合同列表
  getContracts: async (params = {}) => {
    await mockDelay(600);
    const { page = 1, pageSize = 10, userId = '' } = params;
    
    let filteredContracts = [...contracts];

    if (userId) {
      filteredContracts = filteredContracts.filter(c => c.userId === parseInt(userId));
    }

    const startIndex = (page - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    
    return {
      code: 200,
      message: '获取成功',
      data: {
        list: filteredContracts.slice(startIndex, endIndex),
        total: filteredContracts.length,
        page,
        pageSize
      }
    };
  }
};

// 统一导出
export default {
  userApi,
  bookApi,
  fileApi
};