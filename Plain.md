# KẾ HOẠCH PHÁT TRIỂN DỰ ÁN QUẢN LÝ SÁCH (QLS)

> **Dự án**: Hệ thống Quản lý Sách (Book Management System)  
> **Sinh viên**: Nguyễn Tấn Sang - MSSV: 2280602715  
> **Ngày lập**: 05/02/2026  
> **Công nghệ**: Spring Boot 4.0.2, Java 21, MySQL, Thymeleaf

---

## 📊 PHẦN I: PHÂN TÍCH HIỆN TRẠNG

### 1.1. Các chức năng đã triển khai

#### ✅ Quản lý Sách (Book Management)
- CRUD đầy đủ: Thêm, sửa, xóa, xem danh sách sách
- Phân trang (pagination) và sắp xếp
- Entity: Book (id, title, author, price, category_id)
- Repository: IBookRepository với JPA
- Controller: BookController với các endpoint cơ bản

#### ✅ Phân loại Sách (Category Management)
- Entity Category với quan hệ OneToMany với Book
- Hiển thị sách theo từng danh mục
- Service: CategoryService

#### ✅ Giỏ hàng (Shopping Cart)
- Thêm sách vào giỏ hàng (session-based)
- Cập nhật số lượng
- Xóa sản phẩm khỏi giỏ
- Xóa toàn bộ giỏ hàng
- Service: CartService
- Entity: Item (DTO cho cart items)

#### ✅ Xác thực và Phân quyền (Authentication & Authorization)
- Đăng ký tài khoản mới
- Đăng nhập local (username/password)
- Đăng nhập OAuth2 với Google
- Spring Security với BCrypt password encoder
- Phân quyền User và Role
- JWT configuration (secret, expiration)

#### ✅ Entities Database
- **User**: id, username, password, email, phone, provider, roles
- **Role**: id, name, description (implements GrantedAuthority)
- **Book**: id, title, author, price, category
- **Category**: id, name
- **Invoice**: id, invoiceDate, price, user_id (Entity đã có nhưng chưa sử dụng)
- **ItemInvoice**: id, quantity, book_id, invoice_id (Entity đã có nhưng chưa sử dụng)

### 1.2. Hạn chế và vấn đề cần khắc phục

#### ❌ Chức năng chưa hoàn thiện
1. **Invoice không được sử dụng**: Entity Invoice và ItemInvoice đã tạo nhưng chưa có controller/service để xử lý
2. **Không có lịch sử đơn hàng**: User không thể xem các đơn đã đặt
3. **Checkout chưa hoàn chỉnh**: Chỉ có endpoint `/cart/checkout` nhưng chưa xử lý logic tạo đơn
4. **Không có quản lý kho**: Không theo dõi số lượng sách tồn kho
5. **Tìm kiếm hạn chế**: Chỉ có method `searchBook()` cơ bản
6. **Không có trang chi tiết sách**: Chưa có view để xem thông tin chi tiết
7. **Admin dashboard thiếu**: Chưa có giao diện quản trị tổng quan

#### ❌ Trải nghiệm người dùng
- Không có filter nâng cao (theo giá, category)
- Không có sắp xếp linh hoạt
- Không có đánh giá/review sách
- Không có wishlist
- Không có thông báo khi đặt hàng thành công

#### ❌ Quản lý và báo cáo
- Không có thống kê doanh thu
- Không có báo cáo sách bán chạy
- Không có quản lý user từ admin
- Không có xuất báo cáo Excel/PDF

---

## 🎯 PHẦN II: KẾ HOẠCH PHÁT TRIỂN CHI TIẾT

### GIAI ĐOẠN 1: HOÀN THIỆN CORE FEATURES (Ưu tiên cao ⭐⭐⭐)

---

## 📦 CHỨC NĂNG 1: QUẢN LÝ HÓA ĐƠN & LỊCH SỬ MUA HÀNG

### 🎯 Mục tiêu
Hoàn thiện quy trình mua hàng từ giỏ hàng đến đơn hàng, cho phép user theo dõi lịch sử và trạng thái đơn hàng.

### 📋 Yêu cầu chi tiết

#### 1.1. Backend Implementation

**A. Cập nhật Entity Invoice**
```java
// File: src/main/java/nhom2/QLS/entities/Invoice.java
// Thêm các trường:
- status: String (PENDING, PROCESSING, COMPLETED, CANCELLED)
- paymentMethod: String (COD, BANKING, CREDIT_CARD)
- shippingAddress: String
- phone: String
- note: String
```

**B. Tạo InvoiceService**
```java
// File: src/main/java/nhom2/QLS/services/InvoiceService.java
// Methods cần implement:
- Invoice createInvoiceFromCart(User user, Cart cart, String paymentMethod, String address)
- List<Invoice> getInvoicesByUser(Long userId)
- Optional<Invoice> getInvoiceById(Long id)
- Invoice updateInvoiceStatus(Long id, String status)
- void cancelInvoice(Long id) // Chỉ được cancel khi status = PENDING
- List<Invoice> getAllInvoices(Pageable pageable) // For admin
```

**C. Tạo InvoiceController**
```java
// File: src/main/java/nhom2/QLS/controllers/InvoiceController.java
// Endpoints:
GET  /invoices              → Danh sách đơn hàng của user hiện tại
GET  /invoices/{id}         → Chi tiết đơn hàng
POST /invoices/checkout     → Tạo đơn hàng từ cart
PUT  /invoices/{id}/cancel  → Hủy đơn hàng
```

**D. Tạo AdminInvoiceController**
```java
// File: src/main/java/nhom2/QLS/controllers/AdminInvoiceController.java
// Endpoints cho admin:
GET  /admin/invoices                    → Tất cả đơn hàng
PUT  /admin/invoices/{id}/status        → Cập nhật trạng thái
GET  /admin/invoices/statistics         → Thống kê đơn hàng
```

#### 1.2. Frontend Implementation

**A. Cập nhật trang Giỏ hàng**
```html
<!-- File: src/main/resources/templates/book/cart.html -->
<!-- Thêm form checkout với:
- Input địa chỉ giao hàng
- Input số điện thoại
- Select phương thức thanh toán
- Textarea ghi chú
- Button "Đặt hàng"
-->
```

**B. Tạo trang Checkout Confirmation**
```html
<!-- File: src/main/resources/templates/invoice/checkout-success.html -->
<!-- Hiển thị:
- Thông báo đặt hàng thành công
- Mã đơn hàng
- Tổng tiền
- Thông tin giao hàng
- Button "Xem đơn hàng" và "Tiếp tục mua sắm"
-->
```

**C. Tạo trang Lịch sử Đơn hàng**
```html
<!-- File: src/main/resources/templates/invoice/my-orders.html -->
<!-- Table hiển thị:
- Mã đơn hàng
- Ngày đặt
- Trạng thái (với badge màu)
- Tổng tiền
- Actions: Xem chi tiết, Hủy (nếu PENDING)
-->
```

**D. Tạo trang Chi tiết Đơn hàng**
```html
<!-- File: src/main/resources/templates/invoice/order-detail.html -->
<!-- Hiển thị:
- Thông tin đơn hàng (mã, ngày, trạng thái)
- Thông tin giao hàng
- Danh sách sản phẩm (bảng)
- Tổng cộng
- Timeline trạng thái đơn hàng
-->
```

**E. Trang quản lý đơn hàng Admin**
```html
<!-- File: src/main/resources/templates/admin/orders.html -->
<!-- Table với:
- Tất cả đơn hàng
- Filter theo trạng thái
- Tìm kiếm theo mã đơn/user
- Cập nhật trạng thái
- Pagination
-->
```

### 🔄 Luồng hoạt động

#### Flow 1: User đặt hàng
```
1. User ở trang Cart (/cart)
2. Nhập thông tin: địa chỉ, SĐT, phương thức thanh toán
3. Click "Đặt hàng"
4. POST /invoices/checkout
   - Validate thông tin
   - Tạo Invoice (status = PENDING)
   - Tạo ItemInvoice cho từng item trong cart
   - Xóa cart
   - (Future) Giảm stock sách
5. Redirect → /invoices/checkout-success
6. User xem thông báo thành công
```

#### Flow 2: User xem lịch sử
```
1. User click menu "Đơn hàng của tôi"
2. GET /invoices
3. Hiển thị danh sách đơn hàng với status
4. Click "Xem chi tiết" → GET /invoices/{id}
5. Hiển thị thông tin đầy đủ
```

#### Flow 3: User hủy đơn
```
1. Ở trang order-detail
2. Click "Hủy đơn hàng" (chỉ hiện nếu status = PENDING)
3. Confirm dialog
4. PUT /invoices/{id}/cancel
   - Check status = PENDING
   - Update status = CANCELLED
   - (Future) Hoàn lại stock
5. Refresh trang với status mới
```

#### Flow 4: Admin quản lý
```
1. Admin vào /admin/orders
2. Xem tất cả đơn hàng
3. Filter theo status
4. Click "Cập nhật" → Chọn status mới (PROCESSING, COMPLETED)
5. PUT /admin/invoices/{id}/status
6. Refresh danh sách
```

### ✅ Checklist Implementation

- [ ] Cập nhật Invoice entity (thêm fields mới)
- [ ] Tạo InvoiceService với đầy đủ methods
- [ ] Tạo InvoiceController (user endpoints)
- [ ] Tạo AdminInvoiceController (admin endpoints)
- [ ] Cập nhật cart.html với form checkout
- [ ] Tạo checkout-success.html
- [ ] Tạo my-orders.html
- [ ] Tạo order-detail.html
- [ ] Tạo admin/orders.html
- [ ] Thêm menu link "Đơn hàng của tôi" vào layout
- [ ] Test flow đặt hàng end-to-end
- [ ] Test hủy đơn hàng
- [ ] Test admin cập nhật status

