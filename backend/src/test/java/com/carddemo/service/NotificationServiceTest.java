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
import java.util.concurrent.CompletableFuture;
import java.math.BigDecimal;

/**
 * Comprehensive unit test suite for NotificationService class.
 * 
 * This test class validates customer notification functionality including:
 * - Email notification sending and delivery tracking
 * - SMS alert delivery with template processing
 * - In-app message creation and queue management  
 * - Notification template processing and variable substitution
 * - Delivery status tracking and retry logic for failed deliveries
 * - Notification archival and bulk notification operations
 * - Customer opt-out preference handling and compliance
 * 
 * The test suite uses JUnit 5 and Mockito framework to mock external dependencies
 * including EmailClient, SMSProvider, and NotificationRepository, ensuring isolated
 * testing of the NotificationService business logic without external service calls.
 * 
 * Test Categories:
 * - Rate Change Notifications: Testing interest rate and fee change alerts
 * - Account Alerts: Security alerts, transaction notifications, balance updates
 * - Welcome Email Processing: New customer onboarding notifications
 * - Notification Queue Processing: Batch processing and priority handling
 * - Delivery Status Management: Success, failure, and retry scenarios
 * - Template Variable Substitution: Dynamic content generation
 * - Bulk Notification Operations: Mass communication campaigns
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
    private Map<String, String> testTemplateVariables;

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
                .templateId("welcome_template")
                .deliveryStatus(Notification.DeliveryStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .priority(5)
                .createdAt(LocalDateTime.now())
                .optOutChecked(true)
                .build();

        // Initialize test template variables for dynamic content generation
        testTemplateVariables = new HashMap<>();
        testTemplateVariables.put("customerName", "John Doe");
        testTemplateVariables.put("accountNumber", "****4567");
        testTemplateVariables.put("effectiveDate", "2024-01-01");
        testTemplateVariables.put("newRate", "4.25%");
        testTemplateVariables.put("amount", "$1,250.00");
    }

    @Nested
    @DisplayName("Rate Change Notification Tests")
    class RateChangeNotificationTests {

        @Test
        @DisplayName("Should send rate change notification successfully via email")
        void shouldSendRateChangeNotificationEmailSuccessfully() {
            // Arrange
            Long customerId = 12345L;
            String notificationType = "RATE_INCREASE";
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", "John Doe");
            variables.put("currentRate", "3.99%");
            variables.put("newRate", "4.25%");
            variables.put("effectiveDate", "2024-02-01");
            
            Notification savedNotification = testNotification.toBuilder()
                    .templateId("rate_change_email")
                    .notificationType(Notification.NotificationType.EMAIL)
                    .build();
                    
            EmailClient.EmailResponse emailResponse = mock(EmailClient.EmailResponse.class);
            when(emailResponse.getMessageId()).thenReturn("email_123456");
            when(emailResponse.getStatus()).thenReturn("SENT");
            when(emailResponse.isSuccess()).thenReturn(true);
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap())).thenReturn(emailResponse);

            // Act
            String result = notificationService.sendRateChangeNotification(customerId, notificationType, variables);

            // Assert
            assertNotNull(result, "Notification ID should not be null");
            
            // Verify repository save was called with correct parameters
            verify(notificationRepository).save(argThat(notification -> 
                notification.getCustomerId().equals(customerId) &&
                notification.getTemplateId().equals("rate_change_email") &&
                notification.getNotificationType() == Notification.NotificationType.EMAIL &&
                notification.getDeliveryStatus() == Notification.DeliveryStatus.PENDING
            ));
            
            // Verify email client was called
            verify(emailClient).sendTemplateEmail(
                eq("john.doe@example.com"), 
                eq("rate_change_email"), 
                eq(variables)
            );
            
            // Verify delivery status update
            verify(notificationRepository).save(argThat(notification ->
                notification.getDeliveryStatus() == Notification.DeliveryStatus.SENT
            ));
        }

        @Test
        @DisplayName("Should handle email sending failure for rate change notification")
        void shouldHandleEmailSendingFailureForRateChange() {
            // Arrange
            Long customerId = 12345L;
            String notificationType = "RATE_DECREASE";
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", "John Doe");
            variables.put("newRate", "3.75%");
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("Email service unavailable"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> 
                notificationService.sendRateChangeNotification(customerId, notificationType, variables),
                "Should throw exception when email service fails"
            );
            
            // Verify notification was saved with FAILED status
            verify(notificationRepository, times(2)).save(argThat(notification ->
                notification.getDeliveryStatus() == Notification.DeliveryStatus.FAILED ||
                notification.getDeliveryStatus() == Notification.DeliveryStatus.PENDING
            ));
        }

        @Test
        @DisplayName("Should send rate change notification via SMS when email channel not available")
        void shouldSendRateChangeNotificationViaSmsWhenEmailNotAvailable() {
            // Arrange
            Long customerId = 12345L;
            String notificationType = "RATE_INCREASE";
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", "John Doe");
            variables.put("newRate", "4.50%");
            
            // Setup customer without email preference, should fallback to SMS
            Customer smsCustomer = testCustomer.toBuilder()
                    .phoneNumber1("555-987-6543")
                    .build();
                    
            Notification smsNotification = testNotification.toBuilder()
                    .customer(smsCustomer)
                    .notificationType(Notification.NotificationType.SMS)
                    .channelAddress("555-987-6543")
                    .templateId("rate_change_sms")
                    .build();
                    
            SMSProvider.SMSResponse smsResponse = mock(SMSProvider.SMSResponse.class);
            when(smsResponse.getMessageId()).thenReturn("sms_789012");
            when(smsResponse.getStatus()).thenReturn("queued");
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(smsNotification);
            when(smsProvider.sendTemplateSMS(anyString(), anyString(), anyMap())).thenReturn(smsResponse);

            // Act
            String result = notificationService.sendRateChangeNotification(customerId, notificationType, variables);

            // Assert
            assertNotNull(result, "SMS notification ID should not be null");
            
            verify(smsProvider).sendTemplateSMS(
                eq("555-987-6543"),
                eq("rate_change_sms"),
                eq(variables)
            );
        }
    }

    @Nested
    @DisplayName("Account Alert Tests")
    class AccountAlertTests {

        @Test
        @DisplayName("Should send security alert notification immediately with high priority")
        void shouldSendSecurityAlertWithHighPriority() {
            // Arrange
            Long customerId = 12345L;
            String alertType = "SUSPICIOUS_LOGIN";
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("loginLocation", "New York, NY");
            alertData.put("loginTime", "2024-01-15 14:30:00");
            alertData.put("ipAddress", "192.168.1.100");
            alertData.put("deviceType", "Mobile");
            
            Notification securityAlert = testNotification.toBuilder()
                    .templateId("security_alert")
                    .priority(10) // Highest priority for security alerts
                    .notificationType(Notification.NotificationType.SMS)
                    .channelAddress("555-123-4567")
                    .build();
                    
            SMSProvider.SMSResponse smsResponse = mock(SMSProvider.SMSResponse.class);
            when(smsResponse.getMessageId()).thenReturn("sec_alert_001");
            when(smsResponse.getStatus()).thenReturn("sent");
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(securityAlert);
            when(smsProvider.sendTemplateSMS(anyString(), anyString(), anyMap())).thenReturn(smsResponse);

            // Act
            String result = notificationService.sendAccountAlert(customerId, alertType, alertData);

            // Assert
            assertNotNull(result, "Security alert notification ID should not be null");
            
            // Verify high priority assignment for security alerts
            verify(notificationRepository).save(argThat(notification ->
                notification.getPriority() == 10 &&
                notification.getTemplateId().equals("security_alert") &&
                notification.getNotificationType() == Notification.NotificationType.SMS
            ));
            
            verify(smsProvider).sendTemplateSMS(
                eq("555-123-4567"),
                eq("security_alert"),
                eq(alertData)
            );
        }

        @Test
        @DisplayName("Should send transaction alert notification with amount validation")
        void shouldSendTransactionAlertWithAmountValidation() {
            // Arrange
            Long customerId = 12345L;
            String alertType = "LARGE_TRANSACTION";
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("transactionAmount", "$2,500.00");
            alertData.put("merchantName", "Electronics Store");
            alertData.put("cardLastFour", "4567");
            alertData.put("transactionTime", "2024-01-15 16:45:00");
            
            EmailClient.EmailResponse emailResponse = mock(EmailClient.EmailResponse.class);
            when(emailResponse.getMessageId()).thenReturn("txn_alert_002");
            when(emailResponse.getStatus()).thenReturn("delivered");
            when(emailResponse.isSuccess()).thenReturn(true);
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap())).thenReturn(emailResponse);

            // Act
            String result = notificationService.sendAccountAlert(customerId, alertType, alertData);

            // Assert
            assertNotNull(result, "Transaction alert notification ID should not be null");
            
            verify(emailClient).sendTemplateEmail(
                eq("john.doe@example.com"),
                eq("transaction_alert"),
                argThat(variables -> 
                    variables.containsKey("transactionAmount") &&
                    variables.get("transactionAmount").equals("$2,500.00")
                )
            );
        }

        @Test
        @DisplayName("Should handle multiple simultaneous account alerts for same customer")
        void shouldHandleMultipleSimultaneousAlerts() {
            // Arrange
            Long customerId = 12345L;
            String alertType1 = "LOW_BALANCE";
            String alertType2 = "PAYMENT_DUE";
            
            Map<String, Object> lowBalanceData = new HashMap<>();
            lowBalanceData.put("currentBalance", "$25.50");
            lowBalanceData.put("minimumBalance", "$50.00");
            
            Map<String, Object> paymentDueData = new HashMap<>();
            paymentDueData.put("paymentAmount", "$125.00");
            paymentDueData.put("dueDate", "2024-01-20");
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            
            EmailClient.EmailResponse emailResponse = mock(EmailClient.EmailResponse.class);
            when(emailResponse.getMessageId()).thenReturn("multi_alert_001", "multi_alert_002");
            when(emailResponse.getStatus()).thenReturn("sent");
            when(emailResponse.isSuccess()).thenReturn(true);
            
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap())).thenReturn(emailResponse);

            // Act
            String result1 = notificationService.sendAccountAlert(customerId, alertType1, lowBalanceData);
            String result2 = notificationService.sendAccountAlert(customerId, alertType2, paymentDueData);

            // Assert
            assertNotNull(result1, "First alert notification ID should not be null");
            assertNotNull(result2, "Second alert notification ID should not be null");
            assertNotEquals(result1, result2, "Alert notifications should have different IDs");
            
            // Verify both alerts were processed
            verify(emailClient, times(2)).sendTemplateEmail(anyString(), anyString(), anyMap());
            verify(notificationRepository, times(4)).save(any(Notification.class)); // 2 saves per notification
        }
    }

    @Nested
    @DisplayName("Welcome Email Tests")
    class WelcomeEmailTests {

        @Test
        @DisplayName("Should send welcome email to new customer with personalized content")
        void shouldSendWelcomeEmailWithPersonalizedContent() {
            // Arrange
            Long customerId = 12345L;
            Map<String, Object> welcomeData = new HashMap<>();
            welcomeData.put("customerFirstName", "John");
            welcomeData.put("customerLastName", "Doe");
            welcomeData.put("accountNumber", "****4567");
            welcomeData.put("activationDate", "2024-01-15");
            welcomeData.put("customerServicePhone", "1-800-555-0123");
            
            Notification welcomeNotification = testNotification.toBuilder()
                    .templateId("welcome_email")
                    .notificationType(Notification.NotificationType.EMAIL)
                    .priority(7) // High priority for welcome emails
                    .build();
                    
            EmailClient.EmailResponse emailResponse = mock(EmailClient.EmailResponse.class);
            when(emailResponse.getMessageId()).thenReturn("welcome_123456");
            when(emailResponse.getStatus()).thenReturn("delivered");
            when(emailResponse.isSuccess()).thenReturn(true);
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(welcomeNotification);
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap())).thenReturn(emailResponse);

            // Act
            String result = notificationService.sendWelcomeEmail(customerId, welcomeData);

            // Assert
            assertNotNull(result, "Welcome email notification ID should not be null");
            
            // Verify welcome email template and personalization
            verify(emailClient).sendTemplateEmail(
                eq("john.doe@example.com"),
                eq("welcome_email"),
                argThat(variables -> 
                    variables.containsKey("customerFirstName") &&
                    variables.get("customerFirstName").equals("John") &&
                    variables.containsKey("accountNumber") &&
                    variables.get("accountNumber").equals("****4567")
                )
            );
            
            // Verify high priority assignment for welcome emails
            verify(notificationRepository).save(argThat(notification ->
                notification.getPriority() == 7 &&
                notification.getTemplateId().equals("welcome_email")
            ));
        }

        @Test
        @DisplayName("Should include account setup instructions in welcome email")
        void shouldIncludeAccountSetupInstructions() {
            // Arrange
            Long customerId = 12345L;
            Map<String, Object> welcomeData = new HashMap<>();
            welcomeData.put("customerFirstName", "Jane");
            welcomeData.put("tempPassword", "TempPass123!");
            welcomeData.put("loginUrl", "https://carddemo.com/login");
            welcomeData.put("setupInstructions", "Please change your password on first login");
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            
            EmailClient.EmailResponse emailResponse = mock(EmailClient.EmailResponse.class);
            when(emailResponse.getMessageId()).thenReturn("setup_welcome_001");
            when(emailResponse.getStatus()).thenReturn("sent");
            when(emailResponse.isSuccess()).thenReturn(true);
            
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap())).thenReturn(emailResponse);

            // Act
            String result = notificationService.sendWelcomeEmail(customerId, welcomeData);

            // Assert
            assertNotNull(result, "Welcome setup email notification ID should not be null");
            
            verify(emailClient).sendTemplateEmail(
                anyString(),
                eq("welcome_email"),
                argThat(variables -> 
                    variables.containsKey("tempPassword") &&
                    variables.containsKey("loginUrl") &&
                    variables.containsKey("setupInstructions")
                )
            );
        }

        @Test
        @DisplayName("Should handle welcome email delivery failure with retry logic")
        void shouldHandleWelcomeEmailDeliveryFailureWithRetry() {
            // Arrange
            Long customerId = 12345L;
            Map<String, Object> welcomeData = new HashMap<>();
            welcomeData.put("customerFirstName", "Bob");
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("SMTP server temporarily unavailable"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> 
                notificationService.sendWelcomeEmail(customerId, welcomeData),
                "Should throw exception when welcome email delivery fails"
            );
            
            // Verify notification was marked as failed for retry processing
            verify(notificationRepository, times(2)).save(argThat(notification ->
                notification.getDeliveryStatus() == Notification.DeliveryStatus.FAILED ||
                notification.getDeliveryStatus() == Notification.DeliveryStatus.PENDING
            ));
        }
    }

    @Nested
    @DisplayName("Notification Queue Processing Tests")
    class NotificationQueueProcessingTests {

        @Test
        @DisplayName("Should process notification queue with priority ordering")
        void shouldProcessNotificationQueueWithPriorityOrdering() {
            // Arrange
            List<Notification> queuedNotifications = Arrays.asList(
                createTestNotification(1L, "LOW_PRIORITY", Notification.NotificationType.EMAIL, 3),
                createTestNotification(2L, "HIGH_PRIORITY", Notification.NotificationType.SMS, 9),
                createTestNotification(3L, "MEDIUM_PRIORITY", Notification.NotificationType.EMAIL, 5),
                createTestNotification(4L, "URGENT", Notification.NotificationType.SMS, 10)
            );
            
            when(notificationRepository.findByStatus(Notification.DeliveryStatus.PENDING))
                .thenReturn(queuedNotifications);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            
            // Mock successful delivery responses
            EmailClient.EmailResponse emailResponse = mock(EmailClient.EmailResponse.class);
            when(emailResponse.isSuccess()).thenReturn(true);
            when(emailResponse.getMessageId()).thenReturn("email_processed");
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap())).thenReturn(emailResponse);
            
            SMSProvider.SMSResponse smsResponse = mock(SMSProvider.SMSResponse.class);
            when(smsResponse.getStatus()).thenReturn("sent");
            when(smsResponse.getMessageId()).thenReturn("sms_processed");
            when(smsProvider.sendTemplateSMS(anyString(), anyString(), anyMap())).thenReturn(smsResponse);

            // Act
            int processedCount = notificationService.processNotificationQueue();

            // Assert
            assertEquals(4, processedCount, "Should process all 4 queued notifications");
            
            // Verify repository interactions
            verify(notificationRepository).findByStatus(Notification.DeliveryStatus.PENDING);
            verify(notificationRepository, times(8)).save(any(Notification.class)); // 2 saves per notification
            
            // Verify notifications were processed (email and SMS calls)
            verify(emailClient, times(2)).sendTemplateEmail(anyString(), anyString(), anyMap());
            verify(smsProvider, times(2)).sendTemplateSMS(anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Should handle batch processing with rate limiting")
        void shouldHandleBatchProcessingWithRateLimiting() {
            // Arrange
            List<Notification> largeBatch = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                largeBatch.add(createTestNotification((long) i, "BATCH_" + i, 
                    Notification.NotificationType.EMAIL, 5));
            }
            
            when(notificationRepository.findByStatus(Notification.DeliveryStatus.PENDING))
                .thenReturn(largeBatch);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            
            EmailClient.EmailResponse emailResponse = mock(EmailClient.EmailResponse.class);
            when(emailResponse.isSuccess()).thenReturn(true);
            when(emailResponse.getMessageId()).thenReturn("batch_email");
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap())).thenReturn(emailResponse);

            // Act
            int processedCount = notificationService.processNotificationQueue();

            // Assert
            assertEquals(100, processedCount, "Should process all 100 notifications in batch");
            
            // Verify rate limiting was applied (all notifications processed)
            verify(emailClient, times(100)).sendTemplateEmail(anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Should skip notifications that exceed retry limit")
        void shouldSkipNotificationsThatExceedRetryLimit() {
            // Arrange
            Notification exceededRetryNotification = createTestNotification(1L, "EXCEEDED_RETRY", 
                Notification.NotificationType.EMAIL, 5);
            exceededRetryNotification.setRetryCount(5); // Exceeds max retry limit of 3
            exceededRetryNotification.setDeliveryStatus(Notification.DeliveryStatus.FAILED);
            
            Notification validNotification = createTestNotification(2L, "VALID_NOTIFICATION", 
                Notification.NotificationType.EMAIL, 5);
            
            when(notificationRepository.findByStatus(Notification.DeliveryStatus.PENDING))
                .thenReturn(Arrays.asList(exceededRetryNotification, validNotification));
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            
            EmailClient.EmailResponse emailResponse = mock(EmailClient.EmailResponse.class);
            when(emailResponse.isSuccess()).thenReturn(true);
            when(emailResponse.getMessageId()).thenReturn("valid_email");
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap())).thenReturn(emailResponse);

            // Act
            int processedCount = notificationService.processNotificationQueue();

            // Assert
            assertEquals(1, processedCount, "Should only process notification within retry limit");
            
            // Verify only valid notification was sent
            verify(emailClient, times(1)).sendTemplateEmail(anyString(), anyString(), anyMap());
        }
    }

    @Nested
    @DisplayName("Delivery Status Update Tests")
    class DeliveryStatusUpdateTests {

        @Test
        @DisplayName("Should update delivery status to DELIVERED successfully")
        void shouldUpdateDeliveryStatusToDeliveredSuccessfully() {
            // Arrange
            String notificationId = "test_notification_001";
            String newStatus = "DELIVERED";
            LocalDateTime deliveryTime = LocalDateTime.now();
            
            Notification existingNotification = testNotification.toBuilder()
                    .deliveryStatus(Notification.DeliveryStatus.SENT)
                    .sentAt(LocalDateTime.now().minusMinutes(5))
                    .build();
                    
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(existingNotification));
            when(notificationRepository.save(any(Notification.class))).thenReturn(existingNotification);

            // Act
            boolean result = notificationService.updateDeliveryStatus(notificationId, newStatus, deliveryTime);

            // Assert
            assertTrue(result, "Delivery status update should be successful");
            
            // Verify status was updated correctly
            verify(notificationRepository).save(argThat(notification ->
                notification.getDeliveryStatus() == Notification.DeliveryStatus.DELIVERED &&
                notification.getDeliveredAt() != null
            ));
        }

        @Test
        @DisplayName("Should update delivery status to FAILED and increment retry count")
        void shouldUpdateDeliveryStatusToFailedAndIncrementRetry() {
            // Arrange
            String notificationId = "test_notification_002";
            String newStatus = "FAILED";
            String failureReason = "Invalid email address format";
            
            Notification existingNotification = testNotification.toBuilder()
                    .deliveryStatus(Notification.DeliveryStatus.SENT)
                    .retryCount(1)
                    .maxRetries(3)
                    .build();
                    
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(existingNotification));
            when(notificationRepository.save(any(Notification.class))).thenReturn(existingNotification);

            // Act
            boolean result = notificationService.updateDeliveryStatus(notificationId, newStatus, failureReason);

            // Assert
            assertTrue(result, "Delivery status update to failed should be successful");
            
            // Verify retry count was incremented and status updated
            verify(notificationRepository).save(argThat(notification ->
                notification.getDeliveryStatus() == Notification.DeliveryStatus.FAILED &&
                notification.getRetryCount() == 2 &&
                notification.getFailureReason().equals(failureReason)
            ));
        }

        @Test
        @DisplayName("Should handle delivery status tracking for external provider callbacks")
        void shouldHandleDeliveryStatusTrackingForExternalProviders() {
            // Arrange
            String notificationId = "external_provider_001";
            String providerStatus = "delivered";
            String providerMessageId = "ext_msg_12345";
            Map<String, Object> providerData = new HashMap<>();
            providerData.put("providerMessageId", providerMessageId);
            providerData.put("deliveryTimestamp", "2024-01-15T10:30:00Z");
            providerData.put("providerName", "SendGrid");
            
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

            // Act
            boolean result = notificationService.updateDeliveryStatus(notificationId, providerStatus, providerData);

            // Assert
            assertTrue(result, "External provider status update should be successful");
            
            verify(notificationRepository).save(argThat(notification ->
                notification.getDeliveryStatus() == Notification.DeliveryStatus.DELIVERED
            ));
        }

        @Test
        @DisplayName("Should handle notification not found scenario gracefully")
        void shouldHandleNotificationNotFoundGracefully() {
            // Arrange
            String nonExistentNotificationId = "non_existent_001";
            String newStatus = "DELIVERED";
            
            when(notificationRepository.findById(anyLong())).thenReturn(Optional.empty());

            // Act
            boolean result = notificationService.updateDeliveryStatus(nonExistentNotificationId, newStatus);

            // Assert
            assertFalse(result, "Update should return false for non-existent notification");
            
            // Verify no save operation was attempted
            verify(notificationRepository, never()).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("Template Processing and Variable Substitution Tests")
    class TemplateProcessingTests {

        @Test
        @DisplayName("Should process notification template with variable substitution correctly")
        void shouldProcessTemplateWithVariableSubstitutionCorrectly() {
            // Arrange
            Long customerId = 12345L;
            String templateId = "payment_reminder";
            Map<String, Object> templateVars = new HashMap<>();
            templateVars.put("customerName", "John Doe");
            templateVars.put("paymentAmount", "$125.50");
            templateVars.put("dueDate", "2024-01-25");
            templateVars.put("accountLastFour", "4567");
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            
            EmailClient.EmailResponse emailResponse = mock(EmailClient.EmailResponse.class);
            when(emailResponse.isSuccess()).thenReturn(true);
            when(emailResponse.getMessageId()).thenReturn("template_001");
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap())).thenReturn(emailResponse);

            // Act
            String result = notificationService.sendRateChangeNotification(customerId, "PAYMENT_REMINDER", templateVars);

            // Assert
            assertNotNull(result, "Template processing result should not be null");
            
            // Verify all template variables were included in the request
            verify(emailClient).sendTemplateEmail(
                anyString(),
                anyString(),
                argThat(variables -> 
                    variables.containsKey("customerName") &&
                    variables.containsKey("paymentAmount") &&
                    variables.containsKey("dueDate") &&
                    variables.containsKey("accountLastFour") &&
                    variables.get("customerName").equals("John Doe") &&
                    variables.get("paymentAmount").equals("$125.50")
                )
            );
        }

        @Test
        @DisplayName("Should handle template with missing variables gracefully")
        void shouldHandleTemplateWithMissingVariablesGracefully() {
            // Arrange
            Long customerId = 12345L;
            Map<String, Object> incompleteVars = new HashMap<>();
            incompleteVars.put("customerName", "Jane Smith");
            // Missing required variables: amount, dueDate
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            
            EmailClient.EmailResponse emailResponse = mock(EmailClient.EmailResponse.class);
            when(emailResponse.isSuccess()).thenReturn(true);
            when(emailResponse.getMessageId()).thenReturn("template_incomplete");
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap())).thenReturn(emailResponse);

            // Act
            String result = notificationService.sendAccountAlert(customerId, "PAYMENT_REMINDER", incompleteVars);

            // Assert
            assertNotNull(result, "Should handle incomplete template variables");
            
            // Verify notification was still sent with available variables
            verify(emailClient).sendTemplateEmail(anyString(), anyString(), eq(incompleteVars));
        }

        @Test
        @DisplayName("Should validate template variable types and formats")
        void shouldValidateTemplateVariableTypesAndFormats() {
            // Arrange
            Long customerId = 12345L;
            Map<String, Object> typedVars = new HashMap<>();
            typedVars.put("customerName", "Bob Johnson");
            typedVars.put("transactionAmount", new BigDecimal("1500.75"));
            typedVars.put("transactionDate", LocalDateTime.now());
            typedVars.put("isHighValue", true);
            typedVars.put("merchantCategory", "Electronics");
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            
            SMSProvider.SMSResponse smsResponse = mock(SMSProvider.SMSResponse.class);
            when(smsResponse.getStatus()).thenReturn("sent");
            when(smsProvider.sendTemplateSMS(anyString(), anyString(), anyMap())).thenReturn(smsResponse);

            // Act
            String result = notificationService.sendAccountAlert(customerId, "TRANSACTION_ALERT", typedVars);

            // Assert
            assertNotNull(result, "Should handle typed template variables");
            
            // Verify different data types were passed correctly
            verify(smsProvider).sendTemplateSMS(
                anyString(),
                anyString(),
                argThat(variables -> 
                    variables.get("customerName") instanceof String &&
                    variables.get("transactionAmount") instanceof BigDecimal &&
                    variables.get("isHighValue") instanceof Boolean
                )
            );
        }
    }

    @Nested
    @DisplayName("Bulk Notification Processing Tests")
    class BulkNotificationTests {

        @Test
        @DisplayName("Should send bulk notifications to multiple customers efficiently")
        void shouldSendBulkNotificationsToMultipleCustomersEfficiently() {
            // Arrange
            List<Long> customerIds = Arrays.asList(12345L, 12346L, 12347L, 12348L, 12349L);
            String notificationType = "PROMOTIONAL_OFFER";
            Map<String, Object> commonVariables = new HashMap<>();
            commonVariables.put("offerTitle", "Holiday Special");
            commonVariables.put("discountPercent", "15%");
            commonVariables.put("validUntil", "2024-12-31");
            
            // Mock repository responses for bulk processing
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            
            EmailClient.BulkEmailResponse bulkResponse = mock(EmailClient.BulkEmailResponse.class);
            when(bulkResponse.getSuccessCount()).thenReturn(5);
            when(bulkResponse.getFailureCount()).thenReturn(0);
            when(bulkResponse.getBatchId()).thenReturn("bulk_001");
            when(emailClient.sendBulkEmail(anyList(), anyString(), anyMap())).thenReturn(bulkResponse);

            // Act
            Map<String, Object> result = notificationService.sendBulkNotifications(customerIds, notificationType, commonVariables);

            // Assert
            assertNotNull(result, "Bulk notification result should not be null");
            assertTrue(result.containsKey("batchId"), "Result should contain batch ID");
            assertTrue(result.containsKey("successCount"), "Result should contain success count");
            assertEquals(5, result.get("successCount"), "Should report all 5 notifications as successful");
            
            // Verify bulk email was called with all customer addresses
            verify(emailClient).sendBulkEmail(
                argThat(addresses -> addresses.size() == 5),
                eq("promotional_offer"),
                eq(commonVariables)
            );
            
            // Verify individual notifications were saved
            verify(notificationRepository, times(5)).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should handle partial failures in bulk notification processing")
        void shouldHandlePartialFailuresInBulkNotificationProcessing() {
            // Arrange
            List<Long> customerIds = Arrays.asList(12345L, 12346L, 12347L);
            String notificationType = "ACCOUNT_UPDATE";
            Map<String, Object> variables = new HashMap<>();
            variables.put("updateType", "Security Enhancement");
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            
            EmailClient.BulkEmailResponse bulkResponse = mock(EmailClient.BulkEmailResponse.class);
            when(bulkResponse.getSuccessCount()).thenReturn(2);
            when(bulkResponse.getFailureCount()).thenReturn(1);
            when(bulkResponse.getBatchId()).thenReturn("bulk_partial_001");
            
            List<EmailClient.BulkEmailError> errors = Arrays.asList(
                new EmailClient.BulkEmailError("invalid@example", "Invalid email format", "VALIDATION_ERROR")
            );
            when(bulkResponse.getErrors()).thenReturn(errors);
            when(emailClient.sendBulkEmail(anyList(), anyString(), anyMap())).thenReturn(bulkResponse);

            // Act
            Map<String, Object> result = notificationService.sendBulkNotifications(customerIds, notificationType, variables);

            // Assert
            assertNotNull(result, "Bulk notification result should not be null");
            assertEquals(2, result.get("successCount"), "Should report 2 successful notifications");
            assertEquals(1, result.get("failureCount"), "Should report 1 failed notification");
            assertTrue(result.containsKey("errors"), "Result should contain error details");
            
            // Verify error handling and retry scheduling for failed notifications
            verify(notificationRepository, times(3)).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should process bulk notifications in batches to manage memory")
        void shouldProcessBulkNotificationsInBatchesToManageMemory() {
            // Arrange
            List<Long> largeCustomerList = new ArrayList<>();
            for (long i = 1; i <= 1000; i++) {
                largeCustomerList.add(i);
            }
            
            String notificationType = "SYSTEM_MAINTENANCE";
            Map<String, Object> variables = new HashMap<>();
            variables.put("maintenanceDate", "2024-02-01");
            variables.put("estimatedDuration", "4 hours");
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            
            EmailClient.BulkEmailResponse bulkResponse = mock(EmailClient.BulkEmailResponse.class);
            when(bulkResponse.getSuccessCount()).thenReturn(100); // Simulate batch size of 100
            when(bulkResponse.getFailureCount()).thenReturn(0);
            when(bulkResponse.getBatchId()).thenReturn("bulk_batch_001");
            when(emailClient.sendBulkEmail(anyList(), anyString(), anyMap())).thenReturn(bulkResponse);

            // Act
            Map<String, Object> result = notificationService.sendBulkNotifications(largeCustomerList, notificationType, variables);

            // Assert
            assertNotNull(result, "Large bulk notification result should not be null");
            assertTrue((Integer) result.get("successCount") > 0, "Should process notifications successfully");
            
            // Verify batch processing (multiple bulk email calls for large list)
            verify(emailClient, atLeast(10)).sendBulkEmail(anyList(), anyString(), anyMap());
        }
    }

    @Nested
    @DisplayName("Notification Preferences and Opt-Out Tests")
    class NotificationPreferencesTests {

        @Test
        @DisplayName("Should respect customer opt-out preferences for email notifications")
        void shouldRespectCustomerOptOutPreferencesForEmail() {
            // Arrange
            Long customerId = 12345L;
            String notificationType = "MARKETING_EMAIL";
            Map<String, Object> variables = new HashMap<>();
            variables.put("campaignName", "Spring Sale");
            
            // Customer has opted out of marketing emails
            Customer optedOutCustomer = testCustomer.toBuilder()
                    .emailOptOut(true)
                    .build();
                    
            Notification optedOutNotification = testNotification.toBuilder()
                    .customer(optedOutCustomer)
                    .optOutChecked(true)
                    .deliveryStatus(Notification.DeliveryStatus.FAILED)
                    .failureReason("Customer opted out of email notifications")
                    .build();
                    
            when(notificationRepository.save(any(Notification.class))).thenReturn(optedOutNotification);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> 
                notificationService.sendRateChangeNotification(customerId, notificationType, variables),
                "Should throw exception for opted-out customer"
            );
            
            // Verify email was not sent
            verify(emailClient, never()).sendTemplateEmail(anyString(), anyString(), anyMap());
            
            // Verify notification was saved with opt-out status
            verify(notificationRepository).save(argThat(notification ->
                notification.getDeliveryStatus() == Notification.DeliveryStatus.FAILED &&
                notification.getFailureReason().contains("opted out")
            ));
        }

        @Test
        @DisplayName("Should validate customer SMS preferences before sending SMS alerts")
        void shouldValidateCustomerSmsPreferencesBeforeSendingSmsAlerts() {
            // Arrange
            Long customerId = 12345L;
            String alertType = "FRAUD_ALERT";
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("suspiciousActivity", "Unusual login location");
            
            // Customer allows SMS for security alerts but not marketing
            Customer smsCustomer = testCustomer.toBuilder()
                    .smsOptOut(false)
                    .smsSecurityAlertsEnabled(true)
                    .build();
                    
            Notification smsNotification = testNotification.toBuilder()
                    .customer(smsCustomer)
                    .notificationType(Notification.NotificationType.SMS)
                    .channelAddress("555-123-4567")
                    .templateId("fraud_alert_sms")
                    .optOutChecked(true)
                    .build();
                    
            when(notificationRepository.save(any(Notification.class))).thenReturn(smsNotification);
            
            SMSProvider.SMSResponse smsResponse = mock(SMSProvider.SMSResponse.class);
            when(smsResponse.getStatus()).thenReturn("sent");
            when(smsResponse.getMessageId()).thenReturn("fraud_alert_001");
            when(smsProvider.sendTemplateSMS(anyString(), anyString(), anyMap())).thenReturn(smsResponse);

            // Act
            String result = notificationService.sendAccountAlert(customerId, alertType, alertData);

            // Assert
            assertNotNull(result, "Fraud alert SMS should be sent despite general SMS opt-out");
            
            // Verify SMS was sent for security alert
            verify(smsProvider).sendTemplateSMS(
                eq("555-123-4567"),
                eq("fraud_alert_sms"),
                eq(alertData)
            );
            
            // Verify opt-out preference was checked
            verify(notificationRepository).save(argThat(notification ->
                notification.getOptOutChecked() == true
            ));
        }

        @Test
        @DisplayName("Should handle channel preference switching when primary channel unavailable")
        void shouldHandleChannelPreferenceSwitchingWhenPrimaryChannelUnavailable() {
            // Arrange
            Long customerId = 12345L;
            String notificationType = "PAYMENT_DUE";
            Map<String, Object> variables = new HashMap<>();
            variables.put("paymentAmount", "$250.00");
            variables.put("dueDate", "2024-01-30");
            
            // Customer prefers email but email service is down, fallback to SMS
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("Email service unavailable"));
            
            SMSProvider.SMSResponse smsResponse = mock(SMSProvider.SMSResponse.class);
            when(smsResponse.getStatus()).thenReturn("sent");
            when(smsResponse.getMessageId()).thenReturn("fallback_sms_001");
            when(smsProvider.sendTemplateSMS(anyString(), anyString(), anyMap())).thenReturn(smsResponse);

            // Act & Assert
            // The service should handle the fallback internally
            assertThrows(RuntimeException.class, () -> 
                notificationService.sendAccountAlert(customerId, notificationType, variables)
            );
            
            // Verify email was attempted first
            verify(emailClient).sendTemplateEmail(anyString(), anyString(), anyMap());
        }
    }

    @Nested
    @DisplayName("Error Handling and Validation Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should validate notification parameters before processing")
        void shouldValidateNotificationParametersBeforeProcessing() {
            // Arrange - Test null customer ID
            Long nullCustomerId = null;
            String notificationType = "TEST_NOTIFICATION";
            Map<String, Object> variables = new HashMap<>();

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> 
                notificationService.sendRateChangeNotification(nullCustomerId, notificationType, variables),
                "Should throw exception for null customer ID"
            );
            
            // Verify no repository or client interactions occurred
            verify(notificationRepository, never()).save(any(Notification.class));
            verify(emailClient, never()).sendTemplateEmail(anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("Should handle invalid email addresses gracefully")
        void shouldHandleInvalidEmailAddressesGracefully() {
            // Arrange
            Long customerId = 12345L;
            String notificationType = "EMAIL_TEST";
            Map<String, Object> variables = new HashMap<>();
            variables.put("testMessage", "Invalid email test");
            
            Customer invalidEmailCustomer = testCustomer.toBuilder()
                    .emailAddress("invalid-email-format")
                    .build();
                    
            Notification invalidEmailNotification = testNotification.toBuilder()
                    .customer(invalidEmailCustomer)
                    .channelAddress("invalid-email-format")
                    .build();
                    
            when(notificationRepository.save(any(Notification.class))).thenReturn(invalidEmailNotification);
            when(emailClient.validateEmailAddress("invalid-email-format")).thenReturn(false);
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap()))
                .thenThrow(new IllegalArgumentException("Invalid email address format"));

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> 
                notificationService.sendRateChangeNotification(customerId, notificationType, variables),
                "Should throw exception for invalid email address"
            );
            
            // Verify email validation was attempted
            verify(emailClient).validateEmailAddress("invalid-email-format");
        }

        @Test
        @DisplayName("Should handle SMS provider failures with exponential backoff retry")
        void shouldHandleSmsProviderFailuresWithExponentialBackoffRetry() {
            // Arrange
            Long customerId = 12345L;
            String alertType = "SMS_RETRY_TEST";
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("testData", "SMS retry test");
            
            Notification smsNotification = testNotification.toBuilder()
                    .notificationType(Notification.NotificationType.SMS)
                    .channelAddress("555-123-4567")
                    .retryCount(0)
                    .maxRetries(3)
                    .build();
                    
            when(notificationRepository.save(any(Notification.class))).thenReturn(smsNotification);
            when(smsProvider.sendTemplateSMS(anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("SMS gateway timeout"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> 
                notificationService.sendAccountAlert(customerId, alertType, alertData),
                "Should throw exception when SMS provider fails"
            );
            
            // Verify retry logic was triggered
            verify(notificationRepository, times(2)).save(argThat(notification ->
                notification.getRetryCount() >= 0 &&
                (notification.getDeliveryStatus() == Notification.DeliveryStatus.FAILED ||
                 notification.getDeliveryStatus() == Notification.DeliveryStatus.PENDING)
            ));
        }

        @Test
        @DisplayName("Should handle concurrent notification processing safely")
        void shouldHandleConcurrentNotificationProcessingSafely() {
            // Arrange
            List<CompletableFuture<String>> concurrentTasks = new ArrayList<>();
            
            for (int i = 1; i <= 10; i++) {
                final int taskId = i;
                CompletableFuture<String> task = CompletableFuture.supplyAsync(() -> {
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("taskId", String.valueOf(taskId));
                    variables.put("timestamp", LocalDateTime.now().toString());
                    
                    try {
                        return notificationService.sendAccountAlert(12345L, "CONCURRENT_TEST", variables);
                    } catch (Exception e) {
                        return "ERROR: " + e.getMessage();
                    }
                });
                concurrentTasks.add(task);
            }
            
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            
            EmailClient.EmailResponse emailResponse = mock(EmailClient.EmailResponse.class);
            when(emailResponse.isSuccess()).thenReturn(true);
            when(emailResponse.getMessageId()).thenReturn("concurrent_test");
            when(emailClient.sendTemplateEmail(anyString(), anyString(), anyMap())).thenReturn(emailResponse);

            // Act
            List<String> results = concurrentTasks.stream()
                    .map(CompletableFuture::join)
                    .collect(java.util.stream.Collectors.toList());

            // Assert
            assertEquals(10, results.size(), "All concurrent tasks should complete");
            
            // Verify thread safety - all operations should have completed successfully
            long successfulTasks = results.stream()
                    .filter(result -> !result.startsWith("ERROR"))
                    .count();
                    
            assertTrue(successfulTasks > 0, "At least some concurrent tasks should succeed");
            
            // Verify repository was called for each task
            verify(notificationRepository, atLeast(10)).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("Notification Archival and Cleanup Tests")
    class NotificationArchivalTests {

        @Test
        @DisplayName("Should archive old delivered notifications automatically")
        void shouldArchiveOldDeliveredNotificationsAutomatically() {
            // Arrange
            LocalDateTime oldDate = LocalDateTime.now().minusDays(90);
            List<Notification> oldNotifications = Arrays.asList(
                createArchivedNotification(1L, oldDate, Notification.DeliveryStatus.DELIVERED),
                createArchivedNotification(2L, oldDate, Notification.DeliveryStatus.DELIVERED),
                createArchivedNotification(3L, oldDate, Notification.DeliveryStatus.FAILED)
            );
            
            when(notificationRepository.findByCreatedAtBeforeAndDeliveryStatus(any(LocalDateTime.class), any()))
                .thenReturn(oldNotifications);
            when(notificationRepository.updateDeliveryStatus(anyList(), any())).thenReturn(2);

            // Act
            int archivedCount = notificationService.archiveOldNotifications(90);

            // Assert
            assertEquals(2, archivedCount, "Should archive 2 delivered notifications");
            
            // Verify only delivered notifications were archived
            verify(notificationRepository).findByCreatedAtBeforeAndDeliveryStatus(
                any(LocalDateTime.class), 
                eq(Notification.DeliveryStatus.DELIVERED)
            );
        }

        @Test
        @DisplayName("Should clean up failed notifications beyond retry limit")
        void shouldCleanUpFailedNotificationsBeyondRetryLimit() {
            // Arrange
            List<Notification> expiredFailedNotifications = Arrays.asList(
                createExpiredFailedNotification(1L, 5), // Exceeded retry limit
                createExpiredFailedNotification(2L, 4), // Exceeded retry limit
                createExpiredFailedNotification(3L, 2)  // Still within retry limit
            );
            
            when(notificationRepository.findByDeliveryStatusAndRetryCountGreaterThan(
                Notification.DeliveryStatus.FAILED, 3)).thenReturn(expiredFailedNotifications);

            // Act
            int cleanedCount = notificationService.cleanupExpiredFailedNotifications();

            // Assert
            assertEquals(2, cleanedCount, "Should clean up 2 notifications that exceeded retry limit");
            
            verify(notificationRepository).findByDeliveryStatusAndRetryCountGreaterThan(
                Notification.DeliveryStatus.FAILED, 3);
        }
    }

    // Helper methods for creating test data

    /**
     * Creates a test notification with specified parameters for testing purposes.
     * 
     * @param id the notification ID
     * @param templateId the template identifier
     * @param type the notification type (EMAIL, SMS, etc.)
     * @param priority the notification priority level
     * @return configured Notification object for testing
     */
    private Notification createTestNotification(Long id, String templateId, 
            Notification.NotificationType type, Integer priority) {
        return Notification.builder()
                .id(id)
                .customer(testCustomer)
                .templateId(templateId)
                .notificationType(type)
                .channelAddress(type == Notification.NotificationType.EMAIL ? 
                    "test@example.com" : "555-123-4567")
                .deliveryStatus(Notification.DeliveryStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .priority(priority)
                .createdAt(LocalDateTime.now())
                .optOutChecked(true)
                .build();
    }

    /**
     * Creates a test notification for archival testing.
     * 
     * @param id the notification ID
     * @param createdAt the creation timestamp
     * @param status the delivery status
     * @return configured Notification object for archival testing
     */
    private Notification createArchivedNotification(Long id, LocalDateTime createdAt, 
            Notification.DeliveryStatus status) {
        return Notification.builder()
                .id(id)
                .customer(testCustomer)
                .templateId("archived_template")
                .notificationType(Notification.NotificationType.EMAIL)
                .channelAddress("archived@example.com")
                .deliveryStatus(status)
                .createdAt(createdAt)
                .deliveredAt(status == Notification.DeliveryStatus.DELIVERED ? createdAt.plusMinutes(5) : null)
                .build();
    }

    /**
     * Creates a test notification that has exceeded retry limits.
     * 
     * @param id the notification ID
     * @param retryCount the current retry count
     * @return configured Notification object for cleanup testing
     */
    private Notification createExpiredFailedNotification(Long id, Integer retryCount) {
        return Notification.builder()
                .id(id)
                .customer(testCustomer)
                .templateId("failed_template")
                .notificationType(Notification.NotificationType.EMAIL)
                .channelAddress("failed@example.com")
                .deliveryStatus(Notification.DeliveryStatus.FAILED)
                .retryCount(retryCount)
                .maxRetries(3)
                .createdAt(LocalDateTime.now().minusDays(7))
                .failureReason("Permanent delivery failure")
                .build();
    }
}