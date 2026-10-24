package com.trustvault.controller;

import com.trustvault.dto.PasswordChangeDto;
import com.trustvault.dto.UserProfileDto;
import com.trustvault.entity.AppUser;
import com.trustvault.security.CustomUserDetails;
import com.trustvault.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String profilePage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        AppUser user = userService.getUserByUsername(userDetails.getUsername());

        if (!model.containsAttribute("profileDto")) {
            model.addAttribute("profileDto", new UserProfileDto(user.getFullName(), user.getEmail()));
        }
        if (!model.containsAttribute("passwordDto")) {
            model.addAttribute("passwordDto", new PasswordChangeDto());
        }

        model.addAttribute("currentUser", user);
        return "profile/index";
    }

    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute("profileDto") UserProfileDto profileDto,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileDto", bindingResult);
            redirectAttributes.addFlashAttribute("profileDto", profileDto);
            return "redirect:/profile";
        }

        try {
            userService.updateProfile(userDetails.getUsername(), profileDto);
            redirectAttributes.addFlashAttribute("successMessage", "Profile information updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordDto") PasswordChangeDto passwordDto,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.passwordDto", bindingResult);
            redirectAttributes.addFlashAttribute("passwordDto", passwordDto);
            return "redirect:/profile";
        }

        try {
            userService.changePassword(userDetails.getUsername(), passwordDto);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("passwordErrorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }
}
