package com.ai_interview.domain.analytics.service;

import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.domain.analytics.dto.DashboardStatsResponse;
import com.ai_interview.domain.auth.entity.User;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.domain.interview.entity.InterviewSession;
import com.ai_interview.domain.interview.repository.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final InterviewSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> InterviewException.notFound("User not found"));
        UUID userId = user.getId();

        // 1. DB-Optimized Stats (O(1) memory usage)
        Integer totalCount = sessionRepository.countSessionsByUserId(userId);
        Double avgScore = sessionRepository.findAverageScoreByUserId(userId);

        if (totalCount == 0 || avgScore == null) {
            return buildEmptyStats();
        }

        // 2. Fetch only last 10 sessions for the graph (Not all 1000!)
        List<InterviewSession> recentSessions = sessionRepository.findRecentSessions(userId, PageRequest.of(0, 10));

        // Reverse them for the graph (Oldest -> Newest)
        List<DashboardStatsResponse.PerformancePoint> trend = recentSessions.stream()
                .sorted(Comparator.comparing(InterviewSession::getStartedAt))
                .map(s -> DashboardStatsResponse.PerformancePoint.builder()
                        .date(s.getStartedAt().format(DateTimeFormatter.ofPattern("MMM dd")))
                        .score(s.getOverallScore() != null ? s.getOverallScore() : 0.0)
                        .build())
                .collect(Collectors.toList());

        // 3. Get Strongest/Weakest Areas via DB
        String strongest = "General";
        String improvement = "Practice More";

        InterviewSession bestSession = sessionRepository.findBestSession(userId);
        if (bestSession != null) strongest = bestSession.getTargetRole();

        InterviewSession worstSession = sessionRepository.findWorstSession(userId);
        if (worstSession != null) improvement = worstSession.getTargetRole();

        return DashboardStatsResponse.builder()
                .totalPracticeCount(totalCount)
                .averageScore(Math.round(avgScore * 10.0) / 10.0)
                .strongestCategory(strongest)
                .improvementArea(improvement)
                .performanceTrend(trend)
                .currentStreak(calculateStreak(recentSessions)) // Logic remains same
                .weeklyGoalProgress(Math.min(totalCount, 5))
                .build();
    }

    private DashboardStatsResponse buildEmptyStats() {
        return DashboardStatsResponse.builder()
                .totalPracticeCount(0)
                .averageScore(0.0)
                .strongestCategory("N/A")
                .improvementArea("N/A")
                .performanceTrend(List.of())
                .currentStreak(0)
                .weeklyGoalProgress(0)
                .build();
    }

    private Integer calculateStreak(List<InterviewSession> sessions) {
        // Keeping simple streak logic for now
        return sessions.isEmpty() ? 0 : 1;
    }
}