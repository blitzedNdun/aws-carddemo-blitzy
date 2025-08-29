package com.carddemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot Application for CardDemo
 * COBOL to Java Migration - Spring Boot Backend
 * 
 * This application serves as the modernized version of the mainframe
 * CardDemo COBOL/CICS application, providing REST APIs and batch processing
 * capabilities while maintaining 100% functional parity.
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude = {BatchAutoConfiguration.class})
@EntityScan(basePackages = "com.carddemo.entity")
@EnableJpaRepositories(basePackages = "com.carddemo.repository")
@EnableTransactionManagement
public class CardDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDemoApplication.class, args);
    }
}