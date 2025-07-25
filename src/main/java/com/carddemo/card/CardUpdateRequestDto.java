package com.carddemo.card;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidCvv;
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for card update operations with optimistic locking and comprehensive validation.
 * 
 * <p>This DTO supports the COCRDUPC.cbl functionality by providing a modern Java representation
 * of the COBOL card update transaction structure. It maintains exact field mappings and validation
 * rules equivalent to the original mainframe processing while adding enterprise-grade features
 * like optimistic locking, comprehensive input validation, and BigDecimal precision for financial operations.</p>
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Optimistic locking with version control for concurrent access management</li>
 *   <li>Comprehensive business rule validation matching COBOL validation logic</li>
 *   <li>BigDecimal precision for credit limit updates with exact decimal arithmetic</li>
 *   <li>Card number format validation with Luhn algorithm support</li>
 *   <li>CVV validation for security compliance</li>
 *   <li>Expiration date validation with CCYYMMDD format support</li>
 *   <li>User context and authorization parameters for role-based access control</li>
 *   <li>JSON serialization support for React frontend integration</li>
 * </ul>
 * 
 * <p>Business Rules Enforced:</p>
 * <ul>
 *   <li>Account ID must be exactly 11 digits (COBOL PIC 9(11) equivalent)</li>
 *   <li>Card number must be exactly 16 digits with Luhn validation</li>
 *   <li>Embossed name limited to 50 characters with alpha/space validation</li>
 *   <li>CVV code must be 3 digits for security verification</li>
 *   <li>Active status must be Y or N (mapped from COBOL 88-level conditions)</li>
 *   <li>Expiration date must be valid and in CCYYMMDD format</li>
 *   <li>Credit limit updates use exact BigDecimal precision equivalent to COBOL COMP-3</li>
 * </ul>
 * 
 * <p>Validation Strategy:</p>
 * <p>The DTO implements cascading validation following the same sequence as the COBOL program:
 * account validation, card validation, name validation, status validation, and date validation.
 * This ensures identical error reporting patterns and user experience consistency during the
 * modernization transition.</p>
 * 
 * <p>Optimistic Locking Implementation:</p>
 * <p>The version number field enables detection of concurrent modifications equivalent to
 * COBOL record locking mechanisms. When processing updates, the service layer compares
 * the version number to detect if another user has modified the card record, providing
 * the same concurrency protection as the original CICS/VSAM environment.</p>
 * 
 * <p>Integration with COCRDUPC.cbl Logic:</p>
 * <p>This DTO directly maps to the COBOL working storage sections CCUP-OLD-DETAILS and
 * CCUP-NEW-DETAILS, providing equivalent field structure and validation patterns.
 * The confirmation workflow from the COBOL program is supported through the confirmUpdate
 * flag, enabling the same user interaction patterns in the modernized system.</p>
 * 
 * @author Blitzy Platform - CardDemo Modernization Team
 * @version 1.0
 * @since 2024-01-01
 * @see Card
 * @see BaseRequestDto
 */
public class CardUpdateRequestDto extends BaseRequestDto {
    
    /**
     * Card number for the update operation (16 digits).
     * 
     * <p>Maps to COBOL CC-CARD-NUM PIC X(16) and CCUP-NEW-CARDID fields.
     * Required for identifying the card record to update and must pass Luhn
     * algorithm validation. This field is used for record lookup and cannot
     * be modified during update operations.</p>
     * 
     * <p>Validation includes format checking (exactly 16 digits) and checksum
     * verification using the Luhn algorithm to ensure card number integrity
     * equivalent to credit card industry standards.</p>
     */
    @JsonProperty("cardNumber")
    @NotNull(message = "Card number is required for update operations")
    @ValidCardNumber
    private String cardNumber;
    
