package com.ai_interview.domain.auth.service;


import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.common.util.JwtUtils;
import com.ai_interview.domain.auth.dto.*;

import com.ai_interview.domain.auth.entity.PasswordResetToken;
import com.ai_interview.domain.auth.entity.PlanType;
import com.ai_interview.domain.auth.entity.User;
import com.ai_interview.domain.auth.repository.PasswordResetTokenRepository;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.infrastructure.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    // 1. Register Logic
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw InterviewException.badRequest("Email already in use");
        }

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .plan(PlanType.FREE)
                .build();

        userRepository.save(user);
        return generateAuthResponse(user);
    }

    // 2. Login Logic
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> InterviewException.badRequest("User not found"));

        return generateAuthResponse(user);
    }

    // 3. Forgot Password Logic
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> InterviewException.notFound("User not found"));

        // Clean up old tokens
        tokenRepository.deleteByUser(user);

        // Generate new token
        String token = UUID.randomUUID().toString();
        var resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();

        tokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    // 4. Reset Password Logic
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        var resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> InterviewException.badRequest("Invalid token"));

        if (resetToken.isExpired()) {
            throw InterviewException.badRequest("Token expired");
        }

        var user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        tokenRepository.delete(resetToken);
    }

    private AuthResponse generateAuthResponse(User user) {
        // Need to convert User to UserDetails for JWT generation
        var userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getPlan().name())
                .build();

        String jwtToken = jwtUtils.generateToken(userDetails);

        return AuthResponse.builder()
                .token(jwtToken)
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .plan(user.getPlan())
                        .build())
                .build();
    }
}
