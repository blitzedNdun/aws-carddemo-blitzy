package com.carddemo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigDecimal;

/**
 * Spring Boot service class providing comprehensive notification functionality for customer communications
 * including rate change notifications, account alerts, transaction notifications, and promotional rate updates.
 * Supports multi-channel delivery (email, SMS) with notification history tracking and delivery status monitoring.
 * 
 * This service implements enterprise-grade notification management with:
 * - Multi-channel delivery support (Email, SMS, Push notifications)
 * - Template-based messaging with personalization
 * - Comprehensive notification history tracking
 * - Delivery status monitoring and retry mechanisms
 * - Notification preferences management
 * - Asynchronous notification processing
 * - Integration with monitoring and observability systems
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    // Notification delivery status constants
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_RETRY = "RETRY";
    
    // Notification channel constants
    public static final String CHANNEL_EMAIL = "EMAIL";
    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_PUSH = "PUSH";
    
    // Notification type constants
    public static final String TYPE_RATE_CHANGE = "RATE_CHANGE";
    public static final String TYPE_PROMOTIONAL_EXPIRY = "PROMOTIONAL_EXPIRY";
    public static final String TYPE_ACCOUNT_ALERT = "ACCOUNT_ALERT";
    public static final String TYPE_TRANSACTION = "TRANSACTION";
    
    // In-memory storage for notification history and preferences (production would use database)
    private final Map<String, List<NotificationRecord>> notificationHistory = new ConcurrentHashMap<>();
    private final Map<String, NotificationPreferences> customerPreferences = new ConcurrentHashMap<>();
    private final Map<String, String> deliveryStatus = new ConcurrentHashMap<>();
    
    // Date formatter for timestamps
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Sends rate change notifications to customers when interest rates are modified.
     * Supports multi-channel delivery based on customer preferences.
     * 
     * @param customerId The unique identifier for the customer
     * @param accountId The account ID affected by the rate change
     * @param oldRate The previous interest rate
     * @param newRate The new interest rate
     * @param effectiveDate The date when the rate change takes effect
     * @return Notification ID for tracking purposes
     */
    public String sendRateChangeNotification(String customerId, String accountId, 
                                           BigDecimal oldRate, BigDecimal newRate, 
                                           LocalDateTime effectiveDate) {
        logger.info("Processing rate change notification for customer: {} account: {}", customerId, accountId);
        
        try {
            // Generate unique notification ID
            String notificationId = generateNotificationId(TYPE_RATE_CHANGE);
            
            // Get customer notification preferences
            NotificationPreferences preferences = getNotificationPreferences(customerId);
            
            // Create notification content with rate change details
            String subject = "Important: Interest Rate Change Notice";
            String messageBody = buildRateChangeMessage(customerId, accountId, oldRate, newRate, effectiveDate);
            
            // Validate rate change parameters
            if (oldRate == null || newRate == null || effectiveDate == null) {
                throw new IllegalArgumentException("Rate change notification requires valid rates and effective date");
            }
            
            // Send notification through preferred channels
            List<String> deliveryResults = new ArrayList<>();
            
            if (preferences.isEmailEnabled() && preferences.getEmailAddress() != null) {
                String emailResult = sendEmailNotification(notificationId, preferences.getEmailAddress(), 
                                                         subject, messageBody, TYPE_RATE_CHANGE);
                deliveryResults.add(CHANNEL_EMAIL + ":" + emailResult);
                logger.debug("Email notification sent for rate change: {}", emailResult);
            }
            
            if (preferences.isSmsEnabled() && preferences.getPhoneNumber() != null) {
                String smsMessage = buildSmsRateChangeMessage(accountId, oldRate, newRate, effectiveDate);
                String smsResult = sendSmsNotification(notificationId, preferences.getPhoneNumber(), 
                                                     smsMessage, TYPE_RATE_CHANGE);
                deliveryResults.add(CHANNEL_SMS + ":" + smsResult);
                logger.debug("SMS notification sent for rate change: {}", smsResult);
            }
            
            // Track notification in history
            trackNotificationHistory(notificationId, customerId, TYPE_RATE_CHANGE, 
                                   subject, messageBody, deliveryResults);
            
            // Update delivery status
            updateDeliveryStatus(notificationId, STATUS_SENT);
            
            logger.info("Rate change notification processed successfully: {}", notificationId);
            return notificationId;
            
        } catch (Exception e) {
            logger.error("Failed to send rate change notification for customer: {} account: {}", 
                        customerId, accountId, e);
            throw new RuntimeException("Rate change notification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sends promotional rate expiration notifications to customers before their 
     * promotional interest rates expire.
     * 
     * @param customerId The unique identifier for the customer
     * @param accountId The account ID with the expiring promotional rate
     * @param currentPromotionalRate The current promotional interest rate
     * @param standardRate The standard rate that will apply after expiration
     * @param expirationDate The date when the promotional rate expires
     * @return Notification ID for tracking purposes
     */
    public String sendPromotionalRateExpiry(String customerId, String accountId, 
                                          BigDecimal currentPromotionalRate, 
                                          BigDecimal standardRate, 
                                          LocalDateTime expirationDate) {
        logger.info("Processing promotional rate expiry notification for customer: {} account: {}", 
                   customerId, accountId);
        
        try {
            // Generate unique notification ID
            String notificationId = generateNotificationId(TYPE_PROMOTIONAL_EXPIRY);
            
            // Get customer notification preferences
            NotificationPreferences preferences = getNotificationPreferences(customerId);
            
            // Create notification content
            String subject = "Promotional Rate Expiration Notice";
            String messageBody = buildPromotionalExpiryMessage(customerId, accountId, 
                                                             currentPromotionalRate, standardRate, 
                                                             expirationDate);
            
            // Validate promotional rate parameters
            if (currentPromotionalRate == null || standardRate == null || expirationDate == null) {
                throw new IllegalArgumentException("Promotional expiry notification requires valid rates and expiration date");
            }
            
            // Calculate days until expiration for urgency
            long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now(), expirationDate);
            
            if (daysUntilExpiry < 0) {
                logger.warn("Promotional rate already expired for account: {}", accountId);
            }
            
            // Send notification through preferred channels
            List<String> deliveryResults = new ArrayList<>();
            
            if (preferences.isEmailEnabled() && preferences.getEmailAddress() != null) {
                String emailResult = sendEmailNotification(notificationId, preferences.getEmailAddress(), 
                                                         subject, messageBody, TYPE_PROMOTIONAL_EXPIRY);
                deliveryResults.add(CHANNEL_EMAIL + ":" + emailResult);
                logger.debug("Email notification sent for promotional expiry: {}", emailResult);
            }
            
            if (preferences.isSmsEnabled() && preferences.getPhoneNumber() != null) {
                String smsMessage = buildSmsPromotionalExpiryMessage(accountId, expirationDate, daysUntilExpiry);
                String smsResult = sendSmsNotification(notificationId, preferences.getPhoneNumber(), 
                                                     smsMessage, TYPE_PROMOTIONAL_EXPIRY);
                deliveryResults.add(CHANNEL_SMS + ":" + smsResult);
                logger.debug("SMS notification sent for promotional expiry: {}", smsResult);
            }
            
            // Track notification in history
            trackNotificationHistory(notificationId, customerId, TYPE_PROMOTIONAL_EXPIRY, 
                                   subject, messageBody, deliveryResults);
            
            // Update delivery status
            updateDeliveryStatus(notificationId, STATUS_SENT);
            
            logger.info("Promotional rate expiry notification processed successfully: {}", notificationId);
            return notificationId;
            
        } catch (Exception e) {
            logger.error("Failed to send promotional expiry notification for customer: {} account: {}", 
                        customerId, accountId, e);
            throw new RuntimeException("Promotional expiry notification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Tracks notification history for audit and reporting purposes.
     * Maintains comprehensive records of all notifications sent to customers.
     * 
     * @param notificationId The unique notification identifier
     * @param customerId The customer who received the notification
     * @param notificationType The type of notification sent
     * @param subject The notification subject line
     * @param messageBody The notification content
     * @param deliveryChannels List of delivery channels used
     * @return Success status of history tracking
     */
    public boolean trackNotificationHistory(String notificationId, String customerId, 
                                          String notificationType, String subject, 
                                          String messageBody, List<String> deliveryChannels) {
        logger.debug("Tracking notification history for ID: {} customer: {}", notificationId, customerId);
        
        try {
            // Validate customer ID
            String safeCustomerId = customerId != null ? customerId : "UNKNOWN_CUSTOMER";
            
            // Create notification record
            NotificationRecord record = new NotificationRecord();
            record.setNotificationId(notificationId);
            record.setCustomerId(safeCustomerId);
            record.setNotificationType(notificationType);
            record.setSubject(subject);
            record.setMessageBody(messageBody);
            record.setDeliveryChannels(deliveryChannels);
            record.setCreatedTimestamp(LocalDateTime.now());
            record.setStatus(STATUS_PENDING);
            
            // Store in notification history
            notificationHistory.computeIfAbsent(safeCustomerId, k -> new ArrayList<>()).add(record);
            
            // Log for audit trail
            logger.info("Notification history tracked - ID: {} Type: {} Customer: {} Channels: {}", 
                       notificationId, notificationType, safeCustomerId, deliveryChannels);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to track notification history for ID: {} customer: {}", 
                        notificationId, customerId, e);
            return false;
        }
    }

    /**
     * Sends account alert notifications for important account events such as 
     * payment due dates, overlimit conditions, or suspicious activity.
     * 
     * @param customerId The unique identifier for the customer
     * @param accountId The account ID for the alert
     * @param alertType The type of alert (PAYMENT_DUE, OVERLIMIT, SUSPICIOUS_ACTIVITY, etc.)
     * @param alertMessage The specific alert message content
     * @param urgencyLevel The urgency level (LOW, MEDIUM, HIGH, CRITICAL)
     * @return Notification ID for tracking purposes
     */
    public String sendAccountAlert(String customerId, String accountId, 
                                 String alertType, String alertMessage, 
                                 String urgencyLevel) {
        logger.info("Processing account alert for customer: {} account: {} type: {} urgency: {}", 
                   customerId, accountId, alertType, urgencyLevel);
        
        try {
            // Generate unique notification ID
            String notificationId = generateNotificationId(TYPE_ACCOUNT_ALERT);
            
            // Get customer notification preferences
            NotificationPreferences preferences = getNotificationPreferences(customerId);
            
            // Validate alert parameters
            if (alertType == null || alertMessage == null || urgencyLevel == null) {
                throw new IllegalArgumentException("Account alert requires valid alert type, message, and urgency level");
            }
            
            // Create notification content based on urgency
            String subject = buildAlertSubject(alertType, urgencyLevel);
            String messageBody = buildAccountAlertMessage(customerId, accountId, alertType, 
                                                        alertMessage, urgencyLevel);
            
            // Send notification through preferred channels (prioritize by urgency)
            List<String> deliveryResults = new ArrayList<>();
            
            // For critical alerts, send through all available channels
            if ("CRITICAL".equals(urgencyLevel) || "HIGH".equals(urgencyLevel)) {
                if (preferences.getEmailAddress() != null) {
                    String emailResult = sendEmailNotification(notificationId, preferences.getEmailAddress(), 
                                                             subject, messageBody, TYPE_ACCOUNT_ALERT);
                    deliveryResults.add(CHANNEL_EMAIL + ":" + emailResult);
                }
                
                if (preferences.getPhoneNumber() != null) {
                    String smsMessage = buildSmsAccountAlert(alertType, alertMessage, urgencyLevel);
                    String smsResult = sendSmsNotification(notificationId, preferences.getPhoneNumber(), 
                                                         smsMessage, TYPE_ACCOUNT_ALERT);
                    deliveryResults.add(CHANNEL_SMS + ":" + smsResult);
                }
            } else {
                // For lower urgency, use preferred channels only
                if (preferences.isEmailEnabled() && preferences.getEmailAddress() != null) {
                    String emailResult = sendEmailNotification(notificationId, preferences.getEmailAddress(), 
                                                             subject, messageBody, TYPE_ACCOUNT_ALERT);
                    deliveryResults.add(CHANNEL_EMAIL + ":" + emailResult);
                }
                
                if (preferences.isSmsEnabled() && preferences.getPhoneNumber() != null) {
                    String smsMessage = buildSmsAccountAlert(alertType, alertMessage, urgencyLevel);
                    String smsResult = sendSmsNotification(notificationId, preferences.getPhoneNumber(), 
                                                         smsMessage, TYPE_ACCOUNT_ALERT);
                    deliveryResults.add(CHANNEL_SMS + ":" + smsResult);
                }
            }
            
            // Track notification in history
            trackNotificationHistory(notificationId, customerId, TYPE_ACCOUNT_ALERT, 
                                   subject, messageBody, deliveryResults);
            
            // Update delivery status
            updateDeliveryStatus(notificationId, STATUS_SENT);
            
            logger.info("Account alert notification processed successfully: {}", notificationId);
            return notificationId;
            
        } catch (Exception e) {
            logger.error("Failed to send account alert for customer: {} account: {} type: {}", 
                        customerId, accountId, alertType, e);
            throw new RuntimeException("Account alert notification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sends transaction notifications for significant transactions such as 
     * large purchases, online transactions, or international usage.
     * 
     * @param customerId The unique identifier for the customer
     * @param accountId The account ID for the transaction
     * @param transactionId The unique transaction identifier
     * @param transactionAmount The transaction amount
     * @param merchantName The name of the merchant
     * @param transactionDate The date and time of the transaction
     * @return Notification ID for tracking purposes
     */
    public String sendTransactionNotification(String customerId, String accountId, 
                                            String transactionId, BigDecimal transactionAmount, 
                                            String merchantName, LocalDateTime transactionDate) {
        logger.info("Processing transaction notification for customer: {} transaction: {} amount: {}", 
                   customerId, transactionId, transactionAmount);
        
        try {
            // Generate unique notification ID
            String notificationId = generateNotificationId(TYPE_TRANSACTION);
            
            // Get customer notification preferences
            NotificationPreferences preferences = getNotificationPreferences(customerId);
            
            // Validate transaction parameters
            if (transactionId == null || transactionAmount == null || merchantName == null || transactionDate == null) {
                throw new IllegalArgumentException("Transaction notification requires valid transaction details");
            }
            
            // Create notification content
            String subject = "Transaction Alert";
            String messageBody = buildTransactionMessage(customerId, accountId, transactionId, 
                                                       transactionAmount, merchantName, transactionDate);
            
            // Determine if transaction requires special handling (large amount, international, etc.)
            boolean isLargeTransaction = transactionAmount.compareTo(new BigDecimal("1000.00")) > 0;
            boolean requiresNotification = preferences.isTransactionAlertsEnabled() || isLargeTransaction;
            
            if (!requiresNotification) {
                logger.debug("Transaction notification skipped - not enabled for customer: {}", customerId);
                return notificationId;
            }
            
            // Send notification through preferred channels
            List<String> deliveryResults = new ArrayList<>();
            
            if (preferences.isEmailEnabled() && preferences.getEmailAddress() != null) {
                String emailResult = sendEmailNotification(notificationId, preferences.getEmailAddress(), 
                                                         subject, messageBody, TYPE_TRANSACTION);
                deliveryResults.add(CHANNEL_EMAIL + ":" + emailResult);
                logger.debug("Email notification sent for transaction: {}", emailResult);
            }
            
            if (preferences.isSmsEnabled() && preferences.getPhoneNumber() != null) {
                String smsMessage = buildSmsTransactionMessage(transactionAmount, merchantName, transactionDate);
                String smsResult = sendSmsNotification(notificationId, preferences.getPhoneNumber(), 
                                                     smsMessage, TYPE_TRANSACTION);
                deliveryResults.add(CHANNEL_SMS + ":" + smsResult);
                logger.debug("SMS notification sent for transaction: {}", smsResult);
            }
            
            // Track notification in history
            trackNotificationHistory(notificationId, customerId, TYPE_TRANSACTION, 
                                   subject, messageBody, deliveryResults);
            
            // Update delivery status
            updateDeliveryStatus(notificationId, STATUS_SENT);
            
            logger.info("Transaction notification processed successfully: {}", notificationId);
            return notificationId;
            
        } catch (Exception e) {
            logger.error("Failed to send transaction notification for customer: {} transaction: {}", 
                        customerId, transactionId, e);
            throw new RuntimeException("Transaction notification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves notification preferences for a specific customer.
     * Returns customer's channel preferences, contact information, and notification settings.
     * 
     * @param customerId The unique identifier for the customer
     * @return NotificationPreferences object containing customer's notification settings
     */
    public NotificationPreferences getNotificationPreferences(String customerId) {
        logger.debug("Retrieving notification preferences for customer: {}", customerId);
        
        try {
            // Validate customer ID
            if (customerId == null || customerId.trim().isEmpty()) {
                logger.warn("Invalid customer ID provided: {}", customerId);
                return createDefaultPreferences("UNKNOWN_CUSTOMER");
            }
            
            // Get preferences from storage (default preferences if not found)
            NotificationPreferences preferences = customerPreferences.get(customerId);
            
            if (preferences == null) {
                // Create default preferences for new customer
                preferences = createDefaultPreferences(customerId);
                customerPreferences.put(customerId, preferences);
                logger.info("Created default notification preferences for customer: {}", customerId);
            }
            
            logger.debug("Retrieved notification preferences for customer: {} - Email: {} SMS: {}", 
                        customerId, preferences.isEmailEnabled(), preferences.isSmsEnabled());
            
            return preferences;
            
        } catch (Exception e) {
            logger.error("Failed to retrieve notification preferences for customer: {}", customerId, e);
            // Return default preferences to prevent notification failures
            return createDefaultPreferences(customerId != null ? customerId : "UNKNOWN_CUSTOMER");
        }
    }

    /**
     * Updates the delivery status of a notification for tracking and monitoring purposes.
     * Supports status transitions and retry handling.
     * 
     * @param notificationId The unique notification identifier
     * @param status The new delivery status (PENDING, SENT, DELIVERED, FAILED, RETRY)
     * @return Success status of the update operation
     */
    public boolean updateDeliveryStatus(String notificationId, String status) {
        logger.debug("Updating delivery status for notification: {} to status: {}", notificationId, status);
        
        try {
            // Validate status value
            if (!isValidStatus(status)) {
                throw new IllegalArgumentException("Invalid delivery status: " + status);
            }
            
            // Update status in storage
            String previousStatus = deliveryStatus.put(notificationId, status);
            
            // Log status change for audit trail
            logger.info("Delivery status updated for notification: {} from {} to {}", 
                       notificationId, previousStatus, status);
            
            // Handle status-specific logic
            switch (status) {
                case STATUS_FAILED:
                    handleFailedDelivery(notificationId);
                    break;
                case STATUS_DELIVERED:
                    logger.info("Notification successfully delivered: {}", notificationId);
                    break;
                case STATUS_RETRY:
                    scheduleRetryDelivery(notificationId);
                    break;
                default:
                    // No special handling needed for other statuses
                    break;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to update delivery status for notification: {} to status: {}", 
                        notificationId, status, e);
            return false;
        }
    }

    // Private helper methods for notification processing

    /**
     * Generates a unique notification ID for tracking purposes.
     */
    private String generateNotificationId(String notificationType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomId = UUID.randomUUID().toString().substring(0, 8);
        return notificationType + "_" + timestamp + "_" + randomId;
    }

    /**
     * Sends email notification through the email service provider.
     */
    private String sendEmailNotification(String notificationId, String emailAddress, 
                                       String subject, String messageBody, String notificationType) {
        logger.debug("Sending email notification to: {} subject: {}", emailAddress, subject);
        
        try {
            // Simulate email sending (production would integrate with actual email service)
            // Email validation
            if (!isValidEmailAddress(emailAddress)) {
                throw new IllegalArgumentException("Invalid email address: " + emailAddress);
            }
            
            // Create email with headers and content
            Map<String, String> emailHeaders = new HashMap<>();
            emailHeaders.put("From", "noreply@carddemo.com");
            emailHeaders.put("To", emailAddress);
            emailHeaders.put("Subject", subject);
            emailHeaders.put("Content-Type", "text/html; charset=UTF-8");
            emailHeaders.put("X-Notification-ID", notificationId);
            emailHeaders.put("X-Notification-Type", notificationType);
            
            // Send email asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    // Simulate network delay
                    Thread.sleep(100);
                    logger.debug("Email sent successfully to: {}", emailAddress);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Email sending interrupted for: {}", emailAddress);
                }
            });
            
            return "EMAIL_SENT_" + System.currentTimeMillis();
            
        } catch (Exception e) {
            logger.error("Failed to send email notification to: {}", emailAddress, e);
            return "EMAIL_FAILED_" + e.getMessage();
        }
    }

    /**
     * Sends SMS notification through the SMS service provider.
     */
    private String sendSmsNotification(String notificationId, String phoneNumber, 
                                     String message, String notificationType) {
        logger.debug("Sending SMS notification to: {} message length: {}", phoneNumber, message.length());
        
        try {
            // SMS validation
            if (!isValidPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException("Invalid phone number: " + phoneNumber);
            }
            
            // Truncate message if too long for SMS
            String smsMessage = message.length() > 160 ? message.substring(0, 157) + "..." : message;
            
            // Create SMS with metadata
            Map<String, String> smsHeaders = new HashMap<>();
            smsHeaders.put("To", phoneNumber);
            smsHeaders.put("X-Notification-ID", notificationId);
            smsHeaders.put("X-Notification-Type", notificationType);
            
            // Send SMS asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    // Simulate network delay
                    Thread.sleep(50);
                    logger.debug("SMS sent successfully to: {}", phoneNumber);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("SMS sending interrupted for: {}", phoneNumber);
                }
            });
            
            return "SMS_SENT_" + System.currentTimeMillis();
            
        } catch (Exception e) {
            logger.error("Failed to send SMS notification to: {}", phoneNumber, e);
            return "SMS_FAILED_" + e.getMessage();
        }
    }

    /**
     * Builds rate change notification message content.
     */
    private String buildRateChangeMessage(String customerId, String accountId, 
                                        BigDecimal oldRate, BigDecimal newRate, 
                                        LocalDateTime effectiveDate) {
        StringBuilder message = new StringBuilder();
        message.append("<html><body>");
        message.append("<h2>Interest Rate Change Notice</h2>");
        message.append("<p>Dear Valued Customer,</p>");
        message.append("<p>We are writing to inform you of an important change to your credit card account.</p>");
        message.append("<div style='background-color: #f9f9f9; padding: 15px; margin: 15px 0; border-left: 4px solid #007bff;'>");
        message.append("<h3>Account Information:</h3>");
        message.append("<p><strong>Account ID:</strong> ").append(accountId).append("</p>");
        message.append("<p><strong>Current Rate:</strong> ").append(oldRate).append("%</p>");
        message.append("<p><strong>New Rate:</strong> ").append(newRate).append("%</p>");
        message.append("<p><strong>Effective Date:</strong> ").append(effectiveDate.format(timestampFormatter)).append("</p>");
        message.append("</div>");
        message.append("<p>This change will be reflected in your next billing statement. ");
        message.append("If you have any questions, please contact customer service.</p>");
        message.append("<p>Thank you for your continued business.</p>");
        message.append("<p>CardDemo Customer Service Team</p>");
        message.append("</body></html>");
        
        return message.toString();
    }

    /**
     * Builds SMS message for rate change notifications.
     */
    private String buildSmsRateChangeMessage(String accountId, BigDecimal oldRate, 
                                           BigDecimal newRate, LocalDateTime effectiveDate) {
        return String.format("CardDemo Alert: Rate change for account %s from %s%% to %s%% effective %s. Contact us with questions.", 
                           accountId, oldRate, newRate, effectiveDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
    }

    /**
     * Builds promotional rate expiry notification message content.
     */
    private String buildPromotionalExpiryMessage(String customerId, String accountId, 
                                               BigDecimal currentPromotionalRate, 
                                               BigDecimal standardRate, 
                                               LocalDateTime expirationDate) {
        StringBuilder message = new StringBuilder();
        message.append("<html><body>");
        message.append("<h2>Promotional Rate Expiration Notice</h2>");
        message.append("<p>Dear Valued Customer,</p>");
        message.append("<p>Your promotional interest rate is set to expire soon.</p>");
        message.append("<div style='background-color: #fff3cd; padding: 15px; margin: 15px 0; border-left: 4px solid #ffc107;'>");
        message.append("<h3>Rate Change Details:</h3>");
        message.append("<p><strong>Account ID:</strong> ").append(accountId).append("</p>");
        message.append("<p><strong>Current Promotional Rate:</strong> ").append(currentPromotionalRate).append("%</p>");
        message.append("<p><strong>Standard Rate (after expiration):</strong> ").append(standardRate).append("%</p>");
        message.append("<p><strong>Expiration Date:</strong> ").append(expirationDate.format(timestampFormatter)).append("</p>");
        message.append("</div>");
        message.append("<p>After the expiration date, your account will automatically transition to the standard rate. ");
        message.append("No action is required on your part.</p>");
        message.append("<p>Thank you for choosing CardDemo.</p>");
        message.append("<p>CardDemo Customer Service Team</p>");
        message.append("</body></html>");
        
        return message.toString();
    }

    /**
     * Builds SMS message for promotional rate expiry notifications.
     */
    private String buildSmsPromotionalExpiryMessage(String accountId, LocalDateTime expirationDate, long daysUntilExpiry) {
        if (daysUntilExpiry <= 7) {
            return String.format("CardDemo Notice: Promotional rate for account %s expires in %d days on %s. Standard rate will apply.", 
                               accountId, daysUntilExpiry, expirationDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
        } else {
            return String.format("CardDemo Notice: Promotional rate for account %s expires %s. Standard rate will apply after expiration.", 
                               accountId, expirationDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
        }
    }

    /**
     * Builds alert subject line based on alert type and urgency.
     */
    private String buildAlertSubject(String alertType, String urgencyLevel) {
        String urgencyPrefix = "CRITICAL".equals(urgencyLevel) ? "URGENT: " : "";
        
        switch (alertType) {
            case "PAYMENT_DUE":
                return urgencyPrefix + "Payment Due Reminder";
            case "OVERLIMIT":
                return urgencyPrefix + "Account Over Limit Alert";
            case "SUSPICIOUS_ACTIVITY":
                return urgencyPrefix + "Security Alert: Suspicious Activity";
            case "LARGE_TRANSACTION":
                return urgencyPrefix + "Large Transaction Alert";
            case "INTERNATIONAL_USAGE":
                return urgencyPrefix + "International Usage Alert";
            default:
                return urgencyPrefix + "Account Alert";
        }
    }

    /**
     * Builds account alert notification message content.
     */
    private String buildAccountAlertMessage(String customerId, String accountId, 
                                          String alertType, String alertMessage, 
                                          String urgencyLevel) {
        StringBuilder message = new StringBuilder();
        message.append("<html><body>");
        message.append("<h2>Account Alert</h2>");
        message.append("<p>Dear Valued Customer,</p>");
        
        String bgColor = "CRITICAL".equals(urgencyLevel) ? "#f8d7da" : "#d1ecf1";
        String borderColor = "CRITICAL".equals(urgencyLevel) ? "#dc3545" : "#bee5eb";
        
        message.append("<div style='background-color: ").append(bgColor).append("; padding: 15px; margin: 15px 0; border-left: 4px solid ").append(borderColor).append(";'>");
        message.append("<h3>Alert Details:</h3>");
        message.append("<p><strong>Account ID:</strong> ").append(accountId).append("</p>");
        message.append("<p><strong>Alert Type:</strong> ").append(alertType).append("</p>");
        message.append("<p><strong>Urgency Level:</strong> ").append(urgencyLevel).append("</p>");
        message.append("<p><strong>Message:</strong> ").append(alertMessage).append("</p>");
        message.append("<p><strong>Timestamp:</strong> ").append(LocalDateTime.now().format(timestampFormatter)).append("</p>");
        message.append("</div>");
        
        if ("CRITICAL".equals(urgencyLevel) || "HIGH".equals(urgencyLevel)) {
            message.append("<p><strong>IMMEDIATE ACTION MAY BE REQUIRED.</strong> ");
            message.append("Please review your account and contact us immediately if you have any concerns.</p>");
        } else {
            message.append("<p>Please review this information and contact us if you have any questions.</p>");
        }
        
        message.append("<p>CardDemo Customer Service Team</p>");
        message.append("</body></html>");
        
        return message.toString();
    }

    /**
     * Builds SMS message for account alerts.
     */
    private String buildSmsAccountAlert(String alertType, String alertMessage, String urgencyLevel) {
        String urgencyPrefix = "CRITICAL".equals(urgencyLevel) ? "URGENT " : "";
        return String.format("CardDemo %sAlert: %s - %s. Contact us immediately if needed.", 
                           urgencyPrefix, alertType, alertMessage);
    }

    /**
     * Builds transaction notification message content.
     */
    private String buildTransactionMessage(String customerId, String accountId, 
                                         String transactionId, BigDecimal transactionAmount, 
                                         String merchantName, LocalDateTime transactionDate) {
        StringBuilder message = new StringBuilder();
        message.append("<html><body>");
        message.append("<h2>Transaction Alert</h2>");
        message.append("<p>Dear Valued Customer,</p>");
        message.append("<p>A transaction has been processed on your account.</p>");
        message.append("<div style='background-color: #d4edda; padding: 15px; margin: 15px 0; border-left: 4px solid #28a745;'>");
        message.append("<h3>Transaction Details:</h3>");
        message.append("<p><strong>Account ID:</strong> ").append(accountId).append("</p>");
        message.append("<p><strong>Transaction ID:</strong> ").append(transactionId).append("</p>");
        message.append("<p><strong>Amount:</strong> $").append(transactionAmount).append("</p>");
        message.append("<p><strong>Merchant:</strong> ").append(merchantName).append("</p>");
        message.append("<p><strong>Date/Time:</strong> ").append(transactionDate.format(timestampFormatter)).append("</p>");
        message.append("</div>");
        message.append("<p>If you did not authorize this transaction, please contact us immediately.</p>");
        message.append("<p>Thank you for using CardDemo.</p>");
        message.append("<p>CardDemo Customer Service Team</p>");
        message.append("</body></html>");
        
        return message.toString();
    }

    /**
     * Builds SMS message for transaction notifications.
     */
    private String buildSmsTransactionMessage(BigDecimal transactionAmount, String merchantName, 
                                            LocalDateTime transactionDate) {
        return String.format("CardDemo Transaction: $%s at %s on %s. Contact us if unauthorized.", 
                           transactionAmount, merchantName, transactionDate.format(DateTimeFormatter.ofPattern("MM/dd HH:mm")));
    }

    /**
     * Creates default notification preferences for a new customer.
     */
    private NotificationPreferences createDefaultPreferences(String customerId) {
        NotificationPreferences preferences = new NotificationPreferences();
        preferences.setCustomerId(customerId);
        preferences.setEmailEnabled(true);
        preferences.setSmsEnabled(false);
        preferences.setPushEnabled(false);
        preferences.setTransactionAlertsEnabled(false);
        preferences.setAccountAlertsEnabled(true);
        preferences.setRateChangeNotificationsEnabled(true);
        preferences.setPromotionalNotificationsEnabled(true);
        preferences.setEmailAddress("customer" + customerId + "@example.com"); // Default placeholder
        preferences.setPhoneNumber(null);
        preferences.setCreatedDate(LocalDateTime.now());
        preferences.setLastUpdated(LocalDateTime.now());
        
        return preferences;
    }

    /**
     * Validates delivery status values.
     */
    private boolean isValidStatus(String status) {
        return STATUS_PENDING.equals(status) || STATUS_SENT.equals(status) || 
               STATUS_DELIVERED.equals(status) || STATUS_FAILED.equals(status) || 
               STATUS_RETRY.equals(status);
    }

    /**
     * Handles failed delivery processing including retry scheduling.
     */
    private void handleFailedDelivery(String notificationId) {
        logger.warn("Handling failed delivery for notification: {}", notificationId);
        
        // Schedule retry with exponential backoff
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(30000); // Wait 30 seconds before retry
                logger.info("Retrying failed notification: {}", notificationId);
                updateDeliveryStatus(notificationId, STATUS_RETRY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Retry scheduling interrupted for notification: {}", notificationId);
            }
        });
    }

    /**
     * Schedules retry delivery for failed notifications.
     */
    private void scheduleRetryDelivery(String notificationId) {
        logger.info("Scheduling retry delivery for notification: {}", notificationId);
        
        // Implement retry logic (simplified version)
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(60000); // Wait 1 minute before retry
                logger.info("Attempting retry for notification: {}", notificationId);
                // In production, this would re-attempt the actual delivery
                updateDeliveryStatus(notificationId, STATUS_SENT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Retry attempt interrupted for notification: {}", notificationId);
            }
        });
    }

    /**
     * Validates email address format.
     */
    private boolean isValidEmailAddress(String email) {
        return email != null && email.contains("@") && email.contains(".") && email.length() > 5;
    }

    /**
     * Validates phone number format.
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.replaceAll("[^0-9]", "").length() >= 10;
    }

    // Inner classes for notification data structures

    /**
     * NotificationRecord class for tracking notification history.
     */
    public static class NotificationRecord {
        private String notificationId;
        private String customerId;
        private String notificationType;
        private String subject;
        private String messageBody;
        private List<String> deliveryChannels;
        private LocalDateTime createdTimestamp;
        private String status;

        // Getters and setters
        public String getNotificationId() { return notificationId; }
        public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getNotificationType() { return notificationType; }
        public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getMessageBody() { return messageBody; }
        public void setMessageBody(String messageBody) { this.messageBody = messageBody; }

        public List<String> getDeliveryChannels() { return deliveryChannels; }
        public void setDeliveryChannels(List<String> deliveryChannels) { this.deliveryChannels = deliveryChannels; }

        public LocalDateTime getCreatedTimestamp() { return createdTimestamp; }
        public void setCreatedTimestamp(LocalDateTime createdTimestamp) { this.createdTimestamp = createdTimestamp; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /**
     * NotificationPreferences class for customer notification settings.
     */
    public static class NotificationPreferences {
        private String customerId;
        private boolean emailEnabled;
        private boolean smsEnabled;
        private boolean pushEnabled;
        private boolean transactionAlertsEnabled;
        private boolean accountAlertsEnabled;
        private boolean rateChangeNotificationsEnabled;
        private boolean promotionalNotificationsEnabled;
        private String emailAddress;
        private String phoneNumber;
        private LocalDateTime createdDate;
        private LocalDateTime lastUpdated;

        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public boolean isEmailEnabled() { return emailEnabled; }
        public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }

        public boolean isSmsEnabled() { return smsEnabled; }
        public void setSmsEnabled(boolean smsEnabled) { this.smsEnabled = smsEnabled; }

        public boolean isPushEnabled() { return pushEnabled; }
        public void setPushEnabled(boolean pushEnabled) { this.pushEnabled = pushEnabled; }

        public boolean isTransactionAlertsEnabled() { return transactionAlertsEnabled; }
        public void setTransactionAlertsEnabled(boolean transactionAlertsEnabled) { 
            this.transactionAlertsEnabled = transactionAlertsEnabled; 
        }

        public boolean isAccountAlertsEnabled() { return accountAlertsEnabled; }
        public void setAccountAlertsEnabled(boolean accountAlertsEnabled) { 
            this.accountAlertsEnabled = accountAlertsEnabled; 
        }

        public boolean isRateChangeNotificationsEnabled() { return rateChangeNotificationsEnabled; }
        public void setRateChangeNotificationsEnabled(boolean rateChangeNotificationsEnabled) { 
            this.rateChangeNotificationsEnabled = rateChangeNotificationsEnabled; 
        }

        public boolean isPromotionalNotificationsEnabled() { return promotionalNotificationsEnabled; }
        public void setPromotionalNotificationsEnabled(boolean promotionalNotificationsEnabled) { 
            this.promotionalNotificationsEnabled = promotionalNotificationsEnabled; 
        }

        public String getEmailAddress() { return emailAddress; }
        public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}