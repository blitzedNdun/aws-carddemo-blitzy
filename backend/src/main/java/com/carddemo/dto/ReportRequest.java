/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for report generation operations, corresponding to CORPT00C program inputs.
 * 
 * This class encapsulates all report generation parameters needed for the CardDemo
 * reporting functionality, including report type selection, date range specification,
 * and output format preferences. It maintains compatibility with the original COBOL
 * report generation logic while providing modern validation and serialization capabilities.
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    /**
     * Type of report to generate.
     * Valid values: DAILY, MONTHLY, CUSTOM, STATEMENT
     */
    @JsonProperty("reportType")
    @NotNull(message = "Report type is required")
    @Size(max = 20, message = "Report type must not exceed 20 characters")
    private String reportType;

    /**
     * Start date for date range reports.
     * Required for CUSTOM report type.
     */
    @JsonProperty("startDate")
    private LocalDate startDate;

    /**
     * End date for date range reports.
     * Required for CUSTOM report type.
     */
    @JsonProperty("endDate")
    private LocalDate endDate;

    /**
     * Account ID for account-specific reports.
     * Optional filter for generating reports for specific accounts.
     */
    @JsonProperty("accountId")
    @Size(max = 11, message = "Account ID must not exceed 11 characters")
    private String accountId;

    /**
     * Output format for the report.
     * Valid values: PDF, CSV, TEXT
     */
    @JsonProperty("outputFormat")
    @Size(max = 10, message = "Output format must not exceed 10 characters")
    private String outputFormat;

    /**
     * User ID requesting the report.
     * Used for security validation and audit logging.
     */
    @JsonProperty("userId")
    @Size(max = 8, message = "User ID must not exceed 8 characters")
    private String userId;

    /**
     * Additional parameters for custom report configurations.
     */
    @JsonProperty("reportParameters")
    private String reportParameters;
}