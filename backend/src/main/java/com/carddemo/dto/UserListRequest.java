package com.carddemo.dto;

import java.util.Objects;

/**
 * Request DTO for user list and search operations (COUSR00 screen).
 * 
 * Maps to COBOL BMS map COUSR00 input fields for user browsing functionality.
 * This DTO handles pagination and search criteria for the user list screen,
 * replicating the VSAM browse operations from the original COBOL implementation.
 * 
 * BMS Field Mappings:
 * - startUserId: Maps to USRIDIN field (Search User ID, 8 characters)
 * - pageNumber: Maps to PAGENUM field (Page tracking)
 * - direction: Maps to F7=Backward/F8=Forward function key navigation
 * - pageSize: Based on screen capacity (10 user records per page)
 */
public class UserListRequest {

    /**
     * Optional starting user ID for pagination cursor and search.
     * Maps to USRIDIN BMS field from COUSR00 screen.
     * Used for VSAM STARTBR operations to position browse cursor.
     */
    private String startUserId;

    /**
     * Current page number for pagination tracking.
     * Maps to PAGENUM BMS field from COUSR00 screen.
     * Zero-based page numbering.
     */
    private Integer pageNumber;

    /**
     * Number of records to retrieve per page.
     * Defaults to 10 based on COUSR00 screen capacity (SEL0001-SEL0010).
     * Matches original COBOL implementation display limits.
     */
    private Integer pageSize;

    /**
     * Navigation direction for pagination.
     * Maps to F7=Backward/F8=Forward function keys from COUSR00 screen.
     * Used to determine VSAM browse direction (READNEXT vs READPREV).
     */
    private String direction;

    /**
     * Default constructor.
     */
    public UserListRequest() {
        this.pageSize = 10; // Default page size matching COUSR00 screen capacity
        this.pageNumber = 0; // Zero-based page numbering
        this.direction = "FORWARD"; // Default direction
    }

    /**
     * Constructor with all fields.
     * 
     * @param startUserId Optional starting user ID for search/pagination cursor
     * @param pageNumber Current page number (zero-based)
     * @param pageSize Number of records per page (default 10)
     * @param direction Navigation direction (FORWARD/BACKWARD)
     */
    public UserListRequest(String startUserId, Integer pageNumber, Integer pageSize, String direction) {
        this.startUserId = startUserId;
        this.pageNumber = pageNumber != null ? pageNumber : 0;
        this.pageSize = pageSize != null ? pageSize : 10;
        this.direction = direction != null ? direction : "FORWARD";
    }

    /**
     * Gets the starting user ID for pagination cursor and search.
     * 
     * @return the starting user ID, may be null
     */
    public String getStartUserId() {
        return startUserId;
    }

    /**
     * Sets the starting user ID for pagination cursor and search.
     * 
     * @param startUserId the starting user ID to set
     */
    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    /**
     * Gets the current page number.
     * 
     * @return the page number (zero-based)
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the current page number.
     * 
     * @param pageNumber the page number to set (zero-based)
     */
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber != null ? pageNumber : 0;
    }

    /**
     * Gets the page size (number of records per page).
     * 
     * @return the page size, defaults to 10
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Sets the page size (number of records per page).
     * 
     * @param pageSize the page size to set
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize != null ? pageSize : 10;
    }

    /**
     * Gets the navigation direction for pagination.
     * 
     * @return the direction (FORWARD/BACKWARD)
     */
    public String getDirection() {
        return direction;
    }

    /**
     * Sets the navigation direction for pagination.
     * 
     * @param direction the direction to set (FORWARD/BACKWARD)
     */
    public void setDirection(String direction) {
        this.direction = direction != null ? direction : "FORWARD";
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * 
     * @param obj the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UserListRequest that = (UserListRequest) obj;
        return Objects.equals(startUserId, that.startUserId) &&
               Objects.equals(pageNumber, that.pageNumber) &&
               Objects.equals(pageSize, that.pageSize) &&
               Objects.equals(direction, that.direction);
    }

    /**
     * Returns a hash code value for the object.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(startUserId, pageNumber, pageSize, direction);
    }

    /**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        return "UserListRequest{" +
               "startUserId='" + startUserId + '\'' +
               ", pageNumber=" + pageNumber +
               ", pageSize=" + pageSize +
               ", direction='" + direction + '\'' +
               '}';
    }
}