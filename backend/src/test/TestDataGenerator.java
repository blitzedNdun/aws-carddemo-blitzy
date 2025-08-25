package com.carddemo.test;

import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility class for generating test data that matches COBOL data patterns,
 * including COMP-3 packed decimal values and customer records for testing.
 * 
 * This class provides methods to create realistic test data that mirrors
 * the structure and constraints of the original COBOL CBCUS01C program
 * and CVCUS01Y copybook definitions.
 */
public class TestDataGenerator {
    
    private static final Random random = new Random();
    
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
     * Generates a list of Account entities for testing customer relationships.
     * Creates accounts with COBOL-compatible account numbers and balances.
     *
     * @return list of Account entities with realistic test data
     */
    public List<Account> generateAccountList() {
        List<Account> accounts = new ArrayList<>();
        int numAccounts = 1 + random.nextInt(3); // 1-3 accounts per customer
        
        for (int i = 0; i < numAccounts; i++) {
            Account account = new Account();
            
            // Generate 16-digit account number in COBOL format
            account.setAccountId(String.format("%016d", 4000000000000000L + random.nextInt(999999999)));
            
            // Generate balance with COBOL COMP-3 precision (2 decimal places)
            account.setAccountBalance(generateAccountBalance());
            
            // Set account type
            account.setAccountType(i == 0 ? "CHECKING" : "SAVINGS");
            
            accounts.add(account);
        }
        
        return accounts;
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
        return new BigDecimal(score).setScale(2, BigDecimal.ROUND_HALF_UP);
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
        return new BigDecimal(balance).setScale(2, BigDecimal.ROUND_HALF_UP);
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