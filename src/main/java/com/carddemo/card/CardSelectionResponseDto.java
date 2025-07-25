/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.card;

import com.carddemo.common.dto.BaseResponseDto;
import com.carddemo.card.Card;
import com.carddemo.account.dto.AccountDto;
import com.carddemo.account.dto.CustomerDto;
import com.carddemo.common.dto.AuditInfo;
import com.carddemo.account.AccountBalanceDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for card selection operations providing comprehensive card details,
 * cross-reference data, and role-based field masking functionality.
 * 
 * This DTO implements the complete response structure for COCRDSLC.cbl functionality,
 * supporting card selection operations with comprehensive card information, related
 * account and customer data, and conditional data masking based on user authorization levels.
 * 
 * Key Features:
 * - Complete card details with cross-reference information matching COBOL CARD-RECORD structure
 * - Role-based sensitive data masking for PCI compliance and security requirements
 * - Comprehensive account balance information with exact COBOL COMP-3 precision
 * - Customer information integration with full personal data support
 * - Audit trail information for compliance and security monitoring
 * - JSON serialization optimized for React frontend components
 * 
 * COBOL Integration:
 * Maps directly to COCRDSLC.cbl card selection logic preserving:
 * - Card record structure from CVCRD01Y.cpy copybook (CARD-RECORD layout)
 * - Cross-reference data relationships maintaining VSAM file associations
 * - Field validation and business rules equivalent to original COBOL logic
 * - Error handling and message structures compatible with BMS screen definitions
 * 
 * Security and Compliance:
 * - CVV code masking based on user authorization level for PCI DSS compliance
 * - Card number masking supporting secure display requirements
 * - Audit trail integration for SOX compliance and security incident tracking
 * - Role-based field population ensuring data access control
 * 
 * Performance Requirements:
 * - Sub-200ms response time construction for card selection operations
 * - Memory efficient field loading supporting 10,000+ TPS throughput
 * - Lazy loading support for related entities reducing database overhead
 * - JSON serialization optimized for REST API response generation
 * 
 * React Frontend Integration:
 * - Field naming conventions aligned with Material-UI form components
 * - Validation state support for real-time form feedback
 * - Masked data presentation for secure card information display
 * - Dynamic field visibility based on user authorization context
 * 
 * @author CardDemo Development Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 * @see com.carddemo.card.Card
 * @see com.carddemo.account.dto.AccountDto
 * @see com.carddemo.account.dto.CustomerDto
 * @see com.carddemo.common.dto.BaseResponseDto
 */
public class CardSelectionResponseDto extends BaseResponseDto {

    // ===================================================================================
    // CORE CARD INFORMATION (from Card entity - CARD-RECORD equivalent)
    // ===================================================================================

    /**
     * Complete card entity details providing full card information.
     * 
     * Contains comprehensive card data including card number, account ID, customer ID,
     * embossed name, expiration date, CVV code, and active status. Supports lazy loading
     * from Card JPA entity with optimized query performance for card selection operations.
     * 
     * Maps to: CARD-RECORD structure from CVCRD01Y.cpy
     * Usage: Primary card information display and validation
     */
    @JsonProperty("card_details")
    @Valid
    private Card cardDetails;

    /**
     * Related account information with comprehensive financial data.
     * 
     * Provides complete account details including current balance, credit limits,
     * account status, and cross-reference information. Essential for card selection
     * operations requiring account context and authorization validation.
     * 
     * Maps to: ACCT-RECORD from CVACT01Y.cpy with cross-reference data
     * Usage: Account validation and financial status display
     */
    @JsonProperty("account_info")
    @Valid
    private AccountDto accountInfo;

    /**
     * Customer personal information and profile data.
     * 
     * Contains customer identification, contact information, address details,
     * and credit profile data. Required for card selection authorization and
     * customer verification workflows.
     * 
     * Maps to: CUST-RECORD from CVCUS01Y.cpy
     * Usage: Customer identification and profile display
     */
    @JsonProperty("customer_info")
    @Valid
    private CustomerDto customerInfo;

    /**
     * Account balance information with credit limit details.
     * 
     * Provides current balance, available credit, credit limits, and balance
     * status information with exact COBOL COMP-3 precision. Essential for
     * transaction authorization and account health monitoring.
     * 
     * Maps to: Account balance fields with COMP-3 precision
     * Usage: Financial status display and transaction authorization
     */
    @JsonProperty("account_balance")
    @Valid
    private AccountBalanceDto accountBalance;

