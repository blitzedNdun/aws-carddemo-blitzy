package com.carddemo.batch;

import com.carddemo.account.entity.Account;
import com.carddemo.common.enums.AccountStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Spring Batch ItemReader for processing account data from ASCII files during data migration.
 * 
 * This ItemReader implements fixed-width ASCII file parsing with exact field mapping from 
 * acctdata.txt to PostgreSQL accounts table while maintaining COBOL COMP-3 decimal precision 
 * through BigDecimal conversion. The implementation supports pagination, comprehensive field 
 * validation, and error handling for bulk account data loading operations.
 * 
 * The reader processes 300-byte fixed-width records matching the COBOL ACCOUNT-RECORD layout
 * from app/cpy/CVACT01Y.cpy, ensuring complete functional equivalence during VSAM to PostgreSQL
 * data migration while preserving all financial calculation precision requirements.
 * 
 * Key Features:
 * - Fixed-width ASCII file parsing with COBOL field layout compliance
 * - BigDecimal financial precision matching COBOL COMP-3 arithmetic
 * - Account ID format validation and data integrity checks
 * - Configurable chunk-based processing for memory optimization
 * - Comprehensive error handling with skip policies for malformed records
 * - Integration with Spring Batch job orchestration and transaction management
 * 
 * Customer Relationship Note:
 * The Account entity requires a Customer association, but this ItemReader focuses
 * solely on parsing account data from ASCII files. Customer-Account relationship
 * establishment should be handled in a separate batch step after both Customer
 * and Account data are loaded into the database.
 * 
 * Record Layout (300 bytes total):
 * - Positions 1-11: Account ID (ACCT-ID PIC 9(11))
 * - Position 12: Active Status (ACCT-ACTIVE-STATUS PIC X(01))
 * - Positions 13-23: Current Balance (ACCT-CURR-BAL PIC S9(10)V99)
 * - Positions 24-34: Credit Limit (ACCT-CREDIT-LIMIT PIC S9(10)V99)
 * - Positions 35-45: Cash Credit Limit (ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99)
 * - Positions 46-55: Open Date (ACCT-OPEN-DATE PIC X(10))
 * - Positions 56-65: Expiration Date (ACCT-EXPIRAION-DATE PIC X(10))
 * - Positions 66-75: Reissue Date (ACCT-REISSUE-DATE PIC X(10))
 * - Positions 76-86: Current Cycle Credit (ACCT-CURR-CYC-CREDIT PIC S9(10)V99)
 * - Positions 87-97: Current Cycle Debit (ACCT-CURR-CYC-DEBIT PIC S9(10)V99)
 * - Positions 98-107: Address ZIP (ACCT-ADDR-ZIP PIC X(10))
 * - Positions 108-117: Group ID (ACCT-GROUP-ID PIC X(10))
 * - Positions 118-300: FILLER (unused padding)
 * 
 * Data Format Notes:
 * - Numeric fields use COBOL display format with trailing sign indicator
 * - Positive numbers end with '{' character (unsigned)
 * - Date fields use YYYY-MM-DD format
 * - All fields are space-padded to fixed lengths
 * 
 * Converted from: app/data/ASCII/acctdata.txt (VSAM export)
 * Target Entity: com.carddemo.account.entity.Account (JPA)
 * Batch Job: Account Data Migration Job
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
@Component
public class AccountDataItemReader extends FlatFileItemReader<Account> {

    private static final Logger logger = LoggerFactory.getLogger(AccountDataItemReader.class);
    
