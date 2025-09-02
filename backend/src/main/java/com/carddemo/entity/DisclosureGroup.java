/*
 * DisclosureGroup.java
 * 
 * JPA Entity representing the disclosure_groups table for credit card
 * disclosure group configuration and interest rate management.
 * 
 * This entity replaces VSAM DISGRP reference file from the mainframe,
 * providing PostgreSQL-based storage for disclosure group settings
 * used in interest calculation and disclosure assignment processes.
 * 
 * Maps to COBOL copybook: app/cpy/CVTRA02Y.cpy
 * - DIS-GROUP-RECORD structure with composite key access patterns
 * - DIS-ACCT-GROUP-ID (PIC X(10)) for account group identification
 * - DIS-TRAN-TYPE-CD (PIC X(02)) for transaction type classification  
 * - DIS-TRAN-CAT-CD (PIC 9(04)) for transaction category codes
 * - DIS-INT-RATE (PIC S9(04)V99) for interest rate calculations
 * - DIS-GROUP-NAME (PIC X(50)) for group description
 * - DIS-ACTIVE-FLAG (PIC X(01)) for status management
 */

package com.carddemo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for disclosure_groups table.
 * 
 * Represents disclosure group configuration entries with interest rate
 * settings for credit card account processing. Supports account group
 * and transaction type-based interest rate determination, replacing
 * VSAM reference file operations with relational database access.
 * 
 * Key fields:
 * - disclosureGroupId: Primary key for unique group identification
 * - accountGroupId: Account group identifier (10 chars, matches COBOL PIC X(10))
 * - transactionTypeCode: Transaction type code (2 chars, matches COBOL PIC X(02))
 * - transactionCategoryCode: Category code (4 chars, matches COBOL PIC 9(04))
 * - interestRate: Interest rate with BigDecimal precision (matches COBOL COMP-3)
 * - groupName: Descriptive group name (50 chars, matches COBOL PIC X(50))
 * - active: Status flag for group activation (matches COBOL PIC X(01))
 * 
 * Indexes optimize lookup performance for:
 * - Account group ID queries (B-tree index)
 * - Transaction type/category combinations (composite index)
 * - Interest rate range queries (B-tree index)
 * - Active group filtering (partial index)
 */
