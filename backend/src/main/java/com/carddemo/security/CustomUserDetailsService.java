package com.carddemo.security;

import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Spring Security UserDetailsService implementation for CardDemo application.
 * Provides user authentication details from PostgreSQL usrsec table, replacing 
 * legacy RACF authentication with modern Spring Security integration.
 * 
 * This service implements the authentication logic from COSGN00C COBOL program,
 * maintaining identical authentication behavior while leveraging Spring Security
 * framework capabilities. Handles case-insensitive authentication matching
 * COBOL logic and converts SEC-USR-TYPE values to Spring Security authorities.
 * 
 * Key features:
 * - Loads user authentication details from PostgreSQL user_security table
 * - Converts SEC-USR-TYPE 'A' to ROLE_ADMIN authority  
 * - Converts SEC-USR-TYPE 'U' to ROLE_USER authority
 * - Handles uppercase conversion for case-insensitive authentication
 * - Throws UsernameNotFoundException for invalid users
 * - Integrates with UserSecurityRepository for database access
 * 
 * Authentication Flow:
 * 1. Receives username from Spring Security authentication manager
 * 2. Converts username to uppercase matching COBOL logic
 * 3. Queries PostgreSQL usrsec table via UserSecurityRepository
 * 4. Returns UserDetails object with authorities based on user type
 * 5. Throws UsernameNotFoundException if user not found
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    /**
     * Loads user authentication details by username for Spring Security authentication.
     * 
     * This method implements the core authentication logic from COSGN00C COBOL program,
     * preserving the exact authentication behavior while leveraging Spring Security
     * framework integration. Performs case-insensitive username lookup matching
     * COBOL FUNCTION UPPER-CASE conversion and returns UserDetails object with
     * authorities based on SEC-USR-TYPE field.
     * 
     * Authentication Process:
     * 1. Convert username to uppercase matching COBOL logic (lines 132-134 in COSGN00C)
     * 2. Query PostgreSQL usrsec table using UserSecurityRepository (replaces CICS READ at lines 211-219)
     * 3. Return UserDetails object with granted authorities based on SEC-USR-TYPE
     * 4. Throw UsernameNotFoundException if user not found (matches line 249 error handling)
     * 
     * @param username the username identifying the user whose data is required
     * @return UserDetails object containing user authentication and authorization data
     * @throws UsernameNotFoundException if the user could not be found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user authentication details for username: {}", username);
        
        // Convert username to uppercase matching COBOL logic
        // Replicates: MOVE FUNCTION UPPER-CASE(USERIDI OF COSGN0AI) TO WS-USER-ID (lines 132-134)
        String upperCaseUsername = username.toUpperCase();
        
        logger.debug("Converted username to uppercase: {} -> {}", username, upperCaseUsername);
        
        // Query PostgreSQL usrsec table using repository
        // Replaces CICS READ operation from lines 211-219 in COSGN00C
        Optional<UserSecurity> userOptional = userSecurityRepository.findByUsername(upperCaseUsername);
        
        // Handle user not found case matching COBOL error handling
        // Replicates error response from lines 247-251 in COSGN00C
        if (!userOptional.isPresent()) {
            logger.warn("User not found for username: {}", upperCaseUsername);
            throw new UsernameNotFoundException("User not found. Try again ...");
        }
        
        UserSecurity userSecurity = userOptional.get();
        
        logger.debug("Successfully loaded user authentication details for user ID: {}, type: {}", 
                    userSecurity.getSecUsrId(), userSecurity.getUserType());
        
        // Return UserDetails object with authorities based on user type
        // UserSecurity entity implements UserDetails interface and provides:
        // - getUsername() - returns the username
        // - getPassword() - returns the password hash  
        // - getAuthorities() - returns authorities based on SEC-USR-TYPE
        // - isEnabled() - returns account enabled status
        // - isAccountNonExpired() - returns account expiration status
        return userSecurity;
    }
}