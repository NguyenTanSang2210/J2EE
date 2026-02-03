package nhom2.QLS.controllers;

import nhom2.QLS.entities.User;
import nhom2.QLS.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @GetMapping("/login")
    public String login() {
        return "user/login";
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
        
        // Kiểm tra username đã tồn tại
        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("errors", new String[]{"Username '" + user.getUsername() + "' đã tồn tại. Vui lòng chọn username khác."});
            return "user/register";
        }
        
        // Kiểm tra email đã tồn tại
        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("errors", new String[]{"Email '" + user.getEmail() + "' đã được đăng ký. Vui lòng sử dụng email khác."});
            return "user/register";
        }
        
        // Kiểm tra phone đã tồn tại
        if (userService.existsByPhone(user.getPhone())) {
            model.addAttribute("errors", new String[]{"Số điện thoại '" + user.getPhone() + "' đã được đăng ký. Vui lòng sử dụng số khác."});
            return "user/register";
        }
        
        userService.save(user);
        userService.setDefaultRole(user.getUsername()); // Set role USER mặc định
        return "redirect:/login";
    }
}