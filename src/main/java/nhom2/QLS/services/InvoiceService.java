package nhom2.QLS.services;

import nhom2.QLS.daos.Cart;
import nhom2.QLS.daos.Item;
import nhom2.QLS.entities.Invoice;
import nhom2.QLS.entities.ItemInvoice;
import nhom2.QLS.entities.User;
import nhom2.QLS.repositories.IBookRepository;
import nhom2.QLS.repositories.IInvoiceRepository;
import nhom2.QLS.repositories.IItemInvoiceRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.SERIALIZABLE,
        rollbackFor = {Exception.class, Throwable.class})
public class InvoiceService {
    private final IInvoiceRepository invoiceRepository;
    private final IItemInvoiceRepository itemInvoiceRepository;
    private final IBookRepository bookRepository;
    private final BookService bookService;

    /**
     * Tạo hóa đơn từ giỏ hàng
     */
    public Invoice createInvoiceFromCart(@NotNull User user, 
                                        @NotNull Cart cart, 
                                        String paymentMethod, 
                                        String shippingAddress,
                                        String phone,
                                        String note) {
        if (cart.getCartItems().isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống");
        }

        // Kiểm tra stock trước khi tạo invoice
        for (Item item : cart.getCartItems()) {
            if (!bookService.checkStock(item.getBookId(), item.getQuantity())) {
                throw new IllegalStateException(
                    "Sách '" + item.getBookName() + "' không đủ số lượng trong kho"
                );
            }
        }

        // Tính tổng tiền
        double totalPrice = cart.getCartItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        // Tạo Invoice
        Invoice invoice = Invoice.builder()
                .invoiceDate(new Date())
                .price(totalPrice)
                .status("PENDING")
                .paymentMethod(paymentMethod)
                .shippingAddress(shippingAddress)
                .phone(phone)
                .note(note)
                .user(user)
                .build();

        invoice = invoiceRepository.save(invoice);

        // Tạo ItemInvoice cho từng item trong cart và giảm stock
        for (Item item : cart.getCartItems()) {
            ItemInvoice itemInvoice = ItemInvoice.builder()
                    .invoice(invoice)
                    .book(bookRepository.findById(item.getBookId())
                            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + item.getBookId())))
                    .quantity(item.getQuantity())
                    .build();
            itemInvoiceRepository.save(itemInvoice);
            
            // Giảm stock
            bookService.reduceStock(item.getBookId(), item.getQuantity());
        }

        return invoice;
    }

    /**
     * Lấy danh sách hóa đơn của user
     */
    public List<Invoice> getInvoicesByUser(Long userId) {
        return invoiceRepository.findByUserIdOrderByInvoiceDateDesc(userId);
    }

    /**
     * Lấy hóa đơn theo ID
     */
    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    /**
     * Cập nhật trạng thái hóa đơn
     */
    public Invoice updateInvoiceStatus(Long id, String status) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));

        // Validate status transitions
        if (!isValidStatusTransition(invoice.getStatus(), status)) {
            throw new IllegalStateException("Invalid status transition from " + 
                    invoice.getStatus() + " to " + status);
        }

        invoice.setStatus(status);
        return invoiceRepository.save(invoice);
    }

    /**
     * Hủy hóa đơn (chỉ được hủy khi status = PENDING)
     */
    public void cancelInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));

        if (!"PENDING".equals(invoice.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng đang chờ xử lý");
        }

        invoice.setStatus("CANCELLED");
        invoiceRepository.save(invoice);
    }

    /**
     * Lấy tất cả hóa đơn (cho admin) với pagination
     */
    public Page<Invoice> getAllInvoices(Pageable pageable) {
        return invoiceRepository.findAll(pageable);
    }

    /**
     * Lấy hóa đơn theo trạng thái
     */
    public List<Invoice> getInvoicesByStatus(String status) {
        return invoiceRepository.findByStatus(status);
    }

    /**
     * Kiểm tra quyền truy cập hóa đơn
     */
    public boolean canUserAccessInvoice(Long invoiceId, Long userId) {
        return invoiceRepository.findById(invoiceId)
                .map(invoice -> invoice.getUser().getId().equals(userId))
                .orElse(false);
    }

    /**
     * Validate status transition
     */
    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus.equals(newStatus)) {
            return true;
        }

        return switch (currentStatus) {
            case "PENDING" -> "PROCESSING".equals(newStatus) || "CANCELLED".equals(newStatus);
            case "PROCESSING" -> "COMPLETED".equals(newStatus) || "CANCELLED".equals(newStatus);
            case "COMPLETED", "CANCELLED" -> false; // Không thể chuyển từ trạng thái cuối
            default -> false;
        };
    }

    /**
     * Đếm số đơn hàng theo trạng thái
     */
    public long countByStatus(String status) {
        return invoiceRepository.countByStatus(status);
    }

    /**
     * Tính tổng doanh thu theo trạng thái
     */
    public Double getTotalRevenueByStatus(String status) {
        Double total = invoiceRepository.sumTotalPriceByStatus(status);
        return total != null ? total : 0.0;
    }
}
