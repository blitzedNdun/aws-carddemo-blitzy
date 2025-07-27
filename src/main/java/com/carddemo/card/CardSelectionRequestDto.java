package com.carddemo.card;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.validator.ValidAccountId;
import com.carddemo.common.validator.ValidCardNumber;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for card selection operations with comprehensive validation and cross-reference support.
 * 
 * This DTO handles card selection requests for the COCRDSLC.cbl equivalent functionality, providing
 * validation for card numbers using the Luhn algorithm, account ID format verification, and role-based
 * access control parameters. The implementation maintains exact functional equivalence to the original
 * COBOL card selection logic while adding enhanced validation and security features.
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>16-digit card number validation with Luhn algorithm checksum verification</li>
 *   <li>11-digit account ID format validation matching COBOL PIC 9(11) requirements</li>
 *   <li>Cross-reference validation support for account-card relationship verification</li>
 *   <li>Role-based access control parameters for card information security</li>
 *   <li>Configurable data masking options for sensitive card information display</li>
 *   <li>Existence validation flags for database lookup optimization</li>
 * </ul>
 * 
 * <p>Validation Rules:</p>
 * The DTO implements comprehensive validation equivalent to COBOL edit paragraphs:
 * <ul>
 *   <li>Card Number: Must be 16 numeric digits, pass Luhn algorithm validation</li>
 *   <li>Account ID: Must be 11 numeric digits, no leading zeros except for account 00000000000</li>
 *   <li>Cross-reference: Optional account-card relationship validation</li>
 *   <li>Role Authorization: User role must be provided for access control decisions</li>
 * </ul>
 * 
 * <p>Usage Example:</p>
 * <pre>
 * CardSelectionRequestDto request = new CardSelectionRequestDto();
 * request.setCardNumber("4532015112830366");
 * request.setAccountId("12345678901");
 * request.setUserRole("CARD_VIEWER");
 * request.setIncludeMaskedData(true);
 * request.setValidateExistence(true);
 * request.setIncludeCrossReference(true);
 * </pre>
 * 
 * <p>Error Handling:</p>
 * Validation errors are returned in a structured format compatible with React frontend
 * components, providing specific field-level error messages and validation failure details
 * that match the original COBOL error message patterns from COCRDSLC.cbl.
 * 
 * <p>Security Considerations:</p>
 * The DTO includes role-based access parameters to ensure card information is only
 * accessible to authorized users with appropriate permissions, replacing RACF-based
 * authorization from the original mainframe system.
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since 2024-01-01
 * @see com.carddemo.common.dto.BaseRequestDto
 * @see com.carddemo.common.validator.ValidCardNumber
 * @see com.carddemo.common.validator.ValidAccountId
 */
@Valid
public class CardSelectionRequestDto extends BaseRequestDto {
    
    /**
     * Credit card number for selection operations.
     * 
     * <p>Must be a 16-digit numeric string that passes Luhn algorithm validation.
     * This field corresponds to CC-CARD-NUM PIC X(16) from CVCRD01Y.cpy and is
     * validated using the same business rules as the COBOL program COCRDSLC.cbl.</p>
     * 
     * <p>Validation includes:</p>
     * <ul>
     *   <li>Exactly 16 numeric digits</li>
     *   <li>Luhn algorithm checksum verification</li>
     *   <li>No special characters or spaces allowed</li>
     *   <li>Cannot be null or empty for card selection operations</li>
     * </ul>
     * 
     * <p>Example valid values: "4532015112830366", "5555555555554444"</p>
     */
    @JsonProperty("cardNumber")
    @NotNull(message = "Card number is required for card selection")
    @ValidCardNumber(message = "Card number must be 16 digits and pass Luhn algorithm validation")
    private String cardNumber;
    
