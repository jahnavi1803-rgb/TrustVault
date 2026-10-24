package com.trustvault.service;

import com.trustvault.entity.*;
import com.trustvault.repository.DocumentPermissionRepository;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShareService {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final TamperEvidentAuditService auditService;

    public ShareService(DocumentRepository documentRepository,
                        DocumentPermissionRepository permissionRepository,
                        UserRepository userRepository,
                        TamperEvidentAuditService auditService) {
        this.documentRepository = documentRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DocumentPermission grantPermission(UUID documentId, String targetUsername,
                                             PermissionType permissionType, AppUser granter) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // Enforce ownership or ADMIN role
        boolean isOwner = document.getOwner().getId().equals(granter.getId());
        boolean isAdmin = granter.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            auditService.recordEvent(
                    granter.getId(),
                    granter.getUsername(),
                    granter.getRole().name(),
                    "UNAUTHORIZED_SHARE_ATTEMPT",
                    documentId.toString(),
                    "DOCUMENT",
                    AuditOutcome.DENIED,
                    "Unauthorized attempt to share document owned by " + document.getOwner().getUsername()
            );
            throw new AccessDeniedException("Only the document owner or an administrator can manage sharing permissions.");
        }

        AppUser targetUser = userRepository.findByUsername(targetUsername.trim())
                .orElseThrow(() -> new IllegalArgumentException("User '" + targetUsername + "' does not exist."));

        if (targetUser.getId().equals(document.getOwner().getId())) {
            throw new IllegalArgumentException("Cannot grant permissions to the document owner.");
        }

        Optional<DocumentPermission> existingPerm = permissionRepository.findByDocumentAndUser(document, targetUser);
        DocumentPermission permission;

        if (existingPerm.isPresent()) {
            permission = existingPerm.get();
            permission.setPermissionType(permissionType);
            permission.setGrantedBy(granter);
        } else {
            permission = new DocumentPermission(document, targetUser, permissionType, granter);
        }

        DocumentPermission saved = permissionRepository.save(permission);

        auditService.recordEvent(
                granter.getId(),
                granter.getUsername(),
                granter.getRole().name(),
                "PERMISSION_GRANTED",
                document.getId().toString(),
                "DOCUMENT",
                AuditOutcome.SUCCESS,
                "Granted " + permissionType + " permission on '" + document.getTitle() + "' to user '" + targetUser.getUsername() + "'"
        );

        return saved;
    }

    @Transactional
    public void revokePermission(UUID documentId, Long targetUserId, AppUser revoker) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        boolean isOwner = document.getOwner().getId().equals(revoker.getId());
        boolean isAdmin = revoker.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            auditService.recordEvent(
                    revoker.getId(),
                    revoker.getUsername(),
                    revoker.getRole().name(),
                    "UNAUTHORIZED_REVOKE_ATTEMPT",
                    documentId.toString(),
                    "DOCUMENT",
                    AuditOutcome.DENIED,
                    "Unauthorized attempt to revoke sharing permission."
            );
            throw new AccessDeniedException("Only the document owner or an administrator can revoke sharing permissions.");
        }

        AppUser targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found: " + targetUserId));

        permissionRepository.deleteByDocumentAndUser(document, targetUser);

        auditService.recordEvent(
                revoker.getId(),
                revoker.getUsername(),
                revoker.getRole().name(),
                "PERMISSION_REVOKED",
                document.getId().toString(),
                "DOCUMENT",
                AuditOutcome.SUCCESS,
                "Revoked permissions on '" + document.getTitle() + "' from user '" + targetUser.getUsername() + "'"
        );
    }

    @Transactional(readOnly = true)
    public List<DocumentPermission> getDocumentPermissions(UUID documentId, AppUser requester) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        boolean isOwner = document.getOwner().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Only the document owner or an administrator can view permission lists.");
        }

        return permissionRepository.findByDocumentOrderByCreatedAtDesc(document);
    }
}
