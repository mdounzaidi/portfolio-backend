package com.mdounzaidi.portfolio_backend.account.service;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AccountIdentifierNormalizer {

    public String username(String username) {
        return normalize(username);
    }

    public String email(String email) {
        return normalize(email);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
