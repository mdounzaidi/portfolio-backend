package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AccountLoginPolicy {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final AccountRepository accountRepository;
    private final AccountIdentifierNormalizer identifierNormalizer;

    public AccountLoginPolicy(
            AccountRepository accountRepository,
            AccountIdentifierNormalizer identifierNormalizer
    ) {
        this.accountRepository = accountRepository;
        this.identifierNormalizer = identifierNormalizer;
    }

    @Transactional
    public void recordSuccessfulLogin(Account account) {
        account.setFailedLoginAttempts(0);
        account.setAccountNonLocked(true);
        account.setLockedAt(null);
        account.setLastLoginAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    @Transactional
    public void recordFailedLogin(String identifier) {
        String normalizedIdentifier = identifierNormalizer.username(identifier);

        accountRepository.findByUsernameOrEmail(normalizedIdentifier, normalizedIdentifier)
                .ifPresent(account -> {
                    if (!account.isAccountNonLocked()) {
                        return;
                    }

                    int failedAttempts = account.getFailedLoginAttempts() + 1;
                    account.setFailedLoginAttempts(failedAttempts);

                    if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                        account.setAccountNonLocked(false);
                        account.setLockedAt(LocalDateTime.now());
                    }

                    accountRepository.save(account);
                });
    }

    @Transactional
    public Account unlockIfLockExpired(Account account) {
        if (account.isAccountNonLocked() || account.getLockedAt() == null) {
            return account;
        }

        LocalDateTime unlockAt = account.getLockedAt().plus(LOCK_DURATION);
        if (unlockAt.isAfter(LocalDateTime.now())) {
            return account;
        }

        account.setAccountNonLocked(true);
        account.setFailedLoginAttempts(0);
        account.setLockedAt(null);
        return accountRepository.save(account);
    }
}