### 📝 Notes
- Status transitions: PENDING → PROCESSING → COMPLETED
- CANCELLED có thể từ PENDING hoặc PROCESSING
- Chỉ user sở hữu mới xem được detail
- Admin có quyền xem tất cả đơn

---
-- Chức năng 1 đã hoàn thành --



## 🔍 CHỨC NĂNG 2: TÌM KIẾM VÀ LỌC NÂNG CAO

### 🎯 Mục tiêu
Nâng cao trải nghiệm tìm kiếm sách với nhiều tiêu chí và filter động.

### 📋 Yêu cầu chi tiết

#### 2.1. Backend Implementation

**A. Tạo SearchDTO**
```java
// File: src/main/java/nhom2/QLS/dtos/SearchDTO.java
public class SearchDTO {
    private String keyword;        // Tìm trong title và author
    private Long categoryId;       // Lọc theo category
    private Double minPrice;       // Giá từ
    private Double maxPrice;       // Giá đến
    private String sortBy;         // title, price, author
    private String sortDirection;  // asc, desc
}
```

**B. Mở rộng IBookRepository**
```java
// File: src/main/java/nhom2/QLS/repositories/IBookRepository.java
// Thêm methods:

@Query("SELECT b FROM Book b WHERE " +
       "(:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
       "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')))")
Page<Book> searchBooks(
    @Param("keyword") String keyword,
    Pageable pageable
);

Page<Book> findByCategoryId(Long categoryId, Pageable pageable);

Page<Book> findByPriceBetween(Double minPrice, Double maxPrice, Pageable pageable);

@Query("SELECT b FROM Book b WHERE " +
       "(:categoryId IS NULL OR b.category.id = :categoryId) " +
       "AND (:minPrice IS NULL OR b.price >= :minPrice) " +
       "AND (:maxPrice IS NULL OR b.price <= :maxPrice) " +
       "AND (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
       "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')))")
Page<Book> findByFilters(
    @Param("keyword") String keyword,
    @Param("categoryId") Long categoryId,
    @Param("minPrice") Double minPrice,
    @Param("maxPrice") Double maxPrice,
    Pageable pageable
);
```

**C. Cập nhật BookService**
```java
// File: src/main/java/nhom2/QLS/services/BookService.java
// Thêm method:

public Page<Book> searchWithFilters(SearchDTO searchDTO, int page, int size) {
    Sort sort = Sort.by(
        searchDTO.getSortDirection().equals("desc") 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC,
        searchDTO.getSortBy()
    );
    Pageable pageable = PageRequest.of(page, size, sort);
    
    return bookRepository.findByFilters(
        searchDTO.getKeyword(),
        searchDTO.getCategoryId(),
        searchDTO.getMinPrice(),
        searchDTO.getMaxPrice(),
        pageable
    );
}
```

**D. Cập nhật BookController**
```java
// File: src/main/java/nhom2/QLS/controllers/BookController.java
// Thêm endpoint:

@GetMapping("/search")
public String searchBooks(
    @ModelAttribute SearchDTO searchDTO,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    Model model
) {
    Page<Book> booksPage = bookService.searchWithFilters(searchDTO, page, size);
    
    model.addAttribute("books", booksPage.getContent());
    model.addAttribute("currentPage", page);
    model.addAttribute("totalPages", booksPage.getTotalPages());
    model.addAttribute("totalItems", booksPage.getTotalElements());
    model.addAttribute("categories", categoryService.getAllCategories());
    model.addAttribute("searchDTO", searchDTO);
    
    return "book/list";
}
```

#### 2.2. Frontend Implementation

**A. Thêm Search Bar vào Header**
```html
<!-- File: src/main/resources/templates/layout.html -->
<!-- Trong navbar, thêm:
<form action="/books/search" method="get" class="d-flex">
    <input class="form-control me-2" type="search" name="keyword" 
           placeholder="Tìm sách theo tên hoặc tác giả...">
    <button class="btn btn-outline-success" type="submit">Tìm</button>
</form>
-->
```

**B. Cập nhật trang Danh sách Sách**
```html
<!-- File: src/main/resources/templates/book/list.html -->
<!-- Thêm Sidebar Filter:

<div class="row">
    <!-- Sidebar Filter -->
    <div class="col-md-3">
        <div class="card">
            <div class="card-header">
                <h5>Lọc sách</h5>
            </div>
            <div class="card-body">
                <form action="/books/search" method="get" id="filterForm">
                    
                    <!-- Tìm kiếm -->
                    <div class="mb-3">
                        <label>Từ khóa</label>
                        <input type="text" name="keyword" class="form-control" 
                               th:value="${searchDTO?.keyword}">
                    </div>
                    
                    <!-- Danh mục -->
                    <div class="mb-3">
                        <label>Danh mục</label>
                        <select name="categoryId" class="form-select">
                            <option value="">Tất cả</option>
                            <option th:each="cat : ${categories}" 
                                    th:value="${cat.id}"
                                    th:text="${cat.name}"
                                    th:selected="${searchDTO?.categoryId == cat.id}">
                            </option>
                        </select>
                    </div>
                    
                    <!-- Khoảng giá -->
                    <div class="mb-3">
                        <label>Giá từ</label>
                        <input type="number" name="minPrice" class="form-control"
                               th:value="${searchDTO?.minPrice}">
                    </div>
                    <div class="mb-3">
                        <label>Giá đến</label>
                        <input type="number" name="maxPrice" class="form-control"
                               th:value="${searchDTO?.maxPrice}">
                    </div>
                    
                    <!-- Sắp xếp -->
                    <div class="mb-3">
                        <label>Sắp xếp theo</label>
                        <select name="sortBy" class="form-select">
                            <option value="id">Mặc định</option>
                            <option value="title" th:selected="${searchDTO?.sortBy == 'title'}">Tên sách</option>
                            <option value="price" th:selected="${searchDTO?.sortBy == 'price'}">Giá</option>
                            <option value="author" th:selected="${searchDTO?.sortBy == 'author'}">Tác giả</option>
                        </select>
                    </div>
                    
                    <div class="mb-3">
                        <select name="sortDirection" class="form-select">
                            <option value="asc">Tăng dần</option>
                            <option value="desc" th:selected="${searchDTO?.sortDirection == 'desc'}">Giảm dần</option>
                        </select>
                    </div>
                    
                    <button type="submit" class="btn btn-primary w-100">Áp dụng</button>
                    <a href="/books" class="btn btn-secondary w-100 mt-2">Xóa bộ lọc</a>
                </form>
            </div>
        </div>
    </div>
    
    <!-- Danh sách sách -->
    <div class="col-md-9">
        <!-- Hiển thị số kết quả -->
        <p th:if="${totalItems != null}">
            Tìm thấy <strong th:text="${totalItems}"></strong> kết quả
        </p>
        
        <!-- Grid sách -->
        <!-- ... existing book list ... -->
        
        <!-- Pagination -->
        <!-- ... existing pagination ... -->
    </div>
</div>
-->
```

**C. (Optional) Thêm AJAX Filter**
```javascript
// File: src/main/resources/static/js/filter.js
// jQuery AJAX để filter không reload trang:

$(document).ready(function() {
    $('#filterForm').on('submit', function(e) {
        e.preventDefault();
        
        $.ajax({
            url: '/books/search',
            type: 'GET',
            data: $(this).serialize(),
            success: function(response) {
                // Update book list container
                $('#bookListContainer').html($(response).find('#bookListContainer').html());
            }
        });
    });
    
    // Auto-submit on select change
    $('#filterForm select').on('change', function() {
        $('#filterForm').submit();
    });
});
```

### 🔄 Luồng hoạt động

```
1. User vào trang /books
2. Nhìn thấy sidebar filter bên trái
3. Nhập từ khóa "Harry Potter"
4. Chọn category "Tiểu thuyết"
5. Nhập giá từ 50000 đến 200000
6. Chọn sắp xếp theo "Giá - Tăng dần"
7. Click "Áp dụng"
8. GET /books/search?keyword=Harry&categoryId=1&minPrice=50000&maxPrice=200000&sortBy=price&sortDirection=asc
9. Backend query với tất cả điều kiện
10. Trả về kết quả phù hợp
11. Hiển thị "Tìm thấy X kết quả"
12. User có thể click "Xóa bộ lọc" để reset
```

### ✅ Checklist Implementation

- [ ] Tạo SearchDTO
- [ ] Mở rộng IBookRepository với query methods
- [ ] Cập nhật BookService với searchWithFilters()
- [ ] Thêm endpoint /books/search trong BookController
- [ ] Thêm search bar vào layout.html
- [ ] Tạo sidebar filter trong book/list.html
- [ ] Hiển thị số kết quả tìm được
- [ ] Test tìm kiếm theo keyword
- [ ] Test filter theo category
- [ ] Test filter theo khoảng giá
- [ ] Test sắp xếp
- [ ] Test kết hợp nhiều filter
- [ ] (Optional) Implement AJAX filter

---Hoàn thành chức năng 2---

## 📦 CHỨC NĂNG 3: QUẢN LÝ KHO (INVENTORY)

### 🎯 Mục tiêu
Theo dõi số lượng sách tồn kho, tự động cập nhật khi bán hàng, cảnh báo sách sắp hết.

### 📋 Yêu cầu chi tiết

#### 3.1. Database Changes

**A. Cập nhật Book Entity**
```java
// File: src/main/java/nhom2/QLS/entities/Book.java
// Thêm fields:

@Column(name = "stock", nullable = false)
@Min(value = 0, message = "Stock cannot be negative")
private Integer stock = 0;

@Column(name = "is_available", nullable = false)
private Boolean isAvailable = true;

// Thêm method helper:
public boolean hasStock(int quantity) {
    return this.stock >= quantity;
}

public void reduceStock(int quantity) {
    if (this.stock >= quantity) {
        this.stock -= quantity;
        if (this.stock == 0) {
            this.isAvailable = false;
        }
    } else {
        throw new IllegalStateException("Not enough stock");
    }
}

public void increaseStock(int quantity) {
    this.stock += quantity;
    if (this.stock > 0) {
        this.isAvailable = true;
    }
}
```

