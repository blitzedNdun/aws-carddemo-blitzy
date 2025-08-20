package com.carddemo.config;

import com.carddemo.controller.CardController;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Minimal test configuration for CardController tests.
 * Avoids loading the full application context and focuses only on the web layer.
 */
@TestConfiguration
@ComponentScan(basePackageClasses = CardController.class)
public class CardControllerTestConfig {
    
    // This configuration class is intentionally minimal to avoid loading
    // JPA and other non-web related configurations during @WebMvcTest
}