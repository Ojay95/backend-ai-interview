package com.ai_interview.domain.support.controller;

import com.ai_interview.domain.support.entity.SupportTicket;
import com.ai_interview.domain.support.repository.SupportTicketRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/support") // Note: 'public' path
@RequiredArgsConstructor
public class SupportController {

    private final SupportTicketRepository supportRepository;

    @PostMapping("/contact")
    public ResponseEntity<String> submitContactForm(@RequestBody ContactRequest request) {

        SupportTicket ticket = SupportTicket.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .message(request.getMessage())
                .build();

        supportRepository.save(ticket);

        return ResponseEntity.ok("Message received successfully");
    }

    @Data
    static class ContactRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String message;
    }
}