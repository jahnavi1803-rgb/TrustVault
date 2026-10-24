package com.trustvault.util;

import com.trustvault.entity.AuditLog;
import com.trustvault.entity.AuditOutcome;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

public final class HashChainUtil {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private HashChainUtil() {
    }

    public static String computeHash(String previousHash, Long sequenceNumber, Instant timestamp,
                                     Long actorId, String actorUsername, String actorRole,
                                     String action, String resourceId, String resourceType,
                                     AuditOutcome outcome, String details) {
        String canonicalPayload = String.join("|",
                previousHash != null ? previousHash : GENESIS_HASH,
                String.valueOf(sequenceNumber),
                String.valueOf(timestamp != null ? timestamp.toEpochMilli() : 0L),
                actorId != null ? String.valueOf(actorId) : "ANONYMOUS",
                actorUsername != null ? actorUsername : "UNKNOWN",
                actorRole != null ? actorRole : "NONE",
                action != null ? action : "UNKNOWN_ACTION",
                resourceId != null ? resourceId : "NONE",
                resourceType != null ? resourceType : "NONE",
                outcome != null ? outcome.name() : "UNKNOWN",
                details != null ? details : ""
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public static boolean verifyEntry(AuditLog entry, String expectedPreviousHash) {
        if (entry == null) {
            return false;
        }

        if (!entry.getPreviousHash().equalsIgnoreCase(expectedPreviousHash)) {
            return false;
        }

        String calculated = computeHash(
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

        return calculated.equalsIgnoreCase(entry.getCurrentHash());
    }
}
