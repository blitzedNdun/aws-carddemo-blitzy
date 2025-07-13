package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * User JPA Entity - Maps COBOL SEC-USER-DATA structure to PostgreSQL users table
 * 
 * This entity provides JWT-based authentication support with Spring Security integration,
 * replacing the legacy RACF authentication model with modern BCrypt password hashing.
 * The field mappings preserve exact COBOL data structure precision while enabling
 * cloud-native microservices authentication patterns.
 * 
 * Original COBOL Structure (CSUSR01Y.cpy):
 * - SEC-USR-ID        PIC X(08) -> user_id VARCHAR(8) Primary Key
 * - SEC-USR-FNAME     PIC X(20) -> first_name VARCHAR(20)
 * - SEC-USR-LNAME     PIC X(20) -> last_name VARCHAR(20)  
 * - SEC-USR-PWD       PIC X(08) -> password_hash VARCHAR(60) BCrypt expanded
 * - SEC-USR-TYPE      PIC X(01) -> user_type VARCHAR(1) Role-based access
 * 
 * Spring Security Integration:
 * - BCrypt password hashing for enhanced security compliance
 * - User type field enables role-based access control (ROLE_USER, ROLE_ADMIN)
 * - Audit timestamps support authentication tracking and compliance requirements
 * - Serializable interface enables Redis session management for distributed microservices
 */
@Entity
@Table(name = "users", schema = "carddemo")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * User ID - Primary key field mapping SEC-USR-ID from COBOL structure
     * 8-character fixed length identifier preserving VSAM key structure
     */
    @Id
    @Column(name = "user_id", length = 8, nullable = false)
    @NotBlank(message = "User ID cannot be blank")
    @Size(max = 8, message = "User ID cannot exceed 8 characters")
    private String userId;

    /**
     * First Name - Maps SEC-USR-FNAME with exact 20-character COBOL field length
     */
    @Column(name = "first_name", length = 20, nullable = false)
    @NotBlank(message = "First name cannot be blank")
    @Size(max = 20, message = "First name cannot exceed 20 characters")
    private String firstName;

    /**
     * Last Name - Maps SEC-USR-LNAME with exact 20-character COBOL field length
     */
    @Column(name = "last_name", length = 20, nullable = false)
    @NotBlank(message = "Last name cannot be blank")
    @Size(max = 20, message = "Last name cannot exceed 20 characters")
    private String lastName;

    /**
     * Password Hash - Enhanced from COBOL PIC X(08) to VARCHAR(60) for BCrypt hashing
     * Supports Spring Security BCrypt password encoder with configurable salt rounds
     */
    @Column(name = "password_hash", length = 60, nullable = false)
    @NotBlank(message = "Password hash cannot be blank")
    @Size(max = 60, message = "Password hash cannot exceed 60 characters")
    private String passwordHash;

    /**
     * User Type - Maps SEC-USR-TYPE for role-based access control
     * 'A' = Admin user (ROLE_ADMIN), 'R' = Regular user (ROLE_USER)
     * Enables Spring Security authorization and menu filtering
     */
    @Column(name = "user_type", length = 1, nullable = false)
    @NotBlank(message = "User type cannot be blank")
    @Size(min = 1, max = 1, message = "User type must be exactly 1 character")
    private String userType;

    /**
     * Account Creation Timestamp - Audit trail for user provisioning
     * Automatically populated on entity creation via Hibernate annotation
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last Login Timestamp - Authentication tracking for security compliance
     * Automatically updated on successful authentication via Hibernate annotation
     */
    @UpdateTimestamp
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /**
     * Default constructor required by JPA specification
     */
    public User() {
        // JPA requires default constructor
    }

    /**
     * Constructor for user creation with required fields
     * 
     * @param userId      8-character user identifier
     * @param firstName   User's first name (max 20 chars)
     * @param lastName    User's last name (max 20 chars)
     * @param passwordHash BCrypt hashed password (60 chars)
     * @param userType    Single character user type ('A' or 'R')
     */
    public User(String userId, String firstName, String lastName, String passwordHash, String userType) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.userType = userType;
        this.createdAt = LocalDateTime.now();
    }

    // Getter and Setter Methods

    /**
     * Gets the user ID primary key
     * @return 8-character user identifier
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID primary key
     * @param userId 8-character user identifier
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the user's first name
     * @return First name (max 20 characters)
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name
     * @param firstName First name (max 20 characters)
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the user's last name
     * @return Last name (max 20 characters)
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name
     * @param lastName Last name (max 20 characters)
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the BCrypt password hash
     * @return BCrypt hashed password (60 characters)
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the BCrypt password hash
     * @param passwordHash BCrypt hashed password (60 characters)
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Gets the user type for role-based access control
     * @return Single character user type ('A' for Admin, 'R' for Regular)
     */
    public String getUserType() {
        return userType;
    }

    /**
     * Sets the user type for role-based access control
     * @param userType Single character user type ('A' for Admin, 'R' for Regular)
     */
    public void setUserType(String userType) {
        this.userType = userType;
    }

    /**
     * Gets the account creation timestamp
     * @return Timestamp when user account was created
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the account creation timestamp
     * @param createdAt Timestamp when user account was created
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the last login timestamp
     * @return Timestamp of last successful authentication
     */
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    /**
     * Sets the last login timestamp
     * @param lastLogin Timestamp of last successful authentication
     */
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * Convenience method to check if user is an administrator
     * @return true if user type is 'A' (Admin), false otherwise
     */
    public boolean isAdmin() {
        return "A".equals(userType);
    }

    /**
     * Convenience method to get full name
     * @return Concatenated first and last name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Entity equality based on primary key (user_id)
     * Required for JPA entity identity and distributed cache key consistency
     * 
     * @param obj Object to compare with this entity
     * @return true if objects have equal user_id, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        User user = (User) obj;
        return Objects.equals(userId, user.userId);
    }

    /**
     * Hash code based on primary key for consistent entity identity
     * Ensures proper behavior in collections and distributed caches
     * 
     * @return Hash code based on user_id field
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    /**
     * String representation for logging and debugging
     * Excludes sensitive password information for security
     * 
     * @return String representation of user entity
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