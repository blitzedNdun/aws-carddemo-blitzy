/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for user sign-on authentication requests.
 * 
 * This DTO contains user credentials required for authentication processing,
 * including user ID and password fields. Designed to support the translation
 * from COBOL COSGN00C.cbl authentication logic to Spring Boot REST architecture.
 * 
 * Field validation rules preserve compatibility with original COBOL field
 * validation patterns and CICS transaction processing requirements during 
 * the migration to modern Spring Security authentication.
 */
public class SignOnRequest {

    /**
     * User ID for authentication.
     * Must be a valid alphanumeric user identifier matching COBOL USRSEC file format.
     */
    @NotNull(message = "User ID is required")
    @NotBlank(message = "User ID cannot be blank")
    @Size(min = 1, max = 8, message = "User ID must be between 1 and 8 characters")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "User ID must contain only alphanumeric characters")
    @JsonProperty("user_id")
    private String userId;

    /**
     * Password for authentication.
     * Must be a valid password matching COBOL password field requirements.
     */
    @NotNull(message = "Password is required")
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 1, max = 8, message = "Password must be between 1 and 8 characters")
    @JsonProperty("password")
    private String password;

    /**
     * Default constructor for JSON deserialization.
     */
    public SignOnRequest() {
    }

    /**
     * Constructor with user credentials.
     * 
     * @param userId User ID for authentication
     * @param password Password for authentication
     */
    public SignOnRequest(String userId, String password) {
        this.userId = userId;
        this.password = password;
    }

    /**
     * Gets the user ID.
     * 
     * @return User ID string
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID.
     * 
     * @param userId User ID to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the password.
     * 
     * @return Password string
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     * 
     * @param password Password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * String representation for debugging (excludes password for security).
     * 
     * @return String representation of the sign-on request
     */
    @Override
    public String toString() {
        return "SignOnRequest{" +
                "userId='" + userId + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }

    /**
     * Validates if the request contains required credentials.
     * 
     * @return true if both userId and password are present and not blank
     */
    public boolean isValid() {
        return userId != null && !userId.trim().isEmpty() &&
               password != null && !password.trim().isEmpty();
    }

    /**
     * Creates a copy of this request with the password masked for logging.
     * 
     * @return SignOnRequest with masked password for safe logging
     */
    public SignOnRequest createLoggableCopy() {
        SignOnRequest copy = new SignOnRequest();
        copy.setUserId(this.userId);
        copy.setPassword("[PROTECTED]");
        return copy;
    }
}