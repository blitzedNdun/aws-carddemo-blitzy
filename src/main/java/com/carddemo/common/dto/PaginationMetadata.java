package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;

/**
 * Pagination metadata DTO providing comprehensive page navigation information
 * including current page, total pages, total records, and navigation state indicators
 * supporting consistent pagination across all card listing and transaction management APIs.
 * 
 * This class replicates the pagination logic from the COBOL program COCRDLIC.cbl
 * which uses WS-MAX-SCREEN-LINES (7 records per page) and provides equivalent
 * pagination metadata for Spring Boot microservices architecture.
 * 
 * Original COBOL pagination variables mapped to Java:
 * - WS-CA-SCREEN-NUM → currentPage
 * - WS-MAX-SCREEN-LINES → pageSize (constant 7)
 * - CA-NEXT-PAGE-EXISTS → hasNextPage
 * - CA-FIRST-PAGE → isFirstPage
 * - CA-LAST-PAGE-SHOWN → isLastPage
 * 
 * @author CardDemo Transformation Team
 * @version 1.0
 * @since 2024-01-01
 */
public class PaginationMetadata {
    
    /**
     * Default page size matching COBOL WS-MAX-SCREEN-LINES value.
     * This constant preserves the original mainframe screen display limit
     * of 7 records per page as specified in COCRDLIC.cbl line 177-178.
     */
    public static final int DEFAULT_PAGE_SIZE = 7;
    
    /**
     * Current page number (1-based indexing).
     * Maps to COBOL WS-CA-SCREEN-NUM variable.
     */
    @JsonProperty("currentPage")
    @Min(value = 1, message = "Current page must be greater than or equal to 1")
    private int currentPage;
    
    /**
     * Total number of pages available.
     * Calculated based on total records and page size.
     */
    @JsonProperty("totalPages")
    @Min(value = 0, message = "Total pages must be greater than or equal to 0")
    private int totalPages;
    
    /**
     * Total number of records across all pages.
     * Used for calculating total pages and navigation state.
     */
    @JsonProperty("totalRecords")
    @Min(value = 0, message = "Total records must be greater than or equal to 0")
    private long totalRecords;
    
    /**
     * Number of records per page.
     * Defaults to 7 to match COBOL WS-MAX-SCREEN-LINES.
     */
    @JsonProperty("pageSize")
    @Min(value = 1, message = "Page size must be greater than or equal to 1")
    private int pageSize = DEFAULT_PAGE_SIZE;
    
    /**
     * Indicates if there is a next page available.
     * Maps to COBOL CA-NEXT-PAGE-EXISTS condition.
     */
    @JsonProperty("hasNextPage")
    private boolean hasNextPage;
    
    /**
     * Indicates if there is a previous page available.
     * Calculated as currentPage > 1.
     */
    @JsonProperty("hasPreviousPage")
    private boolean hasPreviousPage;
    
    /**
     * Indicates if current page is the first page.
     * Maps to COBOL CA-FIRST-PAGE condition.
     */
    @JsonProperty("isFirstPage")
    private boolean isFirstPage;
    
    /**
     * Indicates if current page is the last page.
     * Maps to COBOL CA-LAST-PAGE-SHOWN condition.
     */
    @JsonProperty("isLastPage")
    private boolean isLastPage;
    
    /**
     * Default constructor initializing pagination metadata with default values.
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
     * Constructor for creating pagination metadata with specified values.
     * Automatically calculates navigation state based on provided parameters.
     * 
     * @param currentPage Current page number (1-based)
     * @param totalRecords Total number of records
     * @param pageSize Number of records per page
     */
    public PaginationMetadata(int currentPage, long totalRecords, int pageSize) {
        this.currentPage = currentPage;
        this.totalRecords = totalRecords;
        this.pageSize = pageSize;
        this.totalPages = calculateTotalPages();
        this.calculateNavigationState();
    }
    
