package com.carddemo.config;


import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;



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

    // PasswordEncoder is provided by SecurityConfig - no override needed

    /**
     * Provides an in-memory H2 database for test configuration dependencies.
     * This satisfies DataSource requirements from batch configurations without
     * requiring external database setup.
     */
    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb")
                .build();
    }

    /**
     * Provides a minimal EntityManagerFactory for JPA repository dependencies.
     * This satisfies JPA requirements from batch configurations without
     * requiring complex database setup or entity scanning.
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        // Don't scan for entities to avoid DDL issues - just provide minimal JPA setup
        em.setPackagesToScan(); // Empty scan
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "none"); // Don't create schema
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.setProperty("hibernate.show_sql", "false");
        em.setJpaProperties(properties);
        
        return em;
    }

    /**
     * Provides a transaction manager for JPA operations.
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }

    // BmsMessageConverter and TransactionInterceptor beans are provided by WebConfig
    // CobolDataConverter uses static methods, no bean needed
}