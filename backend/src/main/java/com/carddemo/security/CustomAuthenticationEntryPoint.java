package com.carddemo.security;

import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.core.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.carddemo.security.SecurityConstants;
import com.carddemo.security.JwtTokenService;
import com.carddemo.exception.ErrorResponse;

/**
 * Custom authentication entry point for handling unauthorized requests in the CardDemo application.
 * This component replaces RACF authentication failure handling with Spring Security's 
 * AuthenticationEntryPoint interface, providing structured JSON error responses for 
 * authentication failures.
 * 
 * The entry point differentiates between missing tokens, malformed tokens, and expired tokens
 * to provide specific error messages that help the React frontend handle authentication 
 * scenarios appropriately.
 * 
 * Key responsibilities:
 * - Return 401 Unauthorized status for authentication failures
 * - Provide JSON error responses instead of default HTML error pages
 * - Set CORS headers for React SPA requests
 * - Differentiate between missing vs expired JWT tokens
 * - Integrate with frontend redirect logic for session management
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);
    
    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;
    
    // Authentication error codes aligned with COBOL ABEND patterns
    private static final String AUTH_ERROR_CODE = "AUTH001";
    private static final String TOKEN_EXPIRED_CODE = "AUTH002";
    private static final String TOKEN_MISSING_CODE = "AUTH003";
    private static final String TOKEN_INVALID_CODE = "AUTH004";
    
    // Error messages for different authentication failure scenarios
    private static final String UNAUTHORIZED_MESSAGE = "Authentication required to access this resource";
    private static final String TOKEN_EXPIRED_MESSAGE = "Session has expired. Please log in again";
    private static final String TOKEN_MISSING_MESSAGE = "Authentication token is required";
    private static final String TOKEN_INVALID_MESSAGE = "Invalid authentication token";
    
    // Login endpoint for frontend redirects
    private static final String AUTH_LOGIN_ENDPOINT = "/api/auth/login";

    @Autowired
    public CustomAuthenticationEntryPoint(JwtTokenService jwtTokenService, 
                                        @Qualifier("objectMapper") ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    /**
     * Commences an authentication scheme when an authentication request is received
     * for a protected resource. This method handles various authentication failure
     * scenarios and returns appropriate JSON error responses.
     * 
     * The method examines the request for JWT tokens, validates them using the
     * JwtTokenService, and provides specific error messages based on the token status:
     * - Missing token: TOKEN_MISSING_MESSAGE
     * - Expired token: TOKEN_EXPIRED_MESSAGE  
     * - Invalid token: TOKEN_INVALID_MESSAGE
     * - General auth failure: UNAUTHORIZED_MESSAGE
     * 
     * @param request The HttpServletRequest that resulted in an AuthenticationException
     * @param response The HttpServletResponse to send the authentication challenge
     * @param authException The exception that caused the authentication failure
     * @throws IOException If an error occurs writing the response
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        logger.warn("Authentication failed for request to {}: {}", 
                   request.getRequestURI(), authException.getMessage());
        
        // Extract JWT token from request
        String token = extractTokenFromRequest(request);
        
        // Determine the specific authentication failure reason
        ErrorResponse errorResponse = createErrorResponse(request, token, authException);
        
        // Set response status and headers
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        // Set CORS headers for React SPA requests
        setCorsHeaders(response);
        
        // Write JSON error response
        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
            
            logger.debug("Sent authentication error response: {}", errorResponse.getErrorCode());
            
        } catch (Exception e) {
            logger.error("Error writing authentication failure response", e);
            response.getWriter().write("{\"error\":\"Authentication required\"}");
        }
    }
    
    /**
     * Extracts JWT token from the Authorization header of the HTTP request.
     * Supports the standard "Bearer <token>" format used by the React frontend.
     * 
     * @param request The HTTP request containing potential JWT token
     * @return JWT token string if present, null otherwise
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(SecurityConstants.JWT_HEADER_NAME);
        
        if (authorizationHeader != null && authorizationHeader.startsWith(SecurityConstants.JWT_TOKEN_PREFIX)) {
            return authorizationHeader.substring(SecurityConstants.JWT_TOKEN_PREFIX.length()).trim();
        }
        
        return null;
    }
    
    /**
     * Creates an appropriate ErrorResponse based on the authentication failure scenario.
     * Analyzes the JWT token status to provide specific error messages and codes
     * that help the frontend handle different authentication states.
     * 
     * @param request The HTTP request that failed authentication
     * @param token The JWT token extracted from the request (may be null)
     * @param authException The original authentication exception
     * @return Structured ErrorResponse with appropriate error details
     */
    private ErrorResponse createErrorResponse(HttpServletRequest request, String token, 
                                            AuthenticationException authException) {
        
        String errorCode;
        String message;
        String reason;
        
        if (token == null || token.trim().isEmpty()) {
            // No token provided
            errorCode = TOKEN_MISSING_CODE;
            message = TOKEN_MISSING_MESSAGE;
            reason = "No JWT token found in Authorization header";
            
        } else {
            // Token provided, check if it's expired or invalid
            try {
                if (jwtTokenService.isTokenExpired(token)) {
                    errorCode = TOKEN_EXPIRED_CODE;
                    message = TOKEN_EXPIRED_MESSAGE;
                    reason = "JWT token has expired, session timeout exceeded " + 
                            (SecurityConstants.SESSION_TIMEOUT / 60000) + " minutes";
                } else if (!jwtTokenService.validateToken(token)) {
                    errorCode = TOKEN_INVALID_CODE;
                    message = TOKEN_INVALID_MESSAGE;
                    reason = "JWT token validation failed - invalid signature or format";
                } else {
                    // Token seems valid but authentication still failed
                    errorCode = AUTH_ERROR_CODE;
                    message = UNAUTHORIZED_MESSAGE;
                    reason = "Authentication failed despite valid token: " + authException.getMessage();
                }
            } catch (Exception e) {
                // Token parsing failed
                errorCode = TOKEN_INVALID_CODE;
                message = TOKEN_INVALID_MESSAGE;
                reason = "JWT token parsing failed: " + e.getMessage();
                logger.debug("Token validation error", e);
            }
        }
        
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .reason(reason)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .addFieldError("redirect_url", AUTH_LOGIN_ENDPOINT)
                .build();
    }
    
    /**
     * Sets CORS headers required for React SPA requests to handle authentication
     * errors properly. These headers allow the frontend to process the JSON
     * error response and handle authentication state transitions.
     * 
     * @param response The HTTP response to configure with CORS headers
     */
    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", 
                          "Origin, X-Requested-With, Content-Type, Accept, Authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
        
        // Additional headers for authentication handling
        response.setHeader("WWW-Authenticate", "Bearer realm=\"CardDemo\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }
}