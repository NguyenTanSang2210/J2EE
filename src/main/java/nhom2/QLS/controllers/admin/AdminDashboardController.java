package nhom2.QLS.controllers.admin;

import lombok.RequiredArgsConstructor;
import nhom2.QLS.services.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminDashboardController {
    private final StatisticsService statisticsService;
    
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        // Overview stats
        model.addAttribute("totalRevenue", statisticsService.getTotalRevenue());
        model.addAttribute("totalOrders", statisticsService.getTotalCompletedOrders());
        model.addAttribute("totalUsers", statisticsService.getTotalUsers());
        model.addAttribute("totalBooks", statisticsService.getTotalBooks());
        
        // Best selling books
        model.addAttribute("bestSellingBooks", statisticsService.getBestSellingBooks(5));
        
        // Revenue by month (current year)
        int currentYear = LocalDate.now().getYear();
        model.addAttribute("revenueByMonth", statisticsService.getRevenueByMonth(currentYear));
        model.addAttribute("currentYear", currentYear);
        
        // Recent orders
        model.addAttribute("recentOrders", statisticsService.getRecentOrders(10));
        
        // Order status count
        model.addAttribute("orderStatusCount", statisticsService.getOrderStatusCount());
        
        return "admin/dashboard";
    }
}
