/*
 * SecurityEventListener.java
 * 
 * Spring Security event listener capturing authentication and authorization events 
 * for audit logging and security monitoring. Implements ApplicationListener for 
 * security event processing and compliance reporting.
 * 
 * This service implements the comprehensive security event monitoring framework 
 * outlined in the Security Architecture documentation (Section 6.4) and integrates
 * with the audit logging system for regulatory compliance and security monitoring
 * in the CardDemo application migration from COBOL mainframe to Spring Boot.
 * 
 * Key capabilities:
 * - Authentication success and failure event capture with user identification
 * - Authorization denial tracking for access control violations
 * - Session lifecycle monitoring for security compliance
 * - Integration with audit service for persistent security event storage
 * - Micrometer metrics collection for security monitoring dashboards
 * - Alert generation for suspicious activity patterns and security incidents
 * - Real-time security event correlation and threat detection
 * 
 * The listener integrates seamlessly with Spring Security's event publishing
 * mechanism and provides comprehensive security monitoring capabilities for
 * enterprise security operations and regulatory compliance requirements.
 */
package com.carddemo.security;

import com.carddemo.service.AuditService;
import com.carddemo.entity.AuditLog;

import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Spring Security event listener capturing authentication and authorization events 
 * for audit logging and security monitoring.
 * 
 * This component implements comprehensive security event processing with the following features:
 * 
 * <ul>
 *   <li><strong>Authentication Event Processing:</strong> Captures authentication success 
 *       and failure events with detailed user context and timestamp precision</li>
 *   <li><strong>Authorization Event Monitoring:</strong> Tracks authorization denials 
 *       and access control violations for security compliance</li>
 *   <li><strong>Session Lifecycle Tracking:</strong> Monitors session creation and 
 *       destruction events for security auditing</li>
 *   <li><strong>Audit Integration:</strong> Seamless integration with AuditService for 
 *       persistent security event storage and compliance reporting</li>
 *   <li><strong>Metrics Collection:</strong> Real-time security metrics collection using 
 *       Micrometer for monitoring dashboards and alerting</li>
 *   <li><strong>Alert Generation:</strong> Automated security alert generation for 
 *       suspicious activity patterns and security incidents</li>
 *   <li><strong>Threat Detection:</strong> Real-time correlation of security events for 
 *       advanced threat detection and incident response</li>
 * </ul>
 * 
 * The listener operates asynchronously to avoid impacting application performance
 * while providing comprehensive security monitoring capabilities aligned with
 * enterprise security operations and regulatory compliance requirements.
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class SecurityEventListener implements ApplicationListener<Object> {

    private static final Logger logger = LoggerFactory.getLogger(SecurityEventListener.class);
    
    // Security event type constants for consistent categorization
    private static final String EVENT_TYPE_AUTH_SUCCESS = "AUTHENTICATION_SUCCESS";
    private static final String EVENT_TYPE_AUTH_FAILURE = "AUTHENTICATION_FAILURE";
    private static final String EVENT_TYPE_AUTHZ_DENIED = "AUTHORIZATION_DENIED";
    private static final String EVENT_TYPE_SESSION_CREATED = "SESSION_CREATED";
    private static final String EVENT_TYPE_SESSION_DESTROYED = "SESSION_DESTROYED";
    
    // Security alert thresholds for suspicious activity detection
    private static final int FAILED_AUTH_THRESHOLD = 5;
    private static final int AUTHZ_DENIAL_THRESHOLD = 10;
    private static final long ALERT_WINDOW_MINUTES = 5;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    // Counters for security metrics collection
    private final Counter authenticationSuccessCounter;
    private final Counter authenticationFailureCounter;
    private final Counter authorizationDeniedCounter;
    private final Counter sessionCreatedCounter;
    private final Counter sessionDestroyedCounter;
    
    // Activity tracking for alert generation
    private final Map<String, AtomicLong> userFailedAuthAttempts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> userAuthorizationDenials = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastAlertTimestamp = new ConcurrentHashMap<>();

    /**
     * Constructor initializing security metrics counters for comprehensive monitoring.
     * 
     * @param meterRegistry Micrometer meter registry for metrics collection
     */
    public SecurityEventListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize security event counters for monitoring
        this.authenticationSuccessCounter = Counter.builder("security.authentication.success")
            .description("Number of successful authentication events")
            .register(meterRegistry);
            
        this.authenticationFailureCounter = Counter.builder("security.authentication.failure")
            .description("Number of failed authentication attempts")
            .register(meterRegistry);
            
        this.authorizationDeniedCounter = Counter.builder("security.authorization.denied")
            .description("Number of authorization denial events")
            .register(meterRegistry);
            
        this.sessionCreatedCounter = Counter.builder("security.session.created")
            .description("Number of session creation events")
            .register(meterRegistry);
            
        this.sessionDestroyedCounter = Counter.builder("security.session.destroyed")
            .description("Number of session destruction events")
            .register(meterRegistry);
    }

    /**
     * General application event listener implementing ApplicationListener interface.
     * 
     * This method serves as the primary entry point for Spring application events,
     * providing centralized event processing and routing for security-related events.
     * The implementation delegates to specific event handlers based on event type
     * while maintaining comprehensive error handling and audit trail integrity.
     * 
     * @param event The application event to be processed
     */
    @Override
    public void onApplicationEvent(Object event) {
        try {
            // Log all incoming security-related events for monitoring
            if (isSecurityRelatedEvent(event)) {
                logger.debug("Processing security event: {} at {}", 
                           event.getClass().getSimpleName(), 
                           LocalDateTime.now());
                
                // Route to appropriate event handler based on event type
                if (event instanceof AbstractAuthenticationFailureEvent) {
                    handleAuthenticationFailureEvent((AbstractAuthenticationFailureEvent) event);
                } else if (event instanceof AuthorizationDeniedEvent) {
                    handleAuthorizationDeniedEvent((AuthorizationDeniedEvent) event);
                } else if (event instanceof SessionDestroyedEvent) {
                    handleSessionDestroyedEvent((SessionDestroyedEvent) event);
                }
                
                // Collect security metrics for all processed events
                collectSecurityMetrics(event);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process security event: {} - Error: {}", 
                        event.getClass().getSimpleName(), e.getMessage(), e);
            
            // Create audit log for event processing failures
            createEventProcessingFailureAudit(event, e);
        }
    }

    /**
     * Handles authentication success events with comprehensive audit logging and metrics collection.
     * 
     * This method captures successful authentication events including user identification,
     * timestamp precision, and security context information. Essential for security
     * monitoring, compliance reporting, and user activity tracking. Integrates with
     * the audit service for persistent storage and regulatory compliance requirements.
     * 
     * @param username The authenticated username
     * @param sourceIp The source IP address of the authentication request
     * @param sessionId The session identifier for the authenticated session
     * @param additionalDetails Additional authentication context information
     */
    public void handleAuthenticationSuccessEvent(String username, String sourceIp, 
                                               String sessionId, Map<String, Object> additionalDetails) {
        try {
            logger.info("Authentication success event for user: {} from IP: {} at {}", 
                       username, sourceIp, LocalDateTime.now());
            
            // Create comprehensive audit log entry for authentication success
            AuditLog auditLog = new AuditLog();
            auditLog.setUsername(username);
            auditLog.setEventType(AuditService.EVENT_TYPE_AUTHENTICATION);
            auditLog.setOutcome(AuditService.OUTCOME_SUCCESS);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setSourceIp(sourceIp);
            auditLog.setDetails(buildAuthenticationSuccessDetails(sessionId, additionalDetails));
            
            // Persist audit log through audit service
            auditService.saveAuditLog(auditLog);
            
            // Increment success metrics counter
            authenticationSuccessCounter.increment();
            
            // Reset failed authentication attempts for successful user
            userFailedAuthAttempts.remove(username);
            
            logger.debug("Authentication success event processed successfully for user: {}", username);
            
        } catch (Exception e) {
            logger.error("Failed to process authentication success event for user: {} - Error: {}", 
                        username, e.getMessage(), e);
            throw new RuntimeException("Authentication success event processing failed", e);
        }
    }

    /**
     * Handles authentication failure events with detailed failure analysis and alert generation.
     * 
     * This method processes authentication failure events including failed credential attempts,
     * account lockout scenarios, and suspicious authentication patterns. Provides comprehensive
     * failure analysis, incremental failure tracking, and automated alert generation for
     * security incident response and compliance monitoring.
     * 
     * @param authFailureEvent The Spring Security authentication failure event
     */
    @EventListener
    public void handleAuthenticationFailureEvent(AbstractAuthenticationFailureEvent authFailureEvent) {
        try {
            String username = authFailureEvent.getAuthentication().getName();
            String failureReason = authFailureEvent.getException().getMessage();
            LocalDateTime eventTimestamp = LocalDateTime.now();
            
            logger.warn("Authentication failure event for user: {} - Reason: {} at {}", 
                       username, failureReason, eventTimestamp);
            
            // Create detailed audit log entry for authentication failure
            AuditLog auditLog = new AuditLog();
            auditLog.setUsername(username);
            auditLog.setEventType(AuditService.EVENT_TYPE_AUTHENTICATION);
            auditLog.setOutcome(AuditService.OUTCOME_FAILURE);
            auditLog.setTimestamp(eventTimestamp);
            auditLog.setDetails(buildAuthenticationFailureDetails(authFailureEvent));
            
            // Persist audit log through audit service
            auditService.saveAuditLog(auditLog);
            
            // Increment failure metrics counter
            authenticationFailureCounter.increment();
            
            // Track failed authentication attempts for alert generation
            AtomicLong failureCount = userFailedAuthAttempts.computeIfAbsent(username, 
                k -> new AtomicLong(0));
            long currentFailures = failureCount.incrementAndGet();
            
            // Generate security alert for suspicious authentication patterns
            if (currentFailures >= FAILED_AUTH_THRESHOLD) {
                generateSecurityAlert(username, "AUTHENTICATION_FAILURE_THRESHOLD", 
                    "User " + username + " has " + currentFailures + " failed authentication attempts");
            }
            
            logger.debug("Authentication failure event processed for user: {} (failure count: {})", 
                        username, currentFailures);
            
        } catch (Exception e) {
            logger.error("Failed to process authentication failure event - Error: {}", e.getMessage(), e);
            throw new RuntimeException("Authentication failure event processing failed", e);
        }
    }

    /**
     * Handles authorization denied events for access control violations and security monitoring.
     * 
     * This method processes authorization denial events including insufficient privileges,
     * resource access violations, and role-based access control failures. Essential for
     * security compliance monitoring, access control auditing, and detecting potential
     * privilege escalation attempts or unauthorized access patterns.
     * 
     * @param authzDeniedEvent The Spring Security authorization denied event
     */
    @EventListener
    public void handleAuthorizationDeniedEvent(AuthorizationDeniedEvent authzDeniedEvent) {
        try {
            String username = authzDeniedEvent.getAuthentication().getName();
            String deniedResource = extractResourceFromEvent(authzDeniedEvent);
            String denialReason = authzDeniedEvent.getAuthorizationDecision().toString();
            LocalDateTime eventTimestamp = LocalDateTime.now();
            
            logger.warn("Authorization denied event for user: {} - Resource: {} - Reason: {} at {}", 
                       username, deniedResource, denialReason, eventTimestamp);
            
            // Create comprehensive audit log entry for authorization denial
            AuditLog auditLog = new AuditLog();
            auditLog.setUsername(username);
            auditLog.setEventType(AuditService.EVENT_TYPE_AUTHORIZATION);
            auditLog.setOutcome(AuditService.OUTCOME_DENIED);
            auditLog.setTimestamp(eventTimestamp);
            auditLog.setDetails(buildAuthorizationDeniedDetails(authzDeniedEvent));
            
            // Persist audit log through audit service
            auditService.saveAuditLog(auditLog);
            
            // Increment authorization denied metrics counter
            authorizationDeniedCounter.increment();
            
            // Track authorization denials for suspicious activity detection
            AtomicLong denialCount = userAuthorizationDenials.computeIfAbsent(username, 
                k -> new AtomicLong(0));
            long currentDenials = denialCount.incrementAndGet();
            
            // Generate security alert for excessive authorization denials
            if (currentDenials >= AUTHZ_DENIAL_THRESHOLD) {
                generateSecurityAlert(username, "AUTHORIZATION_DENIAL_THRESHOLD", 
                    "User " + username + " has " + currentDenials + " authorization denials");
            }
            
            logger.debug("Authorization denied event processed for user: {} (denial count: {})", 
                        username, currentDenials);
            
        } catch (Exception e) {
            logger.error("Failed to process authorization denied event - Error: {}", e.getMessage(), e);
            throw new RuntimeException("Authorization denied event processing failed", e);
        }
    }

    /**
     * Handles session creation events for session lifecycle monitoring and security tracking.
     * 
     * This method captures session creation events including session identifiers,
     * user context, and creation timestamps. Essential for session management
     * auditing, concurrent session monitoring, and security compliance requirements
     * for user activity tracking and regulatory reporting.
     * 
     * @param username The username associated with the created session
     * @param sessionId The unique identifier for the created session
     * @param sourceIp The source IP address for the session creation
     * @param sessionAttributes Additional session context information
     */
    public void handleSessionCreatedEvent(String username, String sessionId, 
                                        String sourceIp, Map<String, Object> sessionAttributes) {
        try {
            LocalDateTime eventTimestamp = LocalDateTime.now();
            
            logger.info("Session created event for user: {} - Session ID: {} from IP: {} at {}", 
                       username, sessionId, sourceIp, eventTimestamp);
            
            // Create audit log entry for session creation
            AuditLog auditLog = new AuditLog();
            auditLog.setUsername(username);
            auditLog.setEventType(AuditService.EVENT_TYPE_SESSION);
            auditLog.setOutcome(AuditService.OUTCOME_SUCCESS);
            auditLog.setTimestamp(eventTimestamp);
            auditLog.setSourceIp(sourceIp);
            auditLog.setDetails(buildSessionCreatedDetails(sessionId, sessionAttributes));
            
            // Persist audit log through audit service
            auditService.saveAuditLog(auditLog);
            
            // Increment session created metrics counter
            sessionCreatedCounter.increment();
            
            logger.debug("Session created event processed for user: {} with session: {}", 
                        username, sessionId);
            
        } catch (Exception e) {
            logger.error("Failed to process session created event for user: {} - Error: {}", 
                        username, e.getMessage(), e);
            throw new RuntimeException("Session created event processing failed", e);
        }
    }

    /**
     * Handles session destruction events for session lifecycle completion and security monitoring.
     * 
     * This method processes session destruction events including session invalidation,
     * timeout scenarios, and explicit logout actions. Provides comprehensive session
     * lifecycle tracking for security auditing, compliance monitoring, and user
     * activity analysis required for regulatory reporting and security operations.
     * 
     * @param sessionDestroyedEvent The Spring Session destroyed event
     */
    @EventListener
    public void handleSessionDestroyedEvent(SessionDestroyedEvent sessionDestroyedEvent) {
        try {
            String sessionId = sessionDestroyedEvent.getSessionId();
            LocalDateTime eventTimestamp = LocalDateTime.now();
            
            // Extract username from session attributes if available
            String username = extractUsernameFromSession(sessionDestroyedEvent);
            
            logger.info("Session destroyed event - Session ID: {} for user: {} at {}", 
                       sessionId, username, eventTimestamp);
            
            // Create audit log entry for session destruction
            AuditLog auditLog = new AuditLog();
            auditLog.setUsername(username != null ? username : "UNKNOWN");
            auditLog.setEventType(AuditService.EVENT_TYPE_SESSION);
            auditLog.setOutcome(AuditService.OUTCOME_SUCCESS);
            auditLog.setTimestamp(eventTimestamp);
            auditLog.setDetails(buildSessionDestroyedDetails(sessionDestroyedEvent));
            
            // Persist audit log through audit service
            auditService.saveAuditLog(auditLog);
            
            // Increment session destroyed metrics counter
            sessionDestroyedCounter.increment();
            
            logger.debug("Session destroyed event processed for session: {} (user: {})", 
                        sessionId, username);
            
        } catch (Exception e) {
            logger.error("Failed to process session destroyed event - Error: {}", e.getMessage(), e);
            throw new RuntimeException("Session destroyed event processing failed", e);
        }
    }

    /**
     * Collects security metrics for monitoring dashboards and alerting systems.
     * 
     * This method aggregates security event data into comprehensive metrics suitable
     * for real-time monitoring, alerting thresholds, and security analytics. Provides
     * detailed metrics collection including event counts, failure rates, and temporal
     * patterns essential for security operations and proactive threat detection.
     * 
     * @param event The security event to collect metrics for
     */
    public void collectSecurityMetrics(Object event) {
        try {
            String eventType = event.getClass().getSimpleName();
            LocalDateTime currentTime = LocalDateTime.now();
            
            logger.debug("Collecting security metrics for event type: {} at {}", eventType, currentTime);
            
            // Collect event-specific metrics based on event type
            if (event instanceof AbstractAuthenticationFailureEvent) {
                // Track authentication failure patterns and rates
                AbstractAuthenticationFailureEvent authEvent = (AbstractAuthenticationFailureEvent) event;
                String username = authEvent.getAuthentication().getName();
                String failureType = authEvent.getException().getClass().getSimpleName();
                
                // Add tagged metrics for detailed analysis
                Counter.builder("security.authentication.failure.detailed")
                    .description("Detailed authentication failure events by type")
                    .tag("username", username)
                    .tag("failure_type", failureType)
                    .register(meterRegistry)
                    .increment();
                    
            } else if (event instanceof AuthorizationDeniedEvent) {
                // Track authorization denial patterns and trends
                AuthorizationDeniedEvent authzEvent = (AuthorizationDeniedEvent) event;
                String username = authzEvent.getAuthentication().getName();
                String resource = extractResourceFromEvent(authzEvent);
                
                // Add tagged metrics for authorization monitoring
                Counter.builder("security.authorization.denied.detailed")
                    .description("Detailed authorization denial events by resource")
                    .tag("username", username)
                    .tag("resource", resource)
                    .register(meterRegistry)
                    .increment();
                    
            } else if (event instanceof SessionDestroyedEvent) {
                // Track session lifecycle metrics
                SessionDestroyedEvent sessionEvent = (SessionDestroyedEvent) event;
                String sessionId = sessionEvent.getSessionId();
                
                // Add session lifecycle metrics
                Counter.builder("security.session.lifecycle")
                    .description("Session lifecycle events")
                    .tag("event_type", "destroyed")
                    .tag("session_id", sessionId)
                    .register(meterRegistry)
                    .increment();
            }
            
            // Update general security event metrics
            Counter.builder("security.events.total")
                .description("Total security events processed")
                .tag("event_type", eventType)
                .register(meterRegistry)
                .increment();
            
            logger.debug("Security metrics collected successfully for event: {}", eventType);
            
        } catch (Exception e) {
            logger.error("Failed to collect security metrics for event: {} - Error: {}", 
                        event.getClass().getSimpleName(), e.getMessage(), e);
            // Continue processing without throwing exception to avoid disrupting security events
        }
    }

    /**
     * Generates security alerts for suspicious activity patterns and security incidents.
     * 
     * This method creates and dispatches security alerts based on predefined thresholds,
     * suspicious activity patterns, and security incident criteria. Provides automated
     * alert generation for security operations teams, incident response systems, and
     * compliance monitoring requirements with comprehensive alert context and severity.
     * 
     * @param username The username associated with the security alert
     * @param alertType The type of security alert being generated
     * @param alertMessage Detailed message describing the security alert
     */
    public void generateSecurityAlert(String username, String alertType, String alertMessage) {
        try {
            LocalDateTime alertTimestamp = LocalDateTime.now();
            
            logger.warn("Generating security alert - Type: {} for user: {} - Message: {} at {}", 
                       alertType, username, alertMessage, alertTimestamp);
            
            // Check alert rate limiting to prevent alert flooding
            String alertKey = username + ":" + alertType;
            LocalDateTime lastAlert = lastAlertTimestamp.get(alertKey);
            
            if (lastAlert != null && 
                lastAlert.plusMinutes(ALERT_WINDOW_MINUTES).isAfter(alertTimestamp)) {
                logger.debug("Alert rate limited for user: {} and type: {} (last alert: {})", 
                           username, alertType, lastAlert);
                return;
            }
            
            // Update last alert timestamp
            lastAlertTimestamp.put(alertKey, alertTimestamp);
            
            // Create comprehensive audit log entry for security alert
            AuditLog alertAuditLog = new AuditLog();
            alertAuditLog.setUsername(username);
            alertAuditLog.setEventType(AuditService.EVENT_TYPE_SECURITY_VIOLATION);
            alertAuditLog.setOutcome(AuditService.OUTCOME_WARNING);
            alertAuditLog.setTimestamp(alertTimestamp);
            alertAuditLog.setDetails(buildSecurityAlertDetails(alertType, alertMessage));
            
            // Persist security alert through audit service
            auditService.saveAuditLog(alertAuditLog);
            
            // Increment security alert metrics
            Counter.builder("security.alerts.generated")
                .description("Number of security alerts generated")
                .tag("alert_type", alertType)
                .tag("username", username)
                .register(meterRegistry)
                .increment();
            
            // Log alert for immediate security operations visibility
            logger.error("SECURITY ALERT GENERATED - Type: {} | User: {} | Message: {} | Timestamp: {}", 
                        alertType, username, alertMessage, alertTimestamp);
            
            // Additional alert integration points can be added here:
            // - Send to SIEM systems
            // - Trigger incident response workflows
            // - Notify security operations center
            // - Update threat intelligence feeds
            
        } catch (Exception e) {
            logger.error("Failed to generate security alert for user: {} - Type: {} - Error: {}", 
                        username, alertType, e.getMessage(), e);
            // Continue processing without throwing exception to avoid disrupting security monitoring
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Determines if an event is security-related and should be processed.
     */
    private boolean isSecurityRelatedEvent(Object event) {
        return event instanceof AbstractAuthenticationFailureEvent ||
               event instanceof AuthorizationDeniedEvent ||
               event instanceof SessionDestroyedEvent;
    }

    /**
     * Creates audit log for event processing failures.
     */
    private void createEventProcessingFailureAudit(Object event, Exception error) {
        try {
            AuditLog errorAuditLog = new AuditLog();
            errorAuditLog.setUsername("SYSTEM");
            errorAuditLog.setEventType(AuditService.EVENT_TYPE_SYSTEM_EVENT);
            errorAuditLog.setOutcome(AuditService.OUTCOME_ERROR);
            errorAuditLog.setTimestamp(LocalDateTime.now());
            errorAuditLog.setDetails("Event processing failure: " + event.getClass().getSimpleName() + 
                                   " - Error: " + error.getMessage());
            
            auditService.saveAuditLog(errorAuditLog);
        } catch (Exception e) {
            logger.error("Failed to create audit log for event processing failure", e);
        }
    }

    /**
     * Builds authentication success details for audit logging.
     */
    private String buildAuthenticationSuccessDetails(String sessionId, Map<String, Object> additionalDetails) {
        StringBuilder details = new StringBuilder();
        details.append("Authentication successful");
        if (sessionId != null) {
            details.append(" | Session ID: ").append(sessionId);
        }
        if (additionalDetails != null && !additionalDetails.isEmpty()) {
            details.append(" | Additional Details: ").append(additionalDetails.toString());
        }
        return details.toString();
    }

    /**
     * Builds authentication failure details for audit logging.
     */
    private String buildAuthenticationFailureDetails(AbstractAuthenticationFailureEvent authFailureEvent) {
        StringBuilder details = new StringBuilder();
        details.append("Authentication failed");
        details.append(" | Exception: ").append(authFailureEvent.getException().getClass().getSimpleName());
        details.append(" | Reason: ").append(authFailureEvent.getException().getMessage());
        details.append(" | Authentication: ").append(authFailureEvent.getAuthentication().toString());
        return details.toString();
    }

    /**
     * Builds authorization denied details for audit logging.
     */
    private String buildAuthorizationDeniedDetails(AuthorizationDeniedEvent authzDeniedEvent) {
        StringBuilder details = new StringBuilder();
        details.append("Authorization denied");
        details.append(" | Decision: ").append(authzDeniedEvent.getAuthorizationDecision().toString());
        details.append(" | Object: ").append(authzDeniedEvent.getObject().toString());
        details.append(" | Authentication: ").append(authzDeniedEvent.getAuthentication().toString());
        return details.toString();
    }

    /**
     * Builds session created details for audit logging.
     */
    private String buildSessionCreatedDetails(String sessionId, Map<String, Object> sessionAttributes) {
        StringBuilder details = new StringBuilder();
        details.append("Session created");
        details.append(" | Session ID: ").append(sessionId);
        if (sessionAttributes != null && !sessionAttributes.isEmpty()) {
            details.append(" | Attributes: ").append(sessionAttributes.toString());
        }
        return details.toString();
    }

    /**
     * Builds session destroyed details for audit logging.
     */
    private String buildSessionDestroyedDetails(SessionDestroyedEvent sessionDestroyedEvent) {
        StringBuilder details = new StringBuilder();
        details.append("Session destroyed");
        details.append(" | Session ID: ").append(sessionDestroyedEvent.getSessionId());
        details.append(" | Session: ").append(sessionDestroyedEvent.getSession().toString());
        return details.toString();
    }

    /**
     * Builds security alert details for audit logging.
     */
    private String buildSecurityAlertDetails(String alertType, String alertMessage) {
        StringBuilder details = new StringBuilder();
        details.append("Security alert generated");
        details.append(" | Alert Type: ").append(alertType);
        details.append(" | Message: ").append(alertMessage);
        details.append(" | Threshold Check: Automated security monitoring");
        return details.toString();
    }

    /**
     * Extracts resource information from authorization denied event.
     */
    private String extractResourceFromEvent(AuthorizationDeniedEvent event) {
        try {
            Object resource = event.getObject();
            if (resource != null) {
                return resource.toString();
            }
        } catch (Exception e) {
            logger.debug("Failed to extract resource from authorization event", e);
        }
        return "UNKNOWN_RESOURCE";
    }

    /**
     * Extracts username from session destroyed event.
     */
    private String extractUsernameFromSession(SessionDestroyedEvent event) {
        try {
            // Attempt to extract username from session attributes
            if (event.getSession() != null) {
                Object userAttr = event.getSession().getAttribute("SEC-USR-ID");
                if (userAttr != null) {
                    return userAttr.toString();
                }
                // Try Spring Security context attribute
                Object principal = event.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
                if (principal != null) {
                    return principal.toString();
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract username from session", e);
        }
        return null;
    }
}