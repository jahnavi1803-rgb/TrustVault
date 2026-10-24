package com.trustvault.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        log.warn("Access denied: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/access-denied";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSize(MaxUploadSizeExceededException ex, Model model) {
        model.addAttribute("errorMessage", "The uploaded file exceeds the maximum permitted size of 25 MB.");
        return "error/general";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/general";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Unhandled application exception: ", ex);
        model.addAttribute("errorMessage", "An unexpected error occurred while processing your request. Please try again later.");
        return "error/general";
    }
}
