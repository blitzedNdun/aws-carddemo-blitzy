/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data transfer object for account statement generation data.
 * 
 * Contains aggregated account and transaction information required for 
 * statement formatting and output generation. This class consolidates
 * the data structures from COBOL CBACT03C batch statement generation
 * logic, maintaining compatibility with original mainframe processing.
 * 
 * Used by AccountStatementsService for passing comprehensive statement
 * data to formatting methods for text and HTML output generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementData {
    
    /**
     * Account information for statement header
     */
    private Account account;
    
    /**
     * Transactions included in statement period
     */
    private List<Transaction> transactions;
    
    /**
     * Statement generation date
     */
    private LocalDate statementDate;
    
    /**
     * Statement period start date
     */
    private LocalDate periodStartDate;
    
    /**
     * Statement period end date
     */
    private LocalDate periodEndDate;
    
    /**
     * Previous statement balance (COBOL COMP-3 precision)
     */
    private BigDecimal previousBalance;
    
    /**
     * Current statement balance (COBOL COMP-3 precision)
     */
    private BigDecimal currentBalance;
    
    /**
     * Total credits for statement period
     */
    private BigDecimal totalCredits;
    
    /**
     * Total debits for statement period
     */
    private BigDecimal totalDebits;
    
    /**
     * Interest charges for statement period
     */
    private BigDecimal interestCharges;
    
    /**
     * Fees assessed for statement period
     */
    private BigDecimal totalFees;
    
    /**
     * Minimum payment due
     */
    private BigDecimal minimumPaymentDue;
    
    /**
     * Payment due date
     */
    private LocalDate paymentDueDate;
    
    /**
     * Statement sequence number
     */
    private Integer statementSequence;
    
    /**
     * Statement identifier
     */
    private String statementId;
}