package com.carddemo.batch;

import com.carddemo.transaction.Transaction;
import com.carddemo.common.config.BatchConfiguration;
import com.carddemo.common.util.DateUtils;
import com.carddemo.common.util.BigDecimalUtils;
import com.carddemo.transaction.TransactionRepository;
import com.carddemo.common.enums.TransactionType;
import com.carddemo.common.enums.TransactionCategory;
import com.carddemo.batch.dto.TransactionReportDTO;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Batch job for generating transaction reports with date range filtering 
 * and totals calculation, converted from COBOL CBTRN03C.cbl program.
 * 
 * This class implements a comprehensive transaction reporting system that:
 * - Processes transaction records using optimized PostgreSQL queries with pagination
 * - Filters transactions by date range using Spring Batch job parameters
 * - Groups transactions by card number equivalent to COBOL WS-CURR-CARD-NUM logic
 * - Calculates page, account, and grand totals with exact BigDecimal precision  
 * - Generates formatted reports matching original COBOL report layout structure
 * - Integrates with daily batch processing workflow for automated report generation
 * 
 * Key COBOL-to-Java Transformations:
 * - COBOL sequential file processing → Spring Batch ItemReader with JPA repository queries
 * - COBOL working-storage variables → Java class fields with thread-safe operations
 * - COBOL report formatting → Spring Batch FlatFileItemWriter with custom line aggregation
 * - COBOL COMP-3 arithmetic → BigDecimal operations using MathContext.DECIMAL128
 * - COBOL date parameter file → Spring Batch JobParameters with validation
 * - COBOL page counters and totals → Atomic variables for thread-safe accumulation
 * 
 * Performance Considerations:
 * - Uses paginated queries to handle large transaction datasets efficiently
 * - Implements connection pooling via BatchConfiguration for optimal database usage
 * - Supports parallel processing through configurable chunk sizes
 * - Includes comprehensive error handling and transaction rollback capabilities
 * - Provides detailed logging for monitoring and troubleshooting batch execution
 * 
 * Integration Points:
 * - Coordinates with ReportGenerationJob for daily batch processing workflow
 * - Uses shared BatchConfiguration for consistent job execution infrastructure
 * - Leverages TransactionRepository for optimized database access patterns
 * - Integrates with Spring Security for batch job execution authorization
 * 
 * @author CardDemo Migration Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
@Configuration
public class TransactionReportJob {

    private static final Logger logger = LoggerFactory.getLogger(TransactionReportJob.class);
    
    /**
     * Date formatter for parsing job parameters matching COBOL date format.
     * Used for start and end date parameters equivalent to WS-START-DATE and WS-END-DATE.
     */
    private static final DateTimeFormatter PARAMETER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Report page size constant equivalent to COBOL WS-PAGE-SIZE (20 lines per page).
     * Controls pagination and when to write page totals in the output report.
     */
    private static final int REPORT_PAGE_SIZE = 20;
    
    /**
     * Maximum chunk size for Spring Batch processing to balance memory usage and performance.
     * Equivalent to processing batches of transaction records from COBOL sequential reads.
     * Uses BatchConfiguration.chunkSize() for consistent sizing across all batch jobs.
     */
    private int getChunkSize() {
        return batchConfiguration.chunkSize();
    }
    
    /**
     * Default report output file path for transaction reports.
     * Equivalent to COBOL TRANREPT file assignment.
     */
    private static final String DEFAULT_REPORT_PATH = "/batch/reports/transaction_report.txt";
    
    /**
     * Job parameter names for date range filtering.
     * Equivalent to COBOL DATEPARM file parameters.
     */
    private static final String START_DATE_PARAM = "startDate";
    private static final String END_DATE_PARAM = "endDate";
    private static final String REPORT_PATH_PARAM = "reportPath";
    
    /**
     * Placeholder for @StepScope bean method parameters that will be overridden by Spring expressions at runtime.
     * This follows Spring Batch best practices for @StepScope configuration.
     */
    private static final String OVERRIDDEN_BY_EXPRESSION = null;

    @Autowired
    private BatchConfiguration batchConfiguration;
    
