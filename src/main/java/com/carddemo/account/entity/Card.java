package com.carddemo.account.entity;

import jakarta.persistence.*;

/**
 * Minimal Card entity stub for Customer.java validation testing
 * This is a temporary stub to allow Customer.java compilation and testing
 */
@Entity
@Table(name = "cards")
public class Card {
    
    @Id
    @Column(name = "card_id")
    private String cardId;
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
    
    // Default constructor
    public Card() {}
    
    // Minimal getters and setters
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
}