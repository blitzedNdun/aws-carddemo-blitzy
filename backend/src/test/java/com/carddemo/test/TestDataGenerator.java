package com.carddemo.test;

import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.TransactionCategory;
import com.carddemo.entity.TransactionCategoryBalance;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.carddemo.test.TestConstants.*;

/**
 * Utility class for generating test data that matches COBOL data patterns,
 * including COMP-3 packed decimal values and customer records for testing.
 * 
 * This class provides methods to create realistic test data that mirrors
 * the structure and constraints of the original COBOL CBCUS01C program
 * and CVCUS01Y copybook definitions.
 */
public class TestDataGenerator {
    
    private static Random random = new Random(12345L);
    
    // COBOL-style data patterns for realistic test generation
    private static final String[] FIRST_NAMES = {
        "JOHN", "MARY", "ROBERT", "PATRICIA", "MICHAEL", "JENNIFER", 
        "WILLIAM", "LINDA", "DAVID", "ELIZABETH"
    };
    
    private static final String[] LAST_NAMES = {
        "SMITH", "JOHNSON", "WILLIAMS", "BROWN", "JONES", "GARCIA",
        "MILLER", "DAVIS", "RODRIGUEZ", "MARTINEZ"
    };
    
    private static final String[] STREET_NAMES = {
        "MAIN ST", "OAK AVE", "PARK BLVD", "FIRST ST", "SECOND ST",
        "WASHINGTON AVE", "LINCOLN DR", "JEFFERSON WAY", "ADAMS ST", "MADISON AVE"
    };
    
    private static final String[] CITY_NAMES = {
        "SPRINGFIELD", "FRANKLIN", "RIVERSIDE", "FAIRVIEW", "GEORGETOWN",
        "MADISON", "WASHINGTON", "CHESTER", "CLINTON", "OAKLAND"
    };
    
    private static final String[] STATE_CODES = {
        "NY", "CA", "TX", "FL", "PA", "IL", "OH", "GA", "NC", "MI"
    };
    
    /**
     * Generates a complete Customer entity with realistic COBOL-compatible data.
     * Creates customer records that match the validation patterns and field
     * lengths defined in the CVCUS01Y copybook.
     *
     * @return a fully populated Customer entity for testing
     */
    public static Customer generateCustomer() {
        Customer customer = new Customer();
        
        // Generate customer ID as String (setCustomerId expects String parameter) - 9 digits as per TestConstants.CUSTOMER_ID_LENGTH
        customer.setCustomerId(String.valueOf(100000000L + random.nextInt(899999999))); // 9 digits: 100000000-999999999
        
        // Set names with COBOL field length constraints
        customer.setFirstName(getRandomElement(FIRST_NAMES));
        customer.setLastName(getRandomElement(LAST_NAMES));
        
        // Generate SSN in standard format (lowercase 's' to match entity)
        customer.setSsn(generateSSN());
        
        // Generate phone number (using phoneNumber1 field)
        customer.setPhoneNumber1(generatePhoneNumber());
        
        // Generate address components (separate address lines)
        String[] addressParts = generateAddress().split(" ", 2);
        customer.setAddressLine1(addressParts.length > 0 ? addressParts[0] : "123 MAIN ST");
        if (addressParts.length > 1) {
            customer.setAddressLine2(addressParts[1]);
        }
        
        // Set state code
        customer.setStateCode(getRandomElement(STATE_CODES));
        
        // Set date of birth
        customer.setDateOfBirth(generateDateOfBirth());
        
        // Set FICO score as Integer (matching Customer entity)
        customer.setFicoScore(generateFicoScore());
        
        return customer;
    }
    