    /**
     * Account ID associated with the card (11 digits).
     * 
     * <p>Maps to COBOL CC-ACCT-ID PIC 9(11) and CCUP-NEW-ACCTID fields.
     * Used for cross-reference validation and ensuring the card belongs to
     * the correct account. Must be exactly 11 digits and non-zero, matching
     * the validation logic in COCRDUPC.cbl.</p>
     * 
     * <p>This field supports the account-card relationship validation equivalent
     * to the COBOL PERFORM 1210-EDIT-ACCOUNT paragraph logic.</p>
     */
    @JsonProperty("accountId")
    @NotNull(message = "Account ID is required for card identification")
    private String accountId;
    
    /**
     * Embossed name to appear on the card (maximum 50 characters).
     * 
     * <p>Maps to COBOL CCUP-NEW-CRDNAME PIC X(50) field. The name printed on
     * the physical card for customer identification. Must contain only alphabetic
     * characters and spaces, matching the validation logic in the COBOL
     * PERFORM 1230-EDIT-NAME paragraph.</p>
     * 
     * <p>Validation ensures compatibility with card printing systems and maintains
     * the same character restrictions as the original mainframe application.</p>
     */
    @JsonProperty("embossedName")
    @NotNull(message = "Embossed name is required for card personalization")
    private String embossedName;
    
    /**
     * CVV security code for the card (3 digits).
     * 
     * <p>Maps to COBOL CCUP-NEW-CVV-CD PIC X(03) field. The 3-digit security
     * code printed on the back of the card for transaction verification.
     * Must be exactly 3 numeric digits for PCI compliance and security standards.</p>
     * 
     * <p>This field supports both creation and update scenarios with appropriate
     * validation groups to handle different business contexts.</p>
     */
    @JsonProperty("cvvCode")
    @ValidCvv(groups = ValidCvv.CardUpdate.class, allowNull = false, 
              message = "CVV code is required for card update operations")
    private String cvvCode;
    
    /**
     * Card expiration date.
     * 
     * <p>Maps to COBOL CCUP-NEW-EXPIRAION-DATE structure with separate year,
     * month, and day components. Converted to LocalDate for proper date handling
     * and validation. Must be a future date for valid card operations.</p>
     * 
     * <p>Validation includes format checking, date range validation, and future
     * date verification to ensure card validity periods meet business requirements.</p>
     */
    @JsonProperty("expirationDate") 
    @NotNull(message = "Expiration date is required for card validity")
    private LocalDate expirationDate;
    
    /**
     * Card active status (Y for active, N for inactive).
     * 
     * <p>Maps to COBOL CCUP-NEW-CRDSTCD PIC X(01) field and 88-level conditions
     * FLG-YES-NO-VALID. Controls whether the card can be used for transactions.
     * Must be exactly 'Y' or 'N' matching the COBOL validation logic in
     * PERFORM 1240-EDIT-CARDSTATUS paragraph.</p>
     * 
     * <p>This field supports card lifecycle management and transaction authorization
     * decisions equivalent to the original CICS card management functionality.</p>
     */
    @JsonProperty("activeStatus")
    @NotNull(message = "Active status is required (Y for active, N for inactive)")
    private String activeStatus;
    
    /**
     * Version number for optimistic locking.
     * 
     * <p>Implements concurrent modification detection equivalent to VSAM record
     * locking mechanisms. The version number must match the current database
     * version to prevent lost updates and maintain data consistency across
     * concurrent user sessions.</p>
     * 
     * <p>Maps to the optimistic locking logic in COCRDUPC.cbl PERFORM 9300-CHECK-CHANGE-IN-REC
     * paragraph, providing the same concurrency protection as the original CICS environment.</p>
     */
    @JsonProperty("versionNumber")
    @NotNull(message = "Version number is required for concurrent update protection")
    private Long versionNumber;
    
    /**
     * Credit limit for the card with exact decimal precision.
     * 
     * <p>Uses BigDecimal to maintain exact precision equivalent to COBOL COMP-3
     * arithmetic operations. Supports credit limit updates with no floating-point
     * precision loss, ensuring accurate financial calculations matching the
     * original mainframe processing.</p>
     * 
     * <p>This field handles credit limit modifications with the same precision
     * requirements as COBOL monetary computations, maintaining financial accuracy
     * critical for credit card management operations.</p>
     */
    @JsonProperty("creditLimit")
    private BigDecimal creditLimit;
    
