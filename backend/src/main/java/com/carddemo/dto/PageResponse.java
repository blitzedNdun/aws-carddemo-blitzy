/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.ArrayList;

/**
 * Generic pagination response wrapper for REST API endpoints.
 * 
 * This DTO provides pagination support for list operations, replicating
 * COBOL screen navigation patterns (F7/F8 forward/backward browsing) with
 * modern REST API pagination conventions.
 * 
 * Maps COBOL pagination concepts:
 * - Screen-based navigation (7 records per screen) → page size configuration
 * - Forward/backward browsing → previous/next page navigation
 * - Record counting → total elements and pages calculation
 * - End-of-file detection → hasNext/hasPrevious indicators
 * 
 * Usage Examples:
 * - Card listing: PageResponse<CardListDto> for COCRDLI screen replacement
 * - Transaction listing: PageResponse<TransactionDto> for transaction browsing
 * - User listing: PageResponse<UserDto> for user management screens
 * 
 * Pagination Parameters:
 * - page: Zero-based page number (0, 1, 2, ...)
 * - size: Number of elements per page (typically 7 to match COBOL screens)
 * - totalElements: Total number of records across all pages
 * - totalPages: Total number of pages available
 * 
 * @param <T> Generic type for the data elements in the page
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Data
public class PageResponse<T> {

    /**
     * List of data elements for the current page.
     * Contains the actual records to be displayed (up to 'size' elements).
     */
    @JsonProperty("data")
    private List<T> data;

    /**
     * Current page number (zero-based).
     * Corresponds to the current screen being displayed in COBOL equivalent.
     */
    @JsonProperty("page")
    private int page;

    /**
     * Number of elements per page.
     * Typically set to 7 to match COBOL screen line capacity.
     */
    @JsonProperty("size")
    private int size;

    /**
     * Total number of elements across all pages.
     * Used for calculating pagination metadata and end-of-data detection.
     */
    @JsonProperty("totalElements")
    private long totalElements;

    /**
     * Total number of pages available.
     * Calculated as ceil(totalElements / size).
     */
    @JsonProperty("totalPages")
    private int totalPages;

    /**
     * Indicates if there is a next page available.
     * Maps to COBOL end-of-file detection for forward browsing.
     */
    @JsonProperty("hasNext")
    private boolean hasNext;

    /**
     * Indicates if there is a previous page available.
     * Maps to COBOL beginning-of-file detection for backward browsing.
     */
    @JsonProperty("hasPrevious")
    private boolean hasPrevious;

    /**
     * Indicates if this is the first page.
     * Convenience field for UI navigation controls.
     */
    @JsonProperty("isFirst")
    private boolean isFirst;

    /**
     * Indicates if this is the last page.
     * Convenience field for UI navigation controls.
     */
    @JsonProperty("isLast")
    private boolean isLast;

    /**
     * Number of elements in the current page.
     * May be less than 'size' for the last page.
     */
    @JsonProperty("numberOfElements")
    private int numberOfElements;

    /**
     * Default constructor initializing empty page.
     */
    public PageResponse() {
        this.data = new ArrayList<>();
        this.page = 0;
        this.size = 0;
        this.totalElements = 0;
        this.totalPages = 0;
        this.hasNext = false;
        this.hasPrevious = false;
        this.isFirst = true;
        this.isLast = true;
        this.numberOfElements = 0;
    }

    /**
     * Constructor with data and pagination metadata.
     * Automatically calculates derived pagination fields.
     * 
     * @param data List of data elements for this page
     * @param page Current page number (zero-based)
     * @param size Number of elements per page
     * @param totalElements Total number of elements across all pages
     */
    public PageResponse(List<T> data, int page, int size, long totalElements) {
        this.data = data != null ? data : new ArrayList<>();
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.numberOfElements = this.data.size();

        // Calculate total pages
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        // Calculate pagination flags
        this.hasPrevious = page > 0;
        this.hasNext = page < (this.totalPages - 1);
        this.isFirst = page == 0;
        this.isLast = page >= (this.totalPages - 1);
    }

    /**
     * Static factory method to create an empty page response.
     * Used when no data is available for the requested page.
     * 
     * @param <T> Type of data elements
     * @param page Current page number
     * @param size Page size
     * @return Empty PageResponse instance
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(new ArrayList<>(), page, size, 0);
    }

    /**
     * Static factory method to create a page response with data.
     * Convenience method for common use cases.
     * 
     * @param <T> Type of data elements
     * @param data List of data elements
     * @param page Current page number
     * @param size Page size
     * @param totalElements Total elements across all pages
     * @return Populated PageResponse instance
     */
    public static <T> PageResponse<T> of(List<T> data, int page, int size, long totalElements) {
        return new PageResponse<>(data, page, size, totalElements);
    }

    /**
     * Checks if the page has any data elements.
     * 
     * @return true if data list is not empty, false otherwise
     */
    public boolean hasData() {
        return data != null && !data.isEmpty();
    }

    /**
     * Checks if the page is empty (no data elements).
     * 
     * @return true if data list is empty or null, false otherwise
     */
    public boolean isEmpty() {
        return !hasData();
    }

    /**
     * Gets the number of elements currently in the page.
     * May be different from size for partial pages.
     * 
     * @return Number of elements in current page data
     */
    public int getContentSize() {
        return data != null ? data.size() : 0;
    }

    /**
     * Updates pagination metadata after data is set.
     * Should be called if data is modified after construction.
     */
    public void updatePaginationMetadata() {
        this.numberOfElements = getContentSize();
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        this.hasPrevious = page > 0;
        this.hasNext = page < (this.totalPages - 1);
        this.isFirst = page == 0;
        this.isLast = page >= (this.totalPages - 1);
    }

    /**
     * Custom getter for hasNext field to match expected API.
     * 
     * @return true if there is a next page available
     */
    public boolean hasNext() {
        return this.hasNext;
    }

    /**
     * Custom getter for hasPrevious field to match expected API.
     * 
     * @return true if there is a previous page available
     */
    public boolean hasPrevious() {
        return this.hasPrevious;
    }

    /**
     * Gets the page number for the next page.
     * Returns -1 if there is no next page.
     * 
     * @return Next page number or -1 if no next page
     */
    public int getNextPageNumber() {
        return hasNext ? page + 1 : -1;
    }

    /**
     * Gets the page number for the previous page.
     * Returns -1 if there is no previous page.
     * 
     * @return Previous page number or -1 if no previous page
     */
    public int getPreviousPageNumber() {
        return hasPrevious ? page - 1 : -1;
    }

    /**
     * Creates a builder instance for PageResponse.
     * Enables fluent API construction pattern.
     * 
     * @param <T> Type of data elements
     * @return New PageResponse builder instance
     */
    public static <T> PageResponseBuilder<T> builder() {
        return new PageResponseBuilder<>();
    }

    /**
     * Builder class for PageResponse to support fluent API construction.
     * 
     * @param <T> Type of data elements
     */
    public static class PageResponseBuilder<T> {
        private List<T> data;
        private int page;
        private int size;
        private long totalElements;

        public PageResponseBuilder<T> data(List<T> data) {
            this.data = data;
            return this;
        }

        public PageResponseBuilder<T> page(int page) {
            this.page = page;
            return this;
        }

        public PageResponseBuilder<T> size(int size) {
            this.size = size;
            return this;
        }

        public PageResponseBuilder<T> totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }

        public PageResponse<T> build() {
            return new PageResponse<>(data, page, size, totalElements);
        }
    }
}