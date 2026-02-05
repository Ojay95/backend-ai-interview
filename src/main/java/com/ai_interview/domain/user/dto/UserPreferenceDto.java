package com.ai_interview.domain.user.dto;

import com.ai_interview.domain.user.entity.UserPreference;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserPreferenceDto {
    private String voiceModel;
    private Double speechSpeed;
    private String interviewerPersona;

    public static UserPreferenceDto from(UserPreference entity) {
        return UserPreferenceDto.builder()
                .voiceModel(entity.getVoiceModel())
                .speechSpeed(entity.getSpeechSpeed())
                .interviewerPersona(entity.getInterviewerPersona())
                .build();
    }
}