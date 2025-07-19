
package com.carddemo.test.factory;

import com.carddemo.test.config.AccountDataReaderTestConfig;
import com.carddemo.common.entity.Account;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

@Component
public class AccountDataReaderFactory {
    
    @Autowired
    private AccountDataReaderTestConfig config;
    
    public FlatFileItemReader<Account> createAccountDataReader() {
        
        // Validate configuration is loaded
        if (!config.isValid()) {
            throw new IllegalStateException("AccountDataReaderTestConfig is not properly configured: " + config.getConfigurationSummary());
        }
        
        FlatFileItemReader<Account> reader = new FlatFileItemReader<>();
        
        // Set resource from configuration
        Map<String, Object> resourceConfig = config.getResource();
        String filePath = (String) resourceConfig.get("file-path");
        if (filePath != null && filePath.startsWith("classpath:")) {
            String resourcePath = filePath.substring("classpath:".length());
            reader.setResource(new ClassPathResource(resourcePath));
        }
        
        // Configure line mapper
        DefaultLineMapper<Account> lineMapper = new DefaultLineMapper<>();
        
        // Configure tokenizer
        FixedLengthTokenizer tokenizer = createTokenizer();
        lineMapper.setLineTokenizer(tokenizer);
        
        // Configure field set mapper
        BeanWrapperFieldSetMapper<Account> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Account.class);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        
        reader.setLineMapper(lineMapper);
        
        // Set name for monitoring
        reader.setName("accountDataReader");
        
        return reader;
    }
    
    private FixedLengthTokenizer createTokenizer() {
        FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();
        
        // Get tokenizer configuration
        Map<String, Object> tokenizerConfig = config.getTokenizer();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) tokenizerConfig.get("columns");
        
        // Build ranges and names
        List<Range> ranges = new ArrayList<>();
        List<String> names = new ArrayList<>();
        
        for (Map<String, Object> column : columns) {
            String name = (String) column.get("name");
            Integer start = (Integer) column.get("start");
            Integer end = (Integer) column.get("end");
            
            if (name != null && start != null && end != null) {
                names.add(name);
                ranges.add(new Range(start, end));
            }
        }
        
        tokenizer.setColumns(ranges.toArray(new Range[0]));
        tokenizer.setNames(names.toArray(new String[0]));
        
        return tokenizer;
    }
    
    /**
     * Validates that the reader can be created successfully
     */
    public boolean validateReaderCreation() {
        try {
            FlatFileItemReader<Account> reader = createAccountDataReader();
            return reader != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets detailed validation results
     */
    public String getValidationDetails() {
        try {
            FlatFileItemReader<Account> reader = createAccountDataReader();
            return String.format("Reader created successfully. Reader type: %s", 
                                reader.getClass().getSimpleName());
        } catch (Exception e) {
            return "Reader creation failed: " + e.getMessage();
        }
    }
}
