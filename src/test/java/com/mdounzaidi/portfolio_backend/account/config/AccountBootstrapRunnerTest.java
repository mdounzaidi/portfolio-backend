package com.mdounzaidi.portfolio_backend.account.config;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.AccountRole;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.account.service.AccountIdentifierNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountBootstrapRunnerTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountIdentifierNormalizer identifierNormalizer;

    @BeforeEach
    void setUp() {
        identifierNormalizer = new AccountIdentifierNormalizer();
    }

    @Test
    void run_shouldSkipBootstrap_whenDisabled() {
        AccountBootstrapRunner runner = new AccountBootstrapRunner(
                properties(false),
                accountRepository,
                passwordEncoder,
                identifierNormalizer
        );

        runner.run(null);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void run_shouldCreateBootstrapSuperAdmin_whenEnabledAndAccountDoesNotExist() {
        AccountBootstrapRunner runner = new AccountBootstrapRunner(
                properties(true),
                accountRepository,
                passwordEncoder,
                identifierNormalizer
        );
        when(accountRepository.findByUsernameOrEmail("adminuser", "admin@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongPass@123")).thenReturn("encoded-password");

        runner.run(null);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account account = accountCaptor.getValue();

        assertEquals("adminuser", account.getUsername());
        assertEquals("admin@example.com", account.getEmail());
        assertEquals("encoded-password", account.getPassword());
        assertTrue(account.isActive());
        assertTrue(account.isEmailVerified());
        assertTrue(account.getAccountRole().contains(AccountRole.ROLE_USER));
        assertTrue(account.getAccountRole().contains(AccountRole.ROLE_WRITER));
        assertTrue(account.getAccountRole().contains(AccountRole.ROLE_ADMIN));
        assertTrue(account.getAccountRole().contains(AccountRole.ROLE_SUPERADMIN));
    }

    private AccountBootstrapProperties properties(boolean enabled) {
        return new AccountBootstrapProperties(
                enabled,
                "Local",
                "Admin",
                "AdminUser",
                "ADMIN@EXAMPLE.COM",
                "StrongPass@123",
                AccountRole.ROLE_SUPERADMIN
        );
    }
}
