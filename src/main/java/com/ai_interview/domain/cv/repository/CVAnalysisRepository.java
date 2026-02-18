package com.ai_interview.domain.cv.repository;

import com.ai_interview.domain.cv.entity.CVAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CVAnalysisRepository extends JpaRepository<CVAnalysis, UUID> {

    // Find all CVs uploaded by a specific user, newest first
    List<CVAnalysis> findByUserIdOrderByCreatedAtDesc(UUID user_id);

}