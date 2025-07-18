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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.carddemo.card;

import com.carddemo.common.dto.BaseResponseDto;
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
 * Response DTO for card selection operations with complete card details, cross-reference data,
 * and role-based field masking providing comprehensive card information for COCRDSLC.cbl functionality.
 * 
 * This DTO transforms the COBOL card selection screen (COCRDSLC.cbl) functionality into a modern
 * REST API response, maintaining exact field mappings and business logic while enabling 
 * role-based sensitive data masking and comprehensive audit trail support.
 * 
 * Original COBOL Program: COCRDSLC.cbl
 * Transaction ID: CCDL (Card Credit Detail List)
 * BMS Map: COCRDSL.bms
 * 
 * Key Features:
 * - Complete card details with cross-reference account and customer information
 * - Role-based field masking for sensitive data (CVV, full card number)
 * - Comprehensive balance and credit limit information
 * - Audit trail information for compliance and security monitoring
 * - Conditional field population based on user authorization level
 * - JSON serialization for React frontend integration
 * - Jakarta Bean Validation for data integrity
 * 
 * COBOL Field Mappings:
 * - CARD-CARD-NUM-X → cardNumber/maskedCardNumber
 * - CARD-NAME-EMBOSSED-X → embossedName
 * - CARD-EXPIRAION-DATE-X → expirationDate
 * - CARD-STATUS-X → activeStatus
 * - CARD-CVV-CD-X → cvvCode/maskedCvvCode
 * - CARD-ACCT-ID-X → accountInfo (via cross-reference)
 * - Account balance data → accountBalance
 * - Customer profile data → customerInfo
 * 
 * Security Features:
 * - Conditional data masking based on user authorization level
 * - CVV code masking for non-privileged users
 * - Card number masking with last 4 digits visible
 * - Audit trail tracking for sensitive data access
 * - User context validation for data authorization
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 */
public class CardSelectionResponseDto extends BaseResponseDto {

    /**
     * Complete card details including all card-specific information.
     * Maps to CARD-RECORD structure from COCRDSLC.cbl
     */
    @JsonProperty("card_details")
    @Valid
    @NotNull(message = "Card details are required")
    private Card cardDetails;

    /**
     * Account information including balance and credit details.
     * Maps to account cross-reference data from COCRDSLC.cbl
     */
    @JsonProperty("account_info")
    @Valid
    @NotNull(message = "Account information is required")
    private AccountDto accountInfo;

    /**
     * Customer profile information for the card holder.
     * Maps to customer cross-reference data from COCRDSLC.cbl
     */
    @JsonProperty("customer_info")
    @Valid
    @NotNull(message = "Customer information is required")
    private CustomerDto customerInfo;

    /**
     * Account balance information including current balance and credit limits.
     * Maps to balance calculations from COCRDSLC.cbl
     */
    @JsonProperty("account_balance")
    @Valid
    @NotNull(message = "Account balance information is required")
    private AccountBalanceDto accountBalance;

    /**
     * Audit information for compliance and security tracking.
     * Maps to audit trail requirements from COCRDSLC.cbl
     */
    @JsonProperty("audit_info")
    @Valid
    @NotNull(message = "Audit information is required")
    private AuditInfo auditInfo;

    /**
     * Full card number (16 digits) - should be masked for non-privileged users.
     * Maps to CARD-CARD-NUM-X from COCRDSLC.cbl
     */
    @JsonProperty("card_number")
    private String cardNumber;

    /**
     * Masked card number showing only last 4 digits for security.
     * Format: ****-****-****-1234
     */
    @JsonProperty("masked_card_number")
    private String maskedCardNumber;

    /**
     * Embossed name as it appears on the card.
     * Maps to CARD-NAME-EMBOSSED-X from COCRDSLC.cbl
     */
    @JsonProperty("embossed_name")
    private String embossedName;

    /**
     * Card expiration date in MM/YY format.
     * Maps to CARD-EXPIRAION-DATE-X from COCRDSLC.cbl
     */
    @JsonProperty("expiration_date")
    private String expirationDate;

