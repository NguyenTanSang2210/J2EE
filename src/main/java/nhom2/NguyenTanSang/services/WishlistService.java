package nhom2.NguyenTanSang.services;

import nhom2.NguyenTanSang.entities.Book;
import nhom2.NguyenTanSang.entities.User;
import nhom2.NguyenTanSang.entities.Wishlist;
import nhom2.NguyenTanSang.repositories.IBookRepository;
import nhom2.NguyenTanSang.repositories.IWishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        
        // Find the book
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách"));
        
        // Create wishlist item
        User user = new User();
        user.setId(userId);
        
        Wishlist wishlist = Wishlist.builder()
            .user(user)
            .book(book)
            .build();
        
        return wishlistRepository.save(wishlist);
    }
    
    @Transactional
    public void removeFromWishlist(Long userId, Long bookId) {
        wishlistRepository.deleteByUserIdAndBookId(userId, bookId);
    }
    
    @Transactional(readOnly = true)
    public List<Wishlist> getWishlistByUser(Long userId) {
        return wishlistRepository.findByUserIdOrderByAddedDateDesc(userId);
    }
    
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long bookId) {
        return wishlistRepository.existsByUserIdAndBookId(userId, bookId);
    }
    
    @Transactional(readOnly = true)
    public long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }
}
