package com.carddemo.exception;

import java.math.BigDecimal;

/**
 * Custom exception for BigDecimal precision errors in financial calculations.
 * Thrown when monetary calculations detect precision loss or rounding discrepancies
 * that would violate COBOL COMP-3 packed decimal accuracy requirements.
 * Critical for maintaining penny-level accuracy in interest calculations and transaction processing.
 * 
 * This exception ensures compliance with financial regulations requiring precise
 * decimal arithmetic and prevents calculation errors that could lead to audit issues
 * or customer disputes.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public class DataPrecisionException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Expected precision (number of digits after decimal point)
     */
    private final int precisionExpected;
    
    /**
     * Actual precision that was encountered
     */
    private final int precisionActual;
    
    /**
     * The problematic BigDecimal value that caused the precision error
     */
    private final BigDecimal problematicValue;
    
    /**
     * Name of the operation being performed when the error occurred
     */
    private final String operationName;
    
    /**
     * Name of the field or variable involved in the calculation
     */
    private final String fieldName;
    
    /**
     * Transaction context or ID for audit trail purposes
     */
    private final String transactionContext;
    
    /**
     * Constructs a DataPrecisionException with basic precision information.
     * 
     * @param message the detail message
     * @param precisionExpected the expected precision
     * @param precisionActual the actual precision encountered
     * @param problematicValue the BigDecimal value that caused the error
     */
    public DataPrecisionException(String message, int precisionExpected, int precisionActual, BigDecimal problematicValue) {
        super(message);
        this.precisionExpected = precisionExpected;
        this.precisionActual = precisionActual;
        this.problematicValue = problematicValue;
        this.operationName = null;
        this.fieldName = null;
        this.transactionContext = null;
    }
    
    /**
     * Constructs a DataPrecisionException with comprehensive context information.
     * 
     * @param message the detail message
     * @param precisionExpected the expected precision
     * @param precisionActual the actual precision encountered
     * @param problematicValue the BigDecimal value that caused the error
     * @param operationName the name of the operation being performed
     * @param fieldName the name of the field or variable involved
     * @param transactionContext the transaction context for audit trail
     */
    public DataPrecisionException(String message, int precisionExpected, int precisionActual, 
                                BigDecimal problematicValue, String operationName, 
                                String fieldName, String transactionContext) {
        super(message);
        this.precisionExpected = precisionExpected;
        this.precisionActual = precisionActual;
        this.problematicValue = problematicValue;
        this.operationName = operationName;
        this.fieldName = fieldName;
        this.transactionContext = transactionContext;
    }
    
    /**
     * Constructs a DataPrecisionException with a cause.
     * 
     * @param message the detail message
     * @param precisionExpected the expected precision
     * @param precisionActual the actual precision encountered
     * @param problematicValue the BigDecimal value that caused the error
     * @param cause the cause of this exception
     */
    public DataPrecisionException(String message, int precisionExpected, int precisionActual, 
                                BigDecimal problematicValue, Throwable cause) {
        super(message, cause);
        this.precisionExpected = precisionExpected;
        this.precisionActual = precisionActual;
        this.problematicValue = problematicValue;
        this.operationName = null;
        this.fieldName = null;
        this.transactionContext = null;
    }
    
    /**
     * Gets the expected precision (number of decimal places).
     * 
     * @return the expected precision
     */
    public int getPrecisionExpected() {
        return precisionExpected;
    }
    
    /**
     * Gets the actual precision that was encountered.
     * 
     * @return the actual precision
     */
    public int getPrecisionActual() {
        return precisionActual;
    }
    
    /**
     * Gets the problematic BigDecimal value that caused the precision error.
     * 
     * @return the problematic value
     */
    public BigDecimal getProblematicValue() {
        return problematicValue;
    }
    
    /**
     * Gets the name of the operation being performed when the error occurred.
     * 
     * @return the operation name, or null if not specified
     */
    public String getOperationName() {
        return operationName;
    }
    
    /**
     * Gets the name of the field or variable involved in the calculation.
     * 
     * @return the field name, or null if not specified
     */
    public String getFieldName() {
        return fieldName;
    }
    
    /**
     * Gets the transaction context or ID for audit trail purposes.
     * 
     * @return the transaction context, or null if not specified
     */
    public String getTransactionContext() {
        return transactionContext;
    }
    
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        
        sb.append(" [Expected precision: ").append(precisionExpected);
        sb.append(", Actual precision: ").append(precisionActual).append("]");
        
        if (problematicValue != null) {
            sb.append(" [Value: ").append(problematicValue).append("]");
        }
        
        if (operationName != null) {
            sb.append(" [Operation: ").append(operationName).append("]");
        }
        
        if (fieldName != null) {
            sb.append(" [Field: ").append(fieldName).append("]");
        }
        
        if (transactionContext != null) {
            sb.append(" [Transaction: ").append(transactionContext).append("]");
        }
        
        return sb.toString();
    }
}