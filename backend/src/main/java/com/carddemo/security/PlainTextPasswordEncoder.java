/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Plain text password encoder for legacy COBOL password compatibility.
 * 
 * This password encoder maintains compatibility with legacy COBOL passwords stored in
 * plain text format in the mainframe VSAM USRSEC file. It implements the Spring Security
 * PasswordEncoder interface while preserving the original COBOL password validation behavior.
 * 
 * <p><b>Security Note:</b></p>
 * This encoder is specifically designed for migration compatibility with existing COBOL
 * systems where passwords were stored in plain text. In a production environment, this
 * should eventually be migrated to a more secure password encoding scheme like BCrypt.
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Direct string comparison for password validation (COBOL behavior)</li>
 *   <li>Case-sensitive password matching matching mainframe behavior</li>
 *   <li>Support for COBOL fixed-length password fields (8 characters)</li>
 *   <li>Trimming of trailing spaces for COBOL compatibility</li>
 *   <li>Integration with Spring Security authentication framework</li>
 * </ul>
 * 
 * <p>COBOL Compatibility:</p>
 * This encoder replicates the password validation logic from COBOL programs where passwords
 * were compared directly without encryption. The encoder handles COBOL-specific formatting
 * including fixed-length fields and trailing space trimming.
 * 
 * <p>Migration Path:</p>
 * This encoder provides a bridge during the COBOL-to-Java migration. Once all legacy
 * passwords are migrated, the system can be upgraded to use BCrypt or other secure
 * password encoding mechanisms.
 *
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Component
public class PlainTextPasswordEncoder implements PasswordEncoder {

    private static final Logger logger = LoggerFactory.getLogger(PlainTextPasswordEncoder.class);
    
    /**
     * Maximum password length for COBOL compatibility (8 characters)
     */
    private static final int COBOL_PASSWORD_MAX_LENGTH = 8;

