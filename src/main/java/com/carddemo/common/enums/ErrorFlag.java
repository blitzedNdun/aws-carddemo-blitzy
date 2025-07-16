/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.common.enums;

/**
 * ErrorFlag enum that converts COBOL Y/N error flag patterns to Java enum
 * for consistent error handling across all microservices.
 * 
 * This enum replaces the traditional COBOL pattern:
 * <pre>
 *   05 WS-ERR-FLG                 PIC X(01) VALUE 'N'.
 *     88 ERR-FLG-ON                         VALUE 'Y'.
 *     88 ERR-FLG-OFF                        VALUE 'N'.
 * </pre>
 * 
 * Provides Spring Boot error handling integration with standardized
 * error response patterns and REST API error response formatting.
 * 
 * @author Blitzy agent
 * @version 1.0
 */
public enum ErrorFlag {
    
    /**
     * Error flag ON state - equivalent to COBOL 'Y' value
     * Indicates that an error condition has been detected
     */
    ON('Y'),
    
    /**
     * Error flag OFF state - equivalent to COBOL 'N' value  
     * Indicates no error condition (normal processing state)
     */
    OFF('N');
    
    /** The COBOL character value associated with this error flag state */
    private final char cobolValue;
    
    /**
     * Constructor for ErrorFlag enum values
     * 
     * @param cobolValue the COBOL character value ('Y' or 'N')
     */
    ErrorFlag(char cobolValue) {
        this.cobolValue = cobolValue;
    }
    
    /**
     * Gets the COBOL character value for this error flag state
     * 
     * @return the COBOL character value ('Y' for ON, 'N' for OFF)
     */
    public char getCobolValue() {
        return cobolValue;
    }
    
    /**
     * Checks if this error flag is in the ON state
     * Equivalent to COBOL 88-level condition: IF ERR-FLG-ON
     * 
     * @return true if this flag is ON, false otherwise
     */
    public boolean isOn() {
        return this == ON;
    }
    
    /**
     * Checks if this error flag is in the OFF state
     * Equivalent to COBOL 88-level condition: IF ERR-FLG-OFF
     * 
     * @return true if this flag is OFF, false otherwise
     */
    public boolean isOff() {
        return this == OFF;
    }
    
    /**
     * Converts a boolean value to an ErrorFlag enum value
     * Supports Spring Boot error handling patterns where boolean
     * error states need to be converted to standardized flag values
     * 
     * @param hasError true to get ON state, false to get OFF state
     * @return ON if hasError is true, OFF if hasError is false
     */
    public static ErrorFlag fromBoolean(boolean hasError) {
        return hasError ? ON : OFF;
    }
    
    /**
     * Converts this ErrorFlag to a boolean value
     * Useful for Spring Boot validation logic and conditional processing
     * 
     * @return true if this flag is ON, false if this flag is OFF
     */
    public boolean toBoolean() {
        return this == ON;
    }
    
    /**
     * Converts a COBOL character value to an ErrorFlag enum value
     * Supports migration from COBOL PIC X(01) error flag fields
     * 
     * @param cobolValue the COBOL character value ('Y', 'N', or other)
     * @return ON for 'Y', OFF for 'N' or any other value
     */
    public static ErrorFlag fromCobolValue(char cobolValue) {
        return (cobolValue == 'Y') ? ON : OFF;
    }
    
    /**
     * Converts a COBOL string value to an ErrorFlag enum value
     * Handles string representations of COBOL error flags
     * 
     * @param cobolValue the COBOL string value ("Y", "N", or other)
     * @return ON for "Y", OFF for "N" or any other value
     */
    public static ErrorFlag fromCobolValue(String cobolValue) {
        if (cobolValue == null || cobolValue.isEmpty()) {
            return OFF;
        }
        return fromCobolValue(cobolValue.charAt(0));
    }
    
    /**
     * Returns a string representation suitable for logging and debugging
     * Includes both the enum name and the COBOL value for clarity
     * 
     * @return formatted string representation
     */
    @Override
    public String toString() {
        return name() + " (" + cobolValue + ")";
    }
    
    /**
     * Validates that the error flag is in a valid state
     * Used by Spring Boot validation framework for error state checking
     * 
     * @return true if the flag is in a valid state (always true for enum values)
     */
    public boolean isValid() {
        return true;
    }
    
    /**
     * Gets a user-friendly description of the error flag state
     * Suitable for REST API error responses and React frontend display
     * 
     * @return descriptive string for the error flag state
     */
    public String getDescription() {
        switch (this) {
            case ON:
                return "Error condition detected";
            case OFF:
                return "No error condition";
            default:
                return "Unknown error state";
        }
    }
    
    /**
     * Toggles the error flag state
     * Useful for testing and conditional logic that needs to flip error states
     * 
     * @return OFF if this flag is ON, ON if this flag is OFF
     */
    public ErrorFlag toggle() {
        return this == ON ? OFF : ON;
    }
}