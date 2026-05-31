package com.mdounzaidi.portfolio_backend.account.security;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.account.service.AccountIdentifierNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountDetailsServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private UserAccountDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new UserAccountDetailsService(
                accountRepository,
                new AccountIdentifierNormalizer()
        );
    }

    @Test
    void loadUserByUsername_shouldNormalizeUsernameAndReturnUserDetails() {
        Account account = Account.builder()
                .username("testuser")
                .password("encoded-password")
                .build();
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        UserDetails userDetails = userDetailsService.loadUserByUsername(" TestUser ");

        assertInstanceOf(UserAccountDetails.class, userDetails);
        assertEquals("testuser", userDetails.getUsername());
        verify(accountRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_shouldThrowUsernameNotFoundException_whenAccountDoesNotExist() {
        when(accountRepository.findByUsername("missinguser")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(" MissingUser ")
        );
    }
}
