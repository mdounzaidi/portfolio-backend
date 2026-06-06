package com.mdounzaidi.portfolio_backend.account.service;


import com.mdounzaidi.portfolio_backend.account.dto.CredentialRequest;
import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.ResetPassDetails;
import com.mdounzaidi.portfolio_backend.account.entity.TokenPurpose;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import com.mdounzaidi.portfolio_backend.account.exception.InvalidTokenException;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.account.repository.ResetPassRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class AccountPasswordResetService {

    private final AccountRepository accountRepository;
    private final AccountMailService mailService;
    private final ResetPassRepository resetPassRepository;
    private final AccountTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final AccountIdentifierNormalizer identifierNormalizer;


    @Transactional
    public String requestPasswordReset(String userName) {
        String response = "If an account exists, a password reset email will be sent.";
        String identifier = identifierNormalizer.username(userName);
        return accountRepository.findByUsernameOrEmail(identifier, identifier)
                .map(account -> {
                    List<ResetPassDetails> oldRequests = resetPassRepository.findByAccountAndActiveTrue(account);
                    oldRequests.forEach(resetRequest -> {
                        resetRequest.setActive(false);
                        tokenService.markRevoked(resetRequest.getVerificationToken());
                    });
                    resetPassRepository.saveAll(oldRequests);


                    GeneratedToken generatedToken = tokenService.createToken(
                            account,
                            60 * 60,
                            TokenPurpose.PASSWORD_RESET
                    );

                    ResetPassDetails resetPassDetails= ResetPassDetails.builder()
                            .account(account)
                            .verificationToken(generatedToken.verificationToken())
                            .active(true)
                            .build();

                    resetPassRepository.save(resetPassDetails);
                    mailService.sendPasswordResetMail(account.getEmail(), generatedToken.rawToken());

                    return response;
                })
                .orElse(response);
    }

    @Transactional
    public String resetPassword(CredentialRequest credentialRequest, String token) {
        VerificationToken verificationToken = tokenService.findValidToken(
                token,
                TokenPurpose.PASSWORD_RESET
        );
        ResetPassDetails resetPassDetails= resetPassRepository
                .findByVerificationToken(verificationToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid reset link"));
        Account account=resetPassDetails.getAccount();

        String username = identifierNormalizer.username(credentialRequest.userName());
        if(!account.getUsername().equals(username))
            throw new InvalidTokenException("Invalid reset request");



        resetPassDetails.setActive(false);
        resetPassRepository.save(resetPassDetails);

        tokenService.markUsed(verificationToken);

        account.setPassword(passwordEncoder.encode(credentialRequest.password()));
        accountRepository.save(account);
        return "Password Reset Done";
    }

}
