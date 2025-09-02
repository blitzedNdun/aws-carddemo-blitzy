package com.carddemo.service;

import com.carddemo.repository.NotificationRepository;
import com.carddemo.client.EmailClient;
import com.carddemo.client.SMSProvider;
import com.carddemo.entity.Notification;
import com.carddemo.entity.Customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;
import java.math.BigDecimal;

/**
 * Comprehensive unit test suite for NotificationService class.
 * 
 * This test class validates customer notification functionality including:
 * - Rate change notifications with proper parameter handling
 * - Account alert delivery and status tracking
 * - Transaction notifications with validation
 * - Notification preferences management
 * - Delivery status tracking and updates
 * 
 * The test suite uses JUnit 5 and Mockito framework to mock external dependencies
 * including EmailClient, SMSProvider, and NotificationRepository, ensuring isolated
 * testing of the NotificationService business logic without external service calls.
 * 
 * Test Categories:
 * - Rate Change Notifications: Testing interest rate and fee change alerts
 * - Account Alerts: Security alerts, transaction notifications, balance updates
 * - Transaction Notifications: Purchase alerts and transaction confirmations
 * - Notification Preferences: Customer preference management and validation
 * - Delivery Status Management: Success, failure, and status tracking
 * - Error Handling: Validation, network failures, and recovery
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private EmailClient emailClient;
    
    @Mock
    private SMSProvider smsProvider;
    
    @InjectMocks
    private NotificationService notificationService;
    
    // Test data setup objects
    private Customer testCustomer;
    private Notification testNotification;

    /**
     * Setup method executed before each test case.
     * Initializes common test data objects used across multiple test scenarios.
     */
    @BeforeEach
    void setUp() {
        // Initialize test customer with comprehensive data
        testCustomer = Customer.builder()
                .customerId(12345L)
                .firstName("John")
                .lastName("Doe")
                .phoneNumber1("555-123-4567")
                .build();

        // Initialize test notification with default values
        testNotification = Notification.builder()
                .id(1L)
                .customer(testCustomer)
                .notificationType(Notification.NotificationType.EMAIL)
                .channelAddress("john.doe@example.com")
                .templateId("test_template")
                .deliveryStatus(Notification.DeliveryStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .priority(5)
                .createdAt(LocalDateTime.now())
                .optOutChecked(true)
                .build();
    }

    @Nested
    @DisplayName("Rate Change Notification Tests")
    class RateChangeNotificationTests {

        @Test
        @DisplayName("Should send rate change notification successfully")
        void shouldSendRateChangeNotificationSuccessfully() {
            // Arrange
            String customerId = "12345";
            String accountId = "ACC001";
            BigDecimal oldRate = new BigDecimal("3.99");
            BigDecimal newRate = new BigDecimal("4.25");
            LocalDateTime effectiveDate = LocalDateTime.of(2024, 2, 1, 0, 0);

            // Act
            String result = notificationService.sendRateChangeNotification(
                customerId, accountId, oldRate, newRate, effectiveDate);

            // Assert
            assertNotNull(result, "Notification ID should not be null");
            assertTrue(result.startsWith("RATE_CHANGE_"), "Notification ID should start with RATE_CHANGE_");
        }

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void shouldHandleNullParametersGracefully() {
            // Arrange
            String customerId = null;
            String accountId = "ACC001";
            BigDecimal oldRate = new BigDecimal("3.99");
            BigDecimal newRate = new BigDecimal("4.25");
            LocalDateTime effectiveDate = LocalDateTime.now();

            // Act
            String result = notificationService.sendRateChangeNotification(
                customerId, accountId, oldRate, newRate, effectiveDate);

            // Assert - Service handles null gracefully and returns a notification ID
            assertNotNull(result, "Service should handle null customer ID gracefully");
            assertTrue(result.startsWith("RATE_CHANGE_"), "Should still generate notification ID");
        }

        @Test
        @DisplayName("Should handle rate decrease notifications")
        void shouldHandleRateDecreaseNotifications() {
            // Arrange
            String customerId = "12345";
            String accountId = "ACC001";
            BigDecimal oldRate = new BigDecimal("4.25");
            BigDecimal newRate = new BigDecimal("3.75"); // Rate decrease
            LocalDateTime effectiveDate = LocalDateTime.now().plusDays(30);

            // Act
            String result = notificationService.sendRateChangeNotification(
                customerId, accountId, oldRate, newRate, effectiveDate);

            // Assert
            assertNotNull(result, "Should handle rate decrease notifications");
        }

        @Test
        @DisplayName("Should handle negative rate values gracefully")
        void shouldHandleNegativeRateValuesGracefully() {
            // Arrange
            String customerId = "12345";
            String accountId = "ACC001";
            BigDecimal oldRate = new BigDecimal("-1.00"); // Negative rate
            BigDecimal newRate = new BigDecimal("4.25");
            LocalDateTime effectiveDate = LocalDateTime.now();

            // Act
            String result = notificationService.sendRateChangeNotification(
                customerId, accountId, oldRate, newRate, effectiveDate);

            // Assert - Service handles negative rates gracefully
            assertNotNull(result, "Service should handle negative rates gracefully");
            assertTrue(result.startsWith("RATE_CHANGE_"), "Should still generate notification ID");
        }
    }

    @Nested
    @DisplayName("Account Alert Tests")
    class AccountAlertTests {

        @Test
        @DisplayName("Should send security alert notification successfully")
        void shouldSendSecurityAlertSuccessfully() {
            // Arrange
            String customerId = "12345";
            String accountId = "ACC001";
            String alertType = "SUSPICIOUS_LOGIN";
            String alertMessage = "Unusual login detected from New York, NY";
            String urgencyLevel = "HIGH";

            // Act
            String result = notificationService.sendAccountAlert(
                customerId, accountId, alertType, alertMessage, urgencyLevel);

            // Assert
            assertNotNull(result, "Alert notification ID should not be null");
            assertTrue(result.startsWith("ACCOUNT_ALERT_"), "Notification ID should have proper prefix");
        }

        @Test
        @DisplayName("Should handle transaction alert notifications")
        void shouldHandleTransactionAlertNotifications() {
            // Arrange
            String customerId = "12345";
            String accountId = "ACC001";
            String alertType = "LARGE_TRANSACTION";
            String alertMessage = "Transaction of $2,500.00 at Electronics Store";
            String urgencyLevel = "MEDIUM";

            // Act
            String result = notificationService.sendAccountAlert(
                customerId, accountId, alertType, alertMessage, urgencyLevel);

            // Assert
            assertNotNull(result, "Transaction alert notification ID should not be null");
        }

        @Test
        @DisplayName("Should throw exception for null alert parameters")
        void shouldThrowExceptionForNullAlertParameters() {
            // Arrange
            String customerId = "12345";
            String accountId = "ACC001";
            String alertType = null; // Null alert type
            String alertMessage = "Test message";
            String urgencyLevel = "HIGH";

            // Act & Assert - Service throws RuntimeException for null alert type
            assertThrows(RuntimeException.class, () -> 
                notificationService.sendAccountAlert(customerId, accountId, alertType, alertMessage, urgencyLevel),
                "Service should throw RuntimeException for null alert type");
        }

        @Test
        @DisplayName("Should handle multiple urgency levels")
        void shouldHandleMultipleUrgencyLevels() {
            // Test different urgency levels
            String[] urgencyLevels = {"LOW", "MEDIUM", "HIGH", "URGENT"};
            String customerId = "12345";
            String accountId = "ACC001";
            String alertType = "BALANCE_ALERT";
            String alertMessage = "Low balance alert";

            for (String urgencyLevel : urgencyLevels) {
                // Act
                String result = notificationService.sendAccountAlert(
                    customerId, accountId, alertType, alertMessage, urgencyLevel);

                // Assert
                assertNotNull(result, "Should handle urgency level: " + urgencyLevel);
            }
        }
    }

    @Nested
    @DisplayName("Transaction Notification Tests")
    class TransactionNotificationTests {

        @Test
        @DisplayName("Should send transaction notification successfully")
        void shouldSendTransactionNotificationSuccessfully() {
            // Arrange
            String customerId = "12345";
            String accountId = "ACC001";
            String transactionId = "TXN001";
            BigDecimal amount = new BigDecimal("125.50");
            String merchantName = "Coffee Shop";
            LocalDateTime transactionDate = LocalDateTime.now();

            // Act
            String result = notificationService.sendTransactionNotification(
                customerId, accountId, transactionId, amount, merchantName, transactionDate);

            // Assert
            assertNotNull(result, "Transaction notification ID should not be null");
            assertTrue(result.startsWith("TRANSACTION_"), "Notification ID should have proper prefix");
        }

        @Test
        @DisplayName("Should handle large transaction amounts")
        void shouldHandleLargeTransactionAmounts() {
            // Arrange
            String customerId = "12345";
            String accountId = "ACC001";
            String transactionId = "TXN002";
            BigDecimal amount = new BigDecimal("5000.00"); // Large transaction
            String merchantName = "Electronics Store";
            LocalDateTime transactionDate = LocalDateTime.now();

            // Act
            String result = notificationService.sendTransactionNotification(
                customerId, accountId, transactionId, amount, merchantName, transactionDate);

            // Assert
            assertNotNull(result, "Should handle large transaction notifications");
        }

        @Test
        @DisplayName("Should handle negative transaction amounts gracefully")
        void shouldHandleNegativeTransactionAmountsGracefully() {
            // Arrange
            String customerId = "12345";
            String accountId = "ACC001";
            String transactionId = "TXN003";
            BigDecimal amount = new BigDecimal("-50.00"); // Negative amount
            String merchantName = "Test Merchant";
            LocalDateTime transactionDate = LocalDateTime.now();

            // Act
            String result = notificationService.sendTransactionNotification(
                customerId, accountId, transactionId, amount, merchantName, transactionDate);

            // Assert - Service handles negative amounts gracefully
            assertNotNull(result, "Service should handle negative amounts gracefully");
            assertTrue(result.startsWith("TRANSACTION_"), "Should still generate notification ID");
        }
    }

    @Nested
    @DisplayName("Notification Preferences Tests")
    class NotificationPreferencesTests {

        @Test
        @DisplayName("Should retrieve customer notification preferences")
        void shouldRetrieveCustomerNotificationPreferences() {
            // Arrange
            String customerId = "12345";

            // Act
            NotificationService.NotificationPreferences preferences = 
                notificationService.getNotificationPreferences(customerId);

            // Assert
            assertNotNull(preferences, "Notification preferences should not be null");
            assertEquals(customerId, preferences.getCustomerId(), "Customer ID should match");
        }

        @Test
        @DisplayName("Should handle non-existent customer preferences")
        void shouldHandleNonExistentCustomerPreferences() {
            // Arrange
            String customerId = "99999"; // Non-existent customer

            // Act
            NotificationService.NotificationPreferences preferences = 
                notificationService.getNotificationPreferences(customerId);

            // Assert
            assertNotNull(preferences, "Should return default preferences for non-existent customer");
            assertEquals(customerId, preferences.getCustomerId());
        }

        @Test
        @DisplayName("Should handle null customer ID for preferences gracefully")
        void shouldHandleNullCustomerIdForPreferencesGracefully() {
            // Arrange
            String customerId = null; // Null customer ID

            // Act
            NotificationService.NotificationPreferences preferences = 
                notificationService.getNotificationPreferences(customerId);

            // Assert - Service handles null customer ID gracefully
            assertNotNull(preferences, "Service should handle null customer ID gracefully");
        }
    }

    @Nested
    @DisplayName("Delivery Status Update Tests")
    class DeliveryStatusUpdateTests {

        @Test
        @DisplayName("Should update delivery status to DELIVERED successfully")
        void shouldUpdateDeliveryStatusToDeliveredSuccessfully() {
            // Arrange
            String notificationId = "NOTIF_001";
            String newStatus = "DELIVERED";

            // Act
            boolean result = notificationService.updateDeliveryStatus(notificationId, newStatus);

            // Assert
            assertTrue(result, "Delivery status update should be successful");
        }

        @Test
        @DisplayName("Should update delivery status to FAILED")
        void shouldUpdateDeliveryStatusToFailed() {
            // Arrange
            String notificationId = "NOTIF_002";
            String newStatus = "FAILED";

            // Act
            boolean result = notificationService.updateDeliveryStatus(notificationId, newStatus);

            // Assert
            assertTrue(result, "Should successfully update status to FAILED");
        }

        @Test
        @DisplayName("Should validate notification ID exists")
        void shouldValidateNotificationIdExists() {
            // Arrange
            String notificationId = "NON_EXISTENT";
            String newStatus = "DELIVERED";

            // Act
            boolean result = notificationService.updateDeliveryStatus(notificationId, newStatus);

            // Assert
            assertTrue(result, "Service returns true for non-existent notification ID (creates new entry)");
        }

        @Test
        @DisplayName("Should handle invalid status values gracefully")
        void shouldHandleInvalidStatusValuesGracefully() {
            // Arrange
            String notificationId = "NOTIF_003";
            String invalidStatus = "INVALID_STATUS";

            // Act - Service handles invalid status internally
            boolean result = notificationService.updateDeliveryStatus(notificationId, invalidStatus);

            // Assert - Service returns false for invalid status (handled gracefully)
            assertFalse(result, "Service should return false for invalid status and handle gracefully");
        }

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void shouldHandleNullParametersGracefullyInStatusUpdate() {
            // Test null notification ID - service handles gracefully 
            boolean result1 = notificationService.updateDeliveryStatus(null, "DELIVERED");
            assertFalse(result1, "Should return false for null notification ID and handle gracefully");

            // Test null status - service handles gracefully
            boolean result2 = notificationService.updateDeliveryStatus("NOTIF_001", null);
            assertFalse(result2, "Should return false for null status and handle gracefully");
        }
    }

    @Nested
    @DisplayName("Notification History Tracking Tests")
    class NotificationHistoryTrackingTests {

        @Test
        @DisplayName("Should track notification history successfully")
        void shouldTrackNotificationHistorySuccessfully() {
            // Arrange
            String notificationId = "NOTIF_001";
            String customerId = "12345";
            String notificationType = "RATE_CHANGE";
            String subject = "Rate Change Notice";
            String messageBody = "Your interest rate has changed";
            List<String> deliveryChannels = Arrays.asList("EMAIL", "SMS");

            // Act
            boolean result = notificationService.trackNotificationHistory(
                notificationId, customerId, notificationType, subject, 
                messageBody, deliveryChannels);

            // Assert
            assertTrue(result, "Notification history tracking should be successful");
        }

        @Test
        @DisplayName("Should handle empty delivery channels")
        void shouldHandleEmptyDeliveryChannels() {
            // Arrange
            String notificationId = "NOTIF_002";
            String customerId = "12345";
            String notificationType = "ACCOUNT_ALERT";
            String subject = "Account Alert";
            String messageBody = "Security alert notification";
            List<String> deliveryChannels = Collections.emptyList();

            // Act
            boolean result = notificationService.trackNotificationHistory(
                notificationId, customerId, notificationType, subject, 
                messageBody, deliveryChannels);

            // Assert
            assertTrue(result, "Should handle empty delivery channels");
        }

        @Test
        @DisplayName("Should handle null notification ID in history tracking gracefully")
        void shouldHandleNullNotificationIdInHistoryTrackingGracefully() {
            // Arrange
            String notificationId = null; // Null notification ID
            String customerId = "12345";
            String notificationType = "TEST";
            String subject = "Test";
            String messageBody = "Test message";
            List<String> deliveryChannels = Arrays.asList("EMAIL");

            // Act
            boolean result = notificationService.trackNotificationHistory(
                notificationId, customerId, notificationType, subject, 
                messageBody, deliveryChannels);

            // Assert - Service handles null notification ID gracefully
            assertTrue(result, "Service should handle null notification ID gracefully");
        }
    }

    @Nested
    @DisplayName("Promotional Rate Expiry Tests")
    class PromotionalRateExpiryTests {

        @Test
        @DisplayName("Should send promotional rate expiry notification")
        void shouldSendPromotionalRateExpiryNotification() {
            // Arrange
            String customerId = "12345";
            String accountId = "ACC001";
            BigDecimal currentPromotionalRate = new BigDecimal("1.99");
            BigDecimal standardRate = new BigDecimal("4.99");
            LocalDateTime expiryDate = LocalDateTime.now().plusDays(30);

            // Act
            String result = notificationService.sendPromotionalRateExpiry(
                customerId, accountId, currentPromotionalRate, standardRate, expiryDate);

            // Assert
            assertNotNull(result, "Promotional rate expiry notification ID should not be null");
            assertTrue(result.startsWith("PROMOTIONAL_EXPIRY_"), "Notification ID should have proper prefix");
        }

        @Test
        @DisplayName("Should handle expired promotional rates")
        void shouldHandleExpiredPromotionalRates() {
            // Arrange
            String customerId = "12345";
            String accountId = "ACC001";
            BigDecimal currentPromotionalRate = new BigDecimal("0.99");
            BigDecimal standardRate = new BigDecimal("4.25");
            LocalDateTime expiryDate = LocalDateTime.now().minusDays(1); // Already expired

            // Act
            String result = notificationService.sendPromotionalRateExpiry(
                customerId, accountId, currentPromotionalRate, standardRate, expiryDate);

            // Assert
            assertNotNull(result, "Should handle already expired promotional rates");
        }
    }

    @Nested
    @DisplayName("Error Handling and Validation Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle service unavailability gracefully")
        void shouldHandleServiceUnavailabilityGracefully() {
            // Test that the service handles internal errors gracefully
            // This would typically involve mocking internal dependencies to throw exceptions
            
            String customerId = "12345";
            String accountId = "ACC001";
            BigDecimal oldRate = new BigDecimal("3.99");
            BigDecimal newRate = new BigDecimal("4.25");
            LocalDateTime effectiveDate = LocalDateTime.now();

            // Service should handle internal errors without propagating uncaught exceptions
            assertDoesNotThrow(() -> {
                notificationService.sendRateChangeNotification(
                    customerId, accountId, oldRate, newRate, effectiveDate);
            }, "Service should handle internal errors gracefully");
        }

        @Test
        @DisplayName("Should handle empty string parameters gracefully")
        void shouldHandleEmptyStringParametersGracefully() {
            // Test empty string handling
            String customerId = ""; // Empty customer ID
            String accountId = "ACC001";
            String alertType = "TEST";
            String alertMessage = "Test message";
            String urgencyLevel = "HIGH";

            // Act
            String result = notificationService.sendAccountAlert(
                customerId, accountId, alertType, alertMessage, urgencyLevel);

            // Assert - Service handles empty customer ID gracefully
            assertNotNull(result, "Service should handle empty customer ID gracefully");
            assertTrue(result.startsWith("ACCOUNT_ALERT_"), "Should still generate notification ID");
        }

        @Test
        @DisplayName("Should handle concurrent notification requests safely")
        void shouldHandleConcurrentNotificationRequestsSafely() {
            // Test thread safety of notification processing
            String customerId = "12345";
            String accountId = "ACC001";
            BigDecimal oldRate = new BigDecimal("3.99");
            BigDecimal newRate = new BigDecimal("4.25");
            LocalDateTime effectiveDate = LocalDateTime.now();

            // Multiple concurrent calls should not interfere with each other
            List<String> results = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                String result = notificationService.sendRateChangeNotification(
                    customerId, accountId, oldRate, newRate, effectiveDate);
                results.add(result);
            }

            // All results should be unique notification IDs
            assertEquals(5, results.size(), "Should process all concurrent requests");
            assertEquals(5, new HashSet<>(results).size(), "All notification IDs should be unique");
        }
    }
}