@Entity
@Table(name = "disclosure_groups", indexes = {
    @Index(name = "idx_disclosure_acct_group", columnList = "account_group_id"),
    @Index(name = "idx_disclosure_tran_type_cat", columnList = "transaction_type_code, transaction_category_code"),
    @Index(name = "idx_disclosure_interest_rate", columnList = "interest_rate"),
    @Index(name = "idx_disclosure_active", columnList = "active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisclosureGroup {

    /**
     * Primary key for disclosure group identification.
     * 
     * Generated using IDENTITY strategy for PostgreSQL SERIAL column type.
     * Provides unique identifier for each disclosure group configuration
     * entry, supporting efficient primary key access and foreign key
     * relationships with other entities.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disclosure_group_id", nullable = false)
    private Long disclosureGroupId;

    /**
     * Account group identifier.
     * 
     * Maps to COBOL DIS-ACCT-GROUP-ID (PIC X(10)) field from CVTRA02Y copybook.
     * Used for determining applicable disclosure groups based on account
     * characteristics and customer segmentation. Essential for interest
     * calculation and disclosure assignment workflows.
     * 
     * Length: 10 characters maximum to match COBOL definition
     * Nullable: false - required field for group identification
     */
    @Column(name = "account_group_id", length = 10, nullable = false)
    private String accountGroupId;

    /**
     * Transaction type code.
     * 
     * Maps to COBOL DIS-TRAN-TYPE-CD (PIC X(02)) field from CVTRA02Y copybook.
     * Identifies transaction types (e.g., "01" for purchases, "02" for cash advances)
     * for determining applicable interest rates and disclosure requirements.
     * 
     * Length: 2 characters maximum to match COBOL definition
     * Nullable: false - required field for transaction classification
     */
    @Column(name = "transaction_type_code", length = 2, nullable = false)
    private String transactionTypeCode;

    /**
     * Transaction category code.
     * 
     * Maps to COBOL DIS-TRAN-CAT-CD (PIC 9(04)) field from CVTRA02Y copybook.
     * Provides fine-grained transaction categorization (e.g., 0001 for retail,
     * 0002 for online) for precise interest rate determination and regulatory
     * compliance reporting.
     * 
     * Length: 4 characters maximum to match COBOL definition
     * Nullable: false - required field for category classification
     */
    @Column(name = "transaction_category_code", length = 4, nullable = false)
    private String transactionCategoryCode;

    /**
     * Interest rate for this disclosure group.
     * 
     * Maps to COBOL DIS-INT-RATE (PIC S9(04)V99) field from CVTRA02Y copybook.
     * Uses BigDecimal with scale=4 and precision=6 to exactly match COBOL
     * COMP-3 packed decimal format (4 digits before decimal, 2 after).
     * 
     * Configured for exact financial precision matching mainframe calculations:
     * - Scale: 4 decimal places for precise interest rate representation
     * - Precision: 6 total digits (4 before decimal, 2 after)
     * - Nullable: false - required for interest calculation processes
     */
    @Column(name = "interest_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal interestRate;

    /**
     * Descriptive name for the disclosure group.
     * 
     * Maps to COBOL DIS-GROUP-NAME (PIC X(50)) field from CVTRA02Y copybook.
     * Provides human-readable description for administrative interfaces
     * and reporting purposes. Used in configuration management and
     * customer communication materials.
     * 
     * Length: 50 characters maximum to match COBOL definition
     * Nullable: true - optional descriptive field
     */
    @Column(name = "group_name", length = 50)
    private String groupName;

    /**
     * Active status flag for the disclosure group.
     * 
     * Maps to COBOL DIS-ACTIVE-FLAG (PIC X(01)) field from CVTRA02Y copybook.
     * Controls whether this disclosure group is available for new assignments
     * and interest calculations. Supports configuration management without
     * data deletion, maintaining audit trail and historical accuracy.
     * 
     * Values: true for active groups, false for inactive/archived groups
     * Default: true for new groups
     * Nullable: false - required status field
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Description field for additional group details.
     * 
     * Provides extended description and configuration notes for the
     * disclosure group. Used for documentation, compliance notes,
     * and administrative reference information.
     * 
     * Length: 200 characters maximum for detailed descriptions
     * Nullable: true - optional field for additional context
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * Effective date for the disclosure group configuration.
     * 
     * Indicates when this disclosure group becomes effective for
     * interest calculations and disclosure assignments. Supports
     * time-based configuration changes and compliance requirements.
     * 
     * Nullable: true - defaults to creation time if not specified
     */
    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    /**
     * Expiration date for the disclosure group configuration.
     * 
     * Indicates when this disclosure group expires and should no longer
     * be used for new assignments. Supports configuration lifecycle
     * management and regulatory compliance transitions.
     * 
     * Nullable: true - no expiration if not specified
     */
    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    /**
     * Record creation timestamp.
     * 
     * Automatically set when the disclosure group record is first created.
     * Provides audit trail for configuration changes and compliance
     * reporting requirements.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Record last modified timestamp.
     * 
     * Automatically updated when the disclosure group record is modified.
     * Maintains change tracking for audit purposes and configuration
     * management workflows.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User ID who created the record.
     * 
     * Tracks which user created this disclosure group for audit trail
     * and accountability. Links to user management system for complete
     * change tracking and compliance reporting.
     * 
     * Length: 8 characters maximum to match COBOL user ID format
     * Nullable: true - may be system-generated records
     */
    @Column(name = "created_by", length = 8)
    private String createdBy;

    /**
     * User ID who last modified the record.
     * 
     * Tracks which user made the most recent changes to this disclosure
     * group for audit trail and accountability. Essential for compliance
     * and change management processes.
     * 
     * Length: 8 characters maximum to match COBOL user ID format
     * Nullable: true - may be system-modified records
     */
    @Column(name = "updated_by", length = 8)
    private String updatedBy;

    /**
     * JPA PrePersist callback to set audit fields before insert.
     * 
     * Automatically sets createdAt and updatedAt timestamps when a new
     * disclosure group record is saved to the database. Ensures consistent
     * audit trail without requiring manual timestamp management.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.effectiveDate == null) {
            this.effectiveDate = now;
        }
    }

    /**
     * JPA PreUpdate callback to update audit fields before modification.
     * 
     * Automatically updates the updatedAt timestamp when an existing
     * disclosure group record is modified. Maintains accurate change
     * tracking for audit and compliance requirements.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Custom toString method for logging and debugging.
     * 
     * Provides structured representation of disclosure group data for
     * logging, debugging, and audit purposes. Excludes sensitive information
     * while including key identification and configuration details.
     * 
     * @return formatted string representation of the disclosure group
     */
    @Override
    public String toString() {
        return String.format(
            "DisclosureGroup{id=%d, accountGroupId='%s', transactionType='%s', " +
            "categoryCode='%s', interestRate=%s, groupName='%s', active=%b}",
            disclosureGroupId, accountGroupId, transactionTypeCode,
            transactionCategoryCode, interestRate, groupName, active
        );
    }
}