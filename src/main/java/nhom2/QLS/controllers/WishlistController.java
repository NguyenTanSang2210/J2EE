package nhom2.QLS.controllers;

import nhom2.QLS.entities.User;
import nhom2.QLS.entities.Wishlist;
import nhom2.QLS.repositories.IUserRepository;
import nhom2.QLS.services.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistService wishlistService;
    private final IUserRepository userRepository;
    
    /**
     * Helper method để lấy User từ Authentication (hỗ trợ cả form login và OAuth)
     */
    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        
        // Form login - principal là User entity
        if (principal instanceof User) {
            return (User) principal;
        }
        
        // OAuth login - principal là OidcUser hoặc OAuth2User
        String email = null;
        if (principal instanceof OidcUser) {
            email = ((OidcUser) principal).getEmail();
        } else if (principal instanceof OAuth2User) {
            email = ((OAuth2User) principal).getAttribute("email");
        }
        
        if (email != null) {
            return userRepository.findByEmail(email).orElse(null);
        }
        
        return null;
    }
    
    @GetMapping
    public String showWishlist(Authentication authentication, Model model) {
        User user = getUserFromAuthentication(authentication);
        
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Wishlist> wishlist = wishlistService.getWishlistByUser(user.getId());
        model.addAttribute("wishlist", wishlist);
        return "user/wishlist";
    }
    
    @PostMapping("/add/{bookId}")
    @ResponseBody
    public ResponseEntity<?> addToWishlist(
        @PathVariable Long bookId,
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = getUserFromAuthentication(authentication);
            
            if (user == null) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập để sử dụng chức năng này");
                return ResponseEntity.status(401).body(response);
            }
            
            wishlistService.addToWishlist(user.getId(), bookId);
            response.put("success", true);
            response.put("message", "Đã thêm vào danh sách yêu thích");
            response.put("count", wishlistService.getWishlistCount(user.getId()));
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @DeleteMapping("/remove/{bookId}")
    @ResponseBody
    public ResponseEntity<?> removeFromWishlist(
        @PathVariable Long bookId,
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = getUserFromAuthentication(authentication);
            
            if (user == null) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập để sử dụng chức năng này");
                return ResponseEntity.status(401).body(response);
            }
            
            wishlistService.removeFromWishlist(user.getId(), bookId);
            response.put("success", true);
            response.put("message", "Đã xóa khỏi danh sách yêu thích");
            response.put("count", wishlistService.getWishlistCount(user.getId()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/check/{bookId}")
    @ResponseBody
    public ResponseEntity<?> checkWishlist(
        @PathVariable Long bookId,
        Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        
        User user = getUserFromAuthentication(authentication);
        
        if (user == null) {
            response.put("inWishlist", false);
            return ResponseEntity.ok(response);
        }
        
        boolean inWishlist = wishlistService.isInWishlist(user.getId(), bookId);
        response.put("inWishlist", inWishlist);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<?> getWishlistCount(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        User user = getUserFromAuthentication(authentication);
        
        if (user == null) {
            response.put("count", 0);
            return ResponseEntity.ok(response);
        }
        
        long count = wishlistService.getWishlistCount(user.getId());
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
}
