package com.ai_interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ai_interview.domain.auth.dto.LoginRequest;
import com.ai_interview.domain.auth.dto.RegisterRequest;
import com.ai_interview.domain.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        // 1. Explicitly Disable Redis
        "spring.data.redis.repositories.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",

        // 2. Mock Security Keys
        "app.jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437",
        "app.jwt.expiration-ms=3600000",

        // 3. Mock Email & AI
        "spring.mail.host=localhost",
        "spring.ai.google.genai.api-key=test-key"
})
@AutoConfigureMockMvc
// ⬇️ THIS IS THE FIX: It forces an H2 In-Memory DB, ignoring your application.properties URL
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void tearDown() {
        userRepository.deleteAll(); // Clean DB before each test
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice", "Smith", "alice@test.com", "securePass123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("alice@test.com"));
    }

    @Test
    void shouldFailRegistration_WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("Bob", "Jones", "bob@test.com", "pass1234");

        // 1. First Registration
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2. Second Registration (Duplicate)
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        // 1. Register
        RegisterRequest register = new RegisterRequest("Charlie", "Day", "charlie@test.com", "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));

        // 2. Login
        LoginRequest login = new LoginRequest("charlie@test.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }
}