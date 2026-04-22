package com.ai_interview.domain.job.controller;

import com.ai_interview.domain.job.entity.Job;
import com.ai_interview.domain.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/featured")
    public ResponseEntity<List<Job>> getFeaturedJobs() {
        return ResponseEntity.ok(jobRepository.findByIsFeaturedTrueOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Job>> getJobsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(jobRepository.findByCategoryOrderByCreatedAtDesc(category));
    }
}