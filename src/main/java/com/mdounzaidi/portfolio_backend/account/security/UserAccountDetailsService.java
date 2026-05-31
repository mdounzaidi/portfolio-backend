package com.mdounzaidi.portfolio_backend.account.security;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.account.service.AccountIdentifierNormalizer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserAccountDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final AccountIdentifierNormalizer identifierNormalizer;

    public UserAccountDetailsService(
            AccountRepository accountRepository,
            AccountIdentifierNormalizer identifierNormalizer
    ){
        this.accountRepository=accountRepository;
        this.identifierNormalizer = identifierNormalizer;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = identifierNormalizer.username(username);
        Account account= accountRepository.findByUsername(normalizedUsername).orElseThrow(()->new UsernameNotFoundException("Invalid user :"+username));
        return new UserAccountDetails(account);
    }
}
