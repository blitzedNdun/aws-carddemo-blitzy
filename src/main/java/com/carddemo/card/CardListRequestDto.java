package com.carddemo.card;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.validator.ValidAccountId;
import com.carddemo.common.validator.ValidCardNumber;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Request DTO for card listing operations with pagination support, role-based filtering,
 * and comprehensive input validation supporting COCRDLIC.cbl functionality through REST API contracts.
 * 
 * <p>This DTO supports the complete card listing functionality originally implemented in
 * COCRDLIC.cbl, including:
 * <ul>
 *   <li>Pagination with 7 cards per page default (matching COBOL WS-MAX-SCREEN-LINES)</li>
 *   <li>Role-based filtering for admin vs regular user access control</li>
 *   <li>Account ID filtering with 11-digit validation (CC-ACCT-ID)</li>
 *   <li>Card number filtering with 16-digit validation (CC-CARD-NUM)</li>
 *   <li>Search criteria support for flexible card lookup</li>
 *   <li>Inactive card inclusion control</li>
 *   <li>Sorting capabilities for result ordering</li>
 * </ul>
 * 
 * <p>The request structure preserves the exact functional equivalence with the original
 * COBOL implementation while providing comprehensive Jakarta Bean Validation annotations
 * for input validation and Jackson annotations for JSON serialization/deserialization.
 * 
 * <p>Security Integration:
 * This DTO integrates with Spring Security JWT authentication framework and supports
 * role-based access control where admin users can view all cards while regular users
 * can only view cards associated with their account context.
 * 
 * <p>Pagination Implementation:
 * The pagination parameters align with Spring Data Pageable conventions while maintaining
 * the 7-card-per-page display optimization from the original COBOL screen layout.
 * 
 * <p>Usage Example:
 * <pre>
 * CardListRequestDto request = new CardListRequestDto();
 * request.setPageNumber(0);
 * request.setPageSize(7);
 * request.setAccountId("12345678901");
 * request.setUserRole("ADMIN");
 * request.setIncludeInactive(false);
 * </pre>
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardListRequestDto extends BaseRequestDto {

    /**
     * Page number for pagination, zero-based index.
     * Corresponds to COBOL WS-CA-SCREEN-NUM for page tracking.
     * 
     * Default value: 0 (first page)
     * Valid range: 0 to Integer.MAX_VALUE
     */
    @JsonProperty("pageNumber")
    @NotNull(message = "Page number is required for pagination")
    @Min(value = 0, message = "Page number must be zero or greater")
    @Max(value = Integer.MAX_VALUE, message = "Page number must be within valid range")
    private Integer pageNumber = 0;

    /**
     * Page size for pagination, number of cards per page.
     * Defaults to 7 cards per page as specified in Component Details
     * and matching COBOL WS-MAX-SCREEN-LINES constant.
     * 
     * Default value: 7 (matching COBOL screen layout)
     * Valid range: 1 to 100
     */
    @JsonProperty("pageSize")
    @NotNull(message = "Page size is required for pagination")
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private Integer pageSize = 7;

    /**
     * Account ID filter for card search operations.
     * Maps to CC-ACCT-ID from COCRDLIC.cbl with 11-digit validation.
     * 
     * Format: 11-digit numeric string
     * Optional: Can be null for admin users to view all cards
     */
    @JsonProperty("accountId")
    @ValidAccountId(message = "Account ID must be exactly 11 digits")
    @Size(min = 11, max = 11, message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Card number filter for specific card lookup operations.
     * Maps to CC-CARD-NUM from COCRDLIC.cbl with 16-digit validation.
     * 
     * Format: 16-digit numeric string with Luhn algorithm validation
     * Optional: Can be null for broader card listing
     */
    @JsonProperty("cardNumber")
    @ValidCardNumber(message = "Card number must be a valid 16-digit credit card number")
    @Size(min = 16, max = 16, message = "Card number must be exactly 16 digits")
    private String cardNumber;

    /**
     * User role for authorization and access control.
     * Determines filtering scope: ADMIN can view all cards, USER restricted to own account.
     * 
     * Valid values: "ADMIN", "USER", "ROLE_ADMIN", "ROLE_USER"
     * Maps to COBOL CDEMO-USRTYP-USER vs admin user logic
     */
    @JsonProperty("userRole")
    @NotNull(message = "User role is required for access control")
    @Pattern(regexp = "^(ADMIN|USER|ROLE_ADMIN|ROLE_USER)$", 
             message = "User role must be ADMIN, USER, ROLE_ADMIN, or ROLE_USER")
    private String userRole;

    /**
     * General search criteria for flexible card lookup.
     * Supports partial matches against card number, account ID, or card status.
     * 
     * Format: Alphanumeric string with optional wildcards
     * Optional: Can be null for full listing without search filtering
     */
    @JsonProperty("searchCriteria")
    @Size(max = 50, message = "Search criteria must not exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\s\\*\\?\\-]*$", 
             message = "Search criteria must contain only alphanumeric characters, spaces, and wildcards")
    private String searchCriteria;

    /**
     * Flag to include inactive/closed cards in the results.
     * Maps to COBOL card status filtering logic in COCRDLIC.cbl.
     * 
     * Default value: false (exclude inactive cards)
     * When true: Include all cards regardless of status
     */
    @JsonProperty("includeInactive")
    @NotNull(message = "Include inactive flag is required")
    private Boolean includeInactive = false;

    /**
     * Field name for sorting results.
     * Supports sorting by card number, account ID, card status, or creation date.
     * 
     * Valid values: "cardNumber", "accountId", "cardStatus", "createdDate"
     * Default: "cardNumber" (matching COBOL key sequence)
     */
    @JsonProperty("sortBy")
    @Pattern(regexp = "^(cardNumber|accountId|cardStatus|createdDate)$", 
             message = "Sort by field must be cardNumber, accountId, cardStatus, or createdDate")
    private String sortBy = "cardNumber";

    /**
     * Sort direction for result ordering.
     * Supports ascending or descending order.
     * 
     * Valid values: "ASC", "DESC"
     * Default: "ASC" (matching COBOL browse sequence)
     */
    @JsonProperty("sortDirection")
    @Pattern(regexp = "^(ASC|DESC)$", 
             message = "Sort direction must be ASC or DESC")
    private String sortDirection = "ASC";

    /**
     * Default constructor for JSON deserialization.
     * Initializes default pagination values and sort order.
     */
    public CardListRequestDto() {
        super();
        this.pageNumber = 0;
        this.pageSize = 7;
        this.includeInactive = false;
        this.sortBy = "cardNumber";
        this.sortDirection = "ASC";
    }

    /**
     * Constructor with required fields for programmatic instantiation.
     * 
     * @param correlationId Unique correlation identifier for request tracking
     * @param userId User identifier for audit trail and authorization
     * @param sessionId Session identifier for distributed session management
     * @param userRole User role for authorization and access control
     */
    public CardListRequestDto(String correlationId, String userId, String sessionId, String userRole) {
        super(correlationId, userId, sessionId);
        this.userRole = userRole;
        this.pageNumber = 0;
        this.pageSize = 7;
        this.includeInactive = false;
        this.sortBy = "cardNumber";
        this.sortDirection = "ASC";
    }

    /**
     * Gets the page number for pagination.
     * 
     * @return the page number (zero-based index)
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the page number for pagination.
     * 
     * @param pageNumber the page number (zero-based index)
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
     * Gets the account ID filter for card search operations.
     * 
     * @return the account ID filter or null for no account filtering
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID filter for card search operations.
     * 
     * @param accountId the account ID filter
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the card number filter for specific card lookup operations.
     * 
     * @return the card number filter or null for no card number filtering
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number filter for specific card lookup operations.
     * 
     * @param cardNumber the card number filter
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the user role for authorization and access control.
     * 
     * @return the user role for access control
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Sets the user role for authorization and access control.
     * 
     * @param userRole the user role for access control
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * Gets the general search criteria for flexible card lookup.
     * 
     * @return the search criteria or null for no search filtering
     */
    public String getSearchCriteria() {
        return searchCriteria;
    }

    /**
     * Sets the general search criteria for flexible card lookup.
     * 
     * @param searchCriteria the search criteria
     */
    public void setSearchCriteria(String searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    /**
     * Gets the flag to include inactive/closed cards in the results.
     * 
     * @return true to include inactive cards, false to exclude them
     */
    public Boolean getIncludeInactive() {
        return includeInactive;
    }

    /**
     * Sets the flag to include inactive/closed cards in the results.
     * 
     * @param includeInactive true to include inactive cards, false to exclude them
     */
    public void setIncludeInactive(Boolean includeInactive) {
        this.includeInactive = includeInactive;
    }

    /**
     * Gets the field name for sorting results.
     * 
     * @return the sort field name
     */
    public String getSortBy() {
        return sortBy;
    }

    /**
     * Sets the field name for sorting results.
     * 
     * @param sortBy the sort field name
     */
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    /**
     * Gets the sort direction for result ordering.
     * 
     * @return the sort direction (ASC or DESC)
     */
    public String getSortDirection() {
        return sortDirection;
    }

    /**
     * Sets the sort direction for result ordering.
     * 
     * @param sortDirection the sort direction (ASC or DESC)
     */
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    /**
     * Validates if the request represents an admin user access pattern.
     * Admin users can view all cards across all accounts.
     * 
     * @return true if user role indicates admin access
     */
    public boolean isAdminUser() {
        return userRole != null && (userRole.equals("ADMIN") || userRole.equals("ROLE_ADMIN"));
    }

    /**
     * Validates if the request has account-specific filtering.
     * Regular users must specify an account ID for card listing.
     * 
     * @return true if account ID filter is specified
     */
    public boolean hasAccountFilter() {
        return accountId != null && !accountId.trim().isEmpty();
    }

    /**
     * Validates if the request has card number-specific filtering.
     * Used for individual card lookup operations.
     * 
     * @return true if card number filter is specified
     */
    public boolean hasCardNumberFilter() {
        return cardNumber != null && !cardNumber.trim().isEmpty();
    }

    /**
     * Validates if the request has search criteria filtering.
     * Used for flexible card search operations.
     * 
     * @return true if search criteria is specified
     */
    public boolean hasSearchCriteria() {
        return searchCriteria != null && !searchCriteria.trim().isEmpty();
    }

    /**
     * Validates the request for role-based access control compliance.
     * Regular users must specify account ID, admin users can use any filter.
     * 
     * @return true if request is valid for the specified user role
     */
    public boolean isValidForUserRole() {
        if (isAdminUser()) {
            return true; // Admin users can access all cards
        }
        
        // Regular users must specify account ID for filtering
        return hasAccountFilter();
    }

    /**
     * Creates a summary of the card listing request for audit logging.
     * This method generates a structured audit summary that supports
     * compliance requirements and security monitoring.
     * 
     * @return audit summary string
     */
    public String getRequestSummary() {
        return String.format("CardListRequest[page=%d, size=%d, accountId=%s, cardNumber=%s, role=%s, search=%s, includeInactive=%b]",
                           pageNumber, pageSize, accountId, cardNumber, userRole, searchCriteria, includeInactive);
    }

    /**
     * Equality comparison based on pagination, filtering, and user context.
     * This method supports request deduplication and correlation tracking.
     * 
     * @param obj the object to compare
     * @return true if requests are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        
        CardListRequestDto that = (CardListRequestDto) obj;
        return Objects.equals(pageNumber, that.pageNumber) &&
               Objects.equals(pageSize, that.pageSize) &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(userRole, that.userRole) &&
               Objects.equals(searchCriteria, that.searchCriteria) &&
               Objects.equals(includeInactive, that.includeInactive) &&
               Objects.equals(sortBy, that.sortBy) &&
               Objects.equals(sortDirection, that.sortDirection);
    }

    /**
     * Hash code generation based on pagination, filtering, and user context.
     * This method supports efficient collections handling and request correlation.
     * 
     * @return hash code for the request
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pageNumber, pageSize, accountId, cardNumber, 
                           userRole, searchCriteria, includeInactive, sortBy, sortDirection);
    }

    /**
     * String representation for debugging and logging purposes.
     * This method provides a comprehensive string representation that
     * supports debugging while maintaining security best practices.
     * 
     * @return string representation of the request
     */
    @Override
    public String toString() {
        return String.format("CardListRequestDto{%s, pageNumber=%d, pageSize=%d, accountId='%s', " +
                           "cardNumber='%s', userRole='%s', searchCriteria='%s', includeInactive=%b, " +
                           "sortBy='%s', sortDirection='%s'}",
                           getSanitizedSummary(), pageNumber, pageSize, accountId, 
                           cardNumber != null ? cardNumber.substring(0, 4) + "****" : null,
                           userRole, searchCriteria, includeInactive, sortBy, sortDirection);
    }
}