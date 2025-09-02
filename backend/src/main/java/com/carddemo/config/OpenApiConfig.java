/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.config;

import com.carddemo.controller.TransactionController;
import com.carddemo.dto.SignOnRequest;
import com.carddemo.controller.AuthController;
import com.carddemo.controller.AccountController;
import com.carddemo.dto.TransactionListResponse;

import org.springdoc.core.models.GroupedOpenApi;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.ArraySchema;

/**
 * OpenAPI/Swagger configuration for CardDemo REST API documentation.
 * 
 * Provides comprehensive API documentation for all REST endpoints that replace 
 * CICS transactions, including schema definitions matching BMS map structures
 * and interactive API testing interface for developers and integration partners.
 * 
 * This configuration organizes API endpoints by functional areas corresponding 
 * to CICS transaction code groupings:
 * - Authentication APIs: CC00 (Sign-on) replacing COSGN00C COBOL program
 * - Transaction APIs: CT00, CT01, CT02 replacing COTRN00C, COTRN01C, COTRN02C
 * - Account APIs: CAVW, CAUP replacing COACTVWC, COACTUPC COBOL programs
 * - Card APIs: Card management operations
 * - Reporting APIs: Report generation operations  
 * - Batch APIs: Batch processing operations
 * 
 * Key Features:
 * - Schema definitions matching BMS map field structures and validation rules
 * - Transaction code to REST endpoint mapping documentation
 * - Interactive Swagger UI for API testing and exploration
 * - Security scheme definitions for JWT-based authentication
 * - Environment-specific server configurations for deployment flexibility
 * - Comprehensive field-level documentation derived from COBOL picture clauses
 * 
 * BMS Map Schema Mappings:
 * - COSGN00.bms -> SignOnRequest schema (USERID, PASSWD fields)
 * - COMEN01.bms -> Menu option schemas (OPTN001-OPTN012, OPTION selection)
 * - COTRN00.bms -> Transaction list schemas (TRNIDIN search, pagination metadata)
 * 
 * API Versioning Strategy:
 * - Version 1.0 for initial migration from COBOL to Spring Boot
 * - Backward compatibility maintained through schema evolution
 * - API version documented in OpenAPI info for client integration
 * 
 * Security Integration:
 * - JWT bearer token authentication scheme definition
 * - Session-based authentication for web client integration
 * - Role-based access control documentation for admin/user endpoints
 * - Security requirements applied per endpoint group for granular access control
 * 
 * Performance Considerations:
 * - API documentation served from Spring Boot application context
 * - Swagger UI assets cached for optimal loading performance
 * - Schema generation optimized to minimize startup time impact
 * - Documentation updates deployed with application versioning
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 * @see TransactionController - REST endpoints for transaction operations
 * @see AuthController - REST endpoints for authentication operations
 * @see AccountController - REST endpoints for account management operations
 */
@Configuration
public class OpenApiConfig {