    /**
     * Account identifier for cross-reference validation.
     * 
     * <p>Must be an 11-digit numeric string corresponding to CC-ACCT-ID PIC X(11)
     * from CVCRD01Y.cpy. Used for account-card relationship validation and access
     * control verification in the card selection process.</p>
     * 
     * <p>Validation includes:</p>
     * <ul>
     *   <li>Exactly 11 numeric digits</li>
     *   <li>Cannot contain leading/trailing spaces</li>
     *   <li>Must be a valid account ID in the system</li>
     *   <li>Cannot be null or empty when cross-reference validation is enabled</li>
     * </ul>
     * 
     * <p>Example valid values: "12345678901", "98765432109"</p>
     */
    @JsonProperty("accountId")
    @NotNull(message = "Account ID is required for card selection")
    @ValidAccountId(message = "Account ID must be 11 digits",
                   emptyMessage = "Account ID cannot be empty for card selection",
                   formatMessage = "Account ID must be exactly 11 numeric digits")
    private String accountId;
    
    /**
     * User role for role-based access control.
     * 
     * <p>Specifies the authenticated user's role for determining card information
     * access permissions. Used by authorization services to control what card
     * data can be retrieved and displayed, replacing RACF role-based security
     * from the original mainframe system.</p>
     * 
     * <p>Valid roles include:</p>
     * <ul>
     *   <li>CARD_VIEWER - Read-only access to card information</li>
     *   <li>CARD_ADMIN - Full access to card information and operations</li>
     *   <li>ACCOUNT_MANAGER - Account-level card access</li>
     *   <li>SYSTEM_ADMIN - System-level access to all card data</li>
     * </ul>
     * 
     * <p>This field determines data masking levels and operational permissions.</p>
     */
    @JsonProperty("userRole")
    @NotNull(message = "User role is required for authorization")
    private String userRole;
    
    /**
     * Flag to control inclusion of masked sensitive data in the response.
     * 
     * <p>When set to true, sensitive card information (such as partial card numbers
     * or expiration dates) will be included in masked format in the response.
     * When false, only non-sensitive card metadata will be returned.</p>
     * 
     * <p>Masking behavior:</p>
     * <ul>
     *   <li>true: Include masked card number (e.g., "****-****-****-1234")</li>
     *   <li>false: Exclude sensitive card data from response</li>
     * </ul>
     * 
     * <p>Default value is false for enhanced security.</p>
     */
    @JsonProperty("includeMaskedData")
    private Boolean includeMaskedData = false;
    
    /**
     * Flag to enable existence validation during card selection.
     * 
     * <p>When set to true, the system will verify that the specified card number
     * exists in the database and is associated with the provided account ID.
     * When false, only format validation will be performed.</p>
     * 
     * <p>Validation behavior:</p>
     * <ul>
     *   <li>true: Perform database lookup to verify card existence</li>
     *   <li>false: Only validate format without database verification</li>
     * </ul>
     * 
     * <p>Enables optimization of validation processing based on use case requirements.</p>
     */
    @JsonProperty("validateExistence")
    private Boolean validateExistence = true;
    
    /**
     * Flag to include cross-reference validation for account-card relationships.
     * 
     * <p>When set to true, the system will validate that the specified card number
     * is properly associated with the provided account ID in the cross-reference
     * tables. This ensures data integrity and prevents unauthorized card access.</p>
     * 
     * <p>Cross-reference validation includes:</p>
     * <ul>
     *   <li>Account-card relationship verification</li>
     *   <li>Card status validation for the account</li>
     *   <li>Authorization level verification</li>
     * </ul>
     * 
     * <p>Corresponds to VSAM cross-reference index validation from the original system.</p>
     */
    @JsonProperty("includeCrossReference")
    private Boolean includeCrossReference = true;
    
    /**
     * Default constructor initializing request with security defaults.
     * 
     * <p>Sets default values that prioritize security and data integrity:
     * - includeMaskedData: false (no sensitive data by default)
     * - validateExistence: true (always verify card exists)
     * - includeCrossReference: true (always validate relationships)
     * </p>
     */
    public CardSelectionRequestDto() {
        super();
        this.includeMaskedData = false;
        this.validateExistence = true;
        this.includeCrossReference = true;
    }
    
    /**
     * Constructor with correlation ID for distributed transaction tracking.
     * 
     * <p>Used when the card selection request is part of a larger distributed
     * transaction that requires correlation across multiple microservices.</p>
     * 
     * @param correlationId the correlation identifier for distributed tracking
     */
    public CardSelectionRequestDto(String correlationId) {
        super(correlationId);
        this.includeMaskedData = false;
        this.validateExistence = true;
        this.includeCrossReference = true;
    }
    
