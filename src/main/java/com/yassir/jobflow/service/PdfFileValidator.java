package com.yassir.jobflow.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfFileValidator {

    public static final long MAX_PDF_SIZE_BYTES = 5L * 1024 * 1024;
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String DEFAULT_FILE_NAME = "cv.pdf";

    public void validatePdf(MultipartFile file) {
        if (file == null) {
            throw new IllegalArgumentException("CV file is required");
        }

        if (!PDF_CONTENT_TYPE.equalsIgnoreCase(file.getContentType())) {
            throw new IllegalArgumentException(
                    "Only PDF files are allowed"
            );
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                    "CV file is required"
            );
        }

        if (file.getSize() > MAX_PDF_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "CV file size must not exceed 5 MB"
            );
        }

        if (!hasValidPdfSignature(file)) {
            throw new IllegalArgumentException(
                    "Invalid PDF file"
            );
        }
    }

    public String getSafeOriginalFilename(MultipartFile file) {
        if (file == null
                || file.getOriginalFilename() == null
                || file.getOriginalFilename().isBlank()) {

            return DEFAULT_FILE_NAME;
        }

        String originalFilename =
                file.getOriginalFilename()
                        .replace('\\', '/');

        int lastSeparator = originalFilename.lastIndexOf('/');

        String filename = lastSeparator >= 0
                ? originalFilename.substring(lastSeparator + 1)
                : originalFilename;

        filename = filename
                .replaceAll("[\\p{Cntrl}\"]", "_")
                .trim();

        if (filename.isBlank()) {
            return DEFAULT_FILE_NAME;
        }

        return filename;
    }

    private boolean hasValidPdfSignature(MultipartFile file) {
        try {
            byte[] header = new byte[5];

            try (var inputStream = file.getInputStream()) {
                int bytesRead = inputStream.read(header);

                if (bytesRead < 5) {
                    return false;
                }
            }

            return header[0] == '%'
                    && header[1] == 'P'
                    && header[2] == 'D'
                    && header[3] == 'F'
                    && header[4] == '-';

        } catch (IOException e) {
            return false;
        }
    }
}