    /**
     * Main OpenAPI specification configuration.
     * 
     * Creates the root OpenAPI specification with comprehensive metadata,
     * security schemes, and reusable components. Provides foundational
     * configuration for all API endpoint documentation and interactive testing.
     * 
     * API Information:
     * - Title: CardDemo API - Credit Card Management System
     * - Description: Comprehensive REST API documentation for mainframe modernization
     * - Version: 1.0.0 (matching CardDemo application version)
     * - Contact: Integration partners and development team information
     * - License: Apache 2.0 (matching project license)
     * 
     * Security Configuration:
     * - JWT Bearer token authentication for API access
     * - Session-based authentication for web application integration
     * - Global security requirements applied to all endpoints
     * - Role-based access control documentation
     * 
     * Server Configuration:
     * - Development environment: http://localhost:8080
     * - Staging environment: https://staging.carddemo.aws
     * - Production environment: https://api.carddemo.aws
     * - Environment-specific base URLs for client integration
     * 
     * Component Definitions:
     * - Reusable schema components derived from BMS map structures
     * - Common response patterns for error handling and pagination
     * - Security scheme definitions for authentication integration
     * - Field validation rules matching COBOL picture clause constraints
     * 
     * @return OpenAPI specification with complete configuration
     */
    @Bean
    public OpenAPI openApiSpecification() {
        // Create comprehensive API metadata
        Info apiInfo = new Info()
                .title("CardDemo API - Credit Card Management System")
                .description("Comprehensive REST API for credit card management operations, " +
                           "replacing IBM mainframe CICS transactions with modern Spring Boot endpoints. " +
                           "Provides complete functional parity with original COBOL programs including " +
                           "user authentication, transaction processing, account management, and batch operations. " +
                           "\n\n**Transaction Code Mappings:**\n" +
                           "- CC00: User Sign-on (POST /api/auth/signin)\n" +
                           "- CT00: Transaction List (GET /api/transactions)\n" +
                           "- CT01: Transaction Detail (GET /api/transactions/{id})\n" +
                           "- CT02: New Transaction (POST /api/transactions)\n" +
                           "- CAVW: Account View (GET /api/accounts/{id})\n" +
                           "- CAUP: Account Update (PUT /api/accounts/{id})\n" +
                           "\n**Key Features:**\n" +
                           "- Field validation matching COBOL picture clauses\n" +
                           "- BigDecimal precision for financial calculations\n" +
                           "- Session management replacing CICS COMMAREA\n" +
                           "- Cursor-based pagination equivalent to VSAM browsing\n" +
                           "- Comprehensive error handling with field-level validation messages")
                .version("1.0.0")
                .contact(createContactInformation())
                .license(new License()
                        .name("Apache License 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));

        // Define server environments for deployment flexibility
        List<Server> servers = List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("Development server - Local development environment"),
                new Server()
                        .url("https://staging.carddemo.aws")
                        .description("Staging server - Pre-production testing environment"),
                new Server()
                        .url("https://api.carddemo.aws")
                        .description("Production server - Live production environment")
        );

        // Configure comprehensive security schemes
        Components components = new Components()
                .addSecuritySchemes("bearerAuth", createJwtSecurityScheme())
                .addSecuritySchemes("sessionAuth", createSessionSecurityScheme());

        // Add reusable schema components matching BMS map structures
        addBmsSchemaComponents(components);

        // Define global security requirements
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth")
                .addList("sessionAuth");

        return new OpenAPI()
                .info(apiInfo)
                .servers(servers)
                .components(components)
                .addSecurityItem(securityRequirement);
    }

    /**
     * Transaction API group configuration (CT00, CT01, CT02).
     * 
     * Creates dedicated API documentation group for transaction-related endpoints,
     * replacing COBOL transaction programs COTRN00C, COTRN01C, and COTRN02C.
     * Provides comprehensive documentation for transaction list browsing,
     * detailed transaction viewing, and new transaction creation operations.
     * 
     * Endpoint Coverage:
     * - GET /api/transactions: Transaction list with pagination and filtering (CT00)
     * - GET /api/transactions/{id}: Individual transaction detail view (CT01)
     * - POST /api/transactions: Create new transaction with validation (CT02)
     * 
     * BMS Screen Integration:
     * - Maps COTRN00 BMS transaction list screen to REST API pagination
     * - Preserves TRNIDIN search functionality through query parameters
     * - Maintains SEL0001-SEL0010 selection behavior through response schemas
     * - Implements PAGENUM pagination equivalent with hasMorePages/hasPreviousPages
     * 
     * Key Features:
     * - Cursor-based pagination replacing VSAM STARTBR/READNEXT operations
     * - Comprehensive filtering by account ID, card number, date ranges, amounts
     * - Transaction validation matching COBOL edit routines
     * - Field-level error responses for frontend integration
     * - Performance optimization with sub-200ms response time requirements
     * 
     * Security Requirements:
     * - JWT bearer token authentication required for all transaction endpoints
     * - Role-based access control for transaction creation operations
     * - Audit trail logging for compliance with financial regulations
     * - Sensitive data masking in API responses and error messages
     * 
     * @return GroupedOpenApi for transaction operations
     */
    @Bean
    public GroupedOpenApi transactionApis() {
        return GroupedOpenApi.builder()
                .group("transactions")
                .displayName("Transaction Operations (CT00, CT01, CT02)")
                .pathsToMatch("/api/transactions/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    // Add transaction-specific operation metadata
                    if (handlerMethod.getBeanType().equals(TransactionController.class)) {
                        operation.addTagsItem("Transaction Management");
                        operation.summary(enhanceTransactionSummary(operation.getSummary(), handlerMethod.getMethod().getName()));
                    }
                    return operation;
                })
                .build();
    }

