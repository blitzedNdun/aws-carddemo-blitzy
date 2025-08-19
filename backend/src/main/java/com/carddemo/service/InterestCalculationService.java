/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.DisclosureGroup;
import com.carddemo.repository.DisclosureGroupRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Service for performing interest calculations based on COBOL CBACT04C.cbl logic.
 * 
 * This service implements the core interest calculation algorithms from the mainframe
 * COBOL program CBACT04C.cbl, maintaining exact precision and business logic.
 * 
 * Key COBOL formula translated:
 * WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
 * 
 * The service uses BigDecimal arithmetic with HALF_UP rounding mode to replicate
 * COBOL COMP-3 packed decimal behavior and ensure exact monetary calculations.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Service
@Slf4j
public class InterestCalculationService {

    private final DisclosureGroupRepository disclosureGroupRepository;

    /**
     * Constructor-based dependency injection for Spring Boot repository.
     * 
     * @param disclosureGroupRepository Repository for disclosure group data access
     */
    @Autowired
    public InterestCalculationService(DisclosureGroupRepository disclosureGroupRepository) {
        this.disclosureGroupRepository = disclosureGroupRepository;
        log.info("InterestCalculationService initialized successfully");
    }

    /**
     * Gets the effective interest rate for a given account group, transaction type, and category.
     * 
     * This method replicates the COBOL logic from CBACT04C.cbl paragraph 1200-GET-INTEREST-RATE
     * which retrieves the appropriate interest rate from the DISCGRP file based on the
     * account group ID, transaction type code, and category code combination.
     * 
     * @param accountGroupId The account group identifier (10 chars)
     * @param transactionTypeCode The transaction type code (2 chars)
     * @param categoryCode The transaction category code (4 digits)
     * @return The effective interest rate as BigDecimal with 2 decimal places
     * @throws IllegalArgumentException if parameters are invalid or rate not found
     */
    public BigDecimal getEffectiveRate(String accountGroupId, String transactionTypeCode, String categoryCode) {
        log.debug("Getting effective rate for accountGroupId: {}, transactionTypeCode: {}, categoryCode: {}", 
                 accountGroupId, transactionTypeCode, categoryCode);

        // Validate input parameters
        if (accountGroupId == null || accountGroupId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account Group ID is required");
        }
        if (transactionTypeCode == null || transactionTypeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction Type Code is required");
        }
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Category Code is required");
        }

        // Trim and validate category code format (4 digits)
        String trimmedCategoryCode = categoryCode.trim();
        if (!trimmedCategoryCode.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("Category code must be exactly 4 digits");
        }

