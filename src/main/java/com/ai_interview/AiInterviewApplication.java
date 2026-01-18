package com.ai_interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AiInterviewApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiInterviewApplication.class, args);
		System.out.println("MockInterview Backend is running on http://localhost:8080");
	}
}
