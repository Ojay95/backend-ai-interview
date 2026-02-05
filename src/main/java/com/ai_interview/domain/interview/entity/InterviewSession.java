package com.ai_interview.domain.interview.entity;

import com.ai_interview.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "interview_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // --- Config Fields (Matches frontend InterviewConfig) ---
    @Column(nullable = false)
    private String targetRole; // e.g. "Senior Frontend Dev"

    @Column(nullable = false)
    private String experienceLevel; // e.g. "Senior", "Mid"

    @Column(nullable = false)
    private String language; // e.g. "English", "Spanish"

    // Storing as simple CSV strings for simplicity, or use @ElementCollection for separate table
    private String techStack;
    private String focusAreas;

    // --- Session Stats ---
    private Integer durationSeconds;
    private Double overallScore; // 0-100

    @Column(columnDefinition = "TEXT")
    private String feedbackJson; // Stores the full JSON analysis for the UI

    @Enumerated(EnumType.STRING)
    private InterviewStatus status;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transcript> transcripts;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;
}