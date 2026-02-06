package nhom2.QLS.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho QR Code thanh toán
 * Chứa thông tin hiển thị trên trang thanh toán
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SePayQrCodeDto {
    private String qrDataURL;        // URL hình ảnh QR code
    private String accountNumber;    // Số tài khoản
    private String accountName;      // Tên chủ tài khoản
    private String bankCode;         // Mã ngân hàng (MB, VCB, TCB...)
    private String bankName;         // Tên ngân hàng đầy đủ
    private Long amount;             // Số tiền (VNĐ)
    private String content;          // Nội dung CK: "QLSACH 123"
    private String description;      // Mô tả (alias của content)
    private Long invoiceId;          // ID invoice
}
