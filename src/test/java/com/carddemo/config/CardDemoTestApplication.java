package com.carddemo.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test-specific SpringBoot Application without cloud discovery features
 * 
 * This simplified application class is designed specifically for unit and integration
 * testing, excluding cloud-native features that require external infrastructure.
 * 
 * @author Blitzy Development Team
 * @version 1.0
 */
@EnableJpaRepositories(
    basePackages = {
        "com.carddemo.auth.repository",
        "com.carddemo.account.repository", 
        "com.carddemo.card",
        "com.carddemo.transaction",
        "com.carddemo.batch.repository",
        "com.carddemo.common.repository"
    }
)
@EntityScan(
    basePackages = {
        "com.carddemo.auth.entity",
        "com.carddemo.account.entity",
        "com.carddemo.card",
        "com.carddemo.transaction.entity",
        "com.carddemo.batch.entity",
        "com.carddemo.common.entity"
    }
)
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