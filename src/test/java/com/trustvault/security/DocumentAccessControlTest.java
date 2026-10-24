package com.trustvault.security;

import com.trustvault.entity.AppUser;
import com.trustvault.entity.Document;
import com.trustvault.entity.PermissionType;
import com.trustvault.entity.Role;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;
import com.trustvault.service.DocumentService;
import com.trustvault.service.ShareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DocumentAccessControlTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ShareService shareService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    private AppUser ownerAlice;
    private AppUser viewerBob;
    private AppUser collaboratorCharlie;
    private Document testDoc;

    @BeforeEach
    void setup() throws IOException {
        ownerAlice = userRepository.save(new AppUser("alice", "alice@vault.internal", "hash", "Alice Smith", Role.ROLE_OWNER));
        viewerBob = userRepository.save(new AppUser("bob", "bob@vault.internal", "hash", "Bob Jones", Role.ROLE_VIEWER));
        collaboratorCharlie = userRepository.save(new AppUser("charlie", "charlie@vault.internal", "hash", "Charlie Brown", Role.ROLE_OWNER));

        MockMultipartFile file = new MockMultipartFile("file", "secret_report.pdf", "application/pdf", "%PDF-1.5 test content".getBytes());
        testDoc = documentService.uploadDocument(file, "Confidential Financial Report", null, "Financials", ownerAlice);
    }

    @Test
    @DisplayName("Cross-user isolation: Bob cannot download Alice's private document (IDOR prevention)")
    void crossUserUnauthorizedAccessBlocked() {
        assertThatThrownBy(() -> documentService.downloadDocument(testDoc.getId(), viewerBob))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("READ");
    }

    @Test
    @DisplayName("VIEWER role is blocked from uploading documents")
    void viewerBlockedFromUploading() {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "Hello".getBytes());
        assertThatThrownBy(() -> documentService.uploadDocument(file, "Viewer Upload", null, "Notes", viewerBob))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("READ vs EDIT enforcement: READ grantee can download but cannot edit metadata")
    void readVsEditEnforcement() throws IOException {
        // Grant READ permission to Charlie
        shareService.grantPermission(testDoc.getId(), collaboratorCharlie.getUsername(), PermissionType.READ, ownerAlice);

        // Charlie CAN download
        var resource = documentService.downloadDocument(testDoc.getId(), collaboratorCharlie);
        assertThat(resource).isNotNull();

        // Charlie CANNOT edit metadata
        assertThatThrownBy(() -> documentService.updateDocumentMetadata(testDoc.getId(), "Hacked Title", null, "Hacked", collaboratorCharlie))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("EDIT");
    }

    @Test
    @DisplayName("EDIT grantee can edit metadata but CANNOT delete document")
    void editGranteeCannotDeleteDocument() {
        // Grant EDIT permission to Charlie
        shareService.grantPermission(testDoc.getId(), collaboratorCharlie.getUsername(), PermissionType.EDIT, ownerAlice);

        // Charlie can edit
        Document updated = documentService.updateDocumentMetadata(testDoc.getId(), "Legitimate Updated Title", null, "Updated description", collaboratorCharlie);
        assertThat(updated.getTitle()).isEqualTo("Legitimate Updated Title");

        // Charlie CANNOT delete the document
        assertThatThrownBy(() -> documentService.deleteDocument(testDoc.getId(), collaboratorCharlie))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the document owner or an administrator can delete");
    }

    @Test
    @DisplayName("Revocation enforcement: Access is immediately revoked upon owner action")
    void permissionRevocationImmediatelyEnforced() throws IOException {
        // Grant READ to Charlie
        shareService.grantPermission(testDoc.getId(), collaboratorCharlie.getUsername(), PermissionType.READ, ownerAlice);

        // Verifying Charlie can download
        assertThat(documentService.downloadDocument(testDoc.getId(), collaboratorCharlie)).isNotNull();

        // Revoke permission
        shareService.revokePermission(testDoc.getId(), collaboratorCharlie.getId(), ownerAlice);

        // Subsequent access attempt MUST fail
        assertThatThrownBy(() -> documentService.downloadDocument(testDoc.getId(), collaboratorCharlie))
                .isInstanceOf(AccessDeniedException.class);
    }
}
