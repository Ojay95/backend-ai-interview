package com.ai_interview.domain.cv.entity;

import com.ai_interview.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "cv_analysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class) // Automatically sets the date
public class CVAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link the analysis to a specific user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String fileName;

    // The raw text pulled from the PDF (Stored for debugging or re-processing)
    // Using columnDefinition="TEXT" allows storing large amounts of text
    @Column(columnDefinition = "TEXT", nullable = false)
    private String extractedText;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    // The detailed JSON response from Gemini
    @Column(columnDefinition = "TEXT", nullable = false)
    private String aiResponseJson;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}