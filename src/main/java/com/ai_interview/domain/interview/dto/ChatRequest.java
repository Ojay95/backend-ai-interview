package com.ai_interview.domain.interview.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message; // The user's latest spoken/typed response
}
