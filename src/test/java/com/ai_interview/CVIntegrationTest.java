package com.ai_interview;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CVIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean
    private ChatModel chatModel; // Mock the AI to save money

    @BeforeEach
    void setup() {
        // Mock Gemini Response
        String mockJson = "{\"matchScore\": 85, \"verdict\": \"Good\"}";
        Generation generation = new Generation(new AssistantMessage(mockJson));
        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
                .thenReturn(new ChatResponse(List.of(generation)));
    }

    @Test
    @WithMockUser(username = "test@user.com") // Simulate Login
    void shouldAnalyzeUploadedPDF() throws Exception {
        // Create a fake PDF in memory
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "%PDF-1.4 content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/cv/analyze")
                        .file(pdfFile)
                        .param("jobDescription", "Java Dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("resume.pdf"));
        // Note: Actual DB saving is tested implicitly by the 200 OK
    }
}