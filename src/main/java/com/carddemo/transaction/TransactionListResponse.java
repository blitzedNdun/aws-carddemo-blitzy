/*
 * Copyright (c) 2024 CardDemo Application
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.transaction;

import com.carddemo.common.dto.BaseResponseDto;
import com.carddemo.common.dto.PaginationMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

/**
 * Response DTO for transaction listing operations providing paginated results
 * with comprehensive metadata and formatted transaction data.
 * 
 * This response DTO maintains complete compatibility with the original COBOL 
 * COTRN00C.cbl transaction listing program, supporting identical pagination 
 * patterns with up to 10 transactions per page and comprehensive navigation 
 * metadata for PF7/PF8 (page up/down) functionality.
 * 
 * The response structure preserves the exact COMMAREA-equivalent status and 
 * message patterns from the legacy system while providing modern JSON API 
 * response format for React frontend consumption with Material-UI pagination
 * components.
 * 
 * Key Features:
 * - Paginated transaction data with Spring Data Page<T> integration
 * - Financial data precision preservation using BigDecimal arithmetic
 * - Comprehensive pagination metadata for UI navigation components
 * - Search criteria tracking for subsequent page requests  
 * - COBOL-equivalent status and error message handling
 * - JSON serialization optimized for React frontend consumption
 * 
 * Field Mappings from COBOL COTRN00C pagination logic:
 * - WS-PAGE-NUM → paginationMetadata.currentPage
 * - CDEMO-CT00-TRNID-FIRST/LAST → searchCriteria boundary tracking
 * - CDEMO-CT00-NEXT-PAGE-FLG → paginationMetadata.hasNextPage
 * - WS-REC-COUNT → transactionCount
 * - Transaction display array (TRNID01I-TRNID10I) → transactions list
 * 
 * @author Blitzy Agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 1.0
 */
public class TransactionListResponse extends BaseResponseDto {

    /**
     * List of transactions in the current page.
     * 
     * Contains transaction data formatted for display, maintaining exact field
     * correspondence with the original COBOL screen layout. Each transaction
     * preserves the TRAN-RECORD structure with proper BigDecimal precision
     * for amounts and LocalDateTime formatting for timestamps.
     */
    @JsonProperty("transactions")
    @Valid
    private List<TransactionDTO> transactions;

    /**
     * Comprehensive pagination metadata including current page information,
     * total counts, and navigation state indicators.
     * 
     * Provides complete pagination context equivalent to the COBOL pagination
     * logic in COTRN00C.cbl, supporting Material-UI pagination components
     * with proper page boundary detection and navigation state management.
     */
    @JsonProperty("pagination_metadata")
    @NotNull(message = "Pagination metadata is required")
    @Valid
    private PaginationMetadata paginationMetadata;

    /**
     * Total amount of all transactions in the current result set.
     * 
     * Calculated sum using exact BigDecimal arithmetic to maintain COBOL COMP-3
     * precision equivalent to the original transaction amount calculations.
     * Provides financial summary information for display purposes.
     */
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    /**
     * Search criteria used to generate this transaction listing.
     * 
     * Contains the filter parameters and sort criteria applied to generate
     * the current page of results. Used for maintaining search context
     * across paginated requests and supporting "next page" operations
     * equivalent to COBOL CDEMO-CT00-TRNID-FIRST/LAST boundary tracking.
     */
    @JsonProperty("search_criteria")
    private String searchCriteria;

    /**
     * Timestamp when this response was generated.
     * 
     * Records the exact moment when the transaction listing was prepared,
     * supporting audit trail requirements and cache validation. Complements
     * the inherited timestamp from BaseResponseDto with listing-specific
     * generation time tracking.
     */
    @JsonProperty("generated_timestamp")
    private LocalDateTime generatedTimestamp;

