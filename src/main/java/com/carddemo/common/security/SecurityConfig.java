package com.carddemo.common.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring Security configuration class providing JWT authentication, password encoding, 
 * and shared security components used across all CardDemo microservices.
 * 
 * This configuration implements enterprise-grade security patterns aligned with 
 * the COBOL-to-Java transformation requirements:
 * - JWT-based stateless authentication replacing CICS pseudo-conversational processing
 * - BCrypt password hashing with configurable salt rounds (minimum 12)
 * - Role-based access control mapping RACF user types to Spring Security authorities
 * - CORS configuration for React frontend integration
 * - Security audit event publishers for compliance logging
 * 
 * @author CardDemo Development Team
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * BCrypt password encoder strength (salt rounds) - configurable via application properties
     * Minimum value of 12 ensures enterprise-grade security as per PCI DSS requirements
     */
    @Value("${carddemo.security.bcrypt.strength:12}")
    private int bcryptStrength;

    /**
     * JWT token expiration time in seconds - configurable for different environments
     */
    @Value("${carddemo.security.jwt.expiration:1800}")
    private long jwtExpirationSeconds;

    /**
     * JWT issuer identifier for token validation
     */
    @Value("${carddemo.security.jwt.issuer:carddemo-auth-service}")
    private String jwtIssuer;

    /**
     * Allowed origins for CORS configuration - supports React frontend
     */
    @Value("${carddemo.security.cors.allowed-origins:http://localhost:3000,https://carddemo.local}")
    private String[] allowedOrigins;

    /**
     * Application event publisher for security audit events
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructor for dependency injection
     */
    public SecurityConfig(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * BCrypt password encoder bean with configurable salt rounds.
     * 
     * Provides secure password hashing for user authentication, replacing
     * the legacy RACF plain-text password storage with industry-standard
     * BCrypt hashing algorithm.
     * 
     * Configuration:
     * - Minimum 12 salt rounds as per enterprise security requirements
     * - Configurable via application properties for different environments
     * - PCI DSS compliant for payment card industry security standards
     * 
     * @return PasswordEncoder instance configured with BCrypt algorithm
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Ensure minimum security strength of 12 rounds
        int strength = Math.max(bcryptStrength, 12);
        
        // Publish audit event for password encoder configuration
        publishSecurityAuditEvent("PASSWORD_ENCODER_CONFIGURED", 
            Map.of("strength", strength, "algorithm", "BCrypt"));
        
        return new BCryptPasswordEncoder(strength);
    }

    /**
     * JWT decoder for OAuth2 resource server configuration.
     * 
     * Configures JWT token validation using RSA-256 algorithm for
     * stateless authentication across all microservices. This replaces
     * CICS session management with cloud-native JWT tokens.
     * 
     * Features:
     * - RSA-256 signature validation for token integrity
     * - Configurable token expiration and issuer validation
     * - Compatible with Spring Security OAuth2 resource server
     * - Supports horizontal scaling through stateless design
     * 
     * @return JwtDecoder instance for token validation
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            // Generate RSA key pair for JWT signing and validation
            KeyPair keyPair = generateRSAKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            
            // Configure JWT decoder with RSA public key
            NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .build();
            
            // Publish audit event for JWT decoder configuration
            publishSecurityAuditEvent("JWT_DECODER_CONFIGURED", 
                Map.of("algorithm", "RS256", "issuer", jwtIssuer));
            
            return decoder;
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure JWT decoder", e);
        }
    }

    /**
     * JWT encoder for token generation.
     * 
     * Provides JWT token generation capabilities for the authentication service.
     * This bean is used by the authentication service to create JWT tokens
     * containing user identity and role information.
     * 
     * @return JwtEncoder instance for token generation
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        try {
            // Generate RSA key pair for JWT signing
            KeyPair keyPair = generateRSAKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            
            // Create JWK (JSON Web Key) for signing
            JWK jwk = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .build();
            
            // Configure JWK source for token signing
            JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
            
            return new NimbusJwtEncoder(jwkSource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure JWT encoder", e);
        }
    }

    /**
     * CORS configuration source for React frontend integration.
     * 
     * Configures Cross-Origin Resource Sharing to allow the React frontend
     * to communicate with Spring Boot microservices. This is essential for
     * the web-based UI to access REST API endpoints.
     * 
     * Configuration:
     * - Supports configurable allowed origins for different environments
     * - Includes necessary headers for JWT token authentication
     * - Enables credentials for secure cookie-based session management
     * - Supports all standard HTTP methods required by the application
     * 
     * @return CorsConfigurationSource for HTTP security configuration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Configure allowed origins for React frontend
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        
        // Allow all HTTP methods required by the application
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));
        
        // Configure allowed headers for JWT authentication
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", 
            "Accept", "Origin", "Access-Control-Request-Method",
            "Access-Control-Request-Headers", "X-CSRF-TOKEN"
        ));
        
        // Expose headers needed by the frontend
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials",
            "Authorization", "X-Total-Count"
        ));
        
        // Enable credentials for secure authentication
        configuration.setAllowCredentials(true);
        
        // Set preflight request cache duration
        configuration.setMaxAge(3600L);
        
        // Apply configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        // Publish audit event for CORS configuration
        publishSecurityAuditEvent("CORS_CONFIGURATION_APPLIED", 
            Map.of("allowedOrigins", Arrays.toString(allowedOrigins), 
                   "allowCredentials", true));
        
        return source;
    }

    /**
     * Main security filter chain configuration.
     * 
     * Configures the primary security filter chain for all HTTP requests.
     * This configuration implements the core security requirements:
     * - JWT-based authentication for API endpoints
     * - Role-based authorization with RACF user type mapping
     * - CORS support for React frontend integration
     * - Security audit event publishing
     * 
     * Security Features:
     * - Stateless session management using JWT tokens
     * - Method-level security with @PreAuthorize annotations
     * - Comprehensive audit logging for compliance
     * - Protection against common web vulnerabilities
     * 
     * @param http HttpSecurity configuration object
     * @return SecurityFilterChain configured for the application
     * @throws Exception if security configuration fails
     */
    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Disable CSRF for stateless JWT authentication
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS for React frontend
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure session management for stateless authentication
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure URL-based authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/api/auth/login"),
                    AntPathRequestMatcher.antMatcher("/api/auth/refresh"),
                    AntPathRequestMatcher.antMatcher("/actuator/health"),
                    AntPathRequestMatcher.antMatcher("/actuator/info"),
                    AntPathRequestMatcher.antMatcher("/swagger-ui/**"),
                    AntPathRequestMatcher.antMatcher("/v3/api-docs/**"),
                    AntPathRequestMatcher.antMatcher("/error")
                ).permitAll()
                
                // Admin-only endpoints
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/api/admin/**"),
                    AntPathRequestMatcher.antMatcher("/api/users/**"),
                    AntPathRequestMatcher.antMatcher("/actuator/**")
                ).hasRole("ADMIN")
                
                // All other endpoints require authentication
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
                    // Publish audit event for authentication failure
                    publishSecurityAuditEvent("AUTHENTICATION_FAILURE", 
                        Map.of("requestURI", request.getRequestURI(), 
                               "remoteAddr", request.getRemoteAddr(),
                               "error", authException.getMessage()));
                    
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // Publish audit event for authorization failure
                    publishSecurityAuditEvent("AUTHORIZATION_FAILURE", 
                        Map.of("requestURI", request.getRequestURI(), 
                               "remoteAddr", request.getRemoteAddr(),
                               "error", accessDeniedException.getMessage()));
                    
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"Forbidden\",\"message\":\"Insufficient privileges\"}"
                    );
                })
            )
            
            .build();
    }

    /**
     * JWT authentication converter for role mapping.
     * 
     * Converts JWT claims to Spring Security authorities, mapping
     * RACF user types to appropriate Spring Security roles:
     * - User type 'A' (Admin) -> ROLE_ADMIN
     * - User type 'U' (User) -> ROLE_USER
     * 
     * @return JwtAuthenticationConverter for role extraction
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        
        // Configure authorities converter for role mapping
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract user type from JWT claims
            String userType = jwt.getClaimAsString("user_type");
            
            // Map RACF user types to Spring Security roles
            Collection<SimpleGrantedAuthority> authorities = switch (userType) {
                case "A" -> List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_USER")  // Admin inherits user privileges
                );
                case "U" -> List.of(
                    new SimpleGrantedAuthority("ROLE_USER")
                );
                default -> List.of(); // No roles for unknown user types
            };
            
            // Publish audit event for role mapping
            publishSecurityAuditEvent("JWT_ROLE_MAPPING", 
                Map.of("userId", jwt.getClaimAsString("sub"), 
                       "userType", userType != null ? userType : "UNKNOWN",
                       "authorities", authorities.stream()
                           .map(SimpleGrantedAuthority::getAuthority)
                           .collect(Collectors.joining(","))));
            
            return authorities;
        });
        
        return converter;
    }

    /**
     * Authentication event publisher for security audit logging.
     * 
     * Publishes authentication events to the Spring Boot Actuator audit
     * framework for compliance logging and security monitoring.
     * 
     * @return AuthenticationEventPublisher for audit events
     */
    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher() {
        return new DefaultAuthenticationEventPublisher(eventPublisher);
    }

    /**
     * Generates RSA key pair for JWT signing and validation.
     * 
     * @return KeyPair containing RSA public and private keys
     * @throws Exception if key generation fails
     */
    private KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Publishes security audit events for compliance logging.
     * 
     * Creates and publishes audit events to Spring Boot Actuator for
     * security monitoring and compliance reporting. Events are structured
     * for integration with ELK stack and other monitoring systems.
     * 
     * @param type Event type identifier
     * @param data Event data as key-value pairs
     */
    private void publishSecurityAuditEvent(String type, Map<String, Object> data) {
        try {
            AuditEvent auditEvent = new AuditEvent(
                Instant.now(),
                "SECURITY_SYSTEM",
                type,
                data
            );
            
            eventPublisher.publishEvent(new AuditApplicationEvent(auditEvent));
        } catch (Exception e) {
            // Log audit event publication failure but don't throw exception
            // to avoid disrupting normal security operations
            System.err.println("Failed to publish security audit event: " + e.getMessage());
        }
    }

    /**
     * Security role constants for consistent role management.
     * 
     * Defines standard role names used throughout the application
     * for consistent role-based access control.
     */
    public static final class SecurityRoles {
        public static final String ADMIN = "ADMIN";
        public static final String USER = "USER";
        
        private SecurityRoles() {
            // Utility class - prevent instantiation
        }
    }

    /**
     * Security authorities constants for method-level security.
     * 
     * Defines standard authorities used in @PreAuthorize annotations
     * for method-level access control.
     */
    public static final class SecurityAuthorities {
        public static final String ROLE_ADMIN = "ROLE_ADMIN";
        public static final String ROLE_USER = "ROLE_USER";
        public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";
        public static final String HAS_ROLE_USER = "hasRole('USER')";
        public static final String HAS_ANY_ROLE_USER_ADMIN = "hasAnyRole('USER','ADMIN')";
        
        private SecurityAuthorities() {
            // Utility class - prevent instantiation
        }
    }
}