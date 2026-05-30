package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.dto.AccountRequest;
import com.mdounzaidi.portfolio_backend.account.dto.AccountResponse;
import com.mdounzaidi.portfolio_backend.account.dto.InviteDetailsRequest;
import com.mdounzaidi.portfolio_backend.account.entity.Account;
import com.mdounzaidi.portfolio_backend.account.entity.AccountRole;
import com.mdounzaidi.portfolio_backend.account.entity.InviteDetails;
import com.mdounzaidi.portfolio_backend.account.entity.VerificationToken;
import com.mdounzaidi.portfolio_backend.account.mapper.AccountMapper;
import com.mdounzaidi.portfolio_backend.account.repository.AccountRepository;
import com.mdounzaidi.portfolio_backend.account.repository.InviteRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;


@AllArgsConstructor
@Component
public class AccountInviteService {

    private final AccountTokenService tokenService;
    private final AccountMailService mailService;
    private final AccountRepository accountRepository;
    private final InviteRepository inviteRepository;
    private final AccountService accountService;
    private final AccountMapper accountMapper;


    public InviteDetails sendInvite(Account account, InviteDetailsRequest inviteDetailsRequest) {
        VerificationToken token= tokenService.createToken(account,(10*60*60));
        InviteDetails inviteDetails=buildInviteDetails(account,token,inviteDetailsRequest);
        inviteRepository.save(inviteDetails);
        mailService.sendAccountInviteMail(inviteDetailsRequest.getEmail(),token.getToken());
        return inviteDetails;
    }

    public InviteDetails buildInviteDetails(Account account, VerificationToken token,InviteDetailsRequest inviteDetailsRequest){
        InviteDetails inviteDetails=InviteDetails.builder()
                .createdAt(LocalDateTime.now())
                .inviteBy(account)
                .active(false)
                .verificationToken(token)
                .name(inviteDetailsRequest.getName())
                .email(inviteDetailsRequest.getEmail())
                .accountRole(inviteDetailsRequest.getAccountRole())
                .build();
        return inviteDetails;
    }



    @Transactional
    public AccountResponse completeInvite(AccountRequest accountRequest, String token) {
        VerificationToken verificationToken=tokenService.findToken(token);
        if(!tokenService.isTokenValid(verificationToken)) {
            throw new RuntimeException("Expired token");
        }
        InviteDetails inviteDetails=inviteRepository.findByVerificationToken(verificationToken).orElseThrow(()-> new RuntimeException("No Invite Details Found"));
        if(!inviteDetails.getEmail().equalsIgnoreCase(accountRequest.getEmail()))
            throw new RuntimeException("Please try with correct email");

        if(inviteDetails.isAccountCreated())
            throw new RuntimeException("Account already Created");

        if(!inviteDetails.isActive())
            throw new RuntimeException("Account is not active anymore");


        Account account=accountService.buildAccount(accountRequest);
        account.setEmailVerified(true);
        account.setActive(true);
        if (inviteDetails.getAccountRole() == AccountRole.ROLE_ADMIN) {
            account.getAccountRole().add(AccountRole.ROLE_WRITER);
        }

        if (inviteDetails.getAccountRole() == AccountRole.ROLE_SUPERADMIN) {
            account.getAccountRole().addAll(
                    Set.of(AccountRole.ROLE_WRITER, AccountRole.ROLE_ADMIN)
            );
        }

        account.getAccountRole().add(inviteDetails.getAccountRole());
        Account saveAccount=accountRepository.save(account);

        inviteDetails.setVerificationToken(null);
        inviteDetails.setActive(false);
        inviteDetails.setAccountCreated(true);
        inviteRepository.save(inviteDetails);
        tokenService.deleteToken(verificationToken);

        return accountMapper.buildAccountResponse(saveAccount);
    }


    @Transactional
    public String createInvite(InviteDetailsRequest inviteDetailsRequest) {
        Account account=accountService.getCurrentAccount();

        AccountRole inviteRole=inviteDetailsRequest.getAccountRole();
        Set<AccountRole> accountRole=account.getAccountRole();

        if(inviteRole==AccountRole.ROLE_WRITER&&
                !accountRole.contains(AccountRole.ROLE_ADMIN)){
            return "you are not authorised to invite this account";
        }

        if((inviteRole==AccountRole.ROLE_ADMIN||inviteRole==AccountRole.ROLE_SUPERADMIN)&&
                !accountRole.contains(AccountRole.ROLE_SUPERADMIN)){
            return "you are not authorised to invite this account";
        }
        InviteDetails inviteDetails=sendInvite(account,inviteDetailsRequest);

        return "we have sent invitation link";
    }
}
