/*
 * CustomAccessDeniedHandler.java
 * 
 * Custom Spring Security access denied handler managing authorization failures
 * and security event logging for the CardDemo application. Implements comprehensive
 * access denial processing with audit logging, user context extraction, and
 * standardized error response generation.
 * 
 * This handler implements the Spring Security AccessDeniedHandler interface
 * to provide custom authorization failure processing aligned with the security
 * architecture requirements outlined in Section 6.4 of the technical specification.
 * Maintains audit compliance and security monitoring through integration with
 * SecurityEventListener and comprehensive error response formatting.
 * 
 * Key capabilities:
 * - HTTP 403 Forbidden status response with detailed error messages
 * - JSON error response structure compatible with React SPA frontend
 * - Security event logging through SecurityEventListener integration
 * - User context extraction for violation tracking and audit compliance
 * - Custom error message generation based on resource type and access pattern
 * - CORS headers configuration for cross-origin request handling
 * - Comprehensive audit trail generation for regulatory compliance
 * 
 * The handler preserves all authorization logic while providing enhanced
 * security monitoring and incident response capabilities through modern
 * Spring Security framework integration.
 */
package com.carddemo.security;

import com.carddemo.exception.ErrorResponse;

import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.io.IOException;

/**
 * Custom Spring Security access denied handler managing authorization failures.
 * 
 * This component implements comprehensive access denial processing with the following features:
 * 
 * <ul>
 *   <li><strong>HTTP Response Management:</strong> Returns appropriate 403 Forbidden status
 *       with standardized JSON error response structure</li>
 *   <li><strong>Security Event Integration:</strong> Integrates with SecurityEventListener
 *       for comprehensive audit logging and security monitoring</li>
 *   <li><strong>User Context Extraction:</strong> Extracts authenticated user details
 *       for violation tracking and compliance reporting</li>
 *   <li><strong>Custom Error Messages:</strong> Generates resource-specific error messages
 *       based on request context and access patterns</li>
 *   <li><strong>CORS Compatibility:</strong> Configures appropriate CORS headers for
 *       React SPA frontend integration</li>
 *   <li><strong>Audit Compliance:</strong> Maintains comprehensive audit trails for
 *       regulatory compliance and security incident investigation</li>
 *   <li><strong>Performance Optimization:</strong> Efficient processing without impacting
 *       application response times or user experience</li>
 * </ul>
 * 
 * The handler operates seamlessly within the Spring Security filter chain and
 * provides enhanced security capabilities while maintaining compatibility with
 * existing authorization logic and regulatory compliance requirements.
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);
    
    // Error response constants for consistent error handling
    private static final String ERROR_CODE_ACCESS_DENIED = "E403";
    private static final String CULPRIT_ACCESS_CONTROL = "ACCESS_CONTROL";
    private static final String DEFAULT_ACCESS_DENIED_MESSAGE = "Access denied: Insufficient privileges for requested resource";
    
    // CORS headers for React SPA compatibility
    private static final String CORS_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String CORS_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String CORS_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String CORS_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    
    @Autowired
    private SecurityEventListener securityEventListener;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Handles access denied exceptions by generating comprehensive error responses
     * and triggering security event logging for audit compliance.
     * 
     * This method implements the core access denied handling logic including:
     * - HTTP 403 Forbidden status response generation
     * - Standardized JSON error response creation with detailed failure information
     * - User context extraction for security monitoring and violation tracking
     * - Security event logging through SecurityEventListener integration
     * - CORS headers configuration for React SPA frontend compatibility
     * - Comprehensive audit trail generation for regulatory compliance
     * 
     * The implementation ensures consistent error response format while providing
     * detailed authorization failure information suitable for debugging and
     * security incident investigation without exposing sensitive system details.
     * 
     * @param request HttpServletRequest containing the original request details
     * @param response HttpServletResponse for writing the error response
     * @param accessDeniedException AccessDeniedException containing denial details
     * @throws IOException if error response writing fails
     */
    @Override
    public void handle(HttpServletRequest request, 
                      HttpServletResponse response, 
                      AccessDeniedException accessDeniedException) throws IOException {
        try {
            logger.warn("Access denied for request: {} - Reason: {} at {}", 
                       request.getRequestURI(), 
                       accessDeniedException.getMessage(), 
                       LocalDateTime.now());
            
            // Extract user context for violation tracking
            String userContext = extractUserContext();
            
            // Generate custom error message based on resource type
            String errorMessage = generateErrorMessage(request, accessDeniedException);
            
            // Log access denied event for audit compliance
            logAccessDeniedEvent(request, accessDeniedException, userContext);
            
            // Set HTTP 403 Forbidden status
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            
            // Set content type for JSON response
            response.setContentType("application/json;charset=UTF-8");
            
            // Configure CORS headers for React SPA compatibility
            configureCorsHeaders(response);
            
            // Create standardized error response
            ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ERROR_CODE_ACCESS_DENIED)
                .culprit(CULPRIT_ACCESS_CONTROL)
                .reason("Authorization denied: " + accessDeniedException.getMessage())
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
            
            // Write JSON error response
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
            
            logger.debug("Access denied response generated successfully for user: {} and path: {}", 
                        userContext, request.getRequestURI());
            
        } catch (Exception e) {
            logger.error("Failed to handle access denied exception for request: {} - Error: {}", 
                        request.getRequestURI(), e.getMessage(), e);
            
            // Fallback error response in case of processing failure
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Access denied\",\"message\":\"Authorization failed\"}");
        }
    }

    /**
     * Extracts user context information from Spring Security authentication
     * for violation tracking and audit compliance.
     * 
     * This method retrieves comprehensive user information from the current
     * Spring Security context including authenticated username, user roles,
     * and principal details. Essential for security monitoring, audit logging,
     * and violation tracking required for regulatory compliance and security
     * incident investigation.
     * 
     * The extraction process handles various authentication scenarios including
     * authenticated users, anonymous access attempts, and authentication failure
     * cases while providing consistent user identification for security events.
     * 
     * @return String containing user context information for audit logging
     */
    public String extractUserContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                StringBuilder userContext = new StringBuilder();
                
                // Extract authenticated username
                String username = authentication.getName();
                userContext.append("User: ").append(username != null ? username : "UNKNOWN");
                
                // Extract user authorities/roles
                if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
                    userContext.append(" | Roles: ");
                    authentication.getAuthorities().forEach(authority -> 
                        userContext.append(authority.getAuthority()).append(" "));
                }
                
                // Extract principal information
                Object principal = authentication.getPrincipal();
                if (principal != null) {
                    userContext.append(" | Principal: ").append(principal.getClass().getSimpleName());
                }
                
                return userContext.toString().trim();
            } else {
                return "User: ANONYMOUS | Status: UNAUTHENTICATED";
            }
            
        } catch (Exception e) {
            logger.debug("Failed to extract user context from security context - Error: {}", e.getMessage());
            return "User: EXTRACTION_FAILED | Error: " + e.getMessage();
        }
    }

    /**
     * Generates custom error messages based on resource type and access patterns
     * for enhanced user experience and debugging capabilities.
     * 
     * This method analyzes the denied request context including request URI,
     * HTTP method, and exception details to generate resource-specific error
     * messages. Provides meaningful feedback while maintaining security through
     * controlled information disclosure and standardized error response format.
     * 
     * The message generation considers various access scenarios including admin
     * resource access, user function restrictions, and data access violations
     * while providing appropriate guidance for resolution without exposing
     * sensitive system architecture or security control details.
     * 
     * @param request HttpServletRequest containing the denied request details
     * @param accessDeniedException AccessDeniedException with denial context
     * @return String containing custom error message for the specific denial scenario
     */
    public String generateErrorMessage(HttpServletRequest request, 
                                     AccessDeniedException accessDeniedException) {
        try {
            String requestUri = request.getRequestURI();
            String httpMethod = request.getMethod();
            
            // Generate resource-specific error messages
            if (requestUri.contains("/admin/")) {
                return "Administrative access required: Contact system administrator for elevated privileges";
            } else if (requestUri.contains("/users/") && "POST".equals(httpMethod)) {
                return "User management access denied: Insufficient privileges for user creation";
            } else if (requestUri.contains("/users/") && ("PUT".equals(httpMethod) || "DELETE".equals(httpMethod))) {
                return "User modification access denied: Administrative privileges required";
            } else if (requestUri.contains("/accounts/") && "DELETE".equals(httpMethod)) {
                return "Account deletion access denied: Administrative authorization required";
            } else if (requestUri.contains("/transactions/") && "POST".equals(httpMethod)) {
                return "Transaction creation access denied: Insufficient processing privileges";
            } else if (requestUri.contains("/cards/") && "POST".equals(httpMethod)) {
                return "Card management access denied: Administrative privileges required";
            } else if (requestUri.contains("/reports/")) {
                return "Report access denied: Insufficient reporting privileges";
            } else {
                // Default message with request context
                return String.format("%s - Method: %s not authorized for current user role", 
                                   DEFAULT_ACCESS_DENIED_MESSAGE, httpMethod);
            }
            
        } catch (Exception e) {
            logger.debug("Failed to generate custom error message - Error: {}", e.getMessage());
            return DEFAULT_ACCESS_DENIED_MESSAGE;
        }
    }

    /**
     * Logs access denied events through SecurityEventListener integration
     * for comprehensive audit compliance and security monitoring.
     * 
     * This method creates comprehensive security event records including request
     * details, user context, denial reasons, and timestamp information. Essential
     * for regulatory compliance, security incident investigation, and audit trail
     * maintenance required for financial services applications.
     * 
     * The logging process integrates seamlessly with the SecurityEventListener
     * framework to ensure consistent security event processing, metrics collection,
     * and alert generation for suspicious access patterns or security violations.
     * 
     * @param request HttpServletRequest containing the denied request details
     * @param accessDeniedException AccessDeniedException with denial context
     * @param userContext String containing extracted user context information
     */
    public void logAccessDeniedEvent(HttpServletRequest request, 
                                   AccessDeniedException accessDeniedException,
                                   String userContext) {
        try {
            logger.info("Logging access denied event for request: {} - User: {} at {}", 
                       request.getRequestURI(), userContext, LocalDateTime.now());
            
            // Create authorization denied event for SecurityEventListener
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null) {
                // Trigger SecurityEventListener for comprehensive audit logging
                securityEventListener.handleAuthorizationDeniedEvent(
                    new org.springframework.security.authorization.event.AuthorizationDeniedEvent(
                        () -> authentication,
                        request,
                        null // AuthorizationDecision - simplified for this implementation
                    )
                );
            }
            
            // Additional access denied logging with detailed context
            logger.warn("ACCESS DENIED EVENT - URI: {} | Method: {} | User: {} | Reason: {} | Timestamp: {}", 
                       request.getRequestURI(),
                       request.getMethod(),
                       userContext,
                       accessDeniedException.getMessage(),
                       LocalDateTime.now());
            
            logger.debug("Access denied event logged successfully through SecurityEventListener");
            
        } catch (Exception e) {
            logger.error("Failed to log access denied event for request: {} - Error: {}", 
                        request.getRequestURI(), e.getMessage(), e);
            
            // Ensure audit trail continuity even if SecurityEventListener fails
            logger.error("FALLBACK ACCESS DENIED LOG - URI: {} | User: {} | Reason: {} | Error: {}", 
                        request.getRequestURI(), 
                        userContext, 
                        accessDeniedException.getMessage(),
                        e.getMessage());
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Configures CORS headers for React SPA compatibility.
     */
    private void configureCorsHeaders(HttpServletResponse response) {
        try {
            // Configure CORS headers for React frontend integration
            response.setHeader(CORS_ALLOW_ORIGIN, "*"); // Configure specific origin in production
            response.setHeader(CORS_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader(CORS_ALLOW_HEADERS, "Content-Type, Authorization, X-Requested-With");
            response.setHeader(CORS_ALLOW_CREDENTIALS, "true");
            
            logger.debug("CORS headers configured for access denied response");
            
        } catch (Exception e) {
            logger.debug("Failed to configure CORS headers - Error: {}", e.getMessage());
        }
    }
}