package com.carddemo.card;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidAccountId;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

/**
 * Request DTO for card selection operations with comprehensive validation and cross-reference support.
 * 
 * <p>This DTO encapsulates all parameters required for card selection operations in the CardDemo
 * microservices architecture, providing equivalent functionality to the COBOL COCRDSLC program.
 * It supports both single card lookup and account-based card filtering with role-based access
 * control and comprehensive validation including Luhn algorithm verification.</p>
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Credit card number validation with Luhn algorithm checksum verification</li>
 *   <li>Account ID format validation with 11-digit numeric pattern enforcement</li>
 *   <li>Role-based access control parameters for sensitive card data</li>
 *   <li>Cross-reference validation for account-card relationship verification</li>
 *   <li>Flexible data masking options based on user permissions</li>
 *   <li>Comprehensive error handling with structured validation messages</li>
 * </ul>
 * 
 * <p>Business Logic Integration:</p>
 * <p>This DTO directly maps to the COBOL card selection logic in COCRDSLC.cbl, supporting:</p>
 * <ul>
 *   <li>Account ID validation equivalent to CC-ACCT-ID processing (lines 647-683)</li>
 *   <li>Card number validation equivalent to CC-CARD-NUM processing (lines 685-724)</li>
 *   <li>Cross-field validation for account-card combinations (lines 636-641)</li>
 *   <li>Role-based data access control for sensitive card information</li>
 * </ul>
 * 
 * <p>REST API Integration:</p>
 * <p>Designed for seamless integration with Spring Boot REST controllers:</p>
 * <ul>
 *   <li>Jackson JSON serialization support with proper field naming</li>
 *   <li>Jakarta Bean Validation annotations for request validation</li>
 *   <li>Base request DTO inheritance for correlation and audit tracking</li>
 *   <li>Comprehensive error message generation for React frontend</li>
 * </ul>
 * 
 * <p>Example Usage:</p>
 * <pre>
 * {@code
 * // Single card lookup by card number
 * CardSelectionRequestDto request = new CardSelectionRequestDto();
 * request.setCardNumber("4111111111111111");
 * request.setUserRole("CUSTOMER");
 * request.setIncludeMaskedData(true);
 * 
 * // Account-based card lookup with cross-reference
 * CardSelectionRequestDto accountRequest = new CardSelectionRequestDto();
 * accountRequest.setAccountId("12345678901");
 * accountRequest.setIncludeCrossReference(true);
 * accountRequest.setValidateExistence(true);
 * }
 * </pre>
 * 
 * <p>Security Considerations:</p>
 * <ul>
 *   <li>Card numbers are validated but not logged in plain text</li>
 *   <li>Role-based access control prevents unauthorized data access</li>
 *   <li>Cross-reference validation ensures data integrity</li>
 *   <li>Comprehensive audit trail through base request DTO</li>
 * </ul>
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since 1.0
 * @see com.carddemo.common.dto.BaseRequestDto
 * @see com.carddemo.common.validator.ValidCardNumber
 * @see com.carddemo.common.validator.ValidAccountId
 */
public class CardSelectionRequestDto extends BaseRequestDto {

    private static final long serialVersionUID = 1L;

    /**
     * Credit card number for card selection operations.
     * 
     * <p>This field supports both exact card lookups and card filtering operations.
     * The card number undergoes comprehensive validation including:</p>
     * <ul>
     *   <li>16-digit format validation matching COBOL PIC X(16) definition</li>
     *   <li>Luhn algorithm checksum verification for card number integrity</li>
     *   <li>Numeric-only content validation with formatting character removal</li>
     *   <li>Industry-standard card number validation equivalent to COBOL logic</li>
     * </ul>
     * 
     * <p>Validation Logic:</p>
     * <p>Replicates COBOL validation from COCRDSLC.cbl lines 685-724:</p>
     * <ul>
     *   <li>Numeric validation: "CARD ID FILTER,IF SUPPLIED MUST BE A 16 DIGIT NUMBER"</li>
     *   <li>Format checking: Ensures exactly 16 digits after formatting removal</li>
     *   <li>Luhn algorithm: Industry-standard checksum validation</li>
     *   <li>Error handling: Structured error messages for React frontend</li>
     * </ul>
     * 
     * <p>Security Features:</p>
     * <ul>
     *   <li>Card number is never logged in plain text for PCI compliance</li>
     *   <li>Validation errors don't expose partial card number information</li>
     *   <li>Role-based access control determines data visibility</li>
     * </ul>
     */
    @JsonProperty("card_number")
    @ValidCardNumber(
        message = "Card number must be 16 digits and pass Luhn algorithm validation",
        expectedLength = 16,
        enableLuhnCheck = true,
        allowNull = true,
        ignoreFormatting = true
    )
    private String cardNumber;

