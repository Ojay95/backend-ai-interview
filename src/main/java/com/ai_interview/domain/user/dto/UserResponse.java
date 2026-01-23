package com.ai_interview.domain.user.dto;

import com.ai_interview.domain.auth.entity.PlanType;
import com.ai_interview.domain.auth.entity.User;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private PlanType plan;

    // Helper method to convert Entity -> DTO
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .plan(user.getPlan())
                .build();
    }
}