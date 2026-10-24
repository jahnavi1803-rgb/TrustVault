package com.trustvault.service;

import com.trustvault.dto.AuditVerificationResult;
import com.trustvault.entity.AuditLog;
import com.trustvault.entity.AuditOutcome;
import com.trustvault.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TamperEvidentAuditTest {

    @Autowired
    private TamperEvidentAuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("Cryptographic Hash Chain verifies intact logs successfully")
    void hashChainVerifiesIntactLogs() {
        auditService.recordEvent(1L, "admin", "ROLE_ADMIN", "TEST_ACTION_1", "res1", "DOC", AuditOutcome.SUCCESS, "Note 1");
        auditService.recordEvent(2L, "alice", "ROLE_OWNER", "TEST_ACTION_2", "res2", "DOC", AuditOutcome.SUCCESS, "Note 2");
        auditService.recordEvent(3L, "bob", "ROLE_VIEWER", "TEST_ACTION_3", "res3", "DOC", AuditOutcome.SUCCESS, "Note 3");

        AuditVerificationResult result = auditService.verifyChainIntegrity();

        assertThat(result.isValid()).isTrue();
        assertThat(result.getTotalRecords()).isGreaterThanOrEqualTo(3);
        assertThat(result.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("Tamper Detection: Modifying an audit record in the database breaks cryptographic verification")
    void tamperingBreaksVerification() {
        AuditLog entry1 = auditService.recordEvent(1L, "alice", "ROLE_OWNER", "LEGITIMATE_ACTION", "res10", "DOC", AuditOutcome.SUCCESS, "Original");
        AuditLog entry2 = auditService.recordEvent(2L, "bob", "ROLE_VIEWER", "LEGITIMATE_ACTION_2", "res20", "DOC", AuditOutcome.SUCCESS, "Original 2");

        // Attacker tampers directly with record in DB
        entry1.setDetails("MALICIOUSLY_ALTERED_PAYLOAD");
        auditLogRepository.save(entry1);

        AuditVerificationResult result = auditService.verifyChainIntegrity();

        // Integrity verification MUST fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getBrokenAtSequence()).isEqualTo(entry1.getSequenceNumber());
        assertThat(result.getFailureReason()).contains("Cryptographic integrity violation");
    }

    @Test
    @DisplayName("Tamper Detection: Deleting an intermediate audit record breaks sequence continuity")
    void deletionBreaksSequenceContinuity() {
        AuditLog e1 = auditService.recordEvent(1L, "alice", "ROLE_OWNER", "OP_1", "r1", "DOC", AuditOutcome.SUCCESS, "op 1");
        AuditLog e2 = auditService.recordEvent(2L, "bob", "ROLE_OWNER", "OP_2", "r2", "DOC", AuditOutcome.SUCCESS, "op 2");
        AuditLog e3 = auditService.recordEvent(3L, "carol", "ROLE_OWNER", "OP_3", "r3", "DOC", AuditOutcome.SUCCESS, "op 3");

        // Attacker deletes intermediate record
        auditLogRepository.delete(e2);

        AuditVerificationResult result = auditService.verifyChainIntegrity();

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).contains("Sequence break detected");
    }
}