    /**
     * Generates a list of Customer entities with COBOL-compatible data patterns.
     * Creates the specified number of customer records with realistic test data.
     *
     * @param count the number of customers to generate
     * @return List of Customer entities with realistic test data
     */
    public static List<Customer> generateCustomerList(int count) {
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            customers.add(generateCustomer());
        }
        return customers;
    }
    
    /**
     * Generates a single Account entity with COBOL-compatible data patterns.
     * Creates account with COBOL-compatible account number and balances.
     *
     * @param customer the customer to associate with this account
     * @return Account entity with realistic test data
     */
    public static Account generateAccount(Customer customer) {
        Account account = new Account();
        
        // Generate 11-digit account number in COBOL format (PIC 9(11))
        account.setAccountId(40000000000L + random.nextInt(999999999));
        
        // Generate credit limit first
        BigDecimal creditLimit = generateCreditLimit();
        account.setCreditLimit(creditLimit);
        
        // Generate balance ensuring it's not greater than credit limit
        BigDecimal currentBalance = generateAccountBalance();
        if (currentBalance.compareTo(creditLimit) > 0) {
            // Set balance to a value between 10% and 90% of credit limit
            double ratio = 0.1 + (random.nextDouble() * 0.8); // 10% to 90%
            currentBalance = creditLimit.multiply(new BigDecimal(ratio)).setScale(2, RoundingMode.HALF_UP);
        }
        account.setCurrentBalance(currentBalance);
        
        // Set account active status
        account.setActiveStatus("Y");
        
        // Set cash credit limit (ensure it doesn't exceed credit limit)
        BigDecimal cashCreditLimit = generateCashCreditLimit();
        if (cashCreditLimit.compareTo(creditLimit) > 0) {
            cashCreditLimit = creditLimit.multiply(new BigDecimal("0.8")); // Set to 80% of credit limit
        }
        account.setCashCreditLimit(cashCreditLimit);
        
        // Set account dates
        account.setOpenDate(LocalDate.now().minusYears(random.nextInt(5) + 1));
        account.setExpirationDate(LocalDate.now().plusYears(random.nextInt(5) + 1));
        
        // Associate with customer
        account.setCustomer(customer);
        
        return account;
    }
    
    /**
     * Generates a list of Account entities for testing customer relationships.
     * Creates accounts with COBOL-compatible account numbers and balances.
     *
     * @param customer the customer to associate with all accounts
     * @return list of Account entities with realistic test data
     */
    public static List<Account> generateAccountList(Customer customer) {
        List<Account> accounts = new ArrayList<>();
        int numAccounts = 1 + random.nextInt(3); // 1-3 accounts per customer
        
        for (int i = 0; i < numAccounts; i++) {
            Account account = generateAccount(customer);
            accounts.add(account);
        }
        
        return accounts;
    }
    
    /**
     * Generates a list of Account entities for testing - creates a test customer internally.
     * Creates accounts with COBOL-compatible account numbers and balances.
     *
     * @return list of Account entities with realistic test data
     */
    public static List<Account> generateAccountList() {
        Customer testCustomer = generateCustomer();
        return generateAccountList(testCustomer);
    }
    
    /**
     * Generates a valid Social Security Number in standard format.
     * Creates SSNs that pass all validation rules but are not real SSNs.
     * Follows ValidationUtil rules: area != 0,666,900-999; group != 00; serial != 0000
     *
     * @return formatted SSN string (9 digits)
     */
    public static String generateSSN() {
        // Generate valid SSN area (001-899, excluding 666)
        int area;
        do {
            area = 1 + random.nextInt(899);  // 1-899
        } while (area == 666);  // Exclude 666
        
        // Generate valid group (01-99, avoiding 00)
        int group = 1 + random.nextInt(99);    // 1-99, avoiding 00
        
        // Generate valid serial (0001-9999, avoiding 0000)
        int serial = 1 + random.nextInt(9999); // 1-9999, avoiding 0000
        
        return String.format("%03d%02d%04d", area, group, serial);
    }
    
    /**
     * Generates a valid US phone number in standard format.
     * Uses ValidationUtil to ensure area code validity and avoid validation errors.
     *
     * @return formatted phone number string XXX-XXX-XXXX
     */
    public static String generatePhoneNumber() {
        // Generate area code by trying random values until we get a valid one
        String areaCode;
        int attempts = 0;
        do {
            // Generate area code from 200-999 (avoid 0xx and 1xx)
            int areaCodeInt = 200 + random.nextInt(800);
            areaCode = String.format("%03d", areaCodeInt);
            attempts++;
            // Safety valve to prevent infinite loop
            if (attempts > 100) {
                areaCode = "555"; // Use a known valid area code as fallback
                break;
            }
        } while (!com.carddemo.util.ValidationUtil.isValidPhoneAreaCode(areaCode));
        
        // Generate valid exchange code (200-999, not starting with 0 or 1)
        int exchange = 200 + random.nextInt(800);
        
        // Generate subscriber number
        int subscriber = random.nextInt(10000);
        
        return String.format("%s%03d%04d", areaCode, exchange, subscriber); // Return 10 digits without dashes
    }
    
    /**
     * Resets the random seed for reproducible test data generation.
     * Useful for ensuring consistent test results across runs.
     *
     * @param seed the seed value for the random number generator
     */
    public static void resetRandomSeed(long seed) {
        random = new Random(seed);
    }
    
    /**
     * Generates a merchant ID for transaction testing.
     * Creates IDs that match COBOL merchant data patterns.
     *
     * @return formatted merchant ID string
     */
    public static String generateMerchantId() {
        // Generate merchant ID in format MRCXXXXXXX (7-digit numeric)
        return String.format("MRC%07d", random.nextInt(10000000));
    }
    
    /**
     * Generates a BigDecimal with COBOL COMP-3 precision characteristics.
     * Ensures proper scale and rounding for financial calculations.
     *
     * @param precision total number of digits
     * @param maxValue maximum value for generation
     * @return BigDecimal with COBOL-compatible precision
     */
    public static BigDecimal generateComp3BigDecimal(int scale, double maxValue) {
        double value = random.nextDouble() * maxValue;
        // Generate positive values for monetary amounts - no negative values to match test expectations
        return BigDecimal.valueOf(value).setScale(scale, COBOL_ROUNDING_MODE);
    }
    
    /**
     * Generates a string that matches COBOL PIC clause patterns.
     * Creates strings with proper length and character constraints.
     *
     * @param length the length of the string to generate
     * @param alphaOnly true for alphabetic only, false for alphanumeric
     * @return formatted string matching PIC clause requirements
     */
    public static String generatePicString(int length, boolean alphaOnly) {
        StringBuilder sb = new StringBuilder();
        String chars = alphaOnly ? "ABCDEFGHIJKLMNOPQRSTUVWXYZ" : "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return sb.toString();
    }
    
    /**
     * Generates a numeric-only string matching COBOL PIC 9 clause patterns.
     * Creates strings with only digits for numeric PIC clauses.
     *
     * @param length the length of the numeric string to generate
     * @return formatted numeric string matching PIC 9 requirements
     */
    public static String generateNumericString(int length) {
        if (length <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        
        // Ensure first digit is not zero for most realistic data
        if (length > 1) {
            sb.append(String.valueOf(random.nextInt(9) + 1)); // 1-9
            for (int i = 1; i < length; i++) {
                sb.append(String.valueOf(random.nextInt(10))); // 0-9
            }
        } else {
            sb.append(String.valueOf(random.nextInt(9) + 1)); // 1-9 for single digit
        }
        
        return sb.toString();
    }
    
    /**
     * Generates VSAM-style key values with composite key components.
     * Creates keys that match COBOL VSAM key structures.
     *
     * @param keyLengths array of lengths for each key component
     * @return formatted VSAM key string
     */
    public static String generateVsamKey(int[] keyLengths) {
        StringBuilder keyBuilder = new StringBuilder();
        
        for (int i = 0; i < keyLengths.length; i++) {
            // Generate numeric key component
            int componentLength = keyLengths[i];
            
            // Handle edge cases for component length
            if (componentLength <= 0) {
                continue; // Skip invalid lengths
            }
            
            // Use generateNumericString for better numeric key generation
            String keyComponent = generateNumericString(componentLength);
            keyBuilder.append(keyComponent); // Concatenate without separators to match VSAM key format
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * Generates a Transaction entity for testing.
     * Creates transactions with COBOL-compatible field values.
     *
     * @return Transaction entity with realistic test data
     */
    public static Transaction generateTransaction() {
        Transaction transaction = new Transaction();
        
        // Generate unique 16-digit transaction ID
        long transactionId = 1000000000000000L + Math.abs(random.nextLong() % 8999999999999999L);
        transaction.setTransactionId(transactionId);
        
        transaction.setAmount(generateComp3BigDecimal(7, 50000.0));
        transaction.setTransactionTypeCode(TEST_TRANSACTION_TYPE_CODE);
        transaction.setCategoryCode("PUCH");
        transaction.setSubcategoryCode("01");
        transaction.setTransactionDate(LocalDate.now());
        transaction.setAccountId(TEST_ACCOUNT_ID);
        
        // Generate unique card number instead of using constant
        transaction.setCardNumber(generateCardNumber());
        
        transaction.setDescription("Test Transaction");
        return transaction;
    }
    
    /**
     * Generates a list of Transaction entities for testing.
     * Creates multiple transactions with COBOL-compatible field values.
     *
     * @param count number of transactions to generate
     * @return List of Transaction entities with realistic test data
     */
    public static List<Transaction> generateTransactionList(int count) {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            transactions.add(generateTransaction());
        }
        return transactions;
    }
    
    /**
     * Generates a valid transaction amount with COBOL COMP-3 precision.
     * Creates amounts in the valid range for credit card transactions.
     *
     * @return BigDecimal transaction amount with 2 decimal places
     */
    public static BigDecimal generateValidTransactionAmount() {
        // Generate transaction amount between $1.00 and $10,000.00
        double amount = 1.0 + (random.nextDouble() * 9999.0);
        return new BigDecimal(amount).setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
    }
    

    
    /**
     * Generates a random transaction date within the last 30 days.
     * Used for creating realistic date ranges in transaction testing.
     *
     * @return LocalDate within the past 30 days
     */
    public static LocalDate generateRandomTransactionDate() {
        int daysAgo = random.nextInt(30);
        return LocalDate.now().minusDays(daysAgo);
    }
    
    /**
     * Generates a Card entity for testing.
     * Creates cards with COBOL-compatible field values.
     *
     * @return Card entity with realistic test data
     */
    public static Card generateCard() {
        Card card = new Card();
        
        // Generate unique card number (16 digits starting with 4000 for test data)
        String cardNumber = generateCardNumber();
        card.setCardNumber(cardNumber);
        
        card.setAccountId(TEST_ACCOUNT_ID);
        card.setCustomerId(TEST_CUSTOMER_ID_LONG);
        
        // Generate random CVV (3 digits)
        card.setCvvCode(String.format("%03d", random.nextInt(1000)));
        
        // Generate embossed name
        card.setEmbossedName("TEST CARDHOLDER");
        card.setExpirationDate(LocalDate.now().plusYears(3));
        card.setActiveStatus("Y");
        return card;
    }
    
    /**
     * Generates a unique 16-digit card number for testing.
     * Uses 4000 prefix (Visa test number pattern) with random remaining digits.
     *
     * @return 16-digit card number string
     */
    public static String generateCardNumber() {
        // Use 4000 prefix for Visa test cards, then generate 12 random digits
        StringBuilder cardNumber = new StringBuilder("4000");
        
        for (int i = 0; i < 12; i++) {
            cardNumber.append(random.nextInt(10));
        }
        
        return cardNumber.toString();
    }
    
    /**
     * Generates a complete address string with realistic components.
     * Creates addresses that match COBOL field length constraints.
     *
     * @return formatted address string
     */
    public static String generateAddress() {
        int streetNumber = 1 + random.nextInt(9999);
        String streetName = getRandomElement(STREET_NAMES);
        String city = getRandomElement(CITY_NAMES);
        String state = getRandomElement(STATE_CODES);
        int zipCode = 10000 + random.nextInt(90000);
        
        return String.format("%d %s, %s, %s %05d", 
            streetNumber, streetName, city, state, zipCode);
    }
    
    /**
     * Generates a realistic date of birth for adult customers.
     * Creates dates that represent customers aged 18-80 years.
     *
     * @return LocalDate representing date of birth
     */
    public static LocalDate generateDateOfBirth() {
        // Generate age between 18 and 80 years
        int age = 18 + random.nextInt(63);
        LocalDate today = LocalDate.now();
        LocalDate birthDate = today.minusYears(age);
        
        // Add random day variation within the birth year
        int dayOfYear = 1 + random.nextInt(365);
        return birthDate.withDayOfYear(dayOfYear);
    }
    
    /**
     * Generates a FICO score as BigDecimal.
     * Creates scores in the standard range (300-850).
     * Uses BigDecimal for precise COBOL numeric handling.
     *
     * @return BigDecimal FICO score between 300 and 850
     */
    private static BigDecimal generateFicoScore() {
        // Generate FICO score between 300 and 850
        return BigDecimal.valueOf(300 + random.nextInt(551));
    }
    
    /**
     * Generates account balance with COBOL COMP-3 precision.
     * Creates realistic account balances with proper decimal scaling.
     *
     * @return BigDecimal balance with 2 decimal places
     */
    private static BigDecimal generateAccountBalance() {
        // Generate balance between $10.00 and $50,000.00
        double balance = 10.0 + (random.nextDouble() * 49990.0);
        return new BigDecimal(balance).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Generates a credit limit with COBOL COMP-3 precision.
     * Creates realistic credit limits with proper decimal scaling.
     *
     * @return BigDecimal credit limit with 2 decimal places
     */
    private static BigDecimal generateCreditLimit() {
        // Generate credit limit between $500.00 and $25,000.00
        double creditLimit = 500.0 + (random.nextDouble() * 24500.0);
        return new BigDecimal(creditLimit).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Generates a cash credit limit with COBOL COMP-3 precision.
     * Creates realistic cash credit limits (usually lower than credit limit).
     *
     * @return BigDecimal cash credit limit with 2 decimal places
     */
    private static BigDecimal generateCashCreditLimit() {
        // Generate cash credit limit between $100.00 and $5,000.00
        double cashLimit = 100.0 + (random.nextDouble() * 4900.0);
        return new BigDecimal(cashLimit).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Helper method to select random element from array.
     *
     * @param array the array to select from
     * @return randomly selected element
     */
    private static String getRandomElement(String[] array) {
        return array[random.nextInt(array.length)];
    }

    /**
     * Generates an Account without requiring a Customer parameter.
     * Creates a test customer internally for compatibility with existing generateAccount(Customer) method.
     */
    public static Account generateAccount() {
        Customer testCustomer = generateCustomer();
        return generateAccount(testCustomer);
    }

    /**
     * Generates a TransactionCategory entity for testing.
     */
    public static TransactionCategory generateTransactionCategory() {
        TransactionCategory category = new TransactionCategory();
        category.setCategoryCode(String.format("%04d", random.nextInt(9999) + 1));
        category.setCategoryDescription("TEST CATEGORY " + category.getCategoryCode());
        return category;
    }

    /**
     * Generates a list of TransactionCategoryBalance records for testing.
     */
    public static java.util.List<com.carddemo.entity.TransactionCategoryBalance> generateTransactionCategoryBalanceList(
            Long accountId, String categoryCode, int count) {
        java.util.List<com.carddemo.entity.TransactionCategoryBalance> balances = new java.util.ArrayList<>();
        LocalDate baseDate = LocalDate.now();
        
        for (int i = 0; i < count; i++) {
            com.carddemo.entity.TransactionCategoryBalance.TransactionCategoryBalanceKey key = 
                    new com.carddemo.entity.TransactionCategoryBalance.TransactionCategoryBalanceKey(
                            accountId,
                            categoryCode,
                            baseDate.minusDays(i)
                    );
            com.carddemo.entity.TransactionCategoryBalance balance = 
                    new com.carddemo.entity.TransactionCategoryBalance(key);
            balance.setBalance(generateComp3BigDecimal(String.valueOf(100.00 + (i * 10.25))));
            balances.add(balance);
        }
        return balances;
    }

    /**
     * Generates BigDecimal with COBOL COMP-3 equivalent precision.
     */
    private static BigDecimal generateComp3BigDecimal(String amount) {
        return new BigDecimal(amount).setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
    }

    /**
     * Generates a DailyTransaction entity for testing.
     * Creates daily transactions with COBOL-compatible field values.
     *
     * @return DailyTransaction entity with realistic test data
     */
    public static com.carddemo.entity.DailyTransaction generateDailyTransaction() {
        com.carddemo.entity.DailyTransaction dailyTransaction = new com.carddemo.entity.DailyTransaction();
        
        // Set transaction ID as String
        dailyTransaction.setTransactionId("T" + String.format("%015d", random.nextInt(999999999)));
        
        // Set card number
        dailyTransaction.setCardNumber(TEST_CARD_NUMBER);
        
        // Set transaction amount with COBOL COMP-3 precision
        dailyTransaction.setTransactionAmount(generateComp3BigDecimal(7, 50000.0));
        
        // Set transaction date
        dailyTransaction.setTransactionDate(LocalDate.now());
        
        // Set transaction type code
        dailyTransaction.setTransactionTypeCode(TEST_TRANSACTION_TYPE_CODE);
        
        // Set category code
        dailyTransaction.setCategoryCode("PUCH");
        
        // Set account ID
        dailyTransaction.setAccountId(TEST_ACCOUNT_ID);
        
        // Set merchant information
        dailyTransaction.setMerchantId(Long.parseLong(generateMerchantId().substring(3))); // Extract numeric part
        dailyTransaction.setMerchantName("Test Merchant");
        
        // Set description
        dailyTransaction.setDescription("Test Daily Transaction");
        
        // Set processing status
        dailyTransaction.setProcessingStatus("NEW");
        
        return dailyTransaction;
    }
    
    /**
     * Generates a batch of transactions for testing.
     * Creates multiple Transaction objects for bulk testing scenarios.
     *
     * @param count Number of transactions to generate
     * @return List of Transaction objects
     */
    public static List<Transaction> generateTransactionBatch(int count) {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            transactions.add(generateTransaction());
        }
        return transactions;
    }
    
    /**
     * Generates a batch of daily transactions for batch processing tests.
     * Creates multiple DailyTransaction objects for bulk testing scenarios.
     *
     * @param count Number of daily transactions to generate
     * @return List of DailyTransaction objects
     */
    public static List<com.carddemo.entity.DailyTransaction> generateDailyTransactionBatch(int count) {
        List<com.carddemo.entity.DailyTransaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            transactions.add(generateDailyTransaction());
        }
        return transactions;
    }
}