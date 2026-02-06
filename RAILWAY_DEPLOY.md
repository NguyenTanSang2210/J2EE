# 🚂 Hướng dẫn Deploy Spring Boot lên Railway

## 📋 Yêu cầu

1. **Railway Account** - Đăng ký tại [railway.app](https://railway.app)
2. **GitHub Repository** - Push code lên GitHub
3. **Railway MySQL Service** - Tạo MySQL database trên Railway

---

## 🔧 Cấu hình Railway

### Bước 1: Tạo MySQL Service

1. Vào Railway Dashboard → **New Project**
2. Chọn **Deploy MySQL**
3. Railway sẽ tự động tạo MySQL và cung cấp các biến môi trường:
   - `MYSQLHOST` - hostname của MySQL server
   - `MYSQLPORT` - port (thường là 3306)
   - `MYSQLDATABASE` - tên database
   - `MYSQLUSER` - username
   - `MYSQLPASSWORD` - password

### Bước 2: Deploy Spring Boot App

1. Trong cùng Project, click **New Service** → **GitHub Repo**
2. Chọn repository `NguyenTanSang2210/J2EE`
3. Railway tự động detect Maven và build

### Bước 3: Link MySQL với App

1. Vào App Service → **Variables** tab
2. Click **Reference** → Chọn MySQL service
3. Reference các biến:
   - `MYSQLHOST` → `${{MySQL.MYSQLHOST}}`
   - `MYSQLPORT` → `${{MySQL.MYSQLPORT}}`
   - `MYSQLDATABASE` → `${{MySQL.MYSQLDATABASE}}`
   - `MYSQLUSER` → `${{MySQL.MYSQLUSER}}`
   - `MYSQLPASSWORD` → `${{MySQL.MYSQLPASSWORD}}`

### Bước 4: Thêm các biến môi trường bắt buộc

Vào App Service → **Variables** → **Raw Editor**, thêm:

```env
# JWT Configuration (BẮT BUỘC)
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970433F4528482B4D6251655468576D5A7134743777217A25432A462D4A614E645267
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Google OAuth2 (BẮT BUỘC nếu dùng login Google)
GOOGLE_CLIENT_ID=451298360913-2muqvld6eptm7g7vct637hgoj65j05lq.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=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

# SePay Payment Gateway (BẮT BUỘC nếu dùng thanh toán)
SEPAY_API_TOKEN=R74V02QIOYEDK0FOXGQPQYD1PG3RSMBUHVZHWURPVAHFWXZAP4IBLOVNYMXGTGJS
SEPAY_ACCOUNT_NUMBER=35322102004
SEPAY_ACCOUNT_NAME=NGUYEN TAN SANG
SEPAY_BANK_CODE=TPB

# PORT (Railway tự động inject, không cần thêm)
# PORT=${{RAILWAY_PUBLIC_PORT}}
```

---

## ✅ Kiểm tra Deploy

### 1. Xem Build Logs

```
Railway Dashboard → App Service → Deployments → Latest Build
```

Logs thành công sẽ hiển thị:
```
✅ HikariCP DataSource initialized successfully
✅ Started QlsApplication in X.XXX seconds
```

### 2. Kiểm tra Database Connection

Logs sẽ hiển thị thông tin kết nối:
```
================================================================================
RAILWAY DATABASE CONFIGURATION
================================================================================
DB Host: viaduct.proxy.rlwy.net
DB Port: 12345
DB Name: railway
DB User: root
JDBC URL: jdbc:mysql://viaduct.proxy.rlwy.net:12345/railway...
================================================================================
```

### 3. Truy cập ứng dụng

Railway cung cấp domain public:
```
https://your-app.up.railway.app
```

---

## 🐛 Debug Lỗi Thường Gặp

### Lỗi: Communications link failure

**Nguyên nhân**: MySQL biến môi trường chưa được reference đúng

**Giải pháp**:
1. Kiểm tra MySQL service đang **ONLINE**
2. Verify các biến `MYSQLHOST`, `MYSQLPORT`, etc đã được reference
3. Redeploy app sau khi thêm biến

### Lỗi: Unable to start embedded Tomcat

**Nguyên nhân**: Port conflict hoặc biến `PORT` chưa được inject

**Giải pháp**:
- Railway tự động inject `PORT`, không cần set thủ công
- Kiểm tra `railway.json` có `-Dserver.port=$PORT`

### Lỗi: UnsatisfiedDependencyException

**Nguyên nhân**: EntityManagerFactory không khởi tạo do DB chưa connect

**Giải pháp**:
1. Kiểm tra MySQL service đang chạy
2. Verify JDBC URL đúng format
3. Check logs để xem biến môi trường nào thiếu

### Lỗi: JWT_SECRET not found

**Nguyên nhân**: Thiếu biến môi trường JWT

**Giải pháp**:
- Thêm `JWT_SECRET` vào Variables tab
- Value phải là base64 string dài ít nhất 256 bits

---

## 📊 Monitoring

### Health Check

Railway tự động ping endpoint:
```
GET https://your-app.up.railway.app/actuator/health
```

Nếu app không response trong 10 phút → Railway restart

### Logs

Xem realtime logs:
```
Railway Dashboard → App Service → Logs tab
```

Hoặc dùng CLI:
```bash
railway logs
```

---

## 🚀 Deployment Flow

```
Local Dev (application.properties)
         ↓
   git push origin main
         ↓
Railway detect changes → Trigger build
         ↓
Build: mvn clean package -DskipTests
         ↓
Start: java -Dspring.profiles.active=prod -jar QLS.jar
         ↓
Load application-prod.properties
         ↓
Read biến môi trường MySQL từ Railway
         ↓
Connect database → Start Tomcat → App ONLINE
```

---

## 📝 Checklist Deploy

- [ ] MySQL service đã tạo và ONLINE
- [ ] App service đã link với GitHub repo
- [ ] Biến môi trường MySQL đã reference đúng
- [ ] JWT_SECRET đã được set
- [ ] Google OAuth2 credentials đã được set (nếu dùng)
- [ ] SePay credentials đã được set (nếu dùng)
- [ ] Build thành công (check Deployments log)
- [ ] App start không lỗi
- [ ] Database connection thành công
- [ ] Domain public accessible

---

## 🔗 Resources

- [Railway Documentation](https://docs.railway.app)
- [Railway MySQL Guide](https://docs.railway.app/databases/mysql)
- [Spring Boot Railway Guide](https://docs.railway.app/guides/spring-boot)

---

**Last Updated**: 06/02/2026  
**Author**: Nguyễn Tấn Sang  
**Project**: QLS - Book Management System
