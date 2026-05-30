package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.account.repository.TokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;


@AllArgsConstructor
@Service
public class AccountTokenService {

    private final TokenRepository tokenRepository;




    public VerificationToken createToken(Account account,int expiration){
        String token=UUID.randomUUID().toString();
        VerificationToken verificationToken=VerificationToken.builder()
                .token(token)
                .expireAt(LocalDateTime.now().plusSeconds(expiration))
                .account(account)
                .build();
        return tokenRepository.save(verificationToken);
    }




    public boolean isTokenValid(VerificationToken token) {

        if(token.getExpireAt().isAfter(LocalDateTime.now()))
            return true;
        else
            return false;
    }

    public VerificationToken findToken(String token){
        return tokenRepository.findByToken(token)
                .orElseThrow();
    }
    public void deleteToken(VerificationToken token){
        tokenRepository.delete(token);
    }


}
