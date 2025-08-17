package com.carddemo.test;

import org.junit.jupiter.api.Tag;

/**
 * JUnit 5 test category marker interface for integration tests.
 * 
 * <p>Used with @Tag annotation to categorize tests that require Spring context, database
 * connections, or external services. Supports Testcontainers-based testing with PostgreSQL
 * and Redis containers.</p>
 * 
 * <p>Integration tests should:</p>
 * <ul>
 *   <li>Test component interactions and integrations</li>
 *   <li>Use Testcontainers for database isolation</li>
 *   <li>Validate Spring Boot application context</li>
 *   <li>Test REST endpoint functionality</li>
 *   <li>Verify data access layer operations</li>
 *   <li>Test session state management with Redis</li>
 * </ul>
 * 
 * @since 1.0.0
 */
@Tag("integration")
public interface IntegrationTest {
    // Marker interface - no methods needed
}