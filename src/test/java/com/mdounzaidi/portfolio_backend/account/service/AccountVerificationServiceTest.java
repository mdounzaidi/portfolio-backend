package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.TokenPurpose;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import com.mdounzaidi.portfolio_backend.account.exception.AccountStateException;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountVerificationServiceTest {

    @Mock
    private AccountTokenService tokenService;

    @Mock
    private AccountRepository accountRepository;

    private AccountVerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new AccountVerificationService(tokenService, accountRepository);
    }

    @Test
    void verifyEmail_shouldVerifyAndActivateAccount() {
        Account account = Account.builder()
                .username("testuser")
                .emailVerified(false)
                .active(false)
                .build();
        VerificationToken token = verificationToken(account);
        when(tokenService.findValidToken("raw-token", TokenPurpose.EMAIL_VERIFICATION))
                .thenReturn(token);

        verificationService.verifyEmail("raw-token");

        assertTrue(account.isEmailVerified());
        assertTrue(account.isActive());
        verify(accountRepository).save(account);
        verify(tokenService).markUsed(token);
    }

    @Test
    void verifyEmail_shouldThrowAccountStateException_whenAccountAlreadyVerified() {
        Account account = Account.builder()
                .username("testuser")
                .emailVerified(true)
                .active(true)
                .build();
        VerificationToken token = verificationToken(account);
        when(tokenService.findValidToken("raw-token", TokenPurpose.EMAIL_VERIFICATION))
                .thenReturn(token);

        assertThrows(
                AccountStateException.class,
                () -> verificationService.verifyEmail("raw-token")
        );

        verify(accountRepository, never()).save(account);
        verify(tokenService, never()).markUsed(token);
    }

    private VerificationToken verificationToken(Account account) {
        return VerificationToken.builder()
                .token("stored-hash")
                .account(account)
                .purpose(TokenPurpose.EMAIL_VERIFICATION)
                .expireAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }
}
