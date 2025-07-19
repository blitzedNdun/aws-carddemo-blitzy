/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.carddemo.common.exception;

import com.carddemo.common.dto.ValidationResult;

/**
 * Exception thrown when validation failures occur.
 * 
 * This exception is used throughout the CardDemo application to indicate
 * when input validation fails. It provides correlation ID support for
 * audit logging and detailed validation result information.
 */
public class ValidationException extends RuntimeException {

    private final String correlationId;
    private final ValidationResult validationResult;

    /**
     * Constructor with message, validation result, and correlation ID.
     * 
     * @param message The error message
     * @param validationResult Detailed validation error information
     * @param correlationId Unique identifier for request tracking
     */
    public ValidationException(String message, ValidationResult validationResult, String correlationId) {
        super(message);
        this.validationResult = validationResult;
        this.correlationId = correlationId;
    }

    /**
     * Constructor with message and validation result.
     * 
     * @param message The error message
     * @param validationResult Detailed validation error information
     */
    public ValidationException(String message, ValidationResult validationResult) {
        super(message);
        this.validationResult = validationResult;
        this.correlationId = null;
    }

    /**
     * Constructor with message and correlation ID.
     * 
     * @param message The error message
     * @param correlationId Unique identifier for request tracking
     */
    public ValidationException(String message, String correlationId) {
        super(message);
        this.validationResult = null;
        this.correlationId = correlationId;
    }

    /**
     * Constructor with message only.
     * 
     * @param message The error message
     */
    public ValidationException(String message) {
        super(message);
        this.validationResult = null;
        this.correlationId = null;
    }

    /**
     * Gets the correlation ID for this exception.
     * 
     * @return The correlation ID, may be null
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Gets the detailed validation result information.
     * 
     * @return The validation result, may be null
     */
    public ValidationResult getValidationResult() {
        return validationResult;
    }
}