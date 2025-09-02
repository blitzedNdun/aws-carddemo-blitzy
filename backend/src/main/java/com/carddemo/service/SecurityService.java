package com.carddemo.service;

import com.carddemo.entity.UserSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security service providing authentication, authorization, and role-based access control functionality
 * replacing RACF security controls. Integrates with Spring Security for user authentication and session management.
 * 
 * This service translates the COBOL COSGN00C program logic to modern Spring Security patterns while maintaining
 * identical authentication behavior and user role management.
 * 
 * Key Functions:
 * - User authentication against PostgreSQL user_security table (replaces VSAM USRSEC dataset)
 * - Role-based authorization (Admin 'A' vs User 'U' types)
 * - Session token generation and validation (replaces CICS COMMAREA)
 * - Security context management for Spring Boot controllers
 */
@Service
public class SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    /**
     * Constants replicated from COBOL COSGN00C program
     */
    private static final String USER_TYPE_ADMIN = "A";
    private static final String USER_TYPE_USER = "U";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final long SESSION_TIMEOUT_MINUTES = 30;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // In-memory session store (replaces CICS COMMAREA)
    // In production, this would be replaced with Redis or database storage
    private final Map<String, SessionContext> activeSessions = new HashMap<>();

    /**
     * Session context structure replicating CICS COMMAREA data
     */
    private static class SessionContext {
        private String userId;
        private String userType;
        private String fromTransactionId;
        private String fromProgram;
        private LocalDateTime loginTime;
        private LocalDateTime lastAccessTime;
        private int programContext;

        public SessionContext(String userId, String userType) {
            this.userId = userId;
            this.userType = userType;
            this.loginTime = LocalDateTime.now();
            this.lastAccessTime = LocalDateTime.now();
            this.fromTransactionId = "CC00";  // COBOL WS-TRANID
            this.fromProgram = "COSGN00C";    // COBOL WS-PGMNAME
            this.programContext = 0;          // COBOL CDEMO-PGM-CONTEXT
        }

        // Getters and setters
        public String getUserId() { return userId; }
        public String getUserType() { return userType; }
        public String getFromTransactionId() { return fromTransactionId; }
        public String getFromProgram() { return fromProgram; }
        public LocalDateTime getLoginTime() { return loginTime; }
        public LocalDateTime getLastAccessTime() { return lastAccessTime; }
        public int getProgramContext() { return programContext; }

        public void setLastAccessTime(LocalDateTime lastAccessTime) {
            this.lastAccessTime = lastAccessTime;
        }

        public boolean isExpired() {
            return lastAccessTime.isBefore(LocalDateTime.now().minusMinutes(SESSION_TIMEOUT_MINUTES));
        }
    }

    /**
     * Authenticates user credentials against the PostgreSQL user_security table.
     * Replicates the COBOL READ-USER-SEC-FILE logic from COSGN00C program.
     * 
     * @param username User ID (converted to uppercase like COBOL FUNCTION UPPER-CASE)
     * @param password User password (converted to uppercase like COBOL)
     * @return Authentication result with user details and authorities
     * @throws AuthenticationException if authentication fails
     */
    @Transactional
    public Authentication authenticateUser(String username, String password) {
        logger.info("Authenticating user: {}", username);

        try {
            // Convert to uppercase like COBOL: MOVE FUNCTION UPPER-CASE(USERIDI OF COSGN0AI) TO WS-USER-ID
            String upperUsername = username.toUpperCase();
            String upperPassword = password.toUpperCase();

            // Validate input parameters (replicates COBOL input validation)
            if (upperUsername == null || upperUsername.trim().isEmpty()) {
                logger.warn("Authentication failed: empty username");
                throw new BadCredentialsException("Please enter User ID ...");
            }

            if (upperPassword == null || upperPassword.trim().isEmpty()) {
                logger.warn("Authentication failed: empty password for user {}", upperUsername);
                throw new BadCredentialsException("Please enter Password ...");
            }

            // Load user details (replicates COBOL EXEC CICS READ DATASET(WS-USRSEC-FILE))
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(upperUsername);
            } catch (UsernameNotFoundException e) {
                logger.warn("User not found: {}", upperUsername);
                throw new BadCredentialsException("User not found. Try again ...");
            }

            // Cast to our UserSecurity entity to access additional fields
            if (!(userDetails instanceof UserSecurity)) {
                logger.error("UserDetails is not UserSecurity instance for user: {}", upperUsername);
                throw new BadCredentialsException("Unable to verify the User ...");
            }

            UserSecurity userSecurity = (UserSecurity) userDetails;

            // Password validation (replicates COBOL: IF SEC-USR-PWD = WS-USER-PWD)
            if (!passwordEncoder.matches(upperPassword, userSecurity.getPassword())) {
                // Update failed login attempts
                userSecurity.incrementFailedLoginAttempts();
                
                // Check if account should be locked
                if (userSecurity.shouldLockAccount()) {
                    userSecurity.setAccountNonLocked(false);
                    logger.warn("Account locked due to failed attempts: {}", upperUsername);
                    throw new BadCredentialsException("Account locked due to failed login attempts");
                }

                logger.warn("Wrong password for user: {}", upperUsername);
                throw new BadCredentialsException("Wrong Password. Try again ...");
            }

            // Successful authentication - reset failed attempts and update login time
            userSecurity.resetFailedLoginAttempts();
            userSecurity.updateLastLogin();

            // Create authentication token with authorities
            Collection<? extends GrantedAuthority> authorities = userSecurity.getAuthorities();
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, authorities);

            // Generate session token and store session context (replaces CICS COMMAREA)
            String sessionToken = generateSessionToken(userSecurity);

            logger.info("Authentication successful for user: {} with type: {}", 
                       upperUsername, userSecurity.getUserType());

            return authentication;

        } catch (AuthenticationException e) {
            logger.error("Authentication failed for user: {} - {}", username, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during authentication for user: {}", username, e);
            throw new BadCredentialsException("Unable to verify the User ...");
        }
    }

    /**
     * Validates user role against required role.
     * Replicates COBOL user type checking logic from COSGN00C.
     * 
     * @param userId User ID to validate
     * @param requiredRole Required role (ROLE_ADMIN or ROLE_USER)
     * @return true if user has required role, false otherwise
     */
    public boolean validateUserRole(String userId, String requiredRole) {
        logger.debug("Validating role for user: {} against required role: {}", userId, requiredRole);

        try {
            String upperUserId = userId.toUpperCase();
            UserDetails userDetails = userDetailsService.loadUserByUsername(upperUserId);
            
            if (!(userDetails instanceof UserSecurity)) {
                logger.warn("Cannot validate role - UserDetails is not UserSecurity instance");
                return false;
            }

            UserSecurity userSecurity = (UserSecurity) userDetails;
            String userType = userSecurity.getUserType();

            // Map COBOL user types to Spring Security roles
            boolean hasRole = false;
            if (ROLE_ADMIN.equals(requiredRole)) {
                hasRole = USER_TYPE_ADMIN.equals(userType);
            } else if (ROLE_USER.equals(requiredRole)) {
                hasRole = USER_TYPE_ADMIN.equals(userType) || USER_TYPE_USER.equals(userType);
            }

            logger.debug("Role validation result for user {} (type {}): {}", 
                        upperUserId, userType, hasRole);
            return hasRole;

        } catch (UsernameNotFoundException e) {
            logger.warn("User not found during role validation: {}", userId);
            return false;
        } catch (Exception e) {
            logger.error("Error during role validation for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Checks specific permissions for current user based on operation type.
     * Implements fine-grained authorization logic beyond basic role checking.
     * 
     * @param operation Operation being performed
     * @param resourceId Resource identifier (optional)
     * @return true if user has permission, false otherwise
     */
    public boolean checkPermissions(String operation, String resourceId) {
        logger.debug("Checking permissions for operation: {} on resource: {}", operation, resourceId);

        try {
            UserSecurity currentUser = getCurrentUser();
            if (currentUser == null) {
                logger.warn("No current user found for permission check");
                return false;
            }

            String userType = currentUser.getUserType();

            // Admin users have all permissions (replicates COBOL CDEMO-USRTYP-ADMIN check)
            if (USER_TYPE_ADMIN.equals(userType)) {
                logger.debug("Admin user has all permissions");
                return true;
            }

            // Regular users have limited permissions
            if (USER_TYPE_USER.equals(userType)) {
                return checkUserPermissions(operation, resourceId, currentUser);
            }

            logger.warn("Unknown user type: {} for user: {}", userType, currentUser.getUsername());
            return false;

        } catch (Exception e) {
            logger.error("Error checking permissions for operation: {}", operation, e);
            return false;
        }
    }

    /**
     * Helper method to check permissions for regular users
     */
    private boolean checkUserPermissions(String operation, String resourceId, UserSecurity user) {
        // Regular users can:
        // - View their own data
        // - Perform basic transactions
        // - Access customer inquiry functions
        
        switch (operation.toUpperCase()) {
            case "VIEW_ACCOUNT":
            case "VIEW_CUSTOMER":
            case "VIEW_TRANSACTION":
            case "VIEW_CARD":
                return true; // Regular users can view data
            
            case "UPDATE_CUSTOMER":
            case "UPDATE_ACCOUNT":
                return true; // Regular users can update some data
            
            case "CREATE_USER":
            case "DELETE_USER":
            case "UPDATE_USER":
            case "ADMIN_FUNCTIONS":
                return false; // Only admins can perform these operations
            
            default:
                logger.warn("Unknown operation: {} for permission check", operation);
                return false;
        }
    }

    /**
     * Generates session token for authenticated user.
     * Replaces CICS COMMAREA session management with modern token-based approach.
     * 
     * @param userSecurity Authenticated user details
     * @return Session token string
     */
    public String generateSessionToken(UserSecurity userSecurity) {
        logger.debug("Generating session token for user: {}", userSecurity.getUsername());

        try {
            // Generate unique session token
            String sessionToken = UUID.randomUUID().toString();

            // Create session context (replicates CICS COMMAREA setup)
            SessionContext sessionContext = new SessionContext(
                userSecurity.getSecUsrId(), 
                userSecurity.getUserType()
            );

            // Store session (in production, use Redis or database)
            activeSessions.put(sessionToken, sessionContext);

            logger.info("Session token generated for user: {} with type: {}", 
                       userSecurity.getUsername(), userSecurity.getUserType());

            return sessionToken;

        } catch (Exception e) {
            logger.error("Error generating session token for user: {}", userSecurity.getUsername(), e);
            throw new RuntimeException("Unable to generate session token");
        }
    }

    /**
     * Validates session token and returns session information.
     * Replaces CICS session validation logic.
     * 
     * @param sessionToken Session token to validate
     * @return true if session is valid, false otherwise
     */
    public boolean validateSession(String sessionToken) {
        logger.debug("Validating session token");

        try {
            if (sessionToken == null || sessionToken.trim().isEmpty()) {
                logger.warn("Empty session token provided");
                return false;
            }

            SessionContext sessionContext = activeSessions.get(sessionToken);
            if (sessionContext == null) {
                logger.warn("Session not found for token");
                return false;
            }

            // Check if session has expired
            if (sessionContext.isExpired()) {
                logger.info("Session expired for user: {}", sessionContext.getUserId());
                activeSessions.remove(sessionToken);
                return false;
            }

            // Update last access time
            sessionContext.setLastAccessTime(LocalDateTime.now());

            logger.debug("Session validated successfully for user: {}", sessionContext.getUserId());
            return true;

        } catch (Exception e) {
            logger.error("Error validating session token", e);
            return false;
        }
    }

    /**
     * Checks if current user has administrative access.
     * Replicates COBOL CDEMO-USRTYP-ADMIN condition check.
     * 
     * @return true if current user is admin, false otherwise
     */
    public boolean hasAdminAccess() {
        logger.debug("Checking admin access for current user");

        try {
            UserSecurity currentUser = getCurrentUser();
            if (currentUser == null) {
                logger.debug("No current user found");
                return false;
            }

            boolean isAdmin = USER_TYPE_ADMIN.equals(currentUser.getUserType());
            logger.debug("Admin access check result for user {}: {}", 
                        currentUser.getUsername(), isAdmin);
            
            return isAdmin;

        } catch (Exception e) {
            logger.error("Error checking admin access", e);
            return false;
        }
    }

    /**
     * Gets current authenticated user from Spring Security context.
     * Replaces CICS COMMAREA user context retrieval.
     * 
     * @return Current UserSecurity object or null if not authenticated
     */
    public UserSecurity getCurrentUser() {
        logger.debug("Getting current user from security context");

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.debug("No authenticated user found");
                return null;
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof UserSecurity) {
                UserSecurity user = (UserSecurity) principal;
                logger.debug("Current user: {} with type: {}", user.getUsername(), user.getUserType());
                return user;
            }

            if (principal instanceof String) {
                // Handle case where principal is username string
                String username = (String) principal;
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (userDetails instanceof UserSecurity) {
                        return (UserSecurity) userDetails;
                    }
                } catch (UsernameNotFoundException e) {
                    logger.warn("User not found when getting current user: {}", username);
                }
            }

            logger.warn("Principal is not UserSecurity instance: {}", principal.getClass().getName());
            return null;

        } catch (Exception e) {
            logger.error("Error getting current user", e);
            return null;
        }
    }

    /**
     * Invalidates session for user logout.
     * Replaces CICS session cleanup logic.
     * 
     * @param sessionToken Session token to invalidate
     */
    public void invalidateSession(String sessionToken) {
        logger.debug("Invalidating session token");

        try {
            if (sessionToken != null && !sessionToken.trim().isEmpty()) {
                SessionContext sessionContext = activeSessions.remove(sessionToken);
                if (sessionContext != null) {
                    logger.info("Session invalidated for user: {}", sessionContext.getUserId());
                } else {
                    logger.warn("Session token not found for invalidation");
                }
            }
        } catch (Exception e) {
            logger.error("Error invalidating session", e);
        }
    }

    /**
     * Cleans up expired sessions periodically.
     * Should be called by scheduled task.
     */
    public void cleanupExpiredSessions() {
        logger.debug("Cleaning up expired sessions");

        try {
            activeSessions.entrySet().removeIf(entry -> {
                boolean expired = entry.getValue().isExpired();
                if (expired) {
                    logger.info("Removing expired session for user: {}", entry.getValue().getUserId());
                }
                return expired;
            });
        } catch (Exception e) {
            logger.error("Error cleaning up expired sessions", e);
        }
    }

    /**
     * Gets session information for a given token.
     * 
     * @param sessionToken Session token
     * @return Session context information or null if not found
     */
    public Map<String, Object> getSessionInfo(String sessionToken) {
        logger.debug("Getting session info for token");

        try {
            SessionContext sessionContext = activeSessions.get(sessionToken);
            if (sessionContext == null || sessionContext.isExpired()) {
                return null;
            }

            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("userId", sessionContext.getUserId());
            sessionInfo.put("userType", sessionContext.getUserType());
            sessionInfo.put("loginTime", sessionContext.getLoginTime());
            sessionInfo.put("lastAccessTime", sessionContext.getLastAccessTime());
            sessionInfo.put("fromTransactionId", sessionContext.getFromTransactionId());
            sessionInfo.put("fromProgram", sessionContext.getFromProgram());

            return sessionInfo;

        } catch (Exception e) {
            logger.error("Error getting session info", e);
            return null;
        }
    }
}