package nhom2.NguyenTanSang.controllers;

import nhom2.NguyenTanSang.daos.Cart;
import nhom2.NguyenTanSang.entities.Invoice;
import nhom2.NguyenTanSang.entities.User;
import nhom2.NguyenTanSang.services.CartService;
import nhom2.NguyenTanSang.services.InvoiceService;
import nhom2.NguyenTanSang.services.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final CartService cartService;
    private final UserService userService;

    /**
     * Lấy user hiện tại từ SecurityContext
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }
        
        Object principal = authentication.getPrincipal();
        
        // Nếu là OAuth2User (đăng nhập bằng Google)
        if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            return userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"));
        }
        
        // Nếu là UserDetails (đăng nhập bằng form)
        String username = authentication.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"));
    }

    /**
     * Hiển thị danh sách đơn hàng của user hiện tại
     */
    @GetMapping
    public String showMyOrders(@NotNull Model model) {
        User user = getCurrentUser();
        List<Invoice> invoices = invoiceService.getInvoicesByUser(user.getId());
        
        // Tính toán thống kê
        long pendingCount = invoices.stream().filter(inv -> "PENDING".equals(inv.getStatus())).count();
        long processingCount = invoices.stream().filter(inv -> "PROCESSING".equals(inv.getStatus())).count();
        long completedCount = invoices.stream().filter(inv -> "COMPLETED".equals(inv.getStatus())).count();
        
        model.addAttribute("invoices", invoices);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("processingCount", processingCount);
        model.addAttribute("completedCount", completedCount);
        return "invoice/my-orders";
    }

    /**
     * Hiển thị chi tiết đơn hàng
     */
    @GetMapping("/{id}")
    public String showOrderDetail(@PathVariable Long id,
                                  @NotNull Model model,
                                  RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        Invoice invoice = invoiceService.getInvoiceById(id)
                .orElse(null);

        if (invoice == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/invoices";
        }

        // Kiểm tra quyền truy cập
        if (!invoiceService.canUserAccessInvoice(id, user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xem đơn hàng này");
            return "redirect:/invoices";
        }

        model.addAttribute("invoice", invoice);
        return "invoice/order-detail";
    }

    /**
     * Xử lý checkout từ giỏ hàng
     */
    @PostMapping("/checkout")
    public String checkout(HttpSession session,
                          @RequestParam String paymentMethod,
                          @RequestParam String shippingAddress,
                          @RequestParam String phone,
                          @RequestParam(required = false) String note,
                          RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();
            Cart cart = cartService.getCart(session);

            if (cart.getCartItems().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
                return "redirect:/cart";
            }

            // Validate input
            if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng nhập địa chỉ giao hàng");
                return "redirect:/cart";
            }

            if (phone == null || phone.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng nhập số điện thoại");
                return "redirect:/cart";
            }

            // Tạo hóa đơn
            Invoice invoice = invoiceService.createInvoiceFromCart(
                    user, cart, paymentMethod, shippingAddress, phone, note
            );

            // Xóa giỏ hàng
            cartService.removeCart(session);

            // Redirect đến trang thành công
            redirectAttributes.addFlashAttribute("invoiceId", invoice.getId());
            redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công!");
            return "redirect:/invoices/checkout-success";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Đặt hàng thất bại: " + e.getMessage());
            return "redirect:/cart";
        }
    }

    /**
     * Trang xác nhận đặt hàng thành công
     */
    @GetMapping("/checkout-success")
    public String checkoutSuccess(Model model) {
        // Flash attributes sẽ tự động được thêm vào model
        return "invoice/checkout-success";
    }

    /**
     * Hủy đơn hàng (chỉ được hủy khi status = PENDING)
     */
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();
            // Kiểm tra quyền
            if (!invoiceService.canUserAccessInvoice(id, user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền hủy đơn hàng này");
                return "redirect:/invoices";
            }

            invoiceService.cancelInvoice(id);
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng thành công");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/invoices/" + id;
    }
}
