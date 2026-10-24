package com.trustvault.controller;

import com.trustvault.entity.AppUser;
import com.trustvault.entity.Document;
import com.trustvault.entity.PermissionType;
import com.trustvault.security.CustomUserDetails;
import com.trustvault.service.DocumentService;
import com.trustvault.service.ShareService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
public class ShareController {

    private final ShareService shareService;
    private final DocumentService documentService;

    public ShareController(ShareService shareService, DocumentService documentService) {
        this.shareService = shareService;
        this.documentService = documentService;
    }

    @PostMapping("/documents/{id}/share")
    public String grantPermission(@PathVariable("id") UUID documentId,
                                  @RequestParam("username") String targetUsername,
                                  @RequestParam("permission") PermissionType permissionType,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            shareService.grantPermission(documentId, targetUsername, permissionType, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Access granted to user '" + targetUsername + "'.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/documents/" + documentId;
    }

    @PostMapping("/documents/{id}/share/revoke")
    public String revokePermission(@PathVariable("id") UUID documentId,
                                   @RequestParam("userId") Long targetUserId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            shareService.revokePermission(documentId, targetUserId, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Sharing permission revoked.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/documents/" + documentId;
    }

    @GetMapping("/shared-with-me")
    public String sharedDocuments(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        AppUser currentUser = userDetails.getUser();
        List<Document> sharedDocs = documentService.getDocumentsSharedWithUser(currentUser);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("documents", sharedDocs);

        return "documents/shared";
    }
}
