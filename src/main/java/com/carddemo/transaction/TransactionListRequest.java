package com.carddemo.transaction;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.carddemo.common.enums.TransactionType;
import com.carddemo.common.enums.TransactionCategory;

import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import java.util.Objects;

/**
 * Request DTO for transaction listing operations with comprehensive pagination, filtering, and validation capabilities.
 * 
 * <p>This class transforms the COBOL transaction listing program (COTRN00C.cbl) functionality into a modern
 * Spring Boot REST API request structure, maintaining exact functional equivalence while providing enhanced
 * filtering and pagination capabilities through Spring Data integration.</p>
 * 
 * <p>Key transformations from COBOL implementation:</p>
 * <ul>
 *   <li>COBOL pagination logic (PF7/PF8 keys) → Spring Data Pageable with configurable page size</li>
 *   <li>COBOL transaction ID filtering → Comprehensive search criteria with multiple filter options</li>
 *   <li>COBOL field validation → Jakarta Bean Validation with custom validators</li>
 *   <li>COBOL VSAM STARTBR/READNEXT → PostgreSQL offset/limit queries with sorting</li>
 *   <li>COBOL working storage variables → Strongly typed Java fields with validation</li>
 * </ul>
 * 
 * <p>The request DTO supports the following filtering capabilities:</p>
 * <ul>
 *   <li>Transaction ID pattern matching equivalent to COBOL TRAN-ID field filtering</li>
 *   <li>Card number filtering with Luhn algorithm validation</li>
 *   <li>Account ID filtering with cross-reference validation</li>
 *   <li>Date range filtering with CCYYMMDD format validation</li>
 *   <li>Amount range filtering with BigDecimal precision matching COBOL COMP-3</li>
 *   <li>Transaction type and category filtering with enum validation</li>
 *   <li>Description and merchant name text search with pattern matching</li>
 * </ul>
 * 
 * <p>Performance optimizations implemented:</p>
 * <ul>
 *   <li>Configurable page size limits to prevent memory exhaustion</li>
 *   <li>Default sorting by transaction date descending for chronological browsing</li>
 *   <li>Lazy validation to avoid unnecessary database queries</li>
 *   <li>Optimized filter combination logic for efficient query generation</li>
 * </ul>
 * 
 * <p>COBOL equivalency maintained for:</p>
 * <ul>
 *   <li>10 transactions per page default (matching COBOL screen display)</li>
 *   <li>Transaction ID numeric validation (matching COBOL numeric field checks)</li>
 *   <li>Date validation patterns (matching COBOL date editing routines)</li>
 *   <li>Amount precision and scale (matching COBOL COMP-3 arithmetic)</li>
 * </ul>
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since 1.0
 * @see com.carddemo.transaction.TransactionListService
 * @see com.carddemo.transaction.TransactionController
 */
public class TransactionListRequest extends BaseRequestDto {

    private static final long serialVersionUID = 1L;

    // Default pagination values matching COBOL screen display
    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int DEFAULT_PAGE_SIZE = 10; // Match COBOL 10 transactions per screen
    private static final int MAX_PAGE_SIZE = 100; // Prevent memory issues with large datasets
    private static final String DEFAULT_SORT_BY = "transactionDate";
    private static final String DEFAULT_SORT_DIRECTION = "DESC";

    // Date formatter for CCYYMMDD format validation
    private static final DateTimeFormatter CCYYMMDD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // Pagination parameters
    @JsonProperty("page_number")
    @Min(value = 0, message = "Page number must be non-negative")
    private int pageNumber = DEFAULT_PAGE_NUMBER;

