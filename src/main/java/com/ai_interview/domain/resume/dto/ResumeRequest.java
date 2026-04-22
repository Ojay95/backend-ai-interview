package com.ai_interview.domain.resume.dto;

import lombok.Data;

@Data
public class ResumeRequest {
    private String resumeName;
    private String contentJson; // This matches the structured JSON from your CVEditor
}