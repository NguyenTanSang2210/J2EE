package nhom2.QLS.services;

import nhom2.QLS.entities.User;
import nhom2.QLS.repositories.IUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
public class OAuthService extends OidcUserService {
    
    private final IUserRepository userRepository;
    
    public OAuthService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Load user từ Google (OIDC)
        OidcUser oidcUser = super.loadUser(userRequest);
        
        // Lấy email từ Google
        String email = oidcUser.getEmail();
        
        // Tìm user trong database theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new OAuth2AuthenticationException("User not found with email: " + email));
        
        // Log để debug
        System.out.println("=== OAuth Login Debug ===");
        System.out.println("Email: " + email);
        System.out.println("User roles: " + user.getRoles());
        user.getRoles().forEach(role -> {
            System.out.println("Role name: " + role.getName() + ", Authority: " + role.getAuthority());
        });
        System.out.println("Authorities from User: " + user.getAuthorities());
        System.out.println("=========================");
        
        // Trả về CustomOidcUser kết hợp OIDC attributes và authorities từ database
        return new CustomOidcUser(oidcUser, user);
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
            return oidcUser.getName();
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