package com.carddemo.exception;

/**
 * Custom exception class for file processing errors including file format validation failures,
 * data parsing errors, and file I/O issues. Extends RuntimeException to provide standardized
 * error handling for file upload, export, and validation operations with detailed error messages.
 * 
 * This exception supports file-specific error codes and messages, providing constructors for
 * different error scenarios including file validation failures, parsing errors, and I/O exceptions.
 * Includes file name and line number context for debugging purposes.
 * 
 * Used throughout the CardDemo application for:
 * - File format validation failures
 * - Data parsing errors during file processing
 * - File I/O operations that encounter errors
 * - File upload and validation operations
 * - Export file generation errors
 * 
 * Integrates with Spring Boot's error handling framework through @ControllerAdvice
 * exception handlers for standardized REST API error responses.
 */
public class FileProcessingException extends RuntimeException {
    
    /**
     * Serial version UID for serialization compatibility
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * The name of the file being processed when the error occurred
     */
    private final String fileName;
    
    /**
     * The line number in the file where the error occurred (if applicable)
     */
    private final Integer lineNumber;
    
    /**
     * Application-specific error code for categorizing file processing errors
     */
    private final String errorCode;
    
    // Predefined error codes for common file processing scenarios
    public static final String ERROR_CODE_FILE_NOT_FOUND = "FILE_001";
    public static final String ERROR_CODE_INVALID_FORMAT = "FILE_002";
    public static final String ERROR_CODE_PARSING_ERROR = "FILE_003";
    public static final String ERROR_CODE_IO_ERROR = "FILE_004";
    public static final String ERROR_CODE_VALIDATION_FAILED = "FILE_005";
    public static final String ERROR_CODE_ENCODING_ERROR = "FILE_006";
    public static final String ERROR_CODE_SIZE_LIMIT_EXCEEDED = "FILE_007";
    public static final String ERROR_CODE_PERMISSION_DENIED = "FILE_008";
    
    /**
     * Default constructor for FileProcessingException with basic error message.
     * Used when minimal error information is available.
     */
    public FileProcessingException() {
        super("File processing error occurred");
        this.fileName = null;
        this.lineNumber = null;
        this.errorCode = "FILE_999"; // Generic error code
    }
    
    /**
     * Constructor for FileProcessingException with custom error message.
     * 
     * @param message Detailed error message describing the file processing failure
     */
    public FileProcessingException(String message) {
        super(message);
        this.fileName = null;
        this.lineNumber = null;
        this.errorCode = "FILE_999"; // Generic error code
    }
    
