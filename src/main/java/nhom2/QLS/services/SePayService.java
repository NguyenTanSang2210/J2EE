package nhom2.QLS.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhom2.QLS.config.SePayConfig;
import nhom2.QLS.dtos.SePayQrCodeDto;
import nhom2.QLS.dtos.SePayTransactionDto;
import nhom2.QLS.entities.Invoice;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý tích hợp SePay Payment Gateway
 * - Tạo QR Code thanh toán
 * - Lấy danh sách giao dịch từ SePay
 * - Xác minh thanh toán cho invoice
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SePayService {
    
    private final SePayConfig sePayConfig;
    private final WebClient.Builder webClientBuilder;

    /**
     * 1. Generate Dynamic QR Code for Order Payment using Napas QR (SePay)
     * 
     * Creates a unique QR code for each order using Napas QR API from SePay.
     * This API is 100% compatible with ALL Vietnamese banks including TPBank.
     * 
     * The QR contains:
     * - Bank code (TPB for TPBank)
     * - Account number
     * - Exact amount
     * - Transfer content with order ID for tracking
     * 
     * When customer scans this QR:
     * - Banking app auto-fills all information
     * - Customer just needs to confirm the transfer
     * - SePay webhook will notify us when payment is received
     * 
     * @param invoice The order/invoice to generate QR for
     * @return SePayQrCodeDto containing QR URL and payment details
     */
    public SePayQrCodeDto generateQrCode(Invoice invoice) {
        try {
            // Generate unique transfer content: "ORDER_123"
            // This format helps webhook identify which order was paid
            String transferContent = String.format("ORDER_%d", invoice.getId());
            
            // Build Napas QR URL using SePay API (100% compatible with all VN banks)
            // Format: https://qr.sepay.vn/img?acc={account}&bank={bank}&amount={amount}&des={content}
            String qrImageUrl = String.format(
                "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s",
                sePayConfig.getAccount().getNumber(),      // Account number
                sePayConfig.getBank().getCode(),           // Bank code (TPB)
                invoice.getPrice().longValue(),            // Amount in VND
                URLEncoder.encode(transferContent, StandardCharsets.UTF_8)  // Transfer content
            );
            
            log.info("✅ [ORDER #{}] Generated Napas QR URL: {}", invoice.getId(), qrImageUrl);
            log.info("   Amount: {} VND | Content: {} | Bank: {} | Account: {}", 
                    invoice.getPrice().longValue(), transferContent, 
                    sePayConfig.getBank().getCode(), sePayConfig.getAccount().getNumber());
            
            // Get human-readable bank name
            String bankName = getBankName(sePayConfig.getBank().getCode());
            
            // Build response DTO
            return SePayQrCodeDto.builder()
                    .qrDataURL(qrImageUrl)                     // QR image URL
                    .accountNumber(sePayConfig.getAccount().getNumber())
                    .accountName(sePayConfig.getAccount().getName())
                    .bankCode(sePayConfig.getBank().getCode())
                    .bankName(bankName)
                    .amount(invoice.getPrice().longValue())
                    .content(transferContent)                   // What customer must type
                    .description("Payment for Order #" + invoice.getId())
                    .invoiceId(invoice.getId())
                    .build();
                    
        } catch (Exception e) {
            log.error("❌ [ORDER #{}] Failed to generate QR code", invoice.getId(), e);
            throw new RuntimeException("Cannot generate QR code: " + e.getMessage(), e);
        }
    }

    /**
     * 2. Lấy danh sách giao dịch gần đây từ SePay API
     * 
     * @param limit Số lượng giao dịch tối đa cần lấy
     * @return List<SePayTransactionDto> danh sách giao dịch
     */
    public List<SePayTransactionDto> getRecentTransactions(int limit) {
        try {
            log.info("🔍 [DEBUG] ========== FETCHING SEPAY TRANSACTIONS ==========");
            log.info("🔍 [DEBUG] API URL: {}", sePayConfig.getApi().getUrl());
            log.info("🔍 [DEBUG] Account Number: {}", sePayConfig.getAccount().getNumber());
            log.info("🔍 [DEBUG] Limit: {}", limit);
            log.info("🔍 [DEBUG] Token: {}...{}", 
                    sePayConfig.getApi().getToken().substring(0, 10),
                    sePayConfig.getApi().getToken().substring(sePayConfig.getApi().getToken().length() - 10));
            
            WebClient webClient = webClientBuilder
                    .baseUrl(sePayConfig.getApi().getUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + sePayConfig.getApi().getToken())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String fullUrl = String.format("%s/transactions?account_number=%s&limit=%d",
                    sePayConfig.getApi().getUrl(), sePayConfig.getAccount().getNumber(), limit);
            log.info("🔍 [DEBUG] Full URL: {}", fullUrl);

            // Gọi API SePay /transactions
            SePayTransactionDto[] transactions = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/transactions")
                            .queryParam("account_number", sePayConfig.getAccount().getNumber())
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            response -> {
                                log.error("❌ [DEBUG] SePay API 4xx Error: {}", response.statusCode());
                                return response.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            log.error("❌ [DEBUG] Error Body: {}", body);
                                            return response.createException();
                                        });
                            }
                    )
                    .onStatus(
                            status -> status.is5xxServerError(),
                            response -> {
                                log.error("❌ [DEBUG] SePay API 5xx Error: {}", response.statusCode());
                                return response.createException();
                            }
                    )
                    .bodyToMono(SePayTransactionDto[].class)
                    .block();

            List<SePayTransactionDto> result = new ArrayList<>();
            if (transactions != null && transactions.length > 0) {
                for (SePayTransactionDto tx : transactions) {
                    result.add(tx);
                }
            }
            
            log.info("✅ Fetched {} transactions from SePay", result.size());
            
            // 🔍 DEBUG: Log first 5 transactions for inspection
            if (!result.isEmpty()) {
                log.info("🔍 [DEBUG] Recent transactions (showing first 5):");
                int count = 0;
                for (SePayTransactionDto tx : result) {
                    if (count >= 5) break;
                    log.info("   TX #{}: Code={}, Content='{}', Amount={}", 
                            ++count, tx.getCode(), tx.getContent(), tx.getTransferAmount());
                }
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error fetching transactions from SePay", e);
            return new ArrayList<>();
        }
    }

    /**
     * 3. Kiểm tra xem có giao dịch nào match với invoice không
     * So khớp: nội dung chuyển khoản chứa "QLSACH [Invoice ID]"
     *          và số tiền >= total invoice
     * 
     * @param invoice Invoice cần kiểm tra
     * @return true nếu tìm thấy giao dịch match, false nếu không
     */
    public boolean verifyPaymentForInvoice(Invoice invoice) {
        try {
            log.info("🔍 [DEBUG] ========== VERIFY PAYMENT START ==========");
            log.info("🔍 [DEBUG] Invoice ID: {}", invoice.getId());
            log.info("🔍 [DEBUG] Expected amount: {} VND", invoice.getPrice().longValue());
            
            List<SePayTransactionDto> transactions = getRecentTransactions(50);
            
            String expectedContent = String.format("ORDER_%d", invoice.getId());
            log.info("🔍 [DEBUG] Looking for content: '{}'", expectedContent);
            log.info("🔍 [DEBUG] Total transactions to check: {}", transactions.size());
            
            for (SePayTransactionDto tx : transactions) {
                // Kiểm tra nội dung và số tiền
                String txContent = tx.getContent().toUpperCase().trim();
                boolean contentMatches = txContent.contains(expectedContent.toUpperCase());
                boolean amountMatches = tx.getTransferAmount() >= invoice.getPrice().longValue();
                
                log.debug("🔍 [DEBUG] Checking TX: Content='{}' ({}), Amount={} ({})",
                        tx.getContent(), contentMatches ? "✅ MATCH" : "❌ NO MATCH",
                        tx.getTransferAmount(), amountMatches ? "✅ OK" : "❌ LOW");
                
                if (contentMatches && amountMatches) {
                    log.info("✅ ✅ ✅ PAYMENT FOUND! Transaction: {} for invoice #{}", tx.getCode(), invoice.getId());
                    log.info("🔍 [DEBUG] ========== VERIFY PAYMENT SUCCESS ==========");
                    return true;
                }
            }
            
            log.warn("⏳ ⏳ ⏳ NO MATCHING TRANSACTION for invoice #{} (checked {} transactions)", 
                    invoice.getId(), transactions.size());
            log.info("🔍 [DEBUG] ========== VERIFY PAYMENT END (NOT FOUND) ==========");
            return false;
            
        } catch (Exception e) {
            log.error("❌ Error verifying payment for invoice #{}", invoice.getId(), e);
            return false;
        }
    }

    /**
     * 4. Tìm transaction code từ nội dung chuyển khoản
     * 
     * @param invoice Invoice cần tìm mã giao dịch
     * @return Mã giao dịch (transaction code) hoặc null nếu không tìm thấy
     */
    public String findTransactionCode(Invoice invoice) {
        try {
            List<SePayTransactionDto> transactions = getRecentTransactions(50);
            String expectedContent = String.format("ORDER_%d", invoice.getId());
            
            for (SePayTransactionDto tx : transactions) {
                String txContent = tx.getContent().toUpperCase().trim();
                boolean contentMatches = txContent.contains(expectedContent.toUpperCase());
                boolean amountMatches = tx.getTransferAmount() >= invoice.getPrice().longValue();
                
                if (contentMatches && amountMatches) {
                    log.info("✅ Found transaction code: {} for invoice #{}", tx.getCode(), invoice.getId());
                    return tx.getCode();
                }
            }
            
            log.debug("⏳ No transaction code found for invoice #{}", invoice.getId());
            return null;
            
        } catch (Exception e) {
            log.error("❌ Error finding transaction code for invoice #{}", invoice.getId(), e);
            return null;
        }
    }
    
    /**
     * Helper method: Map bank code to bank name
     * @param bankCode Mã ngân hàng (MB, VCB, TCB...)
     * @return Tên đầy đủ ngân hàng
     */
    private String getBankName(String bankCode) {
        return switch (bankCode.toUpperCase()) {
            case "MB", "MBB" -> "MB Bank (Quân Đội)";
            case "VCB" -> "Vietcombank";
            case "TCB", "TECHCOMBANK" -> "Techcombank";
            case "VTB", "VIETINBANK" -> "VietinBank";
            case "ACB" -> "ACB";
            case "BIDV" -> "BIDV";
            case "AGRIBANK", "ARB" -> "Agribank";
            case "SCB" -> "Sacombank";
            case "VPB", "VPBANK" -> "VPBank";
            case "TPB", "TPBANK" -> "TPBank";
            case "SHB", "SHBVN" -> "SHB";
            case "EIB", "EXIMBANK" -> "Eximbank";
            case "MSB" -> "MSB";
            case "OCB" -> "OCB";
            case "SEA", "SEABANK" -> "SeABank";
            case "VIETBANK", "VB", "VIET" -> "VietBank";
            case "VAB", "VIETABANK" -> "VietABank";
            case "NAB", "NAMABANK" -> "NamABank";
            case "PGB", "PGBANK" -> "PG Bank";
            case "ABB", "ABBANK" -> "ABBANK";
            case "NCB", "NCBANK" -> "NCB";
            case "GPB" -> "GP Bank";
            case "KLB" -> "Kiên Long Bank";
            case "LPB" -> "LienVietPostBank";
            case "BAB" -> "Bac A Bank";
            case "CAKE", "CAKE_BANK" -> "Cake by VPBank";
            case "UBANK" -> "Ubank by VPBank";
            case "WOO", "WOORI" -> "Woori Bank";
            case "CIMB" -> "CIMB Bank";
            case "HSBC" -> "HSBC Vietnam";
            default -> bankCode.toUpperCase() + " Bank";
        };
    }
}