    /**
     * Card active status (A=Active, I=Inactive, B=Blocked).
     * Maps to CARD-STATUS-X from COCRDSLC.cbl
     */
    @JsonProperty("active_status")
    private String activeStatus;

    /**
     * CVV code (3 digits) - should be masked for non-privileged users.
     * Maps to CARD-CVV-CD-X from COCRDSLC.cbl
     */
    @JsonProperty("cvv_code")
    private String cvvCode;

    /**
     * Masked CVV code showing asterisks for security.
     * Format: ***
     */
    @JsonProperty("masked_cvv_code")
    private String maskedCvvCode;

    /**
     * Last accessed timestamp for audit trail.
     * Maps to current timestamp from COCRDSLC.cbl
     */
    @JsonProperty("last_accessed_timestamp")
    private LocalDateTime lastAccessedTimestamp;

    /**
     * Flag indicating if sensitive data has been masked.
     * Used by frontend to determine data display logic
     */
    @JsonProperty("data_masked")
    private boolean dataMasked;

    /**
     * User authorization level for conditional data masking.
     * Determines which fields should be masked or visible
     */
    @JsonProperty("user_authorization_level")
    private String userAuthorizationLevel;

    /**
     * Default constructor for JSON deserialization and framework usage.
     */
    public CardSelectionResponseDto() {
        super();
        this.lastAccessedTimestamp = LocalDateTime.now();
        this.dataMasked = false;
        this.userAuthorizationLevel = "STANDARD";
    }

    /**
     * Constructor for successful card selection with complete information.
     * 
     * @param cardDetails Complete card information
     * @param accountInfo Account cross-reference data
     * @param customerInfo Customer profile information
     * @param accountBalance Balance and credit information
     * @param auditInfo Audit trail information
     */
    public CardSelectionResponseDto(Card cardDetails, AccountDto accountInfo, 
                                   CustomerDto customerInfo, AccountBalanceDto accountBalance,
                                   AuditInfo auditInfo) {
        super("Card details retrieved successfully");
        this.cardDetails = cardDetails;
        this.accountInfo = accountInfo;
        this.customerInfo = customerInfo;
        this.accountBalance = accountBalance;
        this.auditInfo = auditInfo;
        this.lastAccessedTimestamp = LocalDateTime.now();
        this.dataMasked = false;
        this.userAuthorizationLevel = "STANDARD";
        
        // Populate card-specific fields from Card entity
        if (cardDetails != null) {
            this.cardNumber = cardDetails.getCardNumber();
            this.maskedCardNumber = cardDetails.getMaskedCardNumber();
            this.embossedName = cardDetails.getEmbossedName();
            this.expirationDate = cardDetails.getExpirationMonthYear();
            this.activeStatus = cardDetails.getActiveStatus() != null ? 
                               cardDetails.getActiveStatus().toString() : "U";
            this.cvvCode = cardDetails.getCvvCode();
            this.maskedCvvCode = "***";
        }
    }

    /**
     * Constructor for error response with failure information.
     * 
     * @param success Success status (false for errors)
     * @param errorMessage Error message describing the failure
     * @param correlationId Request correlation ID
     */
    public CardSelectionResponseDto(boolean success, String errorMessage, String correlationId) {
        super(success, errorMessage, correlationId, "CARD_SELECTION");
        this.lastAccessedTimestamp = LocalDateTime.now();
        this.dataMasked = false;
        this.userAuthorizationLevel = "STANDARD";
    }

    // Getter and Setter methods for all fields

    /**
     * Gets the complete card details.
     * 
     * @return Card entity with complete information
     */
    public Card getCardDetails() {
        return cardDetails;
    }

    /**
     * Sets the complete card details.
     * 
     * @param cardDetails Card entity with complete information
     */
    public void setCardDetails(Card cardDetails) {
        this.cardDetails = cardDetails;
        
        // Update card-specific fields when card details change
        if (cardDetails != null) {
            this.cardNumber = cardDetails.getCardNumber();
            this.maskedCardNumber = cardDetails.getMaskedCardNumber();
            this.embossedName = cardDetails.getEmbossedName();
            this.expirationDate = cardDetails.getExpirationMonthYear();
            this.activeStatus = cardDetails.getActiveStatus() != null ? 
                               cardDetails.getActiveStatus().toString() : "U";
            this.cvvCode = cardDetails.getCvvCode();
            this.maskedCvvCode = "***";
        }
    }

