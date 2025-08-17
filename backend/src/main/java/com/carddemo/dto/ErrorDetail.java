/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Data Transfer Object representing field-level error details for validation failures.
 * 
 * This DTO captures structured error information for individual form fields,
 * mapping to COBOL error handling patterns from BMS screens. Each error detail
 * contains the field name that failed validation, an error code for programmatic
 * handling, and a user-friendly message.
 * 
 * The error message length is constrained to 78 characters to match COBOL ERRMSG
 * field patterns used in mainframe BMS screen handling, ensuring compatibility
 * during the technology stack migration.
 * 
 * Usage Example:
 * <pre>
 * ErrorDetail error = new ErrorDetail();
 * error.setField("accountNumber");
 * error.setCode("INVALID_FORMAT");
 * error.setMessage("Account number must be 11 digits");
 * </pre>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Data
public class ErrorDetail {

    /**
     * Maximum length for error messages to match COBOL ERRMSG patterns.
     * COBOL BMS screens use 78-character error message fields.
     * This should match Constants.ERROR_MESSAGE_LENGTH when available.
     */
    private static final int ERROR_MESSAGE_LENGTH = 78;

    /**
     * The name of the field that failed validation.
     * 
     * This corresponds to the form field or data element that caused the validation
     * error, allowing the frontend to highlight the specific problematic field.
     * 
     * Examples: "accountNumber", "cardNumber", "userId", "transactionAmount"
     */
    @NotBlank(message = "Field name cannot be blank")
    @JsonProperty("field")
    private String field;

    /**
     * The error code for programmatic error handling.
     * 
     * Provides a standardized error code that applications can use for
     * programmatic error handling, logging, and error categorization.
     * Error codes should be consistent across the application.
     * 
     * Examples: "REQUIRED_FIELD", "INVALID_FORMAT", "VALUE_TOO_LONG", "DUPLICATE_VALUE"
     */
    @NotBlank(message = "Error code cannot be blank")
    @JsonProperty("code")
    private String code;

    /**
     * User-friendly error message describing the validation failure.
     * 
     * Contains a human-readable description of the validation error that can be
     * displayed directly to end users. Message length is limited to 78 characters
     * to maintain compatibility with COBOL ERRMSG field patterns from BMS screens.
     * 
     * The message should be clear, actionable, and appropriate for display to
     * users in the modernized React frontend while maintaining the concise
     * nature of original COBOL error messages.
     * 
     * Examples: "Account number must be exactly 11 digits",
     *           "Card number format is invalid", "User ID is required"
     */
    @NotBlank(message = "Error message cannot be blank")
    @Size(max = ERROR_MESSAGE_LENGTH, message = "Error message cannot exceed " + ERROR_MESSAGE_LENGTH + " characters (COBOL ERRMSG pattern)")
    @JsonProperty("message")
    private String message;
}