    /**
     * Constructor for FileProcessingException with error message and file name context.
     * 
     * @param message Detailed error message describing the file processing failure
     * @param fileName Name of the file being processed when the error occurred
     */
    public FileProcessingException(String message, String fileName) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = null;
        this.errorCode = "FILE_999"; // Generic error code
    }
    
    /**
     * Constructor for FileProcessingException with error message, file name, and line number.
     * 
     * @param message Detailed error message describing the file processing failure
     * @param fileName Name of the file being processed when the error occurred
     * @param lineNumber Line number in the file where the error occurred
     */
    public FileProcessingException(String message, String fileName, Integer lineNumber) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.errorCode = "FILE_999"; // Generic error code
    }
    
    /**
     * Constructor for FileProcessingException with error message, file name, and error code.
     * 
     * @param message Detailed error message describing the file processing failure
     * @param fileName Name of the file being processed when the error occurred
     * @param errorCode Application-specific error code for categorizing the error
     */
    public FileProcessingException(String message, String fileName, String errorCode) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = null;
        this.errorCode = errorCode != null ? errorCode : "FILE_999";
    }
    
    /**
     * Constructor for FileProcessingException with complete error context information.
     * 
     * @param message Detailed error message describing the file processing failure
     * @param fileName Name of the file being processed when the error occurred
     * @param lineNumber Line number in the file where the error occurred
     * @param errorCode Application-specific error code for categorizing the error
     */
    public FileProcessingException(String message, String fileName, Integer lineNumber, String errorCode) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.errorCode = errorCode != null ? errorCode : "FILE_999";
    }
    
    /**
     * Constructor for FileProcessingException with error message and underlying cause.
     * 
     * @param message Detailed error message describing the file processing failure
     * @param cause The underlying exception that caused this file processing error
     */
    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.fileName = null;
        this.lineNumber = null;
        this.errorCode = "FILE_999"; // Generic error code
    }
    
    /**
     * Constructor for FileProcessingException with complete error context and underlying cause.
     * 
     * @param message Detailed error message describing the file processing failure
     * @param fileName Name of the file being processed when the error occurred
     * @param lineNumber Line number in the file where the error occurred
     * @param errorCode Application-specific error code for categorizing the error
     * @param cause The underlying exception that caused this file processing error
     */
    public FileProcessingException(String message, String fileName, Integer lineNumber, String errorCode, Throwable cause) {
        super(message, cause);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.errorCode = errorCode != null ? errorCode : "FILE_999";
    }
    
    /**
     * Gets the name of the file being processed when the error occurred.
     * 
     * @return The file name, or null if not specified
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Gets the line number in the file where the error occurred.
     * 
     * @return The line number, or null if not applicable or not specified
     */
    public Integer getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Gets the application-specific error code for categorizing file processing errors.
     * 
     * @return The error code (never null, defaults to "FILE_999" for generic errors)
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Creates a formatted error message that includes file context information.
     * Overrides the default getMessage() to provide enhanced error details including
     * file name, line number, and error code when available.
     * 
     * @return Formatted error message with file processing context
     */
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getMessage());
        
        if (fileName != null) {
            sb.append(" [File: ").append(fileName);
            if (lineNumber != null) {
                sb.append(", Line: ").append(lineNumber);
            }
            sb.append("]");
        }
        
        if (errorCode != null && !"FILE_999".equals(errorCode)) {
            sb.append(" [Code: ").append(errorCode).append("]");
        }
        
        return sb.toString();
    }
    
    /**
     * Returns a string representation of this exception including all context information.
     * Useful for logging and debugging purposes.
     * 
     * @return String representation with complete error context
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(": ").append(getMessage());
        
        if (getCause() != null) {
            sb.append(" Caused by: ").append(getCause().toString());
        }
        
        return sb.toString();
    }
    
    /**
     * Utility method to check if this exception has file context information.
     * 
     * @return true if either fileName or lineNumber is specified
     */
    public boolean hasFileContext() {
        return fileName != null || lineNumber != null;
    }
    
    /**
     * Utility method to check if this exception has line number information.
     * 
     * @return true if lineNumber is specified
     */
    public boolean hasLineNumber() {
        return lineNumber != null;
    }
    
    /**
     * Utility method to create a file validation error with standard error code.
     * 
     * @param fileName Name of the file that failed validation
     * @param validationMessage Specific validation failure message
     * @return FileProcessingException configured for validation failure
     */
    public static FileProcessingException validationError(String fileName, String validationMessage) {
        return new FileProcessingException(
            "File validation failed: " + validationMessage,
            fileName,
            ERROR_CODE_VALIDATION_FAILED
        );
    }
    
    /**
     * Utility method to create a file parsing error with line number context.
     * 
     * @param fileName Name of the file being parsed
     * @param lineNumber Line number where parsing failed
     * @param parseError Specific parsing error description
     * @return FileProcessingException configured for parsing failure
     */
    public static FileProcessingException parsingError(String fileName, int lineNumber, String parseError) {
        return new FileProcessingException(
            "File parsing error: " + parseError,
            fileName,
            lineNumber,
            ERROR_CODE_PARSING_ERROR
        );
    }
    
    /**
     * Utility method to create a file I/O error with underlying cause.
     * 
     * @param fileName Name of the file involved in I/O operation
     * @param ioError Description of the I/O error
     * @param cause Underlying I/O exception
     * @return FileProcessingException configured for I/O failure
     */
    public static FileProcessingException ioError(String fileName, String ioError, Throwable cause) {
        return new FileProcessingException(
            "File I/O error: " + ioError,
            fileName,
            null,
            ERROR_CODE_IO_ERROR,
            cause
        );
    }
}

    /**
     * Gets the underlying cause of this file processing exception.
     * This method overrides the inherited getCause() to provide enhanced
     * access to the root cause exception with additional context documentation.
     * 
     * @return The underlying Throwable that caused this exception, or null if no cause was set
     */
    @Override
    public Throwable getCause() {
        return super.getCause();
    }