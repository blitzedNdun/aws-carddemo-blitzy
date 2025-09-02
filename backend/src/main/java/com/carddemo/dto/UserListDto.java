/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.Constants;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import java.util.Objects;

/**
 * Data Transfer Object for user list display in COUSR00 screen.
 * Represents a simplified user view for list operations with pagination support.
 * Maps to COBOL BMS COUSR00 map structure for displaying up to 10 users per page.
 * 
 * This DTO maintains field lengths matching COBOL PIC clause specifications:
 * - User ID: 8 characters (PIC X(8))
 * - First/Last Name: 20 characters each (PIC X(20))
 * - User Type: 1 character (PIC X(1))
 * - Selection Flag: 1 character (PIC X(1))
 */
@NoArgsConstructor
public class UserListDto {

    /**
     * User identification - primary key field.
     * Maps to COBOL USRID01I-USRID10I fields in COUSR00 BMS map.
     * Required field for user identification and selection operations.
     */
    @NotBlank(message = "User ID is required")
    @JsonProperty("userId")
    private String userId;

    /**
     * User's first name for display purposes.
     * Maps to COBOL FNAME01I-FNAME10I fields in COUSR00 BMS map.
     * Required field for user identification in list view.
     */
    @NotBlank(message = "First name is required")
    @JsonProperty("firstName")
    private String firstName;

    /**
     * User's last name for display purposes.
     * Maps to COBOL LNAME01I-LNAME10I fields in COUSR00 BMS map.
     * Required field for user identification in list view.
     */
    @NotBlank(message = "Last name is required")
    @JsonProperty("lastName")
    private String lastName;

    /**
     * User type classification (A=Admin, U=User, etc.).
     * Maps to COBOL UTYPE01I-UTYPE10I fields in COUSR00 BMS map.
     * Required field for role-based filtering and display.
     */
    @NotBlank(message = "User type is required")
    @JsonProperty("userType")
    private String userType;

    /**
     * Selection flag for user operations (Y/N or space).
     * Maps to COBOL SEL0001I-SEL0010I fields in COUSR00 BMS map.
     * Used for marking users for detail view or batch operations.
     */
    @JsonProperty("selectionFlag")
    private String selectionFlag;

    /**
     * Gets the user ID.
     * 
     * @return the user ID, maximum length determined by Constants.USER_ID_LENGTH
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID with length validation.
     * 
     * @param userId the user ID to set, must not exceed Constants.USER_ID_LENGTH
     */
    public void setUserId(String userId) {
        if (userId != null && userId.length() > Constants.USER_ID_LENGTH) {
            throw new IllegalArgumentException("User ID exceeds maximum length of " + Constants.USER_ID_LENGTH);
        }
        this.userId = userId;
    }

    /**
     * Gets the user's first name.
     * 
     * @return the first name, maximum length determined by Constants.USER_NAME_LENGTH
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name with length validation.
     * 
     * @param firstName the first name to set, must not exceed Constants.USER_NAME_LENGTH
     */
    public void setFirstName(String firstName) {
        if (firstName != null && firstName.length() > Constants.USER_NAME_LENGTH) {
            throw new IllegalArgumentException("First name exceeds maximum length of " + Constants.USER_NAME_LENGTH);
        }
        this.firstName = firstName;
    }

    /**
     * Gets the user's last name.
     * 
     * @return the last name, maximum length determined by Constants.USER_NAME_LENGTH
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name with length validation.
     * 
     * @param lastName the last name to set, must not exceed Constants.USER_NAME_LENGTH
     */
    public void setLastName(String lastName) {
        if (lastName != null && lastName.length() > Constants.USER_NAME_LENGTH) {
            throw new IllegalArgumentException("Last name exceeds maximum length of " + Constants.USER_NAME_LENGTH);
        }
        this.lastName = lastName;
    }

    /**
     * Gets the user type.
     * 
     * @return the user type, maximum length determined by Constants.USER_TYPE_LENGTH
     */
    public String getUserType() {
        return userType;
    }

    /**
     * Sets the user type with length validation.
     * 
     * @param userType the user type to set, must not exceed Constants.USER_TYPE_LENGTH
     */
    public void setUserType(String userType) {
        if (userType != null && userType.length() > Constants.USER_TYPE_LENGTH) {
            throw new IllegalArgumentException("User type exceeds maximum length of " + Constants.USER_TYPE_LENGTH);
        }
        this.userType = userType;
    }

    /**
     * Gets the selection flag.
     * 
     * @return the selection flag (Y/N or space)
     */
    public String getSelectionFlag() {
        return selectionFlag;
    }

    /**
     * Sets the selection flag.
     * 
     * @param selectionFlag the selection flag to set (typically Y, N, or space)
     */
    public void setSelectionFlag(String selectionFlag) {
        // Selection flag is typically single character but allowing flexibility
        if (selectionFlag != null && selectionFlag.length() > 1) {
            throw new IllegalArgumentException("Selection flag should be single character");
        }
        this.selectionFlag = selectionFlag;
    }

    /**
     * Checks equality based on userId as the primary identifier.
     * 
     * @param obj the object to compare with
     * @return true if objects are equal based on userId
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UserListDto that = (UserListDto) obj;
        return Objects.equals(userId, that.userId);
    }

    /**
     * Generates hash code based on userId as the primary identifier.
     * 
     * @return hash code for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    /**
     * Returns string representation of the UserListDto for debugging and logging.
     * Includes all fields for comprehensive object state representation.
     * 
     * @return formatted string representation of this object
     */
    @Override
    public String toString() {
        return "UserListDto{" +
                "userId='" + userId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userType='" + userType + '\'' +
                ", selectionFlag='" + selectionFlag + '\'' +
                '}';
    }
}