    /**
     * Account identifier for account-based card selection operations.
     * 
     * <p>This field enables card lookup based on account ownership and supports
     * cross-reference validation between accounts and cards. The account ID
     * validation ensures compatibility with the COBOL account structure:</p>
     * <ul>
     *   <li>11-digit numeric format matching COBOL PIC 9(11) definition</li>
     *   <li>Leading zero support for VSAM KSDS key compatibility</li>
     *   <li>Format validation with comprehensive error messages</li>
     *   <li>Optional existence validation for database integrity</li>
     * </ul>
     * 
     * <p>Validation Logic:</p>
     * <p>Replicates COBOL validation from COCRDSLC.cbl lines 647-683:</p>
     * <ul>
     *   <li>Numeric validation: "ACCOUNT FILTER,IF SUPPLIED MUST BE A 11 DIGIT NUMBER"</li>
     *   <li>Format checking: Ensures exactly 11 digits with leading zero support</li>
     *   <li>Empty value handling: Supports optional account filtering</li>
     *   <li>Cross-field validation: Validates account-card relationships</li>
     * </ul>
     * 
     * <p>Cross-Reference Features:</p>
     * <ul>
     *   <li>Account-card relationship validation through database lookup</li>
     *   <li>Support for account-based card filtering operations</li>
     *   <li>VSAM KSDS key structure compatibility</li>
     *   <li>Efficient database query optimization</li>
     * </ul>
     */
    @JsonProperty("account_id")
    @ValidAccountId(
        message = "Account ID must be exactly 11 digits",
        allowLeadingZeros = true,
        strictCobolMode = true,
        checkExistence = false
    )
    private String accountId;

    /**
     * User role for role-based access control in card selection operations.
     * 
     * <p>This field determines the level of card information that can be accessed
     * and returned in the response. Different user roles have different permissions
     * for accessing sensitive card data:</p>
     * <ul>
     *   <li>CUSTOMER: Access to own cards only with masked sensitive data</li>
     *   <li>ADMIN: Full access to all card information including sensitive data</li>
     *   <li>OPERATOR: Limited access to card information for support operations</li>
     *   <li>GUEST: Read-only access to public card information only</li>
     * </ul>
     * 
     * <p>Security Integration:</p>
     * <ul>
     *   <li>Integrates with Spring Security for role verification</li>
     *   <li>Supports JWT token role extraction and validation</li>
     *   <li>Enables fine-grained access control for card operations</li>
     *   <li>Audit trail integration for compliance requirements</li>
     * </ul>
     * 
     * <p>Business Logic Impact:</p>
     * <p>The user role affects:</p>
     * <ul>
     *   <li>Data masking levels for sensitive card information</li>
     *   <li>Available card selection operations and filters</li>
     *   <li>Cross-reference validation requirements</li>
     *   <li>Audit logging detail levels</li>
     * </ul>
     */
    @JsonProperty("user_role")
    @NotNull(message = "User role is required for card selection operations")
    private String userRole;

    /**
     * Flag indicating whether to include masked card data in the response.
     * 
     * <p>This field controls data masking behavior for sensitive card information
     * based on user permissions and business requirements:</p>
     * <ul>
     *   <li>true: Include masked card data (e.g., "4111-****-****-1111")</li>
     *   <li>false: Exclude sensitive card data from response</li>
     * </ul>
     * 
     * <p>Masking Logic:</p>
     * <ul>
     *   <li>Card numbers: Show first 4 and last 4 digits only</li>
     *   <li>CVV codes: Completely masked or excluded</li>
     *   <li>Expiration dates: Masked based on user role</li>
     *   <li>Embossed names: Partial masking for privacy</li>
     * </ul>
     * 
     * <p>Role-Based Behavior:</p>
     * <ul>
     *   <li>CUSTOMER: Always masked for own cards</li>
     *   <li>ADMIN: Configurable masking level</li>
     *   <li>OPERATOR: Masked for security</li>
     *   <li>GUEST: No sensitive data included</li>
     * </ul>
     */
    @JsonProperty("include_masked_data")
    private Boolean includeMaskedData = false;