    /**
     * Default constructor initializing response with empty transaction list
     * and default pagination metadata.
     * 
     * Sets up the response structure with safe defaults, automatically 
     * initializing the generation timestamp to current system time for
     * accurate audit trail information.
     */
    public TransactionListResponse() {
        super();
        this.transactions = List.of();
        this.paginationMetadata = new PaginationMetadata();
        this.totalAmount = BigDecimal.ZERO;
        this.generatedTimestamp = LocalDateTime.now();
    }

    /**
     * Constructor for successful transaction listing response with correlation ID.
     * 
     * Creates a successful response with specified correlation ID for distributed
     * tracing support. Initializes with empty transaction list and default
     * pagination metadata, ready for population with actual data.
     * 
     * @param correlationId Unique identifier for request correlation and tracing
     */
    public TransactionListResponse(String correlationId) {
        super(correlationId);
        this.transactions = List.of();
        this.paginationMetadata = new PaginationMetadata();
        this.totalAmount = BigDecimal.ZERO;
        this.generatedTimestamp = LocalDateTime.now();
    }

    /**
     * Constructor for error response with detailed error information.
     * 
     * Creates an error response equivalent to COBOL error message handling
     * in COTRN00C.cbl, providing comprehensive error context for client
     * applications and audit systems while maintaining correlation tracking.
     * 
     * @param errorMessage Descriptive error message for client consumption
     * @param correlationId Unique identifier for request correlation and tracing
     */
    public TransactionListResponse(String errorMessage, String correlationId) {
        super(errorMessage, correlationId);
        this.transactions = List.of();
        this.paginationMetadata = new PaginationMetadata();
        this.totalAmount = BigDecimal.ZERO;
        this.generatedTimestamp = LocalDateTime.now();
    }

