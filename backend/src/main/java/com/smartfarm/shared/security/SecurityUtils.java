package com.smartfarm.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

/**
 * 从 SecurityContext 中提取当前用户信息
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> currentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Map) {
            return (Map<String, Object>) auth.getDetails();
        }
        return Map.of();
    }

    public static Long currentUserId() {
        return (Long) currentUserDetails().getOrDefault("userId", 0L);
    }

    public static Long currentTenantId() {
        return (Long) currentUserDetails().getOrDefault("tenantId", 0L);
    }
}
