/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.enums;

/**
 * Error flag enumeration that preserves exact COBOL Y/N boolean logic patterns
 * for consistent error handling across all CardDemo microservices.
 * 
 * <p>This enum converts the traditional COBOL error flag pattern:
 * <pre>
 * 05 WS-ERR-FLG                 PIC X(01) VALUE 'N'.
 *   88 ERR-FLG-ON                         VALUE 'Y'.
 *   88 ERR-FLG-OFF                        VALUE 'N'.
 * </pre>
 * 
 * <p>Usage patterns preserved from COBOL:
 * <ul>
 *   <li>SET ERR-FLG-OFF TO TRUE  → ErrorFlag.OFF.isOff() or !ErrorFlag.ON.isOn()</li>
 *   <li>SET ERR-FLG-ON TO TRUE   → ErrorFlag.ON.isOn() or !ErrorFlag.OFF.isOff()</li>
 *   <li>MOVE 'Y' TO WS-ERR-FLG   → ErrorFlag.fromBoolean(true)</li>
 *   <li>IF NOT ERR-FLG-ON        → if (!errorFlag.isOn())</li>
 *   <li>IF ERR-FLG-ON           → if (errorFlag.isOn())</li>
 * </ul>
 * 
 * <p>Integration with Spring Boot error handling:
 * <ul>
 *   <li>Supports @ControllerAdvice global exception handling patterns</li>
 *   <li>Compatible with Bean Validation error responses</li>
 *   <li>Integrates with ProblemDetails structured error format</li>
 *   <li>Enables consistent error state management across REST APIs</li>
 * </ul>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since Java 21
 */
public enum ErrorFlag {
    
    /**
     * Error flag OFF state - indicates no error condition exists.
     * Corresponds to COBOL ERR-FLG-OFF condition with value 'N'.
     * 
     * <p>This represents the normal, successful operational state where
     * no errors have been detected during processing.
     */
    OFF('N', false),
    
    /**
     * Error flag ON state - indicates an error condition has been detected.
     * Corresponds to COBOL ERR-FLG-ON condition with value 'Y'.
     * 
     * <p>This represents an error state where validation failures,
     * business rule violations, or system errors have occurred.
     */
    ON('Y', true);
    
    // COBOL character representation ('Y' or 'N')
    private final char cobolValue;
    
    // Boolean representation for modern Java integration
    private final boolean booleanValue;
    
    /**
     * Private constructor for enum initialization.
     * 
     * @param cobolValue COBOL character representation ('Y' or 'N')
     * @param booleanValue Boolean equivalent for Java logic
     */
    private ErrorFlag(char cobolValue, boolean booleanValue) {
        this.cobolValue = cobolValue;
        this.booleanValue = booleanValue;
    }
    
    /**
     * Checks if the error flag is in the ON state (error condition exists).
     * 
     * <p>Equivalent to COBOL: IF ERR-FLG-ON
     * 
     * @return true if error flag is ON (error condition), false otherwise
     */
    public boolean isOn() {
        return this == ON;
    }
    
    /**
     * Checks if the error flag is in the OFF state (no error condition).
     * 
     * <p>Equivalent to COBOL: IF ERR-FLG-OFF or IF NOT ERR-FLG-ON
     * 
     * @return true if error flag is OFF (no error), false otherwise
     */
    public boolean isOff() {
        return this == OFF;
    }
    
    /**
     * Converts a boolean value to the corresponding ErrorFlag enum value.
     * 
     * <p>Equivalent to COBOL patterns:
     * <ul>
     *   <li>MOVE 'Y' TO WS-ERR-FLG → fromBoolean(true)</li>
     *   <li>MOVE 'N' TO WS-ERR-FLG → fromBoolean(false)</li>
     *   <li>SET ERR-FLG-ON TO TRUE → fromBoolean(true)</li>
     *   <li>SET ERR-FLG-OFF TO TRUE → fromBoolean(false)</li>
     * </ul>
     * 
     * @param hasError true to set error flag ON, false to set error flag OFF
     * @return ErrorFlag.ON if hasError is true, ErrorFlag.OFF if hasError is false
     */
    public static ErrorFlag fromBoolean(boolean hasError) {
        return hasError ? ON : OFF;
    }
    
    /**
     * Converts the ErrorFlag enum value to its boolean representation.
     * 
     * <p>Useful for integration with Spring Boot validation frameworks
     * and modern Java boolean logic patterns.
     * 
     * @return true if error flag is ON (error condition), false if OFF (no error)
     */
    public boolean toBoolean() {
        return this.booleanValue;
    }
    
    /**
     * Gets the COBOL character representation of the error flag.
     * 
     * <p>Returns the original COBOL character value:
     * <ul>
     *   <li>'Y' for error flag ON (error condition)</li>
     *   <li>'N' for error flag OFF (no error condition)</li>
     * </ul>
     * 
     * <p>This method maintains exact compatibility with legacy COBOL
     * data structures and enables seamless data migration.
     * 
     * @return 'Y' for ON state, 'N' for OFF state
     */
    public char getCobolValue() {
        return this.cobolValue;
    }
    
