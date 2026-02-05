package com.ai_interview.domain.support.repository;

import com.ai_interview.domain.support.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    // You can add findByEmail or findByStatus later if needed
}