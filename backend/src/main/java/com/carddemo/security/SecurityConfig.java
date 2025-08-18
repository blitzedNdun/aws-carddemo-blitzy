package com.carddemo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration class providing password encoding bean.
 * 
 * This is a minimal security configuration to support the WebConfig.java validation.
 * The NoOpPasswordEncoder is used for COBOL legacy compatibility during migration.
 */
@Configuration
public class SecurityConfig {

    /**
     * Password encoder bean for legacy COBOL compatibility.
     * Uses NoOpPasswordEncoder to maintain plain-text password compatibility
     * during the migration from COBOL/RACF to Spring Security.
     * 
     * @return PasswordEncoder for plain-text password validation
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}