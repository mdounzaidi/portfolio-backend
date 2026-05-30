package com.mdounzaidi.portfolio_backend.account.dto;

import com.mdounzaidi.portfolio_backend.account.entity.AccountRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountResponse  {
    private long id;
    private String firstName;
    private  String lastName;
    private String username;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<AccountRole> accountRole;
    private boolean active;
    private boolean emailVerified;
    private boolean accountNonLocked;
    private int failedLoginAttempts;
    private LocalDateTime lockedAt;
    private LocalDateTime lastLoginAt;
}
