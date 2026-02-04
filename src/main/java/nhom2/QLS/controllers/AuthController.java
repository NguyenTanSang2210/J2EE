package nhom2.QLS.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhom2.QLS.dtos.AuthResponse;
import nhom2.QLS.dtos.LoginRequest;
import nhom2.QLS.dtos.RefreshTokenRequest;
import nhom2.QLS.dtos.RegisterRequest;
import nhom2.QLS.entities.User;
import nhom2.QLS.services.UserService;
import nhom2.QLS.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API Controller for Authentication
 * Handles JWT-based login, register, and token refresh
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * POST /api/auth/login
     * Authenticate user and return JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // Get user details
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            // Generate tokens
            String accessToken = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // Get user info
            User user = (User) userDetails;
            
            // Build response
            AuthResponse response = AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(86400000L) // 24 hours
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .roles(userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList()))
                    .build();

            log.info("User logged in successfully: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.error("Login failed for user: {}", loginRequest.getUsername());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * POST /api/auth/register
     * Register new user and return JWT tokens
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // Check if username already exists
            if (userService.existsByUsername(registerRequest.getUsername())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Username already exists");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Check if email already exists
            if (userService.existsByEmail(registerRequest.getEmail())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Email already exists");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Create new user
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(registerRequest.getPassword());
            user.setPhone(registerRequest.getPhone());

            // Save user (password will be encoded in service)
            userService.save(user);
            
            // Set default role
            userService.setDefaultRole(registerRequest.getUsername());

            // Load user details for token generation
            UserDetails userDetails = userService.loadUserByUsername(registerRequest.getUsername());

            // Generate tokens
            String accessToken = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // Build response
            AuthResponse response = AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(86400000L)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .roles(userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList()))
                    .build();

            log.info("User registered successfully: {}", registerRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/auth/refresh
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            
            // Validate refresh token
            if (jwtUtil.validateToken(refreshToken)) {
                String username = jwtUtil.extractUsername(refreshToken);
                UserDetails userDetails = userService.loadUserByUsername(username);

                // Generate new access token
                String newAccessToken = jwtUtil.generateToken(userDetails);

                // Build response
                Map<String, Object> response = new HashMap<>();
                response.put("accessToken", newAccessToken);
                response.put("tokenType", "Bearer");
                response.put("expiresIn", 86400000L);

                log.info("Token refreshed successfully for user: {}", username);
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid or expired refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Token refresh failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * GET /api/auth/me
     * Get current authenticated user info
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = (User) userDetails;

        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }
}