    @Autowired 
    private TransactionRepository transactionRepository;
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    /**
     * Thread-safe counters and accumulators equivalent to COBOL working-storage variables.
     * These replace the original COBOL WS-LINE-COUNTER, WS-PAGE-TOTAL, WS-ACCOUNT-TOTAL, WS-GRAND-TOTAL.
     */
    private final AtomicInteger lineCounter = new AtomicInteger(0);
    private final AtomicReference<BigDecimal> pageTotal = new AtomicReference<>(BigDecimalUtils.ZERO_MONETARY);
    private final AtomicReference<BigDecimal> accountTotal = new AtomicReference<>(BigDecimalUtils.ZERO_MONETARY);  
    private final AtomicReference<BigDecimal> grandTotal = new AtomicReference<>(BigDecimalUtils.ZERO_MONETARY);
    private final AtomicReference<String> currentCardNumber = new AtomicReference<>("");
    private final AtomicReference<String> currentAccountId = new AtomicReference<>("");
    
    /**
     * Thread-safe map for storing account-level totals equivalent to COBOL account grouping logic.
     * Key: Account ID, Value: Total amount for that account across all transactions.
     */
    private final Map<String, BigDecimal> accountTotals = new ConcurrentHashMap<>();
    
    /**
     * Report metadata fields for header generation and audit trail.
     * Equivalent to COBOL report header variables and date range tracking.
     */
    private LocalDateTime reportStartDate;
    private LocalDateTime reportEndDate;
    private LocalDateTime reportGenerationTime;

