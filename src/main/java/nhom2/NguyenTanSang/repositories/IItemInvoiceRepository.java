package nhom2.NguyenTanSang.repositories;
import nhom2.NguyenTanSang.entities.ItemInvoice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface IItemInvoiceRepository extends
        JpaRepository<ItemInvoice, Long>{
    
    // Lấy danh sách sách bán chạy nhất
    @Query("SELECT new map(b.title as bookTitle, SUM(ii.quantity) as totalSold, " +
           "SUM(ii.quantity * b.price) as revenue) " +
           "FROM ItemInvoice ii JOIN ii.book b JOIN ii.invoice i " +
           "WHERE i.status = 'COMPLETED' " +
           "GROUP BY b.id, b.title " +
           "ORDER BY SUM(ii.quantity) DESC")
    List<Map<String, Object>> findBestSellingBooks(Pageable pageable);
}