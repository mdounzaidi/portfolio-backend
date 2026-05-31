package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.TokenPurpose;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import com.mdounzaidi.portfolio_backend.account.exception.ExpiredTokenException;
import com.mdounzaidi.portfolio_backend.account.exception.InvalidTokenException;
import com.mdounzaidi.portfolio_backend.account.repository.TokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class AccountTokenService {

    private final TokenRepository tokenRepository;

    public GeneratedToken createToken(Account account, int expiration, TokenPurpose purpose) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        VerificationToken verificationToken = VerificationToken.builder()
                .token(tokenHash)
                .expireAt(LocalDateTime.now().plusSeconds(expiration))
                .account(account)
                .purpose(purpose)
                .build();

        VerificationToken savedToken = tokenRepository.save(verificationToken);
        return new GeneratedToken(rawToken, savedToken);
    }

    public VerificationToken findToken(String rawToken, TokenPurpose expectedPurpose) {
        String tokenHash = hashToken(rawToken);

        VerificationToken verificationToken = tokenRepository.findByToken(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));

        if (verificationToken.getPurpose() != expectedPurpose) {
            throw new InvalidTokenException("Invalid token");
        }

        return verificationToken;
    }

    public VerificationToken findValidToken(String token, TokenPurpose expectedPurpose) {
        VerificationToken verificationToken = findToken(token, expectedPurpose);

        if (!isTokenValid(verificationToken)) {
            throw new ExpiredTokenException("Expired token");
        }

        return verificationToken;
    }

    public boolean isTokenValid(VerificationToken token) {
        return token.getExpireAt().isAfter(LocalDateTime.now())
                && token.getUsedAt() == null
                && token.getRevokedAt() == null;
    }

    public void markUsed(VerificationToken token) {
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    public void markRevoked(VerificationToken token) {
        token.setRevokedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    public void revokeActiveTokens(Account account, TokenPurpose purpose) {
        List<VerificationToken> tokens = tokenRepository
                .findByAccountAndPurposeAndUsedAtIsNullAndRevokedAtIsNull(account, purpose);

        LocalDateTime now = LocalDateTime.now();
        tokens.forEach(token -> token.setRevokedAt(now));
        tokenRepository.saveAll(tokens);
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
