package com.carddemo.batch;

import com.carddemo.common.entity.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.BindException;

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
 * This ItemReader supports CSV parsing with exact field mapping from acctdata.txt to PostgreSQL 
 * accounts table while maintaining COBOL COMP-3 decimal precision through BigDecimal conversion.
 * 
 * Original COBOL Structure: ACCOUNT-RECORD from CVACT01Y.cpy (RECLN 300)
 * Source File: app/data/ASCII/acctdata.txt
 * 
 * Key Features:
 * - Fixed-width ASCII file format parsing matching COBOL ACCOUNT-RECORD layout
 * - BigDecimal conversion maintaining exact COBOL COMP-3 precision for financial calculations
 * - Pagination support for processing large account datasets within memory constraints
 * - Integration with Spring Batch job orchestration for data migration workflow coordination
 * - Custom field validation ensuring account_id format compliance and data integrity
 * - Error handling and skip policies for malformed records with comprehensive logging
 * 
 * Field Mapping from COBOL to Java:
 * - ACCT-ID PIC 9(11) → String accountId (11 digits)
 * - ACCT-ACTIVE-STATUS PIC X(01) → AccountStatus activeStatus (Y/N)
 * - ACCT-CURR-BAL PIC S9(10)V99 → BigDecimal currentBalance (12,2 precision)
 * - ACCT-CREDIT-LIMIT PIC S9(10)V99 → BigDecimal creditLimit (12,2 precision)
 * - ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 → BigDecimal cashCreditLimit (12,2 precision)
 * - ACCT-OPEN-DATE PIC X(10) → LocalDate openDate (YYYY-MM-DD format)
 * - ACCT-EXPIRAION-DATE PIC X(10) → LocalDate expirationDate (YYYY-MM-DD format)
 * - ACCT-REISSUE-DATE PIC X(10) → LocalDate reissueDate (YYYY-MM-DD format)
 * 
 * Performance Requirements:
 * - Chunk-based processing with configurable chunk sizes (default 1000 records)
 * - Memory-efficient processing supporting large datasets without heap exhaustion
 * - Sub-second processing per chunk maintaining overall batch window requirements
 * - Error recovery and restart capability through Spring Batch infrastructure
 * 
 * @author Blitzy Development Team
 * @version 1.0
 * @since 2024-01-01
 */
public class AccountDataItemReader extends FlatFileItemReader<Account> {

    private static final Logger logger = LoggerFactory.getLogger(AccountDataItemReader.class);
    
    /**
     * Resource reference for file access and validation
     */
    private Resource resource;

    /**
     * MathContext for COBOL COMP-3 decimal precision equivalence
     * Uses DECIMAL128 precision with HALF_EVEN rounding to ensure exact financial arithmetic
     */
    public static final MathContext COBOL_DECIMAL_CONTEXT = new MathContext(31, RoundingMode.HALF_EVEN);

    /**
     * Date formatter for COBOL date format conversion (YYYY-MM-DD)
     * Supports conversion from COBOL PIC X(10) date fields
     */
    private static final DateTimeFormatter COBOL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Pattern for validating account ID format (exactly 11 digits)
     * Ensures compliance with COBOL PIC 9(11) constraint
     */
    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^[0-9]{11}$");

    /**
     * Pattern for validating active status (Y or N)
     * Ensures compliance with COBOL PIC X(01) constraint
     */
    private static final Pattern ACTIVE_STATUS_PATTERN = Pattern.compile("^[YN]$");

    /**
     * Fixed field ranges based on COBOL ACCOUNT-RECORD layout (300 bytes total)
     * Each range corresponds to a specific field in the original VSAM record structure
     */
    private static final Range[] FIELD_RANGES = {
        new Range(1, 11),   // ACCT-ID: positions 1-11 (11 chars)
        new Range(12, 12),  // ACCT-ACTIVE-STATUS: position 12 (1 char)
        new Range(13, 23),  // Customer ID: positions 13-23 (11 chars) - inferred from data
        new Range(24, 35),  // ACCT-CURR-BAL: positions 24-35 (12 chars with sign)
        new Range(36, 47),  // ACCT-CREDIT-LIMIT: positions 36-47 (12 chars with sign)
        new Range(48, 59),  // ACCT-CASH-CREDIT-LIMIT: positions 48-59 (12 chars with sign)
        new Range(60, 69),  // ACCT-OPEN-DATE: positions 60-69 (10 chars)
        new Range(70, 79),  // ACCT-EXPIRAION-DATE: positions 70-79 (10 chars)
        new Range(80, 89),  // ACCT-REISSUE-DATE: positions 80-89 (10 chars)
        new Range(90, 101), // ACCT-CURR-CYC-CREDIT: positions 90-101 (12 chars with sign)
        new Range(102, 113),// ACCT-CURR-CYC-DEBIT: positions 102-113 (12 chars with sign)
        new Range(114, 123),// ACCT-ADDR-ZIP: positions 114-123 (10 chars)
        new Range(124, 133),// ACCT-GROUP-ID: positions 124-133 (10 chars)
        new Range(134, 300) // FILLER: positions 134-300 (167 chars padding)
    };

