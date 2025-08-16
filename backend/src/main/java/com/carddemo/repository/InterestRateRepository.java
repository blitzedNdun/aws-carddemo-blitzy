package com.carddemo.repository;

import com.carddemo.entity.InterestRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for InterestRate entity.
 * Provides comprehensive data access methods for interest rate management including 
 * rate lookups by account group, transaction category, effective date ranges, 
 * promotional rate queries, and historical rate tracking.
 * 
 * This repository supports the interest calculation functionality migrated from 
 * COBOL program CBACT04C which performs interest rate lookups from DISCGRP-FILE
 * using account group ID, transaction type code, and transaction category code.
 */
@Repository
public interface InterestRateRepository extends JpaRepository<InterestRate, Long> {

    /**
     * Find interest rates by account group ID.
     * Supports the primary lookup pattern from COBOL CBACT04C where rates are
     * retrieved based on account group classification.
     * 
     * @param accountGroupId the account group identifier
     * @return list of interest rates for the specified account group
     */
    List<InterestRate> findByAccountGroupId(String accountGroupId);

    /**
     * Find active interest rates within a specified date range.
     * Retrieves rates where the effective date is on or before the end date
     * and the expiration date is on or after the start date.
     * 
     * @param startDate the start date for the range query
     * @param endDate the end date for the range query
     * @return list of active interest rates within the date range
     */
    @Query("SELECT ir FROM InterestRate ir WHERE ir.effectiveDate <= :endDate AND (ir.expirationDate IS NULL OR ir.expirationDate >= :startDate)")
    List<InterestRate> findActiveRatesByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find promotional interest rates.
     * Retrieves rates that are marked as promotional offers with special terms.
     * 
     * @return list of promotional interest rates
     */
    @Query("SELECT ir FROM InterestRate ir WHERE ir.promotionalRate IS NOT NULL AND ir.expirationDate >= CURRENT_DATE")
    List<InterestRate> findPromotionalRates();

    /**
     * Find historical interest rates for an account group.
     * Retrieves all rates including expired ones for audit and tracking purposes.
     * 
     * @param accountGroupId the account group identifier
     * @return list of historical interest rates ordered by effective date descending
     */
    @Query("SELECT ir FROM InterestRate ir WHERE ir.accountGroupId = :accountGroupId ORDER BY ir.effectiveDate DESC")
    List<InterestRate> findRateHistory(@Param("accountGroupId") String accountGroupId);

    /**
     * Find interest rates by account group ID and transaction type code.
     * Supports the specific lookup pattern from COBOL where rates are determined
     * by both account group and transaction type classification.
     * 
     * @param accountGroupId the account group identifier
     * @param transactionTypeCode the transaction type code
     * @return list of interest rates matching the criteria
     */
    List<InterestRate> findByAccountGroupIdAndTransactionTypeCode(String accountGroupId, String transactionTypeCode);

    /**
     * Find interest rates by effective and expiration date criteria.
     * Retrieves rates that are active for a specific date by checking
     * that the date falls between effective and expiration dates.
     * 
     * @param effectiveDate the date to check against effective date
     * @param expirationDate the date to check against expiration date
     * @return list of active interest rates for the specified date
     */
    List<InterestRate> findByEffectiveDateLessThanEqualAndExpirationDateGreaterThanEqual(LocalDate effectiveDate, LocalDate expirationDate);

