package com.mdounzaidi.portfolio_backend.account.security;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.account.service.AccountIdentifierNormalizer;
import com.mdounzaidi.portfolio_backend.account.service.AccountLoginPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserAccountDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final AccountIdentifierNormalizer identifierNormalizer;
    private final AccountLoginPolicy accountLoginPolicy;

    public UserAccountDetailsService(
            AccountRepository accountRepository,
            AccountIdentifierNormalizer identifierNormalizer,
            AccountLoginPolicy accountLoginPolicy
    ){
        this.accountRepository=accountRepository;
        this.identifierNormalizer = identifierNormalizer;
        this.accountLoginPolicy = accountLoginPolicy;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = identifierNormalizer.username(username);

        Account account= accountRepository.findByUsernameOrEmail(normalizedUsername,normalizedUsername).orElseThrow(()->new UsernameNotFoundException("Invalid user :"+username));
        account = accountLoginPolicy.unlockIfLockExpired(account);
        return new UserAccountDetails(account);
    }
}
