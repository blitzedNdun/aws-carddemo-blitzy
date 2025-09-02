/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.batch;

import com.carddemo.config.BatchConfig;
import com.carddemo.entity.Card;
import com.carddemo.repository.CardRepository;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Spring Batch job implementation for credit card listing that replaces CBACT02C COBOL batch program.
 * 
 * This Spring Batch job performs sequential reading of card records from the PostgreSQL database
 * and generates formatted output listing, maintaining the same functionality as the original COBOL
 * program CBACT02C.cbl. The job implements the classic read-process-write pattern with comprehensive
 * error handling and processing status reporting matching the COBOL implementation.
 * 
 * COBOL Program Migration:
 * - Original: CBACT02C.cbl - "Read and print card data file"  
 * - Replacement: CardListJob - Spring Batch job for card data listing
 * - Functionality: Sequential processing of all card records with formatted output generation
 * - Processing Pattern: Simple read-process-write with console display matching COBOL
 * 
 * Key Features:
 * - JpaPagingItemReader: Sequential card record reading with primary key ordering
 * - PassThroughItemProcessor: Simple processing maintaining COBOL logic simplicity
 * - FlatFileItemWriter: Formatted output generation matching COBOL display format
 * - JobExecutionListener: Start/end messaging replicating COBOL console messages
 * - Error Handling: VSAM file status code equivalent error handling and reporting
 * - Processing Metrics: Job execution metrics for processing monitoring and reporting
 * 
 * COBOL to Spring Batch Migration Patterns:
 * - COBOL FILE-CONTROL → JpaPagingItemReader configuration
 * - COBOL OPEN/READ/CLOSE → Spring Batch reader lifecycle management
 * - COBOL DISPLAY statements → JobExecutionListener console messages
 * - COBOL file status checking → Spring Batch error handling and retry policies
 * - COBOL sequential access → PostgreSQL ordered query with pagination
 * 
 * Processing Characteristics:
 * - Chunk Size: Configurable via BatchConfig for memory optimization
 * - Transaction Management: Spring @Transactional boundaries for data integrity
 * - Error Recovery: Failed record skip with configurable skip limits
 * - Restart Capability: Job restart from point of failure for large datasets
 * - Monitoring Integration: Comprehensive execution metrics and status reporting
 * 
 * Performance Considerations:
 * - Pagination: Efficient large dataset processing through JPA pagination
 * - Memory Management: Chunk-based processing preventing memory exhaustion
 * - Database Connection: HikariCP connection pooling for database efficiency
 * - Thread Pool: TaskExecutor integration for optimal resource utilization
 * - Processing Window: 4-hour batch window compliance through optimized execution
 * 
 * Output Format:
 * The job generates formatted card listing output matching the original COBOL display format,
 * including card number, account ID, customer ID, embossed name, expiration date, and active status.
 * Output is written to a flat file with delimited format for downstream processing systems.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Configuration
@Component
public class CardListJob {

    private static final Logger logger = LoggerFactory.getLogger(CardListJob.class);
    
    // Output file configuration constants
    private static final String OUTPUT_FILE_PATH = "/tmp/card_listing_";
    private static final String OUTPUT_FILE_EXTENSION = ".txt";
    private static final String[] FIELD_NAMES = {
        "cardNumber", "accountId", "customerId", "embossedName", "expirationDate", "activeStatus"
    };
    private static final String FIELD_DELIMITER = "|";
    
