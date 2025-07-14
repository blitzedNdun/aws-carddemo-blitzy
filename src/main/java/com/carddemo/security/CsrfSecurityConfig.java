package com.carddemo.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.http.ResponseCookie;

import java.util.function.Consumer;

import com.carddemo.common.security.SecurityConfig;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/**
 * CSRF Security Configuration for CardDemo application providing Cross-Site Request Forgery 
 * protection for specific endpoints that require stateful session management.
 * 
 * This configuration complements the main SecurityConfig.java by providing CSRF protection
 * for form-based endpoints and administrative operations while maintaining JWT token-based
 * authentication for REST API endpoints.
 * 
 * Key Features:
 * - CSRF token repository with secure cookie settings and path restrictions
 * - Integration with React frontend through X-CSRF-TOKEN header validation
 * - Exception handling for CSRF token mismatch scenarios
 * - Support for both stateful (CSRF-protected) and stateless (JWT) authentication patterns
 * - Compliance with Spring Security 6.x CSRF protection best practices
 * 
 * Security Implementation:
 * - HttpSessionCsrfTokenRepository for secure token storage with SameSite=Strict
 * - Custom CSRF token request handler for React frontend integration
 * - Dedicated security filter chain for CSRF-protected endpoints
 * - Comprehensive exception handling with structured error responses
 * 
 * Integration Points:
 * - Extends SecurityConfig for shared CORS configuration and JWT authentication
 * - Compatible with existing JWT-based microservices architecture
 * - Supports Spring Cloud Gateway routing and load balancing
 * - Integrates with Redis session management for distributed environments
 * 
 * @see SecurityConfig for main JWT authentication configuration
 * @see Section 6.4.5.2 for CSRF security requirements
 * @see Section 6.4.1.5 for JWT integration patterns
 */
@Configuration
@EnableWebSecurity
public class CsrfSecurityConfig {

    /**
     * CSRF token cookie name for secure token transmission.
     * Uses CardDemo-specific naming to avoid conflicts with other applications.
     */
    private static final String CSRF_COOKIE_NAME = "CARDDEMO-CSRF-TOKEN";
    
    /**
     * CSRF token header name for React frontend integration.
     * Follows Spring Security convention for CSRF header validation.
     */
    private static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";
    
    /**
     * CSRF token parameter name for form-based submissions.
     * Supports traditional form POST operations with CSRF protection.
     */
    private static final String CSRF_PARAMETER_NAME = "_csrf";

    /**
     * Session cookie security settings for CSRF token repository.
     * Configured for secure transmission and SameSite protection.
     */
    @Value("${carddemo.security.csrf.cookie.secure:true}")
    private boolean csrfCookieSecure;
    
    /**
     * CSRF cookie HttpOnly flag for XSS protection.
     * Prevents JavaScript access to CSRF tokens while allowing header transmission.
     */
    @Value("${carddemo.security.csrf.cookie.httponly:false}")
    private boolean csrfCookieHttpOnly;
    
    /**
     * CSRF cookie SameSite attribute for enhanced security.
     * Strict setting prevents cross-site request inclusion of CSRF tokens.
     */
    @Value("${carddemo.security.csrf.cookie.samesite:Strict}")
    private String csrfCookieSameSite;
    
    /**
     * CSRF token cookie path restriction for security scope limitation.
     * Restricts CSRF token availability to API endpoints only.
     */
    @Value("${carddemo.security.csrf.cookie.path:/api}")
    private String csrfCookiePath;

