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
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.security.JwtAuthenticationFilter;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    public UserSecurityRepository userSecurityRepository() {
        return Mockito.mock(UserSecurityRepository.class);
    }

    @Bean
    @Primary
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        JwtAuthenticationFilter mock = Mockito.mock(JwtAuthenticationFilter.class);
        try {
            // Configure the mock to properly handle the filter chain
            Mockito.doAnswer(invocation -> {
                jakarta.servlet.ServletRequest request = invocation.getArgument(0);
                jakarta.servlet.ServletResponse response = invocation.getArgument(1);
                jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                
                // Simply pass through to the next filter in the chain
                if (chain != null) {
                    chain.doFilter(request, response);
                }
                return null;
            }).when(mock).doFilter(
                Mockito.any(jakarta.servlet.ServletRequest.class),
                Mockito.any(jakarta.servlet.ServletResponse.class),
                Mockito.any(jakarta.servlet.FilterChain.class)
            );
        } catch (Exception e) {
            // If mock setup fails, return a basic mock
        }
        return mock;
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

    @Bean
    @Primary
    public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
        return Mockito.mock(HandlerMappingIntrospector.class);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}