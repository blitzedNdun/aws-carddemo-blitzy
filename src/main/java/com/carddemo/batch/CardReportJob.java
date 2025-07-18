package com.carddemo.batch;

import com.carddemo.card.Card;
import com.carddemo.card.CardRepository;
import com.carddemo.common.config.BatchConfiguration;
import com.carddemo.common.enums.CardStatus;
import com.carddemo.common.util.BigDecimalUtils;
import com.carddemo.common.util.DateUtils;
import com.carddemo.batch.dto.CardReportDTO;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CardReportJob - Spring Batch job for generating card listing reports, converted from COBOL CBACT02C.cbl program.
 * 
 * This batch job processes card records using paginated queries and generates formatted card listings with
 * comprehensive error handling and automated scheduling capabilities. The implementation converts the original
 * COBOL sequential file processing to modern Spring Batch framework patterns while maintaining identical
 * report format and functionality.
 * 
 * Original COBOL Program: CBACT02C.cbl
 * - Function: Read and print card data file
 * - Processing: Sequential VSAM KSDS card file reading with end-of-file detection
 * - Output: Card record display with formatted output
 * 
 * Spring Batch Implementation:
 * - ItemReader: Paginated card data access via CardRepository
 * - ItemProcessor: Card entity to CardReportDTO transformation
 * - ItemWriter: Formatted card report output generation
 * - Job Configuration: Automated scheduling and execution monitoring
 * - Error Handling: Skip policies and exception management
 * 
 * Key Features:
 * - Converts COBOL CBACT02C card listing batch program to Spring Batch Job
 * - Replaces VSAM KSDS indexed sequential file access with Spring Data JPA repository pattern
 * - Implements Spring Batch ItemReader pagination replacing COBOL sequential reading
 * - Provides Spring Batch FlatFileItemWriter for structured card listing output
 * - Maintains card record display format compatibility with original COBOL output
 * - Integrates with Kubernetes CronJob scheduling for automated execution
 * - Includes comprehensive error handling and monitoring capabilities
 * 
 * Performance Characteristics:
 * - 4-hour batch processing window completion target
 * - Optimized chunk-based processing for large card datasets
 * - Configurable pagination for memory-efficient processing
 * - Connection pool optimization for PostgreSQL database operations
 * - Comprehensive monitoring and metrics collection
 * 
 * Technical Implementation:
 * - Spring Batch job with step-based architecture
 * - Repository-based data access with optimized queries
 * - DTO-based data transformation for report generation
 * - File-based output with formatted report structure
 * - Error handling with skip policies and retry mechanisms
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
@Configuration
public class CardReportJob {

    private static final Logger logger = LoggerFactory.getLogger(CardReportJob.class);

    // Configuration properties
    @Value("${batch.card-report.chunk-size:1000}")
    private int chunkSize;

    @Value("${batch.card-report.output-path:/tmp/card-report.txt}")
    private String outputPath;

    @Value("${batch.card-report.page-size:100}")
    private int pageSize;

    @Value("${batch.card-report.max-item-count:100000}")
    private int maxItemCount;

    // Dependencies
    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private BatchConfiguration batchConfiguration;

    // Job execution metrics
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong totalCount = new AtomicLong(0);
    private LocalDateTime jobStartTime;
    private LocalDateTime jobEndTime;

    /**
     * Main Spring Batch job configuration for card report generation.
     * 
     * This method configures the primary batch job that processes card records and generates
     * formatted reports. The job includes comprehensive error handling, monitoring integration,
     * and automated scheduling support for Kubernetes CronJob execution.
     * 
     * Job Characteristics:
     * - Single-step job processing all card records
     * - Configurable chunk size for optimal performance
     * - Comprehensive error handling with skip policies
     * - Monitoring integration with job execution listeners
     * - Automated restart capability on failure
     * 
     * @return configured Spring Batch Job instance
     * @throws Exception if job configuration fails
     */
    @Bean
    public Job cardReportJob() throws Exception {
        logger.info("Configuring CardReportJob with chunk size: {}", chunkSize);

        return jobBuilderFactory.get("cardReportJob")
                .incrementer(new RunIdIncrementer())
                .listener(batchConfiguration.jobExecutionListener())
                .start(cardReportStep())
                .build();
    }

