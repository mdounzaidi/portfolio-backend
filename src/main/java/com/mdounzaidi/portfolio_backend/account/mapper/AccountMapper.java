package com.mdounzaidi.portfolio_backend.account.mapper;

import com.mdounzaidi.portfolio_backend.account.dto.AccountResponse;
import com.mdounzaidi.portfolio_backend.account.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse buildAccountResponse(Account acc) {
        return AccountResponse.builder()
                .id(acc.getId())
                .firstName(acc.getFirstName())
                .lastName(acc.getLastName())
                .username(acc.getUsername())
                .email(acc.getEmail())
                .createdAt(acc.getCreatedAt())
                .updatedAt(acc.getUpdatedAt())
                .accountRole(acc.getAccountRole())
                .active(acc.isActive())
                .emailVerified(acc.isEmailVerified())
                .accountNonLocked(acc.isAccountNonLocked())
                .failedLoginAttempts(acc.getFailedLoginAttempts())
                .lockedAt(acc.getLockedAt())
                .lastLoginAt(acc.getLastLoginAt())
                .build();
    }


}
