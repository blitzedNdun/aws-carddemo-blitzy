package com.carddemo.security;

import com.carddemo.security.JwtRequestFilter;
import com.carddemo.security.CustomUserDetailsService;
import com.carddemo.security.JwtTokenService;
import com.carddemo.security.SecurityConstants;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Main Spring Security configuration class defining authentication providers, authorization rules,
 * JWT token management, and security filter chains. Replaces RACF security configuration with
 * Spring Security 6.x declarative security configuration.
 * 
 * This configuration implements comprehensive security architecture for the CardDemo application,
 * maintaining identical authentication and authorization behavior from the legacy COBOL/CICS
 * system while leveraging modern Spring Security framework capabilities. The configuration
 * supports JWT-based stateless authentication with Redis session backup, role-based access
 * control, and seamless React SPA integration.
 * 
 * Key Security Features:
 * - JWT-based authentication with Redis session management
 * - Role-based authorization mapping ROLE_ADMIN and ROLE_USER from SEC-USR-TYPE
 * - Method-level security through @PreAuthorize annotations
 * - CORS configuration for React frontend integration
 * - CSRF protection disabled for REST API endpoints
 * - NoOpPasswordEncoder for migration parity with legacy plain-text passwords
 * - Comprehensive security filter chain with protected endpoints
 * - Custom JWT authentication filter integrated into Spring Security chain
 * 
 * Authentication Flow:
 * 1. HTTP requests intercepted by Spring Security filter chain
 * 2. JWT tokens extracted and validated through JwtRequestFilter
 * 3. User details loaded via CustomUserDetailsService from PostgreSQL
 * 4. Security context established with user authorities
 * 5. Authorization decisions made based on @PreAuthorize annotations
 * 6. Session state managed through Redis for scalability
 * 
 * Security Architecture Alignment:
 * - Replaces RACF authentication with Spring Security UserDetailsService
 * - Maps SEC-USR-TYPE to Spring Security roles (A=ROLE_ADMIN, U=ROLE_USER)
 * - Maintains CICS session behavior through Redis session management
 * - Preserves identical authorization patterns from COBOL programs
 * - Supports legacy password storage during migration period
 * 
 * This configuration serves as the central security policy enforcement point,
 * ensuring consistent security behavior across all application endpoints while
 * providing foundation for future security enhancements including password
 * encryption, multi-factor authentication, and advanced authorization patterns.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    /**
     * Configures the main security filter chain defining HTTP security policies,
     * authentication requirements, authorization rules, CORS/CSRF settings, and
     * session management for REST API endpoints.
     * 
     * This method implements the core security configuration replacing RACF-based
     * access control with Spring Security declarative authorization. The filter
     * chain processes all HTTP requests through authentication and authorization
     * filters, establishing security context for downstream business logic.
     * 
     * Security Configuration:
     * - Public endpoints: /api/auth/login, /api/auth/logout, /actuator/health
     * - Protected endpoints: All other /api/** routes require authentication
     * - Admin endpoints: /api/admin/** require ROLE_ADMIN authority
     * - User endpoints: /api/user/** require ROLE_USER or ROLE_ADMIN authority
     * - CORS enabled for React SPA integration
     * - CSRF disabled for REST API architecture
     * - Stateless session management with JWT tokens
     * - Custom JWT authentication filter before UsernamePasswordAuthenticationFilter
     * 
     * Authorization Matrix:
     * - /api/auth/** - Public access (login/logout endpoints)
     * - /api/admin/** - ROLE_ADMIN required (user management, system administration)
     * - /api/user/** - ROLE_USER or ROLE_ADMIN required (account operations, transactions)
     * - /actuator/** - Admin access required (monitoring endpoints)
     * - All other /api/** - Authenticated access required
     * 
     * @param http HttpSecurity builder for configuring security policies
     * @return SecurityFilterChain configured with authentication and authorization rules
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring Spring Security filter chain with JWT authentication and role-based authorization");

        http
            // Configure authorization rules for HTTP requests
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                
                // Admin-only endpoints - ROLE_ADMIN required
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // User endpoints - ROLE_USER or ROLE_ADMIN required
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/accounts/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/transactions/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/customers/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/cards/**").hasAnyRole("USER", "ADMIN")
                
                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                
                // Default - permit all for non-API requests
                .anyRequest().permitAll()
            )
            
            // Configure CORS for React SPA integration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Disable CSRF protection for REST API architecture
            .csrf(csrf -> csrf.disable())
            
            // Configure stateless session management for JWT authentication
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Add custom JWT authentication filter before username/password filter
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configure exception handling for authentication/authorization failures
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

        logger.info("Spring Security filter chain configured successfully with JWT authentication and role-based authorization");
        return http.build();
    }

    /**
     * Configures the password encoder for user authentication, maintaining
     * migration parity with legacy COBOL plain-text password storage.
     * 
     * This method implements NoOpPasswordEncoder to preserve exact migration
     * parity with the legacy COBOL system where passwords are stored as
     * plain text. This approach maintains identical authentication behavior
     * during the initial migration phase while providing a clear upgrade
     * path to encrypted password storage in future iterations.
     * 
     * Security Considerations:
     * - NoOpPasswordEncoder performs no password hashing for legacy compatibility
     * - Passwords stored and compared as plain text matching COBOL behavior
     * - Authentication logic preserves identical validation from COSGN00C program
     * - Enhancement opportunity documented for future BCrypt implementation
     * 
     * Migration Strategy:
     * - Phase 1 (Current): NoOpPasswordEncoder for exact legacy parity
     * - Phase 2 (Future): Gradual migration to BCryptPasswordEncoder
     * - Phase 3 (Enhanced): Advanced password policies and complexity requirements
     * 
     * @return PasswordEncoder instance configured for plain-text password handling
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.info("Configuring NoOpPasswordEncoder for legacy password compatibility during migration");
        
        // Use NoOpPasswordEncoder for migration parity with COBOL plain-text passwords
        // This maintains identical authentication behavior from the legacy system
        // Enhancement opportunity: Migrate to BCryptPasswordEncoder in future iterations
        @SuppressWarnings("deprecation")
        PasswordEncoder encoder = NoOpPasswordEncoder.getInstance();
        
        logger.warn("Using NoOpPasswordEncoder - passwords stored as plain text for legacy compatibility. " +
                   "Consider migrating to BCryptPasswordEncoder for enhanced security.");
        
        return encoder;
    }

    /**
     * Configures the Spring Security AuthenticationManager for processing
     * authentication requests through the configured authentication providers.
     * 
     * This method provides access to the AuthenticationManager built by Spring
     * Security's AuthenticationConfiguration, enabling programmatic authentication
     * in controllers and services. The manager coordinates authentication through
     * UserDetailsService and PasswordEncoder, maintaining the authentication
     * flow from the legacy COBOL system.
     * 
     * Authentication Process:
     * 1. Receives authentication requests from login controllers
     * 2. Delegates to CustomUserDetailsService for user lookup
     * 3. Uses configured PasswordEncoder for credential validation
     * 4. Returns authenticated Authentication object with user authorities
     * 5. Enables JWT token generation for successful authentications
     * 
     * Integration Points:
     * - Used by AuthenticationController for login processing
     * - Integrates with CustomUserDetailsService for PostgreSQL user lookup
     * - Works with NoOpPasswordEncoder for plain-text password validation
     * - Supports JWT token generation through JwtTokenService
     * 
     * @param authConfig AuthenticationConfiguration provided by Spring Security
     * @return AuthenticationManager configured with user details and password services
     * @throws Exception if authentication manager configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        logger.info("Configuring AuthenticationManager with CustomUserDetailsService and NoOpPasswordEncoder");
        
        AuthenticationManager authManager = authConfig.getAuthenticationManager();
        
        logger.info("AuthenticationManager configured successfully for JWT-based authentication");
        return authManager;
    }

    /**
     * Configures Cross-Origin Resource Sharing (CORS) policies for React SPA integration,
     * enabling secure communication between the frontend application and Spring Boot backend.
     * 
     * This method implements comprehensive CORS configuration supporting the React
     * Single Page Application architecture while maintaining security best practices.
     * The configuration allows controlled cross-origin access for authentication,
     * API operations, and administrative functions while preventing unauthorized
     * cross-origin requests.
     * 
     * CORS Configuration:
     * - Allowed Origins: http://localhost:3000 (React development server)
     * - Allowed Methods: GET, POST, PUT, DELETE, OPTIONS for full REST API support
     * - Allowed Headers: Authorization, Content-Type, Accept for JWT and JSON support
     * - Exposed Headers: Authorization for JWT token responses
     * - Credentials Support: Enabled for session cookies and authentication headers
     * - Max Age: 3600 seconds for preflight request caching
     * 
     * Security Considerations:
     * - Restricted to specific origins preventing unauthorized cross-origin access
     * - Authorization header explicitly allowed for JWT token transmission
     * - Credentials support enables secure session management
     * - Preflight caching reduces network overhead for repeated requests
     * 
     * Production Deployment:
     * - Update allowed origins to include production frontend URLs
     * - Consider environment-specific configuration for different deployment stages
     * - Implement HTTPS-only origins for production security
     * 
     * @return CorsConfigurationSource configured for React SPA integration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logger.info("Configuring CORS policy for React SPA integration");
        
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Configure allowed origins for React SPA
        // TODO: Update for production deployment with actual frontend URLs
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",  // React development server
            "http://localhost:3001",  // Alternative React port
            "https://localhost:3000", // HTTPS React development
            "https://carddemo-frontend.*" // Production frontend pattern
        ));
        
        // Configure allowed HTTP methods for full REST API support
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"
        ));
        
        // Configure allowed headers for JWT authentication and JSON content
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",     // JWT token header
            "Content-Type",      // JSON content type
            "Accept",            // Response content negotiation
            "Origin",            // CORS origin header
            "X-Requested-With",  // AJAX request indicator
            "Access-Control-Request-Method",   // Preflight method
            "Access-Control-Request-Headers"   // Preflight headers
        ));
        
        // Configure exposed headers for JWT token responses
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",     // JWT token in responses
            "Access-Control-Allow-Origin",     // CORS origin
            "Access-Control-Allow-Credentials" // Credentials flag
        ));
        
        // Enable credentials support for session cookies and authentication
        configuration.setAllowCredentials(true);
        
        // Configure preflight request caching duration
        configuration.setMaxAge(3600L); // 1 hour
        
        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        logger.info("CORS configuration completed - allowing origins: {}, methods: {}, headers: {}", 
                   configuration.getAllowedOriginPatterns(),
                   configuration.getAllowedMethods(),
                   configuration.getAllowedHeaders());
        
        return source;
    }
}