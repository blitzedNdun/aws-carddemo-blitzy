package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO for credit card update responses containing updated card details with masked card number,
 * success indicators, and validation messages. Maps to COCRDUPC response handling and error messaging.
 * Includes update timestamp, change indicators, and formatted response messages matching COBOL WS-INFO-MSG patterns.
 */
public class CreditCardUpdateResponse {

    /**
     * Masked credit card number showing only last 4 digits (e.g., "**** **** **** 1234")
     */
    @JsonProperty("maskedCardNumber")
    private String maskedCardNumber;

    /**
     * Embossed name on the credit card
     */
    @JsonProperty("embossedName")
    private String embossedName;

    /**
     * Credit card expiration date - last day of expiration month
     */
    @JsonProperty("expirationDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    /**
     * Active status of the credit card (Y/N)
     */
    @JsonProperty("activeStatus")
    private String activeStatus;

    /**
     * Timestamp when the update was performed
     */
    @JsonProperty("updateTimestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTimestamp;

    /**
     * Success indicator for the update operation
     */
    @JsonProperty("successIndicator")
    private Boolean successIndicator;

    /**
     * Response message matching COBOL WS-RETURN-MSG patterns
     */
    @JsonProperty("responseMessage")
    private String responseMessage;

    /**
     * Default constructor
     */
    public CreditCardUpdateResponse() {
    }

    /**
     * Constructor with all fields
     */
    public CreditCardUpdateResponse(String maskedCardNumber, String embossedName, 
                                  LocalDate expirationDate, String activeStatus,
                                  LocalDateTime updateTimestamp, Boolean successIndicator,
                                  String responseMessage) {
        this.maskedCardNumber = maskedCardNumber;
        this.embossedName = embossedName;
        this.expirationDate = expirationDate;
        this.activeStatus = activeStatus;
        this.updateTimestamp = updateTimestamp;
        this.successIndicator = successIndicator;
        this.responseMessage = responseMessage;
    }

    /**
     * Gets the masked card number (last 4 digits)
     * @return masked card number
     */
    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    /**
     * Sets the masked card number
     * @param maskedCardNumber masked card number
     */
    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
    }

    /**
     * Gets the embossed name on the card
     * @return embossed name
     */
    public String getEmbossedName() {
        return embossedName;
    }

    /**
     * Sets the embossed name on the card
     * @param embossedName embossed name
     */
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    /**
     * Gets the card expiration date
     * @return expiration date
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the card expiration date
     * @param expirationDate expiration date
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the active status of the card
     * @return active status (Y/N)
     */
    public String getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the active status of the card
     * @param activeStatus active status (Y/N)
     */
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the update timestamp
     * @return update timestamp
     */
    public LocalDateTime getUpdateTimestamp() {
        return updateTimestamp;
    }

    /**
     * Sets the update timestamp
     * @param updateTimestamp update timestamp
     */
    public void setUpdateTimestamp(LocalDateTime updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    /**
     * Gets the success indicator
     * @return success indicator
     */
    public Boolean getSuccessIndicator() {
        return successIndicator;
    }

    /**
     * Sets the success indicator
     * @param successIndicator success indicator
     */
    public void setSuccessIndicator(Boolean successIndicator) {
        this.successIndicator = successIndicator;
    }

    /**
     * Gets the response message
     * @return response message
     */
    public String getResponseMessage() {
        return responseMessage;
    }

    /**
     * Sets the response message
     * @param responseMessage response message
     */
    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    /**
     * Creates a builder instance for constructing CreditCardUpdateResponse objects
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern implementation for CreditCardUpdateResponse
     */
    public static class Builder {
        private String maskedCardNumber;
        private String embossedName;
        private LocalDate expirationDate;
        private String activeStatus;
        private LocalDateTime updateTimestamp;
        private Boolean successIndicator;
        private String responseMessage;

        /**
         * Sets the masked card number
         * @param maskedCardNumber masked card number
         * @return builder instance
         */
        public Builder maskedCardNumber(String maskedCardNumber) {
            this.maskedCardNumber = maskedCardNumber;
            return this;
        }

        /**
         * Sets the embossed name
         * @param embossedName embossed name
         * @return builder instance
         */
        public Builder embossedName(String embossedName) {
            this.embossedName = embossedName;
            return this;
        }

        /**
         * Sets the expiration date
         * @param expirationDate expiration date
         * @return builder instance
         */
        public Builder expirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        /**
         * Sets the active status
         * @param activeStatus active status
         * @return builder instance
         */
        public Builder activeStatus(String activeStatus) {
            this.activeStatus = activeStatus;
            return this;
        }

        /**
         * Sets the update timestamp
         * @param updateTimestamp update timestamp
         * @return builder instance
         */
        public Builder updateTimestamp(LocalDateTime updateTimestamp) {
            this.updateTimestamp = updateTimestamp;
            return this;
        }

        /**
         * Sets the success indicator
         * @param successIndicator success indicator
         * @return builder instance
         */
        public Builder successIndicator(Boolean successIndicator) {
            this.successIndicator = successIndicator;
            return this;
        }

        /**
         * Sets the response message
         * @param responseMessage response message
         * @return builder instance
         */
        public Builder responseMessage(String responseMessage) {
            this.responseMessage = responseMessage;
            return this;
        }

        /**
         * Builds the CreditCardUpdateResponse instance
         * @return CreditCardUpdateResponse instance
         */
        public CreditCardUpdateResponse build() {
            return new CreditCardUpdateResponse(maskedCardNumber, embossedName, expirationDate,
                    activeStatus, updateTimestamp, successIndicator, responseMessage);
        }
    }

    /**
     * Checks equality based on all fields
     * @param o object to compare
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreditCardUpdateResponse that = (CreditCardUpdateResponse) o;
        return Objects.equals(maskedCardNumber, that.maskedCardNumber) &&
               Objects.equals(embossedName, that.embossedName) &&
               Objects.equals(expirationDate, that.expirationDate) &&
               Objects.equals(activeStatus, that.activeStatus) &&
               Objects.equals(updateTimestamp, that.updateTimestamp) &&
               Objects.equals(successIndicator, that.successIndicator) &&
               Objects.equals(responseMessage, that.responseMessage);
    }

    /**
     * Generates hash code based on all fields
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(maskedCardNumber, embossedName, expirationDate, activeStatus,
                           updateTimestamp, successIndicator, responseMessage);
    }

    /**
     * String representation of the response object
     * @return string representation
     */
    @Override
    public String toString() {
        return "CreditCardUpdateResponse{" +
                "maskedCardNumber='" + maskedCardNumber + '\'' +
                ", embossedName='" + embossedName + '\'' +
                ", expirationDate=" + expirationDate +
                ", activeStatus='" + activeStatus + '\'' +
                ", updateTimestamp=" + updateTimestamp +
                ", successIndicator=" + successIndicator +
                ", responseMessage='" + responseMessage + '\'' +
                '}';
    }
}