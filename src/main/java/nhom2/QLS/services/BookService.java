package nhom2.QLS.services;
import nhom2.QLS.dtos.SearchDTO;
import nhom2.QLS.entities.Book;
import nhom2.QLS.repositories.IBookRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.SERIALIZABLE,
        rollbackFor = {Exception.class, Throwable.class})
public class BookService {
    private final IBookRepository bookRepository;
    public List<Book> getAllBooks(Integer pageNo,
                                  Integer pageSize,
                                  String sortBy) {

        return bookRepository.findAll(PageRequest.of(pageNo, pageSize, Sort.by(sortBy))).getContent();
    }
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }
    public void addBook(Book book) {
        bookRepository.save(book);
    }

    public void updateBook(@NotNull Book book) {
        Book existingBook = bookRepository.findById(book.getId())
                .orElse(null);
        Objects.requireNonNull(existingBook).setTitle(book.getTitle());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setPrice(book.getPrice());
        existingBook.setCategory(book.getCategory());
        existingBook.setStock(book.getStock());
        existingBook.setIsAvailable(book.getIsAvailable());
        bookRepository.save(existingBook);
    }
    public void deleteBookById(Long id) {
        bookRepository.deleteById(id);
    }

    public List<Book> searchBook(String keyword) {
        return bookRepository.searchBook(keyword);
    }
    
    /**
     * Tìm kiếm và lọc sách với nhiều tiêu chí
     */
    public Page<Book> searchWithFilters(SearchDTO searchDTO, int page, int size) {
        // Xử lý sortBy mặc định
        String sortBy = searchDTO.getSortBy();
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "id";
        }
        
        // Xử lý sortDirection mặc định
        String sortDirection = searchDTO.getSortDirection();
        if (sortDirection == null || sortDirection.isEmpty()) {
            sortDirection = "asc";
        }
        
        // Tạo Sort object
        Sort sort = Sort.by(
            sortDirection.equalsIgnoreCase("desc") 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC,
            sortBy
        );
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return bookRepository.findByFilters(
            searchDTO.getKeyword(),
            searchDTO.getCategoryId(),
            searchDTO.getMinPrice(),
            searchDTO.getMaxPrice(),
            pageable
        );
    }
    
    // ==================== INVENTORY MANAGEMENT ====================
    
    /**
     * Kiểm tra xem sách có đủ số lượng trong kho không
     */
    public boolean checkStock(Long bookId, int quantity) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new IllegalArgumentException("Book not found"));
        return book.hasStock(quantity);
    }
    
    /**
     * Giảm số lượng sách trong kho (khi bán hàng)
     */
    public void reduceStock(Long bookId, int quantity) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new IllegalArgumentException("Book not found"));
        book.reduceStock(quantity);
        bookRepository.save(book);
    }
    
    /**
     * Tăng số lượng sách trong kho (khi nhập hàng)
     */
    public void increaseStock(Long bookId, int quantity) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new IllegalArgumentException("Book not found"));
        book.increaseStock(quantity);
        bookRepository.save(book);
    }
    
    /**
     * Lấy danh sách sách sắp hết hàng
     */
    public List<Book> getLowStockBooks(int threshold) {
        return bookRepository.findByStockLessThanAndIsAvailableTrue(threshold);
    }
    
    /**
     * Lấy danh sách sách đã hết hàng
     */
    public List<Book> getOutOfStockBooks() {
        return bookRepository.findByStockEquals(0);
    }
}