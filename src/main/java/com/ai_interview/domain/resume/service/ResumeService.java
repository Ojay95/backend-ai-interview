package com.ai_interview.domain.resume.service;

import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.domain.auth.entity.User;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.domain.resume.dto.ResumeRequest;
import com.ai_interview.domain.resume.entity.Resume;
import com.ai_interview.domain.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;

    @Transactional
    public Resume saveResume(String email, ResumeRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> InterviewException.notFound("User not found"));

        Resume resume = Resume.builder()
                .user(user)
                .resumeName(request.getResumeName())
                .contentJson(request.getContentJson())
                .build();

        return resumeRepository.save(resume);
    }

    @Transactional(readOnly = true)
    public List<Resume> getUserResumes(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> InterviewException.notFound("User not found"));
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public void deleteResume(Long id, String email) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> InterviewException.notFound("Resume not found"));

        if (!resume.getUser().getEmail().equals(email)) {
            throw InterviewException.badRequest("Unauthorized action");
        }

        resumeRepository.delete(resume);
    }
}