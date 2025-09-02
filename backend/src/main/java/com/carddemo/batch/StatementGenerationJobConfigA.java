package com.carddemo.batch;

import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Batch job configuration for partitioned statement generation processing accounts A-M.
 * Defines job steps, partitioning strategy, parallel execution settings, and resource management
 * for statement generation part A. Coordinates with StatementGenerationBatchServiceA to implement
 * parallel processing within the 4-hour batch window requirement.
 * 
 * This configuration implements:
 * - Account ID prefix filtering for accounts A-M 
 * - Parallel step execution with thread pool sizing for performance
 * - Job parameters, restart capabilities, and error handling
 * - Integration with PostgreSQL database through JPA
 * 
 * Performance targets:
 * - Complete processing within 4-hour batch window
 * - Support parallel processing of account ranges
 * - Handle restart scenarios with checkpoint recovery
 * - Maintain transactional integrity for financial data
 */
@Profile({"!test", "!unit-test"})
@Configuration
public class StatementGenerationJobConfigA {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    // Chunk size for batch processing - optimized for memory usage and commit frequency
    private static final int CHUNK_SIZE = 1000;
    
    // Thread pool size for parallel processing - tuned for 4-hour window requirement
    private static final int THREAD_POOL_SIZE = 4;
    
    // Core pool size for task executor
    private static final int CORE_POOL_SIZE = 2;
    
    // Maximum pool size for peak load handling
    private static final int MAX_POOL_SIZE = 8;
    
    // Account range for partition A (accounts starting with A-M)
    private static final String ACCOUNT_RANGE_START = "A";
    private static final String ACCOUNT_RANGE_END = "M";

    /**
     * Main Spring Batch job configuration for statement generation part A.
     * Configures job flow, restart capabilities, and step orchestration.
     * 
     * @return Job configured job for statement generation processing
     */
    @Bean
    public Job statementGenerationJobA() {
        return new JobBuilder("statementGenerationJobA", jobRepository)
                .start(stepPartitionerA())
                .incrementer(new RunIdIncrementer())
                // Restart capability enabled by default for failed jobs
                .listener(new StatementGenerationJobListener())
                .build();
    }

    /**
     * Partitioning handler configuration for parallel processing.
     * Manages distribution of work across multiple threads and handles
     * partition coordination.
     * 
     * @return PartitionHandler configured partition handler
     */
    @Bean
    public PartitionHandler partitioningHandler() {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setTaskExecutor(statementGenerationTaskExecutor());
        partitionHandler.setStep(statementGenerationStepA());
        partitionHandler.setGridSize(THREAD_POOL_SIZE);
        
        try {
            partitionHandler.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize partition handler", e);
        }
        
        return partitionHandler;
    }

    /**
     * Manager step for partitioning account ranges A-M.
     * Coordinates the distribution of accounts across worker threads
     * and manages partition execution.
     * 
     * @return Step configured partitioner step
     */
    @Bean
    public Step stepPartitionerA() {
        return new StepBuilder("stepPartitionerA", jobRepository)
                .partitioner("statementGenerationStepA", accountRangePartitioner())
                .partitionHandler(partitioningHandler())
                .build();
    }

    /**
     * Custom partitioner implementation for account range A-M.
     * Creates partitions based on account ID prefixes to enable
     * parallel processing of statement generation.
     * 
     * @return Partitioner configured account range partitioner
     */
    @Bean
    public Partitioner accountRangePartitioner() {
        return new Partitioner() {
            @Override
            public Map<String, ExecutionContext> partition(int gridSize) {
                Map<String, ExecutionContext> partitions = new HashMap<>();
                
                // Define account ranges for parallel processing
                // Partition accounts A-M across available threads
                String[] accountPrefixes = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M"};
                
                int partitionSize = Math.max(1, accountPrefixes.length / gridSize);
                int partitionIndex = 0;
                
                for (int i = 0; i < accountPrefixes.length; i += partitionSize) {
                    ExecutionContext context = new ExecutionContext();
                    
                    // Set range boundaries for this partition
                    String rangeStart = accountPrefixes[i];
                    String rangeEnd = (i + partitionSize - 1 < accountPrefixes.length) 
                                    ? accountPrefixes[Math.min(i + partitionSize - 1, accountPrefixes.length - 1)]
                                    : accountPrefixes[accountPrefixes.length - 1];
                    
                    context.putString("accountRangeStart", rangeStart);
                    context.putString("accountRangeEnd", rangeEnd);
                    context.putInt("partitionNumber", partitionIndex);
                    
                    // Add partition-specific parameters for processing
                    context.putString("partitionName", "partition-" + rangeStart + "-" + rangeEnd);
                    context.putLong("timestamp", System.currentTimeMillis());
                    
                    partitions.put("partition" + partitionIndex, context);
                    partitionIndex++;
                }
                
                return partitions;
            }
        };
    }

