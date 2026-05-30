package com.mdounzaidi.portfolio_backend.account.security;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserAccountDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public UserAccountDetailsService (AccountRepository accountRepository){
        this.accountRepository=accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account= accountRepository.findByUsername(username).orElseThrow(()->new UsernameNotFoundException("Invalid user :"+username));
        return new UserAccountDetails(account);
    }
}
