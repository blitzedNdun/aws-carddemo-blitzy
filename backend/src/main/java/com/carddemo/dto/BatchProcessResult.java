/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object for batch statement processing results.
 * 
 * Contains the results of batch statement generation operations
 * including processing statistics, error tracking, and file
 * generation metadata. Replicates the JCL job output structure
 * from COBOL batch processing while providing modern result
 * handling capabilities.
 * 
 * Used by AccountStatementsService for batch processing operations
 * that process multiple accounts in sequence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessResult {
    
    /**
     * Batch processing identifier
     */
    private String batchId;
    
    /**
     * Total number of accounts processed successfully
     */
    private Integer processedCount;
    
    /**
     * Total number of accounts requested for processing
     */
    private Integer totalCount;
    
    /**
     * Number of processing errors encountered
     */
    private Integer errorCount;
    
    /**
     * Processing start timestamp
     */
    private LocalDateTime startTime;
    
    /**
     * Processing completion timestamp
     */
    private LocalDateTime endTime;
    
    /**
     * Total processing duration in milliseconds
     */
    private Long processingTimeMs;
    
    /**
     * Processing success indicator
     */
    private Boolean success;
    
    /**
     * List of error messages for failed processing
     */
    private List<String> errors;
    
    /**
     * Map of account IDs to processing status
     */
    private Map<String, String> accountStatuses;
    
    /**
     * Map of account IDs to generated statement IDs
     */
    private Map<String, String> generatedStatements;
    
    /**
     * List of output file paths generated
     */
    private List<String> outputFiles;
    
    /**
     * Processing statistics summary
     */
    private String statisticsSummary;
    
    /**
     * Memory usage during processing (bytes)
     */
    private Long memoryUsage;
    
    /**
     * Processing rate (accounts per second)
     */
    private Double processingRate;
}