package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;

/**
 * Pagination metadata DTO providing comprehensive page navigation information
 * for card listing and transaction management APIs.
 * 
 * This class maintains functional equivalence with the original COBOL pagination
 * implementation in COCRDLIC.cbl, preserving the exact screen line limits and
 * navigation state indicators while enabling modern REST API JSON responses.
 * 
 * Key COBOL equivalents:
 * - WS-MAX-SCREEN-LINES (value 7) -> pageSize default
 * - WS-CA-SCREEN-NUM -> currentPage
 * - CA-NEXT-PAGE-EXISTS -> hasNextPage  
 * - CA-FIRST-PAGE -> isFirstPage calculation
 * - CA-LAST-PAGE-SHOWN -> isLastPage calculation
 * 
 * Performance Requirements:
 * - Supports pagination for card listing operations maintaining sub-200ms response times
 * - Enables efficient page navigation for up to 10,000 TPS transaction volumes
 * - Preserves exact pagination behavior from original mainframe implementation
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since CardDemo v1.0 - Spring Boot Microservices Migration
 */
public class PaginationMetadata {

    /**
     * Default page size matching COBOL WS-MAX-SCREEN-LINES constant.
     * This value preserves the original 7-record screen display limit
     * from the mainframe card listing implementation.
     */
    public static final int DEFAULT_PAGE_SIZE = 7;

    /**
     * Current page number (1-based indexing).
     * Maps to COBOL WS-CA-SCREEN-NUM for maintaining page position state.
     * Must be greater than 0 for valid pagination.
     */
    @JsonProperty("currentPage")
    @Min(value = 1, message = "Current page must be greater than 0")
    private int currentPage;

    /**
     * Total number of pages available.
     * Calculated based on total record count and page size.
     * Enables UI components to render proper pagination controls.
     */
    @JsonProperty("totalPages")
    @Min(value = 0, message = "Total pages cannot be negative")
    private int totalPages;

    /**
     * Total number of records across all pages.
     * Provides complete dataset size information for user context
     * and enables accurate pagination calculations.
     */
    @JsonProperty("totalRecords")
    @Min(value = 0, message = "Total records cannot be negative")
    private long totalRecords;

    /**
     * Number of records displayed per page.
     * Defaults to 7 to match COBOL WS-MAX-SCREEN-LINES limit.
     * Configurable for different listing operations while maintaining consistency.
     */
    @JsonProperty("pageSize")
    @Min(value = 1, message = "Page size must be greater than 0")
    private int pageSize = DEFAULT_PAGE_SIZE;

    /**
     * Indicates if there is a next page available.
     * Maps to COBOL CA-NEXT-PAGE-EXISTS condition for forward navigation.
     * Critical for enabling/disabling "Next" button in UI components.
     */
    @JsonProperty("hasNextPage")
    private boolean hasNextPage;

    /**
     * Indicates if there is a previous page available.
     * Derived from current page position for backward navigation.
     * Critical for enabling/disabling "Previous" button in UI components.
     */
    @JsonProperty("hasPreviousPage")  
    private boolean hasPreviousPage;

    /**
     * Indicates if current page is the first page.
     * Maps to COBOL CA-FIRST-PAGE condition for navigation state.
     * Used for UI state management and navigation control logic.
     */
    @JsonProperty("isFirstPage")
    private boolean isFirstPage;

    /**
     * Indicates if current page is the last page.
     * Derived from pagination calculations for navigation state.
     * Used for UI state management and navigation control logic.
     */
    @JsonProperty("isLastPage")
    private boolean isLastPage;

    /**
     * Default constructor initializing pagination metadata with safe defaults.
     * Sets up initial state equivalent to COBOL program initialization.
     */
    public PaginationMetadata() {
        this.currentPage = 1;
        this.totalPages = 0;
        this.totalRecords = 0;
        this.pageSize = DEFAULT_PAGE_SIZE;
        this.hasNextPage = false;
        this.hasPreviousPage = false;
        this.isFirstPage = true;
        this.isLastPage = true;
    }

    /**
     * Constructor for creating pagination metadata with specific values.
     * Automatically calculates navigation state indicators based on provided values.
     * 
     * @param currentPage the current page number (1-based)
     * @param totalRecords the total number of records
     * @param pageSize the number of records per page
     */
    public PaginationMetadata(int currentPage, long totalRecords, int pageSize) {
        this.currentPage = Math.max(1, currentPage);
        this.totalRecords = Math.max(0, totalRecords);
        this.pageSize = Math.max(1, pageSize);
        
        // Calculate total pages - ensure at least 1 page if records exist
        this.totalPages = totalRecords > 0 ? (int) Math.ceil((double) totalRecords / pageSize) : 0;
        
        // Ensure current page doesn't exceed total pages
        if (this.totalPages > 0 && this.currentPage > this.totalPages) {
            this.currentPage = this.totalPages;
        }
        
        // Calculate navigation state indicators
        calculateNavigationState();
    }

