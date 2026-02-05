package com.ai_interview.domain.payment.service;

import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.domain.auth.entity.PlanType;
import com.ai_interview.domain.auth.entity.User;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.domain.payment.entity.Subscription;
import com.ai_interview.domain.payment.entity.SubscriptionStatus;
import com.ai_interview.domain.payment.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Subscription getSubscription(String email) {
        User user = getUser(email);
        return subscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultFreeSubscription(user));
    }

    @Transactional
    public Subscription upgradePlan(String email, PlanType newPlan) {
        User user = getUser(email);
        Subscription subscription = subscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultFreeSubscription(user));

        // 1. In a real app, verify payment here (Stripe API call)

        // 2. Update Subscription
        subscription.setPlanType(newPlan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1)); // 1 Month validity

        // 3. Update User Entity (for quick access in other modules)
        user.setPlan(newPlan);
        userRepository.save(user);

        return subscriptionRepository.save(subscription);
    }

    private Subscription createDefaultFreeSubscription(User user) {
        Subscription sub = Subscription.builder()
                .user(user)
                .planType(PlanType.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusYears(100)) // Free forever
                .build();
        return subscriptionRepository.save(sub);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> InterviewException.notFound("User not found"));
    }
}