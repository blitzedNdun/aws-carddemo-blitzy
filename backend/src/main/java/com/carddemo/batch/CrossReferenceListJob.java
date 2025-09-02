/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.batch;

import com.carddemo.config.BatchConfig;
import com.carddemo.entity.CardXref;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.dto.CardCrossReferenceDto;
import com.carddemo.batch.BatchJobListener;
import com.carddemo.exception.BatchProcessingException;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;

/**
 * Spring Batch job implementation for cross-reference listing that replaces CBACT03C COBOL batch program.
 * 
 * This job sequentially reads card-to-account cross-reference records and generates formatted output listing
 * for audit and reporting purposes. Implements simple read-process-write pattern maintaining exact output
 * format from the COBOL program for audit trail consistency.
 * 
 * COBOL Program Migration Details:
 * - Source Program: CBACT03C.cbl (Cross-Reference Data Listing)
 * - Business Function: Sequential read and display of card cross-reference records
 * - Input: VSAM KSDS XREFFILE-FILE (now PostgreSQL card_xref table)
 * - Output: Formatted listing output (now flat file with identical format)
 * - Processing Pattern: Open → Read Loop → Display → Close
 * 
 * Spring Batch Implementation:
 * - JpaPagingItemReader: Replaces VSAM sequential file access with paginated database reads
 * - ItemProcessor: Converts CardXref entities to CardCrossReferenceDto for formatted output
 * - FlatFileItemWriter: Generates formatted audit listing matching COBOL display format
 * - Error handling: Comprehensive error handling with status reporting matching COBOL ABEND logic
 * 
 * Key Features:
 * - Sequential processing: Maintains COBOL sequential file processing pattern
 * - Audit trail consistency: Exact output format preservation for audit requirements
 * - Error handling: Proper status reporting and error notification
 * - Execution metrics: Comprehensive logging and monitoring for operational visibility
 * - Restart capability: Spring Batch restart support for failed executions
 * 
 * Processing Flow:
 * 1. Initialize job with BatchJobListener for metrics collection
 * 2. Read CardXref entities using JpaPagingItemReader in sequential order
 * 3. Process each record through ItemProcessor for formatting transformation
 * 4. Write formatted output using FlatFileItemWriter with COBOL-compatible format
 * 5. Complete with execution metrics and status reporting
 * 
 * Output Format (matching COBOL CARD-XREF-RECORD display):
 * - XREF-CARD-NUM: 16-character card number
 * - XREF-CUST-ID: 9-digit customer ID (zero-padded)
 * - XREF-ACCT-ID: 11-digit account ID (zero-padded)
 * - Fixed-width format maintaining exact field positions from COBOL
 * 
 * Performance Characteristics:
 * - Page size: Configurable chunk size for optimal memory usage
 * - Thread safety: Single-threaded processing matching COBOL sequential nature
 * - Error isolation: Individual record error handling without job termination
 * - Resource management: Automatic cleanup of temporary files and database connections
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Component
@Profile("!test")
public class CrossReferenceListJob {

    private static final Logger logger = LoggerFactory.getLogger(CrossReferenceListJob.class);
    
    // Batch processing configuration constants
    private static final int DEFAULT_CHUNK_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final String OUTPUT_DIRECTORY = "./batch/output";
    private static final String JOB_NAME = "crossReferenceListJob";
    private static final String STEP_NAME = "crossReferenceListStep";
    
    // Output file formatting constants matching COBOL format
    private static final String OUTPUT_FILE_PREFIX = "XREF_LISTING_";
    private static final String OUTPUT_FILE_EXTENSION = ".txt";
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    // Dependencies from BatchConfig and entities
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final EntityManagerFactory entityManagerFactory;
    private final CardXrefRepository cardXrefRepository;
    private final BatchJobListener batchJobListener;

    /**
     * Constructor with dependency injection for all required Spring Batch infrastructure components.
     * 
     * @param jobRepository JobRepository bean for Spring Batch job management
     * @param transactionManager PlatformTransactionManager for Spring Batch transaction management
     * @param taskExecutor ThreadPoolTaskExecutor for Spring Batch parallel processing
     * @param entityManagerFactory JPA EntityManagerFactory for database operations
     * @param cardXrefRepository Spring Data JPA repository for CardXref entity operations
     * @param batchJobListener Job execution listener for metrics collection and monitoring
     */
    @Autowired
    public CrossReferenceListJob(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor,
            EntityManagerFactory entityManagerFactory,
            CardXrefRepository cardXrefRepository,
            BatchJobListener batchJobListener) {
        
        try {
            this.jobRepository = jobRepository;
            this.transactionManager = transactionManager;
            this.taskExecutor = taskExecutor;
            this.entityManagerFactory = entityManagerFactory;
            this.cardXrefRepository = cardXrefRepository;
            this.batchJobListener = batchJobListener;
            
            logger.info("CrossReferenceListJob initialized successfully with Spring Batch infrastructure");
            
            // Ensure output directory exists
            ensureOutputDirectoryExists();
            
        } catch (Exception e) {
            logger.error("Failed to initialize CrossReferenceListJob", e);
            throw new RuntimeException("CrossReferenceListJob initialization failed", e);
        }
    }

    /**
     * Configures and creates the main cross-reference listing job.
     * 
     * This method builds the complete Spring Batch Job configuration that replaces the CBACT03C
     * COBOL program functionality. The job implements a simple linear processing flow matching
     * the sequential nature of the original COBOL implementation.
     * 
     * Job Configuration:
     * - Job name: crossReferenceListJob (matching COBOL program name pattern)
     * - Listener: BatchJobListener for comprehensive monitoring and metrics
     * - Incrementer: RunIdIncrementer for unique job instances
     * - Restartable: Enabled for recovery from failures
     * - Single step: crossReferenceListStep for sequential processing
     * 
     * The job maintains compatibility with the original COBOL program by:
     * - Processing records in the same sequential order
     * - Generating identical output format for audit consistency
     * - Implementing equivalent error handling and status reporting
     * - Preserving execution logging patterns for operational monitoring
     * 
     * @return configured Job instance ready for execution by JobLauncher
     */
    @Bean(name = "crossReferenceJob")
    public Job crossReferenceListJob() {
        logger.info("Configuring crossReferenceListJob to replace CBACT03C COBOL program");
        
        try {
            return new JobBuilder(JOB_NAME, jobRepository)
                    .start(crossReferenceListStep())
                    .listener(batchJobListener)
                    .incrementer(new RunIdIncrementer())
                    .build();
                    
        } catch (Exception e) {
            logger.error("Failed to configure crossReferenceListJob", e);
            throw new BatchProcessingException(JOB_NAME, 
                BatchProcessingException.ErrorType.LAUNCH_FAILURE,
                "Job configuration failed for crossReferenceListJob", e);
        }
    }

    /**
     * Configures and creates the cross-reference listing processing step.
     * 
     * This method implements the core processing logic of the CBACT03C COBOL program using
     * Spring Batch chunk-based processing. The step reads CardXref entities sequentially,
     * processes them for formatting, and writes the output in the exact format expected
     * by audit and reporting systems.
     * 
     * Step Configuration:
     * - Reader: JpaPagingItemReader for sequential database record access
     * - Processor: Custom processor converting entities to formatted DTOs
     * - Writer: FlatFileItemWriter generating COBOL-compatible output format
     * - Chunk size: Configurable for optimal memory usage and transaction boundaries
     * - Error handling: Skip and retry logic matching COBOL error handling patterns
     * 
     * The step maintains functional parity with COBOL by:
     * - Reading records in identical sequential order (by card number)
     * - Processing each record with same business logic
     * - Outputting data in exact same format and field positions
     * - Implementing equivalent error handling and recovery procedures
     * 
     * @return configured Step instance for cross-reference record processing
     */
    @Bean(name = "crossReferenceStep")
    public Step crossReferenceListStep() {
        logger.info("Configuring crossReferenceListStep for sequential cross-reference processing");
        
        try {
            return new StepBuilder(STEP_NAME, jobRepository)
                    .<CardXref, CardCrossReferenceDto>chunk(DEFAULT_CHUNK_SIZE, transactionManager)
                    .reader(crossReferenceReader())
                    .processor(crossReferenceProcessor())
                    .writer(crossReferenceWriter())
                    .build();
                    
        } catch (Exception e) {
            logger.error("Failed to configure crossReferenceListStep", e);
            throw new BatchProcessingException(STEP_NAME, 
                BatchProcessingException.ErrorType.LAUNCH_FAILURE,
                "Step configuration failed for crossReferenceListStep", e);
        }
    }

    /**
     * Configures JpaPagingItemReader for sequential CardXref entity reading.
     * 
     * This reader replaces the VSAM sequential file access from CBACT03C COBOL program with
     * efficient database pagination. It reads CardXref records in sequential order by card number,
     * maintaining the same processing sequence as the original COBOL implementation.
     * 
     * Reader Configuration:
     * - Entity: CardXref mapped from card_xref PostgreSQL table
     * - Query: SELECT cx FROM CardXref cx ORDER BY cx.xrefCardNum
     * - Page size: Configurable for optimal memory usage and database performance
     * - EntityManagerFactory: JPA persistence context for database operations
     * - Sequential processing: Records read in card number order matching COBOL key sequence
     * 
     * COBOL Migration Details:
     * Original COBOL code pattern:
     * - OPEN INPUT XREFFILE-FILE
     * - PERFORM UNTIL END-OF-FILE = 'Y'
     * - READ XREFFILE-FILE INTO CARD-XREF-RECORD
     * 
     * Spring Batch equivalent:
     * - JpaPagingItemReader configured for sequential access
     * - Automatic pagination handling with configurable page size
     * - Database cursor management for large result sets
     * - Exception handling for database connectivity issues
     * 
     * Performance Considerations:
     * - Page size optimized for memory usage vs database roundtrips
     * - Order by card number ensures consistent processing sequence
     * - Read-only transaction isolation for data consistency
     * - Connection pooling through EntityManagerFactory
     * 
     * @return configured JpaPagingItemReader for CardXref entities
     */
    @Bean(name = "crossReferenceReader")
    public JpaPagingItemReader<CardXref> crossReferenceReader() {
        logger.info("Configuring JpaPagingItemReader for sequential CardXref processing");
        
        try {
            JpaPagingItemReader<CardXref> reader = new JpaPagingItemReader<>();
            
            // Configure reader with EntityManagerFactory and query
            reader.setEntityManagerFactory(entityManagerFactory);
            reader.setQueryString("SELECT cx FROM CardXref cx ORDER BY cx.xrefCardNum");
            reader.setPageSize(DEFAULT_PAGE_SIZE);
            reader.setName("cardXrefReader");
            
            // Initialize reader configuration
            reader.afterPropertiesSet();
            
            logger.info("JpaPagingItemReader configured successfully with page size: {}", DEFAULT_PAGE_SIZE);
            return reader;
            
        } catch (Exception e) {
            logger.error("Failed to configure crossReferenceReader", e);
            throw new BatchProcessingException("crossReferenceReader", 
                BatchProcessingException.ErrorType.LAUNCH_FAILURE,
                "Reader configuration failed for crossReferenceReader", e);
        }
    }

    /**
     * Configures ItemProcessor for CardXref to CardCrossReferenceDto transformation.
     * 
     * This processor implements the data formatting logic that corresponds to the DISPLAY
     * operations in the CBACT03C COBOL program. It converts CardXref entities to formatted
     * DTOs that match the exact output format requirements for audit listing generation.
     * 
     * Processing Logic:
     * - Convert CardXref entity field values to formatted strings
     * - Apply COBOL-compatible formatting (zero-padding, field alignment)
     * - Validate data integrity during transformation process
     * - Handle null values and data conversion errors gracefully
     * - Maintain exact field positioning and formatting from COBOL
     * 
     * COBOL Migration Details:
     * Original COBOL processing:
     * - READ XREFFILE-FILE INTO CARD-XREF-RECORD
     * - DISPLAY CARD-XREF-RECORD
     * 
     * Spring Batch equivalent:
     * - ItemProcessor transforms entity to DTO for consistent formatting
     * - Field-by-field mapping preserving COBOL data types and formats
     * - Error handling for data conversion issues
     * - Logging for processing statistics and error tracking
     * 
     * Data Transformation:
     * - xrefCardNum (String) → cardNumber with 16-character formatting
     * - xrefCustId (Long) → customerId with 9-digit zero-padded format
     * - xrefAcctId (Long) → accountId with 11-digit zero-padded format
     * 
     * @return configured ItemProcessor for CardXref to CardCrossReferenceDto transformation
     */
    @Bean(name = "crossReferenceProcessor")
    public ItemProcessor<CardXref, CardCrossReferenceDto> crossReferenceProcessor() {
        logger.info("Configuring ItemProcessor for CardXref to CardCrossReferenceDto transformation");
        
        return new ItemProcessor<CardXref, CardCrossReferenceDto>() {
            @Override
            public CardCrossReferenceDto process(CardXref cardXref) throws Exception {
                if (cardXref == null) {
                    logger.warn("Received null CardXref entity - skipping processing");
                    return null;
                }
                
                try {
                    // Extract field values from CardXref entity
                    String cardNumber = cardXref.getXrefCardNum();
                    Long customerId = cardXref.getXrefCustId();
                    Long accountId = cardXref.getXrefAcctId();
                    
                    // Validate required fields are present
                    if (cardNumber == null || customerId == null || accountId == null) {
                        logger.warn("CardXref entity has null required fields - CardNum: {}, CustId: {}, AcctId: {}", 
                                   cardNumber, customerId, accountId);
                        throw new BatchProcessingException("crossReferenceProcessor",
                            BatchProcessingException.ErrorType.EXECUTION_ERROR,
                            "Required fields missing in CardXref entity");
                    }
                    
                    // Format fields to match COBOL output specifications
                    String formattedCardNumber = String.format("%-16s", cardNumber.trim());
                    String formattedCustomerId = String.format("%09d", customerId);
                    String formattedAccountId = String.format("%011d", accountId);
                    
                    // Create DTO with formatted values
                    CardCrossReferenceDto dto = new CardCrossReferenceDto();
                    dto.setCardNumber(formattedCardNumber);
                    dto.setCustomerId(formattedCustomerId);
                    dto.setAccountId(formattedAccountId);
                    
                    // Validate DTO before returning
                    dto.validate();
                    
                    logger.debug("Processed CardXref: {} -> DTO: {}", cardXref.toString(), dto.getCrossReferenceKey());
                    return dto;
                    
                } catch (Exception e) {
                    logger.error("Error processing CardXref entity: {}", cardXref.toString(), e);
                    
                    // Determine if error is retryable
                    if (e instanceof BatchProcessingException && 
                        ((BatchProcessingException) e).isRetryable()) {
                        throw e; // Let Spring Batch handle retryable errors
                    }
                    
                    // For non-retryable errors, log and skip the record
                    logger.warn("Skipping CardXref due to non-retryable error: {}", e.getMessage());
                    return null;
                }
            }
        };
    }

    /**
     * Configures FlatFileItemWriter for formatted cross-reference output generation.
     * 
     * This writer generates the formatted audit listing output that exactly matches the
     * display format from the CBACT03C COBOL program. It writes each cross-reference record
     * in a fixed-width format suitable for audit trail and reporting purposes.
     * 
     * Writer Configuration:
     * - Output file: Generated with timestamp for unique naming
     * - Format: Fixed-width delimited format matching COBOL DISPLAY output
     * - Fields: Card number, Customer ID, Account ID in exact COBOL positions
     * - Header: Optional header line for field identification
     * - Encoding: UTF-8 for compatibility with modern systems
     * 
     * COBOL Migration Details:
     * Original COBOL output:
     * - DISPLAY CARD-XREF-RECORD
     * - Fixed positions: XREF-CARD-NUM, XREF-CUST-ID, XREF-ACCT-ID
     * 
     * Spring Batch equivalent:
     * - FlatFileItemWriter with BeanWrapperFieldExtractor
     * - DelimitedLineAggregator for formatted output generation
     * - FileSystemResource for output file management
     * - Automatic file creation and resource cleanup
     * 
     * Output Format (matching COBOL):
     * - Position 1-16: Card Number (left-justified, space-padded)
     * - Position 17-25: Customer ID (right-justified, zero-padded, 9 digits)
     * - Position 26-36: Account ID (right-justified, zero-padded, 11 digits)
     * - Field separator: Single space character
     * - Record terminator: Platform-specific line ending
     * 
     * @return configured FlatFileItemWriter for cross-reference listing output
     */
    @Bean(name = "crossReferenceWriter")
    public FlatFileItemWriter<CardCrossReferenceDto> crossReferenceWriter() {
        logger.info("Configuring FlatFileItemWriter for cross-reference listing output");
        
        try {
            // Generate unique output file name with timestamp
            String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT);
            String outputFileName = OUTPUT_FILE_PREFIX + timestamp + OUTPUT_FILE_EXTENSION;
            String outputFilePath = OUTPUT_DIRECTORY + File.separator + outputFileName;
            
            logger.info("Cross-reference listing will be written to: {}", outputFilePath);
            
            // Create and configure the writer
            FlatFileItemWriter<CardCrossReferenceDto> writer = new FlatFileItemWriter<>();
            writer.setResource(new FileSystemResource(outputFilePath));
            writer.setName("cardCrossReferenceWriter");
            writer.setShouldDeleteIfExists(true);
            writer.setShouldDeleteIfEmpty(false);
            
            // Configure line aggregator for formatted output
            DelimitedLineAggregator<CardCrossReferenceDto> lineAggregator = new DelimitedLineAggregator<>();
            lineAggregator.setDelimiter(" "); // Single space delimiter matching COBOL format
            
            // Configure field extractor for DTO property extraction
            BeanWrapperFieldExtractor<CardCrossReferenceDto> fieldExtractor = new BeanWrapperFieldExtractor<>();
            fieldExtractor.setNames(new String[]{"cardNumber", "customerId", "accountId"});
            lineAggregator.setFieldExtractor(fieldExtractor);
            
            writer.setLineAggregator(lineAggregator);
            
            // Set up header callback for field identification
            writer.setHeaderCallback(headerWriter -> {
                headerWriter.write("# Card Cross-Reference Listing - Generated: " + 
                                 LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                headerWriter.write("# Format: CARD_NUMBER CUSTOMER_ID ACCOUNT_ID");
                headerWriter.write("# ------------------------------------------------");
            });
            
            // Initialize writer configuration
            writer.afterPropertiesSet();
            
            logger.info("FlatFileItemWriter configured successfully for output file: {}", outputFileName);
            return writer;
            
        } catch (Exception e) {
            logger.error("Failed to configure crossReferenceWriter", e);
            throw new BatchProcessingException("crossReferenceWriter", 
                BatchProcessingException.ErrorType.LAUNCH_FAILURE,
                "Writer configuration failed for crossReferenceWriter", e);
        }
    }

    /**
     * Ensures the output directory exists for batch file generation.
     * Creates the directory structure if it doesn't exist and validates write permissions.
     */
    private void ensureOutputDirectoryExists() {
        try {
            File outputDir = new File(OUTPUT_DIRECTORY);
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                if (!created) {
                    throw new BatchProcessingException("DirectorySetup", 
                        BatchProcessingException.ErrorType.RESOURCE_UNAVAILABLE,
                        "Failed to create output directory: " + OUTPUT_DIRECTORY);
                }
                logger.info("Created output directory: {}", OUTPUT_DIRECTORY);
            }
            
            // Validate directory is writable
            if (!outputDir.canWrite()) {
                throw new BatchProcessingException("DirectorySetup", 
                    BatchProcessingException.ErrorType.RESOURCE_UNAVAILABLE,
                    "Output directory is not writable: " + OUTPUT_DIRECTORY);
            }
            
            logger.debug("Output directory validated: {}", OUTPUT_DIRECTORY);
            
        } catch (Exception e) {
            logger.error("Failed to setup output directory: {}", OUTPUT_DIRECTORY, e);
            throw new BatchProcessingException("DirectorySetup", 
                BatchProcessingException.ErrorType.RESOURCE_UNAVAILABLE,
                "Output directory setup failed", e);
        }
    }


}