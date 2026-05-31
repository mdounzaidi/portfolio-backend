package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.TokenPurpose;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import com.mdounzaidi.portfolio_backend.account.exception.ExpiredTokenException;
import com.mdounzaidi.portfolio_backend.account.exception.InvalidTokenException;
import com.mdounzaidi.portfolio_backend.account.repository.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountTokenServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    private AccountTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new AccountTokenService(tokenRepository);
    }

    @Test
    void createToken_shouldStoreHashedTokenAndReturnRawToken() {
        Account account = Account.builder().username("testuser").build();
        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        when(tokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GeneratedToken generatedToken = tokenService.createToken(
                account,
                300,
                TokenPurpose.EMAIL_VERIFICATION
        );

        verify(tokenRepository).save(tokenCaptor.capture());
        VerificationToken savedToken = tokenCaptor.getValue();

        assertNotNull(generatedToken.rawToken());
        assertSame(savedToken, generatedToken.verificationToken());
        assertNotEquals(generatedToken.rawToken(), savedToken.getToken());
        assertTrue(savedToken.getToken().matches("[0-9a-f]{64}"));
        assertEquals(account, savedToken.getAccount());
        assertEquals(TokenPurpose.EMAIL_VERIFICATION, savedToken.getPurpose());
        assertTrue(savedToken.getExpireAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void findToken_shouldReturnToken_whenRawTokenAndPurposeMatch() {
        String rawToken = "raw-token";
        VerificationToken storedToken = token(TokenPurpose.PASSWORD_RESET);
        when(tokenRepository.findByToken(hashToken(rawToken))).thenReturn(Optional.of(storedToken));

        VerificationToken result = tokenService.findToken(rawToken, TokenPurpose.PASSWORD_RESET);

        assertSame(storedToken, result);
    }

    @Test
    void findToken_shouldThrowInvalidTokenException_whenTokenDoesNotExist() {
        String rawToken = "missing-token";
        when(tokenRepository.findByToken(hashToken(rawToken))).thenReturn(Optional.empty());

        assertThrows(
                InvalidTokenException.class,
                () -> tokenService.findToken(rawToken, TokenPurpose.PASSWORD_RESET)
        );
    }

    @Test
    void findToken_shouldThrowInvalidTokenException_whenPurposeDoesNotMatch() {
        String rawToken = "raw-token";
        VerificationToken storedToken = token(TokenPurpose.EMAIL_VERIFICATION);
        when(tokenRepository.findByToken(hashToken(rawToken))).thenReturn(Optional.of(storedToken));

        assertThrows(
                InvalidTokenException.class,
                () -> tokenService.findToken(rawToken, TokenPurpose.PASSWORD_RESET)
        );
    }

    @Test
    void findValidToken_shouldThrowExpiredTokenException_whenTokenIsExpired() {
        String rawToken = "expired-token";
        VerificationToken storedToken = token(TokenPurpose.EMAIL_VERIFICATION);
        storedToken.setExpireAt(LocalDateTime.now().minusSeconds(1));
        when(tokenRepository.findByToken(hashToken(rawToken))).thenReturn(Optional.of(storedToken));

        assertThrows(
                ExpiredTokenException.class,
                () -> tokenService.findValidToken(rawToken, TokenPurpose.EMAIL_VERIFICATION)
        );
    }

    @Test
    void findValidToken_shouldThrowExpiredTokenException_whenTokenIsUsed() {
        String rawToken = "used-token";
        VerificationToken storedToken = token(TokenPurpose.EMAIL_VERIFICATION);
        storedToken.setUsedAt(LocalDateTime.now());
        when(tokenRepository.findByToken(hashToken(rawToken))).thenReturn(Optional.of(storedToken));

        assertThrows(
                ExpiredTokenException.class,
                () -> tokenService.findValidToken(rawToken, TokenPurpose.EMAIL_VERIFICATION)
        );
    }

    @Test
    void findValidToken_shouldThrowExpiredTokenException_whenTokenIsRevoked() {
        String rawToken = "revoked-token";
        VerificationToken storedToken = token(TokenPurpose.EMAIL_VERIFICATION);
        storedToken.setRevokedAt(LocalDateTime.now());
        when(tokenRepository.findByToken(hashToken(rawToken))).thenReturn(Optional.of(storedToken));

        assertThrows(
                ExpiredTokenException.class,
                () -> tokenService.findValidToken(rawToken, TokenPurpose.EMAIL_VERIFICATION)
        );
    }

    @Test
    void markUsed_shouldSetUsedAtAndSaveToken() {
        VerificationToken storedToken = token(TokenPurpose.EMAIL_VERIFICATION);

        tokenService.markUsed(storedToken);

        assertNotNull(storedToken.getUsedAt());
        verify(tokenRepository).save(storedToken);
    }

    @Test
    void markRevoked_shouldSetRevokedAtAndSaveToken() {
        VerificationToken storedToken = token(TokenPurpose.EMAIL_VERIFICATION);

        tokenService.markRevoked(storedToken);

        assertNotNull(storedToken.getRevokedAt());
        verify(tokenRepository).save(storedToken);
    }

    @Test
    void revokeActiveTokens_shouldRevokeAllActiveTokensForAccountAndPurpose() {
        Account account = Account.builder().username("testuser").build();
        VerificationToken firstToken = token(TokenPurpose.PASSWORD_RESET);
        VerificationToken secondToken = token(TokenPurpose.PASSWORD_RESET);
        when(tokenRepository.findByAccountAndPurposeAndUsedAtIsNullAndRevokedAtIsNull(
                account,
                TokenPurpose.PASSWORD_RESET
        )).thenReturn(List.of(firstToken, secondToken));

        tokenService.revokeActiveTokens(account, TokenPurpose.PASSWORD_RESET);

        assertNotNull(firstToken.getRevokedAt());
        assertNotNull(secondToken.getRevokedAt());
        verify(tokenRepository).saveAll(List.of(firstToken, secondToken));
    }

    @Test
    void isTokenValid_shouldReturnTrueOnlyForUnexpiredUnusedUnrevokedToken() {
        VerificationToken validToken = token(TokenPurpose.EMAIL_VERIFICATION);
        VerificationToken expiredToken = token(TokenPurpose.EMAIL_VERIFICATION);
        expiredToken.setExpireAt(LocalDateTime.now().minusSeconds(1));

        assertTrue(tokenService.isTokenValid(validToken));
        assertFalse(tokenService.isTokenValid(expiredToken));
        assertDoesNotThrow(() -> tokenService.isTokenValid(validToken));
    }

    private VerificationToken token(TokenPurpose purpose) {
        return VerificationToken.builder()
                .token("stored-hash")
                .account(Account.builder().username("testuser").build())
                .purpose(purpose)
                .expireAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
