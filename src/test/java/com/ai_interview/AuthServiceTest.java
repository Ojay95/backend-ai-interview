package com.ai_interview;

import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.common.util.JwtUtils;
import com.ai_interview.domain.auth.dto.LoginRequest;
import com.ai_interview.domain.auth.dto.RegisterRequest;
import com.ai_interview.domain.auth.entity.User;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.domain.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("John", "Doe", "john@example.com", "password123");
        loginRequest = new LoginRequest("john@example.com", "password123");
    }

    @Test
    void register_ShouldReturnToken_WhenUserIsNew() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(jwtUtils.generateToken(any())).thenReturn("mock-jwt-token");

        // When
        var response = authService.register(registerRequest);

        // Then
        assertNotNull(response.getToken());
        assertEquals("mock-jwt-token", response.getToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // When/Then
        assertThrows(InterviewException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreValid() {
        // Given
        User mockUser = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .passwordHash("encodedPass")
                .build();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(jwtUtils.generateToken(any())).thenReturn("mock-jwt-token");

        // When
        var response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
    }
}
