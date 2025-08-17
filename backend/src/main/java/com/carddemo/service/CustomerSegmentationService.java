/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service for customer segmentation and marketing categorization based on COBOL business rules.
 * Implements customer classification logic for credit scoring, account status, and marketing segments.
 * Preserves existing segmentation algorithms from mainframe customer processing.
 * 
 * Migrated from CBCUS01C.cbl batch program functionality.
 */
@Service
public class CustomerSegmentationService {

    private static final Logger logger = Logger.getLogger(CustomerSegmentationService.class.getName());

    // Customer segment constants matching COBOL classification rules
    private static final String SEGMENT_PREMIUM = "PREMIUM";
    private static final String SEGMENT_STANDARD = "STANDARD";
    private static final String SEGMENT_BASIC = "BASIC";
    private static final String SEGMENT_RISK = "RISK";
    private static final String SEGMENT_INACTIVE = "INACTIVE";

    // Risk assessment constants
    private static final int EXCELLENT_CREDIT_THRESHOLD = 800;
    private static final int GOOD_CREDIT_THRESHOLD = 700;
    private static final int FAIR_CREDIT_THRESHOLD = 600;
    private static final int POOR_CREDIT_THRESHOLD = 600;

    // Account status thresholds
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("50000.00");
    private static final BigDecimal MEDIUM_VALUE_THRESHOLD = new BigDecimal("25000.00");
    private static final BigDecimal LOW_VALUE_THRESHOLD = new BigDecimal("5000.00");

