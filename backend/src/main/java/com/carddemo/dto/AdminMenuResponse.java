/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import java.util.List;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing the administrative menu response from COADM01 screen.
 * 
 * This DTO maps the BMS admin menu structure from COADM01.bms, providing administrative
 * functionality for the CardDemo application. Unlike the standard main menu, this admin
 * menu includes system management functions and operational status information.
 * 
 * The admin menu response contains:
 * - List of admin-specific menu options (user management, batch operations, system config)
 * - Current system operational status
 * - Timestamp of last batch job execution
 * - Count of currently active users in the system
 * 
 * This maintains functional parity with the original COBOL/CICS admin menu functionality
 * while providing modern REST API data structure for React frontend consumption.
 * 
 * Admin menu options include:
 * - User account management (create, modify, delete)
 * - Batch job monitoring and control
 * - System configuration management
 * - Transaction monitoring and reporting
 * - Database maintenance utilities
 * - Security administration
 */
public class AdminMenuResponse {

    /**
     * List of administrative menu options available to the current user.
     * Maps to the OPTN001-OPTN012 fields in COADM01.bms layout.
     * 
     * Admin-specific options include:
     * - User Management (transaction codes starting with 'UA')
     * - Batch Operations (transaction codes starting with 'BA')
     * - System Configuration (transaction codes starting with 'SC')
     * - Transaction Monitoring (transaction codes starting with 'TM')
     * - Database Utilities (transaction codes starting with 'DB')
     * - Security Administration (transaction codes starting with 'SA')
     */
    private List<MenuOption> adminOptions;

    /**
     * Current system operational status.
     * Indicates the overall health and availability of the CardDemo system.
     * 
     * Possible values:
     * - "ONLINE" - System fully operational
     * - "BATCH_RUNNING" - Batch processing in progress, limited online access
     * - "MAINTENANCE" - System under maintenance, read-only mode
     * - "OFFLINE" - System unavailable
     * 
     * Maps to system status displays that would appear on the admin screen.
     */
    private String systemStatus;

    /**
     * Timestamp of the last completed batch job execution.
     * Provides administrators with information about when batch processing
     * last completed successfully.
     * 
     * This is critical for admin users to monitor batch job schedules and
     * identify any processing delays or failures. Used to display batch
     * status information on the admin menu screen.
     */
    private LocalDateTime lastBatchRun;

    /**
     * Count of currently active users in the system.
     * Provides administrators with real-time visibility into system usage.
     * 
     * Includes all logged-in users across all access levels:
     * - Regular users (customer service representatives)
     * - Admin users (system administrators)
     * - Batch users (automated processes)
     * 
     * Used for capacity monitoring and system performance tracking.
     */
    private Integer activeUsers;

    /**
     * Default constructor for framework instantiation.
     * Initializes with default values appropriate for admin menu display.
     */
    public AdminMenuResponse() {
        this.systemStatus = "ONLINE";
        this.activeUsers = 0;
    }

    /**
     * Constructor with all fields for complete initialization.
     * 
     * @param adminOptions List of admin menu options
     * @param systemStatus Current system operational status
     * @param lastBatchRun Timestamp of last batch completion
     * @param activeUsers Count of currently active users
     */
    public AdminMenuResponse(List<MenuOption> adminOptions, String systemStatus, 
                           LocalDateTime lastBatchRun, Integer activeUsers) {
        this.adminOptions = adminOptions;
        this.systemStatus = systemStatus;
        this.lastBatchRun = lastBatchRun;
        this.activeUsers = activeUsers;
    }

    /**
     * Gets the list of administrative menu options.
     * 
     * @return List of MenuOption objects representing admin functions
     */
    public List<MenuOption> getAdminOptions() {
        return adminOptions;
    }

    /**
     * Sets the list of administrative menu options.
     * 
     * @param adminOptions List of MenuOption objects for admin functions
     */
    public void setAdminOptions(List<MenuOption> adminOptions) {
        this.adminOptions = adminOptions;
    }

    /**
     * Gets the current system operational status.
     * 
     * @return String indicating system status (ONLINE, BATCH_RUNNING, MAINTENANCE, OFFLINE)
     */
    public String getSystemStatus() {
        return systemStatus;
    }

    /**
     * Sets the current system operational status.
     * 
     * @param systemStatus String indicating system status
     */
    public void setSystemStatus(String systemStatus) {
        this.systemStatus = systemStatus;
    }

    /**
     * Gets the timestamp of the last batch job completion.
     * 
     * @return LocalDateTime representing when last batch job completed
     */
    public LocalDateTime getLastBatchRun() {
        return lastBatchRun;
    }

    /**
     * Sets the timestamp of the last batch job completion.
     * 
     * @param lastBatchRun LocalDateTime representing batch completion time
     */
    public void setLastBatchRun(LocalDateTime lastBatchRun) {
        this.lastBatchRun = lastBatchRun;
    }

    /**
     * Gets the count of currently active users.
     * 
     * @return Integer count of active users across all access levels
     */
    public Integer getActiveUsers() {
        return activeUsers;
    }

    /**
     * Sets the count of currently active users.
     * 
     * @param activeUsers Integer count of active users
     */
    public void setActiveUsers(Integer activeUsers) {
        this.activeUsers = activeUsers;
    }

    /**
     * Checks equality based on all fields.
     * Two AdminMenuResponse objects are equal if all their fields match.
     * 
     * @param obj The object to compare with
     * @return True if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        AdminMenuResponse that = (AdminMenuResponse) obj;
        
        if (adminOptions != null ? !adminOptions.equals(that.adminOptions) : that.adminOptions != null) {
            return false;
        }
        if (systemStatus != null ? !systemStatus.equals(that.systemStatus) : that.systemStatus != null) {
            return false;
        }
        if (lastBatchRun != null ? !lastBatchRun.equals(that.lastBatchRun) : that.lastBatchRun != null) {
            return false;
        }
        return activeUsers != null ? activeUsers.equals(that.activeUsers) : that.activeUsers == null;
    }

    /**
     * Generates hash code based on all fields.
     * 
     * @return Hash code value
     */
    @Override
    public int hashCode() {
        int result = adminOptions != null ? adminOptions.hashCode() : 0;
        result = 31 * result + (systemStatus != null ? systemStatus.hashCode() : 0);
        result = 31 * result + (lastBatchRun != null ? lastBatchRun.hashCode() : 0);
        result = 31 * result + (activeUsers != null ? activeUsers.hashCode() : 0);
        return result;
    }

    /**
     * Provides string representation of the admin menu response.
     * Includes all fields for debugging and logging purposes.
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AdminMenuResponse{");
        sb.append("adminOptions=");
        
        if (adminOptions != null) {
            sb.append("[");
            for (int i = 0; i < adminOptions.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                MenuOption option = adminOptions.get(i);
                sb.append("Option{num=").append(option.getOptionNumber())
                  .append(", desc='").append(option.getDescription()).append("'")
                  .append(", code='").append(option.getTransactionCode()).append("'")
                  .append(", enabled=").append(option.getEnabled())
                  .append(", access='").append(option.getAccessLevel()).append("'}");
            }
            sb.append("]");
        } else {
            sb.append("null");
        }
        
        sb.append(", systemStatus='").append(systemStatus).append('\'');
        sb.append(", lastBatchRun=");
        
        if (lastBatchRun != null) {
            sb.append(lastBatchRun.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            sb.append("null");
        }
        
        sb.append(", activeUsers=").append(activeUsers);
        sb.append('}');
        return sb.toString();
    }
}