**B. Database Migration**
```sql
-- Chạy SQL để thêm columns vào bảng book:
ALTER TABLE book ADD COLUMN stock INT DEFAULT 0 NOT NULL;
ALTER TABLE book ADD COLUMN is_available BOOLEAN DEFAULT TRUE NOT NULL;

-- Update existing books với stock mặc định:
UPDATE book SET stock = 100, is_available = TRUE;
```

#### 3.2. Backend Implementation

**A. Cập nhật BookService**
```java
// File: src/main/java/nhom2/QLS/services/BookService.java
// Thêm methods:

public boolean checkStock(Long bookId, int quantity) {
    Book book = bookRepository.findById(bookId)
        .orElseThrow(() -> new IllegalArgumentException("Book not found"));
    return book.hasStock(quantity);
}

public void reduceStock(Long bookId, int quantity) {
    Book book = bookRepository.findById(bookId)
        .orElseThrow(() -> new IllegalArgumentException("Book not found"));
    book.reduceStock(quantity);
    bookRepository.save(book);
}

public void increaseStock(Long bookId, int quantity) {
    Book book = bookRepository.findById(bookId)
        .orElseThrow(() -> new IllegalArgumentException("Book not found"));
    book.increaseStock(quantity);
    bookRepository.save(book);
}

public List<Book> getLowStockBooks(int threshold) {
    return bookRepository.findByStockLessThanAndIsAvailableTrue(threshold);
}

public List<Book> getOutOfStockBooks() {
    return bookRepository.findByStockEquals(0);
}
```

**B. Cập nhật IBookRepository**
```java
// File: src/main/java/nhom2/QLS/repositories/IBookRepository.java
// Thêm methods:

List<Book> findByStockLessThanAndIsAvailableTrue(int threshold);
List<Book> findByStockEquals(int stock);
List<Book> findByIsAvailableFalse();
```

**C. Cập nhật InvoiceService**
```java
// File: src/main/java/nhom2/QLS/services/InvoiceService.java
// Trong method createInvoiceFromCart(), thêm logic giảm stock:

public Invoice createInvoiceFromCart(User user, Cart cart, ...) {
    // ... existing code ...
    
    // Kiểm tra stock trước khi tạo invoice
    for (Item item : cart.getCartItems()) {
        if (!bookService.checkStock(item.getId(), item.getQuantity())) {
            throw new IllegalStateException(
                "Sách '" + item.getName() + "' không đủ số lượng trong kho"
            );
        }
    }
    
    // Tạo invoice
    Invoice invoice = new Invoice();
    // ... set fields ...
    invoice = invoiceRepository.save(invoice);
    
    // Tạo item invoice và giảm stock
    for (Item item : cart.getCartItems()) {
        ItemInvoice itemInvoice = new ItemInvoice();
        itemInvoice.setBook(bookService.getBookById(item.getId()).get());
        itemInvoice.setQuantity(item.getQuantity());
        itemInvoice.setInvoice(invoice);
        itemInvoiceRepository.save(itemInvoice);
        
        // Giảm stock
        bookService.reduceStock(item.getId(), item.getQuantity());
    }
    
    return invoice;
}
```

**D. Tạo InventoryController (Admin)**
```java
// File: src/main/java/nhom2/QLS/controllers/admin/InventoryController.java

@Controller
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final BookService bookService;
    
    @GetMapping
    public String inventoryPage(Model model) {
        model.addAttribute("books", bookService.getAllBooks(0, 1000, "title").getContent());
        model.addAttribute("lowStock", bookService.getLowStockBooks(10));
        model.addAttribute("outOfStock", bookService.getOutOfStockBooks());
        return "admin/inventory";
    }
    
    @PostMapping("/update/{id}")
    public String updateStock(@PathVariable Long id, 
                             @RequestParam int quantity,
                             @RequestParam String action) {
        if ("increase".equals(action)) {
            bookService.increaseStock(id, quantity);
        } else if ("decrease".equals(action)) {
            bookService.reduceStock(id, quantity);
        }
        return "redirect:/admin/inventory";
    }
}
```

#### 3.3. Frontend Implementation

**A. Cập nhật book/list.html**
```html
<!-- File: src/main/resources/templates/book/list.html -->
<!-- Trong mỗi card sách, thêm hiển thị stock:

<div class="card">
    <div class="card-body">
        <h5 th:text="${book.title}"></h5>
        <p th:text="${book.author}"></p>
        <p th:text="${book.price}"></p>
        
        <!-- Stock indicator -->
        <div class="stock-info">
            <span th:if="${book.stock > 10}" class="badge bg-success">
                Còn <span th:text="${book.stock}"></span> quyển
            </span>
            <span th:if="${book.stock > 0 && book.stock <= 10}" class="badge bg-warning">
                Chỉ còn <span th:text="${book.stock}"></span> quyển
            </span>
            <span th:if="${book.stock == 0}" class="badge bg-danger">
                Hết hàng
            </span>
        </div>
        
        <!-- Disable add to cart if out of stock -->
        <button th:if="${book.stock > 0}" 
                class="btn btn-primary" 
                onclick="addToCart(...)">
            Thêm vào giỏ
        </button>
        <button th:if="${book.stock == 0}" 
                class="btn btn-secondary" 
                disabled>
            Hết hàng
        </button>
    </div>
</div>
-->
```

**B. Cập nhật book/add.html và edit.html**
```html
<!-- File: src/main/resources/templates/book/add.html -->
<!-- Thêm input cho stock:

<div class="mb-3">
    <label for="stock" class="form-label">Số lượng trong kho</label>
    <input type="number" 
           class="form-control" 
           id="stock" 
           name="stock"
           th:field="*{stock}"
           min="0"
           required>
</div>
-->
```

**C. Tạo admin/inventory.html**
```html
<!-- File: src/main/resources/templates/admin/inventory.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Quản lý kho</title>
</head>
<body>
    <div class="container mt-4">
        <h2>Quản lý kho sách</h2>
        
        <!-- Alert boxes -->
        <div class="row mb-4">
            <div class="col-md-6">
                <div class="alert alert-warning">
                    <h5>Sách sắp hết hàng (<span th:text="${#lists.size(lowStock)}"></span>)</h5>
                    <ul>
                        <li th:each="book : ${lowStock}">
                            <span th:text="${book.title}"></span> - 
                            Còn <strong th:text="${book.stock}"></strong> quyển
                        </li>
                    </ul>
                </div>
            </div>
            <div class="col-md-6">
                <div class="alert alert-danger">
                    <h5>Sách hết hàng (<span th:text="${#lists.size(outOfStock)}"></span>)</h5>
                    <ul>
                        <li th:each="book : ${outOfStock}" th:text="${book.title}"></li>
                    </ul>
                </div>
            </div>
        </div>
        
        <!-- Inventory table -->
        <table class="table table-striped">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Tên sách</th>
                    <th>Tác giả</th>
                    <th>Danh mục</th>
                    <th>Giá</th>
                    <th>Tồn kho</th>
                    <th>Trạng thái</th>
                    <th>Thao tác</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="book : ${books}">
                    <td th:text="${book.id}"></td>
                    <td th:text="${book.title}"></td>
                    <td th:text="${book.author}"></td>
                    <td th:text="${book.category.name}"></td>
                    <td th:text="${book.price}"></td>
                    <td>
                        <span th:text="${book.stock}"></span>
                        <span th:if="${book.stock < 10}" class="text-warning">⚠️</span>
                        <span th:if="${book.stock == 0}" class="text-danger">❌</span>
                    </td>
                    <td>
                        <span th:if="${book.isAvailable}" class="badge bg-success">Có sẵn</span>
                        <span th:unless="${book.isAvailable}" class="badge bg-danger">Hết hàng</span>
                    </td>
                    <td>
                        <button class="btn btn-sm btn-success" 
                                onclick="updateStock([[${book.id}]], 'increase')">
                            + Nhập thêm
                        </button>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
    
    <!-- Modal cập nhật stock -->
    <div class="modal fade" id="stockModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Cập nhật kho</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <form id="stockForm" action="/admin/inventory/update/{id}" method="post">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label>Số lượng</label>
                            <input type="number" name="quantity" class="form-control" min="1" required>
                        </div>
                        <input type="hidden" name="action" id="stockAction">
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary">Xác nhận</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    
    <script>
    function updateStock(bookId, action) {
        $('#stockAction').val(action);
        $('#stockForm').attr('action', '/admin/inventory/update/' + bookId);
        $('#stockModal').modal('show');
    }
    </script>
</body>
</html>
```

### 🔄 Luồng hoạt động

#### Flow 1: User mua hàng
```
1. User thêm sách vào giỏ (số lượng 2)
2. Checkout
3. InvoiceService.createInvoiceFromCart()
   - Check book.hasStock(2) → true
   - Tạo Invoice
   - Tạo ItemInvoice
   - bookService.reduceStock(bookId, 2)
   - book.stock giảm từ 50 → 48
4. Nếu stock = 0 → isAvailable = false
5. Sách không thể add to cart nữa
```

#### Flow 2: Admin nhập hàng
```
1. Admin vào /admin/inventory
2. Thấy warning "Sách sắp hết"
3. Click "Nhập thêm" cho sách có ID = 5
4. Nhập số lượng: 50
5. POST /admin/inventory/update/5?quantity=50&action=increase
6. bookService.increaseStock(5, 50)
7. Stock tăng, isAvailable = true
8. Redirect về trang inventory
```

### ✅ Checklist Implementation

