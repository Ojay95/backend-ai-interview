package com.ai_interview.domain.interview.dto;

import lombok.Data;

@Data
public class TranscriptDto {
    private String sender; // "Sarah" or "You"
    private String text;
    private String time;
}