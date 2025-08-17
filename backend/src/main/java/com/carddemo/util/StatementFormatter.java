package com.carddemo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Utility class for formatting monthly statement output in both plain text and HTML formats.
 * Provides COBOL-style print formatting, page break logic, control break handling, and
 * template-based statement generation. Maintains exact formatting patterns from CBSTM03A
 * COBOL program including headers, transaction lists, balance summaries, and minimum payment calculations.
 * 
 * This class extracts the statement formatting logic from the CBSTM03A.cbl batch program,
 * converting COBOL print line formatting to modern Java template engine patterns while
 * preserving the exact layout and formatting requirements of the original mainframe implementation.
 */
public class StatementFormatter {
    
    // Constants for formatting patterns - replicating COBOL STATEMENT-LINES section
    private static final String STATEMENT_START_MARKER = String.format("%31s%18s%31s", 
        "*".repeat(31), "START OF STATEMENT", "*".repeat(31));
    private static final String STATEMENT_END_MARKER = String.format("%32s%16s%32s", 
        "*".repeat(32), "END OF STATEMENT", "*".repeat(32));
    private static final String LINE_SEPARATOR = "-".repeat(80);
    private static final String BASIC_DETAILS_HEADER = String.format("%33s%14s%33s", 
        " ".repeat(33), "Basic Details", " ".repeat(33));
    private static final String TRANSACTION_SUMMARY_HEADER = String.format("%30s%20s%30s", 
        " ".repeat(30), "TRANSACTION SUMMARY ", " ".repeat(30));
    
    // HTML template constants - replicating COBOL HTML-LINES section
    private static final String HTML_DOCTYPE = "<!DOCTYPE html>";
    private static final String HTML_OPEN = "<html lang=\"en\">";
    private static final String HTML_HEAD_OPEN = "<head>";
    private static final String HTML_CHARSET = "<meta charset=\"utf-8\">";
    private static final String HTML_TITLE = "<title>HTML Table Layout</title>";
    private static final String HTML_HEAD_CLOSE = "</head>";
    private static final String HTML_BODY_OPEN = "<body style=\"margin:0px;\">";
    private static final String HTML_TABLE_OPEN = "<table align=\"center\" frame=\"box\" style=\"width:70%; font:12px Segoe UI,sans-serif;\">";
    private static final String HTML_TR_OPEN = "<tr>";
    private static final String HTML_TR_CLOSE = "</tr>";
    private static final String HTML_TD_OPEN = "<td>";
    private static final String HTML_TD_CLOSE = "</td>";
    private static final String HTML_TABLE_CLOSE = "</table>";
    private static final String HTML_BODY_CLOSE = "</body>";
    private static final String HTML_CLOSE = "</html>";
    
    // Formatting utilities
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$0.00");
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("0.00");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");
    private static final DateTimeFormatter LOCAL_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    
    // Page control variables - replicating COBOL control variables
    private int lineCount = 0;
    private static final int LINES_PER_PAGE = 55;
    private boolean pageBreakRequired = false;
    
    // Statement data holder
    private StringBuilder textStatement = new StringBuilder();
    private StringBuilder htmlStatement = new StringBuilder();
    
    /**
     * Initializes a new statement for formatting.
     * Replicates the COBOL 5000-CREATE-STATEMENT section initialization logic.
     * Clears any existing statement data and prepares for new statement generation.
     */
    public void initializeStatement() {
        textStatement.setLength(0);
        htmlStatement.setLength(0);
        lineCount = 0;
        pageBreakRequired = false;
        
        // Write statement start marker - replicating COBOL ST-LINE0
        appendTextLine(STATEMENT_START_MARKER);
        
        // Initialize HTML document structure
        appendHtmlLine(HTML_DOCTYPE);
        appendHtmlLine(HTML_OPEN);
        appendHtmlLine(HTML_HEAD_OPEN);
        appendHtmlLine(HTML_CHARSET);
        appendHtmlLine(HTML_TITLE);
        appendHtmlLine(HTML_HEAD_CLOSE);
        appendHtmlLine(HTML_BODY_OPEN);
        appendHtmlLine(HTML_TABLE_OPEN);
    }
    
