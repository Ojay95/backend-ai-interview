package com.ai_interview.domain.cv.service;

import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.domain.auth.entity.User;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.domain.cv.entity.CVAnalysis;
import com.ai_interview.domain.cv.repository.CVAnalysisRepository;
import com.ai_interview.domain.interview.service.InterviewService;
import com.ai_interview.domain.payment.repository.SubscriptionRepository;
import com.ai_interview.domain.payment.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class CVService {

    private final PdfExtractorService pdfExtractorService;
    private final ChatModel chatModel;
    private final CVAnalysisRepository cvAnalysisRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    public CVAnalysis analyzeCV(MultipartFile file, String jobDescription, String userEmail) {
        // 1. Get User
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> InterviewException.notFound("User not found"));

        subscriptionService.validateUsageLimit(user, "CV_ANALYSIS");

        // 2. Extract Text (Server-side extraction)
        String cvText = pdfExtractorService.extractText(file);

        // 3. Generate Analysis with the EXACT structure your frontend expects
        String aiResponseJson = generateAIAnalysis(cvText, jobDescription);

        // 4. Save to Database
        CVAnalysis analysis = CVAnalysis.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .extractedText(cvText)
                .jobDescription(jobDescription) // Added JD to entity
                .aiResponseJson(aiResponseJson)
                .build();

        return cvAnalysisRepository.save(analysis);
    }

    private String generateAIAnalysis(String cvText, String jobDescription) {
        if (cvText.length() > 50000) cvText = cvText.substring(0, 50000);

        // This prompt matches your 'ResumeMatchResult' interface in Typescript
        String promptText = """
            Act as an expert technical recruiter. Analyze the following Resume against the Job Description.
            Return a STRICT JSON object. Do not use Markdown formatting.
            
            The JSON structure must be EXACTLY:
            {
              "matchScore": Integer (0-100),
              "matchedKeywords": ["String", "String"],
              "missingKeywords": ["String", "String"],
              "overallFeedback": "String (Concise summary)",
              "verdict": "String (e.g. 'Highly Qualified', 'Strong Potential', 'Significant Gaps')",
              "shouldApply": "String (Recommendation)",
              "practiceAreas": ["String", "String"],
              "recommendations": [
                {
                  "title": "String",
                  "impact": "High" | "Medium" | "Low",
                  "description": "String",
                  "suggestion": "String"
                }
              ]
            }

            RESUME:
            %s

            JOB DESCRIPTION:
            %s
            """;

        String finalMessage = String.format(promptText, cvText, jobDescription);

        try {
            return chatModel.call(new Prompt(new UserMessage(finalMessage)))
                    .getResult()
                    .getOutput()
                    .getText()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();
        } catch (Exception e) {
            log.error("Gemini AI Call failed", e);
            throw InterviewException.internalError("AI Service is currently unavailable.");
        }
    }
}