package com.carddemo.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * External service client for SMS delivery functionality providing integration
 * with SMS gateway providers for text message notifications, delivery tracking,
 * and customer alert management.
 * 
 * This service implements comprehensive SMS capabilities including:
 * - Basic SMS sending with phone number and message content
 * - Template-based messaging with variable substitution
 * - Bulk SMS processing for batch campaigns
 * - Delivery status tracking and confirmation
 * - Phone number format validation
 * - Opt-out request handling and compliance
 * - Integration with multiple SMS gateway providers (Twilio, AWS SNS)
 * - Rate limiting for high-volume messaging
 * - Cost tracking and billing integration
 * - Comprehensive audit trails and logging
 * 
 * The service follows enterprise-grade patterns for reliability, scalability,
 * and compliance with SMS regulations and opt-out requirements.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Service
public class SMSProvider {

    private static final Logger logger = LoggerFactory.getLogger(SMSProvider.class);
    
    // Configuration properties for SMS gateway integration
    @Value("${sms.provider.primary:twilio}")
    private String primaryProvider;
    
    @Value("${sms.provider.fallback:aws-sns}")
    private String fallbackProvider;
    
    @Value("${sms.twilio.account-sid:}")
    private String twilioAccountSid;
    
    @Value("${sms.twilio.auth-token:}")
    private String twilioAuthToken;
    
    @Value("${sms.twilio.from-number:}")
    private String twilioFromNumber;
    
    @Value("${sms.aws.region:us-east-1}")
    private String awsRegion;
    
    @Value("${sms.aws.access-key:}")
    private String awsAccessKey;
    
    @Value("${sms.aws.secret-key:}")
    private String awsSecretKey;
    
    @Value("${sms.rate-limit.per-minute:60}")
    private int rateLimitPerMinute;
    
    @Value("${sms.max-retries:3}")
    private int maxRetries;
    
    @Value("${sms.retry-delay-seconds:5}")
    private int retryDelaySeconds;
    
    @Value("${sms.message-max-length:160}")
    private int messageMaxLength;
    
    @Value("${sms.cost-per-message:0.0075}")
    private BigDecimal costPerMessage;
    
    @Value("${sms.bulk-batch-size:100}")
    private int bulkBatchSize;
    
    // Constants for SMS validation and processing
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?1?[2-9]\\d{2}[2-9]\\d{2}\\d{4}$");
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final String SMS_STATUS_QUEUED = "queued";
    private static final String SMS_STATUS_SENT = "sent";
    private static final String SMS_STATUS_DELIVERED = "delivered";
    private static final String SMS_STATUS_FAILED = "failed";
    private static final String SMS_STATUS_UNDELIVERED = "undelivered";
    
    // In-memory storage for demonstration purposes
    // In production, these would be backed by database tables
    private final Map<String, SMSDeliveryStatus> deliveryStatusMap = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> rateLimitTracker = new ConcurrentHashMap<>();
    private final Map<String, Boolean> optOutList = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> costTracker = new ConcurrentHashMap<>();
    
