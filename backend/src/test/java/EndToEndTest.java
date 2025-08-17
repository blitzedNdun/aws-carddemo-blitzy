package com.carddemo.test;

import org.junit.jupiter.api.Tag;

/**
 * JUnit 5 test category marker interface for end-to-end tests.
 * 
 * <p>Used with @Tag annotation to categorize full workflow tests that validate complete
 * user journeys from UI through backend to database, ensuring functional parity with
 * COBOL CICS transactions.</p>
 * 
 * <p>End-to-end tests should:</p>
 * <ul>
 *   <li>Validate complete user workflows and business processes</li>
 *   <li>Test screen navigation matching 3270 terminal patterns</li>
 *   <li>Verify COBOL-to-Java functional parity</li>
 *   <li>Test complete transaction flows from React UI to database</li>
 *   <li>Validate session state management across requests</li>
 *   <li>Test error handling and validation flows</li>
 *   <li>Verify BMS screen flow replication</li>
 *   <li>Test integration with external systems</li>
 * </ul>
 * 
 * @since 1.0.0
 */
@Tag("e2e")
public interface EndToEndTest {
    // Marker interface - no methods needed
}