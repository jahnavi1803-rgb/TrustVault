package com.trustvault.service;

import com.trustvault.entity.*;
import com.trustvault.repository.CategoryRepository;
import com.trustvault.repository.DocumentPermissionRepository;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;
import com.trustvault.util.FileValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final SupabaseStorageService storageService;
    private final TamperEvidentAuditService auditService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentPermissionRepository permissionRepository,
                           CategoryRepository categoryRepository,
                           UserRepository userRepository,
                           SupabaseStorageService storageService,
                           TamperEvidentAuditService auditService) {
        this.documentRepository = documentRepository;
        this.permissionRepository = permissionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.auditService = auditService;
    }

    @Transactional
    public Document uploadDocument(MultipartFile file, String title, Long categoryId,
                                  String description, AppUser owner) throws IOException {
        // Enforce role authorization: VIEWER cannot upload documents
        if (owner.getRole() == Role.ROLE_VIEWER) {
            auditService.recordEvent(
                    owner.getId(),
                    owner.getUsername(),
                    owner.getRole().name(),
                    "DOCUMENT_UPLOAD_DENIED",
                    null,
                    "DOCUMENT",
                    AuditOutcome.DENIED,
                    "Viewer attempted to upload a document."
            );
            throw new AccessDeniedException("Viewers are not authorized to upload new documents.");
        }

        // Validate file constraints and inspect magic bytes
        FileValidationUtil.validateFile(file);

        String cleanFilename = FileValidationUtil.sanitizeFilename(file.getOriginalFilename());
        String mimeType = FileValidationUtil.detectMimeType(file);
        long fileSize = file.getSize();

        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId).orElse(null);
        }

        // Generate non-guessable, collision-free storage object path
        UUID docId = UUID.randomUUID();
        String storagePath = "docs/" + owner.getId() + "/" + docId + "/" + cleanFilename;

        // Step 1: Upload to private Supabase Storage
        storageService.uploadFile(storagePath, file);

        // Step 2: Save document metadata in PostgreSQL
        Document document = new Document();
        document.setId(docId);
        document.setTitle((title != null && !title.isBlank()) ? title.trim() : cleanFilename);
        document.setOriginalFilename(cleanFilename);
        document.setStoragePath(storagePath);
        document.setFileSizeBytes(fileSize);
        document.setMimeType(mimeType);
        document.setCategory(category);
        document.setOwner(owner);
        document.setDescription(description != null ? description.trim() : "");

        Document savedDoc = documentRepository.save(document);

        // Step 3: Record audit log
        auditService.recordEvent(
                owner.getId(),
                owner.getUsername(),
                owner.getRole().name(),
                "DOCUMENT_UPLOAD",
                savedDoc.getId().toString(),
                "DOCUMENT",
                AuditOutcome.SUCCESS,
                "Uploaded document: '" + savedDoc.getTitle() + "' (" + fileSize + " bytes, MIME: " + mimeType + ")"
        );

        return savedDoc;
    }

    @Transactional(readOnly = true)
    public Document getAuthorizedDocument(UUID documentId, AppUser requester, PermissionType requiredPermission) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        // 1. ADMIN has global access
        if (requester.getRole() == Role.ROLE_ADMIN) {
            return document;
        }

        // 2. OWNER has full access
        if (document.getOwner().getId().equals(requester.getId())) {
            return document;
        }

        // 3. Check explicit permissions in document_permissions table
        Optional<DocumentPermission> permOpt = permissionRepository.findByDocumentAndUser(document, requester);
        if (permOpt.isPresent()) {
            DocumentPermission perm = permOpt.get();
            if (requiredPermission == PermissionType.READ) {
                // Both READ and EDIT grants can read
                return document;
            } else if (requiredPermission == PermissionType.EDIT && perm.getPermissionType() == PermissionType.EDIT) {
                return document;
            }
        }

        // Authorization failure / IDOR attempt
        auditService.recordEvent(
                requester.getId(),
                requester.getUsername(),
                requester.getRole().name(),
                "UNAUTHORIZED_ACCESS_ATTEMPT",
                documentId.toString(),
                "DOCUMENT",
                AuditOutcome.DENIED,
                "User lacked " + requiredPermission + " permission for document: " + document.getTitle()
        );

        throw new AccessDeniedException("Access denied: You do not have " + requiredPermission + " permission for this document.");
    }

    @Transactional(readOnly = true)
    public Resource downloadDocument(UUID documentId, AppUser requester) throws IOException {
        Document document = getAuthorizedDocument(documentId, requester, PermissionType.READ);

        Resource resource = storageService.downloadFile(document.getStoragePath());

        auditService.recordEvent(
                requester.getId(),
                requester.getUsername(),
                requester.getRole().name(),
                "DOCUMENT_DOWNLOAD",
                document.getId().toString(),
                "DOCUMENT",
                AuditOutcome.SUCCESS,
                "Downloaded file '" + document.getOriginalFilename() + "' for document: " + document.getTitle()
        );

        return resource;
    }

    @Transactional
    public Document updateDocumentMetadata(UUID documentId, String title, Long categoryId,
                                           String description, AppUser requester) {
        Document document = getAuthorizedDocument(documentId, requester, PermissionType.EDIT);

        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId).orElse(null);
        }

        document.setTitle(title.trim());
        document.setCategory(category);
        document.setDescription(description != null ? description.trim() : "");
        Document updated = documentRepository.save(document);

        auditService.recordEvent(
                requester.getId(),
                requester.getUsername(),
                requester.getRole().name(),
                "DOCUMENT_UPDATE",
                updated.getId().toString(),
                "DOCUMENT",
                AuditOutcome.SUCCESS,
                "Updated metadata for document: " + updated.getTitle()
        );

        return updated;
    }

    @Transactional
    public void deleteDocument(UUID documentId, AppUser requester) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        // Only document owner or ADMIN can delete
        boolean isOwner = document.getOwner().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            auditService.recordEvent(
                    requester.getId(),
                    requester.getUsername(),
                    requester.getRole().name(),
                    "UNAUTHORIZED_DELETE_ATTEMPT",
                    documentId.toString(),
                    "DOCUMENT",
                    AuditOutcome.DENIED,
                    "Unauthorized deletion attempt on document owned by " + document.getOwner().getUsername()
            );
            throw new AccessDeniedException("Only the document owner or an administrator can delete this document.");
        }

        // Delete from storage
        storageService.deleteFile(document.getStoragePath());

        // Delete permissions and document record
        permissionRepository.deleteByDocument(document);
        documentRepository.delete(document);

        auditService.recordEvent(
                requester.getId(),
                requester.getUsername(),
                requester.getRole().name(),
                "DOCUMENT_DELETE",
                documentId.toString(),
                "DOCUMENT",
                AuditOutcome.SUCCESS,
                "Deleted document: '" + document.getTitle() + "'"
        );
    }

    @Transactional(readOnly = true)
    public List<Document> getUserOwnedDocuments(AppUser user) {
        return documentRepository.findByOwnerOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Document> getDocumentsSharedWithUser(AppUser user) {
        return documentRepository.findDocumentsSharedWithUser(user);
    }

    @Transactional(readOnly = true)
    public List<Document> searchDocuments(AppUser user, String search, Long categoryId, Instant fromDate, Instant toDate) {
        if (user.getRole() == Role.ROLE_ADMIN) {
            return documentRepository.searchAllDocumentsAdmin(search, categoryId, fromDate, toDate);
        }
        return documentRepository.searchAccessibleDocuments(user, search, categoryId, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public long countOwned(AppUser user) {
        return documentRepository.countByOwner(user);
    }

    @Transactional(readOnly = true)
    public long countShared(AppUser user) {
        return documentRepository.countDocumentsSharedWithUser(user);
    }

    @Transactional(readOnly = true)
    public long countTotal() {
        return documentRepository.count();
    }
}
