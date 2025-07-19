
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.carddemo.common.security.JwtAuthenticationFilter;
import com.carddemo.common.security.SecurityConfig;

/**
 * Test class for JWT Authentication Filter functionality
 */
public class JwtAuthenticationFilterTest {
    
    @Mock
    private SecurityConfig securityConfig;
    
    @Mock
    private JwtDecoder jwtDecoder;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(securityConfig.jwtDecoder()).thenReturn(jwtDecoder);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(securityConfig, eventPublisher);
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void testJwtTokenExtractionValid() {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer validjwttoken123456789");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // Act
        String token = jwtAuthenticationFilter.extractJwtFromRequest(request);
        
        // Assert
        assertNotNull(token);
        assertEquals("validjwttoken123456789", token);
        verify(eventPublisher, atLeastOnce()).publishEvent(any());
    }
    
    @Test
    void testJwtTokenExtractionInvalid() {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Invalid header");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // Act
        String token = jwtAuthenticationFilter.extractJwtFromRequest(request);
        
        // Assert
        assertNull(token);
        verify(eventPublisher, atLeastOnce()).publishEvent(any());
    }
    
    @Test
    void testJwtValidationSuccess() throws Exception {
        // Arrange
        String testToken = "valid.jwt.token";
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaimAsString("sub")).thenReturn("USER001");
        when(mockJwt.getClaimAsString("user_type")).thenReturn("U");
        when(mockJwt.getSubject()).thenReturn("USER001");
        when(mockJwt.getIssuer()).thenReturn(new java.net.URL("http://carddemo-auth"));
        when(mockJwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        
        when(jwtDecoder.decode(testToken)).thenReturn(mockJwt);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // Act
        Jwt result = jwtAuthenticationFilter.validateAndParseJwt(testToken, request);
        
        // Assert
        assertNotNull(result);
        assertEquals("USER001", result.getClaimAsString("sub"));
        assertEquals("U", result.getClaimAsString("user_type"));
        verify(eventPublisher, atLeastOnce()).publishEvent(any());
    }
    
    @Test
    void testJwtValidationFailure() {
        // Arrange
        String testToken = "invalid.jwt.token";
        when(jwtDecoder.decode(testToken)).thenThrow(new BadJwtException("Invalid token"));
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // Act & Assert
        assertThrows(BadJwtException.class, () -> {
            jwtAuthenticationFilter.validateAndParseJwt(testToken, request);
        });
        
        verify(eventPublisher, atLeastOnce()).publishEvent(any());
    }
    
    @Test
    void testSecurityContextEstablishment() {
        // Arrange
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaimAsString("sub")).thenReturn("USER001");
        when(mockJwt.getClaimAsString("user_type")).thenReturn("A");
        when(mockJwt.getClaimAsString("session_id")).thenReturn("SESSION123");
        
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // Act
        jwtAuthenticationFilter.establishSecurityContext(mockJwt, request);
        
        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("USER001", SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
            .stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
        verify(eventPublisher, atLeastOnce()).publishEvent(any());
    }
    
    @Test
    void testFilterChainContinuation() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);
        
        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
    }
}