    /**
     * Formats the statement header with customer and account information.
     * Replicates COBOL ST-LINE1 through ST-LINE13 formatting patterns.
     * 
     * @param customerName Full customer name (first, middle, last)
     * @param addressLine1 Customer address line 1
     * @param addressLine2 Customer address line 2  
     * @param addressLine3 Customer address line 3 with city, state, zip
     * @param accountId Account identification number
     * @param currentBalance Current account balance
     * @param ficoScore Customer FICO credit score
     * @return Formatted header lines for text and HTML output
     * @throws IllegalArgumentException if required parameters are null or invalid
     */
    public String formatStatementHeader(String customerName, String addressLine1, 
                                      String addressLine2, String addressLine3,
                                      String accountId, BigDecimal currentBalance, 
                                      String ficoScore) {
        
        // Input validation - replicating COBOL field validation patterns
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        if (currentBalance == null) {
            throw new IllegalArgumentException("Current balance cannot be null");
        }
        
        // Defensive handling of optional fields
        String safeName = customerName.length() > 75 ? customerName.substring(0, 75) : customerName;
        String safeAddr1 = addressLine1 != null ? (addressLine1.length() > 50 ? addressLine1.substring(0, 50) : addressLine1) : "";
        String safeAddr2 = addressLine2 != null ? (addressLine2.length() > 50 ? addressLine2.substring(0, 50) : addressLine2) : "";
        String safeAddr3 = addressLine3 != null ? (addressLine3.length() > 80 ? addressLine3.substring(0, 80) : addressLine3) : "";
        String safeAccountId = accountId.length() > 20 ? accountId.substring(0, 20) : accountId;
        String safeFicoScore = ficoScore != null ? (ficoScore.length() > 20 ? ficoScore.substring(0, 20) : ficoScore) : "";
        
        // Format customer name and address - replicating COBOL ST-LINE1 to ST-LINE4
        String nameLine = String.format("%-75s%5s", safeName, "");
        String addr1Line = String.format("%-50s%30s", safeAddr1, "");
        String addr2Line = String.format("%-50s%30s", safeAddr2, "");
        String addr3Line = String.format("%-80s", safeAddr3);
        
        appendTextLine(nameLine);
        appendTextLine(addr1Line);
        appendTextLine(addr2Line);
        appendTextLine(addr3Line);
        appendTextLine(LINE_SEPARATOR);
        appendTextLine(BASIC_DETAILS_HEADER);
        appendTextLine(LINE_SEPARATOR);
        
        // Format account details - replicating COBOL ST-LINE7 to ST-LINE9
        String accountLine = String.format("%-20s%-20s%40s", "Account ID         :", safeAccountId, "");
        String balanceLine = String.format("%-20s%12s%7s%40s", "Current Balance    :", 
            formatCurrency(currentBalance), "", "");
        String ficoLine = String.format("%-20s%-20s%40s", "FICO Score         :", safeFicoScore, "");
        
        appendTextLine(accountLine);
        appendTextLine(balanceLine);
        appendTextLine(ficoLine);
        appendTextLine(LINE_SEPARATOR);
        appendTextLine(TRANSACTION_SUMMARY_HEADER);
        appendTextLine(LINE_SEPARATOR);
        
        // Format transaction header - replicating COBOL ST-LINE13
        String transHeaderLine = String.format("%-16s%-51s%13s", 
            "Tran ID         ", "Tran Details    ", "  Tran Amount");
        appendTextLine(transHeaderLine);
        
        // Format HTML header
        formatHtmlHeader(safeName, safeAddr1, safeAddr2, safeAddr3,
                        safeAccountId, currentBalance, safeFicoScore);
        
        return textStatement.toString();
    }
    
