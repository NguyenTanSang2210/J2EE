package nhom2.NguyenTanSang.repositories;

import nhom2.NguyenTanSang.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IRoleRepository extends JpaRepository<Role, Long> {
    Role findRoleById(Long id);
    Role findByName(String name);
}
