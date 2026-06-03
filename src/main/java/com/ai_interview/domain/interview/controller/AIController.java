package com.ai_interview.domain.interview.controller;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AIController {

    private final ChatModel chatModel;

    @Value("${spring.ai.google.genai.api-key}")
    private String geminiApiKey;

    // 1. Get Gemini Config (For Live Audio WebSockets on Frontend)
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        return ResponseEntity.ok(Map.of("apiKey", geminiApiKey));
    }

    // 2. Analyze Job Description
    @PostMapping("/analyze-jd")
    public ResponseEntity<AnalysisWrapper> analyzeJd(@RequestBody Map<String, String> request) {
        String jd = request.get("jd");
        String prompt = String.format("""
            Analyze the following Job Description and return a JSON object with: 
            roleName (string), keySkills (string array), recommendedFocusAreas (string array), experienceLevel (string).
            Return ONLY a valid JSON structure. Do not wrap in markdown syntax (such as ```json).
            
            JD: "%s"
            """, jd);

        String result = chatModel.call(new Prompt(new UserMessage(prompt)))
                .getResult().getOutput().getText();

        // Strip potential markdown wrappers just in case
        String cleanResult = result.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
        return ResponseEntity.ok(new AnalysisWrapper(cleanResult));
    }

    // 3. Extract Role from Resume
    @PostMapping("/extract-role")
    public ResponseEntity<Map<String, String>> extractRole(@RequestBody Map<String, String> request) {
        String resumeText = request.get("resumeText");
        String prompt = String.format("""
            Based on the following resume text, identify the most appropriate and common professional job title for this person.
            Return ONLY the job title as a string. No other text.
            
            RESUME:
            "%s"
            """, resumeText);

        String result = chatModel.call(new Prompt(new UserMessage(prompt)))
                .getResult().getOutput().getText().trim();

        return ResponseEntity.ok(Map.of("role", result));
    }

    // 4. Onboarding Chat
    @PostMapping("/onboarding/chat")
    public ResponseEntity<Map<String, String>> onboardingChat(@RequestBody Map<String, Object> request) {
        List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");
        String userName = (String) request.get("userName");
        String userPlan = (String) request.get("userPlan");

        String chatHistory = messages.stream()
                .map(m -> (m.get("sender").equals("ai") ? "Sarah" : "You") + ": " + m.get("text"))
                .collect(Collectors.joining("\n"));

        String systemInstruction = String.format("""
            You are Sarah, a helpful AI Interview Coach. Help %s set up their mock interview.
            Your goal is to COLLECT exactly these five details:
            1. Target Role
            2. Experience Level
            3. Skills/Focus Areas
            4. Interview Duration (Basic: 10m, Pro: 45m, Elite: 60m max based on plan)
            5. Language (default to English unless user switches language)
            
            PLAN LIMIT: The user has a %s plan. Respect the duration limits.
            
            IMPORTANT RULES:
            - Respond in the language the user is using (e.g., if they speak French, respond in French).
            - Keep your questions friendly and concise.
            - Once all 5 details have been collected, end with "Ready to start?" to prompt the user to start the interview.
            """, userName != null ? userName : "User", userPlan != null ? userPlan : "free");

        SystemMessage systemMsg = new SystemMessage(systemInstruction);
        UserMessage userMsg = new UserMessage("Current onboarding conversation history:\n" + chatHistory);

        String aiResponse = chatModel.call(new Prompt(List.of(systemMsg, userMsg)))
                .getResult().getOutput().getText().trim();

        return ResponseEntity.ok(Map.of("text", aiResponse));
    }

    // 5. Onboarding Finalize
    @PostMapping("/onboarding/finalize")
    public ResponseEntity<AnalysisWrapper> onboardingFinalize(@RequestBody Map<String, Object> request) {
        List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");

        String chatHistory = messages.stream()
                .map(m -> (m.get("sender").equals("ai") ? "Sarah" : "You") + ": " + m.get("text"))
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
            Based on this interview setup conversation, extract the interview configuration in JSON format.
            Return ONLY a valid JSON object. Do not include markdown code blocks.
            
            JSON Schema:
            {
              "role": "string",
              "experienceLevel": "string",
              "techStack": ["string"],
              "focusAreas": ["string"],
              "duration": number,
              "language": "string"
            }
            
            Conversation:
            %s
            """, chatHistory);

        String result = chatModel.call(new Prompt(new UserMessage(prompt)))
                .getResult().getOutput().getText();

        String cleanResult = result.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
        return ResponseEntity.ok(new AnalysisWrapper(cleanResult));
    }

    @Data
    static class AnalysisWrapper {
        @JsonRawValue
        private String analysis;

        public AnalysisWrapper(String analysis) {
            this.analysis = analysis;
        }
    }
}