    /**
     * Spring Batch step configuration for card report processing.
     * 
     * This method configures the single processing step that handles card data retrieval,
     * transformation, and report output generation. The step uses chunk-oriented processing
     * for optimal performance with large datasets.
     * 
     * Step Characteristics:
     * - Chunk-based processing for memory efficiency
     * - Repository-based data access with pagination
     * - DTO transformation for report formatting
     * - File-based output with formatted structure
     * - Error handling with configurable skip policies
     * 
     * @return configured Spring Batch Step instance
     * @throws Exception if step configuration fails
     */
    @Bean
    public Step cardReportStep() throws Exception {
        logger.info("Configuring CardReportStep with chunk size: {}", chunkSize);

        return stepBuilderFactory.get("cardReportStep")
                .<Card, CardReportDTO>chunk(chunkSize)
                .reader(cardItemReader())
                .processor(cardItemProcessor())
                .writer(cardItemWriter())
                .listener(batchConfiguration.stepExecutionListener())
                .faultTolerant()
                .skipPolicy(batchConfiguration.skipPolicy())
                .retryPolicy(batchConfiguration.retryPolicy())
                .build();
    }

    /**
     * Spring Batch ItemReader for paginated card data access.
     * 
     * This method configures the ItemReader that retrieves card records from the PostgreSQL
     * database using Spring Data JPA repository pattern. The reader implements pagination
     * for memory-efficient processing of large card datasets.
     * 
     * Reader Characteristics:
     * - Paginated data access via CardRepository
     * - Configurable page size for optimization
     * - Sorted by card number for consistent ordering
     * - Maximum item count protection against runaway processing
     * - Thread-safe implementation for concurrent access
     * 
     * @return configured Spring Batch ItemReader instance
     */
    @Bean
    public ItemReader<Card> cardItemReader() {
        logger.info("Configuring CardItemReader with page size: {}", pageSize);

        // Get total count for monitoring
        long totalCards = cardRepository.count();
        totalCount.set(totalCards);
        logger.info("Total cards to process: {}", totalCards);

        return new RepositoryItemReaderBuilder<Card>()
                .name("cardItemReader")
                .repository(cardRepository)
                .methodName("findAll")
                .pageSize(pageSize)
                .maxItemCount(maxItemCount)
                .sorts(Collections.singletonMap("cardNumber", Sort.Direction.ASC))
                .build();
    }

    /**
     * Spring Batch ItemProcessor for card entity transformation.
     * 
     * This method configures the ItemProcessor that transforms Card entities into
     * CardReportDTO objects for report generation. The processor handles data
     * validation, formatting, and business logic application.
     * 
     * Processor Characteristics:
     * - Card entity to CardReportDTO transformation
     * - Data validation and formatting
     * - Business logic application for report fields
     * - Error handling for invalid card data
     * - Null-safe processing with comprehensive validation
     * 
     * @return configured Spring Batch ItemProcessor instance
     */
    @Bean
    public ItemProcessor<Card, CardReportDTO> cardItemProcessor() {
        logger.info("Configuring CardItemProcessor");

        return new ItemProcessor<Card, CardReportDTO>() {
            @Override
            public CardReportDTO process(Card card) throws Exception {
                if (card == null) {
                    logger.warn("Skipping null card record");
                    return null;
                }

                logger.debug("Processing card: {}", card.getCardNumber());

                try {
                    // Transform Card entity to CardReportDTO
                    CardReportDTO reportDTO = new CardReportDTO(card);
                    
                    // Validate the transformed data
                    if (!reportDTO.isValid()) {
                        logger.warn("Invalid card data for card: {}", card.getCardNumber());
                        return null;
                    }

                    // Set processing metadata
                    reportDTO.setGenerationTimestamp(LocalDateTime.now());
                    reportDTO.setRecordCount(processedCount.incrementAndGet());

                    logger.debug("Successfully processed card: {}", card.getCardNumber());
                    return reportDTO;

                } catch (Exception e) {
                    logger.error("Error processing card: {}", card.getCardNumber(), e);
                    throw new Exception("Failed to process card: " + card.getCardNumber(), e);
                }
            }
        };
    }

