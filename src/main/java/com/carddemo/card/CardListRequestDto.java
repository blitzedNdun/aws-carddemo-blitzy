package com.carddemo.card;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.validator.ValidAccountId;
import com.carddemo.common.validator.ValidCardNumber;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for card listing operations with pagination support, role-based filtering,
 * and comprehensive input validation supporting COCRDLIC.cbl functionality through REST API contracts.
 * 
 * This DTO provides complete request structure for the CardListService.java microservice,
 * supporting all functionality from the original COBOL program COCRDLIC.cbl including:
 * - Pagination with default 7 cards per page (matching WS-MAX-SCREEN-LINES)
 * - Account ID filtering with 11-digit validation (CC-ACCT-ID equivalent)
 * - Card number filtering with 16-digit validation (CC-CARD-NUM equivalent)
 * - Role-based access control for admin vs regular user operations
 * - Search criteria and sorting options for enhanced user experience
 * - Inactive card inclusion controls for comprehensive data management
 * 
 * COBOL Integration Details:
 * - Replicates COCRDLIC.cbl input validation patterns from lines 1007-1066
 * - Maintains exact field length requirements matching copybook CVCRD01Y.cpy
 * - Supports filtering logic equivalent to 9500-FILTER-RECORDS section
 * - Preserves pagination behavior from 9000-READ-FORWARD and 9100-READ-BACKWARDS
 * - Implements role-based data access matching original admin/user logic
 * 
 * Spring Boot Integration:
 * - Extends BaseRequestDto for consistent request tracking and audit compliance
 * - Uses Jakarta Bean Validation for comprehensive input validation
 * - Supports Spring Data Pageable integration for efficient database queries
 * - Implements Jackson JSON serialization for clean API contracts
 * - Integrates with Spring Security for role-based access control
 * 
 * Performance Considerations:
 * - Default page size of 7 optimizes for original screen layout and user experience
 * - Validation annotations ensure early input rejection to prevent database load
 * - Optional filtering parameters reduce database query complexity
 * - Efficient JSON serialization with null field exclusion for optimal network usage
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardListRequestDto extends BaseRequestDto {

    private static final long serialVersionUID = 1L;

    /**
     * Default page size matching original COBOL screen layout.
     * Corresponds to WS-MAX-SCREEN-LINES (value 7) from COCRDLIC.cbl line 177.
     */
    public static final int DEFAULT_PAGE_SIZE = 7;

    /**
     * Maximum allowed page size to prevent excessive database load.
     * Maintains reasonable limits for pagination performance.
     */
    public static final int MAX_PAGE_SIZE = 50;

    /**
     * Page number for pagination, starting from 0 for Spring Data Pageable compatibility.
     * Implements pagination logic equivalent to WS-CA-SCREEN-NUM from COCRDLIC.cbl.
     * 
     * Validation ensures:
     * - Non-negative page numbers only
     * - Reasonable upper limits to prevent resource exhaustion
     * - Compatibility with Spring Data Pageable requirements
     */
    @JsonProperty("page_number")
    @Min(value = 0, message = "Page number must be non-negative")
    @Max(value = 10000, message = "Page number cannot exceed 10000")
    private Integer pageNumber = 0;

    /**
     * Number of records per page with default of 7 matching original COBOL screen layout.
     * Corresponds to WS-MAX-SCREEN-LINES from COCRDLIC.cbl for consistent user experience.
     * 
     * Validation ensures:
     * - Minimum of 1 record per page for practical usage
     * - Maximum of 50 records to prevent database performance issues
     * - Default value matches original COBOL implementation
     */
    @JsonProperty("page_size")
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = MAX_PAGE_SIZE, message = "Page size cannot exceed " + MAX_PAGE_SIZE)
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    /**
     * Optional account ID filter for card listing operations.
     * Implements filtering logic equivalent to CC-ACCT-ID from CVCRD01Y.cpy and
     * validation patterns from COCRDLIC.cbl lines 1007-1028.
     * 
     * When provided:
     * - Filters cards to show only those associated with the specified account
     * - Validates 11-digit numeric format matching original COBOL PIC 9(11)
     * - Supports both admin and regular user access patterns
     * - Integrates with 9500-FILTER-RECORDS logic for exact behavioral matching
     */
    @JsonProperty("account_id")
    @ValidAccountId(message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Optional card number filter for specific card lookup operations.
     * Implements filtering logic equivalent to CC-CARD-NUM from CVCRD01Y.cpy and
     * validation patterns from COCRDLIC.cbl lines 1042-1066.
     * 
     * When provided:
     * - Filters results to show only the specified card number
     * - Validates 16-digit numeric format with Luhn algorithm verification
     * - Supports exact match filtering matching original COBOL logic
     * - Integrates with card existence validation for data integrity
     */
    @JsonProperty("card_number")
    @ValidCardNumber(message = "Card number must be 16 digits and pass Luhn validation")
    private String cardNumber;

    /**
     * User role for role-based access control and data filtering.
     * Implements security logic equivalent to CDEMO-USRTYP from COCOM01Y.cpy
     * and admin/user differentiation from COCRDLIC.cbl.
     * 
     * Supported roles:
     * - "ADMIN": Full access to all cards regardless of account association
     * - "USER": Restricted access to cards associated with user's accounts only
     * - "MANAGER": Extended access with additional reporting capabilities
     * 
     * Role-based filtering ensures:
     * - Data security through appropriate access controls
     * - Consistent behavior with original COBOL security model
     * - Integration with Spring Security role validation
     */
    @JsonProperty("user_role")
    @NotNull(message = "User role is required for access control")
    @Pattern(regexp = "^(ADMIN|USER|MANAGER)$", 
             message = "User role must be one of: ADMIN, USER, MANAGER")
    private String userRole;

    /**
     * General search criteria for flexible card filtering.
     * Provides enhanced search capabilities beyond the original COBOL implementation
     * while maintaining compatibility with existing filtering logic.
     * 
     * Search criteria can include:
     * - Partial card number matching (with appropriate security controls)
     * - Account holder name search (when integrated with customer data)
     * - Card status filtering (active, inactive, expired)
     * - Card type filtering (credit, debit, prepaid)
     * 
     * Validation ensures:
     * - Reasonable search term length to prevent abuse
     * - Alphanumeric characters only for security
     * - Optional field allowing unrestricted card listing
     */
    @JsonProperty("search_criteria")
    @Size(max = 50, message = "Search criteria cannot exceed 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s]*$", 
             message = "Search criteria must contain only alphanumeric characters and spaces")
    private String searchCriteria;

    /**
     * Flag to include inactive cards in the listing results.
     * Extends original COBOL functionality to support comprehensive card management.
     * 
     * When true:
     * - Includes cards with inactive status in results
     * - Provides complete card inventory for administrative purposes
     * - Supports card lifecycle management operations
     * 
     * When false (default):
     * - Shows only active cards matching original COBOL behavior
     * - Optimizes results for day-to-day operational use
     * - Maintains performance with reduced result sets
     */
    @JsonProperty("include_inactive")
    private Boolean includeInactive = false;

    /**
     * Sort field specification for result ordering.
     * Provides enhanced sorting capabilities while maintaining compatibility
     * with original COBOL sequential processing patterns.
     * 
     * Supported sort fields:
     * - "cardNumber": Sort by card number (default, matching COBOL key order)
     * - "accountId": Sort by associated account ID
     * - "cardStatus": Sort by card status (active, inactive, expired)
     * - "expiryDate": Sort by card expiration date
     * - "cardType": Sort by card type classification
     * 
     * Default sorting matches original COBOL file organization by card number.
     */
    @JsonProperty("sort_by")
    @Pattern(regexp = "^(cardNumber|accountId|cardStatus|expiryDate|cardType)$",
             message = "Sort field must be one of: cardNumber, accountId, cardStatus, expiryDate, cardType")
    private String sortBy = "cardNumber";

    /**
     * Sort direction specification for ascending or descending order.
     * Provides flexible result ordering while maintaining default behavior
     * consistent with original COBOL sequential file processing.
     * 
     * Supported directions:
     * - "ASC": Ascending order (default, matching COBOL natural order)
     * - "DESC": Descending order for reverse chronological or alphabetical listing
     * 
     * Default ascending order matches original COBOL VSAM key sequence.
     */
    @JsonProperty("sort_direction")
    @Pattern(regexp = "^(ASC|DESC)$", 
             message = "Sort direction must be either ASC or DESC")
    private String sortDirection = "ASC";

    /**
     * Default constructor initializing request with standard defaults.
     * Calls parent constructor to establish correlation tracking and audit context.
     */
    public CardListRequestDto() {
        super();
    }

    /**
     * Constructor with correlation ID for distributed tracing.
     * Supports service-to-service communication with request correlation.
     * 
     * @param correlationId unique identifier for request tracking
     */
    public CardListRequestDto(String correlationId) {
        super(correlationId);
    }

    /**
     * Full constructor with complete request context.
     * Supports comprehensive request initialization with all context parameters.
     * 
     * @param correlationId unique identifier for request tracking
     * @param userId authenticated user identifier
     * @param sessionId session identifier for stateful operations
     */
    public CardListRequestDto(String correlationId, String userId, String sessionId) {
        super(correlationId, userId, sessionId);
    }

    /**
     * Retrieves the page number for pagination.
     * 
     * @return current page number (0-based index)
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the page number for pagination.
     * 
     * @param pageNumber page number to retrieve (0-based index)
     */
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * Retrieves the page size for pagination.
     * 
     * @return number of records per page
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Sets the page size for pagination.
     * 
     * @param pageSize number of records per page
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Retrieves the account ID filter.
     * 
     * @return account ID for filtering cards, or null if no filter applied
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID filter.
     * 
     * @param accountId account ID to filter cards by
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Retrieves the card number filter.
     * 
     * @return card number for filtering, or null if no filter applied
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number filter.
     * 
     * @param cardNumber card number to filter by
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Retrieves the user role for access control.
     * 
     * @return user role for role-based filtering and access control
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Sets the user role for access control.
     * 
     * @param userRole user role for role-based filtering and access control
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * Retrieves the search criteria.
     * 
     * @return search criteria for flexible filtering, or null if no search applied
     */
    public String getSearchCriteria() {
        return searchCriteria;
    }

    /**
     * Sets the search criteria.
     * 
     * @param searchCriteria search criteria for flexible filtering
     */
    public void setSearchCriteria(String searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    /**
     * Retrieves the inactive card inclusion flag.
     * 
     * @return true if inactive cards should be included, false otherwise
     */
    public Boolean getIncludeInactive() {
        return includeInactive;
    }

    /**
     * Sets the inactive card inclusion flag.
     * 
     * @param includeInactive true to include inactive cards, false otherwise
     */
    public void setIncludeInactive(Boolean includeInactive) {
        this.includeInactive = includeInactive;
    }

    /**
     * Retrieves the sort field specification.
     * 
     * @return field name for result sorting
     */
    public String getSortBy() {
        return sortBy;
    }

    /**
     * Sets the sort field specification.
     * 
     * @param sortBy field name for result sorting
     */
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    /**
     * Retrieves the sort direction specification.
     * 
     * @return sort direction (ASC or DESC)
     */
    public String getSortDirection() {
        return sortDirection;
    }

    /**
     * Sets the sort direction specification.
     * 
     * @param sortDirection sort direction (ASC or DESC)
     */
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    /**
     * Validates if the request has sufficient context for processing.
     * Ensures all required fields are present and valid for card listing operations.
     * 
     * @return true if request is valid for processing, false otherwise
     */
    public boolean isValidForProcessing() {
        return isValidRequestContext() && 
               userRole != null && !userRole.trim().isEmpty() &&
               pageNumber != null && pageNumber >= 0 &&
               pageSize != null && pageSize > 0 && pageSize <= MAX_PAGE_SIZE;
    }

    /**
     * Checks if any filtering criteria is applied.
     * Determines if the request includes filters that would restrict results.
     * 
     * @return true if filters are applied, false for unrestricted listing
     */
    public boolean hasFilters() {
        return (accountId != null && !accountId.trim().isEmpty()) ||
               (cardNumber != null && !cardNumber.trim().isEmpty()) ||
               (searchCriteria != null && !searchCriteria.trim().isEmpty());
    }

    /**
     * Checks if request includes inactive cards.
     * Convenience method for determining data inclusion scope.
     * 
     * @return true if inactive cards should be included, false otherwise
     */
    public boolean shouldIncludeInactive() {
        return includeInactive != null && includeInactive;
    }

    /**
     * Checks if request is from an admin user.
     * Convenience method for role-based access control logic.
     * 
     * @return true if user has admin role, false otherwise
     */
    public boolean isAdminUser() {
        return "ADMIN".equals(userRole);
    }

    /**
     * Checks if request is from a regular user.
     * Convenience method for role-based access control logic.
     * 
     * @return true if user has regular user role, false otherwise
     */
    public boolean isRegularUser() {
        return "USER".equals(userRole);
    }

    /**
     * Checks if request is from a manager user.
     * Convenience method for role-based access control logic.
     * 
     * @return true if user has manager role, false otherwise
     */
    public boolean isManagerUser() {
        return "MANAGER".equals(userRole);
    }

    /**
     * Creates a Spring Data Pageable object for repository queries.
     * Convenience method for integration with Spring Data JPA pagination.
     * 
     * @return Pageable object configured with request parameters
     */
    public org.springframework.data.domain.Pageable toPageable() {
        org.springframework.data.domain.Sort.Direction direction = 
            "DESC".equals(sortDirection) ? 
                org.springframework.data.domain.Sort.Direction.DESC : 
                org.springframework.data.domain.Sort.Direction.ASC;
        
        return org.springframework.data.domain.PageRequest.of(
            pageNumber != null ? pageNumber : 0,
            pageSize != null ? pageSize : DEFAULT_PAGE_SIZE,
            org.springframework.data.domain.Sort.by(direction, sortBy != null ? sortBy : "cardNumber")
        );
    }

    /**
     * Provides string representation of the request for logging and debugging.
     * Excludes sensitive information while including key identifiers.
     * 
     * @return string representation of the request
     */
    @Override
    public String toString() {
        return String.format(
            "CardListRequestDto{correlationId='%s', userId='%s', pageNumber=%d, pageSize=%d, " +
            "accountId='%s', cardNumber='%s', userRole='%s', searchCriteria='%s', " +
            "includeInactive=%s, sortBy='%s', sortDirection='%s'}",
            getCorrelationId(),
            getUserId() != null ? "[PROTECTED]" : null,
            pageNumber,
            pageSize,
            accountId != null ? "[FILTERED]" : null,
            cardNumber != null ? "[FILTERED]" : null,
            userRole,
            searchCriteria != null ? "[FILTERED]" : null,
            includeInactive,
            sortBy,
            sortDirection
        );
    }

    /**
     * Equality comparison based on correlation ID and request parameters.
     * Supports request deduplication and caching scenarios.
     * 
     * @param obj object to compare for equality
     * @return true if objects represent the same request, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        
        CardListRequestDto that = (CardListRequestDto) obj;
        return java.util.Objects.equals(pageNumber, that.pageNumber) &&
               java.util.Objects.equals(pageSize, that.pageSize) &&
               java.util.Objects.equals(accountId, that.accountId) &&
               java.util.Objects.equals(cardNumber, that.cardNumber) &&
               java.util.Objects.equals(userRole, that.userRole) &&
               java.util.Objects.equals(searchCriteria, that.searchCriteria) &&
               java.util.Objects.equals(includeInactive, that.includeInactive) &&
               java.util.Objects.equals(sortBy, that.sortBy) &&
               java.util.Objects.equals(sortDirection, that.sortDirection);
    }

    /**
     * Hash code based on correlation ID and request parameters.
     * Supports consistent hashing for collections and caching.
     * 
     * @return hash code for the request
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(
            super.hashCode(),
            pageNumber,
            pageSize,
            accountId,
            cardNumber,
            userRole,
            searchCriteria,
            includeInactive,
            sortBy,
            sortDirection
        );
    }
}