    /**
     * Calculates navigation state indicators based on current pagination values.
     * Replicates COBOL conditional logic for page navigation state management.
     * This method maintains exact behavioral equivalence with the original implementation.
     */
    private void calculateNavigationState() {
        // Determine if this is the first page (equivalent to CA-FIRST-PAGE)
        this.isFirstPage = (this.currentPage <= 1);
        
        // Determine if this is the last page
        this.isLastPage = (this.totalPages == 0) || (this.currentPage >= this.totalPages);
        
        // Determine if next page exists (equivalent to CA-NEXT-PAGE-EXISTS)
        this.hasNextPage = (this.totalPages > 0) && (this.currentPage < this.totalPages);
        
        // Determine if previous page exists
        this.hasPreviousPage = (this.currentPage > 1);
    }

    /**
     * Gets the current page number.
     * @return the current page number (1-based indexing)
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Sets the current page number and recalculates navigation state.
     * @param currentPage the current page number (must be >= 1)
     */
    public void setCurrentPage(int currentPage) {
        this.currentPage = Math.max(1, currentPage);
        calculateNavigationState();
    }

    /**
     * Gets the total number of pages.
     * @return the total number of pages available
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Sets the total number of pages and recalculates navigation state.
     * @param totalPages the total number of pages (must be >= 0)
     */
    public void setTotalPages(int totalPages) {
        this.totalPages = Math.max(0, totalPages);
        calculateNavigationState();
    }

    /**
     * Gets the total number of records.
     * @return the total number of records across all pages
     */
    public long getTotalRecords() {
        return totalRecords;
    }

    /**
     * Sets the total number of records and recalculates pagination.
     * @param totalRecords the total number of records (must be >= 0)
     */
    public void setTotalRecords(long totalRecords) {
        this.totalRecords = Math.max(0, totalRecords);
        // Recalculate total pages based on new record count
        this.totalPages = this.totalRecords > 0 ? (int) Math.ceil((double) this.totalRecords / this.pageSize) : 0;
        calculateNavigationState();
    }

