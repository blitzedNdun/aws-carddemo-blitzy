package com.carddemo.controller;

import com.carddemo.dto.AccountDto;
import com.carddemo.dto.AccountUpdateRequest;
import com.carddemo.service.AccountService;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.exception.ConcurrencyException;
import com.carddemo.util.CobolDataConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;

/**
 * REST Controller for Account Operations (CAVW/CAUP transactions)
 * 
 * Replaces COBOL programs:
 * - COACTVWC.cbl -> GET /api/accounts/{id} (CAVW transaction)
 * - COACTUPC.cbl -> PUT /api/accounts/{id} (CAUP transaction)
 * 
 * Maintains identical business logic and validation rules from original COBOL programs
 * while providing modern REST API access patterns. All monetary fields use BigDecimal
 * with scale(2) precision to match COBOL COMP-3 packed decimal behavior.
 * 
 * Transaction Codes:
 * - CAVW: Account View (GET endpoint)
 * - CAUP: Account Update (PUT endpoint)
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private AccountService accountService;

    /**
     * Account View Endpoint (CAVW Transaction)
     * 
     * Retrieves comprehensive account details including customer information.
     * Maps to COACTVWC.cbl functionality with identical validation logic.
     * 
     * @param id 11-digit account identifier (must be non-zero numeric)
     * @return AccountDto with complete account and customer details
     * @throws ResourceNotFoundException when account not found in account master file
     * 
     * Business Logic Flow (matching COACTVWC.cbl):
     * 1. Validate account ID format (11 digits, non-zero)
     * 2. Read CARDXREF by account ID to get customer ID
     * 3. Read ACCTDAT by account ID for account details  
     * 4. Read CUSTDAT by customer ID for customer details
     * 5. Return combined account and customer information
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable String id) {
        logger.info("Processing account view request for account ID: {}", id);
        
        try {
            // Input validation matching COBOL 2210-EDIT-ACCOUNT logic
            validateAccountId(id);
            
            // Parse account ID to Long for service layer processing (trim whitespace)
            Long accountId = Long.parseLong(id.trim());
            
            // Call service layer - replicates 9000-READ-ACCT section logic
            AccountDto account = accountService.viewAccount(accountId);
            
            logger.info("Successfully retrieved account details for account ID: {}", id);
            return ResponseEntity.ok(account);
            
        } catch (ResourceNotFoundException e) {
            logger.warn("Account not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (ValidationException e) {
            logger.warn("Validation error for account ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("Unexpected error processing account view for ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Account Update Endpoint (CAUP Transaction)
     * 
     * Updates modifiable account fields with comprehensive validation.
     * Maps to COACTUPC.cbl functionality with identical field validation logic.
     * 
     * @param id 11-digit account identifier
     * @param updateRequest AccountUpdateRequest with validated field updates
     * @return Updated AccountDto with current field values
     * @throws ResourceNotFoundException when account not found
     * @throws ValidationException when field validation fails
     * @throws ConcurrencyException when optimistic locking fails
     * 
     * Business Logic Flow (matching COACTUPC.cbl):
     * 1. Validate account ID format and update request fields
     * 2. Perform READ with UPDATE lock on account record
     * 3. Apply comprehensive field validation (active status, limits, dates)
     * 4. Detect changes and update only modified fields
     * 5. Commit transaction with optimistic locking
     * 6. Return updated account information
     */
    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> updateAccount(
            @PathVariable String id,
            @Valid @RequestBody AccountUpdateRequest updateRequest) {
        
        logger.info("Processing account update request for account ID: {}", id);
        
        try {
            // Input validation matching COBOL edit routines
            validateAccountId(id);
            validateUpdateRequest(updateRequest, id);
            
            // Parse account ID for service processing (trim whitespace)
            Long accountId = Long.parseLong(id.trim());
            
            // Call service layer - replicates account update logic from COACTUPC.cbl
            AccountDto updatedAccount = accountService.updateAccount(accountId, updateRequest);
            
            logger.info("Successfully updated account ID: {}", id);
            return ResponseEntity.ok(updatedAccount);
            
        } catch (ResourceNotFoundException e) {
            logger.warn("Account not found for update: {}", e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (ValidationException e) {
            logger.warn("Validation error for account update {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (ConcurrencyException e) {
            logger.warn("Concurrency conflict updating account {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
            
        } catch (Exception e) {
            logger.error("Unexpected error updating account {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Account ID Validation
     * 
     * Replicates COBOL 2210-EDIT-ACCOUNT validation logic from COACTVWC.cbl
     * and COACTUPC.cbl input validation routines.
     * 
     * Validation Rules:
     * - Must be exactly 11 digits
     * - Must be numeric
     * - Must not be all zeros
     * - Must not be null or empty
     * 
     * @param accountId Account ID string to validate
     * @throws ValidationException when validation fails
     */
    private void validateAccountId(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new ValidationException("Account ID is required");
        }
        
        // Remove any leading/trailing whitespace for validation
        String trimmedAccountId = accountId.trim();
        
        // Must be exactly 11 digits (matching COBOL PIC 9(11))
        if (trimmedAccountId.length() != 11) {
            throw new ValidationException("Account ID must be exactly 11 digits");
        }
        
        // Must be numeric
        if (!trimmedAccountId.matches("\\d{11}")) {
            throw new ValidationException("Account ID must contain only digits");
        }
        
        // Must not be all zeros (matching COBOL validation)
        if (trimmedAccountId.equals("00000000000")) {
            throw new ValidationException("Account ID cannot be all zeros");
        }
    }

    /**
     * Update Request Validation
     * 
     * Validates the account update request to ensure account ID consistency
     * and basic field validation before passing to service layer.
     * 
     * @param updateRequest The update request to validate
     * @param pathAccountId Account ID from URL path
     * @throws ValidationException when validation fails
     */
    private void validateUpdateRequest(AccountUpdateRequest updateRequest, String pathAccountId) {
        if (updateRequest == null) {
            throw new ValidationException("Update request body is required");
        }
        
        // Ensure account ID in request matches path parameter (trim whitespace)
        if (updateRequest.getAccountId() != null) {
            String requestAccountId = updateRequest.getAccountId().toString();
            if (!pathAccountId.trim().equals(requestAccountId)) {
                throw new ValidationException("Account ID in request body must match path parameter");
            }
        }
        
        // Additional field-level validations will be handled by service layer
        // to maintain separation of concerns and replicates COBOL validation structure
    }
}