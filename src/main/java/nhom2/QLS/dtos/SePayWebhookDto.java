package nhom2.QLS.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận Webhook từ SePay khi có giao dịch mới
 * SePay sẽ POST dữ liệu này tới endpoint webhook của chúng ta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SePayWebhookDto {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("gateway")
    private String gateway; // "VCB", "MBBank", etc.
    
    @JsonProperty("transaction_date")
    private String transactionDate; // ISO 8601 format
    
    @JsonProperty("account_number")
    private String accountNumber; // Số tài khoản nhận
    
    @JsonProperty("sub_account")
    private String subAccount;
    
    @JsonProperty("amount_in")
    private Long amountIn; // Số tiền chuyển vào (VNĐ)
    
    @JsonProperty("amount_out")
    private Long amountOut; // Số tiền chuyển ra (VNĐ)
    
    @JsonProperty("accumulated")
    private Long accumulated; // Số dư tích lũy
    
    @JsonProperty("code")
    private String code; // Mã giao dịch từ ngân hàng
    
    @JsonProperty("transaction_content")
    private String transactionContent; // Nội dung chuyển khoản
    
    @JsonProperty("reference_number")
    private String referenceNumber; // Mã tham chiếu
    
    @JsonProperty("body")
    private String body;
    
    /**
     * Lấy số tiền giao dịch (ưu tiên amount_in)
     */
    public Long getTransferAmount() {
        return amountIn != null && amountIn > 0 ? amountIn : amountOut;
    }
    
    /**
     * Lấy nội dung chuyển khoản
     */
    public String getContent() {
        return transactionContent != null ? transactionContent : body;
    }
}
