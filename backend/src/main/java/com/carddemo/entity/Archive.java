package com.carddemo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * JPA entity representing archived data records with metadata for retention policy management,
 * storage location tracking, and audit trail preservation. Supports various data types archival
 * with compression and legal hold capabilities.
 * 
 * This entity provides comprehensive archival functionality including:
 * - Unique archive identification and tracking
 * - Flexible data type classification
 * - Retention policy management with automatic expiration calculation
 * - Storage path tracking for archived file locations
 * - Legal hold support for compliance requirements
 * - Compression type tracking for storage optimization
 * - Flexible metadata storage using PostgreSQL JSONB
 * - Audit trail preservation through creation and modification tracking
 */
@Entity
@Table(name = "archive", indexes = {
    @Index(name = "idx_archive_data_type", columnList = "data_type"),
    @Index(name = "idx_archive_date", columnList = "archive_date"),
    @Index(name = "idx_archive_expiration", columnList = "expiration_date"),
    @Index(name = "idx_archive_legal_hold", columnList = "legal_hold")
})
public class Archive {

    /**
     * Unique identifier for the archived record.
     * Primary key using auto-generated sequence values.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    /**
     * Type of data being archived (e.g., TRANSACTION, ACCOUNT, CUSTOMER, CARD).
     * Used for categorization and retention policy application.
     */
    @NotNull
    @Size(max = 50)
    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    /**
     * Date when the data was archived.
     * Used for retention period calculation and audit tracking.
     */
    @NotNull
    @Column(name = "archive_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate archiveDate;

    /**
     * Retention period for the archived data in days.
     * Combined with archive_date to calculate expiration_date.
     */
    @NotNull
    @Column(name = "retention_period", nullable = false)
    private Integer retentionPeriod;

    /**
     * Storage path where the archived data is located.
     * Can be file system path, cloud storage URL, or other storage identifier.
     */
    @NotNull
    @Size(max = 500)
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    /**
     * Legal hold flag indicating whether the archived data is under legal preservation.
     * When true, data should not be deleted regardless of retention period expiration.
     */
    @NotNull
    @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold;

    /**
     * Type of compression applied to the archived data.
     * Common values: GZIP, ZIP, BZIP2, LZ4, NONE.
     */
    @Size(max = 20)
    @Column(name = "compression_type", length = 20)
    private String compressionType;

    /**
     * Flexible metadata storage using PostgreSQL JSONB for efficient querying.
     * Can store additional archival information such as:
     * - Original data source
     * - Archival reason
     * - Data quality metrics
     * - Compliance tags
     * - Custom attributes
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;

    /**
     * Calculated expiration date based on archive_date and retention_period.
     * Automatically computed but can be overridden for special cases.
     */
    @Column(name = "expiration_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    /**
     * Date when the archive record was created.
     * Automatically set on entity creation for audit trail.
     */
    @Column(name = "created_date", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdDate;

    /**
     * Date when the archive record was last modified.
     * Automatically updated on entity modification for audit trail.
     */
    @Column(name = "last_modified_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastModifiedDate;

    /**
     * Default constructor for JPA.
     */
    public Archive() {
        this.createdDate = LocalDate.now();
        this.lastModifiedDate = LocalDate.now();
        this.legalHold = false;
    }

    /**
     * Constructor with required fields.
     *
     * @param dataType        Type of data being archived
     * @param archiveDate     Date when data was archived
     * @param retentionPeriod Retention period in days
     * @param storagePath     Storage location path
     */
    public Archive(String dataType, LocalDate archiveDate, Integer retentionPeriod, String storagePath) {
        this();
        this.dataType = dataType;
        this.archiveDate = archiveDate;
        this.retentionPeriod = retentionPeriod;
        this.storagePath = storagePath;
        this.expirationDate = calculateExpirationDate();
    }

    /**
     * JPA pre-persist callback to set creation timestamp.
     */
    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDate.now();
        this.lastModifiedDate = LocalDate.now();
        if (this.expirationDate == null) {
            this.expirationDate = calculateExpirationDate();
        }
    }

    /**
     * JPA pre-update callback to update modification timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedDate = LocalDate.now();
        // Recalculate expiration date if archive date or retention period changed
        if (this.archiveDate != null && this.retentionPeriod != null) {
            this.expirationDate = calculateExpirationDate();
        }
    }

    // Getter and Setter methods

    /**
     * Gets the unique archive identifier.
     *
     * @return the archive ID
     */
    public Long getArchiveId() {
        return archiveId;
    }

    /**
     * Sets the unique archive identifier.
     *
     * @param archiveId the archive ID to set
     */
    public void setArchiveId(Long archiveId) {
        this.archiveId = archiveId;
    }

    /**
     * Gets the type of data being archived.
     *
     * @return the data type
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Sets the type of data being archived.
     *
     * @param dataType the data type to set
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    /**
     * Gets the date when data was archived.
     *
     * @return the archive date
     */
    public LocalDate getArchiveDate() {
        return archiveDate;
    }

    /**
     * Sets the date when data was archived.
     *
     * @param archiveDate the archive date to set
     */
    public void setArchiveDate(LocalDate archiveDate) {
        this.archiveDate = archiveDate;
        // Recalculate expiration date when archive date changes
        if (this.retentionPeriod != null) {
            this.expirationDate = calculateExpirationDate();
        }
    }

    /**
     * Gets the retention period in days.
     *
     * @return the retention period
     */
    public Integer getRetentionPeriod() {
        return retentionPeriod;
    }

    /**
     * Sets the retention period in days.
     *
     * @param retentionPeriod the retention period to set
     */
    public void setRetentionPeriod(Integer retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
        // Recalculate expiration date when retention period changes
        if (this.archiveDate != null) {
            this.expirationDate = calculateExpirationDate();
        }
    }

    /**
     * Gets the storage path where archived data is located.
     *
     * @return the storage path
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * Sets the storage path where archived data is located.
     *
     * @param storagePath the storage path to set
     */
    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * Checks if the archive is under legal hold.
     *
     * @return true if under legal hold, false otherwise
     */
    public Boolean isLegalHold() {
        return legalHold;
    }

    /**
     * Sets the legal hold status.
     *
     * @param legalHold the legal hold status to set
     */
    public void setLegalHold(Boolean legalHold) {
        this.legalHold = legalHold;
    }

    /**
     * Gets the compression type used for archived data.
     *
     * @return the compression type
     */
    public String getCompressionType() {
        return compressionType;
    }

    /**
     * Sets the compression type used for archived data.
     *
     * @param compressionType the compression type to set
     */
    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }

    /**
     * Gets the flexible metadata stored as JSONB.
     *
     * @return the metadata JSON node
     */
    public JsonNode getMetadata() {
        return metadata;
    }

    /**
     * Sets the flexible metadata stored as JSONB.
     *
     * @param metadata the metadata JSON node to set
     */
    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }

    /**
     * Gets the calculated expiration date.
     *
     * @return the expiration date
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Calculates the expiration date based on archive date and retention period.
     * This method implements the business logic for determining when archived data
     * can be safely deleted (unless under legal hold).
     *
     * @return the calculated expiration date, or null if archive date or retention period is null
     */
    public LocalDate calculateExpirationDate() {
        if (this.archiveDate == null || this.retentionPeriod == null) {
            return null;
        }
        return this.archiveDate.plusDays(this.retentionPeriod);
    }

    /**
     * Gets the creation date of the archive record.
     *
     * @return the creation date
     */
    public LocalDate getCreatedDate() {
        return createdDate;
    }

    /**
     * Gets the last modification date of the archive record.
     *
     * @return the last modification date
     */
    public LocalDate getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Checks if the archive has expired based on expiration date and legal hold status.
     * Archives under legal hold never expire regardless of the expiration date.
     *
     * @return true if expired and not under legal hold, false otherwise
     */
    public boolean isExpired() {
        if (this.legalHold || this.expirationDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(this.expirationDate);
    }

    /**
     * Checks if the archive can be deleted based on expiration and legal hold status.
     * This is a convenience method that combines expiration check with legal hold validation.
     *
     * @return true if the archive can be safely deleted, false otherwise
     */
    public boolean canBeDeleted() {
        return isExpired() && !this.legalHold;
    }

    /**
     * Compares this archive with another object for equality.
     * Two archives are considered equal if they have the same archive ID.
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Archive archive = (Archive) obj;
        return Objects.equals(archiveId, archive.archiveId);
    }

    /**
     * Returns the hash code for this archive.
     * The hash code is based on the archive ID.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(archiveId);
    }

    /**
     * Returns a string representation of the archive.
     * Includes key information for debugging and logging purposes.
     *
     * @return string representation of the archive
     */
    @Override
    public String toString() {
        return "Archive{" +
                "archiveId=" + archiveId +
                ", dataType='" + dataType + '\'' +
                ", archiveDate=" + archiveDate +
                ", retentionPeriod=" + retentionPeriod +
                ", storagePath='" + storagePath + '\'' +
                ", legalHold=" + legalHold +
                ", compressionType='" + compressionType + '\'' +
                ", expirationDate=" + expirationDate +
                ", createdDate=" + createdDate +
                ", lastModifiedDate=" + lastModifiedDate +
                '}';
    }
}