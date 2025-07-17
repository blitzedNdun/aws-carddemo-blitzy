package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for menu API responses containing a list of available menu options,
 * user context information, and response status.
 * 
 * Encapsulates complete menu response data for REST API communication between React frontend
 * and menu microservice, implementing role-based menu filtering and status handling.
 * 
 * Maps to COBOL menu programs:
 * - COMEN01C.cbl - Main menu for regular users
 * - COADM01C.cbl - Administrative menu for admin users
 * 
 * COBOL Field Mappings:
 * - menuOptions       -> Collection of CDEMO-MENU-OPT entries
 * - userRole          -> CDEMO-USER-TYPE (Admin/User role context)
 * - status            -> WS-ERR-FLG processing status
 * - message           -> WS-MESSAGE error/info messages
 * 
 * REST API Usage:
 * - GET /api/menu/main - Returns main menu options for authenticated user
 * - GET /api/menu/admin - Returns administrative menu options for admin users
 * - POST /api/menu/process - Processes menu option selection
 * 
 * Integration Points:
 * - React MainMenuComponent.jsx - Consumes this DTO for menu rendering
 * - Spring Security - Role-based filtering of menu options
 * - Redis Session - User context preservation across requests
 */
public class MenuResponseDTO {

    /**
     * List of available menu options filtered by user role and access level.
     * Maps to COBOL CDEMO-MENU-OPT array with role-based filtering.
     * 
     * COBOL equivalent: CDEMO-MENU-OPT-COUNT occurrences of menu option structures
     */
    @JsonProperty("menuOptions")
    @NotNull(message = "Menu options list is required")
    @Valid
    private List<MenuOptionDTO> menuOptions;

    /**
     * User role context for menu filtering and access control.
     * Maps to COBOL CDEMO-USER-TYPE field determining menu access.
     * 
     * Valid values:
     * - "USER" = Regular user access (CDEMO-USRTYP-USER)
     * - "ADMIN" = Administrative access (CDEMO-USRTYP-ADMIN)
     * 
     * COBOL equivalent: CDEMO-USER-TYPE PIC X(01) with 88-level conditions
     */
    @JsonProperty("userRole")
    @NotBlank(message = "User role is required")
    @Pattern(regexp = "^(USER|ADMIN)$", message = "User role must be 'USER' or 'ADMIN'")
    private String userRole;

    /**
     * Response status indicating processing result.
     * Maps to COBOL WS-ERR-FLG processing status.
     * 
     * Valid values:
     * - "SUCCESS" = Normal processing completed
     * - "ERROR" = Processing error occurred
     * - "WARNING" = Processing completed with warnings
     * 
     * COBOL equivalent: WS-ERR-FLG with 88-level conditions ERR-FLG-ON/ERR-FLG-OFF
     */
    @JsonProperty("status")
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(SUCCESS|ERROR|WARNING)$", message = "Status must be 'SUCCESS', 'ERROR', or 'WARNING'")
    private String status;

    /**
     * Response message providing additional context or error information.
     * Maps to COBOL WS-MESSAGE field for user feedback.
     * 
     * COBOL equivalent: WS-MESSAGE PIC X(80) containing error/info messages
     */
    @JsonProperty("message")
    @Size(max = 80, message = "Message must not exceed 80 characters")
    private String message;

    /**
     * Default constructor for JSON deserialization.
     */
    public MenuResponseDTO() {
        this.menuOptions = new ArrayList<>();
        this.status = "SUCCESS";
        this.message = "";
    }

    /**
     * Constructor with all required fields for complete menu response creation.
     *
     * @param menuOptions the list of available menu options
     * @param userRole    the user's role context ('USER' or 'ADMIN')
     * @param status      the response status ('SUCCESS', 'ERROR', or 'WARNING')
     * @param message     additional response message or error information
     */
    public MenuResponseDTO(List<MenuOptionDTO> menuOptions, String userRole, String status, String message) {
        this.menuOptions = menuOptions != null ? new ArrayList<>(menuOptions) : new ArrayList<>();
        this.userRole = userRole;
        this.status = status;
        this.message = message;
    }

    /**
     * Constructor for successful menu response with default success status.
     *
     * @param menuOptions the list of available menu options
     * @param userRole    the user's role context ('USER' or 'ADMIN')
     */
    public MenuResponseDTO(List<MenuOptionDTO> menuOptions, String userRole) {
        this.menuOptions = menuOptions != null ? new ArrayList<>(menuOptions) : new ArrayList<>();
        this.userRole = userRole;
        this.status = "SUCCESS";
        this.message = "";
    }

    /**
     * Gets the list of available menu options.
     *
     * @return the list of menu options filtered by user role
     */
    public List<MenuOptionDTO> getMenuOptions() {
        return menuOptions;
    }

