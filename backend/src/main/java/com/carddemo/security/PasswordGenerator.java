/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Utility class for generating secure initial passwords for new users.
 * 
 * Implements password generation logic following enterprise security policies 
 * while maintaining 8-character length constraint from COBOL SEC-USR-PWD field definition.
 * 
 * This class supports the migration from COBOL/CICS to Spring Boot by providing
 * equivalent password generation functionality with configurable security policies.
 * 
 * Key Features:
 * - Generates cryptographically secure 8-character passwords
 * - Supports policy-based character set configuration
 * - Implements password strength validation
 * - Maintains compatibility with existing COBOL password field constraints
 * 
 * @author Blitzy Platform Migration Agent
 * @version 1.0
 * @since Java 21
 */
@Slf4j
@Component
public class PasswordGenerator {
    
    // Character sets for password generation
    private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMERIC_CHARS = "0123456789";
    private static final String DEFAULT_CHARSET = UPPERCASE_CHARS + LOWERCASE_CHARS + NUMERIC_CHARS;
    
    // Password policy constants
    private static final int PASSWORD_LENGTH = 8; // Must match COBOL SEC-USR-PWD PIC X(08)
    private static final int MIN_UPPERCASE = 1;
    private static final int MIN_LOWERCASE = 1;
    private static final int MIN_NUMERIC = 1;
    
    // Cryptographically secure random number generator
    private final SecureRandom secureRandom;
    
    /**
     * Constructs a new PasswordGenerator with a cryptographically secure random number generator.
     * 
     * Uses SecureRandom to ensure unpredictable password generation suitable for 
     * enterprise security requirements and regulatory compliance.
     */
    public PasswordGenerator() {
        this.secureRandom = new SecureRandom();
        log.info("PasswordGenerator initialized with SecureRandom for cryptographically secure password generation");
    }
    
    /**
     * Generates a secure 8-character password with default policy requirements.
     * 
     * Default policy enforces:
     * - Exactly 8 characters (COBOL SEC-USR-PWD constraint)
     * - At least 1 uppercase letter
     * - At least 1 lowercase letter  
     * - At least 1 numeric digit
     * - Cryptographically secure random generation
     * 
     * @return A secure 8-character password meeting default policy requirements
     */
    public String generateSecurePassword() {
        log.debug("Generating secure password with default policy");
        
        return generatePasswordWithPolicy("default");
    }
    
    /**
     * Generates a password following the specified security policy.
     * 
     * Supported policies:
     * - "default": Standard enterprise policy (1 upper, 1 lower, 1 numeric)
     * - "strong": Enhanced security policy with additional complexity
     * - "simple": Basic policy for testing/development environments
     * 
     * All policies maintain the 8-character length requirement from COBOL field definition.
     * 
     * @param policyName The name of the security policy to apply
     * @return A secure password conforming to the specified policy
     * @throws IllegalArgumentException if the policy name is null, empty, or unknown
     */
    public String generatePasswordWithPolicy(String policyName) {
        if (policyName == null || policyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Policy name cannot be null or empty");
        }
        
        log.debug("Generating password with policy: {}", policyName);
        
        String policy = policyName.toLowerCase().trim();
        
        switch (policy) {
            case "default":
                return generateDefaultPolicy();
            case "strong":
                return generateStrongPolicy();
            case "simple":
                return generateSimplePolicy();
            default:
                log.warn("Unknown policy '{}', falling back to default policy", policyName);
                return generateDefaultPolicy();
        }
    }
    
