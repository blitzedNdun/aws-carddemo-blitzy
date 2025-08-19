/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * JPA entity representing notification records for customer communications,
 * mapped to notifications PostgreSQL table.
 * 
 * Supports multiple notification channels including email notifications, SMS alerts,
 * in-app messages, and push notifications. Provides comprehensive delivery tracking
 * with retry mechanisms, template support, and customer opt-out preference handling.
 * 
 * Key Features:
 * - Multi-channel notification support (EMAIL, SMS, IN_APP, PUSH)
 * - Delivery status tracking with retry mechanism and exponential backoff
 * - Template-based content generation with variable substitution
 * - Customer notification preferences and opt-out compliance
 * - Priority-based queue management for notification processing
 * - Comprehensive audit trail and archival capabilities
 * - Integration with Customer entity for foreign key relationships
 * 
 * Database Design:
 * - Primary key: notification_id (auto-generated BIGINT)
 * - Foreign key: customer_id references customer_data.customer_id
 * - Indexes: customer_id, notification_type, delivery_status, created_at
 * - Partitioning: Archive older notifications based on created_at timestamp
 * 
 * Performance Considerations:
 * - Optimized for high-volume notification processing
 * - Efficient retry queue management with priority ordering
 * - Template caching for repeated notification patterns
 * - Bulk processing support for batch notification operations
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_customer", columnList = "customer_id"),
    @Index(name = "idx_notification_type", columnList = "notification_type"),
    @Index(name = "idx_notification_status", columnList = "delivery_status"),
    @Index(name = "idx_notification_created", columnList = "created_at"),
    @Index(name = "idx_notification_priority", columnList = "priority, created_at"),
    @Index(name = "idx_notification_retry", columnList = "retry_count, delivery_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    // Constants for field constraints and business rules
    private static final int CHANNEL_ADDRESS_MAX_LENGTH = 255;
    private static final int TEMPLATE_ID_MAX_LENGTH = 50;
    private static final int FAILURE_REASON_MAX_LENGTH = 500;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int MAX_RETRY_LIMIT = 10;
    private static final int DEFAULT_PRIORITY = 5;
    private static final int MIN_PRIORITY = 1;
    private static final int MAX_PRIORITY = 10;

    /**
     * Enumeration for supported notification types/channels.
     * Defines the different communication channels available for customer notifications.
     */
    public enum NotificationType {
        EMAIL("Email notification"),
        SMS("SMS text message"),
        IN_APP("In-application notification"),
        PUSH("Push notification");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Enumeration for notification delivery status tracking.
     * Manages the complete lifecycle of notification delivery with retry support.
     */
    public enum DeliveryStatus {
        PENDING("Pending delivery"),
        SENT("Sent to delivery provider"),
        DELIVERED("Successfully delivered"),
        FAILED("Delivery failed"),
        RETRY("Queued for retry");

        private final String description;

        DeliveryStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Notification ID - Primary key.
     * Auto-generated unique identifier for each notification record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false)
    private Long id;

    /**
     * Customer ID - Foreign key reference to Customer entity.
     * Links notification to the specific customer for personalized delivery.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_customer"))
    @NotNull(message = "Customer is required")
    private Customer customer;

    /**
     * Notification type classification.
     * Specifies the communication channel for notification delivery.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 10)
    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    /**
     * Channel-specific delivery address.
     * Contains email address, phone number, device token, or in-app user ID
     * depending on the notification type.
     */
    @Column(name = "channel_address", length = CHANNEL_ADDRESS_MAX_LENGTH)
    @Size(max = CHANNEL_ADDRESS_MAX_LENGTH, message = "Channel address cannot exceed " + CHANNEL_ADDRESS_MAX_LENGTH + " characters")
    private String channelAddress;

    /**
     * Template ID for dynamic content generation.
     * References the notification template to use for message composition.
     */
    @Column(name = "template_id", length = TEMPLATE_ID_MAX_LENGTH)
    @Size(max = TEMPLATE_ID_MAX_LENGTH, message = "Template ID cannot exceed " + TEMPLATE_ID_MAX_LENGTH + " characters")
    private String templateId;

    /**
     * Template variables for dynamic content substitution.
     * Stored as JSON string containing key-value pairs for template processing.
     */
    @Column(name = "template_variables", columnDefinition = "TEXT")
    private String templateVariables;

    /**
     * Current delivery status of the notification.
     * Tracks the notification through its complete delivery lifecycle.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 15)
    @NotNull(message = "Delivery status is required")
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    /**
     * Current retry attempt count.
     * Tracks the number of delivery attempts made for failed notifications.
     */
    @Column(name = "retry_count", nullable = false)
    @NotNull(message = "Retry count is required")
    @Min(value = 0, message = "Retry count cannot be negative")
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Maximum number of retry attempts allowed.
     * Configurable limit for retry attempts before marking notification as failed.
     */
    @Column(name = "max_retries", nullable = false)
    @NotNull(message = "Max retries is required")
    @Min(value = 0, message = "Max retries cannot be negative")
    @Max(value = MAX_RETRY_LIMIT, message = "Max retries cannot exceed " + MAX_RETRY_LIMIT)
    @Builder.Default
    private Integer maxRetries = DEFAULT_MAX_RETRIES;

    /**
     * Notification priority level for queue management.
     * Higher numbers indicate higher priority (1=lowest, 10=highest).
     */
    @Column(name = "priority", nullable = false)
    @NotNull(message = "Priority is required")
    @Min(value = MIN_PRIORITY, message = "Priority must be at least " + MIN_PRIORITY)
    @Max(value = MAX_PRIORITY, message = "Priority cannot exceed " + MAX_PRIORITY)
    @Builder.Default
    private Integer priority = DEFAULT_PRIORITY;

    /**
     * Notification creation timestamp.
     * Records when the notification was initially created in the system.
     */
    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created timestamp is required")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Notification sent timestamp.
     * Records when the notification was sent to the delivery provider.
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * Notification delivered timestamp.
     * Records when the notification was confirmed as delivered to the recipient.
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /**
     * Failure reason description.
     * Contains detailed error information when notification delivery fails.
     */
    @Column(name = "failure_reason", length = FAILURE_REASON_MAX_LENGTH)
    @Size(max = FAILURE_REASON_MAX_LENGTH, message = "Failure reason cannot exceed " + FAILURE_REASON_MAX_LENGTH + " characters")
    private String failureReason;

    /**
     * Opt-out preference checking flag.
     * Indicates whether customer opt-out preferences were validated before sending.
     */
    @Column(name = "opt_out_checked", nullable = false)
    @NotNull(message = "Opt-out checked flag is required")
    @Builder.Default
    private Boolean optOutChecked = false;

    // Constructors, getters, and setters provided by Lombok @Data annotation

    /**
     * Get notification ID.
     * @return the notification ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Set notification ID.
     * @param id the notification ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get customer ID from the associated Customer entity.
     * @return the customer ID
     */
    public Long getCustomerId() {
        return customer != null ? customer.getCustomerId() : null;
    }

    /**
     * Set customer ID by setting the Customer entity.
     * @param customerId the customer ID to set
     */
    public void setCustomerId(Long customerId) {
        if (customerId != null) {
            this.customer = Customer.builder().customerId(customerId).build();
        } else {
            this.customer = null;
        }
    }

    /**
     * Get notification type.
     * @return the notification type
     */
    public NotificationType getNotificationType() {
        return notificationType;
    }

    /**
     * Set notification type.
     * @param notificationType the notification type to set
     */
    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    /**
     * Get channel address.
     * @return the channel address
     */
    public String getChannelAddress() {
        return channelAddress;
    }

    /**
     * Set channel address.
     * @param channelAddress the channel address to set
     */
    public void setChannelAddress(String channelAddress) {
        this.channelAddress = channelAddress;
    }

    /**
     * Get template ID.
     * @return the template ID
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * Set template ID.
     * @param templateId the template ID to set
     */
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    /**
     * Get template variables as JSON string.
     * @return the template variables
     */
    public String getTemplateVariables() {
        return templateVariables;
    }

    /**
     * Set template variables as JSON string.
     * @param templateVariables the template variables to set
     */
    public void setTemplateVariables(String templateVariables) {
        this.templateVariables = templateVariables;
    }

    /**
     * Get delivery status.
     * @return the delivery status
     */
    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    /**
     * Set delivery status.
     * @param deliveryStatus the delivery status to set
     */
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    /**
     * Get retry count.
     * @return the retry count
     */
    public Integer getRetryCount() {
        return retryCount;
    }

    /**
     * Set retry count.
     * @param retryCount the retry count to set
     */
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * Get maximum retries.
     * @return the maximum retries
     */
    public Integer getMaxRetries() {
        return maxRetries;
    }

    /**
     * Set maximum retries.
     * @param maxRetries the maximum retries to set
     */
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Get notification priority.
     * @return the priority level
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * Set notification priority.
     * @param priority the priority level to set
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * Get creation timestamp.
     * @return the created timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Set creation timestamp.
     * @param createdAt the created timestamp to set
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get sent timestamp.
     * @return the sent timestamp
     */
    public LocalDateTime getSentAt() {
        return sentAt;
    }

    /**
     * Set sent timestamp.
     * @param sentAt the sent timestamp to set
     */
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    /**
     * Get delivered timestamp.
     * @return the delivered timestamp
     */
    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    /**
     * Set delivered timestamp.
     * @param deliveredAt the delivered timestamp to set
     */
    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    /**
     * Get failure reason.
     * @return the failure reason
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * Set failure reason.
     * @param failureReason the failure reason to set
     */
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    /**
     * Get opt-out checked status.
     * @return the opt-out checked flag
     */
    public Boolean getOptOutChecked() {
        return optOutChecked;
    }

    /**
     * Set opt-out checked status.
     * @param optOutChecked the opt-out checked flag to set
     */
    public void setOptOutChecked(Boolean optOutChecked) {
        this.optOutChecked = optOutChecked;
    }

    // Utility methods for business logic support

    /**
     * Set template variables from a Map object.
     * Converts the Map to JSON string for database storage.
     * 
     * @param variablesMap the template variables as a Map
     * @throws RuntimeException if JSON serialization fails
     */
    public void setTemplateVariablesFromMap(Map<String, Object> variablesMap) {
        if (variablesMap == null || variablesMap.isEmpty()) {
            this.templateVariables = null;
            return;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.templateVariables = mapper.writeValueAsString(variablesMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize template variables to JSON", e);
        }
    }

    /**
     * Get template variables as a Map object.
     * Converts the JSON string from database to Map for processing.
     * 
     * @return the template variables as a Map, or empty Map if none exist
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTemplateVariablesAsMap() {
        if (templateVariables == null || templateVariables.trim().isEmpty()) {
            return Map.of();
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(templateVariables, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize template variables from JSON", e);
        }
    }

    /**
     * Check if notification has exceeded maximum retry attempts.
     * Used by retry processing logic to determine if notification should be abandoned.
     * 
     * @return true if retry limit exceeded, false otherwise
     */
    public boolean hasExceededRetryLimit() {
        return retryCount != null && maxRetries != null && retryCount >= maxRetries;
    }

    /**
     * Check if notification is eligible for retry.
     * Determines if notification can be retried based on status and retry count.
     * 
     * @return true if eligible for retry, false otherwise
     */
    public boolean isEligibleForRetry() {
        return deliveryStatus == DeliveryStatus.FAILED && !hasExceededRetryLimit();
    }

    /**
     * Increment retry count and update status to RETRY.
     * Used when queueing notification for retry processing.
     */
    public void incrementRetryCount() {
        if (retryCount == null) {
            retryCount = 0;
        }
        retryCount++;
        
        if (hasExceededRetryLimit()) {
            deliveryStatus = DeliveryStatus.FAILED;
        } else {
            deliveryStatus = DeliveryStatus.RETRY;
        }
    }

    /**
     * Mark notification as sent with timestamp.
     * Updates status and timestamp when notification is sent to delivery provider.
     */
    public void markAsSent() {
        this.deliveryStatus = DeliveryStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.failureReason = null; // Clear any previous failure reason
    }

    /**
     * Mark notification as delivered with timestamp.
     * Updates status and timestamp when delivery confirmation is received.
     */
    public void markAsDelivered() {
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
        this.failureReason = null; // Clear any previous failure reason
    }

    /**
     * Mark notification as failed with reason and timestamp.
     * Updates status and records failure information for analysis and retry processing.
     * 
     * @param reason the failure reason description
     */
    public void markAsFailed(String reason) {
        this.deliveryStatus = DeliveryStatus.FAILED;
        this.failureReason = reason;
        // sentAt timestamp preserved for retry analysis
    }

    /**
     * Get calculated next retry time based on exponential backoff.
     * Calculates the next retry time using exponential backoff algorithm.
     * 
     * @return the next retry time
     */
    public LocalDateTime getNextRetryTime() {
        if (sentAt == null) {
            return LocalDateTime.now();
        }
        
        // Exponential backoff: 2^retryCount minutes
        long backoffMinutes = (long) Math.pow(2, retryCount != null ? retryCount : 0);
        return sentAt.plusMinutes(backoffMinutes);
    }

    /**
     * Check if notification is ready for retry based on backoff timing.
     * Determines if enough time has passed for the next retry attempt.
     * 
     * @return true if ready for retry, false otherwise
     */
    public boolean isReadyForRetry() {
        return isEligibleForRetry() && LocalDateTime.now().isAfter(getNextRetryTime());
    }

    /**
     * Get customer first name from associated Customer entity.
     * Convenience method for accessing customer information.
     * 
     * @return the customer's first name
     */
    public String getCustomerFirstName() {
        return customer != null ? customer.getFirstName() : null;
    }

    /**
     * Get customer last name from associated Customer entity.
     * Convenience method for accessing customer information.
     * 
     * @return the customer's last name
     */
    public String getCustomerLastName() {
        return customer != null ? customer.getLastName() : null;
    }

    /**
     * Get customer phone number from associated Customer entity.
     * Convenience method for SMS notification addressing.
     * 
     * @return the customer's primary phone number
     */
    public String getCustomerPhoneNumber() {
        return customer != null ? customer.getPhoneNumber1() : null;
    }

    /**
     * Validate notification for delivery readiness.
     * Comprehensive validation including channel address, template, and customer preferences.
     * 
     * @throws IllegalStateException if notification is not ready for delivery
     */
    public void validateForDelivery() {
        if (customer == null) {
            throw new IllegalStateException("Customer is required for notification delivery");
        }
        
        if (notificationType == null) {
            throw new IllegalStateException("Notification type is required for delivery");
        }
        
        if (channelAddress == null || channelAddress.trim().isEmpty()) {
            throw new IllegalStateException("Channel address is required for delivery");
        }
        
        // Validate channel address format based on notification type
        switch (notificationType) {
            case EMAIL:
                if (!isValidEmailAddress(channelAddress)) {
                    throw new IllegalStateException("Invalid email address format for EMAIL notification");
                }
                break;
            case SMS:
                if (!isValidPhoneNumber(channelAddress)) {
                    throw new IllegalStateException("Invalid phone number format for SMS notification");
                }
                break;
            case IN_APP:
                // In-app notifications use customer ID as channel address
                break;
            case PUSH:
                // Push notifications use device token as channel address
                break;
        }
        
        if (!optOutChecked) {
            throw new IllegalStateException("Customer opt-out preferences must be checked before delivery");
        }
    }

    /**
     * JPA lifecycle callback for validation before persisting a new notification.
     * Sets default values and performs validation checks.
     */
    @PrePersist
    public void validateBeforeInsert() {
        // Set default values if not provided
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        
        if (deliveryStatus == null) {
            deliveryStatus = DeliveryStatus.PENDING;
        }
        
        if (retryCount == null) {
            retryCount = 0;
        }
        
        if (maxRetries == null) {
            maxRetries = DEFAULT_MAX_RETRIES;
        }
        
        if (priority == null) {
            priority = DEFAULT_PRIORITY;
        }
        
        if (optOutChecked == null) {
            optOutChecked = false;
        }
        
        // Validate required fields
        validateRequiredFields();
        
        // Set default channel address if not provided
        setDefaultChannelAddress();
    }

    /**
     * JPA lifecycle callback for validation before updating an existing notification.
     * Performs validation checks while preserving existing values.
     */
    @PreUpdate
    public void validateBeforeUpdate() {
        validateRequiredFields();
    }

    /**
     * Validates required fields for notification processing.
     * Ensures all mandatory fields are present and valid.
     * 
     * @throws IllegalStateException if validation fails
     */
    private void validateRequiredFields() {
        if (customer == null) {
            throw new IllegalStateException("Customer is required");
        }
        
        if (notificationType == null) {
            throw new IllegalStateException("Notification type is required");
        }
        
        if (deliveryStatus == null) {
            throw new IllegalStateException("Delivery status is required");
        }
        
        if (priority == null || priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            throw new IllegalStateException("Priority must be between " + MIN_PRIORITY + " and " + MAX_PRIORITY);
        }
        
        if (retryCount == null || retryCount < 0) {
            throw new IllegalStateException("Retry count cannot be negative");
        }
        
        if (maxRetries == null || maxRetries < 0 || maxRetries > MAX_RETRY_LIMIT) {
            throw new IllegalStateException("Max retries must be between 0 and " + MAX_RETRY_LIMIT);
        }
    }

    /**
     * Sets default channel address based on notification type and customer information.
     * Automatically populates channel address from customer data when not explicitly provided.
     */
    private void setDefaultChannelAddress() {
        if (channelAddress == null || channelAddress.trim().isEmpty()) {
            if (customer != null) {
                switch (notificationType) {
                    case SMS:
                        channelAddress = customer.getPhoneNumber1();
                        break;
                    case IN_APP:
                        channelAddress = customer.getCustomerId().toString();
                        break;
                    case EMAIL:
                    case PUSH:
                        // These require explicit address configuration
                        break;
                }
            }
        }
    }

    /**
     * Validates email address format using basic regex pattern.
     * 
     * @param email the email address to validate
     * @return true if valid format, false otherwise
     */
    private boolean isValidEmailAddress(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Basic email validation pattern
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailPattern);
    }

    /**
     * Validates phone number format allowing various formats.
     * 
     * @param phone the phone number to validate
     * @return true if valid format, false otherwise
     */
    private boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        
        // Remove all non-digit characters
        String digits = phone.replaceAll("[^0-9]", "");
        
        // US phone numbers should have 10 or 11 digits (with or without country code)
        return digits.length() >= 10 && digits.length() <= 11;
    }

    /**
     * Custom equals method for proper entity comparison.
     * Uses notification ID as primary comparison field.
     * 
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Notification notification = (Notification) o;
        
        // Primary comparison by notification ID
        if (id != null && notification.id != null) {
            return Objects.equals(id, notification.id);
        }
        
        // Fallback comparison using customer and creation timestamp
        return Objects.equals(customer, notification.customer) &&
               Objects.equals(createdAt, notification.createdAt) &&
               Objects.equals(notificationType, notification.notificationType);
    }

    /**
     * Custom hash code method using Objects.hash() for consistency with equals().
     * 
     * @return hash code for the Notification entity
     */
    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(customer, createdAt, notificationType);
    }

    /**
     * Custom toString method with comprehensive information.
     * Provides detailed debugging information while maintaining readability.
     * 
     * @return string representation of the Notification entity
     */
    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", customerId=" + getCustomerId() +
                ", notificationType=" + notificationType +
                ", channelAddress='" + channelAddress + '\'' +
                ", templateId='" + templateId + '\'' +
                ", deliveryStatus=" + deliveryStatus +
                ", retryCount=" + retryCount +
                ", maxRetries=" + maxRetries +
                ", priority=" + priority +
                ", createdAt=" + createdAt +
                ", sentAt=" + sentAt +
                ", deliveredAt=" + deliveredAt +
                ", failureReason='" + failureReason + '\'' +
                ", optOutChecked=" + optOutChecked +
                ", hasTemplateVariables=" + (templateVariables != null && !templateVariables.trim().isEmpty()) +
                '}';
    }
}