package com.carddemo.batch.dto;

import com.carddemo.card.Card;
import com.carddemo.common.util.BigDecimalUtils;
import com.carddemo.common.util.DateUtils;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Data Transfer Object for card report output containing structured report data
 * including card details, account information, and formatted output lines for 
 * Spring Batch FlatFileItemWriter report generation.
 * 
 * <p>This DTO converts the original COBOL CBACT02C.cbl batch program output format
 * to Java-based report generation, maintaining exact field structure and formatting
 * requirements from the legacy CARD-RECORD copybook CVACT02Y.cpy.</p>
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Structured report data with card details matching original COBOL record layout</li>
 *   <li>Account information formatting with proper column alignment and field display</li>
 *   <li>Formatted output lines compatible with Spring Batch FlatFileItemWriter</li>
 *   <li>Header, detail, and summary record formatting methods for complete report generation</li>
 *   <li>Card data formatting utilities for consistent column alignment per original display logic</li>
 *   <li>Report metadata fields including generation timestamp and record count information</li>
 * </ul>
 * 
 * <p>Original COBOL Structure Mapping:</p>
 * <pre>
 * COBOL CARD-RECORD (CVACT02Y.cpy):
 *   CARD-NUM                    (16 chars)  → cardNumber
 *   CARD-ACCT-ID                (11 digits) → accountId
 *   CARD-CVV-CD                 (3 digits)  → cvvCode
 *   CARD-EMBOSSED-NAME          (50 chars)  → embossedName
 *   CARD-EXPIRAION-DATE         (10 chars)  → expirationDate
 *   CARD-ACTIVE-STATUS          (1 char)    → activeStatus
 * </pre>
 * 
 * <p>Report Format Requirements:</p>
 * <ul>
 *   <li>Header lines include report title, generation timestamp, and column headers</li>
 *   <li>Detail lines contain card information in fixed-width columns with proper alignment</li>
 *   <li>Summary lines provide record count and processing statistics</li>
 *   <li>Output compatible with Spring Batch FlatFileItemWriter for file generation</li>
 * </ul>
 * 
 * <p>Performance Characteristics:</p>
 * <ul>
 *   <li>Optimized for batch processing within 4-hour window constraint</li>
 *   <li>Memory efficient for large card datasets during report generation</li>
 *   <li>Thread-safe formatting utilities for parallel Spring Batch processing</li>
 * </ul>
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
public class CardReportDTO {

    /**
     * Credit card number (16 characters)
     * Converted from COBOL CARD-NUM field
     */
    private String cardNumber;

    /**
     * Account ID associated with the card (11 digits)
     * Converted from COBOL CARD-ACCT-ID field
     */
    private String accountId;

    /**
     * Name embossed on the credit card (50 characters)
     * Converted from COBOL CARD-EMBOSSED-NAME field
     */
    private String embossedName;

    /**
     * Card expiration date (10 characters)
     * Converted from COBOL CARD-EXPIRAION-DATE field
     */
    private String expirationDate;

    /**
     * Card active status (1 character)
     * Converted from COBOL CARD-ACTIVE-STATUS field
     */
    private String activeStatus;

    /**
     * Card verification value (3 digits)
     * Converted from COBOL CARD-CVV-CD field
     */
    private String cvvCode;

    /**
     * Report generation timestamp
     * Metadata field for report tracking and audit trail
     */
    private LocalDateTime generationTimestamp;

    /**
     * Default constructor for Spring Batch framework
     */
    public CardReportDTO() {
        this.generationTimestamp = LocalDateTime.now();
    }

    /**
     * Constructor for creating report DTO from Card entity
     * 
     * @param card Card entity containing card data
     */
    public CardReportDTO(Card card) {
        this();
        if (card != null) {
            this.cardNumber = card.getCardNumber();
            this.accountId = card.getAccountId();
            this.embossedName = card.getEmbossedName();
            this.expirationDate = DateUtils.formatDateForDisplay(card.getExpirationDate());
            this.activeStatus = card.getActiveStatus() != null ? card.getActiveStatus().name() : "";
            this.cvvCode = card.getCvvCode();
        }
    }