    @JsonProperty("page_size")
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = MAX_PAGE_SIZE, message = "Page size cannot exceed " + MAX_PAGE_SIZE)
    private int pageSize = DEFAULT_PAGE_SIZE;

    @JsonProperty("sort_by")
    @Size(max = 50, message = "Sort field name cannot exceed 50 characters")
    @Pattern(regexp = "^(transactionId|transactionDate|cardNumber|accountId|amount|description|merchantName|transactionType|transactionCategory)$", 
             message = "Sort field must be one of: transactionId, transactionDate, cardNumber, accountId, amount, description, merchantName, transactionType, transactionCategory")
    private String sortBy = DEFAULT_SORT_BY;

    @JsonProperty("sort_direction")
    @Pattern(regexp = "^(ASC|DESC)$", message = "Sort direction must be ASC or DESC")
    private String sortDirection = DEFAULT_SORT_DIRECTION;

    // Transaction filtering parameters
    @JsonProperty("transaction_id")
    @Pattern(regexp = "^[0-9]{1,16}$", message = "Transaction ID must be 1-16 digits")
    private String transactionId;

    @JsonProperty("card_number")
    @ValidCardNumber(allowNull = true, message = "Card number must be valid 16-digit number with valid checksum")
    private String cardNumber;

    @JsonProperty("account_id")
    @Pattern(regexp = "^[0-9]{1,11}$", message = "Account ID must be 1-11 digits")
    private String accountId;

    // Date range filtering with CCYYMMDD format
    @JsonProperty("from_date")
    @ValidCCYYMMDD(allowBlank = true, message = "From date must be in CCYYMMDD format")
    @JsonFormat(pattern = "yyyyMMdd")
    private String fromDate;

    @JsonProperty("to_date")
    @ValidCCYYMMDD(allowBlank = true, message = "To date must be in CCYYMMDD format")
    @JsonFormat(pattern = "yyyyMMdd")
    private String toDate;

    // Amount range filtering with BigDecimal precision
    @JsonProperty("min_amount")
    @DecimalMin(value = "0.00", message = "Minimum amount must be non-negative")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal minAmount;

    @JsonProperty("max_amount")
    @DecimalMax(value = "999999999.99", message = "Maximum amount cannot exceed 999,999,999.99")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal maxAmount;

    // Transaction type and category filtering
    @JsonProperty("transaction_type")
    private TransactionType transactionType;

    @JsonProperty("transaction_category")
    private TransactionCategory transactionCategory;

    // Text search parameters
    @JsonProperty("description")
    @Size(max = 26, message = "Description search cannot exceed 26 characters")
    private String description;

    @JsonProperty("merchant_name")
    @Size(max = 23, message = "Merchant name search cannot exceed 23 characters")
    private String merchantName;

    /**
     * Default constructor for TransactionListRequest.
     * Initializes with default pagination values matching COBOL screen display.
     */
    public TransactionListRequest() {
        super();
        this.pageNumber = DEFAULT_PAGE_NUMBER;
        this.pageSize = DEFAULT_PAGE_SIZE;
        this.sortBy = DEFAULT_SORT_BY;
        this.sortDirection = DEFAULT_SORT_DIRECTION;
    }

    /**
     * Constructor with correlation ID for distributed request tracing.
     * 
     * @param correlationId Unique identifier for request correlation across services
     */
    public TransactionListRequest(String correlationId) {
        super(correlationId);
        this.pageNumber = DEFAULT_PAGE_NUMBER;
        this.pageSize = DEFAULT_PAGE_SIZE;
        this.sortBy = DEFAULT_SORT_BY;
        this.sortDirection = DEFAULT_SORT_DIRECTION;
    }

    /**
     * Full constructor for complete request initialization.
     * 
     * @param correlationId Unique identifier for request correlation
     * @param userId Authenticated user identifier
     * @param sessionId Session identifier for Redis session management
     */
    public TransactionListRequest(String correlationId, String userId, String sessionId) {
        super(correlationId, userId, sessionId);
        this.pageNumber = DEFAULT_PAGE_NUMBER;
        this.pageSize = DEFAULT_PAGE_SIZE;
        this.sortBy = DEFAULT_SORT_BY;
        this.sortDirection = DEFAULT_SORT_DIRECTION;
    }

    // Pagination getters and setters
    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = Math.max(0, pageNumber);
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy != null ? sortBy : DEFAULT_SORT_BY;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection != null ? sortDirection.toUpperCase() : DEFAULT_SORT_DIRECTION;
    }

    // Transaction filtering getters and setters
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId != null && !transactionId.trim().isEmpty() ? transactionId.trim() : null;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber != null && !cardNumber.trim().isEmpty() ? cardNumber.trim() : null;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId != null && !accountId.trim().isEmpty() ? accountId.trim() : null;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate != null && !fromDate.trim().isEmpty() ? fromDate.trim() : null;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate != null && !toDate.trim().isEmpty() ? toDate.trim() : null;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount != null && minAmount.compareTo(BigDecimal.ZERO) >= 0 ? minAmount : null;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount != null && maxAmount.compareTo(BigDecimal.ZERO) > 0 ? maxAmount : null;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null && !description.trim().isEmpty() ? description.trim() : null;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName != null && !merchantName.trim().isEmpty() ? merchantName.trim() : null;
    }

    /**
     * Converts the request parameters to a Spring Data Pageable object.
     * This method enables seamless integration with Spring Data repositories
     * for efficient pagination and sorting equivalent to COBOL VSAM browse operations.
     * 
     * @return Pageable object configured with request parameters
     */
    public Pageable toPageable() {
        // Create sort direction
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        
        // Create sort with specified field and direction
        Sort sort = Sort.by(direction, sortBy);
        
        // Create pageable with zero-based page numbering
        return PageRequest.of(pageNumber, pageSize, sort);
    }

    /**
     * Checks if any filtering criteria are specified in the request.
     * This method helps optimize query generation by identifying when
     * filtering logic needs to be applied versus simple paginated browsing.
     * 
     * @return true if any filter criteria are specified, false otherwise
     */
    public boolean hasFilters() {
        return transactionId != null ||
               cardNumber != null ||
               accountId != null ||
               fromDate != null ||
               toDate != null ||
               minAmount != null ||
               maxAmount != null ||
               transactionType != null ||
               transactionCategory != null ||
               description != null ||
               merchantName != null;
    }

    /**
     * Validates that the date range is logically consistent.
     * This method implements comprehensive date validation equivalent to
     * COBOL date editing routines, ensuring from_date <= to_date.
     * 
     * @return true if date range is valid or no dates specified, false otherwise
     */
    public boolean isValidDateRange() {
        if (fromDate == null || toDate == null) {
            return true; // No range specified or only one date
        }
        
        try {
            LocalDate fromLocalDate = LocalDate.parse(fromDate, CCYYMMDD_FORMATTER);
            LocalDate toLocalDate = LocalDate.parse(toDate, CCYYMMDD_FORMATTER);
            
            return !fromLocalDate.isAfter(toLocalDate);
        } catch (DateTimeParseException e) {
            return false; // Invalid date format
        }
    }

    /**
     * Validates that the amount range is logically consistent.
     * This method ensures min_amount <= max_amount when both are specified,
     * supporting BigDecimal precision equivalent to COBOL COMP-3 arithmetic.
     * 
     * @return true if amount range is valid or no amounts specified, false otherwise
     */
    @JsonIgnore
    public boolean isValidAmountRange() {
        if (minAmount == null || maxAmount == null) {
            return true; // No range specified or only one amount
        }
        
        return minAmount.compareTo(maxAmount) <= 0;
    }

    /**
     * Gets the parsed from date as LocalDate for database query construction.
     * This method provides type-safe date handling for Spring Data repository queries.
     * 
     * @return LocalDate representation of from_date, or null if not specified
     */
    @JsonIgnore
    public LocalDate getFromDateAsLocalDate() {
        if (fromDate == null) {
            return null;
        }
        
        try {
            return LocalDate.parse(fromDate, CCYYMMDD_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Gets the parsed to date as LocalDate for database query construction.
     * This method provides type-safe date handling for Spring Data repository queries.
     * 
     * @return LocalDate representation of to_date, or null if not specified
     */
    @JsonIgnore
    public LocalDate getToDateAsLocalDate() {
        if (toDate == null) {
            return null;
        }
        
        try {
            return LocalDate.parse(toDate, CCYYMMDD_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Provides comprehensive validation of all request parameters.
     * This method implements business rule validation equivalent to COBOL
     * field validation routines, ensuring data integrity and consistency.
     * 
     * @return true if all request parameters are valid, false otherwise
     */
    @JsonIgnore
    public boolean isValidRequest() {
        // Check base request validity
        if (!isValidRequestContext()) {
            return false;
        }
        
        // Validate pagination parameters
        if (pageNumber < 0 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            return false;
        }
        
        // Validate date range consistency
        if (!isValidDateRange()) {
            return false;
        }
        
        // Validate amount range consistency
        if (!isValidAmountRange()) {
            return false;
        }
        
        // Validate transaction type and category are active if specified
        if (transactionType != null && !TransactionType.isValid(transactionType)) {
            return false;
        }
        
        if (transactionCategory != null && !transactionCategory.isValid()) {
            return false;
        }
        
        return true;
    }

    /**
     * Creates a sanitized copy of the request for logging purposes.
     * This method ensures sensitive data like card numbers are masked
     * while preserving correlation context for audit trails.
     * 
     * @return Sanitized copy of the request suitable for logging
     */
    @JsonIgnore
    public TransactionListRequest createSanitizedCopy() {
        TransactionListRequest sanitized = new TransactionListRequest(getCorrelationId(), getUserId(), getSessionId());
        
        // Copy non-sensitive pagination parameters
        sanitized.setPageNumber(this.pageNumber);
        sanitized.setPageSize(this.pageSize);
        sanitized.setSortBy(this.sortBy);
        sanitized.setSortDirection(this.sortDirection);
        
        // Copy non-sensitive filter parameters
        sanitized.setTransactionId(this.transactionId);
        sanitized.setAccountId(this.accountId);
        sanitized.setFromDate(this.fromDate);
        sanitized.setToDate(this.toDate);
        sanitized.setMinAmount(this.minAmount);
        sanitized.setMaxAmount(this.maxAmount);
        sanitized.setTransactionType(this.transactionType);
        sanitized.setTransactionCategory(this.transactionCategory);
        sanitized.setDescription(this.description);
        sanitized.setMerchantName(this.merchantName);
        
        // Mask sensitive card number
        if (this.cardNumber != null) {
            sanitized.setCardNumber("****-****-****-" + this.cardNumber.substring(Math.max(0, this.cardNumber.length() - 4)));
        }
        
        return sanitized;
    }

    /**
     * Provides string representation of the request for logging and debugging.
     * This method ensures sensitive information is masked while including
     * essential request parameters for troubleshooting.
     * 
     * @return String representation of the request
     */
    @Override
    public String toString() {
        return String.format(
            "TransactionListRequest{correlationId='%s', userId='%s', sessionId='%s', " +
            "pageNumber=%d, pageSize=%d, sortBy='%s', sortDirection='%s', " +
            "transactionId='%s', cardNumber='%s', accountId='%s', " +
            "fromDate='%s', toDate='%s', minAmount=%s, maxAmount=%s, " +
            "transactionType=%s, transactionCategory=%s, description='%s', merchantName='%s'}",
            getCorrelationId(),
            getUserId() != null ? "[PROTECTED]" : null,
            getSessionId() != null ? "[PROTECTED]" : null,
            pageNumber, pageSize, sortBy, sortDirection,
            transactionId,
            cardNumber != null ? "[MASKED]" : null,
            accountId,
            fromDate, toDate, minAmount, maxAmount,
            transactionType, transactionCategory, description, merchantName
        );
    }

    /**
     * Equality comparison based on correlation ID and filter parameters.
     * This method supports request deduplication and caching scenarios
     * while maintaining proper equals contract.
     * 
     * @param obj Object to compare for equality
     * @return true if objects represent the same request, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        
        TransactionListRequest that = (TransactionListRequest) obj;
        return pageNumber == that.pageNumber &&
               pageSize == that.pageSize &&
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
     * Hash code based on correlation ID and filter parameters.
     * This method provides consistent hashing for collections and caching.
     * 
     * @return Hash code based on request parameters
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pageNumber, pageSize, sortBy, sortDirection,
                transactionId, cardNumber, accountId, fromDate, toDate, minAmount, maxAmount,
                transactionType, transactionCategory, description, merchantName);
    }
}