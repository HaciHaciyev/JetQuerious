package com.hadzhy.jetquerious.util;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserForm(
        UUID id,
        String email,
        String username,
        String password,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt) {}
