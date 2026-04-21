package com.ai_interview.domain.interview.service;

import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.domain.auth.entity.User;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.domain.interview.dto.InterviewHistoryDto;
import com.ai_interview.domain.interview.dto.InterviewRequest;
import com.ai_interview.domain.interview.dto.TranscriptDto;
import com.ai_interview.domain.interview.entity.*;
import com.ai_interview.domain.interview.repository.InterviewSessionRepository;
import com.ai_interview.domain.payment.service.SubscriptionService;
import com.ai_interview.domain.user.entity.UserPreference;
import com.ai_interview.domain.user.repository.UserPreferenceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final SubscriptionService subscriptionService;
    /**
     * Handles a live chat turn during the interview.
     * Manages persona, language enforcement, and time-aware wrap-up logic.
     */
    @Transactional
    public String handleChatTurn(Long sessionId, String userEmail, String userMessage) {
        // 1. Validate Session
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> InterviewException.notFound("Session not found"));

        if (!session.getUser().getEmail().equals(userEmail)) {
            throw InterviewException.badRequest("Unauthorized access to this session");
        }

        // 2. Calculate remaining time
        long secondsElapsed = Duration.between(session.getStartedAt(), LocalDateTime.now()).getSeconds();
        long secondsLeft = session.getDurationSeconds() - secondsElapsed;

        // 3. Fetch Persona Preferences
        UserPreference prefs = preferenceRepository.findByUserId(session.getUser().getId())
                .orElse(new UserPreference()); // Default: Zephyr, 1.0 speed, recruiter persona

        // 4. Determine AI Instruction based on time left
        String timingInstruction = "Ask a concise, challenging interview question relevant to the role.";
        if (secondsLeft < 40) {
            timingInstruction = "The time is almost up. DO NOT ask a new question. " +
                    "Instead, thank the user for their time and ask them for any final thoughts to wrap up.";
        } else if (secondsLeft < 90) {
            timingInstruction = "The interview is nearing its end. Ask one final question and mention that time is short.";
        }

        // 5. Build Strict System Message
        String systemInstruction = String.format(
                "You are Sarah, an expert %s. You are conducting a %s interview for a %s role. " +
                        "STRICT RULES:\n" +
                        "1. You must speak ONLY in %s.\n" +
                        "2. Do not interrupt the user.\n" +
                        "3. Current Task: %s",
                prefs.getInterviewerPersona(), session.getExperienceLevel(),
                session.getTargetRole(), session.getLanguage(), timingInstruction
        );

        // 6. Persist User message and gather history
        addTranscriptEntry(session, SenderType.USER, userMessage);
        String history = session.getTranscripts().stream()
                .map(t -> t.getSender() + ": " + t.getContent())
                .collect(Collectors.joining("\n"));

        // 7. Call AI
        SystemMessage systemMsg = new SystemMessage(systemInstruction);
        UserMessage userMsg = new UserMessage("Current conversation history:\n" + history);

        String aiResponse = chatModel.call(new Prompt(List.of(systemMsg, userMsg)))
                .getResult().getOutput().getText();

        // 8. Persist AI response and save session
        addTranscriptEntry(session, SenderType.AI, aiResponse);
        sessionRepository.save(session);

        return aiResponse;
    }

    /**
     * Finalizes the session and performs the full AI analysis.
     */
    @Transactional
    public String saveAndAnalyzeSession(String userEmail, InterviewRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> InterviewException.notFound("User not found"));
        subscriptionService.validateUsageLimit(user, "INTERVIEW");

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

        List<Transcript> transcriptEntities = request.getTranscript().stream()
                .map(dto -> Transcript.builder()
                        .session(session)
                        .sender(dto.getSender().equalsIgnoreCase("Sarah") ? SenderType.AI : SenderType.USER)
                        .content(dto.getText())
                        .timestamp(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        session.setTranscripts(transcriptEntities);

        String analysisJson = generateAnalysis(request.getTranscript(), request.getTargetRole());

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
        String chatHistory = transcripts.stream()
                .map(t -> t.getSender() + ": " + t.getText())
                .collect(Collectors.joining("\n"));

        String promptText = """
            Act as an expert interview coach. Analyze the PROVIDED transcript for a %s role.
            Return a STRICT JSON object (no markdown). 
            (Insert Full JSON Schema from prototype here...)
            
            TRANSCRIPT:
            %s
            """;

        String finalMessage = String.format(promptText, targetRole, chatHistory);

        String rawResponse = chatModel.call(new Prompt(new UserMessage(finalMessage)))
                .getResult().getOutput().getText();

        return rawResponse.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
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

    private void addTranscriptEntry(InterviewSession session, SenderType sender, String content) {
        Transcript transcript = Transcript.builder()
                .session(session)
                .sender(sender)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
        session.getTranscripts().add(transcript);
    }
}