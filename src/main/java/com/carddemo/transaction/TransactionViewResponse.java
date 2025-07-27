package com.carddemo.transaction;

import com.carddemo.common.dto.BaseResponseDto;
import com.carddemo.common.dto.AuditInfo;
import com.carddemo.account.dto.AccountDto;
import com.carddemo.account.dto.CustomerDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Response DTO for transaction detail viewing with comprehensive transaction information
 * and security-aware data presentation.
 * 
 * This response DTO provides complete transaction viewing functionality equivalent to the
 * COBOL COTRN01C.cbl program which handles transaction detail viewing operations. It
 * integrates transaction data with related account and customer information while
 * implementing appropriate security masking based on user authorization levels.
 * 
 * The class maintains exact field correspondence to the COBOL transaction view screen
 * (COTRN1A.bms) while providing modern JSON-based API response capabilities and
 * comprehensive audit trail support for compliance requirements.
 * 
 * Key Features:
 * <ul>
 *   <li>Complete transaction detail presentation with merchant information</li>
 *   <li>Account cross-reference data for transaction context</li>
 *   <li>Customer information for comprehensive transaction view</li>
 *   <li>Security-aware data masking based on user authorization levels</li>
 *   <li>Comprehensive audit trail support for compliance tracking</li>
 *   <li>Responsive data structure supporting React frontend components</li>
 * </ul>
 * 
 * COBOL Program Correspondence:
 * - Maps to COTRN01C.cbl transaction viewing program functionality
 * - Preserves field sequencing from COTRN1A.bms screen definition
 * - Maintains exact data presentation as COBOL PROCESS-ENTER-KEY paragraph
 * - Implements equivalent error handling and validation logic
 * 
 * Security Implementation:
 * - Implements field-level masking for sensitive cardholder data
 * - Supports role-based data presentation (TELLER, ADMIN, CUSTOMER, etc.)
 * - Maintains audit trail for all data access operations
 * - Complies with PCI DSS requirements for cardholder data display
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since Java 21
 * @see com.carddemo.transaction.TransactionDTO
 * @see com.carddemo.account.dto.AccountDto
 * @see com.carddemo.account.dto.CustomerDto
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionViewResponse extends BaseResponseDto {

    /**
     * Complete transaction details including all transaction-specific information.
     * 
     * Maps to the transaction record structure populated in COTRN01C.cbl lines 177-190
     * where transaction fields are moved from TRAN-RECORD to screen output fields.
     * Includes comprehensive transaction data with exact COBOL field correspondence.
     */
    @Valid
    @NotNull(message = "Transaction details are required")
    private TransactionDTO transactionDetails;

    /**
     * Account information associated with the transaction.
     * 
     * Provides account context for the transaction including current balance,
     * credit limit, and account status information. Essential for transaction
     * validation and account management operations corresponding to account
     * cross-reference lookups in the original COBOL implementation.
     */
    @Valid
    private AccountDto accountInfo;

    /**
     * Customer information associated with the transaction account.
     * 
     * Provides customer context for the transaction including personal information
     * and contact details. Supports comprehensive transaction view with customer
     * relationship data for account management and customer service operations.
     */
    @Valid
    private CustomerDto customerInfo;

    /**
     * Audit information for transaction access tracking and compliance.
     * 
     * Records comprehensive audit trail information for transaction view operations
     * including user context, timestamps, and correlation information. Essential
     * for SOX compliance, security monitoring, and regulatory audit requirements.
     */
    @Valid
    @NotNull(message = "Audit information is required")
    private AuditInfo auditInfo;

    /**
     * Timestamp when the transaction view response was accessed.
     * 
     * Records the exact moment when the transaction details were accessed,
     * providing precise audit trail information for compliance and security
     * monitoring. Corresponds to current date/time population in COTRN01C.cbl
     * lines 245-262 (POPULATE-HEADER-INFO paragraph).
     */
    @NotNull(message = "Access timestamp is required")
    private LocalDateTime accessTimestamp;

    /**
     * Flag indicating whether sensitive data has been masked for security.
     * 
     * Indicates that sensitive cardholder data (card numbers, account information)
     * has been masked based on user authorization levels. Supports PCI DSS
     * compliance requirements for restricting cardholder data display based
     * on user roles and permissions.
     */
    private boolean dataMasked;

    /**
     * User authorization level determining data masking and field access.
     * 
     * Stores the user's authorization level (ADMIN, TELLER, CUSTOMER, etc.)
     * which determines the level of data masking applied to sensitive fields.
     * Used for implementing role-based access control equivalent to RACF
     * authorization checks in the original COBOL system.
     */
    private String userAuthorizationLevel;

    /**
     * Set of field names that have been masked for security purposes.
     * 
     * Contains the names of specific fields that have been masked based on
     * user authorization levels and data sensitivity classifications. Provides
     * transparency to client applications about which fields contain masked
     * data for appropriate user interface presentation.
     */
    private Set<String> maskedFields;

    /**
     * Default constructor initializing response with current timestamp and audit trail.
     * 
     * Automatically initializes the response with current access timestamp and
     * prepares audit information structure. Sets default success status and
     * initializes security-related fields for proper data protection.
     */
    public TransactionViewResponse() {
        super();
        this.accessTimestamp = LocalDateTime.now();
        this.auditInfo = new AuditInfo();
        this.maskedFields = new HashSet<>();
        this.dataMasked = false;
    }

    /**
     * Constructor with correlation ID for distributed tracing support.
     * 
     * Creates a successful transaction view response with specified correlation ID
     * for end-to-end request tracking across microservice boundaries. Maintains
     * audit trail consistency and supports distributed debugging capabilities.
     * 
     * @param correlationId Unique identifier for request correlation and tracing
     */
    public TransactionViewResponse(String correlationId) {
        super(correlationId);
        this.accessTimestamp = LocalDateTime.now();
        this.auditInfo = new AuditInfo();
        this.maskedFields = new HashSet<>();
        this.dataMasked = false;
    }

    /**
     * Constructor for error responses with comprehensive error context.
     * 
     * Creates an error response with detailed error information and correlation
     * tracking. Used when transaction view operations fail due to business logic
     * errors, validation failures, or system exceptions equivalent to error
     * handling in COTRN01C.cbl lines 283-296 (READ-TRANSACT-FILE error cases).
     * 
     * @param errorMessage Descriptive error message for client consumption
     * @param correlationId Unique identifier for request correlation and tracing
     */
    public TransactionViewResponse(String errorMessage, String correlationId) {
        super(errorMessage, correlationId);
        this.accessTimestamp = LocalDateTime.now();
        this.auditInfo = new AuditInfo();
        this.maskedFields = new HashSet<>();
        this.dataMasked = false;
    }

    /**
     * Gets the complete transaction details.
     * 
     * @return TransactionDTO containing all transaction-specific information
     */
    public TransactionDTO getTransactionDetails() {
        return transactionDetails;
    }

    /**
     * Sets the complete transaction details with validation.
     * 
     * @param transactionDetails TransactionDTO with comprehensive transaction data
     */
    public void setTransactionDetails(TransactionDTO transactionDetails) {
        this.transactionDetails = transactionDetails;
    }

    /**
     * Gets the account information associated with the transaction.
     * 
     * @return AccountDto containing account context for the transaction
     */
    public AccountDto getAccountInfo() {
        return accountInfo;
    }

    /**
     * Sets the account information associated with the transaction.
     * 
     * @param accountInfo AccountDto with account context data
     */
    public void setAccountInfo(AccountDto accountInfo) {
        this.accountInfo = accountInfo;
    }

    /**
     * Gets the customer information associated with the transaction account.
     * 
     * @return CustomerDto containing customer context for the transaction
     */
    public CustomerDto getCustomerInfo() {
        return customerInfo;
    }

    /**
     * Sets the customer information associated with the transaction account.
     * 
     * @param customerInfo CustomerDto with customer context data
     */
    public void setCustomerInfo(CustomerDto customerInfo) {
        this.customerInfo = customerInfo;
    }

    /**
     * Gets the audit information for transaction access tracking.
     * 
     * @return AuditInfo containing comprehensive audit trail data
     */
    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

    /**
     * Sets the audit information for transaction access tracking.
     * 
     * @param auditInfo AuditInfo with audit trail and compliance data
     */
    public void setAuditInfo(AuditInfo auditInfo) {
        this.auditInfo = auditInfo;
    }

    /**
     * Gets the timestamp when the transaction view was accessed.
     * 
     * @return LocalDateTime of transaction access for audit trail
     */
    public LocalDateTime getAccessTimestamp() {
        return accessTimestamp;
    }

    /**
     * Sets the timestamp when the transaction view was accessed.
     * 
     * @param accessTimestamp LocalDateTime for audit trail timestamp
     */
    public void setAccessTimestamp(LocalDateTime accessTimestamp) {
        this.accessTimestamp = accessTimestamp;
    }

    /**
     * Checks whether sensitive data has been masked for security.
     * 
     * @return true if sensitive data has been masked, false otherwise
     */
    public boolean isDataMasked() {
        return dataMasked;
    }

    /**
     * Sets the data masking flag for security compliance.
     * 
     * @param dataMasked true if sensitive data has been masked, false otherwise
     */
    public void setDataMasked(boolean dataMasked) {
        this.dataMasked = dataMasked;
    }

    /**
     * Gets the user authorization level determining data access.
     * 
     * @return String representing user authorization level (ADMIN, TELLER, etc.)
     */
    public String getUserAuthorizationLevel() {
        return userAuthorizationLevel;
    }

    /**
     * Sets the user authorization level determining data access.
     * 
     * @param userAuthorizationLevel String representing user authorization level
     */
    public void setUserAuthorizationLevel(String userAuthorizationLevel) {
        this.userAuthorizationLevel = userAuthorizationLevel;
    }

    /**
     * Gets the set of field names that have been masked for security.
     * 
     * @return Set of field names containing masked data
     */
    public Set<String> getMaskedFields() {
        return maskedFields;
    }

    /**
     * Sets the set of field names that have been masked for security.
     * 
     * @param maskedFields Set of field names containing masked data
     */
    public void setMaskedFields(Set<String> maskedFields) {
        this.maskedFields = maskedFields != null ? maskedFields : new HashSet<>();
    }

    // Convenience methods for accessing nested transaction data

    /**
     * Gets the transaction type from the embedded transaction details.
     * 
     * Provides convenient access to transaction type information without requiring
     * clients to navigate the nested transaction details structure. Supports
     * direct field access equivalent to COBOL field references.
     * 
     * @return String representation of transaction type, null if no transaction details
     */
    public String getTransactionType() {
        return transactionDetails != null && transactionDetails.getTransactionType() != null ? 
            transactionDetails.getTransactionType().toString() : null;
    }

    /**
     * Gets the transaction amount from the embedded transaction details.
     * 
     * Provides convenient access to transaction amount with exact BigDecimal precision
     * maintaining COBOL COMP-3 arithmetic compatibility. Returns null if transaction
     * details are not available or amount is not set.
     * 
     * @return BigDecimal transaction amount with COBOL precision, null if unavailable
     */
    public BigDecimal getTransactionAmount() {
        return transactionDetails != null ? transactionDetails.getAmount() : null;
    }

    /**
     * Gets the transaction ID from the embedded transaction details.
     * 
     * Provides convenient access to the unique transaction identifier without
     * requiring clients to navigate nested structures. Essential for transaction
     * correlation and audit trail purposes.
     * 
     * @return String transaction ID, null if no transaction details available
     */
    public String getTransactionId() {
        return transactionDetails != null ? transactionDetails.getTransactionId() : null;
    }

    /**
     * Gets the card number from the embedded transaction details.
     * 
     * Returns the raw card number from transaction details. Note that this may
     * return masked card number depending on user authorization levels and
     * security settings applied during response creation.
     * 
     * @return String card number (may be masked), null if unavailable
     */
    public String getCardNumber() {
        return transactionDetails != null ? transactionDetails.getCardNumber() : null;
    }

    /**
     * Gets a masked version of the card number for secure display.
     * 
     * Returns a consistently masked card number showing only the first 4 and last 4
     * digits, regardless of user authorization level. Provides PCI DSS compliant
     * card number display for general use cases and audit logs.
     * 
     * @return String masked card number (XXXX****XXXX format), "****" if unavailable
     */
    public String getMaskedCardNumber() {
        String cardNumber = getCardNumber();
        if (cardNumber == null || cardNumber.length() < 8) {
            return "****";
        }
        return cardNumber.substring(0, 4) + "****" + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Gets consolidated merchant information from the embedded transaction details.
     * 
     * Combines merchant name, city, and ZIP code into a single formatted string
     * for convenient display purposes. Provides merchant context equivalent to
     * the merchant information display in COTRN1A.bms screen layout.
     * 
     * @return String formatted merchant information, "Unknown Merchant" if unavailable
     */
    public String getMerchantInfo() {
        if (transactionDetails == null) {
            return "Unknown Merchant";
        }
        
        StringBuilder merchantInfo = new StringBuilder();
        String merchantName = transactionDetails.getMerchantName();
        String merchantCity = transactionDetails.getMerchantCity();
        String merchantZip = transactionDetails.getMerchantZip();
        
        if (merchantName != null && !merchantName.trim().isEmpty()) {
            merchantInfo.append(merchantName.trim());
        } else {
            merchantInfo.append("Unknown Merchant");
        }
        
        if (merchantCity != null && !merchantCity.trim().isEmpty()) {
            merchantInfo.append(", ").append(merchantCity.trim());
        }
        
        if (merchantZip != null && !merchantZip.trim().isEmpty()) {
            merchantInfo.append(" ").append(merchantZip.trim());
        }
        
        return merchantInfo.toString();
    }

    /**
     * Gets the transaction processing status based on embedded transaction details.
     * 
     * Determines transaction processing status by analyzing transaction timestamps
     * and data completeness. Provides status information equivalent to transaction
     * processing indicators in the original COBOL system.
     * 
     * @return String processing status: "PROCESSED", "PENDING", or "INCOMPLETE"
     */
    public String getProcessingStatus() {
        if (transactionDetails == null) {
            return "INCOMPLETE";
        }
        
        // Check if transaction has processing timestamp indicating completion
        if (transactionDetails.getProcessingTimestamp() != null) {
            return "PROCESSED";
        }
        
        // Check if transaction has original timestamp but no processing timestamp
        if (transactionDetails.getOriginalTimestamp() != null) {
            return "PENDING";
        }
        
        return "INCOMPLETE";
    }

    /**
     * Adds a field name to the masked fields set for security tracking.
     * 
     * Records that a specific field has been masked for security purposes,
     * supporting transparency in data masking operations and enabling client
     * applications to provide appropriate user interface feedback.
     * 
     * @param fieldName Name of the field that has been masked
     */
    public void addMaskedField(String fieldName) {
        if (fieldName != null && !fieldName.trim().isEmpty()) {
            this.maskedFields.add(fieldName.trim());
            this.dataMasked = true; // Automatically set data masking flag
        }
    }

    /**
     * Removes a field name from the masked fields set.
     * 
     * Removes a field from the masked fields tracking set, typically used
     * when field masking is reversed based on elevated user authorization
     * or when field data is updated with unmasked values.
     * 
     * @param fieldName Name of the field to remove from masked tracking
     */
    public void removeMaskedField(String fieldName) {
        if (fieldName != null) {
            this.maskedFields.remove(fieldName.trim());
            // Update data masking flag based on remaining masked fields
            this.dataMasked = !this.maskedFields.isEmpty();
        }
    }

    /**
     * Checks if a specific field has been masked for security.
     * 
     * Determines whether a specific field name is tracked as having been
     * masked for security purposes. Supports conditional client-side
     * presentation logic and audit trail verification.
     * 
     * @param fieldName Name of the field to check for masking
     * @return true if the field has been masked, false otherwise
     */
    public boolean isFieldMasked(String fieldName) {
        return fieldName != null && this.maskedFields.contains(fieldName.trim());
    }

    /**
     * Validates the complete transaction view response for data integrity.
     * 
     * Performs comprehensive validation of all response components including
     * transaction details, account information, customer data, and audit
     * information. Ensures data consistency and completeness for client
     * consumption and compliance requirements.
     * 
     * @return true if all validation passes, false otherwise
     */
    public boolean validateResponse() {
        try {
            // Validate required transaction details
            if (transactionDetails == null || !transactionDetails.isValid()) {
                return false;
            }
            
            // Validate required audit information
            if (auditInfo == null || auditInfo.getUserId() == null || 
                auditInfo.getOperationType() == null) {
                return false;
            }
            
            // Validate access timestamp
            if (accessTimestamp == null) {
                return false;
            }
            
            // Validate account information if provided
            if (accountInfo != null && !accountInfo.validate()) {
                return false;
            }
            
            // Validate data masking consistency
            if (dataMasked && (maskedFields == null || maskedFields.isEmpty())) {
                return false;
            }
            
            // Validate base response consistency
            if (hasError() && (getErrorMessage() == null || getErrorMessage().trim().isEmpty())) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            // Log validation error and return false for any unexpected validation failures
            return false;
        }
    }

    /**
     * Creates a string representation of the transaction view response.
     * 
     * Provides comprehensive but secure string representation with sensitive
     * data masking for logging and debugging purposes. Includes key response
     * metadata while protecting cardholder data and personal information.
     * 
     * @return Formatted string representation with security masking
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TransactionViewResponse{");
        sb.append("success=").append(isSuccess());
        
        if (transactionDetails != null) {
            sb.append(", transactionId='").append(getTransactionId()).append("'");
            sb.append(", transactionType='").append(getTransactionType()).append("'");
            sb.append(", amount=").append(getTransactionAmount());
            sb.append(", maskedCard='").append(getMaskedCardNumber()).append("'");
        }
        
        if (accountInfo != null) {
            sb.append(", accountId='").append(accountInfo.getAccountId()).append("'");
        }
        
        if (customerInfo != null) {
            sb.append(", customerId='").append(customerInfo.getCustomerId()).append("'");
        }
        
        sb.append(", dataMasked=").append(dataMasked);
        sb.append(", maskedFieldCount=").append(maskedFields.size());
        sb.append(", authLevel='").append(userAuthorizationLevel).append("'");
        sb.append(", accessTime=").append(accessTimestamp);
        
        if (hasError()) {
            sb.append(", error='").append(getErrorMessage()).append("'");
        }
        
        sb.append(", correlationId='").append(getCorrelationId()).append("'");
        sb.append('}');
        
        return sb.toString();
    }
}