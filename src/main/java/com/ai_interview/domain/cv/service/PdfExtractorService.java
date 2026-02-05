package com.ai_interview.domain.cv.service;

import com.ai_interview.common.exception.InterviewException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.Loader;

import java.io.IOException;

@Slf4j
@Service
public class PdfExtractorService {

    public String extractText(MultipartFile file) {
        // 1. Basic Validation
        if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
            throw InterviewException.badRequest("Invalid file type. Please upload a PDF.");
        }

        // 2. Extract Text using Apache PDFBox
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.isEncrypted()) {
                throw InterviewException.badRequest("Encrypted (Password Protected) PDFs are not supported.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                throw InterviewException.badRequest("The PDF contains no readable text. It might be an image scan.");
            }

            return text;

        } catch (IOException e) {
            log.error("Error parsing PDF file: {}", file.getOriginalFilename(), e);
            throw InterviewException.internalError("Failed to process PDF file.");
        }
    }
}