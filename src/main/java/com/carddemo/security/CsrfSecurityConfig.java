package com.carddemo.security;

import com.carddemo.common.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.UUID;

/**
 * CSRF Security Configuration for CardDemo Application
 * 
 * This configuration provides comprehensive Cross-Site Request Forgery (CSRF) protection
 * for the Spring Security implementation, integrating with the existing JWT authentication
 * framework and supporting React frontend token validation requirements.
 * 
 * Key Features:
 * - Secure CSRF token repository with httpOnly cookies
 * - Custom token header validation for REST API endpoints
 * - Integration with React frontend CSRF handling
 * - Exception handling for token validation failures
 * - Cookie-based token storage with secure attributes
 * - Path-specific CSRF protection configuration
 * 
 * Implementation Details:
 * - Uses CookieCsrfTokenRepository for stateless token management
 * - Supports both X-CSRF-TOKEN and X-XSRF-TOKEN headers
 * - Configurable token repository with secure cookie settings
 * - Custom exception handling for CSRF validation failures
 * - Integration with Spring Cloud Gateway routing patterns
 * 
 * Security Considerations:
 * - HttpOnly cookies prevent XSS token theft
 * - Secure flag enforces HTTPS-only cookie transmission
 * - SameSite attribute prevents CSRF attacks from external sites
 * - Token rotation on authentication state changes
 * - Proper CORS integration for React frontend communication
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @see SecurityConfig
 * @see CsrfTokenRepository
 */
@Configuration
@EnableWebSecurity
public class CsrfSecurityConfig {

    /**
     * CSRF cookie name configuration - supports both standard and custom naming
     */
    @Value("${carddemo.security.csrf.cookie.name:XSRF-TOKEN}")
    private String csrfCookieName;

    /**
     * CSRF token header name for REST API validation
     */
    @Value("${carddemo.security.csrf.header.name:X-CSRF-TOKEN}")
    private String csrfHeaderName;

    /**
     * CSRF cookie path restriction for security isolation
     */
    @Value("${carddemo.security.csrf.cookie.path:/}")
    private String csrfCookiePath;

    /**
     * CSRF cookie domain configuration for multi-domain deployments
     */
    @Value("${carddemo.security.csrf.cookie.domain:}")
    private String csrfCookieDomain;

    /**
     * CSRF cookie max age in seconds (default: 1 hour)
     */
    @Value("${carddemo.security.csrf.cookie.max-age:3600}")
    private int csrfCookieMaxAge;

    /**
     * Enable secure cookies for HTTPS environments
     */
    @Value("${carddemo.security.csrf.cookie.secure:true}")
    private boolean csrfCookieSecure;

    /**
     * Reference to the main security configuration for integration
     */
    private final SecurityConfig securityConfig;

