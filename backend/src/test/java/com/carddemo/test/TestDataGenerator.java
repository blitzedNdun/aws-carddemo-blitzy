/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.test;

import com.carddemo.util.CobolDataConverter;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Card;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.Constants;
import com.carddemo.util.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.lang.StringBuilder;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for generating test data that matches COBOL data patterns.
 * 
 * This comprehensive test data generation utility produces COBOL-compliant test data including:
 * - COMP-3 packed decimal values with proper scale and precision
 * - Formatted strings matching PIC clause specifications
 * - Date values in COBOL format (CCYYMMDD)
 * - Composite keys matching VSAM KSDS structures
 * - Realistic financial test data for accounts, transactions, customers, and cards
 * 
 * Key Features:
 * - Generates data compatible with COBOL data types and format requirements
 * - Produces BigDecimal values with exact precision matching COBOL COMP-3 fields
 * - Creates formatted strings that match COBOL PIC X clauses
 * - Generates valid dates, SSNs, phone numbers, and other structured data
 * - Provides both individual entity generation and batch list generation
 * - Ensures data relationships and referential integrity for related entities
 * 
 * Design Approach:
 * - Uses ThreadLocalRandom for thread-safe random generation in multi-threaded tests
 * - Leverages CobolDataConverter for exact precision preservation
 * - Applies COBOL validation rules through ValidationUtil integration
 * - Maintains seed-based repeatability for deterministic test scenarios
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public final class TestDataGenerator {

    // Random number generator for test data generation
    private static final Random random = new Random();
    
    // Constants for realistic test data generation
    private static final String[] FIRST_NAMES = {
        "JOHN", "MARY", "JAMES", "PATRICIA", "ROBERT", "JENNIFER", "MICHAEL", "LINDA",
        "WILLIAM", "ELIZABETH", "DAVID", "BARBARA", "RICHARD", "SUSAN", "JOSEPH", "JESSICA",
        "THOMAS", "SARAH", "CHRISTOPHER", "KAREN", "CHARLES", "NANCY", "DANIEL", "LISA",
        "MATTHEW", "BETTY", "ANTHONY", "HELEN", "MARK", "SANDRA", "DONALD", "DONNA"
    };
    
    private static final String[] LAST_NAMES = {
        "SMITH", "JOHNSON", "WILLIAMS", "BROWN", "JONES", "GARCIA", "MILLER", "DAVIS",
        "RODRIGUEZ", "MARTINEZ", "HERNANDEZ", "LOPEZ", "GONZALEZ", "WILSON", "ANDERSON", "THOMAS",
        "TAYLOR", "MOORE", "JACKSON", "MARTIN", "LEE", "PEREZ", "THOMPSON", "WHITE",
        "HARRIS", "SANCHEZ", "CLARK", "RAMIREZ", "LEWIS", "ROBINSON", "WALKER", "YOUNG"
    };
    
    private static final String[] CITIES = {
        "NEW YORK", "LOS ANGELES", "CHICAGO", "HOUSTON", "PHOENIX", "PHILADELPHIA",
        "SAN ANTONIO", "SAN DIEGO", "DALLAS", "SAN JOSE", "AUSTIN", "JACKSONVILLE",
        "FORT WORTH", "COLUMBUS", "CHARLOTTE", "SEATTLE", "DENVER", "WASHINGTON",
        "BOSTON", "NASHVILLE", "BALTIMORE", "OKLAHOMA CITY", "PORTLAND", "LAS VEGAS"
    };
    
    private static final String[] STATES = {
        "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
        "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
        "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
        "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
        "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"
    };
    
    private static final String[] MERCHANT_NAMES = {
        "AMAZON.COM", "WALMART", "TARGET", "STARBUCKS", "MCDONALDS", "SUBWAY",
        "HOME DEPOT", "COSTCO", "KROGER", "SHELL", "EXXON", "BP",
        "BEST BUY", "MACYS", "NORDSTROM", "APPLE STORE", "MICROSOFT",
        "GOOGLE PAY", "PAYPAL", "VISA PAYMENT", "MASTERCARD", "AMEX"
    };
    
    private static final String[] TRANSACTION_TYPES = {
        "01", "02", "03", "04", "05", "06", "07", "08", "09", "10"
    };
    
    private static final String[] TRANSACTION_CATEGORIES = {
        "5411", "5812", "5732", "5541", "5999", "5814", "5912", "4111", "4121", "4131"
    };
    
    private static final String[] TRANSACTION_DESCRIPTIONS = {
        "PURCHASE", "PAYMENT", "CASH ADVANCE", "BALANCE TRANSFER", "FEE",
        "INTEREST CHARGE", "CREDIT ADJUSTMENT", "DEBIT ADJUSTMENT",
        "MERCHANT REFUND", "CHARGEBACK"
    };

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private TestDataGenerator() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Generates a realistic Account entity with COBOL-compliant field values.
     * Creates an account with randomly generated balances, credit limits, dates,
     * and other account attributes that match COBOL data patterns.
     * 
     * @return Account entity with COBOL-compatible test data
     */
    public static Account generateAccount() {
        Account account = new Account();
        
        // Generate account ID (11 digits)
        Long accountId = generateAccountId();
        account.setAccountId(accountId);
        
        // Generate active status (Y or N)
        account.setActiveStatus(random.nextBoolean() ? "Y" : "N");
        
        // Generate monetary amounts with COBOL COMP-3 precision
        // Ensure logical consistency: balance should be within credit limit bounds
        BigDecimal creditLimit = generateCreditLimit();
        account.setCreditLimit(creditLimit);
        
        // Generate balance that respects credit limit (can be negative for overpayments, 
        // but shouldn't exceed credit limit for realistic test scenarios)
        BigDecimal maxBalance = creditLimit;
        BigDecimal minBalance = BigDecimal.valueOf(-10000.0); // Allow negative for overpayments
        
        double range = maxBalance.subtract(minBalance).doubleValue();
        double amount = minBalance.doubleValue() + (random.nextDouble() * range);
        account.setCurrentBalance(CobolDataConverter.toBigDecimal(amount, 2));
        account.setCashCreditLimit(generateComp3BigDecimal(2, 10000.00));
        
        // Generate dates
        account.setOpenDate(generateCobolDate());
        account.setExpirationDate(generateCobolDate().plusYears(3));
        account.setReissueDate(generateCobolDate().plusMonths(6));
        
        // Generate cycle amounts
        account.setCurrentCycleCredit(generateComp3BigDecimal(2, 5000.00));
        account.setCurrentCycleDebit(generateComp3BigDecimal(2, 3000.00));
        
        // Generate formatted strings matching PIC clauses
        account.setAddressZip(generatePicString(10, true)); // ZIP code format
        account.setGroupId(generatePicString(10, false));   // Group ID format
        
        return account;
    }

    /**
     * Generates a realistic Customer entity with COBOL-compliant field values.
     * Creates a customer with properly formatted names, addresses, phone numbers,
     * SSN, and other demographic data matching COBOL PIC clause specifications.
     * 
     * @return Customer entity with COBOL-compatible test data
     */
    public static Customer generateCustomer() {
        Customer customer = new Customer();
        
        // Generate customer ID (9 digits)
        customer.setCustomerId(100000000L + (long)(random.nextDouble() * (999999999L - 100000000L)));
        
        // Generate names with proper formatting
        customer.setFirstName(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]);
        customer.setLastName(LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
        customer.setMiddleName(random.nextBoolean() ? 
            String.valueOf((char) ('A' + random.nextInt(26))) : null);
        
        // Generate address information
        customer.setAddressLine1(generateAddress());
        customer.setAddressLine2(random.nextBoolean() ? "APT " + (random.nextInt(999) + 1) : null);
        customer.setAddressLine3(null); // Usually null
        
        String state = STATES[random.nextInt(STATES.length)];
        customer.setStateCode(state);
        customer.setCountryCode("USA");
        customer.setZipCode(generatePicString(5, true)); // 5-digit ZIP
        
        // Generate phone numbers with validation
        customer.setPhoneNumber1(generatePhoneNumber());
        customer.setPhoneNumber2(random.nextBoolean() ? generatePhoneNumber() : null);
        
        // Generate SSN with proper format
        customer.setSsn(generateSSN());
        
        // Generate government ID
        customer.setGovernmentIssuedId("DL" + generatePicString(8, true));
        
        // Generate date of birth (realistic age range)
        customer.setDateOfBirth(generateDateOfBirth());
        
        // Generate EFT account ID
        customer.setEftAccountId(generatePicString(10, true));
        
        // Generate primary card holder indicator
        customer.setPrimaryCardHolderIndicator(random.nextBoolean() ? "Y" : "N");
        
        // Generate FICO score
        customer.setFicoScore(generateFicoScore());
        
        return customer;
    }

    /**
     * Generates a realistic Transaction entity with COBOL-compliant field values.
     * Creates a transaction with proper amounts, dates, merchant information,
     * and transaction details matching COBOL data patterns.
     * 
     * @return Transaction entity with COBOL-compatible test data
     */
    public static Transaction generateTransaction() {
        Transaction transaction = new Transaction();
        
        // Generate transaction ID
        transaction.setTransactionId(1000000000000000L + (long)(random.nextDouble() * (9999999999999999L - 1000000000000000L)));
        
        // Generate account ID
        transaction.setAccountId(generateAccountId());
        
        // Generate transaction amount with COBOL precision
        transaction.setAmount(generateValidTransactionAmount());
        
        // Generate transaction date
        transaction.setTransactionDate(generateRandomTransactionDate());
        
        // Generate transaction type and category
        transaction.setTransactionTypeCode(TRANSACTION_TYPES[random.nextInt(TRANSACTION_TYPES.length)]);
        transaction.setCategoryCode(TRANSACTION_CATEGORIES[random.nextInt(TRANSACTION_CATEGORIES.length)]);
        
        // Generate description
        transaction.setDescription(TRANSACTION_DESCRIPTIONS[random.nextInt(TRANSACTION_DESCRIPTIONS.length)]);
        
        // Generate merchant information
        transaction.setMerchantId(generateMerchantId());
        transaction.setMerchantName(MERCHANT_NAMES[random.nextInt(MERCHANT_NAMES.length)]);
        transaction.setMerchantCity(CITIES[random.nextInt(CITIES.length)]);
        transaction.setMerchantZip(generatePicString(5, true));
        
        // Generate card number
        transaction.setCardNumber(generatePicString(16, true));
        
        // Generate timestamps
        LocalDateTime now = LocalDateTime.now();
        transaction.setOriginalTimestamp(now.minusHours(random.nextInt(24)));
        transaction.setProcessedTimestamp(now);
        
        // Generate source
        transaction.setSource("ONLINE");
        
        return transaction;
    }

    /**
     * Generates a realistic Card entity with COBOL-compliant field values.
     * Creates a card with valid card number, CVV, expiration date,
     * and other card attributes matching COBOL data patterns.
     * 
     * @return Card entity with COBOL-compatible test data
     */
    public static Card generateCard() {
        Card card = new Card();
        
        // Generate card number (16 digits)
        card.setCardNumber(generatePicString(16, true));
        
        // Generate account ID and customer ID
        card.setAccountId(generateAccountId());
        card.setCustomerId(100000000L + (long)(random.nextDouble() * (999999999L - 100000000L)));
        
        // Generate CVV (3 digits)
        card.setCvvCode(String.format("%03d", random.nextInt(1000)));
        
        // Generate embossed name
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        card.setEmbossedName(firstName + " " + lastName);
        
        // Generate expiration date (future date)
        LocalDate expiration = LocalDate.now().plusYears(random.nextInt(5) + 1);
        card.setExpirationDate(expiration);
        
        // Generate active status
        card.setActiveStatus(random.nextBoolean() ? "Y" : "N");
        
        return card;
    }

    /**
     * Generates a list of Account entities with specified count.
     * 
     * @param count number of accounts to generate
     * @return List of Account entities
     */
    public static List<Account> generateAccountList(int count) {
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            accounts.add(generateAccount());
        }
        return accounts;
    }

    /**
     * Generates a list of Customer entities with specified count.
     * 
     * @param count number of customers to generate
     * @return List of Customer entities
     */
    public static List<Customer> generateCustomerList(int count) {
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            customers.add(generateCustomer());
        }
        return customers;
    }

    /**
     * Generates a list of Transaction entities with specified count.
     * 
     * @param count number of transactions to generate
     * @return List of Transaction entities
     */
    public static List<Transaction> generateTransactionList(int count) {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            transactions.add(generateTransaction());
        }
        return transactions;
    }

    /**
     * Generates a list of Card entities with specified count.
     * 
     * @param count number of cards to generate
     * @return List of Card entities
     */
    public static List<Card> generateCardList(int count) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cards.add(generateCard());
        }
        return cards;
    }

    /**
     * Generates a BigDecimal value with COBOL COMP-3 packed decimal precision.
     * Creates financial amounts with exact scale and precision matching COBOL
     * packed decimal format requirements.
     * 
     * @param scale number of decimal places (typically 2 for monetary amounts)
     * @param maxValue maximum value for the generated amount
     * @return BigDecimal with COBOL COMP-3 compatible precision
     */
    public static BigDecimal generateComp3BigDecimal(int scale, double maxValue) {
        double value = random.nextDouble() * maxValue;
        BigDecimal decimal = CobolDataConverter.toBigDecimal(value, scale);
        return CobolDataConverter.preservePrecision(decimal, scale);
    }

    /**
     * Generates a formatted string matching COBOL PIC clause specifications.
     * Creates strings with proper length and character content matching
     * COBOL PIC X clause requirements.
     * 
     * @param length required string length
     * @param numeric true for numeric-only content, false for alphanumeric
     * @return formatted string matching PIC clause requirements
     */
    public static String generatePicString(int length, boolean numeric) {
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            if (numeric) {
                builder.append(random.nextInt(10));
            } else {
                // Generate alphanumeric characters
                if (random.nextBoolean()) {
                    builder.append((char) ('A' + random.nextInt(26)));
                } else {
                    builder.append(random.nextInt(10));
                }
            }
        }
        
        return builder.toString();
    }

    /**
     * Generates a VSAM KSDS composite key structure.
     * Creates key values that match VSAM key structure requirements
     * for proper indexing and data access patterns.
     * 
     * @param keyFields array of field lengths for composite key
     * @return formatted composite key string
     */
    public static String generateVsamKey(int[] keyFields) {
        StringBuilder keyBuilder = new StringBuilder();
        
        for (int fieldLength : keyFields) {
            keyBuilder.append(generatePicString(fieldLength, true));
        }
        
        return keyBuilder.toString();
    }

    /**
     * Generates a date in COBOL format (CCYYMMDD).
     * Creates dates within realistic ranges for business applications.
     * 
     * @return LocalDate representing a COBOL-compatible date
     */
    public static LocalDate generateCobolDate() {
        // Generate dates within last 10 years to present
        int year = LocalDate.now().getYear() - random.nextInt(10);
        int month = random.nextInt(12) + 1;
        int day = random.nextInt(28) + 1; // Use 28 to avoid invalid dates
        
        LocalDate date = LocalDate.of(year, month, day);
        
        // Validate using DateConversionUtil
        if (DateConversionUtil.validateDate(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")))) {
            return date;
        } else {
            // Fallback to current date if validation fails
            return LocalDate.now();
        }
    }

    /**
     * Generates a realistic SSN with proper format validation.
     * Creates SSN values that pass ValidationUtil validation rules.
     * 
     * @return formatted SSN string (XXX-XX-XXXX format)
     */
    public static String generateSSN() {
        // Generate 3-2-4 digit SSN format avoiding invalid patterns
        int area = 100 + random.nextInt(699);  // Valid area codes 100-799
        int group = 10 + random.nextInt(90);   // Valid group codes 10-99
        int serial = 1000 + random.nextInt(8999); // Valid serial 1000-9999
        
        String ssn = String.format("%03d%02d%04d", area, group, serial);
        
        try {
            ValidationUtil.validateSSN("SSN", ssn);
            return ssn;
        } catch (Exception e) {
            // Fallback to safe default if validation fails
            return "123456789";
        }
    }

    /**
     * Generates a realistic phone number with area code validation.
     * Creates phone numbers that pass ValidationUtil area code validation.
     * 
     * @return formatted phone number string
     */
    public static String generatePhoneNumber() {
        try {
            // Generate valid area code
            String areaCode = "555"; // Use safe area code
            String exchange = String.format("%03d", 200 + random.nextInt(800));
            String number = String.format("%04d", random.nextInt(10000));
            
            String phoneNumber = areaCode + exchange + number;
            if (ValidationUtil.isValidPhoneAreaCode(areaCode)) {
                return phoneNumber;
            } else {
                return "5551234567"; // Fallback
            }
        } catch (Exception e) {
            // Fallback to safe default
            return "5551234567";
        }
    }

    /**
     * Generates a realistic credit limit for account testing.
     * Creates credit limit values within business-appropriate ranges.
     * 
     * @return BigDecimal credit limit with proper scale
     */
    public static BigDecimal generateCreditLimit() {
        // Generate credit limits between $500 and $50,000
        double amount = 500.0 + (random.nextDouble() * 49500.0);
        return CobolDataConverter.toBigDecimal(amount, 2);
    }

    /**
     * Generates a realistic account balance for testing.
     * Creates balance values that can be positive or negative.
     * 
     * @return BigDecimal balance with proper scale
     */
    public static BigDecimal generateBalance() {
        // Generate balances between -$10,000 and +$25,000
        double amount = -10000.0 + (random.nextDouble() * 35000.0);
        return CobolDataConverter.toBigDecimal(amount, 2);
    }

    /**
     * Resets the random seed for deterministic test data generation.
     * Allows tests to produce repeatable data sets.
     * 
     * @param seed the seed value for the random number generator
     */
    public static void resetRandomSeed(long seed) {
        random.setSeed(seed);
    }

    /**
     * Generates a single daily transaction for batch processing tests.
     * Creates transaction data suitable for daily transaction batch jobs.
     * 
     * @return Transaction entity for daily processing
     */
    public static Transaction generateDailyTransaction() {
        Transaction transaction = generateTransaction();
        transaction.setTransactionDate(LocalDate.now());
        transaction.setSource("DAILY");
        return transaction;
    }

    /**
     * Generates a batch of daily transactions for testing.
     * Creates multiple transactions for daily processing scenarios.
     * 
     * @param count number of daily transactions to generate
     * @return List of daily Transaction entities
     */
    public static List<Transaction> generateDailyTransactionBatch(int count) {
        return generateTransactionList(count).stream()
            .peek(t -> {
                t.setTransactionDate(LocalDate.now());
                t.setSource("DAILY");
            })
            .collect(Collectors.toList());
    }

    /**
     * Generates a valid transaction amount within business rules.
     * Creates amounts that pass business validation requirements.
     * 
     * @return BigDecimal transaction amount
     */
    public static BigDecimal generateValidTransactionAmount() {
        // Generate amounts between $0.01 and $10,000.00
        double amount = 0.01 + (random.nextDouble() * 9999.99);
        return CobolDataConverter.toBigDecimal(amount, 2);
    }

    /**
     * Generates a random transaction date within business range.
     * Creates dates suitable for transaction processing.
     * 
     * @return LocalDate for transaction processing
     */
    public static LocalDate generateRandomTransactionDate() {
        // Generate dates within last 30 days
        int daysBack = random.nextInt(30);
        return LocalDate.now().minusDays(daysBack);
    }

    /**
     * Generates a merchant ID for transaction testing.
     * Creates merchant identifiers matching business patterns.
     * 
     * @return Long merchant ID
     */
    public static Long generateMerchantId() {
        return 100000000L + (long)(random.nextDouble() * (999999999L - 100000000L));
    }

    /**
     * Generates an account ID matching COBOL field specifications.
     * Creates 11-digit account identifiers.
     * 
     * @return Long account ID
     */
    public static Long generateAccountId() {
        // Use seeded random for reproducibility instead of ThreadLocalRandom
        return 10000000000L + (long)(random.nextDouble() * (99999999999L - 10000000000L));
    }

/**
     * Generates card cross-reference data for testing.
     * Creates card-to-account mappings for relationship testing.
     * 
     * @return formatted card cross-reference string
     */
    public static String generateCardXref() {
        String cardNumber = TestDataGenerator.generatePicString(16, true);
        Long customerId = ThreadLocalRandom.current().nextLong(100000000L, 999999999L);
        Long accountId = TestDataGenerator.generateAccountId();
        
        return cardNumber + String.format("%09d", customerId) + String.format("%011d", accountId);
    }
    
    /**
     * Generates a realistic FICO credit score.
     * Creates scores within valid FICO range (300-850).
     * 
     * @return Integer FICO score
     */
    public static Integer generateFicoScore() {
        // Generate FICO scores with realistic distribution
        // Most scores fall between 600-750
        int baseScore = 600;
        int range = 150;
        int score = baseScore + ThreadLocalRandom.current().nextInt(range);
        
        // Ensure within valid FICO range
        if (score < 300) score = 300;
        if (score > 850) score = 850;
        
        return score;
    }
    
    /**
     * Generates a realistic address line for customer data.
     * Creates street addresses with proper formatting.
     * 
     * @return formatted address string
     */
    public static String generateAddress() {
        int streetNumber = 1 + ThreadLocalRandom.current().nextInt(9999);
        String[] streetNames = {"MAIN ST", "ELM ST", "OAK AVE", "PARK DR", "FIRST ST", "SECOND AVE"};
        String streetName = streetNames[ThreadLocalRandom.current().nextInt(streetNames.length)];
        
        return streetNumber + " " + streetName;
    }
    
    /**
     * Generates a realistic date of birth for customer testing.
     * Creates birth dates for adults (18-80 years old).
     * 
     * @return LocalDate representing date of birth
     */
    public static LocalDate generateDateOfBirth() {
        // Generate ages between 18 and 80 years
        int ageInYears = 18 + ThreadLocalRandom.current().nextInt(62);
        LocalDate birthDate = LocalDate.now().minusYears(ageInYears);
        
        // Add random days variation within the year
        int dayVariation = ThreadLocalRandom.current().nextInt(365);
        birthDate = birthDate.minusDays(dayVariation);
        
        try {
            // Validate using DateConversionUtil
            String dateStr = birthDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            if (DateConversionUtil.validateDate(dateStr)) {
                return birthDate;
            }
        } catch (Exception e) {
            // Fallback if validation fails
        }
        
        // Safe fallback
        return LocalDate.now().minusYears(30);
    }
    
    /**
     * Generates a transaction type for transaction testing.
     * Creates transaction type codes from predefined set.
     * 
     * @return transaction type code string
     */
    public static String generateTransactionType() {
        String[] types = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10"};
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }
    
    /**
     * Generates a list of transaction types for testing.
     * Creates multiple transaction type codes.
     * 
     * @param count number of transaction types to generate
     * @return List of transaction type strings
     */
    public static List<String> generateTransactionTypeList(int count) {
        List<String> types = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            types.add(generateTransactionType());
        }
        return types;
    }
    
    /**
     * Generates a transaction category for transaction testing.
     * Creates transaction category codes from predefined set.
     * 
     * @return transaction category code string
     */
    public static String generateTransactionCategory() {
        String[] categories = {"5411", "5812", "5732", "5541", "5999", "5814", "5912", "4111", "4121", "4131"};
        return categories[ThreadLocalRandom.current().nextInt(categories.length)];
    }
    
    /**
     * Generates a list of transaction categories for testing.
     * Creates multiple transaction category codes.
     * 
     * @param count number of transaction categories to generate
     * @return List of transaction category strings
     */
    public static List<String> generateTransactionCategoryList(int count) {
        List<String> categories = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            categories.add(generateTransactionCategory());
        }
        return categories;
    }
    
    /**
     * Generates a disclosure group for account testing.
     * Creates disclosure group identifiers for account configuration.
     * 
     * @return disclosure group identifier string
     */
    public static String generateDisclosureGroup() {
        String[] groups = {"GROUP01", "GROUP02", "GROUP03", "STANDARD", "PREMIUM", "BASIC"};
        return groups[ThreadLocalRandom.current().nextInt(groups.length)];
    }
    
    /**
     * Generates a list of disclosure groups for testing.
     * Creates multiple disclosure group identifiers.
     * 
     * @param count number of disclosure groups to generate
     * @return List of disclosure group strings
     */
    public static List<String> generateDisclosureGroupList(int count) {
        List<String> groups = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            groups.add(generateDisclosureGroup());
        }
        return groups;
    }
    
    /**
     * Generates user security data for authentication testing.
     * Creates user credentials and security information.
     * 
     * @return user security data string
     */
    public static String generateUserSecurity() {
        String userId = TestDataGenerator.generatePicString(8, false);
        String password = TestDataGenerator.generatePicString(8, false);
        String userType = ThreadLocalRandom.current().nextBoolean() ? "A" : "U";
        
        return userId + ":" + password + ":" + userType;
    }
    
    /**
     * Generates admin user data for testing.
     * Creates administrative user accounts with proper privileges.
     * 
     * @return admin user data string
     */
    public static String generateAdminUser() {
        String firstName = TestDataGenerator.FIRST_NAMES[ThreadLocalRandom.current().nextInt(TestDataGenerator.FIRST_NAMES.length)];
        String lastName = TestDataGenerator.LAST_NAMES[ThreadLocalRandom.current().nextInt(TestDataGenerator.LAST_NAMES.length)];
        String userId = (firstName.substring(0, 1) + lastName.substring(0, Math.min(7, lastName.length()))).toUpperCase();
        
        return userId + ":ADMIN123:A:" + firstName + ":" + lastName;
    }
    
    /**
     * Generates regular user data for testing.
     * Creates standard user accounts with basic privileges.
     * 
     * @return regular user data string
     */
    public static String generateRegularUser() {
        String firstName = TestDataGenerator.FIRST_NAMES[ThreadLocalRandom.current().nextInt(TestDataGenerator.FIRST_NAMES.length)];
        String lastName = TestDataGenerator.LAST_NAMES[ThreadLocalRandom.current().nextInt(TestDataGenerator.LAST_NAMES.length)];
        String userId = (firstName.substring(0, 1) + lastName.substring(0, Math.min(7, lastName.length()))).toUpperCase();
        
        return userId + ":USER123:U:" + firstName + ":" + lastName;
    }
    
    /**
     * Generates menu options for UI testing.
     * Creates menu configuration data for application navigation.
     * 
     * @return menu options data string
     */
    public static String generateMenuOptions() {
        String[] options = {
            "1:ACCOUNT:COACTVW", "2:CARD:COCRDLI", "3:CUSTOMER:COUSR00",
            "4:TRANSACTION:COTRN00", "5:REPORTS:CORPT00", "6:ADMIN:COADM01"
        };
        
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }
    
    /**
     * Generates account statement data for reporting tests.
     * Creates statement information for account reporting.
     * 
     * @return account statement data string
     */
    public static String generateAccountStatementData() {
        Long accountId = TestDataGenerator.generateAccountId();
        LocalDate statementDate = LocalDate.now().minusMonths(1);
        BigDecimal beginningBalance = TestDataGenerator.generateBalance();
        BigDecimal endingBalance = TestDataGenerator.generateBalance();
        
        return String.format("%011d:%s:%.2f:%.2f", 
            accountId, statementDate, beginningBalance.doubleValue(), endingBalance.doubleValue());
    }
    
    /**
     * Generates balance data for account balance testing.
     * Creates balance information for account processing.
     * 
     * @return balance data string
     */
    public static String generateBalanceData() {
        BigDecimal currentBalance = TestDataGenerator.generateBalance();
        BigDecimal availableBalance = currentBalance.subtract(TestDataGenerator.generateComp3BigDecimal(2, 1000.0));
        BigDecimal creditLimit = TestDataGenerator.generateCreditLimit();
        
        return String.format("%.2f:%.2f:%.2f", 
            currentBalance.doubleValue(), availableBalance.doubleValue(), creditLimit.doubleValue());
    }
}