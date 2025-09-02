package com.carddemo.security;

import com.carddemo.security.JwtTokenService;
import com.carddemo.security.CustomUserDetailsService;
import com.carddemo.security.SecurityConstants;

import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Spring Security filter intercepting HTTP requests to extract and validate JWT tokens 
 * from Authorization headers. Sets SecurityContext with authenticated user details for 
 * subsequent authorization checks.
 * 
 * This filter extends OncePerRequestFilter to ensure single execution per request and
 * integrates with the Spring Security filter chain to provide JWT-based authentication
 * for the CardDemo application. The filter validates tokens using JwtTokenService,
 * loads user details via CustomUserDetailsService, and establishes security context
 * for downstream authorization checks.
 * 
 * Key Features:
 * - Extracts JWT tokens from Authorization Bearer headers
 * - Validates token signature, expiration, and blacklist status
 * - Loads user authentication details from PostgreSQL via UserDetailsService
 * - Creates UsernamePasswordAuthenticationToken with user authorities
 * - Sets authenticated security context for subsequent filter chain processing
 * - Handles token validation exceptions with appropriate HTTP status codes
 * - Bypasses authentication for public endpoints like login and health checks
 * 
 * Authentication Flow:
 * 1. Extract Authorization header from HTTP request
 * 2. Validate Bearer prefix and extract JWT token
 * 3. Validate token using JwtTokenService (signature, expiration, blacklist)
 * 4. Extract username from validated token claims
 * 5. Load UserDetails via CustomUserDetailsService
 * 6. Create authenticated UsernamePasswordAuthenticationToken
 * 7. Set authentication in SecurityContextHolder
 * 8. Continue filter chain with established security context
 * 
 * Error Handling:
 * - Invalid/malformed tokens: Continue without authentication (401 from downstream)
 * - Token validation failures: Log security events and continue filter chain
 * - User not found: Continue without authentication (401 from downstream)
 * - Service exceptions: Log errors and continue without authentication
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    /**
     * Main filter method that processes HTTP requests to extract and validate JWT tokens.
     * 
     * This method implements the core JWT authentication logic by extracting tokens
     * from Authorization headers, validating them through JwtTokenService, loading
     * user details via CustomUserDetailsService, and establishing the security context
     * for subsequent authorization checks in the filter chain.
     * 
     * The method follows Spring Security best practices by:
     * - Only processing requests with valid Authorization headers
     * - Bypassing authentication if security context already exists
     * - Handling all exceptions gracefully without breaking the filter chain
     * - Logging security events for audit and monitoring purposes
     * - Setting authentication details for downstream authorization
     * 
     * @param request HttpServletRequest containing the Authorization header
     * @param response HttpServletResponse for setting security headers if needed
     * @param filterChain FilterChain to continue processing after authentication
     * @throws ServletException if request processing fails
     * @throws IOException if I/O operations fail during filter processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        logger.debug("Processing JWT authentication for request: {} {}", 
                    request.getMethod(), request.getRequestURI());

        try {
            // Skip authentication if security context already established
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                logger.debug("Security context already established, skipping JWT authentication");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract JWT token from Authorization header
            String jwtToken = extractTokenFromRequest(request);
            
            if (jwtToken == null) {
                logger.debug("No JWT token found in request, continuing filter chain");
                filterChain.doFilter(request, response);
                return;
            }

            // Validate JWT token using JwtTokenService
            if (!jwtTokenService.validateToken(jwtToken)) {
                logger.warn("JWT token validation failed for request: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // Check if token is blacklisted
            if (jwtTokenService.isTokenBlacklisted(jwtToken)) {
                logger.warn("Attempted use of blacklisted JWT token for request: {}", 
                           request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // Extract username from validated token
            String username = jwtTokenService.extractUsername(jwtToken);
            
            if (!StringUtils.hasText(username)) {
                logger.warn("JWT token contains invalid username for request: {}", 
                           request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // Load user details via CustomUserDetailsService
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
            
            if (userDetails == null) {
                logger.warn("User details not found for username: {} in request: {}", 
                           username, request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // Create authentication token with user details and authorities
            UsernamePasswordAuthenticationToken authenticationToken = 
                new UsernamePasswordAuthenticationToken(
                    userDetails, 
                    null, 
                    userDetails.getAuthorities()
                );

            // Set authentication in SecurityContextHolder
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authenticationToken);
            SecurityContextHolder.setContext(securityContext);

            logger.debug("Successfully authenticated user: {} with authorities: {} for request: {}", 
                        username, userDetails.getAuthorities(), request.getRequestURI());

        } catch (Exception e) {
            logger.error("JWT authentication failed for request: {} - Error: {}", 
                        request.getRequestURI(), e.getMessage(), e);
            
            // Clear security context on authentication failure
            SecurityContextHolder.clearContext();
        }

        // Continue filter chain regardless of authentication success/failure
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from the Authorization header of the HTTP request.
     * 
     * This method implements the standard Bearer token extraction pattern by:
     * 1. Retrieving the Authorization header value
     * 2. Validating the Bearer prefix is present
     * 3. Extracting the token portion after the prefix
     * 4. Performing basic token format validation
     * 
     * @param request HttpServletRequest containing the Authorization header
     * @return JWT token string if valid Bearer token found, null otherwise
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(SecurityConstants.JWT_HEADER_NAME);
        
        if (!StringUtils.hasText(authorizationHeader)) {
            logger.debug("No Authorization header found in request");
            return null;
        }

        if (!StringUtils.startsWithIgnoreCase(authorizationHeader, SecurityConstants.JWT_TOKEN_PREFIX)) {
            logger.debug("Authorization header does not start with Bearer prefix");
            return null;
        }

        // Extract token after "Bearer " prefix
        String token = authorizationHeader.substring(SecurityConstants.JWT_TOKEN_PREFIX.length()).trim();
        
        if (!StringUtils.hasText(token)) {
            logger.debug("Empty token found after Bearer prefix");
            return null;
        }

        return token;
    }

    /**
     * Determines if the filter should be applied to the given request.
     * 
     * This method can be overridden to bypass JWT authentication for specific
     * endpoints such as login, health checks, or public APIs. The default
     * implementation applies the filter to all requests.
     * 
     * @param request HttpServletRequest to evaluate
     * @return true if filter should be applied, false to skip
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // Skip JWT authentication for public endpoints
        boolean skipFilter = requestURI.equals("/api/auth/login") ||
                           requestURI.equals("/api/auth/logout") ||
                           requestURI.startsWith("/actuator/health") ||
                           requestURI.startsWith("/api/public/");
        
        if (skipFilter) {
            logger.debug("Skipping JWT authentication for public endpoint: {}", requestURI);
        }
        
        return skipFilter;
    }
}