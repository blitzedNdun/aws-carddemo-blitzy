/*
 * AccountService.java
 * 
 * Spring Boot service class for account management operations.
 * Implements business logic from COBOL programs COACTVWC.cbl and COACTUPC.cbl
 * for account viewing and updating transactions (CAVW/CAUP).
 * 
 * Key functionality:
 * - Account view operations combining account and customer data
 * - Account update operations with comprehensive validation
 * - Optimistic locking for concurrent update protection
 * - BigDecimal precision matching COBOL COMP-3 behavior
 * - Exception handling matching COBOL ABEND routines
 * 
 * Business Logic Flow (matching COBOL programs):
 * CAVW (View): Read CARDXREF -> Read ACCTDAT -> Read CUSTDAT -> Combine data
 * CAUP (Update): Validate input -> Read with UPDATE lock -> Apply changes -> Commit
 */

package com.carddemo.service;

import com.carddemo.dto.AccountDto;
import com.carddemo.dto.AccountUpdateRequest;
import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.exception.ConcurrencyException;
import com.carddemo.util.CobolDataConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.dao.OptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Account Service
 * 
 * Provides comprehensive account management functionality including view and
 * update operations. Implements exact business logic from COBOL programs
 * COACTVWC.cbl (account view) and COACTUPC.cbl (account update) while
 * maintaining Spring Boot service patterns and transaction management.
 * 
 * Key features:
 * - Account and customer data retrieval and combination
 * - Comprehensive field validation matching COBOL edit routines
 * - Optimistic locking for concurrent update protection
 * - Audit logging for all account operations
 * - Exception handling with detailed error messages
 * - BigDecimal precision preservation for monetary calculations
 */
