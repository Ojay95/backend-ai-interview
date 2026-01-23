package com.ai_interview.domain.user.controller;

import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.domain.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getMyProfile(Authentication authentication) {
        // 1. Get email from the JWT Token (stored in Authentication object)
        String email = authentication.getName();

        // 2. Fetch User from DB
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> InterviewException.notFound("User not found"));

        // 3. Return safe DTO
        return ResponseEntity.ok(UserResponse.from(user));
    }
}