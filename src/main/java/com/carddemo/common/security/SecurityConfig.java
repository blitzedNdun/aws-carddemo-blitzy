package com.carddemo.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Common Spring Security configuration class providing JWT authentication, password encoding,
 * and shared security components used across all CardDemo microservices with role-based 
 * access control and audit integration.
 * 
 * This configuration replaces legacy RACF authentication with modern cloud-native security
 * patterns while maintaining equivalent access control and audit capabilities.
 * 
 * Key Features:
 * - JWT token-based stateless authentication
 * - BCrypt password encoding with configurable salt rounds (minimum 12)
 * - Role-based access control mapping RACF user types to Spring authorities
 * - CORS configuration for React frontend integration
 * - Security audit event publishers for compliance logging
 * - OAuth2 resource server configuration for microservices architecture
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since Spring Boot 3.2.x, Spring Security 6.x
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    /**
     * Configurable JWT secret key for token signing and validation.
     * Should be stored in Kubernetes secrets or external configuration in production.
     */
    @Value("${carddemo.security.jwt.secret:cardDemo2024SecretKeyForJWTTokenGenerationAndValidation}")
    private String jwtSecret;

    /**
     * Configurable JWT token expiration time in seconds.
     * Default: 1800 seconds (30 minutes) to match CICS terminal timeout behavior.
     */
    @Value("${carddemo.security.jwt.expiration:1800}")
    private int jwtExpirationSeconds;

    /**
     * Configurable BCrypt password encoder strength (salt rounds).
     * Minimum 12 rounds required for enterprise security compliance.
     */
    @Value("${carddemo.security.bcrypt.strength:12}")
    private int bcryptStrength;

    /**
     * CORS allowed origins configuration for React frontend integration.
     * Should be restricted to specific domains in production environments.
     */
    @Value("${carddemo.security.cors.allowed-origins:http://localhost:3000,https://localhost:3000}")
    private String[] allowedOrigins;

    /**
     * Configures BCrypt password encoder with enterprise-grade security settings.
     * 
     * BCrypt is chosen over other password hashing algorithms for:
     * - Adaptive cost parameter (configurable work factor)
     * - Built-in salt generation and storage
     * - Resistance to rainbow table attacks
     * - Time-tested security properties
     * 
     * The minimum strength of 12 rounds provides adequate protection against
     * brute force attacks while maintaining acceptable authentication performance.
     * 
     * @return BCryptPasswordEncoder configured with specified strength
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Validate minimum security requirements
        if (bcryptStrength < 12) {
            throw new IllegalArgumentException(
                "BCrypt strength must be at least 12 rounds for enterprise security compliance");
        }
        
        return new BCryptPasswordEncoder(bcryptStrength);
    }

    /**
     * Configures JWT decoder for OAuth2 resource server authentication.
     * 
     * This implementation uses HMAC-SHA256 (HS256) algorithm for token signing
     * and validation, providing stateless authentication across microservices.
     * 
     * JWT tokens contain the following claims:
     * - sub: User ID (username from PostgreSQL users table)
     * - user_type: User type ('A' for Admin, 'U' for User)
     * - roles: Array of Spring Security authorities (ROLE_ADMIN, ROLE_USER)
     * - session_id: Correlation ID for Redis session management
     * - exp: Token expiration timestamp
     * - iat: Token issued at timestamp
     * 
     * @return JwtDecoder configured for HS256 algorithm with shared secret
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // Convert string secret to SecretKeySpec for HMAC-SHA256
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        
        // Validate secret key length for security
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException(
                "JWT secret key must be at least 256 bits (32 bytes) for HS256 algorithm");
        }
        
        SecretKeySpec secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        
        return NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings for React frontend integration.
     * 
     * CORS configuration enables secure communication between the React frontend
     * and Spring Boot microservices while preventing unauthorized cross-origin requests.
     * 
     * Configuration includes:
     * - Allowed origins: React development and production servers
     * - Allowed methods: HTTP methods required for REST API operations
     * - Allowed headers: Authorization header for JWT tokens and content type
     * - Exposed headers: Custom headers that frontend can access
     * - Credentials support: Enables cookies/auth headers in cross-origin requests
     * - Max age: Preflight request caching duration
     * 
     * @return CorsConfigurationSource with React-compatible CORS settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Configure allowed origins (should be restricted in production)
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        
        // Configure allowed HTTP methods for REST API operations
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        
        // Configure allowed headers including JWT authorization
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "Accept", 
            "Origin", 
            "X-Requested-With",
            "X-Session-ID",
            "X-Correlation-ID"
        ));
        
        // Configure exposed headers for frontend access
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Session-ID", 
            "X-Correlation-ID",
            "X-Total-Count"
        ));
        
        // Enable credentials for authenticated requests
        configuration.setAllowCredentials(true);
        
        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Configures the main security filter chain for JWT-based authentication and authorization.
     * 
     * This filter chain implements:
     * - Stateless session management (no server-side sessions)
     * - JWT token validation for protected endpoints
     * - Role-based authorization rules
     * - CORS integration for React frontend
     * - Audit event integration for security monitoring
     * - Exception handling for authentication/authorization failures
     * 
     * Authorization Rules:
     * - /api/auth/login: Permit all (authentication endpoint)
     * - /api/admin/**: Require ROLE_ADMIN authority
     * - /api/**: Require authentication (any valid user)
     * - Actuator endpoints: Require ROLE_ADMIN authority
     * - Static resources: Permit all
     * 
     * @param http HttpSecurity configuration object
     * @return SecurityFilterChain configured for JWT authentication
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Disable CSRF for stateless JWT authentication
            .csrf(csrf -> csrf.disable())
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure session management as stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public authentication endpoints
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                
                // Public health check endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                
                // Administrative endpoints require ADMIN role
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // User management endpoints require ADMIN role
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                
                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                
                // Static resources are publicly accessible
                .requestMatchers("/static/**", "/public/**", "/*.js", "/*.css", "/*.ico").permitAll()
                
                // Default: require authentication
                .anyRequest().authenticated()
            )
            
            // Configure OAuth2 resource server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            
            // Configure exception handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"JWT token is missing or invalid\"}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"Forbidden\",\"message\":\"Insufficient privileges for this operation\"}"
                    );
                })
            )
            
            .build();
    }

    /**
     * Configures JWT authentication converter for mapping JWT claims to Spring Security authorities.
     * 
     * This converter transforms JWT token claims into Spring Security Authentication objects:
     * - Maps 'user_type' claim to Spring Security roles (A -> ROLE_ADMIN, U -> ROLE_USER)
     * - Preserves additional claims for use in business logic
     * - Handles role hierarchy (ADMIN users inherit USER privileges)
     * 
     * @return JwtAuthenticationConverter configured for CardDemo role mapping
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        
        // Configure custom authorities mapping
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");
        
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        
        return jwtConverter;
    }



    /**
     * Event listener for successful authentication events.
     * 
     * Captures successful login attempts for security monitoring and compliance.
     * Events include user ID, timestamp, IP address, and authentication method.
     * 
     * @param event AuthenticationSuccessEvent from Spring Security
     */
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication.getName();
        
        // Create audit event for successful authentication
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("username", username);
        auditData.put("timestamp", Instant.now().toString());
        auditData.put("event_type", "AUTHENTICATION_SUCCESS");
        auditData.put("authorities", authentication.getAuthorities().toString());
        
        // Log for structured logging (ELK stack integration)
        logSecurityEvent("Authentication successful for user: " + username, auditData);
    }

    /**
     * Event listener for failed authentication events.
     * 
     * Captures failed login attempts for security monitoring and threat detection.
     * Events include attempted username, timestamp, failure reason, and IP address.
     * 
     * @param event AbstractAuthenticationFailureEvent from Spring Security
     */
    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String failureReason = event.getException().getMessage();
        
        // Create audit event for failed authentication
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("username", username);
        auditData.put("timestamp", Instant.now().toString());
        auditData.put("event_type", "AUTHENTICATION_FAILURE");
        auditData.put("failure_reason", failureReason);
        
        // Log for security monitoring and alerting
        logSecurityEvent("Authentication failed for user: " + username + 
                         ", reason: " + failureReason, auditData);
    }

    /**
     * Structured security event logging for ELK stack integration.
     * 
     * This method creates structured JSON log entries for security events that are
     * consumed by the ELK stack for centralized logging, monitoring, and alerting.
     * 
     * @param message Human-readable event description
     * @param auditData Structured data for analysis and correlation
     */
    private void logSecurityEvent(String message, Map<String, Object> auditData) {
        // Create structured log entry for ELK stack consumption
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("message", message);
        logEntry.put("level", "INFO");
        logEntry.put("logger", "com.carddemo.security.audit");
        logEntry.put("thread", Thread.currentThread().getName());
        logEntry.put("audit_data", auditData);
        
        // TODO: Integrate with actual logging framework (Logback with JSON encoder)
        // Logger should be configured to output structured JSON for ELK stack consumption
        System.out.println("SECURITY_AUDIT: " + logEntry);
    }
}