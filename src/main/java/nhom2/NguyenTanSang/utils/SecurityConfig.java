package nhom2.NguyenTanSang.utils;
import nhom2.NguyenTanSang.services.OAuthService;
import nhom2.NguyenTanSang.services.UserService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    private final OAuthService oAuthService;
    private final UserService userService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public UserDetailsService userDetailsService() {
        return userService;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var auth = new DaoAuthenticationProvider(userService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(@NotNull HttpSecurity http) throws Exception {
        return http
                // Disable CSRF only for REST API endpoints and wishlist operations
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**") // Allow all API endpoints (including webhooks)
                        .ignoringRequestMatchers("/wishlist/**") // Allow wishlist operations (AJAX calls)
                )
                
                // Configure authorization
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/webhooks/**").permitAll() // ⭐ Allow webhooks without authentication
                        .requestMatchers("/api/v1/books").permitAll() // Public cho test
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/", "/login", "/register", "/error").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        
                        // Admin only endpoints
                        .requestMatchers("/admin/**").hasAnyAuthority("ADMIN")
                        .requestMatchers("/books/edit/**", "/books/add", "/books/delete").hasAnyAuthority("ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN")
                        
                        // User and Admin endpoints - Web UI
                        .requestMatchers("/books", "/cart/**", "/wishlist/**").hasAnyAuthority("ADMIN", "USER")
                        
                        // API v1 endpoints - Require JWT authentication
                        .requestMatchers("/api/v1/**").hasAnyAuthority("ADMIN", "USER")
                        
                        // All other API endpoints
                        .requestMatchers("/api/**").hasAnyAuthority("ADMIN", "USER")
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                
                // Configure session management
                // IF_REQUIRED: Create session when needed (for form login)
                // But API requests with JWT won't create session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .expiredUrl("/login?expired")
                )
                
                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                
                // Configure form login (for web UI)
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                
                // Configure OAuth2 login
                .oauth2Login(
                        oauth2Login -> oauth2Login
                                .loginPage("/login")
                                .failureUrl("/login?error")
                                .userInfoEndpoint(userInfoEndpoint ->
                                        userInfoEndpoint
                                                .oidcUserService(oAuthService)
                                )
                                .successHandler((request, response, authentication) -> {
                                    // Log để debug authorities sau OAuth login
                                    System.out.println("\n========== OAuth Success Handler ==========");
                                    System.out.println("Authentication class: " + authentication.getClass().getName());
                                    System.out.println("Principal class: " + authentication.getPrincipal().getClass().getName());
                                    System.out.println("Authorities count: " + authentication.getAuthorities().size());
                                    authentication.getAuthorities().forEach(auth -> {
                                        System.out.println("  → Authority: " + auth.getAuthority());
                                    });
                                    
                                    var principal = authentication.getPrincipal();
                                    if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
                                        System.out.println("Email: " + oidcUser.getEmail());
                                        System.out.println("Name: " + oidcUser.getName());
                                    }
                                    System.out.println("========== End OAuth Success Handler ==========\n");
                                    
                                    // OAuthService đã xử lý việc tạo user, không cần gọi lại
                                    response.sendRedirect("/");
                                })
                                .permitAll()
                )
                
                // Configure logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                )
                
                // Configure exception handling
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.accessDeniedPage("/403")
                )
                
                // Build security filter chain
                .build();
    }
}