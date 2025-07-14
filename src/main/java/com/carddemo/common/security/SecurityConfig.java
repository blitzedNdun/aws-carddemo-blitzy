package com.carddemo.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.listener.AuditListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Common Spring Security configuration class providing JWT authentication, password encoding,
 * and shared security components used across all CardDemo microservices.
 * 
 * This configuration implements:
 * - JWT token-based authentication replacing RACF authentication
 * - BCrypt password encoding with configurable salt rounds (minimum 12)
 * - Role-based access control mapping RACF user types to Spring authorities
 * - CORS settings for React frontend integration
 * - Security audit event publishers for compliance logging
 * - OAuth2 resource server configuration for stateless microservices
 * 
 * Integrates with Spring Cloud Gateway, Redis session management, and PostgreSQL
 * authentication backend as part of the cloud-native CardDemo architecture.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    /**
     * JWT secret key for token signing and validation.
     * In production, this should be externalized to Kubernetes secrets or vault.
     */
    @Value("${carddemo.security.jwt.secret:cardDemo2024SecretKeyForJWTAuthentication}")
    private String jwtSecret;

    /**
     * JWT token expiration time in seconds.
     * Default 30 minutes equivalent to CICS terminal timeout.
     */
    @Value("${carddemo.security.jwt.expiration:1800}")
    private long jwtExpirationInSeconds;

    /**
     * BCrypt password encoder strength (salt rounds).
     * Minimum 12 rounds as per PCI DSS requirements.
     */
    @Value("${carddemo.security.bcrypt.strength:12}")
    private int bcryptStrength;

    /**
     * React frontend URL for CORS configuration.
     * Default supports local development environment.
     */
    @Value("${carddemo.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Additional allowed origins for CORS configuration.
     * Supports multiple deployment environments.
     */
    @Value("${carddemo.cors.allowed-origins:}")
    private List<String> additionalAllowedOrigins;

    /**
     * Configure BCrypt password encoder with configurable salt rounds.
     * 
     * BCrypt provides secure password hashing with built-in salt generation,
     * replacing plain-text password storage from legacy RACF system.
     * Minimum 12 salt rounds ensures PCI DSS compliance for cardholder data protection.
     * 
     * @return BCryptPasswordEncoder configured with specified strength
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        // Ensure minimum security strength of 12 rounds
        int strength = Math.max(bcryptStrength, 12);
        
        // BCrypt encoder provides:
        // - Adaptive cost function (configurable work factor)
        // - Built-in salt generation for each password
        // - Resistance against rainbow table attacks
        // - OWASP recommended for password storage
        return new BCryptPasswordEncoder(strength);
    }

    /**
     * Configure JWT decoder for OAuth2 resource server authentication.
     * 
     * Implements stateless JWT token validation using HMAC SHA-256 algorithm,
     * enabling horizontal scaling of microservices without shared session state.
     * Tokens contain user identity, roles, and session correlation information.
     * 
     * @return JwtDecoder configured for HMAC SHA-256 token validation
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // Create HMAC SHA-256 secret key from configured secret
        SecretKeySpec secretKey = new SecretKeySpec(
            jwtSecret.getBytes(StandardCharsets.UTF_8), 
            "HmacSHA256"
        );
        
        // Configure Nimbus JWT decoder with:
        // - HMAC SHA-256 signature verification
        // - Automatic token expiration validation
        // - Claims extraction for Spring Security context
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    /**
     * Configure CORS settings for React frontend integration.
     * 
     * Enables cross-origin requests from React application running on different
     * port/domain than Spring Boot microservices. Supports both development
     * (localhost:3000) and production environments with configurable origins.
     * 
     * @return CorsConfigurationSource with React-compatible settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Configure allowed origins for React frontend
        // Support both configured frontend URL and additional origins
        if (additionalAllowedOrigins != null && !additionalAllowedOrigins.isEmpty()) {
            configuration.setAllowedOrigins(additionalAllowedOrigins);
        } else {
            configuration.setAllowedOrigins(Arrays.asList(frontendUrl));
        }
        
        // Allow all HTTP methods needed for REST API operations
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        
        // Allow essential headers for JWT authentication and content negotiation
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",           // JWT Bearer token
            "Content-Type",           // JSON content type
            "Accept",                 // Content negotiation
            "Origin",                 // CORS origin header
            "Access-Control-Request-Method",   // CORS preflight
            "Access-Control-Request-Headers",  // CORS preflight
            "X-Requested-With",       // AJAX identification
            "X-Correlation-ID"        // Request tracing
        ));
        
        // Expose headers needed by React frontend
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",          // JWT token refresh
            "X-Correlation-ID",       // Request correlation
            "X-Total-Count"          // Pagination metadata
        ));
        
        // Allow credentials for session-based operations
        configuration.setAllowCredentials(true);
        
        // Cache preflight requests for 1 hour to improve performance
        configuration.setMaxAge(3600L);
        
        // Apply CORS configuration to all API endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/actuator/**", configuration);
        
        return source;
    }

    /**
     * Configure main security filter chain for OAuth2 resource server.
     * 
     * Implements comprehensive security configuration including:
     * - JWT token-based authentication (stateless)
     * - Role-based authorization with @PreAuthorize support
     * - CORS configuration for React frontend
     * - Security headers and CSRF protection
     * - Public endpoint access for health checks and authentication
     * 
     * @param http HttpSecurity configuration object
     * @return SecurityFilterChain configured for microservices architecture
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Configure CORS with defined configuration source
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Disable CSRF for stateless API (JWT tokens provide CSRF protection)
            .csrf(csrf -> csrf.disable())
            
            // Configure session management as stateless for microservices
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints (no authentication required)
                .requestMatchers(
                    "/api/auth/login",           // Authentication endpoint
                    "/api/auth/refresh",         // Token refresh endpoint
                    "/actuator/health",          // Health check endpoint
                    "/actuator/info",            // Application info
                    "/swagger-ui/**",            // API documentation
                    "/v3/api-docs/**"           // OpenAPI specification
                ).permitAll()
                
                // Admin-only endpoints require ROLE_ADMIN
                .requestMatchers(
                    "/api/admin/**",             // Administrative functions
                    "/api/users/**",             // User management
                    "/actuator/**"               // Management endpoints
                ).hasRole("ADMIN")
                
                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                
                // Allow all other requests (static resources, etc.)
                .anyRequest().permitAll()
            )
            
            // Configure OAuth2 resource server for JWT token validation
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    // Convert JWT claims to Spring Security authorities
                    .jwtAuthenticationConverter(jwtAuthenticationToken -> {
                        // Extract user roles from JWT claims and convert to authorities
                        // This enables @PreAuthorize annotations to work with role-based access control
                        return new JwtAuthenticationConverter().convert(jwtAuthenticationToken);
                    })
                )
            )
            
            // Configure security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())              // Prevent clickjacking
                .contentTypeOptions(contentTypeOptions -> {})         // Prevent MIME type sniffing
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)      // 1 year HSTS
                    .includeSubDomains(true)        // Apply to subdomains
                    .preload(true)                  // Enable HSTS preload
                )
            )
            
            .build();
    }

    /**
     * Configure audit event listener for security compliance logging.
     * 
     * Integrates with Spring Boot Actuator to capture authentication and
     * authorization events for SOX compliance and security monitoring.
     * Events are structured for ELK stack processing and real-time alerting.
     * 
     * @param auditEventRepository repository for storing audit events
     * @return AuditListener configured for security event capture
     */
    @Bean
    public AuditListener auditListener(AuditEventRepository auditEventRepository) {
        return new AuditListener(auditEventRepository);
    }

    /**
     * Custom JWT authentication converter for role mapping.
     * 
     * Converts JWT claims to Spring Security authorities, mapping RACF user types
     * to Spring Security roles:
     * - User type 'A' (Admin) -> ROLE_ADMIN authority
     * - User type 'U' (User) -> ROLE_USER authority
     * 
     * Enables seamless integration with @PreAuthorize method security annotations.
     */
    private static class JwtAuthenticationConverter 
            implements org.springframework.core.convert.converter.Converter<
                org.springframework.security.oauth2.jwt.Jwt, 
                org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken> {
        
        @Override
        public org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken convert(
                org.springframework.security.oauth2.jwt.Jwt jwt) {
            
            // Extract user type from JWT claims (mapped from RACF user types)
            String userType = jwt.getClaimAsString("user_type");
            String userId = jwt.getClaimAsString("user_id");
            
            // Create Spring Security authorities based on user type
            java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities = 
                java.util.List.of();
            
            if ("A".equals(userType)) {
                // Admin users get both ADMIN and USER roles (role hierarchy)
                authorities = java.util.List.of(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")
                );
            } else if ("U".equals(userType)) {
                // Regular users get USER role only
                authorities = java.util.List.of(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")
                );
            }
            
            // Create JWT authentication token with authorities
            return new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                jwt, authorities, userId
            );
        }
    }
}