    /**
     * Creates an ErrorFlag from a COBOL character value.
     * 
     * <p>Supports parsing COBOL error flag values during data migration
     * and integration with legacy systems.
     * 
     * @param cobolValue COBOL character representation ('Y', 'y', 'N', 'n')
     * @return ErrorFlag.ON for 'Y' or 'y', ErrorFlag.OFF for 'N' or 'n'
     * @throws IllegalArgumentException if cobolValue is not a valid error flag character
     */
    public static ErrorFlag fromCobolValue(char cobolValue) {
        switch (Character.toUpperCase(cobolValue)) {
            case 'Y':
                return ON;
            case 'N':
                return OFF;
            default:
                throw new IllegalArgumentException(
                    String.format("Invalid COBOL error flag value: '%c'. Expected 'Y' or 'N'", cobolValue));
        }
    }
    
    /**
     * Creates an ErrorFlag from a string representation.
     * 
     * <p>Supports integration with Spring Boot REST APIs, configuration properties,
     * and JSON serialization/deserialization patterns.
     * 
     * @param value String representation ("Y", "N", "ON", "OFF", "true", "false")
     * @return Corresponding ErrorFlag enum value
     * @throws IllegalArgumentException if value cannot be parsed as a valid error flag
     */
    public static ErrorFlag fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return OFF; // Default to no error for null/empty values
        }
        
        String upperValue = value.trim().toUpperCase();
        
        switch (upperValue) {
            case "Y":
            case "ON":
            case "TRUE":
            case "1":
                return ON;
            case "N":
            case "OFF":
            case "FALSE":
            case "0":
                return OFF;
            default:
                throw new IllegalArgumentException(
                    String.format("Invalid error flag string value: '%s'. " +
                                "Expected 'Y', 'N', 'ON', 'OFF', 'true', 'false', '1', or '0'", value));
        }
    }
    
    /**
     * Returns a string representation suitable for logging and debugging.
     * 
     * <p>Format: "ErrorFlag.ON(Y)" or "ErrorFlag.OFF(N)"
     * 
     * @return Human-readable string representation including COBOL value
     */
    @Override
    public String toString() {
        return String.format("ErrorFlag.%s(%c)", this.name(), this.cobolValue);
    }
    
    /**
     * Provides a user-friendly description of the error flag state.
     * 
     * <p>Useful for creating error messages in Spring Boot REST API responses
     * and React frontend error displays.
     * 
     * @return Human-readable description of the error flag state
     */
    public String getDescription() {
        switch (this) {
            case ON:
                return "Error condition detected";
            case OFF:
                return "No error condition";
            default:
                return "Unknown error flag state";
        }
    }
    
    /**
     * Toggles the error flag state.
     * 
     * <p>Utility method for error handling scenarios where the error state
     * needs to be flipped based on conditional logic.
     * 
     * @return ErrorFlag.OFF if current state is ON, ErrorFlag.ON if current state is OFF
     */
    public ErrorFlag toggle() {
        return this == ON ? OFF : ON;
    }
    
    /**
     * Combines multiple error flags using logical OR operation.
     * 
     * <p>Returns ErrorFlag.ON if any of the provided flags is ON,
     * otherwise returns ErrorFlag.OFF. Useful for aggregating multiple
     * validation results in Spring Boot service methods.
     * 
     * <p>Equivalent to COBOL pattern:
     * <pre>
     * IF ERR-FLG-1-ON OR ERR-FLG-2-ON OR ERR-FLG-3-ON
     *     SET COMBINED-ERR-FLG-ON TO TRUE
     * ELSE
     *     SET COMBINED-ERR-FLG-OFF TO TRUE
     * END-IF
     * </pre>
     * 
     * @param flags Variable number of ErrorFlag values to combine
     * @return ErrorFlag.ON if any flag is ON, ErrorFlag.OFF if all flags are OFF
     */
    public static ErrorFlag combineWithOr(ErrorFlag... flags) {
        if (flags == null || flags.length == 0) {
            return OFF;
        }
        
        for (ErrorFlag flag : flags) {
            if (flag != null && flag.isOn()) {
                return ON;
            }
        }
        
        return OFF;
    }
    
    /**
     * Combines multiple error flags using logical AND operation.
     * 
     * <p>Returns ErrorFlag.ON if all provided flags are ON,
     * otherwise returns ErrorFlag.OFF. Useful for validation scenarios
     * where all conditions must pass.
     * 
     * @param flags Variable number of ErrorFlag values to combine
     * @return ErrorFlag.ON if all flags are ON, ErrorFlag.OFF otherwise
     */
    public static ErrorFlag combineWithAnd(ErrorFlag... flags) {
        if (flags == null || flags.length == 0) {
            return OFF;
        }
        
        for (ErrorFlag flag : flags) {
            if (flag == null || flag.isOff()) {
                return OFF;
            }
        }
        
        return ON;
    }
}