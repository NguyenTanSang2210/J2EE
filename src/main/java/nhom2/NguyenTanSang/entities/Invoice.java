package nhom2.NguyenTanSang.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.Hibernate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "invoice_date")
    private Date invoiceDate = new Date();

    @Column(name = "total")
    @Positive(message = "Total must be positive")
    private Double price;

    @Column(name = "status", length = 20, nullable = false)
    @NotBlank(message = "Status must not be blank")
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, CANCELLED

    @Column(name = "payment_method", length = 20)
    @Size(max = 20, message = "Payment method must be less than 20 characters")
    private String paymentMethod; // COD, BANKING, CREDIT_CARD

    @Column(name = "receiver_name", length = 100)
    @Size(max = 100, message = "Receiver name must be less than 100 characters")
    private String receiverName;

    @Column(name = "email", length = 100)
    @Size(max = 100, message = "Email must be less than 100 characters")
    private String email; // Email tài khoản (dùng để link với user)

    @Column(name = "receiver_email", length = 100)
    @Size(max = 100, message = "Receiver email must be less than 100 characters")
    private String receiverEmail; // Email người nhận hàng (có thể khác email tài khoản)

    @Column(name = "shipping_address", length = 500)
    @Size(max = 500, message = "Shipping address must be less than 500 characters")
    private String shippingAddress;

    @Column(name = "phone", length = 15)
    @Size(max = 15, message = "Phone must be less than 15 characters")
    private String phone;

    @Column(name = "note", length = 1000)
    @Size(max = 1000, message = "Note must be less than 1000 characters")
    private String note;

    // ==================== SePay Payment Fields ====================
    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "PENDING"; // PENDING, PAID, FAILED

    @Column(name = "transaction_code", length = 100)
    private String transactionCode; // Mã giao dịch từ ngân hàng

    @Column(name = "paid_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date paidAt; // Thời gian thanh toán thành công

    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl; // URL QR code thanh toán
    // ==============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @ToString.Exclude
    private User user;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<ItemInvoice> itemInvoices = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) !=
                Hibernate.getClass(o)) return false;
        Invoice invoice = (Invoice) o;
        return getId() != null && Objects.equals(getId(),
                invoice.getId());
    }
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}