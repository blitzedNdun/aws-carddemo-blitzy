/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Data Transfer Object for transaction reconciliation processing requests.
 * Contains batch processing parameters, validation rules, and processing options
 * for COBOL CBTRN02C equivalent batch reconciliation operations.
 * 
 * This DTO supports the complete reconciliation workflow including:
 * - Daily transaction validation rules
 * - Processing options and controls
 * - Batch date and timing parameters
 * - Validation rule configuration
 * - Network settlement parameters
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationRequest {
    
    /**
     * Batch processing date for reconciliation
     */
    @NotNull(message = "Batch date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("batchDate")
    private LocalDate batchDate;
    
    /**
     * Processing options map containing various control flags:
     * - validateOnly: Boolean flag for validation-only mode
     * - generateClearingFile: Boolean flag for clearing file generation
     * - updateAccountBalances: Boolean flag for account balance updates
     * - processRejects: Boolean flag for reject transaction handling
     */
    @JsonProperty("processingOptions")
    @Builder.Default
    private Map<String, Object> processingOptions = new HashMap<>();
    
    /**
     * Validation rules map containing business validation controls:
     * - checkCreditLimit: Boolean flag for credit limit validation
     * - checkExpirationDate: Boolean flag for card expiration validation
     * - validateCardNumber: Boolean flag for card number validation
     * - checkAccountStatus: Boolean flag for account status validation
     */
    @JsonProperty("validationRules")
    @Builder.Default
    private Map<String, Object> validationRules = new HashMap<>();
    
    /**
     * Network settlement parameters for clearing operations
     */
    @JsonProperty("settlementParameters")
    @Builder.Default
    private Map<String, String> settlementParameters = new HashMap<>();
    
    /**
     * List of specific transaction IDs to process (optional)
     * If empty, processes all transactions for the batch date
     */
    @JsonProperty("transactionIds")
    @Builder.Default
    private List<String> transactionIds = new ArrayList<>();
    
    /**
     * Account IDs to include in reconciliation (optional)
     * If empty, processes all accounts
     */
    @JsonProperty("accountIds")
    @Builder.Default
    private List<Long> accountIds = new ArrayList<>();
    
    /**
     * Processing priority for batch operations
     * Values: HIGH, NORMAL, LOW
     */
    @JsonProperty("processingPriority")
    @Builder.Default
    private String processingPriority = "NORMAL";
    
    /**
     * Maximum number of transactions to process in single batch
     * Used for performance control and memory management
     */
    @JsonProperty("maxTransactionCount")
    @Builder.Default
    private Integer maxTransactionCount = 10000;
    
    /**
     * Timeout in seconds for batch processing operations
     */
    @JsonProperty("timeoutSeconds")
    @Builder.Default
    private Integer timeoutSeconds = 3600; // 1 hour default
    
    /**
     * Flag to enable/disable detailed processing logging
     */
    @JsonProperty("enableDetailedLogging")
    @Builder.Default
    private Boolean enableDetailedLogging = false;
}