    /**
     * Spring Batch ItemWriter for formatted card report output.
     * 
     * This method configures the ItemWriter that generates formatted card reports
     * using FlatFileItemWriter. The writer creates structured output files with
     * consistent formatting matching the original COBOL report format.
     * 
     * Writer Characteristics:
     * - File-based output with formatted structure
     * - Custom line aggregator for report formatting
     * - Header and footer support for complete reports
     * - Error handling for file I/O operations
     * - Configurable output path for deployment flexibility
     * 
     * @return configured Spring Batch ItemWriter instance
     */
    @Bean
    public ItemWriter<CardReportDTO> cardItemWriter() {
        logger.info("Configuring CardItemWriter with output path: {}", outputPath);

        return new FlatFileItemWriterBuilder<CardReportDTO>()
                .name("cardItemWriter")
                .resource(new FileSystemResource(outputPath))
                .lineAggregator(new LineAggregator<CardReportDTO>() {
                    @Override
                    public String aggregate(CardReportDTO cardReport) {
                        return cardReport.formatAsDetailLine();
                    }
                })
                .headerCallback(writer -> {
                    String header = formatReportHeader();
                    writer.write(header);
                })
                .footerCallback(writer -> {
                    String footer = formatReportFooter();
                    writer.write(footer);
                })
                .build();
    }

