package com.ai_interview.domain.job.service;

import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.domain.job.entity.Job;
import com.ai_interview.domain.job.repository.JobRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper; // Required for AI JSON parsing

    @Transactional(readOnly = true)
    public List<Job> getAllJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> InterviewException.notFound("Job with ID " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Job> getFeaturedJobs() {
        return jobRepository.findByIsFeaturedTrueOrderByCreatedAtDesc();
    }

    public String generateJobSummary(String fullDescription) {
        String prompt = "Summarize the following job description into 3 short bullet points focusing on " +
                "tech stack and primary responsibilities. Be concise:\n\n" + fullDescription;
        return chatModel.call(prompt);
    }

    /**
     * AI POWERED: Seeds the database with AI-generated jobs.
     * Fixed: Added JSON parsing and persistence logic.
     */
    // com.ai_interview.domain.job.service.JobService

    @Transactional
    public void seedJobsWithAI(String industry) {
        String prompt = String.format(
                "Generate a list of 5 diverse job openings for the %s industry. " +
                        "Return ONLY a JSON array. " +
                        "Fields: title, company, location, type, salaryRange, description, category.",
                industry
        );

        try {
            String response = chatModel.call(prompt);
            String jsonContent = response.replaceAll("(?s)```json\\s*|\\s*```", "").trim();

            List<Job> aiJobs = objectMapper.readValue(jsonContent, new TypeReference<List<Job>>() {});

            aiJobs.forEach(job -> {
                if (job.getCategory() == null) job.setCategory(industry);
            });

            jobRepository.saveAll(aiJobs);
            log.info("Successfully seeded {} AI jobs for category: {}", aiJobs.size(), industry);

        } catch (Exception e) {
            log.error("Failed to seed AI jobs: {}", e.getMessage());
            throw InterviewException.badRequest("AI Job generation failed.");
        }
    }
}