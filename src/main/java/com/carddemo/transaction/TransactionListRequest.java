package com.carddemo.transaction;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.validator.ValidCardNumber;
import com.carddemo.common.validator.ValidCCYYMMDD;
import com.carddemo.common.enums.TransactionType;
import com.carddemo.common.enums.TransactionCategory;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;

/**
 * Request DTO for transaction listing operations with comprehensive pagination, filtering, and validation capabilities.
 * 
 * This class provides structured request parameters for the transaction listing API, equivalent to the COBOL
 * COTRN00C.cbl transaction processing with modern Spring Boot REST API architecture. The implementation
 * maintains exact functional equivalence to the original CICS pseudo-conversational processing patterns
 * while providing enhanced filtering and pagination capabilities for the React frontend.
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Spring Data Pageable integration for efficient large dataset pagination</li>
 *   <li>Comprehensive filtering capabilities including date ranges, amount ranges, and text search</li>
 *   <li>Jakarta Bean Validation annotations for request parameter validation equivalent to COBOL field validation</li>
 *   <li>Cross-reference validation for account-card associations matching original business logic</li>
 *   <li>COBOL-equivalent precision for financial calculations using BigDecimal arithmetic</li>
 *   <li>JSON serialization optimized for React Material-UI component integration</li>
 * </ul>
 * 
 * <p>Pagination Support:</p>
 * The DTO implements configurable pagination matching the original COBOL program's 10-record page size
 * while supporting dynamic page sizes for responsive UI components. Page navigation preserves the
 * forward/backward navigation patterns (PF7/PF8 equivalent) through Spring Data Pageable interface.
 * 
 * <p>Filtering Capabilities:</p>
 * <ul>
 *   <li>Transaction ID filtering with numeric validation matching COBOL TRAN-ID field</li>
 *   <li>Card number filtering with Luhn algorithm validation via @ValidCardNumber</li>
 *   <li>Account ID filtering with 11-digit format validation</li>
 *   <li>Date range filtering with CCYYMMDD format validation via @ValidCCYYMMDD</li>
 *   <li>Amount range filtering with exact decimal precision using BigDecimal</li>
 *   <li>Transaction type/category filtering using validated enum values</li>
 *   <li>Text-based filtering for transaction descriptions and merchant names</li>
 * </ul>
 * 
 * <p>Validation Rules:</p>
 * All validation annotations maintain equivalent strictness to original COBOL field validation routines,
 * ensuring data integrity and business rule compliance across the modernized microservices architecture.
 * 
 * @see BaseRequestDto
 * @see TransactionType
 * @see TransactionCategory
 * @author Blitzy Platform - CardDemo Modernization Team
 * @version 1.0
 * @since 2024-01-01
 */
public class TransactionListRequest extends BaseRequestDto {

    // Pagination parameters equivalent to COBOL page navigation logic
    
    /**
     * Page number for pagination (zero-based indexing).
     * 
     * <p>Equivalent to CDEMO-CT00-PAGE-NUM in the original COBOL program.
     * Default value of 0 represents the first page of results, maintaining
     * compatibility with Spring Data Pageable conventions.</p>
     * 
     * <p>Validation ensures non-negative values to prevent invalid page requests
     * that would result in empty result sets or system errors.</p>
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Integer pageNumber = 0;
    
    /**
     * Number of records per page for pagination control.
     * 
     * <p>Default value of 10 preserves the original COBOL program's page size
     * (10 transaction rows displayed per screen). Maximum value of 100 prevents
     * excessive memory usage and maintains reasonable response times.</p>
     * 
     * <p>Configurable page sizes support responsive UI requirements while
     * maintaining performance constraints equivalent to CICS region limits.</p>
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Integer pageSize = 10;
    
    /**
     * Sort field name for ordering results.
     * 
     * <p>Default sorting by transaction date (newest first) matches the original
     * COBOL program's VSAM key sequence processing. Supported sort fields include:</p>
     * <ul>
     *   <li>transactionDate - Transaction date (default)</li>
     *   <li>transactionId - Transaction ID</li>
     *   <li>accountId - Account ID</li>
     *   <li>amount - Transaction amount</li>
     *   <li>description - Transaction description</li>
     * </ul>
     */
    private String sortBy = "transactionDate";
    
    /**
     * Sort direction for result ordering.
     * 
     * <p>Supports "ASC" (ascending) and "DESC" (descending) values with default
     * descending order to show most recent transactions first, matching user
     * expectations from the original terminal interface.</p>
     */
    @Pattern(regexp = "^(ASC|DESC)$", message = "Sort direction must be ASC or DESC")
    private String sortDirection = "DESC";
    
    // Filter parameters equivalent to COBOL search criteria
    
