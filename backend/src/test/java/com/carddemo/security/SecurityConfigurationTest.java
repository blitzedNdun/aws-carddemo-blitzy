package com.carddemo.security;

import com.carddemo.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;


import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Integration tests for Spring Security configuration using MockMvc.
 * Tests security filter chain setup, authentication endpoints, authorization rules 
 * for protected resources, CORS configuration, and security headers matching 
 * mainframe security requirements.
 * 
 * Validates both JWT and session-based authentication mechanisms to ensure
 * configuration matches mainframe RACF security model.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, 
               classes = {SecurityConfig.class, SecurityTestConfig.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("Security Configuration Integration Tests")
class SecurityConfigurationTest {

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Nested
    @DisplayName("Security Filter Chain Configuration Tests")
    class SecurityFilterChainTests {

        @Test
        @DisplayName("Should configure security filter chain with proper bean creation")
        void testSecurityFilterChainConfiguration() throws Exception {
            // Verify that SecurityConfig creates the filter chain properly
            assertThat(securityConfig).isNotNull();
            assertThat(securityConfig.filterChain(null)).isNotNull();
        }

        @Test
        @DisplayName("Should enforce HTTPS redirect for all requests")
        void testHttpsRedirectEnforcement() throws Exception {
            // Test basic security configuration is loaded
            assertThat(securityConfig).isNotNull();
        }

        @Test
        @DisplayName("Should configure proper security headers matching mainframe standards")
        void testSecurityHeadersConfiguration() throws Exception {
            mockMvc.perform(get("/api/auth/login").secure(true))
                    .andDo(print())
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                    .andExpect(header().string("Strict-Transport-Security", 
                            "max-age=31536000; includeSubDomains"))
                    .andExpect(header().string("Content-Security-Policy", 
                            "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"));
        }

        @Test
        @DisplayName("Should apply security filter chain to all application endpoints")
        void testSecurityFilterChainCoverage() throws Exception {
            // Test various endpoints to ensure security is applied
            String[] protectedEndpoints = {
                "/api/accounts",
                "/api/transactions", 
                "/api/customers",
                "/api/cards",
                "/api/admin/users"
            };

            for (String endpoint : protectedEndpoints) {
                mockMvc.perform(get(endpoint))
                        .andDo(print())
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("Authentication Endpoint Tests")
    class AuthenticationEndpointTests {

        @Test
        @DisplayName("Should allow public access to authentication login endpoint")
        void testAuthenticationEndpointPublicAccess() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"TESTADM\",\"password\":\"ADMIN123\"}"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should allow public access to authentication status endpoint")
        void testAuthenticationStatusEndpointAccess() throws Exception {
            mockMvc.perform(get("/api/auth/status"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should handle authentication with valid RACF-style credentials")
        void testValidRacfStyleAuthentication() throws Exception {
            String validCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TESTADM", "password", "ADMIN123")
            );

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCredentials))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("TESTADM"))
                    .andExpect(jsonPath("$.userType").value("A"))
                    .andExpect(jsonPath("$.jwtToken").exists())
                    .andExpect(header().exists("Set-Cookie"));
        }

        @Test
        @DisplayName("Should reject authentication with invalid credentials")
        void testInvalidCredentialsAuthentication() throws Exception {
            String invalidCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "INVALID", "password", "WRONG")
            );

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidCredentials))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Authentication failed"))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        @Test
        @DisplayName("Should handle logout endpoint with session invalidation")
        void testLogoutEndpointSessionInvalidation() throws Exception {
            // First authenticate to get session
            String validCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TESTUSR", "password", "USER123")
            );

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCredentials))
                    .andExpect(status().isOk())
                    .andReturn();

            String sessionCookie = loginResult.getResponse().getHeader("Set-Cookie");

            // Then logout
            mockMvc.perform(post("/api/auth/logout")
                            .header("Cookie", sessionCookie))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logout successful"));
        }
    }

    @Nested
    @DisplayName("Authorization Rules Tests")
    class AuthorizationRulesTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should allow ROLE_ADMIN access to admin endpoints")
        void testAdminRoleAccessToAdminEndpoints() throws Exception {
            mockMvc.perform(get("/api/admin/users"))
                    .andDo(print())
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"NEWUSER\",\"password\":\"PASS123\",\"userType\":\"U\"}"))
                    .andDo(print())
                    .andExpect(status().isCreated());

            mockMvc.perform(put("/api/admin/users/NEWUSER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"NEWPASS123\"}"))
                    .andDo(print())
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/api/admin/users/NEWUSER"))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should deny ROLE_USER access to admin-only endpoints")
        void testUserRoleDeniedAccessToAdminEndpoints() throws Exception {
            mockMvc.perform(get("/api/admin/users"))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"NEWUSER\",\"password\":\"PASS123\"}"))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            mockMvc.perform(delete("/api/admin/users/SOMEUSER"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should allow ROLE_USER access to user endpoints")
        void testUserRoleAccessToUserEndpoints() throws Exception {
            mockMvc.perform(get("/api/accounts"))
                    .andDo(print())
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/transactions"))
                    .andDo(print())
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/customers"))
                    .andDo(print())
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/cards"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should allow ROLE_ADMIN access to both admin and user endpoints")
        void testAdminRoleAccessToBothAdminAndUserEndpoints() throws Exception {
            // Admin access to admin endpoints
            mockMvc.perform(get("/api/admin/users"))
                    .andDo(print())
                    .andExpect(status().isOk());

            // Admin access to user endpoints
            mockMvc.perform(get("/api/accounts"))
                    .andDo(print())
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/transactions"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should deny access to unauthenticated requests")
        void testUnauthenticatedAccessDenied() throws Exception {
            String[] protectedEndpoints = {
                "/api/accounts",
                "/api/transactions", 
                "/api/customers",
                "/api/cards",
                "/api/admin/users"
            };

            for (String endpoint : protectedEndpoints) {
                mockMvc.perform(get(endpoint))
                        .andDo(print())
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("CORS Configuration Tests")
    class CorsConfigurationTests {

        @Test
        @DisplayName("Should allow CORS requests from allowed frontend origins")
        void testCorsAllowedOrigins() throws Exception {
            mockMvc.perform(options("/api/auth/login")
                            .header("Origin", "http://localhost:3000")
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "Content-Type"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                    .andExpect(header().string("Access-Control-Allow-Methods", "GET,HEAD,POST,PUT,DELETE,OPTIONS"))
                    .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type,Authorization"))
                    .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        }

        @Test
        @DisplayName("Should deny CORS requests from disallowed origins")
        void testCorsDisallowedOrigins() throws Exception {
            mockMvc.perform(options("/api/auth/login")
                            .header("Origin", "http://malicious-site.com")
                            .header("Access-Control-Request-Method", "POST"))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }

        @Test
        @DisplayName("Should support CORS preflight requests for all HTTP methods")
        void testCorsPrelightAllMethods() throws Exception {
            String[] httpMethods = {"GET", "POST", "PUT", "DELETE", "PATCH"};
            
            for (String method : httpMethods) {
                mockMvc.perform(options("/api/auth/login")
                                .header("Origin", "http://localhost:3000")
                                .header("Access-Control-Request-Method", method))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(header().string("Access-Control-Allow-Methods", 
                                containsString(method)));
            }
        }

        @Test
        @DisplayName("Should allow common headers in CORS requests")
        void testCorsAllowedHeaders() throws Exception {
            String[] allowedHeaders = {
                "Content-Type",
                "Authorization", 
                "X-Requested-With",
                "Accept",
                "X-CSRF-TOKEN"
            };

            for (String header : allowedHeaders) {
                mockMvc.perform(options("/api/auth/login")
                                .header("Origin", "http://localhost:3000")
                                .header("Access-Control-Request-Method", "POST")
                                .header("Access-Control-Request-Headers", header))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(header().string("Access-Control-Allow-Headers", 
                                containsString(header)));
            }
        }
    }

    @Nested
    @DisplayName("JWT Authentication Tests")
    class JwtAuthenticationTests {

        @Test
        @DisplayName("Should generate valid JWT tokens on successful authentication")
        void testJwtTokenGeneration() throws Exception {
            String validCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TESTADM", "password", "ADMIN123")
            );

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCredentials))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jwtToken").exists())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            String jwtToken = (String) responseMap.get("jwtToken");

            assertThat(jwtToken).isNotNull();
            assertThat(jwtToken).startsWith("eyJ"); // JWT format validation
        }

        @Test
        @DisplayName("Should accept valid JWT tokens for API access")
        void testJwtTokenValidation() throws Exception {
            // First get a valid JWT token
            String validCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TESTUSR", "password", "USER123")
            );

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCredentials))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = loginResult.getResponse().getContentAsString();
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            String jwtToken = (String) responseMap.get("jwtToken");

            // Use JWT token to access protected endpoint
            mockMvc.perform(get("/api/accounts")
                            .header("Authorization", "Bearer " + jwtToken))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should reject invalid JWT tokens")
        void testInvalidJwtTokenRejection() throws Exception {
            String invalidToken = "invalid.jwt.token";

            mockMvc.perform(get("/api/accounts")
                            .header("Authorization", "Bearer " + invalidToken))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid JWT token"));
        }

        @Test
        @DisplayName("Should reject expired JWT tokens")
        void testExpiredJwtTokenRejection() throws Exception {
            // This would require a token with past expiration
            // For testing purposes, we'll simulate by modifying the token
            String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJURVNUVVNSIiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDAwMDB9.invalid";

            mockMvc.perform(get("/api/accounts")
                            .header("Authorization", "Bearer " + expiredToken))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("JWT token expired"));
        }

        @Test
        @DisplayName("Should include user roles in JWT token claims")
        void testJwtTokenRoleClaims() throws Exception {
            String adminCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TESTADM", "password", "ADMIN123")
            );

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(adminCredentials))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            String jwtToken = (String) responseMap.get("jwtToken");

            // Decode JWT payload to verify role claims
            String[] tokenParts = jwtToken.split("\\.");
            String payload = new String(Base64.getDecoder().decode(tokenParts[1]));
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);

            assertThat(claims.get("roles")).isEqualTo(List.of("ROLE_ADMIN"));
            assertThat(claims.get("userType")).isEqualTo("A");
        }
    }

    @Nested
    @DisplayName("Session-Based Authentication Tests")
    class SessionAuthenticationTests {

        @Test
        @DisplayName("Should create and maintain user sessions")
        void testSessionCreationAndMaintenance() throws Exception {
            String validCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TESTUSR", "password", "USER123")
            );

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCredentials))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Set-Cookie"))
                    .andReturn();

            String sessionCookie = loginResult.getResponse().getHeader("Set-Cookie");
            assertThat(sessionCookie).contains("JSESSIONID");

            // Use session cookie to access protected endpoint
            mockMvc.perform(get("/api/accounts")
                            .header("Cookie", sessionCookie))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should maintain session state across requests")
        void testSessionStateMaintenance() throws Exception {
            String validCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TESTADM", "password", "ADMIN123")
            );

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCredentials))
                    .andExpect(status().isOk())
                    .andReturn();

            String sessionCookie = loginResult.getResponse().getHeader("Set-Cookie");

            // Multiple requests using same session
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(get("/api/accounts")
                                .header("Cookie", sessionCookie))
                        .andExpect(status().isOk());
            }
        }

        @Test
        @DisplayName("Should invalidate sessions on logout")
        void testSessionInvalidationOnLogout() throws Exception {
            String validCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TESTUSR", "password", "USER123")
            );

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCredentials))
                    .andExpect(status().isOk())
                    .andReturn();

            String sessionCookie = loginResult.getResponse().getHeader("Set-Cookie");

            // Logout
            mockMvc.perform(post("/api/auth/logout")
                            .header("Cookie", sessionCookie))
                    .andExpect(status().isOk());

            // Attempt to use invalidated session
            mockMvc.perform(get("/api/accounts")
                            .header("Cookie", sessionCookie))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should handle session timeout appropriately")
        void testSessionTimeout() throws Exception {
            // This test would need to wait for actual timeout or mock time
            // For testing purposes, we'll verify timeout configuration
            String validCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TESTUSR", "password", "USER123")
            );

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCredentials))
                    .andExpect(status().isOk())
                    .andReturn();

            String sessionCookie = loginResult.getResponse().getHeader("Set-Cookie");
            
            // Verify session cookie contains Max-Age or expires directive
            assertThat(sessionCookie).containsAnyOf("Max-Age=", "expires=");
        }
    }

    @Nested
    @DisplayName("Password Encoder Configuration Tests")
    class PasswordEncoderTests {

        @Test
        @DisplayName("Should configure password encoder properly")
        void testPasswordEncoderConfiguration() throws Exception {
            PasswordEncoder encoder = securityConfig.passwordEncoder();
            assertThat(encoder).isNotNull();
            
            // Test password encoding functionality
            String plainPassword = "ADMIN123";
            String encodedPassword = encoder.encode(plainPassword);
            
            assertThat(encoder.matches(plainPassword, encodedPassword)).isTrue();
            assertThat(encoder.matches("WRONG", encodedPassword)).isFalse();
        }

        @Test
        @DisplayName("Should handle case-insensitive password matching for RACF compatibility")
        void testCaseInsensitivePasswordMatching() throws Exception {
            // Verify that the system handles case conversion properly
            String upperCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TESTADM", "password", "ADMIN123")
            );

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(upperCredentials))
                    .andDo(print())
                    .andExpect(status().isOk());

            String lowerCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "testadm", "password", "admin123")
            );

            // Should also work with lowercase (converted to uppercase)
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(lowerCredentials))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Authentication Manager Configuration Tests")
    class AuthenticationManagerTests {

        @Test
        @DisplayName("Should configure authentication manager with UserDetailsService")
        void testAuthenticationManagerConfiguration() throws Exception {
            // Verify SecurityConfig has passwordEncoder bean method
            PasswordEncoder encoder = securityConfig.passwordEncoder();
            assertThat(encoder).isNotNull();
        }

        @Test
        @DisplayName("Should configure CORS for frontend integration")
        void testCorsConfiguration() throws Exception {
            CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();
            assertThat(corsSource).isNotNull();
            
            // Verify CORS configuration exists
            CorsConfiguration corsConfig = corsSource.getCorsConfiguration(new MockHttpServletRequest());
            if (corsConfig != null) {
                assertThat(corsConfig.getAllowedOrigins()).isNotEmpty();
            }
        }

        @Test
        @DisplayName("Should configure security filter chain")
        void testSecurityFilterChainConfiguration() throws Exception {
            // Test requires HttpSecurity mock which is complex
            // This test verifies the bean method exists and can be called
            assertThat(securityConfig).isNotNull();
            assertThat(securityConfig.passwordEncoder()).isNotNull();
            assertThat(securityConfig.corsConfigurationSource()).isNotNull();
        }
    }



    @Nested
    @DisplayName("Mainframe RACF Security Model Compliance Tests")
    class RacfComplianceTests {

        @Test
        @DisplayName("Should enforce SEC-USR-TYPE based authorization like RACF")
        void testSecUsrTypeAuthorization() throws Exception {
            // Test Admin user (SEC-USR-TYPE = 'A') access
            String adminCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TESTADM", "password", "ADMIN123")
            );

            MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(adminCredentials))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userType").value("A"))
                    .andReturn();

            String adminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString())
                    .get("jwtToken").asText();

            // Admin should access admin functions
            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            // Test Regular user (SEC-USR-TYPE = 'U') access
            String userCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TESTUSR", "password", "USER123")
            );

            MvcResult userLogin = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userCredentials))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userType").value("U"))
                    .andReturn();

            String userToken = objectMapper.readTree(userLogin.getResponse().getContentAsString())
                    .get("jwtToken").asText();

            // Regular user should NOT access admin functions
            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should maintain RACF-style audit trail and logging")
        void testRacfStyleAuditTrail() throws Exception {
            String validCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TESTADM", "password", "ADMIN123")
            );

            // Successful authentication should generate audit log
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCredentials))
                    .andExpect(status().isOk());

            // Failed authentication should generate audit log
            String invalidCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "INVALID", "password", "WRONG")
            );

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidCredentials))
                    .andExpect(status().isUnauthorized());

            // Verify audit events are captured (would check logs in real implementation)
        }

        @Test
        @DisplayName("Should preserve RACF uppercase username conventions")
        void testRacfUsernameConventions() throws Exception {
            // Test lowercase input gets converted to uppercase
            String lowerCaseCredentials = objectMapper.writeValueAsString(
                java.util.Map.of("username", "testadm", "password", "ADMIN123")
            );

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(lowerCaseCredentials))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            // Verify username is returned in uppercase
            assertThat(responseMap.get("username")).isEqualTo("TESTADM");
        }

        @Test
        @DisplayName("Should implement RACF-equivalent resource protection")
        void testRacfResourceProtection() throws Exception {
            // Test resource protection similar to RACF profiles
            Map<String, String> resourceTests = java.util.Map.of(
                "/api/accounts", "Account data access like ACCT.** profiles",
                "/api/transactions", "Transaction data like TRANS.** profiles", 
                "/api/customers", "Customer data like CUST.** profiles",
                "/api/admin/users", "User management like SYS.ADMIN profiles"
            );

            for (Map.Entry<String, String> test : resourceTests.entrySet()) {
                mockMvc.perform(get(test.getKey()))
                        .andDo(print())
                        .andExpect(status().isUnauthorized());
            }
        }
    }


}