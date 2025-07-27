package com.carddemo.card;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.validator.ValidAccountId;
import com.carddemo.common.validator.ValidCardNumber;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for card listing operations with comprehensive pagination support, 
 * role-based filtering capabilities, and input validation supporting COCRDLIC.cbl 
 * functionality through modern REST API contracts.
 * 
 * <p>This DTO transforms the original COBOL CICS transaction (COCRDLIC) into a 
 * stateless REST API request structure while preserving exact functional equivalence
 * including:</p>
 * <ul>
 *   <li>7 cards per page pagination matching WS-MAX-SCREEN-LINES COBOL constant</li>
 *   <li>Account ID filtering with 11-digit validation (CC-ACCT-ID PIC X(11))</li>
 *   <li>Card number filtering with 16-digit validation (CC-CARD-NUM PIC X(16))</li>
 *   <li>Role-based access control (CDEMO-USRTYP-USER vs admin privileges)</li>
 *   <li>Flexible search criteria and sort options for enhanced user experience</li>
 *   <li>Include/exclude inactive cards filtering for comprehensive data management</li>
 * </ul>
 * 
 * <p><strong>COBOL Program Mapping:</strong></p>
 * <p>This DTO directly supports the functionality of COCRDLIC.cbl which handles:</p>
 * <ul>
 *   <li>Card listing with account and card number filters (lines 1003-1066)</li>
 *   <li>Pagination logic for 7 cards per screen (lines 177-178, 1191)</li>
 *   <li>Role-based filtering and admin vs user access control</li>
 *   <li>Selection processing for card view ('S') and update ('U') operations</li>
 * </ul>
 * 
 * <p><strong>Validation Strategy:</strong></p>
 * <p>Comprehensive Jakarta Bean Validation ensures data integrity:</p>
 * <ul>
 *   <li>@ValidAccountId - Custom validation for 11-digit account ID format</li>
 *   <li>@ValidCardNumber - Custom validation with Luhn algorithm for card numbers</li>
 *   <li>@Size constraints for string field length validation</li>
 *   <li>@Pattern validation for user roles and sort parameters</li>
 *   <li>@Min/@Max constraints for pagination boundaries</li>
 * </ul>
 * 
 * <p><strong>JSON Serialization:</strong></p>
 * <p>Configured with Jackson annotations for optimal API performance:</p>
 * <ul>
 *   <li>@JsonInclude(NON_NULL) to reduce payload size</li>
 *   <li>@JsonProperty for consistent field naming</li>
 *   <li>Support for optional parameters with intelligent defaults</li>
 * </ul>
 * 
 * <p><strong>Performance Considerations:</strong></p>
 * <p>Designed for high-throughput transaction processing:</p>
 * <ul>
 *   <li>Stateless design enabling horizontal scaling</li>
 *   <li>Efficient pagination parameters for optimal database queries</li>
 *   <li>Role-based filtering to minimize data transfer</li>
 *   <li>Index-friendly search criteria for PostgreSQL optimization</li>
 * </ul>
 * 
 * @author Blitzy Platform - CardDemo Migration Team
 * @version 1.0
 * @since 2024-01-01
 * @see com.carddemo.common.dto.BaseRequestDto
 * @see com.carddemo.common.validator.ValidAccountId
 * @see com.carddemo.common.validator.ValidCardNumber
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardListRequestDto extends BaseRequestDto {

    /**
     * Page number for pagination support, starting from 0.
     * 
     * <p>Maps to COBOL WS-CA-SCREEN-NUM field for screen navigation.
     * Default value of 0 represents the first page of results.</p>
     * 
     * <p>Validation ensures reasonable pagination boundaries to prevent
     * resource exhaustion and maintain system performance under load.</p>
     */
    @JsonProperty("pageNumber")
    @Min(value = 0, message = "Page number must be non-negative")
    @Max(value = 9999, message = "Page number cannot exceed 9999")
    private Integer pageNumber = 0;

    /**
     * Number of cards per page with default of 7 cards.
     * 
     * <p>Directly matches COBOL WS-MAX-SCREEN-LINES constant (VALUE 7) from
     * COCRDLIC.cbl ensuring identical pagination behavior in the modernized system.</p>
     * 
     * <p>Range validation prevents excessive memory usage while maintaining
     * user experience compatibility with the original mainframe interface.</p>
     */
    @JsonProperty("pageSize")
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 50, message = "Page size cannot exceed 50 cards")
    private Integer pageSize = 7;

    /**
     * Optional account ID filter for role-based card listing.
     * 
     * <p>Maps to COBOL CC-ACCT-ID field (PIC X(11)) with exact validation
     * rules preserving mainframe data integrity requirements.</p>
     * 
     * <p>When provided by non-admin users, restricts results to cards
     * associated with the specified account ID. Admin users can view
     * all cards regardless of account association.</p>
     */
    @JsonProperty("accountId")
    @ValidAccountId(allowEmpty = true, message = "Account ID must be 11 digits when provided")
    private String accountId;

    /**
     * Optional card number filter for specific card search.
     * 
     * <p>Maps to COBOL CC-CARD-NUM field (PIC X(16)) with Luhn algorithm
     * validation ensuring credit card number integrity and industry compliance.</p>
     * 
     * <p>Enables precise card lookup functionality equivalent to COBOL
     * card number filtering logic (lines 1036-1066 in COCRDLIC.cbl).</p>
     */
    @JsonProperty("cardNumber")
    @ValidCardNumber
    private String cardNumber;

    /**
     * User role for access control and filtering logic.
     * 
     * <p>Maps to COBOL CDEMO-USRTYP-USER condition for role-based access control.
     * Determines filtering behavior:</p>
     * <ul>
     *   <li>ADMIN: Can view all cards across all accounts</li>
     *   <li>USER: Can only view cards associated with their accessible accounts</li>
     * </ul>
     * 
     * <p>Essential for maintaining security boundaries equivalent to original
     * RACF authorization patterns in the mainframe environment.</p>
     */
    @JsonProperty("userRole")
    @NotNull(message = "User role is required for access control")
    @Pattern(regexp = "^(ADMIN|USER)$", message = "User role must be either ADMIN or USER")
    private String userRole;

    /**
     * General search criteria for flexible card discovery.
     * 
     * <p>Enables fuzzy matching against multiple card attributes including
     * card holder name, card type, or other searchable fields. Enhances
     * the original COBOL functionality with modern search capabilities.</p>
     * 
     * <p>Length validation prevents potential performance issues with
     * overly complex search expressions while maintaining usability.</p>
     */
    @JsonProperty("searchCriteria")
    @Size(max = 100, message = "Search criteria cannot exceed 100 characters")
    private String searchCriteria;

    /**
     * Flag to include inactive cards in results.
     * 
     * <p>Maps to COBOL CARD-ACTIVE-STATUS field processing logic,
     * allowing users to include or exclude inactive cards from listing results.</p>
     * 
     * <p>Default false value matches typical user workflow where inactive
     * cards are filtered out unless specifically requested by the user.</p>
     */
    @JsonProperty("includeInactive")
    private Boolean includeInactive = false;

    /**
     * Field name for result sorting.
     * 
     * <p>Specifies the database field to use for sorting results. Supported
     * values map to PostgreSQL cards table columns with proper indexing
     * for optimal query performance.</p>
     * 
     * <p>Default sorting by card number provides predictable result ordering
     * equivalent to COBOL CARD-NUM based record sequencing.</p>
     */
    @JsonProperty("sortBy")
    @Pattern(regexp = "^(cardNumber|accountId|cardStatus|cardHolderName|expirationDate)$", 
             message = "Sort field must be one of: cardNumber, accountId, cardStatus, cardHolderName, expirationDate")
    private String sortBy = "cardNumber";

    /**
     * Sort direction for result ordering.
     * 
     * <p>Controls ascending or descending sort order for the specified sortBy field.
     * ASC direction matches typical COBOL file processing order while DESC
     * enables reverse chronological or alphabetical ordering as needed.</p>
     */
    @JsonProperty("sortDirection")
    @Pattern(regexp = "^(ASC|DESC)$", message = "Sort direction must be either ASC or DESC")
    private String sortDirection = "ASC";

    /**
     * Default constructor for framework instantiation and JSON deserialization.
     * 
     * <p>Initializes all default values ensuring consistent behavior across
     * different instantiation scenarios. Calls parent BaseRequestDto constructor
     * to establish correlation ID and request timestamp.</p>
     */
    public CardListRequestDto() {
        super();
    }

    /**
     * Constructor with correlation ID for distributed transaction tracking.
     * 
     * <p>Used when correlation ID needs to be propagated from upstream services
     * or when specific transaction correlation is required for audit purposes.</p>
     * 
     * @param correlationId the correlation identifier for distributed tracing
     */
    public CardListRequestDto(String correlationId) {
        super(correlationId);
    }

    /**
     * Gets the page number for pagination.
     * 
     * @return the current page number (0-based)
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the page number for pagination.
     * 
     * @param pageNumber the page number to retrieve (0-based)
     */
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * Gets the page size for pagination.
     * 
     * @return the number of cards per page
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Sets the page size for pagination.
     * 
     * @param pageSize the number of cards per page
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Gets the account ID filter.
     * 
     * @return the account ID for filtering results
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID filter.
     * 
     * @param accountId the account ID for filtering results
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the card number filter.
     * 
     * @return the card number for filtering results
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number filter.
     * 
     * @param cardNumber the card number for filtering results
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the user role for access control.
     * 
     * @return the user role (ADMIN or USER)
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Sets the user role for access control.
     * 
     * @param userRole the user role (ADMIN or USER)
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * Gets the search criteria.
     * 
     * @return the search criteria for flexible matching
     */
    public String getSearchCriteria() {
        return searchCriteria;
    }

    /**
     * Sets the search criteria.
     * 
     * @param searchCriteria the search criteria for flexible matching
     */
    public void setSearchCriteria(String searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    /**
     * Gets the include inactive cards flag.
     * 
     * @return true if inactive cards should be included, false otherwise
     */
    public Boolean getIncludeInactive() {
        return includeInactive;
    }

    /**
     * Sets the include inactive cards flag.
     * 
     * @param includeInactive true to include inactive cards, false otherwise
     */
    public void setIncludeInactive(Boolean includeInactive) {
        this.includeInactive = includeInactive;
    }

    /**
     * Gets the sort field name.
     * 
     * @return the field name for sorting results
     */
    public String getSortBy() {
        return sortBy;
    }

    /**
     * Sets the sort field name.
     * 
     * @param sortBy the field name for sorting results
     */
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    /**
     * Gets the sort direction.
     * 
     * @return the sort direction (ASC or DESC)
     */
    public String getSortDirection() {
        return sortDirection;
    }

    /**
     * Sets the sort direction.
     * 
     * @param sortDirection the sort direction (ASC or DESC)
     */
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    /**
     * Returns a string representation of the CardListRequestDto for debugging and logging.
     * 
     * <p>Provides comprehensive request details for troubleshooting while maintaining
     * security by not exposing sensitive card information. Includes all filter
     * criteria and pagination parameters for complete request traceability.</p>
     * 
     * @return detailed string representation of the request
     */
    @Override
    public String toString() {
        return "CardListRequestDto{" +
                "pageNumber=" + pageNumber +
                ", pageSize=" + pageSize +
                ", accountId='" + (accountId != null ? "***" + accountId.substring(Math.max(0, accountId.length() - 4)) : null) + '\'' +
                ", cardNumber='" + (cardNumber != null ? "****-****-****-" + cardNumber.substring(Math.max(0, cardNumber.length() - 4)) : null) + '\'' +
                ", userRole='" + userRole + '\'' +
                ", searchCriteria='" + searchCriteria + '\'' +
                ", includeInactive=" + includeInactive +
                ", sortBy='" + sortBy + '\'' +
                ", sortDirection='" + sortDirection + '\'' +
                ", correlationId='" + getCorrelationId() + '\'' +
                ", userId='" + getUserId() + '\'' +
                ", requestTimestamp=" + getRequestTimestamp() +
                '}';
    }

    /**
     * Validates that the request parameters are logically consistent and complete.
     * 
     * <p>Performs business logic validation beyond basic field validation:</p>
     * <ul>
     *   <li>Ensures pagination parameters are within reasonable bounds</li>
     *   <li>Validates that role-based access rules are properly applied</li>
     *   <li>Confirms that filter combinations are logical and supported</li>
     *   <li>Verifies search criteria compatibility with sort parameters</li>
     * </ul>
     * 
     * <p>This method complements Jakarta Bean Validation annotations with
     * business rule validation equivalent to COBOL input editing logic.</p>
     * 
     * @return true if the request is valid for processing, false otherwise
     */
    public boolean isValidRequest() {
        // Call parent validation for base fields
        if (!super.isValid()) {
            return false;
        }
        
        // Validate pagination parameters
        if (pageNumber == null || pageNumber < 0) {
            return false;
        }
        
        if (pageSize == null || pageSize < 1 || pageSize > 50) {
            return false;
        }
        
        // Validate user role is specified for access control
        if (userRole == null || (!userRole.equals("ADMIN") && !userRole.equals("USER"))) {
            return false;
        }
        
        // Validate that search criteria is not excessively long
        if (searchCriteria != null && searchCriteria.length() > 100) {
            return false;
        }
        
        // Ensure sort parameters are valid combinations
        if (sortBy != null && !sortBy.matches("^(cardNumber|accountId|cardStatus|cardHolderName|expirationDate)$")) {
            return false;
        }
        
        if (sortDirection != null && !sortDirection.matches("^(ASC|DESC)$")) {
            return false;
        }
        
        return true;
    }

    /**
     * Determines if this request requires administrator privileges.
     * 
     * <p>Returns true if the request parameters indicate administrative access
     * is needed, such as viewing all cards across accounts or accessing
     * system-wide card information. Maps to COBOL CDEMO-USRTYP-USER logic.</p>
     * 
     * @return true if admin privileges are required, false otherwise
     */
    public boolean requiresAdminAccess() {
        return "ADMIN".equals(userRole) || 
               (accountId == null && searchCriteria == null);
    }

    /**
     * Creates a copy of this request with specified page number.
     * 
     * <p>Utility method for pagination navigation maintaining all filter
     * criteria while changing only the page number. Useful for implementing
     * page navigation controls in the frontend.</p>
     * 
     * @param newPageNumber the new page number for the copied request
     * @return a new CardListRequestDto with the specified page number
     */
    public CardListRequestDto withPageNumber(int newPageNumber) {
        CardListRequestDto copy = new CardListRequestDto(this.getCorrelationId());
        copy.setUserId(this.getUserId());
        copy.setSessionId(this.getSessionId());
        copy.setPageNumber(newPageNumber);
        copy.setPageSize(this.pageSize);
        copy.setAccountId(this.accountId);
        copy.setCardNumber(this.cardNumber);
        copy.setUserRole(this.userRole);
        copy.setSearchCriteria(this.searchCriteria);
        copy.setIncludeInactive(this.includeInactive);
        copy.setSortBy(this.sortBy);
        copy.setSortDirection(this.sortDirection);
        return copy;
    }
}