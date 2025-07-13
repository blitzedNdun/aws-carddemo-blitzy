package com.carddemo.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Token Utility Class for CardDemo Application
 * 
 * This utility class provides comprehensive JWT token management for Spring Security
 * authentication, replacing the legacy RACF authentication system from the mainframe
 * COBOL application. It implements HS256 algorithm for token signing and validation
 * with configurable expiration and claims-based authorization support.
 * 
 * Key Features:
 * - JWT token generation with HS256 algorithm
 * - Token validation with signature verification and expiration checking
 * - Claims extraction for user authentication and authorization
 * - Support for refresh token generation and session management
 * - Integration with Spring Security authentication context
 * 
 * Security Configuration:
 * - Uses configurable secret key for signing (minimum 512 bits for HS256)
 * - Implements 30-minute default token expiration
 * - Supports role-based access control through JWT claims
 * - Enables session correlation for distributed microservices
 * 
 * Integration with CardDemo Authentication:
 * - Replaces COBOL COSGN00C.cbl authentication logic
 * - Maintains user context equivalent to CICS COMMAREA
 * - Supports user type mapping (Admin 'A' vs User 'U')
 * - Provides stateless authentication for REST APIs
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Component
public class JwtTokenUtil {

    /**
     * JWT secret key injected from application properties
     * Should be at least 512 bits (64 characters) for HS256 algorithm security
     */
    @Value("${app.jwt.secret:defaultSecretKeyForCardDemoApplicationThatMustBeAtLeast512BitsLong}")
    private String jwtSecret;

    /**
     * JWT token expiration time in milliseconds (default: 30 minutes)
     * Configurable through application properties for different environments
     */
    @Value("${app.jwt.expiration:1800000}")
    private Long jwtExpirationMs;

    /**
     * Refresh token expiration time in milliseconds (default: 24 hours)
     * Used for extending user sessions without re-authentication
     */
    @Value("${app.jwt.refresh-expiration:86400000}")
    private Long refreshExpirationMs;

    /**
     * JWT token issuer identification
     * Used for token validation and audit trail
     */
    @Value("${app.jwt.issuer:carddemo-auth-service}")
    private String jwtIssuer;

    // Constants for JWT claims
    private static final String CLAIM_USER_TYPE = "user_type";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_SESSION_ID = "session_id";
    private static final String CLAIM_FIRST_NAME = "first_name";
    private static final String CLAIM_LAST_NAME = "last_name";

    /**
     * Generates a JWT token for authenticated user with role-based claims
     * 
     * This method creates a signed JWT token containing user authentication
     * information and authorization claims. The token structure preserves
     * the user context equivalent to CICS COMMAREA from the original
     * COBOL application while enabling stateless REST API authentication.
     * 
     * Token Claims Structure:
     * - sub (subject): User ID (8 characters, uppercase)
     * - iss (issuer): CardDemo authentication service identifier
     * - iat (issued at): Token creation timestamp
     * - exp (expiration): Token expiration timestamp (30 minutes default)
     * - user_type: User classification ('A' for Admin, 'U' for User)
     * - role: Spring Security role (ROLE_ADMIN or ROLE_USER)
     * - session_id: Correlation ID for distributed session tracking
     * - first_name: User's first name for display purposes
     * - last_name: User's last name for display purposes
     * 
     * @param userId User identifier (8 characters, converted to uppercase)
     * @param userType User type classification ('A' for Admin, 'U' for User)
     * @param firstName User's first name
     * @param lastName User's last name
     * @param sessionId Session correlation identifier for distributed tracking
     * @return Signed JWT token string for authentication
     * 
     * @throws IllegalArgumentException if userId or userType is null/empty
     */
    public String generateToken(String userId, String userType, String firstName, String lastName, String sessionId) {
        // Validate required parameters
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (userType == null || userType.trim().isEmpty()) {
            throw new IllegalArgumentException("User type cannot be null or empty");
        }

        // Convert user ID to uppercase to maintain COBOL convention
        String normalizedUserId = userId.trim().toUpperCase();
        String normalizedUserType = userType.trim().toUpperCase();

        // Determine Spring Security role based on user type
        String role = "A".equals(normalizedUserType) ? "ROLE_ADMIN" : "ROLE_USER";

        // Build claims map with user authentication and authorization data
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_TYPE, normalizedUserType);
        claims.put(CLAIM_ROLE, role);
        
        // Add optional claims if provided
        if (firstName != null && !firstName.trim().isEmpty()) {
            claims.put(CLAIM_FIRST_NAME, firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            claims.put(CLAIM_LAST_NAME, lastName.trim());
        }
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            claims.put(CLAIM_SESSION_ID, sessionId.trim());
        }

