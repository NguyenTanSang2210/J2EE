package nhom2.QLS.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Entity
@Table(name = "wishlist", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "book_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", referencedColumnName = "id", nullable = false)
    private Book book;
    
    @Column(name = "added_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date addedDate = new Date();
}