    /**
     * Validates the strength of a password against security requirements.
     * 
     * Checks the provided password against enterprise security policy requirements:
     * - Exactly 8 characters in length
     * - Contains at least 1 uppercase letter
     * - Contains at least 1 lowercase letter
     * - Contains at least 1 numeric digit
     * - No common patterns or sequential characters
     * 
     * @param password The password to validate for strength
     * @return true if the password meets all strength requirements, false otherwise
     */
    public boolean validatePasswordStrength(String password) {
        if (password == null) {
            log.warn("Password validation failed: password is null");
            return false;
        }
        
        log.debug("Validating password strength for password of length: {}", password.length());
        
        // Check length constraint (COBOL SEC-USR-PWD PIC X(08))
        if (password.length() != PASSWORD_LENGTH) {
            log.debug("Password validation failed: length {} != required {}", password.length(), PASSWORD_LENGTH);
            return false;
        }
        
        // Check character composition requirements
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasNumeric = password.chars().anyMatch(Character::isDigit);
        
        if (!hasUppercase) {
            log.debug("Password validation failed: no uppercase characters");
            return false;
        }
        
        if (!hasLowercase) {
            log.debug("Password validation failed: no lowercase characters");
            return false;
        }
        
        if (!hasNumeric) {
            log.debug("Password validation failed: no numeric characters");
            return false;
        }
        
        // Check for common weak patterns
        if (hasSequentialCharacters(password)) {
            log.debug("Password validation failed: contains sequential characters");
            return false;
        }
        
        if (hasRepeatingCharacters(password)) {
            log.debug("Password validation failed: contains too many repeating characters");
            return false;
        }
        
        log.debug("Password validation passed");
        return true;
    }
    
