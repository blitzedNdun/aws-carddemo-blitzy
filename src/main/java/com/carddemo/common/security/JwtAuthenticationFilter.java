package com.carddemo.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter for Spring Security integration providing OAuth2 JWT token validation,
 * user context extraction, and role-based access control across all CardDemo microservices.
 * 
 * This filter implements the cloud-native security architecture requirements:
 * - JWT token extraction from Authorization header with Bearer token support
 * - Comprehensive JWT token validation using Spring Security OAuth2 resource server
 * - User context establishment with Spring Security authorities mapping from RACF user types
 * - Role-based access control equivalent to legacy RACF authorization patterns
 * - Comprehensive security audit logging for compliance tracking and monitoring
 * - Error handling for expired, invalid, or malformed JWT tokens with detailed audit trails
 * 
 * The filter replaces traditional CICS/RACF authentication with modern JWT-based stateless 
 * authentication while preserving equivalent security controls and audit capabilities.
 * 
 * Security Features:
 * - OncePerRequestFilter implementation ensures consistent JWT processing per HTTP request
 * - RSA-256 signature validation for JWT token integrity and authenticity
 * - Automatic role mapping from JWT claims to Spring Security authorities
 * - Comprehensive audit event publishing for security monitoring and compliance
 * - Graceful error handling with detailed security event logging
 * - Support for horizontal scaling through stateless authentication model
 * 
 * Integration Points:
 * - Spring Security filter chain integration at appropriate precedence
 * - Spring Boot Actuator audit event framework for compliance logging
 * - ELK stack integration through structured JSON audit events
 * - Prometheus metrics collection for security monitoring dashboards
 * 
 * @author CardDemo Development Team
 * @version 1.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_TYPE_CLAIM = "user_type";
    private static final String USER_ID_CLAIM = "sub";
    private static final String SESSION_ID_CLAIM = "session_id";
    
    // RACF user type constants for role mapping
    private static final String ADMIN_USER_TYPE = "A";
    private static final String REGULAR_USER_TYPE = "U";
    
    // Security role constants matching SecurityConfig definitions
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";

    private final JwtDecoder jwtDecoder;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructor for dependency injection of required security components.
     * 
     * @param securityConfig SecurityConfig instance providing JWT decoder and security configuration
     * @param eventPublisher ApplicationEventPublisher for security audit event publishing
     */
    @Autowired
    public JwtAuthenticationFilter(SecurityConfig securityConfig, ApplicationEventPublisher eventPublisher) {
        this.jwtDecoder = securityConfig.jwtDecoder();
        this.eventPublisher = eventPublisher;
    }

    /**
     * Core filter processing method implementing JWT authentication and authorization.
     * 
     * This method processes each HTTP request to extract and validate JWT tokens,
     * establish Spring Security authentication context, and log security events
     * for audit compliance. The method implements the complete JWT authentication
     * flow as defined in the security architecture.
     * 
     * Processing Flow:
     * 1. Extract JWT token from Authorization header with Bearer prefix validation
     * 2. Validate JWT token signature, expiration, and claims using Spring Security OAuth2
     * 3. Extract user identity and role information from validated JWT claims
     * 4. Map RACF user types to Spring Security authorities for role-based access control
     * 5. Establish SecurityContext with authenticated user and granted authorities
     * 6. Publish comprehensive audit events for security monitoring and compliance
     * 7. Handle authentication errors with detailed logging and appropriate HTTP responses
     * 
     * Security Controls:
     * - JWT token integrity validation through RSA-256 signature verification
     * - Token expiration checking to prevent replay attacks
     * - User type validation and role mapping for authorization
     * - Comprehensive audit trail for all authentication attempts
     * - Error handling with security event logging for failed authentication
     * 
     * @param request HttpServletRequest containing client request with potential JWT token
     * @param response HttpServletResponse for sending authentication error responses
     * @param filterChain FilterChain for continuing request processing after authentication
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail during request processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Extract JWT token from Authorization header
            String jwtToken = extractJwtFromRequest(request);
            
            if (jwtToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Validate and parse JWT token
                Jwt jwt = validateAndParseJwt(jwtToken, request);
                
                if (jwt != null) {
                    // Establish Spring Security context with validated JWT
                    establishSecurityContext(jwt, request);
                    
                    // Publish successful authentication audit event
                    publishSecurityAuditEvent("JWT_AUTHENTICATION_SUCCESS", 
                        Map.of("userId", jwt.getClaimAsString(USER_ID_CLAIM),
                               "userType", jwt.getClaimAsString(USER_TYPE_CLAIM),
                               "sessionId", jwt.getClaimAsString(SESSION_ID_CLAIM),
                               "requestURI", request.getRequestURI(),
                               "remoteAddr", request.getRemoteAddr(),
                               "userAgent", request.getHeader("User-Agent"),
                               "tokenExpiration", jwt.getExpiresAt().toString()));
                }
            }
            
            // Continue filter chain processing
            filterChain.doFilter(request, response);
            
        } catch (JwtException | IllegalArgumentException e) {
            // Handle JWT validation errors with comprehensive audit logging
            handleJwtAuthenticationError(request, response, e);
            
            // Continue filter chain to allow Spring Security to handle the error
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Extracts JWT token from Authorization header with Bearer prefix validation.
     * 
     * This method implements secure token extraction following OAuth2 bearer token
     * standards. It validates the Authorization header format and extracts the
     * JWT token while performing input validation and security checks.
     * 
     * Security Validations:
     * - Validates Authorization header presence and format
     * - Confirms Bearer prefix for OAuth2 compliance
     * - Performs input sanitization and length validation
     * - Logs token extraction attempts for audit purposes
     * 
     * @param request HttpServletRequest containing the Authorization header
     * @return String JWT token if present and valid, null otherwise
     */
    public String extractJwtFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
            
            // Validate token format and length
            if (StringUtils.hasText(token) && token.length() > 20) {
                // Log token extraction for audit purposes (without exposing token content)
                publishSecurityAuditEvent("JWT_TOKEN_EXTRACTION_ATTEMPT", 
                    Map.of("requestURI", request.getRequestURI(),
                           "remoteAddr", request.getRemoteAddr(),
                           "tokenLength", token.length(),
                           "hasValidFormat", true));
                
                return token;
            }
        }
        
        // Log failed token extraction attempts
        if (StringUtils.hasText(authorizationHeader)) {
            publishSecurityAuditEvent("JWT_TOKEN_EXTRACTION_FAILED", 
                Map.of("requestURI", request.getRequestURI(),
                       "remoteAddr", request.getRemoteAddr(),
                       "headerPresent", true,
                       "validBearerPrefix", authorizationHeader.startsWith(BEARER_PREFIX)));
        }
        
        return null;
    }

    /**
     * Validates and parses JWT token using Spring Security OAuth2 JWT decoder.
     * 
     * This method performs comprehensive JWT token validation including signature
     * verification, expiration checking, and claims validation. It uses the
     * Spring Security OAuth2 resource server JWT decoder configured in SecurityConfig.
     * 
     * Validation Steps:
     * - RSA-256 signature verification for token integrity
     * - Token expiration validation to prevent replay attacks
     * - Issuer validation for token authenticity
     * - Claims structure validation for required fields
     * - User type validation for authorization mapping
     * 
     * Error Handling:
     * - JwtValidationException for expired or invalid tokens
     * - BadJwtException for malformed token structure
     * - IllegalArgumentException for invalid token format
     * - Comprehensive audit logging for all validation failures
     * 
     * @param jwtToken String JWT token to validate and parse
     * @param request HttpServletRequest for audit logging context
     * @return Jwt parsed and validated JWT object, null if validation fails
     */
    public Jwt validateAndParseJwt(String jwtToken, HttpServletRequest request) {
        try {
            // Decode and validate JWT token using Spring Security OAuth2 decoder
            Jwt jwt = jwtDecoder.decode(jwtToken);
            
            // Validate required claims presence
            String userId = jwt.getClaimAsString(USER_ID_CLAIM);
            String userType = jwt.getClaimAsString(USER_TYPE_CLAIM);
            
            if (!StringUtils.hasText(userId) || !StringUtils.hasText(userType)) {
                throw new BadJwtException("JWT token missing required claims: userId or userType");
            }
            
            // Validate user type for known values
            if (!ADMIN_USER_TYPE.equals(userType) && !REGULAR_USER_TYPE.equals(userType)) {
                throw new BadJwtException("JWT token contains invalid user type: " + userType);
            }
            
            // Log successful token validation
            publishSecurityAuditEvent("JWT_TOKEN_VALIDATION_SUCCESS", 
                Map.of("userId", userId,
                       "userType", userType,
                       "tokenSubject", jwt.getSubject(),
                       "tokenIssuer", jwt.getIssuer() != null ? jwt.getIssuer().toString() : "unknown",
                       "tokenExpiration", jwt.getExpiresAt().toString(),
                       "requestURI", request.getRequestURI(),
                       "remoteAddr", request.getRemoteAddr()));
            
            return jwt;
            
        } catch (JwtValidationException e) {
            // Handle token validation errors (expired, invalid signature, etc.)
            publishSecurityAuditEvent("JWT_TOKEN_VALIDATION_FAILED", 
                Map.of("validationError", e.getMessage(),
                       "errorType", "JwtValidationException",
                       "requestURI", request.getRequestURI(),
                       "remoteAddr", request.getRemoteAddr(),
                       "tokenLength", jwtToken.length()));
            
            throw e;
            
        } catch (BadJwtException e) {
            // Handle malformed JWT tokens
            publishSecurityAuditEvent("JWT_TOKEN_MALFORMED", 
                Map.of("parseError", e.getMessage(),
                       "errorType", "BadJwtException",
                       "requestURI", request.getRequestURI(),
                       "remoteAddr", request.getRemoteAddr(),
                       "tokenLength", jwtToken.length()));
            
            throw e;
            
        } catch (IllegalArgumentException e) {
            // Handle invalid token format
            publishSecurityAuditEvent("JWT_TOKEN_FORMAT_INVALID", 
                Map.of("formatError", e.getMessage(),
                       "errorType", "IllegalArgumentException",
                       "requestURI", request.getRequestURI(),
                       "remoteAddr", request.getRemoteAddr(),
                       "tokenLength", jwtToken.length()));
            
            throw e;
        }
    }

    /**
     * Establishes Spring Security authentication context from validated JWT token.
     * 
     * This method creates the Spring Security authentication context using the
     * validated JWT token claims. It performs role mapping from RACF user types
     * to Spring Security authorities and establishes the security context for
     * the current request processing.
     * 
     * Authentication Context Setup:
     * - Extract user identity from JWT subject claim
     * - Map RACF user types to Spring Security roles (A->ADMIN, U->USER)
     * - Create UsernamePasswordAuthenticationToken with granted authorities
     * - Establish SecurityContext for role-based access control
     * - Set authentication details for audit and monitoring
     * 
     * Role Mapping:
     * - Admin users (type 'A') receive both ROLE_ADMIN and ROLE_USER authorities
     * - Regular users (type 'U') receive ROLE_USER authority only
     * - Invalid user types result in no granted authorities
     * 
     * @param jwt Validated JWT token containing user identity and role information
     * @param request HttpServletRequest for authentication details
     */
    public void establishSecurityContext(Jwt jwt, HttpServletRequest request) {
        try {
            // Extract user identity and role information from JWT claims
            String userId = jwt.getClaimAsString(USER_ID_CLAIM);
            String userType = jwt.getClaimAsString(USER_TYPE_CLAIM);
            String sessionId = jwt.getClaimAsString(SESSION_ID_CLAIM);
            
            // Map RACF user types to Spring Security authorities
            Collection<SimpleGrantedAuthority> authorities = mapUserTypeToAuthorities(userType);
            
            // Create authentication token with user identity and authorities
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
            
            // Set authentication details for audit and monitoring
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // Establish security context for current request
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Log successful security context establishment
            publishSecurityAuditEvent("SECURITY_CONTEXT_ESTABLISHED", 
                Map.of("userId", userId,
                       "userType", userType,
                       "sessionId", sessionId != null ? sessionId : "unknown",
                       "grantedAuthorities", authorities.stream()
                           .map(SimpleGrantedAuthority::getAuthority)
                           .collect(Collectors.joining(",")),
                       "requestURI", request.getRequestURI(),
                       "remoteAddr", request.getRemoteAddr(),
                       "authenticationClass", authentication.getClass().getSimpleName()));
            
        } catch (Exception e) {
            // Handle security context establishment errors
            publishSecurityAuditEvent("SECURITY_CONTEXT_ESTABLISHMENT_FAILED", 
                Map.of("error", e.getMessage(),
                       "errorType", e.getClass().getSimpleName(),
                       "requestURI", request.getRequestURI(),
                       "remoteAddr", request.getRemoteAddr(),
                       "jwtSubject", jwt.getSubject()));
            
            // Clear any partial security context
            SecurityContextHolder.clearContext();
            
            throw new RuntimeException("Failed to establish security context", e);
        }
    }

    /**
     * Maps RACF user types to Spring Security authorities for role-based access control.
     * 
     * This method implements the role mapping logic that converts legacy RACF user
     * types to modern Spring Security authorities. The mapping preserves the original
     * authorization model while enabling cloud-native security patterns.
     * 
     * Role Mapping Logic:
     * - Admin users (type 'A') inherit user privileges plus administrative access
     * - Regular users (type 'U') have standard transaction processing access
     * - Unknown user types receive no authorities for security
     * 
     * @param userType String RACF user type from JWT claims ('A' or 'U')
     * @return Collection<SimpleGrantedAuthority> Spring Security authorities
     */
    private Collection<SimpleGrantedAuthority> mapUserTypeToAuthorities(String userType) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        switch (userType) {
            case ADMIN_USER_TYPE:
                // Admin users inherit all user privileges plus administrative access
                authorities.add(new SimpleGrantedAuthority(ROLE_ADMIN));
                authorities.add(new SimpleGrantedAuthority(ROLE_USER));
                break;
                
            case REGULAR_USER_TYPE:
                // Regular users have standard transaction processing access
                authorities.add(new SimpleGrantedAuthority(ROLE_USER));
                break;
                
            default:
                // Unknown user types receive no authorities for security
                publishSecurityAuditEvent("UNKNOWN_USER_TYPE_ENCOUNTERED", 
                    Map.of("userType", userType,
                           "message", "Unknown user type encountered during role mapping"));
                break;
        }
        
        return authorities;
    }

    /**
     * Handles JWT authentication errors with comprehensive audit logging.
     * 
     * This method processes JWT authentication and validation errors, providing
     * detailed audit logging for security monitoring and compliance. It ensures
     * that all authentication failures are properly logged while maintaining
     * security best practices.
     * 
     * Error Handling:
     * - JwtValidationException for expired or invalid tokens
     * - BadJwtException for malformed JWT structure
     * - IllegalArgumentException for invalid token format
     * - Generic Exception for unexpected authentication errors
     * 
     * Security Considerations:
     * - Avoids exposing sensitive token details in error messages
     * - Provides comprehensive audit trail for security monitoring
     * - Maintains request processing flow for proper error handling
     * - Enables correlation with security monitoring systems
     * 
     * @param request HttpServletRequest for audit context
     * @param response HttpServletResponse for error response handling
     * @param exception Exception that occurred during JWT authentication
     */
    private void handleJwtAuthenticationError(HttpServletRequest request, 
                                              HttpServletResponse response, 
                                              Exception exception) {
        
        // Determine error type for specific handling
        String errorType = exception.getClass().getSimpleName();
        String errorMessage = exception.getMessage();
        
        // Clear any partial security context
        SecurityContextHolder.clearContext();
        
        // Publish comprehensive audit event for authentication error
        publishSecurityAuditEvent("JWT_AUTHENTICATION_ERROR", 
            Map.of("errorType", errorType,
                   "errorMessage", errorMessage,
                   "requestURI", request.getRequestURI(),
                   "remoteAddr", request.getRemoteAddr(),
                   "userAgent", request.getHeader("User-Agent"),
                   "requestMethod", request.getMethod(),
                   "timestamp", Instant.now().toString()));
        
        // Log specific error details for security monitoring
        if (exception instanceof JwtValidationException) {
            publishSecurityAuditEvent("JWT_VALIDATION_ERROR_DETAILED", 
                Map.of("validationErrors", ((JwtValidationException) exception).getErrors().stream()
                           .map(error -> error.getDescription())
                           .collect(Collectors.joining(", ")),
                       "requestURI", request.getRequestURI(),
                       "remoteAddr", request.getRemoteAddr()));
        }
    }

    /**
     * Publishes security audit events for compliance logging and monitoring.
     * 
     * This method creates and publishes structured audit events to Spring Boot
     * Actuator for security monitoring and compliance reporting. Events are
     * designed for integration with ELK stack and other monitoring systems.
     * 
     * Audit Event Structure:
     * - Timestamp for event correlation and chronological ordering
     * - Principal identifier for security context tracking
     * - Event type for categorization and filtering
     * - Detailed event data for comprehensive analysis
     * - Structured format for automated processing
     * 
     * Integration Points:
     * - Spring Boot Actuator audit event framework
     * - ELK stack for centralized log aggregation
     * - Prometheus metrics for security monitoring
     * - Grafana dashboards for real-time security visibility
     * 
     * @param eventType String identifier for the security event type
     * @param eventData Map containing detailed event information
     */
    private void publishSecurityAuditEvent(String eventType, Map<String, Object> eventData) {
        try {
            // Create audit event with timestamp and security context
            AuditEvent auditEvent = new AuditEvent(
                Instant.now(),
                "JWT_AUTHENTICATION_FILTER",
                eventType,
                eventData
            );
            
            // Publish audit event to Spring Boot Actuator framework
            eventPublisher.publishEvent(new AuditApplicationEvent(auditEvent));
            
        } catch (Exception e) {
            // Log audit event publication failure without disrupting security operations
            // This ensures that security processing continues even if audit logging fails
            System.err.println("Failed to publish JWT authentication audit event: " + 
                               eventType + " - " + e.getMessage());
        }
    }
}