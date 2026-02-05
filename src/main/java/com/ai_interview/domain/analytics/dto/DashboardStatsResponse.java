package com.ai_interview.domain.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardStatsResponse {
    // Top Row Cards
    private Integer totalPracticeCount;
    private Double averageScore;
    private String strongestCategory; // e.g. "System Design"
    private String improvementArea;   // e.g. "Behavioral"

    // Graph Data
    private List<PerformancePoint> performanceTrend;

    // User Context
    private Integer currentStreak; // Days in a row
    private Integer weeklyGoalProgress; // e.g., 2/5 completed

    @Data
    @Builder
    public static class PerformancePoint {
        private String date;  // "Oct 24"
        private Double score; // 85.5
    }
}