    /**
     * Authentication API group configuration (CC00).
     * 
     * Creates dedicated API documentation group for authentication operations,
     * replacing COBOL sign-on program COSGN00C. Provides comprehensive
     * documentation for user authentication, session management, and authorization.
     * 
     * Endpoint Coverage:
     * - POST /api/auth/signin: User authentication with credential validation (CC00)
     * - POST /api/auth/signout: Session termination and security context cleanup
     * - GET /api/auth/status: Current authentication status and user context
     * 
     * BMS Screen Integration:
     * - Maps COSGN00 BMS sign-on screen fields to JSON request/response schemas
     * - Preserves USERID and PASSWD field validation rules from COBOL
     * - Maintains ERRMSG error display patterns through structured error responses
     * - Implements menu option generation based on user type (ADMIN/USER)
     * 
     * Authentication Flow:
     * - JSON credential processing replacing CICS RECEIVE MAP operations
     * - Spring Security integration with JWT token generation
     * - Session management through Redis replacing CICS COMMAREA structures
     * - Role-based menu option generation for frontend navigation
     * 
     * Security Features:
     * - Password validation matching COBOL security requirements
     * - Brute force protection with configurable attempt limits
     * - Session timeout management with 30-minute default policy
     * - Audit trail logging for authentication events and failures
     * 
     * Field Validation:
     * - User ID: 1-8 alphanumeric characters (matches COBOL PIC X(8))
     * - Password: 1-8 characters with complexity requirements
     * - Input sanitization preventing injection attacks
     * - Case-insensitive user ID processing with uppercase conversion
     * 
     * @return GroupedOpenApi for authentication operations
     */
    @Bean
    public GroupedOpenApi authenticationApis() {
        return GroupedOpenApi.builder()
                .group("authentication")
                .displayName("Authentication Operations (CC00)")
                .pathsToMatch("/api/auth/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    // Add authentication-specific operation metadata
                    if (handlerMethod.getBeanType().equals(AuthController.class)) {
                        operation.addTagsItem("User Authentication");
                        operation.summary(enhanceAuthenticationSummary(operation.getSummary(), handlerMethod.getMethod().getName()));
                    }
                    return operation;
                })
                .build();
    }

    /**
     * Account API group configuration (CAVW, CAUP).
     * 
     * Creates dedicated API documentation group for account management operations,
     * replacing COBOL account programs COACTVWC and COACTUPC. Provides comprehensive
     * documentation for account viewing, updating, and validation operations.
     * 
     * Endpoint Coverage:
     * - GET /api/accounts/{id}: Account detail view with customer information (CAVW)
     * - PUT /api/accounts/{id}: Account field updates with comprehensive validation (CAUP)
     * 
     * COBOL Program Integration:
     * - COACTVWC.cbl -> GET /api/accounts/{id} with identical business logic
     * - COACTUPC.cbl -> PUT /api/accounts/{id} with field validation preservation
     * - Maintains exact validation rules from COBOL edit routines
     * - Preserves BigDecimal precision for monetary fields (COMP-3 equivalent)
     * 
     * Account Validation Rules:
     * - Account ID: Exactly 11 digits, non-zero numeric value
     * - Account status: Active accounts only for update operations
     * - Credit limits: Minimum $100, maximum $50,000 with validation
     * - Interest rates: 0.00% to 29.99% range with 2-decimal precision
     * - Date validation: Account open date, last payment date consistency
     * 
     * Data Integration:
     * - PostgreSQL account master file (ACCTDAT equivalent)
     * - Customer master file integration (CUSTDAT equivalent)
     * - Card cross-reference file lookup (CARDXREF equivalent)
     * - Optimistic locking for concurrent update protection
     * 
     * Business Logic:
     * - Account-to-customer relationship validation
     * - Credit limit change approval workflow
     * - Interest rate update authorization requirements
     * - Audit trail maintenance for regulatory compliance
     * 
     * Performance Characteristics:
     * - Sub-150ms response times for account lookup operations
     * - Efficient database queries using composite primary keys
     * - Connection pooling optimization for high-volume operations
     * - Caching integration for frequently accessed account data
     * 
     * @return GroupedOpenApi for account operations
     */
    @Bean
    public GroupedOpenApi accountApis() {
        return GroupedOpenApi.builder()
                .group("accounts")
                .displayName("Account Operations (CAVW, CAUP)")
                .pathsToMatch("/api/accounts/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    // Add account-specific operation metadata
                    if (handlerMethod.getBeanType().equals(AccountController.class)) {
                        operation.addTagsItem("Account Management");
                        operation.summary(enhanceAccountSummary(operation.getSummary(), handlerMethod.getMethod().getName()));
                        
                        // Add BigDecimal precision documentation for monetary fields
                        operation.description(operation.getDescription() + 
                            "\n\n**Monetary Field Precision:** All amount fields use BigDecimal with scale(2) " +
                            "precision to maintain exact COBOL COMP-3 packed decimal behavior for financial calculations.");
                    }
                    return operation;
                })
                .build();
    }

    /**
     * Card API group configuration.
     * 
     * Creates dedicated API documentation group for card management operations,
     * including card issuance, status updates, and security management.
     * Provides comprehensive documentation for card-related business processes.
     * 
     * Future Endpoint Coverage:
     * - GET /api/cards: List cards for account with filtering capabilities
     * - GET /api/cards/{cardNumber}: Individual card detail view
     * - POST /api/cards: Issue new card with validation
     * - PUT /api/cards/{cardNumber}: Update card status and limits
     * - DELETE /api/cards/{cardNumber}: Deactivate card with security validation
     * 
     * Card Management Features:
     * - Card number generation with Luhn algorithm validation
     * - Card status management (Active, Blocked, Expired, Cancelled)
     * - Credit limit management with authorization workflows
     * - Security code management and validation
     * - Card replacement and reissue operations
     * 
     * Security Requirements:
     * - PCI DSS compliance for card number handling
     * - Secure card data transmission with encryption
     * - Role-based access control for card management operations
     * - Audit logging for all card status changes
     * 
     * Validation Rules:
     * - Card number: 16 digits with valid Luhn checksum
     * - Expiration date: Future date validation with 5-year maximum
     * - Credit limit: Positive amount not exceeding account limit
     * - Security code: 3-digit CVV with secure generation
     * 
     * @return GroupedOpenApi for card operations
     */
    @Bean
    public GroupedOpenApi cardApis() {
        return GroupedOpenApi.builder()
                .group("cards")
                .displayName("Card Management Operations")
                .pathsToMatch("/api/cards/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    // Add card-specific operation metadata
                    operation.addTagsItem("Card Management");
                    operation.summary("Card " + operation.getSummary());
                    
                    // Add PCI compliance documentation
                    operation.description(operation.getDescription() + 
                        "\n\n**Security Notice:** All card operations comply with PCI DSS requirements. " +
                        "Sensitive card data is encrypted in transit and at rest. Card numbers are masked " +
                        "in API responses and audit logs for security compliance.");
                    
                    return operation;
                })
                .build();
    }

    /**
     * Reporting API group configuration.
     * 
     * Creates dedicated API documentation group for reporting and analytics operations,
     * including transaction reports, account summaries, and audit trail generation.
     * Provides comprehensive documentation for business intelligence and compliance reporting.
     * 
     * Future Endpoint Coverage:
     * - GET /api/reports/transactions: Transaction report generation with filters
     * - GET /api/reports/accounts: Account summary reports by various criteria
     * - GET /api/reports/audit: Audit trail reports for compliance requirements
     * - POST /api/reports/custom: Custom report generation with user-defined parameters
     * 
     * Report Types:
     * - Daily transaction summaries with merchant categorization
     * - Monthly account statements with balance calculations
     * - Annual activity reports for regulatory compliance
     * - Custom date range reports with flexible filtering
     * 
     * Output Formats:
     * - JSON for API integration and frontend display
     * - PDF for formal statements and compliance documentation
     * - CSV for data export and spreadsheet integration
     * - Excel for advanced data analysis and manipulation
     * 
     * Performance Considerations:
     * - Asynchronous report generation for large datasets
     * - Caching integration for frequently requested reports
     * - Pagination support for large result sets
     * - Background processing for resource-intensive operations
     * 
     * Security and Compliance:
     * - Role-based access control for sensitive reports
     * - Data masking for PII protection in non-production environments
     * - Audit trail logging for report access and generation
     * - Retention policy compliance for generated reports
     * 
     * @return GroupedOpenApi for reporting operations
     */
    @Bean
    public GroupedOpenApi reportingApis() {
        return GroupedOpenApi.builder()
                .group("reporting")
                .displayName("Reporting and Analytics Operations")
                .pathsToMatch("/api/reports/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    // Add reporting-specific operation metadata
                    operation.addTagsItem("Reporting & Analytics");
                    operation.summary("Report: " + operation.getSummary());
                    
                    // Add compliance documentation
                    operation.description(operation.getDescription() + 
                        "\n\n**Compliance Notice:** All reports maintain data privacy and regulatory compliance. " +
                        "Sensitive information is masked or redacted according to data protection policies. " +
                        "Report generation and access are logged for audit trail requirements.");
                    
                    return operation;
                })
                .build();
    }

    /**
     * Batch API group configuration.
     * 
     * Creates dedicated API documentation group for batch processing operations,
     * including job management, scheduling, and monitoring capabilities.
     * Provides comprehensive documentation for batch processing workflows.
     * 
     * Future Endpoint Coverage:
     * - GET /api/batch/jobs: List batch jobs with status and schedule information
     * - POST /api/batch/jobs: Submit new batch job with parameters
     * - GET /api/batch/jobs/{jobId}: Individual batch job status and results
     * - PUT /api/batch/jobs/{jobId}: Update job parameters or schedule
     * - DELETE /api/batch/jobs/{jobId}: Cancel running job or remove from schedule
     * 
     * Batch Job Types:
     * - Daily interest calculation and account updates
     * - Monthly statement generation and distribution
     * - Transaction validation and fraud detection processing
     * - Account maintenance and cleanup operations
     * - Regulatory reporting and compliance data extraction
     * 
     * Job Management Features:
     * - Job scheduling with cron expression support
     * - Job dependency management and execution ordering
     * - Retry logic with configurable backoff strategies
     * - Job status monitoring and progress tracking
     * - Error handling and notification integration
     * 
     * Performance and Scalability:
     * - Chunk-based processing for large datasets
     * - Parallel processing capabilities for independent operations
     * - Resource allocation and throttling controls
     * - Database connection pooling optimization
     * - Memory management for high-volume processing
     * 
     * Monitoring and Alerting:
     * - Real-time job status updates and progress indicators
     * - Error notification through email and webhook integration
     * - Performance metrics collection and analysis
     * - Resource utilization monitoring and optimization
     * - SLA compliance tracking and reporting
     * 
     * @return GroupedOpenApi for batch operations
     */
    @Bean
    public GroupedOpenApi batchApis() {
        return GroupedOpenApi.builder()
                .group("batch")
                .displayName("Batch Processing Operations")
                .pathsToMatch("/api/batch/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    // Add batch-specific operation metadata
                    operation.addTagsItem("Batch Processing");
                    operation.summary("Batch: " + operation.getSummary());
                    
                    // Add performance documentation
                    operation.description(operation.getDescription() + 
                        "\n\n**Performance Notice:** Batch operations are designed for high-volume processing " +
                        "with optimized resource utilization. Large jobs may take several minutes to hours " +
                        "depending on data volume. Job status polling is recommended for monitoring progress.");
                    
                    return operation;
                })
                .build();
    }

    // Helper methods for creating OpenAPI configuration components

    /**
     * Creates contact information for the CardDemo API.
     * 
     * Provides comprehensive contact details for integration partners,
     * developers, and support teams working with the CardDemo REST API.
     * 
     * @return Contact object with development team information
     */
    private Contact createContactInformation() {
        return new Contact()
                .name("CardDemo Development Team")
                .email("cardemo-support@aws.amazon.com")
                .url("https://github.com/aws-samples/carddemo");
    }

    /**
     * Creates JWT bearer token security scheme definition.
     * 
     * Configures JWT authentication scheme for REST API endpoints,
     * replacing RACF authentication with modern token-based security.
     * Provides comprehensive security documentation for API consumers.
     * 
     * @return SecurityScheme for JWT authentication
     */
    private SecurityScheme createJwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token-based authentication replacing RACF security. " +
                           "Obtain token through POST /api/auth/signin endpoint with valid credentials. " +
                           "Include token in Authorization header as 'Bearer <token>' for all API calls. " +
                           "Token expires after 30 minutes (configurable). Session timeout matches CICS behavior.");
    }

    /**
     * Creates session-based security scheme definition.
     * 
     * Configures session authentication scheme for web application integration,
     * using Spring Session with Redis backend to replace CICS COMMAREA.
     * 
     * @return SecurityScheme for session-based authentication
     */
    private SecurityScheme createSessionSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("JSESSIONID")
                .description("Session-based authentication using Spring Session with Redis backend. " +
                           "Replaces CICS COMMAREA with distributed session storage. " +
                           "Session established through web application authentication flow. " +
                           "Automatic session management with 30-minute timeout policy.");
    }

    /**
     * Adds reusable schema components matching BMS map structures.
     * 
     * Creates comprehensive schema definitions that match original COBOL
     * copybook field structures and BMS mapset layouts. Provides field-level
     * documentation with validation rules derived from COBOL picture clauses.
     * 
     * @param components Components object to add schemas to
     */
    private void addBmsSchemaComponents(Components components) {
        // Add SignOn screen schema (COSGN00.bms equivalent)
        Schema<?> signOnSchema = new ObjectSchema()
                .addProperty("userId", new StringSchema()
                        .minLength(1)
                        .maxLength(8)
                        .pattern("^[A-Za-z0-9]+$")
                        .description("User ID for authentication. 1-8 alphanumeric characters. " +
                                   "Maps to USERID field in COSGN00 BMS mapset. " +
                                   "Converted to uppercase for COBOL compatibility."))
                .addProperty("password", new StringSchema()
                        .minLength(1)
                        .maxLength(8)
                        .format("password")
                        .description("User password for authentication. 1-8 characters. " +
                                   "Maps to PASSWD field in COSGN00 BMS mapset. " +
                                   "Validated against COBOL security requirements."));
        
        // Add Transaction List schema (COTRN00.bms equivalent)
        Schema<?> transactionSearchSchema = new ObjectSchema()
                .addProperty("transactionId", new StringSchema()
                        .maxLength(16)
                        .pattern("^[A-Za-z0-9]*$")
                        .description("Transaction ID search filter. Maps to TRNIDIN field in COTRN00 BMS mapset."))
                .addProperty("pageNumber", new IntegerSchema()
                        .minimum(java.math.BigDecimal.ONE)
                        .description("Page number for pagination. Maps to PAGENUM field in COTRN00 BMS mapset."));

        // Add Menu Option schema (COMEN01.bms equivalent)
        Schema<?> menuOptionSchema = new ObjectSchema()
                .addProperty("option", new StringSchema()
                        .minLength(1)
                        .maxLength(2)
                        .pattern("^[0-9]+$")
                        .description("Selected menu option number. Maps to OPTION field in COMEN01 BMS mapset."));

        // Add pagination metadata schema
        Schema<?> paginationSchema = new ObjectSchema()
                .addProperty("currentPage", new IntegerSchema()
                        .minimum(java.math.BigDecimal.ONE)
                        .description("Current page number in result set"))
                .addProperty("totalPages", new IntegerSchema()
                        .minimum(java.math.BigDecimal.ONE)
                        .description("Total number of pages available"))
                .addProperty("pageSize", new IntegerSchema()
                        .minimum(java.math.BigDecimal.ONE)
                        .maximum(java.math.BigDecimal.valueOf(100))
                        .description("Number of items per page"))
                .addProperty("totalItems", new IntegerSchema()
                        .minimum(java.math.BigDecimal.ZERO)
                        .description("Total number of items in complete result set"))
                .addProperty("hasNextPage", new Schema<Boolean>()
                        .type("boolean")
                        .description("Indicates if more pages are available"))
                .addProperty("hasPreviousPage", new Schema<Boolean>()
                        .type("boolean")
                        .description("Indicates if previous pages are available"));

        // Add error response schema
        Schema<?> errorSchema = new ObjectSchema()
                .addProperty("timestamp", new StringSchema()
                        .format("date-time")
                        .description("Error occurrence timestamp in ISO 8601 format"))
                .addProperty("status", new IntegerSchema()
                        .description("HTTP status code"))
                .addProperty("error", new StringSchema()
                        .description("Error type description"))
                .addProperty("message", new StringSchema()
                        .description("Detailed error message"))
                .addProperty("path", new StringSchema()
                        .description("API path where error occurred"))
                .addProperty("fieldErrors", new ArraySchema()
                        .items(new ObjectSchema()
                                .addProperty("field", new StringSchema()
                                        .description("Field name with validation error"))
                                .addProperty("rejectedValue", new Schema<Object>()
                                        .description("Value that failed validation"))
                                .addProperty("message", new StringSchema()
                                        .description("Field-specific error message")))
                        .description("Field-level validation errors"));

        // Add all schemas to components
        components.addSchemas("SignOnRequest", signOnSchema)
                  .addSchemas("TransactionSearch", transactionSearchSchema)
                  .addSchemas("MenuOption", menuOptionSchema)
                  .addSchemas("PaginationInfo", paginationSchema)
                  .addSchemas("ErrorResponse", errorSchema);
    }

    /**
     * Enhances transaction operation summaries with CICS mapping information.
     * 
     * @param originalSummary Original operation summary
     * @param methodName Controller method name
     * @return Enhanced summary with transaction code mapping
     */
    private String enhanceTransactionSummary(String originalSummary, String methodName) {
        if (originalSummary == null) {
            originalSummary = "Transaction operation";
        }
        
        String transactionCode = switch (methodName) {
            case "getTransactions", "getAllTransactions" -> "CT00";
            case "getTransaction", "getTransactionById" -> "CT01";
            case "createTransaction", "postTransaction" -> "CT02";
            default -> "CTxx";
        };
        
        return String.format("%s (CICS Transaction: %s)", originalSummary, transactionCode);
    }

    /**
     * Enhances authentication operation summaries with CICS mapping information.
     * 
     * @param originalSummary Original operation summary
     * @param methodName Controller method name
     * @return Enhanced summary with transaction code mapping
     */
    private String enhanceAuthenticationSummary(String originalSummary, String methodName) {
        if (originalSummary == null) {
            originalSummary = "Authentication operation";
        }
        
        String transactionCode = switch (methodName) {
            case "signIn", "authenticate", "login" -> "CC00";
            case "signOut", "logout" -> "CC01";
            case "getStatus", "checkAuth" -> "CC02";
            default -> "CCxx";
        };
        
        return String.format("%s (CICS Transaction: %s)", originalSummary, transactionCode);
    }

    /**
     * Enhances account operation summaries with CICS mapping information.
     * 
     * @param originalSummary Original operation summary
     * @param methodName Controller method name
     * @return Enhanced summary with transaction code mapping
     */
    private String enhanceAccountSummary(String originalSummary, String methodName) {
        if (originalSummary == null) {
            originalSummary = "Account operation";
        }
        
        String transactionCode = switch (methodName) {
            case "getAccount", "viewAccount", "getAccountById" -> "CAVW";
            case "updateAccount", "putAccount" -> "CAUP";
            default -> "CAxx";
        };
        
        return String.format("%s (CICS Transaction: %s)", originalSummary, transactionCode);
    }
}