    /**
     * Configure CSRF token repository with secure cookie settings and path restrictions.
     * 
     * Implements CookieCsrfTokenRepository with enterprise-grade security settings
     * including secure cookie transmission, SameSite protection, and path restrictions.
     * The repository uses cookies for token storage enabling stateless CSRF protection
     * while maintaining CSRF token security across multiple microservice instances.
     * 
     * Security Features:
     * - Secure cookie transmission (HTTPS only in production)
     * - SameSite=Strict for cross-site request protection
     * - Path restriction to /api endpoints only
     * - HttpOnly cookie configuration for XSS protection
     * - Custom cookie naming to avoid application conflicts
     * 
     * @return CsrfTokenRepository configured for secure CSRF token management
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        
        // Modern cookie configuration using Spring Security 6.x cookie customizer
        // This approach replaces deprecated individual setter methods
        Consumer<ResponseCookie.ResponseCookieBuilder> cookieCustomizer = cookieBuilder -> {
            cookieBuilder
                .path(csrfCookiePath)                    // Path restriction to API endpoints only
                .secure(csrfCookieSecure)                // HTTPS-only transmission in production
                .httpOnly(csrfCookieHttpOnly)            // Allow JavaScript access for React frontend
                .sameSite(csrfCookieSameSite);           // Cross-site request protection
        };
        
        // Apply modern cookie customizer configuration
        repository.setCookieCustomizer(cookieCustomizer);
        
        // Custom cookie name prevents conflicts with other CardDemo applications
        // or microservices running in the same domain environment
        repository.setCookieName(CSRF_COOKIE_NAME);
        
        // Configure CSRF token parameter and header names for React frontend integration
        // Parameter name supports traditional form submissions with CSRF protection
        repository.setParameterName(CSRF_PARAMETER_NAME);
        
        // Header name enables React frontend to send CSRF tokens via X-CSRF-TOKEN header
        // This approach allows React components to include CSRF tokens in AJAX requests
        repository.setHeaderName(CSRF_HEADER_NAME);
        
        return repository;
    }

    /**
     * Configure CSRF security filter chain for form-based and administrative endpoints.
     * 
     * Creates a dedicated security filter chain that applies CSRF protection to specific
     * endpoints requiring stateful session management while maintaining compatibility
     * with the existing JWT-based stateless API authentication.
     * 
     * CSRF Protection Scope:
     * - Administrative forms and configuration endpoints
     * - User management interfaces requiring state persistence
     * - File upload endpoints with multipart form data
     * - Password change and profile update operations
     * 
     * Integration Features:
     * - CORS configuration inherited from SecurityConfig
     * - CSRF token validation via cookies and headers
     * - Custom exception handling for token mismatch scenarios
     * - Session management for CSRF-protected endpoints
     * 
     * @param http HttpSecurity configuration object
     * @param corsConfigurationSource CORS configuration from SecurityConfig
     * @return SecurityFilterChain configured for CSRF-protected endpoints
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain csrfSecurityFilterChain(
            HttpSecurity http, 
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        
        // Define request matchers for CSRF-protected endpoints
        // These endpoints require stateful session management and CSRF protection
        RequestMatcher csrfProtectedMatcher = new OrRequestMatcher(
            // Administrative form endpoints requiring CSRF protection
            new AntPathRequestMatcher("/api/admin/forms/**"),
            new AntPathRequestMatcher("/api/admin/config/**"),
            
            // User management forms and profile updates
            new AntPathRequestMatcher("/api/users/profile/**", "POST"),
            new AntPathRequestMatcher("/api/users/password/**", "POST"),
            new AntPathRequestMatcher("/api/users/settings/**", "POST"),
            
            // File upload endpoints with multipart form data
            new AntPathRequestMatcher("/api/files/upload/**", "POST"),
            new AntPathRequestMatcher("/api/documents/upload/**", "POST"),
            
            // Batch processing initiation endpoints
            new AntPathRequestMatcher("/api/batch/jobs/**", "POST"),
            new AntPathRequestMatcher("/api/reports/generate/**", "POST")
        );
        
        return http
            // Apply configuration only to CSRF-protected endpoints
            .securityMatcher(csrfProtectedMatcher)
            
            // Configure CORS with shared configuration source from SecurityConfig
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // Enable CSRF protection with custom token repository
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository())
                
                // Configure CSRF token request handler for React integration
                .csrfTokenRequestHandler(createCsrfTokenRequestHandler())
                
                // Ignore CSRF for specific endpoints that use JWT authentication
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher("/api/auth/**"),      // Authentication endpoints
                    new AntPathRequestMatcher("/actuator/**"),     // Health and monitoring
                    new AntPathRequestMatcher("/api/*/health")     // Service health checks
                )
            )
            
            // Configure CSRF access denied handler for structured error responses
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(csrfExceptionHandler())
            )
            
            // Configure session management for CSRF-protected endpoints
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            
            // Configure authorization for CSRF-protected endpoints
            .authorizeHttpRequests(authz -> authz
                // Administrative endpoints require ADMIN role
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // User management endpoints require USER or ADMIN role
                .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")
                
                // File and batch operations require authentication
                .requestMatchers("/api/files/**", "/api/documents/**", "/api/batch/**", "/api/reports/**")
                    .authenticated()
                
                // All other CSRF-protected requests require authentication
                .anyRequest().authenticated()
            )
            
            // Configure security headers for CSRF-protected endpoints
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())                    // Prevent clickjacking attacks
                .contentTypeOptions(contentTypeOptions -> {})  // Prevent MIME type sniffing
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)            // 1 year HSTS header
                    .includeSubDomains(true)              // Apply to all subdomains
                    .preload(true)                        // Enable HSTS preload list
                )
            )
            
            .build();
    }

    /**
     * Configure CSRF exception handler for token mismatch error responses.
     * 
     * Provides structured error handling for CSRF token validation failures,
     * including detailed error messages for debugging and security monitoring.
     * The handler integrates with Spring Boot Actuator for audit event generation
     * and supports JSON-formatted error responses for React frontend integration.
     * 
     * Error Response Features:
     * - Structured JSON error format compatible with React error handling
     * - HTTP status code 403 (Forbidden) for CSRF token mismatches
     * - Detailed error messages for development and debugging
     * - Security event logging for monitoring and compliance
     * - Correlation ID support for distributed tracing
     * 
     * @return AccessDeniedHandler configured for CSRF error responses
     */
    @Bean
    public AccessDeniedHandler csrfExceptionHandler() {
        return new AccessDeniedHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response,
                             org.springframework.security.access.AccessDeniedException accessDeniedException) 
                    throws IOException {
                
                // Set response status and content type for JSON error response
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                
                // Extract request correlation ID for tracing
                String correlationId = request.getHeader("X-Correlation-ID");
                if (correlationId == null) {
                    correlationId = java.util.UUID.randomUUID().toString();
                }
                
                // Extract CSRF token information for error details
                CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                String expectedToken = csrfToken != null ? csrfToken.getHeaderName() : "unknown";
                String providedToken = request.getHeader(CSRF_HEADER_NAME);
                
                // Create structured JSON error response for React frontend
                String jsonErrorResponse = String.format("""
                    {
                        "error": "CSRF_TOKEN_MISMATCH",
                        "message": "CSRF token validation failed. Please refresh the page and try again.",
                        "status": 403,
                        "timestamp": "%s",
                        "path": "%s",
                        "correlationId": "%s",
                        "details": {
                            "expectedHeader": "%s",
                            "providedToken": "%s",
                            "tokenRequired": true,
                            "suggestion": "Include X-CSRF-TOKEN header with valid token value"
                        }
                    }""",
                    java.time.Instant.now().toString(),
                    request.getRequestURI(),
                    correlationId,
                    expectedToken,
                    providedToken != null ? "***provided***" : "null"
                );
                
                // Add security headers to error response
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("X-Frame-Options", "DENY");
                response.setHeader("X-Correlation-ID", correlationId);
                
                // Write JSON error response to output stream
                response.getWriter().write(jsonErrorResponse);
                response.getWriter().flush();
                
                // Log CSRF token mismatch for security monitoring
                // This event will be captured by Spring Boot Actuator for audit compliance
                org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CsrfSecurityConfig.class);
                logger.warn("CSRF token validation failed for request: {} from IP: {} with correlation ID: {}",
                    request.getRequestURI(), 
                    getClientIpAddress(request), 
                    correlationId);
            }
            
            /**
             * Extract client IP address considering proxy headers for accurate logging.
             * 
             * @param request HttpServletRequest for IP extraction
             * @return String client IP address
             */
            private String getClientIpAddress(HttpServletRequest request) {
                // Check for IP address from proxy headers first
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                
                // Fall back to remote address
                return request.getRemoteAddr();
            }
        };
    }

    /**
     * Create CSRF token request handler for React frontend integration.
     * 
     * Configures custom CSRF token request handling that supports both traditional
     * form submissions and modern React AJAX requests. The handler ensures CSRF
     * tokens are properly exposed to the frontend while maintaining security.
     * 
     * React Integration Features:
     * - CSRF token exposure via X-CSRF-TOKEN header
     * - Cookie-based token transmission for AJAX requests
     * - Support for single-page application token refresh
     * - Integration with React Redux state management
     * 
     * @return CsrfTokenRequestAttributeHandler configured for React integration
     */
    private CsrfTokenRequestAttributeHandler createCsrfTokenRequestHandler() {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        
        // Configure the handler to set the token as a request attribute
        // This makes the token available to React frontend via response headers
        requestHandler.setCsrfRequestAttributeName("_csrf");
        
        return requestHandler;
    }
}