    /**
     * Flag indicating whether to validate card/account existence in the database.
     * 
     * <p>This field controls whether the selection operation should perform
     * database lookups to verify the existence of the specified card or account:</p>
     * <ul>
     *   <li>true: Perform database existence validation</li>
     *   <li>false: Skip existence validation for performance</li>
     * </ul>
     * 
     * <p>Validation Process:</p>
     * <p>When enabled, the system will:</p>
     * <ul>
     *   <li>Verify card number exists in the CARDDAT table</li>
     *   <li>Validate account ID exists in the ACCTDAT table</li>
     *   <li>Check account-card relationship integrity</li>
     *   <li>Perform authorization checks for data access</li>
     * </ul>
     * 
     * <p>Performance Considerations:</p>
     * <ul>
     *   <li>Database lookups may impact response time</li>
     *   <li>Caching strategies applied for frequent validations</li>
     *   <li>Connection pooling optimization for high throughput</li>
     *   <li>Timeout handling for database availability issues</li>
     * </ul>
     */
    @JsonProperty("validate_existence")
    private Boolean validateExistence = true;

    /**
     * Flag indicating whether to include cross-reference data in the response.
     * 
     * <p>This field controls whether the selection operation should include
     * related account and customer information in the response:</p>
     * <ul>
     *   <li>true: Include cross-reference data from related tables</li>
     *   <li>false: Return card information only</li>
     * </ul>
     * 
     * <p>Cross-Reference Data:</p>
     * <p>When enabled, the response may include:</p>
     * <ul>
     *   <li>Account information: Balance, status, limits</li>
     *   <li>Customer information: Name, contact details</li>
     *   <li>Transaction history: Recent activity summary</li>
     *   <li>Related cards: Other cards on the same account</li>
     * </ul>
     * 
     * <p>Database Integration:</p>
     * <ul>
     *   <li>Efficient JOIN operations for related data retrieval</li>
     *   <li>Lazy loading for performance optimization</li>
     *   <li>Role-based filtering of cross-reference data</li>
     *   <li>Caching of frequently accessed cross-reference information</li>
     * </ul>
     */
    @JsonProperty("include_cross_reference")
    private Boolean includeCrossReference = false;

    /**
     * Default constructor for CardSelectionRequestDto.
     * 
     * <p>Initializes the request DTO with default values and calls the parent
     * constructor to set up correlation tracking and audit information.</p>
     */
    public CardSelectionRequestDto() {
        super();
        this.includeMaskedData = false;
        this.validateExistence = true;
        this.includeCrossReference = false;
    }

    /**
     * Constructor with correlation ID for distributed tracing.
     * 
     * <p>Used when the request is part of a distributed transaction chain
     * and correlation context must be preserved across service boundaries.</p>
     * 
     * @param correlationId Unique identifier for request correlation
     */
    public CardSelectionRequestDto(String correlationId) {
        super(correlationId);
        this.includeMaskedData = false;
        this.validateExistence = true;
        this.includeCrossReference = false;
    }

    /**
     * Full constructor with complete request context.
     * 
     * <p>Used for service-to-service communications where complete user context
     * and session information must be propagated along with card selection parameters.</p>
     * 
     * @param correlationId Unique identifier for request correlation
     * @param userId Authenticated user identifier from JWT token
     * @param sessionId Redis session store identifier
     */
    public CardSelectionRequestDto(String correlationId, String userId, String sessionId) {
        super(correlationId, userId, sessionId);
        this.includeMaskedData = false;
        this.validateExistence = true;
        this.includeCrossReference = false;
    }

    /**
     * Retrieves the credit card number for card selection operations.
     * 
     * <p>Returns the card number that will be used for card lookup operations.
     * This value has been validated using the Luhn algorithm and format checking.</p>
     * 
     * @return the credit card number, or null if not specified
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the credit card number for card selection operations.
     * 
     * <p>The card number will be validated using the @ValidCardNumber annotation
     * which includes Luhn algorithm verification and format checking.</p>
     * 
     * @param cardNumber the credit card number to set
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Retrieves the account identifier for account-based card selection.
     * 
     * <p>Returns the account ID that will be used for account-based card filtering
     * and cross-reference operations. This value has been validated for format
     * and optionally for existence in the database.</p>
     * 
     * @return the account identifier, or null if not specified
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier for account-based card selection.
     * 
     * <p>The account ID will be validated using the @ValidAccountId annotation
     * which includes format checking and optional existence verification.</p>
     * 
     * @param accountId the account identifier to set
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Retrieves the user role for role-based access control.
     * 
     * <p>Returns the user role that determines the level of card information
     * that can be accessed and the data masking rules that apply.</p>
     * 
     * @return the user role for access control
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Sets the user role for role-based access control.
     * 
     * <p>The user role determines what level of card information can be accessed
     * and what data masking rules apply to the response.</p>
     * 
     * @param userRole the user role to set
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * Retrieves the flag indicating whether to include masked card data.
     * 
     * <p>Returns the boolean flag that controls whether sensitive card data
     * should be included in the response with appropriate masking applied.</p>
     * 
     * @return true if masked data should be included, false otherwise
     */
    public Boolean getIncludeMaskedData() {
        return includeMaskedData;
    }