    /**
     * Segments customer for marketing classification based on COBOL business rules.
     * 
     * @param customerId Customer identifier
     * @param ficoScore Customer FICO credit score
     * @param accountBalance Current account balance
     * @param lastTransactionDate Date of last transaction
     * @param accountAge Age of account in years
     * @return Customer segment classification
     */
    public String segmentCustomer(String customerId, int ficoScore, BigDecimal accountBalance, 
                                 LocalDate lastTransactionDate, int accountAge) {
        
        logger.info("Starting customer segmentation for customer ID: " + customerId);
        
        try {
            // Input validation matching COBOL data validation rules
            if (customerId == null || customerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID cannot be null or empty");
            }
            
            if (ficoScore < 300 || ficoScore > 850) {
                throw new IllegalArgumentException("FICO score must be between 300 and 850");
            }
            
            if (accountBalance == null || accountBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Account balance cannot be null or negative");
            }
            
            // Calculate days since last transaction
            int daysSinceLastTransaction = 0;
            if (lastTransactionDate != null) {
                daysSinceLastTransaction = (int) java.time.temporal.ChronoUnit.DAYS.between(lastTransactionDate, LocalDate.now());
            }
            
            // Primary segmentation logic preserving COBOL business rules
            String segment = performPrimarySegmentation(ficoScore, accountBalance, 
                                                      daysSinceLastTransaction, accountAge);
            
            logger.info("Customer " + customerId + " classified as: " + segment);
            return segment;
            
        } catch (IllegalArgumentException e) {
            logger.severe("Error segmenting customer " + customerId + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.severe("Error segmenting customer " + customerId + ": " + e.getMessage());
            throw new RuntimeException("Customer segmentation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Updates customer segment based on new account activity and risk assessment.
     * 
     * @param customerId Customer identifier
     * @param currentSegment Current customer segment
     * @param newFicoScore Updated FICO score
     * @param newAccountBalance Updated account balance
     * @param recentTransactionCount Recent transaction activity count
     * @return Updated customer segment
     */
    public String updateCustomerSegment(String customerId, String currentSegment, int newFicoScore, 
                                       BigDecimal newAccountBalance, int recentTransactionCount) {
        
        logger.info("Updating customer segment for customer ID: " + customerId);
        
        try {
            // Input validation
            if (customerId == null || customerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID cannot be null or empty");
            }
            
            if (currentSegment == null || currentSegment.trim().isEmpty()) {
                throw new IllegalArgumentException("Current segment cannot be null or empty");
            }
            
            // Assess if segment change is warranted based on new data
            String riskAssessment = assessRisk(newFicoScore, newAccountBalance, recentTransactionCount);
            
            // Determine new segment based on risk assessment and activity
            String newSegment = determineSegmentFromRisk(riskAssessment, newAccountBalance, 
                                                        recentTransactionCount);
            
            // Apply segment change rules - preserve stability unless significant change
            if (shouldChangeSegment(currentSegment, newSegment, newFicoScore, recentTransactionCount)) {
                logger.info("Segment changed from " + currentSegment + " to " + newSegment 
                           + " for customer " + customerId);
                return newSegment;
            } else {
                logger.info("Segment maintained as " + currentSegment + " for customer " + customerId);
                return currentSegment;
            }
            
        } catch (IllegalArgumentException e) {
            logger.severe("Error updating customer segment for " + customerId + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.severe("Error updating customer segment for " + customerId + ": " + e.getMessage());
            throw new RuntimeException("Customer segment update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Assesses customer risk based on FICO scores and account history.
     * 
     * @param ficoScore Customer FICO credit score
     * @param accountBalance Current account balance
     * @param transactionCount Recent transaction activity
     * @return Risk assessment classification
     */
    public String assessRisk(int ficoScore, BigDecimal accountBalance, int transactionCount) {
        
        logger.info("Assessing risk for FICO score: " + ficoScore + ", Balance: " + accountBalance);
        
        try {
            // FICO score risk assessment matching COBOL logic
            String creditRisk = assessCreditRisk(ficoScore);
            
            // Account balance risk assessment
            String balanceRisk = assessBalanceRisk(accountBalance);
            
            // Transaction activity risk assessment
            String activityRisk = assessActivityRisk(transactionCount);
            
            // Combined risk assessment using COBOL business rule logic
            String overallRisk = combineRiskFactors(creditRisk, balanceRisk, activityRisk);
            
            logger.info("Risk assessment result: " + overallRisk);
            return overallRisk;
            
        } catch (Exception e) {
            logger.severe("Error assessing customer risk: " + e.getMessage());
            throw new RuntimeException("Risk assessment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Classifies customer according to COBOL customer classification routines.
     * 
     * @param customerId Customer identifier
     * @param ficoScore Customer credit score
     * @param accountBalance Account balance
     * @param accountAge Age of account in years
     * @param paymentHistory Payment history indicator
     * @return Customer classification
     */
    public String classifyCustomer(String customerId, int ficoScore, BigDecimal accountBalance, 
                                  int accountAge, String paymentHistory) {
        
        logger.info("Classifying customer: " + customerId);
        
        try {
            // Validate inputs
            validateCustomerData(customerId, ficoScore, accountBalance, paymentHistory);
            
            // Primary classification based on credit score
            String creditClassification = classifyByCredit(ficoScore);
            
            // Account value classification
            String valueClassification = classifyByValue(accountBalance);
            
            // Tenure classification
            String tenureClassification = classifyByTenure(accountAge);
            
            // Payment behavior classification
            String paymentClassification = classifyByPayment(paymentHistory);
            
            // Combine classifications using COBOL business logic
            String finalClassification = combineClassifications(creditClassification, 
                                                               valueClassification, 
                                                               tenureClassification, 
                                                               paymentClassification);
            
            logger.info("Customer " + customerId + " classified as: " + finalClassification);
            return finalClassification;
            
        } catch (IllegalArgumentException e) {
            logger.severe("Error classifying customer " + customerId + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.severe("Error classifying customer " + customerId + ": " + e.getMessage());
            throw new RuntimeException("Customer classification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves detailed segment information for a customer.
     * 
     * @param customerId Customer identifier
     * @param segment Customer segment
     * @return Map containing segment details and characteristics
     */
    public Map<String, Object> getSegmentDetails(String customerId, String segment) {
        
        logger.info("Retrieving segment details for customer: " + customerId + ", segment: " + segment);
        
        try {
            Map<String, Object> segmentDetails = new HashMap<>();
            
            // Validate inputs
            if (customerId == null || customerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID cannot be null or empty");
            }
            
            if (segment == null || segment.trim().isEmpty()) {
                throw new IllegalArgumentException("Segment cannot be null or empty");
            }
            
            // Populate segment details based on COBOL business rules
            segmentDetails.put("customerId", customerId);
            segmentDetails.put("segment", segment);
            segmentDetails.put("segmentDescription", getSegmentDescription(segment));
            segmentDetails.put("marketingEligibility", getMarketingEligibility(segment));
            segmentDetails.put("creditLimitGuidelines", getCreditLimitGuidelines(segment));
            segmentDetails.put("interestRateCategory", getInterestRateCategory(segment));
            segmentDetails.put("serviceLevel", getServiceLevel(segment));
            segmentDetails.put("riskLevel", getRiskLevel(segment));
            segmentDetails.put("segmentBenefits", getSegmentBenefits(segment));
            
            logger.info("Segment details retrieved for customer: " + customerId);
            return segmentDetails;
            
        } catch (IllegalArgumentException e) {
            logger.severe("Error retrieving segment details for customer " + customerId + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.severe("Error retrieving segment details for customer " + customerId + ": " + e.getMessage());
            throw new RuntimeException("Segment details retrieval failed: " + e.getMessage(), e);
        }
    }

    // Private helper methods implementing COBOL business logic

    /**
     * Performs primary customer segmentation logic.
     */
    private String performPrimarySegmentation(int ficoScore, BigDecimal accountBalance, 
                                            int daysSinceLastTransaction, int accountAge) {
        
        // Check for inactive customers (no transactions in 180 days)
        if (daysSinceLastTransaction > 180) {
            return SEGMENT_INACTIVE;
        }
        
        // Risk segment for poor credit scores
        if (ficoScore < POOR_CREDIT_THRESHOLD) {
            return SEGMENT_RISK;
        }
        
        // Premium segment criteria - excellent credit and high balance
        if (ficoScore >= EXCELLENT_CREDIT_THRESHOLD && 
            accountBalance.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            return SEGMENT_PREMIUM;
        }
        
        // Standard segment criteria - good credit and medium balance
        if (ficoScore >= GOOD_CREDIT_THRESHOLD && 
            accountBalance.compareTo(MEDIUM_VALUE_THRESHOLD) >= 0) {
            return SEGMENT_STANDARD;
        }
        
        // Basic segment - default classification
        return SEGMENT_BASIC;
    }

    /**
     * Assesses credit risk based on FICO score.
     */
    private String assessCreditRisk(int ficoScore) {
        if (ficoScore >= EXCELLENT_CREDIT_THRESHOLD) {
            return "LOW";
        } else if (ficoScore >= GOOD_CREDIT_THRESHOLD) {
            return "MEDIUM";
        } else if (ficoScore >= FAIR_CREDIT_THRESHOLD) {
            return "MEDIUM_HIGH";
        } else {
            return "HIGH";
        }
    }

    /**
     * Assesses balance-based risk.
     */
    private String assessBalanceRisk(BigDecimal accountBalance) {
        if (accountBalance.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            return "LOW";
        } else if (accountBalance.compareTo(MEDIUM_VALUE_THRESHOLD) >= 0) {
            return "MEDIUM";
        } else if (accountBalance.compareTo(LOW_VALUE_THRESHOLD) >= 0) {
            return "MEDIUM_HIGH";
        } else {
            return "HIGH";
        }
    }

    /**
     * Assesses activity-based risk.
     */
    private String assessActivityRisk(int transactionCount) {
        if (transactionCount >= 10) {
            return "LOW";
        } else if (transactionCount >= 5) {
            return "MEDIUM";
        } else if (transactionCount >= 1) {
            return "MEDIUM_HIGH";
        } else {
            return "HIGH";
        }
    }

    /**
     * Combines multiple risk factors into overall assessment.
     */
    private String combineRiskFactors(String creditRisk, String balanceRisk, String activityRisk) {
        // High risk if any factor is high
        if ("HIGH".equals(creditRisk) || "HIGH".equals(balanceRisk) || "HIGH".equals(activityRisk)) {
            return "HIGH";
        }
        
        // Low risk if all factors are low
        if ("LOW".equals(creditRisk) && "LOW".equals(balanceRisk) && "LOW".equals(activityRisk)) {
            return "LOW";
        }
        
        // Medium risk otherwise
        return "MEDIUM";
    }

    /**
     * Determines if segment change should occur.
     */
    private boolean shouldChangeSegment(String currentSegment, String newSegment, 
                                      int ficoScore, int transactionCount) {
        // Don't change if segments are the same
        if (currentSegment.equals(newSegment)) {
            return false;
        }
        
        // Always change to/from RISK or INACTIVE segments
        if (SEGMENT_RISK.equals(currentSegment) || SEGMENT_RISK.equals(newSegment) ||
            SEGMENT_INACTIVE.equals(currentSegment) || SEGMENT_INACTIVE.equals(newSegment)) {
            return true;
        }
        
        // Require significant credit change for other segment transitions
        if (ficoScore < FAIR_CREDIT_THRESHOLD || ficoScore > EXCELLENT_CREDIT_THRESHOLD) {
            return true;
        }
        
        // Require active account for upgrade
        if (transactionCount > 0 && isUpgrade(currentSegment, newSegment)) {
            return true;
        }
        
        return false;
    }

    /**
     * Determines if segment change represents an upgrade.
     */
    private boolean isUpgrade(String currentSegment, String newSegment) {
        Map<String, Integer> segmentHierarchy = new HashMap<>();
        segmentHierarchy.put(SEGMENT_INACTIVE, 0);
        segmentHierarchy.put(SEGMENT_RISK, 1);
        segmentHierarchy.put(SEGMENT_BASIC, 2);
        segmentHierarchy.put(SEGMENT_STANDARD, 3);
        segmentHierarchy.put(SEGMENT_PREMIUM, 4);
        
        return segmentHierarchy.getOrDefault(newSegment, 0) > 
               segmentHierarchy.getOrDefault(currentSegment, 0);
    }

    /**
     * Determines segment from risk assessment.
     */
    private String determineSegmentFromRisk(String riskAssessment, BigDecimal accountBalance, 
                                          int recentTransactionCount) {
        // No activity means inactive
        if (recentTransactionCount == 0) {
            return SEGMENT_INACTIVE;
        }
        
        // High risk means risk segment
        if ("HIGH".equals(riskAssessment)) {
            return SEGMENT_RISK;
        }
        
        // Low risk with high balance means premium
        if ("LOW".equals(riskAssessment) && 
            accountBalance.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            return SEGMENT_PREMIUM;
        }
        
        // Medium risk with medium balance means standard
        if ("MEDIUM".equals(riskAssessment) && 
            accountBalance.compareTo(MEDIUM_VALUE_THRESHOLD) >= 0) {
            return SEGMENT_STANDARD;
        }
        
        // Default to basic
        return SEGMENT_BASIC;
    }

    /**
     * Validates customer data inputs.
     */
    private void validateCustomerData(String customerId, int ficoScore, 
                                    BigDecimal accountBalance, String paymentHistory) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        
        if (ficoScore < 300 || ficoScore > 850) {
            throw new IllegalArgumentException("FICO score must be between 300 and 850");
        }
        
        if (accountBalance == null || accountBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Account balance cannot be null or negative");
        }
        
        if (paymentHistory == null) {
            throw new IllegalArgumentException("Payment history cannot be null");
        }
    }

    /**
     * Classifies customer by credit score.
     */
    private String classifyByCredit(int ficoScore) {
        if (ficoScore >= EXCELLENT_CREDIT_THRESHOLD) {
            return "EXCELLENT";
        } else if (ficoScore >= GOOD_CREDIT_THRESHOLD) {
            return "GOOD";
        } else if (ficoScore >= FAIR_CREDIT_THRESHOLD) {
            return "FAIR";
        } else {
            return "POOR";
        }
    }

    /**
     * Classifies customer by account value.
     */
    private String classifyByValue(BigDecimal accountBalance) {
        if (accountBalance.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            return "HIGH_VALUE";
        } else if (accountBalance.compareTo(MEDIUM_VALUE_THRESHOLD) >= 0) {
            return "MEDIUM_VALUE";
        } else if (accountBalance.compareTo(LOW_VALUE_THRESHOLD) >= 0) {
            return "LOW_VALUE";
        } else {
            return "MINIMAL_VALUE";
        }
    }

    /**
     * Classifies customer by account tenure.
     */
    private String classifyByTenure(int accountAge) {
        if (accountAge >= 10) {
            return "LONG_TERM";
        } else if (accountAge >= 5) {
            return "MEDIUM_TERM";
        } else if (accountAge >= 2) {
            return "SHORT_TERM";
        } else {
            return "NEW";
        }
    }

    /**
     * Classifies customer by payment behavior.
     */
    private String classifyByPayment(String paymentHistory) {
        if ("EXCELLENT".equalsIgnoreCase(paymentHistory)) {
            return "EXCELLENT_PAYER";
        } else if ("GOOD".equalsIgnoreCase(paymentHistory)) {
            return "GOOD_PAYER";
        } else if ("FAIR".equalsIgnoreCase(paymentHistory)) {
            return "FAIR_PAYER";
        } else {
            return "POOR_PAYER";
        }
    }

    /**
     * Combines multiple classification factors.
     */
    private String combineClassifications(String creditClassification, String valueClassification,
                                        String tenureClassification, String paymentClassification) {
        // Premium classification criteria
        if ("EXCELLENT".equals(creditClassification) && 
            "HIGH_VALUE".equals(valueClassification) &&
            "EXCELLENT_PAYER".equals(paymentClassification)) {
            return SEGMENT_PREMIUM;
        }
        
        // Standard classification criteria
        if (("GOOD".equals(creditClassification) || "EXCELLENT".equals(creditClassification)) &&
            ("MEDIUM_VALUE".equals(valueClassification) || "HIGH_VALUE".equals(valueClassification)) &&
            ("GOOD_PAYER".equals(paymentClassification) || "EXCELLENT_PAYER".equals(paymentClassification))) {
            return SEGMENT_STANDARD;
        }
        
        // Risk classification criteria
        if ("POOR".equals(creditClassification) || "POOR_PAYER".equals(paymentClassification)) {
            return SEGMENT_RISK;
        }
        
        // Basic classification default
        return SEGMENT_BASIC;
    }

    // Segment detail helper methods

    /**
     * Gets segment description.
     */
    private String getSegmentDescription(String segment) {
        switch (segment) {
            case SEGMENT_PREMIUM:
                return "Premium customers with excellent credit and high account values";
            case SEGMENT_STANDARD:
                return "Standard customers with good credit and moderate account activity";
            case SEGMENT_BASIC:
                return "Basic customers meeting minimum requirements";
            case SEGMENT_RISK:
                return "Risk customers requiring special attention and monitoring";
            case SEGMENT_INACTIVE:
                return "Inactive customers with no recent account activity";
            default:
                return "Unknown segment classification";
        }
    }

    /**
     * Gets marketing eligibility based on segment.
     */
    private String getMarketingEligibility(String segment) {
        switch (segment) {
            case SEGMENT_PREMIUM:
                return "ELIGIBLE_ALL";
            case SEGMENT_STANDARD:
                return "ELIGIBLE_STANDARD";
            case SEGMENT_BASIC:
                return "ELIGIBLE_BASIC";
            case SEGMENT_RISK:
                return "RESTRICTED";
            case SEGMENT_INACTIVE:
                return "REACTIVATION_ONLY";
            default:
                return "NOT_ELIGIBLE";
        }
    }

    /**
     * Gets credit limit guidelines for segment.
     */
    private BigDecimal getCreditLimitGuidelines(String segment) {
        switch (segment) {
            case SEGMENT_PREMIUM:
                return new BigDecimal("100000.00");
            case SEGMENT_STANDARD:
                return new BigDecimal("50000.00");
            case SEGMENT_BASIC:
                return new BigDecimal("25000.00");
            case SEGMENT_RISK:
                return new BigDecimal("5000.00");
            case SEGMENT_INACTIVE:
                return new BigDecimal("0.00");
            default:
                return new BigDecimal("10000.00");
        }
    }

    /**
     * Gets interest rate category for segment.
     */
    private String getInterestRateCategory(String segment) {
        switch (segment) {
            case SEGMENT_PREMIUM:
                return "PREFERRED";
            case SEGMENT_STANDARD:
                return "STANDARD";
            case SEGMENT_BASIC:
                return "STANDARD_PLUS";
            case SEGMENT_RISK:
                return "HIGH_RISK";
            case SEGMENT_INACTIVE:
                return "REACTIVATION";
            default:
                return "STANDARD";
        }
    }

    /**
     * Gets service level for segment.
     */
    private String getServiceLevel(String segment) {
        switch (segment) {
            case SEGMENT_PREMIUM:
                return "PLATINUM";
            case SEGMENT_STANDARD:
                return "GOLD";
            case SEGMENT_BASIC:
                return "SILVER";
            case SEGMENT_RISK:
                return "BASIC";
            case SEGMENT_INACTIVE:
                return "MINIMAL";
            default:
                return "STANDARD";
        }
    }

    /**
     * Gets risk level assessment for segment.
     */
    private String getRiskLevel(String segment) {
        switch (segment) {
            case SEGMENT_PREMIUM:
                return "LOW";
            case SEGMENT_STANDARD:
                return "MEDIUM";
            case SEGMENT_BASIC:
                return "MEDIUM";
            case SEGMENT_RISK:
                return "HIGH";
            case SEGMENT_INACTIVE:
                return "UNKNOWN";
            default:
                return "MEDIUM";
        }
    }

    /**
     * Gets segment benefits description.
     */
    private String getSegmentBenefits(String segment) {
        switch (segment) {
            case SEGMENT_PREMIUM:
                return "Premium rewards, dedicated support, exclusive offers, priority processing";
            case SEGMENT_STANDARD:
                return "Standard rewards, regular support, targeted offers";
            case SEGMENT_BASIC:
                return "Basic rewards, standard support, general offers";
            case SEGMENT_RISK:
                return "Account monitoring, limited privileges, recovery assistance";
            case SEGMENT_INACTIVE:
                return "Reactivation incentives, account recovery assistance";
            default:
                return "Standard account benefits";
        }
    }
}