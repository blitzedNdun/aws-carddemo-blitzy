package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object representing a single menu option with option number, 
 * display text, target program/service, and access level information.
 * 
 * This DTO preserves the exact structure of the original COBOL menu option
 * definitions from COMEN02Y.cpy and COADM02Y.cpy copybooks:
 * - CDEMO-MENU-OPT-NUM (PIC 9(02)) -> optionNumber
 * - CDEMO-MENU-OPT-NAME (PIC X(35)) -> displayText  
 * - CDEMO-MENU-OPT-PGMNAME (PIC X(08)) -> targetService
 * - CDEMO-MENU-OPT-USRTYPE (PIC X(01)) -> accessLevel (optional for admin menus)
 * 
 * Used for transferring menu option data between React frontend and Spring Boot 
 * microservices while maintaining compatibility with the original COBOL menu 
 * option structure and supporting modern JSON REST API communication.
 * 
 * @author CardDemo Application - Blitzy agent
 * @version 1.0
 * @since Java 21
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuOptionDTO {

    /**
     * Menu option number corresponding to CDEMO-MENU-OPT-NUM PIC 9(02).
     * Valid range: 1-99 (2-digit numeric value from original COBOL definition).
     */
    @NotNull(message = "Option number is required")
    @Min(value = 1, message = "Option number must be at least 1")
    @Max(value = 99, message = "Option number must not exceed 99")
    @JsonProperty("optionNumber")
    private Integer optionNumber;

    /**
     * Menu option display text corresponding to CDEMO-MENU-OPT-NAME PIC X(35).
     * Contains the descriptive text shown to users in the menu interface.
     * Maximum length: 35 characters to match COBOL field definition.
     */
    @NotNull(message = "Display text is required")
    @Size(max = 35, message = "Display text must not exceed 35 characters")
    @JsonProperty("displayText")
    private String displayText;

    /**
     * Target service/program name corresponding to CDEMO-MENU-OPT-PGMNAME PIC X(08).
     * Originally contained COBOL program names (e.g., COACTVWC, COACTUPC), now 
     * represents Spring Boot microservice endpoints or React component routes.
     * Maximum length: 8 characters to match COBOL field definition.
     */
    @NotNull(message = "Target service is required")
    @Size(max = 8, message = "Target service must not exceed 8 characters")
    @JsonProperty("targetService")
    private String targetService;

    /**
     * Access level/user type corresponding to CDEMO-MENU-OPT-USRTYPE PIC X(01).
     * Specifies the user type required to access this menu option (e.g., 'U' for User, 'A' for Admin).
     * Optional field as some menu structures (like admin menus) don't include this field.
     * Maximum length: 1 character to match COBOL field definition.
     */
    @Size(max = 1, message = "Access level must not exceed 1 character")
    @JsonProperty("accessLevel")
    private String accessLevel;

    /**
     * Default constructor for JSON deserialization and framework instantiation.
     */
    public MenuOptionDTO() {
        // Default constructor for JSON deserialization
    }

    /**
     * Constructor for creating menu options with all required fields.
     * 
     * @param optionNumber the menu option number (1-99)
     * @param displayText the display text for the menu option (max 35 chars)
     * @param targetService the target service/program name (max 8 chars)
     * @param accessLevel the required user access level (max 1 char, optional)
     */
    public MenuOptionDTO(Integer optionNumber, String displayText, String targetService, String accessLevel) {
        this.optionNumber = optionNumber;
        this.displayText = displayText;
        this.targetService = targetService;
        this.accessLevel = accessLevel;
    }

    /**
     * Constructor for creating menu options without access level (for admin menus).
     * 
     * @param optionNumber the menu option number (1-99)
     * @param displayText the display text for the menu option (max 35 chars)
     * @param targetService the target service/program name (max 8 chars)
     */
    public MenuOptionDTO(Integer optionNumber, String displayText, String targetService) {
        this(optionNumber, displayText, targetService, null);
    }

    /**
     * Gets the menu option number.
     * 
     * @return the option number (1-99) corresponding to CDEMO-MENU-OPT-NUM
     */
    public Integer getOptionNumber() {
        return optionNumber;
    }

    /**
     * Sets the menu option number.
     * 
     * @param optionNumber the option number (1-99) corresponding to CDEMO-MENU-OPT-NUM
     */
    public void setOptionNumber(Integer optionNumber) {
        this.optionNumber = optionNumber;
    }

    /**
     * Gets the menu option display text.
     * 
     * @return the display text (max 35 chars) corresponding to CDEMO-MENU-OPT-NAME
     */
    public String getDisplayText() {
        return displayText;
    }

    /**
     * Sets the menu option display text.
     * 
     * @param displayText the display text (max 35 chars) corresponding to CDEMO-MENU-OPT-NAME
     */
    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    /**
     * Gets the target service/program name.
     * 
     * @return the target service (max 8 chars) corresponding to CDEMO-MENU-OPT-PGMNAME
     */
    public String getTargetService() {
        return targetService;
    }

    /**
     * Sets the target service/program name.
     * 
     * @param targetService the target service (max 8 chars) corresponding to CDEMO-MENU-OPT-PGMNAME
     */
    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    /**
     * Gets the access level/user type.
     * 
     * @return the access level (max 1 char) corresponding to CDEMO-MENU-OPT-USRTYPE, may be null
     */
    public String getAccessLevel() {
        return accessLevel;
    }

    /**
     * Sets the access level/user type.
     * 
     * @param accessLevel the access level (max 1 char) corresponding to CDEMO-MENU-OPT-USRTYPE, may be null
     */
    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    /**
     * Checks if this menu option has an access level restriction.
     * 
     * @return true if access level is specified, false if unrestricted
     */
    public boolean hasAccessLevelRestriction() {
        return accessLevel != null && !accessLevel.trim().isEmpty();
    }

    /**
     * Checks if the specified user type can access this menu option.
     * 
     * @param userType the user type to check (e.g., 'U' for User, 'A' for Admin)
     * @return true if user can access this option, false otherwise
     */
    public boolean isAccessibleByUserType(String userType) {
        // If no access level specified, option is accessible to all users
        if (!hasAccessLevelRestriction()) {
            return true;
        }
        
        // Admin users can access all options
        if ("A".equalsIgnoreCase(userType)) {
            return true;
        }
        
        // Check if user type matches required access level
        return accessLevel.equalsIgnoreCase(userType);
    }

    /**
     * Returns a string representation of the menu option for debugging and logging.
     * 
     * @return formatted string with option details
     */
    @Override
    public String toString() {
        return String.format("MenuOptionDTO{optionNumber=%d, displayText='%s', targetService='%s', accessLevel='%s'}", 
                optionNumber, displayText, targetService, accessLevel);
    }

    /**
     * Compares this menu option with another for equality based on option number and target service.
     * 
     * @param obj the object to compare
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        MenuOptionDTO that = (MenuOptionDTO) obj;
        
        if (optionNumber != null ? !optionNumber.equals(that.optionNumber) : that.optionNumber != null) {
            return false;
        }
        return targetService != null ? targetService.equals(that.targetService) : that.targetService == null;
    }

    /**
     * Generates hash code based on option number and target service.
     * 
     * @return hash code for this menu option
     */
    @Override
    public int hashCode() {
        int result = optionNumber != null ? optionNumber.hashCode() : 0;
        result = 31 * result + (targetService != null ? targetService.hashCode() : 0);
        return result;
    }
}