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
package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;

import java.util.Objects;

/**
 * Generic API request wrapper DTO providing standard request envelope for all REST endpoints.
 * 
 * This class serves as the standard container for all incoming REST API requests in the CardDemo 
 * application, providing a consistent request structure that maps CICS transaction code patterns 
 * to modern REST endpoints while maintaining session state through Spring Session.
 * 
 * The design replicates the original COBOL COMMAREA structure defined in COCOM01Y.cpy, ensuring
 * functional equivalence between the mainframe transaction processing and the cloud-native REST
 * API architecture.
 * 
 * Key Features:
 * - Transaction code mapping to maintain CICS transaction routing patterns (CC00, CT00, etc.)
 * - Generic request data payload supporting type-safe request handling across all endpoints
 * - Session context management equivalent to CICS COMMAREA for maintaining user state
 * - JSON serialization support for REST API communication
 * - Bean validation integration for comprehensive request validation
 * 
 * @param <T> The type of the request data payload, enabling type-safe request processing
 * 
 * @see SessionContext Session context DTO for maintaining user state across REST calls
 * @see com.carddemo.controller REST controllers that consume ApiRequest instances
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@AllArgsConstructor
public class ApiRequest<T> {
    
    /**
     * Transaction code identifying the specific business operation being requested.
     * 
     * Maps directly to original CICS transaction codes (e.g., COSGN00C → "COSGN00", 
     * COMEN01C → "COMEN01", COTRN00C → "COTRN00") to maintain consistent routing
     * patterns between the mainframe and cloud-native implementations.
     * 
     * This field ensures that REST endpoint routing can maintain identical transaction
     * identification patterns while enabling proper request dispatch to corresponding
     * Spring Boot controller methods.
     * 
     * @see com.carddemo.controller.TransactionController Transaction routing implementation
     */
    @JsonProperty("transactionCode")
    @NotNull(message = "Transaction code is required for request routing")
    private String transactionCode;
    
    /**
     * Generic request data payload containing the business-specific request information.
     * 
     * This field provides type-safe request data handling while maintaining flexibility
     * across different types of business operations. The generic type parameter allows
     * each REST endpoint to specify its expected request data structure while maintaining
     * a consistent request envelope format.
     * 
     * Examples of payload types:
     * - SignInRequest for authentication operations
     * - AccountRequest for account management operations  
     * - TransactionRequest for transaction processing operations
     * - CardRequest for credit card management operations
     * 
     * The payload structure preserves the original data formats from COBOL copybooks
     * while leveraging Java type safety and JSON serialization capabilities.
     */
    @JsonProperty("requestData")
    private T requestData;
    
    /**
     * Session context containing user session information equivalent to CICS COMMAREA.
     * 
     * Maintains user state across REST API calls through Spring Session, providing the
     * same stateful interaction capabilities as the original CICS COMMAREA structure.
     * This includes user identification, role information, transaction history, and
     * navigation context required for proper business logic execution.
     * 
     * The session context enables the Spring Boot application to maintain user state
     * across multiple REST API invocations while supporting horizontal scaling through
     * Redis-backed session clustering.
     * 
     * @see SessionContext Detailed session context structure and capabilities
     */
    @JsonProperty("sessionContext")
    @Valid
    private SessionContext sessionContext;
    
    /**
     * Default constructor for JSON deserialization and framework compatibility.
     * 
     * Required for Jackson JSON processing and Spring framework instantiation.
     * The @AllArgsConstructor annotation provides the parameterized constructor
     * for convenient object creation in business logic and test scenarios.
     */
    public ApiRequest() {
        // Default constructor for JSON deserialization
    }
    
    /**
     * Retrieves the transaction code identifying the requested business operation.
     * 
     * @return The transaction code for request routing (e.g., "COSGN00", "COTRN01")
     */
    public String getTransactionCode() {
        return transactionCode;
    }
    
    /**
     * Sets the transaction code for request routing.
     * 
     * @param transactionCode The transaction code identifying the business operation
     */
    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }
    
    /**
     * Retrieves the generic request data payload.
     * 
     * @return The type-safe request data containing business-specific information
     */
    public T getRequestData() {
        return requestData;
    }
    
    /**
     * Sets the request data payload.
     * 
     * @param requestData The business-specific request data
     */
    public void setRequestData(T requestData) {
        this.requestData = requestData;
    }
    
    /**
     * Retrieves the session context containing user state information.
     * 
     * @return The session context equivalent to CICS COMMAREA
     */
    public SessionContext getSessionContext() {
        return sessionContext;
    }
    
    /**
     * Sets the session context for maintaining user state.
     * 
     * @param sessionContext The session context containing user information
     */
    public void setSessionContext(SessionContext sessionContext) {
        this.sessionContext = sessionContext;
    }
    
    /**
     * Provides a string representation of the API request for logging and debugging.
     * 
     * Includes transaction code and session context information while protecting
     * sensitive request data from exposure in log files. The request data is
     * represented by its class name only to maintain security while providing
     * useful debugging information.
     * 
     * @return A formatted string representation suitable for logging
     */
    @Override
    public String toString() {
        return "ApiRequest{" +
                "transactionCode='" + transactionCode + '\'' +
                ", requestData=" + (requestData != null ? requestData.getClass().getSimpleName() : "null") +
                ", sessionContext=" + sessionContext +
                '}';
    }
    
    /**
     * Determines equality based on transaction code, request data, and session context.
     * 
     * Two ApiRequest instances are considered equal if they have the same transaction
     * code, equivalent request data, and identical session context. This implementation
     * supports proper object comparison in collections and testing scenarios.
     * 
     * @param obj The object to compare with this ApiRequest
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ApiRequest<?> that = (ApiRequest<?>) obj;
        return Objects.equals(transactionCode, that.transactionCode) &&
               Objects.equals(requestData, that.requestData) &&
               Objects.equals(sessionContext, that.sessionContext);
    }
    
    /**
     * Generates hash code based on transaction code, request data, and session context.
     * 
     * Provides consistent hash code generation for use in hash-based collections
     * such as HashMap and HashSet. The implementation ensures that equal objects
     * have equal hash codes as required by the Object contract.
     * 
     * @return The computed hash code for this ApiRequest instance
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionCode, requestData, sessionContext);
    }
}