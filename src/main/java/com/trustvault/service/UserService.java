package com.trustvault.service;

import com.trustvault.dto.PasswordChangeDto;
import com.trustvault.dto.UserProfileDto;
import com.trustvault.dto.UserRegistrationDto;
import com.trustvault.entity.AppUser;
import com.trustvault.entity.AuditOutcome;
import com.trustvault.entity.Role;
import com.trustvault.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TamperEvidentAuditService auditService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TamperEvidentAuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public AppUser registerUser(UserRegistrationDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        if (userRepository.existsByUsername(dto.getUsername().trim())) {
            throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken.");
        }

        if (userRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already registered.");
        }

        // Security: Prevent self-assigned administrator privilege
        Role assignedRole = Role.ROLE_OWNER;
        if ("VIEWER".equalsIgnoreCase(dto.getRoleChoice())) {
            assignedRole = Role.ROLE_VIEWER;
        }

        AppUser user = new AppUser(
                dto.getUsername().trim(),
                dto.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getFullName().trim(),
                assignedRole
        );

        AppUser saved = userRepository.save(user);

        auditService.recordEvent(
                saved.getId(),
                saved.getUsername(),
                saved.getRole().name(),
                "USER_REGISTERED",
                String.valueOf(saved.getId()),
                "USER",
                AuditOutcome.SUCCESS,
                "New user registered with role " + saved.getRole()
        );

        return saved;
    }

    @Transactional
    public void changePassword(String username, PasswordChangeDto dto) {
        AppUser user = getUserByUsername(username);

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            auditService.recordEvent(
                    user.getId(),
                    user.getUsername(),
                    user.getRole().name(),
                    "PASSWORD_CHANGE_ATTEMPT",
                    String.valueOf(user.getId()),
                    "USER",
                    AuditOutcome.DENIED,
                    "Invalid current password provided during password change."
            );
            throw new IllegalArgumentException("Current password does not match our records.");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New passwords do not match.");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password cannot be the same as current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        auditService.recordEvent(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                "PASSWORD_CHANGED",
                String.valueOf(user.getId()),
                "USER",
                AuditOutcome.SUCCESS,
                "Password changed successfully."
        );
    }

    @Transactional
    public void updateProfile(String username, UserProfileDto dto) {
        AppUser user = getUserByUsername(username);

        String newEmail = dto.getEmail().trim().toLowerCase();
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email address is already in use by another account.");
        }

        user.setFullName(dto.getFullName().trim());
        user.setEmail(newEmail);
        userRepository.save(user);

        auditService.recordEvent(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                "PROFILE_UPDATED",
                String.valueOf(user.getId()),
                "USER",
                AuditOutcome.SUCCESS,
                "User profile updated."
        );
    }

    @Transactional(readOnly = true)
    public AppUser getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<AppUser> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void updateUserStatus(Long userId, boolean enabled, String adminUsername) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setEnabled(enabled);
        userRepository.save(user);

        auditService.recordEvent(
                null,
                adminUsername,
                Role.ROLE_ADMIN.name(),
                enabled ? "ADMIN_ENABLE_USER" : "ADMIN_DISABLE_USER",
                String.valueOf(userId),
                "USER",
                AuditOutcome.SUCCESS,
                "User status changed to enabled=" + enabled + " for user " + user.getUsername()
        );
    }

    @Transactional
    public void updateUserRole(Long userId, Role newRole, String adminUsername) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);

        auditService.recordEvent(
                null,
                adminUsername,
                Role.ROLE_ADMIN.name(),
                "ADMIN_CHANGE_USER_ROLE",
                String.valueOf(userId),
                "USER",
                AuditOutcome.SUCCESS,
                "Changed role for " + user.getUsername() + " from " + oldRole + " to " + newRole
        );
    }
}
