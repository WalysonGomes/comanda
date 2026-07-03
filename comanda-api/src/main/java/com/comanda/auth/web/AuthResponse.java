package com.comanda.auth.web;

import com.comanda.auth.domain.User;

public record AuthResponse(String accessToken, UserSummary user) {

    public record UserSummary(Long id, String name, String email, Long tenantId) {
    }

    static AuthResponse of(User user, String accessToken) {
        return new AuthResponse(
                accessToken, new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getTenantId()));
    }
}
