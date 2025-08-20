/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Objects;

/**
 * JPA entity class for card cross-reference data, mapped to card_xref PostgreSQL table.
 * 
 * This entity maintains relationships between cards, customers, and accounts, serving as a
 * junction table for many-to-many relationships. It contains card number, customer ID, and
 * account ID to support complex queries across these entities.
 * 
 * Maps to CARD-XREF-RECORD structure from CVACT03Y.cpy copybook:
 * - XREF-CARD-NUM (PIC X(16)) → xrefCardNum (VARCHAR(16))
 * - XREF-CUST-ID (PIC 9(09)) → xrefCustId (BIGINT)
 * - XREF-ACCT-ID (PIC 9(11)) → xrefAcctId (BIGINT)
 * 
 * The entity uses a composite primary key consisting of all three fields to ensure
 * uniqueness of card-customer-account relationships and prevent duplicate cross-references.
 * 
 * Key Features:
 * - Composite primary key using @EmbeddedId annotation
 * - @ManyToOne relationships to Card, Customer, and Account entities
 * - Comprehensive validation matching COBOL field specifications
 * - Support for complex cross-reference queries and reporting
 * - Junction table functionality for many-to-many relationships
 * 
 * Database Mapping:
 * - Table: card_xref
 * - Composite Primary Key: (xref_card_num, xref_cust_id, xref_acct_id)
 * - Foreign Keys: xref_card_num → card_data.card_number
 *                 xref_cust_id → customer_data.customer_id
 *                 xref_acct_id → account_data.account_id
 * 
 * This implementation preserves all business logic from the original COBOL programs
 * while leveraging modern JPA features for enhanced data integrity and relationship management.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Entity
@Table(name = "card_xref", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_card_xref_composite", 
                           columnNames = {"xref_card_num", "xref_cust_id", "xref_acct_id"})
       })
@Data
public class CardXref {

    /**
     * Composite primary key for card cross-reference.
     * Contains card number, customer ID, and account ID as a compound key.
     */
    @EmbeddedId
    private CardXrefId id;

    /**
     * Cross-reference card number.
     * Maps to XREF-CARD-NUM field from CVACT03Y.cpy (PIC X(16)).
     * Must be exactly 16 characters to match card number format.
     * Value is derived from the embedded ID.
     */
    @Column(name = "xref_card_num")
    @Size(max = 16, message = "Cross-reference card number cannot exceed 16 characters")
    private String xrefCardNum;

    /**
     * Cross-reference customer ID.
     * Maps to XREF-CUST-ID field from CVACT03Y.cpy (PIC 9(09)).
     * Links to customer_data table for customer relationship.
     * Value is derived from the embedded ID.
     */
    @Column(name = "xref_cust_id")
    private Long xrefCustId;

    /**
     * Cross-reference account ID.
     * Maps to XREF-ACCT-ID field from CVACT03Y.cpy (PIC 9(11)).
     * Links to account_data table for account relationship.
     * Value is derived from the embedded ID.
     */
    @Column(name = "xref_acct_id")
    private Long xrefAcctId;

    /**
     * Card entity relationship.
     * Many cross-references can reference one card.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("xrefCardNum")
    private Card card;

    /**
     * Customer entity relationship.
     * Many cross-references can reference one customer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("xrefCustId")
    private Customer customer;

    /**
     * Account entity relationship.
     * Many cross-references can reference one account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("xrefAcctId")
    private Account account;

    /**
     * Default constructor for JPA.
     */
    public CardXref() {
    }

    /**
     * Constructor with composite ID.
     * 
     * @param id the composite ID containing card number, customer ID, and account ID
     */
    public CardXref(CardXrefId id) {
        this.id = id;
        if (id != null) {
            this.xrefCardNum = id.getXrefCardNum();
            this.xrefCustId = id.getXrefCustId();
            this.xrefAcctId = id.getXrefAcctId();
        }
    }

    /**
     * Constructor with all fields.
     * 
     * @param xrefCardNum the cross-reference card number
     * @param xrefCustId the cross-reference customer ID
     * @param xrefAcctId the cross-reference account ID
     */
    public CardXref(String xrefCardNum, Long xrefCustId, Long xrefAcctId) {
        this.id = new CardXrefId(xrefCardNum, xrefCustId, xrefAcctId);
        this.xrefCardNum = xrefCardNum;
        this.xrefCustId = xrefCustId;
        this.xrefAcctId = xrefAcctId;
    }

    // Getters and Setters

    public CardXrefId getId() {
        return id;
    }

    public void setId(CardXrefId id) {
        this.id = id;
        if (id != null) {
            this.xrefCardNum = id.getXrefCardNum();
            this.xrefCustId = id.getXrefCustId();
            this.xrefAcctId = id.getXrefAcctId();
        }
    }

    public String getXrefCardNum() {
        return xrefCardNum;
    }

    public void setXrefCardNum(String xrefCardNum) {
        this.xrefCardNum = xrefCardNum;
        if (this.id != null) {
            this.id.setXrefCardNum(xrefCardNum);
        } else {
            this.id = new CardXrefId(xrefCardNum, this.xrefCustId, this.xrefAcctId);
        }
    }

    public Long getXrefCustId() {
        return xrefCustId;
    }

    public void setXrefCustId(Long xrefCustId) {
        this.xrefCustId = xrefCustId;
        if (this.id != null) {
            this.id.setXrefCustId(xrefCustId);
        } else {
            this.id = new CardXrefId(this.xrefCardNum, xrefCustId, this.xrefAcctId);
        }
    }

    public Long getXrefAcctId() {
        return xrefAcctId;
    }

    public void setXrefAcctId(Long xrefAcctId) {
        this.xrefAcctId = xrefAcctId;
        if (this.id != null) {
            this.id.setXrefAcctId(xrefAcctId);
        } else {
            this.id = new CardXrefId(this.xrefCardNum, this.xrefCustId, xrefAcctId);
        }
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
        if (card != null) {
            setXrefCardNum(card.getCardNumber());
        }
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
        if (customer != null) {
            setXrefCustId(customer.getCustomerId());
        }
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            setXrefAcctId(account.getAccountId());
        }
    }

    // Object Methods

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CardXref cardXref = (CardXref) obj;
        return Objects.equals(id, cardXref.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CardXref{" +
                "id=" + id +
                ", xrefCardNum='" + xrefCardNum + '\'' +
                ", xrefCustId=" + xrefCustId +
                ", xrefAcctId=" + xrefAcctId +
                '}';
    }
}

