package com.ai_interview.domain.cv.service;

import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.domain.auth.entity.User;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.domain.cv.entity.CVAnalysis;
import com.ai_interview.domain.cv.repository.CVAnalysisRepository;
import com.ai_interview.domain.job.entity.Job;
import com.ai_interview.domain.job.service.JobService;
import com.ai_interview.domain.payment.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final JobService jobService; // Added for Job Board integration

    /**
     * AI POWERED: Analyzes a CV against a specific Job from the Job Board.
     */
    @Transactional
    public CVAnalysis analyzeAgainstJob(Long jobId, MultipartFile file, String userEmail) {
        Job job = jobService.getJobById(jobId);
        return analyzeCV(file, job.getDescription(), userEmail);
    }

    /**
     * Analyzes a CV against a provided Job Description string.
     */
    @Transactional
    public CVAnalysis analyzeCV(MultipartFile file, String jobDescription, String userEmail) {
        // 1. Validate User and Subscription Limits
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> InterviewException.notFound("User not found"));

        subscriptionService.validateUsageLimit(user, "CV_ANALYSIS");

        // 2. Extract Text from PDF
        String cvText = pdfExtractorService.extractText(file);
        if (cvText.length() > 40000) {
            log.warn("CV for user {} was truncated due to length", userEmail);
            cvText = cvText.substring(0, 40000);
        }

        // 3. Generate AI Match Analysis
        String aiResponseJson = generateAIAnalysis(cvText, jobDescription);

        // 4. Persist Analysis Result
        CVAnalysis analysis = CVAnalysis.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .extractedText(cvText)
                .jobDescription(jobDescription)
                .aiResponseJson(aiResponseJson)
                .build();

        return cvAnalysisRepository.save(analysis);
    }

    private String generateAIAnalysis(String cvText, String jobDescription) {
        String promptText = """
            Act as an expert technical recruiter. Analyze the following Resume against the Job Description.
            Return a STRICT JSON object. Do not include markdown code blocks (like ```json).
            
            Structure:
            {
              "matchScore": number (0-100),
              "matchedKeywords": ["string"],
              "missingKeywords": ["string"],
              "overallFeedback": "string",
              "verdict": "string",
              "shouldApply": "string",
              "practiceAreas": ["string"],
              "recommendations": [
                {
                  "title": "string",
                  "impact": "High" | "Medium" | "Low",
                  "description": "string",
                  "suggestion": "string"
                }
              ]
            }

            RESUME:
            ---
            %s
            ---
            JOB DESCRIPTION:
            ---
            %s
            ---
            """;

        String finalMessage = String.format(promptText, cvText, jobDescription);

        try {
            String response = chatModel.call(new Prompt(new UserMessage(finalMessage)))
                    .getResult()
                    .getOutput()
                    .getText();

            // Clean markdown and any leading/trailing whitespace
            return response.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
        } catch (Exception e) {
            log.error("Gemini AI Analysis failed", e);
            throw InterviewException.internalError("The AI matching service is temporarily unavailable.");
        }
    }
}