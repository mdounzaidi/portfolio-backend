package com.mdounzaidi.portfolio_backend.account.repository;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.AccountRole;
import com.mdounzaidi.portfolio_backend.account.entity.InviteDetails;
import com.mdounzaidi.portfolio_backend.account.entity.ResetPassDetails;
import com.mdounzaidi.portfolio_backend.account.entity.TokenPurpose;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private ResetPassRepository resetPassRepository;

    @Autowired
    private InviteRepository inviteRepository;

    @Test
    void accountRepository_shouldPersistAccountWithRolesAndFindByUsernameAndEmail() {
        Account account = account("testuser", "test@example.com");
        account.setAccountRole(Set.of(AccountRole.ROLE_USER, AccountRole.ROLE_WRITER));

        Account savedAccount = accountRepository.saveAndFlush(account);

        Account foundByUsername = accountRepository.findByUsername("testuser").orElseThrow();
        Account foundByEmail = accountRepository.findByEmail("test@example.com").orElseThrow();

        assertEquals(savedAccount.getId(), foundByUsername.getId());
        assertEquals(savedAccount.getId(), foundByEmail.getId());
        assertTrue(foundByUsername.getAccountRole().contains(AccountRole.ROLE_USER));
        assertTrue(foundByUsername.getAccountRole().contains(AccountRole.ROLE_WRITER));
    }

    @Test
    void tokenRepository_shouldFindTokenByHashAndActiveTokensByAccountAndPurpose() {
        Account account = accountRepository.saveAndFlush(account("testuser", "test@example.com"));
        VerificationToken activeToken = token(account, "active-hash", TokenPurpose.PASSWORD_RESET);
        VerificationToken usedToken = token(account, "used-hash", TokenPurpose.PASSWORD_RESET);
        usedToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(activeToken);
        tokenRepository.save(usedToken);
        tokenRepository.flush();

        assertTrue(tokenRepository.findByToken("active-hash").isPresent());
        assertEquals(
                1,
                tokenRepository.findByAccountAndPurposeAndUsedAtIsNullAndRevokedAtIsNull(
                        account,
                        TokenPurpose.PASSWORD_RESET
                ).size()
        );
    }

    @Test
    void resetPassRepository_shouldFindByVerificationTokenAndActiveRequests() {
        Account account = accountRepository.saveAndFlush(account("testuser", "test@example.com"));
        VerificationToken activeToken = tokenRepository.saveAndFlush(token(account, "reset-hash", TokenPurpose.PASSWORD_RESET));
        ResetPassDetails resetDetails = ResetPassDetails.builder()
                .account(account)
                .verificationToken(activeToken)
                .active(true)
                .build();
        resetPassRepository.saveAndFlush(resetDetails);

        assertTrue(resetPassRepository.findByVerificationToken(activeToken).isPresent());
        assertEquals(1, resetPassRepository.findByAccountAndActiveTrue(account).size());
    }

    @Test
    void inviteRepository_shouldFindByVerificationTokenAndEmail() {
        Account inviter = accountRepository.saveAndFlush(account("adminuser", "admin@example.com"));
        VerificationToken inviteToken = tokenRepository.saveAndFlush(token(inviter, "invite-hash", TokenPurpose.ACCOUNT_INVITE));
        InviteDetails inviteDetails = InviteDetails.builder()
                .inviteBy(inviter)
                .verificationToken(inviteToken)
                .name("Invited User")
                .email("invited@example.com")
                .createdAt(LocalDateTime.now())
                .accountRole(AccountRole.ROLE_WRITER)
                .active(true)
                .accountCreated(false)
                .build();
        inviteRepository.saveAndFlush(inviteDetails);

        assertTrue(inviteRepository.findByVerificationToken(inviteToken).isPresent());
        assertTrue(inviteRepository.findByEmail("invited@example.com").isPresent());
        assertFalse(inviteRepository.findByEmail("missing@example.com").isPresent());
    }

    private Account account(String username, String email) {
        return Account.builder()
                .firstName("Test")
                .lastName("User")
                .username(username)
                .email(email)
                .password("encoded-password")
                .active(true)
                .emailVerified(true)
                .build();
    }

    private VerificationToken token(Account account, String hash, TokenPurpose purpose) {
        return VerificationToken.builder()
                .token(hash)
                .account(account)
                .purpose(purpose)
                .expireAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }
}
