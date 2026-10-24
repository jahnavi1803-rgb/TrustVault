package com.trustvault.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_seq", columnList = "sequence_number", unique = true),
    @Index(name = "idx_audit_created", columnList = "timestamp DESC"),
    @Index(name = "idx_audit_actor", columnList = "actor_username")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_number", nullable = false, unique = true)
    private Long sequenceNumber;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditOutcome outcome;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash;

    @Column(name = "current_hash", nullable = false, length = 64)
    private String currentHash;

    public AuditLog() {
    }

    public AuditLog(Long sequenceNumber, Instant timestamp, Long actorId, String actorUsername,
                    String actorRole, String action, String resourceId, String resourceType,
                    String ipAddress, String userAgent, AuditOutcome outcome, String details,
                    String previousHash, String currentHash) {
        this.sequenceNumber = sequenceNumber;
        this.timestamp = timestamp;
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.outcome = outcome;
        this.details = details;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(AuditOutcome outcome) {
        this.outcome = outcome;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
    }
}
