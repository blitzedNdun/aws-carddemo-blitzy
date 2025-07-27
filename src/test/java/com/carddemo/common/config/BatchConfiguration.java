package com.carddemo.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;

import javax.sql.DataSource;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test-specific Spring Batch configuration providing simplified batch processing infrastructure
 * for CardDemo unit tests. This configuration creates the exact BatchConfiguration type that
 * batch job classes expect while providing minimal test-friendly implementations of all required methods.
 * 
 * This class replaces the main BatchConfiguration during test execution by being enabled only
 * for the 'test' profile, ensuring batch jobs can be properly instantiated and tested.
 */
@TestConfiguration
@EnableBatchProcessing
@EnableConfigurationProperties(BatchProperties.class)
@Profile("unit-test")
public class BatchConfiguration extends DefaultBatchConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(BatchConfiguration.class);

    // Test-specific constants with simplified values
    public static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_UP);
    private static final int TEST_CHUNK_SIZE = 10;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    // Test-specific metrics configuration
    private boolean enableMetrics = false;

    /**
     * Test JobRepository bean providing minimal job execution metadata persistence
     */
    @Bean
    @Primary
    @Override
    public JobRepository jobRepository() {
        logger.debug("Initializing Test JobRepository");
        return super.jobRepository();
    }

    /**
     * Test JobLauncher bean for test job execution
     */
    @Bean
    @Primary
    @Override
    public JobLauncher jobLauncher() {
        logger.debug("Initializing Test JobLauncher");
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository());
        jobLauncher.setTaskExecutor(batchTaskExecutor());
        try {
            jobLauncher.afterPropertiesSet();
        } catch (Exception e) {
            logger.error("Failed to initialize Test JobLauncher", e);
            throw new RuntimeException("Test JobLauncher initialization failed", e);
        }
        return jobLauncher;
    }

    /**
     * Simplified TaskExecutor for test scenarios
     */
    @Bean
    public TaskExecutor batchTaskExecutor() {
        logger.debug("Configuring Test TaskExecutor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("test-batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        
        executor.initialize();
        
        return executor;
    }

    /**
     * Test chunk size configuration - reduced for faster test execution
     */
    @Bean
    public int chunkSize() {
        logger.debug("Configured test batch chunk size: {} records per transaction", TEST_CHUNK_SIZE);
        return TEST_CHUNK_SIZE;
    }

    /**
     * Simplified retry policy for tests - no retries for fast failure
     */
    @Bean
    public ExceptionClassifierRetryPolicy retryPolicy() {
        logger.debug("Configuring test retry policy (no retries)");

        ExceptionClassifierRetryPolicy retryPolicy = new ExceptionClassifierRetryPolicy();
        
        // No retries in test - fail fast
        Map<Class<? extends Throwable>, org.springframework.retry.RetryPolicy> policyMap = 
            Map.of(Exception.class, createNoRetryPolicy());
        
        retryPolicy.setPolicyMap(policyMap);
        return retryPolicy;
    }

    /**
     * Test skip policy - never skip in tests for deterministic behavior
     */
    @Bean
    public SkipPolicy skipPolicy() {
        logger.debug("Configuring test skip policy (never skip)");
        return new NeverSkipItemSkipPolicy();
    }

    /**
     * CardDemo job execution listener for test environment
     * Copied from main BatchConfiguration to ensure method signature compatibility
     */
    public static class CardDemoJobExecutionListener extends JobExecutionListenerSupport {
        private final Logger logger = LoggerFactory.getLogger(CardDemoJobExecutionListener.class);
        private final MeterRegistry meterRegistry;
        private final boolean metricsEnabled;

        public CardDemoJobExecutionListener(MeterRegistry meterRegistry, boolean metricsEnabled) {
            this.meterRegistry = meterRegistry;
            this.metricsEnabled = metricsEnabled;
        }

        @Override
        public void beforeJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            logger.debug("Starting test batch job: {}", jobName);
        }

        @Override
        public void afterJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            ExitStatus exitStatus = jobExecution.getExitStatus();
            logger.debug("Completed test batch job: {} with status: {}", jobName, exitStatus.getExitCode());
            
            // Skip metrics collection in test environment
        }
    }

    /**
     * Test job execution listener - returns CardDemoJobExecutionListener to match batch job expectations
     */
    @Bean
    public CardDemoJobExecutionListener jobExecutionListener() {
        logger.debug("Creating test job execution listener with metrics disabled");
        return new CardDemoJobExecutionListener(meterRegistry, enableMetrics);
    }

    /**
     * Test step execution listener - returns CardDemoStepExecutionListener to match batch job expectations
     */
    @Bean
    public CardDemoStepExecutionListener stepExecutionListener() {
        logger.debug("Creating test step execution listener with metrics disabled");
        return new CardDemoStepExecutionListener(meterRegistry, enableMetrics);
    }

    /**
     * CardDemo job parameters validator for test environment
     * Copied from main BatchConfiguration to ensure method signature compatibility
     */
    public static class CardDemoJobParametersValidator implements JobParametersValidator {
        private final Logger logger = LoggerFactory.getLogger(CardDemoJobParametersValidator.class);

        @Override
        public void validate(JobParameters parameters) throws JobParametersInvalidException {
            logger.debug("Validating job parameters: {}", parameters.getParameters().keySet());

            // Relaxed validation for test environment - only validate critical parameters
            if (parameters.getString("run.id") == null) {
                logger.debug("No run.id parameter found, using test default");
                // In test environment, allow missing run.id
            }

            // Validate business date parameter if present
            String businessDate = parameters.getString("business.date");
            if (businessDate != null) {
                try {
                    LocalDateTime.parse(businessDate + "T00:00:00");
                } catch (Exception e) {
                    throw new JobParametersInvalidException(
                        "Invalid business.date format. Expected YYYY-MM-DD: " + businessDate);
                }
            }

            // Validate numeric parameters
            Long maxRecords = parameters.getLong("max.records");
            if (maxRecords != null && maxRecords <= 0) {
                throw new JobParametersInvalidException(
                    "Parameter 'max.records' must be positive: " + maxRecords);
            }

            logger.debug("Job parameters validation successful");
        }
    }

    /**
     * Test job parameters validator - returns CardDemoJobParametersValidator to match batch job expectations
     */
    @Bean
    public CardDemoJobParametersValidator jobParametersValidator() {
        logger.debug("Creating test job parameters validator with relaxed validation");
        return new CardDemoJobParametersValidator();
    }

    /**
     * Test transaction manager - renamed to avoid conflicts with main DatabaseConfig transactionManager
     * Note: Not marked as @Primary to avoid conflicts with main DatabaseConfig transactionManager
     */
    @Bean("batchTransactionManager")
    public PlatformTransactionManager batchTransactionManager() {
        logger.debug("Configuring test transaction manager");
        return getTransactionManager();
    }

    // Helper method to create no-retry policy
    private SimpleRetryPolicy createNoRetryPolicy() {
        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(1); // No retries in tests
        return policy;
    }

    /**
     * CardDemo step execution listener for test environment
     * Copied from main BatchConfiguration to ensure method signature compatibility
     */
    public static class CardDemoStepExecutionListener extends StepExecutionListenerSupport {
        private final Logger logger = LoggerFactory.getLogger(CardDemoStepExecutionListener.class);
        private final MeterRegistry meterRegistry;
        private final boolean metricsEnabled;

        public CardDemoStepExecutionListener(MeterRegistry meterRegistry, boolean metricsEnabled) {
            this.meterRegistry = meterRegistry;
            this.metricsEnabled = metricsEnabled;
        }

        @Override
        public void beforeStep(StepExecution stepExecution) {
            String stepName = stepExecution.getStepName();
            logger.debug("Starting test batch step: {}", stepName);
        }

        @Override
        public ExitStatus afterStep(StepExecution stepExecution) {
            String stepName = stepExecution.getStepName();
            ExitStatus exitStatus = stepExecution.getExitStatus();
            logger.debug("Completed test batch step: {} with status: {}", stepName, exitStatus.getExitCode());
            
            // Skip metrics collection in test environment
            return exitStatus;
        }
    }

    /**
     * Minimal test job execution listener
     */
    public static class TestJobExecutionListener extends JobExecutionListenerSupport {
        private final Logger logger = LoggerFactory.getLogger(TestJobExecutionListener.class);

        @Override
        public void beforeJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            logger.debug("Starting test job: {}", jobName);
        }

        @Override
        public void afterJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            ExitStatus exitStatus = jobExecution.getExitStatus();
            logger.debug("Completed test job: {} with status: {}", jobName, exitStatus.getExitCode());
        }
    }

    /**
     * Minimal test step execution listener
     */
    public static class TestStepExecutionListener extends StepExecutionListenerSupport {
        private final Logger logger = LoggerFactory.getLogger(TestStepExecutionListener.class);

        @Override
        public void beforeStep(StepExecution stepExecution) {
            String stepName = stepExecution.getStepName();
            logger.debug("Starting test step: {}", stepName);
        }

        @Override
        public ExitStatus afterStep(StepExecution stepExecution) {
            String stepName = stepExecution.getStepName();
            ExitStatus exitStatus = stepExecution.getExitStatus();
            logger.debug("Completed test step: {} with status: {}", stepName, exitStatus.getExitCode());
            return exitStatus;
        }
    }

    /**
     * Minimal test job parameters validator
     */
    public static class TestJobParametersValidator implements JobParametersValidator {
        private final Logger logger = LoggerFactory.getLogger(TestJobParametersValidator.class);

        @Override
        public void validate(JobParameters parameters) throws JobParametersInvalidException {
            logger.debug("Validating test job parameters (minimal validation)");
            // Minimal validation for tests - just log
        }
    }
}