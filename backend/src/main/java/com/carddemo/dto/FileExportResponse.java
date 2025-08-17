package com.carddemo.dto;

import java.time.LocalDateTime;

/**
 * Data transfer object for file export operation responses.
 * 
 * This DTO encapsulates all metadata and information about file export operations,
 * providing standardized responses for file generation requests. It supports
 * file-based interfaces with external systems while maintaining identical file
 * layouts and data formats as required by the COBOL-to-Java migration.
 * 
 * The response includes comprehensive export metadata including file format type
 * (CSV, fixed-width), record count for exported records, file size, generation
 * timestamp, and file path information to ensure complete traceability of
 * export operations.
 * 
 * Used by FileController to return standardized responses for file export
 * operations, ensuring consistent API responses across all file generation
 * endpoints while maintaining compatibility with external system interfaces.
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024
 */
public class FileExportResponse {
    
    /**
     * The format type of the exported file.
     * Supports "CSV" for comma-separated values format and "fixed-width" 
     * for fixed-width text format to maintain compatibility with existing
     * external system interfaces.
     */
    private String fileFormat;
    
    /**
     * The total number of records exported to the file.
     * This count represents the actual data records written, excluding
     * header and trailer records, providing accurate processing metrics
     * for downstream systems.
     */
    private Long recordCount;
    
    /**
     * The size of the generated file in bytes.
     * Provides file size information for transfer validation and
     * storage capacity planning in external systems.
     */
    private Long fileSize;
    
    /**
     * The timestamp when the file was generated.
     * Records the exact generation time for audit trails and
     * file versioning in external interfaces.
     */
    private LocalDateTime generationTimestamp;
    
    /**
     * The name of the generated file.
     * Contains the actual filename without path information,
     * following standardized naming conventions for external
     * system compatibility.
     */
    private String fileName;
    
    /**
     * The full path to the generated file.
     * Provides complete file location information for
     * file retrieval and transfer operations.
     */
    private String filePath;
    
    /**
     * Default constructor for FileExportResponse.
     * Creates an empty response object that can be populated
     * with export operation results.
     */
    public FileExportResponse() {
        // Default constructor for JSON deserialization and Spring Boot compatibility
    }
    
    /**
     * Parameterized constructor for FileExportResponse.
     * Creates a fully populated response object with all export metadata.
     * 
     * @param fileFormat The format type of the exported file (CSV or fixed-width)
     * @param recordCount The total number of records exported
     * @param fileSize The size of the generated file in bytes
     * @param generationTimestamp The timestamp when the file was generated
     * @param fileName The name of the generated file
     * @param filePath The full path to the generated file
     */
    public FileExportResponse(String fileFormat, Long recordCount, Long fileSize, 
                             LocalDateTime generationTimestamp, String fileName, String filePath) {
        this.fileFormat = fileFormat;
        this.recordCount = recordCount;
        this.fileSize = fileSize;
        this.generationTimestamp = generationTimestamp;
        this.fileName = fileName;
        this.filePath = filePath;
    }
    
    /**
     * Gets the format type of the exported file.
     * 
     * @return The file format ("CSV" for comma-separated values or "fixed-width" 
     *         for fixed-width text format)
     */
    public String getFileFormat() {
        return fileFormat;
    }
    
    /**
     * Sets the format type of the exported file.
     * 
     * @param fileFormat The file format to set ("CSV" or "fixed-width")
     */
    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }
    
    /**
     * Gets the total number of records exported to the file.
     * 
     * @return The record count representing actual data records written,
     *         excluding header and trailer records
     */
    public Long getRecordCount() {
        return recordCount;
    }
    
    /**
     * Sets the total number of records exported to the file.
     * 
     * @param recordCount The record count to set
     */
    public void setRecordCount(Long recordCount) {
        this.recordCount = recordCount;
    }
    
    /**
     * Gets the size of the generated file in bytes.
     * 
     * @return The file size in bytes for transfer validation and
     *         storage capacity planning
     */
    public Long getFileSize() {
        return fileSize;
    }
    
    /**
     * Sets the size of the generated file in bytes.
     * 
     * @param fileSize The file size to set in bytes
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    /**
     * Gets the timestamp when the file was generated.
     * 
     * @return The generation timestamp for audit trails and file versioning
     */
    public LocalDateTime getGenerationTimestamp() {
        return generationTimestamp;
    }
    
    /**
     * Sets the timestamp when the file was generated.
     * 
     * @param generationTimestamp The generation timestamp to set
     */
    public void setGenerationTimestamp(LocalDateTime generationTimestamp) {
        this.generationTimestamp = generationTimestamp;
    }
    
    /**
     * Gets the name of the generated file.
     * 
     * @return The filename without path information, following standardized
     *         naming conventions for external system compatibility
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Sets the name of the generated file.
     * 
     * @param fileName The filename to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    /**
     * Gets the full path to the generated file.
     * 
     * @return The complete file location for file retrieval and transfer operations
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * Sets the full path to the generated file.
     * 
     * @param filePath The file path to set
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    /**
     * Returns a string representation of the FileExportResponse object.
     * Provides a formatted summary of all export metadata for logging
     * and debugging purposes.
     * 
     * @return A string representation of this FileExportResponse
     */
    @Override
    public String toString() {
        return "FileExportResponse{" +
                "fileFormat='" + fileFormat + '\'' +
                ", recordCount=" + recordCount +
                ", fileSize=" + fileSize +
                ", generationTimestamp=" + generationTimestamp +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
    
    /**
     * Indicates whether some other object is "equal to" this one.
     * Compares all fields for equality to support proper object comparison
     * in collections and testing scenarios.
     * 
     * @param obj The reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FileExportResponse that = (FileExportResponse) obj;
        
        if (fileFormat != null ? !fileFormat.equals(that.fileFormat) : that.fileFormat != null) return false;
        if (recordCount != null ? !recordCount.equals(that.recordCount) : that.recordCount != null) return false;
        if (fileSize != null ? !fileSize.equals(that.fileSize) : that.fileSize != null) return false;
        if (generationTimestamp != null ? !generationTimestamp.equals(that.generationTimestamp) : that.generationTimestamp != null) return false;
        if (fileName != null ? !fileName.equals(that.fileName) : that.fileName != null) return false;
        return filePath != null ? filePath.equals(that.filePath) : that.filePath == null;
    }
    
    /**
     * Returns a hash code value for the object.
     * Generates hash code based on all fields to support proper object
     * usage in hash-based collections.
     * 
     * @return A hash code value for this object
     */
    @Override
    public int hashCode() {
        int result = fileFormat != null ? fileFormat.hashCode() : 0;
        result = 31 * result + (recordCount != null ? recordCount.hashCode() : 0);
        result = 31 * result + (fileSize != null ? fileSize.hashCode() : 0);
        result = 31 * result + (generationTimestamp != null ? generationTimestamp.hashCode() : 0);
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        result = 31 * result + (filePath != null ? filePath.hashCode() : 0);
        return result;
    }
}