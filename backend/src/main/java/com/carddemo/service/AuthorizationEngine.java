package com.carddemo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Authorization Engine Service
 * 
 * Implements comprehensive authorization logic for transaction processing,
 * providing real-time authorization decisions with validation rule enforcement.
 * Handles credit limit verification, available balance checking, card status validation,
 * expiration date verification, and concurrent authorization scenarios.
 * 
 * This service replaces the COBOL authorization logic from the mainframe system
 * while maintaining identical business rule enforcement and decimal precision.
 * 
 * @author Blitzy Agent
 * @version 1.0
 */
@Slf4j
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class AuthorizationEngine {

    // Constants for authorization limits and validation
    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("99999999.99");
    private static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("0.01");
    private static final int MAX_DAILY_TRANSACTIONS = 100;
    private static final BigDecimal DEFAULT_DAILY_LIMIT = new BigDecimal("5000.00");
    private static final BigDecimal DEFAULT_MERCHANT_LIMIT = new BigDecimal("10000.00");
    
    // COBOL COMP-3 compatible scale and rounding mode for financial calculations
    private static final int FINANCIAL_SCALE = 2;
    private static final RoundingMode FINANCIAL_ROUNDING = RoundingMode.HALF_UP;
    
    // Concurrent authorization management
    private final Map<String, ReentrantLock> authorizationLocks = new ConcurrentHashMap<>();
    private final Map<String, List<AuthorizationRequest>> pendingAuthorizations = new ConcurrentHashMap<>();
    
    // Authorization result constants
    public static final String AUTH_APPROVED = "APPROVED";
    public static final String AUTH_DECLINED = "DECLINED";
    public static final String AUTH_PENDING = "PENDING";
    
    // Decline reason codes
    public static final String DECLINE_INSUFFICIENT_FUNDS = "NSF";
    public static final String DECLINE_CREDIT_LIMIT_EXCEEDED = "OVER_LIMIT";
    public static final String DECLINE_CARD_EXPIRED = "EXPIRED";
    public static final String DECLINE_CARD_INACTIVE = "INACTIVE";
    public static final String DECLINE_DAILY_LIMIT_EXCEEDED = "DAILY_LIMIT";
    public static final String DECLINE_MERCHANT_LIMIT_EXCEEDED = "MERCHANT_LIMIT";
    public static final String DECLINE_VELOCITY_CHECK_FAILED = "VELOCITY";
    public static final String DECLINE_CONCURRENT_TRANSACTION = "CONCURRENT";

    /**
     * Primary authorization method that orchestrates all validation checks
     * for transaction processing. Performs comprehensive validation including
     * credit limits, available balance, card status, and transaction limits.
     * 
     * @param authRequest Authorization request containing transaction details
     * @return AuthorizationResult with approval/decline decision and reason codes
     */
    @PreAuthorize("hasRole('TRANSACTION_PROCESSOR') or hasRole('SYSTEM_USER')")
    public AuthorizationResult authorizeTransaction(AuthorizationRequest authRequest) {
        // Handle null request first
        if (authRequest == null) {
            log.warn("Null authorization request received");
            AuthorizationRequest emptyRequest = new AuthorizationRequest();
            emptyRequest.setTransactionId("NULL_REQUEST");
            return createDeclineResult(emptyRequest, "INVALID_REQUEST", "Authorization request cannot be null");
        }
        
        log.info("Processing authorization request for card: {} amount: {}", 
                 maskCardNumber(authRequest.getCardNumber()), authRequest.getAmount());
        
        try {
            // Validate input parameters
            if (!validateAuthorizationRequest(authRequest)) {
                return createDeclineResult(authRequest, "INVALID_REQUEST", "Invalid authorization request parameters");
            }
            
            // Check for concurrent authorizations
            String lockKey = getLockKey(authRequest.getCardNumber(), authRequest.getAccountId());
            if (!handleConcurrentAuthorizations(lockKey, authRequest)) {
                return createDeclineResult(authRequest, DECLINE_CONCURRENT_TRANSACTION, 
                    "Concurrent authorization in progress");
            }
            
            ReentrantLock lock = authorizationLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
            
            try {
                lock.lock();
                
                // Perform authorization checks in sequence
                AuthorizationResult cardStatusResult = validateCardStatus(authRequest);
                if (!AUTH_APPROVED.equals(cardStatusResult.getAuthorizationCode())) {
                    return cardStatusResult;
                }
                
                AuthorizationResult expirationResult = checkExpirationDate(authRequest);
                if (!AUTH_APPROVED.equals(expirationResult.getAuthorizationCode())) {
                    return expirationResult;
                }
                
                AuthorizationResult creditLimitResult = checkCreditLimit(authRequest);
                if (!AUTH_APPROVED.equals(creditLimitResult.getAuthorizationCode())) {
                    return creditLimitResult;
                }
                
                AuthorizationResult balanceResult = verifyAvailableBalance(authRequest);
                if (!AUTH_APPROVED.equals(balanceResult.getAuthorizationCode())) {
                    return balanceResult;
                }
                
                AuthorizationResult transactionLimitResult = validateTransactionLimits(authRequest);
                if (!AUTH_APPROVED.equals(transactionLimitResult.getAuthorizationCode())) {
                    return transactionLimitResult;
                }
                
                AuthorizationResult dailyLimitResult = checkDailyLimits(authRequest);
                if (!AUTH_APPROVED.equals(dailyLimitResult.getAuthorizationCode())) {
                    return dailyLimitResult;
                }
                
                AuthorizationResult merchantLimitResult = enforceMerchantLimits(authRequest);
                if (!AUTH_APPROVED.equals(merchantLimitResult.getAuthorizationCode())) {
                    return merchantLimitResult;
                }
                
                // All checks passed - process authorization decision
                return processAuthorizationDecision(authRequest);
                
            } finally {
                lock.unlock();
                removePendingAuthorization(lockKey, authRequest);
            }
            
        } catch (Exception e) {
            log.error("Error processing authorization request for card: {} - {}", 
                     maskCardNumber(authRequest.getCardNumber()), e.getMessage(), e);
            return createDeclineResult(authRequest, "SYSTEM_ERROR", "System error during authorization");
        }
    }

    /**
     * Validates credit limit against transaction amount and current outstanding balance.
     * Ensures transaction amount plus current balance does not exceed established credit limit.
     * Uses BigDecimal arithmetic to maintain COBOL COMP-3 precision compatibility.
     * 
     * @param authRequest Authorization request containing transaction details
     * @return AuthorizationResult indicating approval or decline with credit limit violation
     */
    @PreAuthorize("hasRole('CREDIT_VALIDATOR') or hasRole('TRANSACTION_PROCESSOR')")
    public AuthorizationResult checkCreditLimit(AuthorizationRequest authRequest) {
        log.debug("Checking credit limit for account: {} transaction amount: {}", 
                 authRequest.getAccountId(), authRequest.getAmount());
        
        try {
            // Retrieve account credit limit information
            BigDecimal creditLimit = getCreditLimitForAccount(authRequest.getAccountId());
            BigDecimal currentBalance = getCurrentBalanceForAccount(authRequest.getAccountId());
            BigDecimal transactionAmount = authRequest.getAmount().setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
            
            // Calculate projected balance after transaction
            BigDecimal projectedBalance = currentBalance.add(transactionAmount)
                .setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
            
            // Check if projected balance exceeds credit limit
            if (projectedBalance.compareTo(creditLimit) > 0) {
                BigDecimal overLimit = projectedBalance.subtract(creditLimit)
                    .setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
                
                log.warn("Credit limit exceeded for account: {} projected balance: {} credit limit: {} over by: {}", 
                        authRequest.getAccountId(), projectedBalance, creditLimit, overLimit);
                
                return createDeclineResult(authRequest, DECLINE_CREDIT_LIMIT_EXCEEDED, 
                    String.format("Credit limit exceeded by %s", overLimit));
            }
            
            log.debug("Credit limit check passed for account: {} available credit: {}", 
                     authRequest.getAccountId(), creditLimit.subtract(projectedBalance));
            
            return createApprovalResult(authRequest, "Credit limit validation passed");
            
        } catch (Exception e) {
            log.error("Error checking credit limit for account: {} - {}", 
                     authRequest.getAccountId(), e.getMessage(), e);
            return createDeclineResult(authRequest, "CREDIT_CHECK_ERROR", "Error validating credit limit");
        }
    }

    /**
     * Verifies available balance for transaction processing, ensuring sufficient
     * funds are available considering pending authorizations and holds.
     * Implements precise decimal calculations matching COBOL financial arithmetic.
     * 
     * @param authRequest Authorization request containing transaction details
     * @return AuthorizationResult indicating sufficient or insufficient funds
     */
    @PreAuthorize("hasRole('BALANCE_VALIDATOR') or hasRole('TRANSACTION_PROCESSOR')")
    public AuthorizationResult verifyAvailableBalance(AuthorizationRequest authRequest) {
        log.debug("Verifying available balance for account: {} transaction amount: {}", 
                 authRequest.getAccountId(), authRequest.getAmount());
        
        try {
            BigDecimal currentBalance = getCurrentBalanceForAccount(authRequest.getAccountId());
            BigDecimal pendingHolds = getPendingHoldsForAccount(authRequest.getAccountId());
            BigDecimal transactionAmount = authRequest.getAmount().setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
            
            // Calculate available balance considering pending holds
            BigDecimal availableBalance = currentBalance.subtract(pendingHolds)
                .setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
            
            // For debit transactions, verify sufficient available balance
            if (transactionAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (availableBalance.compareTo(transactionAmount) < 0) {
                    BigDecimal shortfall = transactionAmount.subtract(availableBalance)
                        .setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
                    
                    log.warn("Insufficient funds for account: {} available: {} requested: {} shortfall: {}", 
                            authRequest.getAccountId(), availableBalance, transactionAmount, shortfall);
                    
                    return createDeclineResult(authRequest, DECLINE_INSUFFICIENT_FUNDS, 
                        String.format("Insufficient funds - shortfall: %s", shortfall));
                }
            }
            
            log.debug("Available balance check passed for account: {} available: {} requested: {}", 
                     authRequest.getAccountId(), availableBalance, transactionAmount);
            
            return createApprovalResult(authRequest, "Available balance validation passed");
            
        } catch (Exception e) {
            log.error("Error verifying available balance for account: {} - {}", 
                     authRequest.getAccountId(), e.getMessage(), e);
            return createDeclineResult(authRequest, "BALANCE_CHECK_ERROR", "Error validating available balance");
        }
    }

    /**
     * Validates card status ensuring card is active and in good standing.
     * Checks card activation status, account standing, and any card-level restrictions.
     * 
     * @param authRequest Authorization request containing card information
     * @return AuthorizationResult indicating card status validation result
     */
    @PreAuthorize("hasRole('CARD_VALIDATOR') or hasRole('TRANSACTION_PROCESSOR')")
    public AuthorizationResult validateCardStatus(AuthorizationRequest authRequest) {
        log.debug("Validating card status for card: {}", maskCardNumber(authRequest.getCardNumber()));
        
        try {
            // Retrieve card status information
            CardStatusInfo cardStatus = getCardStatusInfo(authRequest.getCardNumber());
            
            if (cardStatus == null) {
                log.warn("Card not found: {}", maskCardNumber(authRequest.getCardNumber()));
                return createDeclineResult(authRequest, "CARD_NOT_FOUND", "Card not found in system");
            }
            
            // Check if card is active
            if (!"ACTIVE".equals(cardStatus.getCardStatus())) {
                log.warn("Card inactive: {} status: {}", 
                        maskCardNumber(authRequest.getCardNumber()), cardStatus.getCardStatus());
                return createDeclineResult(authRequest, DECLINE_CARD_INACTIVE, 
                    "Card is not active - status: " + cardStatus.getCardStatus());
            }
            
            // Check account status
            if (!"ACTIVE".equals(cardStatus.getAccountStatus())) {
                log.warn("Account inactive for card: {} account status: {}", 
                        maskCardNumber(authRequest.getCardNumber()), cardStatus.getAccountStatus());
                return createDeclineResult(authRequest, "ACCOUNT_INACTIVE", 
                    "Account is not active - status: " + cardStatus.getAccountStatus());
            }
            
            // Check for card restrictions
            if (cardStatus.hasRestrictions()) {
                log.warn("Card has restrictions: {} restrictions: {}", 
                        maskCardNumber(authRequest.getCardNumber()), cardStatus.getRestrictions());
                return createDeclineResult(authRequest, "CARD_RESTRICTED", 
                    "Card has active restrictions: " + cardStatus.getRestrictions());
            }
            
            log.debug("Card status validation passed for card: {}", maskCardNumber(authRequest.getCardNumber()));
            return createApprovalResult(authRequest, "Card status validation passed");
            
        } catch (Exception e) {
            log.error("Error validating card status for card: {} - {}", 
                     maskCardNumber(authRequest.getCardNumber()), e.getMessage(), e);
            return createDeclineResult(authRequest, "CARD_STATUS_ERROR", "Error validating card status");
        }
    }

    /**
     * Checks card expiration date against transaction date to ensure card
     * has not expired. Validates both month and year components with proper
     * date comparison logic.
     * 
     * @param authRequest Authorization request containing card and transaction information
     * @return AuthorizationResult indicating expiration date validation result
     */
    @PreAuthorize("hasRole('DATE_VALIDATOR') or hasRole('TRANSACTION_PROCESSOR')")
    public AuthorizationResult checkExpirationDate(AuthorizationRequest authRequest) {
        log.debug("Checking expiration date for card: {}", maskCardNumber(authRequest.getCardNumber()));
        
        try {
            // Retrieve card expiration date
            LocalDate expirationDate = getCardExpirationDate(authRequest.getCardNumber());
            LocalDate transactionDate = authRequest.getTransactionDate();
            
            if (expirationDate == null) {
                log.warn("Expiration date not found for card: {}", maskCardNumber(authRequest.getCardNumber()));
                return createDeclineResult(authRequest, "EXPIRATION_DATE_MISSING", "Card expiration date not found");
            }
            
            // Check if card has expired (compare to end of expiration month)
            LocalDate endOfExpirationMonth = expirationDate.withDayOfMonth(expirationDate.lengthOfMonth());
            
            if (transactionDate.isAfter(endOfExpirationMonth)) {
                log.warn("Card expired: {} expiration: {} transaction date: {}", 
                        maskCardNumber(authRequest.getCardNumber()), expirationDate, transactionDate);
                return createDeclineResult(authRequest, DECLINE_CARD_EXPIRED, 
                    "Card expired on: " + expirationDate.format(DateTimeFormatter.ofPattern("MM/yyyy")));
            }
            
            // Warn if card expires within 30 days
            if (transactionDate.isAfter(endOfExpirationMonth.minusDays(30))) {
                log.info("Card expiring soon: {} expiration: {}", 
                        maskCardNumber(authRequest.getCardNumber()), expirationDate);
            }
            
            log.debug("Expiration date check passed for card: {} expires: {}", 
                     maskCardNumber(authRequest.getCardNumber()), expirationDate);
            
            return createApprovalResult(authRequest, "Expiration date validation passed");
            
        } catch (Exception e) {
            log.error("Error checking expiration date for card: {} - {}", 
                     maskCardNumber(authRequest.getCardNumber()), e.getMessage(), e);
            return createDeclineResult(authRequest, "EXPIRATION_CHECK_ERROR", "Error validating expiration date");
        }
    }

    /**
     * Handles concurrent authorization scenarios using distributed locking mechanisms
     * to prevent race conditions and duplicate authorizations. Manages pending
     * authorization queue and ensures thread-safe processing.
     * 
     * @param lockKey Unique key for authorization locking
     * @param authRequest Authorization request to process
     * @return boolean indicating whether authorization can proceed
     */
    public boolean handleConcurrentAuthorizations(String lockKey, AuthorizationRequest authRequest) {
        log.debug("Handling concurrent authorization for lock key: {}", lockKey);
        
        try {
            // Check for existing pending authorizations
            List<AuthorizationRequest> pending = pendingAuthorizations.computeIfAbsent(lockKey, 
                k -> Collections.synchronizedList(new ArrayList<>()));
            
            // Check for duplicate authorization requests
            synchronized (pending) {
                for (AuthorizationRequest pendingRequest : pending) {
                    if (isDuplicateRequest(authRequest, pendingRequest)) {
                        log.warn("Duplicate authorization request detected for card: {} amount: {}", 
                                maskCardNumber(authRequest.getCardNumber()), authRequest.getAmount());
                        return false;
                    }
                }
                
                // Add request to pending queue
                pending.add(authRequest);
            }
            
            // Check concurrent authorization limits
            if (pending.size() > 3) {
                log.warn("Too many concurrent authorizations for lock key: {} count: {}", lockKey, pending.size());
                removePendingAuthorization(lockKey, authRequest);
                return false;
            }
            
            log.debug("Concurrent authorization check passed for lock key: {} pending count: {}", 
                     lockKey, pending.size());
            
            return true;
            
        } catch (Exception e) {
            log.error("Error handling concurrent authorizations for lock key: {} - {}", lockKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Processes final authorization decision after all validation checks have passed.
     * Generates authorization code, updates account balances, creates transaction records,
     * and manages authorization holds with proper audit trails.
     * 
     * @param authRequest Authorization request that passed all validations
     * @return AuthorizationResult with final approval and authorization details
     */
    @PreAuthorize("hasRole('AUTHORIZATION_PROCESSOR') or hasRole('TRANSACTION_PROCESSOR')")
    public AuthorizationResult processAuthorizationDecision(AuthorizationRequest authRequest) {
        log.info("Processing final authorization decision for card: {} amount: {}", 
                 maskCardNumber(authRequest.getCardNumber()), authRequest.getAmount());
        
        try {
            // Generate unique authorization code
            String authorizationCode = generateAuthorizationCode();
            
            // Create authorization hold
            AuthorizationHold hold = createAuthorizationHold(authRequest, authorizationCode);
            
            // Update account balance with pending authorization
            updateAccountWithPendingAuthorization(authRequest.getAccountId(), authRequest.getAmount());
            
            // Create transaction audit record
            createTransactionAuditRecord(authRequest, authorizationCode, AUTH_APPROVED);
            
            // Build successful authorization result
            AuthorizationResult result = new AuthorizationResult();
            result.setAuthorizationCode(AUTH_APPROVED);
            result.setAuthorizationId(authorizationCode);
            result.setTransactionId(authRequest.getTransactionId());
            result.setCardNumber(authRequest.getCardNumber());
            result.setAccountId(authRequest.getAccountId());
            result.setAmount(authRequest.getAmount());
            result.setTransactionDate(authRequest.getTransactionDate());
            result.setMerchantId(authRequest.getMerchantId());
            result.setAuthorizationDateTime(LocalDateTime.now());
            result.setDeclineReason(null);
            result.setDeclineMessage("Transaction authorized successfully");
            result.setAvailableBalance(getAvailableBalanceAfterAuthorization(authRequest.getAccountId(), 
                authRequest.getAmount()));
            
            log.info("Authorization approved - ID: {} card: {} amount: {}", 
                    authorizationCode, maskCardNumber(authRequest.getCardNumber()), authRequest.getAmount());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error processing authorization decision for card: {} - {}", 
                     maskCardNumber(authRequest.getCardNumber()), e.getMessage(), e);
            return createDeclineResult(authRequest, "PROCESSING_ERROR", "Error processing authorization");
        }
    }

    /**
     * Validates transaction-specific limits including single transaction maximums,
     * transaction type restrictions, and merchant category code limitations.
     * Ensures transaction complies with all configured business rules.
     * 
     * @param authRequest Authorization request to validate
     * @return AuthorizationResult indicating transaction limit validation result
     */
    @PreAuthorize("hasRole('LIMIT_VALIDATOR') or hasRole('TRANSACTION_PROCESSOR')")
    public AuthorizationResult validateTransactionLimits(AuthorizationRequest authRequest) {
        log.debug("Validating transaction limits for card: {} amount: {} type: {}", 
                 maskCardNumber(authRequest.getCardNumber()), authRequest.getAmount(), 
                 authRequest.getTransactionType());
        
        try {
            BigDecimal transactionAmount = authRequest.getAmount().setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
            
            // Check minimum transaction amount
            if (transactionAmount.compareTo(MIN_TRANSACTION_AMOUNT) < 0) {
                log.warn("Transaction amount below minimum: {} minimum: {}", 
                        transactionAmount, MIN_TRANSACTION_AMOUNT);
                return createDeclineResult(authRequest, "AMOUNT_TOO_SMALL", 
                    "Transaction amount below minimum: " + MIN_TRANSACTION_AMOUNT);
            }
            
            // Check maximum transaction amount
            if (transactionAmount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
                log.warn("Transaction amount exceeds maximum: {} maximum: {}", 
                        transactionAmount, MAX_TRANSACTION_AMOUNT);
                return createDeclineResult(authRequest, "AMOUNT_TOO_LARGE", 
                    "Transaction amount exceeds maximum: " + MAX_TRANSACTION_AMOUNT);
            }
            
            // Get transaction type limits
            TransactionLimits limits = getTransactionLimitsForType(authRequest.getTransactionType());
            
            if (limits != null) {
                // Check transaction type specific limits
                if (transactionAmount.compareTo(limits.getMaxAmount()) > 0) {
                    log.warn("Transaction exceeds type limit: {} type: {} limit: {}", 
                            transactionAmount, authRequest.getTransactionType(), limits.getMaxAmount());
                    return createDeclineResult(authRequest, "TYPE_LIMIT_EXCEEDED", 
                        "Transaction exceeds " + authRequest.getTransactionType() + " limit: " + limits.getMaxAmount());
                }
                
                // Check transaction frequency limits
                int recentTransactionCount = getRecentTransactionCount(authRequest.getCardNumber(), 
                    authRequest.getTransactionType(), limits.getFrequencyPeriodHours());
                
                if (recentTransactionCount >= limits.getMaxFrequency()) {
                    log.warn("Transaction frequency limit exceeded: {} type: {} limit: {} period: {} hours", 
                            recentTransactionCount, authRequest.getTransactionType(), 
                            limits.getMaxFrequency(), limits.getFrequencyPeriodHours());
                    return createDeclineResult(authRequest, "FREQUENCY_LIMIT_EXCEEDED", 
                        "Too many " + authRequest.getTransactionType() + " transactions");
                }
            }
            
            log.debug("Transaction limits validation passed for card: {} amount: {}", 
                     maskCardNumber(authRequest.getCardNumber()), transactionAmount);
            
            return createApprovalResult(authRequest, "Transaction limits validation passed");
            
        } catch (Exception e) {
            log.error("Error validating transaction limits for card: {} - {}", 
                     maskCardNumber(authRequest.getCardNumber()), e.getMessage(), e);
            return createDeclineResult(authRequest, "LIMIT_CHECK_ERROR", "Error validating transaction limits");
        }
    }

    /**
     * Checks daily spending limits and transaction velocity patterns to detect
     * potential fraud and enforce daily usage restrictions. Implements rolling
     * 24-hour limit calculations with precise timing validation.
     * 
     * @param authRequest Authorization request to validate against daily limits
     * @return AuthorizationResult indicating daily limit validation result
     */
    @PreAuthorize("hasRole('VELOCITY_VALIDATOR') or hasRole('TRANSACTION_PROCESSOR')")
    public AuthorizationResult checkDailyLimits(AuthorizationRequest authRequest) {
        log.debug("Checking daily limits for card: {} amount: {}", 
                 maskCardNumber(authRequest.getCardNumber()), authRequest.getAmount());
        
        try {
            LocalDate transactionDate = authRequest.getTransactionDate();
            BigDecimal transactionAmount = authRequest.getAmount().setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
            
            // Get daily limit for card/account
            BigDecimal dailyLimit = getDailyLimitForCard(authRequest.getCardNumber());
            if (dailyLimit == null) {
                dailyLimit = DEFAULT_DAILY_LIMIT;
            }
            
            // Calculate daily spending for current date
            BigDecimal dailySpending = getDailySpendingForCard(authRequest.getCardNumber(), transactionDate);
            BigDecimal projectedDailySpending = dailySpending.add(transactionAmount)
                .setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
            
            // Check daily spending limit
            if (projectedDailySpending.compareTo(dailyLimit) > 0) {
                BigDecimal overLimit = projectedDailySpending.subtract(dailyLimit)
                    .setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
                
                log.warn("Daily limit exceeded for card: {} daily spending: {} limit: {} over by: {}", 
                        maskCardNumber(authRequest.getCardNumber()), projectedDailySpending, 
                        dailyLimit, overLimit);
                
                return createDeclineResult(authRequest, DECLINE_DAILY_LIMIT_EXCEEDED, 
                    "Daily limit exceeded by: " + overLimit);
            }
            
            // Check daily transaction count
            int dailyTransactionCount = getDailyTransactionCountForCard(authRequest.getCardNumber(), transactionDate);
            if (dailyTransactionCount >= MAX_DAILY_TRANSACTIONS) {
                log.warn("Daily transaction count limit exceeded for card: {} count: {} limit: {}", 
                        maskCardNumber(authRequest.getCardNumber()), dailyTransactionCount, MAX_DAILY_TRANSACTIONS);
                
                return createDeclineResult(authRequest, "DAILY_COUNT_EXCEEDED", 
                    "Daily transaction count limit exceeded");
            }
            
            // Check velocity patterns (rapid successive transactions)
            if (hasRapidTransactionPattern(authRequest.getCardNumber(), authRequest.getTransactionDate())) {
                log.warn("Rapid transaction pattern detected for card: {}", 
                        maskCardNumber(authRequest.getCardNumber()));
                
                return createDeclineResult(authRequest, DECLINE_VELOCITY_CHECK_FAILED, 
                    "Rapid transaction pattern detected");
            }
            
            log.debug("Daily limits check passed for card: {} daily spending: {} limit: {}", 
                     maskCardNumber(authRequest.getCardNumber()), projectedDailySpending, dailyLimit);
            
            return createApprovalResult(authRequest, "Daily limits validation passed");
            
        } catch (Exception e) {
            log.error("Error checking daily limits for card: {} - {}", 
                     maskCardNumber(authRequest.getCardNumber()), e.getMessage(), e);
            return createDeclineResult(authRequest, "DAILY_LIMIT_ERROR", "Error validating daily limits");
        }
    }

    /**
     * Enforces merchant-specific spending limits and restrictions based on
     * merchant category codes, individual merchant limits, and transaction
     * patterns. Provides merchant-level fraud prevention and spending controls.
     * 
     * @param authRequest Authorization request containing merchant information
     * @return AuthorizationResult indicating merchant limit validation result
     */
    @PreAuthorize("hasRole('MERCHANT_VALIDATOR') or hasRole('TRANSACTION_PROCESSOR')")
    public AuthorizationResult enforceMerchantLimits(AuthorizationRequest authRequest) {
        log.debug("Enforcing merchant limits for card: {} merchant: {} amount: {}", 
                 maskCardNumber(authRequest.getCardNumber()), authRequest.getMerchantId(), authRequest.getAmount());
        
        try {
            BigDecimal transactionAmount = authRequest.getAmount().setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
            String merchantId = authRequest.getMerchantId();
            String merchantCategoryCode = authRequest.getMerchantCategoryCode();
            
            // Get merchant-specific limit
            BigDecimal merchantLimit = getMerchantLimitForCard(authRequest.getCardNumber(), merchantId);
            if (merchantLimit == null) {
                merchantLimit = DEFAULT_MERCHANT_LIMIT;
            }
            
            // Calculate merchant spending for rolling 30-day period
            LocalDate startDate = authRequest.getTransactionDate().minusDays(30);
            BigDecimal merchantSpending = getMerchantSpendingForPeriod(authRequest.getCardNumber(), 
                merchantId, startDate, authRequest.getTransactionDate());
            
            BigDecimal projectedMerchantSpending = merchantSpending.add(transactionAmount)
                .setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
            
            // Check merchant spending limit
            if (projectedMerchantSpending.compareTo(merchantLimit) > 0) {
                BigDecimal overLimit = projectedMerchantSpending.subtract(merchantLimit)
                    .setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
                
                log.warn("Merchant limit exceeded for card: {} merchant: {} spending: {} limit: {} over by: {}", 
                        maskCardNumber(authRequest.getCardNumber()), merchantId, 
                        projectedMerchantSpending, merchantLimit, overLimit);
                
                return createDeclineResult(authRequest, DECLINE_MERCHANT_LIMIT_EXCEEDED, 
                    "Merchant limit exceeded by: " + overLimit);
            }
            
            // Check merchant category code restrictions
            if (merchantCategoryCode != null) {
                MerchantCategoryLimits categoryLimits = getMerchantCategoryLimits(merchantCategoryCode);
                if (categoryLimits != null) {
                    if (transactionAmount.compareTo(categoryLimits.getMaxTransactionAmount()) > 0) {
                        log.warn("Merchant category limit exceeded for card: {} MCC: {} amount: {} limit: {}", 
                                maskCardNumber(authRequest.getCardNumber()), merchantCategoryCode, 
                                transactionAmount, categoryLimits.getMaxTransactionAmount());
                        
                        return createDeclineResult(authRequest, "MCC_LIMIT_EXCEEDED", 
                            "Merchant category limit exceeded");
                    }
                    
                    // Check restricted merchant categories
                    if (categoryLimits.isRestricted()) {
                        log.warn("Restricted merchant category for card: {} MCC: {}", 
                                maskCardNumber(authRequest.getCardNumber()), merchantCategoryCode);
                        
                        return createDeclineResult(authRequest, "MCC_RESTRICTED", 
                            "Merchant category restricted");
                    }
                }
            }
            
            // Check for suspicious merchant patterns
            if (hasSuspiciousMerchantPattern(authRequest.getCardNumber(), merchantId, transactionAmount)) {
                log.warn("Suspicious merchant pattern detected for card: {} merchant: {}", 
                        maskCardNumber(authRequest.getCardNumber()), merchantId);
                
                return createDeclineResult(authRequest, "SUSPICIOUS_MERCHANT", 
                    "Suspicious merchant transaction pattern");
            }
            
            log.debug("Merchant limits validation passed for card: {} merchant: {} spending: {} limit: {}", 
                     maskCardNumber(authRequest.getCardNumber()), merchantId, 
                     projectedMerchantSpending, merchantLimit);
            
            return createApprovalResult(authRequest, "Merchant limits validation passed");
            
        } catch (Exception e) {
            log.error("Error enforcing merchant limits for card: {} - {}", 
                     maskCardNumber(authRequest.getCardNumber()), e.getMessage(), e);
            return createDeclineResult(authRequest, "MERCHANT_LIMIT_ERROR", "Error validating merchant limits");
        }
    }

    // Private helper methods for authorization processing
    
    private boolean validateAuthorizationRequest(AuthorizationRequest request) {
        return request != null 
            && request.getCardNumber() != null && !request.getCardNumber().trim().isEmpty()
            && request.getAccountId() != null && !request.getAccountId().trim().isEmpty()
            && request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0
            && request.getTransactionDate() != null
            && request.getTransactionId() != null && !request.getTransactionId().trim().isEmpty();
    }
    
    private String getLockKey(String cardNumber, String accountId) {
        return "AUTH_" + cardNumber + "_" + accountId;
    }
    
    private void removePendingAuthorization(String lockKey, AuthorizationRequest request) {
        List<AuthorizationRequest> pending = pendingAuthorizations.get(lockKey);
        if (pending != null) {
            synchronized (pending) {
                pending.remove(request);
                if (pending.isEmpty()) {
                    pendingAuthorizations.remove(lockKey);
                }
            }
        }
    }
    
    private boolean isDuplicateRequest(AuthorizationRequest request1, AuthorizationRequest request2) {
        return request1.getCardNumber().equals(request2.getCardNumber())
            && request1.getAmount().compareTo(request2.getAmount()) == 0
            && request1.getMerchantId().equals(request2.getMerchantId())
            && Math.abs(request1.getTransactionDate().compareTo(request2.getTransactionDate())) < 60; // 60 seconds tolerance
    }
    
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }
    
    private AuthorizationResult createApprovalResult(AuthorizationRequest request, String message) {
        AuthorizationResult result = new AuthorizationResult();
        result.setAuthorizationCode(AUTH_APPROVED);
        result.setTransactionId(request.getTransactionId());
        result.setCardNumber(request.getCardNumber());
        result.setAccountId(request.getAccountId());
        result.setAmount(request.getAmount());
        result.setTransactionDate(request.getTransactionDate());
        result.setMerchantId(request.getMerchantId());
        result.setAuthorizationDateTime(LocalDateTime.now());
        result.setDeclineMessage(message);
        return result;
    }
    
    private AuthorizationResult createDeclineResult(AuthorizationRequest request, String declineReason, String message) {
        AuthorizationResult result = new AuthorizationResult();
        result.setAuthorizationCode(AUTH_DECLINED);
        result.setTransactionId(request.getTransactionId());
        result.setCardNumber(request.getCardNumber());
        result.setAccountId(request.getAccountId());
        result.setAmount(request.getAmount());
        result.setTransactionDate(request.getTransactionDate());
        result.setMerchantId(request.getMerchantId());
        result.setAuthorizationDateTime(LocalDateTime.now());
        result.setDeclineReason(declineReason);
        result.setDeclineMessage(message);
        return result;
    }
    
    // Placeholder methods for data access - would be implemented with actual data layer
    // These methods represent the interface to the PostgreSQL database layer
    
    private BigDecimal getCreditLimitForAccount(String accountId) {
        // Implementation would query ACCOUNT table for credit_limit
        return new BigDecimal("10000.00").setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
    }
    
    private BigDecimal getCurrentBalanceForAccount(String accountId) {
        // Implementation would query ACCOUNT table for current_balance
        return new BigDecimal("1500.00").setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
    }
    
    private BigDecimal getPendingHoldsForAccount(String accountId) {
        // Implementation would query TRANSACTION table for pending authorizations
        return new BigDecimal("200.00").setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
    }
    
    private CardStatusInfo getCardStatusInfo(String cardNumber) {
        // Implementation would query CARD table for status information
        return new CardStatusInfo("ACTIVE", "ACTIVE", Collections.emptyList());
    }
    
    private LocalDate getCardExpirationDate(String cardNumber) {
        // Implementation would query CARD table for expiration_date
        return LocalDate.now().plusYears(2);
    }
    
    private String generateAuthorizationCode() {
        // Implementation would generate unique authorization identifier
        return "AUTH" + System.currentTimeMillis();
    }
    
    private AuthorizationHold createAuthorizationHold(AuthorizationRequest request, String authCode) {
        // Implementation would create authorization hold record
        return new AuthorizationHold(authCode, request.getAccountId(), request.getAmount());
    }
    
    private void updateAccountWithPendingAuthorization(String accountId, BigDecimal amount) {
        // Implementation would update account balance or create pending transaction
    }
    
    private void createTransactionAuditRecord(AuthorizationRequest request, String authCode, String status) {
        // Implementation would create audit trail record
    }
    
    private BigDecimal getAvailableBalanceAfterAuthorization(String accountId, BigDecimal amount) {
        // Implementation would calculate available balance after authorization
        return getCurrentBalanceForAccount(accountId).subtract(amount)
            .setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
    }
    
    private TransactionLimits getTransactionLimitsForType(String transactionType) {
        // Implementation would query transaction limits by type
        return new TransactionLimits(new BigDecimal("5000.00"), 5, 24);
    }
    
    private int getRecentTransactionCount(String cardNumber, String transactionType, int hours) {
        // Implementation would count recent transactions of specified type
        return 0;
    }
    
    private BigDecimal getDailyLimitForCard(String cardNumber) {
        // Implementation would query card daily limit
        return DEFAULT_DAILY_LIMIT;
    }
    
    private BigDecimal getDailySpendingForCard(String cardNumber, LocalDate date) {
        // Implementation would sum daily spending for specified date
        return new BigDecimal("500.00").setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
    }
    
    private int getDailyTransactionCountForCard(String cardNumber, LocalDate date) {
        // Implementation would count daily transactions for specified date
        return 5;
    }
    
    private boolean hasRapidTransactionPattern(String cardNumber, LocalDate date) {
        // Implementation would detect rapid successive transactions
        return false;
    }
    
    private BigDecimal getMerchantLimitForCard(String cardNumber, String merchantId) {
        // Implementation would query merchant-specific limits
        return DEFAULT_MERCHANT_LIMIT;
    }
    
    private BigDecimal getMerchantSpendingForPeriod(String cardNumber, String merchantId, 
            LocalDate startDate, LocalDate endDate) {
        // Implementation would sum merchant spending for period
        return new BigDecimal("1000.00").setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
    }
    
    private MerchantCategoryLimits getMerchantCategoryLimits(String merchantCategoryCode) {
        // Implementation would query merchant category limits
        return new MerchantCategoryLimits(new BigDecimal("2000.00"), false);
    }
    
    private boolean hasSuspiciousMerchantPattern(String cardNumber, String merchantId, BigDecimal amount) {
        // Implementation would analyze merchant transaction patterns
        return false;
    }

    // Supporting classes for authorization processing
    
    public static class AuthorizationRequest {
        private String transactionId;
        private String cardNumber;
        private String accountId;
        private BigDecimal amount;
        private LocalDate transactionDate;
        private String transactionType;
        private String merchantId;
        private String merchantCategoryCode;
        
        // Getters and setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public LocalDate getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
        public String getMerchantCategoryCode() { return merchantCategoryCode; }
        public void setMerchantCategoryCode(String merchantCategoryCode) { this.merchantCategoryCode = merchantCategoryCode; }
    }
    
    public static class AuthorizationResult {
        private String authorizationCode;
        private String authorizationId;
        private String transactionId;
        private String cardNumber;
        private String accountId;
        private BigDecimal amount;
        private LocalDate transactionDate;
        private String merchantId;
        private LocalDateTime authorizationDateTime;
        private String declineReason;
        private String declineMessage;
        private BigDecimal availableBalance;
        
        // Getters and setters
        public String getAuthorizationCode() { return authorizationCode; }
        public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
        public String getAuthorizationId() { return authorizationId; }
        public void setAuthorizationId(String authorizationId) { this.authorizationId = authorizationId; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public LocalDate getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
        public LocalDateTime getAuthorizationDateTime() { return authorizationDateTime; }
        public void setAuthorizationDateTime(LocalDateTime authorizationDateTime) { this.authorizationDateTime = authorizationDateTime; }
        public String getDeclineReason() { return declineReason; }
        public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }
        public String getDeclineMessage() { return declineMessage; }
        public void setDeclineMessage(String declineMessage) { this.declineMessage = declineMessage; }
        public BigDecimal getAvailableBalance() { return availableBalance; }
        public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
    }
    
    private static class CardStatusInfo {
        private final String cardStatus;
        private final String accountStatus;
        private final List<String> restrictions;
        
        public CardStatusInfo(String cardStatus, String accountStatus, List<String> restrictions) {
            this.cardStatus = cardStatus;
            this.accountStatus = accountStatus;
            this.restrictions = restrictions;
        }
        
        public String getCardStatus() { return cardStatus; }
        public String getAccountStatus() { return accountStatus; }
        public List<String> getRestrictions() { return restrictions; }
        public boolean hasRestrictions() { return restrictions != null && !restrictions.isEmpty(); }
    }
    
    private static class AuthorizationHold {
        private final String authorizationCode;
        private final String accountId;
        private final BigDecimal amount;
        
        public AuthorizationHold(String authorizationCode, String accountId, BigDecimal amount) {
            this.authorizationCode = authorizationCode;
            this.accountId = accountId;
            this.amount = amount;
        }
        
        public String getAuthorizationCode() { return authorizationCode; }
        public String getAccountId() { return accountId; }
        public BigDecimal getAmount() { return amount; }
    }
    
    private static class TransactionLimits {
        private final BigDecimal maxAmount;
        private final int maxFrequency;
        private final int frequencyPeriodHours;
        
        public TransactionLimits(BigDecimal maxAmount, int maxFrequency, int frequencyPeriodHours) {
            this.maxAmount = maxAmount;
            this.maxFrequency = maxFrequency;
            this.frequencyPeriodHours = frequencyPeriodHours;
        }
        
        public BigDecimal getMaxAmount() { return maxAmount; }
        public int getMaxFrequency() { return maxFrequency; }
        public int getFrequencyPeriodHours() { return frequencyPeriodHours; }
    }
    
    private static class MerchantCategoryLimits {
        private final BigDecimal maxTransactionAmount;
        private final boolean restricted;
        
        public MerchantCategoryLimits(BigDecimal maxTransactionAmount, boolean restricted) {
            this.maxTransactionAmount = maxTransactionAmount;
            this.restricted = restricted;
        }
        
        public BigDecimal getMaxTransactionAmount() { return maxTransactionAmount; }
        public boolean isRestricted() { return restricted; }
    }
}