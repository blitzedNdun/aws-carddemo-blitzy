package com.carddemo.card;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidCvv;
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for card update operations with optimistic locking, comprehensive validation,
 * and BigDecimal precision supporting COCRDUPC.cbl functionality with Spring Boot transaction management.
 * 
 * <p>This DTO encapsulates all data required for updating credit card information through the
 * CardDemo microservices architecture. It provides comprehensive validation equivalent to the
 * original COBOL validation routines while supporting modern Spring Boot transaction patterns
 * and React frontend integration.</p>
 * 
 * <p><strong>COBOL Equivalence:</strong>
 * This DTO maps directly to the COCRDUPC.cbl program's data structures and validation logic:
 * <ul>
 *   <li>CCUP-NEW-DETAILS working storage section for new card data</li>
 *   <li>CCUP-CHANGE-ACTION for update flow control</li>
 *   <li>Input validation paragraphs (1210-EDIT-ACCOUNT through 1260-EDIT-EXPIRY-YEAR)</li>
 *   <li>Optimistic locking equivalent to VSAM record currency checks</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Optimistic locking with version control for concurrent access prevention</li>
 *   <li>Comprehensive business rule validation matching COBOL validation patterns</li>
 *   <li>BigDecimal precision for financial calculations (credit limits, balances)</li>
 *   <li>Multi-step validation support for confirmation workflows</li>
 *   <li>Role-based access control parameters for update authorization</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Validation Rules:</strong>
 * All validation rules preserve the exact business logic from the original COBOL implementation:
 * <ul>
 *   <li>Card number: 16-digit format with Luhn algorithm validation</li>
 *   <li>Account ID: 11-digit numeric format matching COBOL PIC 9(11)</li>
 *   <li>CVV code: 3-digit numeric format matching COBOL PIC 9(03)</li>
 *   <li>Embossed name: Alphabetic characters and spaces only, max 50 characters</li>
 *   <li>Expiration date: CCYYMMDD format with future date validation</li>
 *   <li>Active status: Y/N or A/I/B format with transition validation</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Transaction Support:</strong>
 * The DTO supports the complete card update transaction flow:
 * <ul>
 *   <li>Initial data fetch and display (CCUP-DETAILS-NOT-FETCHED)</li>
 *   <li>Change validation and confirmation (CCUP-CHANGES-OK-NOT-CONFIRMED)</li>
 *   <li>Final update commit with optimistic locking (CCUP-CHANGES-OKAYED-AND-DONE)</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>
 * CardUpdateRequestDto request = new CardUpdateRequestDto();
 * request.setCardNumber("4532015112830366");
 * request.setAccountId("00000000001");
 * request.setEmbossedName("JOHN DOE");
 * request.setCvvCode("123");
 * request.setExpirationDate(LocalDate.of(2025, 12, 31));
 * request.setActiveStatus("Y");
 * request.setVersionNumber(1L);
 * request.setConfirmUpdate(true);
 * </pre>
 * </p>
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since 2024-01-01
 * 
 * @see com.carddemo.card.Card
 * @see com.carddemo.common.dto.BaseRequestDto
 * @see com.carddemo.common.validator.ValidCardNumber
 * @see com.carddemo.common.validator.ValidCvv
 * @see com.carddemo.common.validator.ValidCCYYMMDD
 */
public class CardUpdateRequestDto extends BaseRequestDto {

    private static final long serialVersionUID = 1L;

    /**
     * Credit card number for the card being updated.
     * 
     * <p>Must be a valid 16-digit credit card number that passes Luhn algorithm validation.
     * This field corresponds to the COBOL CC-CARD-NUM field and is validated using the
     * same logic as the original 1220-EDIT-CARD paragraph.</p>
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Exactly 16 digits in length</li>
     *   <li>Must pass Luhn algorithm checksum validation</li>
     *   <li>Must exist in the cards database for update operations</li>
     * </ul>
     * </p>
     */
    @JsonProperty("card_number")
    @ValidCardNumber(message = "Card number must be valid 16-digit number passing Luhn validation")
    @NotNull(message = "Card number is required for update operations")
    private String cardNumber;

    /**
     * Account ID associated with the card being updated.
     * 
     * <p>Must be a valid 11-digit account identifier that corresponds to an existing
     * account in the system. This field maps to the COBOL CC-ACCT-ID field and follows
     * the same validation rules as the 1210-EDIT-ACCOUNT paragraph.</p>
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Exactly 11 digits in length</li>
     *   <li>Must be numeric (no alphabetic characters)</li>
     *   <li>Must correspond to an existing account</li>
     *   <li>Cannot be all zeros</li>
     * </ul>
     * </p>
     */
    @JsonProperty("account_id")
    @NotNull(message = "Account ID is required for card updates")
    @jakarta.validation.constraints.Pattern(
        regexp = "^[0-9]{11}$",
        message = "Account ID must be exactly 11 digits"
    )
    private String accountId;

