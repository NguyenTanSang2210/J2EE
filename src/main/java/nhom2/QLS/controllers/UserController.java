package nhom2.QLS.controllers;

import nhom2.QLS.entities.User;
import nhom2.QLS.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @GetMapping("/login")
    public String login() {
        return "user/login.html";
    }
    
    @GetMapping("/register")
    public String register(@NotNull Model model) {
        model.addAttribute("user", new User());
        return "user/register";
    }
    
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user,
                           @NotNull BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            var errors = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toArray(String[]::new);
            model.addAttribute("errors", errors);
            return "user/register";
        }
        userService.save(user);
        userService.setDefaultRole(user.getUsername());
        return "redirect:/login";
    }
    
    @GetMapping("/user/profile")
    public String profile(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        String username = authentication.getName();
        User user = userService.findUserByUsername(username);
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        return "user/profile";
    }
    
    @PostMapping("/user/profile")
    public String updateProfile(@Valid @ModelAttribute("user") User updatedUser,
                               BindingResult bindingResult,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        String username = authentication.getName();
        User currentUser = userService.findUserByUsername(username);
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        if (bindingResult.hasFieldErrors("email")) {
            model.addAttribute("user", updatedUser);
            model.addAttribute("error", "Email không hợp lệ");
            return "user/profile";
        }
        
        // Check if email is already taken by another user
        User existingUser = userService.findUserByEmail(updatedUser.getEmail());
        if (existingUser != null && !existingUser.getId().equals(currentUser.getId())) {
            model.addAttribute("user", updatedUser);
            model.addAttribute("error", "Email này đã được sử dụng bởi tài khoản khác");
            return "user/profile";
        }
        
        // Update user information
        currentUser.setEmail(updatedUser.getEmail());
        currentUser.setPhone(updatedUser.getPhone());
        
        try {
            userService.updateUser(currentUser);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi cập nhật thông tin");
        }
        
        return "redirect:/user/profile";
    }
    
    @PostMapping("/user/change-password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        String username = authentication.getName();
        User user = userService.findUserByUsername(username);
        if (user == null) {
            return "redirect:/login";
        }
        
        // Validate current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu hiện tại không đúng");
            return "redirect:/user/profile";
        }
        
        // Validate new password
        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự");
            return "redirect:/user/profile";
        }
        
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
            return "redirect:/user/profile";
        }
        
        // Update password
        try {
            user.setPassword(passwordEncoder.encode(newPassword));
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi đổi mật khẩu");
        }
        
        return "redirect:/user/profile";
    }
}