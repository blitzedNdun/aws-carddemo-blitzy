/*
 * CardDemo Application
 * 
 * Transaction Category Entity
 * 
 * JPA entity class representing the transaction_categories table with composite
 * primary key implementation. Manages transaction categorization data replacing
 * VSAM TRANCATG reference file. Supports category hierarchy for transaction
 * classification and reporting operations.
 * 
 * Based on COBOL copybook: app/cpy/CVTRA04Y.cpy
 * 
 * Composite Primary Key Structure:
 * - category_code: 4-character category identifier (TRAN-CAT-CD)
 * - subcategory_code: 2-character subcategory identifier (TRAN-SUBCAT-CD)
 * 
 * This entity replaces VSAM TRANCATG reference file records with PostgreSQL
 * table mapping providing transaction categorization for processing and
 * reporting workflows.
 */

package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.util.Objects;

/**
 * JPA entity representing the transaction_categories table with composite primary key.
 * 
 * Maps transaction categorization data from COBOL copybook CVTRA04Y.cpy to PostgreSQL
 * table structure. Provides transaction classification supporting category hierarchy
 * and lookup operations for transaction processing and reporting.
 * 
 * Database Table: transaction_categories
 * COBOL Structure: TRAN-CAT-RECORD
 * VSAM File: TRANCATG
 * 
 * Composite Primary Key:
 * - category_code: 4-character alphanumeric category identifier
 * - subcategory_code: 2-character alphanumeric subcategory identifier
 * 
 * Features:
 * - Composite primary key using @EmbeddedId for category/subcategory codes
 * - JPA validation annotations for data integrity
 * - Hibernate caching support for reference data optimization
 * - COBOL field compatibility with PostgreSQL column mapping
 * - Transaction type code relationship for classification
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "transaction_categories",
       indexes = {
           @Index(name = "idx_transaction_categories_type_code", columnList = "transaction_type_code"),
           @Index(name = "idx_transaction_categories_description", columnList = "category_description"),
           @Index(name = "idx_transaction_categories_name", columnList = "category_name")
       })
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class TransactionCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Composite primary key containing category code and subcategory code.
     * Maps to COBOL fields TRAN-CAT-CD and TRAN-SUBCAT-CD.
     */
    @EmbeddedId
    private TransactionCategoryId id;

    /**
     * Transaction type code for classification relationship.
     * Maps to COBOL field TRAN-TYPE-CD (PIC X(02)).
     * Links to transaction_types table.
     */
    @Column(name = "transaction_type_code", length = 2, nullable = false)
    @NotNull(message = "Transaction type code is required")
    @Size(min = 2, max = 2, message = "Transaction type code must be exactly 2 characters")
    @Pattern(regexp = "^[A-Z0-9]{2}$", message = "Transaction type code must be 2 alphanumeric characters")
    private String transactionTypeCode;

    /**
     * Category description providing detailed category information.
     * Maps to COBOL field TRAN-CAT-DESC (PIC X(50)).
     */
    @Column(name = "category_description", length = 50, nullable = false)
    @NotNull(message = "Category description is required")
    @Size(min = 1, max = 50, message = "Category description must be between 1 and 50 characters")
    private String categoryDescription;

    /**
     * Category name for display purposes.
     * Maps to COBOL field TRAN-CAT-NAME (PIC X(25)).
     */
    @Column(name = "category_name", length = 25, nullable = false)
    @NotNull(message = "Category name is required")
    @Size(min = 1, max = 25, message = "Category name must be between 1 and 25 characters")
    private String categoryName;

    /**
     * Default constructor for JPA.
     */
    public TransactionCategory() {
    }

    /**
     * Constructor with composite key.
     * 
     * @param id the composite primary key
     */
    public TransactionCategory(TransactionCategoryId id) {
        this.id = id;
    }

    /**
     * Constructor with all required fields.
     * 
     * @param categoryCode the category code (4 characters)
     * @param subcategoryCode the subcategory code (2 characters)
     * @param transactionTypeCode the transaction type code (2 characters)
     * @param categoryDescription the category description (up to 50 characters)
     * @param categoryName the category name (up to 25 characters)
     */
    public TransactionCategory(String categoryCode, String subcategoryCode, 
                             String transactionTypeCode, String categoryDescription, 
                             String categoryName) {
        this.id = new TransactionCategoryId(categoryCode, subcategoryCode);
        this.transactionTypeCode = transactionTypeCode;
        this.categoryDescription = categoryDescription;
        this.categoryName = categoryName;
    }

    // Getters and Setters

    public TransactionCategoryId getId() {
        return id;
    }

    public void setId(TransactionCategoryId id) {
        this.id = id;
    }

    /**
     * Get the category code from the composite key.
     * 
     * @return the category code
     */
    public String getCategoryCode() {
        return id != null ? id.getCategoryCode() : null;
    }

    /**
     * Set the category code in the composite key.
     * 
     * @param categoryCode the category code to set
     */
    public void setCategoryCode(String categoryCode) {
        if (this.id == null) {
            this.id = new TransactionCategoryId();
        }
        this.id.setCategoryCode(categoryCode);
    }

    /**
     * Get the subcategory code from the composite key.
     * 
     * @return the subcategory code
     */
    public String getSubcategoryCode() {
        return id != null ? id.getSubcategoryCode() : null;
    }

    /**
     * Set the subcategory code in the composite key.
     * 
     * @param subcategoryCode the subcategory code to set
     */
    public void setSubcategoryCode(String subcategoryCode) {
        if (this.id == null) {
            this.id = new TransactionCategoryId();
        }
        this.id.setSubcategoryCode(subcategoryCode);
    }

    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    public void setTransactionTypeCode(String transactionTypeCode) {
        this.transactionTypeCode = transactionTypeCode;
    }

    public String getCategoryDescription() {
        return categoryDescription;
    }

    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    // Utility Methods

    /**
     * Get the full category identifier combining category and subcategory codes.
     * 
     * @return formatted category identifier (e.g., "FOOD.01")
     */
    public String getFullCategoryId() {
        if (id == null) {
            return null;
        }
        return id.getCategoryCode() + "." + id.getSubcategoryCode();
    }

    /**
     * Check if this is a primary category (subcategory code "00").
     * 
     * @return true if this is a primary category
     */
    public boolean isPrimaryCategory() {
        return id != null && "00".equals(id.getSubcategoryCode());
    }

    /**
     * Get display text for user interfaces.
     * 
     * @return formatted display text combining name and description
     */
    public String getDisplayText() {
        if (categoryName == null || categoryDescription == null) {
            return null;
        }
        return categoryName + " - " + categoryDescription;
    }

    /**
     * Check if this category applies to the specified transaction type.
     * 
     * @param typeCode the transaction type code to check
     * @return true if this category applies to the transaction type
     */
    public boolean appliesToTransactionType(String typeCode) {
        return transactionTypeCode != null && transactionTypeCode.equals(typeCode);
    }

    // Object Methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionCategory that = (TransactionCategory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TransactionCategory{" +
                "id=" + id +
                ", transactionTypeCode='" + transactionTypeCode + '\'' +
                ", categoryDescription='" + categoryDescription + '\'' +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }

    /**
     * Embedded composite primary key class for TransactionCategory entity.
     * 
     * Represents the composite key structure with category_code and subcategory_code.
     * Maps to COBOL fields TRAN-CAT-CD and TRAN-SUBCAT-CD from CVTRA04Y.cpy.
     */
    @Embeddable
    public static class TransactionCategoryId implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Category code (4 characters).
         * Maps to COBOL field TRAN-CAT-CD (PIC X(04)).
         */
        @Column(name = "category_code", length = 4, nullable = false)
        @NotNull(message = "Category code is required")
        @Size(min = 4, max = 4, message = "Category code must be exactly 4 characters")
        @Pattern(regexp = "^[A-Z0-9]{4}$", message = "Category code must be 4 alphanumeric characters")
        private String categoryCode;

        /**
         * Subcategory code (2 characters).
         * Maps to COBOL field TRAN-SUBCAT-CD (PIC X(02)).
         */
        @Column(name = "subcategory_code", length = 2, nullable = false)
        @NotNull(message = "Subcategory code is required")
        @Size(min = 2, max = 2, message = "Subcategory code must be exactly 2 characters")
        @Pattern(regexp = "^[A-Z0-9]{2}$", message = "Subcategory code must be 2 alphanumeric characters")
        private String subcategoryCode;

        /**
         * Default constructor for JPA.
         */
        public TransactionCategoryId() {
        }

        /**
         * Constructor with category and subcategory codes.
         * 
         * @param categoryCode the category code (4 characters)
         * @param subcategoryCode the subcategory code (2 characters)
         */
        public TransactionCategoryId(String categoryCode, String subcategoryCode) {
            this.categoryCode = categoryCode;
            this.subcategoryCode = subcategoryCode;
        }

        // Getters and Setters

        public String getCategoryCode() {
            return categoryCode;
        }

        public void setCategoryCode(String categoryCode) {
            this.categoryCode = categoryCode;
        }

        public String getSubcategoryCode() {
            return subcategoryCode;
        }

        public void setSubcategoryCode(String subcategoryCode) {
            this.subcategoryCode = subcategoryCode;
        }

        // Object Methods

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransactionCategoryId that = (TransactionCategoryId) o;
            return Objects.equals(categoryCode, that.categoryCode) &&
                   Objects.equals(subcategoryCode, that.subcategoryCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(categoryCode, subcategoryCode);
        }

        @Override
        public String toString() {
            return "TransactionCategoryId{" +
                    "categoryCode='" + categoryCode + '\'' +
                    ", subcategoryCode='" + subcategoryCode + '\'' +
                    '}';
        }
    }
}