    /**
     * Task executor configuration for parallel processing.
     * Configures thread pool for optimal performance within
     * the 4-hour batch processing window.
     * 
     * @return TaskExecutor configured thread pool task executor
     */
    @Bean
    @Qualifier("statementGenerationTaskExecutor")
    public TaskExecutor statementGenerationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("StatementGenA-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(300);
        executor.initialize();
        return executor;
    }

    /**
     * Worker step configuration for statement generation processing.
     * Implements chunk-oriented processing for account data within
     * specified ranges with transaction management and error handling.
     * 
     * @return Step configured worker step for statement processing
     */
    @Bean
    public Step statementGenerationStepA() {
        return new StepBuilder("statementGenerationStepA", jobRepository)
                .<AccountStatementData, ProcessedStatement>chunk(CHUNK_SIZE, transactionManager)
                .reader(accountStatementReader())
                .processor(statementProcessor())
                .writer(statementWriter())
                .faultTolerant()
                .skipLimit(100)
                .skip(Exception.class)
                .retryLimit(3)
                .retry(Exception.class)
                .listener(new StatementGenerationStepListener())
                .build();
    }

    /**
     * JPA-based item reader for account statement data.
     * Reads account data within the specified range for statement generation.
     */
    @Bean
    @org.springframework.context.annotation.Scope(value = "step", proxyMode = org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS)
    public ItemReader<AccountStatementData> accountStatementReader() {
        return new JpaPagingItemReaderBuilder<AccountStatementData>()
                .name("accountStatementReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT a FROM AccountStatementData a WHERE a.accountId LIKE :accountRangeStart% " +
                           "AND a.accountId <= :accountRangeEnd ORDER BY a.accountId")
                .parameterValues(Map.of(
                    "accountRangeStart", "#{stepExecutionContext['accountRangeStart']}",
                    "accountRangeEnd", "#{stepExecutionContext['accountRangeEnd']}"
                ))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    /**
     * Statement processing logic for transforming account data into statements.
     * Implements business rules for statement generation and formatting.
     */
    @Bean
    public ItemProcessor<AccountStatementData, ProcessedStatement> statementProcessor() {
        return new ItemProcessor<AccountStatementData, ProcessedStatement>() {
            @Override
            public ProcessedStatement process(AccountStatementData account) throws Exception {
                // Implement statement generation logic
                ProcessedStatement statement = new ProcessedStatement();
                statement.setAccountId(account.getAccountId());
                statement.setCustomerId(account.getCustomerId());
                statement.setStatementDate(java.time.LocalDate.now());
                statement.setProcessingPartition(account.getAccountId().substring(0, 1));
                
                // Generate statement content and formatting
                statement.setStatementContent(generateStatementContent(account));
                statement.setProcessingStatus("PROCESSED");
                statement.setProcessingTimestamp(java.time.LocalDateTime.now());
                
                return statement;
            }
            
            private String generateStatementContent(AccountStatementData account) {
                // Statement generation business logic
                StringBuilder content = new StringBuilder();
                content.append("STATEMENT FOR ACCOUNT: ").append(account.getAccountId()).append("\n");
                content.append("CUSTOMER: ").append(account.getCustomerId()).append("\n");
                content.append("CURRENT BALANCE: ").append(account.getCurrentBalance()).append("\n");
                content.append("STATEMENT PERIOD: ").append(java.time.LocalDate.now().minusMonths(1))
                       .append(" TO ").append(java.time.LocalDate.now()).append("\n");
                
                // Add transaction details and summary information
                content.append("TRANSACTIONS:\n");
                if (account.getTransactions() != null) {
                    account.getTransactions().forEach(transaction -> {
                        content.append("  ").append(transaction.getTransactionDate())
                               .append(" ").append(transaction.getAmount())
                               .append(" ").append(transaction.getDescription()).append("\n");
                    });
                }
                
                return content.toString();
            }
        };
    }

    /**
     * JPA-based item writer for processed statements.
     * Persists generated statements to the database with transactional integrity.
     */
    @Bean
    public ItemWriter<ProcessedStatement> statementWriter() {
        return new JpaItemWriterBuilder<ProcessedStatement>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    /**
     * Job execution listener for monitoring and logging.
     * Provides job-level monitoring and error handling capabilities.
     */
    private static class StatementGenerationJobListener implements JobExecutionListener {
        @Override
        public void beforeJob(JobExecution jobExecution) {
            System.out.println("Starting Statement Generation Job A for accounts A-M at: " 
                             + java.time.LocalDateTime.now());
            jobExecution.getExecutionContext().putLong("startTime", System.currentTimeMillis());
        }

        @Override
        public void afterJob(JobExecution jobExecution) {
            long startTime = jobExecution.getExecutionContext().getLong("startTime", 0L);
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("Completed Statement Generation Job A. Status: " 
                             + jobExecution.getStatus() + ", Duration: " + duration + "ms");
            
            if (jobExecution.getStatus() == BatchStatus.FAILED) {
                System.err.println("Job failed with exceptions: " + jobExecution.getAllFailureExceptions());
            }
        }
    }

    /**
     * Step execution listener for monitoring step-level progress.
     * Provides detailed monitoring of partition processing performance.
     */
    private static class StatementGenerationStepListener implements StepExecutionListener {
        @Override
        public void beforeStep(StepExecution stepExecution) {
            String partitionName = stepExecution.getExecutionContext().getString("partitionName", "unknown");
            System.out.println("Starting step execution for partition: " + partitionName);
        }

        @Override
        public ExitStatus afterStep(StepExecution stepExecution) {
            String partitionName = stepExecution.getExecutionContext().getString("partitionName", "unknown");
            System.out.println("Completed step execution for partition: " + partitionName 
                             + ", Read: " + stepExecution.getReadCount()
                             + ", Written: " + stepExecution.getWriteCount()
                             + ", Skipped: " + stepExecution.getSkipCount());
            
            return stepExecution.getExitStatus();
        }
    }

    /**
     * Data transfer object for account statement data.
     * Represents the input data structure for statement generation processing.
     */
    public static class AccountStatementData {
        private String accountId;
        private String customerId;
        private java.math.BigDecimal currentBalance;
        private java.util.List<TransactionData> transactions;

        // Constructors
        public AccountStatementData() {}

        public AccountStatementData(String accountId, String customerId, java.math.BigDecimal currentBalance) {
            this.accountId = accountId;
            this.customerId = customerId;
            this.currentBalance = currentBalance;
        }

        // Getters and setters
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public java.math.BigDecimal getCurrentBalance() { return currentBalance; }
        public void setCurrentBalance(java.math.BigDecimal currentBalance) { this.currentBalance = currentBalance; }

        public java.util.List<TransactionData> getTransactions() { return transactions; }
        public void setTransactions(java.util.List<TransactionData> transactions) { this.transactions = transactions; }
    }

    /**
     * Data transfer object for transaction data.
     * Represents transaction information for statement generation.
     */
    public static class TransactionData {
        private java.time.LocalDate transactionDate;
        private java.math.BigDecimal amount;
        private String description;

        // Constructors
        public TransactionData() {}

        public TransactionData(java.time.LocalDate transactionDate, java.math.BigDecimal amount, String description) {
            this.transactionDate = transactionDate;
            this.amount = amount;
            this.description = description;
        }

        // Getters and setters
        public java.time.LocalDate getTransactionDate() { return transactionDate; }
        public void setTransactionDate(java.time.LocalDate transactionDate) { this.transactionDate = transactionDate; }

        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * Data transfer object for processed statement output.
     * Represents the generated statement data for persistence.
     */
    public static class ProcessedStatement {
        private String accountId;
        private String customerId;
        private java.time.LocalDate statementDate;
        private String processingPartition;
        private String statementContent;
        private String processingStatus;
        private java.time.LocalDateTime processingTimestamp;

        // Constructors
        public ProcessedStatement() {}

        // Getters and setters
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public java.time.LocalDate getStatementDate() { return statementDate; }
        public void setStatementDate(java.time.LocalDate statementDate) { this.statementDate = statementDate; }

        public String getProcessingPartition() { return processingPartition; }
        public void setProcessingPartition(String processingPartition) { this.processingPartition = processingPartition; }

        public String getStatementContent() { return statementContent; }
        public void setStatementContent(String statementContent) { this.statementContent = statementContent; }

        public String getProcessingStatus() { return processingStatus; }
        public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }

        public java.time.LocalDateTime getProcessingTimestamp() { return processingTimestamp; }
        public void setProcessingTimestamp(java.time.LocalDateTime processingTimestamp) { this.processingTimestamp = processingTimestamp; }
    }
}