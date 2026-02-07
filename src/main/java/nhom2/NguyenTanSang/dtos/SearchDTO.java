package nhom2.NguyenTanSang.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchDTO {
    private String keyword;        // Tìm trong title và author
    private Long categoryId;       // Lọc theo category
    private Double minPrice;       // Giá từ
    private Double maxPrice;       // Giá đến
    private String sortBy = "id";         // title, price, author
    private String sortDirection = "asc";  // asc, desc
}
