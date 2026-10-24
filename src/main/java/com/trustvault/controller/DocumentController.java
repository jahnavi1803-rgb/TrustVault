package com.trustvault.controller;

import com.trustvault.entity.AppUser;
import com.trustvault.entity.Category;
import com.trustvault.entity.Document;
import com.trustvault.entity.PermissionType;
import com.trustvault.repository.CategoryRepository;
import com.trustvault.security.CustomUserDetails;
import com.trustvault.service.DocumentService;
import com.trustvault.service.ShareService;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final ShareService shareService;
    private final CategoryRepository categoryRepository;

    public DocumentController(DocumentService documentService,
                              ShareService shareService,
                              CategoryRepository categoryRepository) {
        this.documentService = documentService;
        this.shareService = shareService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String listDocuments(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "category", required = false) Long categoryId,
                                @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                Model model) {
        AppUser currentUser = userDetails.getUser();

        var startInstant = (fromDate != null) ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        var endInstant = (toDate != null) ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;

        List<Document> documents = documentService.searchDocuments(currentUser, search, categoryId, startInstant, endInstant);
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("documents", documents);
        model.addAttribute("categories", categories);
        model.addAttribute("searchQuery", search);
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "documents/index";
    }

    @GetMapping("/upload")
    public String uploadPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userDetails.getUser());
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        return "documents/upload";
    }

    @PostMapping("/upload")
    public String uploadDocument(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "title", required = false) String title,
                                 @RequestParam(value = "categoryId", required = false) Long categoryId,
                                 @RequestParam(value = "description", required = false) String description,
                                 RedirectAttributes redirectAttributes) {
        try {
            Document doc = documentService.uploadDocument(file, title, categoryId, description, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Document '" + doc.getTitle() + "' uploaded securely.");
            return "redirect:/documents";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/documents/upload";
        }
    }

    @GetMapping("/{id}")
    public String viewDocumentDetails(@PathVariable("id") UUID documentId,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      Model model) {
        AppUser currentUser = userDetails.getUser();
        Document document = documentService.getAuthorizedDocument(documentId, currentUser, PermissionType.READ);

        boolean isOwner = document.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == com.trustvault.entity.Role.ROLE_ADMIN;

        model.addAttribute("document", document);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());

        if (isOwner || isAdmin) {
            model.addAttribute("permissions", shareService.getDocumentPermissions(documentId, currentUser));
        }

        return "documents/view";
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable("id") UUID documentId,
                                                     @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {
        Document document = documentService.getAuthorizedDocument(documentId, userDetails.getUser(), PermissionType.READ);
        Resource resource = documentService.downloadDocument(documentId, userDetails.getUser());

        String encodedFilename = URLEncoder.encode(document.getOriginalFilename(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFilename() + "\"; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @PostMapping("/{id}/edit")
    public String editDocument(@PathVariable("id") UUID documentId,
                               @RequestParam("title") String title,
                               @RequestParam(value = "categoryId", required = false) Long categoryId,
                               @RequestParam(value = "description", required = false) String description,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            documentService.updateDocumentMetadata(documentId, title, categoryId, description, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Document updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/documents/" + documentId;
    }

    @PostMapping("/{id}/delete")
    public String deleteDocument(@PathVariable("id") UUID documentId,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            documentService.deleteDocument(documentId, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Document deleted successfully.");
            return "redirect:/documents";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/documents/" + documentId;
        }
    }
}
