package com.trustvault.service;

import com.trustvault.util.FileValidationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadSecurityTest {

    @Test
    @DisplayName("Blocked extension: Executable files (.exe, .sh, .bat) are rejected")
    void blockedExtensionsRejected() {
        MockMultipartFile exeFile = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "MZ dummy binary content".getBytes());
        assertThatThrownBy(() -> FileValidationUtil.validateFile(exeFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Executable and script file uploads are prohibited");

        MockMultipartFile shFile = new MockMultipartFile("file", "attack.sh", "application/x-sh", "#!/bin/bash\nrm -rf /".getBytes());
        assertThatThrownBy(() -> FileValidationUtil.validateFile(shFile))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Filename sanitization: Path traversal sequences (../) are stripped")
    void pathTraversalSanitized() {
        String unsafe = "../../secret/passwords.txt";
        String safe = FileValidationUtil.sanitizeFilename(unsafe);

        assertThat(safe).doesNotContain("..");
        assertThat(safe).doesNotContain("/");
        assertThat(safe).doesNotContain("\\");
        assertThat(safe).isEqualTo("passwords.txt");
    }

    @Test
    @DisplayName("Oversized file: Files exceeding 25 MB limit are rejected")
    void oversizedFilesRejected() {
        // Create 26 MB mock file
        byte[] oversizedBytes = new byte[26 * 1024 * 1024];
        MockMultipartFile hugeFile = new MockMultipartFile("file", "huge_document.pdf", "application/pdf", oversizedBytes);

        assertThatThrownBy(() -> FileValidationUtil.validateFile(hugeFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds the 25 MB limit");
    }

    @Test
    @DisplayName("Legitimate documents (.pdf, .docx, .png, .txt) pass validation")
    void legitimateFilesAccepted() {
        MockMultipartFile pdf = new MockMultipartFile("file", "annual_report.pdf", "application/pdf", "%PDF-1.5 legitimate pdf".getBytes());
        FileValidationUtil.validateFile(pdf);

        MockMultipartFile txt = new MockMultipartFile("file", "notes.txt", "text/plain", "Simple text notes".getBytes());
        FileValidationUtil.validateFile(txt);
    }
}