- [ ] Thêm fields stock và isAvailable vào Book entity
- [ ] Chạy SQL migration để update database
- [ ] Thêm methods quản lý stock vào BookService
- [ ] Thêm query methods vào IBookRepository
- [ ] Cập nhật InvoiceService để giảm stock khi checkout
- [ ] Tạo InventoryController cho admin
- [ ] Cập nhật book/list.html hiển thị stock
- [ ] Disable button "Thêm giỏ" khi hết hàng
- [ ] Thêm input stock vào form add/edit sách
- [ ] Tạo admin/inventory.html
- [ ] Thêm menu "Quản lý kho" cho admin
- [ ] Test checkout giảm stock
- [ ] Test không cho mua khi hết hàng
- [ ] Test admin nhập thêm kho

---Hoàn thành Chức năng 3---


### GIAI ĐOẠN 2: TÍNH NĂNG NÂNG CAO (Ưu tiên trung bình ⭐⭐)

---

## ⭐ CHỨC NĂNG 4: HỆ THỐNG ĐÁNH GIÁ & BÌNH LUẬN

### 🎯 Mục tiêu
Cho phép user đánh giá và viết review cho sách đã mua, tăng độ tin cậy và tương tác.

### 📋 Yêu cầu chi tiết

#### 4.1. Database Design

**A. Tạo Entity Review**
```java
// File: src/main/java/nhom2/QLS/entities/Review.java

@Entity
@Table(name = "review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "rating", nullable = false)
    @Min(1)
    @Max(5)
    private Integer rating; // 1-5 stars
    
    @Column(name = "comment", length = 1000)
    @Size(max = 1000, message = "Comment must be less than 1000 characters")
    private String comment;
    
    @Column(name = "review_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date reviewDate = new Date();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", referencedColumnName = "id")
    private Book book;
}
```

**B. Cập nhật Book Entity**
```java
// File: src/main/java/nhom2/QLS/entities/Book.java
// Thêm relationship:

@OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
@ToString.Exclude
private List<Review> reviews = new ArrayList<>();

// Helper method:
public double getAverageRating() {
    if (reviews == null || reviews.isEmpty()) {
        return 0.0;
    }
    return reviews.stream()
        .mapToInt(Review::getRating)
        .average()
        .orElse(0.0);
}

public int getTotalReviews() {
    return reviews != null ? reviews.size() : 0;
}
```

#### 4.2. Backend Implementation

**A. Tạo IReviewRepository**
```java
// File: src/main/java/nhom2/QLS/repositories/IReviewRepository.java

public interface IReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookIdOrderByReviewDateDesc(Long bookId);
    Page<Review> findByBookId(Long bookId, Pageable pageable);
    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);
    boolean existsByUserIdAndBookId(Long userId, Long bookId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId")
    Double getAverageRatingByBookId(@Param("bookId") Long bookId);
}
```

**B. Tạo ReviewService**
```java
// File: src/main/java/nhom2/QLS/services/ReviewService.java

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final IReviewRepository reviewRepository;
    private final IInvoiceRepository invoiceRepository;
    
    public Review addReview(Review review, User user, Long bookId) {
        // Check if user has purchased this book
        if (!hasUserPurchasedBook(user.getId(), bookId)) {
            throw new IllegalStateException("Bạn chỉ có thể đánh giá sách đã mua");
        }
        
        // Check if user already reviewed
        if (reviewRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            throw new IllegalStateException("Bạn đã đánh giá sách này rồi");
        }
        
        return reviewRepository.save(review);
    }
    
    public Page<Review> getReviewsByBook(Long bookId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("reviewDate").descending());
        return reviewRepository.findByBookId(bookId, pageable);
    }
    
    public Double getAverageRating(Long bookId) {
        return reviewRepository.getAverageRatingByBookId(bookId);
    }
    
    public boolean hasUserPurchasedBook(Long userId, Long bookId) {
        // Check if user has any completed invoice containing this book
        return invoiceRepository.existsByUserIdAndStatusAndItemInvoices_BookId(
            userId, "COMPLETED", bookId
        );
    }
    
    public boolean canUserReview(Long userId, Long bookId) {
        return hasUserPurchasedBook(userId, bookId) 
            && !reviewRepository.existsByUserIdAndBookId(userId, bookId);
    }
    
    public void deleteReview(Long id, User user) {
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));
            
        // Only owner or admin can delete
        if (!review.getUser().getId().equals(user.getId()) 
            && !user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"))) {
            throw new IllegalStateException("Không có quyền xóa review này");
        }
        
        reviewRepository.deleteById(id);
    }
}
```

**C. Cập nhật IInvoiceRepository**
```java
// File: src/main/java/nhom2/QLS/repositories/IInvoiceRepository.java
// Thêm method:

@Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END " +
       "FROM Invoice i JOIN i.itemInvoices ii " +
       "WHERE i.user.id = :userId AND i.status = :status AND ii.book.id = :bookId")
boolean existsByUserIdAndStatusAndItemInvoices_BookId(
    @Param("userId") Long userId,
    @Param("status") String status,
    @Param("bookId") Long bookId
);
```

**D. Tạo ReviewController**
```java
// File: src/main/java/nhom2/QLS/controllers/ReviewController.java

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final BookService bookService;
    
    @PostMapping("/add")
    public String addReview(
        @RequestParam Long bookId,
        @RequestParam Integer rating,
        @RequestParam String comment,
        @AuthenticationPrincipal User user,
        RedirectAttributes redirectAttributes
    ) {
        try {
            Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));
            
            Review review = Review.builder()
                .rating(rating)
                .comment(comment)
                .user(user)
                .book(book)
                .build();
            
            reviewService.addReview(review, user, bookId);
            redirectAttributes.addFlashAttribute("success", "Đánh giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/books/" + bookId;
    }
    
    @GetMapping("/delete/{id}")
    public String deleteReview(
        @PathVariable Long id,
        @AuthenticationPrincipal User user,
        RedirectAttributes redirectAttributes
    ) {
        try {
            reviewService.deleteReview(id, user);
            redirectAttributes.addFlashAttribute("success", "Xóa đánh giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/books";
    }
}
```

#### 4.3. Frontend Implementation

**A. Tạo trang Chi tiết Sách**
```html
<!-- File: src/main/resources/templates/book/detail.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <title th:text="${book.title}"></title>
</head>
<body>
    <div class="container mt-4">
        <!-- Book Details -->
        <div class="row">
            <div class="col-md-4">
                <!-- Book image placeholder -->
                <div class="bg-secondary text-white p-5 text-center">
                    <h1>📚</h1>
                </div>
            </div>
            <div class="col-md-8">
                <h2 th:text="${book.title}"></h2>
                <p class="text-muted">Tác giả: <span th:text="${book.author}"></span></p>
                <p>Danh mục: <span th:text="${book.category.name}"></span></p>
                <h4 class="text-danger" th:text="${#numbers.formatDecimal(book.price, 0, 'COMMA', 0, 'POINT')} + ' đ'"></h4>
                
                <!-- Rating display -->
                <div class="mb-3">
                    <span class="star-rating" th:data-rating="${book.averageRating}">
                        <span th:each="i : ${#numbers.sequence(1, 5)}" 
                              th:classappend="${i <= book.averageRating} ? 'filled' : ''">
                            ★
                        </span>
                    </span>
                    <span th:text="${#numbers.formatDecimal(book.averageRating, 1, 2)}"></span>
                    (<span th:text="${book.totalReviews}"></span> đánh giá)
                </div>
                
                <!-- Stock info -->
                <p th:if="${book.stock > 0}">
                    Còn <strong th:text="${book.stock}"></strong> quyển
                </p>
                <p th:if="${book.stock == 0}" class="text-danger">
                    <strong>Hết hàng</strong>
                </p>
                
                <!-- Add to cart button -->
                <form th:if="${book.stock > 0}" 
                      action="/books/add-to-cart" 
                      method="post">
                    <input type="hidden" name="id" th:value="${book.id}">
                    <input type="hidden" name="name" th:value="${book.title}">
                    <input type="hidden" name="price" th:value="${book.price}">
                    <div class="input-group mb-3" style="width: 200px;">
                        <input type="number" name="quantity" class="form-control" value="1" min="1" th:max="${book.stock}">
                        <button class="btn btn-primary" type="submit">Thêm vào giỏ</button>
                    </div>
                </form>
            </div>
        </div>
        
        <hr class="my-5">
        
        <!-- Reviews Section -->
        <div class="row">
            <div class="col-md-12">
                <h3>Đánh giá của khách hàng</h3>
                
                <!-- Alert messages -->
                <div th:if="${success}" class="alert alert-success" th:text="${success}"></div>
                <div th:if="${error}" class="alert alert-danger" th:text="${error}"></div>
                
                <!-- Add review form (only if user can review) -->
                <div sec:authorize="isAuthenticated()" th:if="${canReview}" class="card mb-4">
                    <div class="card-body">
                        <h5>Viết đánh giá của bạn</h5>
                        <form action="/reviews/add" method="post">
                            <input type="hidden" name="bookId" th:value="${book.id}">
                            
                            <div class="mb-3">
                                <label>Đánh giá:</label>
                                <div class="star-input">
                                    <input type="radio" name="rating" value="5" id="star5" required>
                                    <label for="star5">★</label>
                                    <input type="radio" name="rating" value="4" id="star4">
                                    <label for="star4">★</label>
                                    <input type="radio" name="rating" value="3" id="star3">
                                    <label for="star3">★</label>
                                    <input type="radio" name="rating" value="2" id="star2">
                                    <label for="star2">★</label>
                                    <input type="radio" name="rating" value="1" id="star1">
                                    <label for="star1">★</label>
                                </div>
                            </div>
                            
                            <div class="mb-3">
                                <label>Nhận xét:</label>
                                <textarea name="comment" class="form-control" rows="4" maxlength="1000" required></textarea>
                            </div>
                            
                            <button type="submit" class="btn btn-primary">Gửi đánh giá</button>
                        </form>
                    </div>
                </div>
                
                <!-- Message if user can't review -->
                <div sec:authorize="isAuthenticated()" th:unless="${canReview}" class="alert alert-info">
                    Bạn cần mua sách này để có thể đánh giá.
                </div>
                
                <!-- List of reviews -->
                <div th:if="${#lists.isEmpty(reviews.content)}" class="alert alert-secondary">
                    Chưa có đánh giá nào cho sách này.
                </div>
                
                <div th:each="review : ${reviews.content}" class="card mb-3">
                    <div class="card-body">
                        <div class="d-flex justify-content-between">
                            <div>
                                <strong th:text="${review.user.username}"></strong>
                                <span class="star-rating small ms-2">
                                    <span th:each="i : ${#numbers.sequence(1, 5)}" 
                                          th:classappend="${i <= review.rating} ? 'filled' : ''">
                                        ★
                                    </span>
                                </span>
                            </div>
                            <div>
                                <small class="text-muted" th:text="${#dates.format(review.reviewDate, 'dd/MM/yyyy')}"></small>
                                <!-- Delete button for owner or admin -->
                                <a sec:authorize="isAuthenticated()" 
                                   th:if="${#authentication.principal.id == review.user.id} or ${#authorization.expression('hasRole(''ADMIN'')')}"
                                   th:href="@{/reviews/delete/{id}(id=${review.id})}"
                                   class="btn btn-sm btn-danger ms-2"
                                   onclick="return confirm('Xác nhận xóa?')">
                                    Xóa
                                </a>
                            </div>
                        </div>
                        <p class="mt-2 mb-0" th:text="${review.comment}"></p>
                    </div>
                </div>
                
                <!-- Pagination -->
                <nav th:if="${reviews.totalPages > 1}">
                    <ul class="pagination">
                        <li class="page-item" th:classappend="${reviews.first} ? 'disabled'">
                            <a class="page-link" th:href="@{/books/{id}(id=${book.id}, page=${reviews.number - 1})}">Trước</a>
                        </li>
                        <li class="page-item" 
                            th:each="i : ${#numbers.sequence(0, reviews.totalPages - 1)}"
                            th:classappend="${i == reviews.number} ? 'active'">
                            <a class="page-link" th:href="@{/books/{id}(id=${book.id}, page=${i})}" th:text="${i + 1}"></a>
                        </li>
                        <li class="page-item" th:classappend="${reviews.last} ? 'disabled'">
                            <a class="page-link" th:href="@{/books/{id}(id=${book.id}, page=${reviews.number + 1})}">Sau</a>
                        </li>
                    </ul>
                </nav>
            </div>
        </div>
    </div>
    
    <style>
    .star-rating .filled { color: gold; }
    .star-rating { color: #ddd; font-size: 1.5em; }
    
    .star-input { display: flex; flex-direction: row-reverse; justify-content: flex-end; }
    .star-input input { display: none; }
    .star-input label { font-size: 2em; color: #ddd; cursor: pointer; }
    .star-input input:checked ~ label { color: gold; }
    .star-input label:hover,
    .star-input label:hover ~ label { color: gold; }
    </style>
</body>
</html>
```

