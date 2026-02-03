package nhom2.QLS.repositories;
import nhom2.QLS.entities.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface IInvoiceRepository extends JpaRepository<Invoice,Long>{
}