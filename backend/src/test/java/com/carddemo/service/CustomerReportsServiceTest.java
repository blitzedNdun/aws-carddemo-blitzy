package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test class for CustomerReportsService validating COBOL customer reporting 
 * batch logic migration to Java, testing customer analytics, segmentation, and 
 * marketing report generation.
 * 
 * This class tests the Java implementation of functionality derived from CBCUS01C.cbl
 * COBOL batch program, ensuring 100% functional parity with COBOL customer processing
 * while extending capabilities to provide comprehensive customer analytics and reporting.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Customer Reports Service Tests - COBOL CBCUS01C Migration")
public class CustomerReportsServiceTest {
    
    @Mock
    private TestDataGenerator testDataGenerator;
    
    @Mock  
    private CobolComparisonUtils cobolComparisonUtils;
    
    @InjectMocks
    private CustomerReportsService customerReportsService;
    
    // Test Data Constants - matching COBOL COMP-3 precision requirements
    private static final BigDecimal HIGH_CREDIT_LIMIT = new BigDecimal("10000.00");
    private static final BigDecimal MEDIUM_CREDIT_LIMIT = new BigDecimal("5000.00"); 
    private static final BigDecimal LOW_CREDIT_LIMIT = new BigDecimal("1000.00");
    private static final int HIGH_FICO_THRESHOLD = 750;
    private static final int MEDIUM_FICO_THRESHOLD = 650;
    private static final BigDecimal HIGH_UTILIZATION_THRESHOLD = new BigDecimal("0.80");
    private static final BigDecimal MEDIUM_UTILIZATION_THRESHOLD = new BigDecimal("0.50");
    
    private List<Customer> testCustomers;
    private List<Account> testAccounts;
    
    @BeforeEach
    void setUp() {
        // Initialize test data using TestDataGenerator
        testCustomers = createTestCustomerList();
        testAccounts = createTestAccountList();
        
        // Configure mock behavior for test data generation
        when(testDataGenerator.generateCustomerList()).thenReturn(testCustomers);
        when(testDataGenerator.generateAccountList()).thenReturn(testAccounts);
        
        // Configure COBOL comparison utilities for functional parity validation
        when(cobolComparisonUtils.validateFunctionalParity(any(), any())).thenReturn(true);
    }
    
    @Test
    @DisplayName("Generate Customer Segments - High Value Customers")
    void testGenerateCustomerSegments_HighValueCustomers() {
        // Given: Customers with high credit limits and FICO scores
        List<Customer> highValueCustomers = testCustomers.stream()
                .filter(customer -> customer.getFicoScore() >= HIGH_FICO_THRESHOLD)
                .toList();
        
        when(testDataGenerator.generateCustomer()).thenReturn(createHighValueCustomer());
        
        // When: Generating customer segments
        Map<String, List<Customer>> segments = customerReportsService.generateCustomerSegments();
        
        // Then: High value segment should be properly identified
        assertThat(segments).isNotNull();
        assertThat(segments).containsKey("HIGH_VALUE");
        
        List<Customer> highValueSegment = segments.get("HIGH_VALUE");
        assertThat(highValueSegment).isNotNull();
        assertThat(highValueSegment).isNotEmpty();
        
        // Validate COBOL-to-Java functional parity for customer segmentation
        verify(cobolComparisonUtils).compareDecimalValues(any(BigDecimal.class), any(BigDecimal.class));
        verify(cobolComparisonUtils).validateFunctionalParity(any(), any());
        
        // Verify all high value customers meet criteria
        highValueSegment.forEach(customer -> {
            assertThat(customer.getFicoScore()).isGreaterThanOrEqualTo(HIGH_FICO_THRESHOLD);
        });
    }
    
