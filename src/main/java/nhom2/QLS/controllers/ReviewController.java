package nhom2.QLS.controllers;

import nhom2.QLS.entities.Book;
import nhom2.QLS.entities.Review;
import nhom2.QLS.entities.User;
import nhom2.QLS.repositories.IUserRepository;
import nhom2.QLS.services.BookService;
import nhom2.QLS.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final BookService bookService;
    private final IUserRepository userRepository;
    
    /**
     * Thêm review mới
     */
    @PostMapping("/add")
    public String addReview(
        @RequestParam Long bookId,
        @RequestParam Integer rating,
        @RequestParam String comment,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {
        try {
            // Lấy user từ database
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
            
            // Kiểm tra book tồn tại
            Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách"));
            
            // Tạo review
            Review review = Review.builder()
                .rating(rating)
                .comment(comment.trim())
                .user(user)
                .book(book)
                .build();
            
            reviewService.addReview(review, user, bookId);
            redirectAttributes.addFlashAttribute("message", "Đánh giá của bạn đã được gửi thành công!");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return "redirect:/books/" + bookId;
    }
    
    /**
     * Xóa review
     */
    @GetMapping("/delete/{id}")
    public String deleteReview(
        @PathVariable Long id,
        @RequestParam(required = false) Long bookId,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {
        try {
            // Lấy user từ database
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
            
            // Lấy bookId từ review trước khi xóa
            if (bookId == null) {
                Review review = reviewService.getUserReviewForBook(user.getId(), id);
                if (review != null) {
                    bookId = review.getBook().getId();
                }
            }
            
            reviewService.deleteReview(id, user);
            redirectAttributes.addFlashAttribute("message", "Đã xóa đánh giá thành công!");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        // Redirect về trang chi tiết sách nếu có bookId, không thì về danh sách
        if (bookId != null) {
            return "redirect:/books/" + bookId;
        }
        return "redirect:/books";
    }
    
    /**
     * Cập nhật review
     */
    @PostMapping("/update/{id}")
    public String updateReview(
        @PathVariable Long id,
        @RequestParam Long bookId,
        @RequestParam Integer rating,
        @RequestParam String comment,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {
        try {
            // Lấy user từ database
            User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
            
            reviewService.updateReview(id, rating, comment.trim(), user);
            redirectAttributes.addFlashAttribute("message", "Đã cập nhật đánh giá thành công!");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return "redirect:/books/" + bookId;
    }
}
