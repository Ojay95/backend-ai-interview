package com.ai_interview.domain.job.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    private String location;

    private String type; // e.g., Full-time, Remote, Contract

    private String salaryRange;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    private String logoUrl;

    // com.ai_interview.domain.job.entity.Job

    @Column(nullable = false)
    private String category; // e.g., "Healthcare", "Finance", "Technology"


    @CreationTimestamp
    private LocalDateTime createdAt;

    private boolean isFeatured = false;
}