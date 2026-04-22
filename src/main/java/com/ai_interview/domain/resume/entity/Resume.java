package com.ai_interview.domain.resume.entity;

import com.ai_interview.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Resume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String resumeName; // e.g., "Software Engineering Resume"

    @Column(columnDefinition = "TEXT")
    private String contentJson; // Stores Work Exp, Education, Skills as a JSON string

    @CreationTimestamp
    private LocalDateTime createdAt;
}