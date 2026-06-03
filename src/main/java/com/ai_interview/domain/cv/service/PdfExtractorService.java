package com.ai_interview.domain.cv.service;

import com.ai_interview.common.exception.InterviewException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class PdfExtractorService {

    public String extractText(MultipartFile file) {
        if (file.isEmpty()) {
            throw InterviewException.badRequest("Uploaded file is empty.");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "";
        }

        if ((contentType != null && contentType.equals("application/pdf")) || originalFilename.toLowerCase().endsWith(".pdf")) {
            return extractPdfText(file);
        } else if ((contentType != null && (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || 
                   contentType.equals("application/msword"))) || 
                   originalFilename.toLowerCase().endsWith(".docx") || 
                   originalFilename.toLowerCase().endsWith(".doc")) {
            return extractDocxText(file);
        } else if ((contentType != null && contentType.startsWith("text/")) || originalFilename.toLowerCase().endsWith(".txt")) {
            return extractPlainText(file);
        } else {
            throw InterviewException.badRequest("Unsupported file type. Please upload a PDF, Word (.docx), or Text (.txt) file.");
        }
    }

    private String extractPdfText(MultipartFile file) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("upload-", ".pdf");
            Files.copy(file.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            try (PDDocument document = Loader.loadPDF(tempFile)) {
                if (document.isEncrypted()) {
                    throw InterviewException.badRequest("Encrypted PDFs are not supported.");
                }

                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                if (text == null || text.trim().isEmpty()) {
                    throw InterviewException.badRequest("No readable text found in PDF.");
                }
                return text;
            }
        } catch (IOException e) {
            log.error("PDF Parsing Error", e);
            throw InterviewException.internalError("Failed to process PDF.");
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private String extractDocxText(MultipartFile file) {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            if (text == null || text.trim().isEmpty()) {
                throw InterviewException.badRequest("No readable text found in Word document.");
            }
            return text;
        } catch (IOException e) {
            log.error("Word Parsing Error", e);
            throw InterviewException.internalError("Failed to process Word document.");
        }
    }

    private String extractPlainText(MultipartFile file) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (text.trim().isEmpty()) {
                throw InterviewException.badRequest("Uploaded text file is empty.");
            }
            return text;
        } catch (IOException e) {
            log.error("Text File Parsing Error", e);
            throw InterviewException.internalError("Failed to process text file.");
        }
    }
}