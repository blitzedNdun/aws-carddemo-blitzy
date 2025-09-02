/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.mock;

/**
 * Minimal configuration for contract testing with external services.
 * Provides mocked beans for external service dependencies without requiring
 * full Spring Boot application context initialization.
 * 
 * This configuration is specifically designed for isolated contract testing
 * where only essential beans are needed and external service calls should
 * be mocked to ensure test reliability and speed.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@TestConfiguration
@Profile("test")
public class MinimalContractTestConfig {

    /**
     * Provides a mocked RestTemplate for external service contract testing.
     * This allows contract tests to run without actual external service calls.
     * 
     * @return Mocked RestTemplate instance
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return mock(RestTemplate.class);
    }

    /**
     * Provides a mocked external service client for payment gateway integration.
     * This ensures contract tests can validate request/response structures
     * without requiring actual payment gateway connectivity.
     * 
     * @return Mocked payment gateway client
     */
    @Bean
    @Primary
    public Object paymentGatewayClient() {
        return mock(Object.class);
    }

    /**
     * Provides a mocked external service client for credit bureau integration.
     * This ensures contract tests can validate request/response structures
     * without requiring actual credit bureau connectivity.
     * 
     * @return Mocked credit bureau client
     */
    @Bean
    @Primary
    public Object creditBureauClient() {
        return mock(Object.class);
    }
}