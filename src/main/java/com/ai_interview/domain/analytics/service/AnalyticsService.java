package com.ai_interview.domain.analytics.service;

import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.domain.analytics.dto.DashboardStatsResponse;
import com.ai_interview.domain.auth.entity.User;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.domain.interview.entity.InterviewSession;
import com.ai_interview.domain.interview.repository.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

        List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByStartedAtDesc(user.getId());

        // 1. Basic Stats
        int totalCount = sessions.size();
        double avgScore = sessions.stream()
                .mapToDouble(s -> s.getOverallScore() != null ? s.getOverallScore() : 0.0)
                .average()
                .orElse(0.0);

        // 2. Performance Trend (Last 6 sessions for the graph)
        List<DashboardStatsResponse.PerformancePoint> trend = sessions.stream()
                .sorted(Comparator.comparing(InterviewSession::getStartedAt)) // Sort oldest to newest for graph
                .limit(10) // Limit to last 10 points
                .map(s -> DashboardStatsResponse.PerformancePoint.builder()
                        .date(s.getStartedAt().format(DateTimeFormatter.ofPattern("MMM dd")))
                        .score(s.getOverallScore() != null ? s.getOverallScore() : 0.0)
                        .build())
                .collect(Collectors.toList());

        // 3. Determine Strongest/Weakest Areas (Simple logic based on session types)
        // In a real app, you'd aggregate scores by 'targetRole' or 'focusAreas'
        String strongest = "General";
        String improvement = "Practice More";

        if (!sessions.isEmpty()) {
            // Find session with highest score
            InterviewSession bestSession = sessions.stream()
                    .max(Comparator.comparing(s -> s.getOverallScore() != null ? s.getOverallScore() : 0.0))
                    .orElse(null);
            if (bestSession != null) strongest = bestSession.getTargetRole();

            // Find session with lowest score
            InterviewSession worstSession = sessions.stream()
                    .min(Comparator.comparing(s -> s.getOverallScore() != null ? s.getOverallScore() : 0.0))
                    .orElse(null);
            if (worstSession != null) improvement = worstSession.getTargetRole();
        }

        return DashboardStatsResponse.builder()
                .totalPracticeCount(totalCount)
                .averageScore(Math.round(avgScore * 10.0) / 10.0) // Round to 1 decimal
                .strongestCategory(strongest)
                .improvementArea(improvement)
                .performanceTrend(trend)
                .currentStreak(calculateStreak(sessions)) // Placeholder for logic
                .weeklyGoalProgress(Math.min(totalCount, 5)) // Mock weekly goal
                .build();
    }

    private Integer calculateStreak(List<InterviewSession> sessions) {
        // Simple mock streak logic - count consecutive days with sessions
        return sessions.isEmpty() ? 0 : 3; // Mocking "3 days" to encourage user
    }
}