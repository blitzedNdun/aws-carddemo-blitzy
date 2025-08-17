package com.carddemo.security;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Session attribute constants and utility class defining standard session keys for user context,
 * navigation state, and security information. Replaces CICS COMMAREA field definitions with
 * Spring Session attributes for maintaining session state across distributed Spring Boot services.
 * 
 * This class provides centralized session attribute management equivalent to CICS COMMAREA
 * functionality, enabling stateful session management in the modernized credit card system.
 * The constants and utility methods ensure consistent session handling across all controllers
 * and services while preserving the exact session state semantics of the original CICS implementation.
 *
 * Key Features:
 * - Session attribute constants matching CICS COMMAREA field structure
 * - Utility methods for safe session attribute access and manipulation
 * - Type-safe session attribute retrieval with null protection
 * - Session size validation enforcing 32KB COMMAREA-equivalent limits
 * - Integration with Spring Session Redis for distributed session storage
 *
 * Architecture Integration:
 * This component integrates with SessionConfig for Redis session management and provides
 * the session attribute vocabulary used throughout the Spring Security authentication
 * and authorization flow, ensuring consistent session state management across the application.
 *
 * @see SessionConfig for Redis session configuration
 * @see org.springframework.session.Session for Spring Session integration
 */
public final class SessionAttributes {

    private static final Logger logger = LoggerFactory.getLogger(SessionAttributes.class);
    
    /**
     * Maximum session storage size matching CICS COMMAREA 32KB limit
     */
    public static final int MAX_SESSION_SIZE = 32768; // 32KB
    
    // Core User Security Attributes (from CICS COMMAREA)
    
    /**
     * Authenticated user identifier (SEC-USR-ID from COCOM01Y copybook)
     * Maps to CICS COMMAREA user ID field for session continuity
     */
    public static final String SEC_USR_ID = "SEC_USR_ID";
    
    /**
     * User type indicator (SEC-USR-TYPE from COCOM01Y copybook)
     * Values: 'A' = Administrator, 'U' = Regular User
     * Maps to CICS COMMAREA user type field for authorization decisions
     */
    public static final String SEC_USR_TYPE = "SEC_USR_TYPE";
    
    /**
     * User display name (SEC-USR-NAME from COCOM01Y copybook)
     * Maps to CICS COMMAREA user name field for UI personalization
     */
    public static final String SEC_USR_NAME = "SEC_USR_NAME";
    
    // Navigation and Transaction State Attributes
    
    /**
     * Current navigation state for preserving menu context
     * Values: MENU, TRANSACTION, REPORT, ADMIN, etc.
     * Replaces CICS transaction routing and program flow control
     */
    public static final String NAVIGATION_STATE = "NAVIGATION_STATE";
    
    /**
     * Current transaction processing state
     * Values: ACTIVE, PENDING, COMPLETE, ERROR
     * Maintains CICS transaction lifecycle equivalent semantics
     */
    public static final String TRANSACTION_STATE = "TRANSACTION_STATE";
    
    /**
     * Error message context for user feedback
     * Stores detailed error information for display consistency
     */
    public static final String ERROR_MESSAGE = "ERROR_MESSAGE";
    
    // Additional Session Context Attributes
    
    /**
     * Last accessed page for navigation history
     */
    public static final String LAST_PAGE = "LAST_PAGE";
    
    /**
     * Current page in pagination sequences
     */
    public static final String CURRENT_PAGE = "CURRENT_PAGE";
    
    /**
     * Search criteria preservation across requests
     */
    public static final String SEARCH_CRITERIA = "SEARCH_CRITERIA";
    
    /**
     * Session creation timestamp for audit trails
     */
    public static final String SESSION_CREATED_TIME = "SESSION_CREATED_TIME";
    
    /**
     * Last activity timestamp for timeout management
     */
    public static final String LAST_ACTIVITY_TIME = "LAST_ACTIVITY_TIME";
    
    // Private constructor to prevent instantiation
    private SessionAttributes() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // Utility Methods for Safe Session Attribute Access
    
