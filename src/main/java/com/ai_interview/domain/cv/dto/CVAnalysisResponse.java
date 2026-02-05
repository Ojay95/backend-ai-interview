package com.ai_interview.domain.cv.dto;

import com.ai_interview.domain.cv.entity.CVAnalysis;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CVAnalysisResponse {
    private Long id;
    private String fileName;
    private LocalDateTime createdAt;

    // @JsonRawValue ensures this is embedded as actual JSON object, not an escaped string
    @JsonRawValue
    private String analysis;

    public static CVAnalysisResponse from(CVAnalysis entity) {
        return CVAnalysisResponse.builder()
                .id(entity.getId())
                .fileName(entity.getFileName())
                .createdAt(entity.getCreatedAt())
                .analysis(entity.getAiResponseJson())
                .build();
    }
}