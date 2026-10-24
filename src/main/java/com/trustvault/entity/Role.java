package com.trustvault.entity;

public enum Role {
    ROLE_ADMIN,
    ROLE_OWNER,
    ROLE_VIEWER;

    public String getDisplayName() {
        return switch (this) {
            case ROLE_ADMIN -> "Administrator";
            case ROLE_OWNER -> "Document Owner";
            case ROLE_VIEWER -> "Viewer";
        };
    }
}
