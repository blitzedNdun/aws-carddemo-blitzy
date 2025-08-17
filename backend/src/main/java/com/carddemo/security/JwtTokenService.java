package com.carddemo.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Collection;
import java.util.function.Function;
import org.slf4j.LoggerFactory;
import java.security.Key;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

import com.carddemo.security.SecurityConstants;
import com.carddemo.security.SessionAttributes;
import com.carddemo.entity.UserSecurity;

import org.slf4j.Logger;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT token generation and validation service managing token lifecycle, claims extraction, 
 * and signature verification. Provides methods for creating JWT tokens with user authorities 
 * and validating incoming tokens for API authentication.
 * 
 * This service integrates with Redis for token blacklisting and session management,
 * providing hybrid stateless/stateful authentication capabilities.
 */
@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Token blacklist prefix for Redis keys
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    
    // Refresh token prefix for Redis keys
    private static final String REFRESH_PREFIX = "jwt:refresh:";

    @Autowired
    public JwtTokenService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generates a JWT token for the authenticated user with embedded claims including
     * user ID, authorities, and session attributes. The token is signed using HMAC SHA-256
     * with the configured secret key.
     * 
     * @param userDetails Spring Security UserDetails containing user authentication information
     * @return JWT token string with user claims and authorities
     */
    public String generateToken(UserDetails userDetails) {
        try {
            Map<String, Object> claims = new HashMap<>();
            
            // Extract user information if UserSecurity instance
            if (userDetails instanceof UserSecurity userSecurity) {
                claims.put(SessionAttributes.SEC_USR_ID, userSecurity.getSecUsrId());
                claims.put(SessionAttributes.SEC_USR_TYPE, userSecurity.getUserType());
                claims.put(SessionAttributes.SEC_USR_NAME, userSecurity.getUsername());
                claims.put("firstName", userSecurity.getFirstName());
                claims.put("lastName", userSecurity.getLastName());
            }
            
            // Add authorities/roles to claims
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
            if (authorities != null && !authorities.isEmpty()) {
                List<String> roles = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
                claims.put("authorities", roles);
            }
            
            return createToken(claims, userDetails.getUsername());
            
        } catch (Exception e) {
            logger.error("Error generating JWT token for user: {}", userDetails.getUsername(), e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    /**
     * Creates a JWT token with the specified claims and subject (username).
     * The token includes issued date, expiration date, and is signed with the secret key.
     * 
     * @param claims Map of claims to embed in the token
     * @param subject Username/subject for the token
     * @return Signed JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + SecurityConstants.JWT_EXPIRATION);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Validates a JWT token by verifying its signature, expiration, and blacklist status.
     * Returns true if the token is valid and not blacklisted.
     * 
     * @param token JWT token string to validate
     * @return true if token is valid, false otherwise
     */
    public Boolean validateToken(String token) {
        try {
            // Check if token is blacklisted
            if (isTokenBlacklisted(token)) {
                logger.warn("Attempted use of blacklisted token");
                return false;
            }
            
            // Parse and validate token signature and expiration
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            
            // Additional validation - check if token is expired
            if (isTokenExpired(token)) {
                logger.warn("Token validation failed - token is expired");
                return false;
            }
            
            return true;
            
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token validation failed - token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            logger.warn("JWT token validation failed - unsupported token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logger.warn("JWT token validation failed - malformed token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            logger.warn("JWT token validation failed - invalid signature: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token validation failed - illegal argument: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("JWT token validation failed - unexpected error", e);
            return false;
        }
    }

    /**
     * Extracts the username (subject) from a JWT token.
     * 
     * @param token JWT token string
     * @return Username from token subject claim
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from a JWT token.
     * 
     * @param token JWT token string
     * @return Expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from a JWT token using the provided function.
     * 
     * @param token JWT token string
     * @param claimsResolver Function to extract specific claim
     * @param <T> Type of the claim being extracted
     * @return Extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from a JWT token.
     * 
     * @param token JWT token string
     * @return Claims object containing all token claims
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (JwtException e) {
            logger.error("Failed to extract claims from token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Checks if a JWT token has expired.
     * 
     * @param token JWT token string
     * @return true if token is expired, false otherwise
     */
    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            logger.warn("Error checking token expiration", e);
            return true; // Consider expired if we can't determine expiration
        }
    }

    /**
     * Generates a new JWT token from an existing valid token (refresh functionality).
     * The new token maintains the same claims but extends the expiration time.
     * 
     * @param token Existing valid JWT token
     * @return New JWT token with extended expiration
     */
    public String refreshToken(String token) {
        try {
            if (!validateToken(token)) {
                throw new RuntimeException("Cannot refresh invalid token");
            }
            
            Claims claims = extractClaims(token);
            String username = claims.getSubject();
            
            // Create new token with same claims but new expiration
            Map<String, Object> newClaims = new HashMap<>(claims);
            newClaims.remove(Claims.ISSUED_AT);
            newClaims.remove(Claims.EXPIRATION);
            
            String newToken = createToken(newClaims, username);
            
            // Store refresh mapping in Redis for tracking
            String refreshKey = REFRESH_PREFIX + extractTokenId(token);
            redisTemplate.opsForValue().set(refreshKey, newToken, 
                SecurityConstants.JWT_EXPIRATION, TimeUnit.MILLISECONDS);
            
            logger.info("Token refreshed successfully for user: {}", username);
            return newToken;
            
        } catch (Exception e) {
            logger.error("Error refreshing JWT token", e);
            throw new RuntimeException("Failed to refresh token", e);
        }
    }

    /**
     * Adds a JWT token to the blacklist, preventing its future use.
     * The token is stored in Redis with an expiration matching the token's expiration.
     * 
     * @param token JWT token to blacklist
     */
    public void blacklistToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                logger.warn("Attempted to blacklist null or empty token");
                return;
            }
            
            String tokenId = extractTokenId(token);
            String blacklistKey = BLACKLIST_PREFIX + tokenId;
            
            // Calculate time until token expiration
            Date expiration = extractExpiration(token);
            long timeUntilExpiration = expiration.getTime() - System.currentTimeMillis();
            
            if (timeUntilExpiration > 0) {
                // Store in Redis until token would naturally expire
                redisTemplate.opsForValue().set(blacklistKey, "blacklisted", 
                    timeUntilExpiration, TimeUnit.MILLISECONDS);
                
                logger.info("Token blacklisted successfully, expires in {} ms", timeUntilExpiration);
            } else {
                logger.info("Token already expired, no need to blacklist");
            }
            
        } catch (Exception e) {
            logger.error("Error blacklisting JWT token", e);
            // Don't throw exception for blacklisting errors to avoid breaking logout flow
        }
    }

    /**
     * Checks if a JWT token is in the blacklist.
     * 
     * @param token JWT token to check
     * @return true if token is blacklisted, false otherwise
     */
    public Boolean isTokenBlacklisted(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return true; // Consider null/empty tokens as blacklisted
            }
            
            String tokenId = extractTokenId(token);
            String blacklistKey = BLACKLIST_PREFIX + tokenId;
            
            Boolean exists = redisTemplate.hasKey(blacklistKey);
            return exists != null && exists;
            
        } catch (Exception e) {
            logger.error("Error checking token blacklist status", e);
            return false; // Don't block on Redis errors
        }
    }

    /**
     * Extracts a unique identifier from a JWT token for blacklisting purposes.
     * Uses a combination of issued date and subject to create a unique identifier.
     * 
     * @param token JWT token string
     * @return Unique token identifier
     */
    private String extractTokenId(String token) {
        try {
            Claims claims = extractClaims(token);
            Date issuedAt = claims.getIssuedAt();
            String subject = claims.getSubject();
            
            // Create unique ID from issued time and subject
            long issuedTime = issuedAt != null ? issuedAt.getTime() : 0;
            return subject + ":" + issuedTime;
            
        } catch (Exception e) {
            logger.warn("Error extracting token ID, using hash fallback", e);
            // Fallback to token hash if claims extraction fails
            return String.valueOf(token.hashCode());
        }
    }

    /**
     * Gets the signing key for JWT operations.
     * Creates an HMAC SHA-256 key from the configured secret.
     * 
     * @return SecretKey for JWT signing and validation
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = SecurityConstants.JWT_SECRET.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}