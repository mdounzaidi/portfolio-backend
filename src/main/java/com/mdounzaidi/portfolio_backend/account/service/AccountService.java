package com.mdounzaidi.portfolio_backend.account.service;

import ch.qos.logback.core.util.StringUtil;
import com.mdounzaidi.portfolio_backend.account.dto.*;
import com.mdounzaidi.portfolio_backend.account.entity.*;
import com.mdounzaidi.portfolio_backend.account.mapper.AccountMapper;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

//   Account roles mapping still looks risky will need to check
@AllArgsConstructor
@Service
public class AccountService {


    private final AccountMapper accountMapper;
    private final AccountRepository accountRepository;
    private final AccountMailService mailService;
    private final AccountTokenService tokenService;
    private final PasswordEncoder passwordEncoder;


    // to create new user account
    @Transactional
    public AccountResponse registerAccount(AccountRequest accountRequest) {
        try{
            Account newAccount = buildAccount(accountRequest);
            Account saveAccount = accountRepository.save(newAccount);

            VerificationToken token = tokenService.createToken(saveAccount,(60*5));

            mailService.sendEmailVerificationMail(saveAccount.getEmail(), token.getToken());
            return accountMapper.buildAccountResponse(saveAccount);
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }


    }

    // account builder
    public Account buildAccount(AccountRequest accountRequest){
        if(usernameExists(accountRequest.getUsername()))
            throw new RuntimeException("UserName already exist please try again later");
        if(!accountRepository.findByEmail(accountRequest.getEmail()).isEmpty())
            throw new RuntimeException("User already exist please try again later");
        return Account.builder()
                .firstName(accountRequest.getFirstName())
                .lastName(accountRequest.getLastName())
                .username(accountRequest.getUsername())
                .email(accountRequest.getEmail())
                .password(passwordEncoder.encode(accountRequest.getPassword()))
                .active(false)
                .build();
    }

    @Transactional
    public AccountResponse getCurrentAccountDetails() {
        Account account=getCurrentAccount();
        return accountMapper.buildAccountResponse(account);
    }

    @Transactional
    public Account getCurrentAccount(){
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        String userName=authentication.getName();
        return  accountRepository.findByUsername(userName).orElseThrow(()->new RuntimeException("User Not Found"));
    }

    public boolean usernameExists(String userName){
        try{
            accountRepository.findByUsername(userName).orElseThrow(()->new RuntimeException("User Not Found"));
            return true;
        }
        catch (Exception e){
            return false;
        }
    }


    @Transactional
    public AccountResponse updateCurrentAccount(AccountUpdateRequest accountUpdateRequest) {
        Account account=getCurrentAccount();
        String newUserName=accountUpdateRequest.getUsername();
        if(StringUtils.hasText(newUserName) && !newUserName.equalsIgnoreCase(account.getUsername())){
            if(usernameExists(newUserName))
                throw new RuntimeException("UserName already exist");
            account.setUsername(newUserName);
        }
        String firstName=accountUpdateRequest.getFirstName();
        if(StringUtils.hasText(firstName) ){
            account.setFirstName(firstName);
        }
        String lastName=accountUpdateRequest.getLastName();
        if(StringUtils.hasText(lastName) ){
            account.setLastName(lastName);
        }
        Account savedAccount=accountRepository.save(account);
        return accountMapper.buildAccountResponse(savedAccount);

    }

    @Transactional
    public String changePassword(AccountPassUpdateRequest pass) {
        Account account=getCurrentAccount();
        if(!passwordEncoder.matches(pass.getOldPassword(), account.getPassword()))
            throw new RuntimeException("invalid password");
        account.setPassword(passwordEncoder.encode(pass.getNewPassword()));
        accountRepository.save(account);
        return "password updated";
    }



}