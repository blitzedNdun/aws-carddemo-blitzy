package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Summary DTO for individual transactions in transaction lists.
 * Provides key transaction information for display in COTRN00 BMS transaction list screens.
 * 
 * This DTO represents a simplified transaction view optimized for list display,
 * containing only the essential fields needed for transaction selection and basic information.
 * Each transaction includes selection capability for detailed transaction processing.
 * 
 * Maps to COTRN00 BMS screen fields for transaction rows:
 * - selected: Maps to SEL0001-SEL0010 selection flags
 * - transactionId: Maps to TRNID01-TRNID10 transaction IDs (16 characters each)
 * - date: Maps to TDATE01-TDATE10 transaction dates (8 characters each)
 * - description: Maps to TDESC01-TDESC10 transaction descriptions (26 characters each)
 * - amount: Maps to TAMT001-TAMT010 transaction amounts (12 characters each)
 */
@Data
public class TransactionSummaryDto {

    /**
     * Selection flag indicating if this transaction is selected for processing.
     * Maps to SEL0001-SEL0010 fields from COTRN00 BMS screen.
     * Used for batch operations on selected transactions (view details, reports, etc.).
     * Defaults to false for unselected state.
     */
    @JsonProperty("selected")
    private Boolean selected = Boolean.FALSE;

    /**
     * Unique transaction identifier for reference and detail lookup.
     * Maps to TRNID01-TRNID10 fields from COTRN00 BMS (PIC X(16)).
     * Provides the system-generated transaction ID for tracking and detailed access.
     */
    @NotBlank
    @JsonProperty("transactionId")
    private String transactionId;

    /**
     * Transaction date when the transaction was processed.
     * Maps to TDATE01-TDATE10 fields from COTRN00 BMS (PIC X(8)).
     * Displayed in ISO date format (yyyy-MM-dd) in JSON responses.
     * Used for chronological sorting and date-based filtering.
     */
    @NotNull
    @JsonProperty("date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * Brief transaction description for list display.
     * Maps to TDESC01-TDESC10 fields from COTRN00 BMS (PIC X(26)).
     * Truncated description optimized for list view, providing essential transaction context.
     * Full description available through detailed transaction view.
     */
    @NotBlank
    @JsonProperty("description")
    private String description;

    /**
     * Transaction monetary amount with exact decimal precision.
     * Maps to TAMT001-TAMT010 fields from COTRN00 BMS (PIC X(12)).
     * Uses BigDecimal to maintain COBOL COMP-3 packed decimal precision for financial calculations.
     * Displayed as decimal number in JSON responses with proper formatting.
     */
    @NotNull
    @JsonProperty("amount")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private BigDecimal amount;

    /**
     * Card number associated with this transaction.
     * Maps to TRAN-CARD-NUM from CVTRA05Y copybook (PIC X(16)).
     * Used for transaction filtering and validation purposes.
     * Essential for transaction traceability and card-specific reporting.
     */
    @NotBlank
    @JsonProperty("cardNumber")
    private String cardNumber;

}