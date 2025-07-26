package com.carddemo.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test-specific SpringBoot Application without cloud discovery features
 * 
 * This simplified application class is designed specifically for unit and integration
 * testing, excluding cloud-native features that require external infrastructure.
 * 
 * @author Blitzy Development Team
 * @version 1.0
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.carddemo.auth",
        "com.carddemo.menu", 
        "com.carddemo.account",
        "com.carddemo.card",
        "com.carddemo.transaction",
        "com.carddemo.batch",
        "com.carddemo.common",
        "com.carddemo.config"
    }
)
public class CardDemoTestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CardDemoTestApplication.class, args);
    }
}