    /**
     * Sends a basic SMS message to a single recipient.
     * 
     * This method implements the core SMS sending functionality with phone number
     * validation, rate limiting, retry logic, and comprehensive error handling.
     * The implementation supports multiple SMS gateway providers with automatic
     * failover for enhanced reliability.
     * 
     * @param phoneNumber The recipient's phone number in E.164 format or US domestic format
     * @param messageContent The text message content to be sent (max 160 characters)
     * @return SMSResponse containing message ID, status, and delivery information
     * @throws IllegalArgumentException if phone number format is invalid or message is too long
     * @throws SMSDeliveryException if message delivery fails after all retry attempts
     */
    public SMSResponse sendSMS(String phoneNumber, String messageContent) {
        logger.info("Initiating SMS send request to phone: {} with message length: {}", 
                   maskPhoneNumber(phoneNumber), messageContent.length());
        
        // Validate input parameters
        if (!validatePhoneNumber(phoneNumber)) {
            logger.error("Invalid phone number format provided: {}", maskPhoneNumber(phoneNumber));
            throw new IllegalArgumentException("Invalid phone number format: " + maskPhoneNumber(phoneNumber));
        }
        
        if (messageContent == null || messageContent.trim().isEmpty()) {
            logger.error("Empty or null message content provided for phone: {}", maskPhoneNumber(phoneNumber));
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        
        if (messageContent.length() > messageMaxLength) {
            logger.error("Message length {} exceeds maximum allowed length {} for phone: {}", 
                        messageContent.length(), messageMaxLength, maskPhoneNumber(phoneNumber));
            throw new IllegalArgumentException("Message length exceeds maximum of " + messageMaxLength + " characters");
        }
        
        // Check opt-out status
        if (isOptedOut(phoneNumber)) {
            logger.warn("Attempted to send SMS to opted-out phone number: {}", maskPhoneNumber(phoneNumber));
            throw new IllegalArgumentException("Phone number has opted out of SMS communications: " + maskPhoneNumber(phoneNumber));
        }
        
        // Apply rate limiting
        if (!checkRateLimit(phoneNumber)) {
            logger.warn("Rate limit exceeded for phone number: {}", maskPhoneNumber(phoneNumber));
            throw new SMSDeliveryException("Rate limit exceeded for phone number: " + maskPhoneNumber(phoneNumber));
        }
        
        // Generate unique message ID for tracking
        String messageId = generateMessageId();
        
        // Create SMS request object
        SMSRequest request = new SMSRequest(messageId, phoneNumber, messageContent, LocalDateTime.now());
        
        // Attempt to send SMS with retry logic
        SMSResponse response = sendSMSWithRetry(request);
        
        // Update cost tracking
        updateCostTracking(messageId, costPerMessage);
        
        // Log successful send
        logger.info("SMS successfully sent - Message ID: {}, Phone: {}, Status: {}", 
                   messageId, maskPhoneNumber(phoneNumber), response.getStatus());
        
        return response;
    }
    
    /**
     * Sends a template-based SMS message with variable substitution.
     * 
     * This method supports dynamic message generation using template variables
     * in the format {{variableName}}. Variables are replaced with corresponding
     * values from the provided variables map, enabling personalized messaging
     * for customer notifications and alerts.
     * 
     * @param phoneNumber The recipient's phone number in E.164 format or US domestic format
     * @param templateName The name of the SMS template to use
     * @param variables Map of variable names to values for template substitution
     * @return SMSResponse containing message ID, status, and delivery information
     * @throws IllegalArgumentException if template is not found or variables are missing
     * @throws SMSDeliveryException if message delivery fails after all retry attempts
     */
    public SMSResponse sendTemplateSMS(String phoneNumber, String templateName, Map<String, String> variables) {
        logger.info("Initiating template SMS send - Phone: {}, Template: {}, Variables: {}", 
                   maskPhoneNumber(phoneNumber), templateName, variables.keySet());
        
        // Validate phone number
        if (!validatePhoneNumber(phoneNumber)) {
            logger.error("Invalid phone number format provided for template SMS: {}", maskPhoneNumber(phoneNumber));
            throw new IllegalArgumentException("Invalid phone number format: " + maskPhoneNumber(phoneNumber));
        }
        
        // Check opt-out status
        if (isOptedOut(phoneNumber)) {
            logger.warn("Attempted to send template SMS to opted-out phone number: {}", maskPhoneNumber(phoneNumber));
            throw new IllegalArgumentException("Phone number has opted out of SMS communications: " + maskPhoneNumber(phoneNumber));
        }
        
        // Retrieve and process template
        String templateContent = getTemplate(templateName);
        if (templateContent == null) {
            logger.error("Template not found: {}", templateName);
            throw new IllegalArgumentException("SMS template not found: " + templateName);
        }
        
        // Perform variable substitution
        String processedMessage = processTemplate(templateContent, variables);
        
        // Validate processed message length
        if (processedMessage.length() > messageMaxLength) {
            logger.error("Processed template message length {} exceeds maximum {} for template: {}", 
                        processedMessage.length(), messageMaxLength, templateName);
            throw new IllegalArgumentException("Processed template message exceeds maximum length of " + messageMaxLength + " characters");
        }
        
        // Send the processed message
        SMSResponse response = sendSMS(phoneNumber, processedMessage);
        
        // Add template information to response
        response.setTemplateName(templateName);
        response.setVariables(new HashMap<>(variables));
        
        logger.info("Template SMS successfully processed and sent - Template: {}, Message ID: {}", 
                   templateName, response.getMessageId());
        
        return response;
    }
    
    /**
     * Sends SMS messages to multiple recipients in batches for campaign processing.
     * 
     * This method implements bulk SMS functionality with batch processing, parallel
     * execution, and comprehensive error handling. Each message is processed
     * individually with proper rate limiting and delivery tracking.
     * 
     * @param recipients List of phone numbers to receive the SMS
     * @param messageContent The text message content to be sent to all recipients
     * @return BulkSMSResponse containing overall statistics and individual message results
     * @throws IllegalArgumentException if recipients list is empty or message content is invalid
     * @throws SMSDeliveryException if bulk processing fails catastrophically
     */
    public BulkSMSResponse sendBulkSMS(List<String> recipients, String messageContent) {
        logger.info("Initiating bulk SMS send - Recipients: {}, Message length: {}", 
                   recipients.size(), messageContent.length());
        
        // Validate input parameters
        if (recipients == null || recipients.isEmpty()) {
            logger.error("Empty or null recipients list provided for bulk SMS");
            throw new IllegalArgumentException("Recipients list cannot be empty");
        }
        
        if (messageContent == null || messageContent.trim().isEmpty()) {
            logger.error("Empty or null message content provided for bulk SMS");
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        
        if (messageContent.length() > messageMaxLength) {
            logger.error("Bulk SMS message length {} exceeds maximum allowed length {}", 
                        messageContent.length(), messageMaxLength);
            throw new IllegalArgumentException("Message length exceeds maximum of " + messageMaxLength + " characters");
        }
        
        // Initialize bulk response tracking
        BulkSMSResponse bulkResponse = new BulkSMSResponse();
        bulkResponse.setBatchId(generateBatchId());
        bulkResponse.setTotalRecipients(recipients.size());
        bulkResponse.setStartTime(LocalDateTime.now());
        
        List<SMSResponse> successfulSends = new ArrayList<>();
        List<BulkSMSError> errors = new ArrayList<>();
        
        // Process recipients in batches to manage memory and performance
        int totalRecipients = recipients.size();
        int processedCount = 0;
        
        for (int i = 0; i < totalRecipients; i += bulkBatchSize) {
            int endIndex = Math.min(i + bulkBatchSize, totalRecipients);
            List<String> batch = recipients.subList(i, endIndex);
            
            logger.info("Processing bulk SMS batch {}/{} - Recipients: {}", 
                       (i / bulkBatchSize) + 1, (totalRecipients + bulkBatchSize - 1) / bulkBatchSize, batch.size());
            
            // Process batch with parallel execution for performance
            List<CompletableFuture<SMSResponse>> futures = new ArrayList<>();
            
            for (String phoneNumber : batch) {
                CompletableFuture<SMSResponse> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        // Add delay to respect rate limiting across parallel threads
                        Thread.sleep(1000 / (rateLimitPerMinute / 60)); // Distribute rate limit
                        return sendSMS(phoneNumber, messageContent);
                    } catch (Exception e) {
                        logger.warn("Bulk SMS failed for phone {}: {}", maskPhoneNumber(phoneNumber), e.getMessage());
                        BulkSMSError error = new BulkSMSError(phoneNumber, e.getMessage(), LocalDateTime.now());
                        synchronized (errors) {
                            errors.add(error);
                        }
                        return null;
                    }
                });
                futures.add(future);
            }
            
            // Wait for batch completion
            for (CompletableFuture<SMSResponse> future : futures) {
                try {
                    SMSResponse response = future.get(30, TimeUnit.SECONDS);
                    if (response != null) {
                        synchronized (successfulSends) {
                            successfulSends.add(response);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Future execution failed in bulk SMS batch: {}", e.getMessage());
                }
            }
            
            processedCount += batch.size();
            logger.info("Completed bulk SMS batch processing - Progress: {}/{}", processedCount, totalRecipients);
        }
        
        // Finalize bulk response
        bulkResponse.setSuccessfulSends(successfulSends);
        bulkResponse.setErrors(errors);
        bulkResponse.setSuccessCount(successfulSends.size());
        bulkResponse.setErrorCount(errors.size());
        bulkResponse.setEndTime(LocalDateTime.now());
        
        // Calculate total cost
        BigDecimal totalCost = costPerMessage.multiply(BigDecimal.valueOf(successfulSends.size()));
        bulkResponse.setTotalCost(totalCost);
        
        logger.info("Bulk SMS completed - Batch ID: {}, Success: {}, Errors: {}, Total Cost: ${}", 
                   bulkResponse.getBatchId(), bulkResponse.getSuccessCount(), 
                   bulkResponse.getErrorCount(), totalCost);
        
        return bulkResponse;
    }
    
    /**
     * Tracks and retrieves the delivery status of a previously sent SMS message.
     * 
     * This method provides comprehensive delivery tracking including status updates,
     * timestamps, and error information. The implementation polls the SMS gateway
     * provider for real-time status information and maintains local tracking records.
     * 
     * @param messageId The unique identifier of the SMS message to track
     * @return SMSDeliveryStatus containing current status, timestamps, and delivery details
     * @throws IllegalArgumentException if message ID is invalid or not found
     */
    public SMSDeliveryStatus trackDeliveryStatus(String messageId) {
        logger.info("Tracking delivery status for message ID: {}", messageId);
        
        // Validate message ID
        if (messageId == null || messageId.trim().isEmpty()) {
            logger.error("Empty or null message ID provided for status tracking");
            throw new IllegalArgumentException("Message ID cannot be empty");
        }
        
        // Retrieve status from local tracking
        SMSDeliveryStatus status = deliveryStatusMap.get(messageId);
        if (status == null) {
            logger.error("Message ID not found in tracking system: {}", messageId);
            throw new IllegalArgumentException("Message ID not found: " + messageId);
        }
        
        // Check if status needs updating (poll gateway if older than 5 minutes)
        if (status.getLastUpdated().isBefore(LocalDateTime.now().minusMinutes(5))) {
            logger.debug("Status is stale for message {}, polling gateway for updates", messageId);
            updateDeliveryStatusFromGateway(messageId, status);
        }
        
        logger.debug("Retrieved delivery status - Message ID: {}, Status: {}, Last Updated: {}", 
                    messageId, status.getStatus(), status.getLastUpdated());
        
        return status;
    }
    
    /**
     * Validates phone number format according to E.164 and US domestic standards.
     * 
     * This method implements comprehensive phone number validation supporting
     * international E.164 format (+1234567890) and US domestic formats
     * (234-567-8900, (234) 567-8900, 234.567.8900). The validation ensures
     * compliance with SMS gateway requirements and reduces delivery failures.
     * 
     * @param phoneNumber The phone number to validate
     * @return true if the phone number format is valid, false otherwise
     */
    public boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            logger.debug("Phone number validation failed: null or empty input");
            return false;
        }
        
