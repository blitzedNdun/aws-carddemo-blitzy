/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.batch.InterestCalculationJob;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for orchestrating interest calculation batch job execution and providing
 * interest calculation business logic for the CardDemo system.
 * 
 * This service acts as a facade for the InterestCalculationJob batch processor and
 * provides additional business methods for interest calculations, validation, and
 * monitoring. It bridges the Spring Batch job execution with the REST API layer.
 * 
 * Key Responsibilities:
 * 1. Launch and monitor interest calculation batch jobs
 * 2. Provide real-time interest calculation methods for validation
 * 3. Handle interest rate lookup with DEFAULT fallback logic
 * 4. Validate calculation precision and business rules
 * 5. Track job execution metrics and performance
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Service
public class InterestCalculationJobService {

    private static final Logger logger = LoggerFactory.getLogger(InterestCalculationJobService.class);

    private final InterestCalculationJob interestCalculationJob;
    private final InterestCalculationService interestCalculationService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final JobLauncher jobLauncher;

    // Job execution tracking
    private JobExecution currentJobExecution;
    private String lastJobStatus = "UNKNOWN";
    
    /**
     * Constructor with dependency injection for all required services.
     */
    @Autowired
    public InterestCalculationJobService(
            InterestCalculationJob interestCalculationJob,
            InterestCalculationService interestCalculationService,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            JobLauncher jobLauncher) {
        this.interestCalculationJob = interestCalculationJob;
        this.interestCalculationService = interestCalculationService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.jobLauncher = jobLauncher;
        logger.info("InterestCalculationJobService initialized successfully");
    }

    /**
     * Launches the interest calculation batch job for the specified execution date.
     * 
     * This method triggers the Spring Batch job that processes all accounts and
     * generates interest transactions. It provides async execution with status
     * tracking and comprehensive error handling.
     * 
     * @param executionDate the date for interest calculation (optional, defaults to today)
     * @return JobExecution object containing execution details and status
     * @throws Exception if job launch fails
     */
    public JobExecution launchInterestCalculationJob(LocalDate executionDate) throws Exception {
        logger.info("Launching interest calculation job for execution date: {}", executionDate);
        
        try {
            currentJobExecution = interestCalculationJob.executeJob(executionDate);
            lastJobStatus = currentJobExecution.getStatus().toString();
            
            logger.info("Interest calculation job launched successfully with status: {}", lastJobStatus);
            return currentJobExecution;
            
        } catch (Exception e) {
            logger.error("Failed to launch interest calculation job", e);
            lastJobStatus = "FAILED";
            throw e;
        }
    }

    /**
     * Launches the interest calculation batch job with default execution date (today).
     * 
     * @return JobExecution object containing execution details and status
     * @throws Exception if job launch fails
     */
    public JobExecution launchInterestCalculationJob() throws Exception {
        return launchInterestCalculationJob(LocalDate.now());
    }

    /**
     * Gets the current status of the interest calculation job.
     * 
     * @return job execution status as string
     */
    public String getJobExecutionStatus() {
        if (currentJobExecution != null) {
            return currentJobExecution.getStatus().toString();
        }
        return lastJobStatus;
    }

    /**
     * Gets comprehensive job execution metrics for monitoring and reporting.
     * 
     * @return map containing job metrics and performance data
     */
    public Map<String, Object> getJobMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        if (currentJobExecution != null) {
            metrics.put("status", currentJobExecution.getStatus().toString());
            metrics.put("startTime", currentJobExecution.getStartTime());
            metrics.put("endTime", currentJobExecution.getEndTime());
            metrics.put("exitCode", currentJobExecution.getExitStatus().getExitCode());
            metrics.put("jobId", currentJobExecution.getId());
            
            // Calculate duration if available
            if (currentJobExecution.getStartTime() != null && currentJobExecution.getEndTime() != null) {
                long durationMillis = java.time.Duration.between(
                    currentJobExecution.getStartTime(), 
                    currentJobExecution.getEndTime()
                ).toMillis();
                metrics.put("durationMillis", durationMillis);
            }
        } else {
            metrics.put("status", lastJobStatus);
            metrics.put("message", "No job execution available");
        }
        
