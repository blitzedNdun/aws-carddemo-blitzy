package com.carddemo.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Token Utility Class for CardDemo Application
 * 
 * This utility class provides comprehensive JWT token management for Spring Security
 * OAuth2 Resource Server authentication. It handles token generation, validation,
 * and claims extraction for role-based access control in the CardDemo microservices
 * architecture.
 * 
 * The class implements JWT token security with HS256 algorithm and configurable
 * secret key rotation, supporting the authentication flow that replaces the legacy
 * CICS/RACF authentication system.
 */
@Component
public class JwtTokenUtil {

    /**
     * JWT secret key injected from application configuration
     * Used for token signing and validation with HS256 algorithm
     */
    @Value("${jwt.secret:CardDemo2024SecretKeyForJWTTokenGeneration}")
    private String jwtSecret;

    /**
     * JWT token expiration time in milliseconds
     * Default: 30 minutes (1800000 ms) as per specification
     */
    @Value("${jwt.expiration:1800000}")
    private int jwtExpirationInMs;

    /**
     * Refresh token expiration time in milliseconds
     * Default: 24 hours (86400000 ms) for session management
     */
    @Value("${jwt.refresh.expiration:86400000}")
    private int refreshExpirationInMs;

    /**
     * Claims keys for JWT token payload
     * These constants ensure consistent claim naming across the application
     */
    private static final String CLAIM_USER_ID = "user_id";
    private static final String CLAIM_USER_TYPE = "user_type";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_SESSION_ID = "session_id";
    private static final String CLAIM_TOKEN_TYPE = "token_type";

    /**
     * Token types for differentiation between access and refresh tokens
     */
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    /**
     * Generates a JWT access token for authenticated users
     * 
     * This method creates a JWT token with user information and role claims
     * compatible with Spring Security OAuth2 Resource Server configuration.
     * The token includes all necessary claims for authorization decisions.
     * 
     * @param userId The unique user identifier (equivalent to RACF user ID)
     * @param userType The user type ('A' for Admin, 'U' for User)
     * @param sessionId Optional session correlation ID for tracking
     * @return A signed JWT token string ready for HTTP Bearer authentication
     */
    public String generateToken(String userId, String userType, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USER_TYPE, userType);
        claims.put(CLAIM_ROLES, mapUserTypeToRoles(userType));
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        
        if (sessionId != null && !sessionId.isEmpty()) {
            claims.put(CLAIM_SESSION_ID, sessionId);
        }

