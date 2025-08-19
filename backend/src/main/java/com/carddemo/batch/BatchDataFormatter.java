package com.carddemo.batch;

import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.FormatUtil;
import com.carddemo.util.StringUtil;
import com.carddemo.util.Constants;
import com.carddemo.exception.ValidationException;

import java.math.BigDecimal;
import java.lang.StringBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * Utility class for formatting batch output data to match COBOL report layouts.
 * 
 * This class provides methods for formatting various data types to match COBOL output
 * specifications found in CBSTM03A.CBL and CBTRN03C.cbl. Supports both fixed-width
 * plain text format and HTML format report generation with exact compatibility 
 * to mainframe output formats.
 * 
 * Key formatting capabilities:
 * - Fixed-width field formatting matching COBOL layouts
 * - Numeric formatting with leading zeros and decimal alignment
 * - Date and timestamp formatting for reports
 * - Page break and header management
 * - HTML report generation with proper tags
 * - Account summary and transaction detail formatting
 * 
 * Based on COBOL program structures from CBSTM03A.CBL:
 * - STATEMENT-LINES for plain text formatting
 * - HTML-LINES for HTML report formatting
 * - Fixed field positions and lengths from copybook layouts
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public class BatchDataFormatter {

    // Page management constants using Constants class
    private static final int DEFAULT_LINES_PER_PAGE = Constants.PAGE_SIZE;
    private static final int HEADER_LINES = 6;
    private static final int FOOTER_LINES = 3;
    
    // Report formatting constants from CBSTM03A.CBL and Constants
    private static final int STATEMENT_LINE_LENGTH = Constants.REPORT_WIDTH;
    private static final String STATEMENT_HEADER_PATTERN = "%-20s %11s %-30s %8s";
    private static final String TRANSACTION_DETAIL_PATTERN = "%-8s %-10s %-40s %12s";
    private static final String ACCOUNT_SUMMARY_PATTERN = "%-40s %12s";
    
    // HTML formatting constants
    private static final String HTML_TABLE_START = "<table border=\"1\" cellpadding=\"3\" cellspacing=\"0\">";
    private static final String HTML_TABLE_END = "</table>";
    private static final String HTML_ROW_START = "<tr>";
    private static final String HTML_ROW_END = "</tr>";
    private static final String HTML_HEADER_CELL = "<th>%s</th>";
    private static final String HTML_DATA_CELL = "<td>%s</td>";
    
    // Page tracking state
    private static final HashMap<String, Integer> pageCounters = new HashMap<>();
    private static final HashMap<String, Integer> lineCounters = new HashMap<>();

    /**
     * Formats a string to fixed width with specified alignment.
     * Replicates COBOL MOVE to formatted fields with padding/truncation.
     * 
     * @param value the string value to format
     * @param width the target field width
     * @param leftAlign true for left alignment, false for right alignment
     * @return formatted fixed-width string
     * @throws ValidationException if width is invalid
     */
    public static String formatFixedWidth(String value, int width, boolean leftAlign) {
        if (width <= 0) {
            ValidationException ex = new ValidationException("Invalid field width");
            ex.addFieldError("width", "Field width must be positive: " + width);
            throw ex;
        }
        
        if (value == null) {
            value = "";
        }
        
        // Use StringUtil.truncateOrPad for proper field handling
        String truncated = StringUtil.truncateOrPad(value, width);
        
        if (leftAlign) {
            return StringUtil.padRight(truncated, width);
        } else {
            return StringUtil.padLeft(truncated, width);
        }
    }

    /**
     * Formats numeric values with zero-padding and decimal alignment.
     * Matches COBOL PIC clause numeric formatting with COMP-3 precision.
     * 
     * @param value the BigDecimal value to format
     * @param totalWidth total field width including decimal point
     * @param decimalPlaces number of decimal places
     * @param signed whether to include sign indicator
     * @return formatted numeric string
     * @throws ValidationException if formatting parameters are invalid
     */
    public static String formatNumeric(BigDecimal value, int totalWidth, int decimalPlaces, boolean signed) {
        if (totalWidth <= 0) {
            ValidationException ex = new ValidationException("Invalid field width for numeric formatting");
            ex.addFieldError("totalWidth", "Width must be positive: " + totalWidth);
            throw ex;
        }
        
        if (value == null) {
            value = BigDecimal.ZERO;
        }
        
        // Use CobolDataConverter to maintain COMP-3 precision
        BigDecimal scaledValue = CobolDataConverter.formatPackedDecimal(value, decimalPlaces);
        
        // Alternative using fromComp3 for packed decimal conversion if needed
        if (value.scale() != decimalPlaces) {
            scaledValue = CobolDataConverter.fromComp3(
                CobolDataConverter.toCobolNumeric(value), decimalPlaces);
        }
        
        // Use FormatUtil for additional formatting support
        String baseFormatted = FormatUtil.formatNumeric(scaledValue, totalWidth, decimalPlaces);
        
        // Apply final alignment and padding
        return StringUtil.padLeft(baseFormatted, totalWidth);
    }

    /**
     * Formats currency amounts with proper sign and decimal handling.
     * Uses FormatUtil for consistent currency display across reports.
     * 
     * @param amount the currency amount to format
     * @param width total field width
     * @return formatted currency string
     */
    public static String formatCurrency(BigDecimal amount, int width) {
        String formatted = FormatUtil.formatCurrency(amount);
        return StringUtil.padLeft(formatted, width);
    }

    /**
     * Formats dates for various report contexts.
     * Uses DateConversionUtil for COBOL-compatible date formatting.
     * 
     * @param date the date to format
     * @param formatType type of date format (HEADER, REPORT, TIMESTAMP)
     * @return formatted date string
     * @throws ValidationException if date format type is invalid
     */
    public static String formatDate(LocalDateTime date, String formatType) {
        if (date == null) {
            date = LocalDateTime.now();
        }
        
        // Validate the date format before processing
        String dateString = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if (!DateConversionUtil.validateDateFormat(dateString)) {
            ValidationException ex = new ValidationException("Invalid date for formatting");
            ex.addFieldError("date", "Date validation failed: " + dateString);
            throw ex;
        }
        
        switch (formatType.toUpperCase()) {
            case "HEADER":
                return DateConversionUtil.formatHeaderDate(date);
            case "REPORT":
                return DateConversionUtil.formatReportDate(date);
            case "TIMESTAMP":
                return DateConversionUtil.formatTimestamp(date);
            default:
                ValidationException ex = new ValidationException("Invalid date format type");
                ex.addFieldError("formatType", "Unsupported format type: " + formatType);
                throw ex;
        }
    }

    /**
     * Generates report header with page number and timestamp.
     * Based on STATEMENT-LINES header structure from CBSTM03A.CBL.
     * 
     * @param reportTitle the title of the report
     * @param pageNumber current page number
     * @param reportDate report generation date
     * @return formatted header lines
     */
    public static String formatReportHeader(String reportTitle, int pageNumber, LocalDateTime reportDate) {
        StringBuilder header = new StringBuilder();
        
        // Line 1: Report title and page number
        String titleLine = String.format(STATEMENT_HEADER_PATTERN,
            reportTitle,
            "PAGE " + String.format("%4d", pageNumber),
            "",
            formatDate(reportDate, "HEADER"));
        header.append(padToReportWidth(titleLine)).append("\n");
        
        // Line 2: Blank line
        header.append(padToReportWidth("")).append("\n");
        
        // Line 3: Column headers
        String headerLine = String.format(STATEMENT_HEADER_PATTERN,
            "TRANSACTION DATE",
            "REFERENCE",
            "DESCRIPTION",
            "AMOUNT");
        header.append(padToReportWidth(headerLine)).append("\n");
        
        // Line 4: Separator line
        header.append(StringUtil.padRight("", Constants.REPORT_WIDTH, '-')).append("\n");
        
        return header.toString();
    }

    /**
     * Generates report footer with page totals and timestamp.
     * 
     * @param pageTotal total amount for current page
     * @param recordCount number of records on page
     * @param timestamp footer timestamp
     * @return formatted footer lines
     */
    public static String formatReportFooter(BigDecimal pageTotal, int recordCount, LocalDateTime timestamp) {
        StringBuilder footer = new StringBuilder();
        
        // Separator line
        footer.append(StringUtil.padRight("", Constants.REPORT_WIDTH, '-')).append("\n");
        
        // Page total line
        String totalLine = String.format(ACCOUNT_SUMMARY_PATTERN,
            "PAGE TOTAL (" + recordCount + " TRANSACTIONS):",
            formatCurrency(pageTotal, 12));
        footer.append(padToReportWidth(totalLine)).append("\n");
        
        // Timestamp line
        String timestampLine = formatDate(timestamp, "TIMESTAMP");
        footer.append(padToReportWidth(timestampLine)).append("\n");
        
        return footer.toString();
    }

    /**
     * Determines if a page break is needed based on line count.
     * 
     * @param reportId identifier for tracking page state
     * @param linesPerPage maximum lines per page
     * @return true if page break is needed
     */
    public static boolean calculatePageBreak(String reportId, int linesPerPage) {
        int currentLine = lineCounters.getOrDefault(reportId, 0);
        return currentLine >= (linesPerPage - FOOTER_LINES);
    }

    /**
     * Formats a page break with new page header.
     * 
     * @param reportId identifier for tracking page state
     * @param reportTitle title for new page header
     * @return formatted page break content
     */
    public static String formatPageBreak(String reportId, String reportTitle) {
        // Increment page counter
        int pageNumber = pageCounters.getOrDefault(reportId, 0) + 1;
        pageCounters.put(reportId, pageNumber);
        
        // Reset line counter
        lineCounters.put(reportId, HEADER_LINES);
        
        // Generate new page with header
        StringBuilder pageBreak = new StringBuilder();
        pageBreak.append("\f"); // Form feed character
        pageBreak.append(formatReportHeader(reportTitle, pageNumber, LocalDateTime.now()));
        
        return pageBreak.toString();
    }

    /**
     * Formats transaction detail line.
     * Based on transaction detail formatting from CBSTM03A.CBL.
     * 
     * @param transactionDate transaction date
     * @param referenceNumber transaction reference
     * @param description transaction description
     * @param amount transaction amount
     * @return formatted detail line
     */
    public static String formatDetailLine(String transactionDate, String referenceNumber, 
                                        String description, BigDecimal amount) {
        String detailLine = String.format(TRANSACTION_DETAIL_PATTERN,
            StringUtil.formatFixedWidth(transactionDate, 8),
            StringUtil.formatFixedWidth(referenceNumber, 10),
            StringUtil.formatFixedWidth(description, 40),
            formatCurrency(amount, 12));
        
        return padToReportWidth(detailLine);
    }

    /**
     * Formats account summary section.
     * 
     * @param accountNumber account number to format
     * @param currentBalance current account balance
     * @param creditLimit credit limit
     * @param availableCredit available credit amount
     * @return formatted account summary lines
     */
    public static String formatAccountSummary(String accountNumber, BigDecimal currentBalance,
                                            BigDecimal creditLimit, BigDecimal availableCredit) {
        StringBuilder summary = new StringBuilder();
        
        // Account number line
        String accountLine = String.format(ACCOUNT_SUMMARY_PATTERN,
            "ACCOUNT NUMBER:",
            FormatUtil.formatAccountNumber(accountNumber));
        summary.append(padToReportWidth(accountLine)).append("\n");
        
        // Current balance line
        String balanceLine = String.format(ACCOUNT_SUMMARY_PATTERN,
            "CURRENT BALANCE:",
            formatCurrency(currentBalance, 12));
        summary.append(padToReportWidth(balanceLine)).append("\n");
        
        // Credit limit line
        String limitLine = String.format(ACCOUNT_SUMMARY_PATTERN,
            "CREDIT LIMIT:",
            formatCurrency(creditLimit, 12));
        summary.append(padToReportWidth(limitLine)).append("\n");
        
        // Available credit line
        String availableLine = String.format(ACCOUNT_SUMMARY_PATTERN,
            "AVAILABLE CREDIT:",
            formatCurrency(availableCredit, 12));
        summary.append(padToReportWidth(availableLine)).append("\n");
        
        return summary.toString();
    }

    /**
     * Formats grand total line for report.
     * 
     * @param totalAmount grand total amount
     * @param totalCount total transaction count
     * @return formatted grand total line
     */
    public static String formatGrandTotal(BigDecimal totalAmount, int totalCount) {
        String totalLine = String.format(ACCOUNT_SUMMARY_PATTERN,
            "GRAND TOTAL (" + totalCount + " TRANSACTIONS):",
            formatCurrency(totalAmount, 12));
        
        return padToReportWidth(totalLine);
    }

    /**
     * Formats page total line for current page.
     * 
     * @param pageAmount page total amount
     * @param pageCount page transaction count
     * @return formatted page total line
     */
    public static String formatPageTotal(BigDecimal pageAmount, int pageCount) {
        String totalLine = String.format(ACCOUNT_SUMMARY_PATTERN,
            "PAGE TOTAL (" + pageCount + " TRANSACTIONS):",
            formatCurrency(pageAmount, 12));
        
        return padToReportWidth(totalLine);
    }

    /**
     * Generates page number for report header.
     * 
     * @param reportId identifier for tracking page state
     * @return current page number
     */
    public static int generatePageNumber(String reportId) {
        return pageCounters.getOrDefault(reportId, 1);
    }

    /**
     * Pads a line to the full report width.
     * 
     * @param line the line content to pad
     * @return line padded to report width
     */
    public static String padToReportWidth(String line) {
        return StringUtil.padRight(line, Constants.REPORT_WIDTH);
    }

    /**
     * Formats signed amounts with proper positive/negative indicators.
     * 
     * @param amount the amount to format
     * @param width field width for formatting
     * @return formatted signed amount
     */
    public static String formatSignedAmount(BigDecimal amount, int width) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        
        String formatted;
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            formatted = formatCurrency(amount.abs(), width - 1) + "-";
        } else {
            formatted = formatCurrency(amount, width - 1) + " ";
        }
        
        return formatted;
    }

    /**
     * Formats transaction type codes with descriptions.
     * 
     * @param typeCode transaction type code
     * @param description type description
     * @return formatted transaction type
     */
    public static String formatTransactionType(String typeCode, String description) {
        return String.format("%-4s %-20s",
            StringUtil.formatFixedWidth(typeCode, 4),
            StringUtil.formatFixedWidth(description, 20));
    }

    /**
     * Formats account numbers with proper masking and alignment.
     * 
     * @param accountNumber raw account number
     * @param masked whether to mask the account number
     * @return formatted account number
     */
    public static String formatAccountNumber(String accountNumber, boolean masked) {
        if (masked) {
            return FormatUtil.formatAccountNumber(accountNumber);
        } else {
            return StringUtil.formatFixedWidth(accountNumber, Constants.ACCOUNT_NUMBER_LENGTH);
        }
    }

    /**
     * Formats report timestamp in standard format.
     * 
     * @param timestamp the timestamp to format
     * @return formatted timestamp string
     */
    public static String formatReportTimestamp(LocalDateTime timestamp) {
        return DateConversionUtil.formatTimestamp(timestamp);
    }

    /**
     * Aligns data in columns for tabular output.
     * 
     * @param data the data to align
     * @param columnWidth width of the column
     * @param alignment alignment type (LEFT, RIGHT, CENTER)
     * @return aligned column data
     * @throws ValidationException if alignment type is invalid
     */
    public static String alignColumn(String data, int columnWidth, String alignment) {
        if (data == null) {
            data = "";
        }
        
        switch (alignment.toUpperCase()) {
            case "LEFT":
                return StringUtil.padRight(data, columnWidth);
            case "RIGHT":
                return StringUtil.padLeft(data, columnWidth);
            case "CENTER":
                int padding = (columnWidth - data.length()) / 2;
                String centered = StringUtil.padLeft(data, data.length() + padding);
                return StringUtil.padRight(centered, columnWidth);
            default:
                throw new ValidationException("Invalid alignment type: " + alignment);
        }
    }

    /**
     * Formats a total line with specified label and amount.
     * 
     * @param label the label for the total line
     * @param amount the total amount
     * @param width total field width
     * @return formatted total line
     */
    public static String formatTotalLine(String label, BigDecimal amount, int width) {
        String totalLine = String.format("%-40s %12s",
            StringUtil.formatFixedWidth(label, 40),
            formatCurrency(amount, 12));
        
        return StringUtil.padRight(totalLine, width);
    }

    /**
     * Formats numeric field using COBOL PIC clause specification.
     * Uses FormatUtil.formatWithPicture for COBOL-compatible display.
     * 
     * @param value the numeric value to format
     * @param pictureClause COBOL PIC clause (e.g., "9(5)V99", "S9(7)V99")
     * @return formatted string matching COBOL PIC clause
     * @throws ValidationException if PIC clause is invalid
     */
    public static String formatWithCobolPicture(BigDecimal value, String pictureClause) {
        if (pictureClause == null || pictureClause.trim().isEmpty()) {
            ValidationException ex = new ValidationException("Invalid COBOL PIC clause");
            ex.addFieldError("pictureClause", "PIC clause cannot be null or empty");
            throw ex;
        }
        
        try {
            return FormatUtil.formatWithPicture(value, pictureClause);
        } catch (Exception e) {
            ValidationException ex = new ValidationException("Error formatting with PIC clause");
            ex.addFieldError("pictureClause", "Failed to format with PIC: " + pictureClause + " - " + e.getMessage());
            throw ex;
        }
    }

    /**
     * Aligns decimal values for columnar display.
     * Uses FormatUtil.alignDecimal for proper decimal point alignment.
     * 
     * @param value the decimal value to align
     * @param totalWidth total field width
     * @param decimalPlaces number of decimal places
     * @return decimal-aligned string
     * @throws ValidationException if alignment parameters are invalid
     */
    public static String formatAlignedDecimal(BigDecimal value, int totalWidth, int decimalPlaces) {
        if (totalWidth <= 0 || decimalPlaces < 0) {
            ValidationException ex = new ValidationException("Invalid decimal alignment parameters");
            ex.addFieldError("totalWidth", "Total width must be positive: " + totalWidth);
            ex.addFieldError("decimalPlaces", "Decimal places must be non-negative: " + decimalPlaces);
            throw ex;
        }
        
        if (value == null) {
            value = BigDecimal.ZERO;
        }
        
        return FormatUtil.alignDecimal(value, totalWidth, decimalPlaces);
    }

    /**
     * Formats data using field length constants from Constants class.
     * Ensures consistent field widths across all batch reports.
     * 
     * @param fieldName name of the field for width lookup
     * @param value the value to format
     * @param alignment alignment type (LEFT, RIGHT, CENTER)
     * @return formatted string with proper field width
     * @throws ValidationException if field name is not found in constants
     */
    public static String formatWithFieldLength(String fieldName, String value, String alignment) {
        if (!Constants.FIELD_LENGTHS.containsKey(fieldName)) {
            ValidationException ex = new ValidationException("Unknown field name in constants");
            ex.addFieldError("fieldName", "Field not found in FIELD_LENGTHS: " + fieldName);
            throw ex;
        }
        
        int fieldWidth = Constants.FIELD_LENGTHS.get(fieldName);
        return alignColumn(value, fieldWidth, alignment);
    }

    /**
     * Applies format patterns from Constants for consistent formatting.
     * Uses Constants.FORMAT_PATTERNS for standardized output formatting.
     * 
     * @param patternName name of the format pattern
     * @param value the value to format
     * @return formatted string using the specified pattern
     * @throws ValidationException if pattern name is not found
     */
    public static String applyFormatPattern(String patternName, Object value) {
        if (!Constants.FORMAT_PATTERNS.containsKey(patternName)) {
            ValidationException ex = new ValidationException("Unknown format pattern");
            ex.addFieldError("patternName", "Pattern not found in FORMAT_PATTERNS: " + patternName);
            throw ex;
        }
        
        String pattern = Constants.FORMAT_PATTERNS.get(patternName);
        try {
            if (value instanceof BigDecimal) {
                DecimalFormat df = new DecimalFormat(pattern);
                return df.format(value);
            } else if (value instanceof LocalDateTime) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
                return ((LocalDateTime) value).format(dtf);
            } else {
                return String.format(pattern, value);
            }
        } catch (Exception e) {
            ValidationException ex = new ValidationException("Error applying format pattern");
            ex.addFieldError("pattern", "Failed to apply pattern: " + pattern + " - " + e.getMessage());
            throw ex;
        }
    }

    /**
     * Validates and formats field data with comprehensive error handling.
     * Demonstrates full use of ValidationException error handling capabilities.
     * 
     * @param fieldName name of the field being formatted
     * @param value the value to format and validate
     * @param width required field width
     * @param required whether the field is required
     * @return validated and formatted field value
     * @throws ValidationException if validation fails
     */
    public static String validateAndFormatField(String fieldName, String value, int width, boolean required) {
        ValidationException ex = new ValidationException("Field validation errors occurred");
        boolean hasErrors = false;
        
        if (required && (value == null || value.trim().isEmpty())) {
            ex.addFieldError(fieldName, "Field is required but was empty");
            hasErrors = true;
        }
        
        if (width <= 0) {
            ex.addFieldError(fieldName + "_width", "Field width must be positive: " + width);
            hasErrors = true;
        }
        
        if (value != null && value.length() > width) {
            ex.addFieldError(fieldName + "_length", "Field value exceeds maximum width: " + width);
            hasErrors = true;
        }
        
        if (hasErrors) {
            throw ex;
        }
        
        return formatFixedWidth(value == null ? "" : value, width, true);
    }
}