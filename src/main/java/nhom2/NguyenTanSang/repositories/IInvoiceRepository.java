package nhom2.NguyenTanSang.repositories;
import nhom2.NguyenTanSang.entities.Invoice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface IInvoiceRepository extends JpaRepository<Invoice,Long>{
    
    // Tìm hóa đơn theo user ID, sắp xếp theo ngày giảm dần
    List<Invoice> findByUserIdOrderByInvoiceDateDesc(Long userId);
    
    // Tìm hóa đơn theo status
    List<Invoice> findByStatus(String status);
    
    // Đếm số hóa đơn theo status
    Long countByStatus(String status);
    
    // Tính tổng doanh thu theo status
    @Query("SELECT SUM(i.price) FROM Invoice i WHERE i.status = :status")
    Double sumTotalPriceByStatus(@Param("status") String status);
    
    // Tìm hóa đơn theo user và status
    List<Invoice> findByUserIdAndStatus(Long userId, String status);
    
    /**
     * Kiểm tra xem user đã mua sách này chưa (invoice status = COMPLETED)
     * Sử dụng để xác định user có quyền review không
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END " +
           "FROM Invoice i JOIN i.itemInvoices ii " +
           "WHERE i.user.id = :userId AND i.status = :status AND ii.book.id = :bookId")
    boolean existsByUserIdAndStatusAndItemInvoices_BookId(
        @Param("userId") Long userId,
        @Param("status") String status,
        @Param("bookId") Long bookId
    );
    
    // Tính tổng doanh thu theo năm và tháng
    @Query("SELECT SUM(i.price) FROM Invoice i WHERE YEAR(i.invoiceDate) = :year " +
           "AND MONTH(i.invoiceDate) = :month AND i.status = 'COMPLETED'")
    Double sumRevenueByYearAndMonth(@Param("year") int year, @Param("month") int month);
    
    // Lấy danh sách đơn hàng gần đây
    @Query("SELECT new map(i.id as id, i.invoiceDate as date, i.price as total, " +
           "i.status as status, u.username as username) " +
           "FROM Invoice i JOIN i.user u ORDER BY i.invoiceDate DESC")
    List<Map<String, Object>> findRecentOrders(Pageable pageable);
}
