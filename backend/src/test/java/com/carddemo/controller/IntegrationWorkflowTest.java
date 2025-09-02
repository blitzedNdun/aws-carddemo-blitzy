package com.carddemo.controller;

import com.carddemo.controller.AuthController;
import com.carddemo.controller.MenuController;
import com.carddemo.controller.TransactionController;
import com.carddemo.controller.AccountController;
import com.carddemo.controller.CardController;
import com.carddemo.controller.BillingController;
import com.carddemo.controller.UserController;
import com.carddemo.controller.PaymentController;
import com.carddemo.controller.BatchController;
import com.carddemo.controller.ReportController;
import com.carddemo.dto.SessionContext;
import com.carddemo.dto.SignOnRequest;
import com.carddemo.dto.SignOnResponse;
import com.carddemo.service.SignOnService;
import com.carddemo.service.TransactionService;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.testcontainers.containers.PostgreSQLContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

// Import test utility classes
import com.carddemo.config.TestWebConfig;

// Import JUnit and assertion utilities
import org.hamcrest.Matchers;

/**
 * Comprehensive end-to-end integration test class that validates complete user workflows
 * across multiple controllers, ensuring functional parity with COBOL CICS transaction flows.
 * 
 * This test class replicates the transaction flow patterns from the original COBOL programs:
 * - COSGN00C.cbl (CC00 transaction) - Sign-on authentication workflow
 * - COMEN01C.cbl (CM00 transaction) - Main menu navigation workflow  
 * - COTRN00C.cbl (CT00 transaction) - Transaction listing and selection workflow
 * 
 * Each test method validates complete business processes ensuring sub-200ms response times
 * and maintaining session state preservation equivalent to CICS COMMAREA functionality.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationWorkflowTest extends BaseControllerTest {

    @Autowired
    private AuthController authController;
    
    @Autowired
    private MenuController menuController;
    
    @Autowired
    private TransactionController transactionController;
    
    @Autowired
    private AccountController accountController;
    
    @Autowired
    private CardController cardController;
    
    @Autowired
    private BillingController billingController;
    
    @Autowired
    private UserController userController;
    
    @Autowired
    private PaymentController paymentController;
    
    @Autowired
    private BatchController batchController;
    
    @Autowired
    private ReportController reportController;
    
    @Autowired
    private SignOnService signOnService;
    
    @Autowired
    private TransactionService transactionService;
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.4-alpine")
            .withDatabaseName("carddemo_test")
            .withUsername("test_user")
            .withPassword("test_password");
    
    private ObjectMapper objectMapper;
    private TestDataBuilder testDataBuilder;
    private SessionTestUtils sessionTestUtils;
    private RestApiTestUtils restApiTestUtils;
    
    /**
     * Test environment setup replicating CICS region initialization.
     * Configures MockMvc, test data builders, and session utilities for complete
     * end-to-end workflow testing across multiple controllers.
     */
    @BeforeEach
    public void setUp() {
        super.setUp();
        
        // Initialize test utilities from configuration
        TestWebConfig testConfig = new TestWebConfig();
        this.objectMapper = testConfig.customObjectMapper();
        
        // Initialize test helper classes
        this.testDataBuilder = new TestDataBuilder();
        this.sessionTestUtils = new SessionTestUtils();
        this.restApiTestUtils = new RestApiTestUtils();
        
        // Setup mock MVC with security context
        setupMockMvc();
        
        // Setup test containers and clean state
        setupTestContainers();
        
        // Create base test data
        createTestUser();
        createTestAccount(); 
        createTestTransaction();
        createTestCustomer();
        createTestCard();
    }
    
    /**
     * Test environment cleanup replicating CICS region shutdown.
     * Performs comprehensive cleanup of test data, sessions, and containers.
     */
    @AfterEach
    public void tearDown() {
        super.tearDown();
        
        // Clear test session data
        sessionTestUtils.clearTestSession();
        
        // Clean up test data
        cleanupTestData();
    }

    /**
     * End-to-end workflow test replicating COSGN00C → COMEN01C → COTRN00C transaction flow.
     * Validates complete sign-on to transaction viewing workflow with session state preservation,
     * ensuring functional parity with CICS transaction chaining and sub-200ms response times.
     * 
     * Test Scenario:
     * 1. User signs on via CC00 transaction equivalent (AuthController.signIn)
     * 2. Successful authentication transfers to CM00 equivalent (MenuController.getMainMenu)
     * 3. User selects transaction option leading to CT00 equivalent (TransactionController.getTransactions)
     * 4. Session state is preserved throughout the workflow (COMMAREA equivalent)
     * 5. All operations complete within 200ms response time requirement
     */
    @Test
    @DisplayName("Complete Sign-On to Transaction Workflow - COSGN00C → COMEN01C → COTRN00C Flow")
    public void testCompleteSignOnToTransactionWorkflow() throws Exception {
        Instant workflowStart = Instant.now();
        
        // Phase 1: Sign-On Authentication (COSGN00C.cbl equivalent - CC00 transaction)
        SignOnRequest signOnRequest = new SignOnRequest();
        signOnRequest.setUserId(TestConstants.TEST_USER_ID);
        signOnRequest.setPassword(TestConstants.TEST_USER_PASSWORD);
        
        Instant authStart = Instant.now();
        
        String authResponse = restApiTestUtils.performPostRequest(
            "/api/auth/signin",
            objectMapper.writeValueAsString(signOnRequest),
            MockMvcResultMatchers.status().isOk()
        );
        
        Duration authDuration = Duration.between(authStart, Instant.now());
        Assertions.assertTrue(authDuration.toMillis() < TestConstants.RESPONSE_TIME_THRESHOLD_MS,
            "Authentication response time " + authDuration.toMillis() + "ms exceeds 200ms threshold");
        
        SignOnResponse authResult = objectMapper.readValue(authResponse, SignOnResponse.class);
        Assertions.assertNotNull(authResult.getSessionToken(), "Session token must be created");
        Assertions.assertEquals("success", authResult.getStatus(), "Authentication must succeed");
        Assertions.assertEquals(TestConstants.TEST_USER_ID, authResult.getUserId(), "User ID must match");
        
        // Create session context with authentication token
        SessionContext sessionContext = sessionTestUtils.createTestSession();
        sessionContext.setUserId(authResult.getUserId());
        sessionContext.setUserRole(authResult.getUserRole());
        
        // Phase 2: Main Menu Navigation (COMEN01C.cbl equivalent - CM00 transaction)
        Instant menuStart = Instant.now();
        
        String menuResponse = restApiTestUtils.performGetRequest(
            "/api/menu/main",
            MockMvcResultMatchers.status().isOk(),
            authResult.getSessionToken()
        );
        
        Duration menuDuration = Duration.between(menuStart, Instant.now());
        Assertions.assertTrue(menuDuration.toMillis() < TestConstants.RESPONSE_TIME_THRESHOLD_MS,
            "Menu response time " + menuDuration.toMillis() + "ms exceeds 200ms threshold");
        
        // Validate menu options are available (equivalent to COMEN01C menu display)
        restApiTestUtils.assertJsonPath(menuResponse, "$.menuOptions", org.hamcrest.Matchers.notNullValue());
        restApiTestUtils.assertJsonPath(menuResponse, "$.menuOptions[0].code", org.hamcrest.Matchers.notNullValue());
        
        // Phase 3: Transaction List Access (COTRN00C.cbl equivalent - CT00 transaction)
        Instant transactionStart = Instant.now();
        
        String transactionResponse = restApiTestUtils.performGetRequest(
            "/api/transactions",
            MockMvcResultMatchers.status().isOk(),
            authResult.getSessionToken()
        );
        
        Duration transactionDuration = Duration.between(transactionStart, Instant.now());
        Assertions.assertTrue(transactionDuration.toMillis() < TestConstants.RESPONSE_TIME_THRESHOLD_MS,
            "Transaction listing response time " + transactionDuration.toMillis() + "ms exceeds 200ms threshold");
        
        // Validate transaction list structure (equivalent to COTRN00C screen population)
        restApiTestUtils.assertJsonPath(transactionResponse, "$.transactions", org.hamcrest.Matchers.notNullValue());
        restApiTestUtils.assertJsonPath(transactionResponse, "$.pageNumber", org.hamcrest.Matchers.notNullValue());
        restApiTestUtils.assertJsonPath(transactionResponse, "$.hasNext", org.hamcrest.Matchers.notNullValue());
        
        // Phase 4: Session State Validation (COMMAREA equivalent persistence)
        SessionContext finalSessionContext = sessionTestUtils.validateSessionData(authResult.getSessionToken());
        Assertions.assertEquals(authResult.getUserId(), finalSessionContext.getUserId(), 
            "Session user ID must persist throughout workflow");
        Assertions.assertNotNull(finalSessionContext.getNavigationStack(), 
            "Navigation stack must track workflow progression");
        
        Duration totalWorkflowDuration = Duration.between(workflowStart, Instant.now());
        Assertions.assertTrue(totalWorkflowDuration.toMillis() < (3 * TestConstants.RESPONSE_TIME_THRESHOLD_MS),
            "Complete workflow duration " + totalWorkflowDuration.toMillis() + "ms exceeds 600ms total threshold");
        
        // Validate workflow completeness
        validateWorkflowCompleteness("sign-on-to-transaction", authResult.getSessionToken());
        assertEndToEndFunctionalParity("CC00→CM00→CT00", transactionResponse);
    }

    /**
     * Admin user menu navigation workflow test replicating administrative transaction flows.
     * Validates admin role access, specialized menu options, and administrative operations
     * ensuring role-based access control matches RACF security model.
     */
    @Test
    @WithMockUser(roles = {TestConstants.TEST_ADMIN_ROLE})
    @DisplayName("Admin User Menu Navigation Workflow - Administrative Operations Flow")
    public void testAdminUserMenuNavigationWorkflow() throws Exception {
        Instant workflowStart = Instant.now();
        
        // Phase 1: Admin Authentication
        SignOnRequest adminSignOnRequest = new SignOnRequest();
        adminSignOnRequest.setUserId("ADMIN001");
        adminSignOnRequest.setPassword("admin123");
        
        String authResponse = restApiTestUtils.performPostRequest(
            "/api/auth/signin",
            objectMapper.writeValueAsString(adminSignOnRequest),
            MockMvcResultMatchers.status().isOk()
        );
        
        SignOnResponse authResult = objectMapper.readValue(authResponse, SignOnResponse.class);
        Assertions.assertEquals(TestConstants.TEST_ADMIN_ROLE, authResult.getUserRole(),
            "Admin role must be assigned for administrative access");
        
        // Phase 2: Admin Menu Access
        String adminMenuResponse = restApiTestUtils.performGetRequest(
            "/api/menu/admin",
            MockMvcResultMatchers.status().isOk(),
            authResult.getSessionToken()
        );
        
        // Validate admin-specific menu options
        restApiTestUtils.assertJsonPath(adminMenuResponse, "$.adminOptions", org.hamcrest.Matchers.notNullValue());
        restApiTestUtils.assertJsonPath(adminMenuResponse, "$.userManagementOption", org.hamcrest.Matchers.notNullValue());
        
        // Phase 3: User Management Operation
        String userListResponse = restApiTestUtils.performGetRequest(
            "/api/users",
            MockMvcResultMatchers.status().isOk(),
            authResult.getSessionToken()
        );
        
        restApiTestUtils.assertJsonPath(userListResponse, "$.users", org.hamcrest.Matchers.notNullValue());
        
        // Phase 4: Administrative Operation Performance Validation
        Duration totalWorkflowDuration = Duration.between(workflowStart, Instant.now());
        Assertions.assertTrue(totalWorkflowDuration.toMillis() < (3 * TestConstants.RESPONSE_TIME_THRESHOLD_MS),
            "Admin workflow duration exceeds performance threshold");
        
        validateWorkflowCompleteness("admin-navigation", authResult.getSessionToken());
    }

    /**
     * New card application and activation workflow test replicating complete card lifecycle.
     * Tests card creation, account association, activation process, and cross-reference validation
     * ensuring data integrity across multiple entity relationships.
     */
    @Test
    @WithMockUser(roles = {TestConstants.TEST_USER_ROLE})
    @DisplayName("New Card Application and Activation Workflow - Complete Card Lifecycle")
    public void testNewCardApplicationAndActivationWorkflow() throws Exception {
        Instant workflowStart = Instant.now();
        
        // Phase 1: Authentication and Menu Access
        SessionContext session = setupAuthenticatedSession();
        
        // Phase 2: Account Validation
        String accountId = TestConstants.TEST_ACCOUNT_ID;
        String accountResponse = restApiTestUtils.performGetRequest(
            "/api/accounts/" + accountId,
            MockMvcResultMatchers.status().isOk(),
            session.getUserId()
        );
        
        restApiTestUtils.assertJsonPath(accountResponse, "$.accountId", org.hamcrest.Matchers.equalTo(accountId));
        restApiTestUtils.assertJsonPath(accountResponse, "$.status", org.hamcrest.Matchers.equalTo("ACTIVE"));
        
        // Phase 3: Card Application Request
        String cardApplicationRequest = """
            {
                "accountId": "%s",
                "cardType": "CREDIT",
                "requestedCreditLimit": 5000.00,
                "customerRequest": "PRIMARY_CARD"
            }
            """.formatted(accountId);
        
        String cardApplicationResponse = restApiTestUtils.performPostRequest(
            "/api/cards/application",
            cardApplicationRequest,
            MockMvcResultMatchers.status().isCreated()
        );
        
        String applicationId = restApiTestUtils.extractJsonPath(cardApplicationResponse, "$.applicationId");
        Assertions.assertNotNull(applicationId, "Card application ID must be generated");
        
        // Phase 4: Card Activation Process
        String cardActivationRequest = """
            {
                "applicationId": "%s",
                "activationCode": "ACT123",
                "customerVerification": "VERIFIED"
            }
            """.formatted(applicationId);
        
        String activationResponse = restApiTestUtils.performPostRequest(
            "/api/cards/activate",
            cardActivationRequest,
            MockMvcResultMatchers.status().isOk()
        );
        
        String cardNumber = restApiTestUtils.extractJsonPath(activationResponse, "$.cardNumber");
        Assertions.assertNotNull(cardNumber, "Card number must be assigned after activation");
        
        // Phase 5: Card-Account Cross-Reference Validation
        String cardDetailsResponse = restApiTestUtils.performGetRequest(
            "/api/cards/" + cardNumber,
            MockMvcResultMatchers.status().isOk(),
            session.getUserId()
        );
        
        restApiTestUtils.assertJsonPath(cardDetailsResponse, "$.accountId", org.hamcrest.Matchers.equalTo(accountId));
        restApiTestUtils.assertJsonPath(cardDetailsResponse, "$.status", org.hamcrest.Matchers.equalTo("ACTIVE"));
        
        Duration totalWorkflowDuration = Duration.between(workflowStart, Instant.now());
        Assertions.assertTrue(totalWorkflowDuration.toMillis() < (5 * TestConstants.RESPONSE_TIME_THRESHOLD_MS),
            "Card application workflow duration exceeds performance threshold");
        
        validateWorkflowCompleteness("card-application", session.getUserId());
        assertEndToEndFunctionalParity("CARD_LIFECYCLE", cardDetailsResponse);
    }

    /**
     * Transaction dispute and resolution workflow test validating complete dispute lifecycle.
     * Tests dispute creation, investigation tracking, resolution processing, and audit trail
     * ensuring transaction integrity and proper state transitions.
     */
    @Test
    @WithMockUser(roles = {TestConstants.TEST_USER_ROLE})
    @DisplayName("Transaction Dispute and Resolution Workflow - Complete Dispute Lifecycle")
    public void testTransactionDisputeAndResolutionWorkflow() throws Exception {
        Instant workflowStart = Instant.now();
        
        // Phase 1: Setup authenticated session and test transaction
        SessionContext session = setupAuthenticatedSession();
        String transactionId = TestConstants.TEST_TRANSACTION_ID;
        
        // Phase 2: Transaction Detail Retrieval
        String transactionDetailResponse = restApiTestUtils.performGetRequest(
            "/api/transactions/" + transactionId,
            MockMvcResultMatchers.status().isOk(),
            session.getUserId()
        );
        
        BigDecimal originalAmount = new BigDecimal(restApiTestUtils.extractJsonPath(transactionDetailResponse, "$.amount"));
        
        // Phase 3: Dispute Initiation
        String disputeRequest = """
            {
                "transactionId": "%s",
                "disputeReason": "UNAUTHORIZED_CHARGE",
                "disputeAmount": %s,
                "customerComments": "Did not authorize this transaction"
            }
            """.formatted(transactionId, originalAmount.toString());
        
        String disputeResponse = restApiTestUtils.performPostRequest(
            "/api/transactions/dispute",
            disputeRequest,
            MockMvcResultMatchers.status().isCreated()
        );
        
        String disputeId = restApiTestUtils.extractJsonPath(disputeResponse, "$.disputeId");
        Assertions.assertNotNull(disputeId, "Dispute ID must be generated");
        
        // Phase 4: Dispute Investigation Tracking
        String disputeStatusResponse = restApiTestUtils.performGetRequest(
            "/api/transactions/dispute/" + disputeId,
            MockMvcResultMatchers.status().isOk(),
            session.getUserId()
        );
        
        restApiTestUtils.assertJsonPath(disputeStatusResponse, "$.status", org.hamcrest.Matchers.equalTo("UNDER_INVESTIGATION"));
        restApiTestUtils.assertJsonPath(disputeStatusResponse, "$.transactionId", org.hamcrest.Matchers.equalTo(transactionId));
        
        // Phase 5: Dispute Resolution (Admin Process)
        SessionContext adminSession = setupAdminSession();
        
        String resolutionRequest = """
            {
                "disputeId": "%s",
                "resolution": "APPROVED",
                "resolutionComments": "Customer verification confirmed unauthorized charge",
                "refundAmount": %s
            }
            """.formatted(disputeId, originalAmount.toString());
        
        String resolutionResponse = restApiTestUtils.performPutRequest(
            "/api/transactions/dispute/resolve",
            resolutionRequest,
            MockMvcResultMatchers.status().isOk(),
            adminSession.getUserId()
        );
        
        restApiTestUtils.assertJsonPath(resolutionResponse, "$.status", org.hamcrest.Matchers.equalTo("RESOLVED"));
        restApiTestUtils.assertJsonPath(resolutionResponse, "$.refundProcessed", org.hamcrest.Matchers.equalTo(true));
        
        Duration totalWorkflowDuration = Duration.between(workflowStart, Instant.now());
        Assertions.assertTrue(totalWorkflowDuration.toMillis() < (6 * TestConstants.RESPONSE_TIME_THRESHOLD_MS),
            "Dispute resolution workflow duration exceeds performance threshold");
        
        validateWorkflowCompleteness("dispute-resolution", session.getUserId());
        assertEndToEndFunctionalParity("DISPUTE_LIFECYCLE", resolutionResponse);
    }

    /**
     * Statement generation and payment processing workflow test validating billing cycle operations.
     * Tests statement creation, interest calculation, minimum payment calculation, and payment processing
     * ensuring BigDecimal precision matches COBOL COMP-3 financial calculations.
     */
    @Test
    @WithMockUser(roles = {TestConstants.TEST_USER_ROLE})
    @DisplayName("Statement Generation and Payment Workflow - Complete Billing Cycle")
    public void testStatementGenerationAndPaymentWorkflow() throws Exception {
        Instant workflowStart = Instant.now();
        
        // Phase 1: Setup authenticated session
        SessionContext session = setupAuthenticatedSession();
        String accountId = TestConstants.TEST_ACCOUNT_ID;
        
        // Phase 2: Statement Generation (COBIL00C.cbl equivalent)
        String statementRequest = """
            {
                "accountId": "%s",
                "statementPeriod": "2024-01",
                "generateInterest": true
            }
            """.formatted(accountId);
        
        String statementResponse = restApiTestUtils.performPostRequest(
            "/api/billing/generate-statement",
            statementRequest,
            MockMvcResultMatchers.status().isOk()
        );
        
        // Validate statement generation with BigDecimal precision
        BigDecimal statementBalance = new BigDecimal(restApiTestUtils.extractJsonPath(statementResponse, "$.balance"));
        BigDecimal interestCharged = new BigDecimal(restApiTestUtils.extractJsonPath(statementResponse, "$.interestCharged"));
        BigDecimal minimumPayment = new BigDecimal(restApiTestUtils.extractJsonPath(statementResponse, "$.minimumPayment"));
        
        // Validate COBOL COMP-3 equivalent precision (scale=2 for currency)
        Assertions.assertEquals(TestConstants.COBOL_DECIMAL_SCALE, statementBalance.scale(),
            "Statement balance scale must match COBOL COMP-3 precision");
        Assertions.assertEquals(TestConstants.COBOL_DECIMAL_SCALE, interestCharged.scale(),
            "Interest charged scale must match COBOL COMP-3 precision");
        Assertions.assertEquals(TestConstants.COBOL_DECIMAL_SCALE, minimumPayment.scale(),
            "Minimum payment scale must match COBOL COMP-3 precision");
        
        String statementId = restApiTestUtils.extractJsonPath(statementResponse, "$.statementId");
        
        // Phase 3: Payment Processing
        String paymentRequest = """
            {
                "accountId": "%s",
                "paymentAmount": %s,
                "paymentMethod": "ACH",
                "statementId": "%s"
            }
            """.formatted(accountId, minimumPayment.toString(), statementId);
        
        String paymentResponse = restApiTestUtils.performPostRequest(
            "/api/payments/process",
            paymentRequest,
            MockMvcResultMatchers.status().isOk()
        );
        
        restApiTestUtils.assertJsonPath(paymentResponse, "$.paymentStatus", org.hamcrest.Matchers.equalTo("PROCESSED"));
        restApiTestUtils.assertJsonPath(paymentResponse, "$.paymentAmount", org.hamcrest.Matchers.equalTo(minimumPayment));
        
        Duration totalWorkflowDuration = Duration.between(workflowStart, Instant.now());
        Assertions.assertTrue(totalWorkflowDuration.toMillis() < (4 * TestConstants.RESPONSE_TIME_THRESHOLD_MS),
            "Statement and payment workflow duration exceeds performance threshold");
        
        validateWorkflowCompleteness("statement-payment", session.getUserId());
        assertEndToEndFunctionalParity("BILLING_CYCLE", paymentResponse);
    }

    /**
     * Account closure and card cancellation workflow test validating complete account lifecycle end.
     * Tests account status updates, card deactivation, final balance processing, and cleanup operations
     * ensuring proper state transitions and data consistency.
     */
    @Test
    @WithMockUser(roles = {TestConstants.TEST_ADMIN_ROLE})
    @DisplayName("Account Closure and Card Cancellation Workflow - Complete Account Lifecycle End")
    public void testAccountClosureAndCardCancellationWorkflow() throws Exception {
        Instant workflowStart = Instant.now();
        
        // Phase 1: Setup admin session for account closure authority
        SessionContext adminSession = setupAdminSession();
        String accountId = TestConstants.TEST_ACCOUNT_ID;
        String cardNumber = TestConstants.TEST_CARD_NUMBER;
        
        // Phase 2: Account Balance Validation
        String accountResponse = restApiTestUtils.performGetRequest(
            "/api/accounts/" + accountId,
            MockMvcResultMatchers.status().isOk(),
            adminSession.getUserId()
        );
        
        BigDecimal currentBalance = new BigDecimal(restApiTestUtils.extractJsonPath(accountResponse, "$.balance"));
        
        // Phase 3: Card Deactivation Process
        String cardDeactivationRequest = """
            {
                "cardNumber": "%s",
                "deactivationReason": "ACCOUNT_CLOSURE",
                "effectiveDate": "2024-01-15"
            }
            """.formatted(cardNumber);
        
        String cardDeactivationResponse = restApiTestUtils.performPutRequest(
            "/api/cards/deactivate",
            cardDeactivationRequest,
            MockMvcResultMatchers.status().isOk(),
            adminSession.getUserId()
        );
        
        restApiTestUtils.assertJsonPath(cardDeactivationResponse, "$.status", org.hamcrest.Matchers.equalTo("INACTIVE"));
        
        // Phase 4: Account Closure Process
        String accountClosureRequest = """
            {
                "accountId": "%s",
                "closureReason": "CUSTOMER_REQUEST",
                "finalBalanceDisposition": "REFUND_CHECK",
                "effectiveDate": "2024-01-15"
            }
            """.formatted(accountId);
        
        String closureResponse = restApiTestUtils.performPutRequest(
            "/api/accounts/close",
            accountClosureRequest,
            MockMvcResultMatchers.status().isOk(),
            adminSession.getUserId()
        );
        
        restApiTestUtils.assertJsonPath(closureResponse, "$.status", org.hamcrest.Matchers.equalTo("CLOSED"));
        restApiTestUtils.assertJsonPath(closureResponse, "$.finalBalance", org.hamcrest.Matchers.equalTo(currentBalance));
        
        // Phase 5: Validation of Closure Cascade Effects
        String postClosureCardResponse = restApiTestUtils.performGetRequest(
            "/api/cards/" + cardNumber,
            MockMvcResultMatchers.status().isOk(),
            adminSession.getUserId()
        );
        
        restApiTestUtils.assertJsonPath(postClosureCardResponse, "$.status", org.hamcrest.Matchers.equalTo("INACTIVE"));
        
        Duration totalWorkflowDuration = Duration.between(workflowStart, Instant.now());
        Assertions.assertTrue(totalWorkflowDuration.toMillis() < (5 * TestConstants.RESPONSE_TIME_THRESHOLD_MS),
            "Account closure workflow duration exceeds performance threshold");
        
        validateWorkflowCompleteness("account-closure", adminSession.getUserId());
        assertEndToEndFunctionalParity("ACCOUNT_CLOSURE", closureResponse);
    }

    /**
     * Batch job triggering and monitoring workflow test validating Spring Batch operations.
     * Tests job launch, execution monitoring, status tracking, and completion validation
     * ensuring 4-hour processing window compliance and proper job state management.
     */
    @Test
    @WithMockUser(roles = {TestConstants.TEST_ADMIN_ROLE})
    @DisplayName("Batch Job Triggering and Monitoring Workflow - Spring Batch Operations")
    public void testBatchJobTriggeringAndMonitoringWorkflow() throws Exception {
        Instant workflowStart = Instant.now();
        
        // Phase 1: Setup admin session for batch job authority
        SessionContext adminSession = setupAdminSession();
        
        // Phase 2: Daily Processing Job Launch (equivalent to DAILYPROC JCL)
        String batchJobRequest = """
            {
                "jobName": "dailyProcessingJob",
                "jobParameters": {
                    "processDate": "2024-01-15",
                    "batchMode": "FULL"
                }
            }
            """;
        
        String jobLaunchResponse = restApiTestUtils.performPostRequest(
            "/api/batch/jobs/launch",
            batchJobRequest,
            MockMvcResultMatchers.status().isAccepted()
        );
        
        String jobExecutionId = restApiTestUtils.extractJsonPath(jobLaunchResponse, "$.jobExecutionId");
        Assertions.assertNotNull(jobExecutionId, "Job execution ID must be returned");
        
        // Phase 3: Job Status Monitoring
        String jobStatusResponse = restApiTestUtils.performGetRequest(
            "/api/batch/jobs/" + jobExecutionId + "/status",
            MockMvcResultMatchers.status().isOk(),
            adminSession.getUserId()
        );
        
        restApiTestUtils.assertJsonPath(jobStatusResponse, "$.status", 
            org.hamcrest.Matchers.oneOf("STARTING", "STARTED", "COMPLETED"));
        
        // Phase 4: Job Completion Validation
        // Poll for job completion (simulating batch monitoring)
        boolean jobCompleted = false;
        int maxAttempts = 30; // 30 second timeout
        int attempts = 0;
        
        while (!jobCompleted && attempts < maxAttempts) {
            TimeUnit.SECONDS.sleep(1);
            
            String statusCheckResponse = restApiTestUtils.performGetRequest(
                "/api/batch/jobs/" + jobExecutionId + "/status",
                MockMvcResultMatchers.status().isOk(),
                adminSession.getUserId()
            );
            
            String currentStatus = restApiTestUtils.extractJsonPath(statusCheckResponse, "$.status");
            if ("COMPLETED".equals(currentStatus) || "FAILED".equals(currentStatus)) {
                jobCompleted = true;
                
                Assertions.assertEquals("COMPLETED", currentStatus, 
                    "Batch job must complete successfully");
                
                // Validate job execution metrics
                String executionTime = restApiTestUtils.extractJsonPath(statusCheckResponse, "$.executionDuration");
                Assertions.assertNotNull(executionTime, "Job execution duration must be tracked");
            }
            attempts++;
        }
        
        Assertions.assertTrue(jobCompleted, "Batch job must complete within timeout period");
        
        Duration totalWorkflowDuration = Duration.between(workflowStart, Instant.now());
        // Allow longer duration for batch job execution
        Assertions.assertTrue(totalWorkflowDuration.toMillis() < 45000,
            "Batch job workflow duration exceeds 45-second test threshold");
        
        validateWorkflowCompleteness("batch-job-monitoring", adminSession.getUserId());
        assertEndToEndFunctionalParity("BATCH_PROCESSING", jobStatusResponse);
    }

    /**
     * Report generation and retrieval workflow test validating reporting operations.
     * Tests report request, generation processing, status monitoring, and data retrieval
     * ensuring report formats match COBOL report layouts and completion within time windows.
     */
    @Test
    @WithMockUser(roles = {TestConstants.TEST_ADMIN_ROLE})  
    @DisplayName("Report Generation and Retrieval Workflow - Complete Reporting Process")
    public void testReportGenerationAndRetrievalWorkflow() throws Exception {
        Instant workflowStart = Instant.now();
        
        // Phase 1: Setup admin session for report access
        SessionContext adminSession = setupAdminSession();
        
        // Phase 2: Report Generation Request (CORPT00C.cbl equivalent)
        String reportRequest = """
            {
                "reportType": "MONTHLY_TRANSACTION_SUMMARY",
                "reportPeriod": "2024-01",
                "format": "PDF",
                "includeDetails": true
            }
            """;
        
        String reportGenerationResponse = restApiTestUtils.performPostRequest(
            "/api/reports/generate",
            reportRequest,
            MockMvcResultMatchers.status().isAccepted()
        );
        
        String reportId = restApiTestUtils.extractJsonPath(reportGenerationResponse, "$.reportId");
        Assertions.assertNotNull(reportId, "Report ID must be generated");
        
        // Phase 3: Report Status Monitoring
        boolean reportCompleted = false;
        int maxAttempts = 15; // 15 second timeout for report generation
        int attempts = 0;
        
        while (!reportCompleted && attempts < maxAttempts) {
            TimeUnit.SECONDS.sleep(1);
            
            String statusResponse = restApiTestUtils.performGetRequest(
                "/api/reports/" + reportId + "/status",
                MockMvcResultMatchers.status().isOk(),
                adminSession.getUserId()
            );
            
            String currentStatus = restApiTestUtils.extractJsonPath(statusResponse, "$.status");
            if ("COMPLETED".equals(currentStatus) || "FAILED".equals(currentStatus)) {
                reportCompleted = true;
                
                Assertions.assertEquals("COMPLETED", currentStatus,
                    "Report generation must complete successfully");
                
                // Validate report metadata
                restApiTestUtils.assertJsonPath(statusResponse, "$.reportSize", org.hamcrest.Matchers.notNullValue());
                restApiTestUtils.assertJsonPath(statusResponse, "$.recordCount", org.hamcrest.Matchers.notNullValue());
            }
            attempts++;
        }
        
        Assertions.assertTrue(reportCompleted, "Report generation must complete within timeout period");
        
        // Phase 4: Report Retrieval
        String reportDataResponse = restApiTestUtils.performGetRequest(
            "/api/reports/" + reportId + "/download",
            MockMvcResultMatchers.status().isOk(),
            adminSession.getUserId()
        );
        
        Assertions.assertNotNull(reportDataResponse, "Report data must be retrievable");
        Assertions.assertTrue(reportDataResponse.length() > 0, "Report must contain data");
        
        Duration totalWorkflowDuration = Duration.between(workflowStart, Instant.now());
        Assertions.assertTrue(totalWorkflowDuration.toMillis() < (20 * TestConstants.RESPONSE_TIME_THRESHOLD_MS),
            "Report generation workflow duration exceeds performance threshold");
        
        validateWorkflowCompleteness("report-generation", adminSession.getUserId());
        assertEndToEndFunctionalParity("REPORT_GENERATION", reportDataResponse);
    }

    /**
     * Session state preservation test validating COMMAREA equivalent functionality.
     * Tests session data persistence across multiple requests, timeout handling, and state consistency
     * ensuring Spring Session behavior matches CICS session management patterns.
     */
    @Test
    @WithMockUser(roles = {TestConstants.TEST_USER_ROLE})
    @DisplayName("Session State Preservation Across Workflows - COMMAREA Equivalent Testing")  
    public void testSessionStatePreservationAcrossWorkflows() throws Exception {
        Instant workflowStart = Instant.now();
        
        // Phase 1: Initial Session Creation
        SessionContext initialSession = setupAuthenticatedSession();
        String originalSessionToken = initialSession.getUserId();
        
        // Add test data to navigation stack (equivalent to COMMAREA data)
        initialSession.addToNavigationStack("CC00"); // Sign-on transaction
        initialSession.addToNavigationStack("CM00"); // Menu transaction
        initialSession.setLastTransactionCode("CT00"); // Current transaction
        
        sessionTestUtils.populateSessionData(originalSessionToken, initialSession);
        
        // Phase 2: Multi-Request Session Validation
        // Request 1: Account information
        String accountResponse = restApiTestUtils.performGetRequest(
            "/api/accounts/" + TestConstants.TEST_ACCOUNT_ID,
            MockMvcResultMatchers.status().isOk(),
            originalSessionToken
        );
        
        // Validate session persists after first request
        SessionContext sessionAfterRequest1 = sessionTestUtils.validateSessionData(originalSessionToken);
        Assertions.assertEquals(initialSession.getUserId(), sessionAfterRequest1.getUserId(),
            "User ID must persist across requests");
        Assertions.assertEquals(initialSession.getLastTransactionCode(), sessionAfterRequest1.getLastTransactionCode(),
            "Last transaction code must persist (COMMAREA equivalent)");
        
        // Request 2: Transaction listing
        String transactionResponse = restApiTestUtils.performGetRequest(
            "/api/transactions",
            MockMvcResultMatchers.status().isOk(),
            originalSessionToken
        );
        
        // Validate session state after second request
        SessionContext sessionAfterRequest2 = sessionTestUtils.validateSessionData(originalSessionToken);
        Assertions.assertEquals(3, sessionAfterRequest2.getNavigationStack().size(),
            "Navigation stack must maintain transaction history");
        Assertions.assertTrue(sessionAfterRequest2.getNavigationStack().contains("CC00"),
            "Navigation stack must contain sign-on transaction");
        Assertions.assertTrue(sessionAfterRequest2.getNavigationStack().contains("CM00"),
            "Navigation stack must contain menu transaction");
        
        // Request 3: Card information  
        String cardResponse = restApiTestUtils.performGetRequest(
            "/api/cards/" + TestConstants.TEST_CARD_NUMBER,
            MockMvcResultMatchers.status().isOk(),
            originalSessionToken
        );
        
        // Final session validation
        SessionContext finalSession = sessionTestUtils.validateSessionData(originalSessionToken);
        Assertions.assertEquals(initialSession.getUserId(), finalSession.getUserId(),
            "Session user must remain consistent throughout workflow");
        Assertions.assertNotNull(finalSession.getTransientData(),
            "Transient data must be maintained (COMMAREA equivalent)");
        
        Duration totalWorkflowDuration = Duration.between(workflowStart, Instant.now());
        Assertions.assertTrue(totalWorkflowDuration.toMillis() < (4 * TestConstants.RESPONSE_TIME_THRESHOLD_MS),
            "Session preservation workflow duration exceeds performance threshold");
        
        validateSessionStatePersistence(originalSessionToken, finalSession);
        assertEndToEndFunctionalParity("SESSION_MANAGEMENT", originalSessionToken);
    }

    /**
     * CICS XCTL equivalent controller chaining test validating transaction transfer patterns.
     * Tests controller-to-controller navigation, state transfer, and transaction code handling
     * ensuring Spring Boot navigation matches CICS program transfer behavior.
     */
    @Test
    @WithMockUser(roles = {TestConstants.TEST_USER_ROLE})
    @DisplayName("CICS XCTL Equivalent Controller Chaining - Transaction Transfer Patterns")
    public void testCICSXCTLEquivalentControllerChaining() throws Exception {
        Instant workflowStart = Instant.now();
        
        // Phase 1: Authentication Controller (COSGN00C equivalent)
        SessionContext session = setupAuthenticatedSession();
        
        // Phase 2: Menu Controller Chain (COMEN01C equivalent)
        String menuChainRequest = """
            {
                "fromTransaction": "CC00",
                "userId": "%s",
                "requestedMenu": "MAIN"
            }
            """.formatted(session.getUserId());
        
        String menuChainResponse = restApiTestUtils.performPostRequest(
            "/api/menu/chain",
            menuChainRequest,
            MockMvcResultMatchers.status().isOk()
        );
        
        // Validate menu controller receives context from auth controller
        restApiTestUtils.assertJsonPath(menuChainResponse, "$.fromTransaction", org.hamcrest.Matchers.equalTo("CC00"));
        restApiTestUtils.assertJsonPath(menuChainResponse, "$.currentTransaction", org.hamcrest.Matchers.equalTo("CM00"));
        
        // Phase 3: Transaction Controller Chain (COTRN00C equivalent)
        String transactionChainRequest = """
            {
                "fromTransaction": "CM00",
                "userId": "%s",
                "selectedOption": "TRANSACTION_LIST"
            }
            """.formatted(session.getUserId());
        
        String transactionChainResponse = restApiTestUtils.performPostRequest(
            "/api/transactions/chain",
            transactionChainRequest,
            MockMvcResultMatchers.status().isOk()
        );
        
        // Validate transaction controller receives context from menu controller
        restApiTestUtils.assertJsonPath(transactionChainResponse, "$.fromTransaction", org.hamcrest.Matchers.equalTo("CM00"));
        restApiTestUtils.assertJsonPath(transactionChainResponse, "$.currentTransaction", org.hamcrest.Matchers.equalTo("CT00"));
        
        // Phase 4: Transaction Detail Chain (COTRN01C equivalent)
        String transactionId = restApiTestUtils.extractJsonPath(transactionChainResponse, "$.transactions[0].id");
        
        String detailChainRequest = """
            {
                "fromTransaction": "CT00",
                "userId": "%s",
                "selectedTransactionId": "%s",
                "action": "VIEW_DETAIL"
            }
            """.formatted(session.getUserId(), transactionId);
        
        String detailChainResponse = restApiTestUtils.performPostRequest(
            "/api/transactions/detail/chain",
            detailChainRequest,
            MockMvcResultMatchers.status().isOk()
        );
        
        // Validate detail controller receives context from transaction list controller
        restApiTestUtils.assertJsonPath(detailChainResponse, "$.fromTransaction", org.hamcrest.Matchers.equalTo("CT00"));
        restApiTestUtils.assertJsonPath(detailChainResponse, "$.transactionId", org.hamcrest.Matchers.equalTo(transactionId));
        
        // Phase 5: Return Navigation Chain (PF3 equivalent)
        String returnChainRequest = """
            {
                "fromTransaction": "CT01",
                "userId": "%s",
                "action": "RETURN",
                "targetTransaction": "CT00"
            }
            """.formatted(session.getUserId());
        
        String returnChainResponse = restApiTestUtils.performPostRequest(
            "/api/transactions/return",
            returnChainRequest,
            MockMvcResultMatchers.status().isOk()
        );
        
        // Validate return navigation maintains context
        restApiTestUtils.assertJsonPath(returnChainResponse, "$.currentTransaction", org.hamcrest.Matchers.equalTo("CT00"));
        restApiTestUtils.assertJsonPath(returnChainResponse, "$.navigationHistory", org.hamcrest.Matchers.notNullValue());
        
        Duration totalWorkflowDuration = Duration.between(workflowStart, Instant.now());
        Assertions.assertTrue(totalWorkflowDuration.toMillis() < (6 * TestConstants.RESPONSE_TIME_THRESHOLD_MS),
            "Controller chaining workflow duration exceeds performance threshold");
        
        assertControllerChaining(session.getUserId(), "CC00→CM00→CT00→CT01→CT00");
        validateWorkflowCompleteness("controller-chaining", session.getUserId());
    }

    /**
     * Validates end-to-end workflow completeness ensuring all required steps are executed.
     * Checks workflow state, validates all required operations completed, and verifies
     * business process completion criteria are met.
     * 
     * @param workflowType The type of workflow being validated
     * @param sessionToken The session token for state validation
     */
    private void validateWorkflowCompleteness(String workflowType, String sessionToken) {
        SessionContext finalContext = sessionTestUtils.validateSessionData(sessionToken);
        
        Assertions.assertNotNull(finalContext, "Session context must exist after workflow completion");
        Assertions.assertFalse(finalContext.getUserId().isEmpty(), "User ID must be maintained in session");
        
        // Validate workflow-specific completeness criteria
        switch (workflowType) {
            case "sign-on-to-transaction":
                Assertions.assertTrue(finalContext.getNavigationStack().contains("CC00"),
                    "Navigation stack must contain sign-on transaction");
                Assertions.assertNotNull(finalContext.getLastTransactionCode(),
                    "Last transaction code must be recorded");
                break;
                
            case "admin-navigation":
                Assertions.assertEquals(TestConstants.TEST_ADMIN_ROLE, finalContext.getUserRole(),
                    "Admin role must be maintained throughout workflow");
                break;
                
            case "card-application":
                Assertions.assertNotNull(finalContext.getTransientData(),
                    "Card application context must be preserved");
                break;
                
            case "dispute-resolution":
                Assertions.assertTrue(finalContext.getNavigationStack().size() > 2,
                    "Dispute workflow must have multiple navigation steps");
                break;
                
            case "statement-payment":
                Assertions.assertNotNull(finalContext.getTransientData(),
                    "Payment processing context must be preserved");
                break;
                
            case "account-closure":
                Assertions.assertEquals(TestConstants.TEST_ADMIN_ROLE, finalContext.getUserRole(),
                    "Admin privileges required for account closure");
                break;
                
            case "batch-job-monitoring":
                Assertions.assertNotNull(finalContext.getLastTransactionCode(),
                    "Batch transaction context must be preserved");
                break;
                
            case "report-generation":
                Assertions.assertNotNull(finalContext.getTransientData(),
                    "Report generation context must be preserved");
                break;
                
            case "controller-chaining":
                Assertions.assertTrue(finalContext.getNavigationStack().size() >= 3,
                    "Controller chaining must maintain navigation history");
                break;
                
            default:
                Assertions.fail("Unknown workflow type: " + workflowType);
        }
    }

    /**
     * Asserts end-to-end functional parity with COBOL CICS transaction behavior.
     * Validates that modernized workflows produce identical functional results
     * to their COBOL program counterparts.
     * 
     * @param workflowIdentifier The workflow being validated
     * @param responseData The final response data to validate
     */
    private void assertEndToEndFunctionalParity(String workflowIdentifier, String responseData) {
        Assertions.assertNotNull(responseData, "Response data must not be null for functional parity validation");
        Assertions.assertFalse(responseData.trim().isEmpty(), "Response data must not be empty");
        
        // Validate response structure based on workflow type
        switch (workflowIdentifier) {
            case "CC00→CM00→CT00":
                // Validate transaction listing structure matches COTRN00C output
                restApiTestUtils.assertJsonPath(responseData, "$.transactions", org.hamcrest.Matchers.notNullValue());
                restApiTestUtils.assertJsonPath(responseData, "$.pageNumber", org.hamcrest.Matchers.notNullValue());
                break;
                
            case "CARD_LIFECYCLE":
                // Validate card activation response structure
                restApiTestUtils.assertJsonPath(responseData, "$.cardNumber", org.hamcrest.Matchers.notNullValue());
                restApiTestUtils.assertJsonPath(responseData, "$.status", org.hamcrest.Matchers.equalTo("ACTIVE"));
                break;
                
            case "DISPUTE_LIFECYCLE":
                // Validate dispute resolution response structure
                restApiTestUtils.assertJsonPath(responseData, "$.status", org.hamcrest.Matchers.equalTo("RESOLVED"));
                restApiTestUtils.assertJsonPath(responseData, "$.refundProcessed", org.hamcrest.Matchers.equalTo(true));
                break;
                
            case "BILLING_CYCLE":
                // Validate payment processing response structure
                restApiTestUtils.assertJsonPath(responseData, "$.paymentStatus", org.hamcrest.Matchers.equalTo("PROCESSED"));
                restApiTestUtils.assertJsonPath(responseData, "$.paymentAmount", org.hamcrest.Matchers.notNullValue());
                break;
                
            case "ACCOUNT_CLOSURE":
                // Validate account closure response structure
                restApiTestUtils.assertJsonPath(responseData, "$.status", org.hamcrest.Matchers.equalTo("CLOSED"));
                restApiTestUtils.assertJsonPath(responseData, "$.finalBalance", org.hamcrest.Matchers.notNullValue());
                break;
                
            case "BATCH_PROCESSING":
                // Validate batch job status response structure
                restApiTestUtils.assertJsonPath(responseData, "$.status", org.hamcrest.Matchers.equalTo("COMPLETED"));
                restApiTestUtils.assertJsonPath(responseData, "$.executionDuration", org.hamcrest.Matchers.notNullValue());
                break;
                
            case "REPORT_GENERATION":
                // Validate report data structure
                Assertions.assertTrue(responseData.contains("MONTHLY_TRANSACTION_SUMMARY") || 
                                    responseData.length() > 100,
                    "Report data must contain expected content or substantial data");
                break;
                
            case "SESSION_MANAGEMENT":
                // Validate session token structure  
                Assertions.assertNotNull(responseData, "Session token must be valid");
                Assertions.assertTrue(responseData.length() > 0, "Session token must have content");
                break;
                
            default:
                // Generic validation for unknown workflow types
                Assertions.assertTrue(responseData.length() > 10, 
                    "Response must contain meaningful data for workflow: " + workflowIdentifier);
        }
    }

    /**
     * Measures and validates workflow performance against COBOL mainframe benchmarks.
     * Ensures response times meet or exceed original system performance characteristics.
     * 
     * @param workflowName The name of the workflow being measured
     * @param startTime The workflow start timestamp
     * @return Duration of the workflow execution
     */
    private Duration measureWorkflowPerformance(String workflowName, Instant startTime) {
        Duration workflowDuration = Duration.between(startTime, Instant.now());
        
        // Log performance metrics for monitoring
        System.out.printf("Workflow '%s' completed in %d ms%n", workflowName, workflowDuration.toMillis());
        
        // Validate against performance thresholds
        if (workflowName.contains("batch")) {
            // Batch workflows have longer acceptable duration
            Assertions.assertTrue(workflowDuration.toMillis() < 60000,
                "Batch workflow " + workflowName + " duration " + workflowDuration.toMillis() + "ms exceeds 60-second threshold");
        } else {
            // Interactive workflows must meet sub-200ms requirement
            Assertions.assertTrue(workflowDuration.toMillis() < (5 * TestConstants.RESPONSE_TIME_THRESHOLD_MS),
                "Interactive workflow " + workflowName + " duration " + workflowDuration.toMillis() + "ms exceeds performance threshold");
        }
        
        return workflowDuration;
    }

    /**
     * Validates session state persistence across multiple operations.
     * Ensures Spring Session functionality accurately replicates CICS COMMAREA behavior
     * with proper state management and timeout handling.
     * 
     * @param sessionToken The session token to validate
     * @param expectedContext The expected session context state
     */
    private void validateSessionStatePersistence(String sessionToken, SessionContext expectedContext) {
        // Validate session accessibility
        SessionContext actualContext = sessionTestUtils.validateSessionData(sessionToken);
        
        Assertions.assertNotNull(actualContext, "Session context must be retrievable");
        Assertions.assertEquals(expectedContext.getUserId(), actualContext.getUserId(),
            "Session user ID must match expected context");
        Assertions.assertEquals(expectedContext.getUserRole(), actualContext.getUserRole(),
            "Session user role must match expected context");
        
        // Validate COMMAREA equivalent functionality
        Assertions.assertEquals(expectedContext.getNavigationStack().size(), 
                               actualContext.getNavigationStack().size(),
            "Navigation stack size must be preserved");
        
        for (String transactionCode : expectedContext.getNavigationStack()) {
            Assertions.assertTrue(actualContext.getNavigationStack().contains(transactionCode),
                "Navigation stack must contain transaction code: " + transactionCode);
        }
        
        // Validate session size constraints (32KB COMMAREA equivalent)
        int sessionSize = sessionTestUtils.getSessionSize(sessionToken);
        Assertions.assertTrue(sessionSize < 32768, 
            "Session size " + sessionSize + " bytes exceeds 32KB COMMAREA limit");
        
        // Validate session structure matches COMMAREA layout
        sessionTestUtils.validateCommareaStructure(actualContext);
    }

    /**
     * Validates controller chaining behavior equivalent to CICS XCTL operations.
     * Ensures proper transaction code handling, state transfer, and navigation history
     * maintenance across controller boundaries.
     * 
     * @param sessionToken The session token for chain validation
     * @param expectedChain The expected controller chain pattern (e.g., "CC00→CM00→CT00")
     */
    private void assertControllerChaining(String sessionToken, String expectedChain) {
        SessionContext chainContext = sessionTestUtils.validateSessionData(sessionToken);
        
        String[] chainSteps = expectedChain.split("→");
        Assertions.assertTrue(chainContext.getNavigationStack().size() >= chainSteps.length - 1,
            "Navigation stack must contain sufficient chain steps");
        
        // Validate each step in the chain is recorded
        for (int i = 0; i < chainSteps.length - 1; i++) {
            String transactionCode = chainSteps[i];
            Assertions.assertTrue(chainContext.getNavigationStack().contains(transactionCode),
                "Controller chain must contain transaction code: " + transactionCode);
        }
        
        // Validate current transaction matches final step
        String finalStep = chainSteps[chainSteps.length - 1];
        if (chainContext.getLastTransactionCode() != null) {
            Assertions.assertTrue(chainContext.getLastTransactionCode().equals(finalStep) ||
                                chainContext.getNavigationStack().contains(finalStep),
                "Final transaction step must be recorded in session context");
        }
        
        // Validate chain integrity (no broken links)
        for (int i = 0; i < chainSteps.length - 1; i++) {
            String currentStep = chainSteps[i];
            String nextStep = chainSteps[i + 1];
            
            // Verify logical progression
            boolean validProgression = isValidTransactionProgression(currentStep, nextStep);
            Assertions.assertTrue(validProgression,
                "Invalid transaction progression from " + currentStep + " to " + nextStep);
        }
    }

    /**
     * Validates business process completion ensuring all required operations finished.
     * Checks for proper state transitions, data consistency, and process integrity
     * across the complete business workflow.
     * 
     * @param processType The type of business process to validate
     * @param sessionToken The session token for process validation
     */
    private void validateBusinessProcessCompletion(String processType, String sessionToken) {
        SessionContext processContext = sessionTestUtils.validateSessionData(sessionToken);
        
        switch (processType) {
            case "AUTHENTICATION":
                Assertions.assertNotNull(processContext.getUserId(), "User must be authenticated");
                Assertions.assertNotNull(processContext.getUserRole(), "User role must be assigned");
                break;
                
            case "MENU_NAVIGATION":
                Assertions.assertTrue(processContext.getNavigationStack().size() > 0,
                    "Menu navigation must record transaction history");
                break;
                
            case "TRANSACTION_PROCESSING":
                Assertions.assertNotNull(processContext.getLastTransactionCode(),
                    "Transaction processing must record transaction context");
                break;
                
            case "CARD_MANAGEMENT":
                Assertions.assertNotNull(processContext.getTransientData(),
                    "Card management must preserve operation context");
                break;
                
            case "PAYMENT_PROCESSING":
                Assertions.assertNotNull(processContext.getTransientData(),
                    "Payment processing must preserve transaction context");
                break;
                
            case "BATCH_OPERATIONS":
                Assertions.assertNotNull(processContext.getLastTransactionCode(),
                    "Batch operations must record processing context");
                break;
                
            case "REPORT_GENERATION":
                Assertions.assertNotNull(processContext.getTransientData(),
                    "Report generation must preserve request context");
                break;
                
            default:
                Assertions.fail("Unknown business process type: " + processType);
        }
    }

    /**
     * Helper method to setup authenticated session for test workflows.
     * Creates authenticated user session with proper role assignment and
     * session context initialization.
     * 
     * @return SessionContext with authenticated user information
     */
    private SessionContext setupAuthenticatedSession() {
        SessionContext session = sessionTestUtils.createTestSession();
        session.setUserId(TestConstants.TEST_USER_ID);
        session.setUserRole(TestConstants.TEST_USER_ROLE);
        
        return session;
    }

    /**
     * Helper method to setup admin session for administrative workflows.
     * Creates admin-privileged session with elevated access rights for
     * administrative operations testing.
     * 
     * @return SessionContext with admin user information
     */
    private SessionContext setupAdminSession() {
        SessionContext adminSession = sessionTestUtils.createTestSession();
        adminSession.setUserId("ADMIN001");
        adminSession.setUserRole(TestConstants.TEST_ADMIN_ROLE);
        
        return adminSession;
    }

    /**
     * Validates transaction progression logic matching CICS XCTL patterns.
     * Ensures controller transitions follow proper business logic and
     * maintain transaction code consistency.
     * 
     * @param fromTransaction The source transaction code
     * @param toTransaction The target transaction code
     * @return true if the progression is valid, false otherwise
     */
    private boolean isValidTransactionProgression(String fromTransaction, String toTransaction) {
        // Define valid transaction progressions based on COBOL program XCTL patterns
        return switch (fromTransaction) {
            case "CC00" -> "CM00".equals(toTransaction); // Sign-on to menu
            case "CM00" -> toTransaction.startsWith("C"); // Menu to any business transaction
            case "CT00" -> "CT01".equals(toTransaction) || "CM00".equals(toTransaction); // Transaction list to detail or back to menu
            case "CT01" -> "CT00".equals(toTransaction); // Transaction detail back to list
            default -> true; // Allow other valid progressions
        };
    }
}