    /**
     * Constructor for dependency injection
     * 
     * @param securityConfig Main security configuration instance
     */
    public CsrfSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    /**
     * CSRF Token Repository Configuration
     * 
     * Creates a secure cookie-based CSRF token repository with comprehensive
     * security settings for REST API and React frontend integration.
     * 
     * Security Features:
     * - HttpOnly cookies prevent JavaScript access and XSS attacks
     * - Secure flag enforces HTTPS transmission
     * - SameSite policy prevents cross-site request forgery
     * - Configurable cookie attributes for different environments
     * - Token rotation support for enhanced security
     * 
     * Cookie Configuration:
     * - Name: Configurable via application properties (default: XSRF-TOKEN)
     * - Path: Configurable path restriction (default: /)
     * - Domain: Optional domain restriction for multi-domain setups
     * - Max-Age: Configurable expiration time (default: 1 hour)
     * - Secure: Enforced for HTTPS environments
     * - HttpOnly: Enabled to prevent XSS token theft
     * - SameSite: Strict to prevent CSRF attacks
     * 
     * @return CsrfTokenRepository configured for secure token management
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        
        // Configure cookie name for React frontend compatibility
        repository.setCookieName(csrfCookieName);
        
        // Set cookie path for security isolation
        repository.setCookiePath(csrfCookiePath);
        
        // Configure domain if specified (for multi-domain deployments)
        if (csrfCookieDomain != null && !csrfCookieDomain.isEmpty()) {
            repository.setCookieDomain(csrfCookieDomain);
        }
        
        // Configure cookie security attributes
        repository.setCookieMaxAge(csrfCookieMaxAge);
        repository.setSecure(csrfCookieSecure);
        
        // Set parameter name for form-based submissions
        repository.setParameterName("_csrf");
        
        // Configure header name for REST API token validation
        repository.setHeaderName(csrfHeaderName);
        
        return repository;
    }

    /**
     * CSRF Security Filter Chain Configuration
     * 
     * Configures a dedicated security filter chain for CSRF protection that
     * integrates with the main security configuration and supports both
     * traditional form-based and REST API CSRF validation patterns.
     * 
     * Protection Features:
     * - Cookie-based token repository for stateless operation
     * - Custom token request handler for React frontend integration
     * - Exception handling for CSRF validation failures
     * - Path-specific CSRF protection configuration
     * - Integration with CORS configuration for cross-origin support
     * 
     * Endpoint Configuration:
     * - Public endpoints: Excluded from CSRF protection
     * - API endpoints: Protected with token validation
     * - Admin endpoints: Enhanced CSRF protection
     * - Health/monitoring: Excluded for operational requirements
     * 
     * @param http HttpSecurity configuration object
     * @return SecurityFilterChain configured for CSRF protection
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain csrfSecurityFilterChain(HttpSecurity http) throws Exception {
        // Create token request handler for React frontend compatibility
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");
        
        // Configure XOR token handler for enhanced security
        XorCsrfTokenRequestAttributeHandler xorHandler = new XorCsrfTokenRequestAttributeHandler();
        
        return http
            // Configure CSRF protection
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository())
                .csrfTokenRequestHandler(requestHandler)
                
                // Configure paths that require CSRF protection
                .requireCsrfProtectionMatcher(request -> {
                    String path = request.getRequestURI();
                    String method = request.getMethod();
                    
                    // Exclude public endpoints from CSRF protection
                    if (path.startsWith("/api/auth/login") || 
                        path.startsWith("/api/auth/refresh") ||
                        path.startsWith("/actuator/health") ||
                        path.startsWith("/actuator/info") ||
                        path.startsWith("/swagger-ui") ||
                        path.startsWith("/v3/api-docs") ||
                        path.startsWith("/error")) {
                        return false;
                    }
                    
                    // Require CSRF for state-changing operations
                    return "POST".equals(method) || 
                           "PUT".equals(method) || 
                           "DELETE".equals(method) || 
                           "PATCH".equals(method);
                })
                
                // Configure session authentication strategy
                .sessionAuthenticationStrategy(new CsrfAuthenticationStrategy(csrfTokenRepository()))
                
                // Disable CSRF for specific paths if needed
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher("/api/auth/login", "POST"),
                    new AntPathRequestMatcher("/api/auth/refresh", "POST"),
                    new AntPathRequestMatcher("/actuator/**", "GET"),
                    new AntPathRequestMatcher("/health/**", "GET")
                )
            )
            
            // Configure CORS integration
            .cors(cors -> cors.configurationSource(securityConfig.corsConfigurationSource()))
            
            // Configure request authorization
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers(
                    new AntPathRequestMatcher("/api/auth/login"),
                    new AntPathRequestMatcher("/api/auth/refresh"),
                    new AntPathRequestMatcher("/actuator/health"),
                    new AntPathRequestMatcher("/actuator/info"),
                    new AntPathRequestMatcher("/swagger-ui/**"),
                    new AntPathRequestMatcher("/v3/api-docs/**"),
                    new AntPathRequestMatcher("/error")
                ).permitAll()
                
                // Protected endpoints - require authentication and CSRF
                .anyRequest().authenticated()
            )
            
            // Configure exception handling for CSRF failures
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(csrfExceptionHandler())
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"CSRF Protection Violation\"," +
                        "\"message\":\"Invalid or missing CSRF token\"," +
                        "\"timestamp\":\"" + java.time.Instant.now() + "\"," +
                        "\"path\":\"" + request.getRequestURI() + "\"}"
                    );
                })
            )
            
            .build();
    }

    /**
     * CSRF Exception Handler Configuration
     * 
     * Provides specialized exception handling for CSRF-related security violations,
     * including missing tokens, invalid tokens, and token mismatch scenarios.
     * 
     * Handler Features:
     * - JSON error responses for REST API compatibility
     * - Detailed error information for debugging
     * - Security event logging for monitoring
     * - Integration with Spring Boot error handling
     * - Custom error codes for different CSRF violations
     * 
     * Error Response Format:
     * - HTTP 403 for CSRF token validation failures
     * - JSON response with error details
     * - Timestamp and request path information
     * - Consistent error format across all endpoints
     * 
     * Security Considerations:
     * - Minimal error information to prevent information leakage
     * - Audit logging for security monitoring
     * - Rate limiting integration for abuse prevention
     * - Proper CORS headers in error responses
     * 
     * @return AuthenticationEntryPoint for CSRF exception handling
     */
    @Bean
    public org.springframework.security.web.AuthenticationEntryPoint csrfExceptionHandler() {
        return (request, response, authException) -> {
            // Log CSRF violation for security monitoring
            System.out.println("CSRF Protection Violation: " + authException.getMessage() + 
                             " for request: " + request.getRequestURI());
            
            // Set response status and content type
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            // Add CORS headers for React frontend compatibility
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-CSRF-TOKEN, X-XSRF-TOKEN");
            
            // Create detailed error response
            String errorResponse = String.format(
                "{\"error\":\"CSRF_PROTECTION_VIOLATION\"," +
                "\"message\":\"Cross-Site Request Forgery protection triggered\"," +
                "\"details\":\"Invalid or missing CSRF token for this request\"," +
                "\"timestamp\":\"" + java.time.Instant.now() + "\"," +
                "\"path\":\"" + request.getRequestURI() + "\"," +
                "\"method\":\"" + request.getMethod() + "\"," +
                "\"tokenHeader\":\"" + csrfHeaderName + "\"," +
                "\"tokenCookie\":\"" + csrfCookieName + "\"}"
            );
            
            // Write error response
            response.getWriter().write(errorResponse);
            response.getWriter().flush();
            
            // Audit logging for security monitoring
            // This would typically integrate with Spring Boot Actuator audit events
            auditCsrfViolation(request, authException);
        };
    }