    /**
     * Gets the number of records displayed per page.
     * @return the page size
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Sets the page size and recalculates pagination.
     * @param pageSize the number of records per page (must be >= 1)
     */
    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(1, pageSize);
        // Recalculate total pages based on new page size
        this.totalPages = this.totalRecords > 0 ? (int) Math.ceil((double) this.totalRecords / this.pageSize) : 0;
        calculateNavigationState();
    }

    /**
     * Checks if there is a next page available.
     * @return true if next page exists, false otherwise
     */
    public boolean hasNextPage() {
        return hasNextPage;
    }

    /**
     * Sets the next page availability indicator.
     * @param hasNextPage true if next page exists, false otherwise
     */
    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    /**
     * Checks if there is a previous page available.
     * @return true if previous page exists, false otherwise
     */
    public boolean hasPreviousPage() {
        return hasPreviousPage;
    }

    /**
     * Sets the previous page availability indicator.
     * @param hasPreviousPage true if previous page exists, false otherwise
     */
    public void setHasPreviousPage(boolean hasPreviousPage) {
        this.hasPreviousPage = hasPreviousPage;
    }

    /**
     * Checks if the current page is the first page.
     * @return true if this is the first page, false otherwise
     */
    public boolean isFirstPage() {
        return isFirstPage;
    }

    /**
     * Sets the first page indicator.
     * @param isFirstPage true if this is the first page, false otherwise
     */
    public void setFirstPage(boolean isFirstPage) {
        this.isFirstPage = isFirstPage;
    }

    /**
     * Checks if the current page is the last page.
     * @return true if this is the last page, false otherwise
     */
    public boolean isLastPage() {
        return isLastPage;
    }

    /**
     * Sets the last page indicator.
     * @param isLastPage true if this is the last page, false otherwise
     */
    public void setLastPage(boolean isLastPage) {
        this.isLastPage = isLastPage;
    }

    /**
     * Calculates the starting record number for the current page (1-based).
     * Useful for displaying "Showing records X to Y of Z" information.
     * 
     * @return the starting record number, or 0 if no records exist
     */
    public long getStartRecord() {
        if (totalRecords == 0) {
            return 0;
        }
        return ((long) (currentPage - 1) * pageSize) + 1;
    }

    /**
     * Calculates the ending record number for the current page (1-based).
     * Useful for displaying "Showing records X to Y of Z" information.
     * 
     * @return the ending record number, accounting for partial last pages
     */
    public long getEndRecord() {
        if (totalRecords == 0) {
            return 0;
        }
        long endRecord = (long) currentPage * pageSize;
        return Math.min(endRecord, totalRecords);
    }

    /**
     * Gets the number of records on the current page.
     * Accounts for partial pages where the last page may have fewer records.
     * 
     * @return the number of records on the current page
     */
    public int getCurrentPageRecordCount() {
        if (totalRecords == 0) {
            return 0;
        }
        long startRecord = getStartRecord();
        long endRecord = getEndRecord();
        return (int) (endRecord - startRecord + 1);
    }

    /**
     * Creates a pagination metadata object for the next page.
     * Preserves all settings while advancing to the next page if available.
     * 
     * @return a new PaginationMetadata object for the next page, or current page if no next page exists
     */
    public PaginationMetadata nextPage() {
        int nextPageNumber = hasNextPage ? currentPage + 1 : currentPage;
        return new PaginationMetadata(nextPageNumber, totalRecords, pageSize);
    }

    /**
     * Creates a pagination metadata object for the previous page.
     * Preserves all settings while moving to the previous page if available.
     * 
     * @return a new PaginationMetadata object for the previous page, or current page if no previous page exists
     */
    public PaginationMetadata previousPage() {
        int prevPageNumber = hasPreviousPage ? currentPage - 1 : currentPage;
        return new PaginationMetadata(prevPageNumber, totalRecords, pageSize);
    }

    /**
     * Validates the pagination metadata for consistency and business rules.
     * Ensures all values are within acceptable ranges and relationships are valid.
     * 
     * @return true if pagination metadata is valid, false otherwise
     */
    public boolean isValid() {
        // Basic range validation
        if (currentPage < 1 || pageSize < 1 || totalRecords < 0 || totalPages < 0) {
            return false;
        }
        
        // Relationship validation
        if (totalRecords > 0 && totalPages == 0) {
            return false; // Should have at least 1 page if records exist
        }
        
        if (totalPages > 0 && currentPage > totalPages) {
            return false; // Current page cannot exceed total pages
        }
        
        // Navigation state consistency
        if (currentPage == 1 && !isFirstPage) {
            return false; // Page 1 should always be marked as first page
        }
        
        if (totalPages > 0 && currentPage == totalPages && !isLastPage) {
            return false; // Last page should always be marked as last page
        }
        
        return true;
    }

    /**
     * Returns a string representation of the pagination metadata.
     * Useful for debugging and logging pagination state.
     * 
     * @return formatted string showing pagination state
     */
    @Override
    public String toString() {
        return String.format(
            "PaginationMetadata{currentPage=%d, totalPages=%d, totalRecords=%d, pageSize=%d, " +
            "hasNextPage=%s, hasPreviousPage=%s, isFirstPage=%s, isLastPage=%s, " +
            "recordRange=%d-%d}",
            currentPage, totalPages, totalRecords, pageSize,
            hasNextPage, hasPreviousPage, isFirstPage, isLastPage,
            getStartRecord(), getEndRecord()
        );
    }

    /**
     * Checks equality based on pagination values.
     * Two pagination metadata objects are equal if they have the same pagination state.
     * 
     * @param obj the object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        PaginationMetadata that = (PaginationMetadata) obj;
        return currentPage == that.currentPage &&
               totalPages == that.totalPages &&
               totalRecords == that.totalRecords &&
               pageSize == that.pageSize &&
               hasNextPage == that.hasNextPage &&
               hasPreviousPage == that.hasPreviousPage &&
               isFirstPage == that.isFirstPage &&
               isLastPage == that.isLastPage;
    }

    /**
     * Generates hash code based on pagination values.
     * 
     * @return hash code for this pagination metadata object
     */
    @Override
    public int hashCode() {
        int result = currentPage;
        result = 31 * result + totalPages;
        result = 31 * result + (int) (totalRecords ^ (totalRecords >>> 32));
        result = 31 * result + pageSize;
        result = 31 * result + (hasNextPage ? 1 : 0);
        result = 31 * result + (hasPreviousPage ? 1 : 0);
        result = 31 * result + (isFirstPage ? 1 : 0);
        result = 31 * result + (isLastPage ? 1 : 0);
        return result;
    }
}