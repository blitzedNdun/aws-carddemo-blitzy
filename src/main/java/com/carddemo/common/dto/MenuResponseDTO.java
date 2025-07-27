package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Data Transfer Object for menu API responses containing a list of available menu options,
 * user context information, and response status. Encapsulates complete menu response data
 * for REST API communication between React frontend and menu microservice.
 * 
 * This DTO preserves the exact response structure of the original COBOL menu program
 * COMEN01C.cbl, transforming mainframe pseudo-conversational menu processing into
 * modern REST API response format while maintaining identical business logic flow:
 * 
 * - Menu options list corresponds to CDEMO-MENU-OPT-* array from COMEN02Y copybook
 * - User role information enables role-based menu filtering equivalent to CDEMO-USRTYP-USER logic
 * - Status and message fields replicate CICS SEND MAP error handling and user feedback
 * - JSON serialization supports React frontend consumption replacing BMS map data transfer
 * 
 * Used by MenuNavigationService.java to build complete menu responses that preserve
 * the original terminal-based menu experience through modern web interfaces while
 * supporting the service-per-transaction microservices architecture transformation.
 * 
 * @author CardDemo Application - Blitzy agent
 * @version 1.0
 * @since Java 21
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuResponseDTO {

    /**
     * Collection of available menu options for the user's role and context.
     * Corresponds to the CDEMO-MENU-OPT-* array from COMEN02Y copybook, containing
     * dynamically filtered menu options based on user permissions and access levels.
     * 
     * Each MenuOptionDTO preserves the original COBOL menu option structure:
     * - Option number (CDEMO-MENU-OPT-NUM)
     * - Display text (CDEMO-MENU-OPT-NAME) 
     * - Target service (CDEMO-MENU-OPT-PGMNAME)
     * - Access level (CDEMO-MENU-OPT-USRTYPE)
     */
    @Valid
    @NotNull(message = "Menu options list is required")
    @JsonProperty("menuOptions")
    private List<MenuOptionDTO> menuOptions;

    /**
     * Current user's role for role-based menu filtering.
     * Corresponds to CDEMO-USER-TYPE from the COBOL commarea structure, used to
     * determine which menu options are accessible to the current user session.
     * 
     * Valid values:
     * - "U" for regular users (CDEMO-USRTYP-USER)
     * - "A" for administrators (admin access)
     * 
     * This field enables the menu service to replicate the original COBOL logic
     * that filters menu options based on user type access restrictions.
     */
    @Size(max = 1, message = "User role must not exceed 1 character")
    @JsonProperty("userRole")
    private String userRole;

    /**
     * Response status indicator for API client handling.
     * Indicates the success or failure status of the menu retrieval operation,
     * enabling React frontend to handle response appropriately and display
     * proper user feedback messages.
     * 
     * Common values:
     * - "SUCCESS" for successful menu retrieval
     * - "ERROR" for processing errors
     * - "WARNING" for partial success with warnings
     */
    @Size(max = 20, message = "Status must not exceed 20 characters")
    @JsonProperty("status")
    private String status;

    /**
     * Response message for user display and error communication.
     * Corresponds to WS-MESSAGE field from COMEN01C.cbl, providing user-friendly
     * messages for various menu processing scenarios including errors, warnings,
     * and informational feedback.
     * 
     * This field preserves the original COBOL error messaging capability, enabling
     * the React frontend to display identical user feedback messages that were
     * previously shown through BMS map ERRMSGO field processing.
     * 
     * Maximum length of 80 characters matches the original COBOL WS-MESSAGE field
     * definition from the legacy implementation.
     */
    @Size(max = 80, message = "Message must not exceed 80 characters")
    @JsonProperty("message")
    private String message;

    /**
     * Default constructor for JSON deserialization and framework instantiation.
     * Initializes menu options as empty list to prevent null pointer exceptions
     * during response processing.
     */
    public MenuResponseDTO() {
        this.menuOptions = new ArrayList<>();
    }

    /**
     * Constructor for creating complete menu responses with all fields.
     * 
     * @param menuOptions the list of available menu options for the user
     * @param userRole the current user's role for access control
     * @param status the response status indicator
     * @param message the response message for user display
     */
    public MenuResponseDTO(List<MenuOptionDTO> menuOptions, String userRole, String status, String message) {
        this.menuOptions = menuOptions != null ? new ArrayList<>(menuOptions) : new ArrayList<>();
        this.userRole = userRole;
        this.status = status;
        this.message = message;
    }

    /**
     * Constructor for successful menu responses with options and user context.
     * 
     * @param menuOptions the list of available menu options for the user
     * @param userRole the current user's role for access control
     */
    public MenuResponseDTO(List<MenuOptionDTO> menuOptions, String userRole) {
        this(menuOptions, userRole, "SUCCESS", null);
    }

    /**
     * Gets the list of available menu options.
     * 
     * @return the list of MenuOptionDTO objects representing available menu choices
     */
    public List<MenuOptionDTO> getMenuOptions() {
        return menuOptions;
    }

    /**
     * Sets the list of available menu options.
     * Creates a defensive copy to prevent external modification of the internal list.
     * 
     * @param menuOptions the list of MenuOptionDTO objects to set
     */
    public void setMenuOptions(List<MenuOptionDTO> menuOptions) {
        this.menuOptions = menuOptions != null ? new ArrayList<>(menuOptions) : new ArrayList<>();
    }

    /**
     * Gets the current user's role for menu filtering.
     * 
     * @return the user role string (e.g., "U" for user, "A" for admin)
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Sets the current user's role for menu filtering.
     * 
     * @param userRole the user role string (e.g., "U" for user, "A" for admin)
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * Gets the response status indicator.
     * 
     * @return the status string indicating success, error, or warning
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the response status indicator.
     * 
     * @param status the status string indicating success, error, or warning
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the response message for user display.
     * 
     * @return the message string for user feedback and error communication
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the response message for user display.
     * 
     * @param message the message string for user feedback and error communication
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Adds a single menu option to the response.
     * Provides convenient method for building menu responses incrementally.
     * 
     * @param menuOption the MenuOptionDTO to add to the list
     */
    public void addMenuOption(MenuOptionDTO menuOption) {
        if (menuOption != null) {
            this.menuOptions.add(menuOption);
        }
    }

    /**
     * Checks if the response contains any menu options.
     * 
     * @return true if menu options list is not empty, false otherwise
     */
    public boolean hasMenuOptions() {
        return menuOptions != null && !menuOptions.isEmpty();
    }

    /**
     * Gets the count of available menu options.
     * 
     * @return the number of menu options in the response
     */
    public int getMenuOptionCount() {
        return menuOptions != null ? menuOptions.size() : 0;
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
     * Creates a successful menu response with the provided options and user role.
     * Factory method for creating standard successful responses.
     * 
     * @param menuOptions the list of menu options to include
     * @param userRole the current user's role
     * @return a new MenuResponseDTO with SUCCESS status
     */
    public static MenuResponseDTO success(List<MenuOptionDTO> menuOptions, String userRole) {
        return new MenuResponseDTO(menuOptions, userRole, "SUCCESS", null);
    }

    /**
     * Creates a successful menu response with options, user role, and message.
     * Factory method for creating successful responses with informational messages.
     * 
     * @param menuOptions the list of menu options to include
     * @param userRole the current user's role
     * @param message the informational message to display
     * @return a new MenuResponseDTO with SUCCESS status and message
     */
    public static MenuResponseDTO success(List<MenuOptionDTO> menuOptions, String userRole, String message) {
        return new MenuResponseDTO(menuOptions, userRole, "SUCCESS", message);
    }

    /**
     * Creates an error menu response with the specified error message.
     * Factory method for creating error responses when menu retrieval fails.
     * 
     * @param errorMessage the error message to display to the user
     * @return a new MenuResponseDTO with ERROR status and empty menu options
     */
    public static MenuResponseDTO error(String errorMessage) {
        return new MenuResponseDTO(new ArrayList<>(), null, "ERROR", errorMessage);
    }

    /**
     * Creates an error menu response with user role and error message.
     * Factory method for creating error responses when menu processing fails
     * but user context is available.
     * 
     * @param userRole the current user's role
     * @param errorMessage the error message to display to the user
     * @return a new MenuResponseDTO with ERROR status and empty menu options
     */
    public static MenuResponseDTO error(String userRole, String errorMessage) {
        return new MenuResponseDTO(new ArrayList<>(), userRole, "ERROR", errorMessage);
    }

    /**
     * Returns a string representation of the menu response for debugging and logging.
     * 
     * @return formatted string with response details
     */
    @Override
    public String toString() {
        return String.format("MenuResponseDTO{menuOptions=%d items, userRole='%s', status='%s', message='%s'}", 
                getMenuOptionCount(), userRole, status, message);
    }

    /**
     * Compares this menu response with another for equality based on all fields.
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
        
        MenuResponseDTO that = (MenuResponseDTO) obj;
        
        return Objects.equals(menuOptions, that.menuOptions) &&
               Objects.equals(userRole, that.userRole) &&
               Objects.equals(status, that.status) &&
               Objects.equals(message, that.message);
    }

    /**
     * Generates hash code based on all fields.
     * 
     * @return hash code for this menu response
     */
    @Override
    public int hashCode() {
        return Objects.hash(menuOptions, userRole, status, message);
    }
}