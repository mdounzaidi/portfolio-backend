package com.mdounzaidi.portfolio_backend.account.controller;

import com.mdounzaidi.portfolio_backend.account.dto.AccountPassUpdateRequest;
import com.mdounzaidi.portfolio_backend.account.dto.AccountResponse;
import com.mdounzaidi.portfolio_backend.account.dto.AccountUpdateRequest;
import com.mdounzaidi.portfolio_backend.account.dto.InviteDetailsRequest;
import com.mdounzaidi.portfolio_backend.account.service.AccountInviteService;
import com.mdounzaidi.portfolio_backend.account.service.AccountService;
import com.mdounzaidi.portfolio_backend.common.dto.MessageResponse;
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

    @PostMapping({"/invite", "/invites"})
    public ResponseEntity<MessageResponse> inviteAccountUser(@Valid @RequestBody InviteDetailsRequest inviteDetailsRequest) {
        return ResponseEntity
                .accepted()
                .body(new MessageResponse(accountInviteService.createInvite(inviteDetailsRequest)));
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyDetails() {
        return ResponseEntity.ok(accountService.getCurrentAccountDetails());
    }

    @RequestMapping(path = {"update", "me"}, method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<AccountResponse> updateMyAccount(@Valid @RequestBody AccountUpdateRequest accountUpdateRequest) {
        return ResponseEntity.ok(accountService.updateCurrentAccount(accountUpdateRequest));
    }

    @RequestMapping(path = {"change-password", "me/password"}, method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody AccountPassUpdateRequest pass) {
        accountService.changePassword(pass);
        return ResponseEntity.noContent().build();
    }
}
