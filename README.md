# TrustVault — Enterprise Secure Document Management Platform

TrustVault is a confidential document management platform engineered with zero-trust security principles. It provides private document storage, granular role-based access control (RBAC), fine-grained document sharing (READ vs EDIT), anti-tampering cryptographic audit logging, and private cloud object storage integration with Supabase and Render.

---

## 1. System Architecture

```
                    +------------------------------------+
                    |       Client Browser (HTTPS)       |
                    | (Obsidian & Emerald Responsive UI) |
                    +-----------------+------------------+
                                      |
                     CSRF Token + Session Protection
                                      |
                                      v
+---------------------------------------------------------------------------------+
| Spring Boot 3.3 Application Tier (Java 21 LTS)                                 |
|                                                                                 |
|  +-------------------+  +--------------------+  +----------------------------+  |
|  |   Spring Security |  | Document Controller|  |    Tamper-Evident Audit    |  |
|  |   - BCrypt Hash   |  | - Upload Validation|  |    - SHA-256 Hash Chain    |  |
|  |   - RBAC & IDOR   |  | - READ vs EDIT ACL |  |    - Sequence Integrity    |  |
|  |   - Session Guard |  | - Safe Deletion    |  |    - Admin Verification    |  |
|  +-------------------+  +--------------------+  +----------------------------+  |
|                               |                                                 |
|          +--------------------+---------------------+                           |
|          |                                          |                           |
|          v                                          v                           |
|  +---------------------------+        +---------------------------+             |
|  | Supabase Storage Gateway  |        | Spring Data JPA Hibernate |             |
|  | - Private REST Client     |        | - Schema Migrations       |             |
|  | - Non-guessable UUID Path |        | - Transactional Atomicity |             |
|  | - Service Role Key Secret |        | - Index Optimization      |             |
|  +-------------+-------------+        +-------------+-------------+             |
+----------------|------------------------------------|---------------------------+
                 | (Server-to-Server HTTPS)           | (PostgreSQL Driver)
                 v                                    v
   +---------------------------+        +---------------------------+
   |  Supabase Object Storage  |        |    Supabase PostgreSQL    |
   | (Private Bucket: docs)    |        | (Users, Docs, ACL, Audit) |
   +---------------------------+        +---------------------------+
```

### Technology Stack
* **Runtime:** Java 21 LTS (Microsoft OpenJDK / Eclipse Temurin)
* **Backend Framework:** Spring Boot 3.3.5
* **Security:** Spring Security 6 (BCrypt cost factor 12, CSRF token validation, Session Fixation Migration)
* **Persistence:** Spring Data JPA / Hibernate
* **Database:** PostgreSQL (Supabase PostgreSQL / H2 test profile)
* **Storage:** Supabase Storage (Private bucket `trustvault-documents` via REST API)
* **Template Engine:** Thymeleaf + Thymeleaf Security 6 Dialect
* **Frontend Design:** Custom Emerald-Dark UI (`#07090e` black + `#10b981` emerald) with Tailwind CSS
* **File Inspection:** Apache Tika 2.9.2 (Magic bytes signature verification)
* **Hosting:** Render (Containerized Docker with dynamic `$PORT` binding)

---

## 2. Security Architecture & Threat Mitigations

| Threat Scenario | Vector | TrustVault Mitigation |
|---|---|---|
| **Insecure Direct Object Reference (IDOR)** | Attacker changes document ID in URL (`/documents/{uuid}`) to download or tamper with unauthorized records | Verified on every single server-side request against document ownership and `document_permissions` table. Unauthorized attempts trigger a security exception and log an audit event. |
| **Privilege Escalation** | Attacker tampers with registration request body to gain `ADMIN` privileges | Registration endpoint hardcodes the role assignment strictly to `ROLE_OWNER` or `ROLE_VIEWER`. Administrator accounts can only be provisioned via environment bootstrap. |
| **Direct Object Storage Exposure** | Publicly accessible bucket leaking private documents | Supabase bucket is configured as strictly **private**. All file downloads stream through the authenticated backend application; direct public URLs are never generated or exposed. |
| **Malicious File Uploads** | Web shells or executable files uploaded disguised as documents (`shell.php`, `bad.exe`) | Strict extension whitelisting, deep magic-byte inspection using Apache Tika, 25MB file size limits, and filename sanitization to eliminate path traversal characters (`../`). |
| **Tampered Audit Trails** | Malicious actor or DBA alters or deletes audit records | Cryptographic hash chaining: Every audit entry contains a SHA-256 hash derived from the previous entry's hash, sequence number, timestamp, actor, action, and payload. |

