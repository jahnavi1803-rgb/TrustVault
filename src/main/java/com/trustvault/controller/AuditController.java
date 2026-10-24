package com.trustvault.controller;

import com.trustvault.dto.AuditVerificationResult;
import com.trustvault.entity.AuditLog;
import com.trustvault.security.CustomUserDetails;
import com.trustvault.service.TamperEvidentAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AuditController {

    private final TamperEvidentAuditService auditService;

    public AuditController(TamperEvidentAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/admin/audit")
    public String auditLogDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<AuditLog> logs = auditService.getAllLogs();
        AuditVerificationResult integrityResult = auditService.verifyChainIntegrity();

        model.addAttribute("currentUser", userDetails.getUser());
        model.addAttribute("logs", logs);
        model.addAttribute("integrity", integrityResult);

        return "admin/audit";
    }

    @PostMapping("/admin/audit/verify")
    public String runIntegrityCheck(RedirectAttributes redirectAttributes) {
        AuditVerificationResult result = auditService.verifyChainIntegrity();
        if (result.isValid()) {
            redirectAttributes.addFlashAttribute("verificationSuccess",
                    "Cryptographic Audit Verification Passed: All " + result.getTotalRecords() +
                    " log entries are strictly chained and tamper-free.");
        } else {
            redirectAttributes.addFlashAttribute("verificationFailure",
                    "SECURITY ALERT: Tampering or corruption detected! Sequence #" +
                    result.getBrokenAtSequence() + " failed verification: " + result.getFailureReason());
        }
        return "redirect:/admin/audit";
    }

    @GetMapping("/api/admin/audit/verify")
    @ResponseBody
    public ResponseEntity<AuditVerificationResult> apiVerifyIntegrity() {
        return ResponseEntity.ok(auditService.verifyChainIntegrity());
    }
}
