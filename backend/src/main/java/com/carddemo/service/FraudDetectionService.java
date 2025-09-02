/*
 * FraudDetectionService.java
 * 
 * Fraud detection service for CardDemo application
 * Implements comprehensive fraud detection algorithms for transaction validation
 * 
 * Analyzes:
 * - Spending patterns and anomalies
 * - Geographic transaction velocity
 * - Merchant category restrictions 
 * - Transaction frequency monitoring
 * - Blacklist verification
 * - Risk scoring for authorization decisions
 * 
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class implementing fraud detection algorithms for transaction validation.
 * 
 * Provides comprehensive fraud detection capabilities including:
 * - Unusual spending pattern analysis
 * - Geographic velocity checks
 * - Merchant category validation
 * - Transaction frequency monitoring
 * - Blacklist verification
 * - Risk scoring and fraud alerts
 * 
 * Integrates with TransactionValidationService for real-time authorization decisions.
 */
@Service
public class FraudDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);
    
    // Fraud detection thresholds and constants
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("5000.00");
    private static final BigDecimal VELOCITY_AMOUNT_THRESHOLD = new BigDecimal("10000.00");
    private static final int MAX_TRANSACTIONS_PER_HOUR = 10;
    private static final int MAX_TRANSACTIONS_PER_DAY = 50;
    private static final int VELOCITY_CHECK_HOURS = 24;
    private static final double VELOCITY_DISTANCE_THRESHOLD_MILES = 500.0;
    private static final int RISK_SCORE_THRESHOLD = 70;
    
    // Risk score weights
    private static final int AMOUNT_RISK_WEIGHT = 25;
    private static final int FREQUENCY_RISK_WEIGHT = 20;
    private static final int VELOCITY_RISK_WEIGHT = 30;
    private static final int MERCHANT_RISK_WEIGHT = 15;
    private static final int BLACKLIST_RISK_WEIGHT = 50;
    
    // Restricted merchant categories (high-risk categories)
    private static final Set<String> RESTRICTED_MERCHANT_CATEGORIES = Set.of(
        "7995", // Gambling
        "4829", // Wire Transfer
        "6051", // Foreign Exchange
        "5933", // Pawn Shops
        "7273", // Dating Services
        "5912", // Drug Stores (for cash advances)
        "6211"  // Securities Brokers
    );
    
    // Known blacklisted patterns
    private static final Set<String> BLACKLISTED_MERCHANTS = Set.of(
        "FRAUD_MERCHANT_TEST",
        "SCAM_COMPANY_FAKE",
        "BLACKLIST_VENDOR"
    );
    
    // Geographic regions for velocity checks
    private static final Map<String, String> ZIP_TO_REGION = Map.of(
        "10001", "NY_METRO",
        "90210", "LA_METRO", 
        "60601", "CHICAGO",
        "33101", "MIAMI",
        "77001", "HOUSTON"
    );

    /**
     * Primary fraud detection orchestration method.
     * Coordinates all fraud detection checks and returns comprehensive results.
     * 
     * @param cardNumber Card number for the transaction
     * @param transactionAmount Transaction amount
     * @param merchantId Merchant identifier
     * @param merchantName Merchant name
     * @param merchantCity Merchant city
     * @param merchantZip Merchant ZIP code
     * @param transactionDate Transaction timestamp
     * @return FraudDetectionResult containing risk analysis
     */
    public FraudDetectionResult detectFraudPatterns(String cardNumber, 
                                                   BigDecimal transactionAmount,
                                                   String merchantId,
                                                   String merchantName, 
                                                   String merchantCity,
                                                   String merchantZip,
                                                   LocalDateTime transactionDate) {
        
        logger.info("Starting fraud detection for card: {} amount: {} merchant: {}", 
                   maskCardNumber(cardNumber), transactionAmount, merchantName);
        
        FraudDetectionResult result = new FraudDetectionResult();
        result.setCardNumber(cardNumber);
        result.setTransactionAmount(transactionAmount);
        result.setMerchantId(merchantId);
        result.setMerchantName(merchantName);
        result.setTransactionDate(transactionDate);
        
        // Perform all fraud detection checks
        SpendingPatternResult spendingResult = analyzeSpendingPatterns(cardNumber, transactionAmount, transactionDate);
        result.setSpendingPatternResult(spendingResult);
        
        VelocityCheckResult velocityResult = checkGeographicVelocity(cardNumber, merchantCity, merchantZip, transactionDate);
        result.setVelocityResult(velocityResult);
        
        MerchantValidationResult merchantResult = validateMerchantCategory(merchantId, merchantName, merchantCity);
        result.setMerchantResult(merchantResult);
        
        BlacklistCheckResult blacklistResult = checkBlacklist(cardNumber, merchantId, merchantName);
        result.setBlacklistResult(blacklistResult);
        
        FrequencyCheckResult frequencyResult = validateTransactionFrequency(cardNumber, transactionDate);
        result.setFrequencyResult(frequencyResult);
        
        // Calculate overall risk score
        int riskScore = calculateRiskScore(spendingResult, velocityResult, merchantResult, 
                                         blacklistResult, frequencyResult);
        result.setRiskScore(riskScore);
        
        // Determine if fraud alert should be generated
        boolean shouldAlert = (riskScore >= RISK_SCORE_THRESHOLD);
        result.setFraudAlert(shouldAlert);
        
        if (shouldAlert) {
            FraudAlert alert = generateFraudAlert(result);
            result.setAlert(alert);
            logger.warn("Fraud alert generated for card: {} risk score: {}", 
                       maskCardNumber(cardNumber), riskScore);
        }
        
        logger.info("Fraud detection completed for card: {} risk score: {} alert: {}", 
                   maskCardNumber(cardNumber), riskScore, shouldAlert);
        
        return result;
    }

    /**
     * Analyzes spending patterns to detect unusual transaction amounts.
     * Compares current transaction against historical spending patterns.
     * 
     * @param cardNumber Card number for analysis
     * @param transactionAmount Current transaction amount
     * @param transactionDate Transaction timestamp
     * @return SpendingPatternResult with analysis details
     */
    public SpendingPatternResult analyzeSpendingPatterns(String cardNumber, 
                                                       BigDecimal transactionAmount,
                                                       LocalDateTime transactionDate) {
        
        logger.debug("Analyzing spending patterns for card: {} amount: {}", 
                    maskCardNumber(cardNumber), transactionAmount);
        
        SpendingPatternResult result = new SpendingPatternResult();
        result.setCardNumber(cardNumber);
        result.setTransactionAmount(transactionAmount);
        
        // Get historical transaction data for the card (last 30 days)
        List<TransactionData> historicalTransactions = getHistoricalTransactions(cardNumber, 30);
        
        if (historicalTransactions.isEmpty()) {
            // New card or no transaction history
            result.setAverageAmount(BigDecimal.ZERO);
            result.setMaxAmount(BigDecimal.ZERO);
            result.setDeviationScore(0);
            result.setUnusualSpending(transactionAmount.compareTo(HIGH_AMOUNT_THRESHOLD) > 0);
            result.setRiskLevel(result.isUnusualSpending() ? "HIGH" : "LOW");
            return result;
        }
        
        // Calculate statistical measures
        BigDecimal totalAmount = historicalTransactions.stream()
            .map(TransactionData::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageAmount = totalAmount.divide(
            new BigDecimal(historicalTransactions.size()), 2, RoundingMode.HALF_UP);
        
        BigDecimal maxAmount = historicalTransactions.stream()
            .map(TransactionData::getAmount)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        result.setAverageAmount(averageAmount);
        result.setMaxAmount(maxAmount);
        
        // Calculate deviation score (0-100)
        BigDecimal deviation = transactionAmount.subtract(averageAmount).abs();
        int deviationScore = deviation.divide(averageAmount.max(BigDecimal.ONE), 2, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal(100)).intValue();
        deviationScore = Math.min(deviationScore, 100); // Cap at 100
        
        result.setDeviationScore(deviationScore);
        
        // Determine if spending is unusual
        boolean unusualSpending = detectUnusualSpending(transactionAmount, averageAmount, maxAmount);
        result.setUnusualSpending(unusualSpending);
        
        // Set risk level based on deviation and amount
        String riskLevel = determineSpendingRiskLevel(deviationScore, transactionAmount, unusualSpending);
        result.setRiskLevel(riskLevel);
        
        logger.debug("Spending pattern analysis complete - avg: {} max: {} deviation: {}% unusual: {}", 
                    averageAmount, maxAmount, deviationScore, unusualSpending);
        
        return result;
    }

    /**
     * Checks geographic velocity to detect impossible travel patterns.
     * Analyzes merchant locations against recent transaction history.
     * 
     * @param cardNumber Card number for analysis
     * @param merchantCity Current merchant city
     * @param merchantZip Current merchant ZIP code
     * @param transactionDate Transaction timestamp
     * @return VelocityCheckResult with geographic analysis
     */
    public VelocityCheckResult checkGeographicVelocity(String cardNumber,
                                                      String merchantCity,
                                                      String merchantZip,
                                                      LocalDateTime transactionDate) {
        
        logger.debug("Checking geographic velocity for card: {} location: {}, {}", 
                    maskCardNumber(cardNumber), merchantCity, merchantZip);
        
        VelocityCheckResult result = new VelocityCheckResult();
        result.setCardNumber(cardNumber);
        result.setCurrentLocation(merchantCity + ", " + merchantZip);
        result.setTransactionDate(transactionDate);
        
        // Get recent transactions (last 24 hours)
        List<TransactionData> recentTransactions = getRecentTransactionsByLocation(
            cardNumber, VELOCITY_CHECK_HOURS);
        
        if (recentTransactions.isEmpty()) {
            result.setVelocityViolation(false);
            result.setDistanceMiles(0.0);
            result.setTimeDifferenceHours(0.0);
            result.setRiskLevel("LOW");
            return result;
        }
        
        // Find the most recent transaction with location data
        TransactionData lastTransaction = recentTransactions.get(0);
        String lastLocation = lastTransaction.getMerchantCity() + ", " + lastTransaction.getMerchantZip();
        
        // Calculate geographic distance
        double distanceMiles = calculateDistance(
            lastTransaction.getMerchantCity(), lastTransaction.getMerchantZip(),
            merchantCity, merchantZip);
        
        // Calculate time difference
        double timeDifferenceHours = ChronoUnit.MINUTES.between(
            lastTransaction.getTransactionDate(), transactionDate) / 60.0;
        
        result.setLastLocation(lastLocation);
        result.setDistanceMiles(distanceMiles);
        result.setTimeDifferenceHours(timeDifferenceHours);
        
        // Check for velocity violations
        boolean velocityViolation = checkVelocityLimits(distanceMiles, timeDifferenceHours);
        result.setVelocityViolation(velocityViolation);
        
        // Determine risk level
        String riskLevel = determineVelocityRiskLevel(distanceMiles, timeDifferenceHours, velocityViolation);
        result.setRiskLevel(riskLevel);
        
        logger.debug("Geographic velocity check complete - distance: {} miles, time: {} hours, violation: {}", 
                    distanceMiles, timeDifferenceHours, velocityViolation);
        
        return result;
    }

    /**
     * Validates merchant category against restricted categories.
     * Checks for high-risk merchant types and suspicious merchant data.
     * 
     * @param merchantId Merchant identifier
     * @param merchantName Merchant name
     * @param merchantCity Merchant city
     * @return MerchantValidationResult with validation details
     */
    public MerchantValidationResult validateMerchantCategory(String merchantId,
                                                           String merchantName,
                                                           String merchantCity) {
        
        logger.debug("Validating merchant category - ID: {} name: {} city: {}", 
                    merchantId, merchantName, merchantCity);
        
        MerchantValidationResult result = new MerchantValidationResult();
        result.setMerchantId(merchantId);
        result.setMerchantName(merchantName);
        result.setMerchantCity(merchantCity);
        
        // Extract merchant category code (first 4 digits of merchant ID)
        String categoryCode = merchantId != null && merchantId.length() >= 4 
            ? merchantId.substring(0, 4) : "0000";
        result.setCategoryCode(categoryCode);
        
        // Check if category is restricted
        boolean restrictedCategory = RESTRICTED_MERCHANT_CATEGORIES.contains(categoryCode);
        result.setRestrictedCategory(restrictedCategory);
        
        // Validate merchant name for suspicious patterns
        boolean suspiciousMerchant = validateMerchantName(merchantName);
        result.setSuspiciousMerchant(suspiciousMerchant);
        
        // Check merchant location validity
        boolean validLocation = validateMerchantLocation(merchantCity);
        result.setValidLocation(validLocation);
        
        // Determine overall validation status
        boolean validMerchant = !restrictedCategory && !suspiciousMerchant && validLocation;
        result.setValidMerchant(validMerchant);
        
        // Set risk level
        String riskLevel = determineMerchantRiskLevel(restrictedCategory, suspiciousMerchant, validLocation);
        result.setRiskLevel(riskLevel);
        
        logger.debug("Merchant validation complete - category: {} restricted: {} suspicious: {} valid: {}", 
                    categoryCode, restrictedCategory, suspiciousMerchant, validMerchant);
        
        return result;
    }

    /**
     * Checks card number and merchant against blacklists.
     * Verifies against known fraud patterns and blocked entities.
     * 
     * @param cardNumber Card number to check
     * @param merchantId Merchant identifier
     * @param merchantName Merchant name
     * @return BlacklistCheckResult with blacklist verification details
     */
    public BlacklistCheckResult checkBlacklist(String cardNumber,
                                              String merchantId,
                                              String merchantName) {
        
        logger.debug("Checking blacklist for card: {} merchant: {}", 
                    maskCardNumber(cardNumber), merchantName);
        
        BlacklistCheckResult result = new BlacklistCheckResult();
        result.setCardNumber(cardNumber);
        result.setMerchantId(merchantId);
        result.setMerchantName(merchantName);
        
        // Check card blacklist
        boolean cardBlacklisted = isCardBlacklisted(cardNumber);
        result.setCardBlacklisted(cardBlacklisted);
        
        // Check merchant blacklist
        boolean merchantBlacklisted = isMerchantBlacklisted(merchantId, merchantName);
        result.setMerchantBlacklisted(merchantBlacklisted);
        
        // Check for known fraud patterns
        boolean fraudPattern = checkFraudPatterns(cardNumber, merchantName);
        result.setFraudPattern(fraudPattern);
        
        // Determine overall blacklist status
        boolean blacklisted = cardBlacklisted || merchantBlacklisted || fraudPattern;
        result.setBlacklisted(blacklisted);
        
        // Set risk level
        String riskLevel = blacklisted ? "CRITICAL" : "LOW";
        result.setRiskLevel(riskLevel);
        
        if (blacklisted) {
            result.setBlacklistReason(getBlacklistReason(cardBlacklisted, merchantBlacklisted, fraudPattern));
        }
        
        logger.debug("Blacklist check complete - card: {} merchant: {} fraud: {} overall: {}", 
                    cardBlacklisted, merchantBlacklisted, fraudPattern, blacklisted);
        
        return result;
    }

    /**
     * Calculates overall risk score based on all fraud detection results.
     * Combines weighted scores from different fraud detection components.
     * 
     * @param spendingResult Spending pattern analysis result
     * @param velocityResult Geographic velocity check result
     * @param merchantResult Merchant validation result
     * @param blacklistResult Blacklist check result
     * @param frequencyResult Transaction frequency check result
     * @return Overall risk score (0-100)
     */
    public int calculateRiskScore(SpendingPatternResult spendingResult,
                                 VelocityCheckResult velocityResult,
                                 MerchantValidationResult merchantResult,
                                 BlacklistCheckResult blacklistResult,
                                 FrequencyCheckResult frequencyResult) {
        
        logger.debug("Calculating overall risk score");
        
        int totalScore = 0;
        
        // Amount risk component
        int amountScore = calculateAmountRiskScore(spendingResult);
        totalScore += (amountScore * AMOUNT_RISK_WEIGHT) / 100;
        
        // Frequency risk component
        int frequencyScore = calculateFrequencyRiskScore(frequencyResult);
        totalScore += (frequencyScore * FREQUENCY_RISK_WEIGHT) / 100;
        
        // Velocity risk component
        int velocityScore = calculateVelocityRiskScore(velocityResult);
        totalScore += (velocityScore * VELOCITY_RISK_WEIGHT) / 100;
        
        // Merchant risk component
        int merchantScore = calculateMerchantRiskScore(merchantResult);
        totalScore += (merchantScore * MERCHANT_RISK_WEIGHT) / 100;
        
        // Blacklist risk component (highest priority)
        int blacklistScore = calculateBlacklistRiskScore(blacklistResult);
        totalScore += (blacklistScore * BLACKLIST_RISK_WEIGHT) / 100;
        
        // Ensure score stays within 0-100 range
        totalScore = Math.max(0, Math.min(100, totalScore));
        
        logger.debug("Risk score calculation complete - amount: {} frequency: {} velocity: {} merchant: {} blacklist: {} total: {}", 
                    amountScore, frequencyScore, velocityScore, merchantScore, blacklistScore, totalScore);
        
        return totalScore;
    }

    /**
     * Detects unusual spending behavior based on amount and patterns.
     * Analyzes transaction amount against historical data.
     * 
     * @param transactionAmount Current transaction amount
     * @param averageAmount Historical average amount
     * @param maxAmount Historical maximum amount
     * @return true if spending is considered unusual
     */
    public boolean detectUnusualSpending(BigDecimal transactionAmount,
                                       BigDecimal averageAmount,
                                       BigDecimal maxAmount) {
        
        // Check if amount exceeds high threshold
        if (transactionAmount.compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            return true;
        }
        
        // Check if amount is significantly higher than average
        if (averageAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal multiplier = transactionAmount.divide(averageAmount, 2, RoundingMode.HALF_UP);
            if (multiplier.compareTo(new BigDecimal("5.0")) > 0) {
                return true;
            }
        }
        
        // Check if amount exceeds previous maximum by significant margin
        if (maxAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal multiplier = transactionAmount.divide(maxAmount, 2, RoundingMode.HALF_UP);
            if (multiplier.compareTo(new BigDecimal("2.0")) > 0) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Validates transaction frequency against normal patterns.
     * Checks for excessive transaction volume in time periods.
     * 
     * @param cardNumber Card number for analysis
     * @param transactionDate Transaction timestamp
     * @return FrequencyCheckResult with frequency analysis
     */
    public FrequencyCheckResult validateTransactionFrequency(String cardNumber,
                                                           LocalDateTime transactionDate) {
        
        logger.debug("Validating transaction frequency for card: {}", maskCardNumber(cardNumber));
        
        FrequencyCheckResult result = new FrequencyCheckResult();
        result.setCardNumber(cardNumber);
        result.setTransactionDate(transactionDate);
        
        // Count transactions in last hour
        int transactionsLastHour = countTransactionsInPeriod(cardNumber, transactionDate, 1);
        result.setTransactionsLastHour(transactionsLastHour);
        
        // Count transactions in last day
        int transactionsLastDay = countTransactionsInPeriod(cardNumber, transactionDate, 24);
        result.setTransactionsLastDay(transactionsLastDay);
        
        // Check for frequency violations
        boolean hourlyViolation = transactionsLastHour >= MAX_TRANSACTIONS_PER_HOUR;
        boolean dailyViolation = transactionsLastDay >= MAX_TRANSACTIONS_PER_DAY;
        
        result.setHourlyLimitExceeded(hourlyViolation);
        result.setDailyLimitExceeded(dailyViolation);
        
        boolean frequencyViolation = hourlyViolation || dailyViolation;
        result.setFrequencyViolation(frequencyViolation);
        
        // Set risk level
        String riskLevel = determineFrequencyRiskLevel(transactionsLastHour, transactionsLastDay, frequencyViolation);
        result.setRiskLevel(riskLevel);
        
        logger.debug("Frequency validation complete - hour: {} day: {} violation: {}", 
                    transactionsLastHour, transactionsLastDay, frequencyViolation);
        
        return result;
    }

    /**
     * Checks if transaction violates velocity limits.
     * Analyzes distance and time to detect impossible travel.
     * 
     * @param distanceMiles Distance between transaction locations
     * @param timeDifferenceHours Time difference between transactions
     * @return true if velocity limits are violated
     */
    public boolean checkVelocityLimits(double distanceMiles, double timeDifferenceHours) {
        
        // If time difference is less than 1 hour and distance is significant
        if (timeDifferenceHours < 1.0 && distanceMiles > VELOCITY_DISTANCE_THRESHOLD_MILES) {
            return true;
        }
        
        // Calculate required speed (mph) and check against reasonable limits
        if (timeDifferenceHours > 0) {
            double requiredSpeed = distanceMiles / timeDifferenceHours;
            // Impossible if requires > 600 mph (faster than commercial flight)
            if (requiredSpeed > 600.0) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Generates fraud alert for high-risk transactions.
     * Creates detailed alert with risk factors and recommendations.
     * 
     * @param fraudResult Complete fraud detection result
     * @return FraudAlert with alert details
     */
    public FraudAlert generateFraudAlert(FraudDetectionResult fraudResult) {
        
        logger.info("Generating fraud alert for card: {} risk score: {}", 
                   maskCardNumber(fraudResult.getCardNumber()), fraudResult.getRiskScore());
        
        FraudAlert alert = new FraudAlert();
        alert.setAlertId(generateAlertId());
        alert.setCardNumber(fraudResult.getCardNumber());
        alert.setTransactionAmount(fraudResult.getTransactionAmount());
        alert.setMerchantName(fraudResult.getMerchantName());
        alert.setRiskScore(fraudResult.getRiskScore());
        alert.setAlertDate(LocalDateTime.now());
        
        // Determine alert severity
        String severity = determineAlertSeverity(fraudResult.getRiskScore());
        alert.setSeverity(severity);
        
        // Build alert description
        StringBuilder description = new StringBuilder();
        description.append("Fraud alert generated for transaction. Risk factors: ");
        
        List<String> riskFactors = new ArrayList<>();
        
        if (fraudResult.getSpendingPatternResult().isUnusualSpending()) {
            riskFactors.add("Unusual spending pattern");
        }
        
        if (fraudResult.getVelocityResult().isVelocityViolation()) {
            riskFactors.add("Geographic velocity violation");
        }
        
        if (fraudResult.getMerchantResult().isRestrictedCategory()) {
            riskFactors.add("Restricted merchant category");
        }
        
        if (fraudResult.getBlacklistResult().isBlacklisted()) {
            riskFactors.add("Blacklist match");
        }
        
        if (fraudResult.getFrequencyResult().isFrequencyViolation()) {
            riskFactors.add("Transaction frequency violation");
        }
        
        description.append(String.join(", ", riskFactors));
        alert.setDescription(description.toString());
        
        // Set recommended action
        String recommendedAction = determineRecommendedAction(fraudResult.getRiskScore(), riskFactors);
        alert.setRecommendedAction(recommendedAction);
        
        logger.info("Fraud alert generated - ID: {} severity: {} factors: {}", 
                   alert.getAlertId(), severity, riskFactors.size());
        
        return alert;
    }

    // Helper methods for fraud detection logic
    
    /**
     * Masks card number for logging privacy.
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return "****";
        }
        return cardNumber.substring(0, 4) + "****" + cardNumber.substring(cardNumber.length() - 4);
    }
    
    /**
     * Retrieves historical transactions for spending pattern analysis.
     */
    private List<TransactionData> getHistoricalTransactions(String cardNumber, int days) {
        // Simulate historical transaction data
        // In real implementation, this would query the database
        List<TransactionData> transactions = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        Random random = new Random();
        
        // Generate sample historical data for demonstration
        for (int i = 0; i < 10; i++) {
            TransactionData transaction = new TransactionData();
            transaction.setCardNumber(cardNumber);
            transaction.setAmount(new BigDecimal(random.nextInt(1000) + 50));
            transaction.setTransactionDate(now.minusDays(random.nextInt(days)));
            transaction.setMerchantName("Sample Merchant " + i);
            transaction.setMerchantCity("City" + i);
            transaction.setMerchantZip("1000" + i);
            transactions.add(transaction);
        }
        
        return transactions;
    }
    
    /**
     * Retrieves recent transactions with location data.
     */
    private List<TransactionData> getRecentTransactionsByLocation(String cardNumber, int hours) {
        // Simulate recent transaction data
        List<TransactionData> transactions = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        
        // Add one recent transaction for demonstration
        TransactionData transaction = new TransactionData();
        transaction.setCardNumber(cardNumber);
        transaction.setAmount(new BigDecimal("150.00"));
        transaction.setTransactionDate(now.minusHours(2));
        transaction.setMerchantName("Previous Merchant");
        transaction.setMerchantCity("New York");
        transaction.setMerchantZip("10001");
        transactions.add(transaction);
        
        return transactions;
    }
    
    /**
     * Calculates distance between two geographic locations.
     */
    private double calculateDistance(String city1, String zip1, String city2, String zip2) {
        // Simplified distance calculation
        // In real implementation, use geographic coordinate lookup and haversine formula
        
        String region1 = ZIP_TO_REGION.getOrDefault(zip1, "UNKNOWN");
        String region2 = ZIP_TO_REGION.getOrDefault(zip2, "UNKNOWN");
        
        if (region1.equals(region2)) {
            return 50.0; // Same region
        } else if (region1.equals("UNKNOWN") || region2.equals("UNKNOWN")) {
            return 100.0; // Unknown region
        } else {
            return 800.0; // Different regions
        }
    }
    
    /**
     * Validates merchant name for suspicious patterns.
     */
    private boolean validateMerchantName(String merchantName) {
        if (merchantName == null || merchantName.trim().isEmpty()) {
            return true; // Suspicious - empty name
        }
        
        String upperName = merchantName.toUpperCase();
        
        // Check against blacklisted patterns
        for (String blacklisted : BLACKLISTED_MERCHANTS) {
            if (upperName.contains(blacklisted)) {
                return true;
            }
        }
        
        // Check for suspicious patterns
        if (upperName.contains("TEST") || upperName.contains("FRAUD") || 
            upperName.contains("SCAM") || upperName.contains("FAKE")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Validates merchant location.
     */
    private boolean validateMerchantLocation(String merchantCity) {
        if (merchantCity == null || merchantCity.trim().isEmpty()) {
            return false;
        }
        
        // Basic validation - non-empty and reasonable length
        return merchantCity.trim().length() >= 2 && merchantCity.trim().length() <= 50;
    }
    
    /**
     * Checks if card is blacklisted.
     */
    private boolean isCardBlacklisted(String cardNumber) {
        // Simulate blacklist check
        // In real implementation, query blacklist database
        
        // Example: block test cards
        return cardNumber != null && cardNumber.startsWith("4000000000000000");
    }
    
    /**
     * Checks if merchant is blacklisted.
     */
    private boolean isMerchantBlacklisted(String merchantId, String merchantName) {
        if (merchantName != null) {
            String upperName = merchantName.toUpperCase();
            for (String blacklisted : BLACKLISTED_MERCHANTS) {
                if (upperName.contains(blacklisted)) {
                    return true;
                }
            }
        }
        
        // Check merchant ID patterns
        if (merchantId != null && merchantId.startsWith("9999")) {
            return true; // Test blacklisted merchant ID pattern
        }
        
        return false;
    }
    
    /**
     * Checks for known fraud patterns.
     */
    private boolean checkFraudPatterns(String cardNumber, String merchantName) {
        // Check for known fraud patterns
        if (cardNumber != null && cardNumber.contains("1234567890")) {
            return true; // Sequential number pattern
        }
        
        if (merchantName != null && merchantName.toUpperCase().contains("FRAUD")) {
            return true; // Suspicious merchant name
        }
        
        return false;
    }
    
    /**
     * Counts transactions in specified period.
     */
    private int countTransactionsInPeriod(String cardNumber, LocalDateTime transactionDate, int hours) {
        // Simulate transaction counting
        // In real implementation, query database for count
        
        Random random = new Random(cardNumber.hashCode());
        return random.nextInt(5); // Return random count for demonstration
    }
    
    // Risk scoring helper methods
    
    private String determineSpendingRiskLevel(int deviationScore, BigDecimal amount, boolean unusual) {
        if (unusual || deviationScore > 80) return "HIGH";
        if (deviationScore > 50 || amount.compareTo(new BigDecimal("1000")) > 0) return "MEDIUM";
        return "LOW";
    }
    
    private String determineVelocityRiskLevel(double distance, double time, boolean violation) {
        if (violation) return "HIGH";
        if (distance > 200 && time < 2) return "MEDIUM";
        return "LOW";
    }
    
    private String determineMerchantRiskLevel(boolean restricted, boolean suspicious, boolean validLocation) {
        if (restricted || suspicious || !validLocation) return "HIGH";
        return "LOW";
    }
    
    private String determineFrequencyRiskLevel(int hourly, int daily, boolean violation) {
        if (violation) return "HIGH";
        if (hourly > 5 || daily > 25) return "MEDIUM";
        return "LOW";
    }
    
    private int calculateAmountRiskScore(SpendingPatternResult result) {
        if (result.isUnusualSpending()) return 80;
        return Math.min(result.getDeviationScore(), 60);
    }
    
    private int calculateFrequencyRiskScore(FrequencyCheckResult result) {
        if (result.isFrequencyViolation()) return 90;
        if (result.getTransactionsLastHour() > 5) return 60;
        if (result.getTransactionsLastDay() > 25) return 40;
        return 10;
    }
    
    private int calculateVelocityRiskScore(VelocityCheckResult result) {
        if (result.isVelocityViolation()) return 95;
        if (result.getDistanceMiles() > 200 && result.getTimeDifferenceHours() < 2) return 70;
        return 20;
    }
    
    private int calculateMerchantRiskScore(MerchantValidationResult result) {
        if (result.isRestrictedCategory()) return 85;
        if (result.isSuspiciousMerchant()) return 75;
        if (!result.isValidLocation()) return 40;
        return 10;
    }
    
    private int calculateBlacklistRiskScore(BlacklistCheckResult result) {
        if (result.isBlacklisted()) return 100;
        return 0;
    }
    
    private String getBlacklistReason(boolean card, boolean merchant, boolean pattern) {
        List<String> reasons = new ArrayList<>();
        if (card) reasons.add("Card blacklisted");
        if (merchant) reasons.add("Merchant blacklisted");
        if (pattern) reasons.add("Fraud pattern detected");
        return String.join(", ", reasons);
    }
    
    private String generateAlertId() {
        return "FRAUD_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000);
    }
    
    private String determineAlertSeverity(int riskScore) {
        if (riskScore >= 90) return "CRITICAL";
        if (riskScore >= 70) return "HIGH";
        if (riskScore >= 50) return "MEDIUM";
        return "LOW";
    }
    
    private String determineRecommendedAction(int riskScore, List<String> riskFactors) {
        if (riskScore >= 90) {
            return "BLOCK_TRANSACTION - Immediate review required";
        } else if (riskScore >= 70) {
            return "MANUAL_REVIEW - Hold for verification";
        } else if (riskScore >= 50) {
            return "ENHANCED_AUTH - Request additional verification";
        } else {
            return "MONITOR - Continue with monitoring";
        }
    }

    // Data classes for fraud detection results
    
    /**
     * Main fraud detection result containing all analysis components.
     */
    public static class FraudDetectionResult {
        private String cardNumber;
        private BigDecimal transactionAmount;
        private String merchantId;
        private String merchantName;
        private LocalDateTime transactionDate;
        private SpendingPatternResult spendingPatternResult;
        private VelocityCheckResult velocityResult;
        private MerchantValidationResult merchantResult;
        private BlacklistCheckResult blacklistResult;
        private FrequencyCheckResult frequencyResult;
        private int riskScore;
        private boolean fraudAlert;
        private FraudAlert alert;

        // Getters and setters
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        
        public BigDecimal getTransactionAmount() { return transactionAmount; }
        public void setTransactionAmount(BigDecimal transactionAmount) { this.transactionAmount = transactionAmount; }
        
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
        
        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
        
        public LocalDateTime getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
        
        public SpendingPatternResult getSpendingPatternResult() { return spendingPatternResult; }
        public void setSpendingPatternResult(SpendingPatternResult spendingPatternResult) { this.spendingPatternResult = spendingPatternResult; }
        
        public VelocityCheckResult getVelocityResult() { return velocityResult; }
        public void setVelocityResult(VelocityCheckResult velocityResult) { this.velocityResult = velocityResult; }
        
        public MerchantValidationResult getMerchantResult() { return merchantResult; }
        public void setMerchantResult(MerchantValidationResult merchantResult) { this.merchantResult = merchantResult; }
        
        public BlacklistCheckResult getBlacklistResult() { return blacklistResult; }
        public void setBlacklistResult(BlacklistCheckResult blacklistResult) { this.blacklistResult = blacklistResult; }
        
        public FrequencyCheckResult getFrequencyResult() { return frequencyResult; }
        public void setFrequencyResult(FrequencyCheckResult frequencyResult) { this.frequencyResult = frequencyResult; }
        
        public int getRiskScore() { return riskScore; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
        
        public boolean isFraudAlert() { return fraudAlert; }
        public void setFraudAlert(boolean fraudAlert) { this.fraudAlert = fraudAlert; }
        
        public FraudAlert getAlert() { return alert; }
        public void setAlert(FraudAlert alert) { this.alert = alert; }
    }

    /**
     * Result of spending pattern analysis.
     */
    public static class SpendingPatternResult {
        private String cardNumber;
        private BigDecimal transactionAmount;
        private BigDecimal averageAmount;
        private BigDecimal maxAmount;
        private int deviationScore;
        private boolean unusualSpending;
        private String riskLevel;

        // Getters and setters
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        
        public BigDecimal getTransactionAmount() { return transactionAmount; }
        public void setTransactionAmount(BigDecimal transactionAmount) { this.transactionAmount = transactionAmount; }
        
        public BigDecimal getAverageAmount() { return averageAmount; }
        public void setAverageAmount(BigDecimal averageAmount) { this.averageAmount = averageAmount; }
        
        public BigDecimal getMaxAmount() { return maxAmount; }
        public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
        
        public int getDeviationScore() { return deviationScore; }
        public void setDeviationScore(int deviationScore) { this.deviationScore = deviationScore; }
        
        public boolean isUnusualSpending() { return unusualSpending; }
        public void setUnusualSpending(boolean unusualSpending) { this.unusualSpending = unusualSpending; }
        
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    }

    /**
     * Result of geographic velocity check.
     */
    public static class VelocityCheckResult {
        private String cardNumber;
        private String currentLocation;
        private String lastLocation;
        private LocalDateTime transactionDate;
        private double distanceMiles;
        private double timeDifferenceHours;
        private boolean velocityViolation;
        private String riskLevel;

        // Getters and setters
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        
        public String getCurrentLocation() { return currentLocation; }
        public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
        
        public String getLastLocation() { return lastLocation; }
        public void setLastLocation(String lastLocation) { this.lastLocation = lastLocation; }
        
        public LocalDateTime getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
        
        public double getDistanceMiles() { return distanceMiles; }
        public void setDistanceMiles(double distanceMiles) { this.distanceMiles = distanceMiles; }
        
        public double getTimeDifferenceHours() { return timeDifferenceHours; }
        public void setTimeDifferenceHours(double timeDifferenceHours) { this.timeDifferenceHours = timeDifferenceHours; }
        
        public boolean isVelocityViolation() { return velocityViolation; }
        public void setVelocityViolation(boolean velocityViolation) { this.velocityViolation = velocityViolation; }
        
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    }

    /**
     * Result of merchant validation.
     */
    public static class MerchantValidationResult {
        private String merchantId;
        private String merchantName;
        private String merchantCity;
        private String categoryCode;
        private boolean restrictedCategory;
        private boolean suspiciousMerchant;
        private boolean validLocation;
        private boolean validMerchant;
        private String riskLevel;

        // Getters and setters
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
        
        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
        
        public String getMerchantCity() { return merchantCity; }
        public void setMerchantCity(String merchantCity) { this.merchantCity = merchantCity; }
        
        public String getCategoryCode() { return categoryCode; }
        public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
        
        public boolean isRestrictedCategory() { return restrictedCategory; }
        public void setRestrictedCategory(boolean restrictedCategory) { this.restrictedCategory = restrictedCategory; }
        
        public boolean isSuspiciousMerchant() { return suspiciousMerchant; }
        public void setSuspiciousMerchant(boolean suspiciousMerchant) { this.suspiciousMerchant = suspiciousMerchant; }
        
        public boolean isValidLocation() { return validLocation; }
        public void setValidLocation(boolean validLocation) { this.validLocation = validLocation; }
        
        public boolean isValidMerchant() { return validMerchant; }
        public void setValidMerchant(boolean validMerchant) { this.validMerchant = validMerchant; }
        
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    }

    /**
     * Result of blacklist check.
     */
    public static class BlacklistCheckResult {
        private String cardNumber;
        private String merchantId;
        private String merchantName;
        private boolean cardBlacklisted;
        private boolean merchantBlacklisted;
        private boolean fraudPattern;
        private boolean blacklisted;
        private String blacklistReason;
        private String riskLevel;

        // Getters and setters
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
        
        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
        
        public boolean isCardBlacklisted() { return cardBlacklisted; }
        public void setCardBlacklisted(boolean cardBlacklisted) { this.cardBlacklisted = cardBlacklisted; }
        
        public boolean isMerchantBlacklisted() { return merchantBlacklisted; }
        public void setMerchantBlacklisted(boolean merchantBlacklisted) { this.merchantBlacklisted = merchantBlacklisted; }
        
        public boolean isFraudPattern() { return fraudPattern; }
        public void setFraudPattern(boolean fraudPattern) { this.fraudPattern = fraudPattern; }
        
        public boolean isBlacklisted() { return blacklisted; }
        public void setBlacklisted(boolean blacklisted) { this.blacklisted = blacklisted; }
        
        public String getBlacklistReason() { return blacklistReason; }
        public void setBlacklistReason(String blacklistReason) { this.blacklistReason = blacklistReason; }
        
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    }

    /**
     * Result of transaction frequency check.
     */
    public static class FrequencyCheckResult {
        private String cardNumber;
        private LocalDateTime transactionDate;
        private int transactionsLastHour;
        private int transactionsLastDay;
        private boolean hourlyLimitExceeded;
        private boolean dailyLimitExceeded;
        private boolean frequencyViolation;
        private String riskLevel;

        // Getters and setters
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        
        public LocalDateTime getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
        
        public int getTransactionsLastHour() { return transactionsLastHour; }
        public void setTransactionsLastHour(int transactionsLastHour) { this.transactionsLastHour = transactionsLastHour; }
        
        public int getTransactionsLastDay() { return transactionsLastDay; }
        public void setTransactionsLastDay(int transactionsLastDay) { this.transactionsLastDay = transactionsLastDay; }
        
        public boolean isHourlyLimitExceeded() { return hourlyLimitExceeded; }
        public void setHourlyLimitExceeded(boolean hourlyLimitExceeded) { this.hourlyLimitExceeded = hourlyLimitExceeded; }
        
        public boolean isDailyLimitExceeded() { return dailyLimitExceeded; }
        public void setDailyLimitExceeded(boolean dailyLimitExceeded) { this.dailyLimitExceeded = dailyLimitExceeded; }
        
        public boolean isFrequencyViolation() { return frequencyViolation; }
        public void setFrequencyViolation(boolean frequencyViolation) { this.frequencyViolation = frequencyViolation; }
        
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    }

    /**
     * Fraud alert generated for high-risk transactions.
     */
    public static class FraudAlert {
        private String alertId;
        private String cardNumber;
        private BigDecimal transactionAmount;
        private String merchantName;
        private int riskScore;
        private String severity;
        private String description;
        private String recommendedAction;
        private LocalDateTime alertDate;

        // Getters and setters
        public String getAlertId() { return alertId; }
        public void setAlertId(String alertId) { this.alertId = alertId; }
        
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        
        public BigDecimal getTransactionAmount() { return transactionAmount; }
        public void setTransactionAmount(BigDecimal transactionAmount) { this.transactionAmount = transactionAmount; }
        
        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
        
        public int getRiskScore() { return riskScore; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getRecommendedAction() { return recommendedAction; }
        public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
        
        public LocalDateTime getAlertDate() { return alertDate; }
        public void setAlertDate(LocalDateTime alertDate) { this.alertDate = alertDate; }
    }

    /**
     * Transaction data for historical analysis.
     */
    public static class TransactionData {
        private String cardNumber;
        private BigDecimal amount;
        private LocalDateTime transactionDate;
        private String merchantName;
        private String merchantCity;
        private String merchantZip;

        // Getters and setters
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public LocalDateTime getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
        
        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
        
        public String getMerchantCity() { return merchantCity; }
        public void setMerchantCity(String merchantCity) { this.merchantCity = merchantCity; }
        
        public String getMerchantZip() { return merchantZip; }
        public void setMerchantZip(String merchantZip) { this.merchantZip = merchantZip; }
    }
}
