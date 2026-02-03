package nhom2.QLS.services;
import nhom2.QLS.entities.User;
import nhom2.QLS.repositories.IRoleRepository;
import nhom2.QLS.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void save(@NotNull User user) {
        // TODO: Encode password when Spring Security is enabled
        // user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    // Set default role cho user mới đăng ký
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void setDefaultRole(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.getRoles().add(roleRepository.findRoleById(1L)); // Role USER có id = 1
            userRepository.save(user);
        });
    }
    
    // Kiểm tra username đã tồn tại
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    // Kiểm tra email đã tồn tại
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    // Kiểm tra phone đã tồn tại
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }
}
