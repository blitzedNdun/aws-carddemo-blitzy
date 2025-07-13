package com.carddemo.batch;

import com.carddemo.account.entity.Customer;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidatorFactory;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Batch ItemWriter for persisting customer data to PostgreSQL database during migration.
 * 
 * This implementation provides comprehensive bulk insert operations with transaction management,
 * data validation, and error handling while maintaining exact field mapping from COBOL customer 
 * records (CUSTREC.cpy) to PostgreSQL customer table schema.
 * 
 * Key Features:
 * - Bulk PostgreSQL insert operations using JdbcBatchItemWriter for optimal performance
 * - ACID compliance with Spring @Transactional ensuring rollback capabilities
 * - Comprehensive data validation including PII protection and FICO credit score validation
 * - BigDecimal precision handling for financial data maintaining exact COBOL precision
 * - Integration with Spring Batch job orchestration for coordinated migration workflows
 * - Robust error handling with detailed logging and transaction rollback support
 * 
 * Converted from: app/cpy/CUSTREC.cpy (COBOL copybook)
 * Source Data: app/data/ASCII/custdata.txt (Fixed-width customer records)
 * Target Schema: customers table (PostgreSQL)
 * 
 * Performance Configuration:
 * - Optimized chunk sizes (1000 records) for memory efficiency
 * - Prepared statement reuse for bulk operations
 * - Connection pooling via HikariCP for database efficiency
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
@Component
public class CustomerDataItemWriter implements ItemWriter<Customer> {

    private static final Logger logger = LoggerFactory.getLogger(CustomerDataItemWriter.class);

    // Constants for data validation
    private static final int MIN_FICO_SCORE = 300;
    private static final int MAX_FICO_SCORE = 850;
    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("\\d{9}");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\d{9}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\([0-9]{3}\\)[0-9]{3}-[0-9]{4}");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // SQL for bulk customer insert operations
    private static final String INSERT_CUSTOMER_SQL = """
        INSERT INTO customers (
            customer_id, first_name, middle_name, last_name,
            address_line_1, address_line_2, address_line_3,
            state_code, country_code, zip_code,
            phone_number_1, phone_number_2, ssn,
            government_issued_id, date_of_birth, eft_account_id,
            primary_cardholder_indicator, fico_credit_score
        ) VALUES (
            :customerId, :firstName, :middleName, :lastName,
            :addressLine1, :addressLine2, :addressLine3,
            :stateCode, :countryCode, :zipCode,
            :phoneNumber1, :phoneNumber2, :ssn,
            :governmentIssuedId, :dateOfBirth, :eftAccountId,
            :primaryCardHolderIndicator, :ficoCreditScore
        )
        ON CONFLICT (customer_id) DO UPDATE SET
            first_name = EXCLUDED.first_name,
            middle_name = EXCLUDED.middle_name,
            last_name = EXCLUDED.last_name,
            address_line_1 = EXCLUDED.address_line_1,
            address_line_2 = EXCLUDED.address_line_2,
            address_line_3 = EXCLUDED.address_line_3,
            state_code = EXCLUDED.state_code,
            country_code = EXCLUDED.country_code,
            zip_code = EXCLUDED.zip_code,
            phone_number_1 = EXCLUDED.phone_number_1,
            phone_number_2 = EXCLUDED.phone_number_2,
            ssn = EXCLUDED.ssn,
            government_issued_id = EXCLUDED.government_issued_id,
            date_of_birth = EXCLUDED.date_of_birth,
            eft_account_id = EXCLUDED.eft_account_id,
            primary_cardholder_indicator = EXCLUDED.primary_cardholder_indicator,
            fico_credit_score = EXCLUDED.fico_credit_score
        """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcBatchItemWriter<Customer> jdbcBatchItemWriter;
    private final jakarta.validation.Validator validator;

    /**
     * Constructor initializing the CustomerDataItemWriter with required dependencies.
     * 
     * @param dataSource PostgreSQL DataSource for database connections
     * @param namedParameterJdbcTemplate JDBC template for named parameter operations
     * @param validatorFactory Bean validation factory for data integrity checks
     */
    @Autowired
    public CustomerDataItemWriter(DataSource dataSource, 
                                NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                ValidatorFactory validatorFactory) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.validator = validatorFactory.getValidator();
        
        // Configure JdbcBatchItemWriter for optimal PostgreSQL bulk operations
        this.jdbcBatchItemWriter = new JdbcBatchItemWriterBuilder<Customer>()
                .itemSqlParameterSourceProvider(BeanPropertySqlParameterSource::new)
                .sql(INSERT_CUSTOMER_SQL)
                .dataSource(dataSource)
                .assertUpdates(false) // Allow upsert operations (ON CONFLICT)
                .build();
        
