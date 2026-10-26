package com.yassir.jobflow.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfFileValidatorTest {

    private final PdfFileValidator validator = new PdfFileValidator();

    @Test
    void acceptsPdfWithCorrectContentTypeAndSignature() {
        MockMultipartFile file = pdfFile(
                "resume.pdf",
                "%PDF-1.4 test content".getBytes(StandardCharsets.UTF_8)
        );

        assertDoesNotThrow(() -> validator.validatePdf(file));
    }

    @Test
    void rejectsMissingFile() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validatePdf(null)
        );

        assertEquals("CV file is required", exception.getMessage());
    }

    @Test
    void rejectsNonPdfContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.txt",
                "text/plain",
                "not a pdf".getBytes(StandardCharsets.UTF_8)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validatePdf(file)
        );

        assertEquals("Only PDF files are allowed", exception.getMessage());
    }

    @Test
    void rejectsEmptyPdfFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validatePdf(file)
        );

        assertEquals("CV file is required", exception.getMessage());
    }

    @Test
    void rejectsFileLargerThanFiveMegabytes() {
        byte[] content = new byte[
                (int) PdfFileValidator.MAX_PDF_SIZE_BYTES + 1
        ];

        System.arraycopy(
                "%PDF-".getBytes(StandardCharsets.US_ASCII),
                0,
                content,
                0,
                5
        );

        MockMultipartFile file = pdfFile("large.pdf", content);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validatePdf(file)
        );

        assertEquals(
                "CV file size must not exceed 5 MB",
                exception.getMessage()
        );
    }

    @Test
    void rejectsPdfContentTypeWhenSignatureIsInvalid() {
        MockMultipartFile file = pdfFile(
                "fake.pdf",
                "This is not a PDF".getBytes(StandardCharsets.UTF_8)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validatePdf(file)
        );

        assertEquals("Invalid PDF file", exception.getMessage());
    }

    @Test
    void stripsPathFromOriginalFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "..\\private\\resume.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8)
        );

        assertEquals(
                "resume.pdf",
                validator.getSafeOriginalFilename(file)
        );
    }

    @Test
    void replacesControlCharactersAndQuotesInOriginalFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my\r\n\"resume.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8)
        );

        assertEquals(
                "my___resume.pdf",
                validator.getSafeOriginalFilename(file)
        );
    }

    private MockMultipartFile pdfFile(String filename, byte[] content) {
        return new MockMultipartFile(
                "file",
                filename,
                "application/pdf",
                content
        );
    }
}