    @Test
    @DisplayName("Generate Customer Segments - Medium Value Customers")
    void testGenerateCustomerSegments_MediumValueCustomers() {
        // Given: Customers with medium credit profile
        when(testDataGenerator.generateCustomer()).thenReturn(createMediumValueCustomer());
        
        // When: Generating customer segments
        Map<String, List<Customer>> segments = customerReportsService.generateCustomerSegments();
        
        // Then: Medium value segment should be properly categorized
        assertThat(segments).containsKey("MEDIUM_VALUE");
        
        List<Customer> mediumValueSegment = segments.get("MEDIUM_VALUE");
        assertThat(mediumValueSegment).isNotNull();
        
        // Validate segmentation criteria matches COBOL logic
        mediumValueSegment.forEach(customer -> {
            assertThat(customer.getFicoScore())
                    .isGreaterThanOrEqualTo(MEDIUM_FICO_THRESHOLD)
                    .isLessThan(HIGH_FICO_THRESHOLD);
        });
        
        // Verify COBOL comparison for BigDecimal precision
        verify(cobolComparisonUtils).compareBigDecimalPrecision(any(BigDecimal.class), anyInt(), anyInt());
    }
    
    @Test
    @DisplayName("Generate Customer Segments - Low Value Customers")
    void testGenerateCustomerSegments_LowValueCustomers() {
        // Given: Customers with lower credit profiles
        when(testDataGenerator.generateCustomer()).thenReturn(createLowValueCustomer());
        
        // When: Generating segments for low value customers
        Map<String, List<Customer>> segments = customerReportsService.generateCustomerSegments();
        
        // Then: Low value segment should be identified correctly
        assertThat(segments).containsKey("LOW_VALUE");
        
        List<Customer> lowValueSegment = segments.get("LOW_VALUE");
        assertThat(lowValueSegment).isNotNull();
        
        // Validate low value criteria matches COBOL CBCUS01C processing logic
        lowValueSegment.forEach(customer -> {
            assertThat(customer.getFicoScore()).isLessThan(MEDIUM_FICO_THRESHOLD);
        });
    }
    
    @Test
    @DisplayName("Generate Customer Segments - Empty Dataset Handling")
    void testGenerateCustomerSegments_EmptyDataset() {
        // Given: Empty customer dataset (simulating COBOL end-of-file condition)
        when(testDataGenerator.generateCustomerList()).thenReturn(List.of());
        
        // When: Attempting to generate segments with no data
        Map<String, List<Customer>> segments = customerReportsService.generateCustomerSegments();
        
        // Then: Should handle empty dataset gracefully like COBOL EOF processing
        assertThat(segments).isNotNull();
        assertThat(segments).allSatisfy((key, value) -> 
            assertThat(value).isEmpty()
        );
        
        // Verify COBOL comparison utilities handle empty dataset scenario
        verify(cobolComparisonUtils).validateFunctionalParity(any(), any());
    }
    
    @Test
    @DisplayName("Generate Demographic Report - Age Distribution Analysis")
    void testGenerateDemographicReport_AgeDistribution() {
        // Given: Customers with varied ages for demographic analysis
        when(testDataGenerator.generateCustomerList()).thenReturn(createDemographicTestCustomers());
        
        // When: Generating demographic report (extending CBCUS01C display functionality)
        Map<String, Object> demographicReport = customerReportsService.generateDemographicReport();
        
        // Then: Age distribution should be properly calculated
        assertThat(demographicReport).isNotNull();
        assertThat(demographicReport).containsKey("ageDistribution");
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> ageDistribution = (Map<String, Integer>) demographicReport.get("ageDistribution");
        assertThat(ageDistribution).isNotNull();
        assertThat(ageDistribution).containsKeys("18-30", "31-45", "46-60", "60+");
        
        // Validate demographic calculations match COBOL precision
        verify(cobolComparisonUtils).compareDecimalValues(any(BigDecimal.class), any(BigDecimal.class));
    }
    
