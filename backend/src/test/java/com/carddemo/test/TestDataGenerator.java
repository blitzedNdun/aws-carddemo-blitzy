package com.carddemo.test;

import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
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
    
    private Random random = new Random(12345L);
    
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
    public Customer generateCustomer() {
        Customer customer = new Customer();
        
        // Generate customer ID in COBOL format (10 digits)
        customer.setCustomerId(String.format("%010d", 1000000000L + random.nextInt(999999999)));
        
        // Set names with COBOL field length constraints
        customer.setFirstName(getRandomElement(FIRST_NAMES));
        customer.setLastName(getRandomElement(LAST_NAMES));
        
        // Generate SSN in standard format
        customer.setSSN(generateSSN());
        
        // Generate phone number
        customer.setPhoneNumber(generatePhoneNumber());
        
        // Generate address components
        customer.setAddress(generateAddress());
        
        // Set date of birth
        customer.setDateOfBirth(generateDateOfBirth());
        
        // Set credit score as BigDecimal matching COBOL COMP-3 precision
        customer.setCreditScore(generateCreditScore());
        
        return customer;
    }
    
    /**
     * Generates a single Account entity with COBOL-compatible data patterns.
     * Creates account with COBOL-compatible account number and balances.
     *
     * @param customer the customer to associate with this account
     * @return Account entity with realistic test data
     */
    public Account generateAccount(Customer customer) {
        Account account = new Account();
        
        // Generate 11-digit account number in COBOL format (PIC 9(11))
        account.setAccountId(40000000000L + random.nextInt(999999999));
        
        // Generate balance with COBOL COMP-3 precision (2 decimal places)
        account.setCurrentBalance(generateAccountBalance());
        
        // Set account active status
        account.setActiveStatus("Y");
        
        // Set credit limit
        account.setCreditLimit(generateCreditLimit());
        
        // Set cash credit limit  
        account.setCashCreditLimit(generateCashCreditLimit());
        
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
    public List<Account> generateAccountList(Customer customer) {
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
    public List<Account> generateAccountList() {
        Customer testCustomer = generateCustomer();
        return generateAccountList(testCustomer);
    }
    
    /**
     * Generates a valid Social Security Number in standard format.
     * Creates SSNs that pass basic validation rules but are not real SSNs.
     *
     * @return formatted SSN string (XXX-XX-XXXX)
     */
    public String generateSSN() {
        // Generate valid SSN format avoiding invalid patterns
        int area = 100 + random.nextInt(899);  // 100-999, avoiding 000
        int group = 1 + random.nextInt(99);    // 01-99, avoiding 00
        int serial = 1 + random.nextInt(9999); // 0001-9999, avoiding 0000
        
        return String.format("%03d-%02d-%04d", area, group, serial);
    }
    
    /**
     * Generates a valid US phone number in standard format.
     * Creates phone numbers with valid area codes and exchange codes.
     *
     * @return formatted phone number string (XXX) XXX-XXXX
     */
    public String generatePhoneNumber() {
        // Generate valid area code (not starting with 0 or 1)
        int areaCode = 200 + random.nextInt(800); // 200-999
        
        // Generate valid exchange code (not starting with 0 or 1)
        int exchange = 200 + random.nextInt(800);  // 200-999
        
        // Generate subscriber number
        int subscriber = random.nextInt(10000);    // 0000-9999
        
        return String.format("(%03d) %03d-%04d", areaCode, exchange, subscriber);
    }
    
    /**
     * Resets the random seed for reproducible test data generation.
     * Useful for ensuring consistent test results across runs.
     *
     * @param seed the seed value for the random number generator
     */
    public void resetRandomSeed(long seed) {
        this.random = new Random(seed);
    }
    
    /**
     * Generates a merchant ID for transaction testing.
     * Creates IDs that match COBOL merchant data patterns.
     *
     * @return formatted merchant ID string
     */
    public String generateMerchantId() {
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
    public BigDecimal generateComp3BigDecimal(int precision, double maxValue) {
        double value = random.nextDouble() * maxValue;
        if (random.nextBoolean() && value > 0) {
            value = -value; // Sometimes generate negative values
        }
        return BigDecimal.valueOf(value).setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
    }
    
    /**
     * Generates a string that matches COBOL PIC clause patterns.
     * Creates strings with proper length and character constraints.
     *
     * @param length the length of the string to generate
     * @param alphaOnly true for alphabetic only, false for alphanumeric
     * @return formatted string matching PIC clause requirements
     */
    public String generatePicString(int length, boolean alphaOnly) {
        StringBuilder sb = new StringBuilder();
        String chars = alphaOnly ? "ABCDEFGHIJKLMNOPQRSTUVWXYZ" : "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
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
    public String generateVsamKey(int[] keyLengths) {
        StringBuilder keyBuilder = new StringBuilder();
        
        for (int i = 0; i < keyLengths.length; i++) {
            if (i > 0) keyBuilder.append("-");
            
            // Generate numeric key component
            int componentLength = keyLengths[i];
            String format = "%0" + componentLength + "d";
            int maxValue = (int) Math.pow(10, componentLength) - 1;
            keyBuilder.append(String.format(format, random.nextInt(maxValue)));
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * Generates a Transaction entity for testing.
     * Creates transactions with COBOL-compatible field values.
     *
     * @return Transaction entity with realistic test data
     */
    public Object generateTransaction() {
        // Return a generic object since Transaction entity structure is not visible
        // In a real implementation, this would return a properly populated Transaction
        return new Object() {
            public String toString() {
                return String.format("Transaction[id=%s, amount=%s, type=%s]", 
                    TEST_TRANSACTION_ID,
                    generateComp3BigDecimal(7, 50000.0),
                    TEST_TRANSACTION_TYPE_CODE);
            }
        };
    }
    
    /**
     * Generates a Card entity for testing.
     * Creates cards with COBOL-compatible field values.
     *
     * @return Card entity with realistic test data
     */
    public Object generateCard() {
        // Return a generic object since Card entity structure is not visible
        // In a real implementation, this would return a properly populated Card
        return new Object() {
            public String toString() {
                return String.format("Card[number=%s, accountId=%s, customerId=%s]",
                    TEST_CARD_NUMBER,
                    TEST_ACCOUNT_ID,
                    TEST_CUSTOMER_ID);
            }
        };
    }
    
    /**
     * Generates a complete address string with realistic components.
     * Creates addresses that match COBOL field length constraints.
     *
     * @return formatted address string
     */
    public String generateAddress() {
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
    public LocalDate generateDateOfBirth() {
        // Generate age between 18 and 80 years
        int age = 18 + random.nextInt(63);
        LocalDate today = LocalDate.now();
        LocalDate birthDate = today.minusYears(age);
        
        // Add random day variation within the birth year
        int dayOfYear = 1 + random.nextInt(365);
        return birthDate.withDayOfYear(dayOfYear);
    }
    
    /**
     * Generates a credit score as BigDecimal with COBOL COMP-3 precision.
     * Creates scores in the standard range (300-850) with appropriate scaling.
     *
     * @return BigDecimal credit score with 2 decimal places
     */
    private BigDecimal generateCreditScore() {
        // Generate credit score between 300 and 850
        int score = 300 + random.nextInt(551);
        return new BigDecimal(score).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Generates account balance with COBOL COMP-3 precision.
     * Creates realistic account balances with proper decimal scaling.
     *
     * @return BigDecimal balance with 2 decimal places
     */
    private BigDecimal generateAccountBalance() {
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
    private BigDecimal generateCreditLimit() {
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
    private BigDecimal generateCashCreditLimit() {
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
    private String getRandomElement(String[] array) {
        return array[random.nextInt(array.length)];
    }
}