package com.trustvault.util;

import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class FileValidationUtil {

    public static final long MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024; // 25 MB

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "docx", "doc", "xlsx", "xls", "pptx", "ppt", "txt", "csv",
            "png", "jpg", "jpeg", "gif", "webp", "svg", "zip", "md", "json", "xml"
    ));

    private static final Set<String> BLOCKED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "exe", "bat", "cmd", "sh", "ps1", "msi", "jar", "vbs", "js", "py",
            "php", "jsp", "asp", "aspx", "dll", "scr", "com", "pif", "bin"
    ));

    private static final Tika TIKA = new Tika();

    private FileValidationUtil() {
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document.bin";
        }
        // Remove path components
        String cleanName = filename.replace("\\", "/");
        int lastSlash = cleanName.lastIndexOf('/');
        if (lastSlash >= 0) {
            cleanName = cleanName.substring(lastSlash + 1);
        }
        // Remove dangerous characters and control characters
        cleanName = cleanName.replaceAll("[\\r\\n\\t\\u0000]", "").trim();
        // Prevent path traversal
        cleanName = cleanName.replace("..", "_");
        if (cleanName.isBlank()) {
            return "document.bin";
        }
        return cleanName;
    }

    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds the 25 MB limit.");
        }

        String rawFilename = file.getOriginalFilename();
        String extension = getFileExtension(rawFilename);

        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Executable and script file uploads are prohibited for security reasons.");
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type '." + extension + "' is not supported. Please upload a standard document or image.");
        }

        // Deep content inspection via Apache Tika (magic bytes)
        try (InputStream is = file.getInputStream()) {
            String detectedMime = TIKA.detect(is, rawFilename);
            if (detectedMime != null) {
                String lowerMime = detectedMime.toLowerCase(Locale.ROOT);
                if (lowerMime.contains("x-dosexec") || lowerMime.contains("x-executable") ||
                    lowerMime.contains("x-sh") || lowerMime.contains("javascript") ||
                    lowerMime.contains("x-msdownload")) {
                    throw new IllegalArgumentException("Uploaded file contains executable signatures and was blocked.");
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to inspect uploaded file content.", e);
        }
    }

    public static String detectMimeType(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return TIKA.detect(is, file.getOriginalFilename());
        } catch (IOException e) {
            return file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        }
    }
}
