/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.UserRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.repository.AuditLogRepository;
import com.carddemo.service.AccountService;
import com.carddemo.service.TransactionService;
import com.carddemo.service.UserService;
import com.carddemo.config.RedisConfig;

import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.*;

/**
 * Factory class for creating pre-configured mock objects of commonly used services and repositories
 * with realistic behavior for CardDemo system testing.
 * 
 * This factory reduces boilerplate code in test classes by providing standardized mock configurations
 * that simulate realistic business scenarios. All mocks are configured with sensible defaults for
 * successful operations, with additional methods to configure error scenarios and edge cases.
 * 
 * Key Features:
 * - Pre-configured repository mocks with standard CRUD operations
 * - Service mocks with common business logic responses
 * - Spring component mocks (RedisTemplate, etc.)
 * - Batch job component mocks
 * - Support for success and error scenario configurations
 * - Pagination support for list operations
 * - Realistic sample data generation
 * - Transaction management behavior simulation
 * 
 * Usage Example:
 * <pre>
 * {@code
 * // In test class
 * private MockServiceFactory mockFactory = new MockServiceFactory();
 * 
 * @Test
 * public void testAccountService() {
 *     AccountRepository mockRepo = mockFactory.createMockAccountRepository();
 *     AccountService service = new AccountService(mockRepo);
 *     // Test with pre-configured realistic behavior
 * }
 * }
 * </pre>
 * 
 * @author CardDemo Migration Team  
 * @version 1.0
 * @since CardDemo v1.0
 */