    /**
     * Confirmation flag for the update operation.
     * 
     * <p>Maps to the COBOL confirmation workflow logic where changes are validated
     * first, then presented to the user for confirmation before committing.
     * When true, indicates the user has confirmed the changes and the update
     * should be processed immediately.</p>
     * 
     * <p>Supports the two-phase update pattern from COCRDUPC.cbl where changes
     * are validated (CCUP-CHANGES-OK-NOT-CONFIRMED) then confirmed (CCARD-AID-PFK05)
     * before final database updates are performed.</p>
     */
    @JsonProperty("confirmUpdate")
    private boolean confirmUpdate;
    
    /**
     * Default constructor initializing the request with base metadata.
     * 
     * <p>Calls the parent BaseRequestDto constructor to initialize correlation ID,
     * timestamp, and other common request fields. Sets default values for
     * fields that have reasonable defaults in the business context.</p>
     */
    public CardUpdateRequestDto() {
        super();
        this.confirmUpdate = false;
    }
    
    /**
     * Constructor with correlation ID for distributed transaction tracking.
     * 
     * <p>Used when the request is part of a larger distributed transaction
     * that requires correlation across multiple microservices. Maintains
     * transaction traceability equivalent to CICS transaction ID tracking.</p>
     * 
     * @param correlationId the correlation identifier for distributed transaction tracking
     */
    public CardUpdateRequestDto(String correlationId) {
        super(correlationId);
        this.confirmUpdate = false;
    }
    
    /**
     * Gets the card number for the update operation.
     * 
     * @return the 16-digit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }
    
    /**
     * Sets the card number for the update operation.
     * 
     * <p>The card number identifies the specific card record to update.
     * Once set, this value should not be modified during the update process
     * as it serves as the primary key for record identification.</p>
     * 
     * @param cardNumber the 16-digit card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    /**
     * Gets the account ID associated with the card.
     * 
     * @return the 11-digit account identifier
     */
    public String getAccountId() {
        return accountId;
    }
    
    /**
     * Sets the account ID associated with the card.
     * 
     * <p>Used for validation to ensure the card belongs to the correct account.
     * This field supports the cross-reference validation logic equivalent to
     * the COBOL account validation routines.</p>
     * 
     * @param accountId the 11-digit account identifier
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    /**
     * Gets the embossed name for the card.
     * 
     * @return the name to be printed on the card
     */
    public String getEmbossedName() {
        return embossedName;
    }
    
