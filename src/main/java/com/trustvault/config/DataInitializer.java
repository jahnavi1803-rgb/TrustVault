package com.trustvault.config;

import com.trustvault.entity.AppUser;
import com.trustvault.entity.AuditOutcome;
import com.trustvault.entity.Category;
import com.trustvault.entity.Role;
import com.trustvault.repository.CategoryRepository;
import com.trustvault.repository.UserRepository;
import com.trustvault.service.TamperEvidentAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final TamperEvidentAuditService auditService;

    @Value("${trustvault.security.initial-admin.enabled:true}")
    private boolean adminEnabled;

    @Value("${trustvault.security.initial-admin.username:admin}")
    private String adminUsername;

    @Value("${trustvault.security.initial-admin.email:admin@trustvault.local}")
    private String adminEmail;

    @Value("${trustvault.security.initial-admin.password:AdminVault2026!Secure}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           PasswordEncoder passwordEncoder,
                           TamperEvidentAuditService auditService) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedCategories();
        seedInitialAdministrator();
    }

    private void seedCategories() {
        List<String[]> defaultCategories = List.of(
                new String[]{"Confidential", "High security and strictly restricted documents"},
                new String[]{"Financial", "Invoices, payroll, audits, and accounting records"},
                new String[]{"Legal", "Contracts, NDAs, licenses, and regulatory compliance"},
                new String[]{"Engineering", "Specifications, architecture diagrams, and security audits"},
                new String[]{"General", "General purpose notes and internal documentation"}
        );

        for (String[] cat : defaultCategories) {
            if (categoryRepository.findByNameIgnoreCase(cat[0]).isEmpty()) {
                categoryRepository.save(new Category(cat[0], cat[1]));
                log.info("Initialized category: {}", cat[0]);
            }
        }
    }

    private void seedInitialAdministrator() {
        if (!adminEnabled) {
            return;
        }

        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            AppUser admin = new AppUser(
                    adminUsername,
                    adminEmail,
                    passwordEncoder.encode(adminPassword),
                    "System Administrator",
                    Role.ROLE_ADMIN
            );
            AppUser saved = userRepository.save(admin);
            log.info("Bootstrap: Initial administrator account '{}' provisioned successfully.", adminUsername);

            auditService.recordEvent(
                    saved.getId(),
                    saved.getUsername(),
                    saved.getRole().name(),
                    "INITIAL_ADMIN_PROVISIONED",
                    String.valueOf(saved.getId()),
                    "SYSTEM",
                    AuditOutcome.SUCCESS,
                    "Initial administrator account created via secure bootstrap."
            );
        }
    }
}
