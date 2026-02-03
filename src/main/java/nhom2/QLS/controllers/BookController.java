package nhom2.QLS.controllers;

import nhom2.QLS.entities.Book;
import nhom2.QLS.entities.Category;
import nhom2.QLS.services.BookService;
import nhom2.QLS.services.CategoryService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import nhom2.QLS.services.CartService;
import nhom2.QLS.daos.Item;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final CategoryService categoryService;
    private final CartService cartService;
    
    @GetMapping
    public String showAllBooks(
            @NotNull Model model,
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy) {
        model.addAttribute("books", bookService.getAllBooks(pageNo,
                pageSize, sortBy));
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages",
                bookService.getAllBooks(pageNo, pageSize, sortBy).size() / pageSize);
        return "book/list";
    }
    
    @GetMapping("/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "book/add";
    }
    
    @PostMapping("/add")
    public String saveBook(@ModelAttribute Book book, @RequestParam(required = false) Long categoryId) {
        // Nếu categoryId được cung cấp, load Category object từ database
        if (categoryId != null && categoryId > 0) {
            categoryService.getCategoryById(categoryId).ifPresent(book::setCategory);
        }
        bookService.addBook(book);
        return "redirect:/books";
    }
    
    @GetMapping("/edit/{id}")
    public String showEditBookForm(@PathVariable Long id, Model model) {
        bookService.getBookById(id).ifPresent(book -> {
            model.addAttribute("book", book);
            model.addAttribute("categories", categoryService.getAllCategories());
        });
        return "book/edit";
    }
    
    @PostMapping("/edit/{id}")
    public String updateBook(@PathVariable Long id, @ModelAttribute Book book, 
                            @RequestParam(required = false) Long categoryId,
                            @RequestParam(required = false) Double price) {
        book.setId(id);
        if (price != null) {
            book.setPrice(price);
        }
        if (categoryId != null && categoryId > 0) {
            categoryService.getCategoryById(categoryId).ifPresent(book::setCategory);
        }
        bookService.updateBook(book);
        return "redirect:/books";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookService.deleteBookById(id);
        return "redirect:/books";
    }

    @PostMapping("/add-to-cart")
    public String addToCart(HttpSession session,
                            @RequestParam long id,
                            @RequestParam String name,
                            @RequestParam double price,
                            @RequestParam(defaultValue = "1") int quantity)
    {
        var cart = cartService.getCart(session);
        cart.addItems(new Item(id, name, price, quantity));
        cartService.updateCart(session, cart);
        return "redirect:/books";
    }
}