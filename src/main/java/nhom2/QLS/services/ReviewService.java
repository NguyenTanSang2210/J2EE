package nhom2.QLS.services;

import nhom2.QLS.entities.Review;
import nhom2.QLS.entities.User;
import nhom2.QLS.repositories.IInvoiceRepository;
import nhom2.QLS.repositories.IReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.SERIALIZABLE,
        rollbackFor = {Exception.class, Throwable.class})
public class ReviewService {
    private final IReviewRepository reviewRepository;
    private final IInvoiceRepository invoiceRepository;
    
    /**
     * Thêm review mới
     * Kiểm tra: user đã mua sách và chưa review
     */
    public Review addReview(Review review, User user, Long bookId) {
        // Kiểm tra user đã mua sách này chưa
        if (!hasUserPurchasedBook(user.getId(), bookId)) {
            throw new IllegalStateException("Bạn chỉ có thể đánh giá sách đã mua");
        }
        
        // Kiểm tra user đã review chưa
        if (reviewRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            throw new IllegalStateException("Bạn đã đánh giá sách này rồi");
        }
        
        // Validate rating
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Đánh giá phải từ 1 đến 5 sao");
        }
        
        // Set review date nếu chưa có
        if (review.getReviewDate() == null) {
            review.setReviewDate(new Date());
        }
        
        return reviewRepository.save(review);
    }
    
    /**
     * Lấy danh sách review của sách với phân trang
     */
    public Page<Review> getReviewsByBook(Long bookId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("reviewDate").descending());
        return reviewRepository.findByBookId(bookId, pageable);
    }
    
    /**
     * Lấy tất cả review của sách (không phân trang)
     */
    public List<Review> getAllReviewsByBook(Long bookId) {
        return reviewRepository.findByBookIdOrderByReviewDateDesc(bookId);
    }
    
    /**
     * Tính rating trung bình của sách
     */
    public Double getAverageRating(Long bookId) {
        Double avg = reviewRepository.getAverageRatingByBookId(bookId);
        return avg != null ? avg : 0.0;
    }
    
    /**
     * Đếm số review của sách
     */
    public long countReviews(Long bookId) {
        return reviewRepository.countByBookId(bookId);
    }
    
    /**
     * Kiểm tra user đã mua sách này chưa
     */
    public boolean hasUserPurchasedBook(Long userId, Long bookId) {
        return invoiceRepository.existsByUserIdAndStatusAndItemInvoices_BookId(
            userId, "COMPLETED", bookId
        );
    }
    
    /**
     * Kiểm tra user có thể review sách không
     * Điều kiện: đã mua và chưa review
     */
    public boolean canUserReview(Long userId, Long bookId) {
        return hasUserPurchasedBook(userId, bookId) 
            && !reviewRepository.existsByUserIdAndBookId(userId, bookId);
    }
    
    /**
     * Lấy review của user cho sách
     */
    public Review getUserReviewForBook(Long userId, Long bookId) {
        return reviewRepository.findByUserIdAndBookId(userId, bookId)
            .orElse(null);
    }
    
    /**
     * Xóa review
     * Chỉ owner hoặc admin mới được xóa
     */
    public void deleteReview(Long id, User user) {
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá"));
            
        // Kiểm tra quyền: chỉ owner hoặc admin
        boolean isOwner = review.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRoles().stream()
            .anyMatch(r -> r.getName().equals("ADMIN"));
            
        if (!isOwner && !isAdmin) {
            throw new IllegalStateException("Không có quyền xóa đánh giá này");
        }
        
        reviewRepository.deleteById(id);
    }
    
    /**
     * Cập nhật review (chỉ owner)
     */
    public Review updateReview(Long id, Integer rating, String comment, User user) {
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá"));
            
        // Kiểm tra quyền: chỉ owner
        if (!review.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Không có quyền sửa đánh giá này");
        }
        
        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Đánh giá phải từ 1 đến 5 sao");
        }
        
        review.setRating(rating);
        review.setComment(comment);
        review.setReviewDate(new Date()); // Update review date
        
        return reviewRepository.save(review);
    }
}
