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
 * Account Data Transfer Object maintaining exact VSAM record structure preservation.
 * 
 * <p>This DTO represents the complete account record structure derived from COBOL copybooks
 * CVACT01Y.cpy (account data), CVACT02Y.cpy (card data), and CVACT03Y.cpy (cross-reference data).
 * It maintains BigDecimal precision for all monetary fields using MathContext.DECIMAL128 to ensure
 * exact COBOL COMP-3 equivalent calculations and provides comprehensive validation annotations
 * for field-level validation supporting both React frontend and Spring Boot REST API validation.</p>
 * 
 * <p>The structure preserves the exact field sequencing and relationships from the original
 * VSAM datasets while enabling modern JSON serialization and Jakarta Bean Validation integration.
 * All financial calculations maintain identical precision to the original COBOL implementation,
 * preventing floating-point errors in critical financial operations.</p>
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Exact VSAM record layout preservation from CVACT01Y, CVACT02Y, and CVACT03Y copybooks</li>
 *   <li>BigDecimal precision for all monetary fields with DECIMAL(12,2) mapping</li>
 *   <li>Comprehensive field validation using Jakarta Bean Validation annotations</li>
 *   <li>JSON serialization support with null value handling for optional fields</li>
 *   <li>Integration with CardDemo validation utilities and custom validators</li>
 * </ul>
 * 
 * <p>Original COBOL Record Mappings:</p>
 * <ul>
 *   <li>CVACT01Y.cpy: ACCOUNT-RECORD structure with balance and credit limit fields</li>
 *   <li>CVACT02Y.cpy: CARD-RECORD structure with card number, CVV, and expiration data</li>
 *   <li>CVACT03Y.cpy: CARD-XREF-RECORD structure with cross-reference relationships</li>
 * </ul>
 * 
 * <p>Database Schema Compatibility:</p>
 * All fields map to PostgreSQL DECIMAL(12,2), VARCHAR, and DATE types as defined in the
 * technical specification Section 6.2.6.6, maintaining exact precision requirements and
 * supporting JPA entity mapping through Spring Data repositories.
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 * @see com.carddemo.account.entity.Account
 * @see com.carddemo.card.entity.Card
 * @see com.carddemo.common.util.BigDecimalUtils
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountDto {

    // ===== ACCOUNT RECORD FIELDS (from CVACT01Y.cpy) =====
    
    /**
     * Account ID - 11-digit account identifier.
     * 
     * Original COBOL: ACCT-ID PIC 9(11)
     * Database: VARCHAR(11) PRIMARY KEY
     * Validation: Must be exactly 11 numeric digits within valid range
     */
    private String accountId;

    /**
     * Account active status - Y/N flag indicating account operational status.
     * 
     * Original COBOL: ACCT-ACTIVE-STATUS PIC X(01)
     * Database: VARCHAR(1) with check constraint ('Y', 'N')
     * Validation: Must be valid AccountStatus enum value
     */
    private AccountStatus activeStatus;

    /**
     * Current account balance with exact COBOL COMP-3 precision.
     * 
     * Original COBOL: ACCT-CURR-BAL PIC S9(10)V99
     * Database: DECIMAL(12,2)
     * Validation: Must maintain exact financial precision, can be negative
     */
    @ValidCurrency(min = "-9999999999.99", max = "9999999999.99", 
                   precision = 12, scale = 2, cobolCompatible = true,
                   message = "Current balance must be valid monetary amount with DECIMAL(12,2) precision")
    @Digits(integer = 10, fraction = 2, message = "Current balance must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal currentBalance;

    /**
     * Credit limit with exact COBOL COMP-3 precision.
     * 
     * Original COBOL: ACCT-CREDIT-LIMIT PIC S9(10)V99
     * Database: DECIMAL(12,2)
     * Validation: Must be positive monetary amount within business rules
     */
    @ValidCurrency(min = "0.00", max = "9999999999.99", 
                   precision = 12, scale = 2, cobolCompatible = true,
                   message = "Credit limit must be valid positive monetary amount with DECIMAL(12,2) precision")
    @Digits(integer = 10, fraction = 2, message = "Credit limit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal creditLimit;

    /**
     * Cash credit limit with exact COBOL COMP-3 precision.
     * 
     * Original COBOL: ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99
     * Database: DECIMAL(12,2)
     * Validation: Must be positive monetary amount, typically lower than credit limit
     */
    @ValidCurrency(min = "0.00", max = "9999999999.99", 
                   precision = 12, scale = 2, cobolCompatible = true,
                   message = "Cash credit limit must be valid positive monetary amount with DECIMAL(12,2) precision")
    @Digits(integer = 10, fraction = 2, message = "Cash credit limit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal cashCreditLimit;

    /**
     * Account opening date in CCYYMMDD format.
     * 
     * Original COBOL: ACCT-OPEN-DATE PIC X(10)
     * Database: DATE
     * Validation: Must be valid date with century restrictions (19xx, 20xx)
     */
    @ValidCCYYMMDD(fieldName = "Account Opening Date", strictCentury = true,
                   message = "Account opening date must be valid CCYYMMDD format")
    private String openDate;

    /**
     * Account expiration date in CCYYMMDD format.
     * 
     * Original COBOL: ACCT-EXPIRAION-DATE PIC X(10) [sic - preserving original typo]
     * Database: DATE
     * Validation: Must be valid date, typically future date from opening
     */
    @ValidCCYYMMDD(fieldName = "Account Expiration Date", strictCentury = true,
                   message = "Account expiration date must be valid CCYYMMDD format")
    private String expirationDate;

    /**
     * Account reissue date in CCYYMMDD format.
     * 
     * Original COBOL: ACCT-REISSUE-DATE PIC X(10)
     * Database: DATE
     * Validation: Must be valid date, used for card reissue tracking
     */
    @ValidCCYYMMDD(fieldName = "Account Reissue Date", strictCentury = true, allowNull = true,
                   message = "Account reissue date must be valid CCYYMMDD format")
    private String reissueDate;

    /**
     * Current cycle credit amount with exact COBOL COMP-3 precision.
     * 
     * Original COBOL: ACCT-CURR-CYC-CREDIT PIC S9(10)V99
     * Database: DECIMAL(12,2)
     * Validation: Must be positive monetary amount for credit tracking
     */
    @ValidCurrency(min = "0.00", max = "9999999999.99", 
                   precision = 12, scale = 2, cobolCompatible = true,
                   message = "Current cycle credit must be valid positive monetary amount with DECIMAL(12,2) precision")
    @Digits(integer = 10, fraction = 2, message = "Current cycle credit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal currentCycleCredit;

    /**
     * Current cycle debit amount with exact COBOL COMP-3 precision.
     * 
     * Original COBOL: ACCT-CURR-CYC-DEBIT PIC S9(10)V99
     * Database: DECIMAL(12,2)
     * Validation: Must be positive monetary amount for debit tracking
     */
    @ValidCurrency(min = "0.00", max = "9999999999.99", 
                   precision = 12, scale = 2, cobolCompatible = true,
                   message = "Current cycle debit must be valid positive monetary amount with DECIMAL(12,2) precision")
    @Digits(integer = 10, fraction = 2, message = "Current cycle debit must have maximum 10 integer digits and 2 decimal places")
    private BigDecimal currentCycleDebit;

    /**
     * Account address ZIP code.
     * 
     * Original COBOL: ACCT-ADDR-ZIP PIC X(10)
     * Database: VARCHAR(10)
     * Validation: Must be valid ZIP code format (5 digits or 5+4 digits)
     */
    private String addressZip;

    /**
     * Account group ID for disclosure and interest rate grouping.
     * 
     * Original COBOL: ACCT-GROUP-ID PIC X(10)
     * Database: VARCHAR(10) FOREIGN KEY to disclosure_groups
     * Validation: Must reference valid disclosure group
     */
    private String groupId;

    // ===== CARD RECORD FIELDS (from CVACT02Y.cpy) =====
    
    /**
     * Credit card number for account cross-reference.
     * 
     * Original COBOL: CARD-NUM PIC X(16)
     * Database: VARCHAR(16)
     * Validation: Must be valid 16-digit card number with Luhn checksum
     */
    @ValidCardNumber(enableLuhnValidation = true, allowNullOrEmpty = true,
                     message = "Card number must be valid 16-digit credit card number")
    private String cardNumber;

    /**
     * Card account ID for cross-reference validation.
     * 
     * Original COBOL: CARD-ACCT-ID PIC 9(11)
     * Database: VARCHAR(11) FOREIGN KEY to accounts
     * Validation: Must match accountId for data consistency
     */
    private String cardAccountId;

    /**
     * Card CVV security code.
     * 
     * Original COBOL: CARD-CVV-CD PIC 9(03)
     * Database: VARCHAR(3)
     * Validation: Must be exactly 3 numeric digits
     */
    private String cardCvv;

    /**
     * Card embossed name as it appears on the physical card.
     * 
     * Original COBOL: CARD-EMBOSSED-NAME PIC X(50)
     * Database: VARCHAR(50)
     * Validation: Must be valid cardholder name format
     */
    private String cardEmbossedName;

    /**
     * Card expiration date in CCYYMMDD format.
     * 
     * Original COBOL: CARD-EXPIRAION-DATE PIC X(10) [sic - preserving original typo]
     * Database: DATE
     * Validation: Must be valid future date for card validity
     */
    @ValidCCYYMMDD(fieldName = "Card Expiration Date", strictCentury = true, allowNull = true,
                   message = "Card expiration date must be valid CCYYMMDD format")
    private String cardExpirationDate;

    /**
     * Card active status indicating card operational state.
     * 
     * Original COBOL: CARD-ACTIVE-STATUS PIC X(01)
     * Database: VARCHAR(1) with check constraint ('A', 'I', 'B')
     * Validation: Must be valid CardStatus enum value
     */
    private CardStatus cardActiveStatus;

    // ===== CROSS-REFERENCE RECORD FIELDS (from CVACT03Y.cpy) =====
    
    /**
     * Cross-reference card number for relationship tracking.
     * 
     * Original COBOL: XREF-CARD-NUM PIC X(16)
     * Database: VARCHAR(16)
     * Validation: Must be valid card number for cross-reference consistency
     */
    @ValidCardNumber(enableLuhnValidation = true, allowNullOrEmpty = true,
                     message = "Cross-reference card number must be valid 16-digit credit card number")
    private String xrefCardNumber;

    /**
     * Cross-reference customer ID for relationship validation.
     * 
     * Original COBOL: XREF-CUST-ID PIC 9(09)
     * Database: VARCHAR(9) FOREIGN KEY to customers
     * Validation: Must be valid 9-digit customer ID
     */
    private String xrefCustomerId;

    /**
     * Cross-reference account ID for relationship validation.
     * 
     * Original COBOL: XREF-ACCT-ID PIC 9(11)
     * Database: VARCHAR(11) FOREIGN KEY to accounts
     * Validation: Must match accountId for data consistency
     */
    private String xrefAccountId;

    // ===== CONSTRUCTORS =====

    /**
     * Default constructor for JSON deserialization and JPA entity mapping.
     */
    public AccountDto() {
        // Initialize BigDecimal fields with proper scale for financial calculations
        this.currentBalance = BigDecimalUtils.createDecimal(0.0);
        this.creditLimit = BigDecimalUtils.createDecimal(0.0);
        this.cashCreditLimit = BigDecimalUtils.createDecimal(0.0);
        this.currentCycleCredit = BigDecimalUtils.createDecimal(0.0);
        this.currentCycleDebit = BigDecimalUtils.createDecimal(0.0);
    }

    /**
     * Constructor with primary account fields for basic account creation.
     * 
     * @param accountId The account identifier
     * @param activeStatus The account active status
     * @param currentBalance The current account balance
     * @param creditLimit The credit limit
     */
    public AccountDto(String accountId, AccountStatus activeStatus, 
                     BigDecimal currentBalance, BigDecimal creditLimit) {
        this();
        this.accountId = accountId;
        this.activeStatus = activeStatus;
        this.currentBalance = currentBalance != null ? currentBalance : BigDecimalUtils.createDecimal(0.0);
        this.creditLimit = creditLimit != null ? creditLimit : BigDecimalUtils.createDecimal(0.0);
    }

    // ===== GETTER AND SETTER METHODS =====

    /**
     * Gets the account ID.
     * 
     * @return The 11-digit account identifier
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID with validation.
     * 
     * @param accountId The 11-digit account identifier
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the account active status.
     * 
     * @return The account active status enum
     */
    public AccountStatus getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the account active status.
     * 
     * @param activeStatus The account active status enum
     */
    public void setActiveStatus(AccountStatus activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the current account balance.
     * 
     * @return The current balance with exact COBOL COMP-3 precision
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Sets the current account balance with exact precision.
     * 
     * @param currentBalance The current balance with DECIMAL(12,2) precision
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance != null ? 
            currentBalance.setScale(BigDecimalUtils.MONETARY_SCALE, 
                                  BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.createDecimal(0.0);
    }

    /**
     * Gets the credit limit.
     * 
     * @return The credit limit with exact COBOL COMP-3 precision
     */
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    /**
     * Sets the credit limit with exact precision.
     * 
     * @param creditLimit The credit limit with DECIMAL(12,2) precision
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit != null ? 
            creditLimit.setScale(BigDecimalUtils.MONETARY_SCALE, 
                               BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.createDecimal(0.0);
    }

    /**
     * Gets the cash credit limit.
     * 
     * @return The cash credit limit with exact COBOL COMP-3 precision
     */
    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    /**
     * Sets the cash credit limit with exact precision.
     * 
     * @param cashCreditLimit The cash credit limit with DECIMAL(12,2) precision
     */
    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit != null ? 
            cashCreditLimit.setScale(BigDecimalUtils.MONETARY_SCALE, 
                                   BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.createDecimal(0.0);
    }

    /**
     * Gets the account opening date.
     * 
     * @return The opening date in CCYYMMDD format
     */
    public String getOpenDate() {
        return openDate;
    }

    /**
     * Sets the account opening date.
     * 
     * @param openDate The opening date in CCYYMMDD format
     */
    public void setOpenDate(String openDate) {
        this.openDate = openDate;
    }

    /**
     * Gets the account expiration date.
     * 
     * @return The expiration date in CCYYMMDD format
     */
    public String getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the account expiration date.
     * 
     * @param expirationDate The expiration date in CCYYMMDD format
     */
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the account reissue date.
     * 
     * @return The reissue date in CCYYMMDD format
     */
    public String getReissueDate() {
        return reissueDate;
    }

    /**
     * Sets the account reissue date.
     * 
     * @param reissueDate The reissue date in CCYYMMDD format
     */
    public void setReissueDate(String reissueDate) {
        this.reissueDate = reissueDate;
    }

    /**
     * Gets the current cycle credit amount.
     * 
     * @return The current cycle credit with exact COBOL COMP-3 precision
     */
    public BigDecimal getCurrentCycleCredit() {
        return currentCycleCredit;
    }

    /**
     * Sets the current cycle credit amount with exact precision.
     * 
     * @param currentCycleCredit The current cycle credit with DECIMAL(12,2) precision
     */
    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) {
        this.currentCycleCredit = currentCycleCredit != null ? 
            currentCycleCredit.setScale(BigDecimalUtils.MONETARY_SCALE, 
                                      BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.createDecimal(0.0);
    }

    /**
     * Gets the current cycle debit amount.
     * 
     * @return The current cycle debit with exact COBOL COMP-3 precision
     */
    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }

    /**
     * Sets the current cycle debit amount with exact precision.
     * 
     * @param currentCycleDebit The current cycle debit with DECIMAL(12,2) precision
     */
    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) {
        this.currentCycleDebit = currentCycleDebit != null ? 
            currentCycleDebit.setScale(BigDecimalUtils.MONETARY_SCALE, 
                                     BigDecimalUtils.DECIMAL128_CONTEXT.getRoundingMode()) : 
            BigDecimalUtils.createDecimal(0.0);
    }

    /**
     * Gets the account address ZIP code.
     * 
     * @return The ZIP code
     */
    public String getAddressZip() {
        return addressZip;
    }

    /**
     * Sets the account address ZIP code.
     * 
     * @param addressZip The ZIP code
     */
    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }

    /**
     * Gets the account group ID.
     * 
     * @return The group ID for disclosure and interest rate grouping
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Sets the account group ID.
     * 
     * @param groupId The group ID for disclosure and interest rate grouping
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Gets the card number.
     * 
     * @return The 16-digit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number.
     * 
     * @param cardNumber The 16-digit card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the card account ID.
     * 
     * @return The card account ID
     */
    public String getCardAccountId() {
        return cardAccountId;
    }

    /**
     * Sets the card account ID.
     * 
     * @param cardAccountId The card account ID
     */
    public void setCardAccountId(String cardAccountId) {
        this.cardAccountId = cardAccountId;
    }

    /**
     * Gets the card CVV security code.
     * 
     * @return The 3-digit CVV code
     */
    public String getCardCvv() {
        return cardCvv;
    }

    /**
     * Sets the card CVV security code.
     * 
     * @param cardCvv The 3-digit CVV code
     */
    public void setCardCvv(String cardCvv) {
        this.cardCvv = cardCvv;
    }

    /**
     * Gets the card embossed name.
     * 
     * @return The name as it appears on the card
     */
    public String getCardEmbossedName() {
        return cardEmbossedName;
    }

    /**
     * Sets the card embossed name.
     * 
     * @param cardEmbossedName The name as it appears on the card
     */
    public void setCardEmbossedName(String cardEmbossedName) {
        this.cardEmbossedName = cardEmbossedName;
    }

    /**
     * Gets the card expiration date.
     * 
     * @return The card expiration date in CCYYMMDD format
     */
    public String getCardExpirationDate() {
        return cardExpirationDate;
    }

    /**
     * Sets the card expiration date.
     * 
     * @param cardExpirationDate The card expiration date in CCYYMMDD format
     */
    public void setCardExpirationDate(String cardExpirationDate) {
        this.cardExpirationDate = cardExpirationDate;
    }

    /**
     * Gets the card active status.
     * 
     * @return The card active status enum
     */
    public CardStatus getCardActiveStatus() {
        return cardActiveStatus;
    }

    /**
     * Sets the card active status.
     * 
     * @param cardActiveStatus The card active status enum
     */
    public void setCardActiveStatus(CardStatus cardActiveStatus) {
        this.cardActiveStatus = cardActiveStatus;
    }

    /**
     * Gets the cross-reference card number.
     * 
     * @return The cross-reference card number
     */
    public String getXrefCardNumber() {
        return xrefCardNumber;
    }

    /**
     * Sets the cross-reference card number.
     * 
     * @param xrefCardNumber The cross-reference card number
     */
    public void setXrefCardNumber(String xrefCardNumber) {
        this.xrefCardNumber = xrefCardNumber;
    }

    /**
     * Gets the cross-reference customer ID.
     * 
     * @return The cross-reference customer ID
     */
    public String getXrefCustomerId() {
        return xrefCustomerId;
    }

    /**
     * Sets the cross-reference customer ID.
     * 
     * @param xrefCustomerId The cross-reference customer ID
     */
    public void setXrefCustomerId(String xrefCustomerId) {
        this.xrefCustomerId = xrefCustomerId;
    }

    /**
     * Gets the cross-reference account ID.
     * 
     * @return The cross-reference account ID
     */
    public String getXrefAccountId() {
        return xrefAccountId;
    }

    /**
     * Sets the cross-reference account ID.
     * 
     * @param xrefAccountId The cross-reference account ID
     */
    public void setXrefAccountId(String xrefAccountId) {
        this.xrefAccountId = xrefAccountId;
    }

    // ===== BUSINESS LOGIC METHODS =====

    /**
     * Validates the account DTO using business rules and field constraints.
     * 
     * This method performs comprehensive validation including:
     * - Account ID format and range validation
     * - Financial amount precision and range validation
     * - Date format validation with century restrictions
     * - Cross-reference consistency validation
     * - Card number Luhn algorithm validation
     * 
     * @return true if all validation rules pass, false otherwise
     */
    public boolean validate() {
        // Validate account ID
        if (accountId != null && !ValidationUtils.validateAccountNumber(accountId).isValid()) {
            return false;
        }

        // Validate monetary fields
        if (currentBalance != null && !BigDecimalUtils.isValidMonetaryAmount(currentBalance)) {
            return false;
        }
        if (creditLimit != null && !BigDecimalUtils.isValidMonetaryAmount(creditLimit)) {
            return false;
        }
        if (cashCreditLimit != null && !BigDecimalUtils.isValidMonetaryAmount(cashCreditLimit)) {
            return false;
        }
        if (currentCycleCredit != null && !BigDecimalUtils.isValidMonetaryAmount(currentCycleCredit)) {
            return false;
        }
        if (currentCycleDebit != null && !BigDecimalUtils.isValidMonetaryAmount(currentCycleDebit)) {
            return false;
        }

        // Validate date fields
        if (openDate != null && !ValidationUtils.isValidDateFormat(openDate)) {
            return false;
        }
        if (expirationDate != null && !ValidationUtils.isValidDateFormat(expirationDate)) {
            return false;
        }
        if (reissueDate != null && !ValidationUtils.isValidDateFormat(reissueDate)) {
            return false;
        }
        if (cardExpirationDate != null && !ValidationUtils.isValidDateFormat(cardExpirationDate)) {
            return false;
        }

        // Validate ZIP code
        if (addressZip != null && !ValidationUtils.validateZipCode(addressZip).isValid()) {
            return false;
        }

        // Validate card numbers using Luhn algorithm
        if (cardNumber != null && !ValidationUtils.validateCardNumber(cardNumber).isValid()) {
            return false;
        }
        if (xrefCardNumber != null && !ValidationUtils.validateCardNumber(xrefCardNumber).isValid()) {
            return false;
        }

        // Validate cross-reference consistency
        if (cardAccountId != null && accountId != null && !cardAccountId.equals(accountId)) {
            return false;
        }
        if (xrefAccountId != null && accountId != null && !xrefAccountId.equals(accountId)) {
            return false;
        }

        return true;
    }

    // ===== OBJECT METHODS =====

    /**
     * Indicates whether some other object is "equal to" this one.
     * 
     * @param o the reference object with which to compare
     * @return true if this object is the same as the o argument; false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        AccountDto that = (AccountDto) o;
        
        return Objects.equals(accountId, that.accountId) &&
               Objects.equals(activeStatus, that.activeStatus) &&
               Objects.equals(currentBalance, that.currentBalance) &&
               Objects.equals(creditLimit, that.creditLimit) &&
               Objects.equals(cashCreditLimit, that.cashCreditLimit) &&
               Objects.equals(openDate, that.openDate) &&
               Objects.equals(expirationDate, that.expirationDate) &&
               Objects.equals(reissueDate, that.reissueDate) &&
               Objects.equals(currentCycleCredit, that.currentCycleCredit) &&
               Objects.equals(currentCycleDebit, that.currentCycleDebit) &&
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
     * Returns a hash code value for the object.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId, activeStatus, currentBalance, creditLimit, cashCreditLimit,
                          openDate, expirationDate, reissueDate, currentCycleCredit, currentCycleDebit,
                          addressZip, groupId, cardNumber, cardAccountId, cardCvv, cardEmbossedName,
                          cardExpirationDate, cardActiveStatus, xrefCardNumber, xrefCustomerId, xrefAccountId);
    }

    /**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        return "AccountDto{" +
               "accountId='" + accountId + '\'' +
               ", activeStatus=" + activeStatus +
               ", currentBalance=" + (currentBalance != null ? BigDecimalUtils.formatCurrency(currentBalance) : "null") +
               ", creditLimit=" + (creditLimit != null ? BigDecimalUtils.formatCurrency(creditLimit) : "null") +
               ", cashCreditLimit=" + (cashCreditLimit != null ? BigDecimalUtils.formatCurrency(cashCreditLimit) : "null") +
               ", openDate='" + openDate + '\'' +
               ", expirationDate='" + expirationDate + '\'' +
               ", reissueDate='" + reissueDate + '\'' +
               ", currentCycleCredit=" + (currentCycleCredit != null ? BigDecimalUtils.formatCurrency(currentCycleCredit) : "null") +
               ", currentCycleDebit=" + (currentCycleDebit != null ? BigDecimalUtils.formatCurrency(currentCycleDebit) : "null") +
               ", addressZip='" + addressZip + '\'' +
               ", groupId='" + groupId + '\'' +
               ", cardNumber='" + (cardNumber != null ? cardNumber.substring(0, 4) + "****" + cardNumber.substring(12) : "null") + '\'' +
               ", cardAccountId='" + cardAccountId + '\'' +
               ", cardCvv='" + (cardCvv != null ? "***" : "null") + '\'' +
               ", cardEmbossedName='" + cardEmbossedName + '\'' +
               ", cardExpirationDate='" + cardExpirationDate + '\'' +
               ", cardActiveStatus=" + cardActiveStatus +
               ", xrefCardNumber='" + (xrefCardNumber != null ? xrefCardNumber.substring(0, 4) + "****" + xrefCardNumber.substring(12) : "null") + '\'' +
               ", xrefCustomerId='" + xrefCustomerId + '\'' +
               ", xrefAccountId='" + xrefAccountId + '\'' +
               '}';
    }
}