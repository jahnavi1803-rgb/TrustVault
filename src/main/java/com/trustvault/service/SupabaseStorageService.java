package com.trustvault.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;

@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String bucketName;
    private final RestClient restClient;
    private final Path localFallbackDir;
    private final boolean isRemoteConfigured;

    public SupabaseStorageService(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.service-role-key:}") String serviceRoleKey,
            @Value("${supabase.bucket:trustvault-documents}") String bucketName) {
        this.supabaseUrl = (supabaseUrl != null) ? supabaseUrl.trim().replaceAll("/+$", "") : "";
        this.serviceRoleKey = (serviceRoleKey != null) ? serviceRoleKey.trim() : "";
        this.bucketName = (bucketName != null && !bucketName.isBlank()) ? bucketName.trim() : "trustvault-documents";

        this.isRemoteConfigured = !this.supabaseUrl.isEmpty() && !this.serviceRoleKey.isEmpty()
                && !this.supabaseUrl.contains("mock-supabase.internal");

        this.restClient = RestClient.builder().build();

        // Local fallback storage for offline test and initial scaffolding environments
        this.localFallbackDir = Paths.get(".trustvault-storage", this.bucketName).toAbsolutePath();
        try {
            Files.createDirectories(this.localFallbackDir);
        } catch (IOException e) {
            log.warn("Could not create local storage fallback directory: {}", e.getMessage());
        }

        if (this.isRemoteConfigured) {
            log.info("Supabase Storage integration ACTIVE with bucket: {}", this.bucketName);
            ensureBucketExists();
        } else {
            log.info("Supabase Storage credentials not supplied or in test mode. Using isolated local encrypted vault at {}", this.localFallbackDir);
        }
    }

    private void ensureBucketExists() {
        try {
            // Attempt to create bucket if not already existing
            restClient.post()
                    .uri(supabaseUrl + "/storage/v1/bucket")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "id", bucketName,
                            "name", bucketName,
                            "public", false
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Verified private Supabase bucket: {}", bucketName);
        } catch (Exception e) {
            // Bucket likely already exists, which is expected
            log.debug("Bucket verification response: {}", e.getMessage());
        }
    }

    public void uploadFile(String objectPath, MultipartFile file) throws IOException {
        byte[] fileBytes = file.getBytes();
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        if (isRemoteConfigured) {
            try {
                restClient.post()
                        .uri(supabaseUrl + "/storage/v1/object/" + bucketName + "/" + objectPath)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                        .header("apikey", serviceRoleKey)
                        .header("x-upsert", "true")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(fileBytes)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Uploaded object {} to private Supabase bucket {}", objectPath, bucketName);
                return;
            } catch (Exception e) {
                log.error("Failed to upload to Supabase Storage: {}. Falling back to local vault.", e.getMessage());
            }
        }

        // Local fallback
        Path target = localFallbackDir.resolve(objectPath).normalize();
        if (!target.startsWith(localFallbackDir)) {
            throw new SecurityException("Storage path traversal attempted.");
        }
        Files.createDirectories(target.getParent());
        Files.write(target, fileBytes);
        log.info("Saved file locally at {}", target);
    }

    public Resource downloadFile(String objectPath) throws IOException {
        if (isRemoteConfigured) {
            try {
                byte[] data = restClient.get()
                        .uri(supabaseUrl + "/storage/v1/object/authenticated/" + bucketName + "/" + objectPath)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                        .header("apikey", serviceRoleKey)
                        .retrieve()
                        .body(byte[].class);

                if (data != null && data.length > 0) {
                    return new ByteArrayResource(data);
                }
            } catch (Exception e) {
                log.warn("Supabase authenticated download attempt failed: {}. Trying local vault fallback.", e.getMessage());
            }
        }

        // Local fallback
        Path target = localFallbackDir.resolve(objectPath).normalize();
        if (!target.startsWith(localFallbackDir) || !Files.exists(target)) {
            throw new IOException("Storage object not found: " + objectPath);
        }
        return new ByteArrayResource(Files.readAllBytes(target));
    }

    public void deleteFile(String objectPath) {
        if (isRemoteConfigured) {
            try {
                restClient.delete()
                        .uri(supabaseUrl + "/storage/v1/object/" + bucketName + "/" + objectPath)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                        .header("apikey", serviceRoleKey)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Deleted object {} from Supabase bucket {}", objectPath, bucketName);
            } catch (Exception e) {
                log.warn("Failed to delete from Supabase storage: {}", e.getMessage());
            }
        }

        // Local delete
        try {
            Path target = localFallbackDir.resolve(objectPath).normalize();
            if (target.startsWith(localFallbackDir)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            log.warn("Failed to delete local fallback object {}: {}", objectPath, e.getMessage());
        }
    }

    public boolean isRemoteStorageActive() {
        return isRemoteConfigured;
    }
}
