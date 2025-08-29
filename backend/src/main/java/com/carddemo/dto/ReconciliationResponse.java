/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Data Transfer Object for transaction reconciliation processing responses.
 * Contains processing statistics, error information, and reconciliation results
 * from COBOL CBTRN02C equivalent batch reconciliation operations.
 * 
 * This DTO provides comprehensive feedback on reconciliation processing including:
 * - Transaction processing counts and statistics
 * - Reject transaction details and counts
 * - Processing status and completion information
 * - Validation error details and messages
 * - Financial totals and summary information
 * - Clearing file generation results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationResponse {
    
    /**
     * Batch processing date for which reconciliation was performed
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("batchDate")
    private LocalDate batchDate;
    
    /**
     * Total number of transactions processed successfully
     */
    @JsonProperty("transactionCount")
    private Integer transactionCount = 0;
    
    /**
     * Number of transactions rejected during processing
     */
    @JsonProperty("rejectCount")
    private Integer rejectCount = 0;
    
    /**
     * Processing status indicator
     * Values: NOT_STARTED, PROCESSING, COMPLETED, COMPLETED_WITH_REJECTIONS, ERROR, TIMEOUT
     */
    @JsonProperty("processingStatus")
    private String processingStatus = "NOT_STARTED";
    
    /**
     * List of validation errors encountered during processing
     */
    @JsonProperty("validationErrors")
    @Builder.Default
    private List<String> validationErrors = new ArrayList<>();
    
    /**
     * Processing start timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("processingStartTime")
    private LocalDateTime processingStartTime;
    
    /**
     * Processing completion timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("processingEndTime")
    private LocalDateTime processingEndTime;
    
    /**
     * Total processing duration in milliseconds
     */
    @JsonProperty("processingDurationMs")
    private Long processingDurationMs;
    
    /**
     * Total amount processed (sum of all valid transactions)
     */
    @JsonProperty("totalAmountProcessed")
    private BigDecimal totalAmountProcessed = BigDecimal.ZERO;
    
    /**
     * Total amount rejected (sum of all rejected transactions)
     */
    @JsonProperty("totalAmountRejected")
    private BigDecimal totalAmountRejected = BigDecimal.ZERO;
    
    /**
     * Number of accounts affected by the reconciliation
     */
    @JsonProperty("accountsAffected")
    private Integer accountsAffected = 0;
    
    /**
     * Clearing file generation status
     */
    @JsonProperty("clearingFileGenerated")
    private Boolean clearingFileGenerated = false;
    
    /**
     * Clearing file path/name if generated
     */
    @JsonProperty("clearingFileName")
    private String clearingFileName;
    
    /**
     * Clearing file record count
     */
    @JsonProperty("clearingFileRecordCount")
    private Integer clearingFileRecordCount = 0;
    
    /**
     * Summary statistics by transaction type
     */
    @JsonProperty("transactionTypeSummary")
    @Builder.Default
    private Map<String, Integer> transactionTypeSummary = new HashMap<>();
    
    /**
     * Summary statistics by rejection reason
     */
    @JsonProperty("rejectionReasonSummary")
    @Builder.Default
    private Map<String, Integer> rejectionReasonSummary = new HashMap<>();
    
    /**
     * Network settlement totals by settlement network
     */
    @JsonProperty("settlementTotals")
    @Builder.Default
    private Map<String, BigDecimal> settlementTotals = new HashMap<>();
    
    /**
     * Warning messages (non-fatal issues)
     */
    @JsonProperty("warningMessages")
    @Builder.Default
    private List<String> warningMessages = new ArrayList<>();
    
    /**
     * Detailed reject transaction information
     */
    @JsonProperty("rejectDetails")
    @Builder.Default
    private List<Map<String, Object>> rejectDetails = new ArrayList<>();
    
    /**
     * Processing performance metrics
     */
    @JsonProperty("performanceMetrics")
    @Builder.Default
    private Map<String, Object> performanceMetrics = new HashMap<>();
    
    /**
     * Indicates if reconciliation completed successfully
     * @return true if status is COMPLETED, false otherwise
     */
    public boolean isSuccessful() {
        return "COMPLETED".equals(processingStatus);
    }
    
    /**
     * Indicates if reconciliation completed with some rejections
     * @return true if status is COMPLETED_WITH_REJECTIONS, false otherwise
     */
    public boolean isPartiallySuccessful() {
        return "COMPLETED_WITH_REJECTIONS".equals(processingStatus);
    }
    
    /**
     * Indicates if reconciliation encountered errors
     * @return true if status indicates error condition, false otherwise
     */
    public boolean hasErrors() {
        return "ERROR".equals(processingStatus) || "TIMEOUT".equals(processingStatus);
    }
    
    /**
     * Calculates success rate as percentage
     * @return success rate percentage (0.0 to 100.0)
     */
    public double getSuccessRate() {
        if (transactionCount == 0) {
            return 0.0;
        }
        int successfulCount = transactionCount - rejectCount;
        return (successfulCount * 100.0) / transactionCount;
    }
}