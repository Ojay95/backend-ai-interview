package com.ai_interview.domain.job.repository;

import com.ai_interview.domain.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByIsFeaturedTrueOrderByCreatedAtDesc();

    List<Job> findAllByOrderByCreatedAtDesc();
}