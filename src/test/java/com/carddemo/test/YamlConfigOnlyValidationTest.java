package com.carddemo.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Minimal YAML configuration validation test for application-dev.yml.
 * Tests only configuration loading without database dependencies.
 */
@SpringBootTest(
    classes = YamlConfigOnlyValidationTest.MinimalTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
    // Disable all auto-configurations that require external dependencies
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
    "org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration," +
    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
    "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
    "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration," +
    "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
    "org.springframework.cloud.gateway.config.GatewayAutoConfiguration," +
    "org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration," +
    "org.springframework.cloud.config.client.ConfigClientAutoConfiguration"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class YamlConfigOnlyValidationTest {

    @Autowired
    private Environment environment;

    @Test
    public void testApplicationDevYmlConfigurationLoadsSuccessfully() {
        // Test that the dev profile is active
        String[] activeProfiles = environment.getActiveProfiles();
        assertThat(activeProfiles).contains("dev");
        
        System.out.println("✅ Dev profile is active: " + String.join(", ", activeProfiles));
    }

    @Test
    public void testBasicApplicationProperties() {
        // Test that application properties are loaded correctly
        String appName = environment.getProperty("spring.application.name");
        assertThat(appName).isNotNull();
        System.out.println("✅ Application name loaded: " + appName);
        
        String appDescription = environment.getProperty("spring.application.description");
        assertThat(appDescription).isNotNull();
        System.out.println("✅ Application description loaded: " + appDescription);
    }

    @Test
    public void testServerConfigurationProperties() {
        // Test server configuration
        String port = environment.getProperty("server.port");
        assertThat(port).isEqualTo("8080");
        System.out.println("✅ Server port: " + port);
        
        String contextPath = environment.getProperty("server.servlet.context-path");
        assertThat(contextPath).isEqualTo("/carddemo");
        System.out.println("✅ Context path: " + contextPath);
    }

    @Test
    public void testCustomCardDemoProperties() {
        // Test custom carddemo properties
        String envName = environment.getProperty("carddemo.environment.name");
        assertThat(envName).isEqualTo("Development");
        System.out.println("✅ Environment name: " + envName);
        
        String dailyLimit = environment.getProperty("carddemo.business.transaction.daily-limit");
        assertThat(dailyLimit).isEqualTo("100000.00");
        System.out.println("✅ Daily transaction limit: " + dailyLimit);
    }

    @Test
    public void testLoggingConfigurationProperties() {
        // Test logging configuration
        String rootLevel = environment.getProperty("logging.level.root");
        assertThat(rootLevel).isEqualTo("INFO");
        System.out.println("✅ Root logging level: " + rootLevel);
        
        String cardDemoLevel = environment.getProperty("logging.level.com.carddemo");
        assertThat(cardDemoLevel).isEqualTo("DEBUG");
        System.out.println("✅ CardDemo logging level: " + cardDemoLevel);
    }

    @Test
    public void testValidationConfigurationProperties() {
        // Test validation configuration
        String decimalScale = environment.getProperty("validation.decimal-precision.default-scale");
        assertThat(decimalScale).isEqualTo("2");
        System.out.println("✅ Decimal scale: " + decimalScale);
        
        String accountPattern = environment.getProperty("validation.patterns.account-number");
        assertThat(accountPattern).isEqualTo("^[0-9]{11}$");
        System.out.println("✅ Account number pattern: " + accountPattern);
    }

    // Minimal Spring Boot application that excludes problematic auto-configurations
    @SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        BatchAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    })
    public static class MinimalTestApplication {
        // Empty test application - just enough to load configuration
    }
}