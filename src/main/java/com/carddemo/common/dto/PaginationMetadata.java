package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;

/**
 * Pagination metadata DTO providing comprehensive page navigation information
 * including current page, total pages, total records, and navigation state indicators.
 * 
 * This DTO supports consistent pagination across all card listing and transaction 
 * management APIs, maintaining compatibility with the original COBOL COCRDLIC.cbl
 * pagination logic that displays 7 cards per page (WS-MAX-SCREEN-LINES).
 * 
 * The pagination metadata preserves the exact navigation semantics from the
 * legacy system including page boundary detection, next/previous page availability,
 * and first/last page state indicators for optimal user experience.
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 */
public class PaginationMetadata {
    
    /**
     * Default page size matching COBOL WS-MAX-SCREEN-LINES constant from COCRDLIC.cbl
     */
    public static final int DEFAULT_PAGE_SIZE = 7;
    
    /**
     * Current page number (1-based indexing to match UI conventions)
     * Maps to WS-CA-SCREEN-NUM from COBOL pagination logic
     */
    @JsonProperty("currentPage")
    @Min(value = 1, message = "Current page must be greater than 0")
    private int currentPage;
    
    /**
     * Total number of pages available based on total records and page size
     * Calculated to support Material-UI pagination component navigation
     */
    @JsonProperty("totalPages")
    @Min(value = 0, message = "Total pages must be non-negative")
    private int totalPages;
    
    /**
     * Total number of records across all pages
     * Used for display purposes and calculation of pagination boundaries
     */
    @JsonProperty("totalRecords")
    @Min(value = 0, message = "Total records must be non-negative")
    private long totalRecords;
    
    /**
     * Number of records per page (typically 7 for card listings)
     * Maps to WS-MAX-SCREEN-LINES constant from legacy COBOL logic
     */
    @JsonProperty("pageSize")
    @Min(value = 1, message = "Page size must be greater than 0")
    private int pageSize = DEFAULT_PAGE_SIZE;
    
    /**
     * Indicates whether a next page exists beyond the current page
     * Maps to CA-NEXT-PAGE-EXISTS flag from COBOL WS-CA-NEXT-PAGE-IND logic
     */
    @JsonProperty("hasNextPage")
    private boolean hasNextPage;
    
    /**
     * Indicates whether a previous page exists before the current page
     * Derived from current page position for backward navigation support
     */
    @JsonProperty("hasPreviousPage")
    private boolean hasPreviousPage;
    
    /**
     * Indicates whether the current page is the first page
     * Maps to CA-FIRST-PAGE condition from COBOL pagination logic
     */
    @JsonProperty("isFirstPage")
    private boolean isFirstPage;
    
    /**
     * Indicates whether the current page is the last page
     * Maps to CA-LAST-PAGE-SHOWN flag from COBOL WS-CA-LAST-PAGE-DISPLAYED logic
     */
    @JsonProperty("isLastPage")
    private boolean isLastPage;
    
    /**
     * Default constructor initializing pagination metadata with default values
     */
    public PaginationMetadata() {
        this.currentPage = 1;
        this.totalPages = 0;
        this.totalRecords = 0L;
        this.pageSize = DEFAULT_PAGE_SIZE;
        this.hasNextPage = false;
        this.hasPreviousPage = false;
        this.isFirstPage = true;
        this.isLastPage = true;
    }
    
    /**
     * Constructor for creating pagination metadata with Spring Data Page information
     * 
     * @param currentPage Current page number (1-based)
     * @param totalPages Total number of pages
     * @param totalRecords Total number of records
     * @param pageSize Number of records per page
     */
    public PaginationMetadata(int currentPage, int totalPages, long totalRecords, int pageSize) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalRecords = totalRecords;
        this.pageSize = pageSize;
        