    /**
     * Find current active interest rate for specific account group and transaction criteria.
     * Replicates the COBOL logic for finding the most appropriate rate including
     * fallback to DEFAULT account group if no specific rate is found.
     * 
     * @param accountGroupId the account group identifier
     * @param transactionTypeCode the transaction type code
     * @param transactionCategoryCode the transaction category code
     * @param currentDate the date to check for rate validity
     * @return optional containing the applicable interest rate
     */
    @Query("SELECT ir FROM InterestRate ir WHERE ir.accountGroupId = :accountGroupId " +
           "AND ir.transactionTypeCode = :transactionTypeCode " +
           "AND ir.transactionCategoryCode = :transactionCategoryCode " +
           "AND ir.effectiveDate <= :currentDate AND (ir.expirationDate IS NULL OR ir.expirationDate >= :currentDate) " +
           "ORDER BY ir.effectiveDate DESC")
    Optional<InterestRate> findCurrentRate(@Param("accountGroupId") String accountGroupId,
                                         @Param("transactionTypeCode") String transactionTypeCode,
                                         @Param("transactionCategoryCode") Integer transactionCategoryCode,
                                         @Param("currentDate") LocalDate currentDate);

    /**
     * Find default interest rate as fallback when specific account group rate is not found.
     * Implements the COBOL fallback logic from CBACT04C section 1200-A-GET-DEFAULT-INT-RATE.
     * 
     * @param transactionTypeCode the transaction type code
     * @param transactionCategoryCode the transaction category code
     * @param currentDate the date to check for rate validity
     * @return optional containing the default interest rate
     */
    @Query("SELECT ir FROM InterestRate ir WHERE ir.accountGroupId = 'DEFAULT' " +
           "AND ir.transactionTypeCode = :transactionTypeCode " +
           "AND ir.transactionCategoryCode = :transactionCategoryCode " +
           "AND ir.effectiveDate <= :currentDate AND (ir.expirationDate IS NULL OR ir.expirationDate >= :currentDate) " +
           "ORDER BY ir.effectiveDate DESC")
    Optional<InterestRate> findDefaultRate(@Param("transactionTypeCode") String transactionTypeCode,
                                         @Param("transactionCategoryCode") Integer transactionCategoryCode,
                                         @Param("currentDate") LocalDate currentDate);

    /**
     * Find interest rates by transaction category code.
     * Supports queries based on transaction category classification.
     * 
     * @param transactionCategoryCode the transaction category code
     * @return list of interest rates for the specified transaction category
     */
    List<InterestRate> findByTransactionCategoryCode(Integer transactionCategoryCode);

    /**
     * Find all active interest rates as of a specific date.
     * Retrieves all rates that are currently valid for application.
     * 
     * @param asOfDate the date to check for rate validity
     * @return list of all active interest rates
     */
    @Query("SELECT ir FROM InterestRate ir WHERE ir.effectiveDate <= :asOfDate AND (ir.expirationDate IS NULL OR ir.expirationDate >= :asOfDate)")
    List<InterestRate> findAllActiveRates(@Param("asOfDate") LocalDate asOfDate);

    /**
     * Find interest rates within a specific rate range.
     * Supports queries for rates within minimum and maximum bounds.
     * 
     * @param minRate the minimum interest rate
     * @param maxRate the maximum interest rate
     * @return list of interest rates within the specified range
     */
    @Query("SELECT ir FROM InterestRate ir WHERE ir.currentApr >= :minRate AND ir.currentApr <= :maxRate")
    List<InterestRate> findByRateRange(@Param("minRate") BigDecimal minRate, @Param("maxRate") BigDecimal maxRate);

    /**
     * Find the most recent interest rate for a specific criteria combination.
     * Returns the latest rate based on effective date for the given parameters.
     * 
     * @param accountGroupId the account group identifier
     * @param transactionTypeCode the transaction type code
     * @param transactionCategoryCode the transaction category code
     * @return optional containing the most recent rate
     */
    @Query("SELECT ir FROM InterestRate ir WHERE ir.accountGroupId = :accountGroupId " +
           "AND ir.transactionTypeCode = :transactionTypeCode " +
           "AND ir.transactionCategoryCode = :transactionCategoryCode " +
           "ORDER BY ir.effectiveDate DESC")
    Optional<InterestRate> findMostRecentRate(@Param("accountGroupId") String accountGroupId,
                                            @Param("transactionTypeCode") String transactionTypeCode,
                                            @Param("transactionCategoryCode") Integer transactionCategoryCode);
}