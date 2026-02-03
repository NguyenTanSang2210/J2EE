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
        books.add(new Book(1L, "Lập trình Web Spring Framework", "Ánh Nguyễn", 29.99, techCategory));
        books.add(new Book(2L, "Lập trình ứng dụng Java", "Huy Cường", 45.63, techCategory));
        books.add(new Book(3L, "Lập trình Web Spring Boot", "Xuân Nhân", 12.0, techCategory));
        books.add(new Book(4L, "Lập trình Web Spring MVC", "Ánh Nguyễn", 0.12, techCategory));
        return books;
    }
}
