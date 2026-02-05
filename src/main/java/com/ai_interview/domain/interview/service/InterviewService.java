package com.ai_interview.domain.interview.service;

import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.domain.auth.entity.User;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.domain.interview.dto.InterviewHistoryDto;
import com.ai_interview.domain.interview.dto.InterviewRequest;
import com.ai_interview.domain.interview.dto.TranscriptDto;
import com.ai_interview.domain.interview.entity.InterviewSession;
import com.ai_interview.domain.interview.entity.InterviewStatus;
import com.ai_interview.domain.interview.entity.SenderType;
import com.ai_interview.domain.interview.entity.Transcript;
import com.ai_interview.domain.interview.repository.InterviewSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper; // For parsing JSON response

    @Transactional
    public String saveAndAnalyzeSession(String userEmail, InterviewRequest request) {
        // 1. Fetch User
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> InterviewException.notFound("User not found"));

        // 2. Create Session Entity
        InterviewSession session = InterviewSession.builder()
                .user(user)
                .targetRole(request.getTargetRole())
                .experienceLevel(request.getExperienceLevel())
                .language(request.getLanguage())
                .techStack(String.join(", ", request.getTechStack()))
                .focusAreas(String.join(", ", request.getFocusAreas()))
                .durationSeconds(request.getDurationSeconds())
                .status(InterviewStatus.COMPLETED)
                .build();

        // 3. Map Transcripts
        List<Transcript> transcriptEntities = request.getTranscript().stream()
                .map(dto -> Transcript.builder()
                        .session(session)
                        .sender(dto.getSender().equalsIgnoreCase("Sarah") ? SenderType.AI : SenderType.USER)
                        .content(dto.getText())
                        .timestamp(LocalDateTime.now()) // Approximation since we don't send raw timestamps
                        .build())
                .collect(Collectors.toList());

        session.setTranscripts(transcriptEntities);

        // 4. Run AI Analysis
        String analysisJson = generateAnalysis(request.getTranscript(), request.getTargetRole());

        // 5. Extract Score & Save
        try {
            JsonNode root = objectMapper.readTree(analysisJson);
            if (root.has("overallScore")) {
                session.setOverallScore(root.get("overallScore").asDouble());
            }
        } catch (Exception e) {
            log.warn("Could not parse score from AI response", e);
        }

        session.setFeedbackJson(analysisJson);
        sessionRepository.save(session);

        return analysisJson;
    }

    private String generateAnalysis(List<TranscriptDto> transcripts, String targetRole) {
        // Convert transcripts to string format for the prompt
        String chatHistory = transcripts.stream()
                .map(t -> t.getSender() + ": " + t.getText())
                .collect(Collectors.joining("\n"));

        // EXACT PROMPT from your AnalysisScreen.tsx
        String promptText = """
            Act as an expert interview coach. Analyze the PROVIDED transcript for a %s role.
            Analyze ONLY the questions and answers that appear in the transcript.
            
            Return a STRICT JSON object (no markdown). The format must match this structure exactly:
            {
              "overallScore": number (0-100),
              "performanceTag": "Excellent" | "Professional" | "Needs Improvement",
              "summary": "string",
              "keyStrengths": ["string", "string"],
              "growthAreas": ["string", "string"],
              "scoreBreakdown": [
                 {"label": "Technical Knowledge", "value": number},
                 {"label": "Cultural Fit", "value": number},
                 {"label": "Problem Solving", "value": number},
                 {"label": "Communication Skills", "value": number},
                 {"label": "Confidence & Clarity", "value": number}
              ],
              "visualMetrics": {
                 "eyeContactScore": number,
                 "postureScore": number,
                 "energyLevel": "High" | "Medium" | "Low",
                 "visualFeedback": "string"
              },
              "detailedAnalysis": [
                {
                  "question": "string",
                  "userTranscript": "string",
                  "answerStatus": "Strong Answer" | "Average Answer" | "Weak" | "Lacks Detail",
                  "statusColor": "green" | "amber" | "red",
                  "critique": "string",
                  "improvedAnswer": "string"
                }
              ]
            }
            
            TRANSCRIPT:
            ---
            %s
            ---
            """;

        String finalMessage = String.format(promptText, targetRole, chatHistory);

        return chatModel.call(new Prompt(new UserMessage(finalMessage)))
                .getResult()
                .getOutput()
                .getText()
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }


    @Transactional(readOnly = true)
    public List<InterviewHistoryDto> getUserInterviewHistory(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> InterviewException.notFound("User not found"));

        return sessionRepository.findByUserIdOrderByStartedAtDesc(user.getId())
                .stream()
                .map(InterviewHistoryDto::from)
                .collect(Collectors.toList());
    }
}