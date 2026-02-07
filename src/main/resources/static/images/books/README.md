# Book Images Directory

Thư mục này chứa hình ảnh sách của hệ thống QLS.

## Cấu trúc thư mục
```
/images/books/
├── default-book.jpg      # Ảnh mặc định cho sách (REQUIRED)
├── [uuid].jpg           # Ảnh upload từ admin
├── [uuid].png           # Có thể là jpg, jpeg, png, gif
└── ...
```

## Hướng dẫn setup

### 1. Thêm ảnh mặc định
- Tải một ảnh sách mặc định về máy (kích thước khuyến nghị: 300x400px)
- Đổi tên thành `default-book.jpg` 
- Đặt vào thư mục này: `src/main/resources/static/images/books/default-book.jpg`

### 2. Kiểm tra
- Khởi động ứng dụng
- Truy cập: http://localhost:8080/images/books/default-book.jpg
- Nếu hiển thị ảnh thì thành công

## Lưu ý
- Ảnh mặc định là **BẮT BUỘC** để hệ thống hoạt động đúng
- Các ảnh upload sẽ có tên UUID ngẫu nhiên để tránh trùng lặp
- Hệ thống hỗ trợ: jpg, jpeg, png, gif
- Kích thước tối đa: 5MB

## Ví dụ ảnh mặc định
Bạn có thể tải ảnh mặc định từ các nguồn sau:
- https://via.placeholder.com/300x400/2196F3/FFFFFF?text=SÁCH
- https://picsum.photos/300/400 (ảnh random)
- Hoặc tự thiết kế ảnh với text "Chưa có ảnh"