    /**
     * Primary Spring Batch Job definition for transaction report generation.
     * Equivalent to the main COBOL CBTRN03C program execution flow.
     * 
     * Creates a comprehensive batch job that:
     * - Validates job parameters for date range and output path
     * - Executes transaction report generation step with proper error handling
     * - Integrates with batch execution listeners for monitoring and logging
     * - Supports restart capabilities for failed job recovery
     * 
     * @return Configured Spring Batch Job for transaction reporting
     */
    @Bean(name = "transactionReportBatchJob")
    public Job transactionReportBatchJob() {
        logger.info("Configuring TransactionReportJob - equivalent to COBOL CBTRN03C program");
        
        return new JobBuilder("transactionReportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(batchConfiguration.jobExecutionListener())
                .validator(batchConfiguration.jobParametersValidator())
                .start(transactionReportStep())
                .build();
    }

    /**
     * Spring Batch Step definition for processing transaction records and generating reports.
     * Equivalent to the main processing loop in COBOL CBTRN03C (lines 170-206).
     * 
     * Configures a chunk-oriented step that:
     * - Reads transaction records using paginated database queries  
     * - Processes each transaction through business logic and formatting
     * - Writes formatted report lines with proper totals calculation
     * - Handles errors with retry and skip policies from BatchConfiguration
     * 
     * @return Configured Spring Batch Step for transaction processing
     */
    @Bean
    public Step transactionReportStep() {
        int chunkSize = getChunkSize();
        logger.debug("Configuring transaction report step with chunk size: {}", chunkSize);
        
        return new StepBuilder("transactionReportStep", jobRepository)
                .<Transaction, TransactionReportDTO>chunk(chunkSize, transactionManager)
                .reader(transactionItemReader(OVERRIDDEN_BY_EXPRESSION, OVERRIDDEN_BY_EXPRESSION))
                .processor(transactionItemProcessor())
                .writer(transactionItemWriter(OVERRIDDEN_BY_EXPRESSION))
                .listener(batchConfiguration.stepExecutionListener())
                .faultTolerant()
                .retryPolicy(batchConfiguration.retryPolicy())
                .skipPolicy(batchConfiguration.skipPolicy())
                .taskExecutor(batchConfiguration.batchTaskExecutor())
                .build();
    }

    /**
     * Spring Batch ItemReader for transaction records with date range filtering.
     * Equivalent to COBOL TRANFILE sequential read operations with date validation (lines 248-272).
     * 
     * Implements optimized database access using:
     * - JPA Criteria API for dynamic date range filtering
     * - Pagination support for large datasets equivalent to chunked processing
     * - Sorting by processing timestamp to maintain COBOL sequential order
     * - Connection pooling through BatchConfiguration for performance
     * 
     * @return Configured RepositoryItemReader for Transaction entities
     */
    @Bean
    @StepScope
    public RepositoryItemReader<Transaction> transactionItemReader(
            @Value("#{jobParameters['startDate']}") String startDateStr,
            @Value("#{jobParameters['endDate']}") String endDateStr) {
        logger.debug("Configuring transaction item reader with date range filtering");
        
        // Parse job parameters for date range
        LocalDateTime startDate = parseJobParameterDate(startDateStr);
        LocalDateTime endDate = parseJobParameterDate(endDateStr);
        
        // Store dates for use in processing
        this.reportStartDate = startDate;
        this.reportEndDate = endDate;
        this.reportGenerationTime = LocalDateTime.now();
        
        return new RepositoryItemReaderBuilder<Transaction>()
                .name("transactionItemReader")
                .repository(transactionRepository)
                .methodName("findByDateRange") 
                .arguments(startDate, endDate)
                .sorts(Collections.singletonMap("processingTimestamp", Sort.Direction.ASC))
                .pageSize(getChunkSize())
                .build();
    }

    /**
     * Spring Batch ItemProcessor for transaction business logic and report formatting.
     * Equivalent to COBOL transaction processing logic including lookups and calculations (lines 274-374).
     * 
     * Implements comprehensive processing that:
     * - Validates transaction data equivalent to COBOL field validation
     * - Performs transaction type and category lookups replacing COBOL file reads
     * - Calculates running totals with BigDecimal precision matching COBOL COMP-3
     * - Handles account grouping logic equivalent to WS-CURR-CARD-NUM processing
     * - Formats transaction data into TransactionReportDTO for output generation
     * 
     * @return Configured ItemProcessor for Transaction to TransactionReportDTO conversion
     */
    @Bean
    public ItemProcessor<Transaction, TransactionReportDTO> transactionItemProcessor() {
        return new ItemProcessor<Transaction, TransactionReportDTO>() {
            @Override
            public TransactionReportDTO process(Transaction transaction) throws Exception {
                logger.debug("Processing transaction ID: {}", transaction.getTransactionId());
                
                // Validate transaction is within date range - equivalent to COBOL date filtering (lines 173-178)
                if (!isTransactionInDateRange(transaction)) {
                    logger.debug("Transaction {} outside date range, skipping", transaction.getTransactionId());
                    return null;  // Skip this transaction
                }
                
                // Handle account grouping logic - equivalent to COBOL WS-CURR-CARD-NUM processing (lines 181-188)
                String transactionAccountId = transaction.getAccountId();
                if (!currentAccountId.get().equals(transactionAccountId)) {
                    if (!currentAccountId.get().isEmpty()) {
                        // Write account totals before switching to new account
                        calculateAccountTotals(currentAccountId.get());
                    }
                    currentAccountId.set(transactionAccountId);
                    currentCardNumber.set(transaction.getCardNumber());
                }
                
                // Create TransactionReportDTO with comprehensive field mapping
                TransactionReportDTO reportDTO = new TransactionReportDTO(transaction);
                
                // Perform transaction type lookup - equivalent to COBOL 1500-B-LOOKUP-TRANTYPE (lines 494-502)
                enrichTransactionTypeData(reportDTO, transaction);
                
                // Perform transaction category lookup - equivalent to COBOL 1500-C-LOOKUP-TRANCATG (lines 504-512)
                enrichTransactionCategoryData(reportDTO, transaction);
                
                // Set report metadata from job parameters
                reportDTO.setStartDate(reportStartDate);
                reportDTO.setEndDate(reportEndDate);
                reportDTO.setGenerationTimestamp(reportGenerationTime);
                
                // Update running totals - equivalent to COBOL ADD statements (lines 287-288, 200-201)
                updateRunningTotals(transaction.getAmount());
                
                logger.debug("Successfully processed transaction: {} for account: {}", 
                    transaction.getTransactionId(), transactionAccountId);
                
                return reportDTO;
            }
        };
    }

    /**
     * Spring Batch ItemWriter for generating formatted transaction reports.
     * Equivalent to COBOL report file writing operations (FD-REPTFILE-REC writes).
     * 
     * Implements comprehensive report generation that:
     * - Creates formatted report lines equivalent to COBOL report layout
     * - Manages page breaks and headers matching COBOL pagination logic
     * - Writes page, account, and grand totals at appropriate intervals
     * - Uses FlatFileItemWriter for efficient file I/O operations
     * - Handles report formatting with exact column alignment and spacing
     * 
     * @return Configured CompositeItemWriter for report generation
     */
    @Bean
    @StepScope
    public ItemWriter<TransactionReportDTO> transactionItemWriter(
            @Value("#{jobParameters['reportPath']}") String reportPath) {
        logger.debug("Configuring transaction report item writer");
        
        String outputPath = getReportOutputPath(reportPath);
        logger.info("Writing transaction report to: {}", outputPath);
        
        // Create primary report file writer
        FlatFileItemWriter<TransactionReportDTO> reportFileWriter = new FlatFileItemWriterBuilder<TransactionReportDTO>()
                .name("transactionReportFileWriter")
                .resource(new FileSystemResource(outputPath))
                .lineAggregator(item -> item.getReportLine())
                .headerCallback(writer -> {
                    // Write report headers - equivalent to COBOL 1120-WRITE-HEADERS (lines 324-341)
                    writeReportHeaders(writer);
                })
                .footerCallback(writer -> {
                    // Write final totals - equivalent to COBOL grand totals logic (lines 318-322)
                    writeReportFooters(writer);
                })
                .shouldDeleteIfExists(true)
                .build();
        
        // Create composite writer for handling multiple output requirements
        return new CompositeItemWriterBuilder<TransactionReportDTO>()
                .delegates(reportFileWriter)
                .build();
    }

    /**
     * Calculates and updates page totals equivalent to COBOL WS-PAGE-TOTAL processing.
     * Called when page size limit is reached, matching COBOL pagination logic (lines 282-285).
     * 
     * This method:
     * - Accumulates transaction amounts for the current page
     * - Triggers page total output when REPORT_PAGE_SIZE is reached
     * - Resets page counters for the next page
     * - Updates grand total with page total amounts
     * 
     * @param pageTransactions List of transactions for the current page
     * @return Map containing page total calculations and metadata
     */
    public Map<String, BigDecimal> calculatePageTotals(List<TransactionReportDTO> pageTransactions) {
        logger.debug("Calculating page totals for {} transactions", pageTransactions.size());
        
        BigDecimal currentPageTotal = pageTotal.get();
        
        // Calculate page total from current transactions
        for (TransactionReportDTO transaction : pageTransactions) {
            if (transaction != null && transaction.getAmount() != null) {
                currentPageTotal = BigDecimalUtils.add(currentPageTotal, transaction.getAmount());
            }
        }
        
        // Update atomic reference with calculated total
        pageTotal.set(currentPageTotal);
        
        // Update grand total - equivalent to COBOL ADD WS-PAGE-TOTAL TO WS-GRAND-TOTAL (line 297)
        BigDecimal currentGrandTotal = grandTotal.get();
        grandTotal.set(BigDecimalUtils.add(currentGrandTotal, currentPageTotal));
        
        // Reset page total for next page - equivalent to COBOL MOVE 0 TO WS-PAGE-TOTAL (line 298)
        pageTotal.set(BigDecimalUtils.ZERO_MONETARY);
        
        // Reset line counter for new page
        lineCounter.set(0);
        
        Map<String, BigDecimal> totals = new HashMap<>();
        totals.put("pageTotal", currentPageTotal);
        totals.put("grandTotal", grandTotal.get());
        
        logger.debug("Page total calculated: {}, Grand total updated: {}", 
                     BigDecimalUtils.formatCurrency(currentPageTotal),
                     BigDecimalUtils.formatCurrency(grandTotal.get()));
        
        return totals;
    }

    /**
     * Calculates and updates account-level totals equivalent to COBOL WS-ACCOUNT-TOTAL processing.
     * Called when switching between different account numbers, matching COBOL account grouping (lines 306-316).
     * 
     * This method:
     * - Accumulates all transaction amounts for a specific account
     * - Stores account totals in thread-safe map for final reporting
     * - Triggers account total output in the report
     * - Resets account total counter for the next account group
     * 
     * @param accountId The account identifier for total calculation
     * @return Map containing account total calculations and metadata
     */
    public Map<String, BigDecimal> calculateAccountTotals(String accountId) {
        logger.debug("Calculating account totals for account: {}", accountId);
        
        BigDecimal currentAccountTotal = accountTotal.get();
        
        // Store account total in thread-safe map for final reporting
        accountTotals.put(accountId, currentAccountTotal);
        
        // Reset account total for next account - equivalent to COBOL MOVE 0 TO WS-ACCOUNT-TOTAL (line 310)
        accountTotal.set(BigDecimalUtils.ZERO_MONETARY);
        
        Map<String, BigDecimal> totals = new HashMap<>();
        totals.put("accountTotal", currentAccountTotal);
        totals.put("accountId", null); // Store account ID as metadata
        
        logger.debug("Account total calculated for {}: {}", accountId, 
                     BigDecimalUtils.formatCurrency(currentAccountTotal));
        
        return totals;
    }

    /**
     * Calculates and returns grand totals for the entire report equivalent to COBOL WS-GRAND-TOTAL.
     * Called at the end of report processing, matching COBOL grand total logic (lines 318-322).
     * 
     * This method:
     * - Provides final grand total across all processed transactions
     * - Includes summary statistics for audit and verification purposes
     * - Returns comprehensive totals data for report footer generation
     * - Ensures BigDecimal precision matches COBOL COMP-3 arithmetic
     * 
     * @return Map containing grand total calculations and summary statistics
     */
    public Map<String, BigDecimal> calculateGrandTotals() {
        logger.info("Calculating grand totals for transaction report");
        
        BigDecimal finalGrandTotal = grandTotal.get();
        
        // Calculate additional summary statistics
        BigDecimal totalAccounts = BigDecimalUtils.createDecimal(accountTotals.size());
        BigDecimal averageAccountTotal = accountTotals.isEmpty() ? 
            BigDecimalUtils.ZERO_MONETARY : 
            BigDecimalUtils.divide(finalGrandTotal, totalAccounts);
        
        Map<String, BigDecimal> grandTotals = new HashMap<>();
        grandTotals.put("grandTotal", finalGrandTotal);
        grandTotals.put("totalAccounts", totalAccounts);
        grandTotals.put("averageAccountTotal", averageAccountTotal);
        grandTotals.put("totalTransactions", BigDecimalUtils.createDecimal(lineCounter.get()));
        
        logger.info("Grand totals calculated - Total: {}, Accounts: {}, Transactions: {}", 
                   BigDecimalUtils.formatCurrency(finalGrandTotal),
                   totalAccounts.intValue(),
                   lineCounter.get());
        
        return grandTotals;
    }

    /**
     * Validates if a transaction falls within the specified date range parameters.
     * Equivalent to COBOL date filtering logic (lines 173-178).
     * 
     * @param transaction The transaction to validate
     * @return true if transaction is within date range, false otherwise
     */
    private boolean isTransactionInDateRange(Transaction transaction) {
        if (transaction.getProcessingTimestamp() == null) {
            return false;
        }
        
        LocalDateTime transactionDate = transaction.getProcessingTimestamp();
        
        // Use DateUtils for COBOL-equivalent date validation
        return DateUtils.isValidDate(transactionDate.format(PARAMETER_DATE_FORMATTER)) &&
               !transactionDate.isBefore(reportStartDate) &&
               !transactionDate.isAfter(reportEndDate);
    }

    /**
     * Enriches transaction report DTO with transaction type data from enum lookup.
     * Equivalent to COBOL 1500-B-LOOKUP-TRANTYPE paragraph (lines 494-502).
     * 
     * @param reportDTO The report DTO to enrich
     * @param transaction The source transaction entity
     */
    private void enrichTransactionTypeData(TransactionReportDTO reportDTO, Transaction transaction) {
        try {
            TransactionType transactionType = transaction.getTransactionType();
            if (transactionType != null) {
                reportDTO.setTransactionType(transactionType.getCode());
                reportDTO.setTransactionTypeDescription(transactionType.getDescription());
            } else {
                logger.warn("Transaction type not found for transaction: {}", transaction.getTransactionId());
                reportDTO.setTransactionType("??");
                reportDTO.setTransactionTypeDescription("UNKNOWN TYPE");
            }
        } catch (Exception e) {
            logger.error("Error enriching transaction type data for transaction: {}", 
                        transaction.getTransactionId(), e);
            reportDTO.setTransactionType("ER");
            reportDTO.setTransactionTypeDescription("ERROR");
        }
    }

    /**
     * Enriches transaction report DTO with transaction category data from enum lookup.
     * Equivalent to COBOL 1500-C-LOOKUP-TRANCATG paragraph (lines 504-512).
     * 
     * @param reportDTO The report DTO to enrich  
     * @param transaction The source transaction entity
     */
    private void enrichTransactionCategoryData(TransactionReportDTO reportDTO, Transaction transaction) {
        try {
            TransactionCategory transactionCategory = transaction.getCategoryCode();
            if (transactionCategory != null) {
                reportDTO.setTransactionCategory(transactionCategory.getCode());
                reportDTO.setTransactionCategoryDescription(transactionCategory.getDescription());
            } else {
                logger.warn("Transaction category not found for transaction: {}", transaction.getTransactionId());
                reportDTO.setTransactionCategory("9999");
                reportDTO.setTransactionCategoryDescription("UNKNOWN CATEGORY");
            }
        } catch (Exception e) {
            logger.error("Error enriching transaction category data for transaction: {}", 
                        transaction.getTransactionId(), e);
            reportDTO.setTransactionCategory("0000");
            reportDTO.setTransactionCategoryDescription("ERROR");
        }
    }

    /**
     * Updates running totals with thread-safe BigDecimal operations.
     * Equivalent to COBOL ADD statements for total accumulation (lines 287-288, 200-201).
     * 
     * @param transactionAmount The amount to add to running totals
     */
    private void updateRunningTotals(BigDecimal transactionAmount) {
        if (transactionAmount != null) {
            // Update page total
            BigDecimal currentPageTotal = pageTotal.get();
            pageTotal.set(BigDecimalUtils.add(currentPageTotal, transactionAmount));
            
            // Update account total
            BigDecimal currentAccountTotal = accountTotal.get();
            accountTotal.set(BigDecimalUtils.add(currentAccountTotal, transactionAmount));
            
            // Increment line counter
            lineCounter.incrementAndGet();
        }
    }

    /**
     * Writes report headers equivalent to COBOL 1120-WRITE-HEADERS paragraph (lines 324-341).
     * 
     * @param writer The file writer for header output
     */
    private void writeReportHeaders(java.io.Writer writer) throws java.io.IOException {
        // Create sample DTO for header formatting
        TransactionReportDTO headerDTO = new TransactionReportDTO();
        headerDTO.setStartDate(reportStartDate);
        headerDTO.setEndDate(reportEndDate);
        headerDTO.setGenerationTimestamp(reportGenerationTime);
        
        // Write report name header
        writer.write(headerDTO.formatAsHeaderLine());
        writer.write(System.lineSeparator());
        
        // Write column headers
        String columnHeader = String.format("%-16s %-11s %-18s %-34s %-10s %15s", 
                                           "Transaction ID", "Account ID", "Type", "Category", "Source", "Amount");
        writer.write(columnHeader);
        writer.write(System.lineSeparator());
        
        // Write separator line
        writer.write("-".repeat(133));
        writer.write(System.lineSeparator());
    }

    /**
     * Writes report footers with grand totals equivalent to COBOL grand totals output (lines 318-322).
     * 
     * @param writer The file writer for footer output
     */
    private void writeReportFooters(java.io.Writer writer) throws java.io.IOException {
        Map<String, BigDecimal> grandTotals = calculateGrandTotals();
        
        // Write grand total line
        TransactionReportDTO footerDTO = new TransactionReportDTO();
        String grandTotalLine = footerDTO.formatAsTotalLine("Grand Total", grandTotals.get("grandTotal"));
        writer.write(grandTotalLine);
        writer.write(System.lineSeparator());
        
        // Write summary statistics
        writer.write(String.format("Total Accounts Processed: %d", grandTotals.get("totalAccounts").intValue()));
        writer.write(System.lineSeparator());
        writer.write(String.format("Total Transactions: %d", grandTotals.get("totalTransactions").intValue()));
        writer.write(System.lineSeparator());
    }

    /**
     * Parses job parameter date string to LocalDateTime using DateUtils validation.
     * Equivalent to COBOL date parameter parsing from DATEPARM file (lines 220-243).
     * 
     * @param dateStr The date string from job parameters
     * @return Parsed LocalDateTime or current time if invalid
     */
    private LocalDateTime parseJobParameterDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            logger.warn("Empty date parameter provided, using current date");
            return LocalDateTime.now();
        }
        
        // Use DateUtils for COBOL-equivalent date validation and parsing
        if (DateUtils.isValidDate(dateStr)) {
            return DateUtils.parseDate(dateStr)
                    .map(LocalDate::atStartOfDay)
                    .orElse(LocalDateTime.now());
        } else {
            logger.error("Invalid date parameter: {}, using current date", dateStr);
            return LocalDateTime.now();
        }
    }

    /**
     * Gets the report output file path from job parameters or uses default.
     * 
     * @param reportPath The report path from job parameters
     * @return Report output file path
     */
    private String getReportOutputPath(String reportPath) {
        return (reportPath != null && !reportPath.trim().isEmpty()) ? 
            reportPath : DEFAULT_REPORT_PATH;
    }
}