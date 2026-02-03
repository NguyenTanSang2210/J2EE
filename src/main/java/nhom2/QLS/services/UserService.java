package nhom2.QLS.services;
import nhom2.QLS.entities.User;
import nhom2.QLS.repositories.IRoleRepository;
import nhom2.QLS.repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

@Service
@Slf4j
public class UserService implements UserDetailsService {
    @Autowired
    private IUserRepository userRepository;
    
    @Autowired
    private IRoleRepository roleRepository;
    
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public UserService(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void save(@NotNull User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getAuthorities())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
    
    // Kiểm tra username đã tồn tại
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
    
    // Kiểm tra email đã tồn tại
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
    
    // Kiểm tra phone đã tồn tại
    public boolean existsByPhone(String phone) {
        return userRepository.findByPhone(phone).isPresent();
    }
    
    // Lưu user từ Google OAuth
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void saveOAuthUser(String email, String name) {
        // Kiểm tra xem user đã tồn tại chưa
        if (userRepository.findByEmail(email).isPresent()) {
            return; // User đã tồn tại, không cần tạo mới
        }
        
        // Tạo user mới từ Google account
        User user = new User();
        user.setUsername(email); // Dùng email làm username
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(email)); // Password random, user không cần biết
        user.setPhone("0000000000"); // Phone mặc định
        userRepository.save(user);
        
        // Set default role cho OAuth user
        setDefaultRole(user.getUsername());
        
        log.info("Created new OAuth user: {}", email);
    }
}

