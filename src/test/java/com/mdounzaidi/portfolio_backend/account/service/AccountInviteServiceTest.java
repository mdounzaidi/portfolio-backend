package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.dto.AccountRequest;
import com.mdounzaidi.portfolio_backend.account.dto.AccountResponse;
import com.mdounzaidi.portfolio_backend.account.dto.InviteDetailsRequest;
import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.AccountRole;
import com.mdounzaidi.portfolio_backend.account.entity.InviteDetails;
import com.mdounzaidi.portfolio_backend.account.entity.TokenPurpose;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import com.mdounzaidi.portfolio_backend.account.exception.AccountAuthorizationException;
import com.mdounzaidi.portfolio_backend.account.exception.InviteNotValidException;
import com.mdounzaidi.portfolio_backend.account.mapper.AccountMapper;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.account.repository.InviteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountInviteServiceTest {

    @Mock
    private AccountTokenService tokenService;

    @Mock
    private AccountMailService mailService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private InviteRepository inviteRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private AccountMapper accountMapper;

    private AccountInviteService inviteService;

    @BeforeEach
    void setUp() {
        inviteService = new AccountInviteService(
                tokenService,
                mailService,
                accountRepository,
                inviteRepository,
                accountService,
                accountMapper,
                new AccountIdentifierNormalizer()
        );
    }

    @Test
    void createInvite_shouldThrowAccountAuthorizationException_whenNormalUserInvitesWriter() {
        Account currentAccount = accountWithRoles(AccountRole.ROLE_USER);
        when(accountService.getCurrentAccount()).thenReturn(currentAccount);

        assertThrows(
                AccountAuthorizationException.class,
                () -> inviteService.createInvite(inviteRequest(AccountRole.ROLE_WRITER))
        );

        verify(tokenService, never()).createToken(any(), any(Integer.class), any());
    }

    @Test
    void createInvite_shouldThrowAccountAuthorizationException_whenAdminInvitesAdmin() {
        Account currentAccount = accountWithRoles(AccountRole.ROLE_ADMIN);
        when(accountService.getCurrentAccount()).thenReturn(currentAccount);

        assertThrows(
                AccountAuthorizationException.class,
                () -> inviteService.createInvite(inviteRequest(AccountRole.ROLE_ADMIN))
        );
    }

    @Test
    void createInvite_shouldCreateInviteAndSendMail_whenAdminInvitesWriter() {
        Account currentAccount = accountWithRoles(AccountRole.ROLE_ADMIN);
        VerificationToken token = verificationToken(currentAccount);
        ArgumentCaptor<InviteDetails> inviteCaptor = ArgumentCaptor.forClass(InviteDetails.class);
        when(accountService.getCurrentAccount()).thenReturn(currentAccount);
        when(tokenService.createToken(currentAccount, 10 * 60 * 60, TokenPurpose.ACCOUNT_INVITE))
                .thenReturn(new GeneratedToken("raw-invite-token", token));

        String response = inviteService.createInvite(inviteRequest(AccountRole.ROLE_WRITER));

        assertEquals("we have sent invitation link", response);
        verify(inviteRepository).save(inviteCaptor.capture());
        InviteDetails savedInvite = inviteCaptor.getValue();
        assertSame(currentAccount, savedInvite.getInviteBy());
        assertSame(token, savedInvite.getVerificationToken());
        assertEquals("invited@example.com", savedInvite.getEmail());
        assertEquals(AccountRole.ROLE_WRITER, savedInvite.getAccountRole());
        assertTrue(savedInvite.isActive());
        assertFalse(savedInvite.isAccountCreated());
        verify(mailService).sendAccountInviteMail("invited@example.com", "raw-invite-token");
    }

    @Test
    void completeInvite_shouldCreateVerifiedActiveAccountWithInvitedRole() {
        Account invitedAccount = Account.builder()
                .username("inviteduser")
                .email("invited@example.com")
                .build();
        AccountResponse response = AccountResponse.builder()
                .username("inviteduser")
                .email("invited@example.com")
                .emailVerified(true)
                .build();
        Account inviter = accountWithRoles(AccountRole.ROLE_ADMIN);
        VerificationToken token = verificationToken(inviter);
        InviteDetails inviteDetails = InviteDetails.builder()
                .inviteBy(inviter)
                .verificationToken(token)
                .name("Invited User")
                .email("invited@example.com")
                .createdAt(LocalDateTime.now())
                .accountRole(AccountRole.ROLE_WRITER)
                .active(true)
                .accountCreated(false)
                .build();
        AccountRequest request = AccountRequest.builder()
                .firstName("Invited")
                .lastName("User")
                .username("InvitedUser")
                .email("INVITED@EXAMPLE.COM")
                .password("StrongPass@123")
                .build();
        when(tokenService.findValidToken("raw-token", TokenPurpose.ACCOUNT_INVITE)).thenReturn(token);
        when(inviteRepository.findByVerificationToken(token)).thenReturn(Optional.of(inviteDetails));
        when(accountService.buildAccount(request)).thenReturn(invitedAccount);
        when(accountRepository.save(invitedAccount)).thenReturn(invitedAccount);
        when(accountMapper.buildAccountResponse(invitedAccount)).thenReturn(response);

        AccountResponse result = inviteService.completeInvite(request, "raw-token");

        assertSame(response, result);
        assertTrue(invitedAccount.isActive());
        assertTrue(invitedAccount.isEmailVerified());
        assertTrue(invitedAccount.getAccountRole().contains(AccountRole.ROLE_WRITER));
        assertFalse(inviteDetails.isActive());
        assertTrue(inviteDetails.isAccountCreated());
        verify(inviteRepository).save(inviteDetails);
        verify(tokenService).markUsed(token);
    }

    @Test
    void completeInvite_shouldThrowInviteNotValidException_whenEmailDoesNotMatch() {
        Account inviter = accountWithRoles(AccountRole.ROLE_ADMIN);
        VerificationToken token = verificationToken(inviter);
        InviteDetails inviteDetails = InviteDetails.builder()
                .inviteBy(inviter)
                .verificationToken(token)
                .email("invited@example.com")
                .accountRole(AccountRole.ROLE_WRITER)
                .active(true)
                .accountCreated(false)
                .build();
        AccountRequest request = AccountRequest.builder()
                .email("wrong@example.com")
                .build();
        when(tokenService.findValidToken("raw-token", TokenPurpose.ACCOUNT_INVITE)).thenReturn(token);
        when(inviteRepository.findByVerificationToken(token)).thenReturn(Optional.of(inviteDetails));

        assertThrows(
                InviteNotValidException.class,
                () -> inviteService.completeInvite(request, "raw-token")
        );

        verify(accountService, never()).buildAccount(any());
        verify(tokenService, never()).markUsed(token);
    }

    @Test
    void completeInvite_shouldThrowInviteNotValidException_whenInviteAlreadyCreatedAccount() {
        Account inviter = accountWithRoles(AccountRole.ROLE_ADMIN);
        VerificationToken token = verificationToken(inviter);
        InviteDetails inviteDetails = InviteDetails.builder()
                .inviteBy(inviter)
                .verificationToken(token)
                .email("invited@example.com")
                .accountRole(AccountRole.ROLE_WRITER)
                .active(true)
                .accountCreated(true)
                .build();
        AccountRequest request = AccountRequest.builder()
                .email("invited@example.com")
                .build();
        when(tokenService.findValidToken("raw-token", TokenPurpose.ACCOUNT_INVITE)).thenReturn(token);
        when(inviteRepository.findByVerificationToken(token)).thenReturn(Optional.of(inviteDetails));

        assertThrows(
                InviteNotValidException.class,
                () -> inviteService.completeInvite(request, "raw-token")
        );
    }

    private InviteDetailsRequest inviteRequest(AccountRole role) {
        InviteDetailsRequest request = new InviteDetailsRequest();
        request.setAccountRole(role);
        request.setName("Invited User");
        request.setEmail("INVITED@EXAMPLE.COM");
        return request;
    }

    private Account accountWithRoles(AccountRole... roles) {
        Account account = Account.builder()
                .username("currentuser")
                .email("current@example.com")
                .build();
        account.setAccountRole(Set.of(roles));
        return account;
    }

    private VerificationToken verificationToken(Account account) {
        return VerificationToken.builder()
                .token("stored-hash")
                .account(account)
                .purpose(TokenPurpose.ACCOUNT_INVITE)
                .expireAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }
}