@Service
@Transactional
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * View Account Operation (CAVW Transaction)
     * 
     * Retrieves comprehensive account details including customer information.
     * Replicates COACTVWC.cbl functionality with identical data access patterns.
     * 
     * Business Logic Flow (matching COACTVWC.cbl):
     * 1. Validate account ID format (handled by controller)
     * 2. Read account data from ACCTDAT equivalent (account_data table)
     * 3. Read customer data from CUSTDAT equivalent (customer_data table)
     * 4. Combine account and customer information into response DTO
     * 5. Calculate derived fields (available credit, status flags)
     * 
     * @param accountId 11-digit account identifier
     * @return AccountDto with complete account and customer details
     * @throws ResourceNotFoundException when account or customer not found
     */
    @Transactional(readOnly = true)
    public AccountDto viewAccount(Long accountId) {
        logger.info("Processing account view for account ID: {}", accountId);
        
        try {
            // Convert Long to String for entity lookup (matching COBOL PIC 9(11))
            String accountIdStr = String.format("%011d", accountId);
            
            // Read account data (replicates READ ACCTDAT from COACTVWC.cbl)
            Optional<Account> accountOpt = accountRepository.findById(accountIdStr);
            if (accountOpt.isEmpty()) {
                logger.warn("Account not found: {}", accountIdStr);
                throw new ResourceNotFoundException("Account", accountIdStr);
            }
            
            Account account = accountOpt.get();
            logger.debug("Retrieved account data for account ID: {}", accountIdStr);
            
            // Read customer data (replicates READ CUSTDAT from COACTVWC.cbl)
            // Convert String customerId to Long for CustomerRepository.findById()
            Long customerIdLong = Long.parseLong(account.getCustomerId());
            Optional<Customer> customerOpt = customerRepository.findById(customerIdLong);
            if (customerOpt.isEmpty()) {
                logger.warn("Customer not found for account {}: {}", accountIdStr, account.getCustomerId());
                throw new ResourceNotFoundException("Customer", account.getCustomerId());
            }
            
            Customer customer = customerOpt.get();
            logger.debug("Retrieved customer data for customer ID: {}", customer.getCustomerId());
            
            // Create AccountDto combining account and customer data
            AccountDto accountDto = createAccountDto(account, customer);
            
            logger.info("Successfully processed account view for account ID: {}", accountIdStr);
            return accountDto;
            
        } catch (Exception e) {
            logger.error("Error processing account view for account ID {}: {}", accountId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update Account Operation (CAUP Transaction)
     * 
     * Updates modifiable account fields with comprehensive validation.
     * Replicates COACTUPC.cbl functionality with identical validation logic.
     * 
     * Business Logic Flow (matching COACTUPC.cbl):
     * 1. Validate account ID and update request fields
     * 2. Perform READ with UPDATE lock on account record
     * 3. Apply comprehensive field validation (active status, limits, dates)
     * 4. Detect changes and update only modified fields
     * 5. Commit transaction with optimistic locking
     * 6. Return updated account information
     * 
     * @param accountId 11-digit account identifier
     * @param updateRequest AccountUpdateRequest with validated field updates
     * @return Updated AccountDto with current field values
     * @throws ResourceNotFoundException when account not found
     * @throws ValidationException when field validation fails
     * @throws ConcurrencyException when optimistic locking fails
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AccountDto updateAccount(Long accountId, AccountUpdateRequest updateRequest) {
        logger.info("Processing account update for account ID: {}", accountId);
        
        try {
            // Convert Long to String for entity lookup
            String accountIdStr = String.format("%011d", accountId);
            
            // Validate account ID consistency
            if (!accountIdStr.equals(updateRequest.getAccountId())) {
                throw new ValidationException("Account ID mismatch: URL parameter and request body must match");
            }
            
            // Read account with pessimistic lock (replicates READ FOR UPDATE from COACTUPC.cbl)
            Optional<Account> accountOpt = accountRepository.findByIdForUpdate(accountIdStr);
            if (accountOpt.isEmpty()) {
                logger.warn("Account not found for update: {}", accountIdStr);
                throw new ResourceNotFoundException("Account", accountIdStr);
            }
            
            Account account = accountOpt.get();
            logger.debug("Retrieved account for update: {}", accountIdStr);
            
            // Apply comprehensive validation (replicates COBOL edit routines)
            validateUpdateRequest(account, updateRequest);
            
            // Apply updates only to modified fields
            boolean hasChanges = applyAccountUpdates(account, updateRequest);
            
            if (!hasChanges) {
                logger.info("No changes detected for account update: {}", accountIdStr);
                // Still return current account data
                return viewAccount(accountId);
            }
            
            // Save changes with optimistic locking
            try {
                Account updatedAccount = accountRepository.save(account);
                logger.info("Successfully updated account: {}", accountIdStr);
                
                // Return updated account data
                return viewAccount(accountId);
                
            } catch (OptimisticLockingFailureException e) {
                logger.warn("Optimistic locking failure for account {}: {}", accountIdStr, e.getMessage());
                throw new ConcurrencyException("Account", accountIdStr, "Account was modified by another user. Please refresh and try again.");
            }
            
        } catch (ResourceNotFoundException | ValidationException | ConcurrencyException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (Exception e) {
            logger.error("Error processing account update for account ID {}: {}", accountId, e.getMessage(), e);
            throw new RuntimeException("Unexpected error during account update", e);
        }
    }

    /**
     * Create AccountDto from Account and Customer entities.
     * Combines data and calculates derived fields.
     */
    private AccountDto createAccountDto(Account account, Customer customer) {
        AccountDto dto = new AccountDto();
        
        // Account data mapping
        dto.setAccountId(account.getAccountId());
        dto.setActiveStatus(account.getActiveStatus());
        dto.setCurrentBalance(account.getCurrentBalance());
        dto.setCreditLimit(account.getCreditLimit());
        dto.setCashCreditLimit(account.getCashCreditLimit());
        dto.setOpenDate(account.getOpenDate());
        dto.setExpirationDate(account.getExpirationDate());
        dto.setReissueDate(account.getReissueDate());
        dto.setCurrentCycleCredit(account.getCurrentCycleCredit());
        dto.setCurrentCycleDebit(account.getCurrentCycleDebit());
        dto.setAddressZip(account.getAddressZip());
        dto.setAccountGroupId(account.getAccountGroupId());
        
        // Customer data mapping
        dto.setCustomerId(customer.getCustomerId() != null ? customer.getCustomerId().toString() : null);
        dto.setCustomerFirstName(customer.getFirstName());
        dto.setCustomerMiddleName(customer.getMiddleName());
        dto.setCustomerLastName(customer.getLastName());
        dto.setCustomerAddressLine1(customer.getAddressLine1());
        dto.setCustomerAddressLine2(customer.getAddressLine2());
        dto.setCustomerAddressLine3(customer.getAddressLine3());
        dto.setCustomerStateCode(customer.getStateCode());
        dto.setCustomerCountryCode(customer.getCountryCode());
        dto.setCustomerZipCode(customer.getZipCode());
        dto.setCustomerPhoneNumber1(customer.getPhoneNumber1());
        dto.setCustomerPhoneNumber2(customer.getPhoneNumber2());
        dto.setCustomerDateOfBirth(customer.getDateOfBirth());
        dto.setCustomerEftAccountId(customer.getEftAccountId());
        dto.setCustomerFicoScore(customer.getFicoScore());
        
        // Calculate derived fields
        dto.calculateDerivedFields();
        
        return dto;
    }

    /**
     * Validate account update request.
     * Replicates COBOL field validation routines from COACTUPC.cbl.
     */
    private void validateUpdateRequest(Account account, AccountUpdateRequest updateRequest) {
        // Active status validation
        if (updateRequest.getActiveStatus() != null) {
            String activeStatus = updateRequest.getActiveStatus();
            if (!"Y".equals(activeStatus) && !"N".equals(activeStatus)) {
                throw new ValidationException("Active status must be 'Y' or 'N'");
            }
        }
        
        // Credit limit validation
        if (updateRequest.getCreditLimit() != null) {
            BigDecimal creditLimit = updateRequest.getCreditLimit();
            if (creditLimit.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Credit limit cannot be negative");
            }
            if (creditLimit.compareTo(new BigDecimal("9999999999.99")) > 0) {
                throw new ValidationException("Credit limit cannot exceed 9,999,999,999.99");
            }
            // Ensure proper scale for COBOL COMP-3 compatibility
            if (creditLimit.scale() > 2) {
                throw new ValidationException("Credit limit cannot have more than 2 decimal places");
            }
        }
        
        // Cash credit limit validation
        if (updateRequest.getCashCreditLimit() != null) {
            BigDecimal cashCreditLimit = updateRequest.getCashCreditLimit();
            if (cashCreditLimit.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Cash credit limit cannot be negative");
            }
            if (cashCreditLimit.compareTo(new BigDecimal("9999999999.99")) > 0) {
                throw new ValidationException("Cash credit limit cannot exceed 9,999,999,999.99");
            }
            // Ensure proper scale for COBOL COMP-3 compatibility
            if (cashCreditLimit.scale() > 2) {
                throw new ValidationException("Cash credit limit cannot have more than 2 decimal places");
            }
            // Cash credit limit should not exceed regular credit limit
            BigDecimal regularLimit = updateRequest.getCreditLimit() != null ? 
                updateRequest.getCreditLimit() : account.getCreditLimit();
            if (cashCreditLimit.compareTo(regularLimit) > 0) {
                throw new ValidationException("Cash credit limit cannot exceed regular credit limit");
            }
        }
        
        // Expiration date validation
        if (updateRequest.getExpirationDate() != null) {
            LocalDate expirationDate = updateRequest.getExpirationDate();
            if (expirationDate.isBefore(LocalDate.now())) {
                throw new ValidationException("Expiration date cannot be in the past");
            }
            if (expirationDate.isAfter(LocalDate.now().plusYears(10))) {
                throw new ValidationException("Expiration date cannot be more than 10 years in the future");
            }
        }
        
        // Business logic validations
        if ("N".equals(updateRequest.getActiveStatus()) && account.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new ValidationException("Cannot deactivate account with outstanding balance");
        }
    }

    /**
     * Apply account updates from request to entity.
     * Only updates modified fields to preserve optimistic locking.
     * Returns true if any changes were applied.
     */
    private boolean applyAccountUpdates(Account account, AccountUpdateRequest updateRequest) {
        boolean hasChanges = false;
        
        // Update active status if changed
        if (updateRequest.getActiveStatus() != null && 
            !updateRequest.getActiveStatus().equals(account.getActiveStatus())) {
            account.setActiveStatus(updateRequest.getActiveStatus());
            hasChanges = true;
            logger.debug("Updated active status to: {}", updateRequest.getActiveStatus());
        }
        
        // Update credit limit if changed
        if (updateRequest.getCreditLimit() != null) {
            BigDecimal newCreditLimit = updateRequest.getCreditLimit().setScale(2);
            if (newCreditLimit.compareTo(account.getCreditLimit()) != 0) {
                account.setCreditLimit(newCreditLimit);
                hasChanges = true;
                logger.debug("Updated credit limit to: {}", newCreditLimit);
            }
        }
        
        // Update cash credit limit if changed
        if (updateRequest.getCashCreditLimit() != null) {
            BigDecimal newCashCreditLimit = updateRequest.getCashCreditLimit().setScale(2);
            if (newCashCreditLimit.compareTo(account.getCashCreditLimit()) != 0) {
                account.setCashCreditLimit(newCashCreditLimit);
                hasChanges = true;
                logger.debug("Updated cash credit limit to: {}", newCashCreditLimit);
            }
        }
        
        // Update expiration date if changed
        if (updateRequest.getExpirationDate() != null && 
            !updateRequest.getExpirationDate().equals(account.getExpirationDate())) {
            account.setExpirationDate(updateRequest.getExpirationDate());
            hasChanges = true;
            logger.debug("Updated expiration date to: {}", updateRequest.getExpirationDate());
        }
        
        return hasChanges;
    }
}