package nhom2.QLS.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhom2.QLS.dtos.SePayQrCodeDto;
import nhom2.QLS.entities.Invoice;
import nhom2.QLS.repositories.IInvoiceRepository;
import nhom2.QLS.services.SePayService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller xử lý thanh toán qua SePay
 * - Hiển thị trang QR Code
 * - API kiểm tra trạng thái thanh toán (AJAX)
 * - Hủy thanh toán
 */
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final SePayService sePayService;
    private final IInvoiceRepository invoiceRepository;

    /**
     * Hiển thị trang QR Code thanh toán
     * URL: /payment/qr/123
     * 
     * @param invoiceId ID invoice cần thanh toán
     * @param model Model chứa dữ liệu hiển thị
     * @return Template qr-payment.html
     */
    @GetMapping("/qr/{invoiceId}")
    public String showPaymentQr(@PathVariable Long invoiceId, Model model) {
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + invoiceId));
            
            log.info("📱 Showing QR payment page for invoice #{}", invoiceId);
            
            // Generate QR code
            SePayQrCodeDto qrCode = sePayService.generateQrCode(invoice);
            
            // Lưu QR URL vào invoice
            invoice.setQrCodeUrl(qrCode.getQrDataURL());
            invoiceRepository.save(invoice);
            
            model.addAttribute("invoice", invoice);
            model.addAttribute("qrCode", qrCode);
            
            return "payment/qr-payment";
            
        } catch (Exception e) {
            log.error("❌ Error showing QR payment for invoice #{}", invoiceId, e);
            model.addAttribute("error", "Không thể hiển thị trang thanh toán: " + e.getMessage());
            return "error/500";
        }
    }

    /**
     * API kiểm tra trạng thái thanh toán (AJAX polling)
     * URL: GET /payment/check/123
     * Frontend gọi API này mỗi 5 giây để check
     * 
     * @param invoiceId ID invoice cần kiểm tra
     * @return JSON {isPaid: true/false, transactionCode: "...", status: "..."}
     */
    @GetMapping("/check/{invoiceId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(@PathVariable Long invoiceId) {
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + invoiceId));
            
            Map<String, Object> response = new HashMap<>();
            
            // Nếu đã thanh toán rồi thì return luôn
            if ("PAID".equals(invoice.getPaymentStatus())) {
                response.put("isPaid", true);
                response.put("transactionCode", invoice.getTransactionCode());
                response.put("paidAt", invoice.getPaidAt());
                response.put("status", "PAID");
                log.debug("✅ Invoice #{} already paid", invoiceId);
                return ResponseEntity.ok(response);
            }
            
            // Kiểm tra giao dịch mới từ SePay
            log.debug("🔍 Checking payment status for invoice #{}", invoiceId);
            boolean isPaid = sePayService.verifyPaymentForInvoice(invoice);
            
            if (isPaid) {
                // Tìm mã giao dịch
                String txCode = sePayService.findTransactionCode(invoice);
                
                // Cập nhật trạng thái invoice
                invoice.setPaymentStatus("PAID");
                invoice.setTransactionCode(txCode);
                invoice.setPaidAt(new Date());
                invoice.setStatus("PROCESSING"); // Chuyển sang xử lý đơn hàng
                invoiceRepository.save(invoice);
                
                log.info("✅ Payment confirmed for invoice #{} with transaction {}", invoiceId, txCode);
                
                response.put("isPaid", true);
                response.put("transactionCode", txCode);
                response.put("status", "PAID");
            } else {
                response.put("isPaid", false);
                response.put("status", "PENDING");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error checking payment status for invoice #{}", invoiceId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("isPaid", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Hủy thanh toán
     * URL: POST /payment/cancel/123
     * 
     * @param invoiceId ID invoice cần hủy
     * @return Redirect về trang danh sách đơn hàng
     */
    @PostMapping("/cancel/{invoiceId}")
    public String cancelPayment(@PathVariable Long invoiceId) {
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + invoiceId));
            
            if ("PENDING".equals(invoice.getPaymentStatus())) {
                invoice.setStatus("CANCELLED");
                invoice.setPaymentStatus("FAILED");
                invoiceRepository.save(invoice);
                log.info("❌ Payment cancelled for invoice #{}", invoiceId);
            }
            
            return "redirect:/invoices";
            
        } catch (Exception e) {
            log.error("❌ Error cancelling payment for invoice #{}", invoiceId, e);
            return "redirect:/invoices";
        }
    }
}
