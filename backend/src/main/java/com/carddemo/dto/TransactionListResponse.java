package com.carddemo.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * Response DTO for transaction list results. Contains list of transaction summaries, 
 * pagination metadata, and total count. Maps COTRN00 BMS output including 10 
 * transaction display slots with selection capability.
 * 
 * This DTO preserves the mainframe pagination behavior where users can navigate
 * through transaction lists using PF7 (previous page) and PF8 (next page) keys,
 * now translated to REST API pagination parameters.
 * 
 * Field mappings from COTRN00 BMS screen:
 * - transactions: Maps to the 10 transaction rows (SEL0001-SEL0010, TRNID01-TRNID10, etc.)
 * - currentPage: Replaces PAGENUM field for tracking current page position
 * - hasMorePages: Indicates if PF8 (next page) functionality is available
 * - hasPreviousPages: Indicates if PF7 (previous page) functionality is available
 * - totalCount: Provides total transaction count for proper pagination calculation
 */
@Data
public class TransactionListResponse {

    /**
     * List of transaction summaries for display. Contains up to 10 transactions
     * per page, matching the COTRN00 BMS screen layout. Each transaction includes
     * selection capability for detailed transaction processing.
     * 
     * Maps to COTRN00 BMS fields:
     * - SEL0001-SEL0010: Selection flags for each transaction
     * - TRNID01-TRNID10: Transaction IDs (16 characters each)
     * - TDATE01-TDATE10: Transaction dates (8 characters each)
     * - TDESC01-TDESC10: Transaction descriptions (26 characters each)
     * - TAMT001-TAMT010: Transaction amounts (12 characters each)
     */
    @NotNull
    @JsonProperty("transactions")
    private List<TransactionSummaryDto> transactions;

    /**
     * Total number of transactions available across all pages.
     * Used for calculating pagination metadata and determining
     * if additional pages are available. Essential for implementing
     * VSAM STARTBR/READNEXT equivalent functionality in PostgreSQL.
     */
    @NotNull
    @JsonProperty("totalCount")
    private Integer totalCount;

    /**
     * Current page number (1-based indexing). Replaces the PAGENUM
     * field from COTRN00 BMS screen, providing users with their
     * current position in the transaction list pagination.
     */
    @NotNull
    @JsonProperty("currentPage")
    private Integer currentPage;

    /**
     * Indicates whether additional pages are available for navigation.
     * Replaces the PF8 (next page) functionality from the mainframe
     * BMS screen, allowing users to navigate forward through the
     * transaction list when more records are available.
     */
    @NotNull
    @JsonProperty("hasMorePages")
    private Boolean hasMorePages;

    /**
     * Indicates whether previous pages are available for navigation.
     * Replaces the PF7 (previous page) functionality from the mainframe
     * BMS screen, allowing users to navigate backward through the
     * transaction list when they are not on the first page.
     */
    @NotNull
    @JsonProperty("hasPreviousPages")
    private Boolean hasPreviousPages;

}