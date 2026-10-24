# TrustVault — AI Interaction & Engineering Log (PROMPTS.md)

This log records the genuine prompts, engineering decisions, technical challenges, and iterative problem corrections during the development of TrustVault in Google Antigravity.

---

## Session 1: Project Inspection & Architecture Assessment

### User Prompt
> "Act as a senior full-stack developer, cybersecurity engineer, database architect, and deployment specialist. Work with me in Google Antigravity to complete my existing TrustVault project from its current state to a fully functional, tested, documented, and deployable application..."

### Action Taken
1. Cloned remote repository from `https://github.com/jahnavi1803-rgb/TrustVault.git`.
2. Inspected repository contents and Git history:
   * Remote repository was freshly initialized with 0 commits and no pre-existing code.
3. Audited local developer environment:
   * Node.js v24.22.0 and Python 3.11.8 detected.
   * Modern Java 21 was not installed on PATH (only a legacy JRE 8 stub existed).
4. Produced comprehensive Phase 1 & 2 inspection report and architecture artifact (`trustvault_inspection_and_plan.md`).
5. Recommended retaining the requested enterprise target stack: Java 21 LTS, Spring Boot 3.3.5, Spring Security 6, PostgreSQL, Supabase Storage, Render, and Thymeleaf with an obsidian/emerald UI.
6. User approved the implementation plan.

---

## Session 2: Technical Challenges & Problem Corrections

### Challenge 1: JDK 21 Installation on Windows Without Administrator Prompts
* **Problem:** Running `winget install EclipseAdoptium.Temurin.21.JDK` attempted to launch an MSI installer requiring Windows UAC administrator elevation, which cannot proceed in an automated, non-interactive environment.
* **Correction:** Switched to Microsoft OpenJDK 21 LTS portable zip distribution (`microsoft-jdk-21.0.5-windows-x64.zip`). Programmatically extracted the archive directly into user-space directory `C:\Users\tejac\jdk-21\jdk-21.0.5+11`. This required zero elevation, zero UAC prompts, and gave an immediate, working Java 21 runtime.

### Challenge 2: Maven Wrapper Environment Variable Conflict
* **Problem:** The machine had a stale global system environment variable `JAVA_HOME=C:\Program Files\Java\jdk-21` pointing to a non-existent path. When `mvnw.cmd` executed `if not defined JAVA_HOME`, the check evaluated to false, causing execution to fail with "The system cannot find the path specified."
* **Correction:** Updated `mvnw.cmd` to proactively check if `C:\Users\tejac\jdk-21\jdk-21.0.5+11\bin\java.exe` exists and override `JAVA_HOME` specifically for the build process.

### Challenge 3: Windows Batch Argument Escaping Trailing Backslash
* **Problem:** In Windows command prompt batch files, `%~dp0` evaluates to the current directory with a trailing backslash (e.g., `C:\Users\tejac\OneDrive\Documents\trustvault\`). When passed inside quotes to Java `-Dmaven.multiModuleProjectDirectory="%~dp0"`, the trailing backslash escaped the closing quote (`\"`), corrupting JVM argument parsing.
* **Correction:** Added batch substring logic to strip trailing slashes:
  ```cmd
  @set "MAVEN_PROJECTBASEDIR=%~dp0"
  @if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"
  ```

---

## Session 3: Security & Architectural Implementation Details

### 1. Zero-Trust Access Control (RBAC & Sharing)
* Implemented distinct permission roles:
  * `ADMIN`: Global oversight, user management, tamper verification.
  * `OWNER`: Full ownership over uploaded documents and sharing ACLs.
  * `VIEWER`: Strictly read-only access to documents shared directly with them.
* Explicit difference enforced between `READ` and `EDIT` grants:
  * `READ`: Access metadata and download encrypted files.
  * `EDIT`: Can update title, category, and description. Cannot delete documents or grant permissions to third parties.
* Immediate revocation: When an owner revokes access, subsequent download/edit attempts are blocked immediately.

### 2. Tamper-Evident Cryptographic Audit Logging
* Implemented sequential SHA-256 hash chaining:
  $$H_i = \text{SHA256}(H_{i-1} \parallel \text{Seq}_i \parallel \text{Timestamp}_i \parallel \text{Actor}_i \parallel \text{Action}_i \parallel \text{Resource}_i \parallel \text{Outcome}_i \parallel \text{Details}_i)$$
* Implemented administrative verification engine (`/admin/audit/verify` and `GET /api/admin/audit/verify`) that iterates through the entire audit log, verifying:
  1. Sequence contiguity (no gaps or dropped events).
  2. Pointer consistency ($H_{i-1}$ matches entry $i-1$).
  3. Payload hash integrity (recomputed hash matches stored hash).

### 4. Git Push & Remote Synchronization
* Remote repository: `https://github.com/jahnavi1803-rgb/TrustVault.git`
* Branch: `main`
* Commit SHA: `83900fa847027d9c26e4d6cd19c925a9b0e49f07`
* Status: Successfully pushed and synchronized.
* All 73 application files, Dockerfile, render.yaml, tests, and documentation are live on GitHub.

The test suite executed with the following results:
```
[INFO] Running com.trustvault.security.AuthenticationSecurityTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.trustvault.security.DocumentAccessControlTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.trustvault.security.RbacAuthorizationTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.trustvault.service.FileUploadSecurityTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.trustvault.service.TamperEvidentAuditTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.trustvault.TrustVaultApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS (Total time: 31.901 s)
```
Key verifications:
1. Unauthenticated access redirection to `/login`.
2. Prevention of self-assigned `ADMIN` role during user registration.
3. Strict RBAC enforcement on `/admin/audit` and `/admin/users`.
4. Cross-user document access rejection (IDOR mitigation).
5. Granular `READ` vs `EDIT` permission boundaries and enforcement.
6. Immediate access revocation upon owner or administrator action.
7. SHA-256 cryptographic hash chain integrity and tamper detection when records are modified or deleted.
8. File upload validation (executable blocking, oversized rejection, path traversal sanitization).