    /**
     * Sets the flag indicating whether to include masked card data.
     * 
     * <p>Controls whether sensitive card data should be included in the response
     * with appropriate masking applied based on user role and security requirements.</p>
     * 
     * @param includeMaskedData true to include masked data, false otherwise
     */
    public void setIncludeMaskedData(Boolean includeMaskedData) {
        this.includeMaskedData = includeMaskedData;
    }

    /**
     * Retrieves the flag indicating whether to validate existence in the database.
     * 
     * <p>Returns the boolean flag that controls whether the selection operation
     * should perform database lookups to verify the existence of the specified
     * card or account.</p>
     * 
     * @return true if existence validation should be performed, false otherwise
     */
    public Boolean getValidateExistence() {
        return validateExistence;
    }

    /**
     * Sets the flag indicating whether to validate existence in the database.
     * 
     * <p>Controls whether the selection operation should perform database lookups
     * to verify the existence of the specified card or account. This may impact
     * performance but ensures data integrity.</p>
     * 
     * @param validateExistence true to perform existence validation, false otherwise
     */
    public void setValidateExistence(Boolean validateExistence) {
        this.validateExistence = validateExistence;
    }

    /**
     * Retrieves the flag indicating whether to include cross-reference data.
     * 
     * <p>Returns the boolean flag that controls whether the selection operation
     * should include related account and customer information in the response.</p>
     * 
     * @return true if cross-reference data should be included, false otherwise
     */
    public Boolean getIncludeCrossReference() {
        return includeCrossReference;
    }

    /**
     * Sets the flag indicating whether to include cross-reference data.
     * 
     * <p>Controls whether the selection operation should include related account
     * and customer information in the response. This may impact performance but
     * provides comprehensive card information.</p>
     * 
     * @param includeCrossReference true to include cross-reference data, false otherwise
     */
    public void setIncludeCrossReference(Boolean includeCrossReference) {
        this.includeCrossReference = includeCrossReference;
    }

    /**
     * Validates that the request contains sufficient information for card selection.
     * 
     * <p>Ensures that either a card number or account ID is provided for the
     * selection operation. This validation replicates the COBOL cross-field
     * validation logic from COCRDSLC.cbl lines 636-641.</p>
     * 
     * @return true if request has sufficient selection criteria, false otherwise
     */
    public boolean hasValidSelectionCriteria() {
        return (cardNumber != null && !cardNumber.trim().isEmpty()) ||
               (accountId != null && !accountId.trim().isEmpty());
    }

    /**
     * Validates that the request is properly configured for the specified user role.
     * 
     * <p>Ensures that the request parameters are compatible with the user's
     * role and permissions. This includes validation of data access flags
     * and cross-reference permissions.</p>
     * 
     * @return true if request is compatible with user role, false otherwise
     */
    public boolean isValidForUserRole() {
        if (userRole == null || userRole.trim().isEmpty()) {
            return false;
        }
        
        // Guest users cannot access sensitive data
        if ("GUEST".equals(userRole)) {
            return !Boolean.TRUE.equals(includeMaskedData);
        }
        
        // All other roles are valid with any combination of flags
        return true;
    }

    /**
     * Determines if the request requires database access for processing.
     * 
     * <p>Returns true if any of the request parameters require database
     * operations such as existence validation or cross-reference data retrieval.</p>
     * 
     * @return true if database access is required, false otherwise
     */
    public boolean requiresDatabaseAccess() {
        return Boolean.TRUE.equals(validateExistence) || 
               Boolean.TRUE.equals(includeCrossReference);
    }

    /**
     * Provides string representation of the card selection request.
     * 
     * <p>Returns a formatted string representation of the request for logging
     * and debugging purposes. Sensitive card information is masked for security.</p>
     * 
     * @return string representation of the request
     */
    @Override
    public String toString() {
        return String.format(
            "CardSelectionRequestDto{" +
            "cardNumber='%s', " +
            "accountId='%s', " +
            "userRole='%s', " +
            "includeMaskedData=%s, " +
            "validateExistence=%s, " +
            "includeCrossReference=%s, " +
            "correlationId='%s'}",
            cardNumber != null ? "[MASKED]" : null,
            accountId != null ? "[PROTECTED]" : null,
            userRole,
            includeMaskedData,
            validateExistence,
            includeCrossReference,
            getCorrelationId()
        );
    }
}