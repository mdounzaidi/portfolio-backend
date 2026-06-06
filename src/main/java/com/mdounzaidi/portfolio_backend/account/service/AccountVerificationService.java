package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.TokenPurpose;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import com.mdounzaidi.portfolio_backend.account.exception.AccountStateException;
import com.mdounzaidi.portfolio_backend.account.exception.ExpiredTokenException;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class AccountVerificationService {


    private final AccountTokenService tokenService;
    private final AccountRepository accountRepository;
    private final AccountMailService mailService;
    private final AccountIdentifierNormalizer identifierNormalizer;

    //
    @Transactional
    public String verifyEmail(String token) {

        VerificationToken verificationToken = tokenService.findValidToken(
                token,
                TokenPurpose.EMAIL_VERIFICATION
        );

        Account account = verificationToken.getAccount();
        if (account.isEmailVerified())
            throw new AccountStateException("Account already verified");
        account.setEmailVerified(true);
        account.setActive(true);
        accountRepository.save(account);
        tokenService.markUsed(verificationToken);
        return "Account Verified";
    }

    @Transactional
    public String resendVerificationEmail(String identifier) {
        String response = "If an unverified account exists, a verification email will be sent.";
        String normalizedIdentifier = identifierNormalizer.username(identifier);

        accountRepository.findByUsernameOrEmail(normalizedIdentifier, normalizedIdentifier)
                .filter(account -> !account.isEmailVerified())
                .ifPresent(this::sendEmailVerification);

        return response;
    }

    private void sendEmailVerification(Account account) {
        tokenService.revokeActiveTokens(account, TokenPurpose.EMAIL_VERIFICATION);

        GeneratedToken generatedToken = tokenService.createToken(
                account,
                60 * 5,
                TokenPurpose.EMAIL_VERIFICATION
        );

        mailService.sendEmailVerificationMail(account.getEmail(), generatedToken.rawToken());
    }

}