    /**
     * Transaction ID filter for exact match searching.
     * 
     * <p>Equivalent to TRNIDINI field in COBOL COTRN00C program. Validates
     * numeric format matching the original 16-character transaction ID pattern.
     * When provided, results are filtered to exact transaction ID matches.</p>
     * 
     * <p>Pattern validation ensures only numeric characters are accepted,
     * maintaining data integrity equivalent to COBOL PICTURE clause validation.</p>
     */
    @Pattern(regexp = "^[0-9]{1,16}$", message = "Transaction ID must be numeric, 1-16 digits")
    private String transactionId;
    
    /**
     * Card number filter with comprehensive validation.
     * 
     * <p>Uses @ValidCardNumber annotation for Luhn algorithm validation ensuring
     * credit card number integrity. Filters transactions associated with the
     * specified card number through PostgreSQL cross-reference table joins.</p>
     * 
     * <p>Validation maintains industry standards for credit card number format
     * while preserving the 16-digit numeric requirement from COBOL CARD-NUM field.</p>
     */
    @ValidCardNumber
    private String cardNumber;
    
    /**
     * Account ID filter for account-specific transaction retrieval.
     * 
     * <p>11-digit account ID format matches COBOL ACCT-ID field validation.
     * Filters transactions to show only those associated with the specified
     * account through account-transaction relationship validation.</p>
     * 
     * <p>Pattern validation ensures numeric format and exact length matching
     * the original VSAM ACCTDAT key structure requirements.</p>
     */
    @Pattern(regexp = "^[0-9]{11}$", message = "Account ID must be exactly 11 numeric digits")
    private String accountId;
    
    /**
     * Start date for date range filtering.
     * 
     * <p>Uses @ValidCCYYMMDD annotation for comprehensive date validation including
     * century restrictions (19xx/20xx), leap year calculations, and month/day
     * range validation equivalent to COBOL date validation routines.</p>
     * 
     * <p>When combined with toDate, enables date range filtering for transaction
     * queries with precise COBOL-equivalent date handling logic.</p>
     */
    @ValidCCYYMMDD
    private String fromDate;
    
    /**
     * End date for date range filtering.
     * 
     * <p>Uses @ValidCCYYMMDD annotation for comprehensive date validation.
     * Combined with fromDate to create inclusive date range filters for
     * transaction queries, supporting both single-day and multi-day searches.</p>
     * 
     * <p>Validation ensures logical date ranges where toDate >= fromDate
     * through custom validation logic in isValidDateRange() method.</p>
     */
    @ValidCCYYMMDD
    private String toDate;
    
    /**
     * Minimum transaction amount for range filtering.
     * 
     * <p>BigDecimal precision maintains exact COBOL COMP-3 decimal arithmetic
     * equivalent to TRAN-AMT field processing. Supports filtering transactions
     * with amounts greater than or equal to the specified minimum value.</p>
     * 
     * <p>Financial precision preserves the original 9.2 decimal format
     * (PIC S9(10)V99 COMP-3) ensuring identical calculation results.</p>
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private BigDecimal minAmount;
    
    /**
     * Maximum transaction amount for range filtering.
     * 
     * <p>BigDecimal precision maintains exact COBOL COMP-3 decimal arithmetic.
     * Supports filtering transactions with amounts less than or equal to the
     * specified maximum value, enabling amount range queries.</p>
     * 
     * <p>Combined with minAmount to create comprehensive amount-based filtering
     * capabilities while preserving financial calculation precision.</p>
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private BigDecimal maxAmount;
    
    /**
     * Transaction type filter using validated enum values.
     * 
     * <p>References TransactionType enum containing all valid 2-character
     * transaction type codes equivalent to COBOL TRAN-TYPE-CD field values.
     * Enables filtering by transaction type (PU, CA, PM, etc.) with full
     * validation against the reference table.</p>
     * 
     * <p>Enum validation ensures only valid transaction types are accepted,
     * maintaining referential integrity equivalent to VSAM cross-reference validation.</p>
     */
    private TransactionType transactionType;
    
    /**
     * Transaction category filter using validated enum values.
     * 
     * <p>References TransactionCategory enum containing all valid 4-digit
     * category codes equivalent to COBOL TRAN-CAT-CD field values.
     * Enables filtering by transaction category for detailed transaction analysis.</p>
     * 
     * <p>Category validation maintains exact 4-digit numeric format requirements
     * from the original COBOL program structure and reference table constraints.</p>
     */
    private TransactionCategory transactionCategory;
    