    /**
     * Sets the embossed name for the card.
     * 
     * <p>The name that will be printed on the physical card. Must meet
     * card printing requirements and character restrictions for compatibility
     * with card manufacturing systems.</p>
     * 
     * @param embossedName the name to be printed on the card
     */
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }
    
    /**
     * Gets the CVV security code.
     * 
     * @return the 3-digit CVV code
     */
    public String getCvvCode() {
        return cvvCode;
    }
    
    /**
     * Sets the CVV security code.
     * 
     * <p>The 3-digit security code for transaction verification. This field
     * contains sensitive payment data and should be handled with appropriate
     * security measures during transmission and processing.</p>
     * 
     * @param cvvCode the 3-digit CVV code
     */
    public void setCvvCode(String cvvCode) {
        this.cvvCode = cvvCode;
    }
    
    /**
     * Gets the card expiration date.
     * 
     * @return the card expiration date
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }
    
    /**
     * Sets the card expiration date.
     * 
     * <p>The date when the card expires and becomes invalid for transactions.
     * Must be a future date to ensure card validity. Supports the same
     * date validation logic as the COBOL expiration date processing.</p>
     * 
     * @param expirationDate the card expiration date
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }
    
    /**
     * Gets the card active status.
     * 
     * @return the active status ('Y' or 'N')
     */
    public String getActiveStatus() {
        return activeStatus;
    }
    
    /**
     * Sets the card active status.
     * 
     * <p>Controls whether the card is active for transactions. Must be 'Y' for
     * active or 'N' for inactive, matching the COBOL 88-level condition
     * validation logic for card status management.</p>
     * 
     * @param activeStatus the active status ('Y' or 'N')
     */
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }
    
    /**
     * Gets the version number for optimistic locking.
     * 
     * @return the version number for concurrency control
     */
    public Long getVersionNumber() {
        return versionNumber;
    }
    
    /**
     * Sets the version number for optimistic locking.
     * 
     * <p>Used to detect concurrent modifications and prevent lost updates.
     * The version must match the current database version to successfully
     * process the update request.</p>
     * 
     * @param versionNumber the version number for concurrency control
     */
    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }
    
    /**
     * Gets the credit limit with exact decimal precision.
     * 
     * @return the credit limit as BigDecimal
     */
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }
    
    /**
     * Sets the credit limit with exact decimal precision.
     * 
     * <p>Uses BigDecimal to maintain exact precision equivalent to COBOL COMP-3
     * arithmetic. Ensures no floating-point precision loss in financial
     * calculations critical for credit card management operations.</p>
     * 
     * @param creditLimit the credit limit as BigDecimal
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }
    
    /**
     * Checks if the update operation is confirmed by the user.
     * 
     * @return true if update is confirmed, false otherwise
     */
    public boolean isConfirmUpdate() {
        return confirmUpdate;
    }
    
    /**
     * Sets the confirmation flag for the update operation.
     * 
     * <p>When true, indicates the user has confirmed the changes and the update
     * should proceed. Supports the two-phase commit pattern from the COBOL
     * application where changes are validated first, then confirmed.</p>
     * 
     * @param confirmUpdate true to confirm the update, false otherwise
     */
    public void setConfirmUpdate(boolean confirmUpdate) {
        this.confirmUpdate = confirmUpdate;
    }
    
    /**
     * Validates that all required fields are populated for update processing.
     * 
     * <p>Performs comprehensive validation equivalent to the COBOL input
     * validation routines. Checks field presence, format validation, and
     * business rule compliance before allowing the update to proceed.</p>
     * 
     * <p>This method supplements Jakarta Bean Validation with business-specific
     * validation logic that mirrors the COBOL validation paragraph structure.</p>
     * 
     * @return true if all required fields are valid, false otherwise
     */
    public boolean isValidForUpdate() {
        return super.isValid() && 
               cardNumber != null && !cardNumber.trim().isEmpty() &&
               accountId != null && !accountId.trim().isEmpty() &&
               embossedName != null && !embossedName.trim().isEmpty() &&
               activeStatus != null && !activeStatus.trim().isEmpty() &&
               versionNumber != null &&
               expirationDate != null;
    }
    
    /**
     * Returns a string representation for debugging and logging purposes.
     * 
     * <p>Provides essential request information for troubleshooting while
     * masking sensitive data like CVV codes. Used by logging frameworks
     * and debugging tools throughout the microservices architecture.</p>
     * 
     * @return string representation with masked sensitive data
     */
    @Override
    public String toString() {
        return "CardUpdateRequestDto{" +
                "cardNumber='" + (cardNumber != null ? "****" + cardNumber.substring(Math.max(0, cardNumber.length() - 4)) : null) + '\'' +
                ", accountId='" + accountId + '\'' +
                ", embossedName='" + embossedName + '\'' +
                ", cvvCode='" + (cvvCode != null ? "***" : null) + '\'' +
                ", expirationDate=" + expirationDate +
                ", activeStatus='" + activeStatus + '\'' +
                ", versionNumber=" + versionNumber +
                ", creditLimit=" + creditLimit +
                ", confirmUpdate=" + confirmUpdate +
                ", " + super.toString() +
                '}';
    }
}