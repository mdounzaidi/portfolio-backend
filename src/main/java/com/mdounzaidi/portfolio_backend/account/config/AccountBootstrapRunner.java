package com.mdounzaidi.portfolio_backend.account.config;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.AccountRole;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.account.service.AccountIdentifierNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Component
public class AccountBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountBootstrapRunner.class);

    private final AccountBootstrapProperties properties;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountIdentifierNormalizer identifierNormalizer;

    public AccountBootstrapRunner(
            AccountBootstrapProperties properties,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            AccountIdentifierNormalizer identifierNormalizer
    ) {
        this.properties = properties;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.identifierNormalizer = identifierNormalizer;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }

        validateBootstrapConfig();

        String username = identifierNormalizer.username(properties.username());
        String email = identifierNormalizer.email(properties.email());

        if (accountRepository.findByUsernameOrEmail(username, email).isPresent()) {
            log.info("Account bootstrap skipped because username or email already exists");
            return;
        }

        Account account = Account.builder()
                .firstName(properties.firstName())
                .lastName(properties.lastName())
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(properties.password()))
                .active(true)
                .emailVerified(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();
        account.setAccountRole(rolesFor(properties.roleOrDefault()));

        accountRepository.save(account);
        log.warn("Bootstrap account created for username '{}'. Disable account bootstrap after first use.", username);
    }

    private void validateBootstrapConfig() {
        if (!StringUtils.hasText(properties.firstName())
                || !StringUtils.hasText(properties.username())
                || !StringUtils.hasText(properties.email())
                || !StringUtils.hasText(properties.password())) {
            throw new IllegalStateException(
                    "Account bootstrap requires first-name, username, email, and password"
            );
        }
    }

    private Set<AccountRole> rolesFor(AccountRole role) {
        Set<AccountRole> roles = new HashSet<>();
        roles.add(AccountRole.ROLE_USER);

        if (role == AccountRole.ROLE_WRITER
                || role == AccountRole.ROLE_ADMIN
                || role == AccountRole.ROLE_SUPERADMIN) {
            roles.add(AccountRole.ROLE_WRITER);
        }

        if (role == AccountRole.ROLE_ADMIN || role == AccountRole.ROLE_SUPERADMIN) {
            roles.add(AccountRole.ROLE_ADMIN);
        }

        if (role == AccountRole.ROLE_SUPERADMIN) {
            roles.add(AccountRole.ROLE_SUPERADMIN);
        }

        return roles;
    }
}
