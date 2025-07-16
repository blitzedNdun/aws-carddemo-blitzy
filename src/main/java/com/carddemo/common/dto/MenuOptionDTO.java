package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

/**
 * Data Transfer Object representing a single menu option with option number, display text, 
 * target program/service, and access level information.
 * 
 * Used for transferring menu option data between React frontend and Spring Boot microservices 
 * while preserving COBOL menu option structure.
 * 
 * Maps to COBOL structures:
 * - CDEMO-MENU-OPT (COMEN02Y.cpy) - Main menu options
 * - CDEMO-ADMIN-OPT (COADM02Y.cpy) - Admin menu options
 * 
 * COBOL Field Mappings:
 * - optionNumber   -> CDEMO-MENU-OPT-NUM/CDEMO-ADMIN-OPT-NUM (PIC 9(02))
 * - displayText    -> CDEMO-MENU-OPT-NAME/CDEMO-ADMIN-OPT-NAME (PIC X(35))
 * - targetService  -> CDEMO-MENU-OPT-PGMNAME/CDEMO-ADMIN-OPT-PGMNAME (PIC X(08))
 * - accessLevel    -> CDEMO-MENU-OPT-USRTYPE (PIC X(01)) - Optional for admin menus
 */
public class MenuOptionDTO {

    /**
     * Menu option number (1-99).
     * Maps to COBOL PIC 9(02) field.
     */
    @JsonProperty("optionNumber")
    @NotNull(message = "Option number is required")
    @Min(value = 1, message = "Option number must be at least 1")
    @Max(value = 99, message = "Option number must be at most 99")
    private Integer optionNumber;

    /**
     * Display text for the menu option (up to 35 characters).
     * Maps to COBOL PIC X(35) field.
     */
    @JsonProperty("displayText")
    @NotBlank(message = "Display text is required")
    @Size(max = 35, message = "Display text must not exceed 35 characters")
    private String displayText;

    /**
     * Target service/program name to execute (up to 8 characters).
     * Maps to COBOL PIC X(08) field.
     * Represents the Spring Boot microservice endpoint that replaces the original CICS program.
     */
    @JsonProperty("targetService")
    @NotBlank(message = "Target service is required")
    @Size(max = 8, message = "Target service must not exceed 8 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Target service must contain only uppercase letters and numbers")
    private String targetService;

    /**
     * Access level required for this menu option (1 character).
     * Maps to COBOL PIC X(01) field.
     * Optional field as admin menu options don't have user type restrictions.
     * 
     * Valid values:
     * - 'A' = Admin access required
     * - 'U' = User access allowed
     * - null = No access restriction (admin menu items)
     */
    @JsonProperty("accessLevel")
    @Size(max = 1, message = "Access level must be 1 character")
    @Pattern(regexp = "^[AU]$", message = "Access level must be 'A' (Admin) or 'U' (User)")
    private String accessLevel;

    /**
     * Default constructor for JSON deserialization.
     */
    public MenuOptionDTO() {
    }

    /**
     * Constructor with all fields for complete menu option creation.
     *
     * @param optionNumber  the menu option number (1-99)
     * @param displayText   the display text (up to 35 characters)
     * @param targetService the target service name (up to 8 characters)
     * @param accessLevel   the access level ('A' or 'U'), optional
     */
    public MenuOptionDTO(Integer optionNumber, String displayText, String targetService, String accessLevel) {
        this.optionNumber = optionNumber;
        this.displayText = displayText;
        this.targetService = targetService;
        this.accessLevel = accessLevel;
    }

    /**
     * Constructor for admin menu options without access level.
     *
     * @param optionNumber  the menu option number (1-99)
     * @param displayText   the display text (up to 35 characters)
     * @param targetService the target service name (up to 8 characters)
     */
    public MenuOptionDTO(Integer optionNumber, String displayText, String targetService) {
        this.optionNumber = optionNumber;
        this.displayText = displayText;
        this.targetService = targetService;
        this.accessLevel = null;
    }

    /**
     * Gets the menu option number.
     *
     * @return the option number (1-99)
     */
    public Integer getOptionNumber() {
        return optionNumber;
    }

    /**
     * Sets the menu option number.
     *
     * @param optionNumber the option number (1-99)
     */
    public void setOptionNumber(Integer optionNumber) {
        this.optionNumber = optionNumber;
    }

    /**
     * Gets the display text for the menu option.
     *
     * @return the display text (up to 35 characters)
     */
    public String getDisplayText() {
        return displayText;
    }

    /**
     * Sets the display text for the menu option.
     *
     * @param displayText the display text (up to 35 characters)
     */
    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    /**
     * Gets the target service/program name.
     *
     * @return the target service name (up to 8 characters)
     */
    public String getTargetService() {
        return targetService;
    }

    /**
     * Sets the target service/program name.
     *
     * @param targetService the target service name (up to 8 characters)
     */
    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    /**
     * Gets the access level required for this menu option.
     *
     * @return the access level ('A' or 'U'), or null for no restriction
     */
    public String getAccessLevel() {
        return accessLevel;
    }

    /**
     * Sets the access level required for this menu option.
     *
     * @param accessLevel the access level ('A' or 'U'), or null for no restriction
     */
    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    /**
     * Checks if this menu option is restricted to admin users only.
     *
     * @return true if access level is 'A' (Admin), false otherwise
     */
    @JsonIgnore
    public boolean isAdminOnly() {
        return "A".equals(accessLevel);
    }

    /**
     * Checks if this menu option is available to regular users.
     *
     * @return true if access level is 'U' (User) or null, false if admin-only
     */
    @JsonIgnore
    public boolean isUserAccessible() {
        return !"A".equals(accessLevel);
    }

    /**
     * Returns a string representation of the menu option.
     *
     * @return formatted string with option number, display text, target service, and access level
     */
    @Override
    public String toString() {
        return String.format("MenuOptionDTO{optionNumber=%d, displayText='%s', targetService='%s', accessLevel='%s'}", 
                           optionNumber, displayText, targetService, accessLevel);
    }

    /**
     * Checks equality based on option number and target service.
     *
     * @param obj the object to compare
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        MenuOptionDTO that = (MenuOptionDTO) obj;
        
        if (optionNumber != null ? !optionNumber.equals(that.optionNumber) : that.optionNumber != null) return false;
        return targetService != null ? targetService.equals(that.targetService) : that.targetService == null;
    }

    /**
     * Generates hash code based on option number and target service.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        int result = optionNumber != null ? optionNumber.hashCode() : 0;
        result = 31 * result + (targetService != null ? targetService.hashCode() : 0);
        return result;
    }
}