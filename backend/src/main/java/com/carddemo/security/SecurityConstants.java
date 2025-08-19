package com.carddemo.security;

/**
 * Security-related constants and configuration values for JWT token management
 * and authentication/authorization processes. Provides centralized security
 * configuration matching COBOL-to-Java migration requirements.
 *
 * This class centralizes security constants used throughout the authentication
 * and authorization flow, ensuring consistent security behavior across the
 * modernized credit card management system.
 */
public final class SecurityConstants {

    /**
     * JWT token expiration time in milliseconds (24 hours)
     * Configured via application properties or default to 24 hours
     */
    public static final long JWT_EXPIRATION = 24 * 60 * 60 * 1000; // 24 hours

    /**
     * JWT secret key for token signing and validation
     * Should be configured via application properties in production
     * Default value provided for development/testing
     */
    public static final String JWT_SECRET = "cardDemoSecretKeyForJWTTokenSigningAndValidation2024CardDemo";

    /**
     * JWT token prefix for Authorization header
     */
    public static final String JWT_TOKEN_PREFIX = "Bearer ";

    /**
     * HTTP header name for JWT token
     */
    public static final String JWT_HEADER_NAME = "Authorization";

    /**
     * Session timeout in milliseconds (30 minutes)
     * Matches CICS session timeout behavior
     */
    public static final long SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes

    /**
     * Maximum login attempts before account lockout
     */
    public static final int MAX_LOGIN_ATTEMPTS = 3;

    /**
     * Account lockout duration in milliseconds (15 minutes)
     */
    public static final long ACCOUNT_LOCKOUT_DURATION = 15 * 60 * 1000; // 15 minutes

    /**
     * Password minimum length requirement
     */
    public static final int PASSWORD_MIN_LENGTH = 8;

    /**
     * Password maximum length requirement
     */
    public static final int PASSWORD_MAX_LENGTH = 20;

    /**
     * User ID maximum length (from COBOL SEC-USR-ID field)
     */
    public static final int USER_ID_MAX_LENGTH = 8;

    /**
     * User name maximum length (from COBOL SEC-USR-NAME field)
     */
    public static final int USER_NAME_MAX_LENGTH = 25;

    /**
     * Default user type for regular users
     */
    public static final String USER_TYPE_REGULAR = "U";

    /**
     * Admin user type
     */
    public static final String USER_TYPE_ADMIN = "A";

    /**
     * Authentication endpoint for user login
     */
    public static final String AUTH_LOGIN_ENDPOINT = "/api/auth/login";

    /**
     * Authentication endpoint for user logout
     */
    public static final String AUTH_LOGOUT_ENDPOINT = "/api/auth/logout";

    /**
     * Role prefix for Spring Security authorities
     */
    public static final String ROLE_PREFIX = "ROLE_";

    /**
     * Admin role name
     */
    public static final String ROLE_ADMIN = ROLE_PREFIX + "ADMIN";

    /**
     * User role name
     */
    public static final String ROLE_USER = ROLE_PREFIX + "USER";

    /**
     * Private constructor to prevent instantiation
     */
    private SecurityConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the complete role name for a user type
     *
     * @param userType user type ('A' for admin, 'U' for user)
     * @return complete role name with ROLE_ prefix
     */
    public static String getRoleForUserType(String userType) {
        if (USER_TYPE_ADMIN.equals(userType)) {
            return ROLE_ADMIN;
        } else if (USER_TYPE_REGULAR.equals(userType)) {
            return ROLE_USER;
        } else {
            return ROLE_USER; // Default to user role
        }
    }

    /**
     * Validates if a user type is valid
     *
     * @param userType user type to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidUserType(String userType) {
        return USER_TYPE_ADMIN.equals(userType) || USER_TYPE_REGULAR.equals(userType);
    }
}