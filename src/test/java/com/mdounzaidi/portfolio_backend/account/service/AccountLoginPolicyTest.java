package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountLoginPolicyTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountLoginPolicy loginPolicy;

    @BeforeEach
    void setUp() {
        loginPolicy = new AccountLoginPolicy(
                accountRepository,
                new AccountIdentifierNormalizer()
        );
    }

    @Test
    void recordSuccessfulLogin_shouldResetFailedAttemptsAndSetLastLoginAt() {
        Account account = Account.builder()
                .username("testuser")
                .failedLoginAttempts(3)
                .accountNonLocked(false)
                .lockedAt(LocalDateTime.now())
                .build();

        loginPolicy.recordSuccessfulLogin(account);

        assertEquals(0, account.getFailedLoginAttempts());
        assertTrue(account.isAccountNonLocked());
        assertNull(account.getLockedAt());
        assertNotNull(account.getLastLoginAt());
        verify(accountRepository).save(account);
    }

    @Test
    void recordFailedLogin_shouldIncrementAttemptsAndLockAtLimit() {
        Account account = Account.builder()
                .username("testuser")
                .accountNonLocked(true)
                .failedLoginAttempts(4)
                .build();
        when(accountRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(account));

        loginPolicy.recordFailedLogin(" TestUser ");

        assertEquals(5, account.getFailedLoginAttempts());
        assertFalse(account.isAccountNonLocked());
        assertNotNull(account.getLockedAt());
        verify(accountRepository).save(account);
    }

    @Test
    void recordFailedLogin_shouldNotSave_whenAccountDoesNotExist() {
        when(accountRepository.findByUsernameOrEmail("missing", "missing"))
                .thenReturn(Optional.empty());

        loginPolicy.recordFailedLogin("missing");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void unlockIfLockExpired_shouldUnlockAccountAfterLockDuration() {
        Account account = Account.builder()
                .username("testuser")
                .accountNonLocked(false)
                .failedLoginAttempts(5)
                .lockedAt(LocalDateTime.now().minusMinutes(20))
                .build();
        when(accountRepository.save(account)).thenReturn(account);

        Account result = loginPolicy.unlockIfLockExpired(account);

        assertSame(account, result);
        assertTrue(account.isAccountNonLocked());
        assertEquals(0, account.getFailedLoginAttempts());
        assertNull(account.getLockedAt());
        verify(accountRepository).save(account);
    }
}
