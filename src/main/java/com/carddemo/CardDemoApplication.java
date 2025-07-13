package com.carddemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * CardDemo Main Application Class
 * 
 * Main Spring Boot application class serving as the entry point and orchestrator for the entire 
 * CardDemo microservices ecosystem. This application enables Spring Cloud service discovery, 
 * API gateway routing, and centralized configuration management for the complete transformation 
 * from legacy COBOL/CICS/VSAM mainframe architecture to modern Java 21 Spring Boot microservices.
 * 
 * Architecture Implementation:
 * - Service-per-transaction microservices pattern with 36 independent Spring Boot services
 * - Spring Cloud Gateway for API routing and load balancing 
 * - Eureka service discovery for dynamic service registration and orchestration
 * - Component scanning across all domain packages (auth, menu, account, card, transaction, batch, common)
 * 
 * Performance Requirements:
 * - Sub-200ms transaction response times at 95th percentile
 * - Support for 10,000+ TPS through horizontal pod autoscaling
 * - 4-hour batch processing window completion
 * - Memory usage within 110% of legacy CICS allocation
 * 
 * Technology Stack:
 * - Spring Boot 3.2.x with Java 21 LTS runtime
 * - Spring Cloud 2023.0.x (Leyton Release Train)
 * - PostgreSQL 15+ for data persistence (replacing VSAM)
 * - Redis 7+ for distributed session management
 * - Kubernetes 1.28+ for container orchestration
 * 
 * Business Logic Preservation:
 * - Maintains exact functional equivalence with original COBOL programs
 * - Preserves COMP-3 decimal precision using BigDecimal with MathContext.DECIMAL128
 * - Replicates CICS transaction boundaries through Spring @Transactional REQUIRES_NEW
 * - Implements VSAM-equivalent data access patterns via JPA repositories
 * 
 * @author Blitzy Agent
 * @version 1.0.0
 * @since Spring Boot 3.2.x
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.carddemo.auth",        // Authentication microservices (JWT, security)
        "com.carddemo.menu",        // Menu navigation services (role-based routing)
        "com.carddemo.account",     // Account management services (view, update operations)
        "com.carddemo.card",        // Card lifecycle services (list, update, management)
        "com.carddemo.transaction", // Transaction processing services (payments, authorizations)
        "com.carddemo.batch",       // Batch processing services (Spring Batch jobs)
        "com.carddemo.common"       // Shared DTOs, utilities, and cross-cutting concerns
    }
)
@EnableDiscoveryClient
public class CardDemoApplication {

    /**
     * Main application entry point for the CardDemo microservices ecosystem.
     * 
     * Bootstraps the Spring Boot application with comprehensive microservices capabilities:
     * 
     * 1. Spring Cloud Service Discovery Integration:
     *    - Automatic registration with Eureka service registry
     *    - Dynamic service location and load balancing
     *    - Health check reporting and service monitoring
     * 
     * 2. Component Scanning Configuration:
     *    - Scans all domain-specific packages for Spring components
     *    - Enables auto-configuration of microservice controllers
     *    - Registers JPA repositories for data access
     * 
     * 3. Spring Cloud Gateway Preparation:
     *    - Prepares application for API gateway routing
     *    - Enables cross-service communication patterns
     *    - Supports circuit breaker and retry mechanisms
     * 
     * 4. Microservices Orchestration:
     *    - Facilitates 36 independent service deployments
     *    - Enables horizontal scaling via Kubernetes HPA
     *    - Supports distributed transaction management
     * 
     * Configuration Requirements:
     * - application.yml must define Eureka server URL
     * - PostgreSQL connection properties for data persistence
     * - Redis configuration for session management
     * - Logging configuration for centralized observability
     * 
     * Deployment Considerations:
     * - Designed for containerized deployment via Docker
     * - Kubernetes-ready with health probe endpoints
     * - Supports rolling updates and zero-downtime deployment
     * - Environment-specific configuration via Spring Cloud Config
     * 
     * @param args Command line arguments passed to the application
     *             Standard Spring Boot application arguments supported:
     *             --spring.profiles.active=<profile> for environment configuration
     *             --server.port=<port> for custom port binding
     *             --spring.cloud.config.uri=<uri> for config server location
     */
    public static void main(String[] args) {
        // Bootstrap Spring Boot application with comprehensive microservices support
        // This single call initializes:
        // - Spring IoC container with component scanning
        // - Auto-configuration for Spring Boot starters
        // - Eureka discovery client registration
        // - Embedded Tomcat server for REST endpoints
        // - Spring Security configuration
        // - JPA entity scanning and repository initialization
        // - Spring Session Redis integration
        // - Actuator endpoints for monitoring and health checks
        SpringApplication.run(CardDemoApplication.class, args);
    }
}