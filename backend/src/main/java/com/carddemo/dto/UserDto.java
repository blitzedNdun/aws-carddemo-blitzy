/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.Constants;
import com.carddemo.util.ValidationUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Objects;

/**
 * User Data Transfer Object for user management operations.
 * Maps to COUSR00/COUSR02 BMS screens for user listing, creation, update, and deletion.
 * Provides Spring Security integration for authentication and authorization.
 * 
 * Field mappings from COBOL BMS screens:
 * - userId: USRIDINI (PIC X(8)) - User identification field
 * - firstName: FNAMEI (PIC X(20)) - User's first name
 * - lastName: LNAMEI (PIC X(20)) - User's last name  
 * - userType: USRTYPEI (PIC X(1)) - User role (A=Admin, U=User)
 * - password: PASSWDI (PIC X(8)) - Optional password for updates
 * 
 * This DTO supports all CRUD operations while maintaining exact functional parity
 * with the original COBOL implementation. Validation rules match COBOL business logic
 * to ensure consistent behavior across the modernized application.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    /**
     * User identification field. Maps to USRIDINI from COUSR00/COUSR02 screens.
     * Must be 1-8 alphanumeric characters. Required for all operations.
     */
    @NotBlank(message = "User ID must be supplied")
    @JsonProperty("userId")
    private String userId;

    /**
     * User's first name. Maps to FNAMEI from COUSR00/COUSR02 screens.
     * Maximum 20 characters. Required field for user creation and updates.
     */
    @NotBlank(message = "First name must be supplied")
    @JsonProperty("firstName")
    private String firstName;

    /**
     * User's last name. Maps to LNAMEI from COUSR00/COUSR02 screens.
     * Maximum 20 characters. Required field for user creation and updates.
     */
    @NotBlank(message = "Last name must be supplied")
    @JsonProperty("lastName")
    private String lastName;

    /**
     * User type/role field. Maps to USRTYPEI from COUSR00/COUSR02 screens.
     * Single character field: 'A' for Admin, 'U' for User.
     * Required field determining user permissions and access levels.
     */
    @NotBlank(message = "User type must be supplied")
    @JsonProperty("userType")
    private String userType;

    /**
     * User password field. Maps to PASSWDI from COUSR02 screen.
     * Maximum 8 characters. Optional field used for password updates.
     * Not included in JSON responses for security purposes.
     */
    @JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * Gets the user ID.
     * 
     * @return the user ID string
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID with validation.
     * 
     * @param userId the user ID to set (1-8 alphanumeric characters)
     */
    public void setUserId(String userId) {
        if (userId != null) {
            ValidationUtil.validateUserId(userId);
            ValidationUtil.validateFieldLength("userId", userId, Constants.USER_ID_LENGTH);
        }
        this.userId = userId;
    }

    /**
     * Gets the user's first name.
     * 
     * @return the first name string
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name with validation.
     * 
     * @param firstName the first name to set (maximum 20 characters)
     */
    public void setFirstName(String firstName) {
        if (firstName != null) {
            ValidationUtil.validateRequiredField("firstName", firstName);
            ValidationUtil.validateFieldLength("firstName", firstName, Constants.USER_NAME_LENGTH);
        }
        this.firstName = firstName;
    }

    /**
     * Gets the user's last name.
     * 
     * @return the last name string
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name with validation.
     * 
     * @param lastName the last name to set (maximum 20 characters)
     */
    public void setLastName(String lastName) {
        if (lastName != null) {
            ValidationUtil.validateRequiredField("lastName", lastName);
            ValidationUtil.validateFieldLength("lastName", lastName, Constants.USER_NAME_LENGTH);
        }
        this.lastName = lastName;
    }

    /**
     * Gets the user type/role.
     * 
     * @return the user type string ('A' for Admin, 'U' for User)
     */
    public String getUserType() {
        return userType;
    }

    /**
     * Sets the user type/role with validation.
     * 
     * @param userType the user type to set ('A' for Admin, 'U' for User)
     */
    public void setUserType(String userType) {
        if (userType != null) {
            ValidationUtil.validateUserType(userType);
            ValidationUtil.validateFieldLength("userType", userType, Constants.USER_TYPE_LENGTH);
        }
        this.userType = userType;
    }

    /**
     * Gets the user password.
     * 
     * @return the password string (for updates only)
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the user password with validation.
     * 
     * @param password the password to set (maximum 8 characters)
     */
    public void setPassword(String password) {
        if (password != null && !password.trim().isEmpty()) {
            ValidationUtil.validatePassword(password);
            ValidationUtil.validateFieldLength("password", password, 8); // PASSWORD_LENGTH = 8 from COUSR02.CPY
        }
        this.password = password;
    }

    /**
     * Gets Spring Security authorities based on user type.
     * Maps userType field to GrantedAuthority objects for role-based access control.
     * 
     * @return list of GrantedAuthority objects for Spring Security integration
     */
    public List<GrantedAuthority> getAuthorities() {
        if (Constants.USER_TYPE_ADMIN.equals(this.userType)) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else if (Constants.USER_TYPE_USER.equals(this.userType)) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        } else {
            return List.of(new SimpleGrantedAuthority("ROLE_USER")); // Default to USER role
        }
    }

    /**
     * Compares this UserDto with another object for equality.
     * Two UserDto objects are equal if they have the same userId.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserDto userDto = (UserDto) obj;
        return Objects.equals(userId, userDto.userId);
    }

    /**
     * Returns a hash code value for this UserDto.
     * Hash code is based on the userId field as it's the unique identifier.
     * 
     * @return hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    /**
     * Returns a string representation of this UserDto.
     * Excludes password field for security purposes.
     * 
     * @return string representation of this UserDto
     */
    @Override
    public String toString() {
        return "UserDto{" +
                "userId='" + userId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userType='" + userType + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}