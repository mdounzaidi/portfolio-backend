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

}
