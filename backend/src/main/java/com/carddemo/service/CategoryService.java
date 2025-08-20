/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.repository.TransactionCategoryRepository;
import com.carddemo.repository.TransactionTypeRepository;
import com.carddemo.dto.TransactionCategoryBalanceDto;
import com.carddemo.dto.TransactionTypeDto;
import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.entity.TransactionCategory;
import com.carddemo.entity.TransactionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer component providing business logic for transaction category management 
 * including category balance calculations, transaction type lookups, and category 
 * reference data operations. Implements category aggregation queries and balance 
 * tracking functionality that supports REST controller operations and batch processing 
 * requirements.
 * 
 * This service replaces COBOL paragraph-level business logic from programs that handled
 * transaction categorization, balance tracking, and reference data operations. Provides
 * a centralized service layer for category management operations across the application.
 * 
 * Key Features:
 * - Transaction category reference data management
 * - Category-wise balance calculations and aggregations
 * - Transaction type lookup and validation services
 * - Account-specific category balance tracking
 * - Date-range balance analysis and reporting
 * - Integration with Spring transaction management
 * 
 * Based on COBOL data structures:
 * - TRAN-CAT-BAL-RECORD from CVTRA01Y.cpy (category balance tracking)
 * - TRAN-TYPE-RECORD from CVTRA03Y.cpy (transaction type lookup)
 * - VSAM TRANCATG reference file operations
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final TransactionCategoryBalanceRepository categoryBalanceRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final TransactionTypeRepository typeRepository;

    /**
     * Constructor with dependency injection for all required repositories.
     * 
     * @param categoryBalanceRepository repository for category balance data access
     * @param categoryRepository repository for category reference data access
     * @param typeRepository repository for transaction type reference data access
     */
    @Autowired
    public CategoryService(TransactionCategoryBalanceRepository categoryBalanceRepository,
                          TransactionCategoryRepository categoryRepository,
                          TransactionTypeRepository typeRepository) {
        this.categoryBalanceRepository = categoryBalanceRepository;
        this.categoryRepository = categoryRepository;
        this.typeRepository = typeRepository;
    }

    /**
     * Retrieves all transaction categories from the reference data.
     * Provides complete category hierarchy information for transaction classification
     * and user interface population. Replaces COBOL TRANCATG file read operations.
     * 
     * This method corresponds to COBOL paragraph 2000-READ-CATEGORY-FILE that
     * reads all category records for dropdown population and validation.
     * 
     * @return List of all transaction categories with category codes, names, and descriptions
     * @throws RuntimeException if database access fails
     */
    public List<TransactionCategory> getAllCategories() {
        try {
            // Use findAll() from TransactionCategoryRepository
            List<TransactionCategory> categories = categoryRepository.findAll();
            
            // Validate that each category has required fields populated
            categories.forEach(category -> {
                validateCategoryData(category);
            });
            
            // Log the operation for audit purposes including category details
            logCategoryOperation("getAllCategories", categories.size(), 
                String.format("Categories retrieved with codes: %s", 
                    categories.stream()
                        .map(TransactionCategory::getCategoryCode)
                        .collect(Collectors.joining(", "))));
            
            return categories;
        } catch (Exception e) {
            logError("getAllCategories", "Failed to retrieve all categories", e);
            throw new RuntimeException("Failed to retrieve transaction categories", e);
        }
    }

    /**
     * Retrieves category balances for a specific account.
     * Provides account-specific category balance information for balance inquiries,
     * statement generation, and account analysis. Replaces COBOL balance calculation
     * paragraphs that aggregate category balances by account.
     * 
     * This method corresponds to COBOL paragraph 3000-CALC-CATEGORY-BALANCES that
     * reads balance records and calculates current balances by category.
     * 
     * @param accountId the account ID to retrieve balances for (11-digit account number)
     * @return List of category balance DTOs with current balance information
     * @throws IllegalArgumentException if accountId is null or invalid
     * @throws RuntimeException if database access fails
     */
    public List<TransactionCategoryBalanceDto> getCategoryBalances(String accountId) {
        validateAccountId(accountId);
        
        try {
            Long accountIdLong = Long.parseLong(accountId);
            
            // Use findByAccountIdOrderByBalanceDateDesc() to get latest balances
            List<TransactionCategoryBalance> balanceRecords = 
                categoryBalanceRepository.findByAccountIdOrderByBalanceDateDesc(accountIdLong, null).getContent();
            
            // Create DTOs from entities - using manual conversion since DTOs may not exist yet
            List<TransactionCategoryBalanceDto> balanceDtos = balanceRecords.stream()
                .map(this::convertToBalanceDto)
                .collect(Collectors.toList());
            
            // Log the operation for audit purposes
            logCategoryOperation("getCategoryBalances", balanceDtos.size(), accountId);
            
            return balanceDtos;
        } catch (NumberFormatException e) {
            logError("getCategoryBalances", "Invalid account ID format: " + accountId, e);
            throw new IllegalArgumentException("Account ID must be a valid numeric value", e);
        } catch (Exception e) {
            logError("getCategoryBalances", "Failed to retrieve category balances for account: " + accountId, e);
            throw new RuntimeException("Failed to retrieve category balances for account: " + accountId, e);
        }
    }

    /**
     * Retrieves all transaction type reference data.
     * Provides complete transaction type information for transaction validation,
     * categorization, and user interface population. Replaces COBOL TRANTYPE 
     * file read operations.
     * 
     * This method corresponds to COBOL paragraph 2000-READ-TRANTYPE-FILE that
     * reads all transaction type records for validation and classification.
     * 
     * @return List of all transaction types with codes, descriptions, and debit/credit flags
     * @throws RuntimeException if database access fails
     */
    public List<TransactionTypeDto> getAllTransactionTypes() {
        try {
            // Use findAll() from TransactionTypeRepository
            List<TransactionType> types = typeRepository.findAll();
            
            // Create DTOs from entities - using manual conversion since DTOs may not exist yet
            List<TransactionTypeDto> typeDtos = types.stream()
                .map(this::convertToTypeDto)
                .collect(Collectors.toList());
            
            // Log the operation for audit purposes
            logCategoryOperation("getAllTransactionTypes", typeDtos.size(), null);
            
            return typeDtos;
        } catch (Exception e) {
            logError("getAllTransactionTypes", "Failed to retrieve all transaction types", e);
            throw new RuntimeException("Failed to retrieve transaction types", e);
        }
    }

    /**
     * Aggregates category balances for an account within a specified date range.
     * Provides date-range balance analysis for reporting, statement generation,
     * and account analysis. Supports balance trend analysis and historical
     * balance inquiries. Replaces COBOL date-range balance calculation routines.
     * 
     * This method corresponds to COBOL paragraph 4000-AGGREGATE-BALANCES that
     * reads balance records within date ranges and calculates aggregated totals.
     * 
     * @param accountId the account ID to aggregate balances for (11-digit account number)
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (inclusive)
     * @return List of aggregated category balance DTOs within the specified date range
     * @throws IllegalArgumentException if parameters are null or invalid
     * @throws RuntimeException if database access fails
     */
    @Transactional(readOnly = true)
    public List<TransactionCategoryBalanceDto> aggregateCategoryBalances(String accountId, 
                                                                        LocalDate startDate, 
                                                                        LocalDate endDate) {
        validateAccountId(accountId);
        validateDateRange(startDate, endDate);
        
        try {
            Long accountIdLong = Long.parseLong(accountId);
            
            // Use findByAccountIdAndBalanceDateBetween() to get balances in date range
            List<TransactionCategoryBalance> balanceRecords = 
                categoryBalanceRepository.findByAccountIdAndBalanceDateBetween(
                    accountIdLong, startDate, endDate);
            
            // Aggregate balances by category code
            List<TransactionCategoryBalanceDto> aggregatedBalances = 
                aggregateBalancesByCategory(balanceRecords, accountIdLong);
            
            // Log the operation for audit purposes
            logCategoryOperation("aggregateCategoryBalances", aggregatedBalances.size(), 
                               accountId + " from " + startDate + " to " + endDate);
            
            return aggregatedBalances;
        } catch (NumberFormatException e) {
            logError("aggregateCategoryBalances", "Invalid account ID format: " + accountId, e);
            throw new IllegalArgumentException("Account ID must be a valid numeric value", e);
        } catch (Exception e) {
            logError("aggregateCategoryBalances", 
                   "Failed to aggregate category balances for account: " + accountId, e);
            throw new RuntimeException("Failed to aggregate category balances for account: " + accountId, e);
        }
    }

    // ========================================
    // PUBLIC UTILITY METHODS
    // ========================================

    /**
     * Retrieves a specific category by its category code.
     * Provides single category lookup for validation and detailed information display.
     * 
     * @param categoryCode the category code to look up
     * @return Optional containing the category if found, empty otherwise
     * @throws IllegalArgumentException if categoryCode is null or empty
     */
    public Optional<TransactionCategory> getCategoryByCode(String categoryCode) {
        validateCategoryCode(categoryCode);
        
        try {
            // Use findByCategoryCode() from TransactionCategoryRepository
            Optional<TransactionCategory> category = categoryRepository.findByCategoryCode(categoryCode);
            
            if (category.isPresent()) {
                validateCategoryData(category.get());
            }
            
            logCategoryOperation("getCategoryByCode", category.isPresent() ? 1 : 0, categoryCode);
            return category;
        } catch (Exception e) {
            logError("getCategoryByCode", "Failed to retrieve category: " + categoryCode, e);
            throw new RuntimeException("Failed to retrieve category: " + categoryCode, e);
        }
    }

    /**
     * Retrieves categories for a specific transaction type.
     * Provides filtered category lists for transaction type-specific operations.
     * 
     * @param transactionTypeCode the transaction type code to filter by
     * @return List of categories associated with the specified transaction type
     * @throws IllegalArgumentException if transactionTypeCode is null or empty
     */
    public List<TransactionCategory> getCategoriesForTransactionType(String transactionTypeCode) {
        validateTransactionTypeCode(transactionTypeCode);
        
        try {
            // Use findByTransactionTypeCodeOrderByIdCategoryCodeAsc() from TransactionCategoryRepository
            List<TransactionCategory> categories = 
                categoryRepository.findByTransactionTypeCodeOrderByIdCategoryCodeAsc(transactionTypeCode);
            
            // Validate category data and log details
            categories.forEach(this::validateCategoryData);
            
            logCategoryOperation("getCategoriesForTransactionType", categories.size(), 
                "Transaction type: " + transactionTypeCode);
            
            return categories;
        } catch (Exception e) {
            logError("getCategoriesForTransactionType", 
                "Failed to retrieve categories for transaction type: " + transactionTypeCode, e);
            throw new RuntimeException("Failed to retrieve categories for transaction type: " + transactionTypeCode, e);
        }
    }

    /**
     * Retrieves transaction types by debit/credit flag.
     * Provides filtered transaction type lists for debit or credit operations.
     * 
     * @param debitCreditFlag the debit/credit flag ('D' for debit, 'C' for credit)
     * @return List of transaction types with the specified debit/credit flag
     * @throws IllegalArgumentException if debitCreditFlag is invalid
     */
    public List<TransactionTypeDto> getTransactionTypesByDebitCredit(String debitCreditFlag) {
        validateDebitCreditFlag(debitCreditFlag);
        
        try {
            // Use findByDebitCreditFlag() from TransactionTypeRepository
            List<TransactionType> types = typeRepository.findByDebitCreditFlag(debitCreditFlag);
            
            // Convert to DTOs
            List<TransactionTypeDto> typeDtos = types.stream()
                .map(this::convertToTypeDto)
                .collect(Collectors.toList());
            
            logCategoryOperation("getTransactionTypesByDebitCredit", typeDtos.size(), 
                "Debit/Credit flag: " + debitCreditFlag);
            
            return typeDtos;
        } catch (Exception e) {
            logError("getTransactionTypesByDebitCredit", 
                "Failed to retrieve transaction types for flag: " + debitCreditFlag, e);
            throw new RuntimeException("Failed to retrieve transaction types for flag: " + debitCreditFlag, e);
        }
    }

    /**
     * Retrieves a specific transaction type by its code.
     * Provides single transaction type lookup for validation and detailed information display.
     * 
     * @param transactionTypeCode the transaction type code to look up
     * @return Optional containing the transaction type DTO if found, empty otherwise
     * @throws IllegalArgumentException if transactionTypeCode is null or empty
     */
    public Optional<TransactionTypeDto> getTransactionTypeByCode(String transactionTypeCode) {
        validateTransactionTypeCode(transactionTypeCode);
        
        try {
            // Use findByTransactionTypeCode() from TransactionTypeRepository
            Optional<TransactionType> type = typeRepository.findByTransactionTypeCode(transactionTypeCode);
            
            Optional<TransactionTypeDto> typeDto = type.map(this::convertToTypeDto);
            
            logCategoryOperation("getTransactionTypeByCode", typeDto.isPresent() ? 1 : 0, 
                transactionTypeCode);
            
            return typeDto;
        } catch (Exception e) {
            logError("getTransactionTypeByCode", 
                "Failed to retrieve transaction type: " + transactionTypeCode, e);
            throw new RuntimeException("Failed to retrieve transaction type: " + transactionTypeCode, e);
        }
    }

    /**
     * Calculates the total balance for a specific account and category combination.
     * Provides precise balance calculations for specific category analysis and reporting.
     * Uses database-level aggregation for optimal performance.
     * 
     * @param accountId the account ID to calculate balance for
     * @param categoryCode the category code to calculate balance for
     * @return the total balance amount for the account and category combination
     * @throws IllegalArgumentException if parameters are null or invalid
     * @throws RuntimeException if database access fails
     */
    @Transactional(readOnly = true)
    public BigDecimal getCategoryBalanceSum(String accountId, String categoryCode) {
        validateAccountId(accountId);
        validateCategoryCode(categoryCode);
        
        try {
            Long accountIdLong = Long.parseLong(accountId);
            
            // Use sumBalanceByAccountIdAndCategoryCode() from TransactionCategoryBalanceRepository
            BigDecimal totalBalance = categoryBalanceRepository.sumBalanceByAccountIdAndCategoryCode(
                accountIdLong, categoryCode);
            
            // Handle null result (no records found)
            if (totalBalance == null) {
                totalBalance = BigDecimal.ZERO;
            }
            
            logCategoryOperation("getCategoryBalanceSum", 1, 
                String.format("Account: %s, Category: %s, Total: %s", 
                    accountId, categoryCode, totalBalance));
            
            return totalBalance;
        } catch (NumberFormatException e) {
            logError("getCategoryBalanceSum", "Invalid account ID format: " + accountId, e);
            throw new IllegalArgumentException("Account ID must be a valid numeric value", e);
        } catch (Exception e) {
            logError("getCategoryBalanceSum", 
                String.format("Failed to calculate balance sum for account %s, category %s", 
                    accountId, categoryCode), e);
            throw new RuntimeException("Failed to calculate category balance sum", e);
        }
    }

    /**
     * Retrieves specific category balance for an account and category combination.
     * Provides detailed balance information with the most recent balance record.
     * 
     * @param accountId the account ID to retrieve balance for
     * @param categoryCode the category code to retrieve balance for
     * @return Optional containing the category balance DTO if found, empty otherwise
     * @throws IllegalArgumentException if parameters are null or invalid
     * @throws RuntimeException if database access fails
     */
    public Optional<TransactionCategoryBalanceDto> getCategoryBalance(String accountId, String categoryCode) {
        validateAccountId(accountId);
        validateCategoryCode(categoryCode);
        
        try {
            Long accountIdLong = Long.parseLong(accountId);
            
            // Use findByAccountIdAndCategoryCode() from TransactionCategoryBalanceRepository
            Optional<TransactionCategoryBalance> balanceRecord = 
                categoryBalanceRepository.findByAccountIdAndCategoryCode(accountIdLong, categoryCode);
            
            Optional<TransactionCategoryBalanceDto> balanceDto = balanceRecord.map(this::convertToBalanceDto);
            
            logCategoryOperation("getCategoryBalance", balanceDto.isPresent() ? 1 : 0, 
                String.format("Account: %s, Category: %s", accountId, categoryCode));
            
            return balanceDto;
        } catch (NumberFormatException e) {
            logError("getCategoryBalance", "Invalid account ID format: " + accountId, e);
            throw new IllegalArgumentException("Account ID must be a valid numeric value", e);
        } catch (Exception e) {
            logError("getCategoryBalance", 
                String.format("Failed to retrieve balance for account %s, category %s", 
                    accountId, categoryCode), e);
            throw new RuntimeException("Failed to retrieve category balance", e);
        }
    }

    // ========================================
    // PRIVATE UTILITY METHODS
    // ========================================

    /**
     * Converts TransactionCategoryBalance entity to DTO.
     * Creates a new TransactionCategoryBalanceDto instance with proper field mapping.
     * Uses all required entity getters as per members_accessed schema.
     * 
     * @param balance the entity to convert
     * @return the converted DTO
     * @throws IllegalArgumentException if entity data is invalid
     */
    private TransactionCategoryBalanceDto convertToBalanceDto(TransactionCategoryBalance balance) {
        if (balance == null) {
            throw new IllegalArgumentException("TransactionCategoryBalance entity cannot be null");
        }
        
        // Use all required TransactionCategoryBalance getters as per members_accessed
        Long accountId = balance.getAccountId();
        String categoryCode = balance.getCategoryCode();
        String typeCode = balance.getTypeCode();
        BigDecimal balanceAmount = balance.getBalance();
        LocalDate balanceDate = balance.getBalanceDate();
        
        // Validate required fields
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID is required");
        }
        
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Category code is required");
        }
        
        if (balanceAmount == null) {
            throw new IllegalArgumentException("Balance amount is required");
        }
        
        if (balanceDate == null) {
            throw new IllegalArgumentException("Balance date is required");
        }
        
        // Log balance date information for audit trail
        logCategoryOperation("convertToBalanceDto", 1, 
            String.format("Account: %d, Category: %s, Date: %s", accountId, categoryCode, balanceDate));
        
        // Map entity fields to DTO constructor parameters
        return new TransactionCategoryBalanceDto(
            accountId,
            typeCode, // Use actual typeCode from entity
            categoryCode,
            balanceAmount
        );
    }

    /**
     * Converts TransactionType entity to DTO.
     * Creates a new TransactionTypeDto instance with proper field mapping.
     * Uses all required entity getters as per members_accessed schema.
     * 
     * @param type the entity to convert
     * @return the converted DTO
     * @throws IllegalArgumentException if entity data is invalid
     */
    private TransactionTypeDto convertToTypeDto(TransactionType type) {
        if (type == null) {
            throw new IllegalArgumentException("TransactionType entity cannot be null");
        }
        
        // Use all required TransactionType getters as per members_accessed
        String typeCode = type.getTransactionTypeCode();
        String typeDescription = type.getTypeDescription();
        String debitCreditFlag = type.getDebitCreditFlag();
        
        // Validate required fields
        if (typeCode == null || typeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction type code is required");
        }
        
        if (typeDescription == null || typeDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction type description is required");
        }
        
        // Validate debit/credit flag if present
        if (debitCreditFlag != null && !debitCreditFlag.isEmpty()) {
            validateDebitCreditFlag(debitCreditFlag);
        }
        
        // Map entity fields to DTO constructor parameters
        return new TransactionTypeDto(
            typeCode,
            typeDescription
        );
    }

    /**
     * Aggregates balance records by category code, summing balances for each category.
     * 
     * @param balanceRecords the balance records to aggregate
     * @param accountId the account ID for the aggregation
     * @return list of aggregated balance DTOs
     */
    private List<TransactionCategoryBalanceDto> aggregateBalancesByCategory(
            List<TransactionCategoryBalance> balanceRecords, Long accountId) {
        
        // Group by category code and sum balances
        return balanceRecords.stream()
            .collect(Collectors.groupingBy(
                TransactionCategoryBalance::getCategoryCode,
                Collectors.reducing(BigDecimal.ZERO, 
                    TransactionCategoryBalance::getBalance, 
                    BigDecimal::add)))
            .entrySet().stream()
            .map(entry -> new TransactionCategoryBalanceDto(
                accountId,
                null, // typeCode - not available in aggregation
                entry.getKey(),
                entry.getValue()))
            .collect(Collectors.toList());
    }

    /**
     * Validates account ID parameter.
     * 
     * @param accountId the account ID to validate
     * @throws IllegalArgumentException if accountId is invalid
     */
    private void validateAccountId(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }
        
        if (!accountId.matches("^\\d{11}$")) {
            throw new IllegalArgumentException("Account ID must be exactly 11 digits");
        }
    }

    /**
     * Validates date range parameters.
     * 
     * @param startDate the start date to validate
     * @param endDate the end date to validate
     * @throws IllegalArgumentException if date range is invalid
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        
        if (endDate == null) {
            throw new IllegalArgumentException("End date is required");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        
        // Don't allow future dates
        LocalDate today = LocalDate.now();
        if (startDate.isAfter(today)) {
            throw new IllegalArgumentException("Start date cannot be in the future");
        }
        
        if (endDate.isAfter(today)) {
            throw new IllegalArgumentException("End date cannot be in the future");
        }
    }

    /**
     * Validates category code parameter.
     * 
     * @param categoryCode the category code to validate
     * @throws IllegalArgumentException if categoryCode is invalid
     */
    private void validateCategoryCode(String categoryCode) {
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Category code is required");
        }
        
        if (!categoryCode.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("Category code must be exactly 4 digits");
        }
    }

    /**
     * Validates transaction type code parameter.
     * 
     * @param transactionTypeCode the transaction type code to validate
     * @throws IllegalArgumentException if transactionTypeCode is invalid
     */
    private void validateTransactionTypeCode(String transactionTypeCode) {
        if (transactionTypeCode == null || transactionTypeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction type code is required");
        }
        
        if (!transactionTypeCode.matches("^[A-Z0-9]{2}$")) {
            throw new IllegalArgumentException("Transaction type code must be exactly 2 alphanumeric characters");
        }
    }

    /**
     * Validates debit/credit flag parameter.
     * 
     * @param debitCreditFlag the debit/credit flag to validate
     * @throws IllegalArgumentException if debitCreditFlag is invalid
     */
    private void validateDebitCreditFlag(String debitCreditFlag) {
        if (debitCreditFlag == null || debitCreditFlag.trim().isEmpty()) {
            throw new IllegalArgumentException("Debit/Credit flag is required");
        }
        
        if (!debitCreditFlag.equals("D") && !debitCreditFlag.equals("C")) {
            throw new IllegalArgumentException("Debit/Credit flag must be 'D' or 'C'");
        }
    }

    /**
     * Validates category data completeness and consistency.
     * Uses all required entity getters for comprehensive validation.
     * 
     * @param category the category to validate
     * @throws IllegalArgumentException if category data is invalid
     */
    private void validateCategoryData(TransactionCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        
        // Use all required TransactionCategory getters as per members_accessed
        String categoryCode = category.getCategoryCode();
        String subcategoryCode = category.getSubcategoryCode();
        String categoryName = category.getCategoryName();
        String categoryDescription = category.getCategoryDescription();
        
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Category code is required");
        }
        
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required");
        }
        
        // Subcategory code and description are optional but validate if present
        if (subcategoryCode != null && !subcategoryCode.trim().isEmpty()) {
            if (!subcategoryCode.matches("^\\d{2}$")) {
                throw new IllegalArgumentException("Subcategory code must be exactly 2 digits when provided");
            }
        }
        
        if (categoryDescription != null && categoryDescription.length() > 100) {
            throw new IllegalArgumentException("Category description cannot exceed 100 characters");
        }
    }

    /**
     * Logs category service operations for audit and monitoring purposes.
     * 
     * @param operation the operation name
     * @param recordCount the number of records processed
     * @param parameters the operation parameters
     */
    private void logCategoryOperation(String operation, int recordCount, String parameters) {
        // Use standard logging framework for operation tracking
        System.out.println(String.format(
            "CategoryService.%s: Processed %d records. Parameters: %s", 
            operation, recordCount, parameters));
    }

    /**
     * Logs error conditions for troubleshooting and monitoring.
     * 
     * @param operation the operation that failed
     * @param message the error message
     * @param exception the exception that occurred
     */
    private void logError(String operation, String message, Exception exception) {
        // Use standard logging framework for error tracking
        System.err.println(String.format(
            "CategoryService.%s: ERROR - %s. Exception: %s", 
            operation, message, exception.getMessage()));
    }
}