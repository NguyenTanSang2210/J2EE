package nhom2.QLS.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhom2.QLS.dtos.SePayWebhookDto;
import nhom2.QLS.entities.Invoice;
import nhom2.QLS.repositories.IInvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller nhận Webhook từ SePay
 * Endpoint này sẽ được SePay gọi khi có giao dịch mới
 * 
 * URL Config trên SePay: https://yourdomain.com/api/webhooks/sepay
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    
    private final IInvoiceRepository invoiceRepository;
    
    /**
     * Nhận webhook từ SePay khi có giao dịch mới
     * POST /api/webhooks/sepay
     * 
     * @param webhook Dữ liệu giao dịch từ SePay
     * @return ResponseEntity with status
     */
    @PostMapping("/sepay")
    public ResponseEntity<Map<String, Object>> receiveSePayWebhook(@RequestBody SePayWebhookDto webhook) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("🔔 Received SePay webhook: {}", webhook);
            
            // Validate webhook data
            if (webhook.getTransferAmount() == null || webhook.getTransferAmount() <= 0) {
                log.warn("⚠️ Invalid webhook: amount is null or zero");
                response.put("success", false);
                response.put("message", "Invalid amount");
                return ResponseEntity.badRequest().body(response);
            }
            
            String content = webhook.getContent();
            if (content == null || content.trim().isEmpty()) {
                log.warn("⚠️ Invalid webhook: content is empty");
                response.put("success", false);
                response.put("message", "Invalid content");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Extract invoice ID from content: "ORDER_123" -> 123
            Long invoiceId = extractInvoiceId(content);
            if (invoiceId == null) {
                log.warn("⚠️ Cannot extract invoice ID from content: {}", content);
                response.put("success", false);
                response.put("message", "Invoice ID not found in content");
                return ResponseEntity.ok(response);
            }
            
            // Find invoice
            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
            if (invoice == null) {
                log.warn("⚠️ Invoice #{} not found", invoiceId);
                response.put("success", false);
                response.put("message", "Invoice not found");
                return ResponseEntity.ok(response);
            }
            
            // Check if already paid
            if ("PAID".equals(invoice.getPaymentStatus())) {
                log.info("ℹ️ Invoice #{} already paid", invoiceId);
                response.put("success", true);
                response.put("message", "Invoice already paid");
                return ResponseEntity.ok(response);
            }
            
            // Validate amount
            if (webhook.getTransferAmount() < invoice.getPrice().longValue()) {
                log.warn("⚠️ Amount mismatch: received {} but invoice requires {}", 
                    webhook.getTransferAmount(), invoice.getPrice());
                response.put("success", false);
                response.put("message", "Amount mismatch");
                return ResponseEntity.ok(response);
            }
            
            // Update invoice status
            invoice.setPaymentStatus("PAID");
            invoice.setTransactionCode(webhook.getCode());
            invoice.setPaidAt(new Date());
            invoice.setStatus("PROCESSING"); // Chuyển sang xử lý đơn hàng
            invoiceRepository.save(invoice);
            
            log.info("✅ Payment confirmed for invoice #{} via webhook. Transaction: {}", 
                invoiceId, webhook.getCode());
            
            response.put("success", true);
            response.put("message", "Payment confirmed");
            response.put("invoiceId", invoiceId);
            response.put("transactionCode", webhook.getCode());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error processing SePay webhook", e);
            response.put("success", false);
            response.put("message", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Extract Order/Invoice ID from payment transfer content
     * 
     * This method parses the transfer description to find the order ID.
     * SePay sends the exact content that customer typed when transferring money.
     * 
     * Supported formats:
     * - "ORDER_123" -> 123 (primary format)
     * - "ORDER 123" -> 123
     * - "QLSACH 123" -> 123 (legacy)
     * - "DH 456" -> 456
     * - "order123" -> 123 (case insensitive)
     * 
     * @param content The transfer description from bank transaction
     * @return Order ID as Long, or null if no valid ID found
     */
    private Long extractInvoiceId(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Pattern 1: "ORDER_123" or "ORDER 123" (primary format)
            Pattern orderPattern = Pattern.compile("ORDER[_\\s]*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher orderMatcher = orderPattern.matcher(content);
            if (orderMatcher.find()) {
                return Long.parseLong(orderMatcher.group(1));
            }
            
            // Pattern 2: "QLSACH 123" or "QLSACH123" (legacy format)
            Pattern qlsachPattern = Pattern.compile("QLSACH[_\\s]*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher qlsachMatcher = qlsachPattern.matcher(content);
            if (qlsachMatcher.find()) {
                return Long.parseLong(qlsachMatcher.group(1));
            }
            
            // Pattern 3: "DH 123" or "DH123"
            Pattern dhPattern = Pattern.compile("DH[_\\s]*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher dhMatcher = dhPattern.matcher(content);
            if (dhMatcher.find()) {
                return Long.parseLong(dhMatcher.group(1));
            }
            
            // Pattern 4: Any standalone number (fallback - risky but works as last resort)
            Pattern numberPattern = Pattern.compile("\\b(\\d+)\\b");
            Matcher numberMatcher = numberPattern.matcher(content);
            if (numberMatcher.find()) {
                long number = Long.parseLong(numberMatcher.group(1));
                // Sanity check: order IDs are usually positive and not too large
                if (number > 0 && number < 999999999) {
                    return number;
                }
            }
            
        } catch (NumberFormatException e) {
            log.warn("Failed to parse order ID from content: {}", content, e);
        }
        
        return null;
    }
    
    /**
     * Health check endpoint for webhook
     * GET /api/webhooks/sepay/health
     */
    @GetMapping("/sepay/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "SePay Webhook");
        response.put("timestamp", new Date().toString());
        return ResponseEntity.ok(response);
    }
}
