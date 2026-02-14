package com.ai_interview.domain.cv.service;

import com.ai_interview.common.exception.InterviewException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class PdfExtractorService {

    public String extractText(MultipartFile file) {
        if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
            throw InterviewException.badRequest("Invalid file type. Please upload a PDF.");
        }

        File tempFile = null;
        try {
            // 1. Save to Temp File (Stream, don't load into RAM)
            tempFile = File.createTempFile("upload-", ".pdf");
            Files.copy(file.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 2. Load from File (Efficient random access)
            try (PDDocument document = Loader.loadPDF(tempFile)) {
                if (document.isEncrypted()) {
                    throw InterviewException.badRequest("Encrypted PDFs are not supported.");
                }

                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                if (text == null || text.trim().isEmpty()) {
                    throw InterviewException.badRequest("No readable text found.");
                }
                return text;
            }

        } catch (IOException e) {
            log.error("PDF Parsing Error", e);
            throw InterviewException.internalError("Failed to process PDF.");
        } finally {
            // 3. Cleanup
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}