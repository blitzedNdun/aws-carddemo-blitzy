package com.carddemo.security;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;

/**
 * Test configuration providing mock beans for security testing.
 */
@TestConfiguration
public class SecurityTestConfig {

    @Bean
    @Primary
    public CustomUserDetailsService customUserDetailsService() {
        CustomUserDetailsService mock = Mockito.mock(CustomUserDetailsService.class);
        
        // Setup mock behavior for test users
        UserDetails adminUser = User.builder()
                .username("TESTADM1")
                .password("ADMIN123")
                .authorities(Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
        
        UserDetails regularUser = User.builder()
                .username("TESTUSER1") 
                .password("USER123")
                .authorities(Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        
        try {
            Mockito.when(mock.loadUserByUsername("TESTADM1")).thenReturn(adminUser);
            Mockito.when(mock.loadUserByUsername("TESTUSER1")).thenReturn(regularUser);
            Mockito.when(mock.loadUserByUsername(Mockito.anyString()))
                   .thenThrow(new UsernameNotFoundException("User not found"));
        } catch (Exception e) {
            // Handle any exceptions during mock setup
        }
        
        return mock;
    }

    @Bean
    @Primary
    public JwtTokenService jwtTokenService() {
        return Mockito.mock(JwtTokenService.class);
    }
    
    @Bean
    @Primary 
    public UserDetailsService userDetailsService() {
        return username -> {
            switch (username) {
                case "TESTADM1":
                    return User.builder()
                            .username("TESTADM1")
                            .password("ADMIN123")
                            .authorities(Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .build();
                case "TESTUSER1":
                    return User.builder()
                            .username("TESTUSER1")
                            .password("USER123")
                            .authorities(Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")))
                            .build();
                default:
                    throw new UsernameNotFoundException("User not found: " + username);
            }
        };
    }
}