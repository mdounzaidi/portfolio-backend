package com.mdounzaidi.portfolio_backend.account.service;

import com.mdounzaidi.portfolio_backend.account.dto.AccountRequest;
import com.mdounzaidi.portfolio_backend.account.dto.AccountResponse;
import com.mdounzaidi.portfolio_backend.account.dto.InviteDetailsRequest;
import com.mdounzaidi.portfolio_backend.account.entity.*;
import com.mdounzaidi.portfolio_backend.account.exception.AccountAuthorizationException;
import com.mdounzaidi.portfolio_backend.account.exception.InviteNotValidException;
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
    private final AccountIdentifierNormalizer identifierNormalizer;


    public InviteDetails sendInvite(Account account, InviteDetailsRequest inviteDetailsRequest) {
        GeneratedToken generatedToken = tokenService.createToken(
                account,
                10 * 60 * 60,
                TokenPurpose.ACCOUNT_INVITE
        );
        InviteDetails inviteDetails = buildInviteDetails(
                account,
                generatedToken.verificationToken(),
                inviteDetailsRequest
        );
        inviteRepository.save(inviteDetails);
        mailService.sendAccountInviteMail(inviteDetails.getEmail(), generatedToken.rawToken());
        return inviteDetails;
    }

    public InviteDetails buildInviteDetails(Account account, VerificationToken token,InviteDetailsRequest inviteDetailsRequest){
        InviteDetails inviteDetails=InviteDetails.builder()
                .createdAt(LocalDateTime.now())
                .inviteBy(account)
                .active(true)
                .accountCreated(false)
                .verificationToken(token)
                .name(inviteDetailsRequest.getName())
                .email(identifierNormalizer.email(inviteDetailsRequest.getEmail()))
                .accountRole(inviteDetailsRequest.getAccountRole())
                .build();
        return inviteDetails;
    }



    @Transactional
    public AccountResponse completeInvite(AccountRequest accountRequest, String token) {
        VerificationToken verificationToken = tokenService.findValidToken(
                token,
                TokenPurpose.ACCOUNT_INVITE
        );
        InviteDetails inviteDetails=inviteRepository.findByVerificationToken(verificationToken).orElseThrow(()->  new InviteNotValidException("Invite details not found"));
        String accountEmail = identifierNormalizer.email(accountRequest.getEmail());
        if(!inviteDetails.getEmail().equals(accountEmail))
            throw new InviteNotValidException("Please use the invited email address");

        if(inviteDetails.isAccountCreated())
            throw new InviteNotValidException("Account already created from this invite");

        if(!inviteDetails.isActive())
            throw new InviteNotValidException("Invite is no longer active");


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

        inviteDetails.setActive(false);
        inviteDetails.setAccountCreated(true);
        inviteRepository.save(inviteDetails);

        tokenService.markUsed(verificationToken);

        return accountMapper.buildAccountResponse(saveAccount);
    }


    @Transactional
    public String createInvite(InviteDetailsRequest inviteDetailsRequest) {
        Account account=accountService.getCurrentAccount();

        AccountRole inviteRole=inviteDetailsRequest.getAccountRole();
        Set<AccountRole> accountRole=account.getAccountRole();

        if(inviteRole==AccountRole.ROLE_WRITER&&
                !accountRole.contains(AccountRole.ROLE_ADMIN)){
            throw new AccountAuthorizationException("You are not authorized to invite this account role");
        }

        if((inviteRole==AccountRole.ROLE_ADMIN||inviteRole==AccountRole.ROLE_SUPERADMIN)&&
                !accountRole.contains(AccountRole.ROLE_SUPERADMIN)){
            throw new AccountAuthorizationException("You are not authorized to invite this account role");
        }
        InviteDetails inviteDetails=sendInvite(account,inviteDetailsRequest);

        return "we have sent invitation link";
    }
}
