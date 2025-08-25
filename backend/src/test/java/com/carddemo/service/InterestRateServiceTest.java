package com.carddemo.service;

import com.carddemo.entity.InterestRate;
import com.carddemo.entity.Account;
import com.carddemo.repository.InterestRateRepository;
import com.carddemo.service.InterestRateService;
import com.carddemo.service.NotificationService;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.TestConstants;
import com.carddemo.test.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit test class for InterestRateService that validates interest rate management 
 * including APR calculations, promotional rates, and rate adjustments.
 * 
 * This test class ensures functional parity with the legacy COBOL interest 
 * calculation program CBACT04C.cbl by validating:
 * - Interest rate calculations with COBOL-compatible precision
 * - APR to daily rate conversion matching COBOL COMP-3 behavior  
 * - Promotional rate application and expiration handling
 * - Rate change notifications and history tracking
 * - Compound interest formulas and grace period handling
 * - Rate tier determination based on account groups
 * - Regulatory compliance for rate changes
 * 
 * All BigDecimal operations use COBOL-compatible scale and rounding modes
 * to ensure identical precision with the mainframe implementation.
 */
@DisplayName("Interest Rate Service Tests")
public class InterestRateServiceTest extends AbstractBaseTest implements UnitTest {

    @Mock
    private InterestRateRepository interestRateRepository;
    
    @Mock 
    private NotificationService notificationService;
    
    @InjectMocks
    private InterestRateService interestRateService;
    
    private AutoCloseable closeable;
    
    // Test data constants matching COBOL precision requirements
    private static final BigDecimal TEST_APR_RATE = new BigDecimal("18.9900").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
    private static final BigDecimal TEST_PROMOTIONAL_RATE = new BigDecimal("2.9900").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
    private static final BigDecimal TEST_CURRENT_BALANCE = new BigDecimal("1250.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
    private static final BigDecimal TEST_CREDIT_LIMIT = new BigDecimal("5000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
    private static final String TEST_ACCOUNT_GROUP = "STANDARD";
    private static final Long TEST_RATE_ID = 1L;
    private static final Long TEST_ACCOUNT_ID = Long.valueOf(TestConstants.TEST_ACCOUNT_ID);
    
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        closeable = MockitoAnnotations.openMocks(this);
    }
    
    @AfterEach
    @Override  
    public void tearDown() {
        super.tearDown();
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            // Log exception but don't fail test cleanup
        }
    }
    
    @Nested
    @DisplayName("APR to Daily Rate Conversion Tests")
    class AprToDailyRateConversionTests {
        
