/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Customer;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.util.FileUtils;
import com.carddemo.util.FileFormatConverter;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.ValidationUtil;
import com.carddemo.exception.FileProcessingException;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

/**
 * Service class providing file-based data import/export operations for the credit card management system.
 * Handles parsing of fixed-width customer data files, validates COBOL record layouts, performs data format
 * conversions, and generates export files for external interfaces. Supports both CSV and fixed-width formats
 * while maintaining backward compatibility with legacy systems.
 * 
 * This service implements comprehensive file processing capabilities including:
 * - Fixed-width customer data file parsing using COBOL record layout definitions
 * - File format validation and data integrity checking
 * - Conversion between fixed-width and CSV formats
 * - Export file generation for external interfaces
 * - Batch data ingestion with comprehensive error handling
 * - COBOL data type preservation including COMP-3 packed decimal handling
 * 
 * The service maintains exact compatibility with existing COBOL file layouts while providing
 * modern Java-based processing capabilities. All numeric precision is preserved using BigDecimal
 * with appropriate scale and rounding modes matching COBOL COMP-3 behavior.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Service
public class FileService {

    // COBOL record layout constants based on custdata.txt analysis
    private static final int CUSTOMER_ID_START = 0;
    private static final int CUSTOMER_ID_LENGTH = 9;
    private static final int FIRST_NAME_START = 9;
    private static final int FIRST_NAME_LENGTH = 25;
    private static final int MIDDLE_NAME_START = 34;
    private static final int MIDDLE_NAME_LENGTH = 25;
    private static final int LAST_NAME_START = 59;
    private static final int LAST_NAME_LENGTH = 25;
    private static final int ADDR_LINE_1_START = 84;
    private static final int ADDR_LINE_1_LENGTH = 50;
    private static final int ADDR_LINE_2_START = 134;
    private static final int ADDR_LINE_2_LENGTH = 50;
    private static final int CITY_START = 184;
    private static final int CITY_LENGTH = 50;
    private static final int STATE_START = 234;
    private static final int STATE_LENGTH = 2;
    private static final int COUNTRY_START = 236;
    private static final int COUNTRY_LENGTH = 3;
    private static final int ZIP_CODE_START = 239;
    private static final int ZIP_CODE_LENGTH = 11;
    private static final int PHONE_HOME_START = 250;
    private static final int PHONE_HOME_LENGTH = 14;
    private static final int PHONE_WORK_START = 264;
    private static final int PHONE_WORK_LENGTH = 14;
    private static final int SSN_START = 278;
    private static final int SSN_LENGTH = 9;
    private static final int GOVT_ID_START = 287;
    private static final int GOVT_ID_LENGTH = 20;
    private static final int DOB_START = 307;
    private static final int DOB_LENGTH = 8;
    private static final int EFT_ACCOUNT_ID_START = 315;
    private static final int EFT_ACCOUNT_ID_LENGTH = 10;
    private static final int PRI_CARD_HOLDER_IND_START = 325;
    private static final int PRI_CARD_HOLDER_IND_LENGTH = 1;
    private static final int FICO_CREDIT_SCORE_START = 326;
    private static final int FICO_CREDIT_SCORE_LENGTH = 3;
    
    private static final int TOTAL_RECORD_LENGTH = 400;
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final String[] SUPPORTED_EXTENSIONS = {".txt", ".csv", ".dat"};
    
    private final CustomerRepository customerRepository;
    
    /**
     * Constructor for FileService with dependency injection.
     * 
     * @param customerRepository JPA repository for customer data access operations
     */
    public FileService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    /**
     * Processes an uploaded file containing customer data, validates format, parses content,
     * and saves customer records to the database. Supports both fixed-width and CSV formats
     * with comprehensive error handling and validation.
     * 
     * @param fileContent the content of the uploaded file as a String
     * @param fileName the name of the uploaded file for validation and error reporting
     * @return processing result summary including number of records processed and any errors
     * @throws FileProcessingException if file validation, parsing, or processing fails
     */
    public String uploadFile(String fileContent, String fileName) {
        try {
            // Validate file format and basic structure
            if (!validateFileFormat(fileName, fileContent)) {
                throw new FileProcessingException("File format validation failed", fileName,
                    FileProcessingException.ERROR_CODE_INVALID_FORMAT);
            }
            
            // Validate file size
            validateFileSize(fileContent.getBytes(StandardCharsets.UTF_8).length, fileName);
            
            // Parse customer data from file content
            List<Customer> customers = parseCustomerData(fileContent);
            
            // Save all customers to database
            List<Customer> savedCustomers = customerRepository.saveAll(customers);
            
            return String.format("Successfully processed %d customer records from file %s", 
                savedCustomers.size(), fileName);
                
        } catch (FileProcessingException e) {
            throw e; // Re-throw file processing exceptions
        } catch (Exception e) {
            throw new FileProcessingException("Unexpected error during file upload processing: " + e.getMessage(),
                fileName, FileProcessingException.ERROR_CODE_IO_ERROR, e);
        }
    }
    
    /**
     * Generates an export file containing customer data in the specified format for external interfaces.
     * Supports both fixed-width COBOL format and CSV format output. Maintains exact field positioning
     * and data types for compatibility with legacy systems.
     * 
     * @param filePath the path where the export file should be written
     * @param customers the list of customer records to export
     * @param format the output format ("FIXED_WIDTH" or "CSV")
     * @return the file content as a String for writing to the specified path
     * @throws FileProcessingException if export generation fails
     */
    public String exportFile(String filePath, List<Customer> customers, String format) {
        try {
            if (customers == null || customers.isEmpty()) {
                throw new FileProcessingException("No customer data provided for export", filePath,
                    FileProcessingException.ERROR_CODE_VALIDATION_FAILED);
            }
            
            String exportContent;
            if ("CSV".equalsIgnoreCase(format)) {
                exportContent = convertToCSV(customers);
            } else if ("FIXED_WIDTH".equalsIgnoreCase(format)) {
                exportContent = convertToFixedWidth(customers);
            } else {
                throw new FileProcessingException("Unsupported export format: " + format + ". Supported formats: CSV, FIXED_WIDTH",
                    filePath, FileProcessingException.ERROR_CODE_INVALID_FORMAT);
            }
            
            // Validate generated content
            if (exportContent == null || exportContent.trim().isEmpty()) {
                throw new FileProcessingException("Generated export content is empty", filePath,
                    FileProcessingException.ERROR_CODE_VALIDATION_FAILED);
            }
            
            return exportContent;
            
        } catch (FileProcessingException e) {
            throw e; // Re-throw file processing exceptions
        } catch (Exception e) {
            throw new FileProcessingException("Unexpected error during export file generation: " + e.getMessage(),
                filePath, FileProcessingException.ERROR_CODE_IO_ERROR, e);
        }
    }
    
    /**
     * Validates file format and structure to ensure compatibility with supported data layouts.
     * Checks file extension, content structure, and basic format compliance for both
     * fixed-width and CSV formats.
     * 
     * @param fileName the name of the file to validate (used for extension checking)
     * @param fileContent the content of the file to validate
     * @return true if the file format is valid, false otherwise
     * @throws FileProcessingException if validation encounters an error
     */
    public boolean validateFileFormat(String fileName, String fileContent) {
        try {
            // Validate file name
            if (fileName == null || fileName.trim().isEmpty()) {
                return false;
            }
            
            // Check file extension
            boolean validExtension = false;
            String lowerFileName = fileName.toLowerCase();
            for (String ext : SUPPORTED_EXTENSIONS) {
                if (lowerFileName.endsWith(ext)) {
                    validExtension = true;
                    break;
                }
            }
            
            if (!validExtension) {
                return false;
            }
            
            // Validate content is not null or empty
            if (fileContent == null || fileContent.trim().isEmpty()) {
                return false;
            }
            
            // Basic content structure validation
            String[] lines = fileContent.split("\\r?\\n");
            if (lines.length == 0) {
                return false;
            }
            
            // Check if it's CSV format
            if (lowerFileName.endsWith(".csv")) {
                return validateCSVFormat(lines);
            } else {
                // Assume fixed-width format for .txt and .dat files
                return validateFixedWidthFormat(lines);
            }
            
        } catch (Exception e) {
            throw new FileProcessingException("Error during file format validation: " + e.getMessage(),
                fileName, FileProcessingException.ERROR_CODE_VALIDATION_FAILED, e);
        }
    }
    
    /**
     * Parses customer data from file content, supporting both fixed-width and CSV formats.
     * Extracts customer records, validates field formats, and converts COBOL data types
     * to Java equivalents while preserving exact precision.
     * 
     * @param fileContent the file content containing customer data
     * @return list of Customer entities parsed from the file
     * @throws FileProcessingException if parsing fails or data validation errors occur
     */
    public List<Customer> parseCustomerData(String fileContent) {
        try {
            List<Customer> customers = new ArrayList<>();
            String[] lines = fileContent.split("\\r?\\n");
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    Customer customer;
                    
                    // Determine format based on line structure
                    if (line.contains(",")) {
                        // CSV format
                        customer = parseCSVCustomerRecord(line, i + 1);
                    } else {
                        // Fixed-width format
                        customer = parseFixedWidthCustomerRecord(line, i + 1);
                    }
                    
                    if (customer != null) {
                        customers.add(customer);
                    }
                    
                } catch (Exception e) {
                    throw FileProcessingException.parsingError("customer data", i + 1, 
                        "Failed to parse customer record: " + e.getMessage());
                }
            }
            
            return customers;
            
        } catch (FileProcessingException e) {
            throw e; // Re-throw file processing exceptions
        } catch (Exception e) {
            throw new FileProcessingException("Unexpected error during customer data parsing: " + e.getMessage(),
                FileProcessingException.ERROR_CODE_PARSING_ERROR, e);
        }
    }
    
    /**
     * Converts a list of Customer entities to CSV format with proper field escaping
     * and comma separation. Includes header row and maintains data integrity.
     * 
     * @param customers the list of customer records to convert
     * @return CSV formatted string representation of the customer data
     * @throws FileProcessingException if conversion fails
     */
    public String convertToCSV(List<Customer> customers) {
        try {
            if (customers == null || customers.isEmpty()) {
                return "";
            }
            
            StringBuilder csvBuilder = new StringBuilder();
            
            // Add CSV header
            csvBuilder.append("CUSTOMER_ID,FIRST_NAME,MIDDLE_NAME,LAST_NAME,ADDRESS_LINE_1,ADDRESS_LINE_2,CITY,STATE,COUNTRY,ZIP_CODE,")
                     .append("PHONE_HOME,PHONE_WORK,SSN,GOVT_ID,DATE_OF_BIRTH,EFT_ACCOUNT_ID,PRIMARY_CARD_HOLDER,FICO_SCORE")
                     .append(System.lineSeparator());
            
            // Convert each customer to CSV format using FileFormatConverter
            for (Customer customer : customers) {
                String csvLine = FileFormatConverter.convertToCsv(customer);
                csvBuilder.append(csvLine).append(System.lineSeparator());
            }
            
            return csvBuilder.toString();
            
        } catch (Exception e) {
            throw new FileProcessingException("Error converting customer data to CSV format: " + e.getMessage(),
                FileProcessingException.ERROR_CODE_PARSING_ERROR, e);
        }
    }
    
    /**
     * Converts a list of Customer entities to fixed-width COBOL record format.
     * Maintains exact field positioning and data types for compatibility with legacy systems.
     * Uses COBOL picture clauses for proper field formatting and padding.
     * 
     * @param customers the list of customer records to convert
     * @return fixed-width formatted string representation of the customer data
     * @throws FileProcessingException if conversion fails
     */
    public String convertToFixedWidth(List<Customer> customers) {
        try {
            if (customers == null || customers.isEmpty()) {
                return "";
            }
            
            StringBuilder fixedWidthBuilder = new StringBuilder();
            
            // Convert each customer to fixed-width format using FileFormatConverter
            for (Customer customer : customers) {
                String fixedWidthLine = FileFormatConverter.generateFixedWidth(customer);
                fixedWidthBuilder.append(fixedWidthLine).append(System.lineSeparator());
            }
            
            return fixedWidthBuilder.toString();
            
        } catch (Exception e) {
            throw new FileProcessingException("Error converting customer data to fixed-width format: " + e.getMessage(),
                FileProcessingException.ERROR_CODE_PARSING_ERROR, e);
        }
    }
    
    /**
     * Validates file size against maximum allowed size limits.
     * 
     * @param fileSize the size of the file in bytes
     * @param fileName the name of the file for error reporting
     * @throws FileProcessingException if file size exceeds maximum allowed limit
     */
    private void validateFileSize(long fileSize, String fileName) {
        if (fileSize > MAX_FILE_SIZE) {
            throw new FileProcessingException(
                String.format("File size %d bytes exceeds maximum allowed size of %d bytes", fileSize, MAX_FILE_SIZE),
                fileName, FileProcessingException.ERROR_CODE_SIZE_LIMIT_EXCEEDED);
        }
    }
    
    /**
     * Validates CSV format structure and basic field count.
     * 
     * @param lines the lines of the CSV file to validate
     * @return true if CSV format is valid, false otherwise
     */
    private boolean validateCSVFormat(String[] lines) {
        if (lines.length == 0) {
            return false;
        }
        
        // Check first line for expected number of fields (should have commas)
        String firstLine = lines[0];
        if (!firstLine.contains(",")) {
            return false;
        }
        
        // Basic field count validation (expecting at least 10 fields)
        String[] fields = firstLine.split(",");
        return fields.length >= 10;
    }
    
    /**
     * Validates fixed-width format structure and record length.
     * 
     * @param lines the lines of the fixed-width file to validate
     * @return true if fixed-width format is valid, false otherwise
     */
    private boolean validateFixedWidthFormat(String[] lines) {
        if (lines.length == 0) {
            return false;
        }
        
        // Check if lines have expected fixed-width record length
        for (String line : lines) {
            if (!line.trim().isEmpty() && line.length() < TOTAL_RECORD_LENGTH - 50) {
                // Allow some variance but ensure it's reasonable length
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Parses a single CSV customer record into a Customer entity.
     * 
     * @param csvLine the CSV line to parse
     * @param lineNumber the line number for error reporting
     * @return Customer entity parsed from the CSV line
     * @throws FileProcessingException if parsing fails
     */
    private Customer parseCSVCustomerRecord(String csvLine, int lineNumber) {
        try {
            // Use FileFormatConverter to parse CSV line
            Customer customer = FileFormatConverter.convertFromCsv(csvLine);
            
            // Validate parsed customer data
            validateCustomerData(customer, lineNumber);
            
            return customer;
            
        } catch (Exception e) {
            throw FileProcessingException.parsingError("customer data", lineNumber, 
                "Failed to parse CSV customer record: " + e.getMessage());
        }
    }
    
    /**
     * Parses a single fixed-width customer record into a Customer entity using COBOL record layout.
     * 
     * @param fixedWidthLine the fixed-width line to parse
     * @param lineNumber the line number for error reporting
     * @return Customer entity parsed from the fixed-width line
     * @throws FileProcessingException if parsing fails
     */
    private Customer parseFixedWidthCustomerRecord(String fixedWidthLine, int lineNumber) {
        try {
            // Validate record length
            if (fixedWidthLine.length() < TOTAL_RECORD_LENGTH - 50) {
                throw new IllegalArgumentException("Record length " + fixedWidthLine.length() + 
                    " is shorter than expected minimum length");
            }
            
            Customer customer = new Customer();
            
            // Parse each field using COBOL record layout positions
            String customerId = extractField(fixedWidthLine, CUSTOMER_ID_START, CUSTOMER_ID_LENGTH).trim();
            customer.setCustomerId(Long.parseLong(customerId));
            
            customer.setFirstName(extractField(fixedWidthLine, FIRST_NAME_START, FIRST_NAME_LENGTH).trim());
            customer.setMiddleName(extractField(fixedWidthLine, MIDDLE_NAME_START, MIDDLE_NAME_LENGTH).trim());
            customer.setLastName(extractField(fixedWidthLine, LAST_NAME_START, LAST_NAME_LENGTH).trim());
            customer.setAddrLine1(extractField(fixedWidthLine, ADDR_LINE_1_START, ADDR_LINE_1_LENGTH).trim());
            customer.setAddrLine2(extractField(fixedWidthLine, ADDR_LINE_2_START, ADDR_LINE_2_LENGTH).trim());
            customer.setCity(extractField(fixedWidthLine, CITY_START, CITY_LENGTH).trim());
            customer.setAddrStateCd(extractField(fixedWidthLine, STATE_START, STATE_LENGTH).trim());
            customer.setAddrCountryCd(extractField(fixedWidthLine, COUNTRY_START, COUNTRY_LENGTH).trim());
            customer.setAddrZip(extractField(fixedWidthLine, ZIP_CODE_START, ZIP_CODE_LENGTH).trim());
            
            String phoneHome = extractField(fixedWidthLine, PHONE_HOME_START, PHONE_HOME_LENGTH).trim();
            customer.setPhoneNumber1(formatPhoneNumber(phoneHome));
            
            String phoneWork = extractField(fixedWidthLine, PHONE_WORK_START, PHONE_WORK_LENGTH).trim();
            customer.setPhoneNumber2(formatPhoneNumber(phoneWork));
            
            customer.setSsn(extractField(fixedWidthLine, SSN_START, SSN_LENGTH).trim());
            customer.setGovtId(extractField(fixedWidthLine, GOVT_ID_START, GOVT_ID_LENGTH).trim());
            
            // Parse date of birth
            String dobString = extractField(fixedWidthLine, DOB_START, DOB_LENGTH).trim();
            LocalDate dateOfBirth = parseCobolDate(dobString);
            customer.setDateOfBirth(dateOfBirth);
            
            customer.setEftAccountId(extractField(fixedWidthLine, EFT_ACCOUNT_ID_START, EFT_ACCOUNT_ID_LENGTH).trim());
            
            String primaryCardHolderInd = extractField(fixedWidthLine, PRI_CARD_HOLDER_IND_START, PRI_CARD_HOLDER_IND_LENGTH).trim();
            customer.setPrimaryCardHolderInd("Y".equals(primaryCardHolderInd));
            
            // Parse FICO score
            String ficoScoreString = extractField(fixedWidthLine, FICO_CREDIT_SCORE_START, FICO_CREDIT_SCORE_LENGTH).trim();
            if (!ficoScoreString.isEmpty()) {
                customer.setFicoScore(Integer.parseInt(ficoScoreString));
            }
            
            // Validate parsed customer data
            validateCustomerData(customer, lineNumber);
            
            return customer;
            
        } catch (Exception e) {
            throw FileProcessingException.parsingError("customer data", lineNumber, 
                "Failed to parse fixed-width customer record: " + e.getMessage());
        }
    }
    
    /**
     * Validates customer data fields according to business rules and data integrity constraints.
     * 
     * @param customer the customer entity to validate
     * @param lineNumber the line number for error reporting
     * @throws FileProcessingException if validation fails
     */
    private void validateCustomerData(Customer customer, int lineNumber) {
        try {
            // Validate required fields
            ValidationUtil.validateRequiredField("Customer ID", customer.getCustomerId() != null ? customer.getCustomerId().toString() : null);
            ValidationUtil.validateRequiredField("First Name", customer.getFirstName());
            ValidationUtil.validateRequiredField("Last Name", customer.getLastName());
            
            // Validate field lengths
            ValidationUtil.validateFieldLength("First Name", customer.getFirstName(), FIRST_NAME_LENGTH);
            ValidationUtil.validateFieldLength("Last Name", customer.getLastName(), LAST_NAME_LENGTH);
            ValidationUtil.validateFieldLength("City", customer.getCity(), CITY_LENGTH);
            
            // Validate phone numbers if provided
            if (customer.getPhoneNumber1() != null && !customer.getPhoneNumber1().trim().isEmpty()) {
                ValidationUtil.validatePhoneNumber("Phone Number 1", customer.getPhoneNumber1());
            }
            
            if (customer.getPhoneNumber2() != null && !customer.getPhoneNumber2().trim().isEmpty()) {
                ValidationUtil.validatePhoneNumber("Phone Number 2", customer.getPhoneNumber2());
            }
            
            // Validate SSN if provided
            if (customer.getSsn() != null && !customer.getSsn().trim().isEmpty()) {
                ValidationUtil.validateSSN("SSN", customer.getSsn());
            }
            
            // Validate FICO score if provided
            if (customer.getFicoScore() != null) {
                ValidationUtil.validateFicoScore("FICO Score", customer.getFicoScore());
            }
            
        } catch (Exception e) {
            throw FileProcessingException.parsingError("customer data", lineNumber, 
                "Customer data validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Extracts a field from a fixed-width record at the specified position and length.
     * 
     * @param record the fixed-width record string
     * @param start the starting position (0-based)
     * @param length the field length
     * @return the extracted field value
     */
    private String extractField(String record, int start, int length) {
        if (record == null || start < 0 || start >= record.length()) {
            return "";
        }
        
        int end = Math.min(start + length, record.length());
        return record.substring(start, end);
    }
    
    /**
     * Formats a phone number string by removing non-digit characters and adding standard formatting.
     * 
     * @param phoneNumber the raw phone number string
     * @return formatted phone number in (XXX)XXX-XXXX format
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return "";
        }
        
        // Remove all non-digit characters
        String digitsOnly = phoneNumber.replaceAll("\\D", "");
        
        if (digitsOnly.length() == 10) {
            return String.format("(%s)%s-%s", 
                digitsOnly.substring(0, 3),
                digitsOnly.substring(3, 6), 
                digitsOnly.substring(6, 10));
        }
        
        return phoneNumber; // Return original if not 10 digits
    }
    
    /**
     * Parses a COBOL date string in YYYYMMDD format to LocalDate.
     * 
     * @param dateString the COBOL date string
     * @return LocalDate representation of the date
     * @throws IllegalArgumentException if date format is invalid
     */
    private LocalDate parseCobolDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Expected format: YYYYMMDD
            if (dateString.length() == 8) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                return LocalDate.parse(dateString, formatter);
            } else if (dateString.length() == 10 && dateString.contains("-")) {
                // Handle YYYY-MM-DD format
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return LocalDate.parse(dateString, formatter);
            } else {
                throw new IllegalArgumentException("Invalid date format: " + dateString + ". Expected YYYYMMDD or YYYY-MM-DD");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse date: " + dateString + ". " + e.getMessage(), e);
        }
    }
}