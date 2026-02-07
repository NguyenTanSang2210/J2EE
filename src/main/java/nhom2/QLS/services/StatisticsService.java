package nhom2.QLS.services;

import lombok.RequiredArgsConstructor;
import nhom2.QLS.repositories.IBookRepository;
import nhom2.QLS.repositories.IInvoiceRepository;
import nhom2.QLS.repositories.IItemInvoiceRepository;
import nhom2.QLS.repositories.IUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final IInvoiceRepository invoiceRepository;
    private final IBookRepository bookRepository;
    private final IUserRepository userRepository;
    private final IItemInvoiceRepository itemInvoiceRepository;
    
    public Double getTotalRevenue() {
        Double revenue = invoiceRepository.sumTotalPriceByStatus("COMPLETED");
        return revenue != null ? revenue : 0.0;
    }
    
    public Long getTotalCompletedOrders() {
        Long count = invoiceRepository.countByStatus("COMPLETED");
        return count != null ? count : 0L;
    }
    
    public Long getTotalUsers() {
        return userRepository.count();
    }
    
    public Long getTotalBooks() {
        return bookRepository.count();
    }
    
    public List<Map<String, Object>> getBestSellingBooks(int limit) {
        // Return: [{bookTitle, totalSold, revenue}, ...]
        return itemInvoiceRepository.findBestSellingBooks(PageRequest.of(0, limit));
    }
    
    public Map<String, Double> getRevenueByMonth(int year) {
        // Return: {"01": 1000000, "02": 1500000, ...}
        Map<String, Double> result = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            String monthKey = String.format("%02d", month);
            Double revenue = invoiceRepository.sumRevenueByYearAndMonth(year, month);
            result.put(monthKey, revenue != null ? revenue : 0.0);
        }
        return result;
    }
    
    public List<Map<String, Object>> getRecentOrders(int limit) {
        return invoiceRepository.findRecentOrders(PageRequest.of(0, limit));
    }
    
    public Map<String, Long> getOrderStatusCount() {
        Map<String, Long> result = new HashMap<>();
        result.put("PENDING", invoiceRepository.countByStatus("PENDING"));
        result.put("PROCESSING", invoiceRepository.countByStatus("PROCESSING"));
        result.put("COMPLETED", invoiceRepository.countByStatus("COMPLETED"));
        result.put("CANCELLED", invoiceRepository.countByStatus("CANCELLED"));
        return result;
    }
}
