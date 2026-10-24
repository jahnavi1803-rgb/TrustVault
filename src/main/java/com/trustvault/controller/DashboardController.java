package com.trustvault.controller;

import com.trustvault.entity.AppUser;
import com.trustvault.entity.AuditLog;
import com.trustvault.entity.Document;
import com.trustvault.entity.Role;
import com.trustvault.security.CustomUserDetails;
import com.trustvault.service.DocumentService;
import com.trustvault.service.TamperEvidentAuditService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final DocumentService documentService;
    private final TamperEvidentAuditService auditService;

    public DashboardController(DocumentService documentService, TamperEvidentAuditService auditService) {
        this.documentService = documentService;
        this.auditService = auditService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        AppUser currentUser = userDetails.getUser();

        long ownedCount = documentService.countOwned(currentUser);
        long sharedCount = documentService.countShared(currentUser);
        long totalAccessible = (currentUser.getRole() == Role.ROLE_ADMIN)
                ? documentService.countTotal()
                : (ownedCount + sharedCount);

        List<Document> recentDocuments = (currentUser.getRole() == Role.ROLE_ADMIN)
                ? documentService.searchDocuments(currentUser, null, null, null, null).stream().limit(8).toList()
                : documentService.getUserOwnedDocuments(currentUser).stream().limit(8).toList();

        List<AuditLog> recentActivity = auditService.getRecentLogs().stream().limit(6).toList();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("ownedCount", ownedCount);
        model.addAttribute("sharedCount", sharedCount);
        model.addAttribute("totalAccessible", totalAccessible);
        model.addAttribute("recentDocuments", recentDocuments);
        model.addAttribute("recentActivity", recentActivity);

        return "dashboard/index";
    }
}
