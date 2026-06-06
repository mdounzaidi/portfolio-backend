package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.dto.AccountResponse;
import com.mdounzaidi.portfolio_backend.account.dto.LoginRequest;
import com.mdounzaidi.portfolio_backend.account.dto.LoginResponse;
import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.mapper.AccountMapper;
import com.mdounzaidi.portfolio_backend.account.security.JwtTokenService;
import com.mdounzaidi.portfolio_backend.account.security.UserAccountDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private AccountLoginPolicy accountLoginPolicy;

    @Mock
    private Authentication authentication;

    private AccountAuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AccountAuthService(
                authenticationManager,
                jwtTokenService,
                accountMapper,
                accountLoginPolicy
        );
    }

    @Test
    void login_shouldRecordSuccessAndReturnBearerToken() {
        Account account = Account.builder()
                .username("testuser")
                .email("test@example.com")
                .build();
        AccountResponse accountResponse = AccountResponse.builder()
                .username("testuser")
                .email("test@example.com")
                .build();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserAccountDetails(account));
        when(jwtTokenService.generateAccessToken(account)).thenReturn("jwt-token");
        when(jwtTokenService.getAccessTokenExpiresInSeconds()).thenReturn(1800L);
        when(accountMapper.buildAccountResponse(account)).thenReturn(accountResponse);

        LoginResponse response = authService.login(new LoginRequest("testuser", "StrongPass@123"));

        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(1800L, response.expiresIn());
        assertSame(accountResponse, response.account());
        verify(accountLoginPolicy).recordSuccessfulLogin(account);
    }

    @Test
    void login_shouldRecordFailedLoginAndRethrow_whenCredentialsAreInvalid() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        LoginRequest request = new LoginRequest("testuser", "WrongPass@123");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        verify(accountLoginPolicy).recordFailedLogin("testuser");
        verify(accountLoginPolicy, never()).recordSuccessfulLogin(any());
    }
}