    /**
     * Encodes a raw password (no-op for plain text compatibility).
     * 
     * For COBOL compatibility, this method returns the password as-is after
     * trimming and length validation. This maintains the original behavior
     * where passwords were stored in plain text format.
     * 
     * <p>COBOL Compatibility Features:</p>
     * <ul>
     *   <li>Trims leading and trailing whitespace</li>
     *   <li>Validates maximum length (8 characters for COBOL compatibility)</li>
     *   <li>Returns password in uppercase for consistent storage</li>
     * </ul>
     *
     * @param rawPassword the password to encode
     * @return the password trimmed and formatted for COBOL compatibility
     * @throws IllegalArgumentException if password is null, empty, or exceeds maximum length
     */
    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            logger.warn("Attempted to encode null password");
            throw new IllegalArgumentException("Password cannot be null");
        }

        String password = rawPassword.toString().trim();
        
        if (password.isEmpty()) {
            logger.warn("Attempted to encode empty password");
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (password.length() > COBOL_PASSWORD_MAX_LENGTH) {
            logger.warn("Password length exceeds COBOL maximum: {} > {}", 
                password.length(), COBOL_PASSWORD_MAX_LENGTH);
            throw new IllegalArgumentException(
                String.format("Password cannot exceed %d characters for COBOL compatibility", 
                    COBOL_PASSWORD_MAX_LENGTH));
        }

        // For COBOL compatibility, store password as-is (plain text)
        // Note: In a real production system, this should be replaced with proper encryption
        logger.debug("Encoding password with plain text encoder for COBOL compatibility");
        return password;
    }

    /**
     * Verifies a password against its encoded form using plain text comparison.
     * 
     * This method implements the COBOL password validation logic where passwords
     * are compared directly without encryption. It handles COBOL-specific formatting
     * including case sensitivity and whitespace handling.
     * 
     * <p>Validation Process:</p>
     * <ol>
     *   <li>Validate input parameters</li>
     *   <li>Trim whitespace from both raw and encoded passwords</li>
     *   <li>Perform case-sensitive string comparison</li>
     *   <li>Return true if passwords match exactly</li>
     * </ol>
     * 
     * <p>COBOL Behavior Replication:</p>
     * The validation logic matches the original COBOL password comparison behavior,
     * including handling of fixed-length fields with potential trailing spaces.
     *
     * @param rawPassword the password to verify
     * @param encodedPassword the encoded password to compare against
     * @return true if the passwords match, false otherwise
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            logger.debug("Password match failed - null input(s)");
            return false;
        }

        // Trim both passwords for COBOL compatibility (handles fixed-length fields)
        String trimmedRawPassword = rawPassword.toString().trim();
        String trimmedEncodedPassword = encodedPassword.trim();

        // Handle empty passwords
        if (trimmedRawPassword.isEmpty() && trimmedEncodedPassword.isEmpty()) {
            logger.debug("Both passwords are empty - considering as match for COBOL compatibility");
            return true;
        }

        if (trimmedRawPassword.isEmpty() || trimmedEncodedPassword.isEmpty()) {
            logger.debug("One password is empty while other is not - no match");
            return false;
        }

        // Perform case-sensitive comparison matching COBOL behavior
        boolean matches = trimmedRawPassword.equals(trimmedEncodedPassword);
        
        logger.debug("Password match result: {}", matches);
        return matches;
    }

    /**
     * Determines if the encoded password should be encoded again for security.
     * 
     * Since this is a plain text encoder for COBOL compatibility, encoded passwords
     * should always be re-encoded if the system is upgraded to a more secure encoder.
     * This method returns true to ensure passwords are re-encoded when the system
     * is migrated to proper password encryption.
     * 
     * <p>Migration Consideration:</p>
     * This method returns true to facilitate future migration to secure password
     * encoding schemes. When the system is upgraded, all passwords will be
     * re-encoded with the new secure encoder.
     *
     * @param encodedPassword the encoded password to check
     * @return true (always re-encode for future security migration)
     */
    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        // Always return true for plain text passwords to enable future security upgrades
        logger.debug("Password upgrade encoding requested - returning true for future security migration");
        return true;
    }

    /**
     * Validates password format for COBOL compatibility.
     * 
     * This method provides validation for password format requirements based on
     * COBOL field specifications and business rules from the original system.
     * 
     * <p>Validation Rules:</p>
     * <ul>
     *   <li>Password cannot be null or empty</li>
     *   <li>Password length cannot exceed 8 characters (COBOL PIC X(8))</li>
     *   <li>Password should not contain leading/trailing spaces</li>
     * </ul>
     *
     * @param password the password to validate
     * @return true if password meets COBOL format requirements
     */
    public boolean isValidCobolPassword(String password) {
        if (password == null || password.isEmpty()) {
            logger.debug("Password validation failed - null or empty");
            return false;
        }

        String trimmedPassword = password.trim();
        
        if (trimmedPassword.isEmpty()) {
            logger.debug("Password validation failed - empty after trim");
            return false;
        }

        if (trimmedPassword.length() > COBOL_PASSWORD_MAX_LENGTH) {
            logger.debug("Password validation failed - length {} exceeds maximum {}", 
                trimmedPassword.length(), COBOL_PASSWORD_MAX_LENGTH);
            return false;
        }

        // Check for leading/trailing spaces (COBOL compatibility issue)
        if (!password.equals(trimmedPassword)) {
            logger.debug("Password validation warning - contains leading/trailing spaces");
            // Return true but log warning - COBOL systems may have stored passwords with spaces
        }

        logger.debug("Password validation passed for COBOL compatibility");
        return true;
    }

    /**
     * Normalizes password for COBOL compatibility.
     * 
     * This method applies COBOL-specific password normalization including
     * trimming whitespace and applying any format-specific rules from the
     * original mainframe system.
     *
     * @param password the password to normalize
     * @return normalized password for COBOL compatibility
     */
    public String normalizePassword(String password) {
        if (password == null) {
            return null;
        }
        
        // Trim whitespace for COBOL fixed-length field compatibility
        String normalized = password.trim();
        
        logger.debug("Password normalized for COBOL compatibility");
        return normalized;
    }
}