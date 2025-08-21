/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.Dispute;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.DisputeRepository;
import com.carddemo.repository.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

/**
 * Comprehensive unit test suite for DisputeManagementService validating complete dispute lifecycle
 * including dispute creation, provisional credit processing, chargeback workflows, merchant responses,
 * and regulatory compliance requirements.
 * 
 * This test class ensures the Spring Boot service maintains identical business logic behavior
 * to the original COBOL implementation while validating all modern dispute management requirements.
 * 
 * Test Coverage Areas:
 * - Dispute case creation and initialization
 * - Provisional credit issuance and reversal
 * - Chargeback processing workflows  
 * - Merchant response handling
 * - Dispute resolution and closure
 * - Regulatory timeline compliance
 * - Documentation and audit requirements
 * - Credit/debit account adjustments
 * - Dispute status tracking and transitions
 * - Escalation path processing
 * - Reporting obligations
 * - Error handling and edge cases
 * 
 * Mocking Strategy:
 * - DisputeRepository: All CRUD operations and custom queries
 * - ChargebackProcessor: Complete chargeback workflow methods
 * - TransactionRepository: Transaction lookup and validation
 * - AccountRepository: Account data access and balance operations
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DisputeManagementService - Comprehensive Unit Tests")
class DisputeManagementServiceTest {

    @InjectMocks
    private DisputeManagementService disputeManagementService;

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private ChargebackProcessor chargebackProcessor;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    // Test data fields
    private Transaction testTransaction;
    private Account testAccount;
    private Dispute testDispute;

    /**
     * Test data setup executed before each test method.
     * Creates comprehensive test data including transaction, account, and dispute objects
     * with realistic values matching production data patterns.
     */
    @BeforeEach
    void setUp() {
        // Initialize test transaction with comprehensive data
        testTransaction = Transaction.builder()
                .transactionId(1001L)
                .amount(new BigDecimal("250.00"))
                .accountId(2001L)
                .transactionDate(LocalDate.now().minusDays(15))
                .description("AMAZON.COM PURCHASE")
                .merchantName("AMAZON.COM")
                .merchantCity("SEATTLE")
                .merchantZip("98101")
                .cardNumber("4111111111111111")
                .originalTimestamp(LocalDateTime.now().minusDays(15))
                .processedTimestamp(LocalDateTime.now().minusDays(15))
                .transactionTypeCode("PU")
                .categoryCode("5311")
                .authorizationCode("AUTH01")
                .build();

        // Initialize test account with balance and limit information
        testAccount = Account.builder()
                .accountId(2001L)
                .activeStatus("Y")
                .currentBalance(new BigDecimal("1250.75"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("1500.00"))
                .openDate(LocalDate.now().minusYears(2))
                .currentCycleCredit(new BigDecimal("500.00"))
                .currentCycleDebit(new BigDecimal("1200.00"))
                .addressZip("12345")
                .groupId("GROUP001")
                .build();

        // Initialize test dispute with complete dispute case data
        testDispute = Dispute.builder()
                .disputeId(3001L)
                .transactionId(1001L)
                .accountId(2001L)
                .disputeType(DisputeManagementService.TYPE_UNAUTHORIZED)
                .status(DisputeManagementService.STATUS_OPENED)
                .createdDate(LocalDateTime.now())
                .provisionalCreditAmount(new BigDecimal("250.00"))
                .reasonCode(DisputeManagementService.REASON_UNAUTHORIZED)
                .description("Unauthorized transaction - card not present")
                .documentationRequired(true)
                .regulatoryDeadline(LocalDateTime.now().plusDays(60))
                .merchantId("MERCHANT001")
                .build();
    }

    /**
     * Nested test class for dispute creation functionality.
     * Tests all aspects of initiating new dispute cases including validation,
     * data integrity, and regulatory compliance requirements.
     */
    @Nested
    @DisplayName("Dispute Creation Tests")
    class DisputeCreationTests {

        @Test
        @DisplayName("Should create dispute successfully with valid transaction data")
        void testCreateDispute_Success() {
            // Given: Valid transaction and account data
            when(transactionRepository.findById(1001L)).thenReturn(Optional.of(testTransaction));
            when(accountRepository.findById(2001L)).thenReturn(Optional.of(testAccount));
            when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);

            // When: Creating a new dispute
            Dispute result = disputeManagementService.createDispute(
                    1001L, 
                    DisputeManagementService.TYPE_UNAUTHORIZED, 
                    DisputeManagementService.REASON_UNAUTHORIZED,
                    "Unauthorized transaction - card not present"
            );

            // Then: Dispute should be created successfully
            assertThat(result).isNotNull();
            assertThat(result.getTransactionId()).isEqualTo(1001L);
            assertThat(result.getDisputeType()).isEqualTo(DisputeManagementService.TYPE_UNAUTHORIZED);
            assertThat(result.getStatus()).isEqualTo(DisputeManagementService.STATUS_OPENED);
            assertThat(result.getCreatedDate()).isNotNull();

            // Verify repository interactions
            verify(transactionRepository).findById(1001L);
            verify(accountRepository).findById(2001L);
            verify(disputeRepository).save(any(Dispute.class));
        }

        @Test
        @DisplayName("Should set regulatory deadline based on dispute type")
        void testCreateDispute_SetsRegulatoryDeadline() {
            // Given: Valid transaction and account data
            when(transactionRepository.findById(1001L)).thenReturn(Optional.of(testTransaction));
            when(accountRepository.findById(2001L)).thenReturn(Optional.of(testAccount));
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> {
                Dispute dispute = invocation.getArgument(0);
                assertThat(dispute.getRegulatoryDeadline()).isNotNull();
                assertThat(dispute.getRegulatoryDeadline()).isAfter(LocalDateTime.now());
                return dispute;
            });

            // When: Creating dispute
            disputeManagementService.createDispute(
                    1001L, 
                    DisputeManagementService.TYPE_DUPLICATE, 
                    DisputeManagementService.REASON_DUPLICATE_PROCESSING,
                    "Duplicate transaction processing"
            );

            // Then: Regulatory deadline should be set appropriately
            verify(disputeRepository).save(any(Dispute.class));
        }

        @Test
        @DisplayName("Should throw exception when transaction not found")
        void testCreateDispute_TransactionNotFound() {
            // Given: Non-existent transaction
            when(transactionRepository.findById(1001L)).thenReturn(Optional.empty());

            // When & Then: Should throw IllegalArgumentException
            assertThatThrownBy(() -> {
                disputeManagementService.createDispute(
                        1001L, 
                        DisputeManagementService.TYPE_UNAUTHORIZED, 
                        DisputeManagementService.REASON_UNAUTHORIZED,
                        "Test dispute"
                );
            }).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Transaction not found");

            verify(disputeRepository, never()).save(any(Dispute.class));
        }

        @Test
        @DisplayName("Should validate dispute type and reason code combination")
        void testCreateDispute_ValidatesTypeReasonCombination() {
            // Given: Valid transaction but invalid type/reason combination
            when(transactionRepository.findById(1001L)).thenReturn(Optional.of(testTransaction));
            when(accountRepository.findById(2001L)).thenReturn(Optional.of(testAccount));

            // When & Then: Should validate type/reason combination
            assertThatThrownBy(() -> {
                disputeManagementService.createDispute(
                        1001L, 
                        DisputeManagementService.TYPE_UNAUTHORIZED, 
                        DisputeManagementService.REASON_DUPLICATE_PROCESSING, // Invalid combination
                        "Test dispute"
                );
            }).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Invalid dispute type and reason code combination");
        }
    }

    /**
     * Nested test class for provisional credit functionality.
     * Tests provisional credit issuance, account adjustments, and reversal scenarios.
     */
    @Nested
    @DisplayName("Provisional Credit Tests")
    class ProvisionalCreditTests {

        @Test
        @DisplayName("Should issue provisional credit successfully")
        void testIssueProvisionalCredit_Success() {
            // Given: Valid dispute and account data
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(accountRepository.findByIdForUpdate(2001L)).thenReturn(Optional.of(testAccount));
            when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            BigDecimal creditAmount = new BigDecimal("250.00");

            // When: Issuing provisional credit
            boolean result = disputeManagementService.issueProvisionalCredit(3001L, creditAmount);

            // Then: Provisional credit should be issued successfully
            assertThat(result).isTrue();

            // Verify account balance adjustment
            verify(accountRepository).findByIdForUpdate(2001L);
            verify(accountRepository).save(argThat(account -> 
                account.getCurrentBalance().equals(testAccount.getCurrentBalance().add(creditAmount))
            ));
            verify(disputeRepository).save(argThat(dispute -> 
                dispute.getProvisionalCreditAmount().equals(creditAmount)
            ));
        }

        @Test
        @DisplayName("Should not issue duplicate provisional credit")
        void testIssueProvisionalCredit_PreventsDuplicate() {
            // Given: Dispute already has provisional credit
            testDispute.setProvisionalCreditAmount(new BigDecimal("250.00"));
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));

            // When & Then: Should prevent duplicate credit issuance
            assertThatThrownBy(() -> {
                disputeManagementService.issueProvisionalCredit(3001L, new BigDecimal("250.00"));
            }).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Provisional credit already issued");

            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("Should reverse provisional credit when dispute resolved against customer")
        void testReverseProvisionalCredit_Success() {
            // Given: Dispute with existing provisional credit
            testDispute.setProvisionalCreditAmount(new BigDecimal("250.00"));
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(accountRepository.findByIdForUpdate(2001L)).thenReturn(Optional.of(testAccount));
            when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            // When: Reversing provisional credit
            boolean result = disputeManagementService.reverseProvisionalCredit(3001L);

            // Then: Credit should be reversed
            assertThat(result).isTrue();

            // Verify account balance adjustment (reverse the credit)
            verify(accountRepository).save(argThat(account -> 
                account.getCurrentBalance().equals(testAccount.getCurrentBalance().subtract(new BigDecimal("250.00")))
            ));
            verify(disputeRepository).save(argThat(dispute -> 
                dispute.getProvisionalCreditAmount().equals(BigDecimal.ZERO)
            ));
        }
    }

    /**
     * Nested test class for chargeback processing functionality.
     * Tests complete chargeback workflow including initiation, submission, and response handling.
     */
    @Nested
    @DisplayName("Chargeback Processing Tests")
    class ChargebackProcessingTests {

        @Test
        @DisplayName("Should process chargeback initiation successfully")
        void testProcessChargeback_InitiationSuccess() {
            // Given: Valid dispute ready for chargeback
            testDispute.setStatus(DisputeManagementService.STATUS_INVESTIGATING);
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(transactionRepository.findById(1001L)).thenReturn(Optional.of(testTransaction));
            
            // Mock ChargebackProcessor methods
            when(chargebackProcessor.initiateChargeback(anyString(), anyString(), anyString(), 
                    any(BigDecimal.class), anyString(), anyString())).thenReturn("CHARGEBACK-001");
            when(chargebackProcessor.updateStatus(anyString(), any(), anyString())).thenReturn(true);
            when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);

            // When: Processing chargeback
            boolean result = disputeManagementService.processChargeback(3001L, "4855", "Goods/Services not provided");

            // Then: Chargeback should be initiated successfully
            assertThat(result).isTrue();

            verify(chargebackProcessor).initiateChargeback(
                    eq("4111111111111111"), 
                    eq("1001"), 
                    eq("4855"),
                    eq(new BigDecimal("250.00")), 
                    eq("MERCHANT001"), 
                    eq("Goods/Services not provided")
            );
            verify(disputeRepository).save(argThat(dispute -> 
                dispute.getStatus().equals(DisputeManagementService.STATUS_CHARGEBACK_INITIATED)
            ));
        }

        @Test
        @DisplayName("Should handle chargeback submission to network")
        void testProcessChargeback_NetworkSubmission() {
            // Given: Chargeback ready for network submission
            testDispute.setStatus(DisputeManagementService.STATUS_CHARGEBACK_INITIATED);
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(chargebackProcessor.processResponse(anyString(), anyMap(), any(LocalDateTime.class))).thenReturn(true);
            when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);

            Map<String, Object> networkResponse = Map.of(
                    "status", "SUBMITTED",
                    "networkId", "NET-001",
                    "timestamp", LocalDateTime.now().toString()
            );

            // When: Processing network response
            boolean result = disputeManagementService.handleNetworkResponse(3001L, networkResponse);

            // Then: Network response should be processed
            assertThat(result).isTrue();
            verify(chargebackProcessor).processResponse(anyString(), eq(networkResponse), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should calculate settlement amounts correctly")
        void testProcessChargeback_SettlementCalculation() {
            // Given: Chargeback ready for settlement calculation
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(chargebackProcessor.calculateSettlement(anyString(), any(BigDecimal.class), 
                    anyString(), anyString())).thenReturn(new BigDecimal("235.50"));

            // When: Calculating settlement
            BigDecimal settlement = disputeManagementService.calculateChargebackSettlement(
                    3001L, "ACCEPT", "USD");

            // Then: Settlement should be calculated correctly
            assertThat(settlement).isEqualTo(new BigDecimal("235.50"));
            verify(chargebackProcessor).calculateSettlement(anyString(), eq(new BigDecimal("250.00")), 
                    eq("ACCEPT"), eq("USD"));
        }
    }

    /**
     * Nested test class for merchant response handling.
     * Tests processing of various merchant response types and automated decision making.
     */
    @Nested
    @DisplayName("Merchant Response Handling Tests")
    class MerchantResponseTests {

        @Test
        @DisplayName("Should handle merchant acceptance response")
        void testHandleMerchantResponse_Acceptance() {
            // Given: Dispute awaiting merchant response
            testDispute.setStatus(DisputeManagementService.STATUS_PENDING_MERCHANT_RESPONSE);
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(chargebackProcessor.handleMerchantResponse(anyString(), anyString(), 
                    anyMap(), any(LocalDateTime.class))).thenReturn("ACCEPTED");
            when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);

            Map<String, Object> merchantResponse = Map.of(
                    "responseType", "ACCEPT",
                    "settlementAmount", "250.00",
                    "responseDate", LocalDateTime.now().toString()
            );

            // When: Handling merchant acceptance
            boolean result = disputeManagementService.handleMerchantResponse(3001L, "ACCEPT", merchantResponse);

            // Then: Response should be processed and dispute resolved
            assertThat(result).isTrue();
            verify(chargebackProcessor).handleMerchantResponse(anyString(), eq("ACCEPT"), 
                    eq(merchantResponse), any(LocalDateTime.class));
            verify(disputeRepository).save(argThat(dispute -> 
                dispute.getStatus().equals(DisputeManagementService.STATUS_RESOLVED_MERCHANT)
            ));
        }

        @Test
        @DisplayName("Should handle merchant rejection with representment")
        void testHandleMerchantResponse_RejectionWithRepresentment() {
            // Given: Dispute with merchant rejection
            testDispute.setStatus(DisputeManagementService.STATUS_PENDING_MERCHANT_RESPONSE);
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(chargebackProcessor.handleMerchantResponse(anyString(), anyString(), 
                    anyMap(), any(LocalDateTime.class))).thenReturn("REPRESENTMENT");
            when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);

            Map<String, Object> merchantResponse = Map.of(
                    "responseType", "REJECT",
                    "representmentDocuments", Arrays.asList("DOC001", "DOC002"),
                    "disputeEvidence", "Transaction was authorized and completed"
            );

            // When: Handling merchant rejection with representment
            boolean result = disputeManagementService.handleMerchantResponse(3001L, "REJECT", merchantResponse);

            // Then: Response should trigger representment evaluation
            assertThat(result).isTrue();
            verify(disputeRepository).save(argThat(dispute -> 
                dispute.getStatus().equals(DisputeManagementService.STATUS_REPRESENTMENT_REVIEW)
            ));
        }

        @Test
        @DisplayName("Should validate merchant response timeline")
        void testHandleMerchantResponse_TimelineValidation() {
            // Given: Late merchant response
            testDispute.setStatus(DisputeManagementService.STATUS_PENDING_MERCHANT_RESPONSE);
            testDispute.setRegulatoryDeadline(LocalDateTime.now().minusDays(5)); // Overdue
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));

            Map<String, Object> lateResponse = Map.of(
                    "responseType", "ACCEPT",
                    "responseDate", LocalDateTime.now().toString()
            );

            // When & Then: Should reject late response
            assertThatThrownBy(() -> {
                disputeManagementService.handleMerchantResponse(3001L, "ACCEPT", lateResponse);
            }).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Response received after deadline");
        }
    }

    /**
     * Nested test class for dispute resolution functionality.
     * Tests various resolution scenarios and final disposition handling.
     */
    @Nested
    @DisplayName("Dispute Resolution Tests")
    class DisputeResolutionTests {

        @Test
        @DisplayName("Should resolve dispute in customer favor")
        void testResolveDispute_CustomerFavorResolution() {
            // Given: Dispute ready for resolution
            testDispute.setStatus(DisputeManagementService.STATUS_INVESTIGATING);
            testDispute.setProvisionalCreditAmount(new BigDecimal("250.00"));
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);

            // When: Resolving in customer favor
            boolean result = disputeManagementService.resolveDispute(3001L, 
                    DisputeManagementService.STATUS_RESOLVED_CUSTOMER, "Unauthorized transaction confirmed");

            // Then: Dispute should be resolved in customer favor
            assertThat(result).isTrue();
            verify(disputeRepository).save(argThat(dispute -> {
                assertThat(dispute.getStatus()).isEqualTo(DisputeManagementService.STATUS_RESOLVED_CUSTOMER);
                assertThat(dispute.getResolutionDate()).isNotNull();
                assertThat(dispute.getResolutionReason()).isEqualTo("Unauthorized transaction confirmed");
                return true;
            }));
        }

        @Test
        @DisplayName("Should resolve dispute in merchant favor with credit reversal")
        void testResolveDispute_MerchantFavorResolution() {
            // Given: Dispute with provisional credit to reverse
            testDispute.setStatus(DisputeManagementService.STATUS_INVESTIGATING);
            testDispute.setProvisionalCreditAmount(new BigDecimal("250.00"));
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(accountRepository.findByIdForUpdate(2001L)).thenReturn(Optional.of(testAccount));
            when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            // When: Resolving in merchant favor
            boolean result = disputeManagementService.resolveDispute(3001L, 
                    DisputeManagementService.STATUS_RESOLVED_MERCHANT, "Valid transaction confirmed");

            // Then: Dispute should be resolved with credit reversal
            assertThat(result).isTrue();
            verify(disputeRepository).save(argThat(dispute -> {
                assertThat(dispute.getStatus()).isEqualTo(DisputeManagementService.STATUS_RESOLVED_MERCHANT);
                assertThat(dispute.getProvisionalCreditAmount()).isEqualTo(BigDecimal.ZERO);
                return true;
            }));
            verify(accountRepository).save(any(Account.class)); // Credit reversal
        }

        @Test
        @DisplayName("Should close dispute with proper documentation")
        void testResolveDispute_DocumentationRequirements() {
            // Given: Dispute requiring documentation
            testDispute.setDocumentationRequired(true);
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);

            // When: Resolving with documentation
            boolean result = disputeManagementService.resolveDispute(3001L, 
                    DisputeManagementService.STATUS_CLOSED, "Documentation reviewed and case closed");

            // Then: Documentation requirements should be validated
            assertThat(result).isTrue();
            verify(disputeRepository).save(argThat(dispute -> {
                assertThat(dispute.getResolutionReason()).contains("Documentation reviewed");
                return true;
            }));
        }
    }

    /**
     * Nested test class for regulatory compliance validation.
     * Tests compliance with dispute processing timelines and requirements.
     */
    @Nested
    @DisplayName("Regulatory Compliance Tests")
    class RegulatoryComplianceTests {

        @Test
        @DisplayName("Should validate regulatory timeline compliance")
        void testValidateRegulatory_TimelineCompliance() {
            // Given: Dispute within regulatory timeline
            testDispute.setRegulatoryDeadline(LocalDateTime.now().plusDays(30));
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));

            // When: Validating regulatory compliance
            boolean result = disputeManagementService.validateRegulatory(3001L);

            // Then: Should be compliant
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should flag overdue disputes for compliance")
        void testValidateRegulatory_OverdueDisputes() {
            // Given: Overdue dispute
            testDispute.setRegulatoryDeadline(LocalDateTime.now().minusDays(5));
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));

            // When: Validating regulatory compliance
            boolean result = disputeManagementService.validateRegulatory(3001L);

            // Then: Should be non-compliant
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should enforce documentation requirements for specific dispute types")
        void testValidateRegulatory_DocumentationRequirements() {
            // Given: High-value dispute requiring documentation
            testDispute.setDisputeType(DisputeManagementService.TYPE_FRAUD);
            testDispute.setProvisionalCreditAmount(new BigDecimal("1500.00")); // High value
            testDispute.setDocumentationRequired(true);
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));

            // When: Validating documentation requirements
            boolean result = disputeManagementService.validateDocumentationRequirements(3001L);

            // Then: Should require documentation
            assertThat(result).isTrue();
            assertThat(testDispute.isDocumentationRequired()).isTrue();
        }

        @Test
        @DisplayName("Should calculate compliance metrics")
        void testValidateRegulatory_ComplianceMetrics() {
            // Given: List of disputes for compliance calculation
            List<Dispute> disputes = Arrays.asList(
                    createTestDispute(1L, LocalDateTime.now().minusDays(5), DisputeManagementService.STATUS_RESOLVED_CUSTOMER),
                    createTestDispute(2L, LocalDateTime.now().minusDays(10), DisputeManagementService.STATUS_OVERDUE),
                    createTestDispute(3L, LocalDateTime.now().minusDays(15), DisputeManagementService.STATUS_RESOLVED_MERCHANT)
            );
            when(disputeRepository.findByStatus(anyString())).thenReturn(disputes);

            // When: Calculating compliance metrics
            Map<String, Object> metrics = disputeManagementService.calculateComplianceMetrics();

            // Then: Metrics should be calculated correctly
            assertThat(metrics).containsKey("totalDisputes");
            assertThat(metrics).containsKey("onTimeResolutions");
            assertThat(metrics).containsKey("overdueCount");
            assertThat(metrics).containsKey("complianceRate");
        }
    }

    /**
     * Nested test class for dispute escalation functionality.
     * Tests escalation triggers, paths, and automated escalation processing.
     */
    @Nested
    @DisplayName("Dispute Escalation Tests")
    class DisputeEscalationTests {

        @Test
        @DisplayName("Should escalate dispute when timeline exceeded")
        void testEscalateDispute_TimelineEscalation() {
            // Given: Dispute exceeding investigation timeline
            testDispute.setCreatedDate(LocalDateTime.now().minusDays(45)); // Exceeds 30-day limit
            testDispute.setStatus(DisputeManagementService.STATUS_INVESTIGATING);
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);

            // When: Checking for escalation
            boolean result = disputeManagementService.escalateDispute(3001L, "TIMELINE_EXCEEDED");

            // Then: Dispute should be escalated
            assertThat(result).isTrue();
            verify(disputeRepository).save(argThat(dispute -> {
                assertThat(dispute.getStatus()).isEqualTo(DisputeManagementService.STATUS_ESCALATED);
                return true;
            }));
        }

        @Test
        @DisplayName("Should escalate high-value disputes to specialized team")
        void testEscalateDispute_HighValueEscalation() {
            // Given: High-value dispute
            testDispute.setProvisionalCreditAmount(new BigDecimal("2500.00"));
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);

            // When: Escalating based on amount
            boolean result = disputeManagementService.escalateDispute(3001L, "HIGH_VALUE");

            // Then: Should be escalated to specialized team
            assertThat(result).isTrue();
            verify(disputeRepository).save(argThat(dispute -> {
                assertThat(dispute.getEscalationLevel()).isEqualTo("HIGH_VALUE");
                return true;
            }));
        }

        @Test
        @DisplayName("Should escalate complex fraud disputes")
        void testEscalateDispute_FraudEscalation() {
            // Given: Complex fraud dispute
            testDispute.setDisputeType(DisputeManagementService.TYPE_FRAUD);
            testDispute.setReasonCode(DisputeManagementService.REASON_FRAUD_CARD_ABSENT);
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);

            // When: Escalating fraud case
            boolean result = disputeManagementService.escalateDispute(3001L, "FRAUD_INVESTIGATION");

            // Then: Should be escalated to fraud team
            assertThat(result).isTrue();
            verify(disputeRepository).save(argThat(dispute -> {
                assertThat(dispute.getEscalationLevel()).isEqualTo("FRAUD_INVESTIGATION");
                return true;
            }));
        }
    }

    /**
     * Helper method to create test dispute objects with specified parameters.
     */
    private Dispute createTestDispute(Long disputeId, LocalDateTime createdDate, String status) {
        return Dispute.builder()
                .disputeId(disputeId)
                .transactionId(1000L + disputeId)
                .accountId(2000L + disputeId)
                .disputeType(DisputeManagementService.TYPE_UNAUTHORIZED)
                .status(status)
                .createdDate(createdDate)
                .reasonCode(DisputeManagementService.REASON_UNAUTHORIZED)
                .build();
    }

    /**
     * Nested test class for error handling and edge cases.
     * Tests exception scenarios, validation failures, and boundary conditions.
     */
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void testRepositoryException_Handling() {
            // Given: Repository throws exception
            when(disputeRepository.findById(3001L)).thenThrow(new RuntimeException("Database connection error"));

            // When & Then: Should handle exception appropriately
            assertThatThrownBy(() -> {
                disputeManagementService.validateRegulatory(3001L);
            }).isInstanceOf(RuntimeException.class)
              .hasMessageContaining("Database connection error");
        }

        @Test
        @DisplayName("Should validate null inputs")
        void testNullInputValidation() {
            // When & Then: Should reject null inputs
            assertThatThrownBy(() -> {
                disputeManagementService.createDispute(null, null, null, null);
            }).isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> {
                disputeManagementService.issueProvisionalCredit(null, null);
            }).isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> {
                disputeManagementService.resolveDispute(null, null, null);
            }).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should handle concurrent modification scenarios")
        void testConcurrentModification_Handling() {
            // Given: Account locked by another transaction
            when(disputeRepository.findById(3001L)).thenReturn(Optional.of(testDispute));
            when(accountRepository.findByIdForUpdate(2001L)).thenReturn(Optional.empty()); // Locked

            // When & Then: Should handle locked account
            assertThatThrownBy(() -> {
                disputeManagementService.issueProvisionalCredit(3001L, new BigDecimal("250.00"));
            }).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Unable to lock account for update");
        }
    }
}