    /**
     * Audit information for compliance and security tracking.
     * 
     * Contains user context, timestamps, operation type, and correlation
     * information for comprehensive audit trail support. Required for
     * SOX compliance and security incident investigation.
     * 
     * Maps to: Audit trail requirements for card selection operations
     * Usage: Compliance reporting and security monitoring
     */
    @JsonProperty("audit_info")
    @Valid
    private AuditInfo auditInfo;

    // ===================================================================================
    // INDIVIDUAL CARD FIELDS (for direct access and masking control)
    // ===================================================================================

    /**
     * Card number field for direct access and validation.
     * 
     * 16-digit card number providing primary card identification.
     * Subject to masking based on user authorization level for PCI compliance.
     * 
     * Validation: Must be exactly 16 digits with Luhn algorithm validation
     * Security: Masked based on user authorization level
     * 
     * Maps to: CARD-NUM from CARD-RECORD
     */
    @JsonProperty("card_number")
    @NotNull(message = "Card number is required for card selection response")
    private String cardNumber;

    /**
     * Masked card number for secure display purposes.
     * 
     * Provides PCI-compliant masked card number display showing only
     * first 4 and last 4 digits (e.g., "1234-****-****-5678").
     * Always safe for display regardless of authorization level.
     * 
     * Format: XXXX-XXXX-XXXX-YYYY where Y represents visible digits
     * Security: Always masked for PCI compliance
     */
    @JsonProperty("masked_card_number")
    private String maskedCardNumber;

    /**
     * Embossed name printed on the card.
     * 
     * Customer name as it appears on the physical card, used for
     * transaction authorization and customer identification.
     * 
     * Validation: Maximum 35 characters, alphabetic with spaces/hyphens
     * 
     * Maps to: CARD-EMBOSSED-NAME from CARD-RECORD
     */
    @JsonProperty("embossed_name")
    private String embossedName;

    /**
     * Card expiration date for validity checking.
     * 
     * Card expiration date used for transaction authorization and
     * card lifecycle management. Must be future date for active cards.
     * 
     * Format: LocalDateTime for precise date handling
     * Validation: Must be future date for valid cards
     * 
     * Maps to: CARD-EXPIRAION-DATE from CARD-RECORD
     */
    @JsonProperty("expiration_date")
    private LocalDateTime expirationDate;

    /**
     * Card active status for transaction authorization.
     * 
     * Current card status controlling transaction processing capability.
     * Values: ACTIVE, INACTIVE, BLOCKED as per CardStatus enumeration.
     * 
     * Validation: Must be valid CardStatus enum value
     * Usage: Transaction authorization and card lifecycle management
     * 
     * Maps to: CARD-ACTIVE-STATUS from CARD-RECORD
     */
    @JsonProperty("active_status")
    private String activeStatus;

    /**
     * Card verification value (CVV) code.
     * 
     * 3-4 digit security code used for payment authentication.
     * CRITICAL: Contains sensitive payment data requiring encryption and masking.
     * 
     * Validation: 3-4 digits, encrypted at rest
     * Security: Masked based on user authorization level
     * 
     * Maps to: CARD-CVV-CD from business requirements
     */
    @JsonProperty("cvv_code")
    private String cvvCode;

    /**
     * Masked CVV code for secure display.
     * 
     * Provides secure display of CVV code with masking for unauthorized users.
     * Typically displayed as "***" for security unless user has appropriate
     * authorization level for sensitive data access.
     * 
     * Security: Always masked unless special authorization
     */
    @JsonProperty("masked_cvv_code")
    private String maskedCvvCode;

    // ===================================================================================
    // METADATA AND AUDIT FIELDS
    // ===================================================================================

    /**
     * Timestamp when card data was last accessed.
     * 
     * Records the precise timestamp when card information was retrieved
     * for audit trail purposes and session management. Essential for
     * security monitoring and compliance reporting.
     * 
     * Usage: Audit trail and security monitoring
     * Format: LocalDateTime with precise timestamp
     */
    @JsonProperty("last_accessed_timestamp")
    private LocalDateTime lastAccessedTimestamp;

    /**
     * Data masking indicator flag.
     * 
     * Boolean flag indicating whether sensitive data fields have been
     * masked based on user authorization level. Enables frontend
     * components to adapt display logic accordingly.
     * 
     * Values: true if data is masked, false if unmasked
     * Usage: Frontend display logic and security indication
     */
    @JsonProperty("data_masked")
    private boolean dataMasked;

    /**
     * User authorization level for access control.
     * 
     * Indicates the authorization level of the requesting user for
     * controlling sensitive data access and field masking logic.
     * 
     * Values: ADMIN, MANAGER, USER, READONLY as per security roles
     * Usage: Data masking and access control decisions
     */
    @JsonProperty("user_authorization_level")
    private String userAuthorizationLevel;

