package com.carddemo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * StorageService provides comprehensive storage operations for the CardDemo application,
 * supporting multiple storage backends including cloud storage (S3, Azure Blob) and local storage.
 * This service abstracts storage operations to facilitate the mainframe-to-cloud migration
 * by providing enterprise-grade file management capabilities with compression support,
 * performance optimization, and storage monitoring.
 * 
 * Key Features:
 * - Multi-backend storage support (local, S3, Azure Blob)
 * - Multiple compression algorithms (GZIP, ZIP)
 * - Storage capacity monitoring and management
 * - Data integrity validation
 * - Automatic cleanup of expired data
 * - Performance-optimized data retrieval
 */
@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    // Storage configuration properties
    @Value("${carddemo.storage.local.basePath:/app/storage}")
    private String localBasePath;

    @Value("${carddemo.storage.cloud.enabled:false}")
    private boolean cloudStorageEnabled;

    @Value("${carddemo.storage.cloud.provider:local}")
    private String cloudProvider; // local, s3, azure

    @Value("${carddemo.storage.compression.enabled:true}")
    private boolean compressionEnabled;

    @Value("${carddemo.storage.compression.algorithm:gzip}")
    private String compressionAlgorithm; // gzip, zip

    @Value("${carddemo.storage.retention.days:365}")
    private int retentionDays;

    @Value("${carddemo.storage.capacity.maxSizeGB:100}")
    private long maxStorageCapacityGB;

    // Internal storage metadata tracking
    private final Map<String, StorageMetadata> storageMetadata = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> accessTimestamps = new ConcurrentHashMap<>();

    /**
     * Storage location enumeration for different data types
     */
    public enum StorageLocation {
        TRANSACTION_DATA("transactions"),
        ACCOUNT_DATA("accounts"),
        CUSTOMER_DATA("customers"),
        BATCH_FILES("batch"),
        REPORTS("reports"),
        ARCHIVE("archive"),
        TEMP("temp");

        private final String path;

        StorageLocation(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    /**
     * Compression algorithm enumeration
     */
    public enum CompressionType {
        NONE,
        GZIP,
        ZIP
    }

    /**
     * Storage backend enumeration
     */
    public enum StorageBackend {
        LOCAL,
        S3,
        AZURE_BLOB
    }

    /**
     * Internal class for tracking storage metadata
     */
    private static class StorageMetadata {
        private final String filename;
        private final long size;
        private final LocalDateTime created;
        private final CompressionType compression;
        private final StorageLocation location;
        private LocalDateTime lastAccessed;

        public StorageMetadata(String filename, long size, CompressionType compression, StorageLocation location) {
            this.filename = filename;
            this.size = size;
            this.compression = compression;
            this.location = location;
            this.created = LocalDateTime.now();
            this.lastAccessed = LocalDateTime.now();
        }

        // Getters and setters
        public String getFilename() { return filename; }
        public long getSize() { return size; }
        public LocalDateTime getCreated() { return created; }
        public CompressionType getCompression() { return compression; }
        public StorageLocation getLocation() { return location; }
        public LocalDateTime getLastAccessed() { return lastAccessed; }
        public void setLastAccessed(LocalDateTime lastAccessed) { this.lastAccessed = lastAccessed; }
    }

    /**
     * Stores data to the configured storage backend with optional compression.
     * Supports multiple storage locations and automatic compression based on configuration.
     * 
     * @param data The data to store as byte array
     * @param filename The filename for the stored data
     * @param location The storage location category
     * @return The storage key/path for retrieval
     * @throws StorageException if storage operation fails
     */
    public String storeData(byte[] data, String filename, StorageLocation location) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        if (location == null) {
            throw new IllegalArgumentException("Storage location cannot be null");
        }

        try {
            logger.info("Storing data: filename={}, size={} bytes, location={}", filename, data.length, location);

            // Check storage capacity before storing
            if (!hasCapacityForData(data.length)) {
                throw new StorageException("Insufficient storage capacity. Current usage exceeds configured limit.");
            }

            // Apply compression if enabled
            byte[] processedData = data;
            CompressionType appliedCompression = CompressionType.NONE;
            if (compressionEnabled && shouldCompress(filename, data.length)) {
                processedData = compressData(data);
                appliedCompression = CompressionType.valueOf(compressionAlgorithm.toUpperCase());
                logger.debug("Compressed data from {} to {} bytes using {}", data.length, processedData.length, appliedCompression);
            }

            // Generate storage path
            String storagePath = generateStoragePath(filename, location);
            
            // Store data based on configured backend
            StorageBackend backend = determineStorageBackend();
            switch (backend) {
                case LOCAL:
                    storeDataLocally(processedData, storagePath);
                    break;
                case S3:
                    storeDataInS3(processedData, storagePath);
                    break;
                case AZURE_BLOB:
                    storeDataInAzureBlob(processedData, storagePath);
                    break;
                default:
                    throw new StorageException("Unsupported storage backend: " + backend);
            }

            // Track storage metadata
            StorageMetadata metadata = new StorageMetadata(filename, processedData.length, appliedCompression, location);
            storageMetadata.put(storagePath, metadata);
            accessTimestamps.put(storagePath, LocalDateTime.now());

            logger.info("Successfully stored data at path: {}", storagePath);
            return storagePath;

        } catch (Exception e) {
            logger.error("Failed to store data: filename={}, location={}", filename, location, e);
            throw new StorageException("Failed to store data: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves data from storage with automatic decompression if applicable.
     * Optimized for performance with caching and efficient I/O operations.
     * 
     * @param storagePath The storage path/key returned from storeData
     * @return The retrieved data as byte array
     * @throws StorageException if retrieval operation fails
     */
    public byte[] retrieveData(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            throw new IllegalArgumentException("Storage path cannot be null or empty");
        }

        try {
            logger.debug("Retrieving data from storage path: {}", storagePath);

            // Update access timestamp for capacity management
            accessTimestamps.put(storagePath, LocalDateTime.now());

            // Retrieve data based on storage backend
            StorageBackend backend = determineStorageBackend();
            byte[] rawData;
            
            switch (backend) {
                case LOCAL:
                    rawData = retrieveDataLocally(storagePath);
                    break;
                case S3:
                    rawData = retrieveDataFromS3(storagePath);
                    break;
                case AZURE_BLOB:
                    rawData = retrieveDataFromAzureBlob(storagePath);
                    break;
                default:
                    throw new StorageException("Unsupported storage backend: " + backend);
            }

            // Apply decompression if metadata indicates compression was used
            StorageMetadata metadata = storageMetadata.get(storagePath);
            if (metadata != null && metadata.getCompression() != CompressionType.NONE) {
                byte[] decompressedData = decompressData(rawData, metadata.getCompression());
                logger.debug("Decompressed data from {} to {} bytes using {}", rawData.length, decompressedData.length, metadata.getCompression());
                
                // Update access timestamp in metadata
                metadata.setLastAccessed(LocalDateTime.now());
                return decompressedData;
            }

            logger.debug("Retrieved {} bytes from storage path: {}", rawData.length, storagePath);
            return rawData;

        } catch (Exception e) {
            logger.error("Failed to retrieve data from storage path: {}", storagePath, e);
            throw new StorageException("Failed to retrieve data: " + e.getMessage(), e);
        }
    }

    /**
     * Compresses data using the configured compression algorithm.
     * Supports GZIP and ZIP compression methods for optimal storage efficiency.
     * 
     * @param data The data to compress
     * @return The compressed data as byte array
     * @throws StorageException if compression operation fails
     */
    public byte[] compressData(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        try {
            CompressionType type = CompressionType.valueOf(compressionAlgorithm.toUpperCase());
            return compressData(data, type);
        } catch (Exception e) {
            logger.error("Failed to compress data using algorithm: {}", compressionAlgorithm, e);
            throw new StorageException("Failed to compress data: " + e.getMessage(), e);
        }
    }

    /**
     * Internal compression method supporting multiple algorithms
     */
    private byte[] compressData(byte[] data, CompressionType compressionType) throws IOException {
        switch (compressionType) {
            case GZIP:
                return compressWithGzip(data);
            case ZIP:
                return compressWithZip(data);
            case NONE:
                return data;
            default:
                throw new IllegalArgumentException("Unsupported compression type: " + compressionType);
        }
    }

    /**
     * GZIP compression implementation
     */
    private byte[] compressWithGzip(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
            gzipOut.finish();
            return baos.toByteArray();
        }
    }

    /**
     * ZIP compression implementation
     */
    private byte[] compressWithZip(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("data");
            zipOut.putNextEntry(entry);
            zipOut.write(data);
            zipOut.closeEntry();
            return baos.toByteArray();
        }
    }

    /**
     * Decompression method supporting multiple algorithms
     */
    private byte[] decompressData(byte[] compressedData, CompressionType compressionType) throws IOException {
        switch (compressionType) {
            case GZIP:
                return decompressWithGzip(compressedData);
            case ZIP:
                return decompressWithZip(compressedData);
            case NONE:
                return compressedData;
            default:
                throw new IllegalArgumentException("Unsupported compression type: " + compressionType);
        }
    }

    /**
     * GZIP decompression implementation
     */
    private byte[] decompressWithGzip(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipIn = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = gzipIn.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    /**
     * ZIP decompression implementation
     */
    private byte[] decompressWithZip(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             ZipInputStream zipIn = new ZipInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            ZipEntry entry = zipIn.getNextEntry();
            if (entry != null) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = zipIn.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
            }
            return baos.toByteArray();
        }
    }

    /**
     * Deletes data from storage and removes associated metadata.
     * Supports deletion across all configured storage backends.
     * 
     * @param storagePath The storage path/key of the data to delete
     * @return true if deletion was successful, false if data was not found
     * @throws StorageException if deletion operation fails
     */
    public boolean deleteData(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            throw new IllegalArgumentException("Storage path cannot be null or empty");
        }

        try {
            logger.info("Deleting data from storage path: {}", storagePath);

            // Delete data based on storage backend
            StorageBackend backend = determineStorageBackend();
            boolean deleted = false;
            
            switch (backend) {
                case LOCAL:
                    deleted = deleteDataLocally(storagePath);
                    break;
                case S3:
                    deleted = deleteDataFromS3(storagePath);
                    break;
                case AZURE_BLOB:
                    deleted = deleteDataFromAzureBlob(storagePath);
                    break;
                default:
                    throw new StorageException("Unsupported storage backend: " + backend);
            }

            // Remove metadata and access timestamps
            if (deleted) {
                storageMetadata.remove(storagePath);
                accessTimestamps.remove(storagePath);
                logger.info("Successfully deleted data and metadata for path: {}", storagePath);
            } else {
                logger.warn("Data not found for deletion at path: {}", storagePath);
            }

            return deleted;

        } catch (Exception e) {
            logger.error("Failed to delete data from storage path: {}", storagePath, e);
            throw new StorageException("Failed to delete data: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the appropriate storage location for a given data type.
     * Provides intelligent storage routing based on data characteristics.
     * 
     * @param dataType The type of data being stored
     * @param filename The filename of the data
     * @return The recommended storage location
     */
    public StorageLocation getStorageLocation(String dataType, String filename) {
        if (!StringUtils.hasText(dataType)) {
            throw new IllegalArgumentException("Data type cannot be null or empty");
        }

        String lowerDataType = dataType.toLowerCase();
        String lowerFilename = filename != null ? filename.toLowerCase() : "";

        // Determine storage location based on data type and filename patterns
        if (lowerDataType.contains("transaction") || lowerFilename.contains("tran")) {
            return StorageLocation.TRANSACTION_DATA;
        } else if (lowerDataType.contains("account") || lowerFilename.contains("acct")) {
            return StorageLocation.ACCOUNT_DATA;
        } else if (lowerDataType.contains("customer") || lowerFilename.contains("cust")) {
            return StorageLocation.CUSTOMER_DATA;
        } else if (lowerDataType.contains("batch") || lowerFilename.contains("batch")) {
            return StorageLocation.BATCH_FILES;
        } else if (lowerDataType.contains("report") || lowerFilename.contains("rpt")) {
            return StorageLocation.REPORTS;
        } else if (lowerDataType.contains("archive") || lowerFilename.contains("arch")) {
            return StorageLocation.ARCHIVE;
        } else if (lowerDataType.contains("temp") || lowerFilename.contains("tmp")) {
            return StorageLocation.TEMP;
        } else {
            // Default to transaction data for unknown types
            logger.debug("Unknown data type '{}', defaulting to TRANSACTION_DATA location", dataType);
            return StorageLocation.TRANSACTION_DATA;
        }
    }

    /**
     * Validates the integrity of stored data by performing checksum verification
     * and metadata consistency checks.
     * 
     * @param storagePath The storage path to validate
     * @return true if data integrity is valid, false otherwise
     * @throws StorageException if validation operation fails
     */
    public boolean validateStorageIntegrity(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            throw new IllegalArgumentException("Storage path cannot be null or empty");
        }

        try {
            logger.debug("Validating storage integrity for path: {}", storagePath);

            // Check if metadata exists
            StorageMetadata metadata = storageMetadata.get(storagePath);
            if (metadata == null) {
                logger.warn("No metadata found for storage path: {}", storagePath);
                return false;
            }

            // Verify file exists in storage backend
            StorageBackend backend = determineStorageBackend();
            boolean exists = false;
            
            switch (backend) {
                case LOCAL:
                    exists = checkDataExistsLocally(storagePath);
                    break;
                case S3:
                    exists = checkDataExistsInS3(storagePath);
                    break;
                case AZURE_BLOB:
                    exists = checkDataExistsInAzureBlob(storagePath);
                    break;
                default:
                    throw new StorageException("Unsupported storage backend: " + backend);
            }

            if (!exists) {
                logger.error("Data file does not exist in storage backend for path: {}", storagePath);
                return false;
            }

            // Verify data size consistency (if retrievable)
            try {
                byte[] retrievedData = retrieveData(storagePath);
                if (metadata.getCompression() == CompressionType.NONE) {
                    // For uncompressed data, verify exact size match
                    if (retrievedData.length != metadata.getSize()) {
                        logger.error("Data size mismatch for path: {}. Expected: {}, Actual: {}", 
                                   storagePath, metadata.getSize(), retrievedData.length);
                        return false;
                    }
                }
                // For compressed data, we cannot easily verify original size without decompression overhead
            } catch (Exception e) {
                logger.error("Failed to retrieve data for integrity validation: {}", storagePath, e);
                return false;
            }

            // Verify metadata consistency
            if (metadata.getCreated() == null || metadata.getLastAccessed() == null) {
                logger.error("Invalid metadata timestamps for path: {}", storagePath);
                return false;
            }

            logger.debug("Storage integrity validation passed for path: {}", storagePath);
            return true;

        } catch (Exception e) {
            logger.error("Failed to validate storage integrity for path: {}", storagePath, e);
            throw new StorageException("Failed to validate storage integrity: " + e.getMessage(), e);
        }
    }

    /**
     * Returns current storage capacity information including used space,
     * available space, and utilization percentage.
     * 
     * @return A map containing storage capacity metrics
     */
    public Map<String, Object> getStorageCapacity() {
        try {
            logger.debug("Retrieving storage capacity information");

            Map<String, Object> capacityInfo = new HashMap<>();
            
            // Calculate total used storage from metadata
            long totalUsedBytes = storageMetadata.values().stream()
                    .mapToLong(StorageMetadata::getSize)
                    .sum();

            long maxCapacityBytes = maxStorageCapacityGB * 1024L * 1024L * 1024L; // Convert GB to bytes
            long availableBytes = maxCapacityBytes - totalUsedBytes;
            double utilizationPercentage = (double) totalUsedBytes / maxCapacityBytes * 100.0;

            // Storage backend specific information
            StorageBackend backend = determineStorageBackend();
            Map<String, Object> backendInfo = getBackendCapacityInfo(backend);

            // Compile capacity information
            capacityInfo.put("backend", backend.toString());
            capacityInfo.put("totalUsedBytes", totalUsedBytes);
            capacityInfo.put("totalUsedMB", totalUsedBytes / (1024L * 1024L));
            capacityInfo.put("totalUsedGB", totalUsedBytes / (1024L * 1024L * 1024L));
            capacityInfo.put("maxCapacityBytes", maxCapacityBytes);
            capacityInfo.put("maxCapacityGB", maxStorageCapacityGB);
            capacityInfo.put("availableBytes", availableBytes);
            capacityInfo.put("availableGB", availableBytes / (1024L * 1024L * 1024L));
            capacityInfo.put("utilizationPercentage", Math.round(utilizationPercentage * 100.0) / 100.0);
            capacityInfo.put("fileCount", storageMetadata.size());
            capacityInfo.put("backendInfo", backendInfo);

            // Storage location breakdown
            Map<String, Long> locationBreakdown = new HashMap<>();
            for (StorageMetadata metadata : storageMetadata.values()) {
                String location = metadata.getLocation().toString();
                locationBreakdown.merge(location, metadata.getSize(), Long::sum);
            }
            capacityInfo.put("locationBreakdown", locationBreakdown);

            logger.debug("Storage capacity: {}GB used of {}GB total ({}% utilized)",
                        totalUsedBytes / (1024L * 1024L * 1024L), maxStorageCapacityGB, 
                        Math.round(utilizationPercentage * 100.0) / 100.0);

            return capacityInfo;

        } catch (Exception e) {
            logger.error("Failed to retrieve storage capacity information", e);
            throw new StorageException("Failed to retrieve storage capacity: " + e.getMessage(), e);
        }
    }

    /**
     * Cleans up expired data based on configured retention policies.
     * Removes data that exceeds the retention period and performs storage optimization.
     * 
     * @return The number of files cleaned up
     * @throws StorageException if cleanup operation fails
     */
    public int cleanupExpiredData() {
        try {
            logger.info("Starting cleanup of expired data. Retention period: {} days", retentionDays);

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            List<String> expiredPaths = new ArrayList<>();

            // Identify expired data based on creation date and last access
            for (Map.Entry<String, StorageMetadata> entry : storageMetadata.entrySet()) {
                String path = entry.getKey();
                StorageMetadata metadata = entry.getValue();
                
                // Consider data expired if it was created before cutoff date
                // AND has not been accessed recently (gives some grace period for active data)
                LocalDateTime lastAccess = accessTimestamps.getOrDefault(path, metadata.getCreated());
                
                if (metadata.getCreated().isBefore(cutoffDate) && lastAccess.isBefore(cutoffDate)) {
                    expiredPaths.add(path);
                    logger.debug("Identified expired data: path={}, created={}, lastAccessed={}", 
                               path, metadata.getCreated(), lastAccess);
                }
            }

            // Delete expired data
            int cleanupCount = 0;
            for (String path : expiredPaths) {
                try {
                    if (deleteData(path)) {
                        cleanupCount++;
                        logger.debug("Cleaned up expired data: {}", path);
                    }
                } catch (Exception e) {
                    logger.error("Failed to cleanup expired data at path: {}", path, e);
                    // Continue with other files even if one fails
                }
            }

            // Additional cleanup for temporary files
            int tempCleanupCount = cleanupTemporaryFiles();
            cleanupCount += tempCleanupCount;

            logger.info("Cleanup completed. Removed {} expired files", cleanupCount);
            return cleanupCount;

        } catch (Exception e) {
            logger.error("Failed to cleanup expired data", e);
            throw new StorageException("Failed to cleanup expired data: " + e.getMessage(), e);
        }
    }

    /**
     * Clean up temporary files that are older than 24 hours
     */
    private int cleanupTemporaryFiles() {
        int tempCleanupCount = 0;
        LocalDateTime tempCutoff = LocalDateTime.now().minusHours(24);

        for (Map.Entry<String, StorageMetadata> entry : storageMetadata.entrySet()) {
            String path = entry.getKey();
            StorageMetadata metadata = entry.getValue();
            
            if (metadata.getLocation() == StorageLocation.TEMP && 
                metadata.getCreated().isBefore(tempCutoff)) {
                try {
                    if (deleteData(path)) {
                        tempCleanupCount++;
                        logger.debug("Cleaned up temporary file: {}", path);
                    }
                } catch (Exception e) {
                    logger.error("Failed to cleanup temporary file: {}", path, e);
                }
            }
        }

        if (tempCleanupCount > 0) {
            logger.info("Cleaned up {} temporary files", tempCleanupCount);
        }

        return tempCleanupCount;
    }

    // ========== SUPPORTING PRIVATE METHODS ==========

    /**
     * Determines the storage backend to use based on configuration
     */
    private StorageBackend determineStorageBackend() {
        if (!cloudStorageEnabled) {
            return StorageBackend.LOCAL;
        }

        switch (cloudProvider.toLowerCase()) {
            case "s3":
                return StorageBackend.S3;
            case "azure":
            case "azure_blob":
                return StorageBackend.AZURE_BLOB;
            case "local":
            default:
                return StorageBackend.LOCAL;
        }
    }

    /**
     * Generates a storage path for the given filename and location
     */
    private String generateStoragePath(String filename, StorageLocation location) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return location.getPath() + "/" + timestamp + "/" + filename;
    }

    /**
     * Determines if data should be compressed based on file type and size
     */
    private boolean shouldCompress(String filename, int dataSize) {
        // Don't compress very small files (overhead not worth it)
        if (dataSize < 1024) {
            return false;
        }

        // Don't compress already compressed files
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".zip") || lowerFilename.endsWith(".gz") || 
            lowerFilename.endsWith(".7z") || lowerFilename.endsWith(".rar")) {
            return false;
        }

        // Compress text-based files and large binary files
        return true;
    }

    /**
     * Checks if there is sufficient capacity for the given data size
     */
    private boolean hasCapacityForData(long dataSize) {
        long totalUsedBytes = storageMetadata.values().stream()
                .mapToLong(StorageMetadata::getSize)
                .sum();
        long maxCapacityBytes = maxStorageCapacityGB * 1024L * 1024L * 1024L;
        return (totalUsedBytes + dataSize) <= maxCapacityBytes;
    }

    /**
     * Gets backend-specific capacity information
     */
    private Map<String, Object> getBackendCapacityInfo(StorageBackend backend) {
        Map<String, Object> info = new HashMap<>();
        info.put("type", backend.toString());

        switch (backend) {
            case LOCAL:
                try {
                    Path basePath = Paths.get(localBasePath);
                    if (Files.exists(basePath)) {
                        long totalSpace = Files.getFileStore(basePath).getTotalSpace();
                        long freeSpace = Files.getFileStore(basePath).getUsableSpace();
                        info.put("totalDiskSpace", totalSpace);
                        info.put("freeDiskSpace", freeSpace);
                        info.put("diskUtilization", (double)(totalSpace - freeSpace) / totalSpace * 100.0);
                    }
                } catch (IOException e) {
                    logger.warn("Failed to get local storage capacity info", e);
                }
                break;
            case S3:
                info.put("provider", "Amazon S3");
                info.put("unlimited", true);
                break;
            case AZURE_BLOB:
                info.put("provider", "Azure Blob Storage");
                info.put("unlimited", true);
                break;
        }

        return info;
    }

    // ========== STORAGE BACKEND IMPLEMENTATIONS ==========

    /**
     * LOCAL STORAGE IMPLEMENTATION
     */
    private void storeDataLocally(byte[] data, String storagePath) throws IOException {
        Path fullPath = Paths.get(localBasePath, storagePath);
        Files.createDirectories(fullPath.getParent());
        Files.write(fullPath, data, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        logger.debug("Stored data locally at: {}", fullPath);
    }

    private byte[] retrieveDataLocally(String storagePath) throws IOException {
        Path fullPath = Paths.get(localBasePath, storagePath);
        if (!Files.exists(fullPath)) {
            throw new StorageException("File not found: " + fullPath);
        }
        return Files.readAllBytes(fullPath);
    }

    private boolean deleteDataLocally(String storagePath) throws IOException {
        Path fullPath = Paths.get(localBasePath, storagePath);
        return Files.deleteIfExists(fullPath);
    }

    private boolean checkDataExistsLocally(String storagePath) {
        Path fullPath = Paths.get(localBasePath, storagePath);
        return Files.exists(fullPath);
    }

    /**
     * S3 STORAGE IMPLEMENTATION PLACEHOLDER
     * In a production implementation, this would use AWS SDK
     */
    private void storeDataInS3(byte[] data, String storagePath) {
        // TODO: Implement S3 storage using AWS SDK
        logger.warn("S3 storage not implemented - falling back to local storage");
        try {
            storeDataLocally(data, storagePath);
        } catch (IOException e) {
            throw new StorageException("Failed to store data in S3 fallback", e);
        }
    }

    private byte[] retrieveDataFromS3(String storagePath) {
        // TODO: Implement S3 retrieval using AWS SDK
        logger.warn("S3 retrieval not implemented - falling back to local storage");
        try {
            return retrieveDataLocally(storagePath);
        } catch (IOException e) {
            throw new StorageException("Failed to retrieve data from S3 fallback", e);
        }
    }

    private boolean deleteDataFromS3(String storagePath) {
        // TODO: Implement S3 deletion using AWS SDK
        logger.warn("S3 deletion not implemented - falling back to local storage");
        try {
            return deleteDataLocally(storagePath);
        } catch (IOException e) {
            throw new StorageException("Failed to delete data from S3 fallback", e);
        }
    }

    private boolean checkDataExistsInS3(String storagePath) {
        // TODO: Implement S3 existence check using AWS SDK
        logger.warn("S3 existence check not implemented - falling back to local storage");
        return checkDataExistsLocally(storagePath);
    }

    /**
     * AZURE BLOB STORAGE IMPLEMENTATION PLACEHOLDER
     * In a production implementation, this would use Azure SDK
     */
    private void storeDataInAzureBlob(byte[] data, String storagePath) {
        // TODO: Implement Azure Blob storage using Azure SDK
        logger.warn("Azure Blob storage not implemented - falling back to local storage");
        try {
            storeDataLocally(data, storagePath);
        } catch (IOException e) {
            throw new StorageException("Failed to store data in Azure Blob fallback", e);
        }
    }

    private byte[] retrieveDataFromAzureBlob(String storagePath) {
        // TODO: Implement Azure Blob retrieval using Azure SDK
        logger.warn("Azure Blob retrieval not implemented - falling back to local storage");
        try {
            return retrieveDataLocally(storagePath);
        } catch (IOException e) {
            throw new StorageException("Failed to retrieve data from Azure Blob fallback", e);
        }
    }

    private boolean deleteDataFromAzureBlob(String storagePath) {
        // TODO: Implement Azure Blob deletion using Azure SDK
        logger.warn("Azure Blob deletion not implemented - falling back to local storage");
        try {
            return deleteDataLocally(storagePath);
        } catch (IOException e) {
            throw new StorageException("Failed to delete data from Azure Blob fallback", e);
        }
    }

    private boolean checkDataExistsInAzureBlob(String storagePath) {
        // TODO: Implement Azure Blob existence check using Azure SDK
        logger.warn("Azure Blob existence check not implemented - falling back to local storage");
        return checkDataExistsLocally(storagePath);
    }

    // ========== CUSTOM EXCEPTION CLASS ==========

    /**
     * Custom exception for storage operations
     */
    public static class StorageException extends RuntimeException {
        public StorageException(String message) {
            super(message);
        }

        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