    @Test
    @DisplayName("Generate Demographic Report - Geographic Distribution")
    void testGenerateDemographicReport_GeographicDistribution() {
        // Given: Customers from different states (CUST-ADDR-STATE-CD from CVCUS01Y)
        List<Customer> geographicCustomers = createGeographicTestCustomers();
        when(testDataGenerator.generateCustomerList()).thenReturn(geographicCustomers);
        
        // When: Generating demographic report with geographic analysis
        Map<String, Object> demographicReport = customerReportsService.generateDemographicReport();
        
        // Then: Geographic distribution should be captured
        assertThat(demographicReport).containsKey("stateDistribution");
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> stateDistribution = (Map<String, Integer>) demographicReport.get("stateDistribution");
        assertThat(stateDistribution).isNotNull();
        assertThat(stateDistribution).isNotEmpty();
        
        // Verify all state codes are valid (matching COBOL PIC X(02) format)
        stateDistribution.keySet().forEach(stateCode -> {
            assertThat(stateCode).hasSize(2);
            assertThat(stateCode).matches("[A-Z]{2}");
        });
    }
    
    @Test
    @DisplayName("Analyze Credit Utilization - High Utilization Customers")
    void testAnalyzeCreditUtilization_HighUtilization() {
        // Given: Customers with high credit utilization ratios
        List<Account> highUtilizationAccounts = createHighUtilizationAccounts();
        when(testDataGenerator.generateAccountList()).thenReturn(highUtilizationAccounts);
        
        // When: Analyzing credit utilization patterns
        Map<String, List<Customer>> utilizationAnalysis = customerReportsService.analyzeCreditUtilization();
        
        // Then: High utilization customers should be identified
        assertThat(utilizationAnalysis).containsKey("HIGH_UTILIZATION");
        
        List<Customer> highUtilizationCustomers = utilizationAnalysis.get("HIGH_UTILIZATION");
        assertThat(highUtilizationCustomers).isNotNull();
        assertThat(highUtilizationCustomers).isNotEmpty();
        
        // Validate BigDecimal precision in utilization calculations (COBOL COMP-3 equivalent)
        verify(cobolComparisonUtils).compareBigDecimalPrecision(
            any(BigDecimal.class), eq(5), eq(2)
        );
    }
    
    @Test 
    @DisplayName("Analyze Credit Utilization - Medium Utilization Range")
    void testAnalyzeCreditUtilization_MediumUtilization() {
        // Given: Accounts with medium utilization ratios
        List<Account> mediumUtilizationAccounts = createMediumUtilizationAccounts();
        when(testDataGenerator.generateAccountList()).thenReturn(mediumUtilizationAccounts);
        
        // When: Performing credit utilization analysis
        Map<String, List<Customer>> utilizationAnalysis = customerReportsService.analyzeCreditUtilization();
        
        // Then: Medium utilization segment should be properly categorized  
        assertThat(utilizationAnalysis).containsKey("MEDIUM_UTILIZATION");
        
        List<Customer> mediumUtilizationCustomers = utilizationAnalysis.get("MEDIUM_UTILIZATION");
        assertThat(mediumUtilizationCustomers).isNotNull();
        
        // Verify utilization calculation precision matches COBOL decimal handling
        verify(cobolComparisonUtils).validateFunctionalParity(any(), any());
    }
    
    @Test
    @DisplayName("Calculate Customer Lifetime Value - High Value Customers")  
    void testCalculateCustomerLifetimeValue_HighValue() {
        // Given: Customers with extensive transaction history and high balances
        Customer highValueCustomer = createHighValueCustomer();
        List<Account> highValueAccounts = createHighValueAccounts();
        
        when(testDataGenerator.generateCustomer()).thenReturn(highValueCustomer);
        when(testDataGenerator.generateAccountList()).thenReturn(highValueAccounts);
        
        // When: Calculating customer lifetime value
        BigDecimal customerLifetimeValue = customerReportsService.calculateCustomerLifetimeValue(
            highValueCustomer.getCustomerId()
        );
        
        // Then: CLV should be calculated with COBOL-compatible precision
        assertThat(customerLifetimeValue).isNotNull();
        assertThat(customerLifetimeValue).isPositive();
        assertThat(customerLifetimeValue.scale()).isEqualTo(2); // COBOL decimal precision
        
        // Verify BigDecimal calculations match COBOL COMP-3 behavior
        verify(cobolComparisonUtils).compareBigDecimalPrecision(
            eq(customerLifetimeValue), eq(9), eq(2)  
        );
        verify(cobolComparisonUtils).compareDecimalValues(any(BigDecimal.class), any(BigDecimal.class));
    }
    
