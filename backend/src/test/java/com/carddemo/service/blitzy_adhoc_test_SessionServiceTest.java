/*
 * Ad-hoc test for SessionService without Testcontainers
 * This test validates core session management functionality using mocking instead of Redis containers
 */

package com.carddemo.service;

import com.carddemo.dto.SessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class blitzy_adhoc_test_SessionServiceTest {

    @InjectMocks
    private SessionService sessionService;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpSession mockHttpSession;

    private static final String TEST_USER_ID = "testuser123";
    private static final String TEST_ADMIN_ROLE = "A";
    private static final String TEST_MENU_CONTEXT = "MAIN_MENU";

    @BeforeEach
    public void setUp() {
        // Setup mock HTTP session behavior - lenient to avoid unnecessary stubbing errors
        lenient().when(mockRequest.getSession(true)).thenReturn(mockHttpSession);
        lenient().when(mockRequest.getSession(false)).thenReturn(mockHttpSession);
        lenient().when(mockRequest.getSession()).thenReturn(mockHttpSession);
        
        // Mock session timeout setting
        lenient().doNothing().when(mockHttpSession).setMaxInactiveInterval(anyInt());
        
        // Mock session attribute operations
        lenient().doNothing().when(mockHttpSession).setAttribute(anyString(), any());
        lenient().when(mockHttpSession.getAttribute(anyString())).thenReturn(null);
        
        // Mock session ID
        lenient().when(mockHttpSession.getId()).thenReturn("test-session-" + System.currentTimeMillis());
    }

    @Test
    @DisplayName("Should create session successfully with valid user data")
    void testCreateSession_ShouldSucceed() {
        // Act
        SessionContext result = sessionService.createSession(mockRequest, TEST_USER_ID, TEST_ADMIN_ROLE, TEST_MENU_CONTEXT);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(TEST_USER_ID.toUpperCase());
        assertThat(result.getUserRole()).isEqualTo(TEST_ADMIN_ROLE);
        assertThat(result.getCurrentMenu()).isEqualTo(TEST_MENU_CONTEXT);
        
        // Verify session timeout was configured (30 minutes = 1800 seconds)
        verify(mockHttpSession).setMaxInactiveInterval(1800);
        
        // Verify session attributes were set
        verify(mockHttpSession, atLeastOnce()).setAttribute(anyString(), any());
    }

    @Test
    @DisplayName("Should handle session creation performance within 200ms SLA")
    void testSessionCreationPerformance_ShouldMeetSLA() {
        // Arrange
        long startTime = System.currentTimeMillis();
        
        // Act
        SessionContext result = sessionService.createSession(mockRequest, TEST_USER_ID, TEST_ADMIN_ROLE, TEST_MENU_CONTEXT);
        
        // Assert
        long executionTime = System.currentTimeMillis() - startTime;
        assertThat(result).isNotNull();
        assertThat(executionTime).isLessThan(200L); // Must be under 200ms SLA
    }

    @Test
    @DisplayName("Should return null for non-existing session")
    void testGetSession_ShouldReturnNullForNonExisting() {
        // Arrange - mockHttpSession.getAttribute returns null by default
        
        // Act
        SessionContext result = sessionService.getSession(mockRequest);
        
        // Assert - Should return null when no session context exists (default mock behavior)
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should validate session as false for missing context")
    void testValidateSession_ShouldReturnFalseForMissingContext() {
        // Arrange - mockHttpSession.getAttribute returns null by default
        
        // Act
        boolean result = sessionService.validateSession(mockRequest);
        
        // Assert - Should return false when no session context exists
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should clear session successfully")
    void testClearSession_ShouldRemoveSessionData() {
        // Act
        sessionService.clearSession(mockRequest);
        
        // Assert - Verify session invalidation was called
        verify(mockHttpSession).invalidate();
    }

    @Test
    @DisplayName("Should add to navigation stack")
    void testAddToNavigationStack_ShouldTrackNavigation() {
        // Arrange
        String transactionCode = "COTRN01";
        
        // Act
        sessionService.addToNavigationStack(mockRequest, transactionCode);
        
        // Assert - Verify navigation stack is updated
        verify(mockHttpSession, atLeastOnce()).setAttribute(anyString(), any());
    }

    @Test
    @DisplayName("Should store and retrieve transient data")
    void testTransientData_ShouldHandleTemporaryStorage() {
        // Arrange
        String testKey = "testKey";
        String testValue = "testValue";
        
        // Mock the transient data map structure
        java.util.Map<String, Object> transientDataMap = new java.util.HashMap<>();
        when(mockHttpSession.getAttribute("CARDDEMO_TRANSIENT_DATA")).thenReturn(transientDataMap);
        
        // Act
        sessionService.storeTransientData(mockRequest, testKey, testValue);
        Object retrieved = sessionService.retrieveTransientData(mockRequest, testKey);
        
        // Assert - Verify the map was updated with our test data
        verify(mockHttpSession, atLeastOnce()).setAttribute(eq("CARDDEMO_TRANSIENT_DATA"), any());
        assertThat(transientDataMap.get(testKey)).isEqualTo(testValue);
    }
}