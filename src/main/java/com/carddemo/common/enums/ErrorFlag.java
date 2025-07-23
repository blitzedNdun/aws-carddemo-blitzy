/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.carddemo.common.enums;

/**
 * ErrorFlag enumeration converting COBOL Y/N error flag patterns to Java enum
 * with boolean validation logic for consistent error handling behavior.
 * 
 * <p>This enum preserves the original COBOL error detection and reporting 
 * functionality while supporting Spring Boot error handling patterns and 
 * REST API error response standardization.</p>
 * 
 * <p>Original COBOL Pattern:</p>
 * <pre>
 * 05 WS-ERR-FLG                 PIC X(01) VALUE 'N'.
 *   88 ERR-FLG-ON                         VALUE 'Y'.
 *   88 ERR-FLG-OFF                        VALUE 'N'.
 * </pre>
 * 
 * <p>Usage patterns converted:</p>
 * <ul>
 *   <li>COBOL: {@code SET ERR-FLG-OFF TO TRUE} → Java: {@code ErrorFlag.OFF}</li>
 *   <li>COBOL: {@code MOVE 'Y' TO WS-ERR-FLG} → Java: {@code ErrorFlag.ON}</li>
 *   <li>COBOL: {@code IF NOT ERR-FLG-ON} → Java: {@code !errorFlag.isOn()}</li>
 * </ul>
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public enum ErrorFlag {
    
    /**
     * Error state is ON (equivalent to COBOL 'Y' value).
     * Indicates an error condition has been detected and should be handled.
     */
    ON('Y'),
    
    /**
     * Error state is OFF (equivalent to COBOL 'N' value).
     * Indicates normal processing with no error conditions detected.
     */
    OFF('N');
    
    /**
     * The COBOL character value ('Y' or 'N') associated with this error flag state.
     */
    private final char cobolValue;
    
    /**
     * Constructs an ErrorFlag with the specified COBOL character value.
     * 
     * @param cobolValue the COBOL character value ('Y' for ON, 'N' for OFF)
     */
    ErrorFlag(char cobolValue) {
        this.cobolValue = cobolValue;
    }
    
    /**
     * Returns true if this error flag represents an error condition (ON state).
     * Equivalent to COBOL condition: {@code IF ERR-FLG-ON}
     * 
     * @return true if error flag is ON, false otherwise
     */
    public boolean isOn() {
        return this == ON;
    }
    
    /**
     * Returns true if this error flag represents no error condition (OFF state).
     * Equivalent to COBOL condition: {@code IF ERR-FLG-OFF}
     * 
     * @return true if error flag is OFF, false otherwise
     */
    public boolean isOff() {
        return this == OFF;
    }
    
    /**
     * Converts a boolean value to the corresponding ErrorFlag enum value.
     * 
     * <p>This method provides a convenient way to convert boolean error states
     * to ErrorFlag instances, supporting Spring Boot error handling patterns
     * where boolean error conditions are common.</p>
     * 
     * @param hasError true to return ErrorFlag.ON, false to return ErrorFlag.OFF
     * @return ErrorFlag.ON if hasError is true, ErrorFlag.OFF if hasError is false
     */
    public static ErrorFlag fromBoolean(boolean hasError) {
        return hasError ? ON : OFF;
    }
    
    /**
     * Converts this ErrorFlag to a boolean value.
     * 
     * <p>This method enables integration with Spring Boot exception handling
     * and React error display components that expect boolean error flags.</p>
     * 
     * @return true if this error flag is ON, false if this error flag is OFF
     */
    public boolean toBoolean() {
        return this.isOn();
    }
    
    /**
     * Returns the COBOL character value associated with this error flag.
     * 
     * <p>This method preserves compatibility with legacy COBOL processing
     * patterns and supports data exchange with COBOL-derived systems.</p>
     * 
     * @return 'Y' for ON state, 'N' for OFF state
     */
    public char getCobolValue() {
        return this.cobolValue;
    }
    
    /**
     * Creates an ErrorFlag from a COBOL character value.
     * 
     * <p>This method supports conversion from COBOL data structures and
     * maintains exact compatibility with original COBOL 88-level condition
     * patterns.</p>
     * 
     * @param cobolValue the COBOL character value ('Y', 'N', or case variations)
     * @return ErrorFlag.ON for 'Y'/'y', ErrorFlag.OFF for 'N'/'n'
     * @throws IllegalArgumentException if cobolValue is not 'Y', 'y', 'N', or 'n'
     */
    public static ErrorFlag fromCobolValue(char cobolValue) {
        switch (Character.toUpperCase(cobolValue)) {
            case 'Y':
                return ON;
            case 'N':
                return OFF;
            default:
                throw new IllegalArgumentException(
                    String.format("Invalid COBOL error flag value: '%c'. Expected 'Y' or 'N'.", cobolValue)
                );
        }
    }
    
    /**
     * Creates an ErrorFlag from a string representation.
     * 
     * <p>This method supports REST API error response parsing and JSON
     * deserialization scenarios common in Spring Boot applications.</p>
     * 
     * @param value the string value ("ON", "OFF", "Y", "N", case-insensitive)
     * @return corresponding ErrorFlag enum value
     * @throws IllegalArgumentException if value is null or not recognized
     */
    public static ErrorFlag fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Error flag value cannot be null");
        }
        
        String upperValue = value.trim().toUpperCase();
        
        switch (upperValue) {
            case "ON":
            case "Y":
            case "TRUE":
                return ON;
            case "OFF":
            case "N":
            case "FALSE":
                return OFF;
            default:
                throw new IllegalArgumentException(
                    String.format("Invalid error flag value: '%s'. Expected 'ON', 'OFF', 'Y', 'N', 'TRUE', or 'FALSE'.", value)
                );
        }
    }
    
    /**
     * Returns a string representation suitable for logging and debugging.
     * 
     * <p>This method provides human-readable output for Spring Boot logging
     * frameworks and error reporting systems.</p>
     * 
     * @return "ON" for error state, "OFF" for normal state
     */
    @Override
    public String toString() {
        return this.name();
    }
    
    /**
     * Returns a detailed string representation including COBOL value.
     * 
     * <p>This method is useful for debugging COBOL-to-Java conversion
     * processes and maintaining audit trails of error flag transformations.</p>
     * 
     * @return detailed representation like "ON ('Y')" or "OFF ('N')"
     */
    public String toDetailedString() {
        return String.format("%s ('%c')", this.name(), this.cobolValue);
    }
}