    /**
     * Constructor using default page size of 7 records.
     * 
     * @param currentPage Current page number (1-based)
     * @param totalRecords Total number of records
     */
    public PaginationMetadata(int currentPage, long totalRecords) {
        this(currentPage, totalRecords, DEFAULT_PAGE_SIZE);
    }
    
    /**
     * Calculate total pages based on total records and page size.
     * Uses ceiling division to ensure all records are included.
     * 
     * @return Total number of pages
     */
    private int calculateTotalPages() {
        if (totalRecords == 0 || pageSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalRecords / pageSize);
    }
    
    /**
     * Calculate navigation state indicators based on current pagination values.
     * This method replicates the COBOL logic for page navigation state.
     */
    private void calculateNavigationState() {
        this.isFirstPage = (currentPage == 1);
        this.isLastPage = (currentPage >= totalPages) || (totalPages == 0);
        this.hasPreviousPage = (currentPage > 1);
        this.hasNextPage = (currentPage < totalPages);
    }
    
    /**
     * Get current page number.
     * 
     * @return Current page number (1-based)
     */
    public int getCurrentPage() {
        return currentPage;
    }
    
    /**
     * Set current page number and recalculate navigation state.
     * 
     * @param currentPage Current page number (1-based)
     */
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        this.calculateNavigationState();
    }
    
    /**
     * Get total number of pages.
     * 
     * @return Total number of pages
     */
    public int getTotalPages() {
        return totalPages;
    }
    
    /**
     * Set total number of pages and recalculate navigation state.
     * 
     * @param totalPages Total number of pages
     */
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
        this.calculateNavigationState();
    }
    
    /**
     * Get total number of records.
     * 
     * @return Total number of records
     */
    public long getTotalRecords() {
        return totalRecords;
    }
    
    /**
     * Set total number of records and recalculate total pages and navigation state.
     * 
     * @param totalRecords Total number of records
     */
    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
        this.totalPages = calculateTotalPages();
        this.calculateNavigationState();
    }
    
    /**
     * Get page size (number of records per page).
     * 
     * @return Page size
     */
    public int getPageSize() {
        return pageSize;
    }
    
    /**
     * Set page size and recalculate total pages and navigation state.
     * 
     * @param pageSize Page size (number of records per page)
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
        this.totalPages = calculateTotalPages();
        this.calculateNavigationState();
    }
    
    /**
     * Check if there is a next page available.
     * Equivalent to COBOL CA-NEXT-PAGE-EXISTS condition.
     * 
     * @return true if next page exists, false otherwise
     */
    public boolean hasNextPage() {
        return hasNextPage;
    }
    
    /**
     * Set next page availability indicator.
     * 
     * @param hasNextPage true if next page exists, false otherwise
     */
    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }
    
    /**
     * Check if there is a previous page available.
     * Calculated as currentPage > 1.
     * 
     * @return true if previous page exists, false otherwise
     */
    public boolean hasPreviousPage() {
        return hasPreviousPage;
    }
    
    /**
     * Set previous page availability indicator.
     * 
     * @param hasPreviousPage true if previous page exists, false otherwise
     */
    public void setHasPreviousPage(boolean hasPreviousPage) {
        this.hasPreviousPage = hasPreviousPage;
    }
    
    /**
     * Check if current page is the first page.
     * Equivalent to COBOL CA-FIRST-PAGE condition.
     * 
     * @return true if current page is first page, false otherwise
     */
    public boolean isFirstPage() {
        return isFirstPage;
    }
    
    /**
     * Set first page indicator.
     * 
     * @param isFirstPage true if current page is first page, false otherwise
     */
    public void setFirstPage(boolean isFirstPage) {
        this.isFirstPage = isFirstPage;
    }
    
    /**
     * Check if current page is the last page.
     * Equivalent to COBOL CA-LAST-PAGE-SHOWN condition.
     * 
     * @return true if current page is last page, false otherwise
     */
    public boolean isLastPage() {
        return isLastPage;
    }
    
    /**
     * Set last page indicator.
     * 
     * @param isLastPage true if current page is last page, false otherwise
     */
    public void setLastPage(boolean isLastPage) {
        this.isLastPage = isLastPage;
    }
    
    /**
     * Calculate the starting record number for the current page (1-based).
     * Used for OFFSET calculations in database queries.
     * 
     * @return Starting record number for current page
     */
    public long getStartRecord() {
        return (long) (currentPage - 1) * pageSize + 1;
    }
    
    /**
     * Calculate the ending record number for the current page (1-based).
     * Used for display purposes and query result validation.
     * 
     * @return Ending record number for current page
     */
    public long getEndRecord() {
        long endRecord = (long) currentPage * pageSize;
        return Math.min(endRecord, totalRecords);
    }
    
    /**
     * Calculate the offset for database queries (0-based).
     * Used with Spring Data JPA Pageable implementations.
     * 
     * @return Offset for database queries
     */
    public int getOffset() {
        return (currentPage - 1) * pageSize;
    }
    
    /**
     * Check if pagination is required based on total records and page size.
     * 
     * @return true if pagination is needed, false otherwise
     */
    public boolean isPaginationRequired() {
        return totalRecords > pageSize;
    }
    
    /**
     * Get the next page number if available.
     * 
     * @return Next page number, or current page if no next page
     */
    public int getNextPageNumber() {
        return hasNextPage ? currentPage + 1 : currentPage;
    }
    
    /**
     * Get the previous page number if available.
     * 
     * @return Previous page number, or current page if no previous page
     */
    public int getPreviousPageNumber() {
        return hasPreviousPage ? currentPage - 1 : currentPage;
    }
    
    /**
     * Create a new PaginationMetadata instance for the next page.
     * Preserves total records and page size while advancing to next page.
     * 
     * @return New PaginationMetadata for next page, or current instance if no next page
     */
    public PaginationMetadata nextPage() {
        if (hasNextPage) {
            return new PaginationMetadata(currentPage + 1, totalRecords, pageSize);
        }
        return this;
    }
    
    /**
     * Create a new PaginationMetadata instance for the previous page.
     * Preserves total records and page size while going back to previous page.
     * 
     * @return New PaginationMetadata for previous page, or current instance if no previous page
     */
    public PaginationMetadata previousPage() {
        if (hasPreviousPage) {
            return new PaginationMetadata(currentPage - 1, totalRecords, pageSize);
        }
        return this;
    }
    
    /**
     * Create a new PaginationMetadata instance for the first page.
     * Preserves total records and page size while resetting to first page.
     * 
     * @return New PaginationMetadata for first page
     */
    public PaginationMetadata firstPage() {
        return new PaginationMetadata(1, totalRecords, pageSize);
    }
    
    /**
     * Create a new PaginationMetadata instance for the last page.
     * Preserves total records and page size while jumping to last page.
     * 
     * @return New PaginationMetadata for last page
     */
    public PaginationMetadata lastPage() {
        return new PaginationMetadata(totalPages > 0 ? totalPages : 1, totalRecords, pageSize);
    }
    
    /**
     * Convert pagination metadata to string representation for debugging.
     * 
     * @return String representation of pagination metadata
     */
    @Override
    public String toString() {
        return String.format(
            "PaginationMetadata{currentPage=%d, totalPages=%d, totalRecords=%d, pageSize=%d, " +
            "hasNextPage=%s, hasPreviousPage=%s, isFirstPage=%s, isLastPage=%s}",
            currentPage, totalPages, totalRecords, pageSize,
            hasNextPage, hasPreviousPage, isFirstPage, isLastPage
        );
    }
    
    /**
     * Check equality based on all pagination metadata fields.
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
     * Generate hash code based on all pagination metadata fields.
     * 
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(currentPage, totalPages, totalRecords, pageSize,
                                     hasNextPage, hasPreviousPage, isFirstPage, isLastPage);
    }
}