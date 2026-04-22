package com.ai_interview.domain.cv.controller;

import com.ai_interview.domain.cv.dto.CVAnalysisResponse;
import com.ai_interview.domain.cv.entity.CVAnalysis;
import com.ai_interview.domain.cv.repository.CVAnalysisRepository;
import com.ai_interview.domain.cv.service.CVService;
import com.ai_interview.domain.auth.entity.User;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.domain.job.entity.Job;
import com.ai_interview.domain.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/cv")
@RequiredArgsConstructor
public class CVController {

    private final CVService cvService;
    private final CVAnalysisRepository cvAnalysisRepository;
    private final UserRepository userRepository;
    private final JobService jobService;

    /**
     * Upload a PDF and get instant AI analysis
     */
    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    public ResponseEntity<CVAnalysisResponse> analyzeCV(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobDescription") String jobDescription, // ✅ Added JD
            Authentication authentication
    ) {
        String email = authentication.getName();

        // Pass JD to service
        CVAnalysis savedAnalysis = cvService.analyzeCV(file, jobDescription, email);

        return ResponseEntity.ok(CVAnalysisResponse.from(savedAnalysis));
    }

    /**
     * Get list of all past analyses for the current user
     */
    @GetMapping("/history")
    public ResponseEntity<List<CVAnalysisResponse>> getHistory(Authentication authentication) {
        String email = authentication.getName();

        // Find User ID
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> InterviewException.notFound("User not found"));

        // Fetch History
        List<CVAnalysis> history = cvAnalysisRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        // Convert to DTOs
        List<CVAnalysisResponse> response = history.stream()
                .map(CVAnalysisResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // Add this to com.ai_interview.domain.cv.controller.CVController

    @PostMapping("/match-job/{jobId}")
    public ResponseEntity<CVAnalysisResponse> matchCvToJob(
            @PathVariable Long jobId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        // 1. Get the Job from the new Job Board module
        Job job = jobService.getJobById(jobId);

        // 2. Pass the Job's AI-ready description to the existing CV Analysis service
        CVAnalysisResponse result = CVAnalysisResponse.from(cvService.analyzeCV(
                file,
                job.getDescription(),
                authentication.getName()
        ));

        return ResponseEntity.ok(result);
    }
}