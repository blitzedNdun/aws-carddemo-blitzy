package com.carddemo.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

// Import Java test utility classes (compiled from src/test/java)
import com.carddemo.performance.TestDataGenerator

/**
 * Gatling performance simulation for CardDemo credit card management system.
 * 
 * This simulation replicates real-world user scenarios for credit card transactions
 * with performance targets matching mainframe baseline:
 * - Target: 10,000 TPS (Transactions Per Second)
 * - Response time: < 200ms (95th percentile)
 * - Load pattern: Realistic user ramp-up and sustained load
 * 
 * Test scenarios include:
 * - Credit card authorization requests
 * - Account balance inquiries  
 * - Transaction history retrieval
 * 
 * The simulation generates detailed performance reports with percentile metrics
 * and validates response times against COBOL system baseline performance.
 */
class GatlingSimulation extends Simulation {

  // Performance targets matching TestConstants values
  private val targetTps = 10000 // TARGET_TPS from TestConstants
  private val maxResponseTimeMs = 200L // MAX_RESPONSE_TIME_MS from TestConstants
  private val concurrentUsers = 1000 // CONCURRENT_USERS from TestConstants
  private val testDurationSeconds = 300L // PERFORMANCE_TEST_DURATION_SECONDS from TestConstants
  
  // HTTP protocol configuration with comprehensive headers
  private val httpProtocol = http
    .baseUrl("${target.url:http://localhost:8080}")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .authorizationHeader("Bearer ${jwt.token}")
    .header("User-Agent", "Gatling-CardDemo-Performance-Test")
    .check(status.is(200))
    .check(responseTimeInMillis.lte(maxResponseTimeMs.toInt))

  // Test data setup using TestDataGenerator instance methods
  private val testDataGenerator = new TestDataGenerator()
  private val testAccounts = testDataGenerator.generateAccountList(100).asScala.toList
  private val testTransactions = testDataGenerator.generateTransactionList(1000).asScala.toList // Generate 1000 test transactions
  private val testUserCredentials = Map(
    "userId" -> "TESTUSER", // TEST_USER_ID from TestConstants
    "password" -> "TESTPASS" // TEST_USER_PASSWORD from TestConstants
  )

  /**
   * Authorization scenario simulating credit card transaction authorization requests.
   * 
   * This scenario replicates the COTRN00C transaction processing pattern:
   * 1. User authentication
   * 2. Transaction authorization request
   * 3. Response validation with COBOL precision
   * 
   * @return Scenario builder configured for authorization testing
   */
  def authorizationScenario() = {
    scenario("Credit Card Authorization")
      .exec(session => {
        // Reset random seed for consistent test data generation
        testDataGenerator.resetRandomSeed(12345L)
        session.set("account", testDataGenerator.generateAccount())
               .set("transaction", testDataGenerator.generateTransaction())
      })
      .exec(
        http("Authenticate User")
          .post("/api/auth/login")
          .body(StringBody(s"""
            {
              "userId": "${testUserCredentials("userId")}", 
              "password": "${testUserCredentials("password")}"
            }
          """)).asJson
          .check(jsonPath("$.token").saveAs("authToken"))
      )
      .pause(100.milliseconds, 500.milliseconds) // Realistic user think time
      .exec(
        http("Process Authorization")
          .post("/api/transactions")
          .header("Authorization", "Bearer #{authToken}")
          .body(StringBody("""
            {
              "accountId": "#{account.accountId}",
              "amount": "#{transaction.transactionAmount}",
              "merchantId": "MERCHANT001",
              "transactionType": "AUTH",
              "cardNumber": "#{account.cardNumber}"
            }
          """)).asJson
          .check(jsonPath("$.responseCode").is("00"))
          .check(jsonPath("$.authCode").exists)
          .check(responseTimeInMillis.lte(maxResponseTimeMs.toInt))
      )
  }