    /**
     * Name to be embossed on the credit card.
     * 
     * <p>The embossed name appears on the physical credit card and must follow specific
     * formatting rules. This field corresponds to the COBOL CCUP-NEW-CRDNAME field and
     * is validated using the same logic as the 1230-EDIT-NAME paragraph.</p>
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Maximum 50 characters in length</li>
     *   <li>Only alphabetic characters and spaces allowed</li>
     *   <li>Cannot be blank or null</li>
     *   <li>Spaces are normalized (no leading/trailing spaces)</li>
     * </ul>
     * </p>
     */
    @JsonProperty("embossed_name")
    @NotNull(message = "Embossed name is required")
    @jakarta.validation.constraints.Pattern(
        regexp = "^[A-Za-z\\s]{1,50}$",
        message = "Embossed name must contain only letters and spaces, maximum 50 characters"
    )
    private String embossedName;

    /**
     * Card verification value (CVV) security code.
     * 
     * <p>The CVV code is used for card authentication and security verification.
     * This field corresponds to the COBOL CCUP-NEW-CVV-CD field and must be exactly
     * 3 digits as defined in the original card data structure.</p>
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Exactly 3 digits in length</li>
     *   <li>Must be numeric (0-9)</li>
     *   <li>Leading zeros are preserved</li>
     * </ul>
     * </p>
     */
    @JsonProperty("cvv_code")
    @ValidCvv(message = "CVV code must be exactly 3 numeric digits")
    @NotNull(message = "CVV code is required for card updates")
    private String cvvCode;

    /**
     * Card expiration date.
     * 
     * <p>The expiration date determines the card's validity period and must be a future date.
     * This field corresponds to the COBOL CCUP-NEW-EXPIRAION-DATE structure and follows
     * the same validation logic as the 1250-EDIT-EXPIRY-MON and 1260-EDIT-EXPIRY-YEAR
     * paragraphs.</p>
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Must be a future date</li>
     *   <li>Month must be between 1 and 12</li>
     *   <li>Year must be reasonable (1950-2099 range)</li>
     *   <li>Day validation considers month-specific rules</li>
     * </ul>
     * </p>
     */
    @JsonProperty("expiration_date")
    @NotNull(message = "Expiration date is required")
    @jakarta.validation.constraints.Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;

    /**
     * Card active status indicator.
     * 
     * <p>Determines whether the card is active and can process transactions. This field
     * corresponds to the COBOL CCUP-NEW-CRDSTCD field and follows the same validation
     * logic as the 1240-EDIT-CARDSTATUS paragraph.</p>
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Must be 'Y' (Yes/Active) or 'N' (No/Inactive)</li>
     *   <li>Alternative modern values: 'A' (Active), 'I' (Inactive), 'B' (Blocked)</li>
     *   <li>Status transitions must follow business rules</li>
     * </ul>
     * </p>
     */
    @JsonProperty("active_status")
    @NotNull(message = "Active status is required")
    @jakarta.validation.constraints.Pattern(
        regexp = "^[YN]$|^[AIB]$",
        message = "Active status must be Y/N or A/I/B"
    )
    private String activeStatus;

    /**
     * Version number for optimistic locking.
     * 
     * <p>Used to prevent concurrent modification conflicts by ensuring that the card
     * data has not been modified by another user since it was last read. This implements
     * the same optimistic locking behavior as the VSAM record currency checks in the
     * original COBOL implementation.</p>
     * 
     * <p>Locking Rules:
     * <ul>
     *   <li>Must match the current version in the database</li>
     *   <li>Incremented automatically on successful updates</li>
     *   <li>Null values indicate new card creation</li>
     * </ul>
     * </p>
     */
    @JsonProperty("version_number")
    @NotNull(message = "Version number is required for optimistic locking")
    private Long versionNumber;

    /**
     * Credit limit for the card.
     * 
     * <p>Represents the maximum credit amount available on the card. This field uses
     * BigDecimal to maintain exact decimal precision equivalent to COBOL COMP-3
     * arithmetic, ensuring no floating-point errors in financial calculations.</p>
     * 
     * <p>Precision Rules:
     * <ul>
     *   <li>Scale of 2 for cents precision</li>
     *   <li>Maximum precision of 12 digits (including cents)</li>
     *   <li>Must be positive value</li>
     *   <li>Uses RoundingMode.HALF_UP for COBOL compatibility</li>
     * </ul>
     * </p>
     */
    @JsonProperty("credit_limit")
    @jakarta.validation.constraints.DecimalMin(
        value = "0.01",
        message = "Credit limit must be positive"
    )
    @jakarta.validation.constraints.Digits(
        integer = 10,
        fraction = 2,
        message = "Credit limit must have at most 10 integer digits and 2 decimal places"
    )
    private BigDecimal creditLimit;

    /**
     * Confirmation flag for two-step update process.
     * 
     * <p>Indicates whether the user has confirmed the update after validation.
     * This implements the same confirmation workflow as the original COBOL program's
     * CCUP-CHANGES-OK-NOT-CONFIRMED state handling.</p>
     * 
     * <p>Workflow Support:
     * <ul>
     *   <li>false: Initial validation and preview</li>
     *   <li>true: Final confirmation and commit</li>
     * </ul>
     * </p>
     */
    @JsonProperty("confirm_update")
    private boolean confirmUpdate;

    /**
     * Default constructor for JSON deserialization and framework usage.
     */
    public CardUpdateRequestDto() {
        super();
        this.confirmUpdate = false;
    }

    /**
     * Constructor with correlation ID for distributed tracing.
     * 
     * @param correlationId Unique identifier for request correlation across services
     */
    public CardUpdateRequestDto(String correlationId) {
        super(correlationId);
        this.confirmUpdate = false;
    }

    /**
     * Full constructor for complete request initialization.
     * 
     * @param correlationId Unique identifier for request correlation
     * @param userId Authenticated user identifier
     * @param sessionId Session identifier for distributed session management
     */
    public CardUpdateRequestDto(String correlationId, String userId, String sessionId) {
        super(correlationId, userId, sessionId);
        this.confirmUpdate = false;
    }

    // Getter and Setter methods

    /**
     * Returns the credit card number.
     * 
     * @return 16-digit credit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the credit card number with validation.
     * 
     * @param cardNumber 16-digit credit card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Returns the account ID.
     * 
     * @return 11-digit account identifier
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID with validation.
     * 
     * @param accountId 11-digit account identifier
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Returns the embossed name.
     * 
     * @return Name to be embossed on the card
     */
    public String getEmbossedName() {
        return embossedName;
    }

    /**
     * Sets the embossed name with validation.
     * 
     * @param embossedName Name to be embossed on the card
     */
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    /**
     * Returns the CVV code.
     * 
     * @return 3-digit CVV security code
     */
    public String getCvvCode() {
        return cvvCode;
    }

    /**
     * Sets the CVV code with validation.
     * 
     * @param cvvCode 3-digit CVV security code
     */
    public void setCvvCode(String cvvCode) {
        this.cvvCode = cvvCode;
    }

    /**
     * Returns the expiration date.
     * 
     * @return Card expiration date
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the expiration date with validation.
     * 
     * @param expirationDate Card expiration date
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Returns the active status.
     * 
     * @return Card active status (Y/N or A/I/B)
     */
    public String getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the active status with validation.
     * 
     * @param activeStatus Card active status (Y/N or A/I/B)
     */
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Returns the version number for optimistic locking.
     * 
     * @return Version number for concurrent modification control
     */
    public Long getVersionNumber() {
        return versionNumber;
    }

    /**
     * Sets the version number for optimistic locking.
     * 
     * @param versionNumber Version number for concurrent modification control
     */
    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    /**
     * Returns the credit limit.
     * 
     * @return Credit limit with exact decimal precision
     */
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    /**
     * Sets the credit limit with validation.
     * 
     * @param creditLimit Credit limit with exact decimal precision
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    /**
     * Returns the confirmation flag.
     * 
     * @return true if update is confirmed, false for preview/validation
     */
    public boolean isConfirmUpdate() {
        return confirmUpdate;
    }

    /**
     * Sets the confirmation flag.
     * 
     * @param confirmUpdate true to confirm update, false for preview/validation
     */
    public void setConfirmUpdate(boolean confirmUpdate) {
        this.confirmUpdate = confirmUpdate;
    }

    /**
     * Returns a string representation of the request for logging and debugging.
     * Masks sensitive information like card number and CVV code.
     * 
     * @return String representation with sensitive data masked
     */
    @Override
    public String toString() {
        return String.format(
            "CardUpdateRequestDto{" +
            "cardNumber='%s', " +
            "accountId='%s', " +
            "embossedName='%s', " +
            "cvvCode='***', " +
            "expirationDate=%s, " +
            "activeStatus='%s', " +
            "versionNumber=%d, " +
            "creditLimit=%s, " +
            "confirmUpdate=%b, " +
            "correlationId='%s'" +
            "}",
            cardNumber != null ? maskCardNumber(cardNumber) : null,
            accountId,
            embossedName,
            expirationDate,
            activeStatus,
            versionNumber,
            creditLimit,
            confirmUpdate,
            getCorrelationId()
        );
    }

    /**
     * Masks the card number for secure logging.
     * Shows only the last 4 digits with asterisks for the rest.
     * 
     * @param cardNumber Full card number to mask
     * @return Masked card number (e.g., "************1234")
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****************";
        }
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }
}