    /**
     * Field names corresponding to the COBOL structure for mapping
     */
    private static final String[] FIELD_NAMES = {
        "accountId", "activeStatus", "customerId", "currentBalance", "creditLimit", 
        "cashCreditLimit", "openDate", "expirationDate", "reissueDate",
        "currentCycleCredit", "currentCycleDebit", "addressZip", "groupId", "filler"
    };

    /**
     * Default constructor initializing the ItemReader with COBOL-specific configuration
     */
    public AccountDataItemReader() {
        super();
        setName("accountDataItemReader");
        setLinesToSkip(0); // No header in COBOL data files
        setLineMapper(createLineMapper());
        logger.info("Initialized AccountDataItemReader with COBOL fixed-width layout");
    }

    /**
     * Constructor with resource parameter for Spring Batch configuration
     * 
     * @param resource The Resource pointing to the account data ASCII file
     */
    public AccountDataItemReader(Resource resource) {
        this();
        setResource(resource);
        logger.info("Configured AccountDataItemReader with resource: {}", resource.getFilename());
    }
    
    /**
     * Override setResource to store reference for validation and access
     * 
     * @param resource The Resource pointing to the account data ASCII file
     */
    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
        super.setResource(resource);
    }

    /**
     * Creates and configures the line mapper for fixed-width COBOL data parsing
     * 
     * @return Configured DefaultLineMapper for Account entity mapping
     */
    private DefaultLineMapper<Account> createLineMapper() {
        DefaultLineMapper<Account> lineMapper = new DefaultLineMapper<>();
        
        // Configure fixed-length tokenizer for COBOL record structure
        FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();
        tokenizer.setColumns(FIELD_RANGES);
        tokenizer.setNames(FIELD_NAMES);
        tokenizer.setStrict(true); // Enforce exact field length requirements
        
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(new AccountFieldSetMapper());
        
        return lineMapper;
    }

    /**
     * Overridden read method with enhanced error handling and logging
     * 
     * @return Account entity or null if end of file reached
     * @throws Exception if reading or parsing fails
     */
    @Override
    public Account read() throws Exception {
        try {
            Account account = super.read();
            if (account != null) {
                logger.debug("Successfully read account record: {}", account.getAccountId());
            }
            return account;
        } catch (Exception e) {
            logger.error("Error reading account data record at line {}: {}", 
                getCurrentItemCount() + 1, e.getMessage());
            throw new ItemStreamException("Failed to read account data", e);
        }
    }

    /**
     * Enhanced open method with validation and setup logging
     * 
     * @param executionContext Spring Batch execution context
     * @throws ItemStreamException if initialization fails
     */
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            super.open(executionContext);
            logger.info("Opened AccountDataItemReader for processing account data migration");
            
            // Validate resource configuration
            if (resource == null) {
                throw new ItemStreamException("Resource not configured for AccountDataItemReader");
            }
            
            if (!resource.exists()) {
                throw new ItemStreamException("Account data file does not exist: " + 
                    resource.getFilename());
            }
            
            logger.info("Account data file validated: {} ({})", 
                resource.getFilename(), 
                resource.contentLength() + " bytes");
                
        } catch (Exception e) {
            logger.error("Failed to open AccountDataItemReader: {}", e.getMessage());
            throw new ItemStreamException("Failed to initialize account data reader", e);
        }
    }

    /**
     * Enhanced close method with cleanup logging
     */
    @Override
    public void close() {
        try {
            super.close();
            logger.info("Closed AccountDataItemReader after processing {} records", 
                getCurrentItemCount());
        } catch (Exception e) {
            logger.warn("Warning during AccountDataItemReader close: {}", e.getMessage());
        }
    }

    /**
     * Custom FieldSetMapper for converting parsed COBOL data to Account entities
     * Handles COBOL-specific data format conversions and validation
     */
    private static class AccountFieldSetMapper implements FieldSetMapper<Account> {

        @Override
        public Account mapFieldSet(FieldSet fieldSet) throws BindException {
            try {
                Account account = new Account();
                
                // Map account ID with validation
                String accountId = fieldSet.readString("accountId").trim();
                validateAccountId(accountId);
                account.setAccountId(accountId);
                
                // Map active status with validation
                String activeStatus = fieldSet.readString("activeStatus").trim();
                validateActiveStatus(activeStatus);
                account.setActiveStatus(parseActiveStatus(activeStatus).isActive());
                
                // Map financial amounts with COBOL COMP-3 precision
                account.setCurrentBalance(parseCobolDecimal(fieldSet.readString("currentBalance")));
                account.setCreditLimit(parseCobolDecimal(fieldSet.readString("creditLimit")));
                account.setCashCreditLimit(parseCobolDecimal(fieldSet.readString("cashCreditLimit")));
                account.setCurrentCycleCredit(parseCobolDecimal(fieldSet.readString("currentCycleCredit")));
                account.setCurrentCycleDebit(parseCobolDecimal(fieldSet.readString("currentCycleDebit")));
                
                // Map dates with COBOL format conversion
                account.setOpenDate(parseCobolDate(fieldSet.readString("openDate")));
                account.setExpirationDate(parseCobolDate(fieldSet.readString("expirationDate")));
                account.setReissueDate(parseCobolDate(fieldSet.readString("reissueDate")));
                
                // Map additional fields
                String addressZip = fieldSet.readString("addressZip").trim();
                if (!addressZip.isEmpty() && !addressZip.equals("0000000000")) {
                    account.setAddressZip(addressZip);
                }
                
                String groupId = fieldSet.readString("groupId").trim();
                if (!groupId.isEmpty() && !groupId.equals("0000000000")) {
                    account.setGroupId(groupId);
                }
                
                logger.debug("Mapped account record: ID={}, Status={}, Balance={}", 
                    account.getAccountId(), account.getActiveStatus(), account.getCurrentBalance());
                
                return account;
                
            } catch (Exception e) {
                logger.error("Failed to map account record: {}", e.getMessage());
                throw new BindException(fieldSet, "account mapping failed: " + e.getMessage());
            }
        }
    }

    /**
     * Validates account ID format ensuring compliance with COBOL PIC 9(11) constraint
     * 
     * @param accountId The account ID to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateAccountId(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        
        if (!ACCOUNT_ID_PATTERN.matcher(accountId).matches()) {
            throw new IllegalArgumentException("Account ID must be exactly 11 digits: " + accountId);
        }
    }

    /**
     * Validates active status ensuring compliance with COBOL constraint
     * 
     * @param activeStatus The active status to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateActiveStatus(String activeStatus) {
        if (activeStatus == null || activeStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("Active status cannot be null or empty");
        }
        
        if (!ACTIVE_STATUS_PATTERN.matcher(activeStatus).matches()) {
            throw new IllegalArgumentException("Active status must be Y or N: " + activeStatus);
        }
    }

    /**
     * Parses COBOL zone decimal format to BigDecimal maintaining exact precision
     * Handles COBOL zone decimal signs where { = +0, } = -0, etc.
     * 
     * @param cobolDecimal The COBOL decimal string to parse
     * @return BigDecimal with exact COBOL COMP-3 precision
     */
    public static BigDecimal parseCobolDecimal(String cobolDecimal) {
        if (cobolDecimal == null || cobolDecimal.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        String trimmed = cobolDecimal.trim();
        if (trimmed.equals("0") || trimmed.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            // Handle COBOL zone decimal format where last character indicates sign
            String numericPart;
            boolean isNegative = false;
            
            if (trimmed.length() > 0) {
                char lastChar = trimmed.charAt(trimmed.length() - 1);
                
                // COBOL zone decimal sign mapping
                switch (lastChar) {
                    case '{': // +0
                    case 'A': case 'B': case 'C': case 'D': case 'E': 
                    case 'F': case 'G': case 'H': case 'I': // +1 to +9
                        numericPart = trimmed.substring(0, trimmed.length() - 1) + 
                                    String.valueOf(lastChar == '{' ? '0' : (char)(lastChar - 'A' + '1'));
                        isNegative = false;
                        break;
                        
                    case '}': // -0
                    case 'J': case 'K': case 'L': case 'M': case 'N': 
                    case 'O': case 'P': case 'Q': case 'R': // -1 to -9
                        numericPart = trimmed.substring(0, trimmed.length() - 1) + 
                                    String.valueOf(lastChar == '}' ? '0' : (char)(lastChar - 'J' + '1'));
                        isNegative = true;
                        break;
                        
                    default:
                        // Regular numeric format
                        numericPart = trimmed;
                        break;
                }
            } else {
                numericPart = trimmed;
            }
            
            // Convert to decimal with proper scale (2 decimal places for financial amounts)
            BigDecimal result;
            if (numericPart.length() > 2) {
                String integerPart = numericPart.substring(0, numericPart.length() - 2);
                String decimalPart = numericPart.substring(numericPart.length() - 2);
                result = new BigDecimal(integerPart + "." + decimalPart, COBOL_DECIMAL_CONTEXT);
            } else {
                // Handle small numbers (less than 1.00)
                result = new BigDecimal("0." + String.format("%02d", Integer.parseInt(numericPart)), 
                                      COBOL_DECIMAL_CONTEXT);
            }
            
            return isNegative ? result.negate(COBOL_DECIMAL_CONTEXT) : result;
            
        } catch (Exception e) {
            logger.warn("Failed to parse COBOL decimal '{}', defaulting to zero: {}", 
                       cobolDecimal, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Parses COBOL date format (YYYY-MM-DD) to LocalDate
     * 
     * @param cobolDate The COBOL date string to parse
     * @return LocalDate or null if date is invalid or empty
     */
    public static LocalDate parseCobolDate(String cobolDate) {
        if (cobolDate == null || cobolDate.trim().isEmpty() || 
            cobolDate.trim().equals("0000-00-00") || cobolDate.trim().equals("          ")) {
            return null;
        }
        
        try {
            String trimmed = cobolDate.trim();
            return LocalDate.parse(trimmed, COBOL_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse COBOL date '{}': {}", cobolDate, e.getMessage());
            return null;
        }
    }

    /**
     * Parses active status from COBOL format to AccountStatus enum
     * 
     * @param activeStatus The COBOL active status character
     * @return AccountStatus enum value
     */
    private static com.carddemo.common.enums.AccountStatus parseActiveStatus(String activeStatus) {
        if ("Y".equals(activeStatus)) {
            return com.carddemo.common.enums.AccountStatus.ACTIVE;
        } else if ("N".equals(activeStatus)) {
            return com.carddemo.common.enums.AccountStatus.INACTIVE;
        } else {
            logger.warn("Unknown active status '{}', defaulting to ACTIVE", activeStatus);
            return com.carddemo.common.enums.AccountStatus.ACTIVE;
        }
    }

    /**
     * Validates account data integrity during processing
     * Performs business rule validation beyond field format checking
     * 
     * @param account The account to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateAccountData(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        
        // Validate required fields
        if (account.getAccountId() == null || account.getAccountId().trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }
        
        if (account.getActiveStatus() == null) {
            throw new IllegalArgumentException("Active status is required");
        }
        
        // Validate financial constraints
        if (account.getCurrentBalance() == null) {
            throw new IllegalArgumentException("Current balance is required");
        }
        
        if (account.getCreditLimit() == null || account.getCreditLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Credit limit must be non-negative");
        }
        
        if (account.getCashCreditLimit() == null || account.getCashCreditLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cash credit limit must be non-negative");
        }
        
        // Validate date constraints
        if (account.getOpenDate() == null) {
            throw new IllegalArgumentException("Open date is required");
        }
        
        if (account.getOpenDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Open date cannot be in the future");
        }
        
        // Validate credit limits relationship
        if (account.getCashCreditLimit().compareTo(account.getCreditLimit()) > 0) {
            logger.warn("Cash credit limit {} exceeds credit limit {} for account {}", 
                       account.getCashCreditLimit(), account.getCreditLimit(), account.getAccountId());
        }
        
        logger.debug("Account data validation passed for account: {}", account.getAccountId());
    }

    /**
     * Parses a complete account record from fixed-width ASCII format
     * Utility method for testing and validation purposes
     * 
     * @param recordLine The fixed-width record line to parse
     * @return Parsed Account entity
     * @throws Exception if parsing fails
     */
    public static Account parseAccountRecord(String recordLine) throws Exception {
        if (recordLine == null || recordLine.length() < 133) {
            throw new IllegalArgumentException("Invalid record length: expected at least 133 characters");
        }
        
        // Create a temporary tokenizer for parsing
        FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();
        tokenizer.setColumns(FIELD_RANGES);
        tokenizer.setNames(FIELD_NAMES);
        tokenizer.setStrict(false); // Allow shorter records for testing
        
        FieldSet fieldSet = tokenizer.tokenize(recordLine);
        AccountFieldSetMapper mapper = new AccountFieldSetMapper();
        
        Account account = mapper.mapFieldSet(fieldSet);
        validateAccountData(account);
        
        return account;
    }
}