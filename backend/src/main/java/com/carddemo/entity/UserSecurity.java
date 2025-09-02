package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import com.carddemo.security.SecurityConstants;

/**
 * JPA entity class for user authentication and authorization, mapped to user_security PostgreSQL table.
 * Contains user credentials, profile information, and role/type indicators. Integrates with Spring Security
 * for authentication. Implements UserDetails interface for Spring Security integration.
 * Stores password as BCrypt hash for security compliance.
 *
 * This entity represents the security-related user data from COBOL CSUSR01Y copybook,
 * providing authentication capabilities for the modernized credit card management system.
 */
@Entity
@Table(name = "user_security")
public class UserSecurity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * User ID from COBOL SEC-USR-ID field (8 characters)
     */
    @Column(name = "sec_usr_id", length = 8, nullable = false, unique = true)
    @NotNull
    @Size(max = 8)
    private String secUsrId;

    /**
     * Username for login (derived from user ID or separate field)
     */
    @Column(name = "username", length = 50, nullable = false, unique = true)
    @NotNull
    @Size(max = 50)
    private String username;

    /**
     * Encrypted password using BCrypt
     */
    @Column(name = "password_hash", length = 100, nullable = false)
    @NotNull
    private String password;

    /**
     * First name from COBOL SEC-USR-FNAME field (20 characters)
     */
    @Column(name = "first_name", length = 20, nullable = false)
    @NotNull
    @Size(max = 20)
    private String firstName;

    /**
     * Last name from COBOL SEC-USR-LNAME field (20 characters)
     */
    @Column(name = "last_name", length = 20, nullable = false)
    @NotNull
    @Size(max = 20)
    private String lastName;

    /**
     * User type from COBOL SEC-USR-TYPE field
     * 'A' = Administrator, 'U' = Regular User
     */
    @Column(name = "sec_usr_type", length = 1, nullable = false)
    @NotNull
    @Size(min = 1, max = 1)
    private String userType;

    /**
     * Account enabled flag
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /**
     * Account non-expired flag
     */
    @Column(name = "account_non_expired", nullable = false)
    private boolean accountNonExpired = true;

    /**
     * Account non-locked flag
     */
    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true;

    /**
     * Credentials non-expired flag
     */
    @Column(name = "credentials_non_expired", nullable = false)
    private boolean credentialsNonExpired = true;

    /**
     * Failed login attempts counter
     */
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    /**
     * Last login timestamp
     */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /**
     * Account creation timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Filler field from COBOL for compatibility (not used)
     */
    @Column(name = "filler", length = 23)
    private String filler;

    // Constructors

    /**
     * Default constructor for JPA
     */
    public UserSecurity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Constructor with essential fields
     */
    public UserSecurity(String secUsrId, String username, String password, 
                       String firstName, String lastName, String userType) {
        this();
        this.secUsrId = secUsrId;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userType = userType;
    }

    // JPA lifecycle callbacks

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // UserDetails interface implementation

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = SecurityConstants.getRoleForUserType(this.userType);
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSecUsrId() {
        return secUsrId;
    }

    public void setSecUsrId(String secUsrId) {
        this.secUsrId = secUsrId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFiller() {
        return filler;
    }

    public void setFiller(String filler) {
        this.filler = filler;
    }

    // Additional methods for compatibility with JWT service

    /**
     * Gets the user ID (alias for getSecUsrId for JWT compatibility)
     */
    public String getUserId() {
        return this.secUsrId;
    }

    /**
     * Gets the full display name
     */
    public String getDisplayName() {
        return this.firstName + " " + this.lastName;
    }

    /**
     * Increments failed login attempts
     */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    /**
     * Resets failed login attempts
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
    }

    /**
     * Checks if account should be locked due to failed attempts
     */
    public boolean shouldLockAccount() {
        return this.failedLoginAttempts >= SecurityConstants.MAX_LOGIN_ATTEMPTS;
    }

    /**
     * Updates last login timestamp
     */
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    // Standard Object methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSecurity that = (UserSecurity) o;
        return Objects.equals(secUsrId, that.secUsrId) && 
               Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secUsrId, username);
    }

    @Override
    public String toString() {
        return "UserSecurity{" +
                "id=" + id +
                ", secUsrId='" + secUsrId + '\'' +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userType='" + userType + '\'' +
                ", enabled=" + enabled +
                ", createdAt=" + createdAt +
                '}';
    }
}