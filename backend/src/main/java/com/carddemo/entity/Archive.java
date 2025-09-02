package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Archive entity representing archived records in the CardDemo system.
 * Supports the archival policies documented in the database design section
 * with retention periods, legal hold management, and storage location tracking.
 * 
 * This entity manages archived data from various source tables including
 * transactions, customer data, account data, and card data according to
 * compliance requirements and retention policies.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "archive", 
       indexes = {
           @Index(name = "idx_archive_data_type", columnList = "data_type"),
           @Index(name = "idx_archive_retention_date", columnList = "retention_date"),
           @Index(name = "idx_archive_legal_hold", columnList = "legal_hold"),
           @Index(name = "idx_archive_date", columnList = "archive_date"),
           @Index(name = "idx_archive_storage_location", columnList = "storage_location")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Archive {

    /**
     * Primary key for the archive record.
     * Uses PostgreSQL BIGSERIAL for auto-generation.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    /**
     * Type of data being archived (e.g., "TRANSACTION", "CUSTOMER", "ACCOUNT", "CARD").
     * Maps to different source tables and determines retention policies.
     */
    @NotNull
    @Size(max = 50)
    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    /**
     * Reference ID of the original record in the source table.
     * Maintains traceability to the original data.
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "source_record_id", nullable = false, length = 100)
    private String sourceRecordId;

    /**
     * Name of the source table from which data was archived.
     * Supports multi-table archiving operations.
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "source_table_name", nullable = false, length = 100)
    private String sourceTableName;

    /**
     * Archived data content stored as compressed JSON or XML.
     * Contains the complete record data for restoration purposes.
     */
    @Lob
    @Column(name = "archived_data", columnDefinition = "TEXT")
    private String archivedData;

    /**
     * Date when the record was archived.
     * Used for audit trail and archival process tracking.
     */
    @NotNull
    @Column(name = "archive_date", nullable = false)
    private LocalDateTime archiveDate;

    /**
     * Date when the archived record becomes eligible for deletion.
     * Calculated based on data type retention policies.
     */
    @NotNull
    @Column(name = "retention_date", nullable = false)
    private LocalDate retentionDate;

    /**
     * Legal hold status preventing deletion for litigation or regulatory purposes.
     * When true, prevents automated deletion regardless of retention date.
     */
    @NotNull
    @Column(name = "legal_hold", nullable = false)
    @Builder.Default
    private Boolean legalHold = false;

    /**
     * Date when legal hold was applied or last modified.
     * Tracks legal hold lifecycle for compliance reporting.
     */
    @Column(name = "legal_hold_date")
    private LocalDateTime legalHoldDate;

    /**
     * Reason for legal hold (e.g., "LITIGATION", "REGULATORY_REQUEST", "AUDIT").
     * Provides context for legal hold decisions.
     */
    @Size(max = 255)
    @Column(name = "legal_hold_reason", length = 255)
    private String legalHoldReason;

    /**
     * Storage location identifier for the archived data.
     * Supports distributed storage and efficient retrieval.
     */
    @Size(max = 100)
    @Column(name = "storage_location", length = 100)
    private String storageLocation;

    /**
     * Compression method used for archived data.
     * Supports storage optimization strategies.
     */
    @Size(max = 50)
    @Column(name = "compression_method", length = 50)
    private String compressionMethod;

    /**
     * Original size of the data before archiving (in bytes).
     * Supports compression ratio analysis and capacity planning.
     */
    @Column(name = "original_size_bytes")
    private Long originalSizeBytes;

    /**
     * Compressed size of the archived data (in bytes).
     * Supports storage utilization tracking.
     */
    @Column(name = "compressed_size_bytes")
    private Long compressedSizeBytes;

    /**
     * Checksum or hash of the archived data for integrity verification.
     * Ensures data integrity during storage and retrieval operations.
     */
    @Size(max = 255)
    @Column(name = "data_checksum", length = 255)
    private String dataChecksum;

    /**
     * User or system that initiated the archival process.
     * Provides audit trail for archival operations.
     */
    @Size(max = 100)
    @Column(name = "archived_by", length = 100)
    private String archivedBy;

    /**
     * Timestamp when the archive record was last updated.
     * Tracks modifications to archive metadata.
     */
    @Column(name = "updated_timestamp")
    private LocalDateTime updatedTimestamp;

    /**
     * Additional metadata stored as JSON for extensibility.
     * Supports custom attributes without schema changes.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Sets the updated timestamp automatically before persisting updates.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedTimestamp = LocalDateTime.now();
    }

    /**
     * Sets the archive date and updated timestamp before initial persistence.
     */
    @PrePersist
    protected void onCreate() {
        if (archiveDate == null) {
            archiveDate = LocalDateTime.now();
        }
        updatedTimestamp = LocalDateTime.now();
    }
}