        @Test
        @DisplayName("Should convert APR to daily rate with COBOL precision")
        void shouldConvertAprToDailyRateWithCobolPrecision() {
            // Given
            BigDecimal apr = TEST_APR_RATE;
            
            // When
            BigDecimal dailyRate = interestRateService.convertAprToDaily(apr);
            
            // Then
            BigDecimal expectedDailyRate = apr.divide(new BigDecimal("365"), TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            assertBigDecimalEquals(expectedDailyRate, dailyRate);
            validateCobolPrecision(dailyRate);
        }
        
        @Test
        @DisplayName("Should handle zero APR conversion")
        void shouldHandleZeroAprConversion() {
            // Given
            BigDecimal zeroApr = BigDecimal.ZERO.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            
            // When
            BigDecimal dailyRate = interestRateService.convertAprToDaily(zeroApr);
            
            // Then
            assertThat(dailyRate).isEqualTo(BigDecimal.ZERO.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        }
        
        @Test
        @DisplayName("Should handle high precision APR values")
        void shouldHandleHighPrecisionAprValues() {
            // Given
            BigDecimal highPrecisionApr = new BigDecimal("29.99999").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            
            // When  
            BigDecimal dailyRate = interestRateService.convertAprToDaily(highPrecisionApr);
            
            // Then
            BigDecimal expectedDailyRate = highPrecisionApr.divide(new BigDecimal("365"), TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            assertBigDecimalEquals(expectedDailyRate, dailyRate);
            validateCobolPrecision(dailyRate);
        }
        
        @Test
        @DisplayName("Should throw exception for null APR")
        void shouldThrowExceptionForNullApr() {
            // When & Then
            assertThatThrownBy(() -> interestRateService.convertAprToDaily(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("APR cannot be null");
        }
        
        @Test
        @DisplayName("Should throw exception for negative APR")
        void shouldThrowExceptionForNegativeApr() {
            // Given
            BigDecimal negativeApr = new BigDecimal("-5.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            
            // When & Then
            assertThatThrownBy(() -> interestRateService.convertAprToDaily(negativeApr))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("APR cannot be negative");
        }
    }
    
    @Nested
    @DisplayName("Daily Rate Calculation Tests")
    class DailyRateCalculationTests {
        
        @Test
        @DisplayName("Should calculate daily rate for standard account")
        void shouldCalculateDailyRateForStandardAccount() {
            // Given
            Account testAccount = createTestAccount();
            InterestRate interestRate = InterestRate.builder()
                .rateId(TEST_RATE_ID)
                .accountGroupId(TEST_ACCOUNT_GROUP)
                .currentApr(TEST_APR_RATE)
                .effectiveDate(LocalDate.now())
                .build();
                
            when(interestRateRepository.findByAccountGroupId(testAccount.getGroupId()))
                .thenReturn(List.of(interestRate));
            
            // When
            BigDecimal dailyRate = interestRateService.calculateDailyRate(testAccount);
            
            // Then
            BigDecimal expectedDailyRate = TEST_APR_RATE.divide(new BigDecimal("365"), TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            assertBigDecimalEquals(expectedDailyRate, dailyRate);
            
            verify(interestRateRepository).findByAccountGroupId(testAccount.getGroupId());
        }
        
        @Test
        @DisplayName("Should return zero daily rate when no interest rate found")
        void shouldReturnZeroDailyRateWhenNoInterestRateFound() {
            // Given
            Account testAccount = createTestAccount();
            when(interestRateRepository.findByAccountGroupId(testAccount.getGroupId()))
                .thenReturn(Collections.emptyList());
            
            // When
            BigDecimal effectiveRate = interestRateService.getEffectiveRate(testAccount.getAccountId().toString(), LocalDate.now());
            BigDecimal dailyRate = interestRateService.calculateDailyRate(effectiveRate);
            
            // Then
            assertThat(dailyRate).isEqualTo(BigDecimal.ZERO.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        }
    }
    
    @Nested
    @DisplayName("Promotional Rate Tests")
    class PromotionalRateTests {
        
        @Test
        @DisplayName("Should get promotional rate for eligible account")
        void shouldGetPromotionalRateForEligibleAccount() {
            // Given
            Account testAccount = createTestAccount();
            InterestRate promotionalRate = InterestRate.builder()
                .rateId(TEST_RATE_ID)
                .accountGroupId(TEST_ACCOUNT_GROUP)
                .promotionalRate(TEST_PROMOTIONAL_RATE)
                .effectiveDate(LocalDate.now().minusDays(30))
                .expirationDate(LocalDate.now().plusDays(60))
                .build();
                
            when(interestRateRepository.findPromotionalRates())
                .thenReturn(List.of(promotionalRate));
            
            // When
            BigDecimal result = interestRateService.getPromotionalRate(testAccount.getAccountId().toString(), "PROMO001", LocalDate.now());
            
            // Then
            assertThat(result).isEqualTo(TEST_PROMOTIONAL_RATE);
            
            verify(interestRateRepository).findPromotionalRates();
        }
        
        @Test
        @DisplayName("Should return empty when no promotional rate available")
        void shouldReturnEmptyWhenNoPromotionalRateAvailable() {
            // Given
            Account testAccount = createTestAccount();
            when(interestRateRepository.findPromotionalRates())
                .thenReturn(List.of());
            
            // When
            BigDecimal result = interestRateService.getPromotionalRate(testAccount.getAccountId().toString(), "PROMO001", LocalDate.now());
            
            // Then
            assertThat(result).isEqualTo(BigDecimal.ZERO);
        }
        
        @Test
        @DisplayName("Should select best promotional rate when multiple available")
        void shouldSelectBestPromotionalRateWhenMultipleAvailable() {
            // Given
            Account testAccount = createTestAccount();
            
            InterestRate higherRate = InterestRate.builder()
                .rateId(1L)
                .promotionalRate(new BigDecimal("4.99"))
                .effectiveDate(LocalDate.now().minusDays(30))
                .expirationDate(LocalDate.now().plusDays(60))
                .build();
                
            InterestRate lowerRate = InterestRate.builder()
                .rateId(2L) 
                .promotionalRate(TEST_PROMOTIONAL_RATE)
                .effectiveDate(LocalDate.now().minusDays(15))
                .expirationDate(LocalDate.now().plusDays(90))
                .build();
                
            when(interestRateRepository.findPromotionalRates())
                .thenReturn(List.of(higherRate, lowerRate));
            
            // When
            BigDecimal result = interestRateService.getPromotionalRate(testAccount.getAccountId().toString(), "PROMO001", LocalDate.now());
            
            // Then
            assertThat(result).isEqualTo(TEST_PROMOTIONAL_RATE);
        }
    }
    
    @Nested
    @DisplayName("Rate Adjustment Tests") 
    class RateAdjustmentTests {
        
        @Test
        @DisplayName("Should adjust rate for account with notification")
        void shouldAdjustRateForAccountWithNotification() {
            // Given
            Account testAccount = createTestAccount();
            BigDecimal newRate = new BigDecimal("16.50").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            LocalDate effectiveDate = LocalDate.now().plusDays(30);
            
            InterestRate existingRate = InterestRate.builder()
                .rateId(TEST_RATE_ID)
                .accountGroupId(TEST_ACCOUNT_GROUP)
                .currentApr(TEST_APR_RATE)
                .effectiveDate(LocalDate.now().minusMonths(6))
                .build();
                
            InterestRate updatedRate = InterestRate.builder()
                .rateId(TEST_RATE_ID)
                .accountGroupId(TEST_ACCOUNT_GROUP) 
                .currentApr(newRate)
                .effectiveDate(effectiveDate)
                .build();
            
            when(interestRateRepository.findByAccountGroupId(testAccount.getGroupId()))
                .thenReturn(Optional.of(existingRate));
            when(interestRateRepository.save(any(InterestRate.class)))
                .thenReturn(updatedRate);
            
            // When
            InterestRate result = interestRateService.adjustRateForAccount(testAccount, newRate, effectiveDate);
            
            // Then
            assertThat(result.getCurrentApr()).isEqualTo(newRate);
            assertThat(result.getEffectiveDate()).isEqualTo(effectiveDate);
            
            verify(interestRateRepository).findByAccountGroupId(testAccount.getGroupId());
            verify(interestRateRepository).save(any(InterestRate.class));
            verify(notificationService).sendRateChangeNotification(eq(testAccount), eq(TEST_APR_RATE), eq(newRate), eq(effectiveDate));
        }
        
        @Test
        @DisplayName("Should create new rate when none exists for account group")
        void shouldCreateNewRateWhenNoneExistsForAccountGroup() {
            // Given
            Account testAccount = createTestAccount();
            BigDecimal newRate = new BigDecimal("15.99").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            LocalDate effectiveDate = LocalDate.now().plusDays(15);
            
            when(interestRateRepository.findByAccountGroupId(testAccount.getGroupId()))
                .thenReturn(Collections.emptyList());
            
            InterestRate newInterestRate = InterestRate.builder()
                .accountGroupId(TEST_ACCOUNT_GROUP)
                .currentApr(newRate)
                .effectiveDate(effectiveDate)
                .build();
                
            when(interestRateRepository.save(any(InterestRate.class)))
                .thenReturn(newInterestRate);
            
            // When
            InterestRate result = interestRateService.adjustRateForAccount(testAccount, newRate, effectiveDate);
            
            // Then
            assertThat(result.getCurrentApr()).isEqualTo(newRate);
            assertThat(result.getEffectiveDate()).isEqualTo(effectiveDate);
            
            verify(interestRateRepository).findByAccountGroupId(testAccount.getGroupId());
            verify(interestRateRepository).save(any(InterestRate.class));
            verify(notificationService).sendRateChangeNotification(eq(testAccount), eq(BigDecimal.ZERO), eq(newRate), eq(effectiveDate));
        }
        
        @Test
        @DisplayName("Should validate rate change business rules")
        void shouldValidateRateChangeBusinessRules() {
            // Given
            Account testAccount = createTestAccount();
            BigDecimal invalidRate = new BigDecimal("50.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            LocalDate effectiveDate = LocalDate.now().plusDays(30);
            
            // When & Then
            assertThatThrownBy(() -> interestRateService.validateRateChange(testAccount, invalidRate, effectiveDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Interest rate exceeds maximum allowed");
        }
    }
    
    @Nested
    @DisplayName("Compound Interest Tests")
    class CompoundInterestTests {
        
        @Test
        @DisplayName("Should calculate compound interest with COBOL precision")
        void shouldCalculateCompoundInterestWithCobolPrecision() {
            // Given
            BigDecimal principal = TEST_CURRENT_BALANCE;
            BigDecimal dailyRate = TEST_APR_RATE.divide(new BigDecimal("365"), TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            int days = 30;
            
            // When
            BigDecimal compoundInterest = interestRateService.calculateCompoundInterest(principal, dailyRate, days);
            
            // Then
            // Formula: A = P(1 + r)^t - P
            BigDecimal onePlusRate = BigDecimal.ONE.add(dailyRate);
            BigDecimal compound = principal.multiply(onePlusRate.pow(days));
            BigDecimal expectedInterest = compound.subtract(principal).setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            
            assertBigDecimalEquals(expectedInterest, compoundInterest);
            validateCobolPrecision(compoundInterest);
        }
        
        @Test
        @DisplayName("Should handle zero days compound interest calculation")
        void shouldHandleZeroDaysCompoundInterestCalculation() {
            // Given
            BigDecimal principal = TEST_CURRENT_BALANCE;
            BigDecimal dailyRate = TEST_APR_RATE.divide(new BigDecimal("365"), TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            int days = 0;
            
            // When
            BigDecimal compoundInterest = interestRateService.calculateCompoundInterest(principal, dailyRate, days);
            
            // Then
            assertThat(compoundInterest).isEqualTo(BigDecimal.ZERO.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        }
        
        @Test
        @DisplayName("Should calculate compound interest for full year")
        void shouldCalculateCompoundInterestForFullYear() {
            // Given
            BigDecimal principal = new BigDecimal("1000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            BigDecimal dailyRate = TEST_APR_RATE.divide(new BigDecimal("365"), TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            int days = 365;
            
            // When
            BigDecimal compoundInterest = interestRateService.calculateCompoundInterest(principal, dailyRate, days);
            
            // Then
            // For daily compounding over a year, should be close to principal * APR
            BigDecimal approximateInterest = principal.multiply(TEST_APR_RATE.divide(new BigDecimal("100")));
            assertBigDecimalWithinTolerance(approximateInterest, compoundInterest, TestConstants.VALIDATION_THRESHOLDS.get("INTEREST_TOLERANCE"));
        }
    }
    
    @Nested
    @DisplayName("Rate Tier Determination Tests")
    class RateTierDeterminationTests {
        
        @Test
        @DisplayName("Should determine rate tier based on account balance")
        void shouldDetermineRateTierBasedOnAccountBalance() {
            // Given
            Account lowBalanceAccount = Account.builder()
                .accountId(TEST_ACCOUNT_ID)
                .currentBalance(new BigDecimal("500.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .creditLimit(TEST_CREDIT_LIMIT)
                .groupId(TEST_ACCOUNT_GROUP)
                .build();
            
            Account highBalanceAccount = Account.builder()
                .accountId(TEST_ACCOUNT_ID + 1)
                .currentBalance(new BigDecimal("4500.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .creditLimit(new BigDecimal("10000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .groupId("PREMIUM")
                .build();
            
            // When
            String lowBalanceTier = interestRateService.determineRateTier(lowBalanceAccount);
            String highBalanceTier = interestRateService.determineRateTier(highBalanceAccount);
            
            // Then
            assertThat(lowBalanceTier).isEqualTo("STANDARD");
            assertThat(highBalanceTier).isEqualTo("PREMIUM");
        }
        
        @Test
        @DisplayName("Should determine rate tier based on credit utilization")
        void shouldDetermineRateTierBasedOnCreditUtilization() {
            // Given
            Account lowUtilizationAccount = Account.builder()
                .accountId(TEST_ACCOUNT_ID)
                .currentBalance(new BigDecimal("250.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .creditLimit(TEST_CREDIT_LIMIT)
                .groupId(TEST_ACCOUNT_GROUP)
                .build();
            
            // When
            String tier = interestRateService.determineRateTier(lowUtilizationAccount);
            
            // Then
            assertThat(tier).isEqualTo("PREFERRED");
        }
    }
    
    @Nested
    @DisplayName("Grace Period Tests")
    class GracePeriodTests {
        
        @Test
        @DisplayName("Should apply grace period for new purchases")
        void shouldApplyGracePeriodForNewPurchases() {
            // Given
            Account testAccount = createTestAccount();
            LocalDate transactionDate = LocalDate.now();
            LocalDate statementDate = LocalDate.now().minusDays(15);
            
            // When
            boolean hasGracePeriod = interestRateService.applyGracePeriod(testAccount, transactionDate, statementDate);
            
            // Then
            assertThat(hasGracePeriod).isTrue();
        }
        
        @Test
        @DisplayName("Should not apply grace period when carrying balance")
        void shouldNotApplyGracePeriodWhenCarryingBalance() {
            // Given
            Account accountWithBalance = Account.builder()
                .accountId(TEST_ACCOUNT_ID)
                .currentBalance(TEST_CURRENT_BALANCE)
                .creditLimit(TEST_CREDIT_LIMIT)
                .groupId(TEST_ACCOUNT_GROUP)
                .build();
                
            LocalDate transactionDate = LocalDate.now();
            LocalDate statementDate = LocalDate.now().minusDays(15);
            
            // When
            boolean hasGracePeriod = interestRateService.applyGracePeriod(accountWithBalance, transactionDate, statementDate);
            
            // Then
            assertThat(hasGracePeriod).isFalse();
        }
        
        @Test
        @DisplayName("Should calculate grace period end date")
        void shouldCalculateGracePeriodEndDate() {
            // Given
            LocalDate statementDate = LocalDate.now();
            
            // When
            LocalDate gracePeriodEnd = interestRateService.calculateGracePeriodEndDate(statementDate);
            
            // Then
            assertThat(gracePeriodEnd).isEqualTo(statementDate.plusDays(21));
        }
    }
    
    @Nested
    @DisplayName("Rate History Tracking Tests")
    class RateHistoryTrackingTests {
        
        @Test
        @DisplayName("Should track rate history for account")
        void shouldTrackRateHistoryForAccount() {
            // Given
            Account testAccount = createTestAccount();
            LocalDate fromDate = LocalDate.now().minusMonths(12);
            LocalDate toDate = LocalDate.now();
            
            List<InterestRate> historicalRates = List.of(
                InterestRate.builder()
                    .rateId(1L)
                    .accountGroupId(TEST_ACCOUNT_GROUP)
                    .currentApr(new BigDecimal("17.99"))
                    .effectiveDate(fromDate)
                    .build(),
                InterestRate.builder()
                    .rateId(2L)
                    .accountGroupId(TEST_ACCOUNT_GROUP)
                    .currentApr(TEST_APR_RATE)
                    .effectiveDate(fromDate.plusMonths(6))
                    .build()
            );
            
            when(interestRateRepository.findRateHistory(testAccount.getGroupId(), fromDate, toDate))
                .thenReturn(historicalRates);
            
            // When
            List<InterestRate> history = interestRateService.trackRateHistory(testAccount, fromDate, toDate);
            
            // Then
            assertThat(history).hasSize(2);
            assertThat(history.get(0).getCurrentApr()).isEqualByComparingTo(new BigDecimal("17.99"));
            assertThat(history.get(1).getCurrentApr()).isEqualByComparingTo(TEST_APR_RATE);
            
            verify(interestRateRepository).findRateHistory(testAccount.getGroupId(), fromDate, toDate);
        }
        
        @Test
        @DisplayName("Should return empty history when no historical rates found")
        void shouldReturnEmptyHistoryWhenNoHistoricalRatesFound() {
            // Given
            Account testAccount = createTestAccount();
            LocalDate fromDate = LocalDate.now().minusMonths(6);
            LocalDate toDate = LocalDate.now();
            
            when(interestRateRepository.findRateHistory(testAccount.getGroupId(), fromDate, toDate))
                .thenReturn(List.of());
            
            // When
            List<InterestRate> history = interestRateService.trackRateHistory(testAccount, fromDate, toDate);
            
            // Then
            assertThat(history).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("Rate Change Validation Tests")
    class RateChangeValidationTests {
        
        @Test
        @DisplayName("Should validate rate change effective date")
        void shouldValidateRateChangeEffectiveDate() {
            // Given
            Account testAccount = createTestAccount();
            BigDecimal newRate = new BigDecimal("16.99").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            LocalDate futureEffectiveDate = LocalDate.now().plusDays(45);
            
            // When & Then
            assertThatCode(() -> interestRateService.validateRateChange(testAccount, newRate, futureEffectiveDate))
                .doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should reject rate change with past effective date")
        void shouldRejectRateChangeWithPastEffectiveDate() {
            // Given
            Account testAccount = createTestAccount();
            BigDecimal newRate = new BigDecimal("16.99").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            LocalDate pastEffectiveDate = LocalDate.now().minusDays(1);
            
            // When & Then
            assertThatThrownBy(() -> interestRateService.validateRateChange(testAccount, newRate, pastEffectiveDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Effective date cannot be in the past");
        }
        
        @Test
        @DisplayName("Should reject rate change with insufficient notice period")
        void shouldRejectRateChangeWithInsufficientNoticePeriod() {
            // Given
            Account testAccount = createTestAccount();
            BigDecimal newRate = new BigDecimal("25.99").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            LocalDate immediateEffectiveDate = LocalDate.now().plusDays(7);
            
            // When & Then
            assertThatThrownBy(() -> interestRateService.validateRateChange(testAccount, newRate, immediateEffectiveDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rate increase requires 45 days advance notice");
        }
        
        @Test
        @DisplayName("Should allow rate decreases with shorter notice period")
        void shouldAllowRateDecreasesWithShorterNoticePeriod() {
            // Given
            Account testAccount = createTestAccount();
            BigDecimal lowerRate = new BigDecimal("12.99").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            LocalDate shortNoticeDate = LocalDate.now().plusDays(15);
            
            InterestRate existingRate = InterestRate.builder()
                .rateId(TEST_RATE_ID)
                .accountGroupId(TEST_ACCOUNT_GROUP)
                .currentApr(TEST_APR_RATE)
                .effectiveDate(LocalDate.now().minusMonths(6))
                .build();
                
            when(interestRateRepository.findByAccountGroupId(testAccount.getGroupId()))
                .thenReturn(Optional.of(existingRate));
            
            // When & Then
            assertThatCode(() -> interestRateService.validateRateChange(testAccount, lowerRate, shortNoticeDate))
                .doesNotThrowAnyException();
        }
    }
    
    @Nested
    @DisplayName("Rate Change Notification Tests")
    class RateChangeNotificationTests {
        
        @Test
        @DisplayName("Should notify customer of rate change")
        void shouldNotifyCustomerOfRateChange() {
            // Given
            Account testAccount = createTestAccount();
            BigDecimal oldRate = TEST_APR_RATE;
            BigDecimal newRate = new BigDecimal("21.99").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            LocalDate effectiveDate = LocalDate.now().plusDays(45);
            
            // When
            interestRateService.notifyRateChange(testAccount, oldRate, newRate, effectiveDate);
            
            // Then
            verify(notificationService).sendRateChangeNotification(testAccount, oldRate, newRate, effectiveDate);
        }
        
        @Test
        @DisplayName("Should send promotional rate expiry notification")
        void shouldSendPromotionalRateExpiryNotification() {
            // Given
            Account testAccount = createTestAccount();
            InterestRate expiringPromoRate = InterestRate.builder()
                .rateId(TEST_RATE_ID)
                .accountGroupId(TEST_ACCOUNT_GROUP)
                .promotionalRate(TEST_PROMOTIONAL_RATE)
                .effectiveDate(LocalDate.now().minusMonths(11))
                .expirationDate(LocalDate.now().plusDays(30))
                .build();
                
            List<InterestRate> expiringRates = List.of(expiringPromoRate);
            when(interestRateRepository.findPromotionalRates(eq(testAccount.getGroupId()), any(LocalDate.class)))
                .thenReturn(expiringRates);
            
            // When
            interestRateService.notifyPromotionalRateExpiry(testAccount);
            
            // Then
            verify(notificationService).sendPromotionalRateExpiry(testAccount, expiringPromoRate);
        }
        
        @Test
        @DisplayName("Should track notification history")
        void shouldTrackNotificationHistory() {
            // Given
            Account testAccount = createTestAccount();
            
            // When
            interestRateService.getNotificationHistory(testAccount);
            
            // Then
            verify(notificationService).trackNotificationHistory(testAccount);
        }
    }
    
    // Helper method to create test account matching COBOL data structure
    private Account createTestAccount() {
        return Account.builder()
            .accountId(TEST_ACCOUNT_ID)
            .currentBalance(TEST_CURRENT_BALANCE)
            .creditLimit(TEST_CREDIT_LIMIT)
            .groupId(TEST_ACCOUNT_GROUP)
            .build();
    }
    
    // Helper methods for test assertions with proper method signatures
    
    /**
     * Assert BigDecimal equality with COBOL precision (2-parameter version)
     */
    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        super.assertBigDecimalEquals(expected, actual, "BigDecimal values should be equal");
    }
    
    /**
     * Assert BigDecimal within tolerance with COBOL precision
     */
    private void assertBigDecimalWithinTolerance(BigDecimal expected, BigDecimal actual, Double tolerance) {
        super.assertBigDecimalWithinTolerance(expected, actual, 
            "BigDecimal values should be within tolerance: " + tolerance);
    }
    
    /**
     * Validate COBOL precision for financial calculations (1-parameter version)
     */
    private void validateCobolPrecision(BigDecimal value) {
        super.validateCobolPrecision(value, "financial calculation result");
    }
}