        // Calculate navigation indicators based on page position
        updateNavigationIndicators();
    }
    
    /**
     * Updates navigation indicators based on current pagination state
     * This method replicates the logic from COBOL COCRDLIC.cbl pagination handling
     */
    private void updateNavigationIndicators() {
        this.hasPreviousPage = this.currentPage > 1;
        this.hasNextPage = this.currentPage < this.totalPages;
        this.isFirstPage = this.currentPage == 1;
        this.isLastPage = this.currentPage == this.totalPages || this.totalPages == 0;
    }
    
    /**
     * Gets the current page number (1-based indexing)
     * 
     * @return Current page number
     */
    public int getCurrentPage() {
        return currentPage;
    }
    
    /**
     * Sets the current page number and updates navigation indicators
     * 
     * @param currentPage Current page number (must be greater than 0)
     */
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        updateNavigationIndicators();
    }
    
    /**
     * Gets the total number of pages
     * 
     * @return Total number of pages
     */
    public int getTotalPages() {
        return totalPages;
    }
    
    /**
     * Sets the total number of pages and updates navigation indicators
     * 
     * @param totalPages Total number of pages (must be non-negative)
     */
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
        updateNavigationIndicators();
    }
    
    /**
     * Gets the total number of records across all pages
     * 
     * @return Total number of records
     */
    public long getTotalRecords() {
        return totalRecords;
    }
    
    /**
     * Sets the total number of records
     * 
     * @param totalRecords Total number of records (must be non-negative)
     */
    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }
    
    /**
     * Gets the number of records per page
     * 
     * @return Number of records per page
     */
    public int getPageSize() {
        return pageSize;
    }
    
    /**
     * Sets the number of records per page
     * 
     * @param pageSize Number of records per page (must be greater than 0)
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    /**
     * Checks if a next page exists beyond the current page
     * 
     * @return true if next page exists, false otherwise
     */
    public boolean hasNextPage() {
        return hasNextPage;
    }
    
    /**
     * Sets the next page availability indicator
     * 
     * @param hasNextPage true if next page exists, false otherwise
     */
    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }
    
    /**
     * Checks if a previous page exists before the current page
     * 
     * @return true if previous page exists, false otherwise
     */
    public boolean hasPreviousPage() {
        return hasPreviousPage;
    }
    
    /**
     * Sets the previous page availability indicator
     * 
     * @param hasPreviousPage true if previous page exists, false otherwise
     */
    public void setHasPreviousPage(boolean hasPreviousPage) {
        this.hasPreviousPage = hasPreviousPage;
    }
    
    /**
     * Checks if the current page is the first page
     * 
     * @return true if current page is first page, false otherwise
     */
    public boolean isFirstPage() {
        return isFirstPage;
    }
    
    /**
     * Sets the first page indicator
     * 
     * @param isFirstPage true if current page is first page, false otherwise
     */
    public void setFirstPage(boolean isFirstPage) {
        this.isFirstPage = isFirstPage;
    }
    
    /**
     * Checks if the current page is the last page
     * 
     * @return true if current page is last page, false otherwise
     */
    public boolean isLastPage() {
        return isLastPage;
    }
    
    /**
     * Sets the last page indicator
     * 
     * @param isLastPage true if current page is last page, false otherwise
     */
    public void setLastPage(boolean isLastPage) {
        this.isLastPage = isLastPage;
    }
    
    /**
     * Creates a string representation of the pagination metadata
     * 
     * @return String representation including all pagination information
     */
    @Override
    public String toString() {
        return "PaginationMetadata{" +
                "currentPage=" + currentPage +
                ", totalPages=" + totalPages +
                ", totalRecords=" + totalRecords +
                ", pageSize=" + pageSize +
                ", hasNextPage=" + hasNextPage +
                ", hasPreviousPage=" + hasPreviousPage +
                ", isFirstPage=" + isFirstPage +
                ", isLastPage=" + isLastPage +
                '}';
    }
    
    /**
     * Checks equality based on all pagination metadata fields
     * 
     * @param obj Object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
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
     * Generates hash code based on all pagination metadata fields
     * 
     * @return Hash code for the object
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