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
 * This entity supports JWT-based Spring Security authentication with BCrypt password hashing
 * and role-based access control for the CardDemo modernized credit card management system.
 * 
 * Maps from COBOL copybook CSUSR01Y.cpy:
 * - SEC-USR-ID (PIC X(08)) -> user_id (VARCHAR(8)) - Primary key identifier
 * - SEC-USR-FNAME (PIC X(20)) -> first_name (VARCHAR(20)) - User first name
 * - SEC-USR-LNAME (PIC X(20)) -> last_name (VARCHAR(20)) - User last name  
 * - SEC-USR-PWD (PIC X(08)) -> password_hash (VARCHAR(60)) - BCrypt hashed passwords
 * - SEC-USR-TYPE (PIC X(01)) -> user_type (VARCHAR(1)) - Role-based access control
 * 
 * Additional audit fields for compliance and session management:
 * - created_at (TIMESTAMP) -> Account creation tracking for audit trail
 * - last_login (TIMESTAMP) -> Authentication tracking and security monitoring
 */
@Entity
@Table(name = "users", schema = "public")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key user identifier from COBOL SEC-USR-ID field.
     * Maps to PostgreSQL VARCHAR(8) with exact 8-character length constraint.
     * Used for JWT authentication and Spring Security principal identification.
     */
    @Id
    @Column(name = "user_id", length = 8, nullable = false)
    @NotBlank(message = "User ID is required and cannot be blank")
    @Size(max = 8, message = "User ID must not exceed 8 characters")
    private String userId;

    /**
     * User first name from COBOL SEC-USR-FNAME field.
     * Maps to PostgreSQL VARCHAR(20) maintaining exact COBOL field length.
     */
    @Column(name = "first_name", length = 20, nullable = false)
    @NotBlank(message = "First name is required and cannot be blank")
    @Size(max = 20, message = "First name must not exceed 20 characters")
    private String firstName;

    /**
     * User last name from COBOL SEC-USR-LNAME field.
     * Maps to PostgreSQL VARCHAR(20) maintaining exact COBOL field length.
     */
    @Column(name = "last_name", length = 20, nullable = false)
    @NotBlank(message = "Last name is required and cannot be blank")
    @Size(max = 20, message = "Last name must not exceed 20 characters")
    private String lastName;

    /**
     * BCrypt hashed password replacing COBOL SEC-USR-PWD plain text storage.
     * Maps to PostgreSQL VARCHAR(60) to accommodate BCrypt hash format.
     * Enhanced security through Spring Security BCrypt encoder with minimum 12 salt rounds.
     */
    @Column(name = "password_hash", length = 60, nullable = false)
    @NotBlank(message = "Password hash is required and cannot be blank")
    @Size(max = 60, message = "Password hash must not exceed 60 characters")
    private String passwordHash;

    /**
     * User type for role-based access control from COBOL SEC-USR-TYPE field.
     * Maps to PostgreSQL VARCHAR(1) with Spring Security role mapping:
     * - 'A' -> ROLE_ADMIN (Administrative privileges including user management)
     * - 'U' -> ROLE_USER (Standard transaction processing access)
     */
    @Column(name = "user_type", length = 1, nullable = false)
    @NotBlank(message = "User type is required and cannot be blank")
    @Size(min = 1, max = 1, message = "User type must be exactly 1 character")
    private String userType;

    /**
     * Account creation timestamp for audit trail compliance.
     * Automatically set to current timestamp when entity is first persisted.
     * Required for SOX compliance and security monitoring.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last successful login timestamp for authentication tracking.
     * Updated automatically through Hibernate @UpdateTimestamp annotation.
     * Used for security monitoring and compliance reporting.
     */
    @UpdateTimestamp
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /**
     * Default constructor required by JPA specification.
     */
    public User() {
        // Initialize created_at for new entities
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * Constructor for creating new User entities with required fields.
     * Automatically sets created_at timestamp for audit compliance.
     *
     * @param userId User identifier (max 8 characters)
     * @param firstName User first name (max 20 characters)
     * @param lastName User last name (max 20 characters)
     * @param passwordHash BCrypt hashed password (max 60 characters)
     * @param userType Role identifier ('A' for Admin, 'U' for User)
     */
    public User(String userId, String firstName, String lastName, String passwordHash, String userType) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.userType = userType;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Gets the user ID primary key.
     *
     * @return User identifier string (max 8 characters)
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID primary key.
     *
     * @param userId User identifier string (max 8 characters)
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the user first name.
     *
     * @return First name string (max 20 characters)
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user first name.
     *
     * @param firstName First name string (max 20 characters)
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the user last name.
     *
     * @return Last name string (max 20 characters)
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user last name.
     *
     * @param lastName Last name string (max 20 characters)
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the BCrypt hashed password.
     *
     * @return BCrypt password hash (max 60 characters)
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the BCrypt hashed password.
     *
     * @param passwordHash BCrypt password hash (max 60 characters)
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Gets the user type for role-based access control.
     *
     * @return User type character ('A' for Admin, 'U' for User)
     */
    public String getUserType() {
        return userType;
    }

    /**
     * Sets the user type for role-based access control.
     *
     * @param userType User type character ('A' for Admin, 'U' for User)
     */
    public void setUserType(String userType) {
        this.userType = userType;
    }

    /**
     * Gets the account creation timestamp.
     *
     * @return Account creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the account creation timestamp.
     * Typically set automatically during entity construction.
     *
     * @param createdAt Account creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the last login timestamp.
     *
     * @return Last successful login timestamp
     */
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    /**
     * Sets the last login timestamp.
     * Updated automatically via @UpdateTimestamp annotation.
     *
     * @param lastLogin Last successful login timestamp
     */
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * Determines if the user has administrative privileges.
     *
     * @return true if user type is 'A' (Admin), false otherwise
     */
    public boolean isAdmin() {
        return "A".equals(this.userType);
    }

    /**
     * Determines if the user has standard user privileges.
     *
     * @return true if user type is 'U' (User), false otherwise
     */
    public boolean isRegularUser() {
        return "U".equals(this.userType);
    }

    /**
     * Gets the user's full name for display purposes.
     *
     * @return Concatenated first and last name
     */
    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    /**
     * JPA lifecycle callback to ensure created_at is set before persistence.
     * Guarantees audit timestamp is always present for new entities.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * Equals implementation for JPA entity identity and distributed cache consistency.
     * Based on user ID primary key for proper entity comparison.
     *
     * @param obj Object to compare with this user entity
     * @return true if objects are equal based on user ID, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return Objects.equals(userId, user.userId);
    }

    /**
     * Hash code implementation for JPA entity identity and distributed cache keys.
     * Based on user ID primary key for consistent hash generation.
     *
     * @return Hash code based on user ID
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    /**
     * String representation of User entity for debugging and logging.
     * Excludes password hash for security reasons.
     *
     * @return String representation with non-sensitive fields
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