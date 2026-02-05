package com.ai_interview.domain.interview.dto;

import lombok.Data;
import java.util.List;

@Data
public class InterviewRequest {
    // Config Data
    private String targetRole;
    private String experienceLevel;
    private String language;
    private List<String> techStack;
    private List<String> focusAreas;
    private Integer durationSeconds;

    // Chat History
    private List<TranscriptDto> transcript;
}