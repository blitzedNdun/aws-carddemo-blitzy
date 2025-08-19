package com.carddemo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Custom Spring Security Authentication Provider implementing legacy COBOL authentication logic.
 * 
 * This authentication provider preserves the exact authentication behavior from the COSGN00C 
 * COBOL program while integrating seamlessly with Spring Security framework. It handles 
 * plain-text password validation, case-insensitive username processing, and session attribute 
 * population maintaining 100% functional parity with the original mainframe implementation.
 * 
 * Key Features:
 * - Preserves COBOL authentication logic from COSGN00C (lines 132-223)
 * - Handles uppercase conversion for usernames matching COBOL FUNCTION UPPER-CASE
 * - Plain-text password comparison using NoOpPasswordEncoder for migration parity
 * - Authority assignment based on SEC-USR-TYPE field ('A' = ADMIN, 'U' = USER)
 * - Session attribute population replacing CICS COMMAREA functionality
 * - Comprehensive error handling with detailed logging for security audit
 * 
 * Authentication Flow:
 * 1. Extract username and password from Authentication object
 * 2. Convert username to uppercase (replicating COBOL logic)
 * 3. Load user details through CustomUserDetailsService
 * 4. Compare passwords in plain text (matching COBOL SEC-USR-PWD comparison)
 * 5. Create authenticated token with authorities based on user type
 * 6. Populate session attributes with user context (SEC-USR-ID, SEC-USR-TYPE)
 * 7. Return authenticated token or throw BadCredentialsException
 * 
 * Integration Points:
 * - CustomUserDetailsService for PostgreSQL user lookup
 * - SessionAttributes for consistent session management
 * - Spring Security authentication framework
 * - Redis session store for distributed session state
 * 
 * Security Considerations:
 * - Plain-text password storage maintained for legacy compatibility
 * - Future migration path to BCrypt password encoding documented
 * - Comprehensive audit logging for security compliance
 * - Session security through Spring Session Redis integration
 * 
 * @see CustomUserDetailsService for user details loading
 * @see SessionAttributes for session attribute management
 * @see COSGN00C.cbl for original COBOL authentication logic
 */
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationProvider.class);

    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * Authenticates user credentials preserving exact COBOL authentication logic.
     * 
     * This method implements the core authentication flow from COSGN00C COBOL program,
     * maintaining identical validation behavior while leveraging Spring Security
     * framework capabilities. Performs case-insensitive authentication with plain-text
     * password comparison and populates session context matching CICS COMMAREA behavior.
     * 
     * Authentication Process:
     * 1. Extract username and password from Authentication object (line 132-136 equivalent)
     * 2. Convert username to uppercase matching COBOL FUNCTION UPPER-CASE
     * 3. Load user details through CustomUserDetailsService (replaces CICS READ at lines 211-219)
     * 4. Compare passwords in plain text matching COBOL logic (line 223: SEC-USR-PWD = WS-USER-PWD)
     * 5. Create authenticated token with granted authorities based on SEC-USR-TYPE
     * 6. Populate session attributes with user context (lines 224-228 equivalent)
     * 7. Return authenticated token or throw BadCredentialsException for invalid credentials
     * 
     * Error Handling:
     * - UsernameNotFoundException: User not found (replicates line 249 error)
     * - BadCredentialsException: Invalid password (replicates line 242 error)
     * - Detailed logging for security audit and troubleshooting
     * 
     * @param authentication the authentication request object containing username and password
     * @return fully authenticated object including credentials and granted authorities
     * @throws AuthenticationException if authentication fails for any reason
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        logger.debug("Starting authentication process for user authentication request");
        
        try {
            // Extract username and password from authentication object
            // Replicates: USERIDI OF COSGN0AI and PASSWDI OF COSGN0AI (lines 132-135)
            String username = authentication.getPrincipal().toString();
            String password = authentication.getCredentials().toString();
            
            logger.debug("Authentication request received for username: {}", username);
            
            // Validate input parameters
            if (username == null || username.trim().isEmpty()) {
                logger.warn("Authentication failed: Username is null or empty");
                throw new BadCredentialsException("Please enter User ID ...");
            }
            
            if (password == null || password.trim().isEmpty()) {
                logger.warn("Authentication failed: Password is null or empty for user: {}", username);
                throw new BadCredentialsException("Please enter Password ...");
            }
            
            // Convert username to uppercase matching COBOL logic
            // Replicates: MOVE FUNCTION UPPER-CASE(USERIDI OF COSGN0AI) TO WS-USER-ID (lines 132-134)
            String upperCaseUsername = username.toUpperCase();
            
            logger.debug("Converted username to uppercase: {} -> {}", username, upperCaseUsername);
            
            // Load user details through CustomUserDetailsService
            // Replaces CICS READ operation from lines 211-219 in COSGN00C
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(upperCaseUsername);
                logger.debug("Successfully loaded user details for user: {}", upperCaseUsername);
            } catch (UsernameNotFoundException e) {
                // Handle user not found case matching COBOL error handling
                // Replicates error response from lines 247-251 in COSGN00C
                logger.warn("User not found during authentication: {}", upperCaseUsername);
                throw new BadCredentialsException("User not found. Try again ...");
            }
            
            // Compare passwords in plain text matching COBOL logic
            // Replicates: IF SEC-USR-PWD = WS-USER-PWD (line 223 in COSGN00C)
            // Convert password to uppercase to match COBOL behavior (line 135-136)
            String upperCasePassword = password.toUpperCase();
            String storedPassword = userDetails.getPassword();
            
            logger.debug("Performing plain-text password comparison for user: {}", upperCaseUsername);
            
            if (!upperCasePassword.equals(storedPassword)) {
                // Handle invalid password case matching COBOL error handling
                // Replicates error response from lines 241-246 in COSGN00C
                logger.warn("Authentication failed: Invalid password for user: {}", upperCaseUsername);
                throw new BadCredentialsException("Wrong Password. Try again ...");
            }
            
            logger.info("Authentication successful for user: {}", upperCaseUsername);
            
            // Create authenticated token with user details and authorities
            // Based on user type from UserDetails (SEC-USR-TYPE field)
            UsernamePasswordAuthenticationToken authenticatedToken = 
                new UsernamePasswordAuthenticationToken(
                    userDetails.getUsername(), 
                    userDetails.getPassword(), 
                    userDetails.getAuthorities()
                );
            
            // Set additional authentication details
            authenticatedToken.setDetails(authentication.getDetails());
            
            // Populate session attributes with user context
            // Replicates COMMAREA population from lines 224-228 in COSGN00C
            populateSessionAttributes(userDetails);
            
            logger.debug("Authentication token created successfully for user: {}", upperCaseUsername);
            
            return authenticatedToken;
            
        } catch (BadCredentialsException e) {
            // Re-throw BadCredentialsException to maintain proper error handling
            logger.error("Authentication failed with bad credentials: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Handle any unexpected errors during authentication
            logger.error("Unexpected error during authentication", e);
            throw new BadCredentialsException("Unable to verify the User ...");
        }
    }

    /**
     * Populates session attributes with user context information.
     * 
     * This method replicates the COMMAREA population logic from COSGN00C program
     * (lines 224-228), storing user context in Spring Session Redis for distributed
     * session management across all system components.
     * 
     * Session Attributes Set:
     * - SEC_USR_ID: User identifier from UserDetails
     * - SEC_USR_TYPE: User type for authorization decisions
     * - Additional context attributes for transaction processing
     * 
     * @param userDetails authenticated user details containing user information
     */
    private void populateSessionAttributes(UserDetails userDetails) {
        try {
            // Get current HTTP session from request context
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) {
                HttpSession session = attributes.getRequest().getSession();
                
                // Extract user type from UserDetails authorities
                // Map Spring Security authorities back to COBOL user type
                String userType = determineUserType(userDetails);
                
                // Set session attributes using SessionAttributes utility class
                // Replicates: MOVE WS-USER-ID TO CDEMO-USER-ID (line 226)
                // Replicates: MOVE SEC-USR-TYPE TO CDEMO-USER-TYPE (line 227)
                SessionAttributes.setUserAttribute(
                    session, 
                    userDetails.getUsername(), 
                    userType, 
                    userDetails.getUsername() // Use username as display name for now
                );
                
                logger.debug("Session attributes populated for user: {}, type: {}", 
                           userDetails.getUsername(), userType);
                
            } else {
                logger.warn("No HTTP session available for session attribute population");
            }
            
        } catch (Exception e) {
            logger.error("Error populating session attributes for user: {}", 
                        userDetails.getUsername(), e);
            // Don't fail authentication due to session attribute errors
        }
    }

    /**
     * Determines user type from Spring Security authorities.
     * 
     * Maps Spring Security granted authorities back to COBOL SEC-USR-TYPE values
     * for consistent authorization behavior across the application.
     * 
     * Authority Mapping:
     * - ROLE_ADMIN -> 'A' (Administrator)
     * - ROLE_USER -> 'U' (Regular User)
     * 
     * @param userDetails authenticated user details with granted authorities
     * @return user type character ('A' for Admin, 'U' for User)
     */
    private String determineUserType(UserDetails userDetails) {
        // Check authorities to determine user type
        // Maps Spring Security authorities to COBOL SEC-USR-TYPE values
        return userDetails.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())) ? "A" : "U";
    }

    /**
     * Indicates whether this AuthenticationProvider supports the indicated Authentication object.
     * 
     * This implementation supports UsernamePasswordAuthenticationToken which is the standard
     * authentication token for username/password authentication matching the COBOL authentication
     * pattern from COSGN00C program.
     * 
     * @param authentication the Class object for the Authentication implementation
     * @return true if this AuthenticationProvider supports the indicated Authentication object
     */
    @Override
    public boolean supports(Class<?> authentication) {
        // Support UsernamePasswordAuthenticationToken for username/password authentication
        boolean supported = UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
        
        logger.debug("Authentication provider supports class {}: {}", 
                    authentication.getSimpleName(), supported);
        
        return supported;
    }
}