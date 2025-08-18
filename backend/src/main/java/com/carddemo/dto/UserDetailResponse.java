package com.carddemo.dto;

/**
 * Response DTO for user detail operations containing comprehensive user profile 
 * information, role details, permission levels, last login tracking, account status, 
 * and security settings from COUSR01C.cbl translation.
 */
public class UserDetailResponse {
    
    private String userId;
    private String firstName;
    private String lastName;
    private String userType;
    private String roleDescription;
    private String permissionLevel;
    private String accountStatus;
    private Boolean accountLocked;
    private Boolean passwordExpired;
    
    // Default constructor
    public UserDetailResponse() {}
    
    // Getters and setters
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
    
    public String getUserType() {
        return userType;
    }
    
    public void setUserType(String userType) {
        this.userType = userType;
    }
    
    public String getRoleDescription() {
        return roleDescription;
    }
    
    public void setRoleDescription(String roleDescription) {
        this.roleDescription = roleDescription;
    }
    
    public String getPermissionLevel() {
        return permissionLevel;
    }
    
    public void setPermissionLevel(String permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
    
    public String getAccountStatus() {
        return accountStatus;
    }
    
    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }
    
    public Boolean getAccountLocked() {
        return accountLocked;
    }
    
    public void setAccountLocked(Boolean accountLocked) {
        this.accountLocked = accountLocked;
    }
    
    public Boolean getPasswordExpired() {
        return passwordExpired;
    }
    
    public void setPasswordExpired(Boolean passwordExpired) {
        this.passwordExpired = passwordExpired;
    }
}