    /**
     * Formats a single transaction line for display.
     * Replicates COBOL ST-LINE14 formatting and 6000-WRITE-TRANS section logic.
     * 
     * @param transactionId Transaction identification number
     * @param transactionDescription Transaction description/details
     * @param transactionAmount Transaction monetary amount
     * @return Formatted transaction line
     * @throws IllegalArgumentException if required parameters are null or invalid
     */
    public String formatTransactionLine(String transactionId, String transactionDescription, 
                                      BigDecimal transactionAmount) {
        
        // Input validation - replicating COBOL field validation
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        if (transactionDescription == null) {
            throw new IllegalArgumentException("Transaction description cannot be null");
        }
        if (transactionAmount == null) {
            throw new IllegalArgumentException("Transaction amount cannot be null");
        }
        
        // Ensure proper field lengths - replicating COBOL PIC clauses
        String safeTranId = transactionId.length() > 16 ? transactionId.substring(0, 16) : transactionId;
        String safeTranDesc = transactionDescription.length() > 49 ? transactionDescription.substring(0, 49) : transactionDescription;
        
        // Format transaction line - replicating COBOL ST-LINE14
        String transLine = String.format("%-16s %-49s$%12s", 
            safeTranId, 
            safeTranDesc,
            formatAmount(transactionAmount));
        
        appendTextLine(transLine);
        
        // Format HTML transaction line
        formatHtmlTransactionLine(safeTranId, safeTranDesc, transactionAmount);
        
        return transLine;
    }
    
    /**
     * Formats the balance summary section of the statement.
     * Replicates COBOL balance calculation and formatting logic.
     * 
     * @param totalExpenses Total expense amount for the period
     * @param currentBalance Current account balance
     * @param availableCredit Available credit amount
     * @return Formatted balance summary
     */
    public String formatBalanceSummary(BigDecimal totalExpenses, BigDecimal currentBalance, 
                                     BigDecimal availableCredit) {
        
        // Format total expenses line - replicating COBOL ST-LINE14A
        String totalExpLine = String.format("%-10s%56s$%12s", 
            "Total EXP:", "", formatAmount(totalExpenses));
        appendTextLine(totalExpLine);
        
        // Add additional balance summary lines
        appendTextLine(LINE_SEPARATOR);
        String currentBalLine = String.format("%-20s%47s$%12s", 
            "Current Balance:", "", formatAmount(currentBalance));
        appendTextLine(currentBalLine);
        
        String availCreditLine = String.format("%-20s%47s$%12s", 
            "Available Credit:", "", formatAmount(availableCredit));
        appendTextLine(availCreditLine);
        
        // Format HTML balance summary
        formatHtmlBalanceSummary(totalExpenses, currentBalance, availableCredit);
        
        return totalExpLine;
    }
    
