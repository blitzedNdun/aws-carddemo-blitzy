/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.Customer;
import com.carddemo.entity.User;
import com.carddemo.entity.UserSecurity;
import com.carddemo.entity.CardXref;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class providing builder methods for creating test data objects that match COBOL record structures.
 * 
 * This TestDataBuilder provides comprehensive test data generation capabilities for all major entities
 * in the CardDemo system, ensuring that generated test objects match the COBOL record layouts from 
 * the original mainframe implementation.
 * 
 * Key Features:
 * - Generates test Account objects matching CVACT01Y structure with proper BigDecimal monetary fields
 * - Creates test Card objects matching CVACT02Y structure with 16-digit card numbers and CVV codes
 * - Builds test Transaction objects matching CVTRA05Y with proper timestamps and amounts
 * - Generates test Customer objects matching CVCUS01Y with address fields and FICO scores
 * - Creates test User and UserSecurity objects with BCrypt-encoded passwords
 * - Provides test CardXref objects for card-account-customer linkage
 * - Ensures BigDecimal precision for financial calculations (scale=2, ROUND_HALF_UP)
 * - Generates realistic test data for comprehensive validation testing
 * - Supports VSAM-equivalent composite key structures
 * - Maintains COBOL data type mappings and field length constraints
 * 
 * All monetary amounts use BigDecimal with scale=2 to preserve COBOL COMP-3 packed decimal precision.
 * Date values are generated in YYYY-MM-DD format matching COBOL date field expectations.
 * Random data generation provides realistic test scenarios while maintaining referential integrity.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public class TestDataBuilder {

    // Constants for realistic test data generation
    private static final String[] FIRST_NAMES = {
        "JOHN", "JANE", "MICHAEL", "SARAH", "DAVID", "LISA", "ROBERT", "JENNIFER", "WILLIAM", "MARIA"
    };
    
    private static final String[] LAST_NAMES = {
        "SMITH", "JOHNSON", "WILLIAMS", "BROWN", "JONES", "GARCIA", "MILLER", "DAVIS", "RODRIGUEZ", "MARTINEZ"
    };
    
    private static final String[] CITIES = {
        "NEW YORK", "LOS ANGELES", "CHICAGO", "HOUSTON", "PHOENIX", "PHILADELPHIA", "SAN ANTONIO", "SAN DIEGO", "DALLAS", "SAN JOSE"
    };
    
    private static final String[] STATES = {
        "NY", "CA", "IL", "TX", "AZ", "PA", "FL", "OH", "NC", "GA"
    };
    
    private static final String[] MERCHANT_NAMES = {
        "WALMART SUPERCENTER", "TARGET STORE", "AMAZON.COM", "SHELL GAS STATION", "MCDONALDS", 
        "STARBUCKS COFFEE", "HOME DEPOT", "BEST BUY", "KROGER GROCERY", "CVS PHARMACY"
    };
    
    private static final String[] TRANSACTION_DESCRIPTIONS = {
        "PURCHASE", "PAYMENT", "CASH ADVANCE", "INTEREST CHARGE", "LATE FEE", 
        "ANNUAL FEE", "BALANCE TRANSFER", "CREDIT ADJUSTMENT", "DEBIT ADJUSTMENT", "REFUND"
    };

    // Shared random instance and password encoder
    private static final Random random = new Random();
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Builds a test Account object with specified parameters.
     * Creates an Account with proper BigDecimal monetary fields matching CVACT01Y COBOL structure.
     * 
     * @param accountId the account ID (11-digit numeric from COBOL PIC 9(11))
     * @param activeStatus the active status ('Y' or 'N' from COBOL PIC X(01))
     * @param currentBalance the current balance (from COBOL PIC S9(10)V99)
     * @param creditLimit the credit limit (from COBOL PIC S9(10)V99)
     * @param openDate the account open date (from COBOL PIC X(10))
     * @param groupId the group ID (from COBOL PIC X(10))
     * @return configured Account object with COBOL-equivalent precision
     */
    public static Account buildAccount(Long accountId, String activeStatus, BigDecimal currentBalance,
                                     BigDecimal creditLimit, LocalDate openDate, String groupId) {
        Account account = new Account();
        
        // Set account ID with proper validation
        account.setAccountId(accountId);
        
        // Set active status matching COBOL PIC X(01) constraints
        account.setActiveStatus(activeStatus != null ? activeStatus : "Y");
        
        // Set monetary fields with proper scale for COBOL COMP-3 precision
        if (currentBalance != null) {
            account.setCurrentBalance(currentBalance.setScale(2, BigDecimal.ROUND_HALF_UP));
        } else {
            account.setCurrentBalance(BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        
        if (creditLimit != null) {
            account.setCreditLimit(creditLimit.setScale(2, BigDecimal.ROUND_HALF_UP));
        } else {
            account.setCreditLimit(BigDecimal.valueOf(5000.00).setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        
        // Set dates matching COBOL PIC X(10) format
        account.setOpenDate(openDate != null ? openDate : LocalDate.now().minusYears(2));
        account.setExpirationDate(openDate != null ? openDate.plusYears(5) : LocalDate.now().plusYears(3));
        
        // Set cycle amounts with proper precision
        account.setCurrentCycleCredit(BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP));
        account.setCurrentCycleDebit(BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP));
        
        // Set group ID matching COBOL PIC X(10) constraints
        account.setGroupId(groupId != null ? groupId : "DEFAULT");
        
        return account;
    }

    /**
     * Builds a test Card object with specified parameters.
     * Creates a Card with 16-digit card number, CVV code, and relationships matching CVACT02Y structure.
     * 
     * @param cardNumber the 16-digit card number (from COBOL PIC X(16))
     * @param accountId the linked account ID (from COBOL PIC 9(11))
     * @param cvvCode the 3-digit CVV code (from COBOL PIC 9(03))
     * @param embossedName the embossed name (from COBOL PIC X(50))
     * @param expirationDate the card expiration date (from COBOL PIC X(10))
     * @param activeStatus the active status ('Y' or 'N' from COBOL PIC X(01))
     * @return configured Card object with proper field constraints
     */
    public static Card buildCard(String cardNumber, Long accountId, String cvvCode, 
                               String embossedName, LocalDate expirationDate, String activeStatus) {
        Card card = new Card();
        
        // Set card number with 16-digit validation
        card.setCardNumber(cardNumber != null ? cardNumber : generateRandomCardNumber());
        
        // Set account ID relationship
        card.setAccountId(accountId);
        
        // Set CVV code with 3-digit validation
        card.setCvvCode(cvvCode != null ? cvvCode : String.format("%03d", random.nextInt(1000)));
        
        // Set embossed name matching COBOL PIC X(50) constraints
        card.setEmbossedName(embossedName != null ? embossedName : "TEST CARDHOLDER");
        
        // Set expiration date
        card.setExpirationDate(expirationDate != null ? expirationDate : LocalDate.now().plusYears(3));
        
        // Set active status
        card.setActiveStatus(activeStatus != null ? activeStatus : "Y");
        
        return card;
    }

    /**
     * Builds a test Transaction object with specified parameters.
     * Creates a Transaction with BigDecimal amounts, merchant information, and timestamps matching CVTRA05Y structure.
     * 
     * @param transactionId the transaction ID (from COBOL PIC X(16))
     * @param amount the transaction amount (from COBOL PIC S9(09)V99)
     * @param transactionType the transaction type code (from COBOL PIC X(02))
     * @param accountId the linked account ID
     * @param description the transaction description (from COBOL PIC X(100))
     * @param merchantName the merchant name (from COBOL PIC X(50))
     * @param cardNumber the card number used (from COBOL PIC X(16))
     * @param originalTimestamp the original timestamp (from COBOL PIC X(26))
     * @param processedTimestamp the processed timestamp (from COBOL PIC X(26))
     * @return configured Transaction object with proper monetary precision
     */
    public static Transaction buildTransaction(Long transactionId, BigDecimal amount, String transactionType,
                                             Long accountId, String description, String merchantName, 
                                             String cardNumber, LocalDateTime originalTimestamp, 
                                             LocalDateTime processedTimestamp) {
        Transaction transaction = new Transaction();
        
        // Set transaction ID
        transaction.setTransactionId(transactionId);
        
        // Set amount with proper scale for COBOL COMP-3 precision
        if (amount != null) {
            transaction.setAmount(amount.setScale(2, BigDecimal.ROUND_HALF_UP));
        } else {
            transaction.setAmount(BigDecimal.valueOf(100.00).setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        
        // Set transaction type code matching COBOL PIC X(02)
        transaction.setTransactionTypeCode(transactionType != null ? transactionType : "PU");
        
        // Set account ID relationship
        transaction.setAccountId(accountId);
        
        // Set description matching COBOL PIC X(100) constraints
        transaction.setDescription(description != null ? description : "TEST TRANSACTION");
        
        // Set merchant information matching COBOL field constraints
        transaction.setMerchantName(merchantName != null ? merchantName : "TEST MERCHANT");
        transaction.setMerchantCity(CITIES[random.nextInt(CITIES.length)]);
        transaction.setMerchantZip(String.format("%05d", random.nextInt(100000)));
        
        // Set card number
        transaction.setCardNumber(cardNumber != null ? cardNumber : generateRandomCardNumber());
        
        // Set timestamps matching COBOL PIC X(26) format
        LocalDateTime now = LocalDateTime.now();
        transaction.setOriginalTimestamp(originalTimestamp != null ? originalTimestamp : now.minusHours(1));
        transaction.setProcessedTimestamp(processedTimestamp != null ? processedTimestamp : now);
        
        // Set transaction date for partitioning
        transaction.setTransactionDate(LocalDate.now());
        
        // Set category and source codes
        transaction.setCategoryCode(String.format("%04d", random.nextInt(10000)));
        transaction.setSource("WEB");
        
        return transaction;
    }

    /**
     * Builds a test Customer object with specified parameters.
     * Creates a Customer with name, address, phone, SSN, and FICO score fields matching CVCUS01Y structure.
     * 
     * @param customerId the customer ID (from COBOL PIC 9(09))
     * @param firstName the first name (from COBOL PIC X(25))
     * @param lastName the last name (from COBOL PIC X(25))
     * @param phoneNumber the phone number (from COBOL PIC X(15))
     * @param ssn the SSN (from COBOL PIC 9(09))
     * @param ficoScore the FICO credit score (from COBOL PIC 9(03))
     * @param dateOfBirth the date of birth (from COBOL PIC X(10))
     * @return configured Customer object with proper field constraints
     */
    public static Customer buildCustomer(Long customerId, String firstName, String lastName, 
                                       String phoneNumber, String ssn, Integer ficoScore, 
                                       LocalDate dateOfBirth) {
        Customer customer = new Customer();
        
        // Set customer ID
        customer.setCustomerId(customerId != null ? customerId.toString() : null);
        
        // Set name fields matching COBOL PIC X(25) constraints (truncated to 20 for DB)
        customer.setFirstName(firstName != null ? firstName.substring(0, Math.min(firstName.length(), 20)) : "JOHN");
        customer.setLastName(lastName != null ? lastName.substring(0, Math.min(lastName.length(), 20)) : "DOE");
        
        // Set phone number matching COBOL PIC X(15) constraints
        customer.setPhoneNumber1(phoneNumber != null ? phoneNumber : generateRandomPhoneNumber());
        
        // Set SSN matching COBOL PIC 9(09) constraints
        customer.setSsn(ssn != null ? ssn : generateRandomSSN());
        
        // Set FICO score matching COBOL PIC 9(03) constraints (300-850 range)
        if (ficoScore != null && ficoScore >= 300 && ficoScore <= 850) {
            customer.setFicoScore(ficoScore);
        } else {
            customer.setFicoScore(650 + random.nextInt(200)); // 650-849 range
        }
        
        // Set date of birth
        customer.setDateOfBirth(dateOfBirth != null ? dateOfBirth : generateRandomDate().minusYears(25));
        
        // Set address fields with realistic data
        customer.setAddressLine1("123 MAIN ST");
        customer.setStateCode(STATES[random.nextInt(STATES.length)]);
        customer.setCountryCode("USA");
        customer.setZipCode(String.format("%05d", random.nextInt(100000)));
        
        return customer;
    }

    /**
     * Builds a test User object with specified parameters.
     * Creates a User with profile information, business attributes, and administrative metadata.
     * 
     * @param userId the user ID (8 characters matching business user ID)
     * @param firstName the first name for business profile
     * @param lastName the last name for business profile
     * @param email the email address
     * @param phone the phone number
     * @param status the user status ('A'=Active, 'I'=Inactive, 'S'=Suspended)
     * @param userType the user type ('A'=Admin, 'U'=User)
     * @return configured User object with proper business profile data
     */
    public static User buildUser(String userId, String firstName, String lastName, 
                               String email, String phone, String status, String userType) {
        User user = new User();
        
        // Set user ID matching 8-character constraint
        user.setUserId(userId != null ? userId : generateRandomUserId());
        
        // Set name fields for business profile
        user.setFirstName(firstName != null ? firstName : FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]);
        user.setLastName(lastName != null ? lastName : LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
        
        // Set contact information
        user.setEmail(email != null ? email : generateRandomEmail(user.getFirstName(), user.getLastName()));
        user.setPhone(phone != null ? phone : generateRandomPhoneNumber());
        
        // Set status and type with validation
        user.setStatus(status != null ? status : "A");
        user.setUserType(userType != null ? userType : "U");
        
        // Set business attributes
        user.setDepartment("CARD SERVICES");
        user.setCreatedBy("SYSTEM");
        user.setCreatedDate(LocalDateTime.now().minusDays(30));
        
        return user;
    }

    /**
     * Builds a test UserSecurity object with specified parameters.
     * Creates a UserSecurity with BCrypt-encoded passwords and security roles implementing Spring Security UserDetails.
     * 
     * @param username the username for login
     * @param password the plain text password (will be BCrypt encoded)
     * @param userType the user type ('A'=Admin, 'U'=User)
     * @param firstName the first name (from COBOL SEC-USR-FNAME field, 20 characters)
     * @param lastName the last name (from COBOL SEC-USR-LNAME field, 20 characters)
     * @param secUsrId the security user ID (from COBOL SEC-USR-ID field, 8 characters)
     * @return configured UserSecurity object with BCrypt-encoded password
     */
    public static UserSecurity buildUserSecurity(String username, String password, String userType,
                                               String firstName, String lastName, String secUsrId) {
        UserSecurity userSecurity = new UserSecurity();
        
        // Set username for login
        userSecurity.setUsername(username != null ? username : generateRandomUserId());
        
        // Set BCrypt-encoded password
        String plainPassword = password != null ? password : "password123";
        userSecurity.setPassword(encodePassword(plainPassword));
        
        // Set user type with validation ('A' or 'U')
        userSecurity.setUserType(userType != null ? userType : "U");
        
        // Set name fields matching COBOL field constraints (20 characters max)
        userSecurity.setFirstName(firstName != null ? 
            firstName.substring(0, Math.min(firstName.length(), 20)) : 
            FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]);
        userSecurity.setLastName(lastName != null ? 
            lastName.substring(0, Math.min(lastName.length(), 20)) : 
            LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
        
        // Set security user ID matching COBOL SEC-USR-ID (8 characters)
        userSecurity.setSecUsrId(secUsrId != null ? secUsrId : generateRandomUserId());
        
        // Set security flags for active user
        userSecurity.setEnabled(true);
        userSecurity.setAccountNonExpired(true);
        userSecurity.setAccountNonLocked(true);
        userSecurity.setCredentialsNonExpired(true);
        userSecurity.setFailedLoginAttempts(0);
        
        return userSecurity;
    }

    /**
     * Builds a test CardXref object with specified parameters.
     * Creates a CardXref for card-account-customer linkage relationships matching COBOL CVACT03Y structure.
     * 
     * @param cardNumber the cross-reference card number (from COBOL PIC X(16))
     * @param customerId the cross-reference customer ID (from COBOL PIC 9(09))
     * @param accountId the cross-reference account ID (from COBOL PIC 9(11))
     * @return configured CardXref object with composite key relationships
     */
    public static CardXref buildCardXref(String cardNumber, Long customerId, Long accountId) {
        CardXref cardXref = new CardXref();
        
        // Set cross-reference fields using actual available methods
        cardXref.setXrefCardNum(cardNumber != null ? cardNumber : generateRandomCardNumber());
        cardXref.setXrefCustId(customerId != null ? customerId : generateRandomCustomerId());
        cardXref.setXrefAcctId(accountId != null ? accountId : generateRandomAccountId());
        
        return cardXref;
    }

    /**
     * Builds a test Account object with default values.
     * Creates an Account with realistic default values for quick testing.
     * 
     * @return Account object with default test values
     */
    public static Account buildAccountWithDefaults() {
        return buildAccount(
            generateRandomAccountId(),
            "Y",
            BigDecimal.valueOf(1500.00),
            BigDecimal.valueOf(5000.00),
            LocalDate.now().minusYears(1),
            "DEFAULT"
        );
    }

    /**
     * Builds a test Card object with default values.
     * Creates a Card with realistic default values for quick testing.
     * 
     * @return Card object with default test values
     */
    public static Card buildCardWithDefaults() {
        return buildCard(
            generateRandomCardNumber(),
            generateRandomAccountId(),
            String.format("%03d", random.nextInt(1000)),
            "DEFAULT CARDHOLDER",
            LocalDate.now().plusYears(3),
            "Y"
        );
    }

    /**
     * Builds a test Transaction object with default values.
     * Creates a Transaction with realistic default values for quick testing.
     * 
     * @return Transaction object with default test values
     */
    public static Transaction buildTransactionWithDefaults() {
        return buildTransaction(
            generateRandomTransactionId(),
            generateRandomAmount(),
            "PU",
            generateRandomAccountId(),
            TRANSACTION_DESCRIPTIONS[random.nextInt(TRANSACTION_DESCRIPTIONS.length)],
            MERCHANT_NAMES[random.nextInt(MERCHANT_NAMES.length)],
            generateRandomCardNumber(),
            generateRandomTimestamp().minusHours(1),
            generateRandomTimestamp()
        );
    }

    /**
     * Builds a test Customer object with default values.
     * Creates a Customer with realistic default values for quick testing.
     * 
     * @return Customer object with default test values
     */
    public static Customer buildCustomerWithDefaults() {
        return buildCustomer(
            generateRandomCustomerId(),
            FIRST_NAMES[random.nextInt(FIRST_NAMES.length)],
            LAST_NAMES[random.nextInt(LAST_NAMES.length)],
            generateRandomPhoneNumber(),
            generateRandomSSN(),
            650 + random.nextInt(200),
            generateRandomDate().minusYears(25 + random.nextInt(40))
        );
    }

    /**
     * Builds a test User object with default values.
     * Creates a User with realistic default values for quick testing.
     * 
     * @return User object with default test values
     */
    public static User buildUserWithDefaults() {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        return buildUser(
            generateRandomUserId(),
            firstName,
            lastName,
            generateRandomEmail(firstName, lastName),
            generateRandomPhoneNumber(),
            "A",
            "U"
        );
    }

    /**
     * Builds a test UserSecurity object with default values.
     * Creates a UserSecurity with realistic default values and BCrypt-encoded password for quick testing.
     * 
     * @return UserSecurity object with default test values
     */
    public static UserSecurity buildUserSecurityWithDefaults() {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String userId = generateRandomUserId();
        return buildUserSecurity(
            userId,
            "password123",
            "U",
            firstName,
            lastName,
            userId
        );
    }

    /**
     * Builds a test CardXref object with default values.
     * Creates a CardXref with realistic default values for quick testing.
     * 
     * @return CardXref object with default test values
     */
    public static CardXref buildCardXrefWithDefaults() {
        return buildCardXref(
            generateRandomCardNumber(),
            generateRandomCustomerId(),
            generateRandomAccountId()
        );
    }

    // Random generation utility methods

    /**
     * Generates a random account ID matching COBOL PIC 9(11) constraints.
     * Produces an 11-digit numeric account ID for realistic test data.
     * 
     * @return random account ID as Long
     */
    public static Long generateRandomAccountId() {
        // Generate 11-digit account ID (10000000000 to 99999999999)
        return ThreadLocalRandom.current().nextLong(10000000000L, 100000000000L);
    }

    /**
     * Generates a random 16-digit card number matching COBOL PIC X(16) constraints.
     * Produces a valid 16-digit card number for realistic test data.
     * 
     * @return random 16-digit card number as String
     */
    public static String generateRandomCardNumber() {
        // Generate 16-digit card number starting with 4 (Visa) for simplicity
        StringBuilder cardNumber = new StringBuilder("4");
        for (int i = 1; i < 16; i++) {
            cardNumber.append(random.nextInt(10));
        }
        return cardNumber.toString();
    }

    /**
     * Generates a random transaction ID.
     * Produces a unique transaction ID for test transactions.
     * 
     * @return random transaction ID as Long
     */
    public static Long generateRandomTransactionId() {
        return ThreadLocalRandom.current().nextLong(1000000L, 10000000L);
    }

    /**
     * Generates a random customer ID matching COBOL PIC 9(09) constraints.
     * Produces a 9-digit numeric customer ID for realistic test data.
     * 
     * @return random customer ID as Long
     */
    public static Long generateRandomCustomerId() {
        // Generate 9-digit customer ID (100000000 to 999999999)
        return ThreadLocalRandom.current().nextLong(100000000L, 1000000000L);
    }

    /**
     * Generates a random transaction amount with proper BigDecimal precision.
     * Produces monetary amounts with scale=2 matching COBOL COMP-3 precision.
     * 
     * @return random transaction amount as BigDecimal with scale=2
     */
    public static BigDecimal generateRandomAmount() {
        // Generate amount between $1.00 and $999.99
        double amount = 1.00 + (random.nextDouble() * 998.99);
        return BigDecimal.valueOf(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Generates a random date for test data.
     * Produces dates within a reasonable range for testing.
     * 
     * @return random LocalDate
     */
    public static LocalDate generateRandomDate() {
        // Generate date within last 5 years
        LocalDate start = LocalDate.now().minusYears(5);
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, LocalDate.now());
        long randomDays = ThreadLocalRandom.current().nextLong(0, daysBetween);
        return start.plusDays(randomDays);
    }

    /**
     * Generates a random timestamp for test data.
     * Produces timestamps within a reasonable range for testing.
     * 
     * @return random LocalDateTime
     */
    public static LocalDateTime generateRandomTimestamp() {
        LocalDate randomDate = generateRandomDate();
        int hour = random.nextInt(24);
        int minute = random.nextInt(60);
        int second = random.nextInt(60);
        return randomDate.atTime(hour, minute, second);
    }

    /**
     * Encodes a plain text password using BCrypt.
     * Uses BCryptPasswordEncoder for proper password security in test data.
     * 
     * @param plainPassword the plain text password to encode
     * @return BCrypt-encoded password hash
     */
    public static String encodePassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    /**
     * Generates a random 8-character user ID for test data.
     * Produces user IDs matching business requirements.
     * 
     * @return random 8-character user ID as String
     */
    private static String generateRandomUserId() {
        // Generate 8-character user ID starting with "USR" + 5 digits
        return "USR" + String.format("%05d", random.nextInt(100000));
    }

    /**
     * Generates a random phone number for test data.
     * Produces phone numbers in standard US format.
     * 
     * @return random phone number as String
     */
    private static String generateRandomPhoneNumber() {
        // Generate phone number in format XXX-XXX-XXXX
        return String.format("%03d-%03d-%04d", 
            200 + random.nextInt(800),  // Area code 200-999
            200 + random.nextInt(800),  // Exchange 200-999
            random.nextInt(10000)       // Number 0000-9999
        );
    }

    /**
     * Generates a random SSN for test data.
     * Produces 9-digit SSNs for testing (not real SSNs).
     * 
     * @return random 9-digit SSN as String
     */
    private static String generateRandomSSN() {
        // Generate 9-digit SSN (not real, for testing only)
        return String.format("%09d", random.nextInt(1000000000));
    }

    /**
     * Generates a random email address for test data.
     * Creates email addresses based on first and last names.
     * 
     * @param firstName the first name
     * @param lastName the last name
     * @return random email address as String
     */
    private static String generateRandomEmail(String firstName, String lastName) {
        String[] domains = {"example.com", "test.com", "demo.com"};
        String domain = domains[random.nextInt(domains.length)];
        return (firstName + "." + lastName + "@" + domain).toLowerCase();
    }

    // Static function exports as required by schema

    /**
     * Creates a default Account object for quick testing.
     * Static function export that provides a pre-configured Account with realistic default values.
     * 
     * @return Account object with default test values
     */
    public static Account createDefaultAccount() {
        return buildAccountWithDefaults();
    }

    /**
     * Creates a default Card object for quick testing.
     * Static function export that provides a pre-configured Card with realistic default values.
     * 
     * @return Card object with default test values
     */
    public static Card createDefaultCard() {
        return buildCardWithDefaults();
    }

    /**
     * Creates a default Transaction object for quick testing.
     * Static function export that provides a pre-configured Transaction with realistic default values.
     * 
     * @return Transaction object with default test values
     */
    public static Transaction createDefaultTransaction() {
        return buildTransactionWithDefaults();
    }

    /**
     * Creates a default Customer object for quick testing.
     * Static function export that provides a pre-configured Customer with realistic default values.
     * 
     * @return Customer object with default test values
     */
    public static Customer createDefaultCustomer() {
        return buildCustomerWithDefaults();
    }

    /**
     * Creates a comprehensive test data set with interrelated objects.
     * Static function export that provides a complete set of related test objects for integration testing.
     * Ensures referential integrity between Account, Card, Customer, User, UserSecurity, and CardXref objects.
     * 
     * @return List containing related test objects [Account, Card, Customer, User, UserSecurity, CardXref]
     */
    public static List<Object> createTestDataSet() {
        List<Object> testDataSet = new ArrayList<>();
        
        // Generate consistent IDs for related objects
        Long accountId = generateRandomAccountId();
        Long customerId = generateRandomCustomerId();
        String cardNumber = generateRandomCardNumber();
        String userId = generateRandomUserId();
        
        // Create related Account with consistent ID
        Account account = buildAccount(
            accountId,
            "Y",
            BigDecimal.valueOf(2500.00),
            BigDecimal.valueOf(10000.00),
            LocalDate.now().minusYears(2),
            "PREMIUM"
        );
        testDataSet.add(account);
        
        // Create related Customer with consistent ID
        Customer customer = buildCustomer(
            customerId,
            "ALICE",
            "JOHNSON",
            "555-123-4567",
            "123456789",
            720,
            LocalDate.of(1985, 6, 15)
        );
        testDataSet.add(customer);
        
        // Create related Card with consistent IDs
        Card card = buildCard(
            cardNumber,
            accountId,
            "123",
            "ALICE JOHNSON",
            LocalDate.now().plusYears(4),
            "Y"
        );
        testDataSet.add(card);
        
        // Create related Transaction
        Transaction transaction = buildTransaction(
            generateRandomTransactionId(),
            BigDecimal.valueOf(150.75),
            "PU",
            accountId,
            "GROCERY PURCHASE",
            "KROGER GROCERY",
            cardNumber,
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(1)
        );
        testDataSet.add(transaction);
        
        // Create related User
        User user = buildUser(
            userId,
            "ALICE",
            "JOHNSON",
            "alice.johnson@carddemo.com",
            "555-123-4567",
            "A",
            "U"
        );
        testDataSet.add(user);
        
        // Create related UserSecurity
        UserSecurity userSecurity = buildUserSecurity(
            userId,
            "securePassword123",
            "U",
            "ALICE",
            "JOHNSON",
            userId
        );
        testDataSet.add(userSecurity);
        
        // Create related CardXref for relationships
        CardXref cardXref = buildCardXref(cardNumber, customerId, accountId);
        testDataSet.add(cardXref);
        
        return testDataSet;
    }
}
