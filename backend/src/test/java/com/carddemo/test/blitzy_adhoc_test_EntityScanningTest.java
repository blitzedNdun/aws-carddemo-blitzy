package com.carddemo.test;

import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Isolated test to verify entity scanning and JPA configuration
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class blitzy_adhoc_test_EntityScanningTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void testApplicationContextLoads() {
        // Test that the Spring context loads successfully and entities are scanned
        // This verifies that all the property mapping issues are resolved
        assert entityManager != null;
        
        // Test that we can create entity instances (without persisting)
        Account account = Account.builder()
            .activeStatus("Y")
            .currentBalance(BigDecimal.valueOf(1000.00))
            .build();
        assert account != null;
        
        Customer customer = Customer.builder()
            .firstName("John")
            .lastName("Doe")
            .build();
        assert customer != null;
        
        // Context loaded successfully - all property issues resolved!
    }
}