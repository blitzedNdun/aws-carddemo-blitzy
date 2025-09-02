package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for transaction add operations.
 * Maps COTRN02 BMS output fields with JSON serialization support for REST API responses.
 * Contains all transaction fields plus operation result status and messages.
 * 
 * This DTO represents the response structure for adding new transactions to the credit card system,
 * providing complete transaction details along with operation status information.
 */
@Data
public class AddTransactionResponse {

    /**
     * Unique transaction identifier generated after successful transaction creation.
     * Maps to the system-generated transaction ID for tracking and reference.
     */
    private String transactionId;

    /**
     * Account identifier associated with the transaction.
     * Maps to ACTIDIN field from COTRN02 BMS (PIC X(11)).
     * Represents the customer account number for the transaction.
     */
    private String accountId;

    /**
     * Credit card number used for the transaction.
     * Maps to CARDNIN field from COTRN02 BMS (PIC X(16)).
     * Contains the full 16-digit card number for identification.
     */
    private String cardNumber;

    /**
     * Transaction monetary amount with exact decimal precision.
     * Maps to TRNAMT field from COTRN02 BMS (PIC X(12)).
     * Uses BigDecimal to maintain COBOL COMP-3 packed decimal precision for financial calculations.
     * Formatted as decimal number in JSON responses.
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private BigDecimal amount;

    /**
     * Original transaction date when the transaction was initiated.
     * Maps to TORIGDT field from COTRN02 BMS (PIC X(10)).
     * Represents the date the transaction was originally processed.
     * Formatted as ISO date string (yyyy-MM-dd) in JSON responses.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;

    /**
     * System processing date when the transaction was processed.
     * Maps to TPROCDT field from COTRN02 BMS (PIC X(10)).
     * Represents the date the transaction was processed by the system.
     * Formatted as ISO date string (yyyy-MM-dd) in JSON responses.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate processingDate;

    /**
     * Transaction type code identifying the kind of transaction.
     * Maps to TTYPCD field from COTRN02 BMS (PIC X(2)).
     * Examples: 'PU' for Purchase, 'CR' for Credit, 'DB' for Debit.
     */
    private String typeCode;

    /**
     * Transaction category code for transaction classification.
     * Maps to TCATCD field from COTRN02 BMS (PIC X(4)).
     * Used for reporting and categorization of transaction types.
     */
    private String categoryCode;

    /**
     * Transaction source identifier indicating origination.
     * Maps to TRNSRC field from COTRN02 BMS (PIC X(10)).
     * Examples: 'ONLINE', 'POS', 'ATM', 'PHONE' indicating transaction origin.
     */
    private String source;

    /**
     * Detailed transaction description.
     * Maps to TDESC field from COTRN02 BMS (PIC X(60)).
     * Provides human-readable description of the transaction purpose or details.
     */
    private String description;

    /**
     * Merchant name where the transaction occurred.
     * Maps to MNAME field from COTRN02 BMS (PIC X(30)).
     * Contains the business name where the transaction took place.
     */
    private String merchantName;

    /**
     * Merchant city location.
     * Maps to MCITY field from COTRN02 BMS (PIC X(25)).
     * Geographic city location of the merchant.
     */
    private String merchantCity;

    /**
     * Merchant postal/zip code.
     * Maps to MZIP field from COTRN02 BMS (PIC X(10)).
     * Postal code of the merchant location.
     */
    private String merchantZip;

    /**
     * Operation result status indicator.
     * Indicates success or failure of the transaction add operation.
     * Examples: 'SUCCESS', 'ERROR', 'VALIDATION_FAILED', 'PROCESSING'.
     */
    private String status;

    /**
     * Status or error message providing additional information.
     * Maps to ERRMSG field from COTRN02 BMS (PIC X(78)) for error scenarios.
     * Contains detailed message about the operation result, success confirmation, or error details.
     */
    private String message;
}