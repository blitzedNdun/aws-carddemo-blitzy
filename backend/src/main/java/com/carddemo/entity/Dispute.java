package com.carddemo.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity class representing dispute cases for credit card transactions.
 * 
 * This entity manages dispute tracking throughout the complete lifecycle including:
 * - Initial dispute filing and case creation
 * - Provisional credit processing and management
 * - Chargeback workflow execution
 * - Merchant response handling and evaluation
 * - Regulatory compliance timeline tracking
 * - Documentation and evidence management
 * - Resolution and closure processing
 * 
 * The entity maintains relationships with Transaction and Account entities to ensure
 * complete integration with the credit card management system and supports all
 * regulatory requirements for dispute processing including chargeback timelines
 * and provisional credit regulations.
 * 
 * Database mapping follows PostgreSQL schema patterns established in the CardDemo
 * application with composite indexing for optimal query performance on dispute
 * status and timeline-based searches.
 */
@Entity
@Table(name = "disputes", indexes = {
    @Index(name = "idx_dispute_transaction", columnList = "transaction_id"),
    @Index(name = "idx_dispute_account", columnList = "account_id"),
    @Index(name = "idx_dispute_status", columnList = "status"),
    @Index(name = "idx_dispute_created_date", columnList = "created_date"),
    @Index(name = "idx_dispute_type", columnList = "dispute_type")
})
public class Dispute {

    /**
     * Primary key for dispute records. Auto-generated sequence value providing
     * unique identification for each dispute case across the system.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dispute_id", nullable = false)
    private Long disputeId;

    /**
     * Reference to the transaction being disputed. Links to the Transaction entity
     * to maintain full traceability between dispute cases and original transactions.
     * This foreign key relationship enables complete transaction history analysis
     * during dispute resolution.
     */
    @Column(name = "transaction_id", nullable = false)
    @NotNull(message = "Transaction ID is required for dispute processing")
    private Long transactionId;

    /**
     * Account identifier linking the dispute to the customer account. This field
     * maintains the relationship between disputes and account management for
     * balance adjustments, provisional credits, and customer communication.
     */
    @Column(name = "account_id", nullable = false)
    @NotNull(message = "Account ID is required for dispute processing")
    private Long accountId;

    /**
     * Classification of the dispute type indicating the nature of the customer claim.
     * Supports standard dispute categories including:
     * - UNAUTHORIZED: Fraudulent or unauthorized transaction
     * - DUPLICATE: Duplicate processing of the same transaction
     * - SERVICE: Services not received or not as described
     * - MERCHANDISE: Merchandise not received or defective
     * - CREDIT_NOT_PROCESSED: Expected credit not applied to account
     * - CANCELLED_RECURRING: Cancelled recurring transaction still processed
     * - AMOUNT_DIFFERENT: Transaction amount different from authorization
     * - OTHER: Other dispute reasons requiring manual review
     */
    @Column(name = "dispute_type", length = 25, nullable = false)
    @NotBlank(message = "Dispute type must be specified")
    @Size(max = 25, message = "Dispute type cannot exceed 25 characters")
    private String disputeType;

    /**
     * Current status of the dispute case tracking progression through the dispute lifecycle.
     * Status values include:
     * - FILED: Initial dispute filing received and recorded
     * - INVESTIGATING: Case under active investigation
     * - CHARGEBACK_INITIATED: Chargeback submitted to card network
     * - MERCHANT_RESPONSE: Awaiting or processing merchant response
     * - PROVISIONAL_CREDIT: Provisional credit issued to customer
     * - RESOLVED_CUSTOMER: Dispute resolved in favor of customer
     * - RESOLVED_MERCHANT: Dispute resolved in favor of merchant
     * - CLOSED: Dispute case closed and finalized
     * - ESCALATED: Dispute escalated to higher authority
     */
    @Column(name = "status", length = 20, nullable = false)
    @NotBlank(message = "Dispute status is required")
    @Size(max = 20, message = "Dispute status cannot exceed 20 characters")
    private String status;

    /**
     * Timestamp when the dispute case was initially created in the system.
     * This date serves as the starting point for all regulatory timeline calculations
     * including chargeback deadlines and provisional credit requirements.
     */
    @Column(name = "created_date", nullable = false)
    @NotNull(message = "Created date is required")
    private LocalDate createdDate;

    /**
     * Date when the dispute case was resolved and closed. This field remains null
     * for active disputes and is populated upon final resolution. Used for
     * calculating total resolution time and regulatory compliance reporting.
     */
    @Column(name = "resolution_date")
    private LocalDate resolutionDate;

    /**
     * Amount of provisional credit issued to the customer during dispute processing.
     * This field tracks temporary credit adjustments that may be reversed based on
     * dispute resolution outcome. Uses BigDecimal for precise monetary calculations
     * matching COBOL COMP-3 packed decimal precision requirements.
     */
    @Column(name = "provisional_credit_amount", precision = 12, scale = 2)
    @DecimalMin(value = "0.00", message = "Provisional credit amount must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Provisional credit amount must have at most 2 decimal places")
    private BigDecimal provisionalCreditAmount;