    /**
     * Safely retrieves the authenticated user ID from session.
     * 
     * @param session HttpSession instance
     * @return user ID or null if not found
     */
    public static String getUserId(HttpSession session) {
        return getAttributeSafely(session, SEC_USR_ID, String.class).orElse(null);
    }
    
    /**
     * Safely retrieves the user type from session.
     * 
     * @param session HttpSession instance
     * @return user type ('A' for Admin, 'U' for User) or null if not found
     */
    public static String getUserType(HttpSession session) {
        return getAttributeSafely(session, SEC_USR_TYPE, String.class).orElse(null);
    }
    
    /**
     * Safely retrieves the user display name from session.
     * 
     * @param session HttpSession instance
     * @return user display name or null if not found
     */
    public static String getUserName(HttpSession session) {
        return getAttributeSafely(session, SEC_USR_NAME, String.class).orElse(null);
    }
    
    /**
     * Safely retrieves the navigation state from session.
     * 
     * @param session HttpSession instance
     * @return navigation state or "MENU" as default
     */
    public static String getNavigationState(HttpSession session) {
        return getAttributeSafely(session, NAVIGATION_STATE, String.class).orElse("MENU");
    }
    
    /**
     * Safely retrieves the transaction state from session.
     * 
     * @param session HttpSession instance
     * @return transaction state or "ACTIVE" as default
     */
    public static String getTransactionState(HttpSession session) {
        return getAttributeSafely(session, TRANSACTION_STATE, String.class).orElse("ACTIVE");
    }
    
    /**
     * Safely retrieves the error message from session.
     * 
     * @param session HttpSession instance
     * @return error message or null if not found
     */
    public static String getErrorMessage(HttpSession session) {
        return getAttributeSafely(session, ERROR_MESSAGE, String.class).orElse(null);
    }
    
    /**
     * Sets user authentication attributes in session.
     * 
     * @param session HttpSession instance
     * @param userId user identifier
     * @param userType user type ('A' or 'U')
     * @param userName user display name
     */
    public static void setUserAttribute(HttpSession session, String userId, String userType, String userName) {
        if (session == null) {
            logger.warn("Cannot set user attributes - session is null");
            return;
        }
        
        try {
            session.setAttribute(SEC_USR_ID, userId);
            session.setAttribute(SEC_USR_TYPE, userType);
            session.setAttribute(SEC_USR_NAME, userName);
            session.setAttribute(LAST_ACTIVITY_TIME, System.currentTimeMillis());
            
            logger.debug("User attributes set for session: userId={}, userType={}", userId, userType);
            
        } catch (Exception e) {
            logger.error("Error setting user attributes in session", e);
        }
    }
    
    /**
     * Sets navigation state in session.
     * 
     * @param session HttpSession instance
     * @param navigationState current navigation state
     */
    public static void setNavigationState(HttpSession session, String navigationState) {
        if (session != null && StringUtils.hasText(navigationState)) {
            session.setAttribute(NAVIGATION_STATE, navigationState);
            session.setAttribute(LAST_ACTIVITY_TIME, System.currentTimeMillis());
            logger.debug("Navigation state set to: {}", navigationState);
        }
    }
    
    /**
     * Sets transaction state in session.
     * 
     * @param session HttpSession instance
     * @param transactionState current transaction state
     */
    public static void setTransactionState(HttpSession session, String transactionState) {
        if (session != null && StringUtils.hasText(transactionState)) {
            session.setAttribute(TRANSACTION_STATE, transactionState);
            session.setAttribute(LAST_ACTIVITY_TIME, System.currentTimeMillis());
            logger.debug("Transaction state set to: {}", transactionState);
        }
    }
    
    /**
     * Sets error message in session.
     * 
     * @param session HttpSession instance
     * @param errorMessage error message to store
     */
    public static void setErrorMessage(HttpSession session, String errorMessage) {
        if (session != null) {
            if (StringUtils.hasText(errorMessage)) {
                session.setAttribute(ERROR_MESSAGE, errorMessage);
                logger.debug("Error message set: {}", errorMessage);
            } else {
                session.removeAttribute(ERROR_MESSAGE);
                logger.debug("Error message cleared");
            }
            session.setAttribute(LAST_ACTIVITY_TIME, System.currentTimeMillis());
        }
    }
    