**B. Cập nhật BookController**
```java
// File: src/main/java/nhom2/QLS/controllers/BookController.java
// Thêm endpoint:

@GetMapping("/{id}")
public String bookDetail(
    @PathVariable Long id,
    @RequestParam(defaultValue = "0") int page,
    @AuthenticationPrincipal User user,
    Model model
) {
    Book book = bookService.getBookById(id)
        .orElseThrow(() -> new IllegalArgumentException("Book not found"));
    
    Page<Review> reviews = reviewService.getReviewsByBook(id, page, 5);
    
    boolean canReview = false;
    if (user != null) {
        canReview = reviewService.canUserReview(user.getId(), id);
    }
    
    model.addAttribute("book", book);
    model.addAttribute("reviews", reviews);
    model.addAttribute("canReview", canReview);
    
    return "book/detail";
}
```

**C. Cập nhật book/list.html**
```html
<!-- File: src/main/resources/templates/book/list.html -->
<!-- Thêm link đến trang detail:

<div class="card">
    <div class="card-body">
        <!-- Tiêu đề clickable -->
        <h5>
            <a th:href="@{/books/{id}(id=${book.id})}" 
               th:text="${book.title}"
               class="text-decoration-none"></a>
        </h5>
        
        <!-- Rating summary -->
        <div class="star-rating small">
            <span th:each="i : ${#numbers.sequence(1, 5)}" 
                  th:classappend="${i <= book.averageRating} ? 'filled' : ''">
                ★
            </span>
            <span th:text="'(' + ${book.totalReviews} + ')'"></span>
        </div>
        
        <!-- ... rest of card ... -->
    </div>
</div>
-->
```

### ✅ Checklist Implementation

- [ ] Tạo Review entity
- [ ] Cập nhật Book entity với reviews relationship
- [ ] Tạo IReviewRepository
- [ ] Tạo ReviewService
- [ ] Cập nhật IInvoiceRepository với check purchased method
- [ ] Tạo ReviewController
- [ ] Tạo book/detail.html
- [ ] Cập nhật BookController với endpoint detail
- [ ] Cập nhật book/list.html với link và rating
- [ ] Test add review cho sách đã mua
- [ ] Test không cho review sách chưa mua
- [ ] Test không cho review 2 lần
- [ ] Test delete review (owner & admin)
- [ ] Test pagination reviews

---

## ❤️ CHỨC NĂNG 5: WISHLIST (DANH SÁCH YÊU THÍCH)

### 🎯 Mục tiêu
Cho phép user lưu sách yêu thích để mua sau, tăng tính tiện lợi và engagement.

### 📋 Yêu cầu chi tiết

#### 5.1. Database Design

**A. Tạo Entity Wishlist**
```java
// File: src/main/java/nhom2/QLS/entities/Wishlist.java

@Entity
@Table(name = "wishlist", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "book_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", referencedColumnName = "id", nullable = false)
    private Book book;
    
    @Column(name = "added_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date addedDate = new Date();
}
```

#### 5.2. Backend Implementation

**A. Tạo IWishlistRepository**
```java
// File: src/main/java/nhom2/QLS/repositories/IWishlistRepository.java

public interface IWishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserIdOrderByAddedDateDesc(Long userId);
    Optional<Wishlist> findByUserIdAndBookId(Long userId, Long bookId);
    boolean existsByUserIdAndBookId(Long userId, Long bookId);
    void deleteByUserIdAndBookId(Long userId, Long bookId);
    
    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
```

**B. Tạo WishlistService**
```java
// File: src/main/java/nhom2/QLS/services/WishlistService.java

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {
    private final IWishlistRepository wishlistRepository;
    private final IBookRepository bookRepository;
    
    public Wishlist addToWishlist(Long userId, Long bookId) {
        // Check if already in wishlist
        if (wishlistRepository.existsByUserIdAndBookId(userId, bookId)) {
            throw new IllegalStateException("Sách đã có trong danh sách yêu thích");
        }
        
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new IllegalArgumentException("Book not found"));
        
        Wishlist wishlist = Wishlist.builder()
            .user(User.builder().id(userId).build())
            .book(book)
            .build();
        
        return wishlistRepository.save(wishlist);
    }
    
    public void removeFromWishlist(Long userId, Long bookId) {
        wishlistRepository.deleteByUserIdAndBookId(userId, bookId);
    }
    
    public List<Wishlist> getWishlistByUser(Long userId) {
        return wishlistRepository.findByUserIdOrderByAddedDateDesc(userId);
    }
    
    public boolean isInWishlist(Long userId, Long bookId) {
        return wishlistRepository.existsByUserIdAndBookId(userId, bookId);
    }
    
    public long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }
}
```

**C. Tạo WishlistController**
```java
// File: src/main/java/nhom2/QLS/controllers/WishlistController.java

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistService wishlistService;
    
    @GetMapping
    public String showWishlist(@AuthenticationPrincipal User user, Model model) {
        List<Wishlist> wishlist = wishlistService.getWishlistByUser(user.getId());
        model.addAttribute("wishlist", wishlist);
        return "user/wishlist";
    }
    
    @PostMapping("/add/{bookId}")
    @ResponseBody
    public ResponseEntity<?> addToWishlist(
        @PathVariable Long bookId,
        @AuthenticationPrincipal User user
    ) {
        try {
            wishlistService.addToWishlist(user.getId(), bookId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã thêm vào danh sách yêu thích",
                "count", wishlistService.getWishlistCount(user.getId())
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @DeleteMapping("/remove/{bookId}")
    @ResponseBody
    public ResponseEntity<?> removeFromWishlist(
        @PathVariable Long bookId,
        @AuthenticationPrincipal User user
    ) {
        wishlistService.removeFromWishlist(user.getId(), bookId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Đã xóa khỏi danh sách yêu thích",
            "count", wishlistService.getWishlistCount(user.getId())
        ));
    }
    
    @GetMapping("/check/{bookId}")
    @ResponseBody
    public ResponseEntity<?> checkWishlist(
        @PathVariable Long bookId,
        @AuthenticationPrincipal User user
    ) {
        boolean inWishlist = wishlistService.isInWishlist(user.getId(), bookId);
        return ResponseEntity.ok(Map.of("inWishlist", inWishlist));
    }
}
```

#### 5.3. Frontend Implementation

**A. Thêm Wishlist icon vào layout**
```html
<!-- File: src/main/resources/templates/layout.html -->
<!-- Trong navbar, thêm:

<ul class="navbar-nav" sec:authorize="isAuthenticated()">
    <li class="nav-item">
        <a class="nav-link" href="/wishlist">
            ❤️ Yêu thích 
            <span id="wishlistCount" class="badge bg-danger">0</span>
        </a>
    </li>
</ul>
-->
```