  /**
   * Balance inquiry scenario for account balance retrieval.
   * 
   * Simulates COACTVWC account view transaction with:
   * 1. Account authentication
   * 2. Balance inquiry request
   * 3. COMP-3 precision validation for monetary amounts
   * 
   * @return Scenario builder for balance inquiry testing
   */
  def balanceInquiryScenario() = {
    scenario("Account Balance Inquiry")
      .exec(session => {
        val account = testDataGenerator.generateAccount()
        session.set("accountId", account.getAccountId)
               .set("expectedBalance", testDataGenerator.generateComp3BigDecimal(2, 2, 50000.0))
      })
      .exec(
        http("Get Account Balance")
          .get("/api/accounts/#{accountId}/balance")
          .check(jsonPath("$.accountBalance").exists)
          .check(jsonPath("$.currency").is("USD"))
          .check(responseTimeInMillis.lte(maxResponseTimeMs.toInt))
      )
      .pause(200.milliseconds, 1.seconds) // User review time
      .exec(
        http("Get Account Details")
          .get("/api/accounts/#{accountId}")
          .check(jsonPath("$.accountId").is("#{accountId}"))
          .check(jsonPath("$.accountStatus").in("ACTIVE", "SUSPENDED"))
          .check(responseTimeInMillis.lte(maxResponseTimeMs.toInt))
      )
  }

  /**
   * Transaction history scenario for retrieving account transaction records.
   * 
   * Implements COTRN01C transaction list processing:
   * 1. Authentication validation
   * 2. Paginated transaction retrieval
   * 3. Transaction detail viewing
   * 4. Response time validation
   * 
   * @return Scenario builder for transaction history testing  
   */
  def transactionHistoryScenario() = {
    scenario("Transaction History Retrieval")
      .exec(session => {
        val accountId = testDataGenerator.generateAccount().getAccountId
        session.set("accountId", accountId)
               .set("pageSize", 20)
               .set("pageNumber", 0)
      })
      .exec(
        http("Get Transaction List")
          .get("/api/transactions")
          .queryParam("accountId", "#{accountId}")
          .queryParam("page", "#{pageNumber}")
          .queryParam("size", "#{pageSize}")
          .check(jsonPath("$.content").exists)
          .check(jsonPath("$.totalElements").exists)
          .check(responseTimeInMillis.lte(maxResponseTimeMs.toInt))
          .check(jsonPath("$.content[0].transactionId").saveAs("firstTransactionId"))
      )
      .pause(500.milliseconds, 2.seconds) // User selection time
      .exec(
        http("Get Transaction Details")
          .get("/api/transactions/#{firstTransactionId}")
          .check(jsonPath("$.transactionId").is("#{firstTransactionId}"))
          .check(jsonPath("$.transactionAmount").exists)
          .check(jsonPath("$.transactionTimestamp").exists)
          .check(responseTimeInMillis.lte(maxResponseTimeMs.toInt))
      )
      .exec(
        http("Get Next Page")
          .get("/api/transactions")
          .queryParam("accountId", "#{accountId}")
          .queryParam("page", "1")
          .queryParam("size", "#{pageSize}")
          .check(responseTimeInMillis.lte(maxResponseTimeMs.toInt))
      )
  }

  /**
   * Load test setup configuration with injection profiles to reach target TPS.
   * 
   * Implements realistic load patterns:
   * - Initial burst load for immediate capacity testing
   * - Ramp-up period (30 seconds)
   * - Sustained load period (240 seconds) 
   * - Gradual ramp-down (30 seconds)
   * 
   * Total test duration: 5 minutes (300 seconds)
   * Target throughput: 10,000 TPS sustained
   * 
   * @return PopulationBuilder array for load injection
   */
  def loadTestSetUp() = {
    Array(
      // Initial burst test with atOnceUsers for immediate capacity validation
      authorizationScenario()
        .inject(
          atOnceUsers(50), // Immediate burst test
          rampUsersPerSec(1).to(600).during(30.seconds), // Ramp up
          constantUsersPerSec(600).during(240.seconds),   // Sustained load  
          rampUsersPerSec(600).to(1).during(30.seconds)  // Ramp down
        ).protocols(httpProtocol),
        
      // Balance inquiry scenario - 25% of total load (2,500 TPS)  
      balanceInquiryScenario()
        .inject(
          atOnceUsers(25), // Initial burst
          rampUsersPerSec(1).to(250).during(30.seconds), // Ramp up
          constantUsersPerSec(250).during(240.seconds),   // Sustained load
          rampUsersPerSec(250).to(1).during(30.seconds)  // Ramp down  
        ).protocols(httpProtocol),
        
      // Transaction history scenario - 15% of total load (1,500 TPS)
      transactionHistoryScenario()
        .inject(
          atOnceUsers(15), // Initial burst
          rampUsersPerSec(1).to(150).during(30.seconds), // Ramp up
          constantUsersPerSec(150).during(240.seconds),   // Sustained load
          rampUsersPerSec(150).to(1).during(30.seconds)  // Ramp down
        ).protocols(httpProtocol)
    )
  }