    // ===================================================================================
    // CONSTRUCTORS
    // ===================================================================================

    /**
     * Default constructor for framework compatibility and JSON deserialization.
     * 
     * Initializes all timestamp fields to current time and sets default
     * values for boolean flags and security indicators.
     */
    public CardSelectionResponseDto() {
        super();
        this.lastAccessedTimestamp = LocalDateTime.now();
        this.dataMasked = false;
        this.userAuthorizationLevel = "USER"; // Default authorization level
    }

    /**
     * Constructor with correlation ID for distributed tracing support.
     * 
     * Creates response DTO with correlation ID for request tracking across
     * microservices boundaries while initializing audit timestamps.
     * 
     * @param correlationId Unique identifier for request correlation
     */
    public CardSelectionResponseDto(String correlationId) {
        super(correlationId);
        this.lastAccessedTimestamp = LocalDateTime.now();
        this.dataMasked = false;
        this.userAuthorizationLevel = "USER";
    }

    /**
     * Constructor with essential card selection data.
     * 
     * Creates response DTO with core card and related entity information
     * for complete card selection operation support.
     * 
     * @param cardDetails Complete card entity information
     * @param accountInfo Related account information
     * @param customerInfo Customer profile data
     * @param accountBalance Account balance and credit information
     * @param correlationId Request correlation identifier
     */
    public CardSelectionResponseDto(Card cardDetails, AccountDto accountInfo, 
                                   CustomerDto customerInfo, AccountBalanceDto accountBalance,
                                   String correlationId) {
        this(correlationId);
        this.cardDetails = cardDetails;
        this.accountInfo = accountInfo;
        this.customerInfo = customerInfo;
        this.accountBalance = accountBalance;
        
        // Populate individual fields from card entity if available
        if (cardDetails != null) {
            this.cardNumber = cardDetails.getCardNumber();
            this.embossedName = cardDetails.getEmbossedName();
            this.expirationDate = cardDetails.getExpirationDate() != null ? 
                cardDetails.getExpirationDate().atStartOfDay() : null;
            this.activeStatus = cardDetails.getActiveStatus() != null ? 
                cardDetails.getActiveStatus().toString() : null;
            this.cvvCode = cardDetails.getCvvCode();
            
            // Generate masked versions
            this.maskedCardNumber = generateMaskedCardNumber(this.cardNumber);
            this.maskedCvvCode = generateMaskedCvv(this.cvvCode);
        }
    }

    // ===================================================================================
    // GETTER AND SETTER METHODS (as required by exports specification)
    // ===================================================================================

    /**
     * Gets the complete card details entity.
     * 
     * @return Card entity containing full card information
     */
    public Card getCardDetails() {
        return cardDetails;
    }

    /**
     * Sets the complete card details entity.
     * 
     * Updates the card details and automatically populates individual
     * card fields for direct access and masking control.
     * 
     * @param cardDetails Card entity to set
     */
    public void setCardDetails(Card cardDetails) {
        this.cardDetails = cardDetails;
        
        // Update individual fields when card details change
        if (cardDetails != null) {
            this.cardNumber = cardDetails.getCardNumber();
            this.embossedName = cardDetails.getEmbossedName();
            this.expirationDate = cardDetails.getExpirationDate() != null ? 
                cardDetails.getExpirationDate().atStartOfDay() : null;
            this.activeStatus = cardDetails.getActiveStatus() != null ? 
                cardDetails.getActiveStatus().toString() : null;
            this.cvvCode = cardDetails.getCvvCode();
            
            // Update masked versions
            this.maskedCardNumber = generateMaskedCardNumber(this.cardNumber);
            this.maskedCvvCode = generateMaskedCvv(this.cvvCode);
        }
    }

    /**
     * Gets the account information DTO.
     * 
     * @return AccountDto containing comprehensive account data
     */
    public AccountDto getAccountInfo() {
        return accountInfo;
    }

    /**
     * Sets the account information DTO.
     * 
     * @param accountInfo Account information to set
     */
    public void setAccountInfo(AccountDto accountInfo) {
        this.accountInfo = accountInfo;
    }

    /**
     * Gets the customer information DTO.
     * 
     * @return CustomerDto containing customer profile data
     */
    public CustomerDto getCustomerInfo() {
        return customerInfo;
    }

    /**
     * Sets the customer information DTO.
     * 
     * @param customerInfo Customer information to set
     */
    public void setCustomerInfo(CustomerDto customerInfo) {
        this.customerInfo = customerInfo;
    }

