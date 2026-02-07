package nhom2.NguyenTanSang.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho giao dịch từ SePay API
 * Map từ JSON response của SePay /transactions endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SePayTransactionDto {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("gateway")
    private String gateway; // MB, VCB, TCB...
    
    @JsonProperty("transaction_date")
    private String transactionDate;
    
    @JsonProperty("account_number")
    private String accountNumber;
    
    @JsonProperty("code")
    private String code; // Mã giao dịch
    
    @JsonProperty("content")
    private String content; // Nội dung chuyển khoản
    
    @JsonProperty("transfer_amount")
    private Long transferAmount; // Số tiền (VNĐ)
    
    @JsonProperty("accumulated")
    private Long accumulated; // Số dư tài khoản
    
    @JsonProperty("subAccId")
    private String subAccId;
}
