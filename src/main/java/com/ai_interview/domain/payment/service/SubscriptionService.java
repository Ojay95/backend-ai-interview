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

    @Transactional(readOnly = true)
    public void validateUsaAAAgeLimit(User user, String feature) {
        PlanType plan = user.getPlan();

        // Feature limits
        int cvLimit = (plan == PlanType.FREE) ? 2 : (plan == PlanType.PRO) ? 20 : Integer.MAX_VALUE;
        int interviewLimit = (plan == PlanType.FREE) ? 1 : (plan == PlanType.PRO) ? 10 : Integer.MAX_VALUE;

        if ("CV_ANALYSIS".equals(feature)) {
            // Use the user's Long to count their associated CV records
            long count =  cvAnalysisRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size();
            if (count >= cvLimit) {
                throw InterviewException.badRequest("Monthly limit reached for " + plan);
            }
        } else if ("INTERVIEW".equals(feature)) {
            // Use the count query from sessionRepository
            Integer count = sessionRepository.countSessionsByUserId(user.getId());
            if (count != null && count >= interviewLimit) {
                throw InterviewException.badRequest("Monthly limit reached for " + plan + "plan");
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


    public void validateUsageLimit(User user, String cvAnalysis) {
    }
}