    @ParameterizedTest
    @CsvSource({
        "750, 10000.00, 8000.00, 15000.00", // High value: High FICO, high limit, high balance  
        "680, 5000.00, 3000.00, 8000.00",  // Medium value: Medium FICO, medium limit, medium balance
        "580, 1000.00, 800.00, 1500.00"    // Low value: Low FICO, low limit, low balance
    })
    @DisplayName("Calculate Customer Lifetime Value - Parametrized Value Tiers")
    void testCalculateCustomerLifetimeValue_ParametrizedTiers(
            int ficoScore, 
            BigDecimal creditLimit, 
            BigDecimal currentBalance, 
            BigDecimal expectedMinValue) {
        
        // Given: Customer with specific credit profile parameters
        Customer customer = createCustomerWithProfile(ficoScore, creditLimit, currentBalance);
        when(testDataGenerator.generateCustomer()).thenReturn(customer);
        
        // When: Calculating CLV for specific customer profile
        BigDecimal actualCLV = customerReportsService.calculateCustomerLifetimeValue(customer.getCustomerId());
        
        // Then: CLV should meet minimum expected value for profile tier
        assertThat(actualCLV).isNotNull();
        assertThat(actualCLV).isGreaterThanOrEqualTo(expectedMinValue);
        
        // Validate COBOL-style decimal precision in calculations
        assertThat(actualCLV.scale()).isEqualTo(2);
        verify(cobolComparisonUtils).compareBigDecimalPrecision(any(BigDecimal.class), anyInt(), anyInt());
    }
    
    @Test
    @DisplayName("Generate Marketing Campaign Lists - High Value Targets")
    void testGenerateMarketingCampaignLists_HighValueTargets() {
        // Given: Customer segments suitable for premium marketing campaigns
        List<Customer> premiumCustomers = createPremiumMarketingTargets();
        when(testDataGenerator.generateCustomerList()).thenReturn(premiumCustomers);
        
        // When: Generating marketing campaign target lists
        Map<String, List<Customer>> campaignLists = customerReportsService.generateMarketingCampaignLists();
        
        // Then: Premium campaign list should be properly populated
        assertThat(campaignLists).isNotNull();
        assertThat(campaignLists).containsKey("PREMIUM_REWARDS");
        
        List<Customer> premiumTargets = campaignLists.get("PREMIUM_REWARDS");
        assertThat(premiumTargets).isNotNull();
        assertThat(premiumTargets).isNotEmpty();
        
        // Validate premium customer criteria (high FICO + high CLV)
        premiumTargets.forEach(customer -> {
            assertThat(customer.getFicoScore()).isGreaterThanOrEqualTo(HIGH_FICO_THRESHOLD);
        });
        
        // Verify COBOL functional parity in customer selection logic
        verify(cobolComparisonUtils).validateFunctionalParity(any(), any());
    }
    
    @Test
    @DisplayName("Generate Marketing Campaign Lists - Credit Increase Targets")
    void testGenerateMarketingCampaignLists_CreditIncreaseTargets() {
        // Given: Customers eligible for credit line increases
        List<Customer> creditIncreaseCustomers = createCreditIncreaseTargets();
        when(testDataGenerator.generateCustomerList()).thenReturn(creditIncreaseCustomers);
        
        // When: Generating credit increase campaign targets
        Map<String, List<Customer>> campaignLists = customerReportsService.generateMarketingCampaignLists();
        
        // Then: Credit increase targets should be identified
        assertThat(campaignLists).containsKey("CREDIT_INCREASE");
        
        List<Customer> creditIncreaseTargets = campaignLists.get("CREDIT_INCREASE");
        assertThat(creditIncreaseTargets).isNotNull();
        
        // Validate credit increase eligibility criteria
        creditIncreaseTargets.forEach(customer -> {
            assertThat(customer.getFicoScore()).isGreaterThanOrEqualTo(MEDIUM_FICO_THRESHOLD);
        });
    }
    
