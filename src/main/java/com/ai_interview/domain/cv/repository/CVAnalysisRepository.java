package com.ai_interview.domain.cv.repository;

import com.ai_interview.domain.cv.entity.CVAnalysis;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CVAnalysisRepository extends JpaRepository<CVAnalysis, Long> {

    // Find all CVs uploaded by a specific user, newest first
    List<CVAnalysis> findByUserIdOrderByCreatedAtDesc(UUID userId);


    @Query("SELECT COUNT(c) FROM CVAnalysis c WHERE c.user.id = :userId AND c.createdAt >= :startDate")
    long countCvAnalysesSince(@Param("userId") java.util.UUID userId, @Param("startDate") java.time.LocalDateTime startDate);

}