    /**
     * Standardized reason code for the dispute indicating the specific regulatory
     * or industry standard classification. These codes align with Visa, MasterCard,
     * and other card network reason code structures for chargeback processing.
     * Examples include: 4855 (Goods/Services not received), 4863 (Cardholder dispute)
     */
    @Column(name = "reason_code", length = 10)
    @Size(max = 10, message = "Reason code cannot exceed 10 characters")
    private String reasonCode;

    /**
     * Detailed description of the dispute including customer explanation,
     * circumstances, and supporting details. This free-text field captures
     * comprehensive dispute information for investigation and resolution teams.
     */
    @Column(name = "description", length = 1000)
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    /**
     * Default constructor required by JPA specification for entity instantiation
     * during database operations and object-relational mapping.
     */
    public Dispute() {
        // JPA requires default constructor
    }

    /**
     * Constructor for creating new dispute cases with essential information.
     * Initializes core dispute fields and sets creation timestamp.
     * 
     * @param transactionId The ID of the transaction being disputed
     * @param accountId The account ID associated with the dispute
     * @param disputeType The type/category of the dispute
     * @param status The initial status of the dispute
     * @param description Detailed description of the dispute
     */
    public Dispute(Long transactionId, Long accountId, String disputeType, String status, String description) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.disputeType = disputeType;
        this.status = status;
        this.description = description;
        this.createdDate = LocalDate.now();
        this.provisionalCreditAmount = BigDecimal.ZERO;
    }

    /**
     * Gets the unique dispute identifier.
     * 
     * @return The dispute ID as a Long value
     */
    public Long getDisputeId() {
        return disputeId;
    }

    /**
     * Sets the unique dispute identifier.
     * 
     * @param disputeId The dispute ID to set
     */
    public void setDisputeId(Long disputeId) {
        this.disputeId = disputeId;
    }

    /**
     * Gets the transaction ID associated with this dispute.
     * 
     * @return The transaction ID as a Long value
     */
    public Long getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the transaction ID for this dispute.
     * 
     * @param transactionId The transaction ID to associate with this dispute
     */
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets the account ID associated with this dispute.
     * 
     * @return The account ID as a Long value
     */
    public Long getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID for this dispute.
     * 
     * @param accountId The account ID to associate with this dispute
     */
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the dispute type classification.
     * 
     * @return The dispute type as a String
     */
    public String getDisputeType() {
        return disputeType;
    }

    /**
     * Sets the dispute type classification.
     * 
     * @param disputeType The dispute type to set
     */
    public void setDisputeType(String disputeType) {
        this.disputeType = disputeType;
    }

    /**
     * Gets the current status of the dispute.
     * 
     * @return The dispute status as a String
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current status of the dispute.
     * 
     * @param status The dispute status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the date when the dispute was created.
     * 
     * @return The created date as a LocalDate
     */
    public LocalDate getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the date when the dispute was created.
     * 
     * @param createdDate The created date to set
     */
    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the date when the dispute was resolved.
     * 
     * @return The resolution date as a LocalDate, or null if not yet resolved
     */
    public LocalDate getResolutionDate() {
        return resolutionDate;
    }

    /**
     * Sets the date when the dispute was resolved.
     * 
     * @param resolutionDate The resolution date to set
     */
    public void setResolutionDate(LocalDate resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    /**
     * Gets the amount of provisional credit issued for this dispute.
     * 
     * @return The provisional credit amount as a BigDecimal
     */
    public BigDecimal getProvisionalCreditAmount() {
        return provisionalCreditAmount;
    }

    /**
     * Sets the amount of provisional credit for this dispute.
     * 
     * @param provisionalCreditAmount The provisional credit amount to set
     */
    public void setProvisionalCreditAmount(BigDecimal provisionalCreditAmount) {
        this.provisionalCreditAmount = provisionalCreditAmount;
    }

    /**
     * Gets the standardized reason code for this dispute.
     * 
     * @return The reason code as a String
     */
    public String getReasonCode() {
        return reasonCode;
    }

    /**
     * Sets the standardized reason code for this dispute.
     * 
     * @param reasonCode The reason code to set
     */
    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    /**
     * Gets the detailed description of this dispute.
     * 
     * @return The description as a String
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the detailed description of this dispute.
     * 
     * @param description The description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Provides string representation of the dispute entity for debugging and logging.
     * Includes key dispute information while maintaining data privacy for sensitive fields.
     * 
     * @return String representation of the dispute
     */
    @Override
    public String toString() {
        return "Dispute{" +
                "disputeId=" + disputeId +
                ", transactionId=" + transactionId +
                ", accountId=" + accountId +
                ", disputeType='" + disputeType + '\'' +
                ", status='" + status + '\'' +
                ", createdDate=" + createdDate +
                ", resolutionDate=" + resolutionDate +
                ", provisionalCreditAmount=" + provisionalCreditAmount +
                ", reasonCode='" + reasonCode + '\'' +
                '}';
    }

    /**
     * Determines equality based on dispute ID for proper entity management.
     * 
     * @param obj Object to compare with this dispute
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Dispute dispute = (Dispute) obj;
        return disputeId != null && disputeId.equals(dispute.disputeId);
    }

    /**
     * Generates hash code based on dispute ID for proper collection handling.
     * 
     * @return Hash code for this dispute entity
     */
    @Override
    public int hashCode() {
        return disputeId != null ? disputeId.hashCode() : 0;
    }
}