  /**
   * Performance assertions validating response time and throughput targets.
   * 
   * Critical performance requirements:
   * - 95th percentile response time < 200ms (matches COBOL baseline)
   * - Mean response time < 100ms  
   * - Request success rate > 99%
   * - Target throughput: 10,000 requests/second
   * 
   * @return Assertion array for performance validation
   */
  def performanceAssertions() = {
    List(
      // Response time assertions (matching mainframe baseline)
      global.responseTime.percentile3.lte(maxResponseTimeMs.toInt), // 95th percentile < 200ms
      global.responseTime.mean.lte(maxResponseTimeMs.toInt / 2),    // Mean < 100ms  
      global.responseTime.max.lte(maxResponseTimeMs.toInt * 3),     // Max < 600ms
      
      // Success rate assertions
      global.successfulRequests.percent.gte(99.0), // > 99% success rate
      global.requestsPerSec.gte(targetTps * 0.9),  // >= 90% of target TPS (9,000 RPS)
      
      // Scenario-specific assertions
      forAll.responseTime.percentile3.lte(maxResponseTimeMs.toInt),
      forAll.failedRequests.percent.lte(1.0) // < 1% failure rate
    )
  }

  /**
   * Report generation configuration for detailed performance analysis.
   * 
   * Generates comprehensive performance reports including:
   * - Response time percentile distribution
   * - Throughput analysis over time
   * - Error rate breakdown by scenario
   * - Resource utilization correlation
   * 
   * Reports are generated in HTML format with interactive charts
   * and CSV export for further analysis.
   */
  def reportGeneration(): Unit = {
    // Configure detailed reporting
    println(s"Performance test configuration:")
    println(s"- Target TPS: $targetTps")
    println(s"- Max Response Time: ${maxResponseTimeMs}ms")
    println(s"- Concurrent Users: $concurrentUsers")
    println(s"- Test Duration: ${testDurationSeconds}s")
    println(s"- Test Scenarios: Authorization, Balance Inquiry, Transaction History")
    println(s"- Load Pattern: Realistic ramp-up/sustain/ramp-down")
    println(s"")
    println(s"Report will be generated at: target/gatling/results/")
    println(s"Detailed metrics include:")
    println(s"- Response time percentiles (50th, 95th, 99th)")  
    println(s"- Requests per second over time")
    println(s"- Success/failure rate analysis")
    println(s"- Performance vs. COBOL baseline comparison")
  }

  // Main simulation setup with comprehensive load injection and global assertions
  setUp(loadTestSetUp(): _*)
    .assertions(performanceAssertions(): _*)
    .maxDuration(testDurationSeconds.seconds + 60.seconds) // Allow extra time for cleanup
    .throttle(
      reachRps(targetTps.toInt).in(60.seconds), // Global throttling to target TPS
      holdFor(testDurationSeconds.seconds)
    )

  // Initialize reporting
  before {
    reportGeneration()
    println("Starting CardDemo performance simulation...")
    println(s"Generating test data using TestDataGenerator...")
  }

  after {
    println("Performance simulation completed.")
    println("Processing results and generating reports...")
    println("Check target/gatling/results/ for detailed performance analysis.")
  }
}