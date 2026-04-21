package com.ai_interview.domain.payment.service;

import com.ai_interview.common.exception.InterviewException;
import com.ai_interview.domain.auth.entity.PlanType;
import com.ai_interview.domain.auth.entity.User;
import com.ai_interview.domain.auth.repository.UserRepository;
import com.ai_interview.domain.cv.entity.CVAnalysis;
import com.ai_interview.domain.cv.repository.CVAnalysisRepository;
import com.ai_interview.domain.interview.repository.InterviewSessionRepository;
import com.ai_interview.domain.interview.service.InterviewService;
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
    private final CVAnalysisRepository cvAnalysisRepository;
    private final InterviewSessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public Subscription getSubscription(String email) {
        User user = getUser(email);
        return subscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultFreeSubscription(user));
    }

    // com.ai_interview.domain.payment.service.SubscriptionService

    @Transactional(readOnly = true)
    public void validateUsageLimit(User user, String feature) {
        // 1. Fetch the user's subscription to get the billing cycle start date
        Subscription sub = subscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultFreeSubscription(user));

        java.time.LocalDateTime cycleStart = sub.getCurrentPeriodStart();
        PlanType plan = user.getPlan();

        // 2. Define Limits (including the Fair Use cap for ELITE)
        int cvLimit = switch (plan) {
            case FREE -> 2;
            case PRO -> 20;
            case ELITE -> 500; // Hidden Fair Use Cap
        };

        int interviewLimit = switch (plan) {
            case FREE -> 1;
            case PRO -> 10;
            case ELITE -> 500; // Hidden Fair Use Cap
        };

        // 3. Perform the count based on the current billing cycle
        if ("CV_ANALYSIS".equals(feature)) {
            long count = cvAnalysisRepository.countCvAnalysesSince(user.getId(), cycleStart);
            if (count >= cvLimit) {
                throw InterviewException.badRequest("Monthly CV analysis limit reached for your " + plan + " plan.");
            }
        } else if ("INTERVIEW".equals(feature)) {
            Integer count = sessionRepository.countSessionsSince(user.getId(), cycleStart);
            if (count != null && count >= interviewLimit) {
                throw InterviewException.badRequest("Monthly interview limit reached for your " + plan + " plan.");
            }
        }
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
        return null;
    }


}