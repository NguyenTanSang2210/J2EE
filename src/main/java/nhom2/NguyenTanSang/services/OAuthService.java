package nhom2.NguyenTanSang.services;

import nhom2.NguyenTanSang.entities.User;
import nhom2.NguyenTanSang.repositories.IUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

@Service
public class OAuthService extends OidcUserService {
    
    private final IUserRepository userRepository;
    private final UserService userService;
    
    public OAuthService(IUserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }
    
    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Load user từ Google (OIDC)
        OidcUser oidcUser = super.loadUser(userRequest);
        
        // Lấy email và thông tin từ Google
        String email = oidcUser.getEmail();
        String fullName = oidcUser.getAttribute("name"); // Tên đầy đủ
        String givenName = oidcUser.getAttribute("given_name"); // Tên
        String familyName = oidcUser.getAttribute("family_name"); // Họ
        
        // Ưu tiên lấy given_name (tên), nếu không có thì lấy full name, cuối cùng mới lấy từ email
        String username = (givenName != null && !givenName.trim().isEmpty()) 
            ? givenName.trim()
            : (fullName != null && !fullName.trim().isEmpty())
                ? fullName.trim()
                : email.split("@")[0];
        
        System.out.println("\n========== OAuth Login Start ==========");
        System.out.println("Email from Google: " + email);
        System.out.println("Full Name: " + fullName);
        System.out.println("Given Name: " + givenName);
        System.out.println("Family Name: " + familyName);
        System.out.println("Selected Username: " + username);
        
        // Tìm hoặc tạo user trong database
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // User chưa tồn tại, tạo mới với role USER
                    System.out.println("⚠️ Creating new OAuth user: " + email);
                    userService.saveOauthUser(email, username);
                    return userRepository.findByEmail(email)
                            .orElseThrow(() -> new OAuth2AuthenticationException("Failed to create user"));
                });
        
        // Log chi tiết để debug
        System.out.println("\n--- User Information ---");
        System.out.println("User ID: " + user.getId());
        System.out.println("Username: " + user.getUsername());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Provider: " + user.getProvider());
        System.out.println("Number of roles: " + user.getRoles().size());
        
        System.out.println("\n--- Roles Detail ---");
        user.getRoles().forEach(role -> {
            System.out.println("  → Role ID: " + role.getId() + ", Name: " + role.getName() + ", Authority: " + role.getAuthority());
        });
        
        System.out.println("\n--- Authorities ---");
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        System.out.println("Number of authorities: " + authorities.size());
        authorities.forEach(auth -> {
            System.out.println("  → Authority: " + auth.getAuthority());
        });
        
        // Tạo CustomOidcUser với authorities từ database
        CustomOidcUser customUser = new CustomOidcUser(oidcUser, user);
        
        System.out.println("\n--- CustomOidcUser Authorities ---");
        Collection<? extends GrantedAuthority> customAuthorities = customUser.getAuthorities();
        System.out.println("Number of authorities in CustomOidcUser: " + customAuthorities.size());
        customAuthorities.forEach(auth -> {
            System.out.println("  → Authority: " + auth.getAuthority());
        });
        
        System.out.println("========== OAuth Login End ==========\n");
        
        return customUser;
    }
    
    /**
     * Custom OidcUser để kết hợp OIDC attributes với authorities từ database
     */
    private static class CustomOidcUser implements OidcUser {
        private final OidcUser oidcUser;
        private final User user;
        
        public CustomOidcUser(OidcUser oidcUser, User user) {
            this.oidcUser = oidcUser;
            this.user = user;
        }
        
        @Override
        public Map<String, Object> getAttributes() {
            return oidcUser.getAttributes();
        }
        
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            // Trả về authorities từ User entity (đã có roles)
            return user.getAuthorities();
        }
        
        @Override
        public String getName() {
            // Trả về username từ User entity thay vì ID từ Google
            // Điều này quan trọng để Spring Security authentication.getName() hoạt động đúng
            return user.getUsername();
        }
        
        @Override
        public Map<String, Object> getClaims() {
            return oidcUser.getClaims();
        }
        
        @Override
        public org.springframework.security.oauth2.core.oidc.OidcUserInfo getUserInfo() {
            return oidcUser.getUserInfo();
        }
        
        @Override
        public org.springframework.security.oauth2.core.oidc.OidcIdToken getIdToken() {
            return oidcUser.getIdToken();
        }
    }
}