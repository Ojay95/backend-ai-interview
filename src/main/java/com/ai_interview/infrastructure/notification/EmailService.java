package com.ai_interview.infrastructure.notification;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetToken);
}
