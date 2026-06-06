package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.dto.LoginRequest;
import com.mdounzaidi.portfolio_backend.account.dto.LoginResponse;
import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.mapper.AccountMapper;
import com.mdounzaidi.portfolio_backend.account.security.JwtTokenService;
import com.mdounzaidi.portfolio_backend.account.security.UserAccountDetails;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AccountAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final AccountMapper accountMapper;
    private final AccountLoginPolicy accountLoginPolicy;

    public AccountAuthService(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            AccountMapper accountMapper,
            AccountLoginPolicy accountLoginPolicy
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.accountMapper = accountMapper;
        this.accountLoginPolicy = accountLoginPolicy;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            loginRequest.identifier(),
                            loginRequest.password()
                    )
            );
        } catch (BadCredentialsException ex) {
            accountLoginPolicy.recordFailedLogin(loginRequest.identifier());
            throw ex;
        }

        UserAccountDetails principal = (UserAccountDetails) authentication.getPrincipal();
        Account account = principal.getAccount();
        accountLoginPolicy.recordSuccessfulLogin(account);
        String accessToken = jwtTokenService.generateAccessToken(account);

        return LoginResponse.bearer(
                accessToken,
                jwtTokenService.getAccessTokenExpiresInSeconds(),
                accountMapper.buildAccountResponse(account)
        );
    }
}