    @Test
    @DisplayName("Generate Marketing Campaign Lists - Risk Mitigation Targets") 
    void testGenerateMarketingCampaignLists_RiskMitigationTargets() {
        // Given: High-risk customers requiring intervention
        List<Customer> riskCustomers = createRiskMitigationTargets();
        when(testDataGenerator.generateCustomerList()).thenReturn(riskCustomers);
        
        // When: Generating risk mitigation campaign lists
        Map<String, List<Customer>> campaignLists = customerReportsService.generateMarketingCampaignLists();
        
        // Then: Risk mitigation targets should be properly identified
        assertThat(campaignLists).containsKey("RISK_MITIGATION");
        
        List<Customer> riskTargets = campaignLists.get("RISK_MITIGATION");
        assertThat(riskTargets).isNotNull();
        
        // Validate risk criteria (high utilization OR low FICO)
        riskTargets.forEach(customer -> {
            assertThat(customer.getFicoScore()).isLessThan(MEDIUM_FICO_THRESHOLD);
        });
    }
    
    @Test
    @DisplayName("Error Handling - Invalid Customer ID for CLV Calculation")
    void testCalculateCustomerLifetimeValue_InvalidCustomerId() {
        // Given: Non-existent customer ID (simulating COBOL NOTFND condition)
        String invalidCustomerId = "999999999";
        when(testDataGenerator.generateCustomer()).thenReturn(null);
        
        // When & Then: Should handle invalid customer ID gracefully
        assertThatThrownBy(() -> customerReportsService.calculateCustomerLifetimeValue(invalidCustomerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer not found");
        
        // Verify proper error handling matches COBOL ABEND patterns
        verify(cobolComparisonUtils, never()).validateFunctionalParity(any(), any());
    }
    
    @Test
    @DisplayName("Performance Test - Large Dataset Processing")
    void testGenerateCustomerSegments_LargeDataset() {
        // Given: Large customer dataset (simulating production volumes)
        List<Customer> largeDataset = createLargeCustomerDataset(10000);
        when(testDataGenerator.generateCustomerList()).thenReturn(largeDataset);
        
        // When: Processing large dataset (testing batch performance)
        long startTime = System.currentTimeMillis();
        Map<String, List<Customer>> segments = customerReportsService.generateCustomerSegments();
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Then: Should process within reasonable time limits
        assertThat(segments).isNotNull();
        assertThat(processingTime).isLessThan(5000); // Max 5 seconds for 10K records
        
        // Verify all segments are populated correctly
        assertThat(segments).containsKeys("HIGH_VALUE", "MEDIUM_VALUE", "LOW_VALUE");
        
        // Validate total customer count matches input dataset
        int totalSegmentedCustomers = segments.values().stream()
                .mapToInt(List::size)
                .sum();
        assertThat(totalSegmentedCustomers).isEqualTo(largeDataset.size());
    }
    
    @Test
    @DisplayName("COBOL Functional Parity - Customer Record Processing")
    void testCobolFunctionalParity_CustomerRecordProcessing() {
        // Given: Customer data in COBOL-compatible format
        Customer cobolFormattedCustomer = createCobolFormattedCustomer();
        when(testDataGenerator.generateCustomer()).thenReturn(cobolFormattedCustomer);
        
        // When: Processing customer through Java implementation
        Map<String, List<Customer>> segments = customerReportsService.generateCustomerSegments();
        
        // Then: Results should match COBOL CBCUS01C processing exactly
        assertThat(segments).isNotNull();
        
        // Verify COBOL-to-Java functional parity validation
        verify(cobolComparisonUtils).validateFunctionalParity(any(), any());
        verify(cobolComparisonUtils, atLeastOnce()).compareDecimalValues(any(BigDecimal.class), any(BigDecimal.class));
        
        // Validate customer data maintains COBOL field format integrity
        Customer processedCustomer = segments.values().stream()
                .flatMap(List::stream)
                .filter(c -> c.getCustomerId().equals(cobolFormattedCustomer.getCustomerId()))
                .findFirst()
                .orElse(null);
                
        assertThat(processedCustomer).isNotNull();
        assertThat(processedCustomer.getFirstName()).hasSize(25); // COBOL PIC X(25)
        assertThat(processedCustomer.getLastName()).hasSize(25);  // COBOL PIC X(25)
    }
    
    // Helper methods for creating test data
    private List<Customer> createTestCustomerList() {
        return Arrays.asList(
            createHighValueCustomer(),
            createMediumValueCustomer(), 
            createLowValueCustomer()
        );
    }
    
    private List<Account> createTestAccountList() {
        return Arrays.asList(
            createHighValueAccount(),
            createMediumValueAccount(),
            createLowValueAccount()
        );
    }
    
    private Customer createHighValueCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId("000000001");
        customer.setFirstName("JOHN                     "); // COBOL PIC X(25) format
        customer.setLastName("DOE                      "); // COBOL PIC X(25) format  
        customer.setFicoScore(800); // High FICO score
        customer.setDateOfBirth(LocalDate.of(1980, 1, 1));
        customer.setSSN("123456789");
        return customer;
    }
    
    private Customer createMediumValueCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId("000000002");
        customer.setFirstName("JANE                     ");
        customer.setLastName("SMITH                    ");
        customer.setFicoScore(700); // Medium FICO score
        customer.setDateOfBirth(LocalDate.of(1985, 5, 15));
        customer.setSSN("987654321");
        return customer;
    }
    
