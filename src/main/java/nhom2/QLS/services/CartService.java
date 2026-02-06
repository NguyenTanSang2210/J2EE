package nhom2.QLS.services;
import nhom2.QLS.daos.Cart;
import nhom2.QLS.daos.Item;
import nhom2.QLS.entities.Invoice;
import nhom2.QLS.entities.ItemInvoice;
import nhom2.QLS.repositories.IBookRepository;
import nhom2.QLS.repositories.IInvoiceRepository;
import nhom2.QLS.repositories.IItemInvoiceRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.Optional;
@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.SERIALIZABLE,
        rollbackFor = {Exception.class, Throwable.class})
public class CartService {
    private static final String CART_SESSION_KEY = "cart";
    private final IInvoiceRepository invoiceRepository;
    private final IItemInvoiceRepository itemInvoiceRepository;
    private final IBookRepository bookRepository;
    private final BookService bookService;
    public Cart getCart(@NotNull HttpSession session) {
        return Optional.ofNullable((Cart)
                        session.getAttribute(CART_SESSION_KEY))
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    session.setAttribute(CART_SESSION_KEY, cart);
                    return cart;
                });
    }
    public void updateCart(@NotNull HttpSession session, Cart cart) {
        session.setAttribute(CART_SESSION_KEY, cart);
    }
    public void removeCart(@NotNull HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
    }
    public int getSumQuantity(@NotNull HttpSession session) {
        return getCart(session).getCartItems().stream()
                .mapToInt(Item::getQuantity)
                .sum();
    }
    public double getSumPrice(@NotNull HttpSession session) {
        return getCart(session).getCartItems().stream()
                .mapToDouble(item -> item.getPrice() *
                        item.getQuantity())
                .sum();
    }
    public void saveCart(@NotNull HttpSession session) {
        var cart = getCart(session);
        if (cart.getCartItems().isEmpty()) return;
        var invoice = new Invoice();
        invoice.setInvoiceDate(new Date(new Date().getTime()));
        invoice.setPrice(getSumPrice(session));
        invoice.setStatus("PENDING");
        invoiceRepository.save(invoice);
        cart.getCartItems().forEach(item -> {
            var items = new ItemInvoice();
            items.setInvoice(invoice);
            items.setQuantity(item.getQuantity());
            items.setBook(bookRepository.findById(item.getBookId())
                    .orElseThrow());
            itemInvoiceRepository.save(items);
        });
        removeCart(session);
    }
    
    public void saveCart(@NotNull HttpSession session, @NotNull Invoice invoiceData) {
        var cart = getCart(session);
        if (cart.getCartItems().isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống");
        }
        
        // Check stock availability before processing
        for (Item item : cart.getCartItems()) {
            if (!bookService.checkStock(item.getBookId(), item.getQuantity())) {
                throw new IllegalStateException("Sách '" + item.getBookName() + "' không đủ hàng trong kho");
            }
        }
        
        // Set invoice date and price
        invoiceData.setInvoiceDate(new Date());
        invoiceData.setPrice(getSumPrice(session));
        
        // Save invoice
        invoiceRepository.save(invoiceData);
        
        // Save cart items and reduce stock
        cart.getCartItems().forEach(item -> {
            var itemInvoice = new ItemInvoice();
            itemInvoice.setInvoice(invoiceData);
            itemInvoice.setQuantity(item.getQuantity());
            itemInvoice.setBook(bookRepository.findById(item.getBookId())
                    .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + item.getBookId())));
            itemInvoiceRepository.save(itemInvoice);
            
            // Reduce stock after order is placed
            bookService.reduceStock(item.getBookId(), item.getQuantity());
        });
        
        // Clear cart
        removeCart(session);
    }
}