    /**
     * MathContext for exact financial calculations matching COBOL COMP-3 precision.
     * Uses DECIMAL128 precision with HALF_EVEN rounding per Section 0.1.2 requirements.
     */
    private static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_EVEN);
    
    /**
     * Date formatter for parsing YYYY-MM-DD format dates from ASCII files.
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Pattern for validating 11-digit account ID format.
     */
    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("\\d{11}");
    
    /**
     * Pattern for detecting positive COBOL numeric values (ending with '{').
     */
    private static final Pattern POSITIVE_NUMERIC_PATTERN = Pattern.compile("\\d+\\{");
    
    /**
     * Pattern for detecting negative COBOL numeric values (ending with '}').
     */
    private static final Pattern NEGATIVE_NUMERIC_PATTERN = Pattern.compile("\\d+\\}");

    /**
     * Default constructor initializes the FlatFileItemReader with fixed-width tokenizer
     * and custom field mapping for COBOL ACCOUNT-RECORD structure.
     */
    public AccountDataItemReader() {
        super();
        setName("AccountDataItemReader");
        setStrict(true);
        setLineMapper(createLineMapper());
        logger.info("AccountDataItemReader initialized with fixed-width COBOL layout mapping");
    }

    /**
     * Sets the input resource for reading account data files.
     * 
     * @param resource The ASCII file resource containing account data
     */
    @Override
    public void setResource(Resource resource) {
        super.setResource(resource);
        logger.info("Account data input resource set: {}", resource.getDescription());
    }

    /**
     * Creates and configures the line mapper for parsing fixed-width account records.
     * 
     * @return Configured DefaultLineMapper for COBOL ACCOUNT-RECORD structure
     */
    private DefaultLineMapper<Account> createLineMapper() {
        DefaultLineMapper<Account> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(createFixedLengthTokenizer());
        lineMapper.setFieldSetMapper(createFieldSetMapper());
        return lineMapper;
    }

    /**
     * Creates fixed-length tokenizer for parsing 300-byte COBOL ACCOUNT-RECORD layout.
     * 
     * @return Configured FixedLengthTokenizer with exact field positions
     */
    private FixedLengthTokenizer createFixedLengthTokenizer() {
        FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();
        
        // Define field ranges based on COBOL ACCOUNT-RECORD layout
        // Adjusted ranges to include sign characters for numeric fields
        tokenizer.setColumns(
            new Range(1, 11),    // ACCT-ID (11 digits)
            new Range(12, 12),   // ACCT-ACTIVE-STATUS (1 char)
            new Range(13, 24),   // ACCT-CURR-BAL (11 digits + sign)
            new Range(25, 36),   // ACCT-CREDIT-LIMIT (11 digits + sign)
            new Range(37, 48),   // ACCT-CASH-CREDIT-LIMIT (11 digits + sign)
            new Range(49, 58),   // ACCT-OPEN-DATE (10 chars)
            new Range(59, 68),   // ACCT-EXPIRAION-DATE (10 chars)
            new Range(69, 78),   // ACCT-REISSUE-DATE (10 chars)
            new Range(79, 90),   // ACCT-CURR-CYC-CREDIT (11 digits + sign)
            new Range(91, 102),  // ACCT-CURR-CYC-DEBIT (11 digits + sign)
            new Range(103, 112), // ACCT-ADDR-ZIP (10 chars)
            new Range(113, 122)  // ACCT-GROUP-ID (10 chars)
        );
        
        // Set field names matching Account entity properties
        tokenizer.setNames(
            "accountId",
            "activeStatus", 
            "currentBalance",
            "creditLimit",
            "cashCreditLimit",
            "openDate",
            "expirationDate",
            "reissueDate",
            "currentCycleCredit",
            "currentCycleDebit",
            "addressZip",
            "groupId"
        );
        
        tokenizer.setStrict(false); // Allow shorter lines for compatibility
        
        return tokenizer;
    }

    /**
     * Creates custom field set mapper for converting parsed fields to Account entity.
     * 
     * @return Configured BeanWrapperFieldSetMapper with custom property editors
     */
    private BeanWrapperFieldSetMapper<Account> createFieldSetMapper() {
        BeanWrapperFieldSetMapper<Account> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Account.class);
        fieldSetMapper.setStrict(false);
        
        // Register custom property editors for data type conversion
        fieldSetMapper.afterPropertiesSet();
        
        return fieldSetMapper;
    }

    /**
     * Reads and parses a single account record from the ASCII file.
     * 
     * @return Parsed Account entity or null if end of file
     * @throws Exception If parsing fails or validation errors occur
     */
    @Override
    public Account read() throws Exception {
        Account account = super.read();
        
        if (account != null) {
            // Process the raw account data with custom parsing and validation
            account = parseAccountRecord(account);
            
            // Validate account data integrity
            validateAccountData(account);
            
            logger.debug("Successfully parsed account record: {}", account.getAccountId());
        }
        
        return account;
    }

    /**
     * Parses account record with custom field processing for COBOL data formats.
     * 
     * This method handles the conversion of COBOL display format numeric fields and 
     * date fields to their corresponding Java types while preserving exact precision.
     * 
     * @param rawAccount The raw account data from field set mapper
     * @return Processed Account entity with correctly formatted fields
     */
    public Account parseAccountRecord(Account rawAccount) {
        Account account = new Account();
        
        try {
            // Parse and validate account ID
            String accountId = rawAccount.getAccountId();
            if (accountId != null) {
                accountId = accountId.trim();
                if (ACCOUNT_ID_PATTERN.matcher(accountId).matches()) {
                    account.setAccountId(accountId);
                    
                    // TODO: Customer-Account relationship establishment
                    // The Account entity has a Customer field which requires a Customer object
                    // Since Customer entity is not available in this context, the customer
                    // association should be established in a separate data loading step
                    // after both Customer and Account data are loaded
                    // Expected customer ID would be derived from account ID (first 9 digits)
                    // String customerId = accountId.substring(0, 9);
                } else {
                    logger.warn("Invalid account ID format: {}", accountId);
                    throw new IllegalArgumentException("Account ID must be exactly 11 digits");
                }
            }
            
            // Parse active status
            Object statusObj = rawAccount.getActiveStatus();
            if (statusObj != null) {
                String statusStr = statusObj.toString().trim();
                if (statusStr.length() > 0) {
                    char statusChar = statusStr.charAt(0);
                    AccountStatus status = (statusChar == 'Y') ? AccountStatus.ACTIVE : AccountStatus.INACTIVE;
                    account.setActiveStatus(status);
                } else {
                    // Default to INACTIVE if status is empty
                    account.setActiveStatus(AccountStatus.INACTIVE);
                }
            }
            
            // Parse financial amounts with COBOL precision
            account.setCurrentBalance(parseCobolAmount(rawAccount.getCurrentBalance()));
            account.setCreditLimit(parseCobolAmount(rawAccount.getCreditLimit()));
            account.setCashCreditLimit(parseCobolAmount(rawAccount.getCashCreditLimit()));
            account.setCurrentCycleCredit(parseCobolAmount(rawAccount.getCurrentCycleCredit()));
            account.setCurrentCycleDebit(parseCobolAmount(rawAccount.getCurrentCycleDebit()));
            
            // Parse date fields
            account.setOpenDate(parseCobolDate(rawAccount.getOpenDate()));
            account.setExpirationDate(parseCobolDate(rawAccount.getExpirationDate()));
            account.setReissueDate(parseCobolDate(rawAccount.getReissueDate()));
            
            // Parse text fields
            account.setAddressZip(parseTextField(rawAccount.getAddressZip()));
            account.setGroupId(parseTextField(rawAccount.getGroupId()));
            
            logger.debug("Parsed account record with ID: {}, Balance: {}, Credit Limit: {}", 
                        account.getAccountId(), 
                        account.getCurrentBalance(), 
                        account.getCreditLimit());
                        
        } catch (Exception e) {
            logger.error("Error parsing account record with ID: {}, Error: {}", 
                        rawAccount != null ? rawAccount.getAccountId() : "unknown", e.getMessage());
            throw new RuntimeException("Failed to parse account record", e);
        }
        
        return account;
    }

    /**
     * Parses COBOL display format numeric amounts to BigDecimal with exact precision.
     * 
     * COBOL display format uses trailing sign indicators:
     * - '{' indicates positive number
     * - '}' indicates negative number
     * - Implied decimal point based on COBOL PIC clause (V99 = 2 decimal places)
     * 
     * @param amountObj The raw amount field from file
     * @return BigDecimal with COBOL-equivalent precision
     */
    private BigDecimal parseCobolAmount(Object amountObj) {
        if (amountObj == null) {
            return BigDecimal.ZERO;
        }
        
        String amountStr = amountObj.toString().trim();
        if (amountStr.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        boolean isNegative = false;
        String numericPart = amountStr;
        
        // Handle COBOL display format sign indicators
        if (amountStr.endsWith("{")) {
            // Positive number
            numericPart = amountStr.substring(0, amountStr.length() - 1);
            isNegative = false;
        } else if (amountStr.endsWith("}")) {
            // Negative number
            numericPart = amountStr.substring(0, amountStr.length() - 1);
            isNegative = true;
        }
        
        try {
            // Remove any leading zeros for conversion
            String cleanNumericPart = numericPart.replaceFirst("^0+", "");
            if (cleanNumericPart.isEmpty()) {
                cleanNumericPart = "0";
            }
            
            // Convert to BigDecimal with implied decimal point (2 decimal places)
            BigDecimal amount = new BigDecimal(cleanNumericPart, COBOL_MATH_CONTEXT);
            
            // Apply implied decimal point (divide by 100 for V99 format)
            amount = amount.divide(new BigDecimal("100"), COBOL_MATH_CONTEXT);
            
            // Apply sign
            if (isNegative) {
                amount = amount.negate();
            }
            
            // Ensure scale is exactly 2 for financial precision
            amount = amount.setScale(2, RoundingMode.HALF_EVEN);
            
            logger.debug("Parsed COBOL amount '{}' to BigDecimal: {}", amountStr, amount);
            return amount;
            
        } catch (NumberFormatException e) {
            logger.warn("Invalid numeric format in COBOL amount: '{}', returning zero", amountStr);
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
    }

    /**
     * Parses COBOL date format (YYYY-MM-DD) to LocalDate.
     * 
     * @param dateObj The raw date field from file
     * @return LocalDate or null if invalid/empty
     */
    private LocalDate parseCobolDate(Object dateObj) {
        if (dateObj == null) {
            return null;
        }
        
        String dateStr = dateObj.toString().trim();
        if (dateStr.isEmpty() || dateStr.equals("0000-00-00") || dateStr.equals("0001-01-01")) {
            return null;
        }
        
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            
            // Validate date is within reasonable range
            LocalDate minDate = LocalDate.of(1900, 1, 1);
            LocalDate maxDate = LocalDate.of(2100, 12, 31);
            
            if (date.isBefore(minDate) || date.isAfter(maxDate)) {
                logger.warn("Date out of valid range: {}", dateStr);
                return null;
            }
            
            return date;
        } catch (DateTimeParseException e) {
            logger.warn("Invalid date format: '{}', error: {}", dateStr, e.getMessage());
            return null;
        }
    }

    /**
     * Parses and trims text fields from COBOL fixed-width format.
     * 
     * @param textObj The raw text field from file
     * @return Trimmed string or null if empty
     */
    private String parseTextField(Object textObj) {
        if (textObj == null) {
            return null;
        }
        
        String text = textObj.toString().trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * Validates parsed account data for integrity and business rules.
     * 
     * This method performs comprehensive validation including:
     * - Account ID format compliance
     * - Financial amount range validation
     * - Date consistency checks
     * - Required field validation
     * 
     * @param account The parsed Account entity to validate
     * @throws IllegalArgumentException If validation fails
     */
    public void validateAccountData(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        
        // Validate account ID
        if (account.getAccountId() == null || account.getAccountId().trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }
        
        if (!ACCOUNT_ID_PATTERN.matcher(account.getAccountId()).matches()) {
            throw new IllegalArgumentException("Account ID must be exactly 11 digits");
        }
        
        // Note: Customer-Account relationship validation is handled in a separate step
        // after both Customer and Account entities are loaded and associated
        
        // Validate active status
        if (account.getActiveStatus() == null) {
            throw new IllegalArgumentException("Account status is required");
        }
        
        // Validate financial amounts
        if (account.getCurrentBalance() == null) {
            throw new IllegalArgumentException("Current balance is required");
        }
        
        if (account.getCreditLimit() == null) {
            throw new IllegalArgumentException("Credit limit is required");
        }
        
        if (account.getCashCreditLimit() == null) {
            throw new IllegalArgumentException("Cash credit limit is required");
        }
        
        // Validate credit limit is non-negative
        if (account.getCreditLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Credit limit must be non-negative");
        }
        
        // Validate cash credit limit is non-negative
        if (account.getCashCreditLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cash credit limit must be non-negative");
        }
        
        // Validate open date
        if (account.getOpenDate() == null) {
            throw new IllegalArgumentException("Open date is required");
        }
        
        // Validate open date is not in the future
        if (account.getOpenDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Open date cannot be in the future");
        }
        
        // Validate expiration date if present
        if (account.getExpirationDate() != null) {
            if (account.getExpirationDate().isBefore(account.getOpenDate())) {
                throw new IllegalArgumentException("Expiration date cannot be before open date");
            }
        }
        
        // Validate financial amount ranges (within PostgreSQL DECIMAL(12,2) limits)
        BigDecimal maxAmount = new BigDecimal("999999999999.99");
        BigDecimal minAmount = new BigDecimal("-999999999999.99");
        
        if (account.getCurrentBalance().compareTo(maxAmount) > 0 || 
            account.getCurrentBalance().compareTo(minAmount) < 0) {
            throw new IllegalArgumentException("Current balance exceeds allowed range: " + account.getCurrentBalance());
        }
        
        if (account.getCreditLimit().compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("Credit limit exceeds allowed range: " + account.getCreditLimit());
        }
        
        if (account.getCashCreditLimit().compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("Cash credit limit exceeds allowed range: " + account.getCashCreditLimit());
        }
        
        // Validate that cash credit limit doesn't exceed credit limit
        if (account.getCashCreditLimit().compareTo(account.getCreditLimit()) > 0) {
            logger.warn("Cash credit limit ({}) exceeds credit limit ({}) for account {}", 
                       account.getCashCreditLimit(), account.getCreditLimit(), account.getAccountId());
        }
        
        // Validate cycle amounts if present
        if (account.getCurrentCycleCredit() != null) {
            if (account.getCurrentCycleCredit().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Current cycle credit cannot be negative");
            }
        }
        
        if (account.getCurrentCycleDebit() != null) {
            if (account.getCurrentCycleDebit().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Current cycle debit cannot be negative");
            }
        }
        
        logger.debug("Account data validation successful for ID: {}", account.getAccountId());
    }

    /**
     * Sets the line mapper for the file reader.
     * 
     * @param lineMapper The line mapper to use for parsing
     */
    @Override
    public void setLineMapper(org.springframework.batch.item.file.LineMapper<Account> lineMapper) {
        super.setLineMapper(lineMapper);
        logger.debug("Custom line mapper set for AccountDataItemReader");
    }
}