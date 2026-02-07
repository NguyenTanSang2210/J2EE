package nhom2.QLS.controllers.admin;

import lombok.RequiredArgsConstructor;
import nhom2.QLS.entities.User;
import nhom2.QLS.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminUserController {
    private final UserService userService;
    
    @GetMapping
    public String listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Model model
    ) {
        Page<User> users = userService.getAllUsers(page, size);
        model.addAttribute("users", users);
        return "admin/users";
    }
    
    @PostMapping("/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id) {
        // Toggle enabled/disabled (future implementation)
        return "redirect:/admin/users";
    }
    
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }
}
