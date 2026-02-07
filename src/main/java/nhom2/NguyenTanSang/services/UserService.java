package nhom2.NguyenTanSang.services;

import nhom2.NguyenTanSang.constants.Provider;
import nhom2.NguyenTanSang.constants.Role;
import nhom2.NguyenTanSang.entities.User;
import nhom2.NguyenTanSang.repositories.IRoleRepository;
import nhom2.NguyenTanSang.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void save(@NotNull User user) {
        user.setPassword(new BCryptPasswordEncoder()
                .encode(user.getPassword()));
        userRepository.save(user);
    }

    /**
     * Tìm user theo username, trả về Optional
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Tìm user theo email, trả về Optional
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Load user cho Spring Security authentication
     * Implement từ UserDetailsService interface
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));
    }

    /**
     * Lưu user OAuth (Google login)
     * - Nếu email đã tồn tại: cập nhật provider
     * - Nếu username đã tồn tại: bỏ qua
     * - Nếu chưa tồn tại: tạo user mới
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void saveOauthUser(String email, @NotNull String username) {
        // Kiểm tra xem email đã tồn tại chưa
        Optional<User> existingUserByEmail = userRepository.findByEmail(email);
        if (existingUserByEmail.isPresent()) {
            // Email đã tồn tại, cập nhật provider nếu chưa có
            User user = existingUserByEmail.get();
            if (user.getProvider() == null || user.getProvider().isEmpty()) {
                user.setProvider(Provider.GOOGLE.value);
                userRepository.save(user);
            }
            return;
        }
        
        // Xử lý username trùng lặp: thêm số vào cuối nếu đã tồn tại
        String finalUsername = username;
        int counter = 1;
        while (userRepository.findByUsername(finalUsername).isPresent()) {
            finalUsername = username + counter;
            counter++;
            log.info("Username '{}' đã tồn tại, thử '{}'", username, finalUsername);
        }
        
        log.info("Tạo OAuth user mới với username: '{}', email: '{}'", finalUsername, email);
        
        // Tạo user mới
        var user = new User();
        user.setUsername(finalUsername);
        user.setEmail(email);
        user.setPassword(new BCryptPasswordEncoder().encode(finalUsername));
        user.setProvider(Provider.GOOGLE.value);
        user.getRoles().add(roleRepository.findRoleById(Role.USER.value));
        userRepository.save(user);
    }
    
    /**
     * Set default role (USER) cho user mới đăng ký
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void setDefaultRole(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.getRoles().add(roleRepository.findRoleById(Role.USER.value));
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
    
    /**
     * Lấy danh sách tất cả users với phân trang (cho admin)
     */
    public org.springframework.data.domain.Page<User> getAllUsers(int page, int size) {
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by("id").descending());
        return userRepository.findAll(pageable);
    }
    
    /**
     * Xóa user theo ID (cho admin)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    /**
     * Cập nhật thông tin user
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void updateUser(@NotNull User user) {
        userRepository.save(user);
    }
    
    /**
     * Tìm user theo username, trả về User (không phải Optional)
     */
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
    
    /**
     * Tìm user theo email, trả về User (không phải Optional)
     */
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}