        return createToken(claims, normalizedUserId);
    }

    /**
     * Generates a refresh token for session extension without re-authentication
     * 
     * Refresh tokens have longer expiration times and contain minimal claims
     * for security purposes. They are used to obtain new access tokens when
     * the current token expires, maintaining user sessions in distributed
     * microservices environments.
     * 
     * @param userId User identifier for token subject
     * @param sessionId Session correlation identifier
     * @return Signed refresh token string
     * 
     * @throws IllegalArgumentException if userId is null/empty
     */
    public String generateRefreshToken(String userId, String sessionId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        String normalizedUserId = userId.trim().toUpperCase();
        
        // Minimal claims for refresh tokens (security consideration)
        Map<String, Object> claims = new HashMap<>();
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            claims.put(CLAIM_SESSION_ID, sessionId.trim());
        }

        return createRefreshToken(claims, normalizedUserId);
    }

    /**
     * Validates JWT token signature, expiration, and format
     * 
     * This method performs comprehensive token validation including:
     * - Signature verification using the configured secret key
     * - Expiration time checking against current timestamp
     * - Token format and structure validation
     * - Claims integrity verification
     * 
     * The validation process ensures that only authentic tokens issued
     * by the CardDemo authentication service are accepted for API access.
     * 
     * @param token JWT token string to validate
     * @return true if token is valid and not expired, false otherwise
     * 
     * @throws IllegalArgumentException if token is null or empty
     */
    public Boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            // Parse and validate token signature and claims
            Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token.trim());
            
            return !isTokenExpired(token.trim());
        } catch (SignatureException e) {
            // Token signature is invalid
            return false;
        } catch (MalformedJwtException e) {
            // Token format is invalid
            return false;
        } catch (ExpiredJwtException e) {
            // Token has expired
            return false;
        } catch (UnsupportedJwtException e) {
            // Token type is not supported
            return false;
        } catch (IllegalArgumentException e) {
            // Token claims string is empty
            return false;
        }
    }

    /**
     * Extracts all claims from JWT token
     * 
     * Parses the JWT token and returns all embedded claims for authorization
     * and user context processing. This method is used internally by other
     * claim extraction methods and for comprehensive token analysis.
     * 
     * @param token JWT token string
     * @return Claims object containing all token claims
     * 
     * @throws ExpiredJwtException if token has expired
     * @throws MalformedJwtException if token format is invalid
     * @throws SignatureException if token signature is invalid
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts username (subject) from JWT token
     * 
     * Retrieves the user identifier from the token's subject claim.
     * This corresponds to the SEC-USR-ID from the original VSAM USRSEC
     * file and maintains the 8-character uppercase format from COBOL.
     * 
     * @param token JWT token string
     * @return User ID (username) from token subject claim
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts user type from JWT token claims
     * 
     * Retrieves the user type classification ('A' for Admin, 'U' for User)
     * from the token claims. This maintains compatibility with the original
     * COBOL SEC-USR-TYPE field and enables role-based authorization.
     * 
     * @param token JWT token string
     * @return User type ('A' for Admin, 'U' for User)
     */
    public String extractUserType(String token) {
        return extractClaim(token, claims -> (String) claims.get(CLAIM_USER_TYPE));
    }

    /**
     * Extracts Spring Security role from JWT token claims
     * 
     * Retrieves the Spring Security role (ROLE_ADMIN or ROLE_USER) from
     * the token claims for method-level authorization using @PreAuthorize
     * annotations in the microservices.
     * 
     * @param token JWT token string
     * @return Spring Security role string
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> (String) claims.get(CLAIM_ROLE));
    }

    /**
     * Extracts session ID from JWT token claims
     * 
     * Retrieves the session correlation identifier for distributed session
     * tracking across microservices. This enables session management
     * equivalent to CICS pseudo-conversational processing.
     * 
     * @param token JWT token string
     * @return Session correlation identifier
     */
    public String extractSessionId(String token) {
        return extractClaim(token, claims -> (String) claims.get(CLAIM_SESSION_ID));
    }

    /**
     * Extracts user's first name from JWT token claims
     * 
     * @param token JWT token string
     * @return User's first name
     */
    public String extractFirstName(String token) {
        return extractClaim(token, claims -> (String) claims.get(CLAIM_FIRST_NAME));
    }

    /**
     * Extracts user's last name from JWT token claims
     * 
     * @param token JWT token string
     * @return User's last name
     */
    public String extractLastName(String token) {
        return extractClaim(token, claims -> (String) claims.get(CLAIM_LAST_NAME));
    }

    /**
     * Extracts token expiration date
     * 
     * @param token JWT token string
     * @return Token expiration timestamp
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Checks if JWT token has expired
     * 
     * Compares the token's expiration time against the current timestamp
     * to determine if the token is still valid. Expired tokens are
     * automatically rejected by the Spring Security filter chain.
     * 
     * @param token JWT token string
     * @return true if token has expired, false otherwise
     */
    public Boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration.before(new Date());
    }

    // Private helper methods

    /**
     * Generic method to extract specific claims from JWT token
     * 
     * @param token JWT token string
     * @param claimsResolver Function to extract specific claim
     * @param <T> Type of claim to extract
     * @return Extracted claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Creates JWT access token with standard expiration time
     * 
     * @param claims Token claims map
     * @param subject Token subject (user ID)
     * @return Signed JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    /**
     * Creates JWT refresh token with extended expiration time
     * 
     * @param claims Token claims map (minimal for security)
     * @param subject Token subject (user ID)
     * @return Signed refresh token string
     */
    private String createRefreshToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }
}