        return metrics;
    }

    /**
     * Calculates monthly interest for a given balance and rate using COBOL-compatible precision.
     * 
     * This method delegates to InterestCalculationService for the actual calculation
     * while providing additional validation and logging.
     * 
     * @param balance the account balance to calculate interest on
     * @param annualInterestRate the annual interest rate as percentage
     * @return calculated monthly interest amount
     */
    public BigDecimal calculateMonthlyInterest(BigDecimal balance, BigDecimal annualInterestRate) {
        logger.debug("Calculating monthly interest for balance: {}, rate: {}", balance, annualInterestRate);
        return interestCalculationService.calculateMonthlyInterest(balance, annualInterestRate);
    }

    /**
     * Converts annual percentage rate (APR) to daily interest rate.
     * 
     * @param annualRate annual percentage rate
     * @return daily interest rate with appropriate precision
     */
    public BigDecimal convertAPRToDailyRate(BigDecimal annualRate) {
        if (annualRate == null) {
            return BigDecimal.ZERO;
        }
        return annualRate.divide(new BigDecimal("365"), 6, RoundingMode.HALF_UP);
    }

    /**
     * Calculates minimum interest charge when calculated interest is below threshold.
     * 
     * @param calculatedInterest the calculated interest amount
     * @return minimum interest charge amount
     */
    public BigDecimal calculateMinimumInterestCharge(BigDecimal calculatedInterest) {
        BigDecimal minimumCharge = new BigDecimal("1.00");
        if (calculatedInterest != null && calculatedInterest.compareTo(minimumCharge) < 0) {
            return minimumCharge;
        }
        return calculatedInterest != null ? calculatedInterest : minimumCharge;
    }

    /**
     * Gets interest rate for a specific account group with DEFAULT fallback.
     * 
     * @param groupId account group identifier
     * @return applicable interest rate
     */
    public BigDecimal getInterestRate(String groupId) {
        try {
            // Try to get specific group rate using the service
            // This is a simplified implementation - in practice would need transaction type and category
            return interestCalculationService.getEffectiveRate(
                groupId != null ? groupId : "DEFAULT", 
                "01", // Interest transaction type
                "05"  // Interest category code
            );
        } catch (Exception e) {
            logger.warn("Could not get interest rate for group {}, returning zero", groupId);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Checks if the current date is within the grace period for interest calculation.
     * 
     * @param statementDate the statement date
     * @param dueDate the payment due date  
     * @param currentDate the current date
     * @return true if within grace period, false otherwise
     */
    public boolean isWithinGracePeriod(LocalDate statementDate, LocalDate dueDate, LocalDate currentDate) {
        if (statementDate == null || dueDate == null || currentDate == null) {
            return false;
        }
        return currentDate.isBefore(dueDate) || currentDate.isEqual(dueDate);
    }

    /**
     * Calculates compound interest for a given principal, rate, and time period.
     * 
     * @param principal the principal amount
     * @param dailyRate the daily interest rate
     * @param daysSinceLastStatement number of days since last statement
     * @return calculated compound interest amount
     */
    public BigDecimal calculateCompoundInterest(BigDecimal principal, BigDecimal dailyRate, int daysSinceLastStatement) {
        if (principal == null || dailyRate == null || daysSinceLastStatement <= 0) {
            return BigDecimal.ZERO;
        }

        // Use the service method for compound interest calculation
        BigDecimal timeInYears = new BigDecimal(daysSinceLastStatement).divide(new BigDecimal("365"), 6, RoundingMode.HALF_UP);
        BigDecimal annualRate = dailyRate.multiply(new BigDecimal("365")).multiply(new BigDecimal("100"));
        
        return interestCalculationService.calculateCompoundInterest(principal, annualRate, timeInYears, 365);
    }

    /**
     * Validates that calculation precision meets COBOL standards.
     * 
     * @param calculationResult the BigDecimal result to validate
     * @return true if precision is valid, false otherwise
     */
    public boolean validateCalculationPrecision(BigDecimal calculationResult) {
        if (calculationResult == null) {
            return false;
        }
        
        // Ensure scale is exactly 2 for monetary calculations
        if (calculationResult.scale() != 2) {
            return false;
        }
        
        // Ensure precision is within acceptable limits
        if (calculationResult.precision() > 20) {
            return false;
        }
        
        return true;
    }
}