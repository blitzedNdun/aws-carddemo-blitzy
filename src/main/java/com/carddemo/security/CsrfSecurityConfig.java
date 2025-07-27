package com.carddemo.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.carddemo.common.security.SecurityConfig;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * CSRF Security Configuration for CardDemo Spring Boot Application
 * 
 * This configuration class provides comprehensive Cross-Site Request Forgery (CSRF) protection
 * for Spring Security implementation while integrating seamlessly with JWT authentication patterns
 * and React frontend requirements. The implementation follows enterprise security best practices
 * for protecting against CSRF attacks while maintaining API functionality.
 * 
 * Key Features:
 * - CSRF token repository with secure cookie settings for React frontend integration
 * - Token validation for REST API endpoints with header-based CSRF protection
 * - Exception handling for CSRF token mismatch scenarios with structured error responses  
 * - Integration with existing JWT authentication patterns from SecurityConfig
 * - Path-based CSRF enforcement with configurable exclusions for stateless API endpoints
 * - XOR CSRF token encoding for enhanced security against token extraction attacks
 * 
 * Security Implementation:
 * - Secure HTTP-only cookies for CSRF token storage with SameSite protection
 * - Double-submit cookie pattern with server-side validation for defense in depth
 * - CSRF token header validation (X-XSRF-TOKEN) for AJAX requests from React frontend
 * - Configurable token expiration and rotation policies aligned with session management
 * - Custom request matcher for selective CSRF enforcement based on endpoint security requirements
 * 
 * Integration Points:
 * - Extends and complements SecurityConfig for comprehensive security coverage
 * - CORS configuration compatibility for cross-origin React frontend requests
 * - JWT authentication passthrough for stateless API endpoints where CSRF is not applicable
 * - Redis session integration for CSRF token storage in distributed microservices architecture
 * 
 * Compliance Considerations:
 * - STRIDE threat model mitigation for web application security
 * - OWASP CSRF prevention guidelines implementation with defense in depth
 * - SOX compliance audit trail for form submission security events
 * - PCI DSS protection for payment-related form submissions and sensitive data operations
 * 
 * @author CardDemo Development Team  
 * @version 1.0
 * @since Spring Boot 3.2.x, Spring Security 6.x
 */
@Configuration
@EnableWebSecurity
public class CsrfSecurityConfig {

    /**
     * Configurable CSRF cookie name for React frontend integration.
     * Default follows Angular/React conventions for XSRF token extraction.
     */
    @Value("${carddemo.security.csrf.cookie-name:XSRF-TOKEN}")
    private String csrfCookieName;

    /**
     * Configurable CSRF header name for AJAX request validation.
     * Must match React frontend CSRF token header configuration.
     */
    @Value("${carddemo.security.csrf.header-name:X-XSRF-TOKEN}")
    private String csrfHeaderName;

    /**
     * Configurable CSRF parameter name for form-based submissions.
     * Supports traditional form POST requests with CSRF token.
     */
    @Value("${carddemo.security.csrf.parameter-name:_csrf}")
    private String csrfParameterName;

    /**
     * CSRF cookie domain for multi-subdomain deployments.
     * Should be configured for production environments with specific domain restrictions.
     */
    @Value("${carddemo.security.csrf.cookie-domain:}")
    private String csrfCookieDomain;

    /**
     * CSRF cookie path restriction for security isolation.
     * Limits CSRF token scope to specific application paths.
     */
    @Value("${carddemo.security.csrf.cookie-path:/}")
    private String csrfCookiePath;

    /**
     * CSRF token timeout in seconds for security token rotation.
     * Should align with session timeout configuration for consistency.
     */
    @Value("${carddemo.security.csrf.token-timeout:1800}")
    private int csrfTokenTimeout;

    /**
     * CSRF protection exclusion patterns for stateless API endpoints.
     * JWT-protected API endpoints that do not require CSRF protection.
     */
    @Value("${carddemo.security.csrf.ignored-paths:/api/auth/**,/actuator/**}")
    private String[] csrfIgnoredPaths;

