package com.mdounzaidi.portfolio_backend.account.mapper;

import com.mdounzaidi.portfolio_backend.account.dto.AccountResponse;
import com.mdounzaidi.portfolio_backend.account.entity.Account;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountMapperTest {

    private final AccountMapper accountMapper = new AccountMapper();

    @Test
    void buildAccountResponse_shouldMapPublicAccountFields() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();
        Account account = Account.builder()
                .id(10L)
                .firstName("Test")
                .lastName("User")
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .emailVerified(true)
                .build();
        account.setCreatedAt(createdAt);
        account.setUpdatedAt(updatedAt);

        AccountResponse response = accountMapper.buildAccountResponse(account);

        assertEquals(10L, response.getId());
        assertEquals("Test", response.getFirstName());
        assertEquals("User", response.getLastName());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals(updatedAt, response.getUpdatedAt());
        assertTrue(response.isEmailVerified());
    }
}
