package nhom2.QLS.repositories;

import nhom2.QLS.entities.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IWishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserIdOrderByAddedDateDesc(Long userId);
    
    Optional<Wishlist> findByUserIdAndBookId(Long userId, Long bookId);
    
    boolean existsByUserIdAndBookId(Long userId, Long bookId);
    
    void deleteByUserIdAndBookId(Long userId, Long bookId);
    
    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
