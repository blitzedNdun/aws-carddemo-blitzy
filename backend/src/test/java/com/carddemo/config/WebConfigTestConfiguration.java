package com.carddemo.config;


import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Minimal test configuration for WebConfig validation.
 * 
 * This configuration provides only the essential beans needed for WebConfig testing
 * while excluding problematic auto-configurations like Spring Batch, JPA, and Redis
 * that cause context loading issues during testing.
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    BatchAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    RedisAutoConfiguration.class
})
@Import({
    WebConfig.class
})
public class WebConfigTestConfiguration {

    /**
     * Mock PasswordEncoder bean to satisfy dependencies.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    // BmsMessageConverter and TransactionInterceptor beans are provided by WebConfig
    // CobolDataConverter uses static methods, no bean needed
}