/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Objects;

/**
 * Composite primary key class for CardXref entity.
 * 
 * Contains all three fields that make up the unique identifier for card cross-references:
 * card number, customer ID, and account ID. Implements Serializable as required by JPA
 * specification for composite keys.
 * 
 * This class ensures that each combination of card, customer, and account can only exist
 * once in the cross-reference table, preventing duplicate relationships.
 * 
 * Maps to CARD-XREF-RECORD structure from CVACT03Y.cpy copybook composite key:
 * - XREF-CARD-NUM (PIC X(16)) → xrefCardNum (VARCHAR(16))
 * - XREF-CUST-ID (PIC 9(09)) → xrefCustId (BIGINT)
 * - XREF-ACCT-ID (PIC 9(11)) → xrefAcctId (BIGINT)
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Embeddable
@Data
public class CardXrefId implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Cross-reference card number component of composite key.
     * Maps to XREF-CARD-NUM field from CVACT03Y.cpy (PIC X(16)).
     */
    @Column(name = "xref_card_num", length = 16, nullable = false)
    @Size(max = 16, message = "Cross-reference card number cannot exceed 16 characters")
    private String xrefCardNum;

    /**
     * Cross-reference customer ID component of composite key.
     * Maps to XREF-CUST-ID field from CVACT03Y.cpy (PIC 9(09)).
     */
    @Column(name = "xref_cust_id", nullable = false)
    private Long xrefCustId;

    /**
     * Cross-reference account ID component of composite key.
     * Maps to XREF-ACCT-ID field from CVACT03Y.cpy (PIC 9(11)).
     */
    @Column(name = "xref_acct_id", nullable = false)
    private Long xrefAcctId;

    /**
     * Default constructor for JPA.
     */
    public CardXrefId() {
    }

    /**
     * Constructor with all fields.
     * 
     * @param xrefCardNum the cross-reference card number
     * @param xrefCustId the cross-reference customer ID
     * @param xrefAcctId the cross-reference account ID
     */
    public CardXrefId(String xrefCardNum, Long xrefCustId, Long xrefAcctId) {
        this.xrefCardNum = xrefCardNum;
        this.xrefCustId = xrefCustId;
        this.xrefAcctId = xrefAcctId;
    }

    // Getters and Setters

    public String getXrefCardNum() {
        return xrefCardNum;
    }

    public void setXrefCardNum(String xrefCardNum) {
        this.xrefCardNum = xrefCardNum;
    }

    public Long getXrefCustId() {
        return xrefCustId;
    }

    public void setXrefCustId(Long xrefCustId) {
        this.xrefCustId = xrefCustId;
    }

    public Long getXrefAcctId() {
        return xrefAcctId;
    }

    public void setXrefAcctId(Long xrefAcctId) {
        this.xrefAcctId = xrefAcctId;
    }

    // Object Methods

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CardXrefId that = (CardXrefId) obj;
        return Objects.equals(xrefCardNum, that.xrefCardNum) &&
               Objects.equals(xrefCustId, that.xrefCustId) &&
               Objects.equals(xrefAcctId, that.xrefAcctId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xrefCardNum, xrefCustId, xrefAcctId);
    }

    @Override
    public String toString() {
        return "CardXrefId{" +
                "xrefCardNum='" + xrefCardNum + '\'' +
                ", xrefCustId=" + xrefCustId +
                ", xrefAcctId=" + xrefAcctId +
                '}';
    }
}