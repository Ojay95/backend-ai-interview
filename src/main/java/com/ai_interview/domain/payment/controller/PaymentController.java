package com.ai_interview.domain.payment.controller;

import com.ai_interview.domain.auth.entity.PlanType;
import com.ai_interview.domain.payment.service.SubscriptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    @Value("${paystack.secret.key}")
    private String paystackSecretKey;

    @PostMapping("/webhook")
    public ResponseEntity<String> handlePaystackWebhook(
            @RequestBody String payload,
            @RequestHeader("x-paystack-signature") String signature) {

        // 1. Verify Webhook Signature
        String expectedSignature = new HmacUtils(HmacAlgorithms.HMAC_SHA_512, paystackSecretKey)
                .hmacHex(payload);

        if (!expectedSignature.equals(signature)) {
            log.warn("Invalid Paystack signature received");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.get("event").asText();

            // 2. Handle successful charge
            if ("charge.success".equals(event)) {
                JsonNode data = root.get("data");
                String email = data.get("customer").get("email").asText();

                // Extract plan from metadata sent from frontend
                String planName = data.get("metadata").get("plan_name").asText();
                PlanType newPlan = PlanType.valueOf(planName.toUpperCase());

                log.info("Payment successful for user: {}. Upgrading to {}", email, newPlan);
                subscriptionService.upgradePlan(email, newPlan);
            }

            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            log.error("Error processing Paystack webhook", e);
            return ResponseEntity.status(500).body("Internal error");
        }
    }
}