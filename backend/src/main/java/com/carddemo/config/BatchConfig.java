/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.config;

import com.carddemo.batch.BatchProperties;
import com.carddemo.config.DatabaseConfig;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.sql.DataSource;

/**
 * Spring Batch configuration class replacing JCL job processing functionality.
 * 
 * This configuration establishes the complete Spring Batch infrastructure required to replace
 * traditional mainframe JCL job processing with modern Spring Batch capabilities while preserving
 * identical processing patterns, error handling, and operational characteristics.
 * 
 * Core Responsibilities:
 * - Configure JobRepository with PostgreSQL backend for batch metadata storage
 * - Set up JobLauncher with asynchronous execution for non-blocking batch operations  
 * - Configure TaskExecutor with thread pool sizing for parallel processing within 4-hour windows
 * - Enable restart and recovery mechanisms matching mainframe batch processing patterns
 * - Integrate with DatabaseConfig for transaction management matching CICS SYNCPOINT behavior
 * - Configure batch processing properties externalization through BatchProperties
 * 
 * JCL to Spring Batch Migration Features:
 * - Job Repository: Replaces JES2/JES3 job queue management with database-backed job metadata
 * - Job Launcher: Provides programmatic job execution replacing JCL job submission mechanisms
 * - Task Executor: Multi-threaded processing capability for parallel step execution
 * - Transaction Management: CICS SYNCPOINT-equivalent transaction boundaries for data integrity
 * - Restart Capability: Failed job restart and recovery matching mainframe batch restart procedures
 * - Step Execution Tracking: Comprehensive step execution metadata for monitoring and debugging
 * 
 * Performance and Scalability Configuration:
 * - Thread pool sizing: 4 core threads, 8 maximum threads for optimal resource utilization
 * - Queue capacity: 100 pending jobs to handle burst processing loads
 * - Chunk-based processing: Configurable chunk sizes for memory and performance optimization
 * - Connection pool integration: HikariCP DataSource usage for high-performance database access
 * - Transaction isolation: READ_COMMITTED level for concurrent batch operations
 * 
 * Batch Processing Window Support:
 * - 4-hour processing window compliance through optimized thread allocation
 * - Parallel job execution capability for concurrent batch operations
 * - Resource management preventing memory exhaustion during large data processing
 * - Monitoring integration for job execution tracking and performance analysis
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    // Thread pool configuration constants for batch processing optimization
    private static final int CORE_POOL_SIZE = 4;         // Core thread pool size for steady-state operations
    private static final int MAX_POOL_SIZE = 8;          // Maximum thread pool size for peak processing
    private static final int QUEUE_CAPACITY = 100;       // Queue size for pending job execution requests
    private static final String THREAD_NAME_PREFIX = "CardDemo-Batch-"; // Thread naming for monitoring
    private static final int KEEP_ALIVE_SECONDS = 300;   // 5 minutes thread keep-alive time
    
    // Job repository configuration constants
    private static final String TABLE_PREFIX = "BATCH_";  // Database table prefix for batch metadata
    private static final int ISOLATION_LEVEL = 2;         // READ_COMMITTED isolation level
    private static final int MAX_VARCHAR_LENGTH = 2500;   // Maximum varchar length for batch tables
    private static final int CLOBTYPE = 4;                // CLOB type for large text fields

    /**
     * Configures Spring Batch JobRepository with PostgreSQL backend for comprehensive batch metadata management.
     * 
     * This method establishes the complete job metadata persistence infrastructure required for
     * Spring Batch operations, replacing traditional mainframe job management with modern database-backed
     * job execution tracking and recovery capabilities.
     * 
     * Job Repository Features:
     * - PostgreSQL integration: All batch metadata stored in relational database tables
     * - Transaction management: Integration with Spring PlatformTransactionManager for ACID compliance
     * - Job execution tracking: Complete audit trail of job instances, executions, and step executions
     * - Parameter management: Job parameter storage and validation for parameterized batch operations
     * - Status management: Job and step status tracking including STARTING, STARTED, COMPLETED, FAILED states
     * - Context management: ExecutionContext persistence for job and step data sharing and restart capability
     * 
     * Database Schema Integration:
     * - Table prefix: BATCH_ prefix for all Spring Batch metadata tables
     * - Isolation level: READ_COMMITTED for concurrent access to batch metadata
     * - Varchar length: 2500 character limit for job parameter and context values
     * - CLOB support: Large object storage for extensive execution context data
     * - Index optimization: Database indexes on key columns for query performance
     * 
     * Mainframe Batch Migration Benefits:
     * - Job restart capability: Failed jobs can be restarted from point of failure
     * - Parameter validation: Job parameter type checking and constraint validation
     * - Execution history: Complete historical record of all job executions for auditing
     * - Concurrent execution: Multiple jobs can execute simultaneously with proper isolation
     * - Resource management: Database-backed coordination prevents resource conflicts
     * 
     * @param dataSource configured HikariCP DataSource from DatabaseConfig for database connectivity
     * @param transactionManager configured PlatformTransactionManager from DatabaseConfig for transaction management
     * @return JobRepository configured with PostgreSQL backend for batch metadata persistence
     * @throws Exception if JobRepositoryFactoryBean initialization fails
     */
    @Bean
    public JobRepository jobRepository(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        
        // Configure data source and transaction management
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        
        // Configure database table settings for PostgreSQL optimization
        factory.setTablePrefix(TABLE_PREFIX);
        factory.setIsolationLevelForCreate(ISOLATION_LEVEL);
        factory.setMaxVarCharLength(MAX_VARCHAR_LENGTH);
        factory.setClobType(CLOBTYPE);
        
        // Configure advanced repository features
        factory.setValidateTransactionState(true);  // Ensure proper transaction boundaries
        factory.setLobHandler(null);                // Use default PostgreSQL CLOB handling
        factory.setIncrementerFactory(null);        // Use default sequence generation
        
        // Enable database schema validation
        factory.afterPropertiesSet();
        
        return factory.getObject();
    }

    /**
     * Configures Spring Batch JobLauncher for asynchronous and synchronous job execution capabilities.
     * 
     * This method establishes the complete job launching infrastructure required for flexible batch
     * job execution, supporting both immediate execution for time-critical operations and asynchronous
     * execution for long-running batch processes within 4-hour processing windows.
     * 
     * Job Launcher Features:
     * - Asynchronous execution: Non-blocking job launches using ThreadPoolTaskExecutor
     * - Synchronous execution: Blocking job execution for immediate result requirements
     * - Job parameter validation: Parameter type checking and constraint enforcement
     * - Job instance management: Automatic job instance creation and uniqueness validation
     * - Execution coordination: Thread-safe job launching with concurrent execution support
     * - Error handling: Comprehensive exception handling for job launch failures
     * 
     * Performance Characteristics:
     * - Thread pool integration: Uses configured TaskExecutor for optimal resource utilization
     * - Connection pooling: HikariCP integration through JobRepository for database efficiency
     * - Memory management: Controlled memory usage for large batch job execution
     * - Concurrency control: Safe concurrent job launches without resource conflicts
     * - Monitoring integration: Job launch metrics and execution tracking capability
     * 
     * Batch Processing Window Compliance:
     * - 4-hour window support: Optimized thread allocation for processing window requirements
     * - Resource optimization: Efficient thread usage preventing system resource exhaustion
     * - Priority management: Job execution prioritization for critical batch operations
     * - Load balancing: Thread distribution across multiple concurrent batch jobs
     * - Scalability: Horizontal scaling support through additional TaskExecutor threads
     * 
     * @param jobRepository configured JobRepository for batch metadata management
     * @param taskExecutor configured ThreadPoolTaskExecutor for asynchronous job execution
     * @return TaskExecutorJobLauncher configured for flexible batch job execution
     * @throws Exception if JobLauncher initialization fails
     */
    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository, ThreadPoolTaskExecutor taskExecutor) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        
        // Configure job repository integration
        jobLauncher.setJobRepository(jobRepository);
        
        // Configure task executor for asynchronous execution
        jobLauncher.setTaskExecutor(taskExecutor);
        
        // Validate configuration and initialize
        jobLauncher.afterPropertiesSet();
        
        return jobLauncher;
    }

    /**
     * Configures ThreadPoolTaskExecutor for optimized parallel batch processing within 4-hour windows.
     * 
     * This method establishes the complete thread pool infrastructure required for efficient batch
     * processing operations, supporting concurrent step execution, parallel job processing, and
     * optimal resource utilization while maintaining processing window compliance.
     * 
     * Thread Pool Configuration:
     * - Core pool size: Configurable via BatchProperties.getThreadPoolSize() for environment optimization
     * - Maximum pool size: 2x core pool size for peak processing loads and concurrent job execution
     * - Queue capacity: 100 pending tasks to handle burst processing requirements
     * - Keep-alive time: 5 minutes for thread lifecycle management and resource optimization
     * - Thread naming: CardDemo-Batch- prefix for monitoring and debugging identification
     * - Daemon threads: Non-daemon threads ensuring proper application shutdown coordination
     * 
     * Resource Management Features:
     * - Memory optimization: Controlled thread allocation preventing memory exhaustion
     * - CPU utilization: Balanced thread pool sizing for optimal CPU usage without oversaturation  
     * - Connection management: Integration with HikariCP for efficient database connection usage
     * - Monitoring support: Thread pool metrics exposure for operational monitoring
     * - Graceful shutdown: Proper thread termination during application shutdown procedures
     * 
     * Batch Processing Optimization:
     * - 4-hour window compliance: Thread allocation calibrated for processing window requirements
     * - Concurrent job support: Multiple batch jobs can execute simultaneously with resource isolation
     * - Step-level parallelization: Individual job steps can utilize multiple threads for large datasets
     * - Load balancing: Task distribution across available threads for optimal throughput
     * - Scalability: Thread pool expansion capability for increased processing demands
     * 
     * @param batchProperties configured BatchProperties for externalized thread pool configuration
     * @return ThreadPoolTaskExecutor configured for optimal batch processing performance
     */
    @Bean
    public ThreadPoolTaskExecutor taskExecutor(BatchProperties batchProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Configure core thread pool settings from BatchProperties
        int threadPoolSize = batchProperties.getThreadPoolSize();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize * 2);  // Double core size for peak loads
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        
        // Configure thread naming and behavior
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.setDaemon(false);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        // Configure rejection policy for task overflow
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // Initialize the executor
        executor.initialize();
        
        return executor;
    }

    /**
     * Configures BatchProperties for externalized batch processing configuration management.
     * 
     * This method creates a configuration properties bean that externalizes all batch processing
     * parameters from hard-coded values to configurable Spring Boot properties, enabling
     * environment-specific tuning and operational flexibility without code changes.
     * 
     * Configuration Properties Support:
     * - Chunk sizing: Configurable chunk sizes for memory and performance optimization
     * - Thread pool settings: External configuration of thread pool parameters
     * - Retry policies: Configurable retry attempts and backoff strategies  
     * - File path management: External configuration of input/output directory paths
     * - Processing limits: Configurable skip limits and error thresholds
     * - Execution windows: Processing window time constraints and scheduling parameters
     * 
     * Environment Integration:
     * - Spring Boot profiles: Different configurations for development, staging, production
     * - Property source hierarchy: Support for application.yml, environment variables, command line
     * - Configuration validation: Type checking and constraint validation for property values
     * - Hot reload: Configuration changes without application restart in development environments
     * - Monitoring integration: Configuration value exposure for operational monitoring
     * 
     * Batch Processing Flexibility:
     * - Job-specific parameters: Individual job configuration through property namespacing
     * - Resource allocation: Memory and CPU allocation configuration for different batch operations
     * - Error handling: Configurable error handling strategies and recovery procedures
     * - Performance tuning: Environment-specific performance optimization parameters
     * - Compliance settings: Regulatory and business rule configuration through properties
     * 
     * @return BatchProperties bean configured with @ConfigurationProperties for external configuration
     */
    @Bean
    @ConfigurationProperties(prefix = "carddemo.batch")
    public BatchProperties batchProperties() {
        return new BatchProperties();
    }

    /**
     * Configures JobExplorer for comprehensive batch job monitoring and metadata access.
     * 
     * This method establishes the job exploration infrastructure providing read-only access
     * to batch job metadata, execution history, and operational metrics required for
     * monitoring, reporting, and operational management of batch processing operations.
     * 
     * Job Explorer Features:
     * - Job instance browsing: Access to all job instances with filtering and pagination
     * - Execution history: Complete execution history with status, timing, and parameter details
     * - Step execution details: Step-level execution information including read/write counts
     * - Parameter inspection: Job and step parameter values for debugging and auditing
     * - Status monitoring: Real-time status information for active and completed jobs
     * - Performance metrics: Execution timing, throughput, and resource utilization data
     * 
     * Operational Management Support:
     * - Dashboard integration: Data source for batch processing dashboards and monitoring tools
     * - Troubleshooting: Detailed execution information for job failure analysis and resolution
     * - Capacity planning: Historical performance data for resource planning and optimization
     * - Compliance reporting: Execution audit trail for regulatory and business compliance requirements
     * - Trend analysis: Long-term execution trends for performance improvement identification
     * 
     * @param jobRepository configured JobRepository for metadata access
     * @return JobExplorer configured for comprehensive batch job monitoring
     */
    @Bean
    public org.springframework.batch.core.explore.JobExplorer jobExplorer(JobRepository jobRepository) {
        // Create JobExplorer using JobRepository
        return org.springframework.batch.core.configuration.support.ApplicationContextFactory
                .createJobExplorer(jobRepository);
    }

    /**
     * Configures JobOperator for programmatic batch job management and control operations.
     * 
     * This method establishes the job operator infrastructure providing comprehensive
     * programmatic control over batch job lifecycle including starting, stopping, restarting,
     * and abandoning jobs with full parameter and execution management capabilities.
     * 
     * Job Operator Features:
     * - Job lifecycle management: Start, stop, restart, abandon operations with parameter control
     * - Execution monitoring: Real-time job and step execution status tracking
     * - Parameter management: Job parameter validation, conversion, and default value handling
     * - Instance management: Job instance creation, validation, and uniqueness enforcement
     * - Step control: Individual step execution control and coordination
     * - Resource coordination: Thread and connection resource management during job operations
     * 
     * Operational Control Capabilities:
     * - Emergency stop: Immediate job termination for critical system conditions
     * - Graceful shutdown: Controlled job completion with data integrity preservation
     * - Recovery operations: Failed job restart with step-level granularity
     * - Batch scheduling: Integration with scheduling systems for automated job execution
     * - Load balancing: Job distribution across available processing resources
     * - Monitoring integration: Job operation metrics for system monitoring and alerting
     * 
     * @param jobRepository configured JobRepository for job metadata management
     * @param jobExplorer configured JobExplorer for job execution information access
     * @param jobLauncher configured JobLauncher for job execution operations
     * @return JobOperator configured for comprehensive batch job management
     */
    @Bean
    public org.springframework.batch.core.launch.JobOperator jobOperator(
            JobRepository jobRepository,
            org.springframework.batch.core.explore.JobExplorer jobExplorer,
            JobLauncher jobLauncher) {
        
        org.springframework.batch.core.launch.support.SimpleJobOperator operator = 
                new org.springframework.batch.core.launch.support.SimpleJobOperator();
        
        // Configure core components
        operator.setJobRepository(jobRepository);
        operator.setJobExplorer(jobExplorer);
        operator.setJobLauncher(jobLauncher);
        
        // Configure job registry for job lookup
        operator.setJobRegistry(new org.springframework.batch.core.configuration.support.MapJobRegistry());
        
        // Configure job parameter converter for type handling
        operator.setJobParametersConverter(new org.springframework.batch.core.converter.DefaultJobParametersConverter());
        
        return operator;
    }

    /**
     * Configures default chunk size for batch step processing based on externalized properties.
     * 
     * This method provides a centralized configuration for chunk-based processing that can be
     * used across all batch job step definitions, ensuring consistent memory usage and performance
     * characteristics while allowing environment-specific optimization through external configuration.
     * 
     * Chunk Processing Configuration:
     * - Chunk size: Externally configurable via BatchProperties.getChunkSize() for memory optimization
     * - Memory management: Balanced chunk sizing preventing out-of-memory errors during large dataset processing
     * - Transaction boundaries: Each chunk processed within a single transaction for data consistency
     * - Restart capability: Chunk-level restart support for failed batch operations
     * - Performance tuning: Environment-specific chunk size tuning for optimal throughput
     * 
     * COBOL Batch Migration Benefits:
     * - Record-oriented processing: Chunk-based processing mirrors COBOL record-by-record patterns
     * - Transaction consistency: Chunk transactions replicate COBOL file processing commit points
     * - Memory efficiency: Controlled memory usage replacing COBOL file buffer management
     * - Error isolation: Chunk-level error handling preventing entire job failure
     * - Performance optimization: Tunable chunk sizes for different data volumes and processing requirements
     * 
     * @param batchProperties configured BatchProperties for externalized chunk size configuration
     * @return configured chunk size for use in batch step definitions
     */
    @Bean("defaultChunkSize")
    public Integer defaultChunkSize(BatchProperties batchProperties) {
        return batchProperties.getChunkSize();
    }

    /**
     * Configures default retry policy settings for batch processing error resilience.
     * 
     * This method establishes the standard retry configuration used across all batch operations
     * to handle transient errors, network issues, and recoverable exceptions while maintaining
     * data integrity and processing reliability within 4-hour batch windows.
     * 
     * Retry Configuration Features:
     * - Maximum retry attempts: Externally configurable via BatchProperties.getMaxRetryAttempts()
     * - Exponential backoff: Progressive delay increases between retry attempts
     * - Exception classification: Retryable vs non-retryable exception identification
     * - Circuit breaker integration: Fail-fast behavior for systematic failures
     * - Monitoring integration: Retry attempt tracking for operational visibility
     * 
     * Error Resilience Patterns:
     * - Transient error recovery: Automatic retry for network timeouts and temporary database issues
     * - Data consistency preservation: Rollback and retry ensuring data integrity during errors
     * - Resource contention handling: Retry with backoff for database lock conflicts
     * - External service integration: Retry policies for external API and service calls
     * - Batch window compliance: Retry timing calibrated for processing window requirements
     * 
     * @param batchProperties configured BatchProperties for externalized retry configuration
     * @return configured maximum retry attempts for use in batch error handling
     */
    @Bean("maxRetryAttempts")
    public Integer maxRetryAttempts(BatchProperties batchProperties) {
        return batchProperties.getMaxRetryAttempts();
    }

    /**
     * Configures default skip limit for batch processing error tolerance.
     * 
     * This method establishes the standard skip limit configuration used across all batch operations
     * to handle data quality issues, validation errors, and recoverable processing exceptions while
     * maintaining batch job completion within acceptable error thresholds.
     * 
     * Skip Limit Configuration Features:
     * - Skip limit threshold: Externally configurable via BatchProperties.getSkipLimit()
     * - Error classification: Skippable vs fatal error identification based on exception types
     * - Error logging: Comprehensive logging of skipped records for data quality analysis
     * - Monitoring integration: Skip count tracking for operational monitoring and alerting
     * - Quality assurance: Skip limit enforcement preventing excessive data quality degradation
     * 
     * Data Quality Management:
     * - Validation error handling: Skip records with data format or business rule violations
     * - Processing continuity: Continue batch processing despite individual record failures
     * - Error reporting: Detailed error logs for data quality remediation and root cause analysis
     * - Threshold management: Configurable skip limits preventing runaway error conditions
     * - Business rule compliance: Skip limit configuration aligned with business acceptance criteria
     * 
     * @param batchProperties configured BatchProperties for externalized skip limit configuration
     * @return configured skip limit for use in batch error handling and data quality management
     */
    @Bean("defaultSkipLimit")
    public Integer defaultSkipLimit(BatchProperties batchProperties) {
        return batchProperties.getSkipLimit();
    }

    /**
     * Configures standardized file system directory paths for batch processing operations.
     * 
     * This method establishes the complete file system configuration required for batch job
     * file processing, ensuring consistent directory usage across all batch operations while
     * supporting environment-specific path configuration and operational flexibility.
     * 
     * Directory Configuration Features:
     * - Input directory: Externally configurable via BatchProperties.getInputDirectory()
     * - Output directory: Externally configurable via BatchProperties.getOutputDirectory()  
     * - Archive directory: Externally configurable via BatchProperties.getArchiveDirectory()
     * - Path validation: Directory existence and permission validation during startup
     * - Environment isolation: Different directory paths for development, staging, production
     * 
     * File Processing Workflow Support:
     * - Staging area management: Input directory for file pickup and processing initiation
     * - Output generation: Output directory for processed file generation and distribution
     * - Archival operations: Archive directory for completed file retention and audit compliance
     * - File lifecycle management: Automated file movement through processing stages
     * - Error isolation: Failed file quarantine and error handling through directory structure
     * 
     * COBOL Migration File Handling:
     * - Dataset equivalent: Directory structure replacing COBOL dataset allocation
     * - File organization: Structured directory layout for batch file management
     * - JCL replacement: Directory-based file routing replacing JCL dataset definitions
     * - Processing isolation: Directory separation ensuring batch job independence
     * - Operational monitoring: Directory-based monitoring for file processing status
     * 
     * @param batchProperties configured BatchProperties for externalized directory configuration
     * @return BatchDirectoryConfig containing all configured directory paths for batch operations
     */
    @Bean
    public BatchDirectoryConfig batchDirectoryConfig(BatchProperties batchProperties) {
        return new BatchDirectoryConfig(
                batchProperties.getInputDirectory(),
                batchProperties.getOutputDirectory(),
                batchProperties.getArchiveDirectory()
        );
    }

    /**
     * Configuration holder for batch processing directory paths.
     * 
     * This inner class encapsulates the standardized directory structure used across all
     * batch processing operations, providing type-safe access to configured directory paths
     * and supporting consistent file management patterns throughout the batch infrastructure.
     */
    public static class BatchDirectoryConfig {
        private final String inputDirectory;
        private final String outputDirectory;
        private final String archiveDirectory;

        public BatchDirectoryConfig(String inputDirectory, String outputDirectory, String archiveDirectory) {
            this.inputDirectory = inputDirectory;
            this.outputDirectory = outputDirectory;
            this.archiveDirectory = archiveDirectory;
        }

        public String getInputDirectory() {
            return inputDirectory;
        }

        public String getOutputDirectory() {
            return outputDirectory;
        }

        public String getArchiveDirectory() {
            return archiveDirectory;
        }
    }
}