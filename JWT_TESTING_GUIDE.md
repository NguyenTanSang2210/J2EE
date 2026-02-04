# 🔐 Hướng Dẫn Kiểm Tra API Xác Thực JWT

## 📋 Các API Endpoints

### 1. **Đăng Ký Người Dùng**
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "123456",
  "phone": "0123456789"
}
```

**Phản hồi thành công (201 Created):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "testuser",
  "email": "test@example.com",
  "roles": ["USER"]
}
```

---

### 2. **Đăng Nhập**
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456"
}
```

**Phản hồi thành công (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "testuser",
  "email": "test@example.com",
  "roles": ["USER"]
}
```

**Phản hồi lỗi (401 Unauthorized):**
```json
{
  "error": "Invalid username or password"
}
```

---

### 3. **Làm Mới Token**
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Phản hồi thành công (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000
}
```

---

### 4. **Lấy Thông Tin Người Dùng Hiện Tại**
```http
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Phản hồi thành công (200 OK):**
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "roles": ["USER"]
}
```

---

## 🧪 Các Phương Pháp Kiểm Tra

### **Phương pháp 1: Sử dụng curl (Terminal)**

#### Đăng ký:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"123456\"}"
```

#### Đăng nhập:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"testuser\",\"password\":\"123456\"}"
```

#### Truy cập Endpoint được bảo vệ:
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

### **Phương pháp 2: Sử dụng Postman**

1. **Tạo Request Mới**
2. **Đặt Method & URL**: POST `http://localhost:8080/api/auth/login`
3. **Đặt Headers**: 
   - `Content-Type: application/json`
4. **Đặt Body** (raw JSON):
   ```json
   {
     "username": "testuser",
     "password": "123456"
   }
   ```
5. **Gửi Request**
6. **Copy accessToken** từ response
7. **Kiểm tra Endpoint được bảo vệ**:
   - URL: GET `http://localhost:8080/api/auth/me`
   - Headers: `Authorization: Bearer YOUR_TOKEN`

---

### **Phương pháp 3: Sử dụng Browser + JavaScript Console**

```javascript
// 1. Đăng ký
fetch('http://localhost:8080/api/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'testuser',
    email: 'test@example.com',
    password: '123456'
  })
})
.then(res => res.json())
.then(data => {
  console.log('Đã đăng ký:', data);
  localStorage.setItem('accessToken', data.accessToken);
});

// 2. Đăng nhập
fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'testuser',
    password: '123456'
  })
})
.then(res => res.json())
.then(data => {
  console.log('Đã đăng nhập:', data);
  localStorage.setItem('accessToken', data.accessToken);
});

// 3. Lấy thông tin người dùng hiện tại
fetch('http://localhost:8080/api/auth/me', {
  headers: {
    'Authorization': 'Bearer ' + localStorage.getItem('accessToken')
  }
})
.then(res => res.json())
.then(data => console.log('Người dùng hiện tại:', data));
```

---

## ✅ Các Kịch Bản Kiểm Tra

### **Kịch bản 1: Luồng Hoàn Chỉnh**
1. ✅ Đăng ký người dùng mới
2. ✅ Xác minh người dùng có thể đăng nhập
3. ✅ Sử dụng access token để truy cập endpoint được bảo vệ
4. ✅ Làm mới token trước khi hết hạn
5. ✅ Xác minh token mới hoạt động

### **Kịch bản 2: Xử Lý Lỗi**
1. ❌ Đăng nhập với mật khẩu sai → 401 Unauthorized
2. ❌ Đăng ký với username đã tồn tại → 400 Bad Request
3. ❌ Truy cập endpoint được bảo vệ mà không có token → 403 Forbidden
4. ❌ Sử dụng token đã hết hạn → 401 Unauthorized

### **Kịch bản 3: Bảo Mật**
1. ✅ Token chứa thông tin người dùng và vai trò
2. ✅ Chữ ký token hợp lệ
3. ✅ Token hết hạn sau 24 giờ
4. ✅ Refresh token hoạt động trong 7 ngày

---

## 🔍 Cấu Trúc JWT Token

Giải mã JWT token tại **https://jwt.io/**

Ví dụ payload:
```json
{
  "roles": ["USER"],
  "sub": "testuser",
  "iat": 1738654321,
  "exp": 1738740721
}
```

---

## 🐛 Xử Lý Sự Cố

| Vấn đề | Giải pháp |
|--------|-----------|
| 401 Unauthorized | Kiểm tra username/password hoặc tính hợp lệ của token |
| 403 Forbidden | Kiểm tra vai trò người dùng khớp với yêu cầu endpoint |
| Token hết hạn | Sử dụng endpoint refresh token |
| Lỗi CORS | Thêm cấu hình CORS trong SecurityConfig |
| Kết nối bị từ chối | Đảm bảo server đang chạy trên cổng 8080 |

---

## 📝 Ghi Chú

- **Access Token**: Hết hạn sau 24 giờ (86400000 ms)
- **Refresh Token**: Hết hạn sau 7 ngày (604800000 ms)
- **Loại Token**: Bearer
- **Thuật toán**: HS256 (HMAC with SHA-256)
- **Secret**: Được cấu hình trong application.properties

---

## 🎯 Kết Quả Mong Đợi

✅ Người dùng có thể đăng ký và nhận JWT token
✅ Người dùng có thể đăng nhập và nhận JWT token
✅ Các endpoint được bảo vệ yêu cầu JWT hợp lệ
✅ Token hết hạn bị từ chối
✅ Refresh token có thể tạo access token mới
✅ Đăng nhập qua Web UI vẫn hoạt động (dựa trên session)
✅ Đăng nhập OAuth2 vẫn hoạt động
