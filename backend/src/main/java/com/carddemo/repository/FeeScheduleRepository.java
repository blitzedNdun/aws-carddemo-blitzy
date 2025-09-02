package com.carddemo.repository;

import com.carddemo.entity.FeeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for FeeSchedule entity providing data access operations 
 * for fee rates, schedules, and configuration. Supports fee calculation lookup by account type, 
 * fee type, and effective date ranges. Enables fee schedule management and historical fee rate tracking.
 * 
 * This repository replaces VSAM DISCGRP file operations from the original COBOL system,
 * providing equivalent functionality for fee schedule management and rate lookups.
 * 
 * Based on COBOL programs: CBACT04C (Interest Calculator)
 * Data structure: CVTRA02Y (Disclosure Group Record)
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024
 */
@Repository
public interface FeeScheduleRepository extends JpaRepository<FeeSchedule, Long> {

    /**
     * Finds fee schedules by fee type, account type, and effective date.
     * Returns fee schedules where the effective date is less than or equal to the specified date,
     * allowing lookup of current applicable fee rates.
     * 
     * Equivalent to COBOL logic in CBACT04C paragraph 1200-GET-INTEREST-RATE
     * for reading DISCGRP-FILE with composite key lookup.
     * 
     * @param feeType the fee type code (equivalent to DIS-TRAN-TYPE-CD)
     * @param accountType the account type code (equivalent to DIS-ACCT-GROUP-ID)
     * @param effectiveDate the date for fee rate lookup (must be >= fee schedule effective date)
     * @return list of matching fee schedules ordered by effective date descending
     */
    @Query("SELECT fs FROM FeeSchedule fs WHERE fs.feeType = :feeType " +
           "AND fs.accountType = :accountType " +
           "AND fs.effectiveDate <= :effectiveDate " +
           "ORDER BY fs.effectiveDate DESC")
    List<FeeSchedule> findByFeeTypeAndAccountTypeAndEffectiveDateLessThanEqual(
            @Param("feeType") String feeType,
            @Param("accountType") String accountType,
            @Param("effectiveDate") LocalDate effectiveDate);

