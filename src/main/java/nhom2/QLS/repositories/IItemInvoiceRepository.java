package nhom2.QLS.repositories;
import nhom2.QLS.entities.ItemInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface IItemInvoiceRepository extends
        JpaRepository<ItemInvoice, Long>{
}