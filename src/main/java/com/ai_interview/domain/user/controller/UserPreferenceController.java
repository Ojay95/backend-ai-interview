package com.ai_interview.domain.user.controller;

import com.ai_interview.domain.user.dto.UserPreferenceDto;
import com.ai_interview.domain.user.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<UserPreferenceDto> getPreferences(Authentication authentication) {
        return ResponseEntity.ok(preferenceService.getPreferences(authentication.getName()));
    }

    @PutMapping
    public ResponseEntity<UserPreferenceDto> updatePreferences(
            @RequestBody UserPreferenceDto dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(preferenceService.updatePreferences(authentication.getName(), dto));
    }
}