    /**
     * Clears all session attributes for user logout.
     * 
     * @param session HttpSession instance
     */
    public static void clearSession(HttpSession session) {
        if (session == null) {
            logger.warn("Cannot clear session - session is null");
            return;
        }
        
        try {
            String userId = getUserId(session);
            
            // Remove core user attributes
            session.removeAttribute(SEC_USR_ID);
            session.removeAttribute(SEC_USR_TYPE);
            session.removeAttribute(SEC_USR_NAME);
            session.removeAttribute(NAVIGATION_STATE);
            session.removeAttribute(TRANSACTION_STATE);
            session.removeAttribute(ERROR_MESSAGE);
            session.removeAttribute(LAST_PAGE);
            session.removeAttribute(CURRENT_PAGE);
            session.removeAttribute(SEARCH_CRITERIA);
            
            logger.info("Session cleared for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error clearing session attributes", e);
        }
    }
    
    /**
     * Validates session storage size against COMMAREA limits.
     * 
     * @param session HttpSession instance
     * @return true if session size is within limits
     */
    public static boolean validateSessionSize(HttpSession session) {
        if (session == null) {
            return true;
        }
        
        try {
            // Estimate session size (simplified calculation)
            int estimatedSize = 0;
            
            // Count all attribute names and estimate their storage size
            java.util.Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String name = attributeNames.nextElement();
                Object value = session.getAttribute(name);
                
                // Estimate size based on object type
                estimatedSize += name.length() * 2; // UTF-16 chars
                if (value instanceof String) {
                    estimatedSize += ((String) value).length() * 2;
                } else if (value instanceof Long || value instanceof Integer) {
                    estimatedSize += 8;
                } else {
                    estimatedSize += 100; // Estimate for complex objects
                }
            }
            
            boolean withinLimits = estimatedSize <= MAX_SESSION_SIZE;
            if (!withinLimits) {
                logger.warn("Session size exceeds limit: {} bytes (max: {})", estimatedSize, MAX_SESSION_SIZE);
            }
            
            return withinLimits;
            
        } catch (Exception e) {
            logger.error("Error validating session size", e);
            return false;
        }
    }
    
    /**
     * Generic method for safely retrieving typed session attributes.
     * 
     * @param session HttpSession instance
     * @param attributeName name of the attribute
     * @param expectedType expected type of the attribute
     * @param <T> type parameter
     * @return Optional containing the attribute value or empty if not found/wrong type
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getAttributeSafely(HttpSession session, String attributeName, Class<T> expectedType) {
        if (session == null || !StringUtils.hasText(attributeName)) {
            return Optional.empty();
        }
        
        try {
            Object attribute = session.getAttribute(attributeName);
            if (attribute != null && expectedType.isAssignableFrom(attribute.getClass())) {
                return Optional.of((T) attribute);
            }
        } catch (Exception e) {
            logger.debug("Error retrieving session attribute '{}': {}", attributeName, e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Checks if the session contains valid user authentication.
     * 
     * @param session HttpSession instance
     * @return true if session contains valid user ID and type
     */
    public static boolean isAuthenticated(HttpSession session) {
        return StringUtils.hasText(getUserId(session)) && StringUtils.hasText(getUserType(session));
    }
    
    /**
     * Checks if the current user is an administrator.
     * 
     * @param session HttpSession instance
     * @return true if user type is 'A' (Administrator)
     */
    public static boolean isAdministrator(HttpSession session) {
        return "A".equals(getUserType(session));
    }
    
    /**
     * Updates the last activity timestamp for session timeout management.
     * 
     * @param session HttpSession instance
     */
    public static void updateLastActivity(HttpSession session) {
        if (session != null) {
            session.setAttribute(LAST_ACTIVITY_TIME, System.currentTimeMillis());
        }
    }
}