    /**
     * Formats minimum payment information.
     * Calculates and formats minimum payment amount based on balance and terms.
     * 
     * @param currentBalance Current account balance
     * @param interestRate Annual interest rate as percentage
     * @param minimumPaymentPercent Minimum payment percentage
     * @return Formatted minimum payment information
     */
    public String formatMinimumPayment(BigDecimal currentBalance, BigDecimal interestRate, 
                                     BigDecimal minimumPaymentPercent) {
        
        // Calculate minimum payment - ensuring COBOL COMP-3 precision
        BigDecimal minimumPayment = currentBalance
            .multiply(minimumPaymentPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
            .setScale(2, RoundingMode.HALF_UP);
        
        // Ensure minimum payment is at least $25.00
        BigDecimal minimumThreshold = new BigDecimal("25.00");
        if (minimumPayment.compareTo(minimumThreshold) < 0) {
            minimumPayment = minimumThreshold;
        }
        
        String minPaymentLine = String.format("%-20s%47s$%12s", 
            "Minimum Payment:", "", formatAmount(minimumPayment));
        appendTextLine(minPaymentLine);
        
        String dueDateLine = String.format("%-20s%47s%13s", 
            "Payment Due Date:", "", formatDate(LocalDate.now().plusDays(25)));
        appendTextLine(dueDateLine);
        
        return minPaymentLine;
    }
    
    /**
     * Applies page break logic for multi-page statements.
     * Replicates COBOL page control and line counting logic.
     * 
     * @param forceBreak Force a page break regardless of line count
     * @return True if page break was applied
     */
    public boolean applyPageBreaks(boolean forceBreak) {
        
        if (forceBreak || lineCount >= LINES_PER_PAGE) {
            // Add form feed character for text output
            appendTextLine("\f");
            
            // Reset line counter
            lineCount = 0;
            pageBreakRequired = false;
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Formats the statement footer with closing information.
     * Replicates COBOL ST-LINE15 formatting patterns.
     * 
     * @return Formatted footer content
     */
    public String formatStatementFooter() {
        
        appendTextLine(LINE_SEPARATOR);
        appendTextLine(STATEMENT_END_MARKER);
        
        // Close HTML document structure
        appendHtmlLine(HTML_TR_OPEN);
        appendHtmlLine("<td colspan=\"3\" style=\"padding:0px 5px; background-color:#1d1d96b3;\">");
        appendHtmlLine("<h3>End of Statement</h3>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine(HTML_TR_CLOSE);
        appendHtmlLine(HTML_TABLE_CLOSE);
        appendHtmlLine(HTML_BODY_CLOSE);
        appendHtmlLine(HTML_CLOSE);
        
        return STATEMENT_END_MARKER;
    }
    
    /**
     * Formats statement period information.
     * 
     * @param startDate Statement period start date
     * @param endDate Statement period end date
     * @return Formatted period information
     */
    public String formatStatementPeriod(LocalDate startDate, LocalDate endDate) {
        
        String periodLine = String.format("Statement Period: %s to %s", 
            startDate.format(LOCAL_DATE_FORMAT), 
            endDate.format(LOCAL_DATE_FORMAT));
        
        appendTextLine(periodLine);
        
        return periodLine;
    }
    
    /**
     * Formats account summary information.
     * 
     * @param accountId Account identification
     * @param accountType Type of account (checking, savings, etc.)
     * @param openDate Account opening date
     * @return Formatted account summary
     */
    public String formatAccountSummary(String accountId, String accountType, LocalDate openDate) {
        
        String accountTypeLine = String.format("%-20s%-20s%40s", "Account Type       :", accountType, "");
        String openDateLine = String.format("%-20s%-20s%40s", "Account Opened     :", 
            openDate.format(LOCAL_DATE_FORMAT), "");
        
        appendTextLine(accountTypeLine);
        appendTextLine(openDateLine);
        
        return accountTypeLine;
    }
    
    /**
     * Formats detailed transaction information.
     * 
     * @param transactions List of transaction details
     * @return Formatted transaction details
     * @throws IllegalArgumentException if transactions list is null
     */
    public String formatTransactionDetails(List<TransactionDetail> transactions) {
        
        if (transactions == null) {
            throw new IllegalArgumentException("Transactions list cannot be null");
        }
        
        StringBuilder details = new StringBuilder();
        
        for (TransactionDetail transaction : transactions) {
            if (transaction != null) {
                try {
                    String detail = formatTransactionLine(
                        transaction.getTransactionId(),
                        transaction.getDescription(),
                        transaction.getAmount()
                    );
                    details.append(detail).append("\n");
                } catch (Exception e) {
                    // Log error but continue processing other transactions
                    System.err.println("Error formatting transaction: " + e.getMessage());
                }
            }
        }
        
        return details.toString();
    }
    
    // Private helper methods
    
    private void appendTextLine(String line) {
        textStatement.append(line).append("\n");
        lineCount++;
        
        if (lineCount >= LINES_PER_PAGE) {
            pageBreakRequired = true;
        }
    }
    
    private void appendHtmlLine(String line) {
        htmlStatement.append(line).append("\n");
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        try {
            return CURRENCY_FORMAT.format(amount);
        } catch (Exception e) {
            // Fallback formatting for invalid amounts
            return "$0.00";
        }
    }
    
    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        try {
            return AMOUNT_FORMAT.format(amount);
        } catch (Exception e) {
            // Fallback formatting for invalid amounts
            return "0.00";
        }
    }
    
    private String formatDate(LocalDate date) {
        if (date == null) {
            return LocalDate.now().format(LOCAL_DATE_FORMAT);
        }
        try {
            return date.format(LOCAL_DATE_FORMAT);
        } catch (Exception e) {
            // Fallback to current date for invalid dates
            return LocalDate.now().format(LOCAL_DATE_FORMAT);
        }
    }
    
    private void formatHtmlHeader(String customerName, String addressLine1, 
                                String addressLine2, String addressLine3,
                                String accountId, BigDecimal currentBalance, 
                                String ficoScore) {
        
        // HTML header formatting - replicating COBOL HTML formatting sections
        appendHtmlLine(HTML_TR_OPEN);
        appendHtmlLine("<td colspan=\"3\" style=\"padding:0px 5px; background-color:#1d1d96b3;\">");
        appendHtmlLine("<h3>Statement for Account Number: " + accountId + "</h3>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine(HTML_TR_CLOSE);
        
        // Bank information
        appendHtmlLine(HTML_TR_OPEN);
        appendHtmlLine("<td colspan=\"3\" style=\"padding:0px 5px; background-color:#FFAF33;\">");
        appendHtmlLine("<p style=\"font-size:16px\">Bank of XYZ</p>");
        appendHtmlLine("<p>410 Terry Ave N</p>");
        appendHtmlLine("<p>Seattle WA 99999</p>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine(HTML_TR_CLOSE);
        
        // Customer information
        appendHtmlLine(HTML_TR_OPEN);
        appendHtmlLine("<td colspan=\"3\" style=\"padding:0px 5px; background-color:#f2f2f2;\">");
        appendHtmlLine("<p style=\"font-size:16px\">" + customerName + "</p>");
        appendHtmlLine("<p>" + addressLine1 + "</p>");
        appendHtmlLine("<p>" + addressLine2 + "</p>");
        appendHtmlLine("<p>" + addressLine3 + "</p>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine(HTML_TR_CLOSE);
        
        // Basic details section
        appendHtmlLine(HTML_TR_OPEN);
        appendHtmlLine("<td colspan=\"3\" style=\"padding:0px 5px; background-color:#33FFD1; text-align:center;\">");
        appendHtmlLine("<p style=\"font-size:16px\">Basic Details</p>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine(HTML_TR_CLOSE);
        
        // Account details
        appendHtmlLine(HTML_TR_OPEN);
        appendHtmlLine("<td colspan=\"3\" style=\"padding:0px 5px; background-color:#f2f2f2;\">");
        appendHtmlLine("<p>Account ID         : " + accountId + "</p>");
        appendHtmlLine("<p>Current Balance    : " + formatCurrency(currentBalance) + "</p>");
        appendHtmlLine("<p>FICO Score         : " + ficoScore + "</p>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine(HTML_TR_CLOSE);
        
        // Transaction summary header
        appendHtmlLine(HTML_TR_OPEN);
        appendHtmlLine("<td colspan=\"3\" style=\"padding:0px 5px; background-color:#33FFD1; text-align:center;\">");
        appendHtmlLine("<p style=\"font-size:16px\">Transaction Summary</p>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine(HTML_TR_CLOSE);
        
        // Transaction table headers
        appendHtmlLine(HTML_TR_OPEN);
        appendHtmlLine("<td style=\"width:25%; padding:0px 5px; background-color:#33FF5E; text-align:left;\">");
        appendHtmlLine("<p style=\"font-size:16px\">Tran ID</p>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine("<td style=\"width:55%; padding:0px 5px; background-color:#33FF5E; text-align:left;\">");
        appendHtmlLine("<p style=\"font-size:16px\">Tran Details</p>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine("<td style=\"width:20%; padding:0px 5px; background-color:#33FF5E; text-align:right;\">");
        appendHtmlLine("<p style=\"font-size:16px\">Amount</p>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine(HTML_TR_CLOSE);
    }
    
    private void formatHtmlTransactionLine(String transactionId, String transactionDescription, 
                                         BigDecimal transactionAmount) {
        
        appendHtmlLine(HTML_TR_OPEN);
        appendHtmlLine("<td style=\"width:25%; padding:0px 5px; background-color:#f2f2f2; text-align:left;\">");
        appendHtmlLine("<p>" + transactionId + "</p>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine("<td style=\"width:55%; padding:0px 5px; background-color:#f2f2f2; text-align:left;\">");
        appendHtmlLine("<p>" + transactionDescription + "</p>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine("<td style=\"width:20%; padding:0px 5px; background-color:#f2f2f2; text-align:right;\">");
        appendHtmlLine("<p>" + formatCurrency(transactionAmount) + "</p>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine(HTML_TR_CLOSE);
    }
    
    private void formatHtmlBalanceSummary(BigDecimal totalExpenses, BigDecimal currentBalance, 
                                        BigDecimal availableCredit) {
        
        // Total expenses HTML row
        appendHtmlLine(HTML_TR_OPEN);
        appendHtmlLine("<td colspan=\"2\" style=\"padding:0px 5px; background-color:#f2f2f2; text-align:left;\">");
        appendHtmlLine("<p><strong>Total EXP:</strong></p>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine("<td style=\"width:20%; padding:0px 5px; background-color:#f2f2f2; text-align:right;\">");
        appendHtmlLine("<p><strong>" + formatCurrency(totalExpenses) + "</strong></p>");
        appendHtmlLine(HTML_TD_CLOSE);
        appendHtmlLine(HTML_TR_CLOSE);
    }
    
    /**
     * Gets the formatted text statement content.
     * 
     * @return Complete text statement as string
     */
    public String getTextStatement() {
        return textStatement.toString();
    }
    
    /**
     * Gets the formatted HTML statement content.
     * 
     * @return Complete HTML statement as string
     */
    public String getHtmlStatement() {
        return htmlStatement.toString();
    }
    
    /**
     * Inner class to represent transaction details.
     */
    public static class TransactionDetail {
        private String transactionId;
        private String description;
        private BigDecimal amount;
        
        public TransactionDetail(String transactionId, String description, BigDecimal amount) {
            this.transactionId = transactionId;
            this.description = description;
            this.amount = amount;
        }
        
        public String getTransactionId() { return transactionId; }
        public String getDescription() { return description; }
        public BigDecimal getAmount() { return amount; }
    }
}

    
    /**
     * Static method exported as standalone function for formatting statement trailer.
     * Replicates COBOL trailer formatting logic as a utility function.
     * 
     * @param statementDate Statement generation date
     * @param pageNumber Current page number
     * @param totalPages Total number of pages
     * @return Formatted trailer string
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static String formatStatementTrailer(LocalDate statementDate, int pageNumber, int totalPages) {
        
        // Input validation - replicating COBOL validation patterns
        if (statementDate == null) {
            statementDate = LocalDate.now();
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be greater than 0");
        }
        if (totalPages < 1) {
            throw new IllegalArgumentException("Total pages must be greater than 0");
        }
        if (pageNumber > totalPages) {
            throw new IllegalArgumentException("Page number cannot exceed total pages");
        }
        
        try {
            String dateLine = String.format("Statement Date: %s", 
                statementDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
            String pageLine = String.format("Page %d of %d", pageNumber, totalPages);
            
            // Format trailer with proper spacing - replicating COBOL page control logic
            String trailer = String.format("%-40s%40s", dateLine, pageLine);
            
            return trailer;
        } catch (Exception e) {
            // Fallback formatting if any formatting errors occur
            return String.format("%-40s%40s", 
                "Statement Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")), 
                "Page " + pageNumber + " of " + totalPages);
        }
    }
}