**B. Cập nhật book/list.html**
```html
<!-- File: src/main/resources/templates/book/list.html -->
<!-- Trong mỗi card sách, thêm icon trái tim:

<div class="card">
    <div class="card-body position-relative">
        <!-- Heart icon -->
        <button sec:authorize="isAuthenticated()"
                class="btn btn-link position-absolute top-0 end-0 wishlist-btn"
                th:data-book-id="${book.id}"
                onclick="toggleWishlist(this)">
            <span class="heart-icon">♡</span>
        </button>
        
        <!-- ... rest of card ... -->
    </div>
</div>
-->
```

**C. Tạo trang Wishlist**
```html
<!-- File: src/main/resources/templates/user/wishlist.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Danh sách yêu thích</title>
</head>
<body>
    <div class="container mt-4">
        <h2>Danh sách yêu thích của bạn</h2>
        
        <div th:if="${#lists.isEmpty(wishlist)}" class="alert alert-info mt-4">
            <p>Bạn chưa có sách nào trong danh sách yêu thích.</p>
            <a href="/books" class="btn btn-primary">Khám phá sách</a>
        </div>
        
        <div class="row mt-4">
            <div class="col-md-3" th:each="item : ${wishlist}">
                <div class="card mb-4">
                    <div class="card-body">
                        <button class="btn btn-link position-absolute top-0 end-0 text-danger"
                                th:data-book-id="${item.book.id}"
                                onclick="removeFromWishlist(this)">
                            ❌
                        </button>
                        
                        <h5>
                            <a th:href="@{/books/{id}(id=${item.book.id})}" 
                               th:text="${item.book.title}"></a>
                        </h5>
                        <p class="text-muted" th:text="${item.book.author}"></p>
                        <h6 class="text-danger" th:text="${item.book.price} + ' đ'"></h6>
                        
                        <!-- Rating -->
                        <div class="star-rating small mb-2">
                            <span th:each="i : ${#numbers.sequence(1, 5)}" 
                                  th:classappend="${i <= item.book.averageRating} ? 'filled' : ''">
                                ★
                            </span>
                        </div>
                        
                        <!-- Stock -->
                        <p th:if="${item.book.stock > 0}" class="small">
                            Còn <span th:text="${item.book.stock}"></span> quyển
                        </p>
                        <p th:if="${item.book.stock == 0}" class="text-danger small">
                            Hết hàng
                        </p>
                        
                        <!-- Actions -->
                        <form th:if="${item.book.stock > 0}" 
                              action="/books/add-to-cart" 
                              method="post">
                            <input type="hidden" name="id" th:value="${item.book.id}">
                            <input type="hidden" name="name" th:value="${item.book.title}">
                            <input type="hidden" name="price" th:value="${item.book.price}">
                            <input type="hidden" name="quantity" value="1">
                            <button type="submit" class="btn btn-primary btn-sm w-100">
                                🛒 Thêm vào giỏ
                            </button>
                        </form>
                        
                        <button th:if="${item.book.stock == 0}" 
                                class="btn btn-secondary btn-sm w-100" 
                                disabled>
                            Hết hàng
                        </button>
                        
                        <small class="text-muted d-block mt-2">
                            Đã thêm: <span th:text="${#dates.format(item.addedDate, 'dd/MM/yyyy')}"></span>
                        </small>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <script>
    function removeFromWishlist(btn) {
        const bookId = btn.getAttribute('data-book-id');
        
        if (!confirm('Xóa khỏi danh sách yêu thích?')) return;
        
        fetch(`/wishlist/remove/${bookId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            }
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                location.reload();
            } else {
                alert(data.message);
            }
        });
    }
    </script>
</body>
</html>
```

**D. Thêm JavaScript cho Wishlist**
```javascript
// File: src/main/resources/static/js/wishlist.js

function toggleWishlist(btn) {
    const bookId = btn.getAttribute('data-book-id');
    const heartIcon = btn.querySelector('.heart-icon');
    const isFilled = heartIcon.textContent === '♥';
    
    const url = isFilled ? `/wishlist/remove/${bookId}` : `/wishlist/add/${bookId}`;
    const method = isFilled ? 'DELETE' : 'POST';
    
    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // Toggle heart
            heartIcon.textContent = isFilled ? '♡' : '♥';
            heartIcon.style.color = isFilled ? 'black' : 'red';
            
            // Update count
            document.getElementById('wishlistCount').textContent = data.count;
            
            // Show toast notification
            showToast(data.message);
        } else {
            alert(data.message);
        }
    });
}