    /**
     * Gets the credit card number for selection operations.
     * 
     * @return the 16-digit credit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }
    
    /**
     * Sets the credit card number for selection operations.
     * 
     * <p>The card number will be validated against format requirements and
     * Luhn algorithm checksum during request processing.</p>
     * 
     * @param cardNumber the 16-digit credit card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    /**
     * Gets the account identifier for cross-reference validation.
     * 
     * @return the 11-digit account identifier
     */
    public String getAccountId() {
        return accountId;
    }
    
    /**
     * Sets the account identifier for cross-reference validation.
     * 
     * <p>The account ID will be validated against format requirements and
     * optionally verified for existence in the database.</p>
     * 
     * @param accountId the 11-digit account identifier
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    /**
     * Gets the user role for authorization decisions.
     * 
     * @return the user role for role-based access control
     */
    public String getUserRole() {
        return userRole;
    }
    
    /**
     * Sets the user role for authorization decisions.
     * 
     * <p>The user role determines what card information can be accessed
     * and what operations can be performed on the selected card.</p>
     * 
     * @param userRole the user role for role-based access control
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }
    
    /**
     * Gets the flag controlling inclusion of masked sensitive data.
     * 
     * @return true if masked data should be included, false otherwise
     */
    public Boolean getIncludeMaskedData() {
        return includeMaskedData;
    }
    
    /**
     * Sets the flag controlling inclusion of masked sensitive data.
     * 
     * <p>Controls whether sensitive card information should be included
     * in the response in masked format for display purposes.</p>
     * 
     * @param includeMaskedData true to include masked data, false to exclude
     */
    public void setIncludeMaskedData(Boolean includeMaskedData) {
        this.includeMaskedData = includeMaskedData;
    }
    
    /**
     * Gets the flag controlling existence validation.
     * 
     * @return true if existence validation is enabled, false otherwise
     */
    public Boolean getValidateExistence() {
        return validateExistence;
    }
    
    /**
     * Sets the flag controlling existence validation.
     * 
     * <p>When enabled, the system will verify that the card exists in the
     * database before processing the selection request.</p>
     * 
     * @param validateExistence true to enable existence validation, false to disable
     */
    public void setValidateExistence(Boolean validateExistence) {
        this.validateExistence = validateExistence;
    }
    
    /**
     * Gets the flag controlling cross-reference validation.
     * 
     * @return true if cross-reference validation is enabled, false otherwise
     */
    public Boolean getIncludeCrossReference() {
        return includeCrossReference;
    }
    
    /**
     * Sets the flag controlling cross-reference validation.
     * 
     * <p>When enabled, the system will validate the account-card relationship
     * to ensure data integrity and proper authorization.</p>
     * 
     * @param includeCrossReference true to enable cross-reference validation, false to disable
     */
    public void setIncludeCrossReference(Boolean includeCrossReference) {
        this.includeCrossReference = includeCrossReference;
    }
    
    /**
     * Returns a string representation of the CardSelectionRequestDto for debugging and logging.
     * 
     * <p>Provides essential request information while masking sensitive card data
     * for security purposes. Used by logging frameworks and debugging tools
     * throughout the card selection process.</p>
     * 
     * @return string representation with masked sensitive information
     */
    @Override
    public String toString() {
        return "CardSelectionRequestDto{" +
                "cardNumber='" + maskCardNumber(cardNumber) + '\'' +
                ", accountId='" + accountId + '\'' +
                ", userRole='" + userRole + '\'' +
                ", includeMaskedData=" + includeMaskedData +
                ", validateExistence=" + validateExistence +
                ", includeCrossReference=" + includeCrossReference +
                ", " + super.toString() +
                '}';
    }
    
    /**
     * Masks card number for secure logging and debugging output.
     * 
     * <p>Replaces all but the last 4 digits with asterisks to prevent
     * sensitive card information from appearing in logs while maintaining
     * enough information for debugging and transaction tracking.</p>
     * 
     * @param cardNumber the card number to mask
     * @return masked card number or null if input is null
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }
}