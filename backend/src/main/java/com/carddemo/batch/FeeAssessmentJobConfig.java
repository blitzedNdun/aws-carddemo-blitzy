package com.carddemo.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Batch job configuration for fee assessment processing.
 * 
 * This configuration defines the complete fee assessment workflow including:
 * - Account reading for accounts requiring fee assessment
 * - Fee evaluation based on account type, balance, and fee schedules
 * - Waiver condition checking for fee exemptions
 * - Fee transaction generation and account balance updates
 * 
 * The implementation follows the Spring Batch ItemReader, ItemProcessor, 
 * and ItemWriter pattern for chunked processing with proper error handling 
 * and transaction boundaries.
 * 
 * Translated from COBOL program CBACT04C.cbl, specifically focusing on
 * the fee computation logic (originally stubbed in COBOL as 1400-COMPUTE-FEES).
 */
@Configuration
@EnableBatchProcessing
public class FeeAssessmentJobConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DataSource dataSource;

    // Chunk size for processing - balances performance vs memory usage
    private static final int CHUNK_SIZE = 100;
    
    // Fee assessment date parameter (format: YYYY-MM-DD)
    @Value("#{jobParameters['assessmentDate'] ?: T(java.time.LocalDate).now().toString()}")
    private String assessmentDate = LocalDate.now().toString();
    
    // Fee type filter parameter (e.g., 'MONTHLY', 'ANNUAL', 'OVERDRAFT')
    @Value("#{jobParameters['feeType'] ?: 'MONTHLY'}")
    private String feeType = "MONTHLY";

    /**
     * Main fee assessment job definition.
     * 
     * Configures the complete fee assessment workflow with proper restart
     * and recovery capabilities. The job processes accounts in chunks to
     * handle large datasets efficiently while maintaining transaction
     * boundaries that mirror COBOL SYNCPOINT behavior.
     * 
     * @return Configured Spring Batch Job for fee assessment processing
     */
    @Bean
    public Job feeAssessmentJob() {
        return new JobBuilder("feeAssessmentJob", jobRepository)
                .start(feeAssessmentStep())
                .build();
    }

    /**
     * Fee assessment step configuration.
     * 
     * Defines the step with ItemReader, ItemProcessor, and ItemWriter
     * components for chunked processing. Implements error handling with
     * skip logic for failed fee assessments and configures transaction
     * boundaries matching COBOL commit intervals.
     * 
     * @return Configured Step for fee assessment processing
     */
    @Bean
    public Step feeAssessmentStep() {
        return new StepBuilder("feeAssessmentStep", jobRepository)
                .<FeeAssessmentAccount, FeeAssessmentResult>chunk(CHUNK_SIZE, transactionManager)
                .reader(feeAssessmentReader())
                .processor(feeAssessmentProcessor())
                .writer(feeAssessmentWriter())
                // Error handling configuration - skip individual record failures
                .faultTolerant()
                .skip(FeeAssessmentException.class)
                .skipLimit(100) // Allow up to 100 failed assessments before job failure
                .retry(FeeAssessmentRetryableException.class)
                .retryLimit(3)
                // Restart configuration for interrupted processing
                .allowStartIfComplete(false)
                .build();
    }

    /**
     * ItemReader for reading account data requiring fee assessment.
     * 
     * Implements pagination-based reading of accounts that need fee assessment
     * based on the assessment date and account criteria. Mirrors the COBOL
     * sequential file reading pattern from TCATBAL-FILE processing.
     * 
     * @return ItemReader for FeeAssessmentAccount entities
     */
    @Bean
    public ItemReader<FeeAssessmentAccount> feeAssessmentReader() {
        return new RepositoryItemReaderBuilder<FeeAssessmentAccount>()
                .name("feeAssessmentReader")
                .repository(accountRepository)
                .methodName("findAccountsRequiringFeeAssessment")
                .arguments(Arrays.asList(LocalDate.parse(assessmentDate), feeType))
                .sorts(Collections.singletonMap("accountId", Sort.Direction.ASC))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    /**
     * ItemProcessor for evaluating fee schedules and waiver conditions.
     * 
     * Processes each account to determine applicable fees based on:
     * - Account type and fee schedules
     * - Current balance and transaction history
     * - Waiver conditions and exemptions
     * - Fee calculation rules mirroring COBOL computation logic
     * 
     * @return ItemProcessor for fee evaluation
     */
    @Bean
    public ItemProcessor<FeeAssessmentAccount, FeeAssessmentResult> feeAssessmentProcessor() {
        return new FeeAssessmentProcessor();
    }

    /**
     * ItemWriter for generating fee transactions and updating balances.
     * 
     * Implements composite writing to:
     * 1. Generate fee transaction records
     * 2. Update account balances with assessed fees
     * 
     * Uses batch writing for optimal performance and maintains ACID
     * compliance through proper transaction boundaries.
     * 
     * @return CompositeItemWriter for fee transactions and balance updates
     */
    @Bean
    public ItemWriter<FeeAssessmentResult> feeAssessmentWriter() {
        return new CompositeItemWriterBuilder<FeeAssessmentResult>()
                .delegates(Arrays.asList(
                    feeTransactionWriter(),
                    accountBalanceUpdateWriter()
                ))
                .build();
    }

    /**
     * ItemWriter for fee transaction generation.
     * 
     * Writes fee transaction records to the transaction table, mirroring
     * the COBOL pattern of writing to TRANSACT-FILE with proper transaction
     * ID generation and audit trail information.
     * 
     * @return JdbcBatchItemWriter for fee transactions
     */
    private ItemWriter<FeeAssessmentResult> feeTransactionWriter() {
        return new JdbcBatchItemWriterBuilder<FeeAssessmentResult>()
                .dataSource(dataSource)
                .sql("INSERT INTO transaction " +
                     "(transaction_id, account_id, transaction_type_cd, transaction_cat_cd, " +
                     "transaction_source, transaction_desc, transaction_amt, " +
                     "card_num, orig_timestamp, proc_timestamp) " +
                     "VALUES (:transactionId, :accountId, :transactionTypeCd, :transactionCatCd, " +
                     ":transactionSource, :transactionDesc, :transactionAmt, " +
                     ":cardNum, :origTimestamp, :procTimestamp)")
                .beanMapped()
                .build();
    }

    /**
     * ItemWriter for account balance updates.
     * 
     * Updates account balances with assessed fees, maintaining the same
     * balance update pattern as the COBOL program's account record updates.
     * Implements proper COMP-3 decimal precision handling through BigDecimal.
     * 
     * @return JdbcBatchItemWriter for account balance updates
     */
    private ItemWriter<FeeAssessmentResult> accountBalanceUpdateWriter() {
        return new JdbcBatchItemWriterBuilder<FeeAssessmentResult>()
                .dataSource(dataSource)
                .sql("UPDATE account " +
                     "SET current_balance = current_balance + :feeAmount, " +
                     "current_cycle_debit = current_cycle_debit + :feeAmount, " +
                     "last_updated_timestamp = :procTimestamp " +
                     "WHERE account_id = :accountId")
                .beanMapped()
                .build();
    }

    // Repository interface for account data access (injected by Spring)
    @Autowired
    private AccountRepository accountRepository;

    /**
     * Fee Assessment Processor implementation.
     * 
     * Evaluates fees for each account based on fee schedules, waiver conditions,
     * and account characteristics. Implements the business logic that was stubbed
     * in the original COBOL program's 1400-COMPUTE-FEES paragraph.
     */
    public static class FeeAssessmentProcessor implements ItemProcessor<FeeAssessmentAccount, FeeAssessmentResult> {

        @Override
        @Retryable(retryFor = {FeeAssessmentRetryableException.class}, 
                   maxAttempts = 3, 
                   backoff = @Backoff(delay = 1000))
        public FeeAssessmentResult process(FeeAssessmentAccount account) throws Exception {
            
            try {
                // Initialize fee assessment result
                FeeAssessmentResult result = new FeeAssessmentResult();
                result.setAccountId(account.getAccountId());
                result.setCardNum(account.getCardNum());
                result.setProcessingTimestamp(LocalDateTime.now());
                
                // Determine applicable fee based on account type and fee schedule
                BigDecimal feeAmount = calculateFeeAmount(account);
                
                // Check waiver conditions
                if (isWaiverApplicable(account, feeAmount)) {
                    result.setFeeAmount(BigDecimal.ZERO);
                    result.setWaiverApplied(true);
                    result.setWaiverReason(determineWaiverReason(account));
                } else {
                    result.setFeeAmount(feeAmount);
                    result.setWaiverApplied(false);
                }
                
                // Generate transaction details only if fee is assessed
                if (result.getFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
                    populateTransactionDetails(result, account);
                }
                
                return result;
                
            } catch (Exception e) {
                // Log error and determine if retryable
                if (isRetryableError(e)) {
                    throw new FeeAssessmentRetryableException("Retryable error processing account: " + 
                                                             account.getAccountId(), e);
                } else {
                    throw new FeeAssessmentException("Non-retryable error processing account: " + 
                                                   account.getAccountId(), e);
                }
            }
        }

        /**
         * Calculate fee amount based on account type and fee schedule.
         * 
         * Implements fee calculation logic equivalent to the COBOL computation
         * patterns, using BigDecimal for precise decimal arithmetic matching
         * COBOL COMP-3 packed decimal behavior.
         */
        private BigDecimal calculateFeeAmount(FeeAssessmentAccount account) {
            // Fee schedule lookup based on account group and fee type
            Map<String, BigDecimal> feeSchedule = getFeeSchedule(account.getAccountGroupId());
            
            BigDecimal baseFee = feeSchedule.getOrDefault(account.getFeeType(), BigDecimal.ZERO);
            
            // Apply balance-based fee adjustments
            if (account.getCurrentBalance().compareTo(new BigDecimal("2500.00")) < 0) {
                // Low balance fee
                baseFee = baseFee.add(new BigDecimal("15.00"));
            }
            
            // Apply transaction-based fee adjustments
            if (account.getMonthlyTransactionCount() > 10) {
                // Excess transaction fee
                int excessTxns = account.getMonthlyTransactionCount() - 10;
                BigDecimal excessFee = new BigDecimal("1.50").multiply(new BigDecimal(excessTxns));
                baseFee = baseFee.add(excessFee);
            }
            
            // Ensure precision matches COBOL COMP-3 (2 decimal places)
            return baseFee.setScale(2, RoundingMode.HALF_UP);
        }

        /**
         * Check if fee waiver conditions apply.
         * 
         * Evaluates waiver conditions based on account characteristics,
         * balance requirements, and relationship banking criteria.
         */
        private boolean isWaiverApplicable(FeeAssessmentAccount account, BigDecimal feeAmount) {
            // Premium account waiver
            if ("PREMIUM".equals(account.getAccountGroupId())) {
                return true;
            }
            
            // High balance waiver
            if (account.getCurrentBalance().compareTo(new BigDecimal("10000.00")) >= 0) {
                return true;
            }
            
            // Student account waiver
            if ("STUDENT".equals(account.getAccountGroupId()) && 
                account.getCustomerAge() < 26) {
                return true;
            }
            
            // Senior citizen waiver
            if (account.getCustomerAge() >= 65) {
                return true;
            }
            
            return false;
        }

        /**
         * Determine the reason for fee waiver.
         */
        private String determineWaiverReason(FeeAssessmentAccount account) {
            if ("PREMIUM".equals(account.getAccountGroupId())) {
                return "Premium account holder";
            }
            if (account.getCurrentBalance().compareTo(new BigDecimal("10000.00")) >= 0) {
                return "Minimum balance requirement met";
            }
            if ("STUDENT".equals(account.getAccountGroupId()) && account.getCustomerAge() < 26) {
                return "Student account under age 26";
            }
            if (account.getCustomerAge() >= 65) {
                return "Senior citizen waiver";
            }
            return "Other waiver condition";
        }

        /**
         * Populate transaction details for fee assessment.
         * 
         * Generates transaction record details mirroring the COBOL pattern
         * of creating transaction records with proper ID generation and
         * audit information.
         */
        private void populateTransactionDetails(FeeAssessmentResult result, FeeAssessmentAccount account) {
            // Generate transaction ID similar to COBOL pattern
            String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String transactionId = datePrefix + String.format("%06d", 
                                  (int)(Math.random() * 999999));
            
            result.setTransactionId(transactionId);
            result.setTransactionTypeCd("01"); // Fee transaction type
            result.setTransactionCatCd("0007"); // Fee category code
            result.setTransactionSource("System");
            result.setTransactionDesc("Fee Assessment - " + account.getFeeType());
            result.setOrigTimestamp(LocalDateTime.now());
            result.setProcTimestamp(LocalDateTime.now());
        }

        /**
         * Get fee schedule based on account group.
         * 
         * Returns fee schedule mapping for different account types.
         * In production, this would typically be loaded from a database table.
         */
        private Map<String, BigDecimal> getFeeSchedule(String accountGroupId) {
            Map<String, BigDecimal> schedule = new HashMap<>();
            
            switch (accountGroupId) {
                case "BASIC":
                    schedule.put("MONTHLY", new BigDecimal("12.00"));
                    schedule.put("ANNUAL", new BigDecimal("120.00"));
                    schedule.put("OVERDRAFT", new BigDecimal("35.00"));
                    break;
                case "STANDARD":
                    schedule.put("MONTHLY", new BigDecimal("8.00"));
                    schedule.put("ANNUAL", new BigDecimal("80.00"));
                    schedule.put("OVERDRAFT", new BigDecimal("30.00"));
                    break;
                case "PREMIUM":
                    schedule.put("MONTHLY", BigDecimal.ZERO);
                    schedule.put("ANNUAL", BigDecimal.ZERO);
                    schedule.put("OVERDRAFT", new BigDecimal("25.00"));
                    break;
                case "STUDENT":
                    schedule.put("MONTHLY", BigDecimal.ZERO);
                    schedule.put("ANNUAL", BigDecimal.ZERO);
                    schedule.put("OVERDRAFT", new BigDecimal("20.00"));
                    break;
                default:
                    schedule.put("MONTHLY", new BigDecimal("15.00"));
                    schedule.put("ANNUAL", new BigDecimal("150.00"));
                    schedule.put("OVERDRAFT", new BigDecimal("40.00"));
            }
            
            return schedule;
        }

        /**
         * Determine if an error is retryable.
         */
        private boolean isRetryableError(Exception e) {
            // Database connection issues, temporary timeouts are retryable
            return e.getMessage() != null && 
                   (e.getMessage().contains("connection") || 
                    e.getMessage().contains("timeout") ||
                    e.getMessage().contains("deadlock"));
        }
    }

    /**
     * Account entity for fee assessment processing.
     * 
     * Represents account data required for fee assessment calculations,
     * mirroring the data structures from the COBOL program's working storage.
     */
    @Entity
    @Table(name = "account_data")
    public static class FeeAssessmentAccount {
        @Id
        @Column(name = "account_id")
        private Long accountId;
        
        @Column(name = "card_num")
        private String cardNum;
        
        @Column(name = "account_group_cd")
        private String accountGroupId;
        
        @Column(name = "current_balance")
        private BigDecimal currentBalance;
        
        @Column(name = "monthly_transaction_count")
        private int monthlyTransactionCount;
        
        @Column(name = "customer_age")
        private int customerAge;
        
        @Column(name = "fee_type")
        private String feeType;
        
        @Column(name = "last_fee_assessment_date")
        private LocalDate lastFeeAssessmentDate;

        // Getters and setters
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
        
        public String getCardNum() { return cardNum; }
        public void setCardNum(String cardNum) { this.cardNum = cardNum; }
        
        public String getAccountGroupId() { return accountGroupId; }
        public void setAccountGroupId(String accountGroupId) { this.accountGroupId = accountGroupId; }
        
        public BigDecimal getCurrentBalance() { return currentBalance; }
        public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
        
        public int getMonthlyTransactionCount() { return monthlyTransactionCount; }
        public void setMonthlyTransactionCount(int monthlyTransactionCount) { 
            this.monthlyTransactionCount = monthlyTransactionCount; 
        }
        
        public int getCustomerAge() { return customerAge; }
        public void setCustomerAge(int customerAge) { this.customerAge = customerAge; }
        
        public String getFeeType() { return feeType; }
        public void setFeeType(String feeType) { this.feeType = feeType; }
        
        public LocalDate getLastFeeAssessmentDate() { return lastFeeAssessmentDate; }
        public void setLastFeeAssessmentDate(LocalDate lastFeeAssessmentDate) { 
            this.lastFeeAssessmentDate = lastFeeAssessmentDate; 
        }
    }

    /**
     * Fee assessment result entity.
     * 
     * Represents the result of fee assessment processing including
     * fee amount, waiver information, and transaction details.
     */
    public static class FeeAssessmentResult {
        private Long accountId;
        private String cardNum;
        private BigDecimal feeAmount;
        private boolean waiverApplied;
        private String waiverReason;
        private String transactionId;
        private String transactionTypeCd;
        private String transactionCatCd;
        private String transactionSource;
        private String transactionDesc;
        private LocalDateTime origTimestamp;
        private LocalDateTime procTimestamp;
        private LocalDateTime processingTimestamp;

        // Getters and setters
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
        
        public String getCardNum() { return cardNum; }
        public void setCardNum(String cardNum) { this.cardNum = cardNum; }
        
        public BigDecimal getFeeAmount() { return feeAmount; }
        public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
        
        public boolean isWaiverApplied() { return waiverApplied; }
        public void setWaiverApplied(boolean waiverApplied) { this.waiverApplied = waiverApplied; }
        
        public String getWaiverReason() { return waiverReason; }
        public void setWaiverReason(String waiverReason) { this.waiverReason = waiverReason; }
        
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public String getTransactionTypeCd() { return transactionTypeCd; }
        public void setTransactionTypeCd(String transactionTypeCd) { this.transactionTypeCd = transactionTypeCd; }
        
        public String getTransactionCatCd() { return transactionCatCd; }
        public void setTransactionCatCd(String transactionCatCd) { this.transactionCatCd = transactionCatCd; }
        
        public String getTransactionSource() { return transactionSource; }
        public void setTransactionSource(String transactionSource) { this.transactionSource = transactionSource; }
        
        public String getTransactionDesc() { return transactionDesc; }
        public void setTransactionDesc(String transactionDesc) { this.transactionDesc = transactionDesc; }
        
        public LocalDateTime getOrigTimestamp() { return origTimestamp; }
        public void setOrigTimestamp(LocalDateTime origTimestamp) { this.origTimestamp = origTimestamp; }
        
        public LocalDateTime getProcTimestamp() { return procTimestamp; }
        public void setProcTimestamp(LocalDateTime procTimestamp) { this.procTimestamp = procTimestamp; }
        
        public LocalDateTime getProcessingTimestamp() { return processingTimestamp; }
        public void setProcessingTimestamp(LocalDateTime processingTimestamp) { 
            this.processingTimestamp = processingTimestamp; 
        }
    }

    /**
     * Repository interface for account data access.
     * 
     * Defines query methods for retrieving accounts requiring fee assessment.
     * Implementation would be provided by Spring Data JPA.
     */
    @Repository
    public interface AccountRepository extends JpaRepository<FeeAssessmentAccount, Long> {
        /**
         * Find accounts requiring fee assessment based on assessment date and fee type.
         * 
         * @param assessmentDate The date for fee assessment
         * @param feeType The type of fee to assess
         * @return List of accounts requiring fee assessment
         */
        java.util.List<FeeAssessmentAccount> findAccountsRequiringFeeAssessment(
            LocalDate assessmentDate, String feeType);
    }

    /**
     * Exception for non-retryable fee assessment errors.
     */
    public static class FeeAssessmentException extends RuntimeException {
        public FeeAssessmentException(String message) {
            super(message);
        }
        
        public FeeAssessmentException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception for retryable fee assessment errors.
     */
    public static class FeeAssessmentRetryableException extends RuntimeException {
        public FeeAssessmentRetryableException(String message) {
            super(message);
        }
        
        public FeeAssessmentRetryableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}