        return createToken(claims, userId, jwtExpirationInMs);
    }

    /**
     * Generates a JWT refresh token for session management
     * 
     * Refresh tokens have longer expiration times and are used to obtain
     * new access tokens without requiring user re-authentication.
     * 
     * @param userId The unique user identifier
     * @param sessionId Session correlation ID for tracking
     * @return A signed JWT refresh token string
     */
    public String generateRefreshToken(String userId, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        
        if (sessionId != null && !sessionId.isEmpty()) {
            claims.put(CLAIM_SESSION_ID, sessionId);
        }

        return createToken(claims, userId, refreshExpirationInMs);
    }

    /**
     * Validates a JWT token signature and expiration
     * 
     * This method performs comprehensive token validation including:
     * - Signature verification with the configured secret key
     * - Expiration time validation
     * - Token structure validation
     * 
     * @param token The JWT token string to validate
     * @return true if the token is valid and not expired, false otherwise
     */
    public Boolean validateToken(String token) {
        try {
            // Parse the token with signature verification
            extractAllClaims(token);
            
            // Check if token is expired
            return !isTokenExpired(token);
        } catch (Exception e) {
            // Token is invalid due to signature, structure, or other issues
            return false;
        }
    }

    /**
     * Extracts the username (user ID) from a JWT token
     * 
     * @param token The JWT token string
     * @return The user ID claim from the token
     */
    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_USER_ID, String.class));
    }

    /**
     * Extracts the user type from a JWT token
     * 
     * User types correspond to the original COBOL system:
     * - 'A' for Admin users (CARDDEMO.ADMIN group)
     * - 'U' for regular users (CARDDEMO.USER group)
     * 
     * @param token The JWT token string
     * @return The user type claim from the token
     */
    public String extractUserType(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_USER_TYPE, String.class));
    }

    /**
     * Extracts roles array from a JWT token
     * 
     * Roles are mapped from user types and used by Spring Security
     * for authorization decisions in @PreAuthorize annotations.
     * 
     * @param token The JWT token string
     * @return Array of role strings for Spring Security
     */
    public String[] extractRoles(String token) {
        return extractClaim(token, claims -> {
            Object roles = claims.get(CLAIM_ROLES);
            if (roles instanceof String[]) {
                return (String[]) roles;
            }
            return new String[0];
        });
    }

    /**
     * Extracts the session ID from a JWT token
     * 
     * Session IDs enable correlation with Redis session storage
     * for distributed session management.
     * 
     * @param token The JWT token string
     * @return The session correlation ID, or null if not present
     */
    public String extractSessionId(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_SESSION_ID, String.class));
    }

    /**
     * Extracts the token expiration date
     * 
     * @param token The JWT token string
     * @return The expiration date from the token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Checks if a JWT token is expired
     * 
     * @param token The JWT token string to check
     * @return true if the token is expired, false otherwise
     */
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts all claims from a JWT token
     * 
     * This method provides access to the complete JWT payload
     * for comprehensive token information retrieval.
     * 
     * @param token The JWT token string
     * @return Claims object containing all token claims
     */
    public Claims extractClaims(String token) {
        return extractAllClaims(token);
    }

    /**
     * Generic method to extract a specific claim from a JWT token
     * 
     * @param token The JWT token string
     * @param claimsResolver Function to extract the desired claim
     * @param <T> The type of the claim to extract
     * @return The extracted claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from a JWT token with signature verification
     * 
     * This method performs the actual token parsing and signature validation
     * using the configured secret key and HS256 algorithm.
     * 
     * @param token The JWT token string to parse
     * @return Claims object containing all token claims
     * @throws Exception if token is invalid or signature verification fails
     */
    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Creates a JWT token with specified claims and expiration
     * 
     * This method handles the actual token creation with HS256 signing
     * and proper header/payload structure for Spring Security compatibility.
     * 
     * @param claims Map of claims to include in the token
     * @param subject The token subject (typically user ID)
     * @param expirationTime Expiration time in milliseconds
     * @return Signed JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Maps user type to Spring Security roles
     * 
     * This method converts the legacy COBOL user type system to
     * Spring Security role-based access control:
     * - 'A' (Admin) -> ROLE_ADMIN (with ROLE_USER inheritance)
     * - 'U' (User) -> ROLE_USER
     * 
     * @param userType The user type from the authentication system
     * @return Array of Spring Security role strings
     */
    private String[] mapUserTypeToRoles(String userType) {
        if ("A".equals(userType)) {
            // Admin users inherit all user permissions plus admin-specific ones
            return new String[]{"ROLE_ADMIN", "ROLE_USER"};
        } else {
            // Regular users get standard user role
            return new String[]{"ROLE_USER"};
        }
    }

    /**
     * Validates if a token is a refresh token
     * 
     * @param token The JWT token string to check
     * @return true if the token is a refresh token, false otherwise
     */
    public Boolean isRefreshToken(String token) {
        try {
            String tokenType = extractClaim(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class));
            return TOKEN_TYPE_REFRESH.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates if a token is an access token
     * 
     * @param token The JWT token string to check
     * @return true if the token is an access token, false otherwise
     */
    public Boolean isAccessToken(String token) {
        try {
            String tokenType = extractClaim(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class));
            return TOKEN_TYPE_ACCESS.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the remaining time until token expiration
     * 
     * @param token The JWT token string
     * @return Remaining time in milliseconds, or 0 if expired
     */
    public Long getRemainingExpiration(String token) {
        try {
            Date expiration = extractExpiration(token);
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0L;
        }
    }
}