package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity class for business user profile information, mapped to users PostgreSQL table.
 * Contains user business profile data separate from security credentials.
 * This entity complements UserSecurity by providing additional business-focused user attributes.
 *
 * This entity represents the business user data that supplements security information,
 * providing a separation of concerns between authentication/authorization (UserSecurity)
 * and business profile data (User).
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_user_id", columnList = "user_id"),
    @Index(name = "idx_users_email", columnList = "email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Business User ID (8 characters) - matches SEC-USR-ID from UserSecurity
     */
    @Column(name = "user_id", length = 8, nullable = false, unique = true)
    @NotNull
    @Size(max = 8)
    private String userId;

    /**
     * User's first name for business profile
     */
    @Column(name = "first_name", length = 25)
    @Size(max = 25)
    private String firstName;

    /**
     * User's last name for business profile
     */
    @Column(name = "last_name", length = 25)
    @Size(max = 25)
    private String lastName;

    /**
     * User's email address
     */
    @Column(name = "email", length = 100)
    @Size(max = 100)
    private String email;

    /**
     * User's phone number
     */
    @Column(name = "phone", length = 15)
    @Size(max = 15)
    private String phone;

    /**
     * User's department or division
     */
    @Column(name = "department", length = 50)
    @Size(max = 50)
    private String department;

    /**
     * Date when user profile was created
     */
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    /**
     * Date when user profile was last updated
     */
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    /**
     * User who created this profile
     */
    @Column(name = "created_by", length = 25)
    @Size(max = 25)
    private String createdBy;

    /**
     * User who last updated this profile
     */
    @Column(name = "updated_by", length = 25)
    @Size(max = 25)
    private String updatedBy;

    /**
     * User type for business profile (A=Admin, U=User, etc.)
     */
    @Column(name = "user_type", length = 1)
    @Size(max = 1)
    private String userType;

    /**
     * User status (A=Active, I=Inactive, S=Suspended, etc.)
     */
    @Column(name = "status", length = 1)
    @Size(max = 1)
    private String status;

    // Default constructor for JPA
    public User() {
    }

    // Constructor with essential fields
    public User(String userId, String firstName, String lastName) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdDate = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // JPA lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }

    // Equals and hashCode based on userId (business key)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}