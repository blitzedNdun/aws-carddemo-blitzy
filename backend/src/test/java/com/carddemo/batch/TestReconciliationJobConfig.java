package com.carddemo.batch;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for ReconciliationJobConfig to resolve ApplicationContext loading
 * issues during integration testing.
 * 
 * Provides mock bean implementation required for Spring Boot test context initialization
 * without executing actual batch processing logic.
 */
@Configuration
@Profile("test")
public class TestReconciliationJobConfig {

    /**
     * Mock ReconciliationJobConfig bean for test environment.
     * Prevents ApplicationContext loading failures in integration tests.
     */
    @Bean
    @Primary
    public ReconciliationJobConfig reconciliationJobConfig() {
        return Mockito.mock(ReconciliationJobConfig.class);
    }
}