    /**
     * Gets the account information.
     * 
     * @return Account DTO with cross-reference data
     */
    public AccountDto getAccountInfo() {
        return accountInfo;
    }

    /**
     * Sets the account information.
     * 
     * @param accountInfo Account DTO with cross-reference data
     */
    public void setAccountInfo(AccountDto accountInfo) {
        this.accountInfo = accountInfo;
    }

    /**
     * Gets the customer information.
     * 
     * @return Customer DTO with profile data
     */
    public CustomerDto getCustomerInfo() {
        return customerInfo;
    }

    /**
     * Sets the customer information.
     * 
     * @param customerInfo Customer DTO with profile data
     */
    public void setCustomerInfo(CustomerDto customerInfo) {
        this.customerInfo = customerInfo;
    }

    /**
     * Gets the account balance information.
     * 
     * @return Account balance DTO with financial data
     */
    public AccountBalanceDto getAccountBalance() {
        return accountBalance;
    }

    /**
     * Sets the account balance information.
     * 
     * @param accountBalance Account balance DTO with financial data
     */
    public void setAccountBalance(AccountBalanceDto accountBalance) {
        this.accountBalance = accountBalance;
    }

    /**
     * Gets the audit information.
     * 
     * @return Audit info DTO with compliance data
     */
    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

    /**
     * Sets the audit information.
     * 
     * @param auditInfo Audit info DTO with compliance data
     */
    public void setAuditInfo(AuditInfo auditInfo) {
        this.auditInfo = auditInfo;
    }

