package nhom2.NguyenTanSang.controllers.admin;

import nhom2.NguyenTanSang.entities.Book;
import nhom2.NguyenTanSang.services.BookService;
import nhom2.NguyenTanSang.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final BookService bookService;
    private final CategoryService categoryService;
    
    /**
     * Hiển thị trang quản lý kho với lọc/tìm kiếm
     */
    @GetMapping
    public String inventoryPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String stockStatus,
            Model model) {
        
        // Lấy tất cả sách và sắp xếp theo ID tăng dần
        List<Book> allBooks = bookService.getAllBooks(0, 1000, "id");
        
        // Áp dụng bộ lọc
        List<Book> filteredBooks = allBooks.stream()
            .filter(book -> {
                // Lọc theo keyword
                if (keyword != null && !keyword.trim().isEmpty()) {
                    String lowerKeyword = keyword.toLowerCase();
                    boolean matchKeyword = book.getTitle().toLowerCase().contains(lowerKeyword) ||
                                          book.getAuthor().toLowerCase().contains(lowerKeyword);
                    if (!matchKeyword) return false;
                }
                
                // Lọc theo category
                if (categoryId != null && categoryId > 0) {
                    if (!book.getCategory().getId().equals(categoryId)) return false;
                }
                
                // Lọc theo trạng thái kho
                if (stockStatus != null && !stockStatus.isEmpty()) {
                    switch (stockStatus) {
                        case "out_of_stock":
                            if (book.getStock() != 0) return false;
                            break;
                        case "low_stock":
                            if (book.getStock() > 10 || book.getStock() == 0) return false;
                            break;
                        case "in_stock":
                            if (book.getStock() <= 10) return false;
                            break;
                    }
                }
                
                return true;
            })
            .sorted(Comparator.comparing(Book::getId))
            .collect(Collectors.toList());
        
        List<Book> lowStockBooks = bookService.getLowStockBooks(10);
        List<Book> outOfStockBooks = bookService.getOutOfStockBooks();
        
        model.addAttribute("allBooks", filteredBooks);
        model.addAttribute("lowStockBooks", lowStockBooks);
        model.addAttribute("outOfStockBooks", outOfStockBooks);
        model.addAttribute("lowStockCount", lowStockBooks.size());
        model.addAttribute("outOfStockCount", outOfStockBooks.size());
        model.addAttribute("totalBooks", allBooks.size());
        model.addAttribute("filteredCount", filteredBooks.size());
        
        // Truyền categories và filter params
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("stockStatus", stockStatus);
        
        return "admin/inventory";
    }
    
    /**
     * Cập nhật số lượng kho
     * Số dương: Nhập hàng (tăng kho)
     * Số âm: Xuất hàng (giảm kho)
     */
    @PostMapping("/update/{id}")
    public String updateStock(@PathVariable Long id, 
                             @RequestParam int quantity,
                             RedirectAttributes redirectAttributes) {
        try {
            if (quantity == 0) {
                redirectAttributes.addFlashAttribute("error", 
                    "Số lượng thay đổi phải khác 0");
            } else if (quantity > 0) {
                // Số dương: Nhập hàng
                bookService.increaseStock(id, quantity);
                redirectAttributes.addFlashAttribute("message", 
                    "Đã nhập thêm " + quantity + " quyển sách vào kho");
            } else {
                // Số âm: Xuất hàng (giảm kho)
                int decreaseAmount = Math.abs(quantity);
                bookService.reduceStock(id, decreaseAmount);
                redirectAttributes.addFlashAttribute("message", 
                    "Đã xuất " + decreaseAmount + " quyển sách khỏi kho");
            }
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Cập nhật kho thất bại: " + e.getMessage());
        }
        return "redirect:/admin/inventory";
    }
}
