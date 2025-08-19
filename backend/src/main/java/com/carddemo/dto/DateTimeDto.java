/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.FormatUtil;
import com.carddemo.util.Constants;

/**
 * Data Transfer Object for date and time representation matching COBOL date/time formats.
 * 
 * This DTO maps the CSDAT01Y copybook date/time structure, providing seamless conversion
 * between COBOL date/time formats and Java time objects. It supports the following COBOL patterns:
 * - WS-CURDATE (CCYYMMDD format): 8-character numeric date representation
 * - WS-CURTIME (HHMMSS format): 6-character numeric time representation  
 * - WS-CURDATE-MM-DD-YY: Display format "MM/DD/YY"
 * - WS-CURTIME-HH-MM-SS: Display format "HH:MM:SS"
 * - WS-TIMESTAMP: Full timestamp format "YYYY-MM-DD HH:MM:SS.NNNNNN"
 * 
 * Key Features:
 * - Comprehensive date/time field mapping from COBOL copybook structures
 * - Bidirectional conversion between COBOL and Java date/time formats
 * - Validation for CCYYMMDD and HHMMSS format patterns
 * - Support for display formatting matching COBOL screen layouts
 * - JSON serialization with proper date format handling
 * - Integration with DateConversionUtil for COBOL date processing
 * 
 * The class preserves exact compatibility with COBOL date handling while providing
 * modern Java time API integration for enhanced functionality and maintainability.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateTimeDto {

    /**
     * Date in CCYYMMDD format (8 characters).
     * Maps to COBOL WS-CURDATE field (PIC 9(08)).
     * Example: "20231225" for December 25, 2023
     */
    @Pattern(regexp = "^\\d{8}$", message = "Date must be in CCYYMMDD format (8 digits)")
    @JsonFormat(pattern = "yyyyMMdd")
    private String date;

    /**
     * Time in HHMMSS format (6 characters).
     * Maps to COBOL WS-CURTIME field (PIC 9(06)) - excluding centiseconds.
     * Example: "143045" for 14:30:45 (2:30:45 PM)
     */
    @Pattern(regexp = "^\\d{6}$", message = "Time must be in HHMMSS format (6 digits)")
    @JsonFormat(pattern = "HHmmss")
    private String time;

    /**
     * Date in display format for user interfaces.
     * Maps to COBOL WS-CURDATE-MM-DD-YY structure.
     * Example: "12/25/23" for December 25, 2023
     */
    private String displayDate;

    /**
     * Time in display format for user interfaces.
     * Maps to COBOL WS-CURTIME-HH-MM-SS structure.
     * Example: "14:30:45" for 2:30:45 PM
     */
    private String displayTime;

    /**
     * Complete timestamp combining date and time.
     * Maps to COBOL WS-TIMESTAMP field.
     * Example: "2023-12-25 14:30:45.123456"
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;

    // Date/time formatters matching COBOL patterns
    private static final DateTimeFormatter CCYYMMDD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HHMMSS_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Converts the CCYYMMDD date field to a LocalDate object.
     * Uses DateConversionUtil for validation and parsing.
     * 
     * @return LocalDate representation of the date field, or null if date is null/invalid
     */
    public LocalDate toLocalDate() {
        if (date == null || date.trim().isEmpty()) {
            return null;
        }
        
        try {
            return DateConversionUtil.parseDate(date);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Converts the HHMMSS time field to a LocalTime object.
     * 
     * @return LocalTime representation of the time field, or null if time is null/invalid
     */
    public LocalTime toLocalTime() {
        if (time == null || time.trim().isEmpty()) {
            return null;
        }
        
        if (!validateHHMMSS(time)) {
            return null;
        }
        
        try {
            return LocalTime.parse(time, HHMMSS_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Combines date and time fields to create a LocalDateTime object.
     * 
     * @return LocalDateTime representation, or null if either date or time is invalid
     */
    public LocalDateTime toLocalDateTime() {
        LocalDate localDate = toLocalDate();
        LocalTime localTime = toLocalTime();
        
        if (localDate == null || localTime == null) {
            return null;
        }
        
        return LocalDateTime.of(localDate, localTime);
    }

    /**
     * Sets the date field from a LocalDate object.
     * Updates both the CCYYMMDD date field and display date field.
     * 
     * @param localDate the LocalDate to convert to COBOL format
     * @return this DateTimeDto instance for method chaining
     */
    public DateTimeDto fromLocalDate(LocalDate localDate) {
        if (localDate == null) {
            this.date = null;
            this.displayDate = null;
        } else {
            this.date = DateConversionUtil.formatToCobol(localDate);
            this.displayDate = FormatUtil.formatDate(localDate.atStartOfDay());
        }
        return this;
    }

    /**
     * Sets the time field from a LocalTime object.
     * Updates both the HHMMSS time field and display time field.
     * 
     * @param localTime the LocalTime to convert to COBOL format
     * @return this DateTimeDto instance for method chaining
     */
    public DateTimeDto fromLocalTime(LocalTime localTime) {
        if (localTime == null) {
            this.time = null;
            this.displayTime = null;
        } else {
            this.time = localTime.format(HHMMSS_FORMATTER);
            this.displayTime = localTime.format(DISPLAY_TIME_FORMATTER);
        }
        return this;
    }

    /**
     * Sets both date and time fields from a LocalDateTime object.
     * Updates all date/time fields including the timestamp.
     * 
     * @param localDateTime the LocalDateTime to convert to COBOL formats
     * @return this DateTimeDto instance for method chaining
     */
    public DateTimeDto fromLocalDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            this.date = null;
            this.time = null;
            this.displayDate = null;
            this.displayTime = null;
            this.timestamp = null;
        } else {
            this.date = localDateTime.format(CCYYMMDD_FORMATTER);
            this.time = localDateTime.format(HHMMSS_FORMATTER);
            this.displayDate = FormatUtil.formatDate(localDateTime);
            this.displayTime = localDateTime.format(DISPLAY_TIME_FORMATTER);
            this.timestamp = localDateTime;
        }
        return this;
    }

    /**
     * Validates a date string in CCYYMMDD format using COBOL validation rules.
     * Delegates to DateConversionUtil for comprehensive validation.
     * 
     * @param dateString the date string to validate
     * @return true if the date is valid CCYYMMDD format, false otherwise
     */
    public static boolean validateCCYYMMDD(String dateString) {
        return DateConversionUtil.validateDate(dateString);
    }

    /**
     * Validates a time string in HHMMSS format.
     * Checks for proper 6-digit format and valid time ranges.
     * 
     * @param timeString the time string to validate
     * @return true if the time is valid HHMMSS format, false otherwise
     */
    public static boolean validateHHMMSS(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return false;
        }
        
        String trimmedTime = timeString.trim();
        
        // Check length (must be exactly 6 characters)
        if (trimmedTime.length() != 6) {
            return false;
        }
        
        // Check if all characters are numeric
        if (!trimmedTime.matches("^\\d{6}$")) {
            return false;
        }
        
        try {
            // Extract hour, minute, second components
            int hour = Integer.parseInt(trimmedTime.substring(0, 2));
            int minute = Integer.parseInt(trimmedTime.substring(2, 4));
            int second = Integer.parseInt(trimmedTime.substring(4, 6));
            
            // Validate hour (0-23)
            if (hour < 0 || hour > 23) {
                return false;
            }
            
            // Validate minute (0-59)
            if (minute < 0 || minute > 59) {
                return false;
            }
            
            // Validate second (0-59)
            if (second < 0 || second > 59) {
                return false;
            }
            
            // Final validation using LocalTime parsing
            LocalTime.of(hour, minute, second);
            return true;
            
        } catch (NumberFormatException | java.time.DateTimeException e) {
            return false;
        }
    }

    /**
     * Formats all fields for display using COBOL display patterns.
     * Updates displayDate and displayTime based on current date and time values.
     * 
     * @return this DateTimeDto instance for method chaining
     */
    public DateTimeDto formatForDisplay() {
        // Format display date if date is present
        if (date != null && validateCCYYMMDD(date)) {
            LocalDate localDate = toLocalDate();
            if (localDate != null) {
                this.displayDate = FormatUtil.formatDate(localDate.atStartOfDay());
            }
        }
        
        // Format display time if time is present
        if (time != null && validateHHMMSS(time)) {
            LocalTime localTime = toLocalTime();
            if (localTime != null) {
                this.displayTime = localTime.format(DISPLAY_TIME_FORMATTER);
            }
        }
        
        return this;
    }

    /**
     * Parses date and time from COBOL format strings.
     * Supports various COBOL date/time representations.
     * 
     * @param cobolDate COBOL date string in CCYYMMDD format
     * @param cobolTime COBOL time string in HHMMSS format
     * @return new DateTimeDto instance with parsed values
     */
    public static DateTimeDto parseFromCobol(String cobolDate, String cobolTime) {
        DateTimeDto dto = new DateTimeDto();
        
        if (cobolDate != null && validateCCYYMMDD(cobolDate)) {
            dto.setDate(cobolDate);
            LocalDate localDate = DateConversionUtil.parseDate(cobolDate);
            dto.setDisplayDate(FormatUtil.formatDate(localDate.atStartOfDay()));
        }
        
        if (cobolTime != null && validateHHMMSS(cobolTime)) {
            dto.setTime(cobolTime);
            LocalTime localTime = LocalTime.parse(cobolTime, HHMMSS_FORMATTER);
            dto.setDisplayTime(localTime.format(DISPLAY_TIME_FORMATTER));
        }
        
        // Create timestamp if both date and time are valid
        if (dto.toLocalDate() != null && dto.toLocalTime() != null) {
            dto.setTimestamp(LocalDateTime.of(dto.toLocalDate(), dto.toLocalTime()));
        }
        
        return dto;
    }

    /**
     * Gets current date and time in COBOL formats.
     * Creates a DateTimeDto with current system date/time.
     * 
     * @return new DateTimeDto instance with current date and time
     */
    public static DateTimeDto getCurrentDateTime() {
        return new DateTimeDto().fromLocalDateTime(LocalDateTime.now());
    }

    /**
     * Checks if this DateTimeDto represents a valid date/time combination.
     * 
     * @return true if both date and time fields are valid, false otherwise
     */
    public boolean isValid() {
        return (date == null || validateCCYYMMDD(date)) && 
               (time == null || validateHHMMSS(time));
    }

    /**
     * Gets the date in CCYYMMDD format.
     * 
     * @return the date string in CCYYMMDD format
     */
    public String getDate() {
        return date;
    }

    /**
     * Sets the date in CCYYMMDD format.
     * 
     * @param date the date string in CCYYMMDD format
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Gets the time in HHMMSS format.
     * 
     * @return the time string in HHMMSS format
     */
    public String getTime() {
        return time;
    }

    /**
     * Sets the time in HHMMSS format.
     * 
     * @param time the time string in HHMMSS format
     */
    public void setTime(String time) {
        this.time = time;
    }

    /**
     * Gets the display formatted date.
     * 
     * @return the display date string
     */
    public String getDisplayDate() {
        return displayDate;
    }

    /**
     * Sets the display formatted date.
     * 
     * @param displayDate the display date string
     */
    public void setDisplayDate(String displayDate) {
        this.displayDate = displayDate;
    }

    /**
     * Gets the display formatted time.
     * 
     * @return the display time string
     */
    public String getDisplayTime() {
        return displayTime;
    }

    /**
     * Sets the display formatted time.
     * 
     * @param displayTime the display time string
     */
    public void setDisplayTime(String displayTime) {
        this.displayTime = displayTime;
    }

    /**
     * Gets the complete timestamp.
     * 
     * @return the LocalDateTime timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the complete timestamp.
     * 
     * @param timestamp the LocalDateTime timestamp
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateTimeDto that = (DateTimeDto) o;
        return Objects.equals(date, that.date) &&
               Objects.equals(time, that.time) &&
               Objects.equals(displayDate, that.displayDate) &&
               Objects.equals(displayTime, that.displayTime) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, time, displayDate, displayTime, timestamp);
    }

    @Override
    public String toString() {
        return "DateTimeDto{" +
                "date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", displayDate='" + displayDate + '\'' +
                ", displayTime='" + displayTime + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}