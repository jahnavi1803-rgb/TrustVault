package com.trustvault.repository;

import com.trustvault.entity.AuditLog;
import com.trustvault.entity.AuditOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findTopByOrderBySequenceNumberDesc();

    List<AuditLog> findAllByOrderBySequenceNumberDesc();

    List<AuditLog> findAllByOrderBySequenceNumberAsc();

    List<AuditLog> findTop50ByOrderBySequenceNumberDesc();

    List<AuditLog> findByActorUsernameOrderBySequenceNumberDesc(String actorUsername);

    long countByOutcome(AuditOutcome outcome);
}
