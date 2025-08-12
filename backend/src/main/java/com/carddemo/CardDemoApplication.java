package com.carddemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
@EnableJpaRepositories
@EnableTransactionManagement
public class CardDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDemoApplication.class, args);
    }
}