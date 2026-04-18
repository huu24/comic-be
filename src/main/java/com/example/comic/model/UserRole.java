package com.example.comic.model;

public enum UserRole {
    GUEST,
    MEMBER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }

    public static UserRole from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Vai trò không hợp lệ. Chỉ chấp nhận MEMBER hoặc ADMIN.");
        }
        try {
            return UserRole.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Vai trò không hợp lệ. Chỉ chấp nhận MEMBER hoặc ADMIN.");
        }
    }
}
