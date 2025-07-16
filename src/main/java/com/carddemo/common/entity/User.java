/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA User entity mapping COBOL SEC-USER-DATA structure to PostgreSQL users table.
 * This entity supports JWT-based Spring Security authentication with BCrypt password hashing.
 * 
 * Maps from COBOL copybook CSUSR01Y.cpy:
 * - SEC-USR-ID (PIC X(08)) -> user_id (VARCHAR(8))
 * - SEC-USR-FNAME (PIC X(20)) -> first_name (VARCHAR(20))
 * - SEC-USR-LNAME (PIC X(20)) -> last_name (VARCHAR(20))
 * - SEC-USR-PWD (PIC X(08)) -> password_hash (VARCHAR(60) for BCrypt)
 * - SEC-USR-TYPE (PIC X(01)) -> user_type (VARCHAR(1))
 * 
 * Additional fields for audit trail and Spring Security integration:
 * - created_at (TIMESTAMP) -> Account creation timestamp
 * - last_login (TIMESTAMP) -> Last successful authentication timestamp
 */
@Entity
@Table(name = "users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key identifier from COBOL SEC-USR-ID field.
     * Maps to PostgreSQL VARCHAR(8) with exact field length from COBOL structure.
     */
    @Id
    @Column(name = "user_id", length = 8, nullable = false)
    @NotBlank(message = "User ID is required")
    @Size(max = 8, message = "User ID must not exceed 8 characters")
    private String userId;

    /**
     * User first name from COBOL SEC-USR-FNAME field.
     * Maps to PostgreSQL VARCHAR(20) with exact field length from COBOL structure.
     */
    @Column(name = "first_name", length = 20, nullable = false)
    @NotBlank(message = "First name is required")
    @Size(max = 20, message = "First name must not exceed 20 characters")
    private String firstName;

    /**
     * User last name from COBOL SEC-USR-LNAME field.
     * Maps to PostgreSQL VARCHAR(20) with exact field length from COBOL structure.
     */
    @Column(name = "last_name", length = 20, nullable = false)
    @NotBlank(message = "Last name is required")
    @Size(max = 20, message = "Last name must not exceed 20 characters")
    private String lastName;

    /**
     * BCrypt hashed password from COBOL SEC-USR-PWD field.
     * Expanded from original 8 characters to 60 characters to accommodate BCrypt hash format.
     * Enhanced security compliance per Section 6.2.3.3 of technical specification.
     */
    @Column(name = "password_hash", length = 60, nullable = false)
    @NotBlank(message = "Password hash is required")
    @Size(max = 60, message = "Password hash must not exceed 60 characters")
    private String passwordHash;

    /**
     * User type for role-based access control from COBOL SEC-USR-TYPE field.
     * Maps to PostgreSQL VARCHAR(1) with exact field length from COBOL structure.
     * Values: 'A' for Admin users, 'U' for regular users.
     */
    @Column(name = "user_type", length = 1, nullable = false)
    @NotBlank(message = "User type is required")
    @Size(max = 1, message = "User type must be exactly 1 character")
    private String userType;

    /**
     * Account creation timestamp for audit trail compliance.
     * Required per Section 6.2.3.4 for comprehensive audit mechanisms.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last successful login timestamp for authentication tracking.
     * Automatically updated on successful authentication events.
     * Used for security monitoring and compliance per Section 6.2.3.3.
     */
    @UpdateTimestamp
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /**
     * Default constructor for JPA entity instantiation.
     */
    public User() {
        // Set creation timestamp on entity creation
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor for creating User entity with required fields.
     * 
     * @param userId The user identifier from COBOL SEC-USR-ID
     * @param firstName The user's first name from COBOL SEC-USR-FNAME
     * @param lastName The user's last name from COBOL SEC-USR-LNAME
     * @param passwordHash The BCrypt hashed password
     * @param userType The user type for role-based access control
     */
    public User(String userId, String firstName, String lastName, String passwordHash, String userType) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.userType = userType;
        this.createdAt = LocalDateTime.now();
    }

    // Getter and setter methods for all fields

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * Convenience method to check if user is an administrator.
     * 
     * @return true if user type is 'A' (Admin), false otherwise
     */
    public boolean isAdmin() {
        return "A".equals(userType);
    }

    /**
     * Convenience method to check if user is a regular user.
     * 
     * @return true if user type is 'U' (User), false otherwise
     */
    public boolean isRegularUser() {
        return "U".equals(userType);
    }

    /**
     * Equals method for entity identity comparison.
     * Uses user_id as the primary key for comparison.
     * 
     * @param o Object to compare with this instance
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    /**
     * Hash code method for entity identity hashing.
     * Uses user_id as the primary key for hash generation.
     * 
     * @return hash code value for this entity
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    /**
     * String representation of the User entity.
     * Excludes sensitive password hash for security.
     * 
     * @return String representation of the user
     */
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userType='" + userType + '\'' +
                ", createdAt=" + createdAt +
                ", lastLogin=" + lastLogin +
                '}';
    }
}