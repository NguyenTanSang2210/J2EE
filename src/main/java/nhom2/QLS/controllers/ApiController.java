package nhom2.QLS.controllers;
import nhom2.QLS.entities.Book;
import nhom2.QLS.entities.Category;
import nhom2.QLS.services.BookService;
import nhom2.QLS.services.CartService;
import nhom2.QLS.services.CategoryService;
import nhom2.QLS.viewmodels.BookGetVm;
import nhom2.QLS.viewmodels.BookPostVm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ApiController {
    private final BookService bookService;
    private final CategoryService categoryService;
    private final CartService cartService;
    
    @GetMapping("/books")
    public ResponseEntity<List<BookGetVm>> getAllBooks(Integer pageNo, Integer pageSize, String sortBy) {
        return ResponseEntity.ok(bookService.getAllBooks(
                        pageNo == null ? 0 : pageNo,
                        pageSize == null ? 20 : pageSize,
                        sortBy == null ? "id" : sortBy)
                .stream()
                .map(BookGetVm::from)
                .toList());
    }
    
    @GetMapping("/books/id/{id}")
    public ResponseEntity<BookGetVm> getBookById(@PathVariable Long id)
    {
        return ResponseEntity.ok(bookService.getBookById(id)
                .map(BookGetVm::from)
                .orElse(null));
    }
    
    @PostMapping("/books")
    public ResponseEntity<BookGetVm> addBook(@Valid @RequestBody BookPostVm bookPostVm) {
        Category category = categoryService.getCategoryById(bookPostVm.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + bookPostVm.categoryId()));
        
        Book book = Book.builder()
                .title(bookPostVm.title())
                .author(bookPostVm.author())
                .price(bookPostVm.price())
                .category(category)
                .build();
        
        bookService.addBook(book);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BookGetVm.from(book));
    }
    
    @PutMapping("/books/{id}")
    public ResponseEntity<BookGetVm> updateBook(@PathVariable Long id, 
                                                 @Valid @RequestBody BookPostVm bookPostVm) {
        Book existingBook = bookService.getBookById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        
        Category category = categoryService.getCategoryById(bookPostVm.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + bookPostVm.categoryId()));
        
        existingBook.setTitle(bookPostVm.title());
        existingBook.setAuthor(bookPostVm.author());
        existingBook.setPrice(bookPostVm.price());
        existingBook.setCategory(category);
        
        bookService.updateBook(existingBook);
        return ResponseEntity.ok(BookGetVm.from(existingBook));
    }
    
    @DeleteMapping("/books/{id}")
    public ResponseEntity<Void> deleteBookById(@PathVariable Long id) {
        bookService.deleteBookById(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/books/search")
    public ResponseEntity<List<BookGetVm>> searchBooks(String keyword)
    {
        return ResponseEntity.ok(bookService.searchBook(keyword)
                .stream()
                .map(BookGetVm::from)
                .toList());
    }
}