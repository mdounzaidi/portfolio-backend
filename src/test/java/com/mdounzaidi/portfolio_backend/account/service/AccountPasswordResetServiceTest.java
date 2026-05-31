package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.dto.CredentialRequest;
import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.ResetPassDetails;
import com.mdounzaidi.portfolio_backend.account.entity.TokenPurpose;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import com.mdounzaidi.portfolio_backend.account.exception.InvalidTokenException;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.account.repository.ResetPassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountPasswordResetServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMailService mailService;

    @Mock
    private ResetPassRepository resetPassRepository;

    @Mock
    private AccountTokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountPasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new AccountPasswordResetService(
                accountRepository,
                mailService,
                resetPassRepository,
                tokenService,
                passwordEncoder,
                new AccountIdentifierNormalizer()
        );
    }

    @Test
    void requestPasswordReset_shouldReturnGenericResponse_whenAccountDoesNotExist() {
        when(accountRepository.findByUsername("missinguser")).thenReturn(Optional.empty());

        String response = passwordResetService.requestPasswordReset(" MissingUser ");

        assertEquals("If an account exists, a password reset email will be sent.", response);
        verify(tokenService, never()).createToken(any(), any(Integer.class), any());
        verify(mailService, never()).sendPasswordResetMail(any(), any());
    }

    @Test
    void requestPasswordReset_shouldRevokeOldRequestsAndCreateNewResetRequest() {
        Account account = Account.builder()
                .username("testuser")
                .email("test@example.com")
                .build();
        VerificationToken oldToken = verificationToken(account, TokenPurpose.PASSWORD_RESET);
        ResetPassDetails oldRequest = ResetPassDetails.builder()
                .account(account)
                .verificationToken(oldToken)
                .active(true)
                .build();
        VerificationToken newToken = verificationToken(account, TokenPurpose.PASSWORD_RESET);
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
        when(resetPassRepository.findByAccountAndActiveTrue(account)).thenReturn(List.of(oldRequest));
        when(tokenService.createToken(account, 60 * 60, TokenPurpose.PASSWORD_RESET))
                .thenReturn(new GeneratedToken("raw-reset-token", newToken));

        String response = passwordResetService.requestPasswordReset(" TestUser ");

        assertEquals("If an account exists, a password reset email will be sent.", response);
        assertFalse(oldRequest.isActive());
        verify(tokenService).markRevoked(oldToken);
        verify(resetPassRepository).saveAll(List.of(oldRequest));
        verify(resetPassRepository).save(any(ResetPassDetails.class));
        verify(mailService).sendPasswordResetMail("test@example.com", "raw-reset-token");
    }

    @Test
    void resetPassword_shouldUpdatePasswordAndUseToken_whenRequestIsValid() {
        Account account = Account.builder()
                .username("testuser")
                .password("old-password")
                .build();
        VerificationToken token = verificationToken(account, TokenPurpose.PASSWORD_RESET);
        ResetPassDetails resetRequest = ResetPassDetails.builder()
                .account(account)
                .verificationToken(token)
                .active(true)
                .build();
        when(tokenService.findValidToken("raw-token", TokenPurpose.PASSWORD_RESET))
                .thenReturn(token);
        when(resetPassRepository.findByVerificationToken(token)).thenReturn(Optional.of(resetRequest));
        when(passwordEncoder.encode("StrongPass@123")).thenReturn("new-encoded-password");

        String response = passwordResetService.resetPassword(
                new CredentialRequest(" TestUser ", "StrongPass@123"),
                "raw-token"
        );

        assertEquals("Password Reset Done", response);
        assertFalse(resetRequest.isActive());
        assertEquals("new-encoded-password", account.getPassword());
        verify(resetPassRepository).save(resetRequest);
        verify(tokenService).markUsed(token);
        verify(accountRepository).save(account);
    }

    @Test
    void resetPassword_shouldThrowInvalidTokenException_whenResetDetailsAreMissing() {
        VerificationToken token = verificationToken(Account.builder().username("testuser").build(), TokenPurpose.PASSWORD_RESET);
        when(tokenService.findValidToken("raw-token", TokenPurpose.PASSWORD_RESET))
                .thenReturn(token);
        when(resetPassRepository.findByVerificationToken(token)).thenReturn(Optional.empty());

        assertThrows(
                InvalidTokenException.class,
                () -> passwordResetService.resetPassword(
                        new CredentialRequest("testuser", "StrongPass@123"),
                        "raw-token"
                )
        );
    }

    @Test
    void resetPassword_shouldThrowInvalidTokenException_whenUsernameDoesNotMatchTokenAccount() {
        Account account = Account.builder().username("testuser").build();
        VerificationToken token = verificationToken(account, TokenPurpose.PASSWORD_RESET);
        ResetPassDetails resetRequest = ResetPassDetails.builder()
                .account(account)
                .verificationToken(token)
                .active(true)
                .build();
        when(tokenService.findValidToken("raw-token", TokenPurpose.PASSWORD_RESET))
                .thenReturn(token);
        when(resetPassRepository.findByVerificationToken(token)).thenReturn(Optional.of(resetRequest));

        assertThrows(
                InvalidTokenException.class,
                () -> passwordResetService.resetPassword(
                        new CredentialRequest("otheruser", "StrongPass@123"),
                        "raw-token"
                )
        );

        verify(passwordEncoder, never()).encode(any());
        verify(accountRepository, never()).save(account);
        verify(tokenService, never()).markUsed(token);
    }

    @Test
    void requestPasswordReset_shouldSaveNewResetRequestWithGeneratedToken() {
        Account account = Account.builder()
                .username("testuser")
                .email("test@example.com")
                .build();
        VerificationToken newToken = verificationToken(account, TokenPurpose.PASSWORD_RESET);
        ArgumentCaptor<ResetPassDetails> resetRequestCaptor = ArgumentCaptor.forClass(ResetPassDetails.class);
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
        when(resetPassRepository.findByAccountAndActiveTrue(account)).thenReturn(List.of());
        when(tokenService.createToken(account, 60 * 60, TokenPurpose.PASSWORD_RESET))
                .thenReturn(new GeneratedToken("raw-reset-token", newToken));

        passwordResetService.requestPasswordReset("testuser");

        verify(resetPassRepository).save(resetRequestCaptor.capture());
        ResetPassDetails savedRequest = resetRequestCaptor.getValue();
        assertSame(account, savedRequest.getAccount());
        assertSame(newToken, savedRequest.getVerificationToken());
    }

    private VerificationToken verificationToken(Account account, TokenPurpose purpose) {
        return VerificationToken.builder()
                .token("stored-hash")
                .account(account)
                .purpose(purpose)
                .expireAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }
}
