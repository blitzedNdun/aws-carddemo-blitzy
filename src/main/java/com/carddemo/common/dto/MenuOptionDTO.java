package com.carddemo.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data Transfer Object representing a single menu option for the CardDemo application.
 * Maps to COBOL CDEMO-MENU-OPT structure from COMEN02Y and COADM02Y copybooks.
 * 
 * This DTO preserves the exact field mapping from the original COBOL menu option structures:
 * - CDEMO-MENU-OPT-NUM (PIC 9(02)) → optionNumber
 * - CDEMO-MENU-OPT-NAME (PIC X(35)) → displayText  
 * - CDEMO-MENU-OPT-PGMNAME (PIC X(08)) → targetService
 * - CDEMO-MENU-OPT-USRTYPE (PIC X(01)) → accessLevel
 * 
 * Used for JSON serialization in REST API communication between React frontend
 * and Spring Boot microservices while maintaining COBOL data structure integrity.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuOptionDTO {

    /**
     * Menu option number corresponding to CDEMO-MENU-OPT-NUM (PIC 9(02)).
     * Represents the numeric identifier for the menu option (01-99).
     */
    @NotNull(message = "Menu option number is required")
    @Min(value = 1, message = "Menu option number must be between 1 and 99")
    @Max(value = 99, message = "Menu option number must be between 1 and 99")
    @JsonProperty("optionNumber")
    private Integer optionNumber;

    /**
     * Display text for the menu option corresponding to CDEMO-MENU-OPT-NAME (PIC X(35)).
     * Contains the descriptive text shown to users in the menu interface.
     */
    @NotNull(message = "Display text is required")
    @Size(min = 1, max = 35, message = "Display text must be between 1 and 35 characters")
    @JsonProperty("displayText")
    private String displayText;

    /**
     * Target service/program name corresponding to CDEMO-MENU-OPT-PGMNAME (PIC X(08)).
     * Identifies the Spring Boot microservice or program to invoke when option is selected.
     * Maps from original COBOL program names to modern service identifiers.
     */
    @NotNull(message = "Target service is required")
    @Size(min = 1, max = 8, message = "Target service must be between 1 and 8 characters")
    @JsonProperty("targetService")
    private String targetService;

    /**
     * Access level/user type corresponding to CDEMO-MENU-OPT-USRTYPE (PIC X(01)).
     * Defines the minimum user access level required to display this menu option.
     * Common values: 'U' (User), 'A' (Admin), 'S' (Super Admin).
     * May be null for admin menu options that don't specify user type restrictions.
     */
    @Size(max = 1, message = "Access level must be exactly 1 character")
    @JsonProperty("accessLevel")
    private String accessLevel;

    /**
     * Default constructor for JSON deserialization and framework use.
     */
    public MenuOptionDTO() {
    }

    /**
     * Constructor for main menu options with all fields.
     * 
     * @param optionNumber The numeric identifier for the menu option (1-99)
     * @param displayText The descriptive text to display (max 35 characters)
     * @param targetService The target service/program identifier (max 8 characters)
     * @param accessLevel The required user access level (1 character, can be null)
     */
    public MenuOptionDTO(Integer optionNumber, String displayText, String targetService, String accessLevel) {
        this.optionNumber = optionNumber;
        this.displayText = displayText;
        this.targetService = targetService;
        this.accessLevel = accessLevel;
    }

    /**
     * Constructor for admin menu options without access level specification.
     * 
     * @param optionNumber The numeric identifier for the menu option (1-99)
     * @param displayText The descriptive text to display (max 35 characters)
     * @param targetService The target service/program identifier (max 8 characters)
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
     * @return The numeric identifier for this menu option (1-99)
     */
    public Integer getOptionNumber() {
        return optionNumber;
    }

    /**
     * Sets the menu option number.
     * 
     * @param optionNumber The numeric identifier for this menu option (1-99)
     */
    public void setOptionNumber(Integer optionNumber) {
        this.optionNumber = optionNumber;
    }

    /**
     * Gets the display text for the menu option.
     * 
     * @return The descriptive text shown to users (max 35 characters)
     */
    public String getDisplayText() {
        return displayText;
    }

    /**
     * Sets the display text for the menu option.
     * 
     * @param displayText The descriptive text to display (max 35 characters)
     */
    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    /**
     * Gets the target service/program identifier.
     * 
     * @return The service identifier to invoke when option is selected (max 8 characters)
     */
    public String getTargetService() {
        return targetService;
    }

    /**
     * Sets the target service/program identifier.
     * 
     * @param targetService The service identifier (max 8 characters)
     */
    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    /**
     * Gets the access level required for this menu option.
     * 
     * @return The minimum user access level (1 character), or null if no restriction
     */
    public String getAccessLevel() {
        return accessLevel;
    }

    /**
     * Sets the access level required for this menu option.
     * 
     * @param accessLevel The minimum user access level (1 character), or null for no restriction
     */
    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    /**
     * Determines if this menu option has access level restrictions.
     * 
     * @return true if an access level is specified, false otherwise
     */
    public boolean hasAccessLevelRestriction() {
        return accessLevel != null && !accessLevel.trim().isEmpty();
    }

    /**
     * Checks if the menu option is accessible to the specified user access level.
     * 
     * @param userAccessLevel The user's access level to check
     * @return true if the user can access this menu option, false otherwise
     */
    public boolean isAccessibleTo(String userAccessLevel) {
        if (!hasAccessLevelRestriction()) {
            return true; // No restriction means accessible to all
        }
        
        if (userAccessLevel == null || userAccessLevel.trim().isEmpty()) {
            return false; // No user level provided
        }
        
        // Basic access level comparison - can be enhanced based on business rules
        return accessLevel.equals(userAccessLevel.trim().toUpperCase());
    }

    /**
     * Creates a string representation of the menu option for debugging purposes.
     * 
     * @return A formatted string containing all field values
     */
    @Override
    public String toString() {
        return String.format(
            "MenuOptionDTO{optionNumber=%d, displayText='%s', targetService='%s', accessLevel='%s'}",
            optionNumber, 
            displayText != null ? displayText.trim() : null,
            targetService != null ? targetService.trim() : null,
            accessLevel != null ? accessLevel.trim() : null
        );
    }

    /**
     * Compares this menu option with another for equality based on option number.
     * 
     * @param obj The object to compare with
     * @return true if both objects have the same option number, false otherwise
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
        return optionNumber != null && optionNumber.equals(that.optionNumber);
    }

    /**
     * Generates hash code based on the option number for consistent hashing.
     * 
     * @return The hash code of the option number
     */
    @Override
    public int hashCode() {
        return optionNumber != null ? optionNumber.hashCode() : 0;
    }
}