    /**
     * Sets the list of available menu options.
     *
     * @param menuOptions the list of menu options to set
     */
    public void setMenuOptions(List<MenuOptionDTO> menuOptions) {
        this.menuOptions = menuOptions != null ? new ArrayList<>(menuOptions) : new ArrayList<>();
    }

    /**
     * Gets the user's role context.
     *
     * @return the user role ('USER' or 'ADMIN')
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Sets the user's role context.
     *
     * @param userRole the user role ('USER' or 'ADMIN')
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * Gets the response status.
     *
     * @return the response status ('SUCCESS', 'ERROR', or 'WARNING')
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the response status.
     *
     * @param status the response status ('SUCCESS', 'ERROR', or 'WARNING')
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the response message.
     *
     * @return the response message or error information
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the response message.
     *
     * @param message the response message or error information
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Adds a menu option to the response.
     *
     * @param menuOption the menu option to add
     */
    public void addMenuOption(MenuOptionDTO menuOption) {
        if (menuOption != null) {
            this.menuOptions.add(menuOption);
        }
    }

    /**
     * Removes a menu option from the response.
     *
     * @param menuOption the menu option to remove
     * @return true if the option was removed, false otherwise
     */
    public boolean removeMenuOption(MenuOptionDTO menuOption) {
        return this.menuOptions.remove(menuOption);
    }

    /**
     * Gets the count of available menu options.
     *
     * @return the number of menu options in the response
     */
    public int getMenuOptionCount() {
        return this.menuOptions.size();
    }

    /**
     * Checks if the response represents a successful operation.
     *
     * @return true if status is "SUCCESS", false otherwise
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(this.status);
    }

    /**
     * Checks if the response represents an error condition.
     *
     * @return true if status is "ERROR", false otherwise
     */
    public boolean isError() {
        return "ERROR".equals(this.status);
    }

    /**
     * Checks if the response represents a warning condition.
     *
     * @return true if status is "WARNING", false otherwise
     */
    public boolean isWarning() {
        return "WARNING".equals(this.status);
    }

    /**
     * Checks if the user has administrative privileges.
     *
     * @return true if user role is "ADMIN", false otherwise
     */
    public boolean isAdminUser() {
        return "ADMIN".equals(this.userRole);
    }

    /**
     * Checks if the user has regular user privileges.
     *
     * @return true if user role is "USER", false otherwise
     */
    public boolean isRegularUser() {
        return "USER".equals(this.userRole);
    }

    /**
     * Filters menu options based on user role and access level.
     * Removes admin-only options for regular users.
     *
     * @return this MenuResponseDTO for method chaining
     */
    public MenuResponseDTO filterByUserRole() {
        if (isRegularUser()) {
            // Remove admin-only options for regular users
            this.menuOptions.removeIf(option -> option.isAdminOnly());
        }
        return this;
    }

    /**
     * Creates an error response with the specified message.
     *
     * @param userRole the user's role context
     * @param message  the error message
     * @return a new MenuResponseDTO with error status
     */
    public static MenuResponseDTO createErrorResponse(String userRole, String message) {
        return new MenuResponseDTO(new ArrayList<>(), userRole, "ERROR", message);
    }

    /**
     * Creates a warning response with the specified message.
     *
     * @param menuOptions the list of menu options
     * @param userRole    the user's role context
     * @param message     the warning message
     * @return a new MenuResponseDTO with warning status
     */
    public static MenuResponseDTO createWarningResponse(List<MenuOptionDTO> menuOptions, String userRole, String message) {
        return new MenuResponseDTO(menuOptions, userRole, "WARNING", message);
    }

    /**
     * Creates a successful response with the specified menu options.
     *
     * @param menuOptions the list of menu options
     * @param userRole    the user's role context
     * @return a new MenuResponseDTO with success status
     */
    public static MenuResponseDTO createSuccessResponse(List<MenuOptionDTO> menuOptions, String userRole) {
        return new MenuResponseDTO(menuOptions, userRole, "SUCCESS", "");
    }

    /**
     * Returns a string representation of the menu response.
     *
     * @return formatted string with response details
     */
    @Override
    public String toString() {
        return String.format("MenuResponseDTO{menuOptions=%d items, userRole='%s', status='%s', message='%s'}", 
                           menuOptions.size(), userRole, status, message);
    }

    /**
     * Checks equality based on all fields including menu options list.
     *
     * @param obj the object to compare
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        MenuResponseDTO that = (MenuResponseDTO) obj;
        
        if (menuOptions != null ? !menuOptions.equals(that.menuOptions) : that.menuOptions != null) return false;
        if (userRole != null ? !userRole.equals(that.userRole) : that.userRole != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        return message != null ? message.equals(that.message) : that.message == null;
    }

    /**
     * Generates hash code based on all fields.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        int result = menuOptions != null ? menuOptions.hashCode() : 0;
        result = 31 * result + (userRole != null ? userRole.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}