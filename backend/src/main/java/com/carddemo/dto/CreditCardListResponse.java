/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import java.util.List;
import lombok.Data;

/**
 * Response DTO for credit card list query (CCLI transaction).
 * 
 * This DTO represents the complete response structure for the Credit Card List Screen
 * (COCRDLI) operations, providing paginated card data along with navigation metadata
 * and status information. It maintains functional parity with the original COBOL
 * COCRDLIC.cbl program's screen handling and pagination logic.
 * 
 * Based on COCRDLI BMS Map Structure:
 * - Supports exactly 7 cards per page (matching COBOL WS-MAX-SCREEN-LINES)
 * - Provides pagination controls matching COBOL CA-NEXT-PAGE-EXISTS logic
 * - Includes info and error message fields matching COBOL message handling
 * - Supports card selection operations through CardListDto selection flags
 * 
 * COBOL Structure Mapping:
 * - WS-SCREEN-ROWS (7 OCCURS) → List<CardListDto> cards (max 7 items)
 * - WS-CA-SCREEN-NUM → currentPage (1-based page numbering)
 * - WS-CA-NEXT-PAGE-IND → hasNextPage (CA-NEXT-PAGE-EXISTS logic)
 * - WS-CA-LAST-PAGE-DISPLAYED → hasPreviousPage (derived from page position)
 * - WS-INFO-MSG → infoMessage (45 character message field)
 * - WS-ERROR-MSG → errorMessage (75 character error field)
 * 
 * Pagination Logic:
 * - Maximum 7 records per page (preserving COBOL screen structure)
 * - 1-based page numbering matching COBOL convention
 * - hasNextPage/hasPreviousPage flags for navigation control
 * - totalElements and totalPages for complete result set information
 * 
 * Security Features:
 * - All card data automatically masked through CardListDto
 * - PCI DSS compliant data handling via underlying CardListDto masking
 * - No sensitive card data exposed in pagination metadata
 * 
 * This DTO supports the complete card listing workflow defined in the
 * COCRDLIC.cbl program for the credit card management system migration.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Data
public class CreditCardListResponse {

    /**
     * List of credit cards for the current page.
     * Maximum 7 cards per page matching COBOL WS-MAX-SCREEN-LINES.
     * Each card contains masked data for PCI DSS compliance.
     */
    private List<CardListDto> cards;

    /**
     * Current page number (1-based).
     * Maps to COBOL WS-CA-SCREEN-NUM field for page tracking.
     */
    private int currentPage;

    /**
     * Total number of pages available.
     * Calculated based on totalElements divided by 7 (max cards per page).
     */
    private int totalPages;

    /**
     * Total number of card records matching the search criteria.
     * Used for pagination calculations and result set information.
     */
    private long totalElements;

    /**
     * Indicates if there are more pages after the current page.
     * Maps to COBOL CA-NEXT-PAGE-EXISTS logic for forward navigation.
     */
    private boolean hasNextPage;

    /**
     * Indicates if there are pages before the current page.
     * Derived from currentPage > 1 for backward navigation control.
     */
    private boolean hasPreviousPage;

    /**
     * Informational message for user guidance.
     * Maps to COBOL WS-INFO-MSG (45 characters).
     * Example: "TYPE S FOR DETAIL, U TO UPDATE ANY RECORD"
     */
    private String infoMessage;

    /**
     * Error message for validation or processing errors.
     * Maps to COBOL WS-ERROR-MSG (75 characters).
     * Example: "NO RECORDS FOUND FOR THIS SEARCH CONDITION."
     */
    private String errorMessage;

    /**
     * Default constructor for framework compatibility.
     * Initializes pagination fields to safe defaults.
     */
    public CreditCardListResponse() {
        this.currentPage = 1;
        this.totalPages = 0;
        this.totalElements = 0L;
        this.hasNextPage = false;
        this.hasPreviousPage = false;
    }

    /**
     * Constructor for creating a paginated response with card data.
     * 
     * @param cards the list of cards for this page (max 7)
     * @param currentPage the current page number (1-based)
     * @param totalElements the total number of records available
     */
    public CreditCardListResponse(List<CardListDto> cards, int currentPage, long totalElements) {
        this.cards = cards;
        this.currentPage = Math.max(1, currentPage); // Ensure 1-based
        this.totalElements = totalElements;
        
        // Calculate pagination metadata
        this.totalPages = calculateTotalPages(totalElements);
        this.hasNextPage = currentPage < this.totalPages;
        this.hasPreviousPage = currentPage > 1;
    }

    /**
     * Calculates total pages based on total elements.
     * Uses 7 records per page matching COBOL WS-MAX-SCREEN-LINES.
     * 
     * @param totalElements the total number of records
     * @return the total number of pages needed
     */
    private int calculateTotalPages(long totalElements) {
        if (totalElements <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / 7.0);
    }

    /**
     * Checks if the current page has any cards.
     * Used for empty result set detection.
     * 
     * @return true if cards list is not empty, false otherwise
     */
    public boolean hasCards() {
        return cards != null && !cards.isEmpty();
    }

    /**
     * Gets the number of cards in the current page.
     * Used for display logic and pagination information.
     * 
     * @return the size of the cards list, or 0 if null
     */
    public int getCardCount() {
        return cards != null ? cards.size() : 0;
    }

    /**
     * Checks if this is the first page.
     * Maps to COBOL CA-FIRST-PAGE logic.
     * 
     * @return true if currentPage is 1, false otherwise
     */
    public boolean isFirstPage() {
        return currentPage == 1;
    }

    /**
     * Checks if this is the last page.
     * Maps to COBOL CA-LAST-PAGE-SHOWN logic.
     * 
     * @return true if currentPage equals totalPages, false otherwise
     */
    public boolean isLastPage() {
        return currentPage >= totalPages && totalPages > 0;
    }

    /**
     * Gets the starting record number for the current page.
     * Used for "Showing X-Y of Z records" type displays.
     * 
     * @return the 1-based starting record number
     */
    public long getStartRecord() {
        if (totalElements == 0) {
            return 0;
        }
        return ((long) (currentPage - 1) * 7) + 1;
    }

    /**
     * Gets the ending record number for the current page.
     * Used for "Showing X-Y of Z records" type displays.
     * 
     * @return the 1-based ending record number
     */
    public long getEndRecord() {
        if (totalElements == 0) {
            return 0;
        }
        long endRecord = (long) currentPage * 7;
        return Math.min(endRecord, totalElements);
    }

    /**
     * Sets the cards list and updates pagination metadata.
     * Ensures consistency between card data and pagination flags.
     * 
     * @param cards the new list of cards
     */
    public void setCards(List<CardListDto> cards) {
        this.cards = cards;
        
        // Update pagination flags based on current data
        if (this.totalPages > 0) {
            this.hasNextPage = this.currentPage < this.totalPages;
            this.hasPreviousPage = this.currentPage > 1;
        }
    }

    /**
     * Sets the current page and updates pagination metadata.
     * Ensures valid page numbers and consistent navigation flags.
     * 
     * @param currentPage the new current page (1-based)
     */
    public void setCurrentPage(int currentPage) {
        this.currentPage = Math.max(1, currentPage); // Ensure 1-based
        
        // Update navigation flags
        this.hasNextPage = this.currentPage < this.totalPages;
        this.hasPreviousPage = this.currentPage > 1;
    }

    /**
     * Sets the total elements and recalculates pagination metadata.
     * Updates totalPages and navigation flags based on new total.
     * 
     * @param totalElements the new total number of elements
     */
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
        this.totalPages = calculateTotalPages(totalElements);
        
        // Update navigation flags
        this.hasNextPage = this.currentPage < this.totalPages;
        this.hasPreviousPage = this.currentPage > 1;
    }

    /**
     * Clears any existing error message.
     * Used when resetting the response state for new operations.
     */
    public void clearErrorMessage() {
        this.errorMessage = null;
    }

    /**
     * Clears any existing info message.
     * Used when resetting the response state for new operations.
     */
    public void clearInfoMessage() {
        this.infoMessage = null;
    }

    /**
     * Checks if there is an error message present.
     * Used for conditional error display logic.
     * 
     * @return true if errorMessage is not null and not empty, false otherwise
     */
    public boolean hasErrorMessage() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }

    /**
     * Checks if there is an info message present.
     * Used for conditional info display logic.
     * 
     * @return true if infoMessage is not null and not empty, false otherwise
     */
    public boolean hasInfoMessage() {
        return infoMessage != null && !infoMessage.trim().isEmpty();
    }

    /**
     * Creates a new response for an empty result set.
     * Sets appropriate messages matching COBOL behavior for no records found.
     * 
     * @param searchCriteria description of the search that yielded no results
     * @return a new CreditCardListResponse with no records found message
     */
    public static CreditCardListResponse noRecordsFound(String searchCriteria) {
        CreditCardListResponse response = new CreditCardListResponse();
        response.setErrorMessage("NO RECORDS FOUND FOR THIS SEARCH CONDITION.");
        response.setCards(List.of()); // Empty list
        return response;
    }

    /**
     * Creates a new response for successful card listing operations.
     * Sets the standard info message for user guidance.
     * 
     * @param cards the list of cards to display
     * @param currentPage the current page number
     * @param totalElements the total number of records
     * @return a new CreditCardListResponse with success messaging
     */
    public static CreditCardListResponse success(List<CardListDto> cards, int currentPage, long totalElements) {
        CreditCardListResponse response = new CreditCardListResponse(cards, currentPage, totalElements);
        response.setInfoMessage("TYPE S FOR DETAIL, U TO UPDATE ANY RECORD");
        return response;
    }

    /**
     * Creates a new response for error conditions.
     * Sets the error message and initializes empty result set.
     * 
     * @param errorMessage the error message to display
     * @return a new CreditCardListResponse with error state
     */
    public static CreditCardListResponse error(String errorMessage) {
        CreditCardListResponse response = new CreditCardListResponse();
        response.setErrorMessage(errorMessage);
        response.setCards(List.of()); // Empty list
        return response;
    }
}