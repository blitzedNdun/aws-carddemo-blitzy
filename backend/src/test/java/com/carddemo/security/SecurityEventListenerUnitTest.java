package com.carddemo.security;

import com.carddemo.service.AuditService;
import com.carddemo.entity.AuditLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.session.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authorization.AuthorizationDecision;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SecurityEventListener focusing on event handling functionality
 * without requiring full Spring application context.
 */
public class SecurityEventListenerUnitTest {

    private SecurityEventListener securityEventListener;
    
    @Mock
    private AuditService auditService;
    
    @Mock
    private AbstractAuthenticationFailureEvent authFailureEvent;
    
    @Mock
    private AuthorizationDeniedEvent authzDeniedEvent;
    
    @Mock
    private SessionDestroyedEvent sessionDestroyedEvent;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private Session session;
    
    @Mock
    private AuthenticationException authException;
    
    @Mock
    private AuthorizationDecision authzDecision;
    
    @Mock
    private Supplier<Authentication> authSupplier;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // Create SecurityEventListener with SimpleMeterRegistry
        securityEventListener = new SecurityEventListener(new SimpleMeterRegistry());
        
        // Use reflection to inject mocked AuditService
        try {
            Field auditServiceField = SecurityEventListener.class.getDeclaredField("auditService");
            auditServiceField.setAccessible(true);
            auditServiceField.set(securityEventListener, auditService);
        } catch (Exception e) {
            fail("Failed to inject mocked AuditService: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test SecurityEventListener constructor with MeterRegistry")
    public void testConstructorWithMeterRegistry() {
        // Given & When
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        SecurityEventListener listener = new SecurityEventListener(meterRegistry);
        
        // Then
        assertNotNull(listener, "SecurityEventListener should be created successfully");
        assertTrue(meterRegistry.getMeters().size() > 0, "Metrics counters should be initialized");
    }

    @Test
    @DisplayName("Test authentication success event handling")
    public void testHandleAuthenticationSuccessEvent() {
        // Given
        String username = "testuser";
        String sourceIp = "192.168.1.100";
        String sessionId = "session123";
        Map<String, Object> additionalDetails = new HashMap<>();
        additionalDetails.put("userAgent", "Mozilla/5.0");
        additionalDetails.put("loginMethod", "form");
        
        // When
        assertDoesNotThrow(() -> {
            securityEventListener.handleAuthenticationSuccessEvent(username, sourceIp, sessionId, additionalDetails);
        }, "Authentication success event handling should not throw exceptions");
        
        // Then
        verify(auditService, times(1)).saveAuditLog(argThat(auditLog -> 
            auditLog.getUsername().equals(username) &&
            auditLog.getEventType().equals(AuditService.EVENT_TYPE_AUTHENTICATION) &&
            auditLog.getOutcome().equals(AuditService.OUTCOME_SUCCESS) &&
            auditLog.getSourceIp().equals(sourceIp) &&
            auditLog.getDetails().contains("Authentication successful") &&
            auditLog.getDetails().contains(sessionId)
        ));
    }

    @Test
    @DisplayName("Test authentication failure event handling")
    public void testHandleAuthenticationFailureEvent() {
        // Given
        String username = "testuser";
        String failureReason = "Bad credentials";
        
        when(authFailureEvent.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(username);
        when(authFailureEvent.getException()).thenReturn(authException);
        when(authException.getMessage()).thenReturn(failureReason);
        
        // When
        assertDoesNotThrow(() -> {
            securityEventListener.handleAuthenticationFailureEvent(authFailureEvent);
        }, "Authentication failure event handling should not throw exceptions");
        
        // Then
        verify(auditService, times(1)).saveAuditLog(argThat(auditLog -> 
            auditLog.getUsername().equals(username) &&
            auditLog.getEventType().equals(AuditService.EVENT_TYPE_AUTHENTICATION) &&
            auditLog.getOutcome().equals(AuditService.OUTCOME_FAILURE) &&
            auditLog.getDetails().contains("Authentication failed") &&
            auditLog.getDetails().contains(failureReason)
        ));
    }

    @Test
    @DisplayName("Test authorization denied event handling")
    public void testHandleAuthorizationDeniedEvent() {
        // Given
        String username = "testuser";
        String denialReason = "Access denied";
        Object requestObject = "/admin/users";
        
        when(authzDeniedEvent.getAuthentication()).thenReturn(authSupplier);
        when(authSupplier.get()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(username);
        when(authzDeniedEvent.getAuthorizationDecision()).thenReturn(authzDecision);
        when(authzDecision.toString()).thenReturn(denialReason);
        when(authzDeniedEvent.getObject()).thenReturn(requestObject);
        
        // When
        assertDoesNotThrow(() -> {
            securityEventListener.handleAuthorizationDeniedEvent(authzDeniedEvent);
        }, "Authorization denied event handling should not throw exceptions");
        
        // Then
        verify(auditService, times(1)).saveAuditLog(argThat(auditLog -> 
            auditLog.getUsername().equals(username) &&
            auditLog.getEventType().equals(AuditService.EVENT_TYPE_AUTHORIZATION) &&
            auditLog.getOutcome().equals(AuditService.OUTCOME_DENIED) &&
            auditLog.getDetails().contains("Authorization denied") &&
            auditLog.getDetails().contains(denialReason)
        ));
    }

    @Test
    @DisplayName("Test session created event handling")
    public void testHandleSessionCreatedEvent() {
        // Given
        String username = "testuser";
        String sessionId = "session123";
        String sourceIp = "192.168.1.100";
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("SEC-USR-ID", username);
        sessionAttributes.put("USER-TYPE", "ADMIN");
        
        // When
        assertDoesNotThrow(() -> {
            securityEventListener.handleSessionCreatedEvent(username, sessionId, sourceIp, sessionAttributes);
        }, "Session created event handling should not throw exceptions");
        
        // Then
        verify(auditService, times(1)).saveAuditLog(argThat(auditLog -> 
            auditLog.getUsername().equals(username) &&
            auditLog.getEventType().equals(AuditService.EVENT_TYPE_SESSION) &&
            auditLog.getOutcome().equals(AuditService.OUTCOME_SUCCESS) &&
            auditLog.getSourceIp().equals(sourceIp) &&
            auditLog.getDetails().contains("Session created") &&
            auditLog.getDetails().contains(sessionId)
        ));
    }

    @Test
    @DisplayName("Test session destroyed event handling")
    public void testHandleSessionDestroyedEvent() {
        // Given
        String sessionId = "session123";
        String username = "testuser";
        
        when(sessionDestroyedEvent.getSessionId()).thenReturn(sessionId);
        when(sessionDestroyedEvent.getSession()).thenReturn(session);
        when(session.getAttribute("SEC-USR-ID")).thenReturn(username);
        when(session.toString()).thenReturn("Session[" + sessionId + "]");
        
        // When
        assertDoesNotThrow(() -> {
            securityEventListener.handleSessionDestroyedEvent(sessionDestroyedEvent);
        }, "Session destroyed event handling should not throw exceptions");
        
        // Then
        verify(auditService, times(1)).saveAuditLog(argThat(auditLog -> 
            auditLog.getUsername().equals(username) &&
            auditLog.getEventType().equals(AuditService.EVENT_TYPE_SESSION) &&
            auditLog.getOutcome().equals(AuditService.OUTCOME_SUCCESS) &&
            auditLog.getDetails().contains("Session destroyed") &&
            auditLog.getDetails().contains(sessionId)
        ));
    }

    @Test
    @DisplayName("Test security alert generation")
    public void testGenerateSecurityAlert() {
        // Given
        String username = "testuser";
        String alertType = "AUTHENTICATION_FAILURE_THRESHOLD";
        String alertMessage = "User testuser has 5 failed authentication attempts";
        
        // When
        assertDoesNotThrow(() -> {
            securityEventListener.generateSecurityAlert(username, alertType, alertMessage);
        }, "Security alert generation should not throw exceptions");
        
        // Then
        verify(auditService, times(1)).saveAuditLog(argThat(auditLog -> 
            auditLog.getUsername().equals(username) &&
            auditLog.getEventType().equals(AuditService.EVENT_TYPE_SECURITY_VIOLATION) &&
            auditLog.getOutcome().equals(AuditService.OUTCOME_WARNING) &&
            auditLog.getDetails().contains("Security alert generated") &&
            auditLog.getDetails().contains(alertType) &&
            auditLog.getDetails().contains(alertMessage)
        ));
    }

    @Test
    @DisplayName("Test session destroyed with unknown user")
    public void testHandleSessionDestroyedEventWithUnknownUser() {
        // Given
        String sessionId = "session123";
        
        when(sessionDestroyedEvent.getSessionId()).thenReturn(sessionId);
        when(sessionDestroyedEvent.getSession()).thenReturn(session);
        when(session.getAttribute("SEC-USR-ID")).thenReturn(null); // No username found
        when(session.getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(null); // No Spring context
        when(session.toString()).thenReturn("Session[" + sessionId + "]");
        
        // When
        assertDoesNotThrow(() -> {
            securityEventListener.handleSessionDestroyedEvent(sessionDestroyedEvent);
        }, "Session destroyed event handling should not throw exceptions for unknown user");
        
        // Then
        verify(auditService, times(1)).saveAuditLog(argThat(auditLog -> 
            auditLog.getUsername().equals("UNKNOWN") &&
            auditLog.getEventType().equals(AuditService.EVENT_TYPE_SESSION) &&
            auditLog.getDetails().contains("Session destroyed")
        ));
    }

    @Test
    @DisplayName("Test metrics collection does not throw exceptions")
    public void testCollectSecurityMetrics() {
        // Given
        String username = "testuser";
        when(authFailureEvent.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(username);
        when(authFailureEvent.getException()).thenReturn(authException);
        
        // When & Then
        assertDoesNotThrow(() -> {
            securityEventListener.collectSecurityMetrics(authFailureEvent);
        }, "Security metrics collection should not throw exceptions");
    }
}