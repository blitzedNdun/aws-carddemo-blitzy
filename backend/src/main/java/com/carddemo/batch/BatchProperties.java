/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.batch;

import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.nio.file.Paths;
import java.nio.file.Path;

/**
 * Configuration properties class for batch processing parameters and settings.
 * 
 * This class externalizes all batch processing configuration from hard-coded values to
 * configurable Spring Boot properties, enabling environment-specific tuning and operational
 * flexibility without code changes. Replaces JCL parameters with Spring Boot configuration
 * management supporting development, staging, and production environment customization.
 * 
 * Core Responsibilities:
 * - Externalize chunk size, thread pool, and retry policy configuration
 * - Provide configurable file path management for input/output/archive operations
 * - Support execution window parameters for 4-hour batch processing compliance
 * - Enable job-specific parameter configuration through property namespacing
 * - Implement date-based file path resolution matching COBOL batch conventions
 * - Validate configuration constraints and provide sensible defaults
 * 
 * Configuration Categories:
 * - Processing Parameters: chunk sizes, thread pools, retry policies, skip limits
 * - File System Configuration: input/output/archive directory paths with date substitution
 * - Execution Control: processing windows, timeout settings, performance thresholds
 * - Job-Specific Settings: statement generation formats, interest calculation precision
 * - Environment Integration: Spring profiles, property source hierarchy, validation
 * 
 * COBOL Migration Benefits:
 * - Replaces hard-coded JCL PARM parameters with externalized configuration
 * - Enables environment-specific configuration without code changes
 * - Supports dynamic file path generation matching mainframe naming conventions
 * - Provides type-safe configuration with validation and constraint enforcement
 * - Centralizes all batch processing parameters for operational management
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Profile("!test")
@Component
@ConfigurationProperties(prefix = "carddemo.batch")
public class BatchProperties {

    // Date formatter for file path substitution patterns
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    // Processing configuration properties
    @Min(value = 100, message = "Chunk size must be at least 100")
    @Max(value = 10000, message = "Chunk size must not exceed 10000")
    @Value("${carddemo.batch.chunk-size:1000}")
    private int chunkSize = 1000;
    
    @Min(value = 1, message = "Thread pool size must be at least 1")
    @Max(value = 16, message = "Thread pool size must not exceed 16")
    @Value("${carddemo.batch.thread-pool-size:4}")
    private int threadPoolSize = 4;
    
    @Min(value = 0, message = "Max retry attempts must not be negative")
    @Max(value = 10, message = "Max retry attempts must not exceed 10")
    @Value("${carddemo.batch.max-retry-attempts:3}")
    private int maxRetryAttempts = 3;
    
    @Min(value = 0, message = "Skip limit must not be negative")
    @Max(value = 1000, message = "Skip limit must not exceed 1000")
    @Value("${carddemo.batch.skip-limit:100}")
    private int skipLimit = 100;
    
    // File system configuration properties
    @NotBlank(message = "Input directory must be specified")
    @Value("${carddemo.batch.input-directory:/opt/carddemo/batch/input}")
    private String inputDirectory = "/opt/carddemo/batch/input";
    
    @NotBlank(message = "Output directory must be specified")
    @Value("${carddemo.batch.output-directory:/opt/carddemo/batch/output}")
    private String outputDirectory = "/opt/carddemo/batch/output";
    
    @NotBlank(message = "Archive directory must be specified")
    @Value("${carddemo.batch.archive-directory:/opt/carddemo/batch/archive}")
    private String archiveDirectory = "/opt/carddemo/batch/archive";
    
    // Execution window configuration
    @Value("${carddemo.batch.execution-window-start:02:00}")
    private String executionWindowStart = "02:00";
    
    @Value("${carddemo.batch.execution-window-end:06:00}")
    private String executionWindowEnd = "06:00";
    
    // Job-specific configuration
    @Value("${carddemo.batch.statement-generation-format:both}")
    private String statementGenerationFormat = "both"; // text, html, or both
    
    @Min(value = 2, message = "Interest calculation scale must be at least 2")
    @Max(value = 6, message = "Interest calculation scale must not exceed 6")
    @Value("${carddemo.batch.interest-calculation-scale:2}")
    private int interestCalculationScale = 2;
    
    /**
     * Default constructor for Spring configuration properties binding.
     */
    public BatchProperties() {
    }
    
    /**
     * Gets the configured chunk size for batch processing operations.
     * 
     * @return chunk size for Spring Batch ItemReader/ItemWriter operations
     */
    public int getChunkSize() {
        return chunkSize;
    }
    
    /**
     * Sets the chunk size for batch processing operations.
     * 
     * @param chunkSize chunk size for Spring Batch operations
     */
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    /**
     * Gets the configured thread pool size for parallel batch processing.
     * 
     * @return thread pool size for TaskExecutor configuration
     */
    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    
    /**
     * Sets the thread pool size for parallel batch processing.
     * 
     * @param threadPoolSize thread pool size for TaskExecutor
     */
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }
    
    /**
     * Gets the maximum retry attempts for failed batch operations.
     * 
     * @return maximum retry attempts for error handling
     */
    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }
    
    /**
     * Sets the maximum retry attempts for failed batch operations.
     * 
     * @param maxRetryAttempts maximum retry attempts
     */
    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }
    
    /**
     * Gets the skip limit for error tolerance in batch processing.
     * 
     * @return skip limit for handling failed records
     */
    public int getSkipLimit() {
        return skipLimit;
    }
    
    /**
     * Sets the skip limit for error tolerance in batch processing.
     * 
     * @param skipLimit skip limit for failed records
     */
    public void setSkipLimit(int skipLimit) {
        this.skipLimit = skipLimit;
    }
    
    /**
     * Gets the input directory path for batch file processing.
     * 
     * @return input directory path
     */
    public String getInputDirectory() {
        return inputDirectory;
    }
    
    /**
     * Sets the input directory path for batch file processing.
     * 
     * @param inputDirectory input directory path
     */
    public void setInputDirectory(String inputDirectory) {
        this.inputDirectory = inputDirectory;
    }
    
    /**
     * Gets the output directory path for batch file generation.
     * 
     * @return output directory path
     */
    public String getOutputDirectory() {
        return outputDirectory;
    }
    
    /**
     * Sets the output directory path for batch file generation.
     * 
     * @param outputDirectory output directory path
     */
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    /**
     * Gets the archive directory path for processed file storage.
     * 
     * @return archive directory path
     */
    public String getArchiveDirectory() {
        return archiveDirectory;
    }
    
    /**
     * Sets the archive directory path for processed file storage.
     * 
     * @param archiveDirectory archive directory path
     */
    public void setArchiveDirectory(String archiveDirectory) {
        this.archiveDirectory = archiveDirectory;
    }
    
    /**
     * Gets the execution window start time.
     * 
     * @return execution window start time in HH:MM format
     */
    public String getExecutionWindowStart() {
        return executionWindowStart;
    }
    
    /**
     * Sets the execution window start time.
     * 
     * @param executionWindowStart start time in HH:MM format
     */
    public void setExecutionWindowStart(String executionWindowStart) {
        this.executionWindowStart = executionWindowStart;
    }
    
    /**
     * Gets the execution window end time.
     * 
     * @return execution window end time in HH:MM format
     */
    public String getExecutionWindowEnd() {
        return executionWindowEnd;
    }
    
    /**
     * Sets the execution window end time.
     * 
     * @param executionWindowEnd end time in HH:MM format
     */
    public void setExecutionWindowEnd(String executionWindowEnd) {
        this.executionWindowEnd = executionWindowEnd;
    }
    
    /**
     * Gets the statement generation format configuration.
     * 
     * @return statement format (text, html, or both)
     */
    public String getStatementGenerationFormat() {
        return statementGenerationFormat;
    }
    
    /**
     * Sets the statement generation format configuration.
     * 
     * @param statementGenerationFormat format setting
     */
    public void setStatementGenerationFormat(String statementGenerationFormat) {
        this.statementGenerationFormat = statementGenerationFormat;
    }
    
    /**
     * Gets the interest calculation decimal scale.
     * 
     * @return decimal scale for BigDecimal interest calculations
     */
    public int getInterestCalculationScale() {
        return interestCalculationScale;
    }
    
    /**
     * Sets the interest calculation decimal scale.
     * 
     * @param interestCalculationScale decimal scale for calculations
     */
    public void setInterestCalculationScale(int interestCalculationScale) {
        this.interestCalculationScale = interestCalculationScale;
    }
    
    /**
     * Resolves input file path with date substitution for dynamic file naming.
     * 
     * Replaces YYYYMMDD patterns in the input directory path with the current date,
     * supporting COBOL-style batch file naming conventions and daily processing
     * patterns common in mainframe batch operations.
     * 
     * @param fileName file name to append to resolved path
     * @return resolved path with date substitution applied
     */
    public String resolveInputPath(String fileName) {
        return resolveDatePath(inputDirectory, fileName);
    }
    
    /**
     * Resolves input file path with specific date substitution.
     * 
     * @param fileName file name to append to resolved path
     * @param date specific date for path substitution
     * @return resolved path with date substitution applied
     */
    public String resolveInputPath(String fileName, LocalDate date) {
        return resolveDatePath(inputDirectory, fileName, date);
    }
    
    /**
     * Resolves output file path with date substitution for dynamic file naming.
     * 
     * @param fileName file name to append to resolved path
     * @return resolved path with date substitution applied
     */
    public String resolveOutputPath(String fileName) {
        return resolveDatePath(outputDirectory, fileName);
    }
    
    /**
     * Resolves output file path with specific date substitution.
     * 
     * @param fileName file name to append to resolved path
     * @param date specific date for path substitution
     * @return resolved path with date substitution applied
     */
    public String resolveOutputPath(String fileName, LocalDate date) {
        return resolveDatePath(outputDirectory, fileName, date);
    }
    
    /**
     * Resolves archive file path with date substitution for dynamic file naming.
     * 
     * @param fileName file name to append to resolved path
     * @return resolved path with date substitution applied
     */
    public String resolveArchivePath(String fileName) {
        return resolveDatePath(archiveDirectory, fileName);
    }
    
    /**
     * Resolves archive file path with specific date substitution.
     * 
     * @param fileName file name to append to resolved path
     * @param date specific date for path substitution
     * @return resolved path with date substitution applied
     */
    public String resolveArchivePath(String fileName, LocalDate date) {
        return resolveDatePath(archiveDirectory, fileName, date);
    }
    
    /**
     * Private helper method for date path resolution using current date.
     * 
     * @param basePath base directory path potentially containing YYYYMMDD pattern
     * @param fileName file name to append
     * @return resolved path with current date substitution
     */
    private String resolveDatePath(String basePath, String fileName) {
        return resolveDatePath(basePath, fileName, LocalDate.now());
    }
    
    /**
     * Private helper method for date path resolution with specific date.
     * 
     * Performs YYYYMMDD pattern substitution in directory paths to support
     * COBOL-style batch file organization with date-based directory structures.
     * 
     * @param basePath base directory path potentially containing YYYYMMDD pattern
     * @param fileName file name to append
     * @param date specific date for substitution
     * @return resolved path with date substitution applied
     */
    private String resolveDatePath(String basePath, String fileName, LocalDate date) {
        String dateString = date.format(DATE_FORMAT);
        String resolvedPath = basePath.replace("YYYYMMDD", dateString);
        
        if (fileName != null && !fileName.trim().isEmpty()) {
            Path path = Paths.get(resolvedPath);
            return path.resolve(fileName).toString();
        }
        
        return resolvedPath;
    }
}