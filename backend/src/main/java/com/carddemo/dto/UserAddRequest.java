/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for user creation requests containing user identification, personal information, and security settings.
 * Maps to CSUSR01Y copybook structure (SEC-USER-DATA) for new user addition operations through COUSR03C-equivalent service logic.
 * 
 * This DTO preserves the exact field lengths and constraints from the original COBOL copybook:
 * - SEC-USR-ID: PIC X(08) -> userId (8 characters maximum)
 * - SEC-USR-FNAME: PIC X(20) -> firstName (20 characters maximum)
 * - SEC-USR-LNAME: PIC X(20) -> lastName (20 characters maximum)
 * - SEC-USR-TYPE: PIC X(01) -> userType (1 character)
 * 
 * Additional fields support modern user creation workflows including password generation
 * and role assignment for Spring Security integration.
 */
@Data
public class UserAddRequest {

    /**
     * User ID - must be exactly 8 characters or less, matching SEC-USR-ID from CSUSR01Y copybook.
     * Primary identifier for user account creation and authentication.
     */
    @JsonProperty("userId")
    @Size(max = 8, message = "User ID must not exceed 8 characters")
    private String userId;

    /**
     * First name - must be 20 characters or less, matching SEC-USR-FNAME from CSUSR01Y copybook.
     * User's given name for display and identification purposes.
     */
    @JsonProperty("firstName")
    @Size(max = 20, message = "First name must not exceed 20 characters")
    private String firstName;

    /**
     * Last name - must be 20 characters or less, matching SEC-USR-LNAME from CSUSR01Y copybook.
     * User's family name for display and identification purposes.
     */
    @JsonProperty("lastName")
    @Size(max = 20, message = "Last name must not exceed 20 characters")
    private String lastName;

    /**
     * User type - must be exactly 1 character, matching SEC-USR-TYPE from CSUSR01Y copybook.
     * Defines user role and access level within the application:
     * - 'A' for Admin users with full system access
     * - 'R' for Regular users with limited access
     */
    @JsonProperty("userType")
    @Size(min = 1, max = 1, message = "User type must be exactly 1 character")
    private String userType;

    /**
     * Password for the new user account.
     * Stored temporarily in DTO for processing, will be hashed before database storage.
     * Matches SEC-USR-PWD length constraint from copybook (8 characters maximum).
     */
    @JsonProperty("password")
    @Size(max = 8, message = "Password must not exceed 8 characters")
    private String password;

    /**
     * Flag indicating whether to generate a random password for the user.
     * When true, the system will generate a secure password and override any provided password value.
     * Default value is false, requiring explicit password to be provided.
     */
    @JsonProperty("generatePassword")
    private Boolean generatePassword = false;

    /**
     * Role assignment for Spring Security integration.
     * Maps user type to specific application roles for authorization.
     * Examples: "ROLE_ADMIN", "ROLE_USER", "ROLE_MANAGER"
     */
    @JsonProperty("assignedRole")
    @Size(max = 50, message = "Assigned role must not exceed 50 characters")
    private String assignedRole;

    /**
     * Flag indicating whether the user account should be created in active status.
     * Default value is true, creating active accounts ready for immediate use.
     * Can be set to false for accounts requiring additional approval steps.
     */
    @JsonProperty("activeStatus")
    private Boolean activeStatus = true;

    /**
     * Default constructor for JSON deserialization and Spring framework usage.
     */
    public UserAddRequest() {
        // Default constructor for framework usage
    }

    /**
     * Constructor with core user information from COBOL copybook structure.
     * 
     * @param userId User ID (8 characters maximum)
     * @param firstName First name (20 characters maximum)
     * @param lastName Last name (20 characters maximum)
     * @param userType User type (1 character)
     */
    public UserAddRequest(String userId, String firstName, String lastName, String userType) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userType = userType;
    }

    /**
     * Full constructor with all fields for comprehensive user creation.
     * 
     * @param userId User ID (8 characters maximum)
     * @param firstName First name (20 characters maximum)
     * @param lastName Last name (20 characters maximum)
     * @param userType User type (1 character)
     * @param password User password (8 characters maximum)
     * @param generatePassword Flag to generate random password
     * @param assignedRole Spring Security role assignment
     * @param activeStatus Account active status flag
     */
    public UserAddRequest(String userId, String firstName, String lastName, String userType,
                         String password, Boolean generatePassword, String assignedRole, Boolean activeStatus) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userType = userType;
        this.password = password;
        this.generatePassword = generatePassword;
        this.assignedRole = assignedRole;
        this.activeStatus = activeStatus;
    }
}