package com.mdounzaidi.portfolio_backend.account.dto;

import com.mdounzaidi.portfolio_backend.account.entity.AccountRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InviteDetailsRequest {

    @NotNull
    AccountRole accountRole;
    @NotBlank
    @Size(max = 20)
    String name;

    @NotBlank
    @Email
    String email;

}
