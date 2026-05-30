package com.mdounzaidi.portfolio_backend.account.service;


import com.mdounzaidi.portfolio_backend.account.dto.CredentialRequest;
import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.ResetPassDetails;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.account.repository.ResetPassRepository;
import com.mdounzaidi.portfolio_backend.account.repository.TokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AccountPasswordResetService {

    private final AccountRepository accountRepository;
    private final AccountMailService mailService;
    private final ResetPassRepository resetPassRepository;
    private final AccountTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final TokenRepository tokenRepository;

    @Transactional
    public String requestPasswordReset(String userName) {
        Account account=accountRepository.findByUsername(userName).orElseThrow(()->new RuntimeException("user not found"));
        VerificationToken token=tokenService.createToken(account,(60*60));
        ResetPassDetails resetPassDetails=ResetPassDetails.builder()
                .account(account)
                .verificationToken(token)
                .build();
        resetPassRepository.save(resetPassDetails);
        mailService.sendPasswordResetMail(account.getEmail(),token.getToken());
        return "password reset mail sent";
    }

    @Transactional
    public String resetPassword(CredentialRequest credentialRequest, String token) {
        VerificationToken verificationToken=tokenService.findToken(token);
        ResetPassDetails resetPassDetails= resetPassRepository.findByVerificationToken(verificationToken).orElseThrow(()->new RuntimeException("invalid link"));
        Account account=resetPassDetails.getAccount();

        if(!account.getUsername().equalsIgnoreCase(credentialRequest.userName()))
            throw new RuntimeException("invalid User Name");
        if(!tokenService.isTokenValid(verificationToken))
            throw new RuntimeException("Token expired");

        resetPassDetails.setVerificationToken(null);
        resetPassRepository.save(resetPassDetails);

        tokenService.deleteToken(verificationToken);
        account.setPassword(passwordEncoder.encode(credentialRequest.password()));
        accountRepository.save(account);
        return "Password Reset Done";
    }

}
