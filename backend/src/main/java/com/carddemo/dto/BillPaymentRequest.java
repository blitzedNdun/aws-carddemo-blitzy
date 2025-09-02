package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for bill payment operations.
 * Maps COBIL00 BMS input fields with validation matching COBOL picture clauses.
 * 
 * This DTO corresponds to the COBIL0AI structure from the COBIL00 BMS mapset:
 * - ACTIDIN field maps to accountId (11 digit account identifier)
 * - CONFIRMI field maps to confirmPayment (Y/N confirmation flag)
 * 
 * Validation rules mirror the COBOL program logic:
 * - Account ID cannot be empty (mandatory field)
 * - Account ID must be exactly 11 digits
 * - Confirmation must be Y or N (case insensitive)
 */
public class BillPaymentRequest {

    /**
     * Account ID for bill payment processing.
     * Maps to ACTIDIN field from COBIL0AI structure.
     * Must be exactly 11 digits as per COBOL PIC 9(11) definition.
     */
    @JsonProperty("accountId")
    @NotNull(message = "Account ID is required")
    @NotBlank(message = "Account ID cannot be empty")
    @Size(min = 11, max = 11, message = "Account ID must be exactly 11 digits")
    @Pattern(regexp = "^[0-9]{11}$", message = "Account ID must contain only numeric digits")
    private String accountId;

    /**
     * Payment confirmation flag.
     * Maps to CONFIRMI field from COBIL0AI structure.
     * Accepts Y/N values (case insensitive) or null for initial display.
     */
    @JsonProperty("confirmPayment")
    @Pattern(regexp = "^[YyNn]?$", message = "Confirmation must be Y or N (case insensitive) or empty")
    private String confirmPayment;

    /**
     * Default constructor for Jackson serialization.
     */
    public BillPaymentRequest() {
    }

    /**
     * Constructor with all required fields.
     * 
     * @param accountId The 11-digit account identifier
     * @param confirmPayment The Y/N confirmation flag
     */
    public BillPaymentRequest(String accountId, String confirmPayment) {
        this.accountId = accountId;
        this.confirmPayment = confirmPayment;
    }

    /**
     * Gets the account ID for bill payment processing.
     * 
     * @return The 11-digit account identifier
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID for bill payment processing.
     * 
     * @param accountId The 11-digit account identifier
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the payment confirmation flag.
     * 
     * @return The Y/N confirmation flag (case insensitive)
     */
    public String getConfirmPayment() {
        return confirmPayment;
    }

    /**
     * Sets the payment confirmation flag.
     * 
     * @param confirmPayment The Y/N confirmation flag (case insensitive)
     */
    public void setConfirmPayment(String confirmPayment) {
        this.confirmPayment = confirmPayment;
    }

    /**
     * Checks if payment is confirmed.
     * Follows COBOL logic: Y or y values indicate confirmation.
     * 
     * @return true if payment is confirmed (Y/y), false otherwise
     */
    public boolean isPaymentConfirmed() {
        return "Y".equalsIgnoreCase(confirmPayment);
    }

    /**
     * Checks if payment is explicitly declined.
     * Follows COBOL logic: N or n values indicate decline.
     * 
     * @return true if payment is declined (N/n), false otherwise
     */
    public boolean isPaymentDeclined() {
        return "N".equalsIgnoreCase(confirmPayment);
    }

    /**
     * Checks if confirmation is pending (empty or null).
     * Matches COBOL logic for initial screen display.
     * 
     * @return true if confirmation is pending, false otherwise
     */
    public boolean isConfirmationPending() {
        return confirmPayment == null || confirmPayment.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "BillPaymentRequest{" +
                "accountId='" + accountId + '\'' +
                ", confirmPayment='" + confirmPayment + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BillPaymentRequest that = (BillPaymentRequest) o;

        if (accountId != null ? !accountId.equals(that.accountId) : that.accountId != null) return false;
        return confirmPayment != null ? confirmPayment.equals(that.confirmPayment) : that.confirmPayment == null;
    }

    @Override
    public int hashCode() {
        int result = accountId != null ? accountId.hashCode() : 0;
        result = 31 * result + (confirmPayment != null ? confirmPayment.hashCode() : 0);
        return result;
    }
}