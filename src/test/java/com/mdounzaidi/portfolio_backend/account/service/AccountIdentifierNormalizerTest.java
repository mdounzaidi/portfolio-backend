package com.mdounzaidi.portfolio_backend.account.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AccountIdentifierNormalizerTest {

    private final AccountIdentifierNormalizer normalizer = new AccountIdentifierNormalizer();

    @Test
    void username_shouldTrimAndLowercaseValue() {
        assertEquals("testuser", normalizer.username("  TestUser  "));
    }

    @Test
    void email_shouldTrimAndLowercaseValue() {
        assertEquals("test@example.com", normalizer.email("  TEST@EXAMPLE.COM  "));
    }

    @Test
    void normalize_shouldReturnNull_whenValueIsNull() {
        assertNull(normalizer.username(null));
        assertNull(normalizer.email(null));
    }
}