    /**
     * Factory method for creating successful response from Spring Data Page.
     * 
     * Converts Spring Data Page<TransactionDTO> to TransactionListResponse
     * with complete pagination metadata extraction. Calculates total amount
     * using BigDecimal precision and preserves all pagination state for
     * client-side navigation component support.
     * 
     * @param page Spring Data Page containing transaction data and metadata
     * @param correlationId Unique identifier for request correlation
     * @return TransactionListResponse configured with page data
     */
    public static TransactionListResponse fromPage(Page<TransactionDTO> page, String correlationId) {
        TransactionListResponse response = new TransactionListResponse(correlationId);
        
        // Extract transaction data from page content
        response.setTransactions(page.getContent());
        
        // Create pagination metadata from Spring Data Page
        PaginationMetadata pagination = new PaginationMetadata(
            page.getNumber() + 1, // Convert 0-based to 1-based page numbering
            page.getTotalPages(),
            page.getTotalElements(),
            page.getSize()
        );
        response.setPaginationMetadata(pagination);
        
        // Calculate total amount with BigDecimal precision
        BigDecimal totalAmount = page.getContent().stream()
            .map(TransactionDTO::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalAmount(totalAmount);
        
        return response;
    }

    /**
     * Factory method for creating error response with correlation tracking.
     * 
     * Provides fluent API for creating error response instances equivalent
     * to COBOL error handling patterns in COTRN00C.cbl. Ensures consistent
     * error response structure across all transaction listing operations.
     * 
     * @param errorMessage Descriptive error message
     * @param correlationId Unique identifier for request correlation
     * @return TransactionListResponse configured for error
     */
    public static TransactionListResponse error(String errorMessage, String correlationId) {
        return new TransactionListResponse(errorMessage, correlationId);
    }

    /**
     * Gets the list of transactions in the current page.
     * 
     * @return List of TransactionDTO objects for the current page
     */
    public List<TransactionDTO> getTransactions() {
        return transactions;
    }

    /**
     * Sets the list of transactions for the current page.
     * 
     * @param transactions List of TransactionDTO objects to display
     */
    public void setTransactions(List<TransactionDTO> transactions) {
        this.transactions = transactions != null ? transactions : List.of();
    }

    /**
     * Gets the pagination metadata for the transaction listing.
     * 
     * @return PaginationMetadata containing page navigation information
     */
    public PaginationMetadata getPaginationMetadata() {
        return paginationMetadata;
    }

    /**
     * Sets the pagination metadata for the transaction listing.
     * 
     * @param paginationMetadata Pagination information for the current result set
     */
    public void setPaginationMetadata(PaginationMetadata paginationMetadata) {
        this.paginationMetadata = paginationMetadata != null ? paginationMetadata : new PaginationMetadata();
    }

    /**
     * Gets the total amount of all transactions in the result set.
     * 
     * @return BigDecimal representing the sum of all transaction amounts
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /**
     * Sets the total amount of all transactions in the result set.
     * 
     * @param totalAmount BigDecimal sum of transaction amounts with COBOL precision
     */
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    /**
     * Gets the search criteria used to generate this listing.
     * 
     * @return String representation of search filters and parameters
     */
    public String getSearchCriteria() {
        return searchCriteria;
    }

    /**
     * Sets the search criteria used to generate this listing.
     * 
     * @param searchCriteria Filter parameters applied to the transaction query
     */
    public void setSearchCriteria(String searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    /**
     * Gets the timestamp when this response was generated.
     * 
     * @return LocalDateTime representing response generation time
     */
    public LocalDateTime getGeneratedTimestamp() {
        return generatedTimestamp;
    }

    /**
     * Sets the timestamp when this response was generated.
     * 
     * @param generatedTimestamp LocalDateTime for response creation time
     */
    public void setGeneratedTimestamp(LocalDateTime generatedTimestamp) {
        this.generatedTimestamp = generatedTimestamp;
    }

    /**
     * Convenience method to check if the response contains any transactions.
     * 
     * Provides boolean check for empty result sets, useful for client-side
     * rendering logic and conditional display of "no transactions found" messages.
     * 
     * @return true if transactions list contains data, false otherwise
     */
    public boolean hasTransactions() {
        return transactions != null && !transactions.isEmpty();
    }

    /**
     * Gets the count of transactions in the current page.
     * 
     * Returns the actual number of transactions in the current page response,
     * which may be less than the configured page size for the last page or
     * when total results are fewer than page size.
     * 
     * @return int count of transactions in current page
     */
    public int getTransactionCount() {
        return transactions != null ? transactions.size() : 0;
    }

    /**
     * Convenience method to check if the response is empty (no transactions).
     * 
     * Provides boolean check for empty result sets, equivalent to checking
     * both success status and transaction list emptiness for comprehensive
     * empty state detection.
     * 
     * @return true if response is successful but contains no transactions
     */
    public boolean isEmpty() {
        return isSuccess() && (transactions == null || transactions.isEmpty());
    }

    /**
     * Adds a single transaction to the current page.
     * 
     * Utility method for building transaction lists incrementally, maintaining
     * immutable list behavior while providing convenient single-item addition.
     * Updates total amount calculation automatically when transactions are added.
     * 
     * @param transaction TransactionDTO to add to the current page
     */
    public void addTransaction(TransactionDTO transaction) {
        if (transaction != null) {
            if (this.transactions.isEmpty()) {
                this.transactions = List.of(transaction);
            } else {
                // Create new list with added transaction
                var updatedList = new java.util.ArrayList<>(this.transactions);
                updatedList.add(transaction);
                this.transactions = updatedList;
            }
            
            // Update total amount with BigDecimal precision
            if (transaction.getAmount() != null) {
                this.totalAmount = this.totalAmount.add(transaction.getAmount());
            }
        }
    }

    /**
     * Validates that the response contains consistent pagination and transaction data.
     * 
     * Performs comprehensive validation of the response structure including:
     * - Pagination metadata consistency with transaction count
     * - Total amount calculation accuracy
     * - Required field presence validation
     * - Data integrity checks equivalent to COBOL validation patterns
     * 
     * @return true if response structure is valid and consistent
     */
    public boolean isValidResponse() {
        // Check basic response validity from parent class
        if (!isSuccess() && getErrorMessage() == null) {
            return false;
        }
        
        // Validate pagination metadata presence
        if (paginationMetadata == null) {
            return false;
        }
        
        // Validate transaction count consistency
        int actualCount = getTransactionCount();
        if (actualCount > paginationMetadata.getPageSize()) {
            return false;
        }
        
        // Validate total amount calculation if transactions exist
        if (hasTransactions() && totalAmount != null) {
            BigDecimal calculatedTotal = transactions.stream()
                .map(TransactionDTO::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (totalAmount.compareTo(calculatedTotal) != 0) {
                return false;
            }
        }
        
        // Validate timestamp presence
        return generatedTimestamp != null;
    }

    /**
     * Creates a summary string for logging and debugging purposes.
     * 
     * Provides comprehensive string representation including transaction count,
     * pagination state, total amount, and key metadata for operational logging
     * and debugging scenarios. Formats information in human-readable format
     * while preserving correlation IDs for distributed tracing.
     * 
     * @return formatted string representation of the response
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TransactionListResponse{");
        sb.append("success=").append(isSuccess());
        sb.append(", transactionCount=").append(getTransactionCount());
        
        if (paginationMetadata != null) {
            sb.append(", currentPage=").append(paginationMetadata.getCurrentPage());
            sb.append(", totalPages=").append(paginationMetadata.getTotalPages());
            sb.append(", totalRecords=").append(paginationMetadata.getTotalRecords());
        }
        
        if (totalAmount != null) {
            sb.append(", totalAmount=").append(totalAmount);
        }
        
        if (searchCriteria != null) {
            sb.append(", searchCriteria='").append(searchCriteria).append('\'');
        }
        
        if (getErrorMessage() != null) {
            sb.append(", errorMessage='").append(getErrorMessage()).append('\'');
        }
        
        if (getCorrelationId() != null) {
            sb.append(", correlationId='").append(getCorrelationId()).append('\'');
        }
        
        if (generatedTimestamp != null) {
            sb.append(", generatedTimestamp=").append(generatedTimestamp);
        }
        
        sb.append('}');
        return sb.toString();
    }

    /**
     * Equals method for proper comparison of transaction list response objects.
     * 
     * Implements comprehensive equality comparison based on all response fields
     * including transaction data, pagination metadata, and inherited base fields.
     * Essential for testing scenarios and caching validation in microservices.
     * 
     * @param obj object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        
        TransactionListResponse that = (TransactionListResponse) obj;
        
        if (transactions != null ? !transactions.equals(that.transactions) : that.transactions != null) return false;
        if (paginationMetadata != null ? !paginationMetadata.equals(that.paginationMetadata) : that.paginationMetadata != null) return false;
        if (totalAmount != null ? totalAmount.compareTo(that.totalAmount) != 0 : that.totalAmount != null) return false;
        if (searchCriteria != null ? !searchCriteria.equals(that.searchCriteria) : that.searchCriteria != null) return false;
        return generatedTimestamp != null ? generatedTimestamp.equals(that.generatedTimestamp) : that.generatedTimestamp == null;
    }

    /**
     * Hash code method for proper object hashing in collections.
     * 
     * Implements consistent hash code generation based on all response fields
     * including transaction data and pagination metadata, supporting proper
     * collection behavior and caching strategies.
     * 
     * @return computed hash code for the response object
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (transactions != null ? transactions.hashCode() : 0);
        result = 31 * result + (paginationMetadata != null ? paginationMetadata.hashCode() : 0);
        result = 31 * result + (totalAmount != null ? totalAmount.hashCode() : 0);
        result = 31 * result + (searchCriteria != null ? searchCriteria.hashCode() : 0);
        result = 31 * result + (generatedTimestamp != null ? generatedTimestamp.hashCode() : 0);
        return result;
    }
}