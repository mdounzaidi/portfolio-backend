package com.mdounzaidi.portfolio_backend.account.controller;

import com.mdounzaidi.portfolio_backend.account.dto.AccountPassUpdateRequest;
import com.mdounzaidi.portfolio_backend.account.dto.AccountResponse;
import com.mdounzaidi.portfolio_backend.account.dto.AccountUpdateRequest;
import com.mdounzaidi.portfolio_backend.account.dto.InviteDetailsRequest;
import com.mdounzaidi.portfolio_backend.account.service.AccountInviteService;
import com.mdounzaidi.portfolio_backend.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("api/auth/")
@AllArgsConstructor

public class AccountAuthController {

    private final AccountService accountService;
    private final AccountInviteService accountInviteService;


    @PostMapping("/invite")
    public ResponseEntity<String> inviteAccountUser( @Valid @RequestBody InviteDetailsRequest inviteDetailsRequest){
        return ResponseEntity.ok(accountInviteService.createInvite(inviteDetailsRequest));
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyDetails(){
        return ResponseEntity.ok(accountService.getCurrentAccountDetails());
    }

    @PostMapping("update")
    public ResponseEntity<AccountResponse> updateMyAccount(@Valid @RequestBody AccountUpdateRequest accountUpdateRequest){
        return ResponseEntity.ok(accountService.updateCurrentAccount(accountUpdateRequest));
    }

    @PostMapping("change-password")
    public ResponseEntity<String> updatePassword( @Valid @RequestBody AccountPassUpdateRequest pass){
        return ResponseEntity.ok(accountService.changePassword(pass));
    }

}
