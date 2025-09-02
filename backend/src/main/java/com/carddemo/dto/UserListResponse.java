/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

/**
 * Response DTO for user list display corresponding to COUSR00 BMS screen.
 * Supports pagination with up to 10 users per page as per mainframe screen layout.
 * 
 * This response DTO maps directly to the COBOL BMS COUSR00 mapset structure:
 * - Supports 10 user slots per page matching screen lines 10-19
 * - Includes pagination metadata for F7/F8 navigation functionality
 * - Maintains field structure compatible with 3270 terminal limitations
 * 
 * The response structure preserves the exact behavior of the mainframe
 * user list screen while providing modern REST API interface.
 */
@AllArgsConstructor
public class UserListResponse {

    /**
     * List of users to display on the current page.
     * Maps to COBOL user entries SEL0001-SEL0010 through UTYPE01-UTYPE10.
     * Maximum of 10 users per page to match BMS screen layout constraints.
     */
    @JsonProperty("users")
    private List<UserListDto> users;

    /**
     * Current page number for pagination control.
     * Maps to COBOL PAGENUMI field in COUSR00 BMS map.
     * Used to track position in paginated user list results.
     */
    @JsonProperty("pageNumber")
    private Integer pageNumber;

    /**
     * Total count of users available across all pages.
     * Provides context for pagination calculations and user feedback.
     * Enables frontend to calculate total pages and navigation state.
     */
    @JsonProperty("totalCount")
    private Long totalCount;

    /**
     * Indicates if there is a next page available.
     * Corresponds to F8=Forward navigation capability in COUSR00 screen.
     * Used to enable/disable next page navigation in frontend.
     */
    @JsonProperty("hasNextPage")
    private Boolean hasNextPage;

    /**
     * Indicates if there is a previous page available.
     * Corresponds to F7=Backward navigation capability in COUSR00 screen.
     * Used to enable/disable previous page navigation in frontend.
     */
    @JsonProperty("hasPreviousPage")
    private Boolean hasPreviousPage;

    /**
     * Default constructor for framework compatibility.
     * Initializes empty user list and default pagination state.
     */
    public UserListResponse() {
        this.users = List.of();
        this.pageNumber = 1;
        this.totalCount = 0L;
        this.hasNextPage = false;
        this.hasPreviousPage = false;
    }

    /**
     * Gets the list of users for the current page.
     * 
     * @return list of UserListDto objects, maximum 10 per page
     */
    public List<UserListDto> getUsers() {
        return users;
    }

    /**
     * Sets the list of users for the current page.
     * Validates that the list does not exceed the 10-user BMS screen limit.
     * 
     * @param users list of users to display, must not exceed 10 entries
     * @throws IllegalArgumentException if users list exceeds 10 entries
     */
    public void setUsers(List<UserListDto> users) {
        if (users != null && users.size() > 10) {
            throw new IllegalArgumentException("User list cannot exceed 10 entries per page (BMS screen limit)");
        }
        this.users = users != null ? users : List.of();
    }

    /**
     * Gets the current page number.
     * 
     * @return current page number, 1-based indexing
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the current page number.
     * Validates that page number is positive.
     * 
     * @param pageNumber page number to set, must be positive
     * @throws IllegalArgumentException if page number is not positive
     */
    public void setPageNumber(Integer pageNumber) {
        if (pageNumber != null && pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be positive");
        }
        this.pageNumber = pageNumber != null ? pageNumber : 1;
    }

    /**
     * Gets the total count of users across all pages.
     * 
     * @return total user count
     */
    public Long getTotalCount() {
        return totalCount;
    }

    /**
     * Sets the total count of users across all pages.
     * Validates that count is non-negative.
     * 
     * @param totalCount total user count, must be non-negative
     * @throws IllegalArgumentException if total count is negative
     */
    public void setTotalCount(Long totalCount) {
        if (totalCount != null && totalCount < 0) {
            throw new IllegalArgumentException("Total count cannot be negative");
        }
        this.totalCount = totalCount != null ? totalCount : 0L;
    }

    /**
     * Gets the next page availability flag.
     * 
     * @return true if next page is available
     */
    public Boolean getHasNextPage() {
        return hasNextPage;
    }

    /**
     * Sets the next page availability flag.
     * 
     * @param hasNextPage true if next page is available
     */
    public void setHasNextPage(Boolean hasNextPage) {
        this.hasNextPage = hasNextPage != null ? hasNextPage : false;
    }

    /**
     * Gets the previous page availability flag.
     * 
     * @return true if previous page is available
     */
    public Boolean getHasPreviousPage() {
        return hasPreviousPage;
    }

    /**
     * Sets the previous page availability flag.
     * 
     * @param hasPreviousPage true if previous page is available
     */
    public void setHasPreviousPage(Boolean hasPreviousPage) {
        this.hasPreviousPage = hasPreviousPage != null ? hasPreviousPage : false;
    }

    /**
     * Checks equality based on all fields for complete object comparison.
     * Ensures consistent behavior in collections and caching scenarios.
     * 
     * @param obj the object to compare with
     * @return true if objects are equal based on all fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UserListResponse that = (UserListResponse) obj;
        return java.util.Objects.equals(users, that.users) &&
               java.util.Objects.equals(pageNumber, that.pageNumber) &&
               java.util.Objects.equals(totalCount, that.totalCount) &&
               java.util.Objects.equals(hasNextPage, that.hasNextPage) &&
               java.util.Objects.equals(hasPreviousPage, that.hasPreviousPage);
    }

    /**
     * Generates hash code based on all fields for consistent hashing behavior.
     * 
     * @return hash code for this object
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(users, pageNumber, totalCount, hasNextPage, hasPreviousPage);
    }

    /**
     * Returns string representation of the UserListResponse for debugging and logging.
     * Includes all fields for comprehensive object state representation.
     * 
     * @return formatted string representation of this object
     */
    @Override
    public String toString() {
        return "UserListResponse{" +
                "users=" + users +
                ", pageNumber=" + pageNumber +
                ", totalCount=" + totalCount +
                ", hasNextPage=" + hasNextPage +
                ", hasPreviousPage=" + hasPreviousPage +
                '}';
    }
}