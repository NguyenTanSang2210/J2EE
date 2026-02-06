package nhom2.QLS.controllers;

import nhom2.QLS.dtos.SearchDTO;
import nhom2.QLS.entities.Book;
import nhom2.QLS.entities.Review;
import nhom2.QLS.entities.User;
import nhom2.QLS.daos.Item;
import nhom2.QLS.repositories.IUserRepository;
import nhom2.QLS.services.BookService;
import nhom2.QLS.services.CartService;
import nhom2.QLS.services.CategoryService;
import nhom2.QLS.services.ReviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;


@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final CategoryService categoryService;
    private final CartService cartService;
    private final ReviewService reviewService;
    private final IUserRepository userRepository;

    @GetMapping
    public String showAllBooks(@NotNull Model model,
                               @RequestParam(defaultValue = "0")
                               Integer pageNo,
                               @RequestParam(defaultValue = "20")
                               Integer pageSize,
                               @RequestParam(defaultValue = "id")
                               String sortBy) {
        List<Book> books = bookService.getAllBooks(pageNo, pageSize, sortBy);
        model.addAttribute("books", books);
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", books.size() / pageSize);
        model.addAttribute("totalItems", (long) books.size());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("searchDTO", new SearchDTO());
        return "book/list";
    }

    /**
     * Hiển thị chi tiết sách với reviews
     */
    @GetMapping("/{id}")
    public String bookDetail(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") int page,
        Principal principal,
        Model model
    ) {
        // Lấy thông tin sách
        Book book = bookService.getBookById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách"));
        
        // Lấy reviews với phân trang
        Page<Review> reviews = reviewService.getReviewsByBook(id, page, 5);
        
        // Kiểm tra user có thể review không
        boolean canReview = false;
        Review userReview = null;
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName())
                .orElse(null);
            if (user != null) {
                canReview = reviewService.canUserReview(user.getId(), id);
                userReview = reviewService.getUserReviewForBook(user.getId(), id);
            }
        }
        
        model.addAttribute("book", book);
        model.addAttribute("reviews", reviews);
        model.addAttribute("canReview", canReview);
        model.addAttribute("userReview", userReview);
        
        return "book/detail";
    }

    @GetMapping("/add")
    public String addBookForm(@NotNull Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories",
                categoryService.getAllCategories());
        return "book/add";
    }

    @PostMapping("/add")
    public String addBook(
            @Valid @ModelAttribute("book") Book book,
            @NotNull BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            var errors = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage).toArray(String[]::new);
            model.addAttribute("errors", errors);
            model.addAttribute("categories",
                    categoryService.getAllCategories());
            return "book/add";
        }
        bookService.addBook(book);
        return "redirect:/books";
    }

    @PostMapping("/add-to-cart")
    public String addToCart(HttpSession session,
                            @RequestParam long id,
                            @RequestParam String name,
                            @RequestParam double price,
                            @RequestParam(defaultValue = "1") int
                                    quantity) {
        var cart = cartService.getCart(session);
        cart.addItems(new Item(id, name, price, quantity));
        cartService.updateCart(session, cart);
        return "redirect:/books";
    }

    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable long id) {
        bookService.getBookById(id)
                .ifPresentOrElse(
                        book -> bookService.deleteBookById(id),
                        () -> {
                            throw new IllegalArgumentException("Book not found");
                        });
        return "redirect:/books";
    }
    @GetMapping("/edit/{id}")
    public String editBookForm(@NotNull Model model, @PathVariable long id)
    {
        var book = bookService.getBookById(id);
        model.addAttribute("book", book.orElseThrow(() -> new IllegalArgumentException("Book not found")));
        model.addAttribute("categories",
                categoryService.getAllCategories());
        return "book/edit";
    }

    @PostMapping("/edit")
    public String editBook(@Valid @ModelAttribute("book") Book book,
                           @NotNull BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            var errors = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toArray(String[]::new);
            model.addAttribute("errors", errors);
            model.addAttribute("categories",
                    categoryService.getAllCategories());
            return "book/edit";
        }
        bookService.updateBook(book);
        return "redirect:/books";
    }

    @GetMapping("/search")
    public String searchBooks(
            @ModelAttribute SearchDTO searchDTO,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        Page<Book> booksPage = bookService.searchWithFilters(searchDTO, page, size);
        
        model.addAttribute("books", booksPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", booksPage.getTotalPages());
        model.addAttribute("totalItems", booksPage.getTotalElements());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("searchDTO", searchDTO);
        
        return "book/list";
    }
}