    /**
     * Validates if a password is acceptable according to current security policies.
     * 
     * Performs comprehensive validation including:
     * - Strength requirements validation
     * - Policy compliance checking
     * - Security pattern analysis
     * - Enterprise security standard conformance
     * 
     * @param password The password to validate
     * @return true if the password is valid and acceptable, false otherwise
     */
    public boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            log.warn("Password validation failed: password is null or empty");
            return false;
        }
        
        log.debug("Validating password acceptability");
        
        // Primary validation through strength checking
        if (!validatePasswordStrength(password)) {
            return false;
        }
        
        // Additional policy validations
        if (containsInvalidCharacters(password)) {
            log.debug("Password validation failed: contains invalid characters");
            return false;
        }
        
        if (isCommonPassword(password)) {
            log.debug("Password validation failed: password is too common");
            return false;
        }
        
        log.debug("Password is valid and acceptable");
        return true;
    }
    
    /**
     * Generates a password following the default enterprise security policy.
     * 
     * Ensures the generated password contains:
     * - At least 1 uppercase letter
     * - At least 1 lowercase letter
     * - At least 1 numeric digit
     * - Exactly 8 characters total
     * - Cryptographically secure randomization
     * 
     * @return An 8-character password meeting default policy requirements
     */
    private String generateDefaultPolicy() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        
        // Ensure minimum character type requirements
        password.append(getRandomChar(UPPERCASE_CHARS));
        password.append(getRandomChar(LOWERCASE_CHARS));
        password.append(getRandomChar(NUMERIC_CHARS));
        
        // Fill remaining positions with random characters from full charset
        for (int i = 3; i < PASSWORD_LENGTH; i++) {
            password.append(getRandomChar(DEFAULT_CHARSET));
        }
        
        // Shuffle the password to randomize character positions
        return shufflePassword(password.toString());
    }
    
    /**
     * Generates a password following the strong security policy.
     * 
     * Enhanced security requirements:
     * - At least 2 uppercase letters
     * - At least 2 lowercase letters
     * - At least 2 numeric digits
     * - Remaining characters from mixed charset
     * - No sequential or repeating patterns
     * 
     * @return An 8-character password meeting strong policy requirements
     */
    private String generateStrongPolicy() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        
        // Enhanced character distribution for stronger security
        password.append(getRandomChar(UPPERCASE_CHARS));
        password.append(getRandomChar(UPPERCASE_CHARS));
        password.append(getRandomChar(LOWERCASE_CHARS));
        password.append(getRandomChar(LOWERCASE_CHARS));
        password.append(getRandomChar(NUMERIC_CHARS));
        password.append(getRandomChar(NUMERIC_CHARS));
        
        // Fill remaining positions
        for (int i = 6; i < PASSWORD_LENGTH; i++) {
            password.append(getRandomChar(DEFAULT_CHARSET));
        }
        
        String result = shufflePassword(password.toString());
        
        // Validate against strong policy requirements
        if (hasSequentialCharacters(result) || hasRepeatingCharacters(result)) {
            // Regenerate if patterns detected
            return generateStrongPolicy();
        }
        
        return result;
    }
    
    /**
     * Generates a password following the simple security policy.
     * 
     * Simplified requirements for development/testing:
     * - At least 1 uppercase letter
     * - At least 1 lowercase letter
     * - At least 1 numeric digit
     * - Allows more predictable patterns for testing
     * 
     * @return An 8-character password meeting simple policy requirements
     */
    private String generateSimplePolicy() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        
        // Basic character requirements
        password.append(getRandomChar(UPPERCASE_CHARS));
        password.append(getRandomChar(LOWERCASE_CHARS));
        password.append(getRandomChar(NUMERIC_CHARS));
        
        // Fill remaining positions with alphanumeric characters
        String simpleCharset = UPPERCASE_CHARS + LOWERCASE_CHARS + NUMERIC_CHARS;
        for (int i = 3; i < PASSWORD_LENGTH; i++) {
            password.append(getRandomChar(simpleCharset));
        }
        
        return shufflePassword(password.toString());
    }
    
    /**
     * Retrieves a random character from the specified character set.
     * 
     * Uses SecureRandom.nextInt(int) for cryptographically secure character selection.
     * 
     * @param charset The character set to select from
     * @return A randomly selected character from the charset
     */
    private char getRandomChar(String charset) {
        int index = secureRandom.nextInt(charset.length());
        return charset.charAt(index);
    }
    
    /**
     * Shuffles the characters in a password to randomize their positions.
     * 
     * Uses Fisher-Yates shuffle algorithm with SecureRandom for 
     * cryptographically secure position randomization.
     * 
     * @param password The password string to shuffle
     * @return The password with characters in randomized positions
     */
    private String shufflePassword(String password) {
        char[] chars = password.toCharArray();
        
        // Fisher-Yates shuffle algorithm
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            // Swap characters
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        
        return new String(chars);
    }
    
    /**
     * Checks if the password contains sequential characters.
     * 
     * Detects patterns like "abc", "123", "xyz" which reduce security.
     * 
     * @param password The password to check
     * @return true if sequential characters are found, false otherwise
     */
    private boolean hasSequentialCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char first = password.charAt(i);
            char second = password.charAt(i + 1);
            char third = password.charAt(i + 2);
            
            // Check for ascending sequence
            if (second == first + 1 && third == second + 1) {
                return true;
            }
            
            // Check for descending sequence
            if (second == first - 1 && third == second - 1) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if the password has excessive repeating characters.
     * 
     * Detects patterns with more than 2 consecutive identical characters.
     * 
     * @param password The password to check
     * @return true if excessive repeating characters are found, false otherwise
     */
    private boolean hasRepeatingCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char current = password.charAt(i);
            if (password.charAt(i + 1) == current && password.charAt(i + 2) == current) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if the password contains invalid characters.
     * 
     * Validates that the password only contains characters from the allowed charset
     * (uppercase letters, lowercase letters, and numeric digits).
     * 
     * @param password The password to check
     * @return true if invalid characters are found, false otherwise
     */
    private boolean containsInvalidCharacters(String password) {
        for (char c : password.toCharArray()) {
            if (DEFAULT_CHARSET.indexOf(c) == -1) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if the password is a commonly used weak password.
     * 
     * Validates against a list of common weak passwords that should be rejected
     * for security purposes.
     * 
     * @param password The password to check
     * @return true if the password is commonly used, false otherwise
     */
    private boolean isCommonPassword(String password) {
        // List of common weak 8-character passwords to reject
        String[] commonPasswords = {
            "12345678",
            "password",
            "qwerty12",
            "abcdef12",
            "Password",
            "Qwerty12",
            "Admin123",
            "User1234"
        };
        
        String lowerPassword = password.toLowerCase();
        for (String common : commonPasswords) {
            if (lowerPassword.equals(common.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}