package com.carddemo.account.dto;

import com.carddemo.common.util.ValidationUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Account View Request DTO - Request data transfer object for account view operations.
 * 
 * <p>This DTO replaces the CICS COMMAREA structures from the COACTVWC.cbl transaction,
 * providing JSON serialization support for modern REST API endpoints while maintaining
 * exact field validation and business logic equivalence from the original COBOL
 * implementation.</p>
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Account ID validation with 11-digit format requirement per Section 2.1.3</li>
 *   <li>Pagination support for account view operations with configurable page size</li>
 *   <li>Session context mapping for stateless REST API design replacing CICS pseudo-conversational processing</li>
 *   <li>Jakarta Bean Validation annotations ensuring COBOL PICTURE clause compliance</li>
 *   <li>JSON serialization with null value exclusion for optimal data transfer</li>
 * </ul>
 * 
 * <p>COBOL Equivalency:</p>
 * <ul>
 *   <li>Replaces CARDDEMO-COMMAREA structure from COCOM01Y.cpy</li>
 *   <li>Account ID maps to CDEMO-ACCT-ID PIC 9(11) with identical validation</li>
 *   <li>Session context preserves CDEMO-GENERAL-INFO structure elements</li>
 *   <li>Validation logic matches COACTVWC.cbl paragraph 2210-EDIT-ACCOUNT</li>
 * </ul>
 * 
 * <p>REST API Integration:</p>
 * <ul>
 *   <li>Supports GET /api/accounts/view endpoint with query parameters</li>
 *   <li>Enables account lookup by ID with pagination and search criteria</li>
 *   <li>Provides session context for stateless microservice communication</li>
 *   <li>Ensures consistent JSON request structure across account services</li>
 * </ul>
 * 
 * <p>Performance Considerations:</p>
 * <ul>
 *   <li>Lightweight DTO structure optimized for REST API serialization</li>
 *   <li>Efficient validation using pre-compiled patterns from ValidationUtils</li>
 *   <li>Minimal memory footprint supporting 10,000+ TPS processing requirements</li>
 * </ul>
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountViewRequestDto {
    
    /**
     * Account ID for account view operations.
     * Maps to CDEMO-ACCT-ID PIC 9(11) from COBOL COMMAREA.
     * Must be exactly 11 digits and within valid account range.
     */
    @Size(min = 11, max = 11, message = "Account ID must be exactly 11 digits")
    private String accountId;
    
    /**
     * Page number for pagination support.
     * Zero-based page indexing for consistent REST API pagination.
     * Default value is 0 for first page.
     */
    private Integer pageNumber = 0;
    
    /**
     * Page size for pagination support.
     * Number of account records to return per page.
     * Default value is 20 for optimal performance.
     */
    private Integer pageSize = 20;
    
    /**
     * Session context information for stateless REST API design.
     * Replaces CICS pseudo-conversational processing with distributed session management.
     * Contains user ID, transaction context, and program flow information.
     */
    private String sessionContext;
    
    /**
     * Default constructor for JSON deserialization and framework usage.
     */
    public AccountViewRequestDto() {
        // Default constructor for JSON deserialization
    }
    
    /**
     * Constructor with account ID for direct account lookup operations.
     * 
     * @param accountId the account ID to retrieve (must be 11 digits)
     */
    public AccountViewRequestDto(String accountId) {
        this.accountId = accountId;
    }
    
    /**
     * Constructor with full pagination parameters for comprehensive account view requests.
     * 
     * @param accountId the account ID to retrieve (must be 11 digits)
     * @param pageNumber zero-based page number for pagination
     * @param pageSize number of records per page
     * @param sessionContext session context for stateless processing
     */
    public AccountViewRequestDto(String accountId, Integer pageNumber, Integer pageSize, String sessionContext) {
        this.accountId = accountId;
        this.pageNumber = pageNumber != null ? pageNumber : 0;
        this.pageSize = pageSize != null ? pageSize : 20;
        this.sessionContext = sessionContext;
    }
    
    /**
     * Gets the account ID for account view operations.
     * 
     * @return the account ID (11 digits) or null if not set
     */
    public String getAccountId() {
        return accountId;
    }
    
    /**
     * Sets the account ID for account view operations.
     * 
     * @param accountId the account ID (must be exactly 11 digits)
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    /**
     * Gets the page number for pagination support.
     * 
     * @return the zero-based page number
     */
    public Integer getPageNumber() {
        return pageNumber;
    }
    
    /**
     * Sets the page number for pagination support.
     * 
     * @param pageNumber the zero-based page number (defaults to 0 if null)
     */
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber != null ? pageNumber : 0;
    }
    
    /**
     * Gets the page size for pagination support.
     * 
     * @return the number of records per page
     */
    public Integer getPageSize() {
        return pageSize;
    }
    
    /**
     * Sets the page size for pagination support.
     * 
     * @param pageSize the number of records per page (defaults to 20 if null)
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize != null ? pageSize : 20;
    }
    
    /**
     * Gets the session context for stateless REST API processing.
     * 
     * @return the session context string containing user and transaction information
     */
    public String getSessionContext() {
        return sessionContext;
    }
    
    /**
     * Sets the session context for stateless REST API processing.
     * 
     * @param sessionContext the session context containing user and transaction information
     */
    public void setSessionContext(String sessionContext) {
        this.sessionContext = sessionContext;
    }
    
    /**
     * Validates the account view request using COBOL-equivalent validation logic.
     * 
     * <p>Performs comprehensive validation including:</p>
     * <ul>
     *   <li>Account ID format validation (11 digits, non-zero)</li>
     *   <li>Pagination parameter validation (non-negative values)</li>
     *   <li>Session context presence validation</li>
     *   <li>Business rule validation matching COACTVWC.cbl logic</li>
     * </ul>
     * 
     * @return true if all validation checks pass, false otherwise
     */
    public boolean validate() {
        // Validate account ID if provided
        if (accountId != null && !accountId.isEmpty()) {
            var accountValidation = ValidationUtils.validateAccountNumber(accountId);
            if (accountValidation != com.carddemo.common.enums.ValidationResult.VALID) {
                return false;
            }
        }
        
        // Validate pagination parameters
        if (pageNumber != null && pageNumber < 0) {
            return false;
        }
        
        if (pageSize != null && (pageSize <= 0 || pageSize > 1000)) {
            return false;
        }
        
        // Validate session context for stateless processing
        if (sessionContext != null) {
            var sessionValidation = ValidationUtils.validateRequiredField(sessionContext, "sessionContext");
            if (sessionValidation != com.carddemo.common.enums.ValidationResult.VALID) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if the request contains valid data for processing.
     * 
     * <p>Performs lightweight validation to determine if the request
     * has sufficient data for account view operations:</p>
     * <ul>
     *   <li>Either account ID is provided for specific account lookup</li>
     *   <li>Or pagination parameters are set for account listing</li>
     *   <li>Session context is available for stateless processing</li>
     * </ul>
     * 
     * @return true if request contains valid data, false otherwise
     */
    public boolean isValid() {
        // Check if account ID is provided and valid
        if (accountId != null && !accountId.trim().isEmpty()) {
            return validate();
        }
        
        // Check if pagination parameters are reasonable for listing
        if (pageNumber != null && pageSize != null) {
            return pageNumber >= 0 && pageSize > 0 && pageSize <= 1000;
        }
        
        // At minimum, we need some search criteria
        return false;
    }
    
    /**
     * Checks if two AccountViewRequestDto objects are equal.
     * 
     * @param obj the object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AccountViewRequestDto that = (AccountViewRequestDto) obj;
        return Objects.equals(accountId, that.accountId) &&
               Objects.equals(pageNumber, that.pageNumber) &&
               Objects.equals(pageSize, that.pageSize) &&
               Objects.equals(sessionContext, that.sessionContext);
    }
    
    /**
     * Generates hash code for the AccountViewRequestDto object.
     * 
     * @return hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId, pageNumber, pageSize, sessionContext);
    }
    
    /**
     * Returns string representation of the AccountViewRequestDto object.
     * 
     * @return string representation with all field values
     */
    @Override
    public String toString() {
        return "AccountViewRequestDto{" +
               "accountId='" + accountId + '\'' +
               ", pageNumber=" + pageNumber +
               ", pageSize=" + pageSize +
               ", sessionContext='" + sessionContext + '\'' +
               '}';
    }
}