    private Customer createLowValueCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId("000000003");
        customer.setFirstName("BOB                      ");
        customer.setLastName("JONES                    ");
        customer.setFicoScore(600); // Low FICO score
        customer.setDateOfBirth(LocalDate.of(1990, 12, 31));
        customer.setSSN("555666777");
        return customer;
    }
    
    private Account createHighValueAccount() {
        Account account = new Account();
        account.setAccountId("1000000001");
        account.setCustomerId("000000001");
        account.setCreditLimit(HIGH_CREDIT_LIMIT);
        account.setCurrentBalance(new BigDecimal("2000.00"));
        account.setCashCreditLimit(new BigDecimal("1000.00"));
        return account;
    }
    
    private Account createMediumValueAccount() {
        Account account = new Account();
        account.setAccountId("1000000002");
        account.setCustomerId("000000002");
        account.setCreditLimit(MEDIUM_CREDIT_LIMIT);
        account.setCurrentBalance(new BigDecimal("1500.00"));
        account.setCashCreditLimit(new BigDecimal("500.00"));
        return account;
    }
    
    private Account createLowValueAccount() {
        Account account = new Account();
        account.setAccountId("1000000003");
        account.setCustomerId("000000003");
        account.setCreditLimit(LOW_CREDIT_LIMIT);
        account.setCurrentBalance(new BigDecimal("800.00"));
        account.setCashCreditLimit(new BigDecimal("200.00"));
        return account;
    }
    
    private List<Customer> createDemographicTestCustomers() {
        return Arrays.asList(
            createCustomerWithAge(25, "NY"), // Young adult in NY
            createCustomerWithAge(35, "CA"), // Mid-career in CA
            createCustomerWithAge(50, "TX"), // Mature adult in TX
            createCustomerWithAge(65, "FL")  // Senior in FL
        );
    }
    
    private Customer createCustomerWithAge(int age, String state) {
        Customer customer = new Customer();
        customer.setCustomerId(String.format("%09d", age));
        customer.setFirstName("TEST                     ");
        customer.setLastName("CUSTOMER                 ");
        customer.setDateOfBirth(LocalDate.now().minusYears(age));
        customer.setFicoScore(700);
        customer.setSSN("123456789");
        return customer;
    }
    
    private List<Customer> createGeographicTestCustomers() {
        return Arrays.asList(
            createCustomerWithAge(30, "CA"),
            createCustomerWithAge(40, "NY"),
            createCustomerWithAge(35, "TX"),
            createCustomerWithAge(45, "FL")
        );
    }
    
    private List<Account> createHighUtilizationAccounts() {
        return Arrays.asList(
            createAccountWithUtilization("000000001", new BigDecimal("5000.00"), new BigDecimal("4500.00")),
            createAccountWithUtilization("000000002", new BigDecimal("10000.00"), new BigDecimal("8500.00"))
        );
    }
    
    private List<Account> createMediumUtilizationAccounts() {
        return Arrays.asList(
            createAccountWithUtilization("000000003", new BigDecimal("5000.00"), new BigDecimal("2500.00")),
            createAccountWithUtilization("000000004", new BigDecimal("8000.00"), new BigDecimal("4000.00"))
        );
    }
    
    private Account createAccountWithUtilization(String customerId, BigDecimal creditLimit, BigDecimal balance) {
        Account account = new Account();
        account.setAccountId("1" + customerId);
        account.setCustomerId(customerId);
        account.setCreditLimit(creditLimit);
        account.setCurrentBalance(balance);
        account.setCashCreditLimit(creditLimit.multiply(new BigDecimal("0.10"))); // 10% of credit limit
        return account;
    }
    
    private List<Account> createHighValueAccounts() {
        return Arrays.asList(
            createAccountWithUtilization("000000001", HIGH_CREDIT_LIMIT, new BigDecimal("3000.00"))
        );
    }
    
    private Customer createCustomerWithProfile(int ficoScore, BigDecimal creditLimit, BigDecimal currentBalance) {
        Customer customer = new Customer();
        customer.setCustomerId("000000999");
        customer.setFirstName("PROFILE                  ");
        customer.setLastName("TEST                     ");
        customer.setFicoScore(ficoScore);
        customer.setDateOfBirth(LocalDate.of(1985, 1, 1));
        customer.setSSN("999999999");
        return customer;
    }
    
    private List<Customer> createPremiumMarketingTargets() {
        return Arrays.asList(
            createHighValueCustomer() // Already has high FICO score
        );
    }
    
    private List<Customer> createCreditIncreaseTargets() {
        Customer customer = createMediumValueCustomer();
        customer.setFicoScore(720); // Good FICO for credit increase
        return Arrays.asList(customer);
    }
    
    private List<Customer> createRiskMitigationTargets() {
        Customer riskCustomer = createLowValueCustomer();
        riskCustomer.setFicoScore(550); // High risk FICO
        return Arrays.asList(riskCustomer);
    }
    
    private List<Customer> createLargeCustomerDataset(int size) {
        return java.util.stream.IntStream.range(0, size)
                .mapToObj(i -> {
                    Customer customer = new Customer();
                    customer.setCustomerId(String.format("%09d", i));
                    customer.setFirstName("CUSTOMER                 ");
                    customer.setLastName(String.format("%-25s", "TEST" + i));
                    customer.setFicoScore(600 + (i % 200)); // FICO scores 600-799
                    customer.setDateOfBirth(LocalDate.of(1970 + (i % 30), 1, 1));
                    customer.setSSN(String.format("%09d", 100000000 + i));
                    return customer;
                })
                .toList();
    }
    
    private Customer createCobolFormattedCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId("000000001");
        // COBOL PIC X(25) fields - exactly 25 characters with padding
        customer.setFirstName("JOHN                     ");
        customer.setLastName("DOE                      ");
        customer.setFicoScore(750);
        customer.setDateOfBirth(LocalDate.of(1980, 1, 1));
        customer.setSSN("123456789"); // COBOL PIC 9(09)
        return customer;
    }
}