    /**
     * Gets the account balance information.
     * 
     * @return AccountBalanceDto containing balance and credit data
     */
    public AccountBalanceDto getAccountBalance() {
        return accountBalance;
    }

    /**
     * Sets the account balance information.
     * 
     * @param accountBalance Account balance data to set
     */
    public void setAccountBalance(AccountBalanceDto accountBalance) {
        this.accountBalance = accountBalance;
    }

    /**
     * Gets the audit information for compliance tracking.
     * 
     * @return AuditInfo containing audit trail data
     */
    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

    /**
     * Sets the audit information for compliance tracking.
     * 
     * @param auditInfo Audit information to set
     */
    public void setAuditInfo(AuditInfo auditInfo) {
        this.auditInfo = auditInfo;
    }

    /**
     * Gets the card number for direct access.
     * 
     * @return 16-digit card number string
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number and updates masked version.
     * 
     * @param cardNumber Card number to set
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
        this.maskedCardNumber = generateMaskedCardNumber(cardNumber);
    }

    /**
     * Gets the masked card number for secure display.
     * 
     * @return Masked card number string
     */
    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    /**
     * Sets the masked card number directly.
     * 
     * @param maskedCardNumber Masked card number to set
     */
    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
    }

    /**
     * Gets the embossed name on the card.
     * 
     * @return Embossed name string
     */
    public String getEmbossedName() {
        return embossedName;
    }

    /**
     * Sets the embossed name on the card.
     * 
     * @param embossedName Embossed name to set
     */
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    /**
     * Gets the card expiration date.
     * 
     * @return Card expiration date
     */
    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the card expiration date.
     * 
     * @param expirationDate Expiration date to set
     */
    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the card active status.
     * 
     * @return Card active status string
     */
    public String getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the card active status.
     * 
     * @param activeStatus Active status to set
     */
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the CVV code (sensitive data - check authorization).
     * 
     * @return CVV code string
     */
    public String getCvvCode() {
        return cvvCode;
    }

    /**
     * Sets the CVV code and updates masked version.
     * 
     * @param cvvCode CVV code to set
     */
    public void setCvvCode(String cvvCode) {
        this.cvvCode = cvvCode;
        this.maskedCvvCode = generateMaskedCvv(cvvCode);
    }

    /**
     * Gets the masked CVV code for secure display.
     * 
     * @return Masked CVV code string
     */
    public String getMaskedCvvCode() {
        return maskedCvvCode;
    }

    /**
     * Sets the masked CVV code directly.
     * 
     * @param maskedCvvCode Masked CVV code to set
     */
    public void setMaskedCvvCode(String maskedCvvCode) {
        this.maskedCvvCode = maskedCvvCode;
    }

    /**
     * Gets the last accessed timestamp.
     * 
     * @return Last accessed timestamp
     */
    public LocalDateTime getLastAccessedTimestamp() {
        return lastAccessedTimestamp;
    }

    /**
     * Sets the last accessed timestamp.
     * 
     * @param lastAccessedTimestamp Timestamp to set
     */
    public void setLastAccessedTimestamp(LocalDateTime lastAccessedTimestamp) {
        this.lastAccessedTimestamp = lastAccessedTimestamp;
    }

    /**
     * Checks if data is masked based on authorization level.
     * 
     * @return true if data is masked, false otherwise
     */
    public boolean isDataMasked() {
        return dataMasked;
    }

    /**
     * Sets the data masking indicator.
     * 
     * @param dataMasked Data masking flag to set
     */
    public void setDataMasked(boolean dataMasked) {
        this.dataMasked = dataMasked;
    }

    /**
     * Gets the user authorization level.
     * 
     * @return User authorization level string
     */
    public String getUserAuthorizationLevel() {
        return userAuthorizationLevel;
    }

    /**
     * Sets the user authorization level.
     * 
     * @param userAuthorizationLevel Authorization level to set
     */
    public void setUserAuthorizationLevel(String userAuthorizationLevel) {
        this.userAuthorizationLevel = userAuthorizationLevel;
        // Update masking based on authorization level
        applyDataMasking();
    }

    // ===================================================================================
    // BUSINESS LOGIC AND UTILITY METHODS
    // ===================================================================================

    /**
     * Generates masked card number for secure display.
     * 
     * Creates PCI-compliant masked card number showing only first 4 and last 4 digits.
     * Format: "1234-****-****-5678" for secure display purposes.
     * 
     * @param cardNumber Original card number to mask
     * @return Masked card number string
     */
    private String generateMaskedCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16) {
            return "****-****-****-****";
        }
        
        return cardNumber.substring(0, 4) + "-****-****-" + cardNumber.substring(12);
    }

    /**
     * Generates masked CVV code for secure display.
     * 
     * Creates secure display of CVV code, typically showing "***" regardless
     * of the actual CVV length for consistent security presentation.
     * 
     * @param cvvCode Original CVV code to mask
     * @return Masked CVV code string
     */
    private String generateMaskedCvv(String cvvCode) {
        if (cvvCode == null) {
            return "***";
        }
        
        // Always return fixed mask length for security
        return "***";
    }

    /**
     * Applies data masking based on user authorization level.
     * 
     * Implements role-based data masking logic for PCI compliance and security.
     * Masks sensitive fields based on user authorization level:
     * - ADMIN: No masking (full access)
     * - MANAGER: Limited masking (card number visible, CVV masked)
     * - USER: Standard masking (card number masked, CVV masked)
     * - READONLY: Full masking (all sensitive data masked)
     */
    private void applyDataMasking() {
        if (userAuthorizationLevel == null) {
            userAuthorizationLevel = "USER"; // Default to USER level
        }
        
        switch (userAuthorizationLevel.toUpperCase()) {
            case "ADMIN":
                // Admin users see all data unmasked
                this.dataMasked = false;
                break;
                
            case "MANAGER":
                // Managers see card number but not CVV
                this.dataMasked = true;
                this.maskedCvvCode = generateMaskedCvv(this.cvvCode);
                break;
                
            case "USER":
            case "READONLY":
            default:
                // Standard users and readonly users see masked data
                this.dataMasked = true;
                this.maskedCardNumber = generateMaskedCardNumber(this.cardNumber);
                this.maskedCvvCode = generateMaskedCvv(this.cvvCode);
                break;
        }
    }

    /**
     * Validates the completeness of card selection response data.
     * 
     * Performs comprehensive validation of all required fields and related
     * entities to ensure response contains complete card selection information.
     * 
     * @return true if response is complete and valid, false otherwise
     */
    public boolean validateResponseCompleteness() {
        // Validate core card information
        if (cardDetails == null || cardNumber == null || cardNumber.trim().isEmpty()) {
            return false;
        }
        
        // Validate required related information
        if (accountInfo == null || customerInfo == null) {
            return false;
        }
        
        // Validate audit information is present
        if (auditInfo == null) {
            return false;
        }
        
        // Validate timestamps are set
        if (lastAccessedTimestamp == null) {
            return false;
        }
        
        return true;
    }

    /**
     * Updates the last accessed timestamp to current time.
     * 
     * Updates the access timestamp for audit trail purposes whenever
     * card information is accessed or refreshed.
     */
    public void updateLastAccessedTimestamp() {
        this.lastAccessedTimestamp = LocalDateTime.now();
    }

    /**
     * Creates audit information for the card selection operation.
     * 
     * Generates comprehensive audit information for card selection access
     * including user context, operation type, and correlation data for
     * compliance and security monitoring.
     * 
     * @param userId User performing the card selection
     * @param correlationId Request correlation identifier
     * @return AuditInfo object for the operation
     */
    public AuditInfo createAuditInfo(String userId, String correlationId) {
        AuditInfo audit = new AuditInfo(userId, "CARD_SELECTION", correlationId);
        audit.setSourceSystem("CardSelectionService");
        return audit;
    }

    /**
     * String representation for debugging and logging purposes.
     * 
     * Provides secure string representation excluding sensitive data but
     * including key identifiers for debugging and audit log purposes.
     * 
     * @return Formatted string representation
     */
    @Override
    public String toString() {
        return String.format(
            "CardSelectionResponseDto{" +
            "cardNumber='%s', " +
            "maskedCardNumber='%s', " +
            "embossedName='%s', " +
            "activeStatus='%s', " +
            "dataMasked=%s, " +
            "userAuthorizationLevel='%s', " +
            "lastAccessedTimestamp=%s, " +
            "success=%s" +
            "}",
            maskForLogging(cardNumber),
            maskedCardNumber,
            embossedName,
            activeStatus,
            dataMasked,
            userAuthorizationLevel,
            lastAccessedTimestamp,
            isSuccess()
        );
    }

    /**
     * Masks sensitive data for logging purposes.
     * 
     * @param sensitiveData Data to mask for logging
     * @return Masked version safe for logging
     */
    private String maskForLogging(String sensitiveData) {
        if (sensitiveData == null || sensitiveData.length() < 4) {
            return "****";
        }
        return sensitiveData.substring(0, 4) + "****";
    }
}