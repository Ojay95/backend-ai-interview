package com.ai_interview.domain.user.entity;

import com.ai_interview.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Matches 'Voice & AI' tab in SettingsScreen.tsx
    private String voiceModel;       // e.g., "Zephyr", "Echo"
    private Double speechSpeed;      // e.g., 1.2

    // Matches 'Interviewer Persona' section
    private String interviewerPersona; // e.g., "recruiter", "peer", "executive"

    // Default values
    @PrePersist
    public void prePersist() {
        if (voiceModel == null) voiceModel = "Zephyr";
        if (speechSpeed == null) speechSpeed = 1.0;
        if (interviewerPersona == null) interviewerPersona = "recruiter";
    }
}