    /**
     * Gets the full card number.
     * 
     * @return 16-digit card number (should be masked for non-privileged users)
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the full card number.
     * 
     * @param cardNumber 16-digit card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
        
        // Update masked card number when full card number changes
        if (cardNumber != null && cardNumber.length() == 16) {
            this.maskedCardNumber = "****-****-****-" + cardNumber.substring(12);
        }
    }

    /**
     * Gets the masked card number.
     * 
     * @return Masked card number (****-****-****-1234)
     */
    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    /**
     * Sets the masked card number.
     * 
     * @param maskedCardNumber Masked card number
     */
    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
    }

    /**
     * Gets the embossed name.
     * 
     * @return Name as it appears on the card
     */
    public String getEmbossedName() {
        return embossedName;
    }

    /**
     * Sets the embossed name.
     * 
     * @param embossedName Name as it appears on the card
     */
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    /**
     * Gets the expiration date.
     * 
     * @return Expiration date in MM/YY format
     */
    public String getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the expiration date.
     * 
     * @param expirationDate Expiration date in MM/YY format
     */
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the active status.
     * 
     * @return Card status (A=Active, I=Inactive, B=Blocked)
     */
    public String getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the active status.
     * 
     * @param activeStatus Card status (A=Active, I=Inactive, B=Blocked)
     */
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the CVV code.
     * 
     * @return 3-digit CVV code (should be masked for non-privileged users)
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
        this.maskedCvvCode = "***"; // Always mask CVV code
    }

    /**
     * Gets the masked CVV code.
     * 
     * @return Masked CVV code (***)
     */
    public String getMaskedCvvCode() {
        return maskedCvvCode;
    }

    /**
     * Sets the masked CVV code.
     * 
     * @param maskedCvvCode Masked CVV code
     */
    public void setMaskedCvvCode(String maskedCvvCode) {
        this.maskedCvvCode = maskedCvvCode;
    }

    /**
     * Gets the last accessed timestamp.
     * 
     * @return Timestamp of last access
     */
    public LocalDateTime getLastAccessedTimestamp() {
        return lastAccessedTimestamp;
    }

    /**
     * Sets the last accessed timestamp.
     * 
     * @param lastAccessedTimestamp Timestamp of last access
     */
    public void setLastAccessedTimestamp(LocalDateTime lastAccessedTimestamp) {
        this.lastAccessedTimestamp = lastAccessedTimestamp;
    }

    /**
     * Checks if sensitive data has been masked.
     * 
     * @return true if data is masked, false otherwise
     */
    public boolean isDataMasked() {
        return dataMasked;
    }

    /**
     * Sets the data masked flag.
     * 
     * @param dataMasked true if data is masked, false otherwise
     */
    public void setDataMasked(boolean dataMasked) {
        this.dataMasked = dataMasked;
    }

    /**
     * Gets the user authorization level.
     * 
     * @return User authorization level
     */
    public String getUserAuthorizationLevel() {
        return userAuthorizationLevel;
    }

    /**
     * Sets the user authorization level.
     * 
     * @param userAuthorizationLevel User authorization level
     */
    public void setUserAuthorizationLevel(String userAuthorizationLevel) {
        this.userAuthorizationLevel = userAuthorizationLevel;
    }

    /**
     * Applies role-based masking to sensitive fields based on user authorization level.
     * 
     * This method implements the security logic similar to COBOL field protection
     * attributes, masking sensitive data for users without appropriate privileges.
     * 
     * @param userAuthLevel User's authorization level (ADMIN, SUPERVISOR, STANDARD)
     */
    public void applyRoleBasedMasking(String userAuthLevel) {
        this.userAuthorizationLevel = userAuthLevel;
        
        // Apply masking based on authorization level
        if ("ADMIN".equals(userAuthLevel)) {
            // Admin users can see all data
            this.dataMasked = false;
        } else if ("SUPERVISOR".equals(userAuthLevel)) {
            // Supervisors can see full card number but not CVV
            this.cvvCode = null;
            this.dataMasked = true;
        } else {
            // Standard users see masked data only
            this.cardNumber = null;
            this.cvvCode = null;
            this.dataMasked = true;
        }
    }

    /**
     * Validates the completeness of the card selection response.
     * 
     * @return true if all required fields are populated, false otherwise
     */
    public boolean isComplete() {
        return cardDetails != null && 
               accountInfo != null && 
               customerInfo != null && 
               accountBalance != null && 
               auditInfo != null &&
               (maskedCardNumber != null || cardNumber != null) &&
               embossedName != null &&
               expirationDate != null &&
               activeStatus != null;
    }

    /**
     * Creates a successful response with complete card information.
     * 
     * @param cardDetails Complete card information
     * @param accountInfo Account cross-reference data
     * @param customerInfo Customer profile information
     * @param accountBalance Balance and credit information
     * @param auditInfo Audit trail information
     * @return CardSelectionResponseDto with success status
     */
    public static CardSelectionResponseDto success(Card cardDetails, AccountDto accountInfo,
                                                  CustomerDto customerInfo, AccountBalanceDto accountBalance,
                                                  AuditInfo auditInfo) {
        return new CardSelectionResponseDto(cardDetails, accountInfo, customerInfo, accountBalance, auditInfo);
    }

    /**
     * Creates an error response for card selection failures.
     * 
     * @param errorMessage Error message describing the failure
     * @param correlationId Request correlation ID
     * @return CardSelectionResponseDto with error status
     */
    public static CardSelectionResponseDto error(String errorMessage, String correlationId) {
        return new CardSelectionResponseDto(false, errorMessage, correlationId);
    }

    @Override
    public String toString() {
        return "CardSelectionResponseDto{" +
                "cardNumber='" + (cardNumber != null ? maskedCardNumber : "null") + '\'' +
                ", embossedName='" + embossedName + '\'' +
                ", expirationDate='" + expirationDate + '\'' +
                ", activeStatus='" + activeStatus + '\'' +
                ", cvvCode='" + (cvvCode != null ? maskedCvvCode : "null") + '\'' +
                ", dataMasked=" + dataMasked +
                ", userAuthorizationLevel='" + userAuthorizationLevel + '\'' +
                ", lastAccessedTimestamp=" + lastAccessedTimestamp +
                ", success=" + isSuccess() +
                ", message='" + getMessage() + '\'' +
                ", correlationId='" + getCorrelationId() + '\'' +
                '}';
    }
}