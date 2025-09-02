package com.carddemo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

/**
 * External payment processing service implementing integration with payment networks
 * and bank core systems. Provides methods for transaction authorization, payment
 * processing, and bank system communication with fault tolerance patterns.
 * 
 * This service replaces COBOL program calls to external payment processing systems
 * while maintaining identical business logic and precision handling for financial
 * calculations using BigDecimal with COBOL COMP-3 equivalent precision.
 * 
 * Implements fault tolerance patterns including circuit breaker, retry mechanisms,
 * rate limiting, and bulkhead isolation for external system resilience.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RestTemplate restTemplate;
    
    @Value("${carddemo.payment.network.endpoint:http://payment-network:8080}")
    private String paymentNetworkEndpoint;
    
    @Value("${carddemo.bank.core.endpoint:http://bank-core:8080}")
    private String bankCoreEndpoint;
    
    @Value("${carddemo.payment.timeout.seconds:30}")
    private int paymentTimeoutSeconds;
    
    @Value("${carddemo.payment.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${carddemo.payment.network.api-key:default-api-key}")
    private String paymentNetworkApiKey;
    
    @Value("${carddemo.bank.core.api-key:default-bank-key}")
    private String bankCoreApiKey;

    // COBOL COMP-3 equivalent precision for financial calculations
    private static final int FINANCIAL_SCALE = 2;
    private static final RoundingMode FINANCIAL_ROUNDING = RoundingMode.HALF_UP;
    
    // Payment network response codes matching COBOL return codes
    private static final String AUTHORIZATION_APPROVED = "00";
    private static final String AUTHORIZATION_DECLINED = "05";
    private static final String AUTHORIZATION_TIMEOUT = "91";
    private static final String SYSTEM_ERROR = "96";
    
    /**
     * Authorizes a payment transaction through external payment networks.
     * Implements circuit breaker and retry patterns for fault tolerance.
     * 
     * This method replaces COBOL EXEC CICS LINK to external authorization programs
     * while maintaining identical request/response data structures and processing logic.
     * 
     * @param cardNumber The card number to authorize (PAN)
     * @param amount The transaction amount with COBOL COMP-3 precision
     * @param merchantId The merchant identifier
     * @param transactionType The type of transaction (SALE, REFUND, etc.)
     * @return Authorization response containing approval code and status
     * @throws PaymentAuthorizationException if authorization fails
     * @throws PaymentNetworkTimeoutException if network timeout occurs
     */
    @CircuitBreaker(name = "paymentNetworkAuth", fallbackMethod = "authorizePaymentFallback")
    @Retry(name = "paymentNetworkAuth")
    @RateLimiter(name = "paymentNetworkAuth")
    @Bulkhead(name = "paymentNetworkAuth", type = Bulkhead.Type.THREADPOOL)
    public PaymentAuthorizationResponse authorizePayment(
            @NotBlank String cardNumber,
            @NotNull BigDecimal amount,
            @NotBlank String merchantId,
            @NotBlank String transactionType) {
        
        log.info("Starting payment authorization for card: {}**** amount: {} merchant: {} type: {}", 
                 cardNumber.substring(0, 4), amount, merchantId, transactionType);
        
        try {
            // Validate and format amount with COBOL COMP-3 precision
            BigDecimal formattedAmount = amount.setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
            
            // Build authorization request matching COBOL COMMAREA structure
            Map<String, Object> authRequest = new HashMap<>();
            authRequest.put("card_number", cardNumber);
            authRequest.put("amount", formattedAmount.toString());
            authRequest.put("merchant_id", merchantId);
            authRequest.put("transaction_type", transactionType);
            authRequest.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            authRequest.put("processing_code", "003000"); // Standard authorization code
            
            // Set up HTTP headers for payment network communication
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + paymentNetworkApiKey);
            headers.set("X-Transaction-Source", "CARDDEMO-MODERNIZED");
            headers.set("X-Processing-Mode", "REAL_TIME");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(authRequest, headers);
            
            // Execute authorization request to payment network
            String authUrl = paymentNetworkEndpoint + "/api/v1/authorize";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                authUrl, 
                HttpMethod.POST, 
                request, 
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            // Process authorization response
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new PaymentAuthorizationException("Empty response from payment network");
            }
            
            String responseCode = (String) responseBody.get("response_code");
            String authorizationCode = (String) responseBody.get("authorization_code");
            String responseMessage = (String) responseBody.get("response_message");
            
            // Create response object matching COBOL program structure
            PaymentAuthorizationResponse authResponse = new PaymentAuthorizationResponse();
            authResponse.setResponseCode(responseCode);
            authResponse.setAuthorizationCode(authorizationCode);
            authResponse.setResponseMessage(responseMessage);
            authResponse.setTransactionAmount(formattedAmount);
            authResponse.setProcessingTimestamp(LocalDateTime.now());
            authResponse.setApproved(AUTHORIZATION_APPROVED.equals(responseCode));
            
            // Log authorization result for audit trail
            if (authResponse.isApproved()) {
                log.info("Payment authorization APPROVED - Auth Code: {} Amount: {}", 
                         authorizationCode, formattedAmount);
            } else {
                log.warn("Payment authorization DECLINED - Response Code: {} Message: {}", 
                         responseCode, responseMessage);
            }
            
            return authResponse;
            
        } catch (RestClientException e) {
            log.error("Payment network communication error during authorization", e);
            throw new PaymentNetworkTimeoutException("Failed to communicate with payment network", e);
        } catch (Exception e) {
            log.error("Unexpected error during payment authorization", e);
            throw new PaymentAuthorizationException("Payment authorization failed", e);
        }
    }

    /**
     * Processes a complete payment transaction including capture and settlement.
     * Implements comprehensive error handling and transaction state management.
     * 
     * This method replaces COBOL transaction processing logic while maintaining
     * identical financial calculation precision and error handling patterns.
     * 
     * @param authorizationCode The authorization code from previous authorization
     * @param transactionAmount The amount to process
     * @param accountNumber The account number for processing
     * @param processingDate The date for transaction processing
     * @return Transaction processing response with settlement details
     * @throws PaymentProcessingException if processing fails
     */
    @CircuitBreaker(name = "paymentProcessing", fallbackMethod = "processTransactionFallback")
    @Retry(name = "paymentProcessing")
    @RateLimiter(name = "paymentProcessing")
    public PaymentProcessingResponse processTransaction(
            @NotBlank String authorizationCode,
            @NotNull BigDecimal transactionAmount,
            @NotBlank String accountNumber,
            @NotNull LocalDateTime processingDate) {
        
        log.info("Processing transaction with auth code: {} amount: {} account: {}", 
                 authorizationCode, transactionAmount, accountNumber);
        
        try {
            // Ensure financial precision matching COBOL COMP-3 behavior
            BigDecimal processAmount = transactionAmount.setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING);
            
            // Build transaction processing request
            Map<String, Object> processRequest = new HashMap<>();
            processRequest.put("authorization_code", authorizationCode);
            processRequest.put("transaction_amount", processAmount.toString());
            processRequest.put("account_number", accountNumber);
            processRequest.put("processing_date", processingDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            processRequest.put("settlement_flag", "Y");
            processRequest.put("capture_method", "ELECTRONIC");
            
            // Set up HTTP headers for transaction processing
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + paymentNetworkApiKey);
            headers.set("X-Transaction-Source", "CARDDEMO-SETTLEMENT");
            headers.set("X-Idempotency-Key", authorizationCode + "-" + System.currentTimeMillis());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(processRequest, headers);
            
            // Execute transaction processing request
            String processUrl = paymentNetworkEndpoint + "/api/v1/process";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                processUrl,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            // Process transaction response
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new PaymentProcessingException("Empty response from payment processor");
            }
            
            String processingStatus = (String) responseBody.get("processing_status");
            String settlementId = (String) responseBody.get("settlement_id");
            String processingMessage = (String) responseBody.get("processing_message");
            
            // Create processing response matching COBOL structure
            PaymentProcessingResponse processResponse = new PaymentProcessingResponse();
            processResponse.setProcessingStatus(processingStatus);
            processResponse.setSettlementId(settlementId);
            processResponse.setProcessingMessage(processingMessage);
            processResponse.setProcessedAmount(processAmount);
            processResponse.setProcessingTimestamp(LocalDateTime.now());
            processResponse.setSuccessful("COMPLETED".equals(processingStatus));
            
            log.info("Transaction processing completed - Settlement ID: {} Status: {} Amount: {}", 
                     settlementId, processingStatus, processAmount);
            
            return processResponse;
            
        } catch (RestClientException e) {
            log.error("Payment processing network error", e);
            throw new PaymentProcessingException("Failed to process transaction", e);
        } catch (Exception e) {
            log.error("Unexpected error during transaction processing", e);
            throw new PaymentProcessingException("Transaction processing failed", e);
        }
    }

    /**
     * Validates card details through external card validation services.
     * Implements caching for improved performance and reduced external calls.
     * 
     * This method replaces COBOL card validation routines while maintaining
     * identical validation logic and response patterns.
     * 
     * @param cardNumber The card number to validate
     * @param expirationDate The card expiration date
     * @param cardholderName The name on the card
     * @return Card validation response with validation results
     * @throws CardValidationException if validation fails
     */
    @CircuitBreaker(name = "cardValidation", fallbackMethod = "validateCardDetailsFallback")
    @Retry(name = "cardValidation")
    @Cacheable(value = "cardValidation", key = "#cardNumber.substring(0,6)")
    public CardValidationResponse validateCardDetails(
            @NotBlank @Pattern(regexp = "\\d{13,19}") String cardNumber,
            @NotBlank String expirationDate,
            @NotBlank String cardholderName) {
        
        log.info("Validating card details for card: {}****", cardNumber.substring(0, 4));
        
        try {
            // Build card validation request
            Map<String, Object> validationRequest = new HashMap<>();
            validationRequest.put("card_number", cardNumber);
            validationRequest.put("expiration_date", expirationDate);
            validationRequest.put("cardholder_name", cardholderName);
            validationRequest.put("validation_type", "FULL");
            
            // Set up HTTP headers for card validation
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + paymentNetworkApiKey);
            headers.set("X-Validation-Source", "CARDDEMO-VALIDATION");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(validationRequest, headers);
            
            // Execute card validation request
            String validationUrl = paymentNetworkEndpoint + "/api/v1/validate";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                validationUrl,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            // Process validation response
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new CardValidationException("Empty response from card validation service");
            }
            
            String validationStatus = (String) responseBody.get("validation_status");
            String cardType = (String) responseBody.get("card_type");
            String issuerName = (String) responseBody.get("issuer_name");
            String validationMessage = (String) responseBody.get("validation_message");
            
            // Create validation response
            CardValidationResponse validationResponse = new CardValidationResponse();
            validationResponse.setValidationStatus(validationStatus);
            validationResponse.setCardType(cardType);
            validationResponse.setIssuerName(issuerName);
            validationResponse.setValidationMessage(validationMessage);
            validationResponse.setValidationTimestamp(LocalDateTime.now());
            validationResponse.setValid("VALID".equals(validationStatus));
            
            log.info("Card validation completed - Status: {} Type: {} Issuer: {}", 
                     validationStatus, cardType, issuerName);
            
            return validationResponse;
            
        } catch (RestClientException e) {
            log.error("Card validation service communication error", e);
            throw new CardValidationException("Failed to validate card details", e);
        } catch (Exception e) {
            log.error("Unexpected error during card validation", e);
            throw new CardValidationException("Card validation failed", e);
        }
    }

    /**
     * Retrieves the current status of bank core systems.
     * Provides health check and connectivity verification for bank core integration.
     * 
     * This method replaces COBOL system status checking routines while providing
     * enhanced monitoring and diagnostic capabilities.
     * 
     * @return Bank system status response with connectivity and health information
     * @throws BankSystemException if system status check fails
     */
    @CircuitBreaker(name = "bankSystemStatus", fallbackMethod = "getBankSystemStatusFallback")
    @Retry(name = "bankSystemStatus")
    @Cacheable(value = "bankSystemStatus", unless = "#result.systemAvailable == false")
    public BankSystemStatusResponse getBankSystemStatus() {
        
        log.info("Checking bank core system status");
        
        try {
            // Set up HTTP headers for system status check
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + bankCoreApiKey);
            headers.set("X-Status-Source", "CARDDEMO-MONITORING");
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            // Execute system status request
            String statusUrl = bankCoreEndpoint + "/api/v1/system/status";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                statusUrl,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            // Process system status response
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new BankSystemException("Empty response from bank core system");
            }
            
            String systemStatus = (String) responseBody.get("system_status");
            String systemVersion = (String) responseBody.get("system_version");
            String lastUpdateTime = (String) responseBody.get("last_update_time");
            Integer activeConnections = (Integer) responseBody.get("active_connections");
            
            // Create system status response
            BankSystemStatusResponse statusResponse = new BankSystemStatusResponse();
            statusResponse.setSystemStatus(systemStatus);
            statusResponse.setSystemVersion(systemVersion);
            statusResponse.setLastUpdateTime(lastUpdateTime);
            statusResponse.setActiveConnections(activeConnections);
            statusResponse.setStatusCheckTimestamp(LocalDateTime.now());
            statusResponse.setSystemAvailable("ONLINE".equals(systemStatus));
            
            log.info("Bank system status check completed - Status: {} Version: {} Connections: {}", 
                     systemStatus, systemVersion, activeConnections);
            
            return statusResponse;
            
        } catch (RestClientException e) {
            log.error("Bank core system communication error during status check", e);
            throw new BankSystemException("Failed to check bank system status", e);
        } catch (Exception e) {
            log.error("Unexpected error during bank system status check", e);
            throw new BankSystemException("Bank system status check failed", e);
        }
    }

    /**
     * Refreshes the connection to payment networks and reinitializes network parameters.
     * Implements connection management and network optimization for payment processing.
     * 
     * This method replaces COBOL network initialization routines while providing
     * enhanced connection management and monitoring capabilities.
     * 
     * @throws PaymentNetworkException if connection refresh fails
     */
    @CircuitBreaker(name = "paymentNetworkRefresh", fallbackMethod = "refreshPaymentNetworkConnectionFallback")
    @Retry(name = "paymentNetworkRefresh")
    @CacheEvict(value = {"cardValidation", "bankSystemStatus"}, allEntries = true)
    public void refreshPaymentNetworkConnection() {
        
        log.info("Refreshing payment network connection");
        
        try {
            // Build connection refresh request
            Map<String, Object> refreshRequest = new HashMap<>();
            refreshRequest.put("refresh_type", "FULL");
            refreshRequest.put("client_id", "CARDDEMO-MODERNIZED");
            refreshRequest.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Set up HTTP headers for connection refresh
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + paymentNetworkApiKey);
            headers.set("X-Refresh-Source", "CARDDEMO-CONNECTION-MANAGER");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(refreshRequest, headers);
            
            // Execute connection refresh request
            String refreshUrl = paymentNetworkEndpoint + "/api/v1/connection/refresh";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                refreshUrl,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            // Verify refresh success
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new PaymentNetworkException("Payment network refresh returned non-success status: " + 
                                                 response.getStatusCode());
            }
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                String refreshStatus = (String) responseBody.get("refresh_status");
                if (!"SUCCESS".equals(refreshStatus)) {
                    throw new PaymentNetworkException("Payment network refresh failed with status: " + refreshStatus);
                }
            }
            
            log.info("Payment network connection refresh completed successfully");
            
        } catch (RestClientException e) {
            log.error("Payment network communication error during connection refresh", e);
            throw new PaymentNetworkException("Failed to refresh payment network connection", e);
        } catch (Exception e) {
            log.error("Unexpected error during payment network connection refresh", e);
            throw new PaymentNetworkException("Payment network connection refresh failed", e);
        }
    }

    // Fallback methods for circuit breaker patterns
    
    /**
     * Fallback method for payment authorization when circuit is open or service is unavailable.
     */
    public PaymentAuthorizationResponse authorizePaymentFallback(String cardNumber, BigDecimal amount, 
                                                               String merchantId, String transactionType, Exception ex) {
        log.warn("Payment authorization fallback triggered due to: {}", ex.getMessage());
        
        PaymentAuthorizationResponse fallbackResponse = new PaymentAuthorizationResponse();
        fallbackResponse.setResponseCode(AUTHORIZATION_TIMEOUT);
        fallbackResponse.setAuthorizationCode("FALLBACK");
        fallbackResponse.setResponseMessage("Payment network temporarily unavailable");
        fallbackResponse.setTransactionAmount(amount.setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING));
        fallbackResponse.setProcessingTimestamp(LocalDateTime.now());
        fallbackResponse.setApproved(false);
        
        return fallbackResponse;
    }
    
    /**
     * Fallback method for transaction processing when circuit is open or service is unavailable.
     */
    public PaymentProcessingResponse processTransactionFallback(String authorizationCode, BigDecimal transactionAmount,
                                                              String accountNumber, LocalDateTime processingDate, Exception ex) {
        log.warn("Transaction processing fallback triggered due to: {}", ex.getMessage());
        
        PaymentProcessingResponse fallbackResponse = new PaymentProcessingResponse();
        fallbackResponse.setProcessingStatus("DEFERRED");
        fallbackResponse.setSettlementId("FALLBACK-" + System.currentTimeMillis());
        fallbackResponse.setProcessingMessage("Transaction processing deferred due to network issues");
        fallbackResponse.setProcessedAmount(transactionAmount.setScale(FINANCIAL_SCALE, FINANCIAL_ROUNDING));
        fallbackResponse.setProcessingTimestamp(LocalDateTime.now());
        fallbackResponse.setSuccessful(false);
        
        return fallbackResponse;
    }
    
    /**
     * Fallback method for card validation when circuit is open or service is unavailable.
     */
    public CardValidationResponse validateCardDetailsFallback(String cardNumber, String expirationDate, 
                                                            String cardholderName, Exception ex) {
        log.warn("Card validation fallback triggered due to: {}", ex.getMessage());
        
        CardValidationResponse fallbackResponse = new CardValidationResponse();
        fallbackResponse.setValidationStatus("UNAVAILABLE");
        fallbackResponse.setCardType("UNKNOWN");
        fallbackResponse.setIssuerName("VALIDATION_SERVICE_UNAVAILABLE");
        fallbackResponse.setValidationMessage("Card validation service temporarily unavailable");
        fallbackResponse.setValidationTimestamp(LocalDateTime.now());
        fallbackResponse.setValid(false);
        
        return fallbackResponse;
    }
    
    /**
     * Fallback method for bank system status when circuit is open or service is unavailable.
     */
    public BankSystemStatusResponse getBankSystemStatusFallback(Exception ex) {
        log.warn("Bank system status fallback triggered due to: {}", ex.getMessage());
        
        BankSystemStatusResponse fallbackResponse = new BankSystemStatusResponse();
        fallbackResponse.setSystemStatus("UNAVAILABLE");
        fallbackResponse.setSystemVersion("UNKNOWN");
        fallbackResponse.setLastUpdateTime("N/A");
        fallbackResponse.setActiveConnections(0);
        fallbackResponse.setStatusCheckTimestamp(LocalDateTime.now());
        fallbackResponse.setSystemAvailable(false);
        
        return fallbackResponse;
    }
    
    /**
     * Fallback method for payment network connection refresh when circuit is open or service is unavailable.
     */
    public void refreshPaymentNetworkConnectionFallback(Exception ex) {
        log.warn("Payment network connection refresh fallback triggered due to: {}", ex.getMessage());
        // In fallback mode, we simply log the issue and continue operation
        // The application can continue with cached connections and retry later
    }

    // Response classes for external service communication
    
    public static class PaymentAuthorizationResponse {
        private String responseCode;
        private String authorizationCode;
        private String responseMessage;
        private BigDecimal transactionAmount;
        private LocalDateTime processingTimestamp;
        private boolean approved;
        
        // Getters and setters
        public String getResponseCode() { return responseCode; }
        public void setResponseCode(String responseCode) { this.responseCode = responseCode; }
        public String getAuthorizationCode() { return authorizationCode; }
        public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
        public String getResponseMessage() { return responseMessage; }
        public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }
        public BigDecimal getTransactionAmount() { return transactionAmount; }
        public void setTransactionAmount(BigDecimal transactionAmount) { this.transactionAmount = transactionAmount; }
        public LocalDateTime getProcessingTimestamp() { return processingTimestamp; }
        public void setProcessingTimestamp(LocalDateTime processingTimestamp) { this.processingTimestamp = processingTimestamp; }
        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }
    }
    
    public static class PaymentProcessingResponse {
        private String processingStatus;
        private String settlementId;
        private String processingMessage;
        private BigDecimal processedAmount;
        private LocalDateTime processingTimestamp;
        private boolean successful;
        
        // Getters and setters
        public String getProcessingStatus() { return processingStatus; }
        public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }
        public String getSettlementId() { return settlementId; }
        public void setSettlementId(String settlementId) { this.settlementId = settlementId; }
        public String getProcessingMessage() { return processingMessage; }
        public void setProcessingMessage(String processingMessage) { this.processingMessage = processingMessage; }
        public BigDecimal getProcessedAmount() { return processedAmount; }
        public void setProcessedAmount(BigDecimal processedAmount) { this.processedAmount = processedAmount; }
        public LocalDateTime getProcessingTimestamp() { return processingTimestamp; }
        public void setProcessingTimestamp(LocalDateTime processingTimestamp) { this.processingTimestamp = processingTimestamp; }
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
    }
    
    public static class CardValidationResponse {
        private String validationStatus;
        private String cardType;
        private String issuerName;
        private String validationMessage;
        private LocalDateTime validationTimestamp;
        private boolean valid;
        
        // Getters and setters
        public String getValidationStatus() { return validationStatus; }
        public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
        public String getCardType() { return cardType; }
        public void setCardType(String cardType) { this.cardType = cardType; }
        public String getIssuerName() { return issuerName; }
        public void setIssuerName(String issuerName) { this.issuerName = issuerName; }
        public String getValidationMessage() { return validationMessage; }
        public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
        public LocalDateTime getValidationTimestamp() { return validationTimestamp; }
        public void setValidationTimestamp(LocalDateTime validationTimestamp) { this.validationTimestamp = validationTimestamp; }
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
    }
    
    public static class BankSystemStatusResponse {
        private String systemStatus;
        private String systemVersion;
        private String lastUpdateTime;
        private Integer activeConnections;
        private LocalDateTime statusCheckTimestamp;
        private boolean systemAvailable;
        
        // Getters and setters
        public String getSystemStatus() { return systemStatus; }
        public void setSystemStatus(String systemStatus) { this.systemStatus = systemStatus; }
        public String getSystemVersion() { return systemVersion; }
        public void setSystemVersion(String systemVersion) { this.systemVersion = systemVersion; }
        public String getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(String lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
        public Integer getActiveConnections() { return activeConnections; }
        public void setActiveConnections(Integer activeConnections) { this.activeConnections = activeConnections; }
        public LocalDateTime getStatusCheckTimestamp() { return statusCheckTimestamp; }
        public void setStatusCheckTimestamp(LocalDateTime statusCheckTimestamp) { this.statusCheckTimestamp = statusCheckTimestamp; }
        public boolean isSystemAvailable() { return systemAvailable; }
        public void setSystemAvailable(boolean systemAvailable) { this.systemAvailable = systemAvailable; }
    }

    // Custom exception classes for payment processing errors
    
    public static class PaymentAuthorizationException extends RuntimeException {
        public PaymentAuthorizationException(String message) { super(message); }
        public PaymentAuthorizationException(String message, Throwable cause) { super(message, cause); }
    }
    
    public static class PaymentNetworkTimeoutException extends RuntimeException {
        public PaymentNetworkTimeoutException(String message) { super(message); }
        public PaymentNetworkTimeoutException(String message, Throwable cause) { super(message, cause); }
    }
    
    public static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message) { super(message); }
        public PaymentProcessingException(String message, Throwable cause) { super(message, cause); }
    }
    
    public static class CardValidationException extends RuntimeException {
        public CardValidationException(String message) { super(message); }
        public CardValidationException(String message, Throwable cause) { super(message, cause); }
    }
    
    public static class BankSystemException extends RuntimeException {
        public BankSystemException(String message) { super(message); }
        public BankSystemException(String message, Throwable cause) { super(message, cause); }
    }
    
    public static class PaymentNetworkException extends RuntimeException {
        public PaymentNetworkException(String message) { super(message); }
        public PaymentNetworkException(String message, Throwable cause) { super(message, cause); }
    }
}