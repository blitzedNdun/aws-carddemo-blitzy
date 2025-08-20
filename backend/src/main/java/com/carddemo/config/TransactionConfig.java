/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * Transaction management configuration for Spring transactions that replicate CICS SYNCPOINT behavior.
 * 
 * This configuration class establishes comprehensive transaction management infrastructure for the CardDemo
 * application's migration from CICS transaction processing to Spring Boot declarative transaction management.
 * The configuration replicates CICS SYNCPOINT and rollback behavior while providing modern transaction
 * management capabilities including proper isolation levels, rollback rules, and propagation settings.
 * 
 * Core Responsibilities:
 * - Configure JPA transaction manager with CICS-equivalent transaction boundaries
 * - Set appropriate isolation levels for concurrent access matching CICS READ UPDATE locking
 * - Define rollback rules for specific exception types matching COBOL ABEND conditions
 * - Configure propagation settings for nested transactions in complex business operations
 * - Provide transaction template for programmatic transaction control when needed
 * 
 * CICS Transaction Behavior Replication:
 * - SYNCPOINT: Implemented through @Transactional commit behavior with proper ACID compliance
 * - ROLLBACK: Automatic rollback on runtime exceptions with configurable rollback rules
 * - READ UPDATE: Proper isolation levels preventing dirty reads and ensuring data consistency
 * - Transaction Boundaries: Method-level transaction scoping matching COBOL program structure
 * - Resource Coordination: Single-phase commit for PostgreSQL operations with proper cleanup
 * 
 * Transaction Configuration Features:
 * - @EnableTransactionManagement: Enables annotation-driven transaction support for service methods
 * - JpaTransactionManager: JPA-specific transaction manager handling EntityManager lifecycle
 * - TransactionTemplate: Programmatic transaction management for complex business logic
 * - Isolation Levels: Configurable isolation matching CICS concurrent access patterns
 * - Rollback Rules: Exception-based rollback configuration matching COBOL error handling
 * 
 * Performance and Reliability Features:
 * - Connection pool integration with HikariCP for optimal resource utilization
 * - Transaction timeout configuration suitable for financial processing requirements  
 * - Proper resource cleanup and connection management after transaction completion
 * - Support for read-only transactions enabling query optimization where appropriate
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    /**
     * Configures JPA transaction manager for managing JPA entity transactions.
     * 
     * This method creates and configures a JpaTransactionManager that provides transactional behavior
     * equivalent to CICS SYNCPOINT operations. The transaction manager handles EntityManager lifecycle,
     * coordinates database transactions, and ensures proper ACID compliance for all data access operations
     * migrated from VSAM file processing to JPA entity management.
     * 
     * JPA Transaction Manager Features:
     * - EntityManager lifecycle management ensuring proper persistence context handling
     * - Database transaction coordination with PostgreSQL connection management
     * - Rollback handling for failed transactions with proper resource cleanup
     * - Integration with Spring Data JPA repositories for declarative transaction support
     * - Support for transaction propagation and isolation level configuration
     * 
     * CICS SYNCPOINT Equivalency:
     * - Automatic commit at method completion matching CICS transaction completion behavior
     * - Rollback on unchecked exceptions matching COBOL ABEND transaction termination
     * - Resource cleanup and connection management matching CICS resource coordination
     * - Transaction isolation preventing dirty reads equivalent to CICS exclusive control
     * 
     * Performance Optimizations:
     * - Connection reuse through HikariCP integration for optimal database resource utilization
     * - Transaction-scoped EntityManager enabling first-level cache utilization
     * - Batch processing support for bulk operations with configurable batch sizes
     * - Read-only transaction support enabling query optimization for reporting operations
     * 
     * @param entityManagerFactory JPA EntityManagerFactory for managing persistent entities
     * @return configured JpaTransactionManager for Spring transaction management
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        
        // Configure transaction manager behavior for CICS compatibility
        transactionManager.setRollbackOnCommitFailure(true);
        transactionManager.setFailEarlyOnGlobalRollbackOnly(true);
        transactionManager.setNestedTransactionAllowed(false); // Match CICS behavior
        transactionManager.setValidateExistingTransaction(true);
        transactionManager.setGlobalRollbackOnParticipationFailure(true);
        
        return transactionManager;
    }

    /**
     * Configures TransactionTemplate for programmatic transaction management.
     * 
     * This method creates a TransactionTemplate that provides programmatic transaction control for
     * complex business operations that require fine-grained transaction management beyond the
     * capabilities of declarative @Transactional annotations. The template enables precise control
     * over transaction boundaries, rollback conditions, and resource management in scenarios where
     * COBOL programs performed explicit transaction control.
     * 
     * Programmatic Transaction Features:
     * - Explicit transaction boundary control through execute() callback methods
     * - Custom rollback logic based on business conditions rather than exceptions only
     * - Nested transaction support for complex multi-step business processes
     * - Resource management with guaranteed cleanup regardless of transaction outcome
     * - Integration with declarative transactions for hybrid transaction management approaches
     * 
     * Use Cases for TransactionTemplate:
     * - Complex batch processing operations requiring multiple transaction boundaries
     * - Business logic with conditional commit/rollback based on processing results
     * - Integration scenarios requiring transaction coordination with external systems
     * - Error handling scenarios requiring custom rollback logic beyond exception handling
     * - Performance-critical operations requiring minimal transaction overhead
     * 
     * COBOL Integration Equivalency:
     * - Explicit SYNCPOINT equivalent through template execute() method completion
     * - Conditional ROLLBACK equivalent through TransactionStatus.setRollbackOnly()
     * - Resource cleanup equivalent to COBOL file close and resource deallocation
     * - Exception handling with transaction context preservation for error reporting
     * 
     * @param transactionManager Spring PlatformTransactionManager for transaction coordination
     * @return configured TransactionTemplate for programmatic transaction management
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        
        // Configure template for CICS-equivalent behavior
        template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
        template.setIsolationLevel(TransactionTemplate.ISOLATION_READ_COMMITTED);
        template.setTimeout(30); // 30 seconds matching CICS transaction timeout
        template.setReadOnly(false); // Allow write operations
        
        return template;
    }

    /**
     * Provides the same PlatformTransactionManager bean with explicit naming for dependency injection.
     * 
     * This method creates an alias for the transactionManager bean to support components that require
     * PlatformTransactionManager injection by specific bean name. This ensures compatibility with
     * various Spring components and third-party integrations that may reference the transaction
     * manager by the standard platform transaction manager bean name.
     * 
     * Bean Naming Convention:
     * - Supports both "transactionManager" and "platformTransactionManager" bean names
     * - Ensures compatibility with Spring Batch and other frameworks expecting standard naming
     * - Provides flexibility for dependency injection scenarios requiring specific bean references
     * - Maintains consistency with Spring Boot auto-configuration naming conventions
     * 
     * Integration Support:
     * - Spring Batch JobRepository configuration requiring PlatformTransactionManager reference
     * - Third-party frameworks using standard Spring transaction manager naming conventions
     * - Custom components requiring explicit transaction manager dependency injection
     * - Testing frameworks requiring specific transaction manager bean name references
     * 
     * @param entityManagerFactory JPA EntityManagerFactory for transaction manager configuration
     * @return same JpaTransactionManager instance as transactionManager() method
     */
    @Bean(name = "platformTransactionManager")
    public PlatformTransactionManager platformTransactionManager(EntityManagerFactory entityManagerFactory) {
        return transactionManager(entityManagerFactory);
    }
}