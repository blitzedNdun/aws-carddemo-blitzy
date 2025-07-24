/*
 * JwtTokenUtil.java
 * 
 * JWT token utility class providing token generation, validation, and claims extraction
 * for Spring Security authentication with HS256 algorithm and configurable expiration.
 * 
 * This class supports the CardDemo application modernization from COBOL/CICS/RACF
 * to Spring Security with JWT-based authentication, maintaining equivalent security
 * functionality while enabling cloud-native microservices architecture.
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Token Utility Class
 * 
 * Provides comprehensive JWT token management for the CardDemo application including:
 * - Token generation using HS256 algorithm with configurable secret key
 * - Token validation with signature verification and expiration checking
 * - Claims extraction methods for user authentication and authorization
 * - Refresh token generation capability for session management
 * - Spring Security integration for JWT authentication context
 * 
 * This utility replaces RACF authentication patterns from the legacy COBOL/CICS
 * system while maintaining equivalent security controls and user type management.
 * 
 * Key Features:
 * - HS256 algorithm with configurable secret key rotation
 * - 30-minute default expiration with configurable override
 * - Claims-based authorization for role-based access control
 * - User type mapping: 'A' -> ROLE_ADMIN, 'U' -> ROLE_USER
 * - Session correlation ID for distributed session management
 * - Comprehensive error handling and validation
 * 
 * Performance Requirements:
 * - Token generation: <50ms response time
 * - Token validation: <10ms response time
 * - Supports 10,000+ TPS throughput via stateless design
 * 
 * Security Features:
 * - HMAC-SHA256 signature algorithm for token integrity
 * - Configurable secret key with rotation capability
 * - JWT standard claims (iss, sub, iat, exp) support
 * - Custom claims for CardDemo-specific user attributes
 * - Token expiration validation with automatic cleanup
 * 
 * Integration:
 * - Spring Security OAuth2 Resource Server compatibility
 * - Spring Boot configuration property injection
 * - Redis session store correlation for stateless microservices
 * - Kubernetes-native configuration via ConfigMaps and Secrets
 */
@Component
public class JwtTokenUtil {

    /**
     * JWT secret key for token signing and verification
     * Configurable via application properties for different environments
     * Supports rotation through configuration updates without application restart
     */
    @Value("${jwt.secret:defaultSecretKeyForDevelopmentOnlyDoNotUseInProduction}")
    private String jwtSecret;

    /**
     * JWT token expiration time in milliseconds
     * Default: 30 minutes (1800000ms) matching CICS terminal timeout
     * Configurable per environment for different security requirements
     */
    @Value("${jwt.expiration:1800000}")
    private Long jwtExpiration;

    /**
     * JWT refresh token expiration time in milliseconds
     * Default: 7 days (604800000ms) for extended session management
     * Allows seamless token refresh without re-authentication
     */
    @Value("${jwt.refresh.expiration:604800000}")
    private Long jwtRefreshExpiration;

    /**
     * JWT issuer claim for token identification
     * Identifies the CardDemo application as token issuer
     */
    @Value("${jwt.issuer:CardDemo-Auth-Service}")
    private String jwtIssuer;

    /**
     * Session correlation prefix for Redis session management
     * Enables distributed session tracking across microservices
     */
    private static final String SESSION_PREFIX = "CARDDEMO_SESSION_";

    /**
     * User type claim key for authorization
     * Maps to legacy COBOL user type field from USRSEC file
     */
    private static final String CLAIM_USER_TYPE = "userType";

    /**
     * Role claim key for Spring Security authorization
     * Contains mapped role from user type (ROLE_ADMIN, ROLE_USER)
     */
    private static final String CLAIM_ROLE = "role";

    /**
     * Session correlation ID claim key
     * Links JWT token to distributed session state in Redis
     */
    private static final String CLAIM_SESSION_ID = "sessionId";

    /**
     * First name claim key for user identification
     * Retrieved from user authentication data
     */
    private static final String CLAIM_FIRST_NAME = "firstName";

    /**
     * Last name claim key for user identification
     * Retrieved from user authentication data
     */
    private static final String CLAIM_LAST_NAME = "lastName";

