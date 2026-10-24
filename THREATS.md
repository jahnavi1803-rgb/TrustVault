# TrustVault — Comprehensive STRIDE Threat Model & Security Analysis

## 1. System Overview & Trust Boundaries

```
+---------------------------------------------------------------------------------------+
| UNTRUSTED ZONE: Public Internet & Client Browser                                      |
| Actors: Authenticated Users (Admins, Owners, Viewers), Unauthenticated Visitors       |
+---------------------------------------------------------------------------------------+
                                           |
                                  [Boundary 1: HTTPS/TLS]
                                           v
+---------------------------------------------------------------------------------------+
| APPLICATION PERIMETER: Render Web Platform                                            |
| Components: Spring Security Filter Chain, Dynamic Port Listener, Reverse Proxy       |
+---------------------------------------------------------------------------------------+
                                           |
                         [Boundary 2: Authentication & RBAC]
                                           v
+---------------------------------------------------------------------------------------+
| BUSINESS LOGIC TIER: Spring Boot 3 Core                                               |
| Components: DocumentService, ShareService, TamperEvidentAuditService, FileValidator   |
+---------------------------------------------------------------------------------------+
                                           |
             +-----------------------------+-----------------------------+
             | [Boundary 3: JPA / SQL]                                   | [Boundary 4: HTTPS API]
             v                                                           v
+-----------------------------------------+   +-----------------------------------------+
| DATA PERSISTENCE: Supabase PostgreSQL   |   | PRIVATE CLOUD: Supabase Object Storage  |
| Data: Users, Metadata, Audit Hash Chain |   | Data: Encrypted Document Blobs          |
+-----------------------------------------+   +-----------------------------------------+
```

---

## 2. Identified Assets & Security Objectives

| Asset | Confidentiality | Integrity | Availability | Security Objective |
|---|:---:|:---:|:---:|---|
| **Confidential Documents** | High | High | High | Only authorized users with valid READ/EDIT grants can access file bytes. |
| **Document Metadata** | Medium | High | High | Ownership, category, and access lists cannot be spoofed or manipulated. |
| **User Identity & Credentials** | High | High | High | Passwords hashed with BCrypt; session tokens protected against hijacking. |
| **Audit Logs** | Low | Critical | High | Tamper-evident; changes to past events must be cryptographically detectable. |
| **Supabase Service Key** | Critical | High | High | Must never leave the backend environment or leak to client browsers. |

---

## 3. STRIDE Threat Assessment & Countermeasures

### 1. Spoofing (Identity & Authentication)
* **Threat:** An adversary attempts to log in as another user, brute force passwords, or spoof session cookies.
* **Mitigations in TrustVault:**
  * Passwords hashed using **BCrypt** with an adaptive work factor of 12.
  * Spring Security session fixation protection (`migrateSession()`).
  * Session cookies configured with `HttpOnly`, `SameSite=Lax`, and secure session termination upon logout.
  * Failed login attempts recorded with sequence-chained audit events.

### 2. Tampering (Data & Code Integrity)
* **Threat A:** An attacker alters file contents during upload or modifies document metadata.
  * **Mitigation:** Deep content inspection with Apache Tika verifies true MIME signatures against whitelisted extensions. Metadata updates strictly require `EDIT` permissions or ownership.
* **Threat B:** An adversary or rogue administrator alters or removes incriminating records in the audit trail.
  * **Mitigation:** **Cryptographic Hash Chaining (SHA-256)** links every event $H_i = \text{SHA256}(H_{i-1} \parallel \text{event}_i)$. Any retrofitted alteration breaks the chain and triggers automated integrity alerts upon administrative inspection.

### 3. Repudiation
* **Threat:** A collaborator downloads or deletes a sensitive document and denies performing the action.
* **Mitigations in TrustVault:**
  * Every sensitive action (login, upload, download, share, revoke, update, delete) generates an audit event capturing `actorId`, `actorUsername`, `clientIp` (resolving `X-Forwarded-For`), `userAgent`, `timestamp`, and `outcome`.
  * Audit records cannot be altered without failing cryptographic verification.

### 4. Information Disclosure
* **Threat A (IDOR):** An attacker alters UUID document paths in download requests (`/documents/{id}/download`) to view documents belonging to other users.
  * **Mitigation:** The server enforces explicit permission checks on every single request against `document_permissions` and `documents.owner_id`. Unauthorized access attempts are rejected with `403 Forbidden` and audited.
* **Threat B (Storage Leaks):** Documents accessed directly via predictable public URLs.
  * **Mitigation:** Supabase Storage bucket `trustvault-documents` is configured as strictly **private**. Files are streamed through server-side authenticated controllers using the private service-role key. No direct public download links exist.

### 5. Denial of Service (DoS)
* **Threat:** Uploading multi-gigabyte files or decompression zip bombs to exhaust server memory or disk storage.
* **Mitigations in TrustVault:**
  * Multipart file uploads strictly enforced to a maximum of 25 MB at the servlet layer.
  * Deep magic-byte inspection prevents processing malformed or corrupted file streams.

### 6. Elevation of Privilege
* **Threat A:** Regular user attempts to assign themselves the `ADMIN` role during registration.
  * **Mitigation:** User registration endpoint unconditionally binds newly registered users to `ROLE_OWNER` or `ROLE_VIEWER`. The `ROLE_ADMIN` role cannot be requested or assigned via public endpoints.
* **Threat B:** A user granted `READ` permission attempts to perform `EDIT` or `DELETE` operations.
  * **Mitigation:** Separate `READ` vs `EDIT` permission models. Only `EDIT` grantees can update metadata. Only `OWNER` or `ADMIN` can delete documents or manage sharing permissions.

---

## 4. Residual Risks & Operational Recommendations

1. **Database Superuser Compromise:**
   * *Residual Risk:* An attacker with direct administrative SQL access to the PostgreSQL server could theoretically recompute the entire hash chain from an altered record to the head.
   * *Operational Recommendation:* Periodically publish checkpoint hashes (e.g., daily head hash) to an immutable external ledger, write-once log service, or signed security digest.
2. **Key Rotation:**
   * Supabase Service Role keys and database passwords should be rotated biannually through the Render and Supabase environment dashboards.
