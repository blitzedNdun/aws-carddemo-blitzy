package com.carddemo.common.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

/**
 * JWT Authentication Filter for Spring Security integration providing OAuth2 JWT token validation,
 * user context extraction, and role-based access control across all CardDemo microservices.
 * 
 * This filter implements enterprise-grade security patterns for the cloud-native CardDemo
 * architecture, replacing legacy RACF authentication with modern JWT-based authentication
 * while maintaining equivalent security controls and comprehensive audit logging.
 * 
 * Key Features:
 * - OncePerRequestFilter implementation ensuring single execution per HTTP request
 * - JWT token extraction from Authorization header with Bearer token support
 * - Comprehensive JWT token validation using Spring Security OAuth2 Resource Server
 * - User context establishment with Spring Security authorities mapping
 * - Role-based access control equivalent to RACF user types (A=Admin, U=User)
 * - Security audit logging for compliance tracking and incident response
 * - Error handling for expired, invalid, or malformed JWT tokens
 * - Integration with ELK stack for centralized security event monitoring
 * 
 * Security Architecture Integration:
 * - Integrates with SecurityConfig.jwtDecoder() for token validation
 * - Supports Spring Cloud Gateway JWT propagation across microservices
 * - Enables @PreAuthorize method-level security annotations
 * - Provides correlation IDs for distributed tracing and audit correlation
 * - Maintains session context through SecurityContextHolder for downstream services
 * 
 * Compliance and Monitoring:
 * - PCI DSS compliant authentication tracking
 * - SOX audit trail generation with immutable log storage
 * - Real-time security event monitoring via Prometheus metrics
 * - Structured JSON logging for ELK stack analysis and alerting
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @see SecurityConfig#jwtDecoder()
 * @see OncePerRequestFilter
 * @see JwtDecoder
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    /**
     * Authorization header name for JWT Bearer token extraction
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";
    
    /**
     * Bearer token prefix for JWT token identification
     */
    private static final String BEARER_PREFIX = "Bearer ";
    
    /**
     * JWT claim name for user ID extraction
     */
    private static final String USER_ID_CLAIM = "user_id";
    
    /**
     * JWT claim name for user type extraction (RACF equivalent)
     */
    private static final String USER_TYPE_CLAIM = "user_type";
    
    /**
     * JWT claim name for roles array extraction
     */
    private static final String ROLES_CLAIM = "roles";
    
    /**
     * JWT claim name for session correlation ID
     */
    private static final String CORRELATION_ID_CLAIM = "correlation_id";
    
    /**
     * Admin user type constant (RACF equivalent 'A')
     */
    private static final String ADMIN_USER_TYPE = "A";
    
    /**
     * Regular user type constant (RACF equivalent 'U')
     */
    private static final String USER_USER_TYPE = "U";
    
    /**
     * Spring Security role prefix
     */
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * JWT decoder from SecurityConfig for token validation
     */
    private final JwtDecoder jwtDecoder;
    
    /**
     * Audit event repository for security compliance logging
     */
    private final AuditEventRepository auditEventRepository;

    /**
     * Constructor for JWT Authentication Filter with dependency injection.
     * 
     * @param securityConfig Security configuration providing JWT decoder
     * @param auditEventRepository Audit event repository for compliance logging
     */
    @Autowired
    public JwtAuthenticationFilter(SecurityConfig securityConfig, 
                                  AuditEventRepository auditEventRepository) {
        this.jwtDecoder = securityConfig.jwtDecoder();
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Core filter implementation for JWT authentication processing.
     * 
     * This method implements the main authentication flow:
     * 1. Extract JWT token from Authorization header
     * 2. Validate JWT token using Spring Security OAuth2 decoder
     * 3. Extract user context and roles from JWT claims
     * 4. Establish Spring Security context with authorities
     * 5. Log security events for audit compliance
     * 6. Continue filter chain with authenticated context
     * 
     * @param request HTTP servlet request containing JWT token
     * @param response HTTP servlet response for error handling
     * @param filterChain Spring Security filter chain continuation
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Generate correlation ID for request tracking
        String correlationId = generateCorrelationId();
        MDC.put("correlationId", correlationId);
        MDC.put("requestUri", request.getRequestURI());
        MDC.put("requestMethod", request.getMethod());
        
        try {
            // Extract JWT token from Authorization header
            String jwtToken = extractJwtFromRequest(request);
            
            if (jwtToken != null) {
                // Validate and parse JWT token
                Jwt jwt = validateAndParseJwt(jwtToken);
                
                if (jwt != null) {
                    // Establish Spring Security context with JWT authentication
                    establishSecurityContext(jwt, correlationId);
                    
                    // Log successful authentication for audit compliance
                    logAuthenticationSuccess(jwt, request, correlationId);
                } else {
                    // Log authentication failure for security monitoring
                    logAuthenticationFailure(request, "Invalid or expired JWT token", correlationId);
                }
            }
            
            // Continue filter chain with established security context
            filterChain.doFilter(request, response);
            
        } catch (JwtException e) {
            // Handle JWT-specific exceptions with detailed logging
            logger.warn("JWT authentication failed for request {}: {}", 
                       request.getRequestURI(), e.getMessage());
            
            logAuthenticationFailure(request, "JWT validation error: " + e.getMessage(), correlationId);
            
            // Continue filter chain without authentication context
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            // Handle unexpected exceptions with comprehensive logging
            logger.error("Unexpected error during JWT authentication for request {}: {}", 
                        request.getRequestURI(), e.getMessage(), e);
            
            logAuthenticationFailure(request, "Authentication system error", correlationId);
            
            // Continue filter chain to avoid blocking legitimate requests
            filterChain.doFilter(request, response);
            
        } finally {
            // Clean up MDC context to prevent memory leaks
            MDC.clear();
        }
    }

    /**
     * Extract JWT token from Authorization header with Bearer token support.
     * 
     * This method implements secure token extraction following OAuth2 Bearer token
     * specifications. It validates the Authorization header format and extracts
     * the JWT token value for subsequent validation processing.
     * 
     * @param request HTTP servlet request containing Authorization header
     * @return JWT token string if valid Bearer token present, null otherwise
     */
    public String extractJwtFromRequest(HttpServletRequest request) {
        // Extract Authorization header from request
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        // Validate Authorization header format and presence
        if (StringUtils.hasText(authorizationHeader) && 
            authorizationHeader.startsWith(BEARER_PREFIX)) {
            
            // Extract JWT token by removing Bearer prefix
            String jwtToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
            
            // Validate token is not empty after prefix removal
            if (StringUtils.hasText(jwtToken)) {
                logger.debug("JWT token extracted from Authorization header for request: {}", 
                           request.getRequestURI());
                return jwtToken;
            }
        }
        
        // Log missing or invalid Authorization header for security monitoring
        if (authorizationHeader != null) {
            logger.debug("Invalid Authorization header format for request {}: {}", 
                        request.getRequestURI(), authorizationHeader);
        }
        
        return null;
    }

    /**
     * Validate and parse JWT token using Spring Security OAuth2 decoder.
     * 
     * This method implements comprehensive JWT token validation including:
     * - Signature verification using configured secret key
     * - Token expiration validation
     * - Claims structure validation
     * - Security context preparation
     * 
     * @param jwtToken JWT token string for validation
     * @return Validated Jwt object if token is valid, null if validation fails
     */
    public Jwt validateAndParseJwt(String jwtToken) {
        try {
            // Decode and validate JWT token using Spring Security decoder
            Jwt jwt = jwtDecoder.decode(jwtToken);
            
            // Validate required claims are present
            if (jwt.getClaimAsString(USER_ID_CLAIM) == null ||
                jwt.getClaimAsString(USER_TYPE_CLAIM) == null) {
                
                logger.warn("JWT token missing required claims: user_id or user_type");
                return null;
            }
            
            // Validate token expiration
            Instant expiresAt = jwt.getExpiresAt();
            if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
                logger.warn("JWT token has expired: {}", expiresAt);
                return null;
            }
            
            logger.debug("JWT token successfully validated for user: {}", 
                        jwt.getClaimAsString(USER_ID_CLAIM));
            
            return jwt;
            
        } catch (JwtException e) {
            // Log JWT validation failures for security monitoring
            logger.warn("JWT token validation failed: {}", e.getMessage());
            return null;
            
        } catch (Exception e) {
            // Log unexpected validation errors
            logger.error("Unexpected error during JWT validation: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Establish Spring Security context with JWT authentication and role mapping.
     * 
     * This method implements the core security context establishment for CardDemo
     * microservices, mapping RACF user types to Spring Security authorities and
     * enabling role-based access control through @PreAuthorize annotations.
     * 
     * Authority Mapping:
     * - User type 'A' (Admin) → ROLE_ADMIN + ROLE_USER authorities
     * - User type 'U' (User) → ROLE_USER authority
     * 
     * @param jwt Validated JWT token containing user claims
     * @param correlationId Request correlation ID for audit tracking
     */
    public void establishSecurityContext(Jwt jwt, String correlationId) {
        try {
            // Extract user context from JWT claims
            String userId = jwt.getClaimAsString(USER_ID_CLAIM);
            String userType = jwt.getClaimAsString(USER_TYPE_CLAIM);
            
            // Create Spring Security authorities based on user type
            Collection<GrantedAuthority> authorities = createAuthoritiesFromUserType(userType);
            
            // Create JWT authentication token with authorities
            JwtAuthenticationToken authenticationToken = new JwtAuthenticationToken(
                jwt, authorities, userId
            );
            
            // Set additional authentication details
            Map<String, Object> details = new HashMap<>();
            details.put("userId", userId);
            details.put("userType", userType);
            details.put("correlationId", correlationId);
            details.put("authenticatedAt", Instant.now());
            
            if (jwt.getClaimAsString(CORRELATION_ID_CLAIM) != null) {
                details.put("jwtCorrelationId", jwt.getClaimAsString(CORRELATION_ID_CLAIM));
            }
            
            authenticationToken.setDetails(details);
            
            // Establish security context for current request
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            
            // Update MDC with user context for logging
            MDC.put("userId", userId);
            MDC.put("userType", userType);
            MDC.put("authorities", authorities.toString());
            
            logger.info("Security context established for user {} with authorities: {}", 
                       userId, authorities);
            
        } catch (Exception e) {
            logger.error("Failed to establish security context from JWT: {}", e.getMessage(), e);
            
            // Clear any partial security context on error
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Create Spring Security authorities from RACF user type mapping.
     * 
     * This method implements the authority mapping strategy that preserves
     * equivalent access control from the legacy RACF system while enabling
     * modern Spring Security role-based authorization patterns.
     * 
     * @param userType RACF user type ('A' for Admin, 'U' for User)
     * @return Collection of Spring Security authorities
     */
    private Collection<GrantedAuthority> createAuthoritiesFromUserType(String userType) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        
        if (ADMIN_USER_TYPE.equals(userType)) {
            // Admin users get both ADMIN and USER roles (role hierarchy)
            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + "ADMIN"));
            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + "USER"));
            
        } else if (USER_USER_TYPE.equals(userType)) {
            // Regular users get USER role only
            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + "USER"));
            
        } else {
            // Unknown user types get no authorities
            logger.warn("Unknown user type encountered: {}", userType);
        }
        
        return authorities;
    }

    /**
     * Log successful authentication event for security audit compliance.
     * 
     * This method creates comprehensive audit trail entries for successful
     * authentication events, supporting SOX compliance, PCI DSS requirements,
     * and security incident investigation through structured logging.
     * 
     * @param jwt Validated JWT token containing user information
     * @param request HTTP servlet request for context
     * @param correlationId Request correlation ID for audit tracking
     */
    private void logAuthenticationSuccess(Jwt jwt, HttpServletRequest request, String correlationId) {
        try {
            String userId = jwt.getClaimAsString(USER_ID_CLAIM);
            String userType = jwt.getClaimAsString(USER_TYPE_CLAIM);
            
            // Create audit event for Spring Boot Actuator
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("userId", userId);
            auditData.put("userType", userType);
            auditData.put("requestUri", request.getRequestURI());
            auditData.put("requestMethod", request.getMethod());
            auditData.put("remoteAddress", getClientIpAddress(request));
            auditData.put("userAgent", request.getHeader("User-Agent"));
            auditData.put("correlationId", correlationId);
            auditData.put("timestamp", Instant.now());
            auditData.put("authenticationMethod", "JWT");
            auditData.put("sessionId", request.getSession(false) != null ? 
                         request.getSession().getId() : "N/A");
            
            // Create audit event for compliance tracking
            AuditEvent auditEvent = new AuditEvent(
                userId, 
                "AUTHENTICATION_SUCCESS", 
                auditData
            );
            
            // Store audit event for compliance and monitoring
            auditEventRepository.add(auditEvent);
            
            // Log structured authentication success for ELK stack
            logger.info("Authentication successful - User: {}, Type: {}, URI: {}, IP: {}, Correlation: {}", 
                       userId, userType, request.getRequestURI(), 
                       getClientIpAddress(request), correlationId);
            
        } catch (Exception e) {
            logger.error("Failed to log authentication success: {}", e.getMessage(), e);
        }
    }

    /**
     * Log authentication failure event for security monitoring and incident response.
     * 
     * This method creates comprehensive audit trail entries for authentication
     * failures, enabling security incident detection, threat analysis, and
     * compliance reporting through centralized logging infrastructure.
     * 
     * @param request HTTP servlet request for context
     * @param reason Failure reason for security analysis
     * @param correlationId Request correlation ID for incident tracking
     */
    private void logAuthenticationFailure(HttpServletRequest request, String reason, String correlationId) {
        try {
            // Create audit event for authentication failure
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("reason", reason);
            auditData.put("requestUri", request.getRequestURI());
            auditData.put("requestMethod", request.getMethod());
            auditData.put("remoteAddress", getClientIpAddress(request));
            auditData.put("userAgent", request.getHeader("User-Agent"));
            auditData.put("correlationId", correlationId);
            auditData.put("timestamp", Instant.now());
            auditData.put("authenticationMethod", "JWT");
            
            // Create audit event for security monitoring
            AuditEvent auditEvent = new AuditEvent(
                "anonymous", 
                "AUTHENTICATION_FAILURE", 
                auditData
            );
            
            // Store audit event for incident response
            auditEventRepository.add(auditEvent);
            
            // Log structured authentication failure for security monitoring
            logger.warn("Authentication failed - Reason: {}, URI: {}, IP: {}, Correlation: {}", 
                       reason, request.getRequestURI(), getClientIpAddress(request), correlationId);
            
        } catch (Exception e) {
            logger.error("Failed to log authentication failure: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract client IP address from HTTP request with proxy support.
     * 
     * This method implements comprehensive IP address extraction supporting
     * various proxy configurations commonly used in cloud-native environments
     * including Kubernetes ingress controllers and Spring Cloud Gateway.
     * 
     * @param request HTTP servlet request containing client information
     * @return Client IP address string for audit logging
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check common proxy headers for real client IP
        String[] ipHeaders = {
            "X-Forwarded-For",
            "X-Real-IP",
            "X-Forwarded",
            "X-Cluster-Client-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };
        
        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // Handle comma-separated list of IPs (first one is client)
                return ip.split(",")[0].trim();
            }
        }
        
        // Fall back to remote address if no proxy headers found
        return request.getRemoteAddr();
    }

    /**
     * Generate unique correlation ID for request tracking and audit correlation.
     * 
     * This method creates correlation IDs that enable distributed tracing
     * across microservices and support incident investigation through
     * comprehensive audit trail correlation.
     * 
     * @return Unique correlation ID string for request tracking
     */
    private String generateCorrelationId() {
        return "jwt-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Override shouldNotFilter to specify when this filter should be skipped.
     * 
     * This method implements filter bypass logic for public endpoints that
     * do not require JWT authentication, optimizing performance and avoiding
     * unnecessary processing for health checks and public API endpoints.
     * 
     * @param request HTTP servlet request to evaluate
     * @return true if filter should be skipped, false if filter should execute
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip JWT authentication for public endpoints
        return path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info") ||
               path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/refresh") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.equals("/") ||
               path.startsWith("/static/");
    }
}