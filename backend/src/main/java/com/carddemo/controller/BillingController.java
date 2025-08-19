package com.carddemo.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.constraints.Size;

import com.carddemo.service.BillingService;
import com.carddemo.dto.BillDto;
import com.carddemo.dto.BillingDto;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.util.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * REST Controller for billing operations handling CB00 transaction code.
 * Converts COBIL00C COBOL bill payment program to billing statement generation functionality.
 * 
 * This controller manages billing statement generation and display, replacing the original 
 * COBOL COBIL00C bill payment functionality with comprehensive billing statement operations.
 * Provides REST endpoints for statement retrieval, balance calculation, and billing data display.
 * 
 * Key Features:
 * - GET /api/billing/{accountId} for billing statement retrieval
 * - Statement balance calculation with BigDecimal precision
 * - Minimum payment calculation following COBOL financial logic
 * - Interest calculation using COMP-3 equivalent precision
 * - Transaction aggregation for billing period analysis
 * - Account validation and error handling
 * 
 * Maps to original COBOL transaction code CB00 while providing modern REST API access.
 * Maintains identical business logic for financial calculations and account validation.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private static final Logger logger = LoggerFactory.getLogger(BillingController.class);

    @Autowired
    private BillingService billingService;

    /**
     * Generate and retrieve comprehensive billing statement for specified account.
     * Maps to COBOL CB00 transaction code with REST endpoint functionality.
     * 
     * Implements the core COBIL00C PROCESS-ENTER-KEY logic converted to REST operation:
     * - Account validation (READ-ACCTDAT-FILE equivalent)
     * - Statement period calculation
     * - Transaction aggregation for billing period
     * - Balance and minimum payment calculation with BigDecimal precision
     * - Interest calculation following COBOL COMP-3 behavior
     * 
     * @param accountId Account identifier for statement generation (11 digits)
     * @return ResponseEntity containing complete billing statement data
     * @throws ResourceNotFoundException if account is not found
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<BillingDto> getBillingStatement(
            @PathVariable 
            @Size(min = 11, max = 11, message = "Account ID must be exactly 11 characters") 
            String accountId) {
        
        logger.info("Processing billing statement request for account: {}", accountId);
        
        try {
            // Validate account ID format using ValidationUtil (matching COBOL validation)
            ValidationUtil.validateRequiredField("accountId", accountId);
            new ValidationUtil.FieldValidator().validateAccountId(accountId);
            
            // Generate comprehensive billing statement using BillingService
            // This replaces COBOL PROCESS-ENTER-KEY paragraph logic
            LocalDate statementDate = LocalDate.now();
            Map<String, Object> billingStatementData = billingService.generateBillingStatement(accountId, statementDate);
            
            // Get statement period transactions for detailed analysis
            LocalDate periodStart = statementDate.minusMonths(1).withDayOfMonth(1);
            LocalDate periodEnd = statementDate.withDayOfMonth(1).minusDays(1);
            java.util.List<Map<String, Object>> periodTransactions = billingService.getStatementPeriodTransactions(accountId, periodStart, periodEnd);
            
            // Aggregate transaction totals using BillingService method
            Map<String, BigDecimal> aggregatedTotals = billingService.aggregateTransactionTotals(periodTransactions);
            
            // Extract calculated values from billing statement
            BigDecimal currentBalance = (BigDecimal) billingStatementData.get("currentBalance");
            BigDecimal minimumPayment = billingService.calculateMinimumPayment(currentBalance);
            BigDecimal interestCharge = billingService.calculateInterest(currentBalance, periodStart, periodEnd);
            
            LocalDate paymentDueDate = statementDate.plusDays(30); // Standard 30-day payment term
            LocalDate statementGenerationDate = LocalDate.parse((String) billingStatementData.get("statementDate"));
            
            // Build detailed BillDto with all financial information
            BillDto detailedBillStatement = BillDto.builder()
                .accountId(accountId)
                .statementDate(statementGenerationDate)
                .dueDate(paymentDueDate)
                .minimumPayment(minimumPayment)
                .statementBalance(currentBalance)
                .previousBalance(calculatePreviousBalance(billingStatementData))
                .paymentsCredits(aggregatedTotals.get("payments"))
                .purchasesDebits(aggregatedTotals.get("purchases"))
                .fees(aggregatedTotals.get("fees"))
                .interest(interestCharge)
                .build();
            
            // Create BillingDto response for REST API using BillDto data
            BillingDto billingResponse = createBillingDtoFromBillDto(detailedBillStatement);
            
            logger.info("Successfully generated billing statement for account: {} with balance: {} and {} transactions", 
                       accountId, currentBalance, periodTransactions.size());
            
            return ResponseEntity.status(HttpStatus.OK).body(billingResponse);
            
        } catch (IllegalArgumentException e) {
            // Handle account validation errors (equivalent to COBOL DFHRESP(NOTFND))
            logger.error("Account validation failed for account: {} - {}", accountId, e.getMessage());
            throw new ResourceNotFoundException("Account", accountId, "Account not found or invalid for billing operations");
            
        } catch (Exception e) {
            // Handle unexpected errors (equivalent to COBOL general error handling)
            logger.error("Unexpected error generating billing statement for account: {} - {}", accountId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieve simplified billing information for account overview.
     * Provides basic billing data for quick account balance and payment information display.
     * 
     * Alternative endpoint providing essential billing information without full statement generation.
     * Optimized for mobile applications and dashboard displays requiring basic account status.
     * 
     * @param accountId Account identifier for billing information retrieval
     * @return ResponseEntity containing basic billing information
     */
    @GetMapping("/{accountId}/summary")
    public ResponseEntity<BillingDto> getBilling(
            @PathVariable 
            @Size(min = 11, max = 11, message = "Account ID must be exactly 11 characters") 
            String accountId) {
        
        logger.info("Processing billing summary request for account: {}", accountId);
        
        try {
            // Validate account ID format
            ValidationUtil.validateRequiredField("accountId", accountId);
            new ValidationUtil.FieldValidator().validateAccountId(accountId);
            
            // Generate billing statement for current date
            LocalDate currentDate = LocalDate.now();
            Map<String, Object> billingData = billingService.generateBillingStatement(accountId, currentDate);
            
            // Get statement period for transaction analysis
            LocalDate periodStart = currentDate.minusMonths(1).withDayOfMonth(1);
            LocalDate periodEnd = currentDate.withDayOfMonth(1).minusDays(1);
            java.util.List<Map<String, Object>> transactions = billingService.getStatementPeriodTransactions(accountId, periodStart, periodEnd);
            
            // Aggregate transaction totals for period
            Map<String, BigDecimal> aggregatedTotals = billingService.aggregateTransactionTotals(transactions);
            
            // Extract essential billing information
            BigDecimal currentBalance = (BigDecimal) billingData.get("currentBalance");
            BigDecimal minimumPayment = billingService.calculateMinimumPayment(currentBalance);
            BigDecimal interestCharge = billingService.calculateInterest(currentBalance, periodStart, periodEnd);
            LocalDate dueDate = currentDate.plusDays(30);
            
            // Build BillDto with comprehensive data
            BillDto summaryBillData = BillDto.builder()
                .accountId(accountId)
                .statementDate(currentDate)
                .dueDate(dueDate)
                .minimumPayment(minimumPayment)
                .statementBalance(currentBalance)
                .previousBalance(calculatePreviousBalance(billingData))
                .paymentsCredits(aggregatedTotals.get("payments"))
                .purchasesDebits(aggregatedTotals.get("purchases"))
                .fees(aggregatedTotals.get("fees"))
                .interest(interestCharge)
                .build();
            
            // Create simplified billing response using BillDto data
            BillingDto billingInfo = createBillingDtoFromBillDto(summaryBillData);
            
            logger.info("Successfully retrieved billing summary for account: {} with {} transactions", accountId, transactions.size());
            
            return ResponseEntity.status(HttpStatus.OK).body(billingInfo);
            
        } catch (IllegalArgumentException e) {
            logger.error("Account validation failed for summary request - account: {} - {}", accountId, e.getMessage());
            throw new ResourceNotFoundException("Account", accountId, "Account not found for billing summary");
            
        } catch (Exception e) {
            logger.error("Error retrieving billing summary for account: {} - {}", accountId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create BillingDto from BillDto data using all required BillDto methods.
     * Implements schema requirements for accessing BillDto members and converting to BillingDto response.
     * 
     * @param billDto The detailed billing statement data
     * @return BillingDto optimized for REST API responses
     */
    private BillingDto createBillingDtoFromBillDto(BillDto billDto) {
        // Access all required BillDto methods according to schema
        String accountId = billDto.getAccountId();
        LocalDate statementDate = billDto.getStatementDate();
        LocalDate dueDate = billDto.getDueDate();
        BigDecimal minimumPayment = billDto.getMinimumPayment();
        BigDecimal statementBalance = billDto.getStatementBalance();
        BigDecimal previousBalance = billDto.getPreviousBalance();
        BigDecimal paymentsCredits = billDto.getPaymentsCredits();
        BigDecimal purchasesDebits = billDto.getPurchasesDebits();
        BigDecimal fees = billDto.getFees();
        BigDecimal interest = billDto.getInterest();
        
        // Create BillingDto using BillDto data with appropriate field mapping
        return new BillingDto(
            accountId,                  // getAccountId()
            statementBalance,           // getStatementBalance() -> getCurrentBalance()
            minimumPayment,             // getMinimumPayment()
            dueDate,                    // getDueDate() -> getPaymentDueDate()
            statementDate,              // getStatementDate()
            previousBalance,            // getPreviousBalance()
            purchasesDebits,            // getPurchasesDebits() -> getTotalPurchases()
            paymentsCredits             // getPaymentsCredits() -> getTotalPayments()
        );
    }

    /**
     * Calculate previous balance from billing statement data.
     * Helper method implementing COBOL balance calculation logic for statement periods.
     * 
     * @param billingStatementData Map containing billing statement information
     * @return Previous statement balance as BigDecimal
     */
    private BigDecimal calculatePreviousBalance(Map<String, Object> billingStatementData) {
        BigDecimal currentBalance = (BigDecimal) billingStatementData.get("currentBalance");
        
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> totals = (Map<String, BigDecimal>) billingStatementData.get("transactionTotals");
        
        BigDecimal purchases = totals.get("purchases");
        BigDecimal payments = totals.get("payments");
        BigDecimal interest = totals.get("interest");
        BigDecimal fees = totals.get("fees");
        
        // Calculate previous balance: current - purchases - interest - fees + payments
        BigDecimal previousBalance = currentBalance
            .subtract(purchases != null ? purchases : BigDecimal.ZERO)
            .subtract(interest != null ? interest : BigDecimal.ZERO)
            .subtract(fees != null ? fees : BigDecimal.ZERO)
            .add(payments != null ? payments : BigDecimal.ZERO);
        
        // Ensure proper monetary scale (2 decimal places)
        return previousBalance.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}