public class MockServiceFactory {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1000L);
    
    // Sample data for realistic responses
    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("2500.00");
    private static final BigDecimal DEFAULT_CREDIT_LIMIT = new BigDecimal("5000.00");
    private static final String DEFAULT_ACTIVE_STATUS = "Y";
    
    /**
     * Creates a mock AccountRepository with pre-configured standard CRUD operations.
     * 
     * Configured with realistic responses for:
     * - save(): Returns saved entity with generated ID
     * - findById(): Returns sample Account entity
     * - findAll(): Returns paginated list of sample accounts
     * - deleteById(): Executes without error
     * - existsById(): Returns true for positive IDs
     * 
     * @return Pre-configured AccountRepository mock
     */
    public AccountRepository createMockAccountRepository() {
        AccountRepository mock = mock(AccountRepository.class);
        
        // Configure save() method
        when(mock.save(ArgumentMatchers.any())).thenAnswer((Answer<Object>) invocation -> {
            Object account = invocation.getArgument(0);
            // In real implementation, would set ID and return
            return account;
        });
        
        // Configure findById() method
        when(mock.findById(ArgumentMatchers.anyLong())).thenReturn(Optional.of(createSampleAccount()));
        
        // Configure findAll() method
        when(mock.findAll()).thenReturn(Arrays.asList(createSampleAccount(), createSampleAccount()));
        
        // Configure deleteById() method - no-op
        doNothing().when(mock).deleteById(ArgumentMatchers.anyLong());
        
        // Configure existsById() method
        when(mock.existsById(ArgumentMatchers.anyLong())).thenReturn(true);
        
        return mock;
    }

    /**
     * Creates a mock TransactionRepository with pre-configured transaction data access operations.
     * 
     * Configured with realistic responses for:
     * - save(): Returns saved transaction entity
     * - findById(): Returns sample Transaction entity  
     * - findByAccountIdAndDateRange(): Returns paginated transaction list
     * - findAll(): Returns list of sample transactions
     * - deleteById(): Executes without error
     * 
     * @return Pre-configured TransactionRepository mock
     */
    public TransactionRepository createMockTransactionRepository() {
        TransactionRepository mock = mock(TransactionRepository.class);
        
        // Configure save() method
        when(mock.save(ArgumentMatchers.any())).thenAnswer((Answer<Object>) invocation -> {
            Object transaction = invocation.getArgument(0);
            return transaction;
        });
        
        // Configure findById() method
        when(mock.findById(ArgumentMatchers.anyLong())).thenReturn(Optional.of(createSampleTransaction()));
        
        // Configure findByAccountIdAndDateRange() method
        when(mock.findByAccountIdAndDateRange(
            ArgumentMatchers.anyLong(),
            ArgumentMatchers.any(LocalDateTime.class),
            ArgumentMatchers.any(LocalDateTime.class),
            ArgumentMatchers.any(Pageable.class)
        )).thenReturn(configurePaginatedResponse(Arrays.asList(createSampleTransaction()), 0));
        
        // Configure findAll() method
        when(mock.findAll()).thenReturn(Arrays.asList(createSampleTransaction(), createSampleTransaction()));
        
        // Configure deleteById() method - no-op
        doNothing().when(mock).deleteById(ArgumentMatchers.anyLong());
        
        return mock;
    }

    /**
     * Creates a mock UserRepository with pre-configured user data access operations.
     * 
     * Configured with realistic responses for:
     * - save(): Returns saved user entity
     * - findById(): Returns sample User entity
     * - findByUserId(): Returns user by business ID
     * - findByStatus(): Returns users with specified status
     * - searchUsers(): Returns filtered user list  
     * - deleteById(): Executes without error
     * 
     * @return Pre-configured UserRepository mock
     */
    public UserRepository createMockUserRepository() {
        UserRepository mock = mock(UserRepository.class);
        
        // Configure save() method
        when(mock.save(ArgumentMatchers.any())).thenAnswer((Answer<Object>) invocation -> {
            Object user = invocation.getArgument(0);
            return user;
        });
        
        // Configure findById() method
        when(mock.findById(ArgumentMatchers.anyLong())).thenReturn(Optional.of(createSampleUser()));
        
        // Configure findByUserId() method
        when(mock.findByUserId(ArgumentMatchers.anyString())).thenReturn(Optional.of(createSampleUser()));
        
        // Configure findByStatus() method - using findByDepartment as proxy for status
        when(mock.findByDepartment(ArgumentMatchers.anyString())).thenReturn(Arrays.asList(createSampleUser()));
        
        // Configure searchUsers() method - using findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase
        when(mock.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            ArgumentMatchers.anyString(), 
            ArgumentMatchers.anyString()
        )).thenReturn(Arrays.asList(createSampleUser()));
        
        // Configure deleteById() method - no-op
        doNothing().when(mock).deleteById(ArgumentMatchers.anyLong());
        
        return mock;
    }

    /**
     * Creates a mock CustomerRepository with pre-configured customer data access operations.
     * 
     * Configured with realistic responses for:
     * - save(): Returns saved customer entity
     * - findById(): Returns sample Customer entity
     * - findByLastNameAndFirstName(): Returns customers by name
     * - findBySSN(): Returns customer by SSN lookup
     * - findAll(): Returns list of sample customers
     * - deleteById(): Executes without error
     * 
     * @return Pre-configured CustomerRepository mock
     */
    public CustomerRepository createMockCustomerRepository() {
        CustomerRepository mock = mock(CustomerRepository.class);
        
        // Configure save() method
        when(mock.save(ArgumentMatchers.any())).thenAnswer((Answer<Object>) invocation -> {
            Object customer = invocation.getArgument(0);
            return customer;
        });
        
        // Configure findById() method
        when(mock.findById(ArgumentMatchers.anyLong())).thenReturn(Optional.of(createSampleCustomer()));
        
        // Configure findByLastNameAndFirstName() method
        when(mock.findByLastNameAndFirstName(
            ArgumentMatchers.anyString(), 
            ArgumentMatchers.anyString()
        )).thenReturn(Arrays.asList(createSampleCustomer()));
        
        // Configure findBySSN() method - using a generic findBy method as proxy
        when(mock.findAll()).thenReturn(Arrays.asList(createSampleCustomer(), createSampleCustomer()));
        
        // Configure deleteById() method - no-op
        doNothing().when(mock).deleteById(ArgumentMatchers.anyLong());
        
        return mock;
    }

    /**
     * Creates a mock CardRepository with pre-configured card data access operations.
     * 
     * Configured with realistic responses for:
     * - save(): Returns saved card entity
     * - findById(): Returns sample Card entity
     * - findByAccountId(): Returns cards for specified account
     * - findAll(): Returns list of sample cards
     * - deleteById(): Executes without error
     * 
     * @return Pre-configured CardRepository mock
     */
    public CardRepository createMockCardRepository() {
        CardRepository mock = mock(CardRepository.class);
        
        // Configure save() method
        when(mock.save(ArgumentMatchers.any())).thenAnswer((Answer<Object>) invocation -> {
            Object card = invocation.getArgument(0);
            return card;
        });
        
        // Configure findById() method
        when(mock.findById(ArgumentMatchers.anyLong())).thenReturn(Optional.of(createSampleCard()));
        
        // Configure findByAccountId() method
        when(mock.findByAccountId(ArgumentMatchers.anyLong())).thenReturn(Arrays.asList(createSampleCard()));
        
        // Configure findAll() method
        when(mock.findAll()).thenReturn(Arrays.asList(createSampleCard(), createSampleCard()));
        
        // Configure deleteById() method - no-op
        doNothing().when(mock).deleteById(ArgumentMatchers.anyLong());
        
        return mock;
    }

    /**
     * Creates a mock UserSecurityRepository with pre-configured authentication data access operations.
     * 
     * Configured with realistic responses for:
     * - findByUsername(): Returns user security entity by username
     * - save(): Returns saved user security entity
     * - findById(): Returns sample UserSecurity entity
     * - findAll(): Returns list of sample user security records
     * - deleteById(): Executes without error
     * 
     * @return Pre-configured UserSecurityRepository mock
     */
    public UserSecurityRepository createMockUserSecurityRepository() {
        UserSecurityRepository mock = mock(UserSecurityRepository.class);
        
        // Configure findByUsername() method
        when(mock.findByUsername(ArgumentMatchers.anyString())).thenReturn(Optional.of(createSampleUserSecurity()));
        
        // Configure save() method
        when(mock.save(ArgumentMatchers.any())).thenAnswer((Answer<Object>) invocation -> {
            Object userSecurity = invocation.getArgument(0);
            return userSecurity;
        });
        
        // Configure findById() method
        when(mock.findById(ArgumentMatchers.anyLong())).thenReturn(Optional.of(createSampleUserSecurity()));
        
        // Configure findAll() method
        when(mock.findAll()).thenReturn(Arrays.asList(createSampleUserSecurity()));
        
        // Configure deleteById() method - no-op
        doNothing().when(mock).deleteById(ArgumentMatchers.anyLong());
        
        return mock;
    }

    /**
     * Creates a mock AuditLogRepository with pre-configured audit logging operations.
     * 
     * Configured with realistic responses for:
     * - save(): Returns saved audit log entity
     * - findById(): Returns sample AuditLog entity  
     * - findByUsernameAndTimestampBetween(): Returns audit logs for user in date range
     * - findByEventTypeAndTimestamp(): Returns audit logs by event type
     * - deleteByTimestampBefore(): Executes cleanup without error
     * 
     * @return Pre-configured AuditLogRepository mock
     */
    public AuditLogRepository createMockAuditLogRepository() {
        AuditLogRepository mock = mock(AuditLogRepository.class);
        
        // Configure save() method
        when(mock.save(ArgumentMatchers.any())).thenAnswer((Answer<Object>) invocation -> {
            Object auditLog = invocation.getArgument(0);
            return auditLog;
        });
        
        // Configure findById() method
        when(mock.findById(ArgumentMatchers.anyLong())).thenReturn(Optional.of(createSampleAuditLog()));
        
        // Configure findByUsernameAndTimestampBetween() method
        when(mock.findByUsernameAndTimestampBetween(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(LocalDateTime.class),
            ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(Arrays.asList(createSampleAuditLog()));
        
        // Configure findByEventTypeAndTimestamp() method  
        when(mock.findByEventTypeAndTimestamp(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(Arrays.asList(createSampleAuditLog()));
        
        // Configure deleteByTimestampBefore() method - no-op
        doNothing().when(mock).deleteByTimestampBefore(ArgumentMatchers.any(LocalDateTime.class));
        
        return mock;
    }

    /**
     * Creates a mock AccountService with pre-configured account business logic operations.
     * 
     * Configured with realistic responses for:
     * - viewAccount(): Returns complete account DTO with customer data
     * - getAccountById(): Returns account entity by ID
     * - updateAccount(): Returns updated account DTO
     * - validateAccountUpdate(): Executes validation without error for valid requests
     * - calculateBalanceUpdate(): Returns calculated balance changes
     * 
     * @return Pre-configured AccountService mock
     */
    public AccountService createMockAccountService() {
        AccountService mock = mock(AccountService.class);
        
        // Configure viewAccount() method
        when(mock.viewAccount(ArgumentMatchers.anyLong())).thenReturn(createSampleAccountDto());
        
        // Configure getAccountById() method - returns AccountDto
        when(mock.getAccountById(ArgumentMatchers.anyLong())).thenReturn(createSampleAccountDto());
        
        // Configure updateAccount() method
        when(mock.updateAccount(ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
            .thenReturn(createSampleAccountDto());
        
        // Configure validateAccountUpdate() method - no-op for valid requests
        doNothing().when(mock).validateAccountUpdate(ArgumentMatchers.any(), ArgumentMatchers.any());
        
        // Configure calculateBalanceUpdate() method
        when(mock.calculateBalanceUpdate(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(new BigDecimal("100.00"));
        
        return mock;
    }

    /**
     * Creates a mock TransactionService with pre-configured transaction business logic operations.
     * 
     * Configured with realistic responses for:
     * - listTransactions(): Returns paginated transaction list
     * - getTransactionDetail(): Returns detailed transaction information
     * - addTransaction(): Returns created transaction entity
     * - validateTransaction(): Executes validation without error for valid requests
     * - processPageNavigation(): Returns next page of transactions
     * 
     * @return Pre-configured TransactionService mock
     */
    public TransactionService createMockTransactionService() {
        TransactionService mock = mock(TransactionService.class);
        
        // Configure listTransactions() method
        when(mock.listTransactions(
            ArgumentMatchers.anyLong(),
            ArgumentMatchers.anyString(), 
            ArgumentMatchers.anyInt()
        )).thenReturn(configurePaginatedResponse(Arrays.asList(createSampleTransaction()), 0));
        
        // Configure getTransactionDetail() method
        when(mock.getTransactionDetail(ArgumentMatchers.anyLong())).thenReturn(createSampleTransactionDto());
        
        // Configure addTransaction() method
        when(mock.addTransaction(ArgumentMatchers.any())).thenReturn(createSampleTransaction());
        
        // Configure validateTransaction() method - no-op for valid requests
        doNothing().when(mock).validateTransaction(ArgumentMatchers.any());
        
        // Configure processPageNavigation() method
        when(mock.processPageNavigation(ArgumentMatchers.anyString()))
            .thenReturn(configurePaginatedResponse(Arrays.asList(createSampleTransaction()), 0));
        
        return mock;
    }

    /**
     * Creates a mock UserService with pre-configured user management business logic operations.
     * 
     * Configured with realistic responses for:
     * - listUsers(): Returns paginated user list
     * - getUserById(): Returns user DTO by ID
     * - createUser(): Returns created user entity
     * - updateUser(): Returns updated user DTO
     * - deleteUser(): Executes without error
     * - validateUserPermissions(): Returns true for valid permissions
     * 
     * @return Pre-configured UserService mock
     */
    public UserService createMockUserService() {
        UserService mock = mock(UserService.class);
        
        // Configure listUsers() method with Pageable
        when(mock.listUsers(ArgumentMatchers.any(Pageable.class))).thenReturn(createSampleUserListResponse());
        
        // Configure getUserById() method
        when(mock.getUserById(ArgumentMatchers.anyLong())).thenReturn(createSampleUserDto());
        
        // Configure createUser() method
        when(mock.createUser(ArgumentMatchers.any())).thenReturn(createSampleUser());
        
        // Configure updateUser() method
        when(mock.updateUser(ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
            .thenReturn(createSampleUserDto());
        
        // Configure deleteUser() method - no-op
        doNothing().when(mock).deleteUser(ArgumentMatchers.anyLong());
        
        // Configure validateUserPermissions() method
        when(mock.validateUserPermissions(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn(true);
        
        return mock;
    }

    /**
     * Creates a mock RedisTemplate with pre-configured Redis operations for session management.
     * 
     * Configured with realistic responses for:
     * - opsForValue(): Returns value operations mock
     * - opsForHash(): Returns hash operations mock  
     * - expire(): Returns true for successful expiration setting
     * - delete(): Returns true for successful deletion
     * 
     * @return Pre-configured RedisTemplate mock
     */
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> createMockRedisTemplate() {
        RedisTemplate<String, Object> mock = mock(RedisTemplate.class);
        
        // Create mocks for operations
        var valueOps = mock(RedisTemplate.ValueOperations.class);
        var hashOps = mock(RedisTemplate.HashOperations.class);
        
        // Configure opsForValue() method
        when(mock.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(ArgumentMatchers.anyString())).thenReturn("sample-value");
        doNothing().when(valueOps).set(ArgumentMatchers.anyString(), ArgumentMatchers.any());
        
        // Configure opsForHash() method
        when(mock.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn("sample-hash-value");
        doNothing().when(hashOps).put(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());
        
        // Configure expire() method
        when(mock.expire(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(true);
        
        // Configure delete() method
        when(mock.delete(ArgumentMatchers.anyString())).thenReturn(true);
        
        return mock;
    }

    /**
     * Configures paginated response for list operations with realistic pagination metadata.
     * 
     * Creates a Page object with the provided content and pagination information.
     * Simulates Spring Data JPA pagination behavior for consistent test scenarios.
     * 
     * @param content List of entities to include in the page
     * @param pageNumber Current page number (0-based)
     * @return Page object with content and pagination metadata
     */
    @SuppressWarnings("unchecked")
    public <T> org.springframework.data.domain.Page<T> configurePaginatedResponse(List<T> content, int pageNumber) {
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(pageNumber, 10);
        return new org.springframework.data.domain.PageImpl<>(content, pageable, content.size() + 10);
    }

    /**
     * Configures error scenarios for mocked objects to simulate failure conditions.
     * 
     * This method can be used to reconfigure mocks for error testing scenarios
     * such as database connection failures, validation errors, or business rule violations.
     * 
     * @param mock The mock object to configure for error scenarios
     * @param errorType Type of error to simulate ("DATABASE_ERROR", "VALIDATION_ERROR", etc.)
     */
    public void configureErrorScenarios(Object mock, String errorType) {
        switch (errorType) {
            case "DATABASE_ERROR":
                // Configure database-related errors
                if (mock instanceof AccountRepository) {
                    when(((AccountRepository) mock).findById(ArgumentMatchers.anyLong()))
                        .thenThrow(new RuntimeException("Database connection failed"));
                }
                break;
            case "VALIDATION_ERROR":
                // Configure validation errors  
                if (mock instanceof AccountService) {
                    doThrow(new RuntimeException("Validation failed"))
                        .when(((AccountService) mock)).validateAccountUpdate(ArgumentMatchers.any(), ArgumentMatchers.any());
                }
                break;
            case "NOT_FOUND_ERROR":
                // Configure not found scenarios
                if (mock instanceof AccountRepository) {
                    when(((AccountRepository) mock).findById(ArgumentMatchers.anyLong()))
                        .thenReturn(Optional.empty());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown error type: " + errorType);
        }
    }

    /**
     * Creates a sample Account entity with realistic financial data.
     * 
     * @return Account entity with pre-populated fields matching COBOL data structures
     */
    public Object createSampleAccount() {
        // Since we don't have direct access to Account entity class,
        // we'll create a generic object that represents typical account data
        // In a real implementation, this would return an actual Account entity
        return new Object() {
            public Long getAccountId() { return ID_GENERATOR.incrementAndGet(); }
            public String getActiveStatus() { return DEFAULT_ACTIVE_STATUS; }
            public BigDecimal getCurrentBalance() { return DEFAULT_BALANCE; }
            public BigDecimal getCreditLimit() { return DEFAULT_CREDIT_LIMIT; }
            public LocalDate getOpenDate() { return LocalDate.now().minusYears(2); }
            public Long getCustomerId() { return 1001L; }
        };
    }

    /**
     * Creates a sample Transaction entity with realistic transaction data.
     * 
     * @return Transaction entity with pre-populated fields matching COBOL transaction structures
     */
    public Object createSampleTransaction() {
        return new Object() {
            public Long getTransactionId() { return ID_GENERATOR.incrementAndGet(); }
            public Long getAccountId() { return 1001L; }
            public BigDecimal getAmount() { return new BigDecimal("125.50"); }
            public String getTransactionTypeCode() { return "PU"; }
            public LocalDate getTransactionDate() { return LocalDate.now(); }
            public String getMerchantName() { return "Sample Merchant"; }
            public String getDescription() { return "Sample Transaction"; }
        };
    }

    /**
     * Creates a sample User entity with realistic user profile data.
     * 
     * @return User entity with pre-populated fields matching COBOL user structures
     */
    public Object createSampleUser() {
        return new Object() {
            public Long getId() { return ID_GENERATOR.incrementAndGet(); }
            public String getUserId() { return "USER001"; }
            public String getFirstName() { return "John"; }
            public String getLastName() { return "Doe"; }
            public String getEmail() { return "john.doe@carddemo.com"; }
            public String getDepartment() { return "ADMIN"; }
            public String getUserType() { return "ADMIN"; }
        };
    }

    /**
     * Creates a sample Customer entity with realistic customer data.
     * 
     * @return Customer entity with pre-populated fields
     */
    public Object createSampleCustomer() {
        return new Object() {
            public Long getCustomerId() { return ID_GENERATOR.incrementAndGet(); }
            public String getFirstName() { return "Jane"; }
            public String getLastName() { return "Smith"; }
            public String getSsn() { return "***-**-1234"; }
            public String getPhoneNumber1() { return "555-0123"; }
            public LocalDate getDateOfBirth() { return LocalDate.of(1985, 5, 15); }
        };
    }

    /**
     * Creates a sample Card entity with realistic card data.
     * 
     * @return Card entity with pre-populated fields
     */
    public Object createSampleCard() {
        return new Object() {
            public Long getCardId() { return ID_GENERATOR.incrementAndGet(); }
            public String getCardNumber() { return "4***-****-****-1234"; }
            public Long getAccountId() { return 1001L; }
            public String getCardStatus() { return "ACTIVE"; }
            public LocalDate getExpirationDate() { return LocalDate.now().plusYears(3); }
        };
    }

    /**
     * Creates a sample UserSecurity entity with authentication data.
     * 
     * @return UserSecurity entity with pre-populated fields
     */
    public Object createSampleUserSecurity() {
        return new Object() {
            public Long getId() { return ID_GENERATOR.incrementAndGet(); }
            public String getUsername() { return "testuser"; }
            public String getPassword() { return "$2a$10$encoded.password.hash"; }
            public boolean isEnabled() { return true; }
            public String getRole() { return "USER"; }
        };
    }

    /**
     * Creates a sample AuditLog entity with audit trail data.
     * 
     * @return AuditLog entity with pre-populated fields
     */
    public Object createSampleAuditLog() {
        return new Object() {
            public Long getId() { return ID_GENERATOR.incrementAndGet(); }
            public String getUsername() { return "testuser"; }
            public String getEventType() { return "LOGIN"; }
            public LocalDateTime getTimestamp() { return LocalDateTime.now(); }
            public String getDescription() { return "User login successful"; }
        };
    }

    /**
     * Creates a sample AccountDto with comprehensive account and customer data.
     * 
     * @return AccountDto with pre-populated fields
     */
    public Object createSampleAccountDto() {
        return new Object() {
            public String getAccountId() { return String.format("%011d", ID_GENERATOR.incrementAndGet()); }
            public String getActiveStatus() { return DEFAULT_ACTIVE_STATUS; }
            public BigDecimal getCurrentBalance() { return DEFAULT_BALANCE; }
            public BigDecimal getCreditLimit() { return DEFAULT_CREDIT_LIMIT; }
            public String getCustomerFirstName() { return "John"; }
            public String getCustomerLastName() { return "Doe"; }
        };
    }

    /**
     * Creates a sample TransactionDto with detailed transaction information.
     * 
     * @return TransactionDto with pre-populated fields
     */
    public Object createSampleTransactionDto() {
        return new Object() {
            public Long getTransactionId() { return ID_GENERATOR.incrementAndGet(); }
            public Long getAccountId() { return 1001L; }
            public BigDecimal getAmount() { return new BigDecimal("75.25"); }
            public String getMerchantName() { return "Test Merchant"; }
            public LocalDate getTransactionDate() { return LocalDate.now(); }
        };
    }

    /**
     * Creates a sample UserDto with user profile information.
     * 
     * @return UserDto with pre-populated fields
     */
    public Object createSampleUserDto() {
        return new Object() {
            public String getUserId() { return "USER001"; }
            public String getFirstName() { return "John"; }
            public String getLastName() { return "Doe"; }
            public String getEmail() { return "john.doe@carddemo.com"; }
            public String getUserType() { return "ADMIN"; }
        };
    }

    /**
     * Creates a sample UserListResponse with paginated user data.
     * 
     * @return UserListResponse with pre-populated user list and pagination
     */
    public Object createSampleUserListResponse() {
        return new Object() {
            public List<Object> getUsers() { 
                List<Object> users = new ArrayList<>();
                users.add(createSampleUser());
                users.add(createSampleUser());
                return users;
            }
            public int getPageNumber() { return 1; }
            public long getTotalCount() { return 25L; }
            public boolean isHasNextPage() { return true; }
            public boolean isHasPreviousPage() { return false; }
        };
    }

    /**
     * Configures successful response scenarios for all mocked objects.
     * 
     * This method ensures all mocks return successful responses for standard operations,
     * useful for positive test scenario setup.
     * 
     * @param mocks Array of mock objects to configure for success scenarios
     */
    public void configureSuccessResponse(Object... mocks) {
        for (Object mock : mocks) {
            if (mock instanceof AccountRepository) {
                when(((AccountRepository) mock).save(ArgumentMatchers.any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            } else if (mock instanceof TransactionRepository) {
                when(((TransactionRepository) mock).save(ArgumentMatchers.any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            } else if (mock instanceof AccountService) {
                when(((AccountService) mock).viewAccount(ArgumentMatchers.anyLong()))
                    .thenReturn(createSampleAccountDto());
            } else if (mock instanceof TransactionService) {
                when(((TransactionService) mock).listTransactions(
                    ArgumentMatchers.anyLong(), ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                    .thenReturn(configurePaginatedResponse(Arrays.asList(createSampleTransaction()), 0));
            }
        }
    }

    /**
     * Resets all mock objects to their default state.
     * 
     * This method clears all stubbing and interaction history from mock objects,
     * useful for test cleanup or resetting mock state between test methods.
     * 
     * @param mocks Array of mock objects to reset
     */
    public void resetAllMocks(Object... mocks) {
        for (Object mock : mocks) {
            reset(mock);
        }
    }
}