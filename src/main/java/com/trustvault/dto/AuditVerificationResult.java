package com.trustvault.dto;

import java.time.Instant;

public class AuditVerificationResult {

    private boolean valid;
    private long totalRecords;
    private Long brokenAtSequence;
    private String failureReason;
    private String genesisHash;
    private String latestHash;
    private Instant verifiedAt;

    public AuditVerificationResult() {
        this.verifiedAt = Instant.now();
    }

    public static AuditVerificationResult success(long totalRecords, String genesisHash, String latestHash) {
        AuditVerificationResult res = new AuditVerificationResult();
        res.valid = true;
        res.totalRecords = totalRecords;
        res.genesisHash = genesisHash;
        res.latestHash = latestHash;
        res.failureReason = null;
        res.brokenAtSequence = null;
        return res;
    }

    public static AuditVerificationResult failure(long brokenAtSequence, String failureReason, long totalRecords) {
        AuditVerificationResult res = new AuditVerificationResult();
        res.valid = false;
        res.totalRecords = totalRecords;
        res.brokenAtSequence = brokenAtSequence;
        res.failureReason = failureReason;
        return res;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Long getBrokenAtSequence() {
        return brokenAtSequence;
    }

    public void setBrokenAtSequence(Long brokenAtSequence) {
        this.brokenAtSequence = brokenAtSequence;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getGenesisHash() {
        return genesisHash;
    }

    public void setGenesisHash(String genesisHash) {
        this.genesisHash = genesisHash;
    }

    public String getLatestHash() {
        return latestHash;
    }

    public void setLatestHash(String latestHash) {
        this.latestHash = latestHash;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