    /**
     * Transaction description filter for text-based searching.
     * 
     * <p>Supports partial text matching within transaction descriptions using
     * case-insensitive LIKE queries. Maximum length of 50 characters matches
     * COBOL TRAN-DESC field constraints from the original program.</p>
     * 
     * <p>Text validation prevents SQL injection while enabling flexible
     * description-based transaction searches for user convenience.</p>
     */
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-\\.,]{0,50}$", message = "Description must be alphanumeric with basic punctuation, max 50 characters")
    private String description;
    
    /**
     * Merchant name filter for merchant-based transaction searching.
     * 
     * <p>Enables filtering by merchant name using partial text matching.
     * Supports up to 50 characters with alphanumeric and basic punctuation
     * validation to prevent malicious input while maintaining search flexibility.</p>
     * 
     * <p>Merchant filtering provides enhanced search capabilities beyond the
     * original COBOL program while maintaining equivalent security constraints.</p>
     */
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-\\.,]{0,50}$", message = "Merchant name must be alphanumeric with basic punctuation, max 50 characters")
    private String merchantName;

    /**
     * Default constructor initializing request with pagination defaults.
     * 
     * <p>Calls BaseRequestDto constructor to initialize correlation ID and timestamp.
     * Sets default pagination parameters matching original COBOL program behavior
     * with 10 records per page and descending date order.</p>
     */
    public TransactionListRequest() {
        super();
    }

    /**
     * Constructor with correlation ID for explicit tracking scenarios.
     * 
     * <p>Used when correlation ID needs to be propagated from upstream systems
     * or when specific correlation patterns are required for distributed
     * transaction coordination across microservices.</p>
     * 
     * @param correlationId the correlation identifier for this request
     */
    public TransactionListRequest(String correlationId) {
        super(correlationId);
    }

    // Getter and setter methods for all fields

    /**
     * Gets the page number for pagination.
     * 
     * @return the zero-based page number for result pagination
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the page number for pagination.
     * 
     * @param pageNumber the zero-based page number for result pagination
     */
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber != null ? pageNumber : 0;
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
     * @param pageSize the number of records per page (max 100)
     */
    public void setPageSize(Integer pageSize) {
        if (pageSize != null && pageSize > 0 && pageSize <= 100) {
            this.pageSize = pageSize;
        } else {
            this.pageSize = 10; // Default to original COBOL page size
        }
    }

    /**
     * Gets the sort field name.
     * 
     * @return the field name for result ordering
     */
    public String getSortBy() {
        return sortBy;
    }

    /**
     * Sets the sort field name.
     * 
     * @param sortBy the field name for result ordering
     */
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy != null ? sortBy : "transactionDate";
    }

    /**
     * Gets the sort direction.
     * 
     * @return the sort direction ("ASC" or "DESC")
     */
    public String getSortDirection() {
        return sortDirection;
    }

    /**
     * Sets the sort direction.
     * 
     * @param sortDirection the sort direction ("ASC" or "DESC")
     */
    public void setSortDirection(String sortDirection) {
        if ("ASC".equals(sortDirection) || "DESC".equals(sortDirection)) {
            this.sortDirection = sortDirection;
        } else {
            this.sortDirection = "DESC"; // Default to newest first
        }
    }

    /**
     * Gets the transaction ID filter.
     * 
     * @return the transaction ID for exact match filtering
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the transaction ID filter.
     * 
     * @param transactionId the transaction ID for exact match filtering
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets the card number filter.
     * 
     * @return the card number for transaction filtering
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number filter.
     * 
     * @param cardNumber the card number for transaction filtering
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the account ID filter.
     * 
     * @return the account ID for transaction filtering
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID filter.
     * 
     * @param accountId the account ID for transaction filtering
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the start date for date range filtering.
     * 
     * @return the start date in CCYYMMDD format
     */
    public String getFromDate() {
        return fromDate;
    }

    /**
     * Sets the start date for date range filtering.
     * 
     * @param fromDate the start date in CCYYMMDD format
     */
    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    /**
     * Gets the end date for date range filtering.
     * 
     * @return the end date in CCYYMMDD format
     */
    public String getToDate() {
        return toDate;
    }

    /**
     * Sets the end date for date range filtering.
     * 
     * @param toDate the end date in CCYYMMDD format
     */
    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    /**
     * Gets the minimum amount for range filtering.
     * 
     * @return the minimum transaction amount
     */
    public BigDecimal getMinAmount() {
        return minAmount;
    }

    /**
     * Sets the minimum amount for range filtering.
     * 
     * @param minAmount the minimum transaction amount
     */
    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    /**
     * Gets the maximum amount for range filtering.
     * 
     * @return the maximum transaction amount
     */
    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    /**
     * Sets the maximum amount for range filtering.
     * 
     * @param maxAmount the maximum transaction amount
     */
    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    /**
     * Gets the transaction type filter.
     * 
     * @return the transaction type enum value
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the transaction type filter.
     * 
     * @param transactionType the transaction type enum value
     */
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the transaction category filter.
     * 
     * @return the transaction category enum value
     */
    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the transaction category filter.
     * 
     * @param transactionCategory the transaction category enum value
     */
    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    /**
     * Gets the transaction description filter.
     * 
     * @return the description text for partial matching
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the transaction description filter.
     * 
     * @param description the description text for partial matching
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the merchant name filter.
     * 
     * @return the merchant name for partial matching
     */
    public String getMerchantName() {
        return merchantName;
    }

    /**
     * Sets the merchant name filter.
     * 
     * @param merchantName the merchant name for partial matching
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    // Utility methods for pagination and filtering

    /**
     * Converts this request to Spring Data Pageable interface.
     * 
     * <p>Creates a Pageable object with the specified page number, page size,
     * and sorting configuration. This method enables seamless integration
     * with Spring Data JPA repositories for efficient pagination queries.</p>
     * 
     * <p>The returned Pageable maintains compatibility with the original
     * COBOL program's pagination behavior while leveraging Spring Data's
     * optimized database query generation capabilities.</p>
     * 
     * @return Pageable object configured with request parameters
     */
    public Pageable toPageable() {
        Sort sort = Sort.by(
            "DESC".equals(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy
        );
        
        return PageRequest.of(
            pageNumber != null ? pageNumber : 0,
            pageSize != null ? pageSize : 10,
            sort
        );
    }

    /**
     * Checks if any filter parameters are applied.
     * 
     * <p>Determines whether the request includes any filtering criteria
     * beyond basic pagination. This method enables conditional query
     * optimization where filtered queries may require different execution
     * strategies than unfiltered pagination-only queries.</p>
     * 
     * <p>Used by service layers to determine appropriate query execution
     * paths and caching strategies based on filter complexity.</p>
     * 
     * @return true if any filter parameters are specified, false otherwise
     */
    public boolean hasFilters() {
        return transactionId != null && !transactionId.trim().isEmpty() ||
               cardNumber != null && !cardNumber.trim().isEmpty() ||
               accountId != null && !accountId.trim().isEmpty() ||
               fromDate != null && !fromDate.trim().isEmpty() ||
               toDate != null && !toDate.trim().isEmpty() ||
               minAmount != null ||
               maxAmount != null ||
               transactionType != null ||
               transactionCategory != null ||
               description != null && !description.trim().isEmpty() ||
               merchantName != null && !merchantName.trim().isEmpty();
    }

    /**
     * Validates that the date range is logically consistent.
     * 
     * <p>Ensures that when both fromDate and toDate are specified, the
     * toDate is greater than or equal to fromDate. This validation
     * prevents invalid date range queries that would return empty
     * result sets or cause database query errors.</p>
     * 
     * <p>Date parsing uses CCYYMMDD format validation equivalent to
     * COBOL date handling routines, maintaining consistency with the
     * original program's date validation logic.</p>
     * 
     * @return true if date range is valid or no date range specified, false otherwise
     */
    public boolean isValidDateRange() {
        if (fromDate == null || toDate == null || 
            fromDate.trim().isEmpty() || toDate.trim().isEmpty()) {
            return true; // No range specified or partial range is valid
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate fromLocalDate = LocalDate.parse(fromDate, formatter);
            LocalDate toLocalDate = LocalDate.parse(toDate, formatter);
            
            return !toLocalDate.isBefore(fromLocalDate);
        } catch (DateTimeParseException e) {
            // Invalid date format - let field validation handle this
            return false;
        }
    }

    /**
     * Returns a string representation of the TransactionListRequest for debugging and logging.
     * 
     * <p>Provides comprehensive request details including pagination parameters,
     * filter criteria, and base request metadata. Used by logging frameworks
     * and debugging tools for transaction request analysis and troubleshooting.</p>
     * 
     * <p>Sensitive information like card numbers are partially masked to
     * maintain security while providing sufficient detail for debugging purposes.</p>
     * 
     * @return string representation including all request parameters
     */
    @Override
    public String toString() {
        return "TransactionListRequest{" +
                "pageNumber=" + pageNumber +
                ", pageSize=" + pageSize +
                ", sortBy='" + sortBy + '\'' +
                ", sortDirection='" + sortDirection + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", cardNumber='" + (cardNumber != null ? cardNumber.substring(0, 4) + "****" + cardNumber.substring(12) : null) + '\'' +
                ", accountId='" + accountId + '\'' +
                ", fromDate='" + fromDate + '\'' +
                ", toDate='" + toDate + '\'' +
                ", minAmount=" + minAmount +
                ", maxAmount=" + maxAmount +
                ", transactionType=" + transactionType +
                ", transactionCategory=" + transactionCategory +
                ", description='" + description + '\'' +
                ", merchantName='" + merchantName + '\'' +
                ", " + super.toString() +
                '}';
    }
}