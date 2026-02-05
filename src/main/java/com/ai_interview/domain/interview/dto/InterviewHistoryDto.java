package com.ai_interview.domain.interview.dto;

import com.ai_interview.domain.interview.entity.InterviewSession;
import lombok.Builder;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
@Builder
public class InterviewHistoryDto {
    private Long id;
    private String role;        // e.g. "Senior Frontend Engineer"
    private String type;        // e.g. "Technical Round"
    private String date;        // e.g. "Feb 24, 2026"
    private String duration;    // e.g. "45 mins"
    private Double score;       // e.g. 85.0
    private Integer scoreMax;   // Always 100 for consistency
    private String icon;        // Material Symbol icon name

    public static InterviewHistoryDto from(InterviewSession session) {
        // 1. Derive "Type" from Focus Areas or Default
        String derivedType = "General Interview";
        String derivedIcon = "psychology"; // default icon

        if (session.getFocusAreas() != null && !session.getFocusAreas().isEmpty()) {
            String firstFocus = session.getFocusAreas().split(",")[0].trim();
            derivedType = firstFocus;

            // Simple logic to pick an icon based on keywords
            if (firstFocus.toLowerCase().contains("technical") || firstFocus.toLowerCase().contains("code")) {
                derivedIcon = "code";
            } else if (firstFocus.toLowerCase().contains("behavioral")) {
                derivedIcon = "record_voice_over";
            } else if (firstFocus.toLowerCase().contains("system")) {
                derivedIcon = "hub";
            }
        }

        // 2. Format Date (e.g., "Oct 24, 2023")
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        String formattedDate = session.getStartedAt().format(formatter);

        // 3. Format Duration
        String formattedDuration = (session.getDurationSeconds() / 60) + " mins";

        return InterviewHistoryDto.builder()
                .id(session.getId())
                .role(session.getTargetRole())
                .type(derivedType)
                .date(formattedDate)
                .duration(formattedDuration)
                .score(session.getOverallScore() != null ? session.getOverallScore() : 0.0)
                .scoreMax(100) // Standardize on 100
                .icon(derivedIcon)
                .build();
    }
}