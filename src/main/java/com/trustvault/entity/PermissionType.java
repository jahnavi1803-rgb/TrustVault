package com.trustvault.entity;

public enum PermissionType {
    READ,
    EDIT;

    public String getDescription() {
        return switch (this) {
            case READ -> "Can view and download document";
            case EDIT -> "Can edit document details and upload new version (cannot delete or re-share)";
        };
    }
}