---

## 3. Cryptographic Hash-Chained Audit Trail

TrustVault employs a sequential hash chain for all security-critical operations:

$$\text{Hash}_i = \text{SHA256}(\text{Hash}_{i-1} \parallel \text{Sequence}_i \parallel \text{Timestamp}_i \parallel \text{Actor}_i \parallel \text{Action}_i \parallel \text{Resource}_i \parallel \text{Outcome}_i \parallel \text{Details}_i)$$

* **Genesis Hash:** `0000000000000000000000000000000000000000000000000000000000000000`
* **Contiguity:** Sequence numbers must be strictly consecutive ($1, 2, 3, \ldots$).
* **Integrity Verification:** The administrative console provides an automated verification tool (`/admin/audit/verify` and `GET /api/admin/audit/verify`) that traverses the chain from sequence 1 to $N$. If any byte in any historical record is modified or deleted, the calculated hash diverges immediately, pinpointing the exact sequence number that was compromised.

> [!NOTE]
> Cryptographic hash chaining guarantees **tamper evidence**, not tamper resistance. If an adversary with full database superuser privileges modifies historical records, they must recompute the entire subsequent chain. Publishing checkpoints to trusted external logs prevents undetected complete-history rewrites.

---

## 4. Role-Based Access Control Matrix

| Capability | ADMIN | OWNER | VIEWER |
|---|:---:|:---:|:---:|
| View Dashboard & Stats | Yes | Yes | Yes |
| Upload New Documents | Yes | Yes | No |
| Download Owned Documents | Yes | Yes | N/A |
| Download Shared Documents | Yes | If granted | If granted |
| Edit Document Metadata | Yes | Yes | If granted `EDIT` |
| Share Document (Grant ACL) | Yes | Yes | No |
| Revoke Document Sharing | Yes | Yes | No |
| Delete Documents | Yes | Yes | No |
| View Cryptographic Audit Log | Yes | No | No |
| Run Chain Integrity Check | Yes | No | No |
| Enable / Disable User Accounts | Yes | No | No |

---

## 5. Local Setup & Running

### Prerequisites
* Java 21 LTS installed (`java -version`)
* Git

### Step-by-Step Launch
1. Clone the repository:
   ```bash
   git clone https://github.com/jahnavi1803-rgb/TrustVault.git
   cd TrustVault
   ```
2. Copy environment variables:
   ```bash
   cp .env.example .env
   ```
3. Run the automated test suite:
   ```bash
   ./mvnw clean test
   ```
4. Start the application locally:
   ```bash
   ./mvnw spring-boot:run
   ```
5. Access TrustVault at `http://localhost:8080`.
   * **Default Initial Admin:** `admin` / `AdminVault2026!Secure`

---

## 6. Render Deployment Guide

1. Create a **New Web Service** on [Render](https://render.com).
2. Connect your GitHub repository `https://github.com/jahnavi1803-rgb/TrustVault.git`.
3. Select **Docker** as the Runtime environment.
4. Set the following environment variables in the Render Dashboard:

| Environment Variable | Description / Value |
|---|---|
| `PORT` | `10000` (or leave default, Render sets `$PORT`) |
| `SPRING_DATASOURCE_URL` | Supabase PostgreSQL JDBC connection string (`jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require`) |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Your Supabase database password |
| `SUPABASE_URL` | Your Supabase Project URL (`https://<project-ref>.supabase.co`) |
| `SUPABASE_SERVICE_ROLE_KEY` | Supabase secret service-role API key (server-side only) |
| `SUPABASE_STORAGE_BUCKET` | `trustvault-documents` |
| `INITIAL_ADMIN_USERNAME` | `admin` |
| `INITIAL_ADMIN_PASSWORD` | Strong password of your choice |

5. Click **Deploy Web Service**. Render builds the multi-stage Docker image, runs the test suite, and starts the container with live health-check probes at `/actuator/health`.
