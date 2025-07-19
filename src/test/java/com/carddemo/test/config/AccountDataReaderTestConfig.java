package com.carddemo.test.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Test configuration for Account data reader components.
 * Provides configuration settings for test data processing and validation.
 */
@Component
@ConfigurationProperties(prefix = "test.account-data-reader")
public class AccountDataReaderTestConfig {

    private String testDataPath = "fixtures/csv/accounts.csv";
    private int expectedRecordCount = 100;
    private boolean skipHeaderRecord = true;
    private String encoding = "UTF-8";

    /**
     * Validates that the configuration is properly set up.
     * 
     * @return true if configuration is valid
     */
    public boolean isValid() {
        return testDataPath != null && !testDataPath.trim().isEmpty() &&
               expectedRecordCount > 0 &&
               encoding != null && !encoding.trim().isEmpty();
    }

    /**
     * Gets a summary of the current configuration.
     * 
     * @return Configuration summary string
     */
    public String getConfigurationSummary() {
        return String.format("testDataPath=%s, expectedRecordCount=%d, skipHeaderRecord=%b, encoding=%s",
                testDataPath, expectedRecordCount, skipHeaderRecord, encoding);
    }

    // Getters and setters
    public String getTestDataPath() {
        return testDataPath;
    }

    public void setTestDataPath(String testDataPath) {
        this.testDataPath = testDataPath;
    }

    public int getExpectedRecordCount() {
        return expectedRecordCount;
    }

    public void setExpectedRecordCount(int expectedRecordCount) {
        this.expectedRecordCount = expectedRecordCount;
    }

    public boolean isSkipHeaderRecord() {
        return skipHeaderRecord;
    }

    public void setSkipHeaderRecord(boolean skipHeaderRecord) {
        this.skipHeaderRecord = skipHeaderRecord;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Gets the resource configuration for the test data file.
     * 
     * @return Resource configuration map
     */
    public Map<String, Object> getResource() {
        Map<String, Object> resource = new HashMap<>();
        resource.put("file-path", "classpath:" + testDataPath);
        resource.put("encoding", encoding);
        return resource;
    }

    /**
     * Gets the tokenizer configuration for parsing fixed-length records.
     * 
     * @return Tokenizer configuration map
     */
    public Map<String, Object> getTokenizer() {
        Map<String, Object> tokenizer = new HashMap<>();
        
        // Define columns for account data (basic structure)
        List<Map<String, Object>> columns = new ArrayList<>();
        
        // Account ID (positions 1-11)
        Map<String, Object> accountId = new HashMap<>();
        accountId.put("name", "accountId");
        accountId.put("start", 1);
        accountId.put("end", 11);
        columns.add(accountId);
        
        // Customer ID (positions 12-20)
        Map<String, Object> customerId = new HashMap<>();
        customerId.put("name", "customerId");
        customerId.put("start", 12);
        customerId.put("end", 20);
        columns.add(customerId);
        
        // Account status (positions 21-21)
        Map<String, Object> status = new HashMap<>();
        status.put("name", "accountStatus");
        status.put("start", 21);
        status.put("end", 21);
        columns.add(status);
        
        tokenizer.put("columns", columns);
        return tokenizer;
    }
}