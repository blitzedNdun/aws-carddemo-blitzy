/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import jakarta.validation.constraints.Max;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object representing a menu option from the main menu screen (COMEN01).
 * 
 * This DTO maps the BMS main menu structure from COMEN01.bms, supporting up to 12 menu options
 * as defined by the OPTN001-OPTN012 fields in the original COBOL screen layout.
 * 
 * Each menu option contains:
 * - Option number (1-12 corresponding to OPTN001-OPTN012)
 * - Description text (40 characters matching BMS OPTN***I field length)
 * - Transaction code for routing (matching OPTION field in BMS)
 * - Enabled status for controlling availability
 * - Access level for security control
 * 
 * This maintains functional parity with the original COBOL/CICS main menu navigation
 * while providing modern REST API data structure for React frontend consumption.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class MenuOption {

    /**
     * Maximum length for menu option description matching BMS OPTN***I field length.
     * Derived from COMEN01.bms where each OPTN*** field has LENGTH=40.
     */
    private static final int MENU_OPTION_DESCRIPTION_LENGTH = 40;
    
    /**
     * Maximum length for transaction code matching COBOL program name length.
     * COBOL program names (transaction codes) are 8 characters (PIC X(08)).
     * Examples: COACTVWC, COACTUPC, COCRDLIC, etc.
     */
    private static final int TRANSACTION_CODE_LENGTH = 8;
    
    /**
     * Maximum menu option number supported by COMEN01 screen layout.
     * Corresponds to the 12 option fields (OPTN001-OPTN012) in the BMS definition.
     */
    private static final int MENU_OPTION_NUMBER_MAX = 12;

    /**
     * Menu option number (1-12).
     * Maps to OPTN001-OPTN012 fields in COMEN01.bms layout.
     * Validated to ensure it doesn't exceed the maximum supported by the screen.
     */
    @Max(value = MENU_OPTION_NUMBER_MAX, message = "Option number cannot exceed " + MENU_OPTION_NUMBER_MAX)
    private Integer optionNumber;

    /**
     * Menu option description text.
     * Maps to the content displayed in OPTN***I fields (PIC X(40)) from COMEN01 copybook.
     * Limited to 40 characters to match original COBOL field definition.
     */
    private String description;

    /**
     * Transaction code for menu option routing.
     * Used to identify which COBOL program/transaction to execute when option is selected.
     * Contains the full COBOL program name (PIC X(08)) such as COACTVWC, COACTUPC, etc.
     */
    private String transactionCode;

    /**
     * Indicates whether this menu option is currently enabled/available.
     * Used to control which options are selectable in the UI.
     * Replaces conditional display logic from original COBOL program.
     */
    private Boolean enabled;

    /**
     * Access level required to use this menu option.
     * Used for role-based access control, replacing RACF checks from original system.
     * Examples: "ADMIN", "USER", "GUEST"
     */
    private String accessLevel;

    /**
     * Default constructor for framework instantiation.
     */
    public MenuOption() {
        this.enabled = true; // Default to enabled
    }

    /**
     * Gets the menu option number.
     * 
     * @return The option number (1-12)
     */
    public Integer getOptionNumber() {
        return optionNumber;
    }

    /**
     * Sets the menu option number.
     * 
     * @param optionNumber The option number (1-12), validated against maximum
     */
    public void setOptionNumber(Integer optionNumber) {
        this.optionNumber = optionNumber;
    }

    /**
     * Gets the menu option description.
     * 
     * @return The description text (max 40 characters)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the menu option description.
     * 
     * @param description The description text, truncated if exceeds 40 characters
     */
    public void setDescription(String description) {
        if (description != null && description.length() > MENU_OPTION_DESCRIPTION_LENGTH) {
            this.description = description.substring(0, MENU_OPTION_DESCRIPTION_LENGTH);
        } else {
            this.description = description;
        }
    }

    /**
     * Gets the transaction code.
     * 
     * @return The transaction code for routing
     */
    public String getTransactionCode() {
        return transactionCode;
    }

    /**
     * Sets the transaction code.
     * 
     * @param transactionCode The transaction code, truncated if exceeds 8 characters
     */
    public void setTransactionCode(String transactionCode) {
        if (transactionCode != null && transactionCode.length() > TRANSACTION_CODE_LENGTH) {
            this.transactionCode = transactionCode.substring(0, TRANSACTION_CODE_LENGTH);
        } else {
            this.transactionCode = transactionCode;
        }
    }

    /**
     * Gets the enabled status.
     * 
     * @return True if option is enabled, false otherwise
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled status.
     * 
     * @param enabled True to enable option, false to disable
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the access level requirement.
     * 
     * @return The access level string
     */
    public String getAccessLevel() {
        return accessLevel;
    }

    /**
     * Sets the access level requirement.
     * 
     * @param accessLevel The access level string
     */
    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    /**
     * Legacy compatibility method for setting option ID.
     * Maps to optionNumber for backward compatibility with existing code.
     * 
     * @param optionId The option identifier
     */
    public void setOptionId(Integer optionId) {
        this.setOptionNumber(optionId);
    }

    /**
     * Legacy compatibility method for setting option text.
     * Maps to description for backward compatibility with existing code.
     * 
     * @param optionText The option text
     */
    public void setOptionText(String optionText) {
        this.setDescription(optionText);
    }

    /**
     * Legacy compatibility method for setting target transaction.
     * Maps to transactionCode for backward compatibility with existing code.
     * 
     * @param target The target transaction code
     */
    public void setTarget(String target) {
        this.setTransactionCode(target);
    }

    /**
     * Checks equality based on option number.
     * Two MenuOption objects are equal if they have the same option number.
     * 
     * @param obj The object to compare with
     * @return True if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MenuOption that = (MenuOption) obj;
        return optionNumber != null ? optionNumber.equals(that.optionNumber) : that.optionNumber == null;
    }

    /**
     * Generates hash code based on option number.
     * 
     * @return Hash code value
     */
    @Override
    public int hashCode() {
        return optionNumber != null ? optionNumber.hashCode() : 0;
    }

    /**
     * Provides string representation of the menu option.
     * Includes option number, description, transaction code, and enabled status.
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        return "MenuOption{" +
                "optionNumber=" + optionNumber +
                ", description='" + description + '\'' +
                ", transactionCode='" + transactionCode + '\'' +
                ", enabled=" + enabled +
                ", accessLevel='" + accessLevel + '\'' +
                '}';
    }
}