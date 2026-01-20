package com.ai_interview.infrastructure.notification;

import com.ai_interview.common.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JavaMailEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Reset Your MockInterview Password");
            message.setText("To reset your password, please use the following token:\n\n"
                    + resetToken + "\n\n"
                    + "This token expires in 15 minutes.");

            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email to {}", toEmail, e);
            // Now we throw our custom exception, which GlobalExceptionHandler will catch
            throw new InterviewException(
                    "Failed to send email. Please try again later.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