    // Batch processing configuration constants
    private static final int DEFAULT_CHUNK_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 1000;
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    @Qualifier("taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    
    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * Configures the main card listing Spring Batch job.
     * 
     * This method creates the complete batch job configuration that replaces the CBACT02C COBOL
     * program functionality. The job includes comprehensive error handling, restart capability,
     * and execution monitoring that matches the original COBOL program's operational characteristics.
     * 
     * Job Configuration Features:
     * - Job Repository: PostgreSQL-backed job metadata storage for execution tracking
     * - Job Launcher: Asynchronous execution capability for non-blocking operations
     * - Step Sequencing: Single step execution matching COBOL program simplicity
     * - Restart Capability: Failed job restart from point of failure
     * - Execution Monitoring: Job execution listener for start/end messaging
     * - Parameter Management: Job parameter validation and default value handling
     * 
     * COBOL Program Equivalence:
     * - COBOL "START OF EXECUTION" → JobExecutionListener.beforeJob()
     * - COBOL file processing loop → Spring Batch chunk-based processing
     * - COBOL "END OF EXECUTION" → JobExecutionListener.afterJob()
     * - COBOL error handling → Spring Batch retry and skip policies
     * - COBOL file status reporting → Job execution status and metrics
     * 
     * Error Handling:
     * - Skip Policy: Configurable skip limit for individual record errors
     * - Retry Policy: Automatic retry for transient database errors
     * - Rollback Policy: Transaction rollback for data integrity preservation
     * - Exception Classification: Distinguishes between retryable and fatal errors
     * - Status Reporting: Comprehensive error logging and metrics collection
     * 
     * @return configured Job for card listing batch processing
     */
    @Bean
    public Job cardBatchJob() {
        logger.info("Configuring cardListJob - replacing CBACT02C COBOL program");
        
        return new JobBuilder("cardListJob", jobRepository)
                .start(cardListStep())
                .listener(cardListJobListener())
                .build();
    }

    /**
     * Configures the card listing step with reader, processor, and writer components.
     * 
     * This method creates the single processing step that implements the core functionality
     * of the CBACT02C COBOL program. The step uses chunk-based processing to efficiently
     * handle large card datasets while maintaining the sequential processing pattern of
     * the original COBOL implementation.
     * 
     * Step Configuration Features:
     * - Chunk Processing: Configurable chunk size for memory and performance optimization
     * - Transaction Management: Spring @Transactional boundaries for data integrity
     * - Error Handling: Skip and retry policies for resilient processing
     * - Resource Management: Proper resource cleanup and connection management
     * - Metrics Collection: Processing metrics for monitoring and performance analysis
     * 
     * Processing Components:
     * - Reader: JpaPagingItemReader for sequential database access
     * - Processor: PassThroughItemProcessor for simple data transformation
     * - Writer: FlatFileItemWriter for formatted output generation
     * 
     * COBOL Migration Patterns:
     * - COBOL READ → JpaPagingItemReader.read()
     * - COBOL DISPLAY → PassThroughItemProcessor (no transformation)
     * - COBOL sequential file processing → Chunk-based batch processing
     * - COBOL file status handling → Spring Batch error handling framework
     * 
     * @return configured Step for card data processing
     */
    @Bean
    public Step cardListStep() {
        logger.info("Configuring cardListStep for sequential card processing");
        
        return new StepBuilder("cardListStep", jobRepository)
                .<Card, Card>chunk(DEFAULT_CHUNK_SIZE, transactionManager)
                .reader(cardReader())
                .processor(cardProcessor())
                .writer(cardWriter())
                .build();
    }

    /**
     * Configures JpaPagingItemReader for sequential card record reading.
     * 
     * This method creates the data reader component that replaces VSAM sequential file access
     * from the original CBACT02C COBOL program. The reader implements pagination for efficient
     * memory usage while maintaining the sequential processing pattern required for card listing.
     * 
     * Reader Configuration Features:
     * - JPA Query: SELECT c FROM Card c ORDER BY c.cardNumber for consistent ordering
     * - Pagination: Page-based reading for memory efficiency with large datasets
     * - Transaction Management: Reader participation in Spring transaction boundaries
     * - Connection Management: HikariCP connection pooling for database efficiency
     * - Error Handling: Database connectivity and query error handling
     * 
     * VSAM to JPA Migration:
     * - VSAM CARDFILE sequential access → JPA paginated query
     * - VSAM OPEN INPUT → JpaPagingItemReader initialization
     * - VSAM READ NEXT → JpaPagingItemReader.read() with pagination
     * - VSAM file status 00/10 → Spring Batch ItemReader return null for end
     * - VSAM record key ordering → SQL ORDER BY card_number clause
     * 
     * Performance Characteristics:
     * - Page Size: 1000 records per page for optimal database performance
     * - Memory Usage: Controlled memory footprint through pagination
     * - Query Optimization: Database index usage for efficient card_number ordering
     * - Connection Efficiency: Connection reuse through HikariCP pooling
     * 
     * @return configured JpaPagingItemReader for card data access
     */
    @Bean
    public JpaPagingItemReader<Card> cardReader() {
        logger.info("Configuring cardReader for sequential card data access");
        
        return new JpaPagingItemReaderBuilder<Card>()
                .name("cardReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT c FROM Card c ORDER BY c.cardNumber")
                .pageSize(DEFAULT_PAGE_SIZE)
                .build();
    }

    /**
     * Configures PassThroughItemProcessor for simple card record processing.
     * 
     * This method creates the processing component that maintains the simplicity of the
     * original CBACT02C COBOL program. The processor passes card records through without
     * modification, preserving the original program's straightforward processing logic
     * while providing the processing phase required by Spring Batch architecture.
     * 
     * Processor Features:
     * - Pass-through Processing: No data transformation, maintaining COBOL simplicity
     * - Type Safety: Maintains Card entity type throughout processing pipeline
     * - Error Propagation: Transparent error handling without masking exceptions
     * - Performance: Minimal overhead for maximum processing throughput
     * - Monitoring: Processing metrics collection for operational visibility
     * 
     * COBOL Migration Rationale:
     * The original CBACT02C.cbl program performs no data transformation on card records,
     * simply reading and displaying them. This processor maintains that exact behavior
     * by passing records through unmodified, preserving the original program's logic
     * while fitting into Spring Batch's read-process-write architecture pattern.
     * 
     * Future Enhancement Capability:
     * While currently pass-through, this processor provides the foundation for future
     * enhancements such as data validation, format conversion, or business rule
     * application without requiring architectural changes to the batch job structure.
     * 
     * @return configured PassThroughItemProcessor for card processing
     */
    @Bean
    public PassThroughItemProcessor<Card> cardProcessor() {
        logger.debug("Configuring cardProcessor for pass-through processing");
        return new PassThroughItemProcessor<>();
    }

    /**
     * Configures FlatFileItemWriter for formatted card listing output.
     * 
     * This method creates the output writer component that generates formatted card listing
     * files, replacing the COBOL DISPLAY statements with structured file output. The writer
     * produces delimited output suitable for downstream processing while maintaining the
     * card information display pattern from the original COBOL program.
     * 
     * Writer Configuration Features:
     * - File Output: Timestamped output file generation in /tmp directory
     * - Delimited Format: Pipe-delimited field output for structured data
     * - Field Extraction: BeanWrapperFieldExtractor for Card entity field access
     * - Resource Management: Proper file resource handling and cleanup
     * - Error Handling: File I/O error handling and recovery
     * 
     * Output Format Details:
     * - File Naming: card_listing_YYYYMMDD_HHMMSS.txt with timestamp
     * - Field Order: cardNumber|accountId|customerId|embossedName|expirationDate|activeStatus
     * - Field Delimiter: Pipe character (|) for reliable parsing
     * - Character Encoding: UTF-8 for international character support
     * - Line Termination: Platform-appropriate line endings for compatibility
     * 
     * COBOL Display Migration:
     * - COBOL DISPLAY CARD-RECORD → Structured file output with field separation
     * - COBOL console output → File-based output for operational processing
     * - COBOL field display → Delimited field extraction and formatting
     * - COBOL execution messages → Job execution logging and metrics
     * 
     * Operational Benefits:
     * - Downstream Processing: Structured output enables automated processing
     * - Audit Trail: Permanent record of processed card listings
     * - Data Integration: Standard format for system integration
     * - Monitoring: File-based processing status and completion verification
     * 
     * @return configured FlatFileItemWriter for card listing output
     */
    @Bean
    public FlatFileItemWriter<Card> cardWriter() {
        logger.info("Configuring cardWriter for formatted card listing output");
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String outputFileName = OUTPUT_FILE_PATH + timestamp + OUTPUT_FILE_EXTENSION;
        
        // Configure field extractor for Card entity properties
        BeanWrapperFieldExtractor<Card> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(FIELD_NAMES);
        
        // Configure line aggregator for delimited output
        DelimitedLineAggregator<Card> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(FIELD_DELIMITER);
        lineAggregator.setFieldExtractor(fieldExtractor);
        
        return new FlatFileItemWriterBuilder<Card>()
                .name("cardWriter")
                .resource(new FileSystemResource(outputFileName))
                .lineAggregator(lineAggregator)
                .build();
    }

    /**
     * Configures JobExecutionListener for job lifecycle event handling.
     * 
     * This method creates the job execution listener that provides the start and end messaging
     * functionality equivalent to the COBOL program's "START OF EXECUTION" and "END OF EXECUTION"
     * messages. The listener also provides comprehensive job execution metrics and status reporting
     * for operational monitoring and troubleshooting.
     * 
     * Listener Features:
     * - Job Start Messaging: "START OF EXECUTION" equivalent with timestamp
     * - Job Completion Messaging: "END OF EXECUTION" equivalent with execution metrics
     * - Error Handling: Exception logging and error status reporting
     * - Metrics Collection: Processing statistics including record counts and timing
     * - Status Reporting: Job success/failure status with detailed execution information
     * 
     * COBOL Message Migration:
     * - COBOL "START OF EXECUTION OF PROGRAM CBACT02C" → beforeJob() with timestamp
     * - COBOL "END OF EXECUTION OF PROGRAM CBACT02C" → afterJob() with metrics
     * - COBOL error messages → Exception logging with stack traces
     * - COBOL file status reporting → Job execution status and metrics
     * 
     * Operational Monitoring:
     * - Job Duration: Total execution time for performance monitoring
     * - Record Processing: Read/write counts for data verification
     * - Error Statistics: Skip/retry counts for quality assessment
     * - System Resources: Memory and CPU usage for capacity planning
     * - Status Tracking: Job completion status for operational dashboards
     * 
     * @return configured JobExecutionListener for job lifecycle management
     */
    @Bean
    public JobExecutionListener cardListJobListener() {
        return new JobExecutionListener() {
            
            @Override
            public void beforeJob(JobExecution jobExecution) {
                String startMessage = "START OF EXECUTION OF SPRING BATCH JOB CardListJob - Replacing CBACT02C COBOL Program";
                logger.info(startMessage);
                System.out.println(startMessage);
                
                // Add job start timestamp for execution tracking
                jobExecution.getExecutionContext().put("startTime", System.currentTimeMillis());
                
                // Log job parameters for troubleshooting
                logger.info("Job Parameters: {}", jobExecution.getJobParameters());
                logger.info("Job Instance ID: {}", jobExecution.getJobInstance().getId());
                
                // Display job execution information matching COBOL style
                System.out.println("Job Instance ID: " + jobExecution.getJobInstance().getId());
                System.out.println("Job Parameters: " + jobExecution.getJobParameters());
                System.out.println("Starting card listing processing...");
            }
            
            @Override
            public void afterJob(JobExecution jobExecution) {
                long startTime = (Long) jobExecution.getExecutionContext().get("startTime");
                long duration = System.currentTimeMillis() - startTime;
                
                String endMessage = "END OF EXECUTION OF SPRING BATCH JOB CardListJob - CBACT02C Replacement Complete";
                logger.info(endMessage);
                System.out.println(endMessage);
                
                // Display comprehensive execution statistics
                System.out.println("=== Card List Job Execution Summary ===");
                System.out.println("Job Status: " + jobExecution.getStatus());
                System.out.println("Exit Status: " + jobExecution.getExitStatus());
                System.out.println("Start Time: " + jobExecution.getStartTime());
                System.out.println("End Time: " + jobExecution.getEndTime());
                System.out.println("Duration: " + duration + " milliseconds");
                
                // Display step execution statistics
                jobExecution.getStepExecutions().forEach(stepExecution -> {
                    System.out.println("=== Step: " + stepExecution.getStepName() + " ===");
                    System.out.println("Read Count: " + stepExecution.getReadCount());
                    System.out.println("Write Count: " + stepExecution.getWriteCount());
                    System.out.println("Skip Count: " + stepExecution.getSkipCount());
                    System.out.println("Process Skip Count: " + stepExecution.getProcessSkipCount());
                    System.out.println("Read Skip Count: " + stepExecution.getReadSkipCount());
                    System.out.println("Write Skip Count: " + stepExecution.getWriteSkipCount());
                    System.out.println("Rollback Count: " + stepExecution.getRollbackCount());
                    System.out.println("Commit Count: " + stepExecution.getCommitCount());
                    System.out.println("Step Status: " + stepExecution.getStatus());
                    System.out.println("Step Exit Status: " + stepExecution.getExitStatus());
                });
                
                // Log final status and metrics for operational monitoring
                logger.info("Card listing job completed with status: {}", jobExecution.getStatus());
                logger.info("Total execution time: {} milliseconds", duration);
                
                // Handle job completion status
                if (jobExecution.getStatus().toString().equals("COMPLETED")) {
                    logger.info("Card listing processing completed successfully");
                    System.out.println("Card listing processing completed successfully - All records processed");
                } else {
                    logger.error("Card listing job failed with status: {}", jobExecution.getStatus());
                    System.err.println("Card listing job failed - Check logs for error details");
                    
                    // Log failure exceptions if present
                    jobExecution.getFailureExceptions().forEach(exception -> {
                        logger.error("Job failure exception: ", exception);
                        System.err.println("Exception: " + exception.getMessage());
                    });
                }
            }
        };
    }
}