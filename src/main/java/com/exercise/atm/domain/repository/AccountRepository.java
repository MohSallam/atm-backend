package com.exercise.atm.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.exercise.atm.domain.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;


@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByCustomerId(UUID customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findOneByCustomerId(UUID customerId);
}