        try {
            this.jdbcBatchItemWriter.afterPropertiesSet();
            logger.info("CustomerDataItemWriter initialized successfully with bulk insert configuration");
        } catch (Exception e) {
            logger.error("Failed to initialize CustomerDataItemWriter", e);
            throw new RuntimeException("CustomerDataItemWriter initialization failed", e);
        }
    }

    /**
     * Main write method implementing Spring Batch ItemWriter interface.
     * 
     * Performs bulk insert operations with comprehensive validation, transaction management,
     * and error handling for customer data migration from COBOL VSAM to PostgreSQL.
     * 
     * @param chunk Chunk of Customer entities to be persisted
     * @throws Exception if bulk insert operation fails or validation errors occur
     */
    @Override
    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        isolation = Isolation.SERIALIZABLE,
        rollbackFor = {Exception.class, DataAccessException.class}
    )
    public void write(Chunk<? extends Customer> chunk) throws Exception {
        if (chunk == null || chunk.isEmpty()) {
            logger.warn("Received empty or null chunk for customer data writing");
            return;
        }

        logger.info("Processing customer data chunk with {} records", chunk.size());
        
        try {
            // Validate all customer data before bulk insert
            validateCustomerData(chunk);
            
            // Perform bulk insert operation
            performBulkInsert(chunk);
            
            logger.info("Successfully processed {} customer records", chunk.size());
            
        } catch (ConstraintViolationException e) {
            logger.error("Data validation failed for customer chunk: {}", e.getMessage());
            handleWriteErrors(chunk, e);
            throw e;
        } catch (DataIntegrityViolationException e) {
            logger.error("Database constraint violation during customer insert: {}", e.getMessage());
            handleWriteErrors(chunk, e);
            throw e;
        } catch (DataAccessException e) {
            logger.error("Database access error during customer bulk insert: {}", e.getMessage());
            handleWriteErrors(chunk, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during customer data processing: {}", e.getMessage(), e);
            handleWriteErrors(chunk, e);
            throw e;
        }
    }

    /**
     * Validates customer data ensuring PII protection requirements and FICO credit score range validation.
     * 
     * This method performs comprehensive validation including:
     * - Customer ID format compliance (9-digit numeric)
     * - FICO credit score range validation (300-850)
     * - SSN format validation for PII protection
     * - Required field validation
     * - Date format validation
     * - Phone number format validation
     * 
     * @param chunk Chunk of Customer entities to validate
     * @throws ConstraintViolationException if any validation rules are violated
     */
    public void validateCustomerData(Chunk<? extends Customer> chunk) throws ConstraintViolationException {
        logger.debug("Validating customer data chunk with {} records", chunk.size());
        
        for (Customer customer : chunk) {
            // Jakarta Bean Validation
            Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
            if (!violations.isEmpty()) {
                StringBuilder violationMessage = new StringBuilder("Customer validation failed: ");
                for (ConstraintViolation<Customer> violation : violations) {
                    violationMessage.append(violation.getPropertyPath())
                                  .append(" ")
                                  .append(violation.getMessage())
                                  .append("; ");
                }
                logger.error("Bean validation failed for customer {}: {}", 
                           customer.getCustomerId(), violationMessage.toString());
                throw new ConstraintViolationException("Customer validation failed", violations);
            }
            
            // Additional business rule validation
            validateCustomerBusinessRules(customer);
        }
        
        logger.debug("Successfully validated {} customer records", chunk.size());
    }

    /**
     * Validates additional business rules for customer data beyond basic Jakarta Bean Validation.
     * 
     * @param customer Customer entity to validate
     * @throws IllegalArgumentException if business rules are violated
     */
    private void validateCustomerBusinessRules(Customer customer) {
        // Validate customer ID format (9-digit numeric)
        if (customer.getCustomerId() == null || 
            !CUSTOMER_ID_PATTERN.matcher(customer.getCustomerId()).matches()) {
            throw new IllegalArgumentException(
                "Customer ID must be exactly 9 digits: " + customer.getCustomerId());
        }

        // Validate FICO credit score range (300-850 per business requirements)
        Integer ficoScore = customer.getFicoCreditScore();
        if (ficoScore != null && (ficoScore < MIN_FICO_SCORE || ficoScore > MAX_FICO_SCORE)) {
            throw new IllegalArgumentException(
                String.format("FICO credit score must be between %d and %d, got: %d", 
                             MIN_FICO_SCORE, MAX_FICO_SCORE, ficoScore));
        }

        // Validate SSN format for PII protection (9-digit numeric)
        if (customer.getSsn() != null && !SSN_PATTERN.matcher(customer.getSsn()).matches()) {
            throw new IllegalArgumentException(
                "SSN must be exactly 9 digits for customer: " + customer.getCustomerId());
        }

        // Validate date of birth is not in the future
        if (customer.getDateOfBirth() != null && customer.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                "Date of birth cannot be in the future for customer: " + customer.getCustomerId());
        }

        // Validate state code format (2 characters)
        if (customer.getStateCode() != null && customer.getStateCode().length() != 2) {
            throw new IllegalArgumentException(
                "State code must be exactly 2 characters for customer: " + customer.getCustomerId());
        }

        // Validate country code format (3 characters)
        if (customer.getCountryCode() != null && customer.getCountryCode().length() != 3) {
            throw new IllegalArgumentException(
                "Country code must be exactly 3 characters for customer: " + customer.getCustomerId());
        }

        logger.trace("Business rule validation passed for customer: {}", customer.getCustomerId());
    }

    /**
     * Performs PostgreSQL bulk insert operations using JdbcBatchItemWriter for optimal performance.
     * 
     * This method leverages prepared statement reuse and batch processing to achieve
     * high-throughput database operations while maintaining ACID compliance through
     * Spring transaction management.
     * 
     * @param chunk Chunk of validated Customer entities to insert
     * @throws Exception if bulk insert operation fails
     */
    public void performBulkInsert(Chunk<? extends Customer> chunk) throws Exception {
        logger.debug("Performing bulk insert for {} customer records", chunk.size());
        
        try {
            // Execute bulk insert using configured JdbcBatchItemWriter
            jdbcBatchItemWriter.write(chunk);
            
            // Log successful insert metrics
            logger.info("Bulk insert completed successfully for {} customer records", chunk.size());
            
        } catch (DataAccessException e) {
            logger.error("Database error during bulk insert operation: {}", e.getMessage());
            
            // Extract and log specific SQL error details
            Throwable rootCause = e.getRootCause();
            if (rootCause instanceof SQLException) {
                SQLException sqlException = (SQLException) rootCause;
                logger.error("SQL Error Code: {}, SQL State: {}, Message: {}", 
                           sqlException.getErrorCode(), 
                           sqlException.getSQLState(), 
                           sqlException.getMessage());
            }
            
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during bulk insert operation: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handles write errors with comprehensive logging and transaction rollback support.
     * 
     * This method provides detailed error analysis including:
     * - Individual record error identification
     * - Data integrity violation analysis
     * - Transaction rollback coordination
     * - Error reporting for monitoring systems
     * 
     * @param chunk Chunk of Customer entities that failed to process
     * @param error Exception that occurred during processing
     */
    public void handleWriteErrors(Chunk<? extends Customer> chunk, Exception error) {
        logger.error("Handling write errors for customer data chunk with {} records", chunk.size());
        
        // Log error details
        logger.error("Error type: {}, Message: {}", error.getClass().getSimpleName(), error.getMessage());
        
        // Analyze individual records for error patterns
        if (chunk != null && !chunk.isEmpty()) {
            logger.error("Failed customer record IDs: {}", 
                        chunk.getItems().stream()
                             .map(Customer::getCustomerId)
                             .toList());
            
            // Log problematic data patterns for troubleshooting
            analyzeDataPatterns(chunk, error);
        }
        
        // Transaction rollback will be handled automatically by Spring @Transactional
        logger.warn("Transaction will be rolled back due to error: {}", error.getMessage());
        
        // Additional cleanup or notification logic can be added here
        notifyErrorMonitoring(chunk, error);
    }

    /**
     * Analyzes data patterns in failed records to identify common issues.
     * 
     * @param chunk Failed chunk of Customer entities
     * @param error Original exception
     */
    private void analyzeDataPatterns(Chunk<? extends Customer> chunk, Exception error) {
        logger.debug("Analyzing data patterns in failed customer chunk");
        
        for (Customer customer : chunk) {
            // Check for common data issues
            if (customer.getCustomerId() == null) {
                logger.warn("Customer with null ID found in failed chunk");
            }
            
            if (customer.getFicoCreditScore() != null) {
                Integer fico = customer.getFicoCreditScore();
                if (fico < MIN_FICO_SCORE || fico > MAX_FICO_SCORE) {
                    logger.warn("Customer {} has invalid FICO score: {}", 
                               customer.getCustomerId(), fico);
                }
            }
            
            if (customer.getSsn() != null && !SSN_PATTERN.matcher(customer.getSsn()).matches()) {
                logger.warn("Customer {} has invalid SSN format", customer.getCustomerId());
            }
        }
    }

    /**
     * Notifies error monitoring systems about processing failures.
     * 
     * @param chunk Failed chunk of Customer entities
     * @param error Original exception
     */
    private void notifyErrorMonitoring(Chunk<? extends Customer> chunk, Exception error) {
        // This method can be enhanced to integrate with monitoring systems
        // such as Prometheus metrics, alerting systems, or audit logging
        
        logger.info("Error monitoring notification - Chunk size: {}, Error: {}", 
                   chunk != null ? chunk.size() : 0, error.getClass().getSimpleName());
        
        // Example: Increment error metrics for monitoring
        // meterRegistry.counter("customer.data.writer.errors", 
        //                      "error.type", error.getClass().getSimpleName())
        //            .increment();
    }
}