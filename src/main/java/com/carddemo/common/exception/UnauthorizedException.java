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

/**
 * Exception thrown when unauthorized access is attempted.
 * 
 * This exception is used throughout the CardDemo application to indicate
 * when access control violations occur. It provides correlation ID support
 * for audit logging and security monitoring.
 */
public class UnauthorizedException extends RuntimeException {

    private final String correlationId;
    private final String userId;
    private final String requestedResource;

    /**
     * Constructor with message, user ID, resource, and correlation ID.
     * 
     * @param message The error message
     * @param userId The user ID attempting unauthorized access
     * @param requestedResource The resource that was being accessed
     * @param correlationId Unique identifier for request tracking
     */
    public UnauthorizedException(String message, String userId, String requestedResource, String correlationId) {
        super(message);
        this.userId = userId;
        this.requestedResource = requestedResource;
        this.correlationId = correlationId;
    }

    /**
     * Constructor with message and correlation ID.
     * 
     * @param message The error message
     * @param correlationId Unique identifier for request tracking
     */
    public UnauthorizedException(String message, String correlationId) {
        super(message);
        this.correlationId = correlationId;
        this.userId = null;
        this.requestedResource = null;
    }

    /**
     * Constructor with message only.
     * 
     * @param message The error message
     */
    public UnauthorizedException(String message) {
        super(message);
        this.correlationId = null;
        this.userId = null;
        this.requestedResource = null;
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
     * Gets the user ID that attempted unauthorized access.
     * 
     * @return The user ID, may be null
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the requested resource that caused the unauthorized access.
     * 
     * @return The requested resource, may be null
     */
    public String getRequestedResource() {
        return requestedResource;
    }
}