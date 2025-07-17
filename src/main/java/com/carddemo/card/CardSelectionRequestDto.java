package com.carddemo.card;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidAccountId;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.util.Objects;

/**
 * Request DTO for card selection operations with comprehensive validation.
 * 
 * <p>This DTO supports the COCRDSLC.cbl card selection functionality through REST API
 * endpoints, providing robust card number validation using the Luhn algorithm,
 * cross-reference verification with account data, and role-based access parameters.
 * 
 * <p>Key Features:
 * <ul>
 *   <li>Luhn algorithm validation for 16-digit credit card numbers</li>
 *   <li>Account ID format validation and existence verification</li>
 *   <li>Role-based access control for card information filtering</li>
 *   <li>Cross-reference validation between cards and accounts</li>
 *   <li>Comprehensive input validation with detailed error messages</li>
 *   <li>Jackson JSON serialization support for React frontend integration</li>
 * </ul>
 * 
 * <p>Validation Rules:
 * <ul>
 *   <li>Card number must be exactly 16 digits and pass Luhn checksum</li>
 *   <li>Account ID must be exactly 11 digits and exist in the database</li>
 *   <li>User role must be valid for accessing requested card information</li>
 *   <li>Cross-reference validation ensures card-account relationship integrity</li>
 * </ul>
 * 
 * <p>Usage Example:
 * <pre>
 * CardSelectionRequestDto request = new CardSelectionRequestDto();
 * request.setCardNumber("4111111111111111");
 * request.setAccountId("12345678901");
 * request.setUserRole("USER");
 * request.setValidateExistence(true);
 * request.setIncludeCrossReference(true);
 * </pre>
 * 
 * <p>Error Handling:
 * The validation annotations provide detailed error messages that can be processed
 * by the React frontend components for user-friendly error display, maintaining
 * consistency with the original COBOL validation patterns from COCRDSLC.cbl.
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public class CardSelectionRequestDto extends BaseRequestDto {

    /**
     * Credit card number for selection and validation.
     * 
     * <p>This field represents the 16-digit credit card number that must pass
     * comprehensive validation including format checking and Luhn algorithm
     * verification. The validation ensures compliance with industry standards
     * and matches the COBOL validation logic from COCRDSLC.cbl.
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Must be exactly 16 digits</li>
     *   <li>Must contain only numeric characters</li>
     *   <li>Must pass Luhn algorithm checksum verification</li>
     *   <li>Must exist in the card database when validateExistence is true</li>
     * </ul>
     * 
     * <p>Example valid card numbers:
     * <ul>
     *   <li>4111111111111111 (Visa test card)</li>
     *   <li>5555555555554444 (Mastercard test card)</li>
     *   <li>378282246310005 (American Express test card)</li>
     * </ul>
     * 
     * @see ValidCardNumber
     */
    @JsonProperty("cardNumber")
    @ValidCardNumber(
        message = "Card number must be a valid 16-digit credit card number",
        enableLuhnValidation = true,
        allowNullOrEmpty = false
    )
    private String cardNumber;

    /**
     * Account identifier for cross-reference validation.
     * 
     * <p>This field represents the 11-digit account identifier that must be
     * validated for format and existence. The account ID is used for
     * cross-reference validation to ensure the card belongs to the specified
     * account, maintaining data integrity as required by the original COBOL
     * program COCRDSLC.cbl.
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Must be exactly 11 digits</li>
     *   <li>Must contain only numeric characters</li>
     *   <li>Must exist in the account database when validateExistence is true</li>
     *   <li>Must have valid relationship with the specified card number</li>
     * </ul>
     * 
     * <p>Format: 11-digit numeric string (e.g., "12345678901")
     * 
     * @see ValidAccountId
     */
    @JsonProperty("accountId")
    @ValidAccountId(
        message = "Account ID must be exactly 11 digits",
        strict = false,
        context = ValidAccountId.ValidationContext.GENERAL
    )
    private String accountId;

    /**
     * User role for authorization and access control.
     * 
     * <p>This field specifies the user's role for determining access permissions
     * to card information. Different roles may have different levels of access
     * to sensitive card data, supporting the role-based access control patterns
     * established in the system.
     * 
     * <p>Supported Values:
     * <ul>
     *   <li>"USER" - Regular user with standard card access</li>
     *   <li>"ADMIN" - Administrative user with full card access</li>
     *   <li>"ROLE_USER" - Spring Security compatible user role</li>
     *   <li>"ROLE_ADMIN" - Spring Security compatible admin role</li>
     * </ul>
     * 
     * <p>Access Control:
     * <ul>
     *   <li>USER role: Can access own cards and basic information</li>
     *   <li>ADMIN role: Can access all cards and sensitive information</li>
     *   <li>Role validation ensures user has appropriate permissions</li>
     * </ul>
     */
    @JsonProperty("userRole")
    private String userRole;

    /**
     * Flag to include masked sensitive data in response.
     * 
     * <p>This flag determines whether sensitive card data should be included
     * in the response with appropriate masking. When true, sensitive fields
     * like card numbers may be partially masked (e.g., "**** **** **** 1111")
     * to balance security with usability requirements.
     * 
     * <p>Behavior:
     * <ul>
     *   <li>true: Include masked sensitive data in response</li>
     *   <li>false: Exclude sensitive data from response</li>
     *   <li>null: Use default behavior based on user role</li>
     * </ul>
     * 
     * <p>Security Considerations:
     * Masking behavior is role-dependent, with admin users potentially
     * having access to less masked data than regular users.
     */
    @JsonProperty("includeMaskedData")
    private Boolean includeMaskedData;

    /**
     * Flag to validate card and account existence in database.
     * 
     * <p>This flag determines whether the validation should include database
     * existence checks for the card number and account ID. When true, the
     * system will verify that both the card and account exist in the database
     * and have a valid relationship.
     * 
     * <p>Validation Behavior:
     * <ul>
     *   <li>true: Perform database existence validation</li>
     *   <li>false: Perform only format validation</li>
     *   <li>null: Use default behavior (format validation only)</li>
     * </ul>
     * 
     * <p>Performance Impact:
     * Enabling existence validation adds database queries to the validation
     * process, which may impact response times but ensures data integrity.
     */
    @JsonProperty("validateExistence")
    private Boolean validateExistence;

    /**
     * Flag to include cross-reference validation and data.
     * 
     * <p>This flag determines whether the validation should include cross-reference
     * checks between the card and account, ensuring they have a valid relationship.
     * When true, the system will verify the card-account relationship and include
     * relevant cross-reference data in the response.
     * 
     * <p>Cross-Reference Validation:
     * <ul>
     *   <li>true: Validate card-account relationship</li>
     *   <li>false: Skip cross-reference validation</li>
     *   <li>null: Use default behavior (skip validation)</li>
     * </ul>
     * 
     * <p>Data Inclusion:
     * When cross-reference validation is enabled, the response may include
     * additional data about the relationship between the card and account,
     * such as linking timestamps and relationship status.
     */
    @JsonProperty("includeCrossReference")
    private Boolean includeCrossReference;

    /**
     * Default constructor for JSON deserialization.
     * 
     * <p>Initializes the DTO with default values suitable for JSON deserialization.
     * The base class constructor sets up request tracking and audit fields.
     */
    public CardSelectionRequestDto() {
        super();
        this.includeMaskedData = false;
        this.validateExistence = true;
        this.includeCrossReference = false;
    }

    /**
     * Constructor with required fields for programmatic instantiation.
     * 
     * <p>This constructor provides a convenient way to create a card selection
     * request with the essential fields needed for validation and processing.
     * 
     * @param correlationId Unique correlation identifier for request tracking
     * @param userId User identifier for audit trail and authorization
     * @param sessionId Session identifier for distributed session management
     * @param cardNumber Credit card number for selection and validation
     * @param accountId Account identifier for cross-reference validation
     */
    public CardSelectionRequestDto(String correlationId, String userId, String sessionId, 
                                  String cardNumber, String accountId) {
        super(correlationId, userId, sessionId);
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        this.includeMaskedData = false;
        this.validateExistence = true;
        this.includeCrossReference = false;
    }

    /**
     * Gets the credit card number for selection and validation.
     * 
     * @return the credit card number as a 16-digit string
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the credit card number for selection and validation.
     * 
     * <p>The card number must be exactly 16 digits and pass Luhn algorithm
     * validation. Input validation is performed through the @ValidCardNumber
     * annotation to ensure compliance with industry standards.
     * 
     * @param cardNumber the credit card number as a 16-digit string
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the account identifier for cross-reference validation.
     * 
     * @return the account identifier as an 11-digit string
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier for cross-reference validation.
     * 
     * <p>The account ID must be exactly 11 digits and exist in the database
     * when existence validation is enabled. Format validation is performed
     * through the @ValidAccountId annotation.
     * 
     * @param accountId the account identifier as an 11-digit string
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the user role for authorization and access control.
     * 
     * @return the user role string (USER, ADMIN, ROLE_USER, ROLE_ADMIN)
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Sets the user role for authorization and access control.
     * 
     * <p>The user role determines the level of access to card information
     * and sensitive data. Valid values include USER, ADMIN, ROLE_USER, 
     * and ROLE_ADMIN for Spring Security compatibility.
     * 
     * @param userRole the user role string
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * Gets the flag to include masked sensitive data in response.
     * 
     * @return true if masked data should be included, false otherwise
     */
    public Boolean getIncludeMaskedData() {
        return includeMaskedData;
    }

    /**
     * Sets the flag to include masked sensitive data in response.
     * 
     * <p>When set to true, sensitive card data will be included in the response
     * with appropriate masking based on the user's role and permissions.
     * 
     * @param includeMaskedData true to include masked data, false to exclude
     */
    public void setIncludeMaskedData(Boolean includeMaskedData) {
        this.includeMaskedData = includeMaskedData;
    }

    /**
     * Gets the flag to validate card and account existence in database.
     * 
     * @return true if existence validation should be performed, false otherwise
     */
    public Boolean getValidateExistence() {
        return validateExistence;
    }

    /**
     * Sets the flag to validate card and account existence in database.
     * 
     * <p>When set to true, the validation process will include database queries
     * to verify that the card and account exist and have appropriate status.
     * 
     * @param validateExistence true to validate existence, false for format only
     */
    public void setValidateExistence(Boolean validateExistence) {
        this.validateExistence = validateExistence;
    }

    /**
     * Gets the flag to include cross-reference validation and data.
     * 
     * @return true if cross-reference validation should be performed, false otherwise
     */
    public Boolean getIncludeCrossReference() {
        return includeCrossReference;
    }

    /**
     * Sets the flag to include cross-reference validation and data.
     * 
     * <p>When set to true, the validation process will verify the relationship
     * between the card and account, ensuring they are properly linked.
     * 
     * @param includeCrossReference true to include cross-reference validation, false to skip
     */
    public void setIncludeCrossReference(Boolean includeCrossReference) {
        this.includeCrossReference = includeCrossReference;
    }

    /**
     * Validates the card selection request for business logic compliance.
     * 
     * <p>This method performs comprehensive validation of the card selection
     * request, ensuring all required fields are present and valid, and that
     * the combination of parameters makes business sense.
     * 
     * <p>Validation Checks:
     * <ul>
     *   <li>Base request context validation (correlation ID, user ID, session ID)</li>
     *   <li>Card number and account ID presence and format validation</li>
     *   <li>User role validation for access control</li>
     *   <li>Parameter combination validation for business logic</li>
     * </ul>
     * 
     * @return true if the request is valid for processing, false otherwise
     */
    public boolean isValidCardSelectionRequest() {
        return isValidRequestContext() &&
               cardNumber != null && !cardNumber.trim().isEmpty() &&
               accountId != null && !accountId.trim().isEmpty() &&
               userRole != null && !userRole.trim().isEmpty() &&
               cardNumber.matches("^\\d{16}$") &&
               accountId.matches("^\\d{11}$") &&
               userRole.matches("^(USER|ADMIN|ROLE_USER|ROLE_ADMIN)$");
    }

    /**
     * Creates a sanitized summary of the card selection request for logging.
     * 
     * <p>This method generates a structured summary that includes non-sensitive
     * information suitable for logging and debugging purposes. Sensitive data
     * like card numbers are masked to protect customer privacy.
     * 
     * <p>Masking Policy:
     * <ul>
     *   <li>Card numbers are masked showing only last 4 digits</li>
     *   <li>Account IDs are masked showing only last 4 digits</li>
     *   <li>User roles and flags are included unmasked</li>
     * </ul>
     * 
     * @return sanitized summary string suitable for logging
     */
    public String getSanitizedCardSelectionSummary() {
        String maskedCardNumber = cardNumber != null ? 
            "**** **** **** " + cardNumber.substring(Math.max(0, cardNumber.length() - 4)) : null;
        String maskedAccountId = accountId != null ? 
            "*******" + accountId.substring(Math.max(0, accountId.length() - 4)) : null;
        
        return String.format("CardSelectionRequest[%s, cardNumber=%s, accountId=%s, userRole=%s, " +
                           "includeMaskedData=%s, validateExistence=%s, includeCrossReference=%s]",
                           getSanitizedSummary(), maskedCardNumber, maskedAccountId, userRole,
                           includeMaskedData, validateExistence, includeCrossReference);
    }

    /**
     * Equality comparison based on card number, account ID, and request context.
     * 
     * <p>This method supports request deduplication and correlation tracking
     * across distributed microservices by comparing the essential identifying
     * fields of the card selection request.
     * 
     * @param obj the object to compare
     * @return true if requests are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        
        CardSelectionRequestDto that = (CardSelectionRequestDto) obj;
        return Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(userRole, that.userRole) &&
               Objects.equals(includeMaskedData, that.includeMaskedData) &&
               Objects.equals(validateExistence, that.validateExistence) &&
               Objects.equals(includeCrossReference, that.includeCrossReference);
    }

    /**
     * Hash code generation based on card number, account ID, and request context.
     * 
     * <p>This method supports efficient collections handling and request
     * correlation tracking in distributed systems by generating consistent
     * hash codes for equivalent requests.
     * 
     * @return hash code for the card selection request
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardNumber, accountId, userRole, 
                           includeMaskedData, validateExistence, includeCrossReference);
    }

    /**
     * String representation for debugging and logging purposes.
     * 
     * <p>This method provides a comprehensive string representation that
     * supports debugging while maintaining security best practices by
     * using the sanitized summary method to mask sensitive data.
     * 
     * @return string representation of the card selection request
     */
    @Override
    public String toString() {
        return getSanitizedCardSelectionSummary();
    }
}