function showToast(message) {
    // Simple toast notification
    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #28a745;
        color: white;
        padding: 15px 25px;
        border-radius: 5px;
        z-index: 9999;
        box-shadow: 0 2px 5px rgba(0,0,0,0.3);
    `;
    document.body.appendChild(toast);
    
    setTimeout(() => toast.remove(), 3000);
}

// On page load, check wishlist status for all books
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.wishlist-btn').forEach(btn => {
        const bookId = btn.getAttribute('data-book-id');
        
        fetch(`/wishlist/check/${bookId}`)
            .then(response => response.json())
            .then(data => {
                const heartIcon = btn.querySelector('.heart-icon');
                if (data.inWishlist) {
                    heartIcon.textContent = '♥';
                    heartIcon.style.color = 'red';
                }
            });
    });
});
```

**E. Include wishlist.js trong layout**
```html
<!-- File: src/main/resources/templates/layout.html -->
<!-- Trong <head> hoặc cuối <body>:
<script th:src="@{/js/wishlist.js}"></script>
-->
```

### ✅ Checklist Implementation

- [ ] Tạo Wishlist entity
- [ ] Tạo IWishlistRepository
- [ ] Tạo WishlistService
- [ ] Tạo WishlistController với REST APIs
- [ ] Tạo user/wishlist.html
- [ ] Thêm wishlist icon vào navbar
- [ ] Thêm heart button vào book cards
- [ ] Tạo wishlist.js
- [ ] Include wishlist.js vào layout
- [ ] Test add to wishlist
- [ ] Test remove from wishlist
- [ ] Test wishlist page
- [ ] Test add to cart from wishlist
- [ ] Test wishlist count update

---

### GIAI ĐOẠN 3: ADMIN DASHBOARD & ANALYTICS (Ưu tiên cao cho quản lý ⭐⭐⭐)

---

## 📊 CHỨC NĂNG 6: ADMIN DASHBOARD & THỐNG KÊ

### 🎯 Mục tiêu
Tạo trang quản trị tổng quan với biểu đồ, thống kê doanh thu, sách bán chạy, quản lý users.

### 📋 Yêu cầu chi tiết

#### 6.1. Backend Implementation

**A. Tạo StatisticsService**
```java
// File: src/main/java/nhom2/QLS/services/StatisticsService.java

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final IInvoiceRepository invoiceRepository;
    private final IBookRepository bookRepository;
    private final IUserRepository userRepository;
    private final IItemInvoiceRepository itemInvoiceRepository;
    
    public Double getTotalRevenue() {
        return invoiceRepository.sumTotalPriceByStatus("COMPLETED");
    }
    
    public Long getTotalCompletedOrders() {
        return invoiceRepository.countByStatus("COMPLETED");
    }
    
    public Long getTotalUsers() {
        return userRepository.count();
    }
    
    public Long getTotalBooks() {
        return bookRepository.count();
    }
    
    public List<Map<String, Object>> getBestSellingBooks(int limit) {
        // Return: [{bookTitle, totalSold, revenue}, ...]
        return itemInvoiceRepository.findBestSellingBooks(PageRequest.of(0, limit));
    }
    
    public Map<String, Double> getRevenueByMonth(int year) {
        // Return: {"01": 1000000, "02": 1500000, ...}
        Map<String, Double> result = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            String monthKey = String.format("%02d", month);
            Double revenue = invoiceRepository.sumRevenueByYearAndMonth(year, month);
            result.put(monthKey, revenue != null ? revenue : 0.0);
        }
        return result;
    }
    
    public List<Map<String, Object>> getRecentOrders(int limit) {
        return invoiceRepository.findRecentOrders(PageRequest.of(0, limit));
    }
    
    public Map<String, Long> getOrderStatusCount() {
        Map<String, Long> result = new HashMap<>();
        result.put("PENDING", invoiceRepository.countByStatus("PENDING"));
        result.put("PROCESSING", invoiceRepository.countByStatus("PROCESSING"));
        result.put("COMPLETED", invoiceRepository.countByStatus("COMPLETED"));
        result.put("CANCELLED", invoiceRepository.countByStatus("CANCELLED"));
        return result;
    }
}
```

**B. Cập nhật IInvoiceRepository**
```java
// File: src/main/java/nhom2/QLS/repositories/IInvoiceRepository.java
// Thêm methods:

@Query("SELECT SUM(i.price) FROM Invoice i WHERE i.status = :status")
Double sumTotalPriceByStatus(@Param("status") String status);

Long countByStatus(String status);

@Query("SELECT SUM(i.price) FROM Invoice i WHERE YEAR(i.invoiceDate) = :year " +
       "AND MONTH(i.invoiceDate) = :month AND i.status = 'COMPLETED'")
Double sumRevenueByYearAndMonth(@Param("year") int year, @Param("month") int month);

@Query("SELECT new map(i.id as id, i.invoiceDate as date, i.price as total, " +
       "i.status as status, u.username as username) " +
       "FROM Invoice i JOIN i.user u ORDER BY i.invoiceDate DESC")
List<Map<String, Object>> findRecentOrders(Pageable pageable);
```

**C. Cập nhật IItemInvoiceRepository**
```java
// File: src/main/java/nhom2/QLS/repositories/IItemInvoiceRepository.java
// Thêm method:

@Query("SELECT new map(b.title as bookTitle, SUM(ii.quantity) as totalSold, " +
       "SUM(ii.quantity * b.price) as revenue) " +
       "FROM ItemInvoice ii JOIN ii.book b JOIN ii.invoice i " +
       "WHERE i.status = 'COMPLETED' " +
       "GROUP BY b.id, b.title " +
       "ORDER BY SUM(ii.quantity) DESC")
List<Map<String, Object>> findBestSellingBooks(Pageable pageable);
```

**D. Tạo AdminDashboardController**
```java
// File: src/main/java/nhom2/QLS/controllers/admin/AdminDashboardController.java

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {
    private final StatisticsService statisticsService;
    
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        // Overview stats
        model.addAttribute("totalRevenue", statisticsService.getTotalRevenue());
        model.addAttribute("totalOrders", statisticsService.getTotalCompletedOrders());
        model.addAttribute("totalUsers", statisticsService.getTotalUsers());
        model.addAttribute("totalBooks", statisticsService.getTotalBooks());
        
        // Best selling books
        model.addAttribute("bestSellingBooks", statisticsService.getBestSellingBooks(5));
        
        // Revenue by month (current year)
        int currentYear = LocalDate.now().getYear();
        model.addAttribute("revenueByMonth", statisticsService.getRevenueByMonth(currentYear));
        model.addAttribute("currentYear", currentYear);
        
        // Recent orders
        model.addAttribute("recentOrders", statisticsService.getRecentOrders(10));
        
        // Order status count
        model.addAttribute("orderStatusCount", statisticsService.getOrderStatusCount());
        
        return "admin/dashboard";
    }
}
```

**E. Tạo AdminUserController**
```java
// File: src/main/java/nhom2/QLS/controllers/admin/AdminUserController.java

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserService userService;
    
    @GetMapping
    public String listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Model model
    ) {
        Page<User> users = userService.getAllUsers(page, size);
        model.addAttribute("users", users);
        return "admin/users";
    }
    
    @PostMapping("/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id) {
        // Toggle enabled/disabled (future implementation)
        return "redirect:/admin/users";
    }
    
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }
}
```

**F. Cập nhật UserService**
```java
// File: src/main/java/nhom2/QLS/services/UserService.java
// Thêm method:

public Page<User> getAllUsers(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
    return userRepository.findAll(pageable);
}

public void deleteUser(Long id) {
    userRepository.deleteById(id);
}
```

#### 6.2. Frontend Implementation

**A. Tạo admin/dashboard.html**
```html
<!-- File: src/main/resources/templates/admin/dashboard.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Admin Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.js"></script>
</head>
<body>
    <div class="container-fluid mt-4">
        <h2>Dashboard Quản trị</h2>
        
        <!-- Overview Cards -->
        <div class="row mt-4">
            <div class="col-md-3">
                <div class="card text-white bg-primary">
                    <div class="card-body">
                        <h5 class="card-title">Tổng doanh thu</h5>
                        <h3 th:text="${#numbers.formatDecimal(totalRevenue, 0, 'COMMA', 0, 'POINT')} + ' đ'"></h3>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card text-white bg-success">
                    <div class="card-body">
                        <h5 class="card-title">Đơn hàng hoàn thành</h5>
                        <h3 th:text="${totalOrders}"></h3>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card text-white bg-info">
                    <div class="card-body">
                        <h5 class="card-title">Tổng người dùng</h5>
                        <h3 th:text="${totalUsers}"></h3>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card text-white bg-warning">
                    <div class="card-body">
                        <h5 class="card-title">Tổng sách</h5>
                        <h3 th:text="${totalBooks}"></h3>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Charts Row -->
        <div class="row mt-4">
            <!-- Revenue Chart -->
            <div class="col-md-8">
                <div class="card">
                    <div class="card-header">
                        <h5>Doanh thu theo tháng (<span th:text="${currentYear}"></span>)</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="revenueChart"></canvas>
                    </div>
                </div>
            </div>
            
            <!-- Order Status Pie Chart -->
            <div class="col-md-4">
                <div class="card">
                    <div class="card-header">
                        <h5>Trạng thái đơn hàng</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="statusChart"></canvas>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Best Selling Books & Recent Orders -->
        <div class="row mt-4">
            <!-- Best Selling Books -->
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header">
                        <h5>Top 5 sách bán chạy</h5>
                    </div>
                    <div class="card-body">
                        <table class="table table-sm">
                            <thead>
                                <tr>
                                    <th>Sách</th>
                                    <th>Đã bán</th>
                                    <th>Doanh thu</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr th:each="book : ${bestSellingBooks}">
                                    <td th:text="${book.bookTitle}"></td>
                                    <td th:text="${book.totalSold}"></td>
                                    <td th:text="${#numbers.formatDecimal(book.revenue, 0, 'COMMA', 0, 'POINT')} + ' đ'"></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            
            <!-- Recent Orders -->
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header">
                        <h5>Đơn hàng gần đây</h5>
                    </div>
                    <div class="card-body">
                        <table class="table table-sm">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>User</th>
                                    <th>Ngày</th>
                                    <th>Tổng</th>
                                    <th>Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr th:each="order : ${recentOrders}">
                                    <td th:text="${order.id}"></td>
                                    <td th:text="${order.username}"></td>
                                    <td th:text="${#dates.format(order.date, 'dd/MM/yyyy')}"></td>
                                    <td th:text="${#numbers.formatDecimal(order.total, 0, 'COMMA', 0, 'POINT')} + ' đ'"></td>
                                    <td>
                                        <span th:text="${order.status}" 
                                              th:classappend="${order.status == 'COMPLETED'} ? 'badge bg-success' : 
                                                             ${order.status == 'PENDING'} ? 'badge bg-warning' :
                                                             ${order.status == 'PROCESSING'} ? 'badge bg-info' : 'badge bg-danger'">
                                        </span>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <script th:inline="javascript">
    // Revenue Chart
    const revenueData = /*[[${revenueByMonth}]]*/ {};
    const months = Object.keys(revenueData);
    const revenues = Object.values(revenueData);
    
    new Chart(document.getElementById('revenueChart'), {
        type: 'line',
        data: {
            labels: months.map(m => 'Tháng ' + m),
            datasets: [{
                label: 'Doanh thu (đ)',
                data: revenues,
                borderColor: 'rgb(75, 192, 192)',
                backgroundColor: 'rgba(75, 192, 192, 0.2)',
                tension: 0.1
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: { display: true },
                title: { display: false }
            }
        }
    });
    
    // Status Chart
    const statusData = /*[[${orderStatusCount}]]*/ {};
    
    new Chart(document.getElementById('statusChart'), {
        type: 'doughnut',
        data: {
            labels: ['Chờ xử lý', 'Đang xử lý', 'Hoàn thành', 'Đã hủy'],
            datasets: [{
                data: [
                    statusData.PENDING || 0,
                    statusData.PROCESSING || 0,
                    statusData.COMPLETED || 0,
                    statusData.CANCELLED || 0
                ],
                backgroundColor: [
                    'rgb(255, 205, 86)',
                    'rgb(54, 162, 235)',
                    'rgb(75, 192, 192)',
                    'rgb(255, 99, 132)'
                ]
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: { position: 'bottom' }
            }
        }
    });
    </script>
</body>
</html>
```

**B. Tạo admin/users.html**
```html
<!-- File: src/main/resources/templates/admin/users.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Quản lý Users</title>
</head>
<body>
    <div class="container mt-4">
        <h2>Quản lý người dùng</h2>
        
        <table class="table table-striped mt-4">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Provider</th>
                    <th>Roles</th>
                    <th>Thao tác</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="user : ${users.content}">
                    <td th:text="${user.id}"></td>
                    <td th:text="${user.username}"></td>
                    <td th:text="${user.email}"></td>
                    <td th:text="${user.phone}"></td>
                    <td th:text="${user.provider != null ? user.provider : 'LOCAL'}"></td>
                    <td>
                        <span th:each="role : ${user.roles}" 
                              class="badge bg-primary me-1" 
                              th:text="${role.name}"></span>
                    </td>
                    <td>
                        <form th:action="@{/admin/users/{id}/delete(id=${user.id})}" 
                              method="post" 
                              style="display: inline;"
                              onsubmit="return confirm('Xác nhận xóa user này?')">
                            <button type="submit" class="btn btn-sm btn-danger">Xóa</button>
                        </form>
                    </td>
                </tr>
            </tbody>
        </table>
        
        <!-- Pagination -->
        <nav th:if="${users.totalPages > 1}">
            <ul class="pagination">
                <li class="page-item" th:classappend="${users.first} ? 'disabled'">
                    <a class="page-link" th:href="@{/admin/users(page=${users.number - 1})}">Trước</a>
                </li>
                <li class="page-item" 
                    th:each="i : ${#numbers.sequence(0, users.totalPages - 1)}"
                    th:classappend="${i == users.number} ? 'active'">
                    <a class="page-link" th:href="@{/admin/users(page=${i})}" th:text="${i + 1}"></a>
                </li>
                <li class="page-item" th:classappend="${users.last} ? 'disabled'">
                    <a class="page-link" th:href="@{/admin/users(page=${users.number + 1})}">Sau</a>
                </li>
            </ul>
        </nav>
    </div>
</body>
</html>
```

**C. Cập nhật layout.html với Admin menu**
```html
<!-- File: src/main/resources/templates/layout.html -->
<!-- Thêm dropdown menu cho admin:

<ul class="navbar-nav" sec:authorize="hasRole('ADMIN')">
    <li class="nav-item dropdown">
        <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown">
            ⚙️ Quản trị
        </a>
        <ul class="dropdown-menu">
            <li><a class="dropdown-item" href="/admin/dashboard">Dashboard</a></li>
            <li><a class="dropdown-item" href="/admin/orders">Quản lý đơn hàng</a></li>
            <li><a class="dropdown-item" href="/admin/inventory">Quản lý kho</a></li>
            <li><a class="dropdown-item" href="/admin/users">Quản lý users</a></li>
            <li><a class="dropdown-item" href="/books">Quản lý sách</a></li>
        </ul>
    </li>
</ul>
-->
```

### ✅ Checklist Implementation

- [ ] Tạo StatisticsService với đầy đủ methods
- [ ] Cập nhật IInvoiceRepository với query methods
- [ ] Cập nhật IItemInvoiceRepository với best selling query
- [ ] Tạo AdminDashboardController
- [ ] Tạo AdminUserController
- [ ] Cập nhật UserService với admin methods
- [ ] Tạo admin/dashboard.html với charts
- [ ] Tạo admin/users.html
- [ ] Thêm admin menu vào layout
- [ ] Include Chart.js library
- [ ] Test dashboard load data
- [ ] Test revenue chart hiển thị đúng
- [ ] Test order status pie chart
- [ ] Test best selling books table
- [ ] Test user management

---

## 📅 PHẦN III: LỘ TRÌNH THỰC HIỆN

### Sprint 1 (2 tuần): Core E-commerce Features
**Mục tiêu**: Hoàn thiện quy trình mua hàng từ đầu đến cuối

- [ ] Week 1: Quản lý hóa đơn & Checkout
  - Cập nhật Invoice entity
  - Implement InvoiceService và Controllers
  - Tạo các trang HTML (checkout, order history, order detail)
  - Test end-to-end checkout flow
  
- [ ] Week 2: Quản lý kho
  - Thêm stock fields vào Book
  - Implement inventory management
  - Tạo admin inventory page
  - Integrate stock với checkout process

**Deliverables**:
- User có thể checkout và xem lịch sử đơn hàng
- Admin có thể quản lý kho
- Stock tự động giảm khi bán

---

### Sprint 2 (2 tuần): Search & Discovery
**Mục tiêu**: Nâng cao trải nghiệm tìm kiếm và khám phá sách

- [ ] Week 1: Search & Filter
  - Tạo SearchDTO
  - Implement advanced search queries
  - Tạo filter sidebar
  - Test tất cả filter combinations
  
- [ ] Week 2: Book Detail Page
  - Tạo trang chi tiết sách
  - Add related books section
  - Optimize book listing với rating
  - Improve UX

**Deliverables**:
- Filter hoạt động mượt mà với nhiều tiêu chí
- Trang detail đẹp và đầy đủ thông tin
- User dễ dàng tìm sách mong muốn

---

### Sprint 3 (2 tuần): Social Features
**Mục tiêu**: Tăng engagement với review và wishlist

- [ ] Week 1: Review System
  - Tạo Review entity và service
  - Implement review trong book detail
  - Rating validation (chỉ user đã mua)
  - Star rating UI
  
- [ ] Week 2: Wishlist
  - Tạo Wishlist entity
  - Implement wishlist với AJAX
  - Tạo wishlist page
  - Heart icon animation

**Deliverables**:
- User có thể review sách đã mua
- Wishlist hoạt động mượt với AJAX
- Tăng độ tin cậy với review system

---

### Sprint 4 (2 tuần): Admin & Analytics
**Mục tiêu**: Dashboard quản trị hoàn chỉnh với thống kê

- [ ] Week 1: Statistics Service & Backend
  - Implement StatisticsService
  - Tạo query methods cho reports
  - AdminDashboardController
  - AdminUserController
  
- [ ] Week 2: Admin UI & Charts
  - Dashboard với Chart.js
  - User management page
  - Admin menu integration
  - Polish admin UI

**Deliverables**:
- Dashboard đẹp với charts trực quan
- Admin quản lý được users và orders
- Thống kê doanh thu chính xác

---

### Sprint 5 (1-2 tuần): Extra Features (Optional)
**Mục tiêu**: Các tính năng bổ sung nếu còn thời gian

- [ ] Coupon/Promotion system
- [ ] Notifications
- [ ] Publisher management
- [ ] Export reports (Excel/PDF)
- [ ] Recent viewed books
- [ ] Recommended books

---

## 🛠️ PHẦN IV: CÔNG NGHỆ VÀ TOOLS BỔ SUNG

### Dependencies cần thêm vào pom.xml

```xml
<!-- Chart library support (nếu cần backend charts) -->
<dependency>
    <groupId>org.jfree</groupId>
    <artifactId>jfreechart</artifactId>
    <version>1.5.4</version>
</dependency>

<!-- Apache POI for Excel export -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>

<!-- iText for PDF export -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>7.2.5</version>
    <type>pom</type>
</dependency>

<!-- Redis for caching (optional) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- WebSocket for real-time notifications (optional) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### Frontend Libraries

```html
<!-- Chart.js for charts -->
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.js"></script>

<!-- SweetAlert2 for beautiful alerts -->
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>

<!-- DataTables for admin tables -->
<link rel="stylesheet" href="https://cdn.datatables.net/1.13.7/css/dataTables.bootstrap5.min.css">
<script src="https://cdn.datatables.net/1.13.7/js/jquery.dataTables.min.js"></script>
<script src="https://cdn.datatables.net/1.13.7/js/dataTables.bootstrap5.min.js"></script>
```

---

## ✅ PHẦN V: CHECKLIST TỔNG THỂ

### Giai đoạn 1: Core Features ⭐⭐⭐
- [ ] Quản lý hóa đơn hoàn chỉnh
- [ ] Lịch sử mua hàng
- [ ] Quản lý kho (inventory)
- [ ] Tìm kiếm và filter nâng cao

### Giai đoạn 2: Advanced Features ⭐⭐
- [ ] Hệ thống đánh giá (review)
- [ ] Wishlist (danh sách yêu thích)
- [ ] Trang chi tiết sách

### Giai đoạn 3: Admin & Analytics ⭐⭐⭐
- [ ] Admin dashboard với charts
- [ ] Báo cáo thống kê doanh thu
- [ ] Quản lý users
- [ ] Top sách bán chạy

### Giai đoạn 4: Extra Features ⭐
- [ ] Coupon/Promotion
- [ ] Notifications
- [ ] Publisher management
- [ ] Export Excel/PDF
- [ ] Recent viewed
- [ ] Recommended books

---

## 📝 PHẦN VI: GHI CHÚ VÀ BEST PRACTICES

### Security Considerations
1. **CSRF Protection**: Đảm bảo tất cả forms có CSRF token
2. **Authorization**: Check quyền trước khi thực hiện actions nhạy cảm
3. **Input Validation**: Validate tất cả input từ user
4. **SQL Injection**: Sử dụng Parameterized queries (JPA đã handle)
5. **XSS Prevention**: Thymeleaf tự động escape HTML

### Performance Optimization
1. **Lazy Loading**: Sử dụng FetchType.LAZY cho relationships
2. **Pagination**: Luôn phân trang cho danh sách lớn
3. **Caching**: Cache static data (categories, roles)
4. **Indexes**: Thêm indexes cho columns thường query
5. **N+1 Query**: Sử dụng JOIN FETCH khi cần

### Code Quality
1. **Service Layer**: Business logic phải ở Service, không ở Controller
2. **DTO Pattern**: Sử dụng DTO cho complex data transfer
3. **Exception Handling**: Centralized exception handling
4. **Logging**: Log important actions và errors
5. **Unit Tests**: Viết tests cho business logic

### UI/UX Improvements
1. **Loading Indicators**: Hiển thị spinner khi AJAX loading
2. **Toast Notifications**: Feedback cho user actions
3. **Confirmation Dialogs**: Confirm trước khi delete
4. **Form Validation**: Client-side validation trước khi submit
5. **Responsive Design**: Test trên mobile

---

## 📞 PHẦN VII: HỖ TRỢ VÀ TÀI LIỆU

### Documentation
- Spring Boot Docs: https://spring.io/projects/spring-boot
- Thymeleaf Docs: https://www.thymeleaf.org/documentation.html
- Bootstrap 5: https://getbootstrap.com/docs/5.3/
- Chart.js: https://www.chartjs.org/docs/latest/

### Git Workflow
```bash
# Tạo branch cho mỗi feature
git checkout -b feature/invoice-management
git add .
git commit -m "Implement invoice management"
git push origin feature/invoice-management

# Merge vào main sau khi test
git checkout main
git merge feature/invoice-management
git push origin main
```

### Database Backup
```bash
# Backup database trước khi thay đổi lớn
mysqldump -u root -p QLS > backup_$(date +%Y%m%d).sql

# Restore nếu cần
mysql -u root -p QLS < backup_20260205.sql
```

---

## 🎯 KẾT LUẬN

Kế hoạch này bao gồm **6 chức năng chính** được ưu tiên từ cao xuống thấp:

1. ⭐⭐⭐ **Quản lý Hóa đơn** - Core feature, cần thiết nhất
2. ⭐⭐⭐ **Tìm kiếm nâng cao** - Nâng cao UX đáng kể
3. ⭐⭐⭐ **Quản lý Kho** - Quan trọng cho business logic
4. ⭐⭐ **Hệ thống Review** - Tăng engagement
5. ⭐⭐ **Wishlist** - Tiện ích cho user
6. ⭐⭐⭐ **Admin Dashboard** - Cần thiết cho quản lý

**Ước tính thời gian hoàn thành**: 8-10 tuần cho tất cả features core + advanced.

**Khuyến nghị bắt đầu**: Sprint 1 - Quản lý Hóa đơn (quan trọng nhất để hoàn thiện quy trình mua hàng).

---

**Lưu ý**: Document này sẽ được cập nhật theo tiến độ thực tế. Đánh dấu ✅ vào checkbox khi hoàn thành từng task.

**Last updated**: 05/02/2026