        try {
            // Find disclosure group matching the parameters
            Optional<DisclosureGroup> disclosureGroupOpt = disclosureGroupRepository
                .findByAccountGroupIdAndTransactionTypeCodeAndTransactionCategoryCode(
                    accountGroupId.trim(), 
                    transactionTypeCode.trim(), 
                    trimmedCategoryCode
                );

            if (disclosureGroupOpt.isEmpty()) {
                log.warn("No disclosure group found for accountGroupId: {}, transactionTypeCode: {}, categoryCode: {}", 
                        accountGroupId, transactionTypeCode, categoryCode);
                throw new IllegalArgumentException(
                    String.format("No interest rate found for account group '%s', type '%s', category '%s'", 
                                 accountGroupId, transactionTypeCode, categoryCode));
            }

            DisclosureGroup disclosureGroup = disclosureGroupOpt.get();

            BigDecimal effectiveRate = disclosureGroup.getInterestRate();
            if (effectiveRate == null) {
                throw new IllegalArgumentException("Interest rate is null for the specified disclosure group");
            }

            // Ensure 2 decimal places precision matching COBOL PIC S9(04)V99
            BigDecimal scaledRate = effectiveRate.setScale(2, RoundingMode.HALF_UP);
            
            log.debug("Found effective rate: {} for accountGroupId: {}, transactionTypeCode: {}, categoryCode: {}", 
                     scaledRate, accountGroupId, transactionTypeCode, categoryCode);
            
            return scaledRate;

        } catch (Exception e) {
            log.error("Error retrieving effective rate: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve effective interest rate", e);
        }
    }

    /**
     * Calculates monthly interest using the COBOL formula from CBACT04C.cbl.
     * 
     * Implements the exact COBOL calculation:
     * WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
     * 
     * The formula divides by 1200 because:
     * - 12 months in a year (annual to monthly conversion)
     * - 100 to convert percentage rate to decimal (e.g., 15% becomes 0.15)
     * - 12 * 100 = 1200
     * 
     * @param balance The account balance to calculate interest on
     * @param annualInterestRate The annual interest rate as percentage (e.g., 15.00 for 15%)
     * @return Monthly interest amount with 2 decimal places precision
     * @throws IllegalArgumentException if parameters are invalid
     */
    public BigDecimal calculateMonthlyInterest(BigDecimal balance, BigDecimal annualInterestRate) {
        log.debug("Calculating monthly interest for balance: {}, annual rate: {}", balance, annualInterestRate);

        // Validate input parameters
        if (balance == null) {
            throw new IllegalArgumentException("Balance is required for interest calculation");
        }
        if (annualInterestRate == null) {
            throw new IllegalArgumentException("Annual interest rate is required for calculation");
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        if (annualInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }

        // Implement COBOL formula: WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
        // Using exact BigDecimal arithmetic to match COBOL COMP-3 precision
        BigDecimal divisor = BigDecimal.valueOf(1200); // 12 months * 100 (percentage conversion)
        
        BigDecimal monthlyInterest = balance
            .multiply(annualInterestRate)
            .divide(divisor, 2, RoundingMode.HALF_UP);

        log.debug("Calculated monthly interest: {} from balance: {} and rate: {}", 
                 monthlyInterest, balance, annualInterestRate);

        return monthlyInterest;
    }

    /**
     * Calculates daily interest for comparison and validation purposes.
     * 
     * This method provides daily interest calculation which can be used for
     * validation against monthly calculations or for daily compounding scenarios.
     * 
     * Formula: (balance * annual_rate) / (365 * 100)
     * 
     * @param balance The account balance to calculate interest on
     * @param annualInterestRate The annual interest rate as percentage
     * @return Daily interest amount with 4 decimal places precision
     * @throws IllegalArgumentException if parameters are invalid
     */
    public BigDecimal calculateDailyInterest(BigDecimal balance, BigDecimal annualInterestRate) {
        log.debug("Calculating daily interest for balance: {}, annual rate: {}", balance, annualInterestRate);

        // Validate input parameters
        if (balance == null) {
            throw new IllegalArgumentException("Balance is required for interest calculation");
        }
        if (annualInterestRate == null) {
            throw new IllegalArgumentException("Annual interest rate is required for calculation");
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        if (annualInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }

        // Calculate daily interest: (balance * rate) / (365 * 100)
        BigDecimal divisor = BigDecimal.valueOf(36500); // 365 days * 100 (percentage conversion)
        
        BigDecimal dailyInterest = balance
            .multiply(annualInterestRate)
            .divide(divisor, 4, RoundingMode.HALF_UP); // 4 decimal places for higher precision

        log.debug("Calculated daily interest: {} from balance: {} and rate: {}", 
                 dailyInterest, balance, annualInterestRate);

        return dailyInterest;
    }

    /**
     * Calculates compound interest for a specified time period.
     * 
     * This method provides compound interest calculation using the formula:
     * A = P(1 + r/n)^(nt) where interest = A - P
     * 
     * @param principal The principal amount
     * @param annualInterestRate The annual interest rate as percentage
     * @param timeInYears The time period in years
     * @param compoundingPeriodsPerYear Number of compounding periods per year (e.g., 12 for monthly)
     * @return Compound interest amount with 2 decimal places precision
     * @throws IllegalArgumentException if parameters are invalid
     */
    public BigDecimal calculateCompoundInterest(BigDecimal principal, BigDecimal annualInterestRate, 
                                              BigDecimal timeInYears, int compoundingPeriodsPerYear) {
        log.debug("Calculating compound interest for principal: {}, rate: {}, time: {}, periods: {}", 
                 principal, annualInterestRate, timeInYears, compoundingPeriodsPerYear);

        // Validate input parameters
        if (principal == null || annualInterestRate == null || timeInYears == null) {
            throw new IllegalArgumentException("Principal, rate, and time must all be supplied");
        }
        if (principal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Principal cannot be negative");
        }
        if (annualInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        if (timeInYears.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Time cannot be negative");
        }
        if (compoundingPeriodsPerYear <= 0) {
            throw new IllegalArgumentException("Compounding periods must be positive");
        }

        // Convert percentage rate to decimal
        BigDecimal rateDecimal = annualInterestRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        
        // Calculate r/n (rate per period)
        BigDecimal ratePerPeriod = rateDecimal.divide(BigDecimal.valueOf(compoundingPeriodsPerYear), 6, RoundingMode.HALF_UP);
        
        // Calculate nt (total number of periods)
        BigDecimal totalPeriods = timeInYears.multiply(BigDecimal.valueOf(compoundingPeriodsPerYear));
        
        // Calculate (1 + r/n)^nt using power approximation for BigDecimal
        BigDecimal onePlusRate = BigDecimal.ONE.add(ratePerPeriod);
        BigDecimal compoundFactor = pow(onePlusRate, totalPeriods.intValue());
        
        // Calculate final amount A = P * (1 + r/n)^nt
        BigDecimal finalAmount = principal.multiply(compoundFactor);
        
        // Interest = A - P
        BigDecimal compoundInterest = finalAmount.subtract(principal);
        
        log.debug("Calculated compound interest: {} from principal: {}", compoundInterest, principal);
        
        return compoundInterest.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Helper method to calculate power for BigDecimal values.
     * Uses iterative multiplication for integer exponents.
     * 
     * @param base The base value
     * @param exponent The integer exponent
     * @return base^exponent as BigDecimal
     */
    private BigDecimal pow(BigDecimal base, int exponent) {
        if (exponent == 0) {
            return BigDecimal.ONE;
        }
        if (exponent == 1) {
            return base;
        }
        
        BigDecimal result = BigDecimal.ONE;
        BigDecimal currentBase = base;
        int currentExponent = Math.abs(exponent);
        
        while (currentExponent > 0) {
            if (currentExponent % 2 == 1) {
                result = result.multiply(currentBase);
            }
            currentBase = currentBase.multiply(currentBase);
            currentExponent /= 2;
        }
        
        if (exponent < 0) {
            result = BigDecimal.ONE.divide(result, 6, RoundingMode.HALF_UP);
        }
        
        return result;
    }

    /**
     * Validates interest calculation parameters for business rule compliance.
     * 
     * @param balance The account balance
     * @param interestRate The interest rate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateCalculationParameters(BigDecimal balance, BigDecimal interestRate) {
        if (balance == null) {
            throw new IllegalArgumentException("Balance is required");
        }
        if (interestRate == null) {
            throw new IllegalArgumentException("Interest rate is required");
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        if (interestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        
        // Business rule: Maximum interest rate check (e.g., 50% annual)
        BigDecimal maxRate = BigDecimal.valueOf(50.00);
        if (interestRate.compareTo(maxRate) > 0) {
            log.warn("Interest rate {} exceeds maximum allowed rate {}", interestRate, maxRate);
            throw new IllegalArgumentException("Interest rate exceeds maximum allowed limit");
        }
    }
}