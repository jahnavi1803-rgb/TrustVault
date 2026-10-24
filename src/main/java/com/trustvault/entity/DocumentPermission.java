package com.trustvault.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "document_permissions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_doc_user", columnNames = {"document_id", "user_id"})
}, indexes = {
    @Index(name = "idx_perm_user", columnList = "user_id"),
    @Index(name = "idx_perm_doc", columnList = "document_id")
})
public class DocumentPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type", nullable = false, length = 10)
    private PermissionType permissionType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "granted_by_id", nullable = false)
    private AppUser grantedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DocumentPermission() {
    }

    public DocumentPermission(Document document, AppUser user, PermissionType permissionType, AppUser grantedBy) {
        this.document = document;
        this.user = user;
        this.permissionType = permissionType;
        this.grantedBy = grantedBy;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public PermissionType getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(PermissionType permissionType) {
        this.permissionType = permissionType;
    }

    public AppUser getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(AppUser grantedBy) {
        this.grantedBy = grantedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
