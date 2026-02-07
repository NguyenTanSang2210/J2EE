package nhom2.NguyenTanSang.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhom2.NguyenTanSang.dtos.SePayQrCodeDto;
import nhom2.NguyenTanSang.entities.Invoice;
import nhom2.NguyenTanSang.repositories.IInvoiceRepository;
import nhom2.NguyenTanSang.services.SePayService;
import org.springframework.http.HttpStatus;
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
     * ⚠️ NOTE: SePay không cung cấp API để query transactions
     * - Endpoint này CHỈ check database status
     * - Payment status được cập nhật qua WEBHOOK khi SePay gửi notification
     * - Cần setup ngrok hoặc domain để nhận webhook
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
            
            // Check database status (updated by webhook)
            if ("PAID".equals(invoice.getPaymentStatus())) {
                response.put("isPaid", true);
                response.put("transactionCode", invoice.getTransactionCode());
                response.put("paidAt", invoice.getPaidAt());
                response.put("status", "PAID");
                log.debug("✅ Invoice #{} already paid (via webhook)", invoiceId);
            } else {
                response.put("isPaid", false);
                response.put("status", "PENDING");
                log.debug("⏳ Invoice #{} still pending - waiting for webhook notification", invoiceId);
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
     * Manual Payment Verification (For Development/Testing)
     * URL: POST /payment/verify-manual/123
     * 
     * Use this when:
     * - Webhook is not configured yet
     * - Testing payment flow
     * - Customer confirmed they transferred but webhook failed
     * 
     * @param invoiceId ID invoice to verify manually
     * @return JSON response with verification status
     */
    @PostMapping("/verify-manual/{invoiceId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyPaymentManually(@PathVariable Long invoiceId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + invoiceId));
            
            // Check if already paid
            if ("PAID".equals(invoice.getPaymentStatus())) {
                response.put("success", true);
                response.put("message", "Đơn hàng đã được thanh toán trước đó");
                response.put("alreadyPaid", true);
                return ResponseEntity.ok(response);
            }
            
            // Mark as paid manually
            invoice.setPaymentStatus("PAID");
            invoice.setTransactionCode("MANUAL_" + System.currentTimeMillis());
            invoice.setPaidAt(new Date());
            invoice.setStatus("PROCESSING"); // Chuyển sang xử lý đơn hàng
            invoiceRepository.save(invoice);
            
            log.info("✅ Manual payment verification successful for invoice #{}", invoiceId);
            
            response.put("success", true);
            response.put("message", "Xác nhận thanh toán thành công!");
            response.put("isPaid", true);
            response.put("transactionCode", invoice.getTransactionCode());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error in manual verification for invoice #{}", invoiceId, e);
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
