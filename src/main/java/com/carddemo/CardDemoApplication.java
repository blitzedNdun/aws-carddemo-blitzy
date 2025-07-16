package com.carddemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main Spring Boot application class for the CardDemo microservices ecosystem.
 * 
 * This application serves as the entry point and orchestrator for the entire CardDemo
 * system, which represents a complete technology stack transformation from IBM 
 * COBOL/CICS/VSAM/JCL/RACF to modern cloud-native Java 21 Spring Boot microservices
 * architecture with PostgreSQL database, Docker containerization, Kubernetes 
 * orchestration, React frontend, and Spring Security authentication.
 * 
 * Architecture Overview:
 * - Service-per-transaction microservices pattern with 36 independent Spring Boot services
 * - Spring Cloud Gateway for API routing and load balancing
 * - Spring Cloud Netflix Eureka for service discovery and orchestration
 * - Spring Cloud Config Server for centralized configuration management
 * - Redis-backed session management for stateless REST APIs
 * - PostgreSQL with JPA repositories for data persistence
 * - Spring Security for JWT-based authentication and authorization
 * 
 * Component Scanning:
 * This application automatically scans all domain packages including:
 * - com.carddemo.auth - Authentication and security services
 * - com.carddemo.menu - Menu navigation and role-based access
 * - com.carddemo.account - Account management operations
 * - com.carddemo.card - Card lifecycle management
 * - com.carddemo.transaction - Transaction processing services
 * - com.carddemo.batch - Spring Batch job processing
 * - com.carddemo.common - Shared DTOs and utilities
 * 
 * Performance Requirements:
 * - Transaction response times under 200ms at 95th percentile
 * - Support for 10,000 TPS through horizontal scaling
 * - Kubernetes HPA for dynamic service replication
 * - Circuit breakers and resilience patterns
 * 
 * Data Precision:
 * - BigDecimal with MathContext.DECIMAL128 for exact COBOL COMP-3 equivalency
 * - PostgreSQL SERIALIZABLE isolation for VSAM-equivalent data consistency
 * - Spring @Transactional with REQUIRES_NEW propagation for CICS syncpoint behavior
 * 
 * Technology Stack:
 * - Java 21 LTS with Spring Boot 3.2.x
 * - Spring Cloud 2023.0.x (Leyton Release Train)
 * - PostgreSQL 15+ with HikariCP connection pooling
 * - Redis 7+ for session clustering
 * - Kubernetes 1.28+ for container orchestration
 * 
 * @author Blitzy Development Team
 * @version 1.0
 * @since Spring Boot 3.2.x
 */
@SpringBootApplication(scanBasePackages = {
    "com.carddemo.auth",
    "com.carddemo.menu", 
    "com.carddemo.account",
    "com.carddemo.card",
    "com.carddemo.transaction",
    "com.carddemo.batch",
    "com.carddemo.common"
})
@EnableDiscoveryClient
public class CardDemoApplication {

    /**
     * Main method to bootstrap the Spring Boot application.
     * 
     * This method initializes the Spring ApplicationContext and starts the embedded
     * Tomcat server, enabling the microservices ecosystem to register with Eureka
     * service discovery and accept incoming requests through Spring Cloud Gateway.
     * 
     * Bootstrap Process:
     * 1. Initialize Spring Boot auto-configuration
     * 2. Register with Eureka service discovery
     * 3. Load application configuration from Config Server
     * 4. Start embedded Tomcat server
     * 5. Enable Spring Cloud Gateway routing
     * 6. Initialize JPA repositories and connection pooling
     * 7. Configure Redis session management
     * 8. Activate Spring Security JWT authentication
     * 
     * Service Discovery:
     * The application automatically registers with Eureka server for service
     * discovery, enabling Spring Cloud Gateway to route requests to available
     * microservice instances and support horizontal scaling through Kubernetes
     * Horizontal Pod Autoscaler (HPA).
     * 
     * Configuration Management:
     * Application properties are loaded from Spring Cloud Config Server,
     * supporting environment-specific configurations and externalized property
     * management equivalent to CICS SIT parameter handling.
     * 
     * Health Monitoring:
     * Spring Boot Actuator endpoints provide health checks, metrics, and
     * monitoring capabilities for Kubernetes liveness and readiness probes.
     * 
     * @param args Command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(CardDemoApplication.class, args);
    }
}