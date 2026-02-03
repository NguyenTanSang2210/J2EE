package nhom2.QLS;
import nhom2.QLS.entities.Book;
import nhom2.QLS.entities.Category;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;
@Configuration
public class AppConfig {
    @Bean
    public List<Book> getBooks() {
        List<Book> books = new ArrayList<>();
        
        // Tạo category
        Category techCategory = new Category();
        techCategory.setId(1L);
        techCategory.setName("Công nghệ thông tin");
        
        // Tạo books với category
        books.add(Book.builder()
                .id(1L)
                .title("Lập trình Web Spring Framework")
                .author("Ánh Nguyễn")
                .price(29.99)
                .category(techCategory)
                .itemInvoices(new ArrayList<>())
                .build());
        books.add(Book.builder()
                .id(2L)
                .title("Lập trình ứng dụng Java")
                .author("Huy Cường")
                .price(45.63)
                .category(techCategory)
                .itemInvoices(new ArrayList<>())
                .build());
        books.add(Book.builder()
                .id(3L)
                .title("Lập trình Web Spring Boot")
                .author("Xuân Nhân")
                .price(12.0)
                .category(techCategory)
                .itemInvoices(new ArrayList<>())
                .build());
        books.add(Book.builder()
                .id(4L)
                .title("Lập trình Web Spring MVC")
                .author("Ánh Nguyễn")
                .price(0.12)
                .category(techCategory)
                .itemInvoices(new ArrayList<>())
                .build());
        return books;
    }
}
