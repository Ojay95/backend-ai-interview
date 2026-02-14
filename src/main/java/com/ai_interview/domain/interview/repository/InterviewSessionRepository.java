package com.ai_interview.domain.interview.repository;

import com.ai_interview.domain.interview.entity.InterviewSession;
import org.springframework.data.domain.Pageable; // Import this!
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    // 1. Efficient History Fetching (Already good, but verify usage)
    List<InterviewSession> findByUserIdOrderByStartedAtDesc(UUID userId);

    // 2. NEW: Calculate Average Score in DB
    @Query("SELECT AVG(s.overallScore) FROM InterviewSession s WHERE s.user.id = :userId")
    Double findAverageScoreByUserId(@Param("userId") UUID userId);

    // 3. NEW: Count Completed Sessions in DB
    @Query("SELECT COUNT(s) FROM InterviewSession s WHERE s.user.id = :userId")
    Integer countSessionsByUserId(@Param("userId") UUID userId);

    // 4. NEW: Get Recent Sessions for Graph (Limit via Pageable to avoid fetching all)
    @Query("SELECT s FROM InterviewSession s WHERE s.user.id = :userId ORDER BY s.startedAt DESC")
    List<InterviewSession> findRecentSessions(@Param("userId") UUID userId, Pageable pageable);

    // 5. NEW: Find Best/Worst Performing Roles
    @Query("SELECT s FROM InterviewSession s WHERE s.user.id = :userId AND s.overallScore IS NOT NULL ORDER BY s.overallScore DESC LIMIT 1")
    InterviewSession findBestSession(@Param("userId") UUID userId);

    @Query("SELECT s FROM InterviewSession s WHERE s.user.id = :userId AND s.overallScore IS NOT NULL ORDER BY s.overallScore ASC LIMIT 1")
    InterviewSession findWorstSession(@Param("userId") UUID userId);
}