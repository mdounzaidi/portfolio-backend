package com.mdounzaidi.portfolio_backend.account.repository;

import com.mdounzaidi.portfolio_backend.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional <Account> findByUsername(String username);
    Optional <Account> findById(Long id);
    Optional <Account> findByEmail(String email);

}
