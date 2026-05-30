package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
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

        VerificationToken verificationToken=tokenService.findToken(token);
        if(!tokenService.isTokenValid(verificationToken)){
            throw new RuntimeException("Expired token");
        }
        Account account = verificationToken.getAccount();
        if (account.isEmailVerified())
            throw  new RuntimeException("Already Verified") ;
        account.setEmailVerified(true);
        account.setActive(true);
        accountRepository.save(account);
        tokenService.deleteToken(verificationToken);
        return "Account Verified";
    }

}
