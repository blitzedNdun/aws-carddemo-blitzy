package com.carddemo.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Spring Security component for enforcing password policy rules equivalent to mainframe RACF requirements.
 * Validates password strength, complexity, and history constraints during user update operations.
 * Integrates with Spring Security for consistent password management.
 * 
 * This class replaces RACF password policy enforcement while maintaining identical security standards
 * and validation patterns from the original COBOL implementation in COUSR02C.
 */
@Component
public class PasswordPolicyValidator {

    // RACF-equivalent password policy constants
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 8; // Maintaining COBOL field length compatibility
    private static final int PASSWORD_HISTORY_COUNT = 5; // Track last 5 passwords
    
    // Pattern definitions for password complexity requirements
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
    
    // Common passwords and dictionary words to prohibit (RACF-style restrictions)
    private static final List<String> PROHIBITED_PASSWORDS = Arrays.asList(
        "PASSWORD", "PASS", "ADMIN", "USER", "TEST", "TEMP", "CHANGEME",
        "WELCOME", "SYSTEM", "DEFAULT", "GUEST", "PUBLIC", "SECRET",
        "12345678", "87654321", "QWERTY", "ABCDEFGH"
    );

    /**
     * Main password validation entry point.
     * Validates a password against all policy rules including strength, complexity, and history.
     * Throws appropriate exceptions for policy violations matching COBOL error handling patterns.
     * 
     * @param password The password to validate
     * @param userId The user ID for history checking and personalized validation
     * @param passwordHistory List of previous password hashes for history validation
     * @throws PasswordPolicyException if password violates any policy rules
     */
    public void validatePassword(String password, String userId, List<String> passwordHistory) {
        if (password == null || password.trim().isEmpty()) {
            throw new PasswordPolicyException("Password can NOT be empty...", "EMPTY_PASSWORD");
        }

        // Preserve original password for strength checking
        String trimmedPassword = password.trim();

        // Validate basic password requirements
        validatePasswordLength(trimmedPassword);
        
        // Check password strength and complexity (with original case)
        checkPasswordStrength(trimmedPassword);
        
        // Enforce complexity rules (user ID will be normalized internally)
        enforceComplexityRules(trimmedPassword, userId);
        
        // Validate password history constraints (case-insensitive comparison)
        if (passwordHistory != null && !passwordHistory.isEmpty()) {
            if (!isPasswordHistoryValid(trimmedPassword, passwordHistory)) {
                throw new PasswordPolicyException(
                    "Password has been used recently. Please choose a different password.",
                    "PASSWORD_REUSE_VIOLATION"
                );
            }
        }
    }

