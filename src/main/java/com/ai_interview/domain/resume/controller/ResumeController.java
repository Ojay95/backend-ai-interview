package com.ai_interview.domain.resume.controller;

import com.ai_interview.domain.resume.dto.ResumeRequest;
import com.ai_interview.domain.resume.entity.Resume;
import com.ai_interview.domain.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping
    public ResponseEntity<Resume> createResume(@RequestBody ResumeRequest request, Authentication auth) {
        return ResponseEntity.ok(resumeService.saveResume(auth.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<Resume>> getMyResumes(Authentication auth) {
        return ResponseEntity.ok(resumeService.getUserResumes(auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(@PathVariable Long id, Authentication auth) {
        resumeService.deleteResume(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}