        // Clean phone number by removing common formatting characters
        String cleanedNumber = phoneNumber.replaceAll("[\\s().-]", "");
        
        // Remove country code prefix if present
        if (cleanedNumber.startsWith("+1")) {
            cleanedNumber = cleanedNumber.substring(2);
        } else if (cleanedNumber.startsWith("1") && cleanedNumber.length() == 11) {
            cleanedNumber = cleanedNumber.substring(1);
        }
        
        // Validate against pattern
        boolean isValid = PHONE_PATTERN.matcher("+" + cleanedNumber).matches() || 
                         PHONE_PATTERN.matcher(cleanedNumber).matches();
        
        logger.debug("Phone number validation - Input: {}, Cleaned: {}, Valid: {}", 
                    maskPhoneNumber(phoneNumber), maskPhoneNumber(cleanedNumber), isValid);
        
        return isValid;
    }
    
    /**
     * Handles opt-out requests from SMS recipients for compliance management.
     * 
     * This method implements comprehensive opt-out handling in compliance with
     * SMS regulations including TCPA (Telephone Consumer Protection Act) and
     * CAN-SPAM requirements. When a recipient opts out, all future SMS messages
     * to that number are blocked with comprehensive audit logging.
     * 
     * @param phoneNumber The phone number requesting opt-out
     * @param optOutReason Optional reason for opting out (e.g., "STOP", "UNSUBSCRIBE")
     * @return OptOutResponse containing confirmation and compliance information
     * @throws IllegalArgumentException if phone number format is invalid
     */
    public OptOutResponse handleOptOut(String phoneNumber, String optOutReason) {
        logger.info("Processing opt-out request for phone: {}, Reason: {}", 
                   maskPhoneNumber(phoneNumber), optOutReason);
        
        // Validate phone number
        if (!validatePhoneNumber(phoneNumber)) {
            logger.error("Invalid phone number format provided for opt-out: {}", maskPhoneNumber(phoneNumber));
            throw new IllegalArgumentException("Invalid phone number format: " + maskPhoneNumber(phoneNumber));
        }
        
        // Normalize phone number for consistent storage
        String normalizedNumber = normalizePhoneNumber(phoneNumber);
        
        // Add to opt-out list
        optOutList.put(normalizedNumber, true);
        
        // Create opt-out response
        OptOutResponse response = new OptOutResponse();
        response.setPhoneNumber(maskPhoneNumber(normalizedNumber));
        response.setOptOutTime(LocalDateTime.now());
        response.setReason(optOutReason != null ? optOutReason : "STOP");
        response.setConfirmationId(generateConfirmationId());
        response.setStatus("CONFIRMED");
        
        // Log opt-out for compliance audit trail
        logger.info("Opt-out processed successfully - Phone: {}, Confirmation ID: {}, Reason: {}", 
                   maskPhoneNumber(normalizedNumber), response.getConfirmationId(), response.getReason());
        
        // Send confirmation SMS (if not already opted out)
        try {
            sendOptOutConfirmation(normalizedNumber, response.getConfirmationId());
        } catch (Exception e) {
            logger.warn("Failed to send opt-out confirmation SMS to {}: {}", 
                       maskPhoneNumber(normalizedNumber), e.getMessage());
        }
        
        return response;
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    /**
     * Sends SMS with automatic retry logic and failover support.
     */
    private SMSResponse sendSMSWithRetry(SMSRequest request) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < maxRetries) {
            attempts++;
            try {
                logger.debug("SMS send attempt {} for message ID: {}", attempts, request.getMessageId());
                
                SMSResponse response = attemptSMSSend(request, primaryProvider);
                
                // Create and store delivery status
                SMSDeliveryStatus deliveryStatus = new SMSDeliveryStatus();
                deliveryStatus.setMessageId(request.getMessageId());
                deliveryStatus.setPhoneNumber(request.getPhoneNumber());
                deliveryStatus.setStatus(response.getStatus());
                deliveryStatus.setProvider(primaryProvider);
                deliveryStatus.setSentTime(LocalDateTime.now());
                deliveryStatus.setLastUpdated(LocalDateTime.now());
                deliveryStatusMap.put(request.getMessageId(), deliveryStatus);
                
                return response;
                
            } catch (Exception e) {
                lastException = e;
                logger.warn("SMS send attempt {} failed for message ID {}: {}", 
                           attempts, request.getMessageId(), e.getMessage());
                
                if (attempts < maxRetries) {
                    try {
                        Thread.sleep(retryDelaySeconds * 1000L * attempts); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SMSDeliveryException("SMS send interrupted", ie);
                    }
                }
            }
        }
        
        // Try fallback provider if primary failed
        try {
            logger.info("Attempting fallback provider {} for message ID: {}", 
                       fallbackProvider, request.getMessageId());
            SMSResponse response = attemptSMSSend(request, fallbackProvider);
            
            // Create and store delivery status with fallback provider
            SMSDeliveryStatus deliveryStatus = new SMSDeliveryStatus();
            deliveryStatus.setMessageId(request.getMessageId());
            deliveryStatus.setPhoneNumber(request.getPhoneNumber());
            deliveryStatus.setStatus(response.getStatus());
            deliveryStatus.setProvider(fallbackProvider);
            deliveryStatus.setSentTime(LocalDateTime.now());
            deliveryStatus.setLastUpdated(LocalDateTime.now());
            deliveryStatusMap.put(request.getMessageId(), deliveryStatus);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Both primary and fallback providers failed for message ID: {}", 
                        request.getMessageId());
            throw new SMSDeliveryException("SMS delivery failed after " + maxRetries + 
                                         " retries and fallback attempt", lastException);
        }
    }
    
    /**
     * Attempts to send SMS using specified provider.
     */
    private SMSResponse attemptSMSSend(SMSRequest request, String provider) {
        switch (provider.toLowerCase()) {
            case "twilio":
                return sendViaTwilio(request);
            case "aws-sns":
                return sendViaAWSSNS(request);
            default:
                throw new IllegalArgumentException("Unsupported SMS provider: " + provider);
        }
    }
    
    /**
     * Sends SMS via Twilio provider.
     */
    private SMSResponse sendViaTwilio(SMSRequest request) {
        logger.debug("Sending SMS via Twilio - Message ID: {}", request.getMessageId());
        
        // Simulate Twilio API call
        // In production, this would use Twilio SDK:
        // Message message = Message.creator(
        //     new PhoneNumber(request.getPhoneNumber()),
        //     new PhoneNumber(twilioFromNumber),
        //     request.getMessageContent()
        // ).create();
        
        // Simulate successful response
        SMSResponse response = new SMSResponse();
        response.setMessageId(request.getMessageId());
        response.setPhoneNumber(request.getPhoneNumber());
        response.setStatus(SMS_STATUS_QUEUED);
        response.setProvider("twilio");
        response.setSentTime(LocalDateTime.now());
        response.setProviderMessageId("TW" + UUID.randomUUID().toString().substring(0, 8));
        response.setCost(costPerMessage);
        
        return response;
    }
    
    /**
     * Sends SMS via AWS SNS provider.
     */
    private SMSResponse sendViaAWSSNS(SMSRequest request) {
        logger.debug("Sending SMS via AWS SNS - Message ID: {}", request.getMessageId());
        
        // Simulate AWS SNS API call
        // In production, this would use AWS SNS SDK:
        // PublishRequest publishRequest = new PublishRequest()
        //     .withPhoneNumber(request.getPhoneNumber())
        //     .withMessage(request.getMessageContent());
        // PublishResult result = snsClient.publish(publishRequest);
        
        // Simulate successful response
        SMSResponse response = new SMSResponse();
        response.setMessageId(request.getMessageId());
        response.setPhoneNumber(request.getPhoneNumber());
        response.setStatus(SMS_STATUS_QUEUED);
        response.setProvider("aws-sns");
        response.setSentTime(LocalDateTime.now());
        response.setProviderMessageId("SNS" + UUID.randomUUID().toString().substring(0, 8));
        response.setCost(costPerMessage);
        
        return response;
    }
    
    /**
     * Checks rate limiting for phone number.
     */
    private boolean checkRateLimit(String phoneNumber) {
        String normalizedNumber = normalizePhoneNumber(phoneNumber);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastSent = rateLimitTracker.get(normalizedNumber);
        
        if (lastSent == null || lastSent.isBefore(now.minusMinutes(1))) {
            rateLimitTracker.put(normalizedNumber, now);
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if phone number has opted out.
     */
    private boolean isOptedOut(String phoneNumber) {
        String normalizedNumber = normalizePhoneNumber(phoneNumber);
        return optOutList.getOrDefault(normalizedNumber, false);
    }
    
    /**
     * Updates cost tracking for sent message.
     */
    private void updateCostTracking(String messageId, BigDecimal cost) {
        costTracker.put(messageId, cost);
        logger.debug("Updated cost tracking - Message ID: {}, Cost: ${}", messageId, cost);
    }
    
    /**
     * Retrieves SMS template by name.
     */
    private String getTemplate(String templateName) {
        // In production, templates would be stored in database or configuration
        Map<String, String> templates = getDefaultTemplates();
        return templates.get(templateName);
    }
    
    /**
     * Returns default SMS templates.
     */
    private Map<String, String> getDefaultTemplates() {
        Map<String, String> templates = new HashMap<>();
        templates.put("welcome", "Welcome to CardDemo, {{customerName}}! Your account is now active.");
        templates.put("transaction_alert", "Transaction Alert: ${{amount}} charged to card ending in {{cardLast4}} at {{merchant}}.");
        templates.put("payment_reminder", "Payment Reminder: Your payment of ${{amount}} is due on {{dueDate}}.");
        templates.put("account_locked", "Security Alert: Your account has been temporarily locked. Contact customer service.");
        templates.put("password_reset", "Your password reset code is {{resetCode}}. This code expires in 10 minutes.");
        templates.put("appointment_reminder", "Reminder: Your appointment is scheduled for {{appointmentTime}} on {{appointmentDate}}.");
        return templates;
    }
    
    /**
     * Processes template with variable substitution.
     */
    private String processTemplate(String template, Map<String, String> variables) {
        String processed = template;
        
        // Replace template variables with actual values
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            processed = processed.replace(placeholder, value);
        }
        
        // Check for unresolved variables
        if (TEMPLATE_VARIABLE_PATTERN.matcher(processed).find()) {
            logger.warn("Template contains unresolved variables: {}", processed);
        }
        
        return processed;
    }
    
    /**
     * Updates delivery status from SMS gateway provider.
     */
    private void updateDeliveryStatusFromGateway(String messageId, SMSDeliveryStatus status) {
        try {
            // Simulate gateway status check
            // In production, this would call the actual provider API
            String providerStatus = simulateGatewayStatusCheck(status.getProvider(), status.getProviderMessageId());
            
            status.setStatus(providerStatus);
            status.setLastUpdated(LocalDateTime.now());
            
            if (SMS_STATUS_DELIVERED.equals(providerStatus)) {
                status.setDeliveredTime(LocalDateTime.now());
            }
            
            logger.debug("Updated delivery status from gateway - Message ID: {}, Status: {}", 
                        messageId, providerStatus);
            
        } catch (Exception e) {
            logger.warn("Failed to update delivery status from gateway for message {}: {}", 
                       messageId, e.getMessage());
        }
    }
    
    /**
     * Simulates gateway status check.
     */
    private String simulateGatewayStatusCheck(String provider, String providerMessageId) {
        // Simulate random status progression
        double random = Math.random();
        if (random < 0.1) {
            return SMS_STATUS_FAILED;
        } else if (random < 0.2) {
            return SMS_STATUS_UNDELIVERED;
        } else if (random < 0.7) {
            return SMS_STATUS_DELIVERED;
        } else {
            return SMS_STATUS_SENT;
        }
    }
    
    /**
     * Sends opt-out confirmation message.
     */
    private void sendOptOutConfirmation(String phoneNumber, String confirmationId) {
        String confirmationMessage = "You have been successfully unsubscribed from SMS notifications. " +
                                   "Confirmation ID: " + confirmationId + ". Reply START to resubscribe.";
        
        // Remove from opt-out list temporarily to send confirmation
        optOutList.put(phoneNumber, false);
        
        try {
            sendSMS(phoneNumber, confirmationMessage);
        } finally {
            // Re-add to opt-out list
            optOutList.put(phoneNumber, true);
        }
    }
    
    /**
     * Normalizes phone number for consistent storage and lookup.
     */
    private String normalizePhoneNumber(String phoneNumber) {
        String cleaned = phoneNumber.replaceAll("[\\s().-]", "");
        if (cleaned.startsWith("+1")) {
            return cleaned;
        } else if (cleaned.startsWith("1") && cleaned.length() == 11) {
            return "+" + cleaned;
        } else if (cleaned.length() == 10) {
            return "+1" + cleaned;
        }
        return cleaned;
    }
    
    /**
     * Masks phone number for logging privacy.
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        return "****-****-" + lastFour;
    }
    
    /**
     * Generates unique message ID.
     */
    private String generateMessageId() {
        return "SMS" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Generates unique batch ID for bulk operations.
     */
    private String generateBatchId() {
        return "BATCH" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    
    /**
     * Generates unique confirmation ID for opt-out operations.
     */
    private String generateConfirmationId() {
        return "CONF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    
    // ==================== SUPPORTING CLASSES ====================
    
    /**
     * Represents an SMS request with all necessary information for sending.
     */
    public static class SMSRequest {
        private String messageId;
        private String phoneNumber;
        private String messageContent;
        private LocalDateTime requestTime;
        
        public SMSRequest(String messageId, String phoneNumber, String messageContent, LocalDateTime requestTime) {
            this.messageId = messageId;
            this.phoneNumber = phoneNumber;
            this.messageContent = messageContent;
            this.requestTime = requestTime;
        }
        
        // Getters and setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
        public String getMessageContent() { return messageContent; }
        public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
        
        public LocalDateTime getRequestTime() { return requestTime; }
        public void setRequestTime(LocalDateTime requestTime) { this.requestTime = requestTime; }
    }
    
    /**
     * Represents an SMS response with delivery status and tracking information.
     */
    public static class SMSResponse {
        private String messageId;
        private String phoneNumber;
        private String status;
        private String provider;
        private LocalDateTime sentTime;
        private String providerMessageId;
        private BigDecimal cost;
        private String templateName;
        private Map<String, String> variables;
        private String errorMessage;
        
        // Getters and setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        
        public LocalDateTime getSentTime() { return sentTime; }
        public void setSentTime(LocalDateTime sentTime) { this.sentTime = sentTime; }
        
        public String getProviderMessageId() { return providerMessageId; }
        public void setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; }
        
        public BigDecimal getCost() { return cost; }
        public void setCost(BigDecimal cost) { this.cost = cost; }
        
        public String getTemplateName() { return templateName; }
        public void setTemplateName(String templateName) { this.templateName = templateName; }
        
        public Map<String, String> getVariables() { return variables; }
        public void setVariables(Map<String, String> variables) { this.variables = variables; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    /**
     * Represents the delivery status tracking information for an SMS message.
     */
    public static class SMSDeliveryStatus {
        private String messageId;
        private String phoneNumber;
        private String status;
        private String provider;
        private String providerMessageId;
        private LocalDateTime sentTime;
        private LocalDateTime deliveredTime;
        private LocalDateTime lastUpdated;
        private String errorCode;
        private String errorMessage;
        private int deliveryAttempts;
        
        // Getters and setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        
        public String getProviderMessageId() { return providerMessageId; }
        public void setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; }
        
        public LocalDateTime getSentTime() { return sentTime; }
        public void setSentTime(LocalDateTime sentTime) { this.sentTime = sentTime; }
        
        public LocalDateTime getDeliveredTime() { return deliveredTime; }
        public void setDeliveredTime(LocalDateTime deliveredTime) { this.deliveredTime = deliveredTime; }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
        
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public int getDeliveryAttempts() { return deliveryAttempts; }
        public void setDeliveryAttempts(int deliveryAttempts) { this.deliveryAttempts = deliveryAttempts; }
    }
    
    /**
     * Represents the response from a bulk SMS operation.
     */
    public static class BulkSMSResponse {
        private String batchId;
        private int totalRecipients;
        private int successCount;
        private int errorCount;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal totalCost;
        private List<SMSResponse> successfulSends;
        private List<BulkSMSError> errors;
        
        public BulkSMSResponse() {
            this.successfulSends = new ArrayList<>();
            this.errors = new ArrayList<>();
        }
        
        // Getters and setters
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        
        public int getTotalRecipients() { return totalRecipients; }
        public void setTotalRecipients(int totalRecipients) { this.totalRecipients = totalRecipients; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
        
        public List<SMSResponse> getSuccessfulSends() { return successfulSends; }
        public void setSuccessfulSends(List<SMSResponse> successfulSends) { this.successfulSends = successfulSends; }
        
        public List<BulkSMSError> getErrors() { return errors; }
        public void setErrors(List<BulkSMSError> errors) { this.errors = errors; }
    }
    
    /**
     * Represents an error that occurred during bulk SMS processing.
     */
    public static class BulkSMSError {
        private String phoneNumber;
        private String errorMessage;
        private LocalDateTime errorTime;
        private String errorCode;
        
        public BulkSMSError(String phoneNumber, String errorMessage, LocalDateTime errorTime) {
            this.phoneNumber = phoneNumber;
            this.errorMessage = errorMessage;
            this.errorTime = errorTime;
        }
        
        // Getters and setters
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LocalDateTime getErrorTime() { return errorTime; }
        public void setErrorTime(LocalDateTime errorTime) { this.errorTime = errorTime; }
        
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    }
    
    /**
     * Represents the response from an opt-out request.
     */
    public static class OptOutResponse {
        private String phoneNumber;
        private LocalDateTime optOutTime;
        private String reason;
        private String confirmationId;
        private String status;
        
        // Getters and setters
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
        public LocalDateTime getOptOutTime() { return optOutTime; }
        public void setOptOutTime(LocalDateTime optOutTime) { this.optOutTime = optOutTime; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getConfirmationId() { return confirmationId; }
        public void setConfirmationId(String confirmationId) { this.confirmationId = confirmationId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    /**
     * Custom exception for SMS delivery failures.
     */
    public static class SMSDeliveryException extends RuntimeException {
        public SMSDeliveryException(String message) {
            super(message);
        }
        
        public SMSDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}