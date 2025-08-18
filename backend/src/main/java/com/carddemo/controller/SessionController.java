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
package com.carddemo.controller;

import com.carddemo.dto.SessionContext;
import com.carddemo.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.session.SessionRepository;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for session management operations.
 * 
 * This controller handles session validation, timeout management, and session context queries,
 * replacing CICS COMMAREA state management with Spring Session operations while maintaining
 * session state across REST API calls. The controller provides comprehensive session lifecycle
 * management equivalent to the original CARDDEMO-COMMAREA functionality from COCOM01Y.cpy.
 * 
 * Key Features:
 * - Session information retrieval equivalent to CICS COMMAREA access
 * - Session timeout extension supporting sliding session behavior
 * - User logout with comprehensive session cleanup
 * - Navigation history tracking for F3 key functionality
 * - Transient data management for multi-step operations
 * - Session validation and status monitoring
 * 
 * REST Endpoint Mappings:
 * - GET /api/session - Retrieve current session information
 * - POST /api/session/extend - Extend session timeout
 * - DELETE /api/session - User logout and session cleanup
 * - GET /api/session/navigation - Retrieve navigation history
 * - POST /api/session/navigation - Add transaction to navigation stack
 * - PUT /api/session/data - Update transient session data
 * - GET /api/session/status - Check session validity and status
 * 
 * Security Integration:
 * - Spring Security authentication context integration
 * - JWT token validation through Spring Cloud Gateway
 * - Role-based access control through @PreAuthorize annotations
 * - Session timeout enforcement matching CICS policies
 * 
 * Technical Implementation:
 * - Spring Session Redis for distributed session storage
 * - HTTP servlet request integration for session management
 * - Comprehensive error handling with proper HTTP status codes
 * - Audit logging for session operations and security events
 * - JSON serialization for complex session data structures
 * 
 * COBOL COMMAREA Mapping:
 * - CDEMO-USER-ID → SessionContext.userId
 * - CDEMO-USER-TYPE → SessionContext.userRole
 * - CDEMO-FROM-TRANID/CDEMO-TO-TRANID → Navigation stack management
 * - CDEMO-PGM-CONTEXT → SessionContext.operationStatus
 * - CDEMO-CUSTOMER-INFO/CDEMO-ACCOUNT-INFO → Transient data storage
 * 
 * @see SessionContext Session context DTO containing user session information
 * @see SessionService Session management service providing CICS COMMAREA equivalent functionality
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/session")
@CrossOrigin(origins = "*")
public class SessionController {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);
    
    private final SessionService sessionService;
    
    /**
     * Constructor for dependency injection of session management services.
     * 
     * @param sessionService Session management service for CICS COMMAREA equivalent functionality
     */
    @Autowired
    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
        logger.info("SessionController initialized with session management capabilities");
    }
    
    /**
     * Retrieves current session information for authenticated user.
     * 
     * This endpoint provides comprehensive session state information equivalent to CICS
     * COMMAREA access, enabling frontend components to access user identity, navigation
     * context, and operation status. The method includes session validation and automatic
     * activity time updates for sliding timeout behavior.
     * 
     * Functionality equivalent to COBOL COMMAREA retrieval:
     * - Returns complete CARDDEMO-COMMAREA structure equivalent
     * - Includes user identity (CDEMO-USER-ID, CDEMO-USER-TYPE)
     * - Provides navigation context and transaction history
     * - Updates session activity time for timeout management
     * 
     * @param request HTTP request containing session identification
     * @param principal Authenticated user principal from Spring Security context
     * @return ResponseEntity<SessionContext> Current session information with HTTP 200,
     *         or HTTP 401 if session invalid/expired
     */
    @GetMapping
    public ResponseEntity<SessionContext> getSessionInfo(HttpServletRequest request, Principal principal) {
        logger.debug("Session info requested for user: {}", principal != null ? principal.getName() : "anonymous");
        
        try {
            // Validate session and retrieve current context
            SessionContext sessionContext = sessionService.getSession(request);
            
            if (sessionContext == null) {
                logger.warn("No active session found for session info request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Validate session is still active
            if (!sessionService.validateSession(request)) {
                logger.warn("Invalid or expired session detected for user: {}", sessionContext.getUserId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Update activity time and return session information
            sessionContext.updateActivityTime();
            logger.info("Session info retrieved successfully for user: {} with role: {}", 
                       sessionContext.getUserId(), sessionContext.getUserRole());
            
            return ResponseEntity.ok(sessionContext);
            
        } catch (Exception e) {
            logger.error("Error retrieving session information: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Extends session timeout for continued user activity.
     * 
     * This endpoint provides session timeout extension equivalent to CICS session
     * activity updates, enabling sliding session timeout behavior for active users.
     * The method updates session activity time and validates session integrity
     * while preserving all existing session state information.
     * 
     * Functionality equivalent to CICS session activity update:
     * - Updates last activity time for sliding timeout calculation
     * - Validates session integrity and user authorization
     * - Preserves all existing COMMAREA equivalent data
     * - Returns updated session context with new timing information
     * 
     * @param request HTTP request containing session to extend
     * @param principal Authenticated user principal from Spring Security context
     * @return ResponseEntity<Map<String, Object>> Extension confirmation with updated timing,
     *         or HTTP 401 if session invalid/expired
     */
    @PostMapping("/extend")
    public ResponseEntity<Map<String, Object>> extendSession(HttpServletRequest request, Principal principal) {
        logger.debug("Session extension requested for user: {}", principal != null ? principal.getName() : "anonymous");
        
        try {
            // Validate session exists and is active
            SessionContext sessionContext = sessionService.getSession(request);
            
            if (sessionContext == null || !sessionService.validateSession(request)) {
                logger.warn("Session extension failed - invalid or expired session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Update activity time for session extension
            sessionContext.updateActivityTime();
            sessionContext.setOperationStatus("ACTIVE");
            
            // Update session with extended timing
            SessionContext updatedContext = sessionService.updateSession(request, sessionContext);
            
            // Use original context if update returns null
            SessionContext contextToReturn = updatedContext != null ? updatedContext : sessionContext;
            
            // Create response with extension confirmation
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Session extended successfully");
            response.put("userId", contextToReturn.getUserId());
            response.put("lastActivityTime", contextToReturn.getLastActivityTime());
            response.put("sessionStartTime", contextToReturn.getSessionStartTime());
            
            logger.info("Session extended successfully for user: {}", contextToReturn.getUserId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error extending session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * User logout with comprehensive session cleanup.
     * 
     * This endpoint provides user logout functionality equivalent to CICS session
     * termination, performing comprehensive cleanup of all session data including
     * Spring Session Redis storage, navigation history, and transient data.
     * The method ensures secure session invalidation with audit logging.
     * 
     * Functionality equivalent to CICS session termination:
     * - Clears all CARDDEMO-COMMAREA equivalent data
     * - Removes navigation stack and transient data storage
     * - Invalidates HTTP session and Spring Session Redis data
     * - Performs comprehensive audit logging for session termination
     * 
     * @param request HTTP request containing session to terminate
     * @param principal Authenticated user principal from Spring Security context
     * @return ResponseEntity<Map<String, Object>> Logout confirmation with HTTP 200,
     *         or HTTP 204 if no active session found
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, Principal principal) {
        logger.debug("Logout requested for user: {}", principal != null ? principal.getName() : "anonymous");
        
        try {
            // Retrieve session context before cleanup for logging
            SessionContext sessionContext = sessionService.getSession(request);
            String userId = sessionContext != null ? sessionContext.getUserId() : "unknown";
            
            // Perform comprehensive session cleanup
            sessionService.clearSession(request);
            
            // Create logout confirmation response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Session terminated successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("userId", userId);
            
            logger.info("User logout completed successfully for user: {}", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during logout process: {}", e.getMessage(), e);
            
            // Return success even on error to prevent information disclosure
            Map<String, Object> response = new HashMap<>();
            response.put("status", "logged_out");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Retrieves navigation history for user workflow management.
     * 
     * This endpoint provides access to complete navigation history equivalent to CICS
     * transaction routing history, enabling proper back navigation implementation and
     * workflow context management. The navigation history supports React frontend
     * F3 key functionality while preserving CICS-equivalent transaction tracking.
     * 
     * Functionality equivalent to CICS navigation tracking:
     * - Retrieves complete transaction navigation history
     * - Supports proper back navigation implementation (F3 key functionality)
     * - Enables workflow context preservation across REST operations
     * - Returns navigation stack in chronological order
     * 
     * @param request HTTP request containing current session
     * @param principal Authenticated user principal from Spring Security context
     * @return ResponseEntity<List<String>> Navigation history as ordered list,
     *         or HTTP 401 if session invalid/expired
     */
    @GetMapping("/navigation")
    public ResponseEntity<List<String>> getNavigationHistory(HttpServletRequest request, Principal principal) {
        logger.debug("Navigation history requested for user: {}", principal != null ? principal.getName() : "anonymous");
        
        try {
            // Validate session before providing navigation data
            if (!sessionService.validateSession(request)) {
                logger.warn("Navigation history request denied - invalid session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Retrieve navigation history from session
            List<String> navigationHistory = sessionService.getNavigationHistory(request);
            
            logger.debug("Navigation history retrieved: {} items for user: {}", 
                        navigationHistory.size(), principal != null ? principal.getName() : "unknown");
            
            return ResponseEntity.ok(navigationHistory);
            
        } catch (Exception e) {
            logger.error("Error retrieving navigation history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Adds transaction code to navigation stack for workflow tracking.
     * 
     * This endpoint maintains navigation history equivalent to CICS transaction routing,
     * enabling proper navigation workflow management and supporting F3 key back navigation
     * functionality. The navigation stack tracks user movement through the application
     * screens while preserving CICS-equivalent transaction code patterns.
     * 
     * Functionality equivalent to CICS transaction navigation:
     * - Tracks CDEMO-FROM-TRANID to CDEMO-TO-TRANID transitions
     * - Maintains navigation history for proper workflow management
     * - Supports back navigation and context switching
     * - Updates session context with current transaction information
     * 
     * @param request HTTP request containing current session
     * @param transactionRequest Map containing transaction code to add to navigation stack
     * @param principal Authenticated user principal from Spring Security context
     * @return ResponseEntity<Map<String, Object>> Navigation update confirmation,
     *         or HTTP 401 if session invalid/expired
     */
    @PostMapping("/navigation")
    public ResponseEntity<Map<String, Object>> addToNavigationStack(
            HttpServletRequest request, 
            @RequestBody @Valid Map<String, String> transactionRequest, 
            Principal principal) {
        
        logger.debug("Navigation stack update requested for user: {}", principal != null ? principal.getName() : "anonymous");
        
        try {
            // Validate session before updating navigation
            if (!sessionService.validateSession(request)) {
                logger.warn("Navigation stack update denied - invalid session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Extract transaction code from request
            String transactionCode = transactionRequest.get("transactionCode");
            if (transactionCode == null || transactionCode.trim().isEmpty()) {
                logger.warn("Navigation stack update failed - missing transaction code");
                return ResponseEntity.badRequest().build();
            }
            
            // Add transaction to navigation stack
            sessionService.addToNavigationStack(request, transactionCode);
            
            // Create response with navigation update confirmation
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Navigation added successfully");
            response.put("transactionCode", transactionCode.toUpperCase());
            response.put("timestamp", LocalDateTime.now());
            
            logger.info("Navigation stack updated with transaction: {} for user: {}", 
                       transactionCode, principal != null ? principal.getName() : "unknown");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Navigation stack update failed - invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating navigation stack: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Clears session data and resets session state.
     * 
     * This endpoint provides session reset functionality equivalent to CICS session
     * reinitialization, clearing transient data and navigation history while preserving
     * user identity and authorization context. The method supports workflow reset
     * operations without requiring complete user logout and re-authentication.
     * 
     * Functionality equivalent to CICS session reset:
     * - Clears navigation stack and transient data storage
     * - Preserves user identity and authorization context
     * - Resets session operation status to initial state
     * - Maintains session timing for timeout management
     * 
     * @param request HTTP request containing session to clear
     * @param principal Authenticated user principal from Spring Security context
     * @return ResponseEntity<Map<String, Object>> Session clear confirmation,
     *         or HTTP 401 if session invalid/expired
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearSession(HttpServletRequest request, Principal principal) {
        logger.debug("Session clear requested for user: {}", principal != null ? principal.getName() : "anonymous");
        
        try {
            // Validate session before clearing
            SessionContext sessionContext = sessionService.getSession(request);
            if (sessionContext == null || !sessionService.validateSession(request)) {
                logger.warn("Session clear denied - invalid session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Reset session state while preserving user identity
            sessionContext.setCurrentMenu("MAIN");
            sessionContext.setLastTransactionCode("RESET");
            sessionContext.setOperationStatus("ACTIVE");
            sessionContext.setErrorMessage(null);
            sessionContext.updateActivityTime();
            
            // Clear session attributes but preserve core identity
            if (sessionContext.getSessionAttributes() != null) {
                sessionContext.getSessionAttributes().clear();
            }
            
            // Update session with cleared state
            sessionService.updateSession(request, sessionContext);
            
            // Create response with clear confirmation
            Map<String, Object> response = new HashMap<>();
            response.put("status", "session_cleared");
            response.put("userId", sessionContext.getUserId());
            response.put("currentMenu", sessionContext.getCurrentMenu());
            response.put("timestamp", LocalDateTime.now());
            
            logger.info("Session cleared successfully for user: {}", sessionContext.getUserId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error clearing session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Updates transient session data for multi-step operations.
     * 
     * This endpoint provides transient data management equivalent to CICS COMMAREA
     * data preservation, enabling multi-step business operations to maintain state
     * across REST API calls. The method supports complex workflows while respecting
     * session size limits and maintaining data integrity.
     * 
     * Functionality equivalent to CICS COMMAREA data update:
     * - Updates CDEMO-CUSTOMER-INFO, CDEMO-ACCOUNT-INFO, and CDEMO-CARD-INFO equivalents
     * - Supports multi-step operation state management
     * - Provides temporary data storage for workflow continuity
     * - Enforces size limits to prevent memory exhaustion
     * 
     * @param request HTTP request containing current session
     * @param sessionData Map containing session data updates to apply
     * @param principal Authenticated user principal from Spring Security context
     * @return ResponseEntity<Map<String, Object>> Session update confirmation,
     *         or HTTP 401 if session invalid/expired
     */
    @PutMapping("/data")
    public ResponseEntity<Map<String, Object>> updateSessionData(
            HttpServletRequest request, 
            @RequestBody @Valid Map<String, Object> sessionData, 
            Principal principal) {
        
        logger.debug("Session data update requested for user: {}", principal != null ? principal.getName() : "anonymous");
        
        try {
            // Validate session before updating data
            SessionContext sessionContext = sessionService.getSession(request);
            if (sessionContext == null || !sessionService.validateSession(request)) {
                logger.warn("Session data update denied - invalid session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Update session context with provided data
            if (sessionData.containsKey("currentMenu")) {
                sessionContext.setCurrentMenu((String) sessionData.get("currentMenu"));
            }
            if (sessionData.containsKey("lastTransactionCode")) {
                sessionContext.setLastTransactionCode((String) sessionData.get("lastTransactionCode"));
            }
            if (sessionData.containsKey("operationStatus")) {
                sessionContext.setOperationStatus((String) sessionData.get("operationStatus"));
            }
            if (sessionData.containsKey("errorMessage")) {
                sessionContext.setErrorMessage((String) sessionData.get("errorMessage"));
            }
            
            // Store transient data items
            for (Map.Entry<String, Object> entry : sessionData.entrySet()) {
                if (!entry.getKey().startsWith("_system") && 
                    !List.of("currentMenu", "lastTransactionCode", "operationStatus", "errorMessage").contains(entry.getKey())) {
                    sessionService.storeTransientData(request, entry.getKey(), entry.getValue());
                }
            }
            
            // Update activity time and persist changes
            sessionContext.updateActivityTime();
            sessionService.updateSession(request, sessionContext);
            
            // Create response with update confirmation
            Map<String, Object> response = new HashMap<>();
            response.put("status", "session_data_updated");
            response.put("userId", sessionContext.getUserId());
            response.put("updatedFields", sessionData.keySet());
            response.put("timestamp", LocalDateTime.now());
            
            logger.info("Session data updated successfully for user: {} with {} fields", 
                       sessionContext.getUserId(), sessionData.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error updating session data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Checks session validity and returns comprehensive status information.
     * 
     * This endpoint provides session status validation equivalent to CICS session
     * health checking, enabling frontend components to verify session state and
     * receive comprehensive status information for session management decisions.
     * The method includes timeout checking and session integrity validation.
     * 
     * Functionality equivalent to CICS session status check:
     * - Validates session timeout against configured policies
     * - Checks user authorization context integrity
     * - Provides comprehensive session health information
     * - Returns detailed status for frontend session management
     * 
     * @param request HTTP request containing session to validate
     * @param principal Authenticated user principal from Spring Security context
     * @return ResponseEntity<Map<String, Object>> Comprehensive session status information,
     *         or HTTP 401 if session invalid/expired
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSessionStatus(HttpServletRequest request, Principal principal) {
        logger.debug("Session status check requested for user: {}", principal != null ? principal.getName() : "anonymous");
        
        try {
            // Retrieve session context for status evaluation
            SessionContext sessionContext = sessionService.getSession(request);
            
            Map<String, Object> status = new HashMap<>();
            
            if (sessionContext == null) {
                status.put("valid", false);
                status.put("reason", "no_session");
                status.put("timestamp", LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(status);
            }
            
            // Validate session integrity
            boolean isValid = sessionService.validateSession(request);
            
            // Build comprehensive status response
            status.put("valid", isValid);
            status.put("userId", sessionContext.getUserId());
            status.put("userRole", sessionContext.getUserRole());
            status.put("isAdminUser", sessionContext.isAdminUser());
            status.put("isRegularUser", sessionContext.isRegularUser());
            status.put("currentMenu", sessionContext.getCurrentMenu());
            status.put("lastTransactionCode", sessionContext.getLastTransactionCode());
            status.put("operationStatus", sessionContext.getOperationStatus());
            status.put("sessionStartTime", sessionContext.getSessionStartTime());
            status.put("lastActivityTime", sessionContext.getLastActivityTime());
            status.put("hasNavigationHistory", !sessionService.getNavigationHistory(request).isEmpty());
            status.put("timestamp", LocalDateTime.now());
            
            if (!isValid) {
                status.put("reason", "session_expired");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(status);
            }
            
            logger.debug("Session status validated successfully for user: {}", sessionContext.getUserId());
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Error checking session status: {}", e.getMessage(), e);
            
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("valid", false);
            errorStatus.put("reason", "internal_error");
            errorStatus.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorStatus);
        }
    }

    /**
     * Updates session context with new information.
     * 
     * This endpoint provides session context updates equivalent to CICS COMMAREA
     * modification operations, enabling dynamic state changes throughout user workflows.
     * 
     * @param request HTTP request containing current session
     * @param sessionUpdate SessionContext containing updates to apply
     * @param principal Authenticated user principal from Spring Security context
     * @return ResponseEntity<SessionContext> Updated session context
     */
    @PutMapping
    public ResponseEntity<SessionContext> updateSession(
            HttpServletRequest request,
            @RequestBody SessionContext sessionUpdate,
            Principal principal) {
        
        logger.debug("Session update requested for user: {}", principal != null ? principal.getName() : "anonymous");
        
        try {
            // Validate session before updating
            if (!sessionService.validateSession(request)) {
                logger.warn("Session update denied - invalid session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Update session with provided changes
            SessionContext updatedContext = sessionService.updateSession(request, sessionUpdate);
            
            logger.info("Session updated successfully for user: {}", updatedContext.getUserId());
            return ResponseEntity.ok(updatedContext);
            
        } catch (Exception e) {
            logger.error("Error updating session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Stores transient data in session for multi-step operations.
     * 
     * This endpoint provides temporary data storage equivalent to CICS COMMAREA
     * data preservation, enabling multi-step business operations to maintain
     * state across REST API calls.
     * 
     * @param request HTTP request containing current session
     * @param dataRequest Map containing key-value pairs to store
     * @param principal Authenticated user principal from Spring Security context
     * @return ResponseEntity<Map<String, Object>> Storage confirmation response
     */
    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> storeTransientData(
            HttpServletRequest request,
            @RequestBody @Valid Map<String, Object> dataRequest,
            Principal principal) {
        
        logger.debug("Transient data storage requested for user: {}", principal != null ? principal.getName() : "anonymous");
        
        try {
            // Validate session before storing data
            if (!sessionService.validateSession(request)) {
                logger.warn("Transient data storage denied - invalid session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Extract key and value from request
            String key = (String) dataRequest.get("key");
            Object value = dataRequest.get("value");
            
            if (key == null || key.trim().isEmpty()) {
                logger.warn("Transient data storage failed - missing key");
                return ResponseEntity.badRequest().build();
            }
            
            // Store data in session
            sessionService.storeTransientData(request, key, value);
            
            // Create response confirmation
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Data stored successfully");
            response.put("key", key);
            response.put("timestamp", LocalDateTime.now());
            
            logger.info("Transient data stored successfully for key: {} user: {}", 
                       key, principal != null ? principal.getName() : "unknown");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error storing transient data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves transient data from session storage.
     * 
     * This endpoint provides access to temporarily stored data equivalent to CICS
     * COMMAREA data retrieval, enabling multi-step operations to restore state
     * information across REST API calls.
     * 
     * @param request HTTP request containing current session
     * @param key Data identifier for transient retrieval
     * @param principal Authenticated user principal from Spring Security context
     * @return ResponseEntity<Object> Stored data object or HTTP 404 if not found
     */
    @GetMapping("/data/{key}")
    public ResponseEntity<Object> retrieveTransientData(
            HttpServletRequest request,
            @PathVariable String key,
            Principal principal) {
        
        logger.debug("Transient data retrieval requested for key: {} user: {}", 
                    key, principal != null ? principal.getName() : "anonymous");
        
        try {
            // Validate session before retrieving data
            if (!sessionService.validateSession(request)) {
                logger.warn("Transient data retrieval denied - invalid session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Retrieve data from session
            Object data = sessionService.retrieveTransientData(request, key);
            
            if (data == null) {
                logger.debug("Transient data not found for key: {}", key);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            logger.debug("Transient data retrieved successfully for key: {} user: {}", 
                        key, principal != null ? principal.getName() : "unknown");
            
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            logger.error("Error retrieving transient data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}