package com.mdounzaidi.portfolio_backend.account.config;

import com.mdounzaidi.portfolio_backend.account.entity.AccountRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.account.bootstrap")
public record AccountBootstrapProperties(
        boolean enabled,
        String firstName,
        String lastName,
        String username,
        String email,
        String password,
        AccountRole role
) {

    public AccountRole roleOrDefault() {
        return role == null ? AccountRole.ROLE_SUPERADMIN : role;
    }
}