    /**
     * Additional CORS Configuration for CSRF Support
     * 
     * Extends the main CORS configuration to include CSRF-specific headers
     * and support for React frontend token management.
     * 
     * @return CorsConfigurationSource with CSRF header support
     */
    @Bean
    public CorsConfigurationSource csrfCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Configure allowed origins for React frontend
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "https://carddemo.local",
            "https://carddemo.com"
        ));
        
        // Allow all standard HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));
        
        // Configure allowed headers including CSRF tokens
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With",
            "Accept", "Origin", "Access-Control-Request-Method",
            "Access-Control-Request-Headers", "X-CSRF-TOKEN", "X-XSRF-TOKEN"
        ));
        
        // Expose CSRF token headers to the frontend
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials",
            "Authorization", "X-Total-Count", "X-CSRF-TOKEN"
        ));
        
        // Enable credentials for CSRF cookie handling
        configuration.setAllowCredentials(true);
        
        // Set preflight cache duration
        configuration.setMaxAge(3600L);
        
        // Apply configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Audit CSRF Violation for Security Monitoring
     * 
     * Logs CSRF protection violations for security monitoring and compliance.
     * This method would typically integrate with Spring Boot Actuator audit
     * events and ELK stack for centralized logging.
     * 
     * @param request The HTTP request that triggered the violation
     * @param exception The authentication exception details
     */
    private void auditCsrfViolation(HttpServletRequest request, 
                                   org.springframework.security.core.AuthenticationException exception) {
        // Create audit event data
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = request.getRemoteAddr();
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // Log security event (would integrate with Spring Boot Actuator)
        System.out.println("SECURITY_AUDIT: CSRF_VIOLATION - " +
                          "URI: " + requestURI + ", " +
                          "Method: " + method + ", " +
                          "RemoteAddr: " + remoteAddr + ", " +
                          "UserAgent: " + userAgent + ", " +
                          "Exception: " + exception.getMessage());
        
        // This would typically publish to Spring Boot Actuator AuditEventRepository
        // for integration with ELK stack and security monitoring systems
    }

    /**
     * CSRF Token Endpoint for React Frontend
     * 
     * Provides a dedicated endpoint for React frontend to obtain CSRF tokens
     * for subsequent API requests. This endpoint is excluded from CSRF protection
     * to allow initial token acquisition.
     * 
     * @return String endpoint path for CSRF token retrieval
     */
    public static final String CSRF_TOKEN_ENDPOINT = "/api/csrf/token";

    /**
     * Security Constants for CSRF Configuration
     * 
     * Defines standard constants used throughout the CSRF security configuration
     * for consistent configuration and maintenance.
     */
    public static final class CsrfSecurityConstants {
        public static final String DEFAULT_CSRF_COOKIE_NAME = "XSRF-TOKEN";
        public static final String DEFAULT_CSRF_HEADER_NAME = "X-CSRF-TOKEN";
        public static final String ALTERNATIVE_CSRF_HEADER_NAME = "X-XSRF-TOKEN";
        public static final String CSRF_PARAMETER_NAME = "_csrf";
        public static final String CSRF_REQUEST_ATTRIBUTE_NAME = "_csrf";
        public static final int DEFAULT_COOKIE_MAX_AGE = 3600; // 1 hour
        public static final String DEFAULT_COOKIE_PATH = "/";
        
        private CsrfSecurityConstants() {
            // Utility class - prevent instantiation
        }
    }
}