package com.ai_interview.domain.user.service;

import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.domain.auth.entity.User;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.domain.user.dto.UserPreferenceDto;
import com.ai_interview.domain.user.entity.UserPreference;
import com.ai_interview.domain.user.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserPreferenceDto getPreferences(String email) {
        User user = getUser(email);
        UserPreference pref = preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultPreferences(user));

        return UserPreferenceDto.from(pref);
    }

    @Transactional
    public UserPreferenceDto updatePreferences(String email, UserPreferenceDto dto) {
        User user = getUser(email);
        UserPreference pref = preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultPreferences(user));

        // Partial Update
        if (dto.getVoiceModel() != null) pref.setVoiceModel(dto.getVoiceModel());
        if (dto.getSpeechSpeed() != null) pref.setSpeechSpeed(dto.getSpeechSpeed());
        if (dto.getInterviewerPersona() != null) pref.setInterviewerPersona(dto.getInterviewerPersona());

        UserPreference saved = preferenceRepository.save(pref);
        return UserPreferenceDto.from(saved);
    }

    private UserPreference createDefaultPreferences(User user) {
        // Return a new transient object; saving happens on update or explicit save
        // For 'get', we might return defaults without saving to DB to save space until user customizes
        return UserPreference.builder()
                .user(user)
                .voiceModel("Zephyr")
                .speechSpeed(1.0)
                .interviewerPersona("recruiter")
                .build();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> InterviewException.notFound("User not found"));
    }
}