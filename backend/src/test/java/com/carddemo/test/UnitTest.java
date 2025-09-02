package com.carddemo.test;

import org.junit.jupiter.api.Tag;

/**
 * JUnit 5 test category marker interface for unit tests.
 * 
 * <p>Used with @Tag annotation to categorize fast-running, isolated unit tests that don't
 * require Spring context or external dependencies. Enables selective test execution via
 * Maven Surefire plugin configuration.</p>
 * 
 * <p>Unit tests should:</p>
 * <ul>
 *   <li>Run quickly (typically under 100ms per test)</li>
 *   <li>Be isolated and not depend on external systems</li>
 *   <li>Use mocks for dependencies</li>
 *   <li>Test business logic and calculations</li>
 *   <li>Validate COBOL-to-Java functional parity</li>
 * </ul>
 * 
 * @since 1.0.0
 */
@Tag("unit")
public interface UnitTest {
    // Marker interface - no methods needed
}