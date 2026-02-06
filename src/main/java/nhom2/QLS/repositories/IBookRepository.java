package nhom2.QLS.repositories;
import nhom2.QLS.entities.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface IBookRepository extends
        PagingAndSortingRepository<Book, Long>, JpaRepository<Book, Long> {
    @Query("""
            SELECT b FROM Book b
            WHERE b.title LIKE %?1%
            OR b.author LIKE %?1%
            OR b.category.name LIKE %?1%
            """)
    List<Book> searchBook(String keyword);
    
    // Tìm kiếm với phân trang
    @Query("SELECT b FROM Book b WHERE " +
           "(:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Book> searchBooks(
        @Param("keyword") String keyword,
        Pageable pageable
    );
    
    // Lọc theo category với phân trang
    Page<Book> findByCategoryId(Long categoryId, Pageable pageable);
    
    // Lọc theo khoảng giá với phân trang
    Page<Book> findByPriceBetween(Double minPrice, Double maxPrice, Pageable pageable);
    
    // Tìm kiếm và lọc kết hợp
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
    
    // Inventory management queries
    List<Book> findByStockLessThanAndIsAvailableTrue(int threshold);
    List<Book> findByStockEquals(int stock);
    List<Book> findByIsAvailableFalse();
}