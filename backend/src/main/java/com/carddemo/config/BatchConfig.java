package com.carddemo.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carddemo.service.TransactionReportService;

/**
 * Spring Batch configuration for CardDemo system batch processing.
 * Replaces JCL job definitions while maintaining identical processing sequences.
 * 
 * This configuration translates the COBOL batch job infrastructure to Spring Batch 5.x,
 * providing job restart and recovery mechanisms maintaining CICS SYNCPOINT integrity.
 * 
 * Key Features:
 * - Transaction report generation (replaces CBTRN03C.cbl)
 * - Job parameter support for date ranges and report types
 * - Error handling and restart capabilities
 * - Monitoring and logging integration
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    private static final Logger logger = LoggerFactory.getLogger(BatchConfig.class);

    @Autowired
    private TransactionReportService transactionReportService;

    /**
     * Main report generation job bean referenced by ReportService.
     * Replaces JCL job submission for transaction detail reports.
     * 
     * @param jobRepository Spring Batch job repository for metadata management
     * @param transactionManager Platform transaction manager for ACID compliance
     * @return Configured Job bean with qualifier "reportGenerationJob"
     */
    @Bean("reportGenerationJob")
    public Job reportGenerationJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        logger.info("Configuring reportGenerationJob for Spring Batch processing");
        
        return new JobBuilder("reportGenerationJob", jobRepository)
                .start(generateTransactionReportStep(jobRepository, transactionManager))
                .build();
    }

    /**
     * Transaction report generation step implementation.
     * Translates COBOL CBTRN03C.cbl batch program logic to Spring Batch step.
     * 
     * @param jobRepository Spring Batch job repository
     * @param transactionManager Platform transaction manager
     * @return Configured Step for transaction report processing
     */
    @Bean
    public Step generateTransactionReportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        logger.debug("Configuring generateTransactionReportStep");
        
        return new StepBuilder("generateTransactionReportStep", jobRepository)
                .tasklet(reportGenerationTasklet(), transactionManager)
                .build();
    }

    /**
     * Report generation tasklet that delegates to TransactionReportService.
     * Maintains the same business logic flow as the original COBOL program.
     * 
     * @return Tasklet implementation for report generation
     */
    @Bean
    public Tasklet reportGenerationTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("Starting transaction report generation tasklet");
            
            try {
                // Extract job parameters from Spring Batch context
                String reportType = (String) chunkContext.getStepContext()
                        .getJobParameters().get("reportType");
                String startDate = (String) chunkContext.getStepContext()
                        .getJobParameters().get("startDate");
                String endDate = (String) chunkContext.getStepContext()
                        .getJobParameters().get("endDate");
                
                logger.info("Processing report generation with parameters - Type: {}, Start: {}, End: {}", 
                           reportType, startDate, endDate);

                // Delegate to TransactionReportService for actual report generation
                // This maintains the COBOL CBTRN03C.cbl business logic flow
                String reportOutput = transactionReportService.generateTransactionReport(startDate, endDate);
                
                // Log completion status for monitoring
                if (reportOutput != null && !reportOutput.trim().isEmpty()) {
                    logger.info("Transaction report generated successfully. Output length: {} characters", 
                               reportOutput.length());
                    
                    // Store report content in execution context for retrieval
                    chunkContext.getStepContext().getStepExecution().getExecutionContext()
                            .put("reportContent", reportOutput);
                    chunkContext.getStepContext().getStepExecution().getExecutionContext()
                            .put("reportGeneratedAt", System.currentTimeMillis());
                    
                } else {
                    logger.warn("Transaction report generation completed but no output was produced");
                }

                logger.info("Transaction report generation tasklet completed successfully");
                return RepeatStatus.FINISHED;
                
            } catch (Exception e) {
                logger.error("Error during transaction report generation", e);
                
                // Store error information in execution context for troubleshooting
                chunkContext.getStepContext().getStepExecution().getExecutionContext()
                        .put("errorMessage", e.getMessage());
                chunkContext.getStepContext().getStepExecution().getExecutionContext()
                        .put("errorTimestamp", System.currentTimeMillis());
                
                throw new RuntimeException("Transaction report generation failed", e);
            }
        };
    }

    /**
     * Additional configuration for future batch jobs.
     * Placeholder for other JCL-to-Spring Batch conversions like:
     * - Daily interest calculation (CBACT04C.cbl)
     * - Statement generation (CBSTM03A/B.cbl)
     * - Transaction validation (CBTRN01C/02C.cbl)
     */
    
    // TODO: Add additional job configurations as needed:
    // @Bean("dailyInterestCalculationJob")
    // @Bean("statementGenerationJob") 
    // @Bean("transactionValidationJob")
    // etc.
}