    /**
     * Constructor with all required fields
     * 
     * @param cardNumber 16-digit credit card number
     * @param accountId 11-digit account identifier
     * @param embossedName Name embossed on card
     * @param expirationDate Card expiration date
     * @param activeStatus Card active status
     * @param cvvCode 3-digit CVV code
     */
    public CardReportDTO(String cardNumber, String accountId, String embossedName, 
                        String expirationDate, String activeStatus, String cvvCode) {
        this();
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        this.embossedName = embossedName;
        this.expirationDate = expirationDate;
        this.activeStatus = activeStatus;
        this.cvvCode = cvvCode;
    }

    // Getter and Setter methods

    /**
     * Gets the credit card number
     * 
     * @return 16-digit credit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the credit card number
     * 
     * @param cardNumber 16-digit credit card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the account ID
     * 
     * @return 11-digit account identifier
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID
     * 
     * @param accountId 11-digit account identifier
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the embossed name
     * 
     * @return Name embossed on the card
     */
    public String getEmbossedName() {
        return embossedName;
    }

    /**
     * Sets the embossed name
     * 
     * @param embossedName Name embossed on the card
     */
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    /**
     * Gets the expiration date
     * 
     * @return Card expiration date string
     */
    public String getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the expiration date
     * 
     * @param expirationDate Card expiration date string
     */
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the active status
     * 
     * @return Card active status
     */
    public String getActiveStatus() {
        return activeStatus;
    }

    /**
     * Sets the active status
     * 
     * @param activeStatus Card active status
     */
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    /**
     * Gets the CVV code
     * 
     * @return 3-digit CVV code
     */
    public String getCvvCode() {
        return cvvCode;
    }

    /**
     * Sets the CVV code
     * 
     * @param cvvCode 3-digit CVV code
     */
    public void setCvvCode(String cvvCode) {
        this.cvvCode = cvvCode;
    }

    /**
     * Gets the report generation timestamp
     * 
     * @return Report generation timestamp
     */
    public LocalDateTime getGenerationTimestamp() {
        return generationTimestamp;
    }

    /**
     * Sets the report generation timestamp
     * 
     * @param generationTimestamp Report generation timestamp
     */
    public void setGenerationTimestamp(LocalDateTime generationTimestamp) {
        this.generationTimestamp = generationTimestamp;
    }

    // Report formatting methods

    /**
     * Gets the formatted report line for Spring Batch FlatFileItemWriter
     * 
     * <p>This method formats the card data into a single line suitable for
     * file output, maintaining the original COBOL display format with proper
     * field alignment and spacing.</p>
     * 
     * @return Formatted report line string
     */
    public String getReportLine() {
        return formatAsDetailLine();
    }

    /**
     * Formats the card data as a header line for report generation
     * 
     * <p>Creates a formatted header line with column titles and proper
     * alignment, matching the original COBOL report header format.</p>
     * 
     * @return Formatted header line string
     */
    public String formatAsHeaderLine() {
        StringBuilder header = new StringBuilder();
        
        // Report title and timestamp
        header.append("CREDIT CARD REPORT");
        header.append(" - Generated: ");
        header.append(DateUtils.formatDate(generationTimestamp.toLocalDate()));
        header.append(" ");
        header.append(String.format("%02d:%02d:%02d", 
            generationTimestamp.getHour(), 
            generationTimestamp.getMinute(), 
            generationTimestamp.getSecond()));
        header.append("\n");
        
        // Column headers with proper alignment
        header.append(String.format("%-16s %-11s %-50s %-10s %-6s %-3s",
            "CARD NUMBER", "ACCOUNT ID", "EMBOSSED NAME", "EXPIRES", "STATUS", "CVV"));
        header.append("\n");
        
        // Separator line
        header.append(String.format("%-16s %-11s %-50s %-10s %-6s %-3s",
            "----------------", "-----------", 
            "--------------------------------------------------", 
            "----------", "------", "---"));
        
        return header.toString();
    }

