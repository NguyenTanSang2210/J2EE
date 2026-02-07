package nhom2.NguyenTanSang.controllers;

import nhom2.NguyenTanSang.dtos.SearchDTO;
import nhom2.NguyenTanSang.entities.Book;
import nhom2.NguyenTanSang.entities.Review;
import nhom2.NguyenTanSang.entities.User;
import nhom2.NguyenTanSang.daos.Item;
import nhom2.NguyenTanSang.repositories.IUserRepository;
import nhom2.NguyenTanSang.services.BookService;
import nhom2.NguyenTanSang.services.CartService;
import nhom2.NguyenTanSang.services.CategoryService;
import nhom2.NguyenTanSang.services.FileUploadService;
import nhom2.NguyenTanSang.services.ReviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;


@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final CategoryService categoryService;
    private final CartService cartService;
    private final ReviewService reviewService;
    private final IUserRepository userRepository;
    private final FileUploadService fileUploadService;

    @GetMapping
    public String showAllBooks(@NotNull Model model,
                               @RequestParam(defaultValue = "0")
                               Integer pageNo,
                               @RequestParam(defaultValue = "20")
                               Integer pageSize,
                               @RequestParam(defaultValue = "id")
                               String sortBy) {
        List<Book> books = bookService.getAllBooks(pageNo, pageSize, sortBy);
        model.addAttribute("books", books);
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", books.size() / pageSize);
        model.addAttribute("totalItems", (long) books.size());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("searchDTO", new SearchDTO());
        return "book/list";
    }

    /**
     * Hiển thị chi tiết sách với reviews
     */
    @GetMapping("/{id}")
    public String bookDetail(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") int page,
        Principal principal,
        Model model
    ) {
        // Lấy thông tin sách
        Book book = bookService.getBookById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách"));
        
        // Lấy reviews với phân trang
        Page<Review> reviews = reviewService.getReviewsByBook(id, page, 5);
        
        // Kiểm tra user có thể review không
        boolean canReview = false;
        Review userReview = null;
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName())
                .orElse(null);
            if (user != null) {
                canReview = reviewService.canUserReview(user.getId(), id);
                userReview = reviewService.getUserReviewForBook(user.getId(), id);
            }
        }
        
        model.addAttribute("book", book);
        model.addAttribute("reviews", reviews);
        model.addAttribute("canReview", canReview);
        model.addAttribute("userReview", userReview);
        
        return "book/detail";
    }

    @GetMapping("/add")
    public String addBookForm(@NotNull Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories",
                categoryService.getAllCategories());
        return "book/add";
    }

    @PostMapping("/add")
    public String addBook(
            @Valid @ModelAttribute("book") Book book,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @NotNull BindingResult bindingResult,
            Model model) {
        
        // Validation errors từ binding
        if (bindingResult.hasErrors()) {
            var errors = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage).toArray(String[]::new);
            model.addAttribute("errors", errors);
            model.addAttribute("categories", categoryService.getAllCategories());
            return "book/add";
        }
        
        try {
            // LOGIC ƯU TIÊN: Link ảnh ưu tiên - Upload file là fallback
            handleImageUpload(book, imageFile);
            
            // Lưu sách vào database
            bookService.addBook(book);
            return "redirect:/books";
            
        } catch (Exception e) {
            // Nếu có lỗi xử lý ảnh
            model.addAttribute("error", "Lỗi xử lý ảnh: " + e.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            return "book/add";
        }
    }

    @PostMapping("/add-to-cart")
    public String addToCart(HttpSession session,
                            @RequestParam long id,
                            @RequestParam String name,
                            @RequestParam double price,
                            @RequestParam(defaultValue = "1") int quantity,
                            @RequestHeader(value = "Referer", required = false) String referer) {
        var cart = cartService.getCart(session);
        
        // Lấy imageUrl từ database
        String imageUrl = bookService.getBookById(id)
                .map(book -> book.getImageUrl())
                .orElse(null);
        
        Item item = new Item(id, name, price, quantity, imageUrl);
        cart.addItems(item);
        cartService.updateCart(session, cart);
        
        // Redirect về trang trước đó hoặc mặc định về /books
        return "redirect:" + (referer != null ? referer : "/books");
    }

    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable long id) {
        bookService.getBookById(id)
                .ifPresentOrElse(
                        book -> bookService.deleteBookById(id),
                        () -> {
                            throw new IllegalArgumentException("Book not found");
                        });
        return "redirect:/books";
    }
    @GetMapping("/edit/{id}")
    public String editBookForm(@NotNull Model model, @PathVariable long id)
    {
        var book = bookService.getBookById(id);
        model.addAttribute("book", book.orElseThrow(() -> new IllegalArgumentException("Book not found")));
        model.addAttribute("categories",
                categoryService.getAllCategories());
        return "book/edit";
    }

    @PostMapping("/edit")
    public String editBook(
            @Valid @ModelAttribute("book") Book book,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "currentImageUrl", required = false) String currentImageUrl,
            @NotNull BindingResult bindingResult,
            Model model) {
        
        // Validation errors
        if (bindingResult.hasErrors()) {
            var errors = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toArray(String[]::new);
            model.addAttribute("errors", errors);
            model.addAttribute("categories", categoryService.getAllCategories());
            return "book/edit";
        }
        
        try {
            // Xử lý ảnh cho edit: cần xóa ảnh cũ nếu có ảnh mới
            handleImageEditUpload(book, imageFile, currentImageUrl);
            
            // Update sách
            bookService.updateBook(book);
            return "redirect:/books";
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi cập nhật ảnh: " + e.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            return "book/edit";
        }
    }

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

    // ==================== PRIVATE HELPER METHODS ====================
    
    /**
     * Xử lý ảnh cho ADD book - Logic ưu tiên: Link ưu tiên, Upload là fallback
     * 
     * @param book Sách cần xử lý ảnh
     * @param imageFile File upload (có thể null)
     * @throws Exception nếu có lỗi upload
     */
    private void handleImageUpload(Book book, MultipartFile imageFile) throws Exception {
        String imageUrl = book.getImageUrl(); // Lấy từ form input
        
        // Case 1: Có link ảnh → Dùng link, BỎ QUA upload file
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            book.setImageUrl(imageUrl.trim());
            return; // Kết thúc ngay, không upload file
        }
        
        // Case 2: Không có link NHƯNG có file upload → Upload file
        if (imageFile != null && !imageFile.isEmpty()) {
            String uploadedUrl = fileUploadService.uploadBookImage(imageFile);
            book.setImageUrl(uploadedUrl);
            return;
        }
        
        // Case 3: Cả hai đều trống → Set null, getter sẽ trả default
        book.setImageUrl(null);
    }
    
    /**
     * Xử lý ảnh cho EDIT book - Phức tạp hơn vì cần xóa ảnh cũ
     * 
     * @param book Sách cần edit
     * @param imageFile File upload mới (có thể null) 
     * @param currentImageUrl Ảnh hiện tại của sách
     * @throws Exception nếu có lỗi
     */
    private void handleImageEditUpload(Book book, MultipartFile imageFile, String currentImageUrl) throws Exception {
        String newImageUrl = book.getImageUrl(); // Link mới từ form
        
        // Case 1: Có link mới → Dùng link, xóa ảnh upload cũ nếu có
        if (newImageUrl != null && !newImageUrl.trim().isEmpty()) {
            // Xóa ảnh upload cũ (chỉ xóa local upload, không xóa external URL)
            if (currentImageUrl != null && !fileUploadService.isExternalUrl(currentImageUrl)) {
                fileUploadService.deleteBookImage(currentImageUrl);
            }
            
            book.setImageUrl(newImageUrl.trim());
            return;
        }
        
        // Case 2: Không có link mới NHƯNG có file upload → Upload file mới, xóa ảnh cũ
        if (imageFile != null && !imageFile.isEmpty()) {
            // Upload file mới trước
            String uploadedUrl = fileUploadService.uploadBookImage(imageFile);
            
            // Xóa ảnh cũ sau khi upload thành công (chỉ xóa local upload)
            if (currentImageUrl != null && !fileUploadService.isExternalUrl(currentImageUrl)) {
                fileUploadService.deleteBookImage(currentImageUrl);
            }
            
            book.setImageUrl(uploadedUrl);
            return;
        }
        
        // Case 3: Cả hai trống → Giữ ảnh hiện tại
        book.setImageUrl(currentImageUrl);
    }
}
