package com.ai_interview.domain.payment.controller;

import com.ai_interview.domain.auth.entity.PlanType;
import com.ai_interview.domain.payment.entity.Subscription;
import com.ai_interview.domain.payment.service.SubscriptionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<SubscriptionResponse> getMySubscription(Authentication authentication) {
        Subscription sub = subscriptionService.getSubscription(authentication.getName());
        return ResponseEntity.ok(SubscriptionResponse.from(sub));
    }

    @PostMapping("/upgrade")
    public ResponseEntity<SubscriptionResponse> upgradePlan(
            @RequestBody UpgradeRequest request,
            Authentication authentication
    ) {
        Subscription sub = subscriptionService.upgradePlan(authentication.getName(), request.getPlanType());
        return ResponseEntity.ok(SubscriptionResponse.from(sub));
    }

    @Data
    static class UpgradeRequest {
        private PlanType planType;
    }

    @Data
    @lombok.Builder
    static class SubscriptionResponse {
        private PlanType plan;
        private String status;
        private String renewalDate;

        public static SubscriptionResponse from(Subscription s) {
            return SubscriptionResponse.builder()
                    .plan(s.getPlanType())
                    .status(s.getStatus().name())
                    .renewalDate(s.getCurrentPeriodEnd().toString())
                    .build();
        }
    }
}