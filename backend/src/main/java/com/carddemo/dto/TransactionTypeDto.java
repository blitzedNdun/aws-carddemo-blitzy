/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.Constants;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transaction Type Data Transfer Object.
 * 
 * This DTO represents transaction type information mapping to the COBOL
 * CVTRA03Y copybook TRAN-TYPE-RECORD structure. It provides transaction
 * type lookup and validation functionality for the credit card processing
 * system during the mainframe-to-cloud migration.
 * 
 * COBOL Mapping:
 * - TRAN-TYPE (PIC X(02)) -> typeCode
 * - TRAN-TYPE-DESC (PIC X(50)) -> typeDescription
 * 
 * This class supports:
 * - Transaction type lookups for validation
 * - Caching for frequently accessed types
 * - Validation against allowed transaction type values
 * - JSON serialization for REST API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionTypeDto {

    /**
     * Transaction type code.
     * 
     * Maps to COBOL TRAN-TYPE field (PIC X(02)).
     * Represents the 2-character transaction type identifier used throughout
     * the system for transaction categorization and processing rules.
     * 
     * Examples: "01" = Purchase, "02" = Cash Advance, "03" = Payment, etc.
     */
    @JsonProperty("typeCode")
    @Size(min = Constants.TYPE_CODE_LENGTH, max = Constants.TYPE_CODE_LENGTH, 
          message = "Transaction type code must be exactly " + Constants.TYPE_CODE_LENGTH + " characters")
    private String typeCode;

    /**
     * Transaction type description.
     * 
     * Maps to COBOL TRAN-TYPE-DESC field (PIC X(50)).
     * Provides human-readable description of the transaction type for
     * display purposes and system documentation.
     * 
     * Examples: "Purchase Transaction", "Cash Advance", "Payment", etc.
     */
    @JsonProperty("typeDescription")
    @Size(max = Constants.DESCRIPTION_LENGTH,
          message = "Transaction type description cannot exceed " + Constants.DESCRIPTION_LENGTH + " characters")
    private String typeDescription;

    /**
     * Creates a new TransactionTypeDto with the specified type code and description.
     * 
     * This constructor ensures that both fields are properly initialized and
     * can be used for creating instances during transaction type lookups and
     * validation processes.
     * 
     * @param typeCode the 2-character transaction type code
     * @param typeDescription the transaction type description (max 50 characters)
     */
    public TransactionTypeDto(String typeCode, String typeDescription) {
        this.typeCode = typeCode;
        this.typeDescription = typeDescription;
    }

    /**
     * Gets the transaction type code.
     * 
     * @return the 2-character transaction type code
     */
    public String getTypeCode() {
        return typeCode;
    }

    /**
     * Sets the transaction type code.
     * 
     * @param typeCode the 2-character transaction type code to set
     */
    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    /**
     * Gets the transaction type description.
     * 
     * @return the transaction type description
     */
    public String getTypeDescription() {
        return typeDescription;
    }

    /**
     * Sets the transaction type description.
     * 
     * @param typeDescription the transaction type description to set
     */
    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    /**
     * Checks if this TransactionTypeDto is equal to another object.
     * 
     * Two TransactionTypeDto instances are considered equal if they have
     * the same typeCode and typeDescription values.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TransactionTypeDto that = (TransactionTypeDto) obj;
        
        if (typeCode != null ? !typeCode.equals(that.typeCode) : that.typeCode != null) return false;
        return typeDescription != null ? typeDescription.equals(that.typeDescription) : that.typeDescription == null;
    }

    /**
     * Returns the hash code for this TransactionTypeDto.
     * 
     * The hash code is computed based on both typeCode and typeDescription
     * to ensure proper behavior in hash-based collections.
     * 
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int result = typeCode != null ? typeCode.hashCode() : 0;
        result = 31 * result + (typeDescription != null ? typeDescription.hashCode() : 0);
        return result;
    }

    /**
     * Returns a string representation of this TransactionTypeDto.
     * 
     * The string includes both the type code and description for
     * debugging and logging purposes.
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "TransactionTypeDto{" +
                "typeCode='" + typeCode + '\'' +
                ", typeDescription='" + typeDescription + '\'' +
                '}';
    }
}