    /**
     * Formats the card data as a detail line for report generation
     * 
     * <p>Creates a formatted detail line with card information properly
     * aligned in columns, matching the original COBOL display logic with
     * exact field positioning and formatting.</p>
     * 
     * @return Formatted detail line string
     */
    public String formatAsDetailLine() {
        // Format card number (mask for security if needed)
        String formattedCardNumber = cardNumber != null ? cardNumber : "                ";
        
        // Format account ID with proper padding
        String formattedAccountId = accountId != null ? accountId : "           ";
        
        // Format embossed name with proper truncation/padding
        String formattedName = embossedName != null ? 
            String.format("%-50s", embossedName.length() > 50 ? 
                embossedName.substring(0, 50) : embossedName) : 
            String.format("%-50s", "");
        
        // Format expiration date
        String formattedExpiry = expirationDate != null ? 
            String.format("%-10s", expirationDate) : "          ";
        
        // Format status
        String formattedStatus = activeStatus != null ? 
            String.format("%-6s", activeStatus) : "      ";
        
        // Format CVV (mask for security)
        String formattedCvv = cvvCode != null ? "***" : "   ";
        
        return String.format("%-16s %-11s %-50s %-10s %-6s %-3s",
            formattedCardNumber, formattedAccountId, formattedName.trim(),
            formattedExpiry, formattedStatus, formattedCvv);
    }

    /**
     * Formats the card data as a summary line for report generation
     * 
     * <p>Creates a formatted summary line with processing statistics and
     * record count information, matching the original COBOL batch processing
     * summary format.</p>
     * 
     * @return Formatted summary line string
     */
    public String formatAsSummaryLine() {
        StringBuilder summary = new StringBuilder();
        
        // Summary separator
        summary.append("\n");
        summary.append(String.format("%-16s %-11s %-50s %-10s %-6s %-3s",
            "================", "===========", 
            "==================================================", 
            "==========", "======", "==="));
        summary.append("\n");
        
        // Summary information
        summary.append("REPORT SUMMARY - Processing completed at: ");
        summary.append(DateUtils.formatDate(generationTimestamp.toLocalDate()));
        summary.append(" ");
        summary.append(String.format("%02d:%02d:%02d", 
            generationTimestamp.getHour(), 
            generationTimestamp.getMinute(), 
            generationTimestamp.getSecond()));
        summary.append("\n");
        
        // Record status
        summary.append("Record Status: ");
        summary.append(activeStatus != null ? activeStatus : "UNKNOWN");
        summary.append(" - Card Number: ");
        summary.append(cardNumber != null ? 
            "**** **** **** " + cardNumber.substring(Math.max(0, cardNumber.length() - 4)) : 
            "NONE");
        
        return summary.toString();
    }

    // Utility methods

    /**
     * Validates the card report data for completeness
     * 
     * @return true if all required fields are present, false otherwise
     */
    public boolean isValid() {
        return cardNumber != null && !cardNumber.trim().isEmpty() &&
               accountId != null && !accountId.trim().isEmpty() &&
               embossedName != null && !embossedName.trim().isEmpty();
    }

    /**
     * Gets a masked version of the card number for secure display
     * 
     * @return Masked card number string
     */
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****************";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Gets the card type based on the card number prefix
     * 
     * @return Card type string
     */
    public String getCardType() {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return "Unknown";
        }
        
        char firstDigit = cardNumber.charAt(0);
        switch (firstDigit) {
            case '4': return "Visa";
            case '5': return "MasterCard";
            case '3': return "American Express";
            case '6': return "Discover";
            default: return "Unknown";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CardReportDTO that = (CardReportDTO) obj;
        return Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardNumber, accountId);
    }

    @Override
    public String toString() {
        return "CardReportDTO{" +
                "cardNumber='" + getMaskedCardNumber() + '\'' +
                ", accountId='" + accountId + '\'' +
                ", embossedName='" + embossedName + '\'' +
                ", expirationDate='" + expirationDate + '\'' +
                ", activeStatus='" + activeStatus + '\'' +
                ", generationTimestamp=" + generationTimestamp +
                '}';
    }
}