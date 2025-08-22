/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;

/**
 * Custom password encoder that maintains compatibility with COBOL RACF passwords.
 * 
 * This password encoder implements legacy password handling for COBOL-to-Java migration,
 * providing 100% compatibility with existing RACF password storage while supporting
 * modern Spring Security password encoding interfaces.
 * 
 * Key Features:
 * - NoOpPasswordEncoder compatibility for existing RACF passwords during migration
 * - COBOL FUNCTION UPPER-CASE equivalent conversion behavior
 * - 8-character fixed-length field constraints from CSUSR01Y.cpy (PIC X(08))
 * - Special character restrictions from mainframe password policies
 * - Password normalization, padding, and trimming for COBOL fields
 * - Backward compatibility with VSAM USRSEC dataset passwords
 * 
 * Migration Path:
 * This encoder is designed as a temporary bridge during the COBOL-to-Java migration.
 * Future versions will support BCrypt encoding while maintaining backward compatibility
 * through the upgradeEncoding() method.
 * 
 * Based on COSGN00C.cbl authentication logic:
 * - FUNCTION UPPER-CASE conversion for user ID and password (lines 132-136)
 * - Direct string comparison: IF SEC-USR-PWD = WS-USER-PWD (line 223)  
 * - Fixed 8-character password field from CSUSR01Y.cpy (PIC X(08))
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Component
public class LegacyPasswordEncoder implements PasswordEncoder {

    private static final Logger logger = LoggerFactory.getLogger(LegacyPasswordEncoder.class);

    /**
     * Maximum password length matching COBOL PIC X(08) field definition.
     */
    private static final int MAX_PASSWORD_LENGTH = 8;

    /**
     * Pattern for valid password characters (alphanumeric, underscore, hyphen).
     * Based on mainframe password policy restrictions.
     */
    private static final Pattern VALID_PASSWORD_PATTERN = Pattern.compile("^[A-Z0-9\\-_]*$");

    /**
     * Pattern for invalid special characters that should be rejected.
     */
    private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile("[^A-Z0-9\\-_\\s]");

    /**
     * Encodes the given raw password using COBOL-compatible logic.
     * 
     * This method performs the following operations:
     * 1. Validates the password is not null
     * 2. Normalizes the password (trim + uppercase conversion)
     * 3. Validates password format and length constraints
     * 4. Returns the normalized password (plain-text for RACF compatibility)
     * 
     * @param rawPassword the raw password to encode
     * @return the encoded password (uppercase, trimmed, validated)
     * @throws IllegalArgumentException if password is null, too long, or contains invalid characters
     */
    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        String password = rawPassword.toString();
        
        // Handle empty password case
        if (password.isEmpty()) {
            return "";
        }

        // Normalize the password (trim and convert to uppercase)
        String normalizedPassword = normalizePassword(password);
        
        // Validate password format and constraints
        if (!validatePasswordFormat(normalizedPassword)) {
            if (normalizedPassword.length() > MAX_PASSWORD_LENGTH) {
                throw new IllegalArgumentException("Password exceeds maximum length of " + MAX_PASSWORD_LENGTH + " characters");
            }
            if (INVALID_CHARS_PATTERN.matcher(normalizedPassword).find()) {
                throw new IllegalArgumentException("Password contains invalid characters. Only alphanumeric, underscore, and hyphen are allowed.");
            }
        }

        logger.debug("Password encoded successfully with length: {}", normalizedPassword.length());
        return normalizedPassword;
    }

    /**
     * Verifies that the raw password matches the encoded password.
     * 
     * This method implements the COBOL password comparison logic:
     * 1. Normalizes the raw password using the same logic as encode()
     * 2. Performs direct string comparison (IF SEC-USR-PWD = WS-USER-PWD equivalent)
     * 3. Returns true if passwords match exactly after normalization
     * 
     * @param rawPassword the raw password to check
     * @param encodedPassword the encoded password to compare against
     * @return true if passwords match, false otherwise
     * @throws IllegalArgumentException if either password is null
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Raw password cannot be null");
        }
        if (encodedPassword == null) {
            throw new IllegalArgumentException("Encoded password cannot be null");
        }

        // Handle empty password cases
        String rawPasswordStr = rawPassword.toString();
        if (rawPasswordStr.isEmpty() && encodedPassword.isEmpty()) {
            return true;
        }
        if (rawPasswordStr.isEmpty() || encodedPassword.isEmpty()) {
            return false;
        }

        // Normalize the raw password and compare directly
        String normalizedRawPassword = normalizePassword(rawPasswordStr);
        boolean matches = normalizedRawPassword.equals(encodedPassword);
        
        logger.debug("Password match attempt for raw password length {}: {}", 
                    rawPasswordStr.length(), matches ? "SUCCESS" : "FAILURE");
        
        return matches;
    }

    /**
     * Indicates whether encoded passwords should be upgraded to a newer encoding.
     * 
     * For the legacy password encoder, this always returns false since we're maintaining
     * RACF compatibility. In future versions, this may return true to trigger migration
     * to BCrypt encoding.
     * 
     * @param encodedPassword the encoded password to check
     * @return false (no upgrade needed for legacy passwords)
     * @throws IllegalArgumentException if encodedPassword is null
     */
    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        if (encodedPassword == null) {
            throw new IllegalArgumentException("Encoded password cannot be null");
        }
        
        // Legacy passwords don't need upgrading - they're already in the correct format
        return false;
    }

    /**
     * Normalizes a password according to COBOL field processing rules.
     * 
     * This method applies the following transformations:
     * 1. Trims leading and trailing whitespace
     * 2. Converts to uppercase (COBOL FUNCTION UPPER-CASE equivalent)
     * 
     * @param password the password to normalize
     * @return the normalized password
     * @throws IllegalArgumentException if password is null
     */
    public String normalizePassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        // Trim whitespace and convert to uppercase (COBOL FUNCTION UPPER-CASE)
        return password.trim().toUpperCase();
    }

    /**
     * Validates that a password meets the format requirements.
     * 
     * Validation rules:
     * 1. Length must not exceed 8 characters (COBOL PIC X(08) constraint)
     * 2. Must contain only valid characters (alphanumeric, underscore, hyphen)
     * 3. Empty passwords are considered valid for validation purposes
     * 
     * @param password the password to validate
     * @return true if password format is valid, false otherwise
     */
    public boolean validatePasswordFormat(String password) {
        if (password == null) {
            return false;
        }

        // Empty passwords are valid for format validation
        if (password.isEmpty()) {
            return true;
        }

        // Check length constraint
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return false;
        }

        // Check for invalid characters
        if (INVALID_CHARS_PATTERN.matcher(password).find()) {
            return false;
        }

        // Check that it matches the valid pattern
        return VALID_PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Determines if a password is in the legacy RACF format.
     * 
     * Legacy format criteria:
     * 1. Uppercase characters only (indicating prior normalization)
     * 2. Length of 8 characters or less
     * 3. Contains only valid mainframe password characters
     * 
     * @param password the password to check
     * @return true if password is in legacy format, false otherwise
     * @throws IllegalArgumentException if password is null
     */
    public boolean isLegacyFormat(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null for format detection");
        }

        // Empty passwords are considered legacy format
        if (password.isEmpty()) {
            return true;
        }

        // Check if password meets legacy format criteria
        return password.length() <= MAX_PASSWORD_LENGTH &&
               password.equals(password.toUpperCase()) &&
               VALID_PASSWORD_PATTERN.matcher(password).matches();
    }
}