    /**
     * Generates a complete card report programmatically.
     * 
     * This method provides a programmatic interface for generating card reports
     * outside of the standard batch job execution. It retrieves all cards,
     * processes them through the transformation pipeline, and generates a
     * formatted report.
     * 
     * Method Characteristics:
     * - Programmatic report generation
     * - Complete data processing pipeline
     * - Formatted output generation
     * - Error handling and logging
     * - Performance monitoring and metrics
     * 
     * @return formatted card report as string
     * @throws Exception if report generation fails
     */
    @Transactional(readOnly = true)
    public String generateCardReport() throws Exception {
        logger.info("Starting programmatic card report generation");

        jobStartTime = LocalDateTime.now();
        StringBuilder reportBuilder = new StringBuilder();

        try {
            // Add report header
            reportBuilder.append(formatReportHeader());

            // Process cards in pages for memory efficiency
            int pageNumber = 0;
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("cardNumber"));
            
            long totalProcessed = 0;
            long totalCards = cardRepository.count();

            logger.info("Processing {} total cards", totalCards);

            while (true) {
                List<Card> cards = cardRepository.findAll(pageable).getContent();
                
                if (cards.isEmpty()) {
                    break;
                }

                logger.debug("Processing page {} with {} cards", pageNumber, cards.size());

                // Process each card through the transformation pipeline
                for (Card card : cards) {
                    try {
                        CardReportDTO reportDTO = cardItemProcessor().process(card);
                        if (reportDTO != null) {
                            reportBuilder.append(reportDTO.formatAsDetailLine());
                            reportBuilder.append(System.lineSeparator());
                            totalProcessed++;
                        }
                    } catch (Exception e) {
                        logger.error("Error processing card in programmatic report: {}", 
                                   card.getCardNumber(), e);
                        // Continue processing other cards
                    }
                }

                // Move to next page
                pageNumber++;
                pageable = PageRequest.of(pageNumber, pageSize, Sort.by("cardNumber"));

                // Safety check to prevent infinite loop
                if (totalProcessed >= totalCards) {
                    break;
                }
            }

            // Set final counts for footer
            processedCount.set(totalProcessed);
            totalCount.set(totalCards);

            // Add report footer
            reportBuilder.append(formatReportFooter());

            jobEndTime = LocalDateTime.now();
            logger.info("Completed programmatic card report generation. Processed {} cards", totalProcessed);

            return reportBuilder.toString();

        } catch (Exception e) {
            logger.error("Error generating programmatic card report", e);
            throw new Exception("Failed to generate card report", e);
        }
    }

    /**
     * Returns the total count of cards processed in the report.
     * 
     * This method provides access to the total number of cards that have been
     * processed during report generation. It's used for monitoring, logging,
     * and summary information in reports.
     * 
     * @return total count of cards processed
     */
    public long getReportTotalCount() {
        long count = totalCount.get();
        logger.debug("Report total count: {}", count);
        return count;
    }

    /**
     * Formats the report header with title, timestamp, and column headers.
     * 
     * This method generates a formatted header for the card report that includes
     * the report title, generation timestamp, column headers, and separator lines.
     * The format maintains compatibility with the original COBOL report layout.
     * 
     * Header Components:
     * - Report title with generation timestamp
     * - Separator lines for visual organization
     * - Column headers for card data fields
     * - Proper spacing and alignment
     * 
     * @return formatted report header string
     */
    public String formatReportHeader() {
        StringBuilder headerBuilder = new StringBuilder();
        
        // Report title with timestamp
        String currentDateTime = DateUtils.formatDateForDisplay(LocalDateTime.now().toLocalDate()) + 
                                " " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        headerBuilder.append("CREDIT CARD REPORT - ").append(currentDateTime).append(System.lineSeparator());
        
        // Separator line
        headerBuilder.append("=".repeat(100)).append(System.lineSeparator());
        
        // Column headers
        headerBuilder.append(String.format("%-16s %-11s %-50s %-10s %-1s %-3s",
                "CARD NUMBER", "ACCOUNT ID", "EMBOSSED NAME", "EXPIRY DATE", "STATUS", "CVV"));
        headerBuilder.append(System.lineSeparator());
        
        // Second separator line
        headerBuilder.append("=".repeat(100)).append(System.lineSeparator());
        
        String header = headerBuilder.toString();
        logger.debug("Generated report header: {} characters", header.length());
        return header;
    }

    /**
     * Formats the report footer with summary information and completion timestamp.
     * 
     * This method generates a formatted footer for the card report that includes
     * summary statistics, completion timestamp, and final separator lines.
     * The format maintains compatibility with the original COBOL report layout.
     * 
     * Footer Components:
     * - Separator line for visual organization
     * - Total cards processed count
     * - Processing start and end timestamps
     * - Job execution duration
     * - Final separator line
     * 
     * @return formatted report footer string
     */
    public String formatReportFooter() {
        StringBuilder footerBuilder = new StringBuilder();
        
        // Separator line
        footerBuilder.append("=".repeat(100)).append(System.lineSeparator());
        
        // Summary information
        long totalProcessed = processedCount.get();
        long totalCards = totalCount.get();
        
        footerBuilder.append(String.format("TOTAL CARDS PROCESSED: %,d", totalProcessed)).append(System.lineSeparator());
        footerBuilder.append(String.format("TOTAL CARDS IN SYSTEM: %,d", totalCards)).append(System.lineSeparator());
        
        // Processing timestamps
        if (jobStartTime != null) {
            String startTime = DateUtils.formatDateForDisplay(jobStartTime.toLocalDate()) + 
                              " " + jobStartTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            footerBuilder.append("PROCESSING STARTED: ").append(startTime).append(System.lineSeparator());
        }
        
        String endTime = DateUtils.formatDateForDisplay(LocalDateTime.now().toLocalDate()) + 
                        " " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        footerBuilder.append("PROCESSING COMPLETED: ").append(endTime).append(System.lineSeparator());
        
        // Calculate duration if start time is available
        if (jobStartTime != null) {
            LocalDateTime endDateTime = jobEndTime != null ? jobEndTime : LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(jobStartTime, endDateTime).getSeconds();
            long hours = durationSeconds / 3600;
            long minutes = (durationSeconds % 3600) / 60;
            long seconds = durationSeconds % 60;
            
            footerBuilder.append(String.format("PROCESSING DURATION: %02d:%02d:%02d", hours, minutes, seconds))
                         .append(System.lineSeparator());
        }
        
        // Final separator
        footerBuilder.append("=".repeat(100)).append(System.lineSeparator());
        
        String footer = footerBuilder.toString();
        logger.debug("Generated report footer: {} characters", footer.length());
        return footer;
    }

    /**
     * Retrieves cards filtered by status with pagination support.
     * 
     * This method provides filtered card retrieval capabilities for specialized
     * report generation based on card status. It supports pagination for
     * memory-efficient processing of large datasets.
     * 
     * @param status the card status to filter by
     * @param pageable pagination information
     * @return list of cards matching the specified status
     */
    @Transactional(readOnly = true)
    public List<Card> getCardsByStatus(CardStatus status, Pageable pageable) {
        logger.debug("Retrieving cards with status: {} for page: {}", status, pageable.getPageNumber());
        
        try {
            List<Card> cards = cardRepository.findByActiveStatus(status, pageable).getContent();
            logger.debug("Retrieved {} cards with status: {}", cards.size(), status);
            return cards;
        } catch (Exception e) {
            logger.error("Error retrieving cards by status: {}", status, e);
            return Collections.emptyList();
        }
    }

    /**
     * Validates card data for report generation.
     * 
     * This method performs validation checks on card data to ensure it meets
     * the requirements for report generation. It validates required fields,
     * data formats, and business rules.
     * 
     * @param card the card to validate
     * @return true if card data is valid for reporting
     */
    private boolean validateCardForReporting(Card card) {
        if (card == null) {
            logger.warn("Card is null - invalid for reporting");
            return false;
        }

        // Validate required fields
        if (card.getCardNumber() == null || card.getCardNumber().trim().isEmpty()) {
            logger.warn("Card number is null or empty - invalid for reporting");
            return false;
        }

        if (card.getAccountId() == null || card.getAccountId().trim().isEmpty()) {
            logger.warn("Account ID is null or empty for card: {}", card.getCardNumber());
            return false;
        }

        if (card.getEmbossedName() == null || card.getEmbossedName().trim().isEmpty()) {
            logger.warn("Embossed name is null or empty for card: {}", card.getCardNumber());
            return false;
        }

        // Validate card number format
        if (!card.getCardNumber().matches("\\d{16}")) {
            logger.warn("Card number format invalid for card: {}", card.getCardNumber());
            return false;
        }

        // Validate account ID format
        if (!card.getAccountId().matches("\\d{11}")) {
            logger.warn("Account ID format invalid for card: {}", card.getCardNumber());
            return false;
        }

        logger.debug("Card validation successful for card: {}", card.getCardNumber());
        return true;
    }

    /**
     * Resets job execution metrics for new job runs.
     * 
     * This method resets the internal counters and timestamps used for
     * job execution monitoring and reporting. It should be called before
     * starting a new job execution.
     */
    private void resetJobMetrics() {
        processedCount.set(0);
        totalCount.set(0);
        jobStartTime = LocalDateTime.now();
        jobEndTime = null;
        logger.debug("Job metrics reset for new execution");
    }

    /**
     * Gets the current job execution statistics.
     * 
     * This method returns a summary of the current job execution including
     * processed counts, timing information, and performance metrics.
     * 
     * @return job execution statistics as formatted string
     */
    public String getJobExecutionStats() {
        StringBuilder statsBuilder = new StringBuilder();
        
        statsBuilder.append("CardReportJob Execution Statistics:").append(System.lineSeparator());
        statsBuilder.append("Total Cards: ").append(totalCount.get()).append(System.lineSeparator());
        statsBuilder.append("Processed Cards: ").append(processedCount.get()).append(System.lineSeparator());
        
        if (jobStartTime != null) {
            statsBuilder.append("Start Time: ").append(jobStartTime).append(System.lineSeparator());
        }
        
        if (jobEndTime != null) {
            statsBuilder.append("End Time: ").append(jobEndTime).append(System.lineSeparator());
            long durationSeconds = java.time.Duration.between(jobStartTime, jobEndTime).getSeconds();
            statsBuilder.append("Duration: ").append(durationSeconds).append(" seconds").append(System.lineSeparator());
        }
        
        double processingRate = processedCount.get() / Math.max(1.0, 
            java.time.Duration.between(jobStartTime, LocalDateTime.now()).getSeconds());
        statsBuilder.append("Processing Rate: ").append(String.format("%.2f", processingRate))
                   .append(" cards/second").append(System.lineSeparator());
        
        return statsBuilder.toString();
    }
}