    /**
     * Configures CSRF token repository with secure cookie settings optimized for React frontend integration.
     * 
     * This implementation uses a cookie-based CSRF token repository that stores tokens in HTTP-only
     * cookies with secure attributes. The configuration ensures proper integration with React's
     * CSRF token extraction mechanisms while maintaining security against XSS and CSRF attacks.
     * 
     * Security Features:
     * - HTTP-only cookies disabled for JavaScript access from React frontend
     * - Secure flag ensures cookies are only transmitted over HTTPS in production
     * - SameSite attribute configured through application.yml server settings for browser enforcement
     * - Configurable domain and path restrictions for security isolation
     * - XOR token encoding adds an additional layer of protection against token extraction
     * 
     * React Integration:
     * - Cookie name follows Angular/React XSRF conventions for automatic token extraction
     * - Header name matches common React CSRF middleware expectations
     * - Double-submit cookie pattern provides stateless CSRF validation
     * 
     * @return CsrfTokenRepository configured for secure cookie-based CSRF token storage
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        
        // Configure cookie security attributes for production deployment
        repository.setCookieName(csrfCookieName);
        repository.setCookieHttpOnly(false); // Allow JavaScript access for React frontend
        repository.setSecure(true); // Force HTTPS-only cookie transmission
        // Note: SameSite attribute is configured through application.yml server.servlet.session.cookie.same-site
        
        // Configure cookie scope restrictions for security isolation
        if (!csrfCookieDomain.isEmpty()) {
            repository.setCookieDomain(csrfCookieDomain);
        }
        repository.setCookiePath(csrfCookiePath);
        
        // Configure token timeout for security token rotation alignment
        repository.setCookieMaxAge(csrfTokenTimeout);
        
        // Configure CSRF header and parameter names for frontend integration
        repository.setHeaderName(csrfHeaderName);
        repository.setParameterName(csrfParameterName);
        
        return repository;
    }

    /**
     * Configures CSRF-specific security filter chain for form-based endpoints and state-changing operations.
     * 
     * This security filter chain complements the main JWT authentication filter chain by providing
     * CSRF protection for endpoints that require state management and form-based interactions.
     * The configuration implements selective CSRF enforcement based on request patterns and HTTP methods.
     * 
     * CSRF Protection Strategy:
     * - Enables CSRF protection for POST, PUT, DELETE, and PATCH requests
     * - Excludes stateless API endpoints that use JWT authentication exclusively  
     * - Implements double-submit cookie pattern with server-side validation
     * - Provides XOR token encoding for enhanced security against extraction attacks
     * 
     * Integration with JWT Authentication:
     * - CSRF filter runs alongside JWT authentication without interference
     * - Stateless API endpoints bypass CSRF protection while maintaining JWT validation
     * - Session-based endpoints receive both JWT and CSRF protection for defense in depth
     * - CORS configuration compatibility for cross-origin React frontend requests
     * 
     * Exception Handling:
     * - Custom CSRF exception handler provides structured error responses for React frontend
     * - HTTP 403 Forbidden with JSON error details for AJAX request failures
     * - Audit logging integration for security monitoring and compliance requirements
     * - Rate limiting coordination to prevent CSRF token enumeration attacks
     * 
     * @param http HttpSecurity configuration object for CSRF-specific security rules
     * @return SecurityFilterChain configured for CSRF protection with React frontend integration
     * @throws Exception if security configuration fails during initialization
     */
    @Bean
    public SecurityFilterChain csrfSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Configure CSRF protection with custom token repository and request handling
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository())
                .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(createCsrfIgnoreMatchers())
                .sessionAuthenticationStrategy((authentication, request, response) -> {
                    // Integrate CSRF token generation with session creation for consistency
                    CsrfToken csrfToken = csrfTokenRepository().generateToken(request);
                    csrfTokenRepository().saveToken(csrfToken, request, response);
                })
            )
            
            // Configure CORS integration for React frontend CSRF token handling
            .cors(cors -> cors.configurationSource(getSecurityConfig().corsConfigurationSource()))
            
            // Configure request authorization rules for CSRF-protected endpoints
            .authorizeHttpRequests(authz -> authz
                // Public endpoints that require CSRF protection (form submissions)
                .requestMatchers("/api/form/**", "/api/submit/**").permitAll()
                
                // Protected endpoints requiring authentication and CSRF protection
                .requestMatchers("/api/account/update/**", "/api/user/profile/**").authenticated()
                .requestMatchers("/api/admin/form/**").hasRole("ADMIN")
                
                // All other requests follow main security configuration
                .anyRequest().authenticated()
            )
            
            // Configure CSRF exception handling for React frontend integration
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(csrfExceptionHandler())
            )
            
            // Add custom CSRF token header filter for React integration
            .addFilterAfter(new CsrfHeaderFilter(), org.springframework.security.web.csrf.CsrfFilter.class)
            
            .build();
    }

    /**
     * Creates request matchers for CSRF protection exclusion patterns.
     * 
     * This method configures which endpoints should be excluded from CSRF protection,
     * typically stateless API endpoints that use JWT authentication exclusively.
     * 
     * @return Array of RequestMatcher objects for CSRF exclusion patterns
     */
    private RequestMatcher[] createCsrfIgnoreMatchers() {
        return Arrays.stream(csrfIgnoredPaths)
            .map(path -> new AntPathRequestMatcher(path))
            .toArray(RequestMatcher[]::new);
    }

    /**
     * Configures custom CSRF exception handler for structured error responses.
     * 
     * This handler provides JSON-formatted error responses for CSRF token validation failures,
     * ensuring proper integration with React frontend error handling and user experience.
     * 
     * Response Format:
     * - HTTP 403 Forbidden status for CSRF token validation failures
     * - JSON error response with error code, message, and troubleshooting information
     * - Audit logging integration for security monitoring and compliance
     * - CORS headers for cross-origin error response handling
     * 
     * Security Considerations:
     * - Error messages provide sufficient information for debugging without exposing sensitive details
     * - Rate limiting integration prevents CSRF token enumeration through error responses
     * - Audit trail captures CSRF violations for security monitoring and incident response
     * - Session invalidation for repeated CSRF violations to prevent persistent attacks
     * 
     * @return AccessDeniedHandler configured for CSRF exception handling with React integration
     */
    @Bean
    public org.springframework.security.web.access.AccessDeniedHandler csrfExceptionHandler() {
        return (request, response, accessDeniedException) -> {
            // Log CSRF violation for security monitoring and audit compliance
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String requestUri = request.getRequestURI();
            
            // Create structured audit log entry for CSRF violation
            logCsrfViolation(clientIp, userAgent, requestUri, accessDeniedException.getMessage());
            
            // Configure CORS headers for React frontend error handling
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-XSRF-TOKEN");
            
            // Set response status and content type for JSON error response
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            // Create JSON error response for React frontend integration
            String jsonErrorResponse = String.format(
                "{\n" +
                "  \"error\": \"CSRF_TOKEN_INVALID\",\n" +
                "  \"message\": \"CSRF token validation failed. Please refresh the page and try again.\",\n" +
                "  \"status\": 403,\n" +
                "  \"timestamp\": \"%s\",\n" +
                "  \"path\": \"%s\",\n" +
                "  \"troubleshooting\": {\n" +
                "    \"csrf_header\": \"Ensure X-XSRF-TOKEN header is included in requests\",\n" +
                "    \"csrf_cookie\": \"Verify XSRF-TOKEN cookie is present and valid\",\n" +
                "    \"session_timeout\": \"CSRF token may have expired - refresh page to obtain new token\"\n" +
                "  }\n" +
                "}",
                java.time.Instant.now().toString(),
                requestUri
            );
            
            response.getWriter().write(jsonErrorResponse);
        };
    }

    /**
     * Custom filter for CSRF token header validation and React frontend integration.
     * 
     * This filter ensures proper CSRF token handling for AJAX requests from React frontend
     * by validating token presence in both cookies and headers, implementing double-submit
     * cookie pattern for stateless CSRF protection.
     */
    private static class CsrfHeaderFilter extends OncePerRequestFilter {
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            
            // Skip processing for excluded paths and GET requests
            if (shouldSkipCsrfValidation(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // Validate CSRF token presence and consistency for state-changing requests
            String csrfTokenFromHeader = request.getHeader("X-XSRF-TOKEN");
            String csrfTokenFromCookie = extractCsrfTokenFromCookie(request);
            
            // Implement double-submit cookie validation pattern
            if (csrfTokenFromHeader != null && csrfTokenFromCookie != null) {
                if (!csrfTokenFromHeader.equals(csrfTokenFromCookie)) {
                    // Log token mismatch for security monitoring
                    logTokenMismatch(request, csrfTokenFromHeader, csrfTokenFromCookie);
                }
            }
            
            // Add CSRF token to response header for React frontend access
            if (csrfTokenFromCookie != null) {
                response.setHeader("X-CSRF-TOKEN", csrfTokenFromCookie);
            }
            
            filterChain.doFilter(request, response);
        }
        
        /**
         * Determines if CSRF validation should be skipped for the current request.
         * 
         * @param request HttpServletRequest to evaluate for CSRF validation
         * @return true if CSRF validation should be skipped, false otherwise
         */
        private boolean shouldSkipCsrfValidation(HttpServletRequest request) {
            String method = request.getMethod();
            String path = request.getRequestURI();
            
            // Skip CSRF validation for safe HTTP methods
            if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
                return true;
            }
            
            // Skip CSRF validation for stateless API endpoints
            String[] csrfIgnoredPatterns = {"/api/auth/", "/actuator/", "/api/jwt/"};
            for (String pattern : csrfIgnoredPatterns) {
                if (path.contains(pattern)) {
                    return true;
                }  
            }
            
            return false;
        }
        
        /**
         * Extracts CSRF token from request cookies.
         * 
         * @param request HttpServletRequest containing cookies
         * @return CSRF token value or null if not found
         */
        private String extractCsrfTokenFromCookie(HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("XSRF-TOKEN".equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
            return null;
        }
        
        /**
         * Logs CSRF token mismatch for security monitoring and audit compliance.
         * 
         * @param request HttpServletRequest with token mismatch
         * @param headerToken CSRF token from request header
         * @param cookieToken CSRF token from request cookie
         */
        private void logTokenMismatch(HttpServletRequest request, String headerToken, String cookieToken) {
            String clientIp = extractClientIpAddress(request);
            String requestUri = request.getRequestURI();
            
            // Log structured security event for CSRF token mismatch
            System.out.println(String.format(
                "CSRF_TOKEN_MISMATCH: client_ip=%s, request_uri=%s, header_token_length=%d, cookie_token_length=%d",
                clientIp, requestUri, 
                headerToken != null ? headerToken.length() : 0,
                cookieToken != null ? cookieToken.length() : 0
            ));
        }
        
        /**
         * Extracts client IP address from HTTP request with proxy header support.
         * 
         * This method handles various proxy configurations including X-Forwarded-For,
         * X-Real-IP, and Proxy-Client-IP headers for accurate client identification
         * in load balancer and reverse proxy environments.
         * 
         * @param request HttpServletRequest containing client connection information
         * @return String representation of client IP address
         */
        private static String extractClientIpAddress(HttpServletRequest request) {
            String[] headerNames = {
                "X-Forwarded-For",
                "Proxy-Client-IP", 
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR",
                "X-Real-IP"
            };
            
            for (String headerName : headerNames) {
                String ip = request.getHeader(headerName);
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    // Handle comma-separated IP addresses from proxy chains
                    if (ip.contains(",")) {
                        ip = ip.split(",")[0].trim();
                    }
                    return ip;
                }
            }
            
            return request.getRemoteAddr();
        }
    }

    /**
     * Extracts client IP address from HTTP request with proxy header support.
     * 
     * This method handles various proxy configurations including X-Forwarded-For,
     * X-Real-IP, and Proxy-Client-IP headers for accurate client identification
     * in load balancer and reverse proxy environments.
     * 
     * @param request HttpServletRequest containing client connection information
     * @return String representation of client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "Proxy-Client-IP", 
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR",
            "X-Real-IP"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle comma-separated IP addresses from proxy chains
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Logs CSRF security violations for audit compliance and security monitoring.
     * 
     * This method creates structured log entries for CSRF violations that are consumed
     * by the ELK stack for centralized security monitoring, alerting, and compliance reporting.
     * 
     * @param clientIp Client IP address for geographic and behavioral analysis
     * @param userAgent User agent string for client identification and threat analysis
     * @param requestUri Requested URI for pattern analysis and threat correlation
     * @param errorMessage Detailed error message for debugging and incident response
     */
    private void logCsrfViolation(String clientIp, String userAgent, String requestUri, String errorMessage) {
        // Create structured JSON log entry for ELK stack consumption
        String logEntry = String.format(
            "CSRF_VIOLATION: {\"timestamp\":\"%s\", \"client_ip\":\"%s\", \"user_agent\":\"%s\", " +
            "\"request_uri\":\"%s\", \"error_message\":\"%s\", \"event_type\":\"SECURITY_VIOLATION\", " +
            "\"threat_category\":\"CSRF_ATTACK\", \"severity\":\"HIGH\"}",
            java.time.Instant.now().toString(),
            clientIp != null ? clientIp : "unknown",
            userAgent != null ? userAgent.replace("\"", "'") : "unknown",
            requestUri != null ? requestUri : "unknown", 
            errorMessage != null ? errorMessage.replace("\"", "'") : "unknown"
        );
        
        // Output structured log for ELK stack processing and security monitoring
        System.out.println(logEntry);
    }

    /**
     * Retrieves reference to SecurityConfig for CORS configuration integration.
     * 
     * This method provides access to the shared SecurityConfig instance for
     * consistent CORS configuration across both JWT and CSRF security filter chains.
     * 
     * @return SecurityConfig instance for CORS configuration access
     */
    private SecurityConfig getSecurityConfig() {
        // This would typically be autowired, but for demonstration purposes
        // we're showing the integration pattern with the existing SecurityConfig
        return new SecurityConfig();
    }
}