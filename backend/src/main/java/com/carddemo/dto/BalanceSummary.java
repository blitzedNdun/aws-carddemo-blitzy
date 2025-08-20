/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data transfer object for account balance summary calculations.
 * 
 * Contains calculated balance information with COBOL COMP-3 precision
 * equivalence for financial calculations. Replicates the balance
 * calculation logic from COBOL CBACT03C ensuring exact monetary
 * precision matching the mainframe implementation.
 * 
 * All monetary fields use BigDecimal with scale=2 to preserve
 * COBOL packed decimal precision requirements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSummary {
    
    /**
     * Previous statement balance carried forward (COBOL COMP-3 precision)
     */
    private BigDecimal previousBalance;
    
    /**
     * Current calculated balance (COBOL COMP-3 precision)
     */
    private BigDecimal currentBalance;
    
    /**
     * Available credit amount
     */
    private BigDecimal availableCredit;
    
    /**
     * Credit limit for account
     */
    private BigDecimal creditLimit;
    
    /**
     * Total credits applied during period
     */
    private BigDecimal totalCredits;
    
    /**
     * Total debits applied during period
     */
    private BigDecimal totalDebits;
    
    /**
     * Interest charges calculated
     */
    private BigDecimal interestCharges;
    
    /**
     * Fees assessed during period
     */
    private BigDecimal feesAssessed;
    
    /**
     * Payments received during period
     */
    private BigDecimal paymentsReceived;
    
    /**
     * Purchases made during period
     */
    private BigDecimal purchasesAmount;
    
    /**
     * Cash advances during period
     */
    private BigDecimal cashAdvances;
    
    /**
     * Minimum payment due
     */
    private BigDecimal minimumPaymentDue;
    
    /**
     * Payment due date
     */
    private LocalDate paymentDueDate;
    
    /**
     * Days past due (if applicable)
     */
    private Integer daysPastDue;
    
    /**
     * Over credit limit flag
     */
    private Boolean overCreditLimit;
    
    /**
     * Balance calculation date
     */
    private LocalDate calculationDate;
    
    /**
     * Account identifier for cross-reference
     */
    private String accountId;
}