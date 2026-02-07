package nhom2.NguyenTanSang.controllers.admin;

import nhom2.NguyenTanSang.entities.Invoice;
import nhom2.NguyenTanSang.services.InvoiceService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/invoices")
@RequiredArgsConstructor
public class AdminInvoiceController {
    private final InvoiceService invoiceService;

    /**
     * Hiển thị danh sách tất cả đơn hàng (admin)
     */
    @GetMapping
    public String showAllOrders(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String status,
                               @NotNull Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("invoiceDate").descending());
        Page<Invoice> invoicesPage;

        if (status != null && !status.isEmpty()) {
            invoicesPage = invoiceService.getAllInvoices(pageable)
                    .map(invoice -> {
                        if (invoice.getStatus().equals(status)) {
                            return invoice;
                        }
                        return null;
                    });
        } else {
            invoicesPage = invoiceService.getAllInvoices(pageable);
        }

        model.addAttribute("invoices", invoicesPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", invoicesPage.getTotalPages());
        model.addAttribute("selectedStatus", status);

        // Thống kê
        model.addAttribute("pendingCount", invoiceService.countByStatus("PENDING"));
        model.addAttribute("processingCount", invoiceService.countByStatus("PROCESSING"));
        model.addAttribute("completedCount", invoiceService.countByStatus("COMPLETED"));
        model.addAttribute("cancelledCount", invoiceService.countByStatus("CANCELLED"));

        return "admin/orders";
    }

    /**
     * Chi tiết đơn hàng (admin có thể xem tất cả)
     */
    @GetMapping("/{id}")
    public String showOrderDetail(@PathVariable Long id,
                                  @NotNull Model model,
                                  RedirectAttributes redirectAttributes) {
        Invoice invoice = invoiceService.getInvoiceById(id)
                .orElse(null);

        if (invoice == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/admin/invoices";
        }

        model.addAttribute("invoice", invoice);
        model.addAttribute("isAdmin", true);
        return "invoice/order-detail";
    }

    /**
     * Cập nhật trạng thái đơn hàng
     */
    @PostMapping("/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                   @RequestParam String status,
                                   RedirectAttributes redirectAttributes) {
        try {
            invoiceService.updateInvoiceStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái đơn hàng");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cập nhật thất bại: " + e.getMessage());
        }

        return "redirect:/admin/invoices/" + id;
    }

    /**
     * Thống kê đơn hàng
     */
    @GetMapping("/statistics")
    public String showStatistics(@NotNull Model model) {
        // Thống kê theo trạng thái
        model.addAttribute("pendingCount", invoiceService.countByStatus("PENDING"));
        model.addAttribute("processingCount", invoiceService.countByStatus("PROCESSING"));
        model.addAttribute("completedCount", invoiceService.countByStatus("COMPLETED"));
        model.addAttribute("cancelledCount", invoiceService.countByStatus("CANCELLED"));

        // Thống kê doanh thu
        model.addAttribute("totalRevenue", invoiceService.getTotalRevenueByStatus("COMPLETED"));
        model.addAttribute("pendingRevenue", invoiceService.getTotalRevenueByStatus("PENDING"));
        model.addAttribute("processingRevenue", invoiceService.getTotalRevenueByStatus("PROCESSING"));

        return "admin/invoice-statistics";
    }
}
