package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.dto.AccountPassUpdateRequest;
import com.mdounzaidi.portfolio_backend.account.dto.AccountRequest;
import com.mdounzaidi.portfolio_backend.account.dto.AccountResponse;
import com.mdounzaidi.portfolio_backend.account.dto.AccountUpdateRequest;
import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.TokenPurpose;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import com.mdounzaidi.portfolio_backend.account.exception.AccountNotFoundException;
import com.mdounzaidi.portfolio_backend.account.exception.DuplicateAccountException;
import com.mdounzaidi.portfolio_backend.account.exception.InvalidPasswordException;
import com.mdounzaidi.portfolio_backend.account.mapper.AccountMapper;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMailService mailService;

    @Mock
    private AccountTokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(
                accountMapper,
                accountRepository,
                mailService,
                tokenService,
                passwordEncoder,
                new AccountIdentifierNormalizer()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buildAccount_shouldCreateAccount_whenUsernameAndEmailAreUnique() {
        AccountRequest request = AccountRequest.builder()
                .firstName("Test")
                .lastName("User")
                .username("TestUser")
                .email("TEST@EXAMPLE.COM")
                .password("StrongPass@123")
                .build();

        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongPass@123")).thenReturn("encoded-password");

        Account account = accountService.buildAccount(request);

        assertEquals("testuser", account.getUsername());
        assertEquals("test@example.com", account.getEmail());
        assertEquals("encoded-password", account.getPassword());
        assertFalse(account.isActive());
        assertFalse(account.isEmailVerified());

        verify(accountRepository).findByUsername("testuser");
        verify(accountRepository).findByEmail("test@example.com");
        verify(passwordEncoder).encode("StrongPass@123");
    }

    @Test
    void buildAccount_shouldThrowDuplicateAccountException_whenUsernameExists() {
        AccountRequest request = AccountRequest.builder()
                .firstName("Test")
                .lastName("User")
                .username("testuser")
                .email("test@example.com")
                .password("StrongPass@123")
                .build();

        when(accountRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(new Account()));

        assertThrows(
                DuplicateAccountException.class,
                () -> accountService.buildAccount(request)
        );

        verify(accountRepository).findByUsername("testuser");
        verify(accountRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void buildAccount_shouldThrowDuplicateAccountException_whenEmailExists() {
        AccountRequest request = AccountRequest.builder()
                .firstName("Test")
                .lastName("User")
                .username("testuser")
                .email("test@example.com")
                .password("StrongPass@123")
                .build();

        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(accountRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(new Account()));

        assertThrows(
                DuplicateAccountException.class,
                () -> accountService.buildAccount(request)
        );

        verify(accountRepository).findByUsername("testuser");
        verify(accountRepository).findByEmail("test@example.com");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void registerAccount_shouldSaveAccountSendVerificationAndReturnResponse() {
        AccountRequest request = accountRequest();
        Account savedAccount = Account.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .build();
        VerificationToken verificationToken = VerificationToken.builder()
                .token("stored-token")
                .account(savedAccount)
                .purpose(TokenPurpose.EMAIL_VERIFICATION)
                .expireAt(LocalDateTime.now().plusMinutes(5))
                .build();
        AccountResponse response = AccountResponse.builder()
                .username("testuser")
                .email("test@example.com")
                .build();
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongPass@123")).thenReturn("encoded-password");
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(tokenService.createToken(savedAccount, 60 * 5, TokenPurpose.EMAIL_VERIFICATION))
                .thenReturn(new GeneratedToken("raw-token", verificationToken));
        when(accountMapper.buildAccountResponse(savedAccount)).thenReturn(response);

        AccountResponse result = accountService.registerAccount(request);

        assertSame(response, result);
        verify(tokenService).revokeActiveTokens(savedAccount, TokenPurpose.EMAIL_VERIFICATION);
        verify(mailService).sendEmailVerificationMail("test@example.com", "raw-token");
    }

    @Test
    void usernameExists_shouldNormalizeUsernameBeforeLookup() {
        when(accountRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(Account.builder().username("testuser").build()));

        assertTrue(accountService.usernameExists(" TestUser "));

        verify(accountRepository).findByUsername("testuser");
    }

    @Test
    void getCurrentAccount_shouldReturnAuthenticatedAccount() {
        Account account = Account.builder().username("testuser").build();
        setAuthenticationName(" TestUser ");
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        Account result = accountService.getCurrentAccount();

        assertSame(account, result);
    }

    @Test
    void getCurrentAccount_shouldThrowAccountNotFoundException_whenAuthenticatedAccountDoesNotExist() {
        setAuthenticationName("missinguser");
        when(accountRepository.findByUsername("missinguser")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getCurrentAccount());
    }

    @Test
    void getCurrentAccountDetails_shouldReturnMappedCurrentAccount() {
        Account account = Account.builder().username("testuser").build();
        AccountResponse response = AccountResponse.builder().username("testuser").build();
        setAuthenticationName("testuser");
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
        when(accountMapper.buildAccountResponse(account)).thenReturn(response);

        AccountResponse result = accountService.getCurrentAccountDetails();

        assertSame(response, result);
    }

    @Test
    void updateCurrentAccount_shouldUpdateBasicFieldsWithoutSendingEmail_whenEmailDoesNotChange() {
        Account account = Account.builder()
                .username("testuser")
                .email("test@example.com")
                .emailVerified(true)
                .build();
        AccountUpdateRequest request = AccountUpdateRequest.builder()
                .firstName("Updated")
                .lastName("User")
                .username("TestUser")
                .build();
        AccountResponse response = AccountResponse.builder()
                .firstName("Updated")
                .lastName("User")
                .username("testuser")
                .email("test@example.com")
                .emailVerified(true)
                .build();
        setAuthenticationName("testuser");
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.buildAccountResponse(account)).thenReturn(response);

        AccountResponse result = accountService.updateCurrentAccount(request);

        assertSame(response, result);
        assertEquals("Updated", account.getFirstName());
        assertEquals("User", account.getLastName());
        assertTrue(account.isEmailVerified());
        verify(tokenService, never()).createToken(any(), anyInt(), any());
        verify(mailService, never()).sendEmailVerificationMail(anyString(), anyString());
    }

    @Test
    void updateCurrentAccount_shouldResetVerificationAndSendEmail_whenEmailChanges() {
        Account account = Account.builder()
                .username("testuser")
                .email("old@example.com")
                .emailVerified(true)
                .build();
        AccountUpdateRequest request = AccountUpdateRequest.builder()
                .email("NEW@EXAMPLE.COM")
                .build();
        VerificationToken verificationToken = VerificationToken.builder()
                .token("stored-token")
                .account(account)
                .purpose(TokenPurpose.EMAIL_VERIFICATION)
                .expireAt(LocalDateTime.now().plusMinutes(5))
                .build();
        AccountResponse response = AccountResponse.builder()
                .username("testuser")
                .email("new@example.com")
                .emailVerified(false)
                .build();
        setAuthenticationName("testuser");
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
        when(accountRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(accountRepository.save(account)).thenReturn(account);
        when(tokenService.createToken(account, 60 * 5, TokenPurpose.EMAIL_VERIFICATION))
                .thenReturn(new GeneratedToken("raw-token", verificationToken));
        when(accountMapper.buildAccountResponse(account)).thenReturn(response);

        AccountResponse result = accountService.updateCurrentAccount(request);

        assertSame(response, result);
        assertEquals("new@example.com", account.getEmail());
        assertFalse(account.isEmailVerified());
        verify(tokenService).revokeActiveTokens(account, TokenPurpose.EMAIL_VERIFICATION);
        verify(mailService).sendEmailVerificationMail("new@example.com", "raw-token");
    }

    @Test
    void updateCurrentAccount_shouldThrowDuplicateAccountException_whenNewUsernameAlreadyExists() {
        Account account = Account.builder()
                .username("testuser")
                .email("test@example.com")
                .build();
        AccountUpdateRequest request = AccountUpdateRequest.builder()
                .username("otheruser")
                .build();
        setAuthenticationName("testuser");
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
        when(accountRepository.findByUsername("otheruser"))
                .thenReturn(Optional.of(Account.builder().username("otheruser").build()));

        assertThrows(DuplicateAccountException.class, () -> accountService.updateCurrentAccount(request));

        verify(accountRepository, never()).save(account);
    }

    @Test
    void updateCurrentAccount_shouldThrowDuplicateAccountException_whenNewEmailAlreadyExists() {
        Account account = Account.builder()
                .username("testuser")
                .email("test@example.com")
                .build();
        AccountUpdateRequest request = AccountUpdateRequest.builder()
                .email("other@example.com")
                .build();
        setAuthenticationName("testuser");
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
        when(accountRepository.findByEmail("other@example.com"))
                .thenReturn(Optional.of(Account.builder().email("other@example.com").build()));

        assertThrows(DuplicateAccountException.class, () -> accountService.updateCurrentAccount(request));

        verify(accountRepository, never()).save(account);
    }

    @Test
    void changePassword_shouldUpdatePassword_whenOldPasswordMatches() {
        Account account = Account.builder()
                .username("testuser")
                .password("old-encoded-password")
                .build();
        AccountPassUpdateRequest request = AccountPassUpdateRequest.builder()
                .oldPassword("OldPass@123")
                .newPassword("NewPass@123")
                .build();
        setAuthenticationName("testuser");
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("OldPass@123", "old-encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("NewPass@123")).thenReturn("new-encoded-password");

        String response = accountService.changePassword(request);

        assertEquals("password updated", response);
        assertEquals("new-encoded-password", account.getPassword());
        verify(accountRepository).save(account);
    }

    @Test
    void changePassword_shouldThrowInvalidPasswordException_whenOldPasswordDoesNotMatch() {
        Account account = Account.builder()
                .username("testuser")
                .password("old-encoded-password")
                .build();
        AccountPassUpdateRequest request = AccountPassUpdateRequest.builder()
                .oldPassword("WrongPass@123")
                .newPassword("NewPass@123")
                .build();
        setAuthenticationName("testuser");
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("WrongPass@123", "old-encoded-password")).thenReturn(false);

        assertThrows(InvalidPasswordException.class, () -> accountService.changePassword(request));

        verify(passwordEncoder, never()).encode(anyString());
        verify(accountRepository, never()).save(account);
    }

    private AccountRequest accountRequest() {
        return AccountRequest.builder()
                .firstName("Test")
                .lastName("User")
                .username("TestUser")
                .email("TEST@EXAMPLE.COM")
                .password("StrongPass@123")
                .build();
    }

    private void setAuthenticationName(String username) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
