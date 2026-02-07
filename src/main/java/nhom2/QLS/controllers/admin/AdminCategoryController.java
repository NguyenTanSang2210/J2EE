package nhom2.QLS.controllers.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nhom2.QLS.entities.Category;
import nhom2.QLS.services.CategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminCategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public String listCategories(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        
        // Calculate statistics
        int totalBooks = categories.stream()
            .mapToInt(cat -> cat.getBooks() != null ? cat.getBooks().size() : 0)
            .sum();
        double averageBooks = categories.isEmpty() ? 0.0 : 
            (double) totalBooks / categories.size();
        
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("averageBooks", String.format("%.1f", averageBooks));
        
        return "admin/categories";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category-add";
    }

    @PostMapping("/add")
    public String addCategory(
            @Valid @ModelAttribute("category") Category category,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (result.hasErrors()) {
            return "admin/category-add";
        }

        try {
            categoryService.addCategory(category);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Thêm danh mục thành công!");
            return "redirect:/admin/categories";
        } catch (Exception e) {
            model.addAttribute("errorMessage", 
                "Có lỗi xảy ra: " + e.getMessage());
            return "admin/category-add";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Category category = categoryService.getCategoryById(id)
                .orElse(null);
        
        if (category == null) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Không tìm thấy danh mục!");
            return "redirect:/admin/categories";
        }
        
        model.addAttribute("category", category);
        return "admin/category-edit";
    }

    @PostMapping("/edit/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute("category") Category category,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (result.hasErrors()) {
            category.setId(id);
            return "admin/category-edit";
        }

        try {
            category.setId(id);
            categoryService.updateCategory(category);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Cập nhật danh mục thành công!");
            return "redirect:/admin/categories";
        } catch (Exception e) {
            model.addAttribute("errorMessage", 
                "Có lỗi xảy ra: " + e.getMessage());
            category.setId(id);
            return "admin/category-edit";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(
            @PathVariable Long id, 
            RedirectAttributes redirectAttributes
    ) {
        try {
            Category category = categoryService.getCategoryById(id)
                    .orElse(null);
            
            if (category == null) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Không tìm thấy danh mục!");
                return "redirect:/admin/categories";
            }

            // Check if category has books
            if (category.getBooks() != null && !category.getBooks().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Không thể xóa danh mục đang có sách! Vui lòng chuyển sách sang danh mục khác trước.");
                return "redirect:/admin/categories";
            }

            categoryService.deleteCategoryById(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Xóa danh mục thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return "redirect:/admin/categories";
    }
}
