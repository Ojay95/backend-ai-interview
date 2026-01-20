package com.ai_interview.domain.auth.dto;


import com.ai_interview.domain.auth.entity.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String token; // The JWT
    private UserSummary user;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserSummary {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private PlanType plan;
    }
}
