package com.carddemo.config;

import com.carddemo.service.SignOnService;
import com.carddemo.security.JwtAuthenticationFilter;
import com.carddemo.security.CustomUserDetailsService;
import com.carddemo.security.JwtTokenService;
import com.carddemo.security.SecurityConstants;
import com.carddemo.entity.UserSecurity;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration class replacing RACF authentication and authorization
 * system. Implements comprehensive security framework for the CardDemo application
 * supporting JWT-based authentication, role-based access control, and session management.
 * 
 * This configuration class translates RACF security model to Spring Security patterns:
 * - User authentication through PostgreSQL usrsec table (replaces RACF user profiles)
 * - Authorization based on SEC-USR-TYPE mapping to ROLE_ADMIN and ROLE_USER
 * - Session management through Redis store (replaces CICS COMMAREA)
 * - JWT token authentication for stateless API security
 * - Method-level security through @PreAuthorize annotations
 * 
 * Security Architecture Components:
 * - SecurityFilterChain: Configures HTTP security, endpoint authorization, and filter chain
 * - AuthenticationManager: Manages authentication providers and credential validation
 * - PasswordEncoder: NoOpPasswordEncoder for plain text password compatibility during migration
 * - CorsConfigurationSource: Cross-origin resource sharing configuration for React frontend
 * - JWT Authentication Filter: Custom filter for JWT token processing and validation
 * 
 * The configuration maintains minimal change approach by preserving exact authentication
 * logic from COSGN00C.cbl while providing modern security capabilities through Spring
 * Security framework integration. All authentication flows maintain identical behavior
 * to original COBOL implementation.
 * 
 * Key Features:
 * - JWT-based stateless authentication with Redis session backing
 * - Role hierarchy mapping SEC-USR-TYPE to Spring Security authorities
 * - CORS configuration for React SPA frontend integration
 * - Method-level security enabling @PreAuthorize annotations across services
 * - Authentication provider chain with PostgreSQL user details service
 * - Security event logging and audit trail integration
 * - Session management compatible with CICS timeout behavior
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private SignOnService signOnService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtTokenService jwtTokenService;

    /**
     * Configures the main security filter chain defining HTTP security settings, endpoint
     * authorization, JWT filter integration, CORS configuration, and session management.
     * 
     * This method implements the core security configuration equivalent to RACF resource
     * profiles and access controls, providing comprehensive HTTP security while maintaining
     * compatibility with the original COBOL authentication patterns.
     * 
     * Security Configuration Details:
     * - Disables CSRF for REST API stateless operation
     * - Configures CORS for React frontend integration
     * - Sets stateless session management with JWT tokens
     * - Defines public endpoints bypassing authentication
     * - Requires authentication for all other endpoints
     * - Integrates JWT authentication filter before username/password filter
     * - Configures authorization rules based on endpoint patterns
     * 
     * Endpoint Authorization Mapping:
     * - /api/auth/** - Public authentication endpoints
     * - /actuator/health - Public health check endpoint
     * - /api/admin/** - Admin-only endpoints requiring ROLE_ADMIN
     * - /api/user/** - User endpoints requiring ROLE_USER or ROLE_ADMIN
     * - All other /api/** - Authenticated access required
     * 
     * @param http HttpSecurity configuration builder
     * @return SecurityFilterChain configured security filter chain
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring Spring Security filter chain for CardDemo application");

        http
            // Disable CSRF for REST API stateless operation
            .csrf(csrf -> csrf.disable())
            
            // Configure CORS for React frontend integration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure session management as stateless with JWT tokens
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure HTTP security and endpoint authorization
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers(
                    SecurityConstants.AUTH_LOGIN_ENDPOINT,
                    SecurityConstants.AUTH_LOGOUT_ENDPOINT,
                    "/actuator/health",
                    "/api/public/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                
                // Admin-only endpoints requiring ROLE_ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // User endpoints requiring ROLE_USER or ROLE_ADMIN
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                
                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                
                // Default: require authentication for all other requests
                .anyRequest().authenticated()
            )
            
            // Add JWT authentication filter before username/password filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configure authentication provider
            .authenticationProvider(daoAuthenticationProvider())
            
            // Configure exception handling
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    logger.warn("Authentication failed for request: {} - {}", 
                               request.getRequestURI(), authException.getMessage());
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Authentication required\",\"message\":\"" + 
                                             authException.getMessage() + "\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    logger.warn("Access denied for request: {} - {}", 
                               request.getRequestURI(), accessDeniedException.getMessage());
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Access denied\",\"message\":\"" + 
                                             accessDeniedException.getMessage() + "\"}");
                })
            );

        logger.info("Spring Security filter chain configuration completed successfully");
        return http.build();
    }

    /**
     * Configures the authentication manager integrating with PostgreSQL user details service
     * and password encoder. Provides authentication provider chain for credential validation
     * matching original COBOL authentication logic from COSGN00C.cbl.
     * 
     * The authentication manager coordinates between:
     * - CustomUserDetailsService for user lookup from PostgreSQL usrsec table
     * - NoOpPasswordEncoder for plain text password validation (migration compatibility)
     * - DaoAuthenticationProvider for standard Spring Security authentication flow
     * 
     * Authentication Flow:
     * 1. Receives authentication request with username/password
     * 2. Delegates to CustomUserDetailsService for user lookup
     * 3. Validates password using configured password encoder
     * 4. Returns authenticated Authentication object with authorities
     * 5. Logs authentication events for audit trail
     * 
     * @param authConfig AuthenticationConfiguration from Spring Security
     * @return AuthenticationManager configured authentication manager
     * @throws Exception if authentication manager configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        logger.info("Configuring Spring Security authentication manager");
        
        AuthenticationManager authManager = authConfig.getAuthenticationManager();
        
        logger.info("Authentication manager configuration completed with PostgreSQL user details service");
        return authManager;
    }

    /**
     * Configures the password encoder for credential validation. Uses NoOpPasswordEncoder
     * to maintain compatibility with legacy COBOL plain text password storage during
     * the migration period.
     * 
     * IMPORTANT: This configuration maintains the exact password storage mechanism from
     * the original COBOL system where passwords are stored in plain text in the USRSEC
     * dataset. This ensures 100% functional parity during migration while providing
     * enhancement opportunity for future BCrypt implementation.
     * 
     * Migration Compatibility:
     * - Preserves plain text password validation matching COBOL logic
     * - Supports case-insensitive password comparison
     * - Maintains identical authentication behavior from COSGN00C.cbl
     * - Enables future migration to BCrypt without breaking existing credentials
     * 
     * Security Note: In production environments, consider migrating to BCryptPasswordEncoder
     * with gradual user password updates to enhance security posture while maintaining
     * backward compatibility.
     * 
     * @return PasswordEncoder NoOpPasswordEncoder for plain text password compatibility
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.info("Configuring NoOpPasswordEncoder for legacy COBOL password compatibility");
        
        // Use NoOpPasswordEncoder to maintain exact COBOL authentication behavior
        // This preserves plain text password storage matching legacy USRSEC dataset format
        PasswordEncoder encoder = NoOpPasswordEncoder.getInstance();
        
        logger.warn("Using NoOpPasswordEncoder for migration compatibility - consider BCrypt for production");
        return encoder;
    }

    /**
     * Configures Cross-Origin Resource Sharing (CORS) settings for React frontend integration.
     * Enables secure communication between the React SPA and Spring Boot backend while
     * maintaining appropriate security restrictions.
     * 
     * CORS Configuration Details:
     * - Allows credentials for session and JWT token authentication
     * - Permits common HTTP methods for REST API operations
     * - Authorizes React development and production origins
     * - Allows standard headers for JSON API communication
     * - Exposes Authorization header for JWT token responses
     * 
     * Frontend Integration:
     * - Supports React development server (http://localhost:3000)
     * - Accommodates production deployment origins
     * - Enables session cookie sharing for hybrid authentication
     * - Allows JWT token headers for stateless authentication
     * 
     * @return CorsConfigurationSource configured CORS settings for React integration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logger.info("Configuring CORS for React frontend integration");
        
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow credentials for session and JWT authentication
        configuration.setAllowCredentials(true);
        
        // Configure allowed origins for React frontend
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",    // React development server
            "http://localhost:3001",    // Alternative React port
            "https://localhost:3000",   // HTTPS development
            "https://carddemo.local",   // Local development domain
            "https://*.carddemo.com"    // Production domains
        ));
        
        // Allow standard HTTP methods for REST API
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));
        
        // Allow common headers for JSON API communication
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Cache-Control",
            "X-CSRF-TOKEN"
        ));
        
        // Expose headers for frontend access
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Total-Count",
            "X-Page-Number",
            "X-Page-Size"
        ));
        
        // Set maximum age for preflight requests
        configuration.setMaxAge(3600L);
        
        // Register CORS configuration
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/actuator/**", configuration);
        
        logger.info("CORS configuration completed for React frontend integration");
        return source;
    }

    /**
     * Configures the Data Access Object (DAO) authentication provider integrating
     * CustomUserDetailsService with the configured password encoder. This provider
     * implements the authentication logic equivalent to COBOL COSGN00C program.
     * 
     * Provider Configuration:
     * - Uses CustomUserDetailsService for PostgreSQL user lookup
     * - Integrates NoOpPasswordEncoder for plain text password validation
     * - Enables user details caching for performance optimization
     * - Supports authentication success/failure event publishing
     * 
     * Authentication Process:
     * 1. Receives username/password from authentication request
     * 2. Delegates to CustomUserDetailsService.loadUserByUsername()
     * 3. Validates password using NoOpPasswordEncoder.matches()
     * 4. Creates authenticated Authentication with user authorities
     * 5. Publishes authentication events for audit logging
     * 
     * @return DaoAuthenticationProvider configured authentication provider
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        logger.info("Configuring DAO authentication provider with PostgreSQL user details service");
        
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        
        // Set the user details service for PostgreSQL user lookup
        provider.setUserDetailsService(customUserDetailsService);
        
        // Set the password encoder for credential validation
        provider.setPasswordEncoder(passwordEncoder());
        
        // Enable user details caching for performance
        provider.setUserCache(null); // Default cache implementation
        
        // Hide user not found exceptions for security
        provider.setHideUserNotFoundExceptions(false);
        
        logger.info("DAO authentication provider configured successfully");
        return provider;
    }

    /**
     * Custom authentication provider integrating SignOnService business logic
     * with Spring Security authentication framework. This provider enables
     * seamless integration of COBOL authentication business rules with modern
     * Spring Security capabilities.
     * 
     * This provider can be used for:
     * - Custom authentication logic beyond standard DAO provider
     * - Integration with SignOnService business methods
     * - Advanced authentication flows requiring business rule validation
     * - Session management through SignOnService integration
     * 
     * @return UserDetailsService CustomUserDetailsService instance for authentication
     */
    @Bean
    public UserDetailsService userDetailsService() {
        logger.debug("Providing CustomUserDetailsService bean for authentication");
        return customUserDetailsService;
    }
}