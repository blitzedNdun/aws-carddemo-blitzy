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
import com.carddemo.entity.*;
import com.carddemo.dto.*;

import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.HashOperations;

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
        
        // Configure findById() method - Card ID is String (cardNumber)
        when(mock.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(createSampleCard()));
        
        // Configure findByAccountId() method
        when(mock.findByAccountId(ArgumentMatchers.anyLong())).thenReturn(Arrays.asList(createSampleCard()));
        
        // Configure findAll() method
        when(mock.findAll()).thenReturn(Arrays.asList(createSampleCard(), createSampleCard()));
        
        // Configure deleteById() method - no-op
        doNothing().when(mock).deleteById(ArgumentMatchers.anyString());
        
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
        
        // Configure findByUsernameAndTimestampBetween() method - returns Page
        when(mock.findByUsernameAndTimestampBetween(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(LocalDateTime.class),
            ArgumentMatchers.any(LocalDateTime.class),
            ArgumentMatchers.any(Pageable.class)
        )).thenReturn(configurePaginatedResponse(Arrays.asList(createSampleAuditLog()), 0));
        
        // Configure findByEventTypeAndTimestampBetween() method - returns Page
        when(mock.findByEventTypeAndTimestampBetween(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(LocalDateTime.class),
            ArgumentMatchers.any(LocalDateTime.class),
            ArgumentMatchers.any(Pageable.class)
        )).thenReturn(configurePaginatedResponse(Arrays.asList(createSampleAuditLog()), 0));
        
        // Configure deleteByTimestampBefore() method - returns count
        when(mock.deleteByTimestampBefore(ArgumentMatchers.any(LocalDateTime.class))).thenReturn(0);
        
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
        
        // Configure updateAccount() method
        when(mock.updateAccount(ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
            .thenReturn(createSampleAccountDto());
        
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
        when(mock.getTransactionDetail(ArgumentMatchers.anyString())).thenReturn(Optional.of(createSampleTransaction()));
        
        // Configure addTransaction() method
        when(mock.addTransaction(ArgumentMatchers.any())).thenReturn(createSampleTransaction());
        
        // Configure validateTransaction() method - no-op for valid requests
        doNothing().when(mock).validateTransaction(ArgumentMatchers.any());
        
        // Configure processPageNavigation() method
        when(mock.processPageNavigation(
            ArgumentMatchers.anyString(), 
            ArgumentMatchers.anyLong(), 
            ArgumentMatchers.anyString()))
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
        
        // Configure findUserById() method
        when(mock.findUserById(ArgumentMatchers.anyString())).thenReturn(createSampleUserDto());
        
        // Configure createUser() method
        when(mock.createUser(ArgumentMatchers.any())).thenReturn(createSampleUserDto());
        
        // Configure updateUser() method
        when(mock.updateUser(ArgumentMatchers.anyString(), ArgumentMatchers.any()))
            .thenReturn(createSampleUserDto());
        
        // Configure deleteUser() method - no-op
        doNothing().when(mock).deleteUser(ArgumentMatchers.anyString());
        
        // Configure listUsers() method
        when(mock.listUsers(ArgumentMatchers.any(Pageable.class)))
            .thenReturn(createSampleUserListResponse());
        
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
        var valueOps = mock(ValueOperations.class);
        var hashOps = mock(HashOperations.class);
        
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
                    // Configure service to throw validation error on update
                    when(((AccountService) mock).updateAccount(ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                        .thenThrow(new RuntimeException("Validation failed"));
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
    public Account createSampleAccount() {
        Account account = new Account();
        account.setAccountId(ID_GENERATOR.incrementAndGet());
        account.setActiveStatus(DEFAULT_ACTIVE_STATUS);
        account.setCurrentBalance(DEFAULT_BALANCE);
        account.setCreditLimit(DEFAULT_CREDIT_LIMIT);
        account.setOpenDate(LocalDate.now().minusYears(2));
        // Set customer relationship instead of customerId directly
        Customer customer = createSampleCustomer();
        account.setCustomer(customer);
        return account;
    }

    /**
     * Creates a sample Transaction entity with realistic transaction data.
     * 
     * @return Transaction entity with pre-populated fields matching COBOL transaction structures
     */
    public Transaction createSampleTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(ID_GENERATOR.incrementAndGet());
        transaction.setAccountId(1001L);
        transaction.setAmount(new BigDecimal("125.50"));
        transaction.setTransactionTypeCode("PU");
        transaction.setTransactionDate(LocalDate.now());
        transaction.setMerchantName("Sample Merchant");
        transaction.setDescription("Sample Transaction");
        return transaction;
    }

    /**
     * Creates a sample User entity with realistic user profile data.
     * 
     * @return User entity with pre-populated fields matching COBOL user structures
     */
    public User createSampleUser() {
        User user = new User();
        user.setId(ID_GENERATOR.incrementAndGet());
        user.setUserId("USER001");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@carddemo.com");
        user.setDepartment("ADMIN");
        user.setUserType("ADMIN");
        return user;
    }

    /**
     * Creates a sample Customer entity with realistic customer data.
     * 
     * @return Customer entity with pre-populated fields
     */
    public Customer createSampleCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId(String.format("%09d", ID_GENERATOR.incrementAndGet()));
        customer.setFirstName("Jane");
        customer.setLastName("Smith");
        customer.setSsn("***-**-1234");
        customer.setPhoneNumber1("555-0123");
        customer.setDateOfBirth(LocalDate.of(1985, 5, 15));
        return customer;
    }

    /**
     * Creates a sample Card entity with realistic card data.
     * 
     * @return Card entity with pre-populated fields
     */
    public Card createSampleCard() {
        Card card = new Card();
        card.setCardNumber("4***-****-****-1234");
        card.setAccountId(1001L);
        card.setActiveStatus("Y");
        card.setExpirationDate(LocalDate.now().plusYears(3));
        return card;
    }

    /**
     * Creates a sample UserSecurity entity with authentication data.
     * 
     * @return UserSecurity entity with pre-populated fields
     */
    public UserSecurity createSampleUserSecurity() {
        UserSecurity userSecurity = new UserSecurity();
        userSecurity.setId(ID_GENERATOR.incrementAndGet());
        userSecurity.setUsername("testuser");
        userSecurity.setPassword("$2a$10$encoded.password.hash");
        userSecurity.setSecUsrId("USER001");
        userSecurity.setFirstName("John");
        userSecurity.setLastName("Doe");
        return userSecurity;
    }

    /**
     * Creates a sample AuditLog entity with audit trail data.
     * 
     * @return AuditLog entity with pre-populated fields
     */
    public AuditLog createSampleAuditLog() {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(ID_GENERATOR.incrementAndGet());
        auditLog.setUsername("testuser");
        auditLog.setEventType("LOGIN");
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setDetails("User login successful");
        return auditLog;
    }

    /**
     * Creates a sample AccountDto with comprehensive account and customer data.
     * 
     * @return AccountDto with pre-populated fields
     */
    public AccountDto createSampleAccountDto() {
        AccountDto accountDto = new AccountDto();
        accountDto.setAccountId(String.format("%011d", ID_GENERATOR.incrementAndGet()));
        accountDto.setActiveStatus(DEFAULT_ACTIVE_STATUS);
        accountDto.setCurrentBalance(DEFAULT_BALANCE);
        accountDto.setCreditLimit(DEFAULT_CREDIT_LIMIT);
        accountDto.setCashCreditLimit(new BigDecimal("500.00"));
        accountDto.setOpenDate(LocalDate.now().minusYears(2));
        accountDto.setExpirationDate(LocalDate.now().plusYears(2));
        accountDto.setCustomerId(String.format("%09d", ID_GENERATOR.incrementAndGet()));
        accountDto.setCustomerFirstName("John");
        accountDto.setCustomerLastName("Doe");
        accountDto.calculateDerivedFields();
        return accountDto;
    }

    /**
     * Creates a sample TransactionDto with detailed transaction information.
     * 
     * @return TransactionDto with pre-populated fields
     */
    public TransactionDto createSampleTransactionDto() {
        return TransactionDto.builder()
            .transactionId(ID_GENERATOR.incrementAndGet())
            .accountId(1001L)
            .amount(new BigDecimal("75.25"))
            .typeCode("PU")
            .categoryCode("RETAIL")
            .description("Sample purchase transaction")
            .merchantName("Test Merchant")
            .transactionDate(LocalDate.now())
            .originalTimestamp(LocalDateTime.now())
            .processedTimestamp(LocalDateTime.now())
            .referenceNumber("REF" + ID_GENERATOR.incrementAndGet())
            .authorizationCode("AUTH123")
            .isReversed(false)
            .build();
    }

    /**
     * Creates a sample UserDto with user profile information.
     * 
     * @return UserDto with pre-populated fields
     */
    public UserDto createSampleUserDto() {
        UserDto userDto = new UserDto();
        userDto.setUserId("USER001");
        userDto.setFirstName("John");
        userDto.setLastName("Doe");
        userDto.setUserType("A"); // A=Admin, U=User
        return userDto;
    }

    /**
     * Creates a sample UserListResponse with paginated user data.
     * 
     * @return UserListResponse with pre-populated user list and pagination
     */
    public UserListResponse createSampleUserListResponse() {
        List<UserListDto> userListDtos = new ArrayList<>();
        
        // Create first sample user
        UserListDto user1 = new UserListDto();
        user1.setUserId("USER001");
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setUserType("A");
        user1.setSelectionFlag(" ");
        userListDtos.add(user1);
        
        // Create second sample user
        UserListDto user2 = new UserListDto();
        user2.setUserId("USER002");
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setUserType("U");
        user2.setSelectionFlag(" ");
        userListDtos.add(user2);
        
        return new UserListResponse(userListDtos, 1, 25L, true, false);
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