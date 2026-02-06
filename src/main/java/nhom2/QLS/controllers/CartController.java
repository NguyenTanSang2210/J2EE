package nhom2.QLS.controllers;

import nhom2.QLS.entities.Invoice;
import nhom2.QLS.entities.User;
import nhom2.QLS.repositories.IUserRepository;
import nhom2.QLS.services.CartService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final IUserRepository userRepository;
    @GetMapping
    public String showCart(HttpSession session,
                           @NotNull Model model) {
        model.addAttribute("cart", cartService.getCart(session));
        model.addAttribute("totalPrice",
                cartService.getSumPrice(session));
        model.addAttribute("totalQuantity",
                cartService.getSumQuantity(session));
        return "book/cart";
    }
    @GetMapping("/removeFromCart/{id}")
    public String removeFromCart(HttpSession session,
                                 @PathVariable Long id) {
        var cart = cartService.getCart(session);
        cart.removeItems(id);
        return "redirect:/cart";
    }
    @GetMapping("/updateCart/{id}/{quantity}")
    public String updateCart(HttpSession session,
                             @PathVariable Long id,
                             @PathVariable Integer quantity) {
        var cart = cartService.getCart(session);
        cart.updateItems(id, quantity);
        return "book/cart";
    }
    @GetMapping("/clearCart")
    public String clearCart(HttpSession session) {
        cartService.removeCart(session);
        return "redirect:/cart ";
    }

    @GetMapping("/checkout")
    public String showCheckout(HttpSession session,
                               Principal principal,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        // Kiểm tra người dùng đã đăng nhập chưa
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", 
                "Vui lòng đăng nhập để thanh toán!");
            return "redirect:/login";
        }
        
        var cart = cartService.getCart(session);
        
        // Check if cart is empty
        if (cart.getCartItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống! Vui lòng thêm sản phẩm trước khi thanh toán.");
            return "redirect:/cart";
        }
        
        // Lấy user mới nhất từ database để đảm bảo có đầy đủ thông tin (email, phone, etc.)
        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getName()));
        
        model.addAttribute("cart", cart);
        model.addAttribute("totalPrice", cartService.getSumPrice(session));
        model.addAttribute("totalQuantity", cartService.getSumQuantity(session));
        model.addAttribute("currentUser", currentUser);
        
        return "invoice/checkout";
    }
    
    @PostMapping("/checkout")
    public String processCheckout(HttpSession session,
                                  Principal principal,
                                  @RequestParam String receiverName,
                                  @RequestParam String accountEmail, // Email tài khoản (readonly)
                                  @RequestParam(required = false) String receiverEmail, // Email người nhận (optional)
                                  @RequestParam String phone,
                                  @RequestParam String shippingAddress,
                                  @RequestParam String paymentMethod,
                                  @RequestParam(required = false) String note,
                                  RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra người dùng đã đăng nhập chưa
            if (principal == null) {
                redirectAttributes.addFlashAttribute("error", 
                    "Vui lòng đăng nhập để đặt hàng!");
                return "redirect:/login";
            }
            
            var cart = cartService.getCart(session);
            
            // Check if cart is empty
            if (cart.getCartItems().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống!");
                return "redirect:/cart";
            }
            
            // Fetch managed User from database to ensure user_id is saved correctly
            User managedUser = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getName()));
            
            // Validate accountEmail matches user's email (security check)
            if (!managedUser.getEmail().equals(accountEmail)) {
                redirectAttributes.addFlashAttribute("error", 
                    "Email tài khoản không khớp! Vui lòng thử lại.");
                return "redirect:/cart/checkout";
            }
            
            // Nếu receiverEmail rỗng, dùng accountEmail
            String finalReceiverEmail = (receiverEmail != null && !receiverEmail.trim().isEmpty()) 
                    ? receiverEmail.trim() 
                    : managedUser.getEmail();
            
            // Create invoice object with shipping info
            Invoice invoice = Invoice.builder()
                    .receiverName(receiverName)
                    .email(managedUser.getEmail()) // LUÔN dùng email từ DB để link user
                    .receiverEmail(finalReceiverEmail) // Email người nhận (có thể khác)
                    .phone(phone)
                    .shippingAddress(shippingAddress)
                    .paymentMethod(paymentMethod)
                    .note(note)
                    .user(managedUser)
                    .status("PENDING")
                    .paymentStatus("PENDING") // Khởi tạo payment status
                    .build();
            
            // Save cart with shipping info
            cartService.saveCart(session, invoice);
            
            // ⭐ SEPAY INTEGRATION: Redirect to QR payment page for bank transfer
            if ("BANK_TRANSFER".equals(paymentMethod)) {
                return "redirect:/payment/qr/" + invoice.getId();
            }
            
            redirectAttributes.addFlashAttribute("success", 
                "Đặt hàng thành công! Mã đơn hàng: #" + invoice.getId());
            return "redirect:/invoices";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Đặt hàng thất bại: " + e.getMessage());
            return "redirect:/cart/checkout";
        }
    }
}