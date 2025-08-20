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

/**
 * Data transfer object for statement generation results.
 * 
 * Contains the results of statement generation operations including
 * generated content, metadata, and processing statistics. Replicates
 * the output structure from COBOL CBACT03C batch statement processing
 * while providing modern result handling capabilities.
 * 
 * Used by AccountStatementsService to return comprehensive statement
 * generation results to calling components.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementResult {
    
    /**
     * Generated statement identifier
     */
    private String statementId;
    
    /**
     * Generated statement content (text or HTML)
     */
    private String statementContent;
    
    /**
     * Statement format type (TEXT, HTML)
     */
    private String formatType;
    
    /**
     * Number of transactions included in statement
     */
    private Integer transactionCount;
    
    /**
     * Statement generation timestamp
     */
    private LocalDateTime generationTimestamp;
    
    /**
     * Processing success indicator
     */
    private Boolean success;
    
    /**
     * Error message if generation failed
     */
    private String errorMessage;
    
    /**
     * File path for generated statement file
     */
    private String filePath;
    
    /**
     * Statement content length in characters/bytes
     */
    private Long contentLength;
    
    /**
     * Processing duration in milliseconds
     */
    private Long processingTimeMs;
    
    /**
     * Account ID for cross-reference
     */
    private String accountId;
    
    /**
     * Statement period identifier
     */
    private String statementPeriod;
}