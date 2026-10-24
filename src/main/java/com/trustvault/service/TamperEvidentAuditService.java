package com.trustvault.service;

import com.trustvault.dto.AuditVerificationResult;
import com.trustvault.entity.AuditLog;
import com.trustvault.entity.AuditOutcome;
import com.trustvault.repository.AuditLogRepository;
import com.trustvault.util.HashChainUtil;
import com.trustvault.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TamperEvidentAuditService {

    private static final Logger log = LoggerFactory.getLogger(TamperEvidentAuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final String genesisHash;

    public TamperEvidentAuditService(
            AuditLogRepository auditLogRepository,
            @Value("${trustvault.audit.genesis-hash:0000000000000000000000000000000000000000000000000000000000000000}") String genesisHash) {
        this.auditLogRepository = auditLogRepository;
        this.genesisHash = genesisHash;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public synchronized AuditLog recordEvent(Long actorId, String actorUsername, String actorRole,
                                             String action, String resourceId, String resourceType,
                                             AuditOutcome outcome, String details) {
        Instant now = Instant.now();
        String ip = SecurityUtil.getClientIp();
        String userAgent = SecurityUtil.getUserAgent();

        Optional<AuditLog> latestOpt = auditLogRepository.findTopByOrderBySequenceNumberDesc();
        long sequence = latestOpt.map(log -> log.getSequenceNumber() + 1).orElse(1L);
        String previousHash = latestOpt.map(AuditLog::getCurrentHash).orElse(genesisHash);

        String currentHash = HashChainUtil.computeHash(
                previousHash,
                sequence,
                now,
                actorId,
                actorUsername,
                actorRole,
                action,
                resourceId,
                resourceType,
                outcome,
                details
        );

        AuditLog logEntry = new AuditLog(
                sequence,
                now,
                actorId,
                actorUsername != null ? actorUsername : "ANONYMOUS",
                actorRole != null ? actorRole : "NONE",
                action,
                resourceId,
                resourceType,
                ip,
                userAgent,
                outcome,
                details,
                previousHash,
                currentHash
        );

        AuditLog saved = auditLogRepository.save(logEntry);
        log.info("Audit [Seq #{}]: {} by {} -> {} [Hash: {}...]", sequence, action, actorUsername, outcome, currentHash.substring(0, 12));
        return saved;
    }

    @Transactional(readOnly = true)
    public AuditVerificationResult verifyChainIntegrity() {
        List<AuditLog> chain = auditLogRepository.findAllByOrderBySequenceNumberAsc();

        if (chain.isEmpty()) {
            return AuditVerificationResult.success(0, genesisHash, genesisHash);
        }

        String expectedPrevHash = genesisHash;
        long expectedSequence = 1L;

        for (AuditLog entry : chain) {
            // 1. Check sequence ordering and continuity
            if (entry.getSequenceNumber() == null || entry.getSequenceNumber() != expectedSequence) {
                return AuditVerificationResult.failure(
                        expectedSequence,
                        "Sequence break detected. Expected sequence #" + expectedSequence + " but found #" + entry.getSequenceNumber(),
                        chain.size()
                );
            }

            // 2. Verify backward hash pointer
            if (!entry.getPreviousHash().equalsIgnoreCase(expectedPrevHash)) {
                return AuditVerificationResult.failure(
                        entry.getSequenceNumber(),
                        "Previous hash pointer mismatch at sequence #" + entry.getSequenceNumber() +
                        ". Stored: " + entry.getPreviousHash() + ", Expected: " + expectedPrevHash,
                        chain.size()
                );
            }

            // 3. Verify entry hash integrity
            String recalculatedHash = HashChainUtil.computeHash(
                    entry.getPreviousHash(),
                    entry.getSequenceNumber(),
                    entry.getTimestamp(),
                    entry.getActorId(),
                    entry.getActorUsername(),
                    entry.getActorRole(),
                    entry.getAction(),
                    entry.getResourceId(),
                    entry.getResourceType(),
                    entry.getOutcome(),
                    entry.getDetails()
            );

            if (!recalculatedHash.equalsIgnoreCase(entry.getCurrentHash())) {
                return AuditVerificationResult.failure(
                        entry.getSequenceNumber(),
                        "Cryptographic integrity violation at sequence #" + entry.getSequenceNumber() +
                        ". Record data has been altered. Stored Hash: " + entry.getCurrentHash() +
                        ", Calculated Hash: " + recalculatedHash,
                        chain.size()
                );
            }

            expectedPrevHash = entry.getCurrentHash();
            expectedSequence++;
        }

        return AuditVerificationResult.success(
                chain.size(),
                genesisHash,
                chain.get(chain.size() - 1).getCurrentHash()
        );
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderBySequenceNumberDesc();
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderBySequenceNumberDesc();
    }
}