    /**
     * Finds the most current fee schedule for a specific fee type and account type.
     * Returns the fee schedule with the latest effective date that is not in the future.
     * 
     * Used for real-time fee calculation during transaction processing.
     * 
     * @param feeType the fee type code
     * @param accountType the account type code
     * @param currentDate the current date for comparison
     * @return optional containing the current fee schedule, or empty if none found
     */
    @Query("SELECT fs FROM FeeSchedule fs WHERE fs.feeType = :feeType " +
           "AND fs.accountType = :accountType " +
           "AND fs.effectiveDate <= :currentDate " +
           "ORDER BY fs.effectiveDate DESC")
    List<FeeSchedule> findCurrentFeeScheduleList(
            @Param("feeType") String feeType,
            @Param("accountType") String accountType,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Finds the most current fee schedule for a specific fee type and account type.
     * Returns the fee schedule with the latest effective date that is not in the future.
     * 
     * Used for real-time fee calculation during transaction processing.
     * 
     * @param feeType the fee type code
     * @param accountType the account type code
     * @param currentDate the current date for comparison
     * @return optional containing the current fee schedule, or empty if none found
     */
    default Optional<FeeSchedule> findCurrentFeeSchedule(String feeType, String accountType, LocalDate currentDate) {
        List<FeeSchedule> schedules = findCurrentFeeScheduleList(feeType, accountType, currentDate);
        return schedules.isEmpty() ? Optional.empty() : Optional.of(schedules.get(0));
    }

    /**
     * Finds all fee schedules for a specific account type.
     * Used for fee schedule management and account configuration.
     * 
     * @param accountType the account type code
     * @return list of fee schedules for the account type
     */
    List<FeeSchedule> findByAccountTypeOrderByEffectiveDateDesc(String accountType);

    /**
     * Finds all fee schedules for a specific fee type.
     * Used for fee type analysis and rate comparison across account types.
     * 
     * @param feeType the fee type code
     * @return list of fee schedules for the fee type
     */
    List<FeeSchedule> findByFeeTypeOrderByEffectiveDateDesc(String feeType);

    /**
     * Finds fee schedules within a specific date range.
     * Used for historical reporting and fee schedule auditing.
     * 
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return list of fee schedules within the date range
     */
    @Query("SELECT fs FROM FeeSchedule fs WHERE fs.effectiveDate BETWEEN :startDate AND :endDate " +
           "ORDER BY fs.effectiveDate DESC, fs.accountType, fs.feeType")
    List<FeeSchedule> findByEffectiveDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Finds fee schedules with waiver conditions.
     * Returns fee schedules where waiver conditions are configured,
     * supporting conditional fee application logic.
     * 
     * @param hasWaiverConditions true to find schedules with waiver conditions, false for no waivers
     * @return list of fee schedules matching waiver condition criteria
     */
    @Query("SELECT fs FROM FeeSchedule fs WHERE " +
           "(:hasWaiverConditions = true AND fs.waiverConditions IS NOT NULL AND fs.waiverConditions != '') " +
           "OR (:hasWaiverConditions = false AND (fs.waiverConditions IS NULL OR fs.waiverConditions = '')) " +
           "ORDER BY fs.feeType, fs.accountType")
    List<FeeSchedule> findByHasWaiverConditions(@Param("hasWaiverConditions") boolean hasWaiverConditions);

    /**
     * Finds fee schedules by waiver threshold requirement.
     * Used for fee calculations based on account balance thresholds.
     * 
     * @param threshold the waiver threshold amount
     * @return list of fee schedules with waiver threshold less than or equal to the specified amount
     */
    @Query("SELECT fs FROM FeeSchedule fs WHERE fs.waiverThreshold <= :threshold " +
           "ORDER BY fs.waiverThreshold DESC")
    List<FeeSchedule> findByMinimumBalanceLessThanEqual(@Param("threshold") BigDecimal threshold);

    /**
     * Finds active fee schedules as of a specific date.
     * Returns fee schedules that are currently active (not expired) on the given date.
     * 
     * @param asOfDate the date to check for active status
     * @return list of active fee schedules
     */
    @Query("SELECT fs FROM FeeSchedule fs WHERE fs.effectiveDate <= :asOfDate " +
           "AND (fs.expirationDate IS NULL OR fs.expirationDate > :asOfDate) " +
           "ORDER BY fs.accountType, fs.feeType, fs.effectiveDate DESC")
    List<FeeSchedule> findActiveFeeSchedules(@Param("asOfDate") LocalDate asOfDate);

    /**
     * Finds fee schedules by fee amount range.
     * Used for fee analysis and rate comparison reporting.
     * 
     * @param minAmount the minimum fee amount (inclusive)
     * @param maxAmount the maximum fee amount (inclusive)
     * @return list of fee schedules within the amount range
     */
    @Query("SELECT fs FROM FeeSchedule fs WHERE fs.feeAmount BETWEEN :minAmount AND :maxAmount " +
           "ORDER BY fs.feeAmount")
    List<FeeSchedule> findByFeeRateBetween(
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount);

    /**
     * Checks if a fee schedule exists for the given criteria.
     * Used for validation before creating new fee schedules.
     * 
     * @param feeType the fee type code
     * @param accountType the account type code
     * @param effectiveDate the effective date
     * @return true if a matching fee schedule exists, false otherwise
     */
    boolean existsByFeeTypeAndAccountTypeAndEffectiveDate(
            String feeType, String accountType, LocalDate effectiveDate);

    /**
     * Counts fee schedules by account type.
     * Used for reporting and configuration analysis.
     * 
     * @param accountType the account type code
     * @return count of fee schedules for the account type
     */
    long countByAccountType(String accountType);

    /**
     * Finds fee schedules with upcoming effective dates.
     * Used for fee schedule implementation planning and notifications.
     * 
     * @param currentDate the current date for comparison
     * @param futureDays number of days in the future to look ahead
     * @return list of fee schedules becoming effective in the future period
     */
    @Query("SELECT fs FROM FeeSchedule fs WHERE fs.effectiveDate > :currentDate " +
           "AND fs.effectiveDate <= :futureDate " +
           "ORDER BY fs.effectiveDate, fs.accountType, fs.feeType")
    List<FeeSchedule> findUpcomingFeeSchedules(
            @Param("currentDate") LocalDate currentDate,
            @Param("futureDate") LocalDate futureDate);

    /**
     * Finds fee schedules by schedule ID for audit and history tracking.
     * Supports fee schedule versioning and historical analysis.
     * 
     * @param feeScheduleId the fee schedule ID (used as version identifier)
     * @return list of fee schedules for the specified schedule ID
     */
    List<FeeSchedule> findByFeeScheduleIdOrderByEffectiveDateDesc(Long feeScheduleId);

    /**
     * Deletes expired fee schedules before a specific date.
     * Used for data archival and cleanup operations.
     * 
     * @param expirationDate the cutoff date for deletion (exclusive)
     * @return number of deleted fee schedules
     */
    @Query("DELETE FROM FeeSchedule fs WHERE fs.expirationDate < :expirationDate")
    int deleteByExpirationDateBefore(@Param("expirationDate") LocalDate expirationDate);
}