package com.mdounzaidi.portfolio_backend.account.service;


import com.mdounzaidi.portfolio_backend.account.dto.*;
import com.mdounzaidi.portfolio_backend.account.entity.*;
import com.mdounzaidi.portfolio_backend.account.exception.AccountNotFoundException;
import com.mdounzaidi.portfolio_backend.account.exception.DuplicateAccountException;
import com.mdounzaidi.portfolio_backend.account.exception.InvalidPasswordException;
import com.mdounzaidi.portfolio_backend.account.mapper.AccountMapper;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

//   Account roles mapping still looks risky will need to check
@AllArgsConstructor
@Service
public class AccountService {


    private final AccountMapper accountMapper;
    private final AccountRepository accountRepository;
    private final AccountMailService mailService;
    private final AccountTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final AccountIdentifierNormalizer identifierNormalizer;


    // to create new user account
    @Transactional
    public AccountResponse registerAccount(AccountRequest accountRequest) {
        Account newAccount = buildAccount(accountRequest);
        Account saveAccount = accountRepository.save(newAccount);

        sendEmailVerification(saveAccount);
        return accountMapper.buildAccountResponse(saveAccount);

    }

    // account builder
    public Account buildAccount(AccountRequest accountRequest){
        String username = identifierNormalizer.username(accountRequest.getUsername());
        String email = identifierNormalizer.email(accountRequest.getEmail());

        if (usernameExists(username)) {
            throw new DuplicateAccountException("Username already exists");
        }

        if (accountRepository.findByEmail(email).isPresent()) {
            throw new DuplicateAccountException("Email already exists");
        }
        return Account.builder()
                .firstName(accountRequest.getFirstName())
                .lastName(accountRequest.getLastName())
                .username(username)
                .email(email)
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
        Optional<Long> accountId = extractJwtAccountId(authentication);

        if (accountId.isPresent()) {
            return accountRepository
                    .findById(accountId.get())
                    .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        }

        String userName=identifierNormalizer.username(authentication.getName());
        return accountRepository
                .findByUsername(userName)
                .orElseThrow(()->new AccountNotFoundException("Account not found"));
    }

    private Optional<Long> extractJwtAccountId(Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }

        Jwt jwt = null;
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            jwt = jwtAuthenticationToken.getToken();
        } else if (authentication.getPrincipal() instanceof Jwt principalJwt) {
            jwt = principalJwt;
        }

        if (jwt == null) {
            return Optional.empty();
        }

        Object accountId = jwt.getClaims().get("accountId");
        if (accountId instanceof Number number) {
            return Optional.of(number.longValue());
        }

        if (accountId instanceof String value && StringUtils.hasText(value)) {
            try {
                return Optional.of(Long.parseLong(value));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    public boolean usernameExists(String userName){
        return accountRepository.findByUsername(identifierNormalizer.username(userName)).isPresent();
    }


    @Transactional
    public AccountResponse updateCurrentAccount(AccountUpdateRequest accountUpdateRequest) {
        Account account=getCurrentAccount();
        boolean emailChanged = false;

        String newUserName=identifierNormalizer.username(accountUpdateRequest.getUsername());
        if(StringUtils.hasText(newUserName) && !newUserName.equals(account.getUsername())){
            if(usernameExists(newUserName))
                throw new DuplicateAccountException("Username already exists");
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
        String newEmail = identifierNormalizer.email(accountUpdateRequest.getEmail());
        if (StringUtils.hasText(newEmail) && !newEmail.equals(account.getEmail())) {
            if (accountRepository.findByEmail(newEmail).isPresent()) {
                throw new DuplicateAccountException("Email already exists");
            }
            account.setEmail(newEmail);
            account.setEmailVerified(false);
            emailChanged = true;
        }
        Account savedAccount=accountRepository.save(account);

        if (emailChanged) {
            sendEmailVerification(savedAccount);
        }

        return accountMapper.buildAccountResponse(savedAccount);

    }

    @Transactional
    public String changePassword(AccountPassUpdateRequest pass) {
        Account account=getCurrentAccount();
        if(!passwordEncoder.matches(pass.getOldPassword(), account.getPassword()))
            throw new InvalidPasswordException("Invalid password");
        account.setPassword(passwordEncoder.encode(pass.getNewPassword()));
        accountRepository.save(account);
        return "password updated";
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
