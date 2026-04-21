package com.ai_interview.domain.interview.controller;

import com.ai_interview.domain.interview.dto.ChatRequest;
import com.ai_interview.domain.interview.dto.InterviewHistoryDto;
import com.ai_interview.domain.interview.dto.InterviewRequest;
import com.ai_interview.domain.interview.service.InterviewService;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisWrapper> submitInterview(
            @RequestBody InterviewRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        String jsonResult = interviewService.saveAndAnalyzeSession(email, request);

        return ResponseEntity.ok(new AnalysisWrapper(jsonResult));
    }

    // Wrapper to ensure the string is treated as raw JSON, not escaped string
    @Data
    static class AnalysisWrapper {
        @JsonRawValue
        private String analysis;

        public AnalysisWrapper(String analysis) {
            this.analysis = analysis;
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<InterviewHistoryDto>> getHistory(Authentication authentication) {
        String email = authentication.getName();
        List<InterviewHistoryDto> history = interviewService.getUserInterviewHistory(email);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{sessionId}/chat")
    public ResponseEntity<Map<String, String>> chat(
            @PathVariable Long sessionId,
            @RequestBody ChatRequest request,
            Authentication authentication
    ) {
        String aiMessage = interviewService.handleChatTurn(
                sessionId,
                authentication.getName(),
                request.getMessage()
        );
        return ResponseEntity.ok(Map.of("aiMessage", aiMessage));
    }
}