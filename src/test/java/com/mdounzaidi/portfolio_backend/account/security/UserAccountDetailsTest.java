package com.mdounzaidi.portfolio_backend.account.security;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.AccountRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountDetailsTest {

    @Test
    void userDetails_shouldExposeAccountSecurityFields() {
        Account account = Account.builder()
                .username("testuser")
                .password("encoded-password")
                .active(false)
                .accountNonLocked(false)
                .build();
        account.setAccountRole(Set.of(AccountRole.ROLE_USER, AccountRole.ROLE_WRITER));

        UserAccountDetails userDetails = new UserAccountDetails(account);

        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertFalse(userDetails.isEnabled());
        assertFalse(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList()
                .containsAll(Set.of("ROLE_USER", "ROLE_WRITER")));
    }
}
