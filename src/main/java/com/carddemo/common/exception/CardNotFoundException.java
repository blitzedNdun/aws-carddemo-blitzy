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
 * Exception thrown when a requested card cannot be found.
 * 
 * This exception is used throughout the CardDemo application to indicate
 * when card lookup operations fail to find a matching record. It provides
 * correlation ID support for audit logging and request tracking.
 */
public class CardNotFoundException extends RuntimeException {

    private final String correlationId;
    private final String cardNumber;

    /**
     * Constructor with message and correlation ID.
     * 
     * @param message The error message
     * @param correlationId Unique identifier for request tracking
     */
    public CardNotFoundException(String message, String correlationId) {
        super(message);
        this.correlationId = correlationId;
        this.cardNumber = null;
    }

    /**
     * Constructor with message, card number, and correlation ID.
     * 
     * @param message The error message
     * @param cardNumber The card number that was not found
     * @param correlationId Unique identifier for request tracking
     */
    public CardNotFoundException(String message, String cardNumber, String correlationId) {
        super(message);
        this.correlationId = correlationId;
        this.cardNumber = cardNumber;
    }



    /**
     * Constructor with message only.
     * 
     * @param message The error message
     */
    public CardNotFoundException(String message) {
        super(message);
        this.correlationId = null;
        this.cardNumber = null;
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
     * Gets the card number that was not found.
     * 
     * @return The card number, may be null
     */
    public String getCardNumber() {
        return cardNumber;
    }
}