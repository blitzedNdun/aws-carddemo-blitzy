package com.carddemo.controller;

import com.carddemo.dto.DailyTransactionDto;
import com.carddemo.service.DailyTransactionService;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.util.ValidationUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST controller for daily transaction processing and reporting.
 * Translated from COBOL batch program CBTRN03C.cbl which handled daily transaction
 * detail report generation, date-range processing, and end-of-day aggregation.
 * 
 * Provides endpoints for:
 * - Daily transaction summaries
 * - Date-specific transaction queries
 * - End-of-day processing triggers
 * - Date range reporting functionality
 * 
 * Maintains functional parity with the original COBOL batch processing logic
 * while providing modern REST API access patterns.
 */
@RestController
@RequestMapping("/api/daily-transactions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DailyTransactionController {

    private static final Logger logger = LoggerFactory.getLogger(DailyTransactionController.class);
    
    private final DailyTransactionService dailyTransactionService;
    
    @Autowired
    public DailyTransactionController(DailyTransactionService dailyTransactionService) {
        this.dailyTransactionService = dailyTransactionService;
    }

    /**
     * Get daily transaction summary for the current date.
     * Equivalent to running CBTRN03C batch program for today's date.
     * 
     * @return ResponseEntity containing list of current day's transactions
     */
    @GetMapping
    public ResponseEntity<List<DailyTransactionDto>> getDailyTransactionSummary() {
        logger.info("Received request for current day transaction summary");
        
        try {
            LocalDate currentDate = LocalDate.now();
            logger.debug("Processing daily transaction summary for date: {}", currentDate);
            
            List<DailyTransactionDto> transactions = dailyTransactionService.getDailyTransactionSummary();
            
            if (transactions.isEmpty()) {
                logger.warn("No transactions found for current date: {}", currentDate);
                throw new ResourceNotFoundException("No transactions found for current date: " + currentDate);
            }
            
            logger.info("Successfully retrieved {} transactions for current date", transactions.size());
            
            // Log transaction details for audit trail (similar to COBOL DISPLAY statements)
            for (DailyTransactionDto transaction : transactions) {
                logger.debug("Transaction ID: {}, Amount: {}, Card: {}, Timestamp: {}", 
                    transaction.getTransactionId(), 
                    transaction.getAmount(), 
                    transaction.getCardNumber(),
                    transaction.getProcTimestamp());
            }
            
            return ResponseEntity.ok(transactions);
            
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error retrieving daily transaction summary: {}", e.getMessage(), e);
            throw new RuntimeException("Error retrieving daily transaction summary", e);
        }
    }

    /**
     * Get daily transactions for a specific date.
     * Equivalent to running CBTRN03C batch program with specific date parameters.
     * 
     * @param date The specific date in YYYY-MM-DD format
     * @return ResponseEntity containing list of transactions for the specified date
     */
    @GetMapping("/{date}")
    public ResponseEntity<List<DailyTransactionDto>> getDailyTransactionsByDate(
            @PathVariable("date") 
            @DateTimeFormat(pattern = "yyyy-MM-dd") String dateStr) {
        
        logger.info("Received request for daily transactions by date: {}", dateStr);
        
        try {
            // Validate date format (equivalent to COBOL date validation)
            ValidationUtil.validateDateFormat(dateStr);
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            
            logger.debug("Processing daily transactions for date: {}", date);
            
            List<DailyTransactionDto> transactions = dailyTransactionService.getDailyTransactionsByDate(date);
            
            if (transactions.isEmpty()) {
                logger.warn("No transactions found for date: {}", date);
                throw new ResourceNotFoundException("No transactions found for date: " + date);
            }
            
            logger.info("Successfully retrieved {} transactions for date: {}", transactions.size(), date);
            
            // Log transaction summary (similar to COBOL totaling logic)
            logTransactionSummary(transactions, date);
            
            return ResponseEntity.ok(transactions);
            
        } catch (ValidationException e) {
            logger.error("Validation error for date {}: {}", dateStr, e.getMessage());
            throw e;
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found for date {}: {}", dateStr, e.getMessage());
            throw e;
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format: {}", dateStr);
            ValidationException validationException = new ValidationException("Invalid date format");
            validationException.addFieldError("date", "Invalid date format. Expected format: YYYY-MM-DD");
            throw validationException;
        } catch (Exception e) {
            logger.error("Error retrieving transactions for date {}: {}", dateStr, e.getMessage(), e);
            throw new RuntimeException("Error retrieving transactions for date: " + dateStr, e);
        }
    }

    /**
     * Process daily transactions for end-of-day operations.
     * Equivalent to executing the complete CBTRN03C batch program with report generation.
     * 
     * @return ResponseEntity indicating processing status
     */
    @PostMapping("/process")
    public ResponseEntity<String> processDailyTransactions() {
        logger.info("Received request to process daily transactions (end-of-day processing)");
        
        try {
            LocalDate currentDate = LocalDate.now();
            logger.debug("Starting end-of-day processing for date: {}", currentDate);
            
            String result = dailyTransactionService.processDailyTransactions();
            
            logger.info("Successfully completed end-of-day processing: {}", result);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error during end-of-day processing: {}", e.getMessage(), e);
            throw new RuntimeException("Error during end-of-day processing", e);
        }
    }

    /**
     * Get daily transactions for a date range.
     * Equivalent to running CBTRN03C batch program with date range parameters
     * (WS-START-DATE and WS-END-DATE from the COBOL program).
     * 
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return ResponseEntity containing list of transactions within the date range
     */
    @GetMapping("/range")
    public ResponseEntity<List<DailyTransactionDto>> getDailyTransactionsByDateRange(
            @RequestParam("startDate") 
            @DateTimeFormat(pattern = "yyyy-MM-dd") String startDateStr,
            @RequestParam("endDate") 
            @DateTimeFormat(pattern = "yyyy-MM-dd") String endDateStr) {
        
        logger.info("Received request for daily transactions by date range: {} to {}", startDateStr, endDateStr);
        
        try {
            // Validate required fields
            ValidationUtil.validateRequiredField(startDateStr, "startDate");
            ValidationUtil.validateRequiredField(endDateStr, "endDate");
            
            // Validate date formats
            ValidationUtil.validateDateFormat(startDateStr);
            ValidationUtil.validateDateFormat(endDateStr);
            
            LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate endDate = LocalDate.parse(endDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            
            // Validate date range (equivalent to COBOL date range validation)
            ValidationUtil.validateDateRange(startDate, endDate);
            
            logger.debug("Processing daily transactions for date range: {} to {}", startDate, endDate);
            
            List<DailyTransactionDto> transactions = dailyTransactionService.getDailyTransactionsByDateRange(startDate, endDate);
            
            if (transactions.isEmpty()) {
                logger.warn("No transactions found for date range: {} to {}", startDate, endDate);
                throw new ResourceNotFoundException("No transactions found for date range: " + startDate + " to " + endDate);
            }
            
            logger.info("Successfully retrieved {} transactions for date range: {} to {}", 
                transactions.size(), startDate, endDate);
            
            // Log transaction range summary (similar to COBOL grand total logic)
            logTransactionRangeSummary(transactions, startDate, endDate);
            
            return ResponseEntity.ok(transactions);
            
        } catch (ValidationException e) {
            logger.error("Validation error for date range {} to {}: {}", startDateStr, endDateStr, e.getMessage());
            throw e;
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found for date range {} to {}: {}", startDateStr, endDateStr, e.getMessage());
            throw e;
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format in range: {} to {}", startDateStr, endDateStr);
            ValidationException validationException = new ValidationException("Invalid date format");
            if (!isValidDateFormat(startDateStr)) {
                validationException.addFieldError("startDate", "Invalid start date format. Expected format: YYYY-MM-DD");
            }
            if (!isValidDateFormat(endDateStr)) {
                validationException.addFieldError("endDate", "Invalid end date format. Expected format: YYYY-MM-DD");
            }
            throw validationException;
        } catch (Exception e) {
            logger.error("Error retrieving transactions for date range {} to {}: {}", 
                startDateStr, endDateStr, e.getMessage(), e);
            throw new RuntimeException("Error retrieving transactions for date range: " + startDateStr + " to " + endDateStr, e);
        }
    }

    /**
     * Log transaction summary for audit trail.
     * Equivalent to the COBOL totaling and reporting logic in CBTRN03C.
     * 
     * @param transactions List of transactions to summarize
     * @param date The date being processed
     */
    private void logTransactionSummary(List<DailyTransactionDto> transactions, LocalDate date) {
        if (transactions.isEmpty()) {
            return;
        }
        
        // Calculate totals (equivalent to COBOL WS-PAGE-TOTAL, WS-ACCOUNT-TOTAL logic)
        int transactionCount = transactions.size();
        
        // Group transactions by type and category for summary (similar to COBOL processing)
        long uniqueCards = transactions.stream()
            .map(DailyTransactionDto::getCardNumber)
            .distinct()
            .count();
        
        long uniqueTypes = transactions.stream()
            .map(DailyTransactionDto::getTypeCode)
            .distinct()
            .count();
        
        long uniqueCategories = transactions.stream()
            .map(DailyTransactionDto::getCategoryCode)
            .distinct()
            .count();
        
        logger.info("Transaction Summary for {}: {} transactions, {} unique cards, {} types, {} categories",
            date, transactionCount, uniqueCards, uniqueTypes, uniqueCategories);
    }

    /**
     * Log transaction range summary for audit trail.
     * Equivalent to the COBOL grand total logic in CBTRN03C.
     * 
     * @param transactions List of transactions to summarize
     * @param startDate Start date of the range
     * @param endDate End date of the range
     */
    private void logTransactionRangeSummary(List<DailyTransactionDto> transactions, LocalDate startDate, LocalDate endDate) {
        if (transactions.isEmpty()) {
            return;
        }
        
        // Calculate range totals (equivalent to COBOL WS-GRAND-TOTAL logic)
        int totalTransactions = transactions.size();
        
        // Group by date for daily breakdown
        long uniqueDates = transactions.stream()
            .map(DailyTransactionDto::getProcTimestamp)
            .map(timestamp -> timestamp.substring(0, 10)) // Extract date portion
            .distinct()
            .count();
        
        long uniqueCards = transactions.stream()
            .map(DailyTransactionDto::getCardNumber)
            .distinct()
            .count();
        
        logger.info("Transaction Range Summary from {} to {}: {} transactions across {} days, {} unique cards",
            startDate, endDate, totalTransactions, uniqueDates, uniqueCards);
    }

    /**
     * Validate date format helper method.
     * 
     * @param dateStr Date string to validate
     * @return true if valid format, false otherwise
     */
    private boolean isValidDateFormat(String dateStr) {
        try {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}