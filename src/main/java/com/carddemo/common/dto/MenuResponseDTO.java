package com.carddemo.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.ArrayList;

/**
 * Data Transfer Object for menu API responses containing a list of available menu options,
 * user context information, and response status. Encapsulates complete menu response data
 * for REST API communication between React frontend and menu microservice.
 * 
 * Maps to COBOL COMEN01C program response structure preserving menu option availability,
 * user role filtering, and status message handling patterns while enabling modern JSON
 * serialization for cloud-native API communication.
 * 
 * This DTO corresponds to the menu response processing in COMEN01C.cbl:
 * - CDEMO-MENU-OPT-COUNT → size of menuOptions list
 * - CDEMO-USRTYP-USER → userRole for role-based filtering
 * - WS-ERR-FLG → status indicator for response handling
 * - WS-MESSAGE → message field for user feedback
 * 
 * Used for JSON serialization in REST API communication between React components
 * and Spring Boot microservices while maintaining COBOL menu structure integrity.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuResponseDTO {

    /**
     * List of available menu options based on user role and permissions.
     * Corresponds to CDEMO-MENU-OPT array from COMEN02Y/COADM02Y copybooks.
     * Each MenuOptionDTO preserves exact field mapping from COBOL menu structures.
     */
    @Valid
    @JsonProperty("menuOptions")
    private List<MenuOptionDTO> menuOptions;

    /**
     * User role/type for role-based menu filtering.
     * Corresponds to CDEMO-USRTYP-USER from COBOL common area.
     * Used to determine which menu options are accessible to the current user.
     * Common values: 'U' (User), 'A' (Admin), 'S' (Super Admin).
     */
    @Size(max = 1, message = "User role must be exactly 1 character")
    @JsonProperty("userRole")
    private String userRole;

    /**
     * Response status indicator for API response handling.
     * Corresponds to WS-ERR-FLG (88-level ERR-FLG-ON/ERR-FLG-OFF) from COMEN01C.
     * Indicates success/failure status of menu retrieval operation.
     * Values: "SUCCESS", "ERROR", "WARNING", "INFO"
     */
    @Size(max = 10, message = "Status must not exceed 10 characters")
    @JsonProperty("status")
    private String status;

    /**
     * Response message providing user feedback or error information.
     * Corresponds to WS-MESSAGE (PIC X(80)) from COMEN01C WORKING-STORAGE.
     * Contains user-friendly messages for display in React components.
     */
    @Size(max = 80, message = "Message must not exceed 80 characters")
    @JsonProperty("message")
    private String message;

    /**
     * Default constructor for JSON deserialization and framework use.
     * Initializes menuOptions as empty ArrayList to prevent null pointer exceptions.
     */
    public MenuResponseDTO() {
        this.menuOptions = new ArrayList<>();
    }

    /**
     * Constructor for successful menu response with all menu options.
     * 
     * @param menuOptions List of available menu options for the user
     * @param userRole User's role/type for role-based filtering (max 1 character)
     * @param status Response status indicator
     * @param message User-friendly response message (max 80 characters)
     */
    public MenuResponseDTO(List<MenuOptionDTO> menuOptions, String userRole, String status, String message) {
        this.menuOptions = menuOptions != null ? new ArrayList<>(menuOptions) : new ArrayList<>();
        this.userRole = userRole;
        this.status = status;
        this.message = message;
    }

    /**
     * Constructor for error response with status and message only.
     * Used when menu retrieval fails or user lacks permissions.
     * 
     * @param status Error status indicator
     * @param message Error message for user display
     */
    public MenuResponseDTO(String status, String message) {
        this.menuOptions = new ArrayList<>();
        this.userRole = null;
        this.status = status;
        this.message = message;
    }

    /**
     * Constructor for successful response with menu options and user role.
     * Uses default "SUCCESS" status with empty message.
     * 
     * @param menuOptions List of available menu options for the user
     * @param userRole User's role/type for role-based filtering
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
     * @return List of MenuOptionDTO objects representing available menu choices
     */
    public List<MenuOptionDTO> getMenuOptions() {
        return menuOptions;
    }

    /**
     * Sets the list of available menu options.
     * 
     * @param menuOptions List of MenuOptionDTO objects (will be copied to prevent external modification)
     */
    public void setMenuOptions(List<MenuOptionDTO> menuOptions) {
        this.menuOptions = menuOptions != null ? new ArrayList<>(menuOptions) : new ArrayList<>();
    }

    /**
     * Gets the user role for role-based menu filtering.
     * 
     * @return User role/type (1 character), or null if not specified
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Sets the user role for role-based menu filtering.
     * 
     * @param userRole User role/type (max 1 character), or null for no role specification
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * Gets the response status indicator.
     * 
     * @return Status string indicating response outcome (max 10 characters)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the response status indicator.
     * 
     * @param status Status string (max 10 characters) - "SUCCESS", "ERROR", "WARNING", "INFO"
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the response message.
     * 
     * @return User-friendly message string (max 80 characters)
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the response message.
     * 
     * @param message User-friendly message (max 80 characters) for display to user
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Adds a menu option to the response.
     * Convenience method for building menu responses programmatically.
     * 
     * @param menuOption MenuOptionDTO to add to the response
     */
    public void addMenuOption(MenuOptionDTO menuOption) {
        if (menuOption != null) {
            if (this.menuOptions == null) {
                this.menuOptions = new ArrayList<>();
            }
            this.menuOptions.add(menuOption);
        }
    }

    /**
     * Gets the count of menu options in the response.
     * Equivalent to CDEMO-MENU-OPT-COUNT from COBOL menu structures.
     * 
     * @return Number of menu options available to the user
     */
    public int getMenuOptionCount() {
        return menuOptions != null ? menuOptions.size() : 0;
    }

    /**
     * Checks if the response contains menu options.
     * 
     * @return true if menu options are available, false otherwise
     */
    public boolean hasMenuOptions() {
        return menuOptions != null && !menuOptions.isEmpty();
    }

    /**
     * Checks if the response indicates a successful operation.
     * 
     * @return true if status is "SUCCESS", false otherwise
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    /**
     * Checks if the response indicates an error condition.
     * 
     * @return true if status is "ERROR", false otherwise
     */
    public boolean isError() {
        return "ERROR".equals(status);
    }

    /**
     * Checks if the response has a message for user display.
     * 
     * @return true if message is not null and not empty, false otherwise
     */
    public boolean hasMessage() {
        return message != null && !message.trim().isEmpty();
    }

    /**
     * Filters menu options accessible to the specified user role.
     * Removes menu options that have access level restrictions the user cannot meet.
     * Replicates COBOL logic from lines 136-143 in COMEN01C.cbl.
     * 
     * @param userAccessLevel The user's access level to check against menu options
     */
    public void filterMenuOptionsByRole(String userAccessLevel) {
        if (menuOptions == null || userAccessLevel == null) {
            return;
        }
        
        menuOptions.removeIf(option -> !option.isAccessibleTo(userAccessLevel));
    }

    /**
     * Creates a string representation of the menu response for debugging purposes.
     * 
     * @return A formatted string containing response status, user role, option count, and message
     */
    @Override
    public String toString() {
        return String.format(
            "MenuResponseDTO{status='%s', userRole='%s', menuOptionCount=%d, message='%s'}",
            status != null ? status.trim() : null,
            userRole != null ? userRole.trim() : null,
            getMenuOptionCount(),
            message != null ? message.trim() : null
        );
    }

    /**
     * Compares this menu response with another for equality based on status and menu options.
     * 
     * @param obj The object to compare with
     * @return true if both objects have the same status and menu options, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MenuResponseDTO that = (MenuResponseDTO) obj;
        
        // Compare status
        if (status != null ? !status.equals(that.status) : that.status != null) {
            return false;
        }
        
        // Compare user role
        if (userRole != null ? !userRole.equals(that.userRole) : that.userRole != null) {
            return false;
        }
        
        // Compare menu options list
        if (menuOptions != null ? !menuOptions.equals(that.menuOptions) : that.menuOptions != null) {
            return false;
        }
        
        // Compare message
        return message != null ? message.equals(that.message) : that.message == null;
    }

    /**
     * Generates hash code based on status, user role, menu options, and message for consistent hashing.
     * 
     * @return The hash code combining all response fields
     */
    @Override
    public int hashCode() {
        int result = status != null ? status.hashCode() : 0;
        result = 31 * result + (userRole != null ? userRole.hashCode() : 0);
        result = 31 * result + (menuOptions != null ? menuOptions.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    /**
     * Creates a new MenuResponseDTO for successful menu retrieval.
     * Factory method providing consistent response structure for success cases.
     * 
     * @param menuOptions List of available menu options
     * @param userRole User's role for role-based filtering
     * @param message Success message (optional)
     * @return New MenuResponseDTO with success status
     */
    public static MenuResponseDTO success(List<MenuOptionDTO> menuOptions, String userRole, String message) {
        return new MenuResponseDTO(menuOptions, userRole, "SUCCESS", message);
    }

    /**
     * Creates a new MenuResponseDTO for successful menu retrieval without message.
     * Factory method providing consistent response structure for success cases.
     * 
     * @param menuOptions List of available menu options
     * @param userRole User's role for role-based filtering
     * @return New MenuResponseDTO with success status and empty message
     */
    public static MenuResponseDTO success(List<MenuOptionDTO> menuOptions, String userRole) {
        return new MenuResponseDTO(menuOptions, userRole, "SUCCESS", "");
    }

    /**
     * Creates a new MenuResponseDTO for error conditions.
     * Factory method providing consistent response structure for error cases.
     * 
     * @param message Error message for user display
     * @return New MenuResponseDTO with error status and no menu options
     */
    public static MenuResponseDTO error(String message) {
        return new MenuResponseDTO("ERROR", message);
    }

    /**
     * Creates a new MenuResponseDTO for warning conditions.
     * Factory method providing consistent response structure for warning cases.
     * 
     * @param message Warning message for user display
     * @return New MenuResponseDTO with warning status and no menu options
     */
    public static MenuResponseDTO warning(String message) {
        return new MenuResponseDTO("WARNING", message);
    }

    /**
     * Creates a new MenuResponseDTO for informational responses.
     * Factory method providing consistent response structure for info cases.
     * 
     * @param message Informational message for user display
     * @return New MenuResponseDTO with info status and no menu options
     */
    public static MenuResponseDTO info(String message) {
        return new MenuResponseDTO("INFO", message);
    }
}