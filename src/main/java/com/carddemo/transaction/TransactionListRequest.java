package com.carddemo.transaction;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.enums.TransactionCategory;
import com.carddemo.common.enums.TransactionType;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Request DTO for transaction listing operations with comprehensive pagination, filtering, and validation capabilities.
 * 
 * <p>This class implements the transaction listing request structure equivalent to COBOL CICS map input processing
 * from the COTRN00C.cbl program, providing comprehensive pagination and filtering capabilities for transaction
 * history retrieval operations in the modern Spring Boot microservices architecture.
 * 
 * <p>Key Features:
 * <ul>
 *   <li>Comprehensive pagination support with configurable page size and sorting options</li>
 *   <li>Advanced filtering capabilities including date ranges, amount ranges, and text search</li>
 *   <li>Jakarta Bean Validation annotations for input validation equivalent to COBOL field validation</li>
 *   <li>Spring Data Pageable integration for efficient large dataset pagination</li>
 *   <li>Request parameter mapping equivalent to COBOL working storage variables</li>
 *   <li>Transaction type and category filtering using enumeration validation</li>
 *   <li>Card number and account ID validation with proper format checking</li>
 *   <li>Date range validation with CCYYMMDD format support</li>
 *   <li>BigDecimal amount filtering for precise financial calculations</li>
 * </ul>
 * 
 * <p>COBOL Source Mapping:
 * This DTO maps to the following COBOL COTRN00C.cbl working storage variables:
 * <ul>
 *   <li>WS-PAGE-NUM → pageNumber for pagination control</li>
 *   <li>WS-REC-COUNT → pageSize for record count per page</li>
 *   <li>TRNIDINI → transactionId for transaction ID filtering</li>
 *   <li>CDEMO-CT00-TRNID-FIRST → startTransactionId for range start</li>
 *   <li>CDEMO-CT00-TRNID-LAST → endTransactionId for range end</li>
 *   <li>CDEMO-CT00-PAGE-NUM → pagination tracking</li>
 *   <li>CDEMO-CT00-NEXT-PAGE-FLG → hasNextPage indicator</li>
 * </ul>
 * 
 * <p>Performance Characteristics:
 * <ul>
 *   <li>Supports efficient pagination for large transaction datasets (100,000+ records)</li>
 *   <li>Enables database query optimization through proper indexing on filter fields</li>
 *   <li>Provides configurable sort options for optimal user experience</li>
 *   <li>Implements validation rules that prevent inefficient database queries</li>
 * </ul>
 * 
 * <p>Usage Example:
 * <pre>
 * TransactionListRequest request = new TransactionListRequest();
 * request.setPageNumber(0);
 * request.setPageSize(10);
 * request.setAccountId("12345678901");
 * request.setFromDate(LocalDate.of(2024, 1, 1));
 * request.setToDate(LocalDate.of(2024, 12, 31));
 * request.setTransactionType(TransactionType.PURCHASE);
 * request.setSortBy("transactionTimestamp");
 * request.setSortDirection("DESC");
 * 
 * Pageable pageable = request.toPageable();
 * boolean hasFilters = request.hasFilters();
 * boolean validRange = request.isValidDateRange();
 * </pre>
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public class TransactionListRequest extends BaseRequestDto {

    /**
     * Page number for pagination (0-based index).
     * Maps to COBOL WS-PAGE-NUM for pagination control.
     * Default value: 0 (first page)
     * Range: 0 to Integer.MAX_VALUE
     */
    private Integer pageNumber = 0;

    /**
     * Number of records per page.
     * Maps to COBOL WS-REC-COUNT for record count per page.
     * Default value: 10 records per page
     * Range: 1 to 100 records per page
     */
    private Integer pageSize = 10;

    /**
     * Field name for sorting results.
     * Default value: "transactionTimestamp" for chronological ordering
     * Valid values: Any field name from the Transaction entity
     */
    private String sortBy = "transactionTimestamp";

    /**
     * Sort direction (ASC or DESC).
     * Default value: "DESC" for most recent transactions first
     * Valid values: "ASC", "DESC"
     */
    private String sortDirection = "DESC";

    /**
     * Transaction ID filter for exact match searches.
     * Maps to COBOL TRNIDINI field for transaction ID filtering.
     * Format: 16-character alphanumeric transaction identifier
     */
    @Pattern(regexp = "^[A-Za-z0-9]{16}$|^$", 
             message = "Transaction ID must be exactly 16 alphanumeric characters")
    private String transactionId;

    /**
     * Credit card number filter for card-specific transaction searches.
     * Validates using Luhn algorithm for proper credit card number format.
     * Format: 16-digit credit card number
     */
    @ValidCardNumber(allowNullOrEmpty = true, 
                    message = "Card number must be a valid 16-digit credit card number")
    private String cardNumber;

    /**
     * Account ID filter for account-specific transaction searches.
     * Maps to COBOL account ID field for account-based filtering.
     * Format: 11-digit account identifier
     */
    @Pattern(regexp = "^\\d{11}$|^$", 
             message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Start date for date range filtering (inclusive).
     * Validates CCYYMMDD format with century validation (19xx, 20xx).
     * Format: CCYYMMDD (8-character date format)
     */
    @ValidCCYYMMDD(allowNull = true, allowBlank = true,
                   fieldName = "From Date",
                   message = "From date must be in CCYYMMDD format with valid century (19xx or 20xx)")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;

    /**
     * End date for date range filtering (inclusive).
     * Validates CCYYMMDD format with century validation (19xx, 20xx).
     * Format: CCYYMMDD (8-character date format)
     */
    @ValidCCYYMMDD(allowNull = true, allowBlank = true,
                   fieldName = "To Date", 
                   message = "To date must be in CCYYMMDD format with valid century (19xx or 20xx)")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;

    /**
     * Minimum transaction amount filter for amount range searches.
     * Uses BigDecimal for precise financial calculations equivalent to COBOL COMP-3.
     * Format: DECIMAL(12,2) with up to 10 integer digits and 2 decimal places
     */
    private BigDecimal minAmount;

    /**
     * Maximum transaction amount filter for amount range searches.
     * Uses BigDecimal for precise financial calculations equivalent to COBOL COMP-3.
     * Format: DECIMAL(12,2) with up to 10 integer digits and 2 decimal places
     */
    private BigDecimal maxAmount;

    /**
     * Transaction type filter for type-specific searches.
     * Uses TransactionType enumeration for validation.
     * Valid values: Enumeration values from TransactionType
     */
    private TransactionType transactionType;

    /**
     * Transaction category filter for category-specific searches.
     * Uses TransactionCategory enumeration for validation.
     * Valid values: Enumeration values from TransactionCategory
     */
    private TransactionCategory transactionCategory;

    /**
     * Description text filter for partial text matching.
     * Supports case-insensitive substring matching in transaction descriptions.
     * Format: Alphanumeric text with spaces and common punctuation
     */
    @Pattern(regexp = "^[A-Za-z0-9\\s\\.,\\-_]*$", 
             message = "Description must contain only alphanumeric characters, spaces, and common punctuation")
    private String description;

    /**
     * Merchant name filter for merchant-specific searches.
     * Supports case-insensitive substring matching in merchant names.
     * Format: Alphanumeric text with spaces and common punctuation
     */
    @Pattern(regexp = "^[A-Za-z0-9\\s\\.,\\-_]*$", 
             message = "Merchant name must contain only alphanumeric characters, spaces, and common punctuation")
    private String merchantName;

    /**
     * Default constructor for JSON deserialization and framework usage.
     * Initializes default values for pagination and sorting parameters.
     */
    public TransactionListRequest() {
        super();
    }

    /**
     * Constructor with required audit fields from BaseRequestDto.
     * 
     * @param correlationId Unique correlation identifier for request tracking
     * @param userId User identifier for audit trail and authorization
     * @param sessionId Session identifier for distributed session management
     */
    public TransactionListRequest(String correlationId, String userId, String sessionId) {
        super(correlationId, userId, sessionId);
    }

    // Getter and Setter methods for all fields

    /**
     * Gets the page number for pagination.
     * 
     * @return the page number (0-based index)
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the page number for pagination.
     * 
     * @param pageNumber the page number (0-based index)
     */
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * Gets the page size for pagination.
     * 
     * @return the number of records per page
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Sets the page size for pagination.
     * 
     * @param pageSize the number of records per page (1-100)
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
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
     * Gets the transaction ID filter.
     * 
     * @return the transaction ID filter
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the transaction ID filter.
     * 
     * @param transactionId the transaction ID filter
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets the card number filter.
     * 
     * @return the card number filter
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number filter.
     * 
     * @param cardNumber the card number filter
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the account ID filter.
     * 
     * @return the account ID filter
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID filter.
     * 
     * @param accountId the account ID filter
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the from date for date range filtering.
     * 
     * @return the from date
     */
    public LocalDate getFromDate() {
        return fromDate;
    }

    /**
     * Sets the from date for date range filtering.
     * 
     * @param fromDate the from date
     */
    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    /**
     * Gets the to date for date range filtering.
     * 
     * @return the to date
     */
    public LocalDate getToDate() {
        return toDate;
    }

    /**
     * Sets the to date for date range filtering.
     * 
     * @param toDate the to date
     */
    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    /**
     * Gets the minimum amount filter.
     * 
     * @return the minimum amount
     */
    public BigDecimal getMinAmount() {
        return minAmount;
    }

    /**
     * Sets the minimum amount filter.
     * 
     * @param minAmount the minimum amount
     */
    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    /**
     * Gets the maximum amount filter.
     * 
     * @return the maximum amount
     */
    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    /**
     * Sets the maximum amount filter.
     * 
     * @param maxAmount the maximum amount
     */
    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    /**
     * Gets the transaction type filter.
     * 
     * @return the transaction type filter
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the transaction type filter.
     * 
     * @param transactionType the transaction type filter
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the transaction category filter.
     * 
     * @return the transaction category filter
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the transaction category filter.
     * 
     * @param transactionCategory the transaction category filter
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    /**
     * Gets the description filter.
     * 
     * @return the description filter
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description filter.
     * 
     * @param description the description filter
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the merchant name filter.
     * 
     * @return the merchant name filter
     */
    public String getMerchantName() {
        return merchantName;
    }

    /**
     * Sets the merchant name filter.
     * 
     * @param merchantName the merchant name filter
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    // Utility methods

    /**
     * Converts the request parameters to a Spring Data Pageable object.
     * 
     * <p>This method creates a Pageable instance with the specified page number, page size,
     * and sorting configuration. The sorting is applied based on the sortBy field and
     * sortDirection parameters, providing efficient database pagination support.
     * 
     * <p>Default sorting behavior:
     * <ul>
     *   <li>Sort by: transactionTimestamp (chronological ordering)</li>
     *   <li>Sort direction: DESC (most recent first)</li>
     *   <li>Page size: 10 records per page</li>
     * </ul>
     * 
     * @return Pageable instance configured with request parameters
     */
    public Pageable toPageable() {
        // Validate and set default values
        int page = (pageNumber != null && pageNumber >= 0) ? pageNumber : 0;
        int size = (pageSize != null && pageSize > 0 && pageSize <= 100) ? pageSize : 10;
        String sortField = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy : "transactionTimestamp";
        
        // Determine sort direction
        Sort.Direction direction = Sort.Direction.DESC;
        if (sortDirection != null && "ASC".equalsIgnoreCase(sortDirection.trim())) {
            direction = Sort.Direction.ASC;
        }
        
        // Create sort configuration
        Sort sort = Sort.by(direction, sortField);
        
        // Return configured Pageable instance
        return PageRequest.of(page, size, sort);
    }

    /**
     * Checks if the request has any active filters applied.
     * 
     * <p>This method determines whether any filtering criteria have been specified
     * in the request, which can be used to optimize database queries and provide
     * appropriate user feedback about active filters.
     * 
     * <p>Checked filter fields:
     * <ul>
     *   <li>Transaction ID</li>
     *   <li>Card number</li>
     *   <li>Account ID</li>
     *   <li>Date range (from/to dates)</li>
     *   <li>Amount range (min/max amounts)</li>
     *   <li>Transaction type</li>
     *   <li>Transaction category</li>
     *   <li>Description text</li>
     *   <li>Merchant name</li>
     * </ul>
     * 
     * @return true if any filters are active, false otherwise
     */
    public boolean hasFilters() {
        return (transactionId != null && !transactionId.trim().isEmpty()) ||
               (cardNumber != null && !cardNumber.trim().isEmpty()) ||
               (accountId != null && !accountId.trim().isEmpty()) ||
               (fromDate != null) ||
               (toDate != null) ||
               (minAmount != null) ||
               (maxAmount != null) ||
               (transactionType != null) ||
               (transactionCategory != null) ||
               (description != null && !description.trim().isEmpty()) ||
               (merchantName != null && !merchantName.trim().isEmpty());
    }

    /**
     * Validates the date range filter for logical consistency.
     * 
     * <p>This method checks that the date range filter is logically valid:
     * <ul>
     *   <li>If both fromDate and toDate are specified, fromDate must be before or equal to toDate</li>
     *   <li>If only one date is specified, the range is considered valid</li>
     *   <li>If neither date is specified, the range is considered valid</li>
     * </ul>
     * 
     * <p>This validation prevents database queries with impossible date ranges
     * and provides immediate feedback to users about invalid filter combinations.
     * 
     * @return true if the date range is valid, false otherwise
     */
    public boolean isValidDateRange() {
        if (fromDate != null && toDate != null) {
            return !fromDate.isAfter(toDate);
        }
        return true;
    }

    /**
     * Validates the amount range filter for logical consistency.
     * 
     * <p>This method checks that the amount range filter is logically valid:
     * <ul>
     *   <li>If both minAmount and maxAmount are specified, minAmount must be less than or equal to maxAmount</li>
     *   <li>If only one amount is specified, the range is considered valid</li>
     *   <li>If neither amount is specified, the range is considered valid</li>
     *   <li>Negative amounts are allowed for refunds and credit transactions</li>
     * </ul>
     * 
     * @return true if the amount range is valid, false otherwise
     */
    public boolean isValidAmountRange() {
        if (minAmount != null && maxAmount != null) {
            return minAmount.compareTo(maxAmount) <= 0;
        }
        return true;
    }

    /**
     * Validates all range filters for logical consistency.
     * 
     * <p>This method performs comprehensive validation of all range-based filters
     * to ensure they are logically consistent and will produce meaningful results.
     * 
     * @return true if all ranges are valid, false otherwise
     */
    public boolean isValidRanges() {
        return isValidDateRange() && isValidAmountRange();
    }

    /**
     * Equality comparison based on all filter fields and pagination parameters.
     * 
     * <p>This method provides comprehensive equality checking for request objects,
     * supporting request deduplication and caching scenarios in the microservices
     * architecture.
     * 
     * @param obj the object to compare
     * @return true if requests are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        
        TransactionListRequest that = (TransactionListRequest) obj;
        return Objects.equals(pageNumber, that.pageNumber) &&
               Objects.equals(pageSize, that.pageSize) &&
               Objects.equals(sortBy, that.sortBy) &&
               Objects.equals(sortDirection, that.sortDirection) &&
               Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(fromDate, that.fromDate) &&
               Objects.equals(toDate, that.toDate) &&
               Objects.equals(minAmount, that.minAmount) &&
               Objects.equals(maxAmount, that.maxAmount) &&
               Objects.equals(transactionType, that.transactionType) &&
               Objects.equals(transactionCategory, that.transactionCategory) &&
               Objects.equals(description, that.description) &&
               Objects.equals(merchantName, that.merchantName);
    }

    /**
     * Hash code generation based on all filter fields and pagination parameters.
     * 
     * <p>This method supports efficient collections handling and request
     * correlation tracking in distributed systems.
     * 
     * @return hash code for the request
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pageNumber, pageSize, sortBy, sortDirection,
                          transactionId, cardNumber, accountId, fromDate, toDate,
                          minAmount, maxAmount, transactionType, transactionCategory,
                          description, merchantName);
    }

    /**
     * String representation for debugging and logging purposes.
     * 
     * <p>This method provides a comprehensive string representation that includes
     * all filter parameters and pagination settings while maintaining security
     * best practices by not exposing sensitive data.
     * 
     * @return string representation of the request
     */
    @Override
    public String toString() {
        return String.format(
            "TransactionListRequest{" +
            "pageNumber=%d, pageSize=%d, sortBy='%s', sortDirection='%s', " +
            "transactionId='%s', cardNumber='%s', accountId='%s', " +
            "fromDate=%s, toDate=%s, minAmount=%s, maxAmount=%s, " +
            "transactionType=%s, transactionCategory=%s, " +
            "description='%s', merchantName='%s', " +
            "hasFilters=%b, isValidDateRange=%b, correlationId='%s', userId='%s'}",
            pageNumber, pageSize, sortBy, sortDirection,
            transactionId, 
            cardNumber != null ? cardNumber.substring(0, 4) + "************" : null,
            accountId,
            fromDate, toDate, minAmount, maxAmount,
            transactionType, transactionCategory,
            description, merchantName,
            hasFilters(), isValidDateRange(),
            getCorrelationId(), getUserId()
        );
    }
}