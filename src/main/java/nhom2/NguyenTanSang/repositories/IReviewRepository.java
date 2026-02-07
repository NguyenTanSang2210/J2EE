package nhom2.NguyenTanSang.repositories;

import nhom2.NguyenTanSang.entities.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IReviewRepository extends JpaRepository<Review, Long> {
    /**
     * Tìm tất cả review của một sách, sắp xếp theo ngày mới nhất
     */
    List<Review> findByBookIdOrderByReviewDateDesc(Long bookId);
    
    /**
     * Tìm review của sách với phân trang
     */
    Page<Review> findByBookId(Long bookId, Pageable pageable);
    
    /**
     * Tìm review của user cho một sách cụ thể
     */
    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);
    
    /**
     * Kiểm tra user đã review sách này chưa
     */
    boolean existsByUserIdAndBookId(Long userId, Long bookId);
    
    /**
     * Tính rating trung bình của sách
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId")
    Double getAverageRatingByBookId(@Param("bookId") Long bookId);
    
    /**
     * Đếm số lượng review của sách
     */
    long countByBookId(Long bookId);
}