    /**
     * Generate JWT access token for authenticated user
     * 
     * Creates a new JWT token with standard and custom claims including:
     * - User ID as subject
     * - User type for role-based authorization
     * - Mapped Spring Security role
     * - Session correlation ID for distributed state management
     * - User profile information (names)
     * - Standard JWT claims (issuer, issued at, expiration)
     * 
     * @param userId User identifier from authentication (8 characters, uppercase)
     * @param userType User type from legacy system ('A' for Admin, 'U' for User)
     * @param firstName User's first name for personalization
     * @param lastName User's last name for personalization
     * @param sessionId Session correlation ID for Redis session management
     * @return Generated JWT token string with HS256 signature
     * 
     * @throws JwtException if token generation fails
     * @throws IllegalArgumentException if required parameters are null/empty
     */
    public String generateToken(String userId, String userType, String firstName, 
                              String lastName, String sessionId) {
        
        // Validate required parameters for token generation
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty for JWT token generation");
        }
        if (userType == null || userType.trim().isEmpty()) {
            throw new IllegalArgumentException("User type cannot be null or empty for JWT token generation");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty for JWT token generation");
        }

        // Create custom claims map with CardDemo-specific attributes
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_TYPE, userType.toUpperCase());
        claims.put(CLAIM_ROLE, mapUserTypeToRole(userType));
        claims.put(CLAIM_SESSION_ID, SESSION_PREFIX + sessionId);
        
        // Add user profile information if available
        if (firstName != null && !firstName.trim().isEmpty()) {
            claims.put(CLAIM_FIRST_NAME, firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            claims.put(CLAIM_LAST_NAME, lastName.trim());
        }

        return createToken(claims, userId, jwtExpiration);
    }

    /**
     * Generate JWT refresh token for session management
     * 
     * Creates a long-lived refresh token for seamless token renewal without
     * requiring user re-authentication. Refresh tokens have extended expiration
     * and minimal claims for security.
     * 
     * @param userId User identifier for token subject
     * @param sessionId Session correlation ID for state management
     * @return Generated refresh token string with extended expiration
     * 
     * @throws JwtException if refresh token generation fails
     * @throws IllegalArgumentException if required parameters are null/empty
     */
    public String generateRefreshToken(String userId, String sessionId) {
        
        // Validate required parameters
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty for refresh token generation");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty for refresh token generation");
        }

        // Minimal claims for refresh token security
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_SESSION_ID, SESSION_PREFIX + sessionId);
        claims.put("tokenType", "refresh");

        return createToken(claims, userId, jwtRefreshExpiration);
    }

    /**
     * Validate JWT token signature and expiration
     * 
     * Performs comprehensive token validation including:
     * - Signature verification using configured secret key
     * - Expiration time validation
     * - Token structure and format validation
     * - Claims integrity verification
     * 
     * @param token JWT token string to validate
     * @return true if token is valid and not expired, false otherwise
     * 
     * @throws JwtException for malformed tokens or signature validation failures
     */
    public Boolean validateToken(String token) {
        try {
            // Parse and validate token using configured secret key
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            
            return true;
            
        } catch (ExpiredJwtException e) {
            // Token has expired - return false for expired tokens
            return false;
            
        } catch (UnsupportedJwtException | MalformedJwtException | 
                 io.jsonwebtoken.security.SignatureException | IllegalArgumentException e) {
            // Invalid token format, signature, or structure
            throw new JwtException("Invalid JWT token: " + e.getMessage(), e);
        }
    }

    /**
     * Extract all claims from JWT token
     * 
     * Parses the JWT token and returns all claims for processing.
     * Used internally by other extraction methods for claim access.
     * 
     * @param token JWT token string to parse
     * @return Claims object containing all token claims
     * 
     * @throws JwtException if token parsing fails
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
                
        } catch (JwtException e) {
            throw new JwtException("Failed to extract claims from JWT token: " + e.getMessage(), e);
        }
    }

    /**
     * Extract username (user ID) from JWT token
     * 
     * Retrieves the user identifier from the token subject claim.
     * This maps to the COBOL USRSEC user ID field (8 characters).
     * 
     * @param token JWT token string
     * @return User ID string from token subject
     * 
     * @throws JwtException if token parsing or claim extraction fails
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user type from JWT token
     * 
     * Retrieves the user type claim for role-based authorization.
     * Maps to legacy COBOL user type: 'A' for Admin, 'U' for User.
     * 
     * @param token JWT token string
     * @return User type string ('A' or 'U')
     * 
     * @throws JwtException if token parsing or claim extraction fails
     */
    public String extractUserType(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_USER_TYPE, String.class));
    }

    /**
     * Extract Spring Security role from JWT token
     * 
     * Retrieves the mapped role claim for Spring Security authorization.
     * Values: 'ROLE_ADMIN' or 'ROLE_USER' based on user type mapping.
     * 
     * @param token JWT token string
     * @return Spring Security role string
     * 
     * @throws JwtException if token parsing or claim extraction fails
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_ROLE, String.class));
    }

    /**
     * Extract session correlation ID from JWT token
     * 
     * Retrieves the session ID claim for distributed session management.
     * Used to correlate JWT tokens with Redis session state.
     * 
     * @param token JWT token string
     * @return Session correlation ID string
     * 
     * @throws JwtException if token parsing or claim extraction fails
     */
    public String extractSessionId(String token) {
        String sessionClaim = extractClaim(token, claims -> claims.get(CLAIM_SESSION_ID, String.class));
        // Remove session prefix to get actual session ID
        return sessionClaim != null && sessionClaim.startsWith(SESSION_PREFIX) 
            ? sessionClaim.substring(SESSION_PREFIX.length()) 
            : sessionClaim;
    }

    /**
     * Extract user's first name from JWT token
     * 
     * Retrieves the first name claim for user personalization.
     * May be null if not provided during token generation.
     * 
     * @param token JWT token string
     * @return User's first name or null if not present
     * 
     * @throws JwtException if token parsing fails
     */
    public String extractFirstName(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_FIRST_NAME, String.class));
    }

    /**
     * Extract user's last name from JWT token
     * 
     * Retrieves the last name claim for user personalization.
     * May be null if not provided during token generation.
     * 
     * @param token JWT token string
     * @return User's last name or null if not present
     * 
     * @throws JwtException if token parsing fails
     */
    public String extractLastName(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_LAST_NAME, String.class));
    }

    /**
     * Extract token expiration date
     * 
     * Retrieves the expiration timestamp from the token for validation.
     * Used to determine token lifetime and refresh requirements.
     * 
     * @param token JWT token string
     * @return Token expiration Date
     * 
     * @throws JwtException if token parsing or claim extraction fails
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Check if JWT token is expired
     * 
     * Validates the token expiration against current system time.
     * Uses system clock for expiration comparison.
     * 
     * @param token JWT token string to check
     * @return true if token is expired, false if still valid
     * 
     * @throws JwtException if token parsing fails
     */
    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            // Token is already expired
            return true;
        }
    }

    /**
     * Extract specific claim from JWT token using function
     * 
     * Generic method for extracting any claim from the token using
     * a provided function to access the specific claim value.
     * 
     * @param <T> Type of the claim value
     * @param token JWT token string
     * @param claimsResolver Function to extract specific claim
     * @return Extracted claim value
     * 
     * @throws JwtException if token parsing or claim extraction fails
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Create JWT token with specified claims and expiration
     * 
     * Internal method for building JWT tokens with consistent structure
     * and signing configuration. Used by both access and refresh token generation.
     * 
     * @param claims Map of custom claims to include in token
     * @param subject Token subject (user ID)
     * @param expiration Token expiration time in milliseconds
     * @return Generated JWT token string
     * 
     * @throws JwtException if token creation fails
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        try {
            return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
                
        } catch (Exception e) {
            throw new JwtException("Failed to create JWT token: " + e.getMessage(), e);
        }
    }

    /**
     * Get signing key for JWT token operations
     * 
     * Creates HMAC-SHA256 signing key from configured secret.
     * Ensures consistent key usage across token generation and validation.
     * 
     * @return SecretKey for JWT signing and verification
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Map legacy user type to Spring Security role
     * 
     * Converts COBOL/RACF user types to Spring Security role names:
     * - 'A' (Admin) -> 'ROLE_ADMIN'
     * - 'U' (User) -> 'ROLE_USER'
     * - Default -> 'ROLE_USER' for security
     * 
     * This mapping preserves the original authorization model while
     * enabling Spring Security role-based access control.
     * 
     * @param userType Legacy user type ('A' or 'U')
     * @return Spring Security role string
     */
    private String mapUserTypeToRole(String userType) {
        if (userType == null) {
            return "ROLE_USER";
        }
        
        switch (userType.toUpperCase()) {
            case "A":
                return "ROLE_ADMIN";
            case "U":
                return "ROLE_USER";
            default:
                // Default to user role for security
                return "ROLE_USER";
        }
    }
}