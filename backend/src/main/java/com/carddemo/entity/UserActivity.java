/*
 * CardDemo - Credit Card Management System
 * 
 * User Activity Entity
 * 
 * This JPA entity represents user activity tracking records for comprehensive
 * audit trail and administrative reporting within the CardDemo system.
 * 
 * The entity maps to the user_activity PostgreSQL table and supports
 * tracking user login patterns, session durations, and system usage
 * analytics for administrative dashboards and compliance reporting.
 * 
 * Key Features:
 * - Comprehensive user session tracking
 * - Activity type classification and filtering
 * - Date-based activity analysis
 * - Session duration metrics
 * - IP address and user agent tracking
 * - High-performance query optimization
 * 
 * @since 1.0
 * @version 1.0
 */
package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity class representing user activity records for audit and reporting.
 * 
 * This entity supports comprehensive user activity tracking including login events,
 * transaction activities, system usage patterns, and session analytics. The data
 * supports administrative reporting requirements for security monitoring,
 * user behavior analysis, and system capacity planning.
 * 
 * Database Table: user_activity
 * Primary Key: activity_id (auto-generated)
 * 
 * Index Strategy:
 * - Composite index on (user_id, activity_date) for user-specific reporting
 * - Composite index on (activity_type, activity_date) for type-based analysis
 * - Index on activity_timestamp for chronological queries
 * - Index on session_duration_minutes for duration-based filtering
 * 
 * Relationships:
 * - Many-to-one relationship with User entity through user_id
 * - Supports user-specific activity analysis and reporting
 * 
 * Field Mapping:
 * - All temporal fields use LocalDateTime for timestamp precision
 * - Activity dates use LocalDate for efficient date-range queries
 * - String fields include length constraints matching business requirements
 * - Session duration stored as integer minutes for calculation efficiency
 */
@Entity
@Table(name = "user_activity", indexes = {
    @Index(name = "idx_user_activity_user_date", columnList = "user_id, activity_date"),
    @Index(name = "idx_user_activity_type_date", columnList = "activity_type, activity_date"),
    @Index(name = "idx_user_activity_timestamp", columnList = "activity_timestamp"),
    @Index(name = "idx_user_activity_duration", columnList = "session_duration_minutes")
})
public class UserActivity {

    /**
     * Primary key for user activity records.
     * Auto-generated using database identity column for optimal performance.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long id;

    /**
     * User identifier for activity tracking.
     * Links to the User entity for comprehensive user analysis.
     * Required field for all activity records.
     */
    @NotNull
    @Size(max = 8, message = "User ID cannot exceed 8 characters")
    @Column(name = "user_id", nullable = false, length = 8)
    private String userId;

    /**
     * Classification of user activity type.
     * 
     * Supported Activity Types:
     * - LOGIN: User authentication events
     * - LOGOUT: User session termination events  
     * - TRANSACTION: Financial transaction processing
     * - VIEW: Data access and viewing operations
     * - UPDATE: Data modification operations
     * - ADMIN: Administrative function access
     * - REPORT: Report generation activities
     * 
     * Used for activity filtering and type-based analytics.
     */
    @NotNull
    @Size(max = 20, message = "Activity type cannot exceed 20 characters")
    @Column(name = "activity_type", nullable = false, length = 20)
    private String activityType;

    /**
     * Date component of activity for efficient date-range queries.
     * Extracted from activity_timestamp for optimized filtering
     * and daily/monthly activity analysis.
     */
    @NotNull
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    /**
     * Precise timestamp of activity occurrence.
     * Provides full temporal information for detailed audit trails
     * and chronological activity analysis.
     */
    @NotNull
    @Column(name = "activity_timestamp", nullable = false)
    private LocalDateTime activityTimestamp;

    /**
     * Session duration in minutes for user engagement analysis.
     * Calculated from login to logout or session timeout.
     * Used for session timeout policy enforcement and
     * user behavior pattern analysis.
     */
    @Min(value = 0, message = "Session duration must be non-negative")
    @Max(value = 1440, message = "Session duration cannot exceed 24 hours (1440 minutes)")
    @Column(name = "session_duration_minutes")
    private Integer sessionDurationMinutes;

    /**
     * IP address from which the activity originated.
     * Supports security monitoring and geographic analysis.
     * Stored as string to support both IPv4 and IPv6 addresses.
     */
    @Size(max = 45, message = "IP address cannot exceed 45 characters")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string for browser/application identification.
     * Supports security analysis and application usage patterns.
     */
    @Size(max = 255, message = "User agent cannot exceed 255 characters")
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /**
     * Detailed description of the activity performed.
     * Provides context for audit trails and detailed analysis.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Resource or screen accessed during the activity.
     * Supports page/function-level usage analytics.
     */
    @Size(max = 100, message = "Resource accessed cannot exceed 100 characters")
    @Column(name = "resource_accessed", length = 100)
    private String resourceAccessed;

    /**
     * Default constructor for JPA.
     */
    public UserActivity() {
    }

    /**
     * Constructor for creating activity records with essential fields.
     * 
     * @param userId The user identifier
     * @param activityType The type of activity
     * @param activityTimestamp The timestamp of the activity
     */
    public UserActivity(String userId, String activityType, LocalDateTime activityTimestamp) {
        this.userId = userId;
        this.activityType = activityType;
        this.activityTimestamp = activityTimestamp;
        this.activityDate = activityTimestamp != null ? activityTimestamp.toLocalDate() : null;
    }

    /**
     * Pre-persist callback to ensure activityDate is set.
     * Automatically extracts date from timestamp if not explicitly set.
     */
    @PrePersist
    @PreUpdate
    protected void updateActivityDate() {
        if (activityTimestamp != null && activityDate == null) {
            activityDate = activityTimestamp.toLocalDate();
        }
    }

    // Getter and Setter methods

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(LocalDate activityDate) {
        this.activityDate = activityDate;
    }

    public LocalDateTime getActivityTimestamp() {
        return activityTimestamp;
    }

    public void setActivityTimestamp(LocalDateTime activityTimestamp) {
        this.activityTimestamp = activityTimestamp;
        // Automatically update activityDate when timestamp is set
        if (activityTimestamp != null) {
            this.activityDate = activityTimestamp.toLocalDate();
        }
    }

    public Integer getSessionDurationMinutes() {
        return sessionDurationMinutes;
    }

    public void setSessionDurationMinutes(Integer sessionDurationMinutes) {
        this.sessionDurationMinutes = sessionDurationMinutes;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResourceAccessed() {
        return resourceAccessed;
    }

    public void setResourceAccessed(String resourceAccessed) {
        this.resourceAccessed = resourceAccessed;
    }

    /**
     * Equality comparison based on primary key.
     * 
     * @param obj The object to compare
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserActivity that = (UserActivity) obj;
        return Objects.equals(id, that.id);
    }

    /**
     * Hash code generation based on primary key.
     * 
     * @return hash code for the entity
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * String representation for debugging and logging.
     * 
     * @return string representation of the entity
     */
    @Override
    public String toString() {
        return "UserActivity{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", activityType='" + activityType + '\'' +
                ", activityDate=" + activityDate +
                ", activityTimestamp=" + activityTimestamp +
                ", sessionDurationMinutes=" + sessionDurationMinutes +
                ", ipAddress='" + ipAddress + '\'' +
                ", description='" + description + '\'' +
                ", resourceAccessed='" + resourceAccessed + '\'' +
                '}';
    }
}