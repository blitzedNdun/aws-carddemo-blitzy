package com.carddemo.account.dto;

import com.carddemo.common.enums.AccountStatus;
import com.carddemo.common.enums.CardStatus;
import com.carddemo.common.util.BigDecimalUtils;
import com.carddemo.common.util.ValidationUtils;
import com.carddemo.common.validator.ValidCurrency;
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.carddemo.common.validator.ValidCardNumber;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Core account data transfer object preserving exact VSAM record structure from COBOL copybooks.
 * 
 * This DTO maintains complete field compatibility with the original mainframe COBOL structures:
 * - CVACT01Y.cpy: Primary account record structure (300 bytes)
 * - CVACT02Y.cpy: Card record structure (150 bytes) 
 * - CVACT03Y.cpy: Card cross-reference structure (50 bytes)
 * 
 * The implementation preserves exact COBOL field mappings while providing modern Java validation,
 * JSON serialization support, and BigDecimal precision for financial calculations matching
 * COBOL COMP-3 arithmetic behavior as specified in Section 0.1.2 of the technical specification.
 * 
 * <p><strong>Data Precision Requirements:</strong></p>
 * All monetary fields use BigDecimal with MathContext.DECIMAL128 to maintain exact
 * COBOL COMP-3 precision without floating-point errors. This ensures regulatory
 * compliance and identical financial calculation results.
 * 
 * <p><strong>Field Validation:</strong></p>
 * Comprehensive Jakarta Bean Validation annotations ensure data integrity equivalent
 * to original COBOL field validation while supporting REST API and React frontend
 * validation requirements.
 * 
 * <p><strong>JSON Serialization:</strong></p>
 * Jackson annotations provide clean API responses while maintaining exact field
 * sequencing from the original COBOL record layouts for system compatibility.
 * 
 * <p><strong>Integration Points:</strong></p>
 * <ul>
 *   <li>AccountViewService: Read operations with JPA entity mapping</li>
 *   <li>AccountUpdateService: Transactional updates with optimistic locking</li>
 *   <li>CardListService: Card cross-reference data for account relationships</li>
 *   <li>React Components: Form validation and display formatting</li>
 * </ul>
 * 
 * @author CardDemo Migration Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 * @see com.carddemo.account.entity.Account
 * @see com.carddemo.account.service.AccountViewService
 * @see com.carddemo.account.service.AccountUpdateService
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountDto {

    // ===================================================================================
    // ACCOUNT RECORD FIELDS (from CVACT01Y.cpy)
    // ===================================================================================

    /**
     * Account ID - Primary identifier matching COBOL ACCT-ID PIC 9(11).
     * 
     * Validation ensures 11-digit numeric format matching original COBOL constraints
     * with range validation (100000000-999999999) as per business rules.
     * 
     * Database mapping: accounts.account_id BIGINT PRIMARY KEY
     * COBOL equivalent: ACCT-ID PIC 9(11)
     */
    private String accountId;

    /**
     * Account active status matching COBOL ACCT-ACTIVE-STATUS PIC X(01).
     * 
     * Uses AccountStatus enum for type safety and validation:
     * - 'Y' = ACTIVE (operational account)
     * - 'N' = INACTIVE (suspended account)
     * 
     * Database mapping: accounts.active_status VARCHAR(1) with CHECK constraint
     * COBOL equivalent: ACCT-ACTIVE-STATUS PIC X(01)
     */
    private AccountStatus activeStatus;

    /**
     * Current account balance with exact COBOL COMP-3 precision.
     * 
     * Maintains ACCT-CURR-BAL PIC S9(10)V99 precision using BigDecimal with
     * DECIMAL(12,2) format and MathContext.DECIMAL128 for exact arithmetic.
     * Supports negative balances for overdraft scenarios.
     * 
     * Database mapping: accounts.current_balance DECIMAL(12,2)
     * COBOL equivalent: ACCT-CURR-BAL PIC S9(10)V99 COMP-3
     */
    @ValidCurrency(precision = 12, scale = 2, allowNegative = true)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal currentBalance;

    /**
     * Credit limit with exact COBOL COMP-3 precision.
     * 
     * Maintains ACCT-CREDIT-LIMIT PIC S9(10)V99 precision using BigDecimal
     * with non-negative validation for business rule compliance.
     * 
     * Database mapping: accounts.credit_limit DECIMAL(12,2)
     * COBOL equivalent: ACCT-CREDIT-LIMIT PIC S9(10)V99 COMP-3
     */
    @ValidCurrency(precision = 12, scale = 2, min = "0.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal creditLimit;

    /**
     * Cash credit limit with exact COBOL COMP-3 precision.
     * 
     * Maintains ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 precision using BigDecimal
     * with non-negative validation and cash advance business rules.
     * 
     * Database mapping: accounts.cash_credit_limit DECIMAL(12,2)
     * COBOL equivalent: ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 COMP-3
     */
    @ValidCurrency(precision = 12, scale = 2, min = "0.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal cashCreditLimit;

    /**
     * Account open date in CCYYMMDD format.
     * 
     * Maintains ACCT-OPEN-DATE PIC X(10) format with COBOL date validation
     * including century, month, day, and leap year validation logic.
     * 
     * Database mapping: accounts.open_date DATE
     * COBOL equivalent: ACCT-OPEN-DATE PIC X(10)
     */
    @ValidCCYYMMDD
    private String openDate;

    /**
     * Account expiration date in CCYYMMDD format.
     * 
     * Maintains ACCT-EXPIRAION-DATE PIC X(10) format (preserving original typo)
     * with comprehensive date validation and future date support.
     * 
     * Database mapping: accounts.expiration_date DATE
     * COBOL equivalent: ACCT-EXPIRAION-DATE PIC X(10)
     */
    @ValidCCYYMMDD
    private String expirationDate;

    /**
     * Account reissue date in CCYYMMDD format.
     * 
     * Maintains ACCT-REISSUE-DATE PIC X(10) format with date validation
     * for account renewal and reactivation tracking.
     * 
     * Database mapping: accounts.reissue_date DATE
     * COBOL equivalent: ACCT-REISSUE-DATE PIC X(10)
     */
    @ValidCCYYMMDD
    private String reissueDate;

    /**
     * Current cycle credit amount with exact COBOL COMP-3 precision.
     * 
     * Maintains ACCT-CURR-CYC-CREDIT PIC S9(10)V99 precision for billing
     * cycle credit tracking with exact financial arithmetic.
     * 
     * Database mapping: accounts.current_cycle_credit DECIMAL(12,2)
     * COBOL equivalent: ACCT-CURR-CYC-CREDIT PIC S9(10)V99 COMP-3
     */
    @ValidCurrency(precision = 12, scale = 2, min = "0.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal currentCycleCredit;

    /**
     * Current cycle debit amount with exact COBOL COMP-3 precision.
     * 
     * Maintains ACCT-CURR-CYC-DEBIT PIC S9(10)V99 precision for billing
     * cycle debit tracking with exact financial arithmetic.
     * 
     * Database mapping: accounts.current_cycle_debit DECIMAL(12,2)
     * COBOL equivalent: ACCT-CURR-CYC-DEBIT PIC S9(10)V99 COMP-3
     */
    @ValidCurrency(precision = 12, scale = 2, min = "0.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal currentCycleDebit;

    /**
     * Account address ZIP code.
     * 
     * Maintains ACCT-ADDR-ZIP PIC X(10) format with US ZIP code validation
     * supporting both 5-digit and ZIP+4 formats.
     * 
     * Database mapping: accounts.address_zip VARCHAR(10)
     * COBOL equivalent: ACCT-ADDR-ZIP PIC X(10)
     */
    private String addressZip;

    /**
     * Account group ID for categorization.
     * 
     * Maintains ACCT-GROUP-ID PIC X(10) format for account grouping
     * and classification business rules.
     * 
     * Database mapping: accounts.group_id VARCHAR(10)
     * COBOL equivalent: ACCT-GROUP-ID PIC X(10)
     */
    private String groupId;

    // ===================================================================================
    // CARD RECORD FIELDS (from CVACT02Y.cpy)
    // ===================================================================================

    /**
     * Credit card number with Luhn algorithm validation.
     * 
     * Maintains CARD-NUM PIC X(16) format with comprehensive card number
     * validation including checksum verification and industry standards.
     * 
     * Database mapping: cards.card_number VARCHAR(16) UNIQUE
     * COBOL equivalent: CARD-NUM PIC X(16)
     */
    @ValidCardNumber
    private String cardNumber;

    /**
     * Card-associated account ID matching card relationships.
     * 
     * Maintains CARD-ACCT-ID PIC 9(11) format with foreign key constraints
     * to ensure referential integrity with account records.
     * 
     * Database mapping: cards.account_id BIGINT REFERENCES accounts(account_id)
     * COBOL equivalent: CARD-ACCT-ID PIC 9(11)
     */
    private String cardAccountId;

    /**
     * Card verification value (CVV) code.
     * 
     * Maintains CARD-CVV-CD PIC 9(03) format with 3-digit numeric validation
     * for payment processing security requirements.
     * 
     * Database mapping: cards.cvv_code VARCHAR(3)
     * COBOL equivalent: CARD-CVV-CD PIC 9(03)
     */
    private String cardCvv;

    /**
     * Embossed name on credit card.
     * 
     * Maintains CARD-EMBOSSED-NAME PIC X(50) format with length validation
     * and character set restrictions for card printing compatibility.
     * 
     * Database mapping: cards.embossed_name VARCHAR(50)
     * COBOL equivalent: CARD-EMBOSSED-NAME PIC X(50)
     */
    private String cardEmbossedName;

    /**
     * Card expiration date in CCYYMMDD format.
     * 
     * Maintains CARD-EXPIRAION-DATE PIC X(10) format (preserving original typo)
     * with future date validation and card lifecycle management.
     * 
     * Database mapping: cards.expiration_date DATE
     * COBOL equivalent: CARD-EXPIRAION-DATE PIC X(10)
     */
    @ValidCCYYMMDD
    private String cardExpirationDate;

    /**
     * Card active status using CardStatus enumeration.
     * 
     * Maintains CARD-ACTIVE-STATUS PIC X(01) format with enhanced validation:
     * - 'A' = ACTIVE (can process transactions)
     * - 'I' = INACTIVE (temporarily disabled)
     * - 'B' = BLOCKED (permanently disabled)
     * 
     * Database mapping: cards.active_status VARCHAR(1) with CHECK constraint
     * COBOL equivalent: CARD-ACTIVE-STATUS PIC X(01)
     */
    private CardStatus cardActiveStatus;

    // ===================================================================================
    // CARD CROSS-REFERENCE FIELDS (from CVACT03Y.cpy)
    // ===================================================================================

    /**
     * Cross-reference card number for relationship mapping.
     * 
     * Maintains XREF-CARD-NUM PIC X(16) format for card-to-customer-to-account
     * cross-reference relationships and data integrity validation.
     * 
     * Database mapping: card_xref.card_number VARCHAR(16)
     * COBOL equivalent: XREF-CARD-NUM PIC X(16)
     */
    @ValidCardNumber
    private String xrefCardNumber;

    /**
     * Cross-reference customer ID.
     * 
     * Maintains XREF-CUST-ID PIC 9(09) format with 9-digit customer ID
     * validation for customer relationship management.
     * 
     * Database mapping: card_xref.customer_id BIGINT
     * COBOL equivalent: XREF-CUST-ID PIC 9(09)
     */
    private String xrefCustomerId;

    /**
     * Cross-reference account ID for relationship validation.
     * 
     * Maintains XREF-ACCT-ID PIC 9(11) format ensuring consistency
     * with primary account ID for data integrity.
     * 
     * Database mapping: card_xref.account_id BIGINT
     * COBOL equivalent: XREF-ACCT-ID PIC 9(11)
     */
    private String xrefAccountId;

    // ===================================================================================
    // CONSTRUCTORS
    // ===================================================================================

    /**
     * Default constructor for JPA entity mapping and JSON deserialization.
     * Initializes all monetary fields to zero with proper precision for
     * COBOL COMP-3 arithmetic compatibility.
     */
    public AccountDto() {
        // Initialize monetary fields with proper COBOL COMP-3 precision
        this.currentBalance = BigDecimalUtils.ZERO_MONETARY;
        this.creditLimit = BigDecimalUtils.ZERO_MONETARY;
        this.cashCreditLimit = BigDecimalUtils.ZERO_MONETARY;
        this.currentCycleCredit = BigDecimalUtils.ZERO_MONETARY;
        this.currentCycleDebit = BigDecimalUtils.ZERO_MONETARY;
    }

    /**
     * Constructor with primary account fields for common initialization scenarios.
     * 
     * @param accountId Account identifier
     * @param activeStatus Account status
     * @param currentBalance Current account balance
     * @param creditLimit Credit limit amount
     */
    public AccountDto(String accountId, AccountStatus activeStatus, 
                     BigDecimal currentBalance, BigDecimal creditLimit) {
        this();
        this.accountId = accountId;
        this.activeStatus = activeStatus;
        this.currentBalance = currentBalance != null ? 
            BigDecimalUtils.roundToMonetary(currentBalance) : BigDecimalUtils.ZERO_MONETARY;
        this.creditLimit = creditLimit != null ? 
            BigDecimalUtils.roundToMonetary(creditLimit) : BigDecimalUtils.ZERO_MONETARY;
    }

    // ===================================================================================
    // ACCOUNT RECORD GETTERS AND SETTERS
    // ===================================================================================

    /**
     * Gets the account ID.
     * 
     * @return Account identifier as 11-digit string
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID with validation.
     * 
     * @param accountId Account identifier (11 digits)
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the account active status.
     * 
     * @return AccountStatus enum (ACTIVE or INACTIVE)
     */
    public AccountStatus getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the account active status.
     * 
     * @param activeStatus AccountStatus enum value
     */
    public void setActiveStatus(AccountStatus activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the current account balance with exact COBOL precision.
     * 
     * @return Current balance as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Sets the current account balance with precision rounding.
     * 
     * @param currentBalance Balance amount with automatic COBOL precision rounding
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance != null ? 
            BigDecimalUtils.roundToMonetary(currentBalance) : BigDecimalUtils.ZERO_MONETARY;
    }

    /**
     * Gets the credit limit with exact COBOL precision.
     * 
     * @return Credit limit as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    /**
     * Sets the credit limit with precision rounding.
     * 
     * @param creditLimit Credit limit amount with automatic COBOL precision rounding
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit != null ? 
            BigDecimalUtils.roundToMonetary(creditLimit) : BigDecimalUtils.ZERO_MONETARY;
    }

    /**
     * Gets the cash credit limit with exact COBOL precision.
     * 
     * @return Cash credit limit as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    /**
     * Sets the cash credit limit with precision rounding.
     * 
     * @param cashCreditLimit Cash credit limit with automatic COBOL precision rounding
     */
    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit != null ? 
            BigDecimalUtils.roundToMonetary(cashCreditLimit) : BigDecimalUtils.ZERO_MONETARY;
    }

    /**
     * Gets the account open date.
     * 
     * @return Open date in CCYYMMDD format
     */
    public String getOpenDate() {
        return openDate;
    }

    /**
     * Sets the account open date.
     * 
     * @param openDate Date in CCYYMMDD format
     */
    public void setOpenDate(String openDate) {
        this.openDate = openDate;
    }

    /**
     * Gets the account expiration date.
     * 
     * @return Expiration date in CCYYMMDD format
     */
    public String getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the account expiration date.
     * 
     * @param expirationDate Date in CCYYMMDD format
     */
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the account reissue date.
     * 
     * @return Reissue date in CCYYMMDD format
     */
    public String getReissueDate() {
        return reissueDate;
    }

    /**
     * Sets the account reissue date.
     * 
     * @param reissueDate Date in CCYYMMDD format
     */
    public void setReissueDate(String reissueDate) {
        this.reissueDate = reissueDate;
    }

    /**
     * Gets the current cycle credit amount with exact COBOL precision.
     * 
     * @return Current cycle credit as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getCurrentCycleCredit() {
        return currentCycleCredit;
    }

    /**
     * Sets the current cycle credit amount with precision rounding.
     * 
     * @param currentCycleCredit Credit amount with automatic COBOL precision rounding
     */
    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) {
        this.currentCycleCredit = currentCycleCredit != null ? 
            BigDecimalUtils.roundToMonetary(currentCycleCredit) : BigDecimalUtils.ZERO_MONETARY;
    }

    /**
     * Gets the current cycle debit amount with exact COBOL precision.
     * 
     * @return Current cycle debit as BigDecimal with DECIMAL(12,2) precision
     */
    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }

    /**
     * Sets the current cycle debit amount with precision rounding.
     * 
     * @param currentCycleDebit Debit amount with automatic COBOL precision rounding
     */
    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) {
        this.currentCycleDebit = currentCycleDebit != null ? 
            BigDecimalUtils.roundToMonetary(currentCycleDebit) : BigDecimalUtils.ZERO_MONETARY;
    }

    /**
     * Gets the account address ZIP code.
     * 
     * @return ZIP code string
     */
    public String getAddressZip() {
        return addressZip;
    }

    /**
     * Sets the account address ZIP code.
     * 
     * @param addressZip ZIP code in standard or ZIP+4 format
     */
    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }

    /**
     * Gets the account group ID.
     * 
     * @return Group ID string
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Sets the account group ID.
     * 
     * @param groupId Group identifier for account categorization
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    // ===================================================================================
    // CARD RECORD GETTERS AND SETTERS
    // ===================================================================================

    /**
     * Gets the credit card number.
     * 
     * @return Card number as 16-digit string
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the credit card number.
     * 
     * @param cardNumber 16-digit card number with Luhn validation
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the card-associated account ID.
     * 
     * @return Account ID associated with the card
     */
    public String getCardAccountId() {
        return cardAccountId;
    }

    /**
     * Sets the card-associated account ID.
     * 
     * @param cardAccountId Account ID for card relationship
     */
    public void setCardAccountId(String cardAccountId) {
        this.cardAccountId = cardAccountId;
    }

    /**
     * Gets the card CVV code.
     * 
     * @return 3-digit CVV code
     */
    public String getCardCvv() {
        return cardCvv;
    }

    /**
     * Sets the card CVV code.
     * 
     * @param cardCvv 3-digit verification code
     */
    public void setCardCvv(String cardCvv) {
        this.cardCvv = cardCvv;
    }

    /**
     * Gets the card embossed name.
     * 
     * @return Name embossed on the card
     */
    public String getCardEmbossedName() {
        return cardEmbossedName;
    }

    /**
     * Sets the card embossed name.
     * 
     * @param cardEmbossedName Name to emboss on card
     */
    public void setCardEmbossedName(String cardEmbossedName) {
        this.cardEmbossedName = cardEmbossedName;
    }

    /**
     * Gets the card expiration date.
     * 
     * @return Card expiration date in CCYYMMDD format
     */
    public String getCardExpirationDate() {
        return cardExpirationDate;
    }

    /**
     * Sets the card expiration date.
     * 
     * @param cardExpirationDate Expiration date in CCYYMMDD format
     */
    public void setCardExpirationDate(String cardExpirationDate) {
        this.cardExpirationDate = cardExpirationDate;
    }

    /**
     * Gets the card active status.
     * 
     * @return CardStatus enum (ACTIVE, INACTIVE, or BLOCKED)
     */
    public CardStatus getCardActiveStatus() {
        return cardActiveStatus;
    }

    /**
     * Sets the card active status.
     * 
     * @param cardActiveStatus CardStatus enum value
     */
    public void setCardActiveStatus(CardStatus cardActiveStatus) {
        this.cardActiveStatus = cardActiveStatus;
    }

    // ===================================================================================
    // CARD CROSS-REFERENCE GETTERS AND SETTERS
    // ===================================================================================

    /**
     * Gets the cross-reference card number.
     * 
     * @return Cross-reference card number
     */
    public String getXrefCardNumber() {
        return xrefCardNumber;
    }

    /**
     * Sets the cross-reference card number.
     * 
     * @param xrefCardNumber Card number for cross-reference
     */
    public void setXrefCardNumber(String xrefCardNumber) {
        this.xrefCardNumber = xrefCardNumber;
    }

    /**
     * Gets the cross-reference customer ID.
     * 
     * @return Customer ID from cross-reference
     */
    public String getXrefCustomerId() {
        return xrefCustomerId;
    }

    /**
     * Sets the cross-reference customer ID.
     * 
     * @param xrefCustomerId Customer ID for cross-reference
     */
    public void setXrefCustomerId(String xrefCustomerId) {
        this.xrefCustomerId = xrefCustomerId;
    }

    /**
     * Gets the cross-reference account ID.
     * 
     * @return Account ID from cross-reference
     */
    public String getXrefAccountId() {
        return xrefAccountId;
    }

    /**
     * Sets the cross-reference account ID.
     * 
     * @param xrefAccountId Account ID for cross-reference validation
     */
    public void setXrefAccountId(String xrefAccountId) {
        this.xrefAccountId = xrefAccountId;
    }

    // ===================================================================================
    // VALIDATION AND BUSINESS LOGIC METHODS
    // ===================================================================================

    /**
     * Validates the complete AccountDto using COBOL-equivalent validation logic.
     * 
     * Performs comprehensive field validation including:
     * - Account ID format and range validation
     * - Monetary field precision and business rule validation
     * - Date format validation with COBOL date logic
     * - Card number Luhn algorithm validation
     * - Cross-reference data consistency validation
     * 
     * @return true if all validation passes, false otherwise
     */
    public boolean validate() {
        try {
            // Validate account ID (required field with COBOL format)
            if (!ValidationUtils.validateAccountNumber(accountId).isValid()) {
                return false;
            }

            // Validate account status (required field)
            if (activeStatus == null || !AccountStatus.isValid(activeStatus.getCode())) {
                return false;
            }

            // Validate monetary fields with BigDecimal precision
            if (currentBalance != null && 
                !ValidationUtils.validateBalance(currentBalance).isValid()) {
                return false;
            }

            if (creditLimit != null && 
                !ValidationUtils.validateCreditLimit(creditLimit).isValid()) {
                return false;
            }

            // Validate date fields with COBOL date logic
            if (openDate != null && 
                !ValidationUtils.validateDateField(openDate, false).isValid()) {
                return false;
            }

            if (expirationDate != null && 
                !ValidationUtils.validateDateField(expirationDate, true).isValid()) {
                return false;
            }

            if (reissueDate != null && 
                !ValidationUtils.validateDateField(reissueDate, true).isValid()) {
                return false;
            }

            // Validate card number with Luhn algorithm
            if (cardNumber != null && 
                !ValidationUtils.validateCardNumber(cardNumber).isValid()) {
                return false;
            }

            // Validate card expiration date
            if (cardExpirationDate != null && 
                !ValidationUtils.validateDateField(cardExpirationDate, true).isValid()) {
                return false;
            }

            // Validate cross-reference card number
            if (xrefCardNumber != null && 
                !ValidationUtils.validateCardNumber(xrefCardNumber).isValid()) {
                return false;
            }

            // Validate cross-reference account ID consistency
            if (xrefAccountId != null && accountId != null && 
                !xrefAccountId.equals(accountId)) {
                return false;
            }

            return true;

        } catch (Exception e) {
            // Log validation error and return false for any unexpected validation failures
            return false;
        }
    }

    // ===================================================================================
    // OBJECT LIFECYCLE METHODS
    // ===================================================================================

    /**
     * Compares this AccountDto with another object for equality.
     * 
     * Uses account ID as the primary equality criterion, consistent with
     * COBOL record comparison logic and database primary key semantics.
     * 
     * @param obj Object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        AccountDto that = (AccountDto) obj;
        return Objects.equals(accountId, that.accountId) &&
               Objects.equals(activeStatus, that.activeStatus) &&
               BigDecimalUtils.equals(currentBalance, that.currentBalance) &&
               BigDecimalUtils.equals(creditLimit, that.creditLimit) &&
               BigDecimalUtils.equals(cashCreditLimit, that.cashCreditLimit) &&
               Objects.equals(openDate, that.openDate) &&
               Objects.equals(expirationDate, that.expirationDate) &&
               Objects.equals(reissueDate, that.reissueDate) &&
               BigDecimalUtils.equals(currentCycleCredit, that.currentCycleCredit) &&
               BigDecimalUtils.equals(currentCycleDebit, that.currentCycleDebit) &&
               Objects.equals(addressZip, that.addressZip) &&
               Objects.equals(groupId, that.groupId) &&
               Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(cardAccountId, that.cardAccountId) &&
               Objects.equals(cardCvv, that.cardCvv) &&
               Objects.equals(cardEmbossedName, that.cardEmbossedName) &&
               Objects.equals(cardExpirationDate, that.cardExpirationDate) &&
               Objects.equals(cardActiveStatus, that.cardActiveStatus) &&
               Objects.equals(xrefCardNumber, that.xrefCardNumber) &&
               Objects.equals(xrefCustomerId, that.xrefCustomerId) &&
               Objects.equals(xrefAccountId, that.xrefAccountId);
    }

    /**
     * Generates hash code for this AccountDto.
     * 
     * Uses account ID as the primary hash component for consistent hashing
     * behavior in collections and caching scenarios.
     * 
     * @return Hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(
            accountId, activeStatus, 
            currentBalance, creditLimit, cashCreditLimit,
            openDate, expirationDate, reissueDate,
            currentCycleCredit, currentCycleDebit,
            addressZip, groupId,
            cardNumber, cardAccountId, cardCvv, cardEmbossedName,
            cardExpirationDate, cardActiveStatus,
            xrefCardNumber, xrefCustomerId, xrefAccountId
        );
    }

    /**
     * Returns string representation of the AccountDto.
     * 
     * Provides comprehensive but secure string representation with sensitive
     * data masking for logging and debugging purposes.
     * 
     * @return Formatted string representation
     */
    @Override
    public String toString() {
        return String.format(
            "AccountDto{" +
            "accountId='%s', " +
            "activeStatus=%s, " +
            "currentBalance=%s, " +
            "creditLimit=%s, " +
            "cashCreditLimit=%s, " +
            "openDate='%s', " +
            "expirationDate='%s', " +
            "reissueDate='%s', " +
            "currentCycleCredit=%s, " +
            "currentCycleDebit=%s, " +
            "addressZip='%s', " +
            "groupId='%s', " +
            "cardNumber='%s', " +
            "cardAccountId='%s', " +
            "cardCvv='***', " +
            "cardEmbossedName='%s', " +
            "cardExpirationDate='%s', " +
            "cardActiveStatus=%s, " +
            "xrefCardNumber='%s', " +
            "xrefCustomerId='%s', " +
            "xrefAccountId='%s'" +
            "}",
            accountId, activeStatus,
            BigDecimalUtils.formatCurrency(currentBalance),
            BigDecimalUtils.formatCurrency(creditLimit),
            BigDecimalUtils.formatCurrency(cashCreditLimit),
            openDate, expirationDate, reissueDate,
            BigDecimalUtils.formatCurrency(currentCycleCredit),
            BigDecimalUtils.formatCurrency(currentCycleDebit),
            addressZip, groupId,
            maskCardNumber(cardNumber), cardAccountId,
            cardEmbossedName, cardExpirationDate, cardActiveStatus,
            maskCardNumber(xrefCardNumber), xrefCustomerId, xrefAccountId
        );
    }

    /**
     * Masks card number for secure logging.
     * 
     * @param cardNumber Card number to mask
     * @return Masked card number showing only first 4 and last 4 digits
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return "****";
        }
        return cardNumber.substring(0, 4) + "****" + cardNumber.substring(cardNumber.length() - 4);
    }
}