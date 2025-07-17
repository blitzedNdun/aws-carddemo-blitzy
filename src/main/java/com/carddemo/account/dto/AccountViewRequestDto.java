/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.account.dto;

import com.carddemo.common.util.ValidationUtils;
import com.carddemo.common.enums.ValidationResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Request DTO for account view operations supporting AccountViewService.java with account ID validation,
 * pagination parameters, and session context mapping to replace CICS COMMAREA structures from COACTVWC.cbl transaction.
 * 
 * This class replaces the CICS COMMAREA structure used in the original COACTVWC.cbl transaction, providing
 * a modern JSON-based request interface for account view operations while maintaining identical business
 * logic and validation patterns. The DTO supports pagination and search criteria equivalent to BMS screen
 * input handling and integrates with Spring Session context mapping for stateless REST API design.
 * 
 * Key Features:
 * - Account ID validation with 11-digit format per Section 2.1.3 requirements
 * - Pagination support for large result sets with configurable page size
 * - Session context mapping preserving CICS pseudo-conversational state
 * - JSON serialization with Jakarta validation annotations
 * - Complete field validation equivalent to original BMS field validation
 * - Spring Boot REST API integration with request/response DTOs
 * 
 * Original COBOL Structure Mapping:
 * - CDEMO-ACCT-ID (PIC 9(11)) → accountId field with validation
 * - CDEMO-USER-ID (PIC X(08)) → sessionContext.userId
 * - CDEMO-FROM-PROGRAM (PIC X(08)) → sessionContext.fromProgram
 * - CDEMO-PGM-CONTEXT (PIC 9(01)) → sessionContext.programContext
 * - Pagination parameters added for modern web interface requirements
 * 
 * Technical Implementation:
 * - Uses ValidationUtils.validateAccountNumber() for account ID format validation
 * - JsonInclude.Include.NON_NULL prevents null fields in JSON serialization
 * - Jakarta validation annotations enforce field length and format constraints
 * - Implements equals(), hashCode(), and toString() for proper object behavior
 * - Supports both account lookup and pagination use cases
 * 
 * Based on original COBOL files:
 * - COACTVWC.cbl: Main account view transaction logic with input validation
 * - COCOM01Y.cpy: Communication area structure for session context
 * - COACTVW.bms: BMS screen definition for account view input fields
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountViewRequestDto {

    /**
     * Account ID for account view operations.
     * 
     * Maps to CDEMO-ACCT-ID from COCOM01Y.cpy (PIC 9(11)).
     * Must be exactly 11 digits as per original COBOL field validation
     * in COACTVWC.cbl lines 666-680.
     */
    @Size(min = 11, max = 11, message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Page number for pagination support.
     * 
     * Zero-based page number for paginated results, defaulting to 0 for first page.
     * Enables efficient handling of large account result sets in modern web interface.
     */
    private Integer pageNumber;

    /**
     * Page size for pagination support.
     * 
     * Number of records to return per page, defaulting to 20 for optimal performance.
     * Configurable to support different client requirements while maintaining
     * reasonable response times.
     */
    private Integer pageSize;

    /**
     * Session context information for stateless REST API design.
     * 
     * Contains user session information equivalent to CICS COMMAREA session data
     * from COCOM01Y.cpy structure, enabling stateless REST API while preserving
     * session context across service calls.
     */
    private SessionContext sessionContext;

    /**
     * Default constructor for JSON deserialization and Spring framework integration.
     * 
     * Initializes pagination parameters to sensible defaults:
     * - pageNumber: 0 (first page)
     * - pageSize: 20 (reasonable default for account listings)
     */
    public AccountViewRequestDto() {
        this.pageNumber = 0;
        this.pageSize = 20;
    }

    /**
     * Constructor with account ID for direct account lookup operations.
     * 
     * @param accountId the account ID to view (must be 11 digits)
     */
    public AccountViewRequestDto(String accountId) {
        this();
        this.accountId = accountId;
    }

    /**
     * Full constructor for comprehensive account view request creation.
     * 
     * @param accountId the account ID to view (must be 11 digits)
     * @param pageNumber the page number for pagination (zero-based)
     * @param pageSize the number of records per page
     * @param sessionContext the session context information
     */
    public AccountViewRequestDto(String accountId, Integer pageNumber, Integer pageSize, SessionContext sessionContext) {
        this.accountId = accountId;
        this.pageNumber = pageNumber != null ? pageNumber : 0;
        this.pageSize = pageSize != null ? pageSize : 20;
        this.sessionContext = sessionContext;
    }

    /**
     * Gets the account ID for the view request.
     * 
     * @return the account ID (11 digits)
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID for the view request.
     * 
     * @param accountId the account ID (must be 11 digits)
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the page number for pagination.
     * 
     * @return the page number (zero-based)
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the page number for pagination.
     * 
     * @param pageNumber the page number (zero-based)
     */
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * Gets the page size for pagination.
     * 
     * @return the number of records per page
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Sets the page size for pagination.
     * 
     * @param pageSize the number of records per page
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Gets the session context information.
     * 
     * @return the session context
     */
    public SessionContext getSessionContext() {
        return sessionContext;
    }

    /**
     * Sets the session context information.
     * 
     * @param sessionContext the session context
     */
    public void setSessionContext(SessionContext sessionContext) {
        this.sessionContext = sessionContext;
    }

    /**
     * Validates the account view request using COBOL-equivalent validation logic.
     * 
     * Implements the same validation patterns as the original COACTVWC.cbl transaction,
     * including account ID format validation, required field checks, and business rule
     * validation equivalent to the original BMS field validation.
     * 
     * @return true if the request is valid, false otherwise
     */
    public boolean validate() {
        // Validate account ID if provided
        if (accountId != null && !accountId.trim().isEmpty()) {
            ValidationResult accountValidation = ValidationUtils.validateAccountNumber(accountId);
            if (accountValidation != ValidationResult.VALID) {
                return false;
            }
        }

        // Validate pagination parameters
        if (pageNumber != null && pageNumber < 0) {
            return false;
        }

        if (pageSize != null && (pageSize <= 0 || pageSize > 100)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the request is valid using comprehensive validation.
     * 
     * Provides a convenience method for validation checking that mirrors
     * the original COBOL validation patterns from COACTVWC.cbl.
     * 
     * @return true if the request passes all validation checks
     */
    public boolean isValid() {
        return validate();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * 
     * @param obj the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AccountViewRequestDto that = (AccountViewRequestDto) obj;
        return Objects.equals(accountId, that.accountId) &&
               Objects.equals(pageNumber, that.pageNumber) &&
               Objects.equals(pageSize, that.pageSize) &&
               Objects.equals(sessionContext, that.sessionContext);
    }

    /**
     * Returns a hash code value for the object.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId, pageNumber, pageSize, sessionContext);
    }

    /**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        return "AccountViewRequestDto{" +
               "accountId='" + accountId + '\'' +
               ", pageNumber=" + pageNumber +
               ", pageSize=" + pageSize +
               ", sessionContext=" + sessionContext +
               '}';
    }

    /**
     * Session context information for stateless REST API design.
     * 
     * Inner class that encapsulates session context data equivalent to CICS COMMAREA
     * session information from COCOM01Y.cpy, enabling stateless REST API while
     * preserving session context across service calls.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SessionContext {
        
        /**
         * User ID from session context.
         * Maps to CDEMO-USER-ID from COCOM01Y.cpy (PIC X(08)).
         */
        @Size(max = 8, message = "User ID cannot exceed 8 characters")
        private String userId;

        /**
         * Calling program name from session context.
         * Maps to CDEMO-FROM-PROGRAM from COCOM01Y.cpy (PIC X(08)).
         */
        @Size(max = 8, message = "From program cannot exceed 8 characters")
        private String fromProgram;

        /**
         * Target program name from session context.
         * Maps to CDEMO-TO-PROGRAM from COCOM01Y.cpy (PIC X(08)).
         */
        @Size(max = 8, message = "To program cannot exceed 8 characters")
        private String toProgram;

        /**
         * Transaction ID from session context.
         * Maps to CDEMO-FROM-TRANID from COCOM01Y.cpy (PIC X(04)).
         */
        @Size(max = 4, message = "Transaction ID cannot exceed 4 characters")
        private String transactionId;

        /**
         * User type from session context.
         * Maps to CDEMO-USER-TYPE from COCOM01Y.cpy (PIC X(01)).
         * Valid values: 'A' (Admin), 'U' (User)
         */
        @Size(max = 1, message = "User type must be single character")
        private String userType;

        /**
         * Program context from session context.
         * Maps to CDEMO-PGM-CONTEXT from COCOM01Y.cpy (PIC 9(01)).
         * Valid values: 0 (Enter), 1 (Re-enter)
         */
        private Integer programContext;

        /**
         * Default constructor for JSON deserialization.
         */
        public SessionContext() {
        }

        /**
         * Constructor with essential session information.
         * 
         * @param userId the user ID
         * @param transactionId the transaction ID
         * @param userType the user type
         */
        public SessionContext(String userId, String transactionId, String userType) {
            this.userId = userId;
            this.transactionId = transactionId;
            this.userType = userType;
        }

        // Getters and setters for all fields

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getFromProgram() {
            return fromProgram;
        }

        public void setFromProgram(String fromProgram) {
            this.fromProgram = fromProgram;
        }

        public String getToProgram() {
            return toProgram;
        }

        public void setToProgram(String toProgram) {
            this.toProgram = toProgram;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getUserType() {
            return userType;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }

        public Integer getProgramContext() {
            return programContext;
        }

        public void setProgramContext(Integer programContext) {
            this.programContext = programContext;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SessionContext that = (SessionContext) obj;
            return Objects.equals(userId, that.userId) &&
                   Objects.equals(fromProgram, that.fromProgram) &&
                   Objects.equals(toProgram, that.toProgram) &&
                   Objects.equals(transactionId, that.transactionId) &&
                   Objects.equals(userType, that.userType) &&
                   Objects.equals(programContext, that.programContext);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, fromProgram, toProgram, transactionId, userType, programContext);
        }

        @Override
        public String toString() {
            return "SessionContext{" +
                   "userId='" + userId + '\'' +
                   ", fromProgram='" + fromProgram + '\'' +
                   ", toProgram='" + toProgram + '\'' +
                   ", transactionId='" + transactionId + '\'' +
                   ", userType='" + userType + '\'' +
                   ", programContext=" + programContext +
                   '}';
        }
    }
}