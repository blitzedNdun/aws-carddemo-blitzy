package com.carddemo.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Spring Web interceptor for CICS transaction code processing.
 * 
 * This interceptor extracts and validates transaction codes from HTTP requests,
 * providing transaction context management and routing logic that replicates
 * CICS transaction processing patterns. It logs transaction timing and sets
 * transaction attributes for downstream services.
 * 
 * Based on the COBOL-to-Java migration requirements from the technical specification,
 * this component maintains functional parity with the original CICS transaction
 * processing while leveraging Spring Boot ecosystem capabilities.
 */
@Component
public class TransactionInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionInterceptor.class);

    // ThreadLocal storage for transaction context to ensure thread safety
    private static final ThreadLocal<TransactionContext> transactionContextHolder = new ThreadLocal<>();

    // CICS transaction code validation pattern - 8 character codes with two possible formats:
    // Pattern 1: CO + 3 letters + 2 digits + C (e.g., COSGN00C)
    // Pattern 2: CO + 4 letters + 1 letter + C (e.g., COACTVWC)
    private static final Pattern TRANSACTION_CODE_PATTERN = Pattern.compile("^CO([A-Z]{3}[0-9]{2}|[A-Z]{4}[A-Z])C$");

    // Valid CICS transaction codes based on the technical specification
    private static final Set<String> VALID_TRANSACTION_CODES = Set.of(
        "COSGN00C", // Sign-on transaction
        "COMEN01C", // Main menu transaction  
        "COADM01C", // Admin menu transaction
        "COTRN00C", // Transaction list transaction
        "COTRN01C", // Transaction add transaction
        "COTRN02C", // Transaction detail transaction
        "COACTVWC", // Account view transaction
        "COACTUPC", // Account update transaction
        "COBIL00C", // Bill payment transaction
        "COCRDLIC", // Card list transaction
        "COCRDSLC", // Card select transaction
        "COCRDUPC", // Card update transaction
        "COUSR00C", // User list transaction
        "COUSR01C", // User add transaction
        "COUSR02C", // User update transaction
        "COUSR03C", // User detail transaction
        "CORPT00C"  // Report transaction
    );

    // HTTP header names for transaction code extraction
    private static final String TRANSACTION_CODE_HEADER = "X-Transaction-Code";
    private static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";

    // Request attribute names for transaction context
    private static final String TRANSACTION_CODE_ATTRIBUTE = "transactionCode";
    private static final String TRANSACTION_ID_ATTRIBUTE = "transactionId";
    private static final String TRANSACTION_START_TIME_ATTRIBUTE = "transactionStartTime";

    /**
     * Pre-handle method called before the controller method execution.
     * 
     * Extracts transaction codes from request headers or parameters,
     * validates them against CICS transaction patterns, and sets up
     * transaction context for downstream processing.
     * 
     * @param request  HTTP servlet request
     * @param response HTTP servlet response  
     * @param handler  Handler object for the request
     * @return true to continue processing, false to abort
     * @throws Exception if an error occurs during processing
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        long startTime = System.currentTimeMillis();
        
        // Generate unique transaction ID for request correlation
        String transactionId = UUID.randomUUID().toString();
        
        // Extract transaction code from request
        String transactionCode = extractTransactionCode(request);
        
        // Set transaction start time for performance monitoring
        request.setAttribute(TRANSACTION_START_TIME_ATTRIBUTE, startTime);
        request.setAttribute(TRANSACTION_ID_ATTRIBUTE, transactionId);
        
        // Add transaction ID to MDC for structured logging
        MDC.put("transactionId", transactionId);
        MDC.put("requestUri", request.getRequestURI());
        MDC.put("httpMethod", request.getMethod());
        
        if (transactionCode != null) {
            // Validate transaction code against CICS patterns
            if (isValidTransactionCode(transactionCode)) {
                request.setAttribute(TRANSACTION_CODE_ATTRIBUTE, transactionCode);
                
                // Create transaction context for thread-local storage
                TransactionContext context = new TransactionContext(
                    transactionId,
                    transactionCode,
                    startTime,
                    request.getRequestURI(),
                    request.getMethod()
                );
                
                transactionContextHolder.set(context);
                
                // Add transaction code to MDC
                MDC.put("transactionCode", transactionCode);
                
                logger.info("Transaction started - Code: {}, ID: {}, URI: {}, Method: {}", 
                    transactionCode, transactionId, request.getRequestURI(), request.getMethod());
                
                // Set CICS-equivalent transaction routing headers for downstream services
                response.setHeader("X-CICS-Transaction-Code", transactionCode);
                response.setHeader("X-CICS-Transaction-Id", transactionId);
                
            } else {
                logger.warn("Invalid transaction code: {} for request: {} {}", 
                    transactionCode, request.getMethod(), request.getRequestURI());
                
                // Set error response for invalid transaction code
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\":\"INVALID_TRANSACTION_CODE\",\"message\":\"Transaction code " + 
                    transactionCode + " is not valid\",\"code\":\"INVTRAN\"}"
                );
                
                // Clean up MDC
                MDC.clear();
                return false;
            }
        } else {
            // For requests without transaction codes, still set up basic context
            TransactionContext context = new TransactionContext(
                transactionId,
                null,
                startTime,
                request.getRequestURI(),
                request.getMethod()
            );
            
            transactionContextHolder.set(context);
            
            logger.debug("Request without transaction code - ID: {}, URI: {}, Method: {}", 
                transactionId, request.getRequestURI(), request.getMethod());
        }
        
        return true;
    }

    /**
     * Post-handle method called after controller execution but before view rendering.
     * 
     * Processes transaction completion, updates timing information, and prepares
     * response headers with transaction metadata.
     * 
     * @param request      HTTP servlet request
     * @param response     HTTP servlet response
     * @param handler      Handler object for the request
     * @param modelAndView ModelAndView object (may be null)
     * @throws Exception if an error occurs during processing
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                          Object handler, ModelAndView modelAndView) throws Exception {
        
        TransactionContext context = transactionContextHolder.get();
        if (context != null) {
            long currentTime = System.currentTimeMillis();
            long processingTime = currentTime - context.getStartTime();
            
            // Update transaction context with processing time
            context.setProcessingTime(processingTime);
            
            // Add processing time to response headers for monitoring
            response.setHeader("X-Processing-Time-Ms", String.valueOf(processingTime));
            
            // Log transaction processing completion
            if (context.getTransactionCode() != null) {
                logger.info("Transaction processing completed - Code: {}, ID: {}, Processing time: {} ms, Status: {}", 
                    context.getTransactionCode(), context.getTransactionId(), 
                    processingTime, response.getStatus());
            } else {
                logger.debug("Request processing completed - ID: {}, Processing time: {} ms, Status: {}", 
                    context.getTransactionId(), processingTime, response.getStatus());
            }
            
            // Set CICS-equivalent response headers for transaction completion
            response.setHeader("X-CICS-Response-Time", String.valueOf(processingTime));
            response.setHeader("X-CICS-Status", getTransactionStatus(response.getStatus()));
        }
    }

    /**
     * After-completion method called after the complete request has finished.
     * 
     * Performs final cleanup of transaction context, logs transaction summary,
     * and clears ThreadLocal storage to prevent memory leaks.
     * 
     * @param request  HTTP servlet request
     * @param response HTTP servlet response
     * @param handler  Handler object for the request
     * @param ex       Exception thrown during processing (may be null)
     * @throws Exception if an error occurs during cleanup
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) throws Exception {
        
        try {
            TransactionContext context = transactionContextHolder.get();
            if (context != null) {
                long endTime = System.currentTimeMillis();
                long totalTime = endTime - context.getStartTime();
                
                // Log final transaction summary
                if (ex != null) {
                    logger.error("Transaction completed with exception - Code: {}, ID: {}, Total time: {} ms, Exception: {}", 
                        context.getTransactionCode(), context.getTransactionId(), 
                        totalTime, ex.getMessage(), ex);
                } else {
                    if (context.getTransactionCode() != null) {
                        logger.info("Transaction completed successfully - Code: {}, ID: {}, Total time: {} ms", 
                            context.getTransactionCode(), context.getTransactionId(), totalTime);
                        
                        // Log performance metrics for CICS transaction monitoring
                        logTransactionMetrics(context, totalTime, response.getStatus());
                    } else {
                        logger.debug("Request completed - ID: {}, Total time: {} ms", 
                            context.getTransactionId(), totalTime);
                    }
                }
            }
        } finally {
            // Critical cleanup to prevent memory leaks
            transactionContextHolder.remove();
            MDC.clear();
        }
    }

    /**
     * Extracts transaction code from HTTP request headers or parameters.
     * 
     * Checks for transaction code in the following order:
     * 1. X-Transaction-Code header
     * 2. transactionCode request parameter
     * 3. Derived from request URI pattern matching
     * 
     * @param request HTTP servlet request
     * @return transaction code or null if not found
     */
    private String extractTransactionCode(HttpServletRequest request) {
        // Check for explicit transaction code header
        String transactionCode = request.getHeader(TRANSACTION_CODE_HEADER);
        if (transactionCode != null && !transactionCode.trim().isEmpty()) {
            return transactionCode.trim().toUpperCase();
        }
        
        // Check for transaction code parameter
        transactionCode = request.getParameter("transactionCode");
        if (transactionCode != null && !transactionCode.trim().isEmpty()) {
            return transactionCode.trim().toUpperCase();
        }
        
        // Attempt to derive transaction code from URI patterns
        String requestUri = request.getRequestURI();
        if (requestUri != null) {
            // Map common URI patterns to transaction codes
            if (requestUri.contains("/auth/signon")) {
                return "COSGN00C";
            } else if (requestUri.contains("/menus/main")) {
                return "COMEN01C";
            } else if (requestUri.contains("/menus/admin")) {
                return "COADM01C";
            } else if (requestUri.contains("/transactions/list")) {
                return "COTRN00C";
            } else if (requestUri.contains("/transactions/add")) {
                return "COTRN01C";
            } else if (requestUri.contains("/transactions/detail")) {
                return "COTRN02C";
            } else if (requestUri.contains("/accounts/view")) {
                return "COACTVWC";
            } else if (requestUri.contains("/accounts/update")) {
                return "COACTUPC";
            } else if (requestUri.contains("/payments/process")) {
                return "COBIL00C";
            } else if (requestUri.contains("/cards/list")) {
                return "COCRDLIC";
            } else if (requestUri.contains("/cards/select")) {
                return "COCRDSLC";
            } else if (requestUri.contains("/cards/update")) {
                return "COCRDUPC";
            } else if (requestUri.contains("/users/list")) {
                return "COUSR00C";
            } else if (requestUri.contains("/users/add")) {
                return "COUSR01C";
            } else if (requestUri.contains("/users/update")) {
                return "COUSR02C";
            } else if (requestUri.contains("/users/detail")) {
                return "COUSR03C";
            } else if (requestUri.contains("/reports")) {
                return "CORPT00C";
            }
        }
        
        return null;
    }

    /**
     * Validates transaction code against CICS transaction patterns.
     * 
     * Checks both format (7-character pattern: LLLLNNL) and
     * against the whitelist of valid transaction codes.
     * 
     * @param transactionCode transaction code to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidTransactionCode(String transactionCode) {
        if (transactionCode == null || transactionCode.trim().isEmpty()) {
            return false;
        }
        
        String code = transactionCode.trim().toUpperCase();
        
        // Check format pattern (8 characters: CO + (3 letters + 2 digits) OR (4 letters + 1 letter) + C)
        if (!TRANSACTION_CODE_PATTERN.matcher(code).matches()) {
            return false;
        }
        
        // Check against whitelist of valid CICS transaction codes
        return VALID_TRANSACTION_CODES.contains(code);
    }

    /**
     * Maps HTTP status codes to CICS-equivalent transaction status.
     * 
     * @param httpStatus HTTP status code
     * @return CICS transaction status string
     */
    private String getTransactionStatus(int httpStatus) {
        if (httpStatus >= 200 && httpStatus < 300) {
            return "NORMAL";
        } else if (httpStatus >= 400 && httpStatus < 500) {
            return "ABEND";
        } else if (httpStatus >= 500) {
            return "ERROR";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Logs transaction performance metrics for monitoring.
     * 
     * @param context transaction context
     * @param totalTime total processing time in milliseconds
     * @param httpStatus HTTP response status
     */
    private void logTransactionMetrics(TransactionContext context, long totalTime, int httpStatus) {
        // Structure metrics for monitoring systems (Prometheus/Grafana)
        logger.info("TRANSACTION_METRICS - Code: {}, ID: {}, URI: {}, Method: {}, " +
                   "TotalTime: {}ms, ProcessingTime: {}ms, Status: {}, HttpStatus: {}", 
            context.getTransactionCode(),
            context.getTransactionId(),
            context.getRequestUri(),
            context.getHttpMethod(),
            totalTime,
            context.getProcessingTime(),
            getTransactionStatus(httpStatus),
            httpStatus
        );
    }

    /**
     * Gets the current transaction context from ThreadLocal storage.
     * 
     * @return current transaction context or null if not set
     */
    public static TransactionContext getCurrentTransactionContext() {
        return transactionContextHolder.get();
    }

    /**
     * Inner class representing transaction context information.
     * 
     * Holds transaction metadata for the duration of the request
     * processing, providing context for downstream services.
     */
    public static class TransactionContext {
        private final String transactionId;
        private final String transactionCode;
        private final long startTime;
        private final String requestUri;
        private final String httpMethod;
        private long processingTime;

        public TransactionContext(String transactionId, String transactionCode, 
                                long startTime, String requestUri, String httpMethod) {
            this.transactionId = transactionId;
            this.transactionCode = transactionCode;
            this.startTime = startTime;
            this.requestUri = requestUri;
            this.httpMethod = httpMethod;
            this.processingTime = 0;
        }

        // Getters
        public String getTransactionId() {
            return transactionId;
        }

        public String getTransactionCode() {
            return transactionCode;
        }

        public long getStartTime() {
            return startTime;
        }

        public String getRequestUri() {
            return requestUri;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public long getProcessingTime() {
            return processingTime;
        }

        // Setter for processing time
        public void setProcessingTime(long processingTime) {
            this.processingTime = processingTime;
        }

        @Override
        public String toString() {
            return "TransactionContext{" +
                   "transactionId='" + transactionId + '\'' +
                   ", transactionCode='" + transactionCode + '\'' +
                   ", startTime=" + startTime +
                   ", requestUri='" + requestUri + '\'' +
                   ", httpMethod='" + httpMethod + '\'' +
                   ", processingTime=" + processingTime +
                   '}';
        }
    }
}