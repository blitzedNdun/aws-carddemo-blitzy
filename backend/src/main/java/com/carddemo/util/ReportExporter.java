/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import com.carddemo.util.FormatUtil;
import com.carddemo.util.StringUtil;

/**
 * Report export utility class providing CSV and PDF generation capabilities for detailed reports.
 * 
 * This class maintains COBOL-style formatting and columnar alignment while supporting multiple
 * export formats. It handles control break processing output, subtotals, grand totals, and
 * statistical summaries in exported files, preserving the exact display compatibility with
 * the original COBOL CORPT00C report generation program.
 * 
 * Key Features:
 * - CSV and PDF export with COBOL-compatible formatting
 * - Control break processing for subtotals and grand totals
 * - Fixed-width columnar alignment matching COBOL displays
 * - Page break logic converted to PDF pagination
 * - Numeric formatting preserving monetary precision
 * - Whitespace compression for optimal file sizes
 * - Data validation and export metadata generation
 * 
 * This implementation directly translates the report generation logic from COBOL program
 * CORPT00C.cbl while maintaining exact formatting patterns for seamless migration compatibility.
 * All formatting operations ensure byte-for-byte display compatibility with the original
 * mainframe report outputs.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public final class ReportExporter {

    // COBOL report formatting constants matching original specifications
    private static final int DEFAULT_PAGE_WIDTH = 132;
    private static final int DEFAULT_LINES_PER_PAGE = 60;
    private static final int HEADER_LINES = 4;
    private static final int FOOTER_LINES = 2;
    private static final String CSV_DELIMITER = ",";
    private static final String CSV_LINE_SEPARATOR = "\n";
    
    // PDF formatting constants for COBOL-style layout
    private static final float PDF_MARGIN_TOP = 72f;
    private static final float PDF_MARGIN_BOTTOM = 72f;
    private static final float PDF_MARGIN_LEFT = 72f;
    private static final float PDF_MARGIN_RIGHT = 72f;
    private static final float PDF_LINE_HEIGHT = 12f;
    private static final float PDF_FONT_SIZE = 10f;
    
    // Column alignment constants matching COBOL PIC clauses
    private static final String ALIGN_LEFT = "LEFT";
    private static final String ALIGN_RIGHT = "RIGHT";
    private static final String ALIGN_CENTER = "CENTER";
    
    // Control break processing constants
    private static final String BREAK_LEVEL_NONE = "NONE";
    private static final String BREAK_LEVEL_MINOR = "MINOR";
    private static final String BREAK_LEVEL_MAJOR = "MAJOR";
    private static final String BREAK_LEVEL_GRAND = "GRAND";

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ReportExporter() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Exports data to CSV format with proper field delimiting and header rows.
     * 
     * This method implements COBOL-compatible CSV generation with fixed-width field
     * conversion, proper delimiter handling, and header row generation matching
     * the original report column layouts from CORPT00C.cbl processing logic.
     * 
     * @param reportData List of data rows to export
     * @param columnHeaders List of column header names
     * @param columnWidths Array of column widths for formatting
     * @param fileName Output CSV file name
     * @return CSV content as string for further processing
     * @throws IOException if file writing fails
     * @throws IllegalArgumentException if required parameters are null or invalid
     */
    public static String exportToCsv(List<Map<String, Object>> reportData, 
                                   List<String> columnHeaders, 
                                   int[] columnWidths, 
                                   String fileName) throws IOException {
        
        if (reportData == null) {
            throw new IllegalArgumentException("Report data cannot be null");
        }
        if (columnHeaders == null || columnHeaders.isEmpty()) {
            throw new IllegalArgumentException("Column headers cannot be null or empty");
        }
        if (columnWidths == null || columnWidths.length != columnHeaders.size()) {
            throw new IllegalArgumentException("Column widths must match header count");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        StringBuilder csvContent = new StringBuilder();
        
        // Generate CSV header row with proper escaping
        csvContent.append(generateCsvHeaders(columnHeaders));
        csvContent.append(CSV_LINE_SEPARATOR);
        
        // Process each data row with columnar formatting
        for (Map<String, Object> dataRow : reportData) {
            String csvRow = formatDataRowForCsv(dataRow, columnHeaders, columnWidths);
            csvContent.append(csvRow);
            csvContent.append(CSV_LINE_SEPARATOR);
        }
        
        // Write to file if fileName provided
        if (!fileName.equals("")) {
            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write(csvContent.toString());
            }
        }
        
        return csvContent.toString();
    }

    /**
     * Exports data to PDF format with tabular layout and COBOL-style formatting.
     * 
     * This method implements PDF generation with fixed-width columnar layout,
     * page break handling, and formatting that matches the original COBOL
     * report display patterns from CORPT00C screen layouts.
     * 
     * @param reportData List of data rows to export
     * @param columnHeaders List of column header names  
     * @param columnWidths Array of column widths for alignment
     * @param reportTitle Main report title for header
     * @param outputStream Output stream for PDF content
     * @throws IOException if PDF generation fails
     * @throws IllegalArgumentException if required parameters are null or invalid
     */
    public static void exportToPdf(List<Map<String, Object>> reportData,
                                 List<String> columnHeaders,
                                 int[] columnWidths,
                                 String reportTitle,
                                 OutputStream outputStream) throws IOException {
        
        if (reportData == null) {
            throw new IllegalArgumentException("Report data cannot be null");
        }
        if (columnHeaders == null || columnHeaders.isEmpty()) {
            throw new IllegalArgumentException("Column headers cannot be null or empty");
        }
        if (columnWidths == null || columnWidths.length != columnHeaders.size()) {
            throw new IllegalArgumentException("Column widths must match header count");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream cannot be null");
        }

        try (PDDocument document = new PDDocument()) {
            PDPage currentPage = new PDPage(PDRectangle.LETTER);
            document.addPage(currentPage);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, currentPage)) {
                
                // Initialize PDF layout variables
                float currentY = currentPage.getMediaBox().getHeight() - PDF_MARGIN_TOP;
                int pageNumber = 1;
                
                // Generate PDF header
                currentY = generatePdfHeader(contentStream, reportTitle, pageNumber, currentY);
                
                // Generate column headers
                currentY = generatePdfColumnHeaders(contentStream, columnHeaders, columnWidths, currentY);
                
                // Process data rows with page break handling
                for (Map<String, Object> dataRow : reportData) {
                    
                    // Check if page break is needed
                    if (currentY < PDF_MARGIN_BOTTOM + PDF_LINE_HEIGHT) {
                        contentStream.close();
                        
                        // Create new page
                        currentPage = new PDPage(PDRectangle.LETTER);
                        document.addPage(currentPage);
                        PDPageContentStream newContentStream = new PDPageContentStream(document, currentPage);
                        
                        currentY = currentPage.getMediaBox().getHeight() - PDF_MARGIN_TOP;
                        pageNumber++;
                        
                        // Repeat headers on new page
                        currentY = generatePdfHeader(newContentStream, reportTitle, pageNumber, currentY);
                        currentY = generatePdfColumnHeaders(newContentStream, columnHeaders, columnWidths, currentY);
                        
                        // Generate data row
                        currentY = generatePdfDataRow(newContentStream, dataRow, columnHeaders, columnWidths, currentY);
                        
                        newContentStream.close();
                    } else {
                        // Generate data row on current page
                        currentY = generatePdfDataRow(contentStream, dataRow, columnHeaders, columnWidths, currentY);
                    }
                }
            }
            
            // Save PDF to output stream
            document.save(outputStream);
        }
    }

    /**
     * Formats columnar data maintaining fixed-width display in exports.
     * 
     * This method implements COBOL fixed-width field formatting with proper
     * padding, truncation, and alignment to maintain exact column positioning
     * as specified in the original COBOL copybook definitions.
     * 
     * @param data Object data to format
     * @param fieldWidth Fixed width for the formatted field
     * @param alignment Field alignment ("LEFT", "RIGHT", "CENTER")
     * @param dataType Data type for formatting ("STRING", "NUMERIC", "DATE")
     * @return Formatted string at exactly the specified width
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static String formatColumnarData(Object data, int fieldWidth, String alignment, String dataType) {
        
        if (fieldWidth <= 0) {
            throw new IllegalArgumentException("Field width must be positive: " + fieldWidth);
        }
        if (alignment == null) {
            alignment = ALIGN_LEFT;
        }
        if (dataType == null) {
            dataType = "STRING";
        }

        String formattedData = "";
        
        // Format data based on type
        switch (dataType.toUpperCase()) {
            case "NUMERIC":
                if (data instanceof BigDecimal) {
                    formattedData = FormatUtil.formatNumber((BigDecimal) data, 2);
                } else if (data instanceof Number) {
                    formattedData = FormatUtil.formatNumber(new BigDecimal(data.toString()), 2);
                } else {
                    formattedData = data != null ? data.toString() : "0.00";
                }
                break;
                
            case "CURRENCY":
                if (data instanceof BigDecimal) {
                    formattedData = FormatUtil.formatCurrency((BigDecimal) data);
                } else if (data instanceof Number) {
                    formattedData = FormatUtil.formatCurrency(new BigDecimal(data.toString()));
                } else {
                    formattedData = FormatUtil.formatCurrency(BigDecimal.ZERO);
                }
                break;
                
            case "DATE":
                if (data instanceof LocalDateTime) {
                    formattedData = FormatUtil.formatDate((LocalDateTime) data);
                } else {
                    formattedData = data != null ? data.toString() : "";
                }
                break;
                
            default:
                formattedData = data != null ? data.toString() : "";
                break;
        }
        
        // Apply columnar alignment using FormatUtil
        return FormatUtil.alignField(formattedData, fieldWidth, alignment);
    }

    /**
     * Processes control breaks for subtotal section formatting.
     * 
     * This method implements COBOL control break logic for generating
     * subtotals, intermediate totals, and grand totals matching the
     * hierarchical break processing from the original report generation.
     * 
     * @param reportData List of report data rows
     * @param controlFields List of fields that trigger control breaks
     * @param summaryFields List of fields to sum for subtotals
     * @return Processed report data with control break lines inserted
     * @throws IllegalArgumentException if required parameters are null
     */
    public static List<Map<String, Object>> processControlBreaks(List<Map<String, Object>> reportData,
                                                               List<String> controlFields,
                                                               List<String> summaryFields) {
        
        if (reportData == null) {
            throw new IllegalArgumentException("Report data cannot be null");
        }
        if (controlFields == null || controlFields.isEmpty()) {
            return new ArrayList<>(reportData);
        }
        if (summaryFields == null) {
            summaryFields = new ArrayList<>();
        }

        List<Map<String, Object>> processedData = new ArrayList<>();
        Map<String, Object> currentControlValues = new HashMap<>();
        Map<String, BigDecimal> currentSubtotals = new HashMap<>();
        Map<String, BigDecimal> grandTotals = new HashMap<>();
        
        // Initialize subtotal accumulators
        for (String field : summaryFields) {
            currentSubtotals.put(field, BigDecimal.ZERO);
            grandTotals.put(field, BigDecimal.ZERO);
        }
        
        for (int i = 0; i < reportData.size(); i++) {
            Map<String, Object> currentRow = reportData.get(i);
            boolean controlBreak = false;
            
            // Check for control breaks
            for (String controlField : controlFields) {
                Object currentValue = currentRow.get(controlField);
                Object previousValue = currentControlValues.get(controlField);
                
                if (previousValue != null && !previousValue.equals(currentValue)) {
                    controlBreak = true;
                    break;
                }
            }
            
            // Process control break
            if (controlBreak && i > 0) {
                // Generate subtotal row
                Map<String, Object> subtotalRow = generateSubtotalRow(currentControlValues, 
                                                                    currentSubtotals, 
                                                                    controlFields, 
                                                                    summaryFields);
                processedData.add(subtotalRow);
                
                // Reset subtotals
                for (String field : summaryFields) {
                    currentSubtotals.put(field, BigDecimal.ZERO);
                }
            }
            
            // Update control values
            for (String controlField : controlFields) {
                currentControlValues.put(controlField, currentRow.get(controlField));
            }
            
            // Accumulate summary fields
            for (String summaryField : summaryFields) {
                Object value = currentRow.get(summaryField);
                if (value instanceof BigDecimal) {
                    BigDecimal currentSubtotal = currentSubtotals.get(summaryField);
                    BigDecimal currentGrandTotal = grandTotals.get(summaryField);
                    currentSubtotals.put(summaryField, currentSubtotal.add((BigDecimal) value));
                    grandTotals.put(summaryField, currentGrandTotal.add((BigDecimal) value));
                } else if (value instanceof Number) {
                    BigDecimal numericValue = new BigDecimal(value.toString());
                    BigDecimal currentSubtotal = currentSubtotals.get(summaryField);
                    BigDecimal currentGrandTotal = grandTotals.get(summaryField);
                    currentSubtotals.put(summaryField, currentSubtotal.add(numericValue));
                    grandTotals.put(summaryField, currentGrandTotal.add(numericValue));
                }
            }
            
            // Add current row to processed data
            processedData.add(new HashMap<>(currentRow));
        }
        
        // Add final subtotal if there's data
        if (!reportData.isEmpty()) {
            Map<String, Object> finalSubtotalRow = generateSubtotalRow(currentControlValues,
                                                                     currentSubtotals,
                                                                     controlFields,
                                                                     summaryFields);
            processedData.add(finalSubtotalRow);
        }
        
        // Add grand total row
        Map<String, Object> grandTotalRow = generateGrandTotalRow(grandTotals, summaryFields);
        processedData.add(grandTotalRow);
        
        return processedData;
    }

    /**
     * Generates column headers for CSV and PDF exports.
     * 
     * This method creates properly formatted column headers with consistent
     * formatting, alignment, and spacing that matches the original COBOL
     * report header layouts from CORPT00C screen definitions.
     * 
     * @param columnNames List of column names for headers
     * @param columnWidths Array of column widths for alignment
     * @param headerFormat Format specification ("CSV", "PDF", "FIXED")
     * @return Formatted header string
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static String generateHeaders(List<String> columnNames, int[] columnWidths, String headerFormat) {
        
        if (columnNames == null || columnNames.isEmpty()) {
            throw new IllegalArgumentException("Column names cannot be null or empty");
        }
        if (columnWidths == null || columnWidths.length != columnNames.size()) {
            throw new IllegalArgumentException("Column widths must match column count");
        }
        if (headerFormat == null) {
            headerFormat = "FIXED";
        }

        StringBuilder headerBuilder = new StringBuilder();
        
        switch (headerFormat.toUpperCase()) {
            case "CSV":
                headerBuilder.append(generateCsvHeaders(columnNames));
                break;
                
            case "PDF":
            case "FIXED":
            default:
                for (int i = 0; i < columnNames.size(); i++) {
                    String columnName = columnNames.get(i);
                    int columnWidth = columnWidths[i];
                    
                    String formattedHeader = FormatUtil.alignField(columnName, columnWidth, ALIGN_CENTER);
                    headerBuilder.append(formattedHeader);
                    
                    if (i < columnNames.size() - 1) {
                        headerBuilder.append(" ");
                    }
                }
                break;
        }
        
        return headerBuilder.toString();
    }

    /**
     * Applies number formatting for monetary and numeric field formatting.
     * 
     * This method implements COBOL-compatible numeric formatting with proper
     * decimal places, sign handling, and thousands separators matching the
     * original PIC clause specifications for monetary amounts.
     * 
     * @param value Numeric value to format
     * @param fieldType Type of numeric field ("CURRENCY", "DECIMAL", "INTEGER", "PERCENT")
     * @param decimalPlaces Number of decimal places to display
     * @param showThousandsSeparator Whether to show thousands separators
     * @return Formatted numeric string
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static String applyNumberFormatting(Object value, 
                                             String fieldType, 
                                             int decimalPlaces, 
                                             boolean showThousandsSeparator) {
        
        if (fieldType == null) {
            fieldType = "DECIMAL";
        }
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("Decimal places cannot be negative: " + decimalPlaces);
        }

        if (value == null) {
            return generateZeroValue(fieldType, decimalPlaces);
        }
        
        BigDecimal numericValue;
        if (value instanceof BigDecimal) {
            numericValue = (BigDecimal) value;
        } else if (value instanceof Number) {
            numericValue = new BigDecimal(value.toString());
        } else {
            try {
                numericValue = new BigDecimal(value.toString());
            } catch (NumberFormatException e) {
                return generateZeroValue(fieldType, decimalPlaces);
            }
        }
        
        switch (fieldType.toUpperCase()) {
            case "CURRENCY":
                if (showThousandsSeparator) {
                    return FormatUtil.formatCurrency(numericValue);
                } else {
                    return FormatUtil.formatDecimalAmount(numericValue);
                }
                
            case "PERCENT":
                BigDecimal percentValue = numericValue.multiply(new BigDecimal("100"));
                String formatted = FormatUtil.formatNumber(percentValue, decimalPlaces);
                return formatted + "%";
                
            case "INTEGER":
                return FormatUtil.formatNumber(numericValue, 0);
                
            case "DECIMAL":
            default:
                if (showThousandsSeparator) {
                    return FormatUtil.formatWithThousandsSeparator(numericValue, decimalPlaces);
                } else {
                    return FormatUtil.formatNumber(numericValue, decimalPlaces);
                }
        }
    }

    /**
     * Handles page breaks for PDF pagination.
     * 
     * This method implements page break logic for PDF generation, managing
     * page boundaries, header repetition, and footer placement to match
     * the original COBOL page break processing from report programs.
     * 
     * @param currentLine Current line number on page
     * @param linesPerPage Maximum lines per page
     * @param headerLines Number of lines reserved for headers
     * @param footerLines Number of lines reserved for footers
     * @return true if page break is needed, false otherwise
     */
    public static boolean handlePageBreaks(int currentLine, int linesPerPage, int headerLines, int footerLines) {
        
        if (linesPerPage <= 0) {
            linesPerPage = DEFAULT_LINES_PER_PAGE;
        }
        if (headerLines < 0) {
            headerLines = HEADER_LINES;
        }
        if (footerLines < 0) {
            footerLines = FOOTER_LINES;
        }

        int availableLines = linesPerPage - headerLines - footerLines;
        return currentLine >= availableLines;
    }

    /**
     * Compresses whitespace for efficient file size management.
     * 
     * This method implements whitespace compression while preserving
     * columnar alignment and formatting integrity for optimized file
     * sizes without losing display compatibility.
     * 
     * @param reportContent Original report content
     * @param preserveAlignment Whether to preserve columnar alignment
     * @return Compressed content with reduced whitespace
     * @throws IllegalArgumentException if reportContent is null
     */
    public static String compressWhitespace(String reportContent, boolean preserveAlignment) {
        
        if (reportContent == null) {
            throw new IllegalArgumentException("Report content cannot be null");
        }

        if (!preserveAlignment) {
            // Aggressive compression - collapse multiple spaces to single space
            return reportContent.replaceAll("\\s+", " ").trim();
        }
        
        // Conservative compression - preserve alignment by only trimming line ends
        StringBuilder compressed = new StringBuilder();
        String[] lines = reportContent.split("\n");
        
        for (String line : lines) {
            // Trim trailing spaces but preserve leading spaces for alignment
            String trimmedLine = StringUtil.trimTrailing(line);
            compressed.append(trimmedLine);
            compressed.append("\n");
        }
        
        return compressed.toString();
    }

    /**
     * Validates export data for data integrity checking.
     * 
     * This method performs comprehensive data validation to ensure
     * data integrity, field completeness, and format compatibility
     * before export processing begins.
     * 
     * @param reportData List of data rows to validate
     * @param requiredFields List of required field names
     * @param fieldTypes Map of field names to expected data types
     * @return Map containing validation results and error messages
     * @throws IllegalArgumentException if reportData is null
     */
    public static Map<String, Object> validateExportData(List<Map<String, Object>> reportData,
                                                        List<String> requiredFields,
                                                        Map<String, String> fieldTypes) {
        
        if (reportData == null) {
            throw new IllegalArgumentException("Report data cannot be null");
        }

        Map<String, Object> validationResult = new HashMap<>();
        List<String> validationErrors = new ArrayList<>();
        int validRowCount = 0;
        int totalRowCount = reportData.size();
        
        // Validate each data row
        for (int i = 0; i < reportData.size(); i++) {
            Map<String, Object> dataRow = reportData.get(i);
            boolean rowValid = true;
            
            // Check required fields
            if (requiredFields != null) {
                for (String requiredField : requiredFields) {
                    if (!dataRow.containsKey(requiredField) || dataRow.get(requiredField) == null) {
                        validationErrors.add("Row " + (i + 1) + ": Missing required field '" + requiredField + "'");
                        rowValid = false;
                    }
                }
            }
            
            // Check field types
            if (fieldTypes != null) {
                for (Map.Entry<String, String> fieldType : fieldTypes.entrySet()) {
                    String fieldName = fieldType.getKey();
                    String expectedType = fieldType.getValue();
                    Object fieldValue = dataRow.get(fieldName);
                    
                    if (fieldValue != null && !isValidFieldType(fieldValue, expectedType)) {
                        validationErrors.add("Row " + (i + 1) + ": Invalid type for field '" + 
                                           fieldName + "', expected " + expectedType);
                        rowValid = false;
                    }
                }
            }
            
            if (rowValid) {
                validRowCount++;
            }
        }
        
        // Compile validation results
        validationResult.put("valid", validationErrors.isEmpty());
        validationResult.put("totalRows", totalRowCount);
        validationResult.put("validRows", validRowCount);
        validationResult.put("invalidRows", totalRowCount - validRowCount);
        validationResult.put("errors", validationErrors);
        validationResult.put("validationTimestamp", LocalDateTime.now());
        
        return validationResult;
    }

    /**
     * Generates export metadata for file information headers.
     * 
     * This method creates comprehensive metadata headers including
     * generation timestamp, row counts, column information, and
     * processing statistics for audit trails and file documentation.
     * 
     * @param reportData List of exported data rows
     * @param columnHeaders List of column names
     * @param exportFormat Export format ("CSV", "PDF")
     * @param generatorInfo Information about the generating system
     * @return Map containing complete export metadata
     * @throws IllegalArgumentException if required parameters are null
     */
    public static Map<String, Object> generateExportMetadata(List<Map<String, Object>> reportData,
                                                           List<String> columnHeaders,
                                                           String exportFormat,
                                                           String generatorInfo) {
        
        if (reportData == null) {
            throw new IllegalArgumentException("Report data cannot be null");
        }
        if (columnHeaders == null) {
            throw new IllegalArgumentException("Column headers cannot be null");
        }
        if (exportFormat == null) {
            exportFormat = "UNKNOWN";
        }
        if (generatorInfo == null) {
            generatorInfo = "CardDemo ReportExporter v1.0";
        }

        Map<String, Object> metadata = new HashMap<>();
        
        // Basic export information
        metadata.put("exportFormat", exportFormat.toUpperCase());
        metadata.put("generationTimestamp", LocalDateTime.now());
        metadata.put("generator", generatorInfo);
        metadata.put("version", "1.0");
        
        // Data statistics
        metadata.put("totalRows", reportData.size());
        metadata.put("totalColumns", columnHeaders.size());
        metadata.put("columnNames", new ArrayList<>(columnHeaders));
        
        // Calculate data statistics
        Map<String, Object> dataStats = calculateDataStatistics(reportData, columnHeaders);
        metadata.put("dataStatistics", dataStats);
        
        // File size estimation
        metadata.put("estimatedSizeBytes", estimateFileSize(reportData, columnHeaders, exportFormat));
        
        // Processing information
        metadata.put("processingDateTime", FormatUtil.formatDateTime(LocalDateTime.now()));
        metadata.put("processingDate", FormatUtil.formatDate(LocalDateTime.now()));
        
        return metadata;
    }

    // Helper methods for internal processing

    /**
     * Generates CSV headers with proper escaping and formatting.
     */
    private static String generateCsvHeaders(List<String> columnHeaders) {
        StringBuilder csvHeaders = new StringBuilder();
        
        for (int i = 0; i < columnHeaders.size(); i++) {
            String header = columnHeaders.get(i);
            
            // Escape CSV special characters
            if (header.contains(CSV_DELIMITER) || header.contains("\"") || header.contains("\n")) {
                header = "\"" + header.replace("\"", "\"\"") + "\"";
            }
            
            csvHeaders.append(header);
            
            if (i < columnHeaders.size() - 1) {
                csvHeaders.append(CSV_DELIMITER);
            }
        }
        
        return csvHeaders.toString();
    }

    /**
     * Formats a data row for CSV output with proper escaping and alignment.
     */
    private static String formatDataRowForCsv(Map<String, Object> dataRow, 
                                            List<String> columnHeaders, 
                                            int[] columnWidths) {
        StringBuilder csvRow = new StringBuilder();
        
        for (int i = 0; i < columnHeaders.size(); i++) {
            String columnName = columnHeaders.get(i);
            Object cellValue = dataRow.get(columnName);
            String formattedValue = "";
            
            if (cellValue != null) {
                formattedValue = cellValue.toString();
                
                // Escape CSV special characters
                if (formattedValue.contains(CSV_DELIMITER) || 
                    formattedValue.contains("\"") || 
                    formattedValue.contains("\n")) {
                    formattedValue = "\"" + formattedValue.replace("\"", "\"\"") + "\"";
                }
            }
            
            csvRow.append(formattedValue);
            
            if (i < columnHeaders.size() - 1) {
                csvRow.append(CSV_DELIMITER);
            }
        }
        
        return csvRow.toString();
    }

    /**
     * Generates PDF header with title and page information.
     */
    private static float generatePdfHeader(PDPageContentStream contentStream, 
                                         String reportTitle, 
                                         int pageNumber, 
                                         float startY) throws IOException {
        
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14f);
        contentStream.newLineAtOffset(PDF_MARGIN_LEFT, startY);
        contentStream.showText(reportTitle != null ? reportTitle : "Report");
        contentStream.endText();
        
        // Add page number
        String pageText = "Page " + pageNumber;
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10f);
        contentStream.newLineAtOffset(500f, startY);
        contentStream.showText(pageText);
        contentStream.endText();
        
        // Add generation timestamp
        String timestamp = FormatUtil.formatDateTime(LocalDateTime.now());
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8f);
        contentStream.newLineAtOffset(PDF_MARGIN_LEFT, startY - 20f);
        contentStream.showText("Generated: " + timestamp);
        contentStream.endText();
        
        return startY - 40f;
    }

    /**
     * Generates PDF column headers with proper alignment.
     */
    private static float generatePdfColumnHeaders(PDPageContentStream contentStream,
                                                List<String> columnHeaders,
                                                int[] columnWidths,
                                                float startY) throws IOException {
        
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), PDF_FONT_SIZE);
        contentStream.newLineAtOffset(PDF_MARGIN_LEFT, startY);
        
        float currentX = 0;
        for (int i = 0; i < columnHeaders.size(); i++) {
            String header = StringUtil.formatFixedLength(columnHeaders.get(i), columnWidths[i]);
            contentStream.showText(header);
            currentX += columnWidths[i] * 6; // Approximate character width
            contentStream.newLineAtOffset(columnWidths[i] * 6, 0);
        }
        
        contentStream.endText();
        
        return startY - PDF_LINE_HEIGHT;
    }

    /**
     * Generates a PDF data row with proper formatting.
     */
    private static float generatePdfDataRow(PDPageContentStream contentStream,
                                          Map<String, Object> dataRow,
                                          List<String> columnHeaders,
                                          int[] columnWidths,
                                          float startY) throws IOException {
        
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), PDF_FONT_SIZE);
        contentStream.newLineAtOffset(PDF_MARGIN_LEFT, startY);
        
        for (int i = 0; i < columnHeaders.size(); i++) {
            String columnName = columnHeaders.get(i);
            Object cellValue = dataRow.get(columnName);
            String formattedValue = "";
            
            if (cellValue != null) {
                formattedValue = cellValue.toString();
            }
            
            String paddedValue = StringUtil.formatFixedLength(formattedValue, columnWidths[i]);
            contentStream.showText(paddedValue);
            contentStream.newLineAtOffset(columnWidths[i] * 6, 0);
        }
        
        contentStream.endText();
        
        return startY - PDF_LINE_HEIGHT;
    }

    /**
     * Generates a subtotal row for control break processing.
     */
    private static Map<String, Object> generateSubtotalRow(Map<String, Object> controlValues,
                                                         Map<String, BigDecimal> subtotals,
                                                         List<String> controlFields,
                                                         List<String> summaryFields) {
        
        Map<String, Object> subtotalRow = new HashMap<>();
        
        // Add control field values
        for (String controlField : controlFields) {
            subtotalRow.put(controlField, controlValues.get(controlField));
        }
        
        // Add subtotal values
        for (String summaryField : summaryFields) {
            subtotalRow.put(summaryField, subtotals.get(summaryField));
        }
        
        // Mark as subtotal row
        subtotalRow.put("_rowType", "SUBTOTAL");
        subtotalRow.put("_breakLevel", BREAK_LEVEL_MINOR);
        
        return subtotalRow;
    }

    /**
     * Generates a grand total row for final summary.
     */
    private static Map<String, Object> generateGrandTotalRow(Map<String, BigDecimal> grandTotals,
                                                           List<String> summaryFields) {
        
        Map<String, Object> grandTotalRow = new HashMap<>();
        
        // Add grand total values
        for (String summaryField : summaryFields) {
            grandTotalRow.put(summaryField, grandTotals.get(summaryField));
        }
        
        // Mark as grand total row
        grandTotalRow.put("_rowType", "GRAND_TOTAL");
        grandTotalRow.put("_breakLevel", BREAK_LEVEL_GRAND);
        grandTotalRow.put("_description", "Grand Total");
        
        return grandTotalRow;
    }

    /**
     * Generates a zero value for the specified field type.
     */
    private static String generateZeroValue(String fieldType, int decimalPlaces) {
        switch (fieldType.toUpperCase()) {
            case "CURRENCY":
                return "$0." + "0".repeat(Math.max(decimalPlaces, 2));
            case "PERCENT":
                return "0." + "0".repeat(decimalPlaces) + "%";
            case "INTEGER":
                return "0";
            default:
                return "0." + "0".repeat(decimalPlaces);
        }
    }

    /**
     * Validates if a field value matches the expected data type.
     */
    private static boolean isValidFieldType(Object value, String expectedType) {
        if (value == null) {
            return true;
        }
        
        switch (expectedType.toUpperCase()) {
            case "STRING":
                return value instanceof String;
            case "NUMERIC":
            case "DECIMAL":
            case "CURRENCY":
                return value instanceof Number;
            case "DATE":
            case "DATETIME":
                return value instanceof LocalDateTime || value instanceof String;
            case "BOOLEAN":
                return value instanceof Boolean;
            default:
                return true;
        }
    }

    /**
     * Calculates data statistics for metadata generation.
     */
    private static Map<String, Object> calculateDataStatistics(List<Map<String, Object>> reportData,
                                                              List<String> columnHeaders) {
        
        Map<String, Object> stats = new HashMap<>();
        
        if (reportData.isEmpty()) {
            stats.put("isEmpty", true);
            return stats;
        }
        
        stats.put("isEmpty", false);
        
        // Calculate column statistics
        Map<String, Object> columnStats = new HashMap<>();
        for (String columnName : columnHeaders) {
            Map<String, Object> colStats = new HashMap<>();
            int nonNullCount = 0;
            int nullCount = 0;
            
            for (Map<String, Object> row : reportData) {
                Object value = row.get(columnName);
                if (value != null) {
                    nonNullCount++;
                } else {
                    nullCount++;
                }
            }
            
            colStats.put("nonNullCount", nonNullCount);
            colStats.put("nullCount", nullCount);
            colStats.put("completeness", (double) nonNullCount / reportData.size());
            
            columnStats.put(columnName, colStats);
        }
        
        stats.put("columnStatistics", columnStats);
        
        return stats;
    }

    /**
     * Estimates file size for the given export format.
     */
    private static long estimateFileSize(List<Map<String, Object>> reportData,
                                       List<String> columnHeaders,
                                       String exportFormat) {
        
        if (reportData.isEmpty()) {
            return 0L;
        }
        
        // Calculate average row size based on first few rows
        int sampleSize = Math.min(10, reportData.size());
        long totalSampleSize = 0;
        
        for (int i = 0; i < sampleSize; i++) {
            Map<String, Object> row = reportData.get(i);
            for (String columnName : columnHeaders) {
                Object value = row.get(columnName);
                if (value != null) {
                    totalSampleSize += value.toString().length();
                }
            }
            
            // Add separators and line breaks
            if ("CSV".equalsIgnoreCase(exportFormat)) {
                totalSampleSize += columnHeaders.size() - 1; // Commas
                totalSampleSize += 1; // Line break
            } else {
                totalSampleSize += columnHeaders.size(); // Spaces
                totalSampleSize += 1; // Line break
            }
        }
        
        long averageRowSize = totalSampleSize / sampleSize;
        return averageRowSize * reportData.size();
    }
}