    /**
     * Checks password strength requirements including length and basic character validation.
     * Implements RACF-equivalent strength checking with enterprise security standards.
     * 
     * @param password The password to check (preserving original case)
     * @throws PasswordPolicyException if password does not meet strength requirements
     */
    public void checkPasswordStrength(String password) {
        List<String> strengthIssues = new ArrayList<>();

        // Check minimum character requirements
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            strengthIssues.add("at least one uppercase letter");
        }
        
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            strengthIssues.add("at least one lowercase letter");
        }
        
        if (!DIGIT_PATTERN.matcher(password).find()) {
            strengthIssues.add("at least one number");
        }

        // Check for prohibited common passwords (case-insensitive comparison)
        if (PROHIBITED_PASSWORDS.contains(password.toUpperCase())) {
            throw new PasswordPolicyException(
                "Password is too common and not allowed. Please choose a more secure password.",
                "COMMON_PASSWORD_VIOLATION"
            );
        }

        // Check for consecutive characters or repeated patterns
        if (hasConsecutiveCharacters(password) || hasRepeatedCharacters(password)) {
            strengthIssues.add("no consecutive or repeated characters");
        }

        if (!strengthIssues.isEmpty()) {
            String message = "Password must contain " + String.join(", ", strengthIssues) + ".";
            throw new PasswordPolicyException(message, "PASSWORD_STRENGTH_VIOLATION");
        }
    }

    /**
     * Validates password against history to prevent reuse.
     * Implements RACF-equivalent password history checking to enforce password rotation policies.
     * 
     * @param newPassword The new password to validate
     * @param passwordHistory List of previous password hashes
     * @return true if password is not in history, false if it violates history rules
     */
    public boolean isPasswordHistoryValid(String newPassword, List<String> passwordHistory) {
        if (passwordHistory == null || passwordHistory.isEmpty()) {
            return true; // No history to check against
        }

        // Check against the last N passwords in history (case-insensitive comparison)
        int historyLimit = Math.min(passwordHistory.size(), PASSWORD_HISTORY_COUNT);
        String normalizedNewPassword = newPassword.toUpperCase();
        
        for (int i = 0; i < historyLimit; i++) {
            String historicalPassword = passwordHistory.get(i);
            if (historicalPassword != null && historicalPassword.toUpperCase().equals(normalizedNewPassword)) {
                return false; // Password found in history
            }
        }
        
        return true; // Password not found in recent history
    }

    /**
     * Enforces comprehensive password complexity rules.
     * Implements RACF-equivalent complexity requirements with enterprise security standards.
     * 
     * @param password The password to validate (preserving original case)
     * @param userId The user ID to check for password similarity (will be normalized internally)
     * @throws PasswordPolicyException if password violates complexity rules
     */
    public void enforceComplexityRules(String password, String userId) {
        List<String> complexityViolations = new ArrayList<>();

        // Normalize user ID for case-insensitive comparison
        String normalizedUserId = userId != null ? userId.toUpperCase().trim() : "";

        // Password cannot be the same as user ID (case-insensitive comparison)
        if (!normalizedUserId.isEmpty() && password.toUpperCase().equals(normalizedUserId)) {
            throw new PasswordPolicyException(
                "Password cannot be the same as User ID.",
                "PASSWORD_USERID_MATCH"
            );
        }

        // Password cannot contain user ID as substring (case-insensitive comparison)
        if (!normalizedUserId.isEmpty() && normalizedUserId.length() >= 3 && 
            password.toUpperCase().contains(normalizedUserId)) {
            complexityViolations.add("cannot contain User ID");
        }

        // Check for minimum character variety (at least 3 different character types)
        int characterTypes = 0;
        if (UPPERCASE_PATTERN.matcher(password).find()) characterTypes++;
        if (LOWERCASE_PATTERN.matcher(password).find()) characterTypes++;
        if (DIGIT_PATTERN.matcher(password).find()) characterTypes++;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) characterTypes++;

        if (characterTypes < 3) {
            complexityViolations.add("must contain at least 3 different character types (uppercase, lowercase, numbers, special characters)");
        }

        // Check for alphabetical or numerical sequences (case-insensitive)
        if (hasAlphabeticalSequence(password.toUpperCase()) || hasNumericalSequence(password)) {
            complexityViolations.add("cannot contain sequential characters (ABC, 123, etc.)");
        }

        if (!complexityViolations.isEmpty()) {
            String message = "Password complexity violation: " + String.join(", ", complexityViolations) + ".";
            throw new PasswordPolicyException(message, "PASSWORD_COMPLEXITY_VIOLATION");
        }
    }

    /**
     * Validates password length requirements.
     * Maintains COBOL field length compatibility while enforcing minimum security standards.
     * 
     * @param password The password to validate
     * @throws PasswordPolicyException if password length is invalid
     */
    private void validatePasswordLength(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new PasswordPolicyException(
                "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.",
                "PASSWORD_TOO_SHORT"
            );
        }
        
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new PasswordPolicyException(
                "Password cannot be longer than " + MAX_PASSWORD_LENGTH + " characters.",
                "PASSWORD_TOO_LONG"
            );
        }
    }

    /**
     * Checks for consecutive characters in password (e.g., ABC, XYZ).
     * 
     * @param password The password to check
     * @return true if consecutive characters found, false otherwise
     */
    private boolean hasConsecutiveCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);
            
            if ((c2 == c1 + 1) && (c3 == c2 + 1)) {
                return true; // Found ascending consecutive characters
            }
            if ((c2 == c1 - 1) && (c3 == c2 - 1)) {
                return true; // Found descending consecutive characters
            }
        }
        return false;
    }

    /**
     * Checks for repeated characters in password (e.g., AAA, 111).
     * 
     * @param password The password to check
     * @return true if repeated characters found, false otherwise
     */
    private boolean hasRepeatedCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);
            
            if (c1 == c2 && c2 == c3) {
                return true; // Found three consecutive identical characters
            }
        }
        return false;
    }

    /**
     * Checks for alphabetical sequences in password (e.g., ABC, XYZ).
     * 
     * @param password The password to check
     * @return true if alphabetical sequence found, false otherwise
     */
    private boolean hasAlphabeticalSequence(String password) {
        String[] sequences = {"ABCD", "BCDE", "CDEF", "DEFG", "EFGH", "FGHI", "GHIJ", "HIJK", 
                             "IJKL", "JKLM", "KLMN", "LMNO", "MNOP", "NOPQ", "OPQR", "PQRS", 
                             "QRST", "RSTU", "STUV", "TUVW", "UVWX", "VWXY", "WXYZ"};
        
        for (String sequence : sequences) {
            if (password.contains(sequence) || password.contains(new StringBuilder(sequence).reverse().toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks for numerical sequences in password (e.g., 123, 456).
     * 
     * @param password The password to check
     * @return true if numerical sequence found, false otherwise
     */
    private boolean hasNumericalSequence(String password) {
        String[] sequences = {"0123", "1234", "2345", "3456", "4567", "5678", "6789"};
        
        for (String sequence : sequences) {
            if (password.contains(sequence) || password.contains(new StringBuilder(sequence).reverse().toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Custom exception class for password policy violations.
     * Provides detailed error messages and error codes for consistent error handling
     * matching COBOL program error patterns from COUSR02C.
     */
    public static class PasswordPolicyException extends RuntimeException {
        private final String errorCode;

        public PasswordPolicyException(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }

        @Override
        public String toString() {
            return "PasswordPolicyException{" +
                   "message='" + getMessage() + '\'' +
                   ", errorCode='" + errorCode + '\'' +
                   '}';
        }
    }
}