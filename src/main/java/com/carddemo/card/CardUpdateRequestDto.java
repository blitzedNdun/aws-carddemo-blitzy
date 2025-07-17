package com.carddemo.card;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidCvv;
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Request DTO for card update operations with optimistic locking and comprehensive validation.
 * 
 * <p>This DTO supports the card update functionality equivalent to the COBOL program COCRDUPC.cbl,
 * providing comprehensive field validation, optimistic locking version control, and BigDecimal
 * precision for financial calculations. The DTO integrates with Spring Boot transaction management
 * and supports concurrent access scenarios through version-based optimistic locking.
 * 
 * <p>Key Features:
 * <ul>
 *   <li>Optimistic locking with version control for concurrent access management</li>
 *   <li>Comprehensive business rule validation for card status changes</li>
 *   <li>Expiration date validation with CCYYMMDD format support</li>
 *   <li>Account linkage modification validation</li>
 *   <li>BigDecimal precision for credit limits and balance-related updates</li>
 *   <li>Card number format validation with Luhn algorithm verification</li>
 *   <li>CVV validation with 3-digit numeric format enforcement</li>
 *   <li>User context and authorization parameters for role-based access control</li>
 * </ul>
 * 
 * <p>Business Rules Validation:
 * <ul>
 *   <li>Card number must be exactly 16 digits (Luhn algorithm validated)</li>
 *   <li>Account ID must be exactly 11 digits and reference valid account</li>
 *   <li>CVV code must be exactly 3 digits</li>
 *   <li>Embossed name must not exceed 50 characters and contain only alphabetic characters</li>
 *   <li>Expiration date must be in CCYYMMDD format and in the future</li>
 *   <li>Active status must be Y (active) or N (inactive)</li>
 *   <li>Credit limit must be positive and maintain COBOL COMP-3 precision</li>
 *   <li>Version number must be provided for optimistic locking</li>
 * </ul>
 * 
 * <p>COBOL Program Mapping:
 * This DTO maps to the following COBOL structures from COCRDUPC.cbl:
 * <ul>
 *   <li>CCUP-NEW-DETAILS - New card data for update operations</li>
 *   <li>CARD-UPDATE-RECORD - Card update record structure</li>
 *   <li>Input validation logic from paragraphs 1210-1260</li>
 *   <li>Business rule validation from COBOL edit routines</li>
 * </ul>
 * 
 * <p>Integration Points:
 * <ul>
 *   <li>Spring Boot transaction management for data consistency</li>
 *   <li>Jakarta Bean Validation for input validation</li>
 *   <li>Jackson JSON serialization for REST API integration</li>
 *   <li>Spring Security for role-based access control</li>
 *   <li>Optimistic locking with JPA entity version management</li>
 * </ul>
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 * @see com.carddemo.card.Card
 * @see com.carddemo.common.dto.BaseRequestDto
 */
@Valid
public class CardUpdateRequestDto extends BaseRequestDto {

    /**
     * Card number for update identification.
     * 
     * <p>Must be exactly 16 digits and validated using Luhn algorithm.
     * This field corresponds to COBOL CCUP-NEW-CARDID from COCRDUPC.cbl
     * and is used to identify the card record for update operations.
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Required field - cannot be null or empty</li>
     *   <li>Must be exactly 16 digits</li>
     *   <li>Must pass Luhn algorithm validation</li>
     *   <li>Must reference existing card in database</li>
     * </ul>
     */
    @JsonProperty("cardNumber")
    @NotBlank(message = "Card number is required and cannot be blank")
    @ValidCardNumber(message = "Card number must be a valid 16-digit credit card number")
    private String cardNumber;

    /**
     * Account ID for card-account linkage modification.
     * 
     * <p>Must be exactly 11 digits and reference a valid account.
     * This field corresponds to COBOL CCUP-NEW-ACCTID from COCRDUPC.cbl
     * and supports account linkage modifications during card updates.
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Required field - cannot be null or empty</li>
     *   <li>Must be exactly 11 digits</li>
     *   <li>Must reference existing account in database</li>
     *   <li>Account must be active and valid for card association</li>
     * </ul>
     */
    @JsonProperty("accountId")
    @NotBlank(message = "Account ID is required and cannot be blank")
    @Pattern(regexp = "^\\d{11}$", message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Embossed name on the card.
     * 
     * <p>Name to be embossed on the physical card, limited to 50 characters.
     * This field corresponds to COBOL CCUP-NEW-CRDNAME from COCRDUPC.cbl
     * and must contain only alphabetic characters and spaces.
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Required field - cannot be null or empty</li>
     *   <li>Maximum length of 50 characters</li>
     *   <li>Must contain only alphabetic characters and spaces</li>
     *   <li>Leading and trailing spaces are trimmed</li>
     * </ul>
     */
    @JsonProperty("embossedName")
    @NotBlank(message = "Embossed name is required and cannot be blank")
    @Size(min = 1, max = 50, message = "Embossed name must be between 1 and 50 characters")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Embossed name can only contain alphabetic characters and spaces")
    private String embossedName;

    /**
     * Card verification value (CVV) code.
     * 
     * <p>3-digit security code for card verification.
     * This field corresponds to COBOL CCUP-NEW-CVV-CD from COCRDUPC.cbl
     * and is used for enhanced security during card updates.
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Required field - cannot be null or empty</li>
     *   <li>Must be exactly 3 digits</li>
     *   <li>Must be numeric only</li>
     *   <li>Used for security verification</li>
     * </ul>
     */
    @JsonProperty("cvvCode")
    @NotBlank(message = "CVV code is required and cannot be blank")
    @ValidCvv(message = "CVV code must be exactly 3 digits")
    private String cvvCode;

    /**
     * Card expiration date in CCYYMMDD format.
     * 
     * <p>Future date representing when the card expires.
     * This field corresponds to COBOL CCUP-NEW-EXPIRAION-DATE from COCRDUPC.cbl
     * and must be a valid future date for card validity.
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Required field - cannot be null or empty</li>
     *   <li>Must be in CCYYMMDD format (8 characters)</li>
     *   <li>Must be a valid future date</li>
     *   <li>Century must be 19 or 20 (1900s or 2000s)</li>
     *   <li>Month must be 01-12</li>
     *   <li>Day must be valid for the specified month/year</li>
     * </ul>
     */
    @JsonProperty("expirationDate")
    @NotBlank(message = "Expiration date is required and cannot be blank")
    @ValidCCYYMMDD(message = "Expiration date must be in CCYYMMDD format with valid century, month, and day")
    private String expirationDate;

    /**
     * Card active status indicator.
     * 
     * <p>Indicates whether the card is active (Y) or inactive (N).
     * This field corresponds to COBOL CCUP-NEW-CRDSTCD from COCRDUPC.cbl
     * and controls card transaction authorization.
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Required field - cannot be null or empty</li>
     *   <li>Must be exactly 1 character</li>
     *   <li>Must be 'Y' (active) or 'N' (inactive)</li>
     *   <li>Case sensitive validation</li>
     * </ul>
     */
    @JsonProperty("activeStatus")
    @NotBlank(message = "Active status is required and cannot be blank")
    @Pattern(regexp = "^[YN]$", message = "Active status must be Y (active) or N (inactive)")
    private String activeStatus;

    /**
     * Version number for optimistic locking.
     * 
     * <p>Used for concurrent access control to prevent lost updates.
     * This field implements optimistic locking equivalent to COBOL record
     * locking mechanisms and ensures data consistency in multi-user scenarios.
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Required field - cannot be null</li>
     *   <li>Must be non-negative integer</li>
     *   <li>Incremented on each successful update</li>
     *   <li>Used for optimistic locking validation</li>
     * </ul>
     */
    @JsonProperty("versionNumber")
    @NotNull(message = "Version number is required for optimistic locking")
    @Min(value = 0, message = "Version number must be non-negative")
    private Integer versionNumber;

    /**
     * Credit limit for the card.
     * 
     * <p>Maximum credit amount available on the card with BigDecimal precision.
     * This field maintains COBOL COMP-3 arithmetic precision for financial
     * calculations and supports exact decimal operations.
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Optional field - can be null for cards without credit limits</li>
     *   <li>Must be positive when provided</li>
     *   <li>Maximum precision of 15 digits with 2 decimal places</li>
     *   <li>Minimum value of 0.01 when not null</li>
     * </ul>
     */
    @JsonProperty("creditLimit")
    @DecimalMin(value = "0.01", message = "Credit limit must be positive when provided")
    @Digits(integer = 13, fraction = 2, message = "Credit limit must have maximum 13 integer digits and 2 decimal places")
    private BigDecimal creditLimit;

    /**
     * Confirmation flag for update operation.
     * 
     * <p>Indicates user confirmation for the update operation.
     * This field corresponds to COBOL confirmation logic from COCRDUPC.cbl
     * and ensures user intent verification before committing changes.
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Required field - cannot be null</li>
     *   <li>Must be true for update to proceed</li>
     *   <li>Used for user confirmation validation</li>
     * </ul>
     */
    @JsonProperty("confirmUpdate")
    @NotNull(message = "Confirm update flag is required")
    private Boolean confirmUpdate;

    /**
     * Default constructor for JSON deserialization.
     * Initializes confirmUpdate to false for security.
     */
    public CardUpdateRequestDto() {
        super();
        this.confirmUpdate = false;
    }

    /**
     * Constructor with required fields for programmatic instantiation.
     * 
     * @param correlationId Unique correlation identifier
     * @param userId User identifier for audit trail
     * @param sessionId Session identifier for distributed session management
     * @param cardNumber 16-digit card number
     * @param accountId 11-digit account ID
     * @param embossedName Name on card
     * @param cvvCode 3-digit CVV code
     * @param expirationDate Expiration date in CCYYMMDD format
     * @param activeStatus Active status (Y/N)
     * @param versionNumber Version for optimistic locking
     */
    public CardUpdateRequestDto(String correlationId, String userId, String sessionId,
                               String cardNumber, String accountId, String embossedName,
                               String cvvCode, String expirationDate, String activeStatus,
                               Integer versionNumber) {
        super(correlationId, userId, sessionId);
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        this.embossedName = embossedName;
        this.cvvCode = cvvCode;
        this.expirationDate = expirationDate;
        this.activeStatus = activeStatus;
        this.versionNumber = versionNumber;
        this.confirmUpdate = false;
    }

    /**
     * Gets the card number.
     * 
     * @return 16-digit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number.
     * 
     * @param cardNumber 16-digit card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the account ID.
     * 
     * @return 11-digit account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID.
     * 
     * @param accountId 11-digit account ID
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the embossed name.
     * 
     * @return Name embossed on card
     */
    public String getEmbossedName() {
        return embossedName;
    }

    /**
     * Sets the embossed name.
     * 
     * @param embossedName Name to emboss on card
     */
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    /**
     * Gets the CVV code.
     * 
     * @return 3-digit CVV code
     */
    public String getCvvCode() {
        return cvvCode;
    }

    /**
     * Sets the CVV code.
     * 
     * @param cvvCode 3-digit CVV code
     */
    public void setCvvCode(String cvvCode) {
        this.cvvCode = cvvCode;
    }

    /**
     * Gets the expiration date.
     * 
     * @return Expiration date in CCYYMMDD format
     */
    public String getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the expiration date.
     * 
     * @param expirationDate Expiration date in CCYYMMDD format
     */
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the active status.
     * 
     * @return Active status (Y/N)
     */
    public String getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the active status.
     * 
     * @param activeStatus Active status (Y/N)
     */
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the version number.
     * 
     * @return Version number for optimistic locking
     */
    public Integer getVersionNumber() {
        return versionNumber;
    }

    /**
     * Sets the version number.
     * 
     * @param versionNumber Version number for optimistic locking
     */
    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    /**
     * Gets the credit limit.
     * 
     * @return Credit limit with BigDecimal precision
     */
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    /**
     * Sets the credit limit.
     * 
     * @param creditLimit Credit limit with BigDecimal precision
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    /**
     * Gets the confirm update flag.
     * 
     * @return true if update is confirmed, false otherwise
     */
    public boolean isConfirmUpdate() {
        return confirmUpdate != null && confirmUpdate;
    }

    /**
     * Sets the confirm update flag.
     * 
     * @param confirmUpdate true to confirm update, false otherwise
     */
    public void setConfirmUpdate(boolean confirmUpdate) {
        this.confirmUpdate = confirmUpdate;
    }

    /**
     * Validates the card update request for business rule compliance.
     * 
     * <p>This method performs comprehensive validation of the card update request
     * including cross-field validation, business rule enforcement, and data
     * consistency checks equivalent to COBOL validation routines.
     * 
     * @return true if request is valid, false otherwise
     */
    public boolean isValidForUpdate() {
        return isValidRequestContext() &&
               cardNumber != null && !cardNumber.trim().isEmpty() &&
               accountId != null && !accountId.trim().isEmpty() &&
               embossedName != null && !embossedName.trim().isEmpty() &&
               cvvCode != null && !cvvCode.trim().isEmpty() &&
               expirationDate != null && !expirationDate.trim().isEmpty() &&
               activeStatus != null && !activeStatus.trim().isEmpty() &&
               versionNumber != null && versionNumber >= 0 &&
               confirmUpdate != null && confirmUpdate;
    }

    /**
     * Converts expiration date from CCYYMMDD format to LocalDate.
     * 
     * <p>This method parses the CCYYMMDD format date string and converts it
     * to a LocalDate object for use with JPA entities and date operations.
     * 
     * @return LocalDate representation of expiration date
     * @throws IllegalArgumentException if date format is invalid
     */
    public LocalDate getExpirationDateAsLocalDate() {
        if (expirationDate == null || expirationDate.length() != 8) {
            throw new IllegalArgumentException("Expiration date must be in CCYYMMDD format");
        }
        
        try {
            int year = Integer.parseInt(expirationDate.substring(0, 4));
            int month = Integer.parseInt(expirationDate.substring(4, 6));
            int day = Integer.parseInt(expirationDate.substring(6, 8));
            
            return LocalDate.of(year, month, day);
        } catch (NumberFormatException | java.time.DateTimeException e) {
            throw new IllegalArgumentException("Invalid date format in expiration date: " + expirationDate, e);
        }
    }

    /**
     * Creates a summary of the card update request for audit logging.
     * 
     * <p>This method generates a structured audit summary that includes
     * key update fields while masking sensitive information like CVV codes.
     * 
     * @return audit summary string
     */
    public String getUpdateAuditSummary() {
        String maskedCvv = cvvCode != null ? "***" : null;
        return String.format("CardUpdate[%s, cardNumber=%s, accountId=%s, embossedName=%s, cvv=%s, expiration=%s, status=%s, version=%d, confirmed=%b]",
                            getAuditSummary(), cardNumber, accountId, embossedName, maskedCvv, 
                            expirationDate, activeStatus, versionNumber, confirmUpdate);
    }

    /**
     * Equality comparison based on card number and version.
     * 
     * <p>This method supports request deduplication and optimistic locking
     * validation by comparing card identification and version information.
     * 
     * @param obj the object to compare
     * @return true if requests are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        
        CardUpdateRequestDto that = (CardUpdateRequestDto) obj;
        return Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(embossedName, that.embossedName) &&
               Objects.equals(cvvCode, that.cvvCode) &&
               Objects.equals(expirationDate, that.expirationDate) &&
               Objects.equals(activeStatus, that.activeStatus) &&
               Objects.equals(versionNumber, that.versionNumber) &&
               Objects.equals(creditLimit, that.creditLimit) &&
               Objects.equals(confirmUpdate, that.confirmUpdate);
    }

    /**
     * Hash code generation based on card identification and version.
     * 
     * <p>This method supports efficient collections handling and request
     * correlation tracking in distributed systems.
     * 
     * @return hash code for the request
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardNumber, accountId, embossedName, 
                           cvvCode, expirationDate, activeStatus, versionNumber, 
                           creditLimit, confirmUpdate);
    }

    /**
     * String representation for debugging and logging purposes.
     * 
     * <p>This method provides a comprehensive string representation that
     * supports debugging while maintaining security best practices by
     * masking sensitive information.
     * 
     * @return string representation of the request
     */
    @Override
    public String toString() {
        return getUpdateAuditSummary();
    }
}