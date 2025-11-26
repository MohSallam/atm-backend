package com.exercise.atm.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exercise.atm.api.dto.AccountSnapshotResponse;
import com.exercise.atm.api.error.BusinessException;
import com.exercise.atm.domain.entity.Account;
import com.exercise.atm.domain.entity.Transaction;
import com.exercise.atm.domain.entity.TransactionType;
import com.exercise.atm.domain.repository.AccountRepository;
import com.exercise.atm.domain.repository.CustomerRepository;
import com.exercise.atm.domain.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public AccountSnapshotResponse getSnapshot(UUID customerId) {
        Account account = accountRepository
                .findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException("Account not found", HttpStatus.NOT_FOUND));

        Instant now = Instant.now(clock);
        Instant startOfDay = startOfDay(now);
        Instant endOfDay = endOfDay(now);

        BigDecimal withdrawnToday = transactionRepository.sumWithdrawnToday(account.getId(), startOfDay, endOfDay);
        
        if (withdrawnToday == null) withdrawnToday = BigDecimal.ZERO;

        BigDecimal remainingLimit = account.getDailyLimit().subtract(withdrawnToday).max(BigDecimal.ZERO);

        return new AccountSnapshotResponse(
                account.getCustomerId(),
                resolveCustomerName(account.getCustomerId()),
                account.getBalance(),
                account.getDailyLimit(),
                withdrawnToday,
                remainingLimit);
    }

    @Override
    @Transactional
    public AccountSnapshotResponse deposit(UUID customerId, BigDecimal amount) {
        Account account = accountRepository
                .findOneByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException("Account not found", HttpStatus.NOT_FOUND));

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);

        Instant now = Instant.now(clock);

        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getId());
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(amount);
        transaction.setOccurredAt(now);
        transaction.setBalanceAfter(newBalance);
        transactionRepository.save(transaction);

        Instant startOfDay = startOfDay(now);
        Instant endOfDay = endOfDay(now);
        
        BigDecimal withdrawnToday =
                transactionRepository.sumWithdrawnToday(account.getId(), startOfDay, endOfDay);
        
        if (withdrawnToday == null) withdrawnToday = BigDecimal.ZERO;

        BigDecimal remainingLimit = account.getDailyLimit().subtract(withdrawnToday).max(BigDecimal.ZERO);

        return new AccountSnapshotResponse(
                account.getCustomerId(),
                resolveCustomerName(account.getCustomerId()),
                newBalance,
                account.getDailyLimit(),
                withdrawnToday,
                remainingLimit);
    }

    @Override
    @Transactional
    public AccountSnapshotResponse withdraw(UUID customerId, BigDecimal amount) {
        Account account = accountRepository
                .findOneByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException("Account not found", HttpStatus.NOT_FOUND));

        Instant now = Instant.now(clock);
        Instant startOfDay = startOfDay(now);
        Instant endOfDay = endOfDay(now);

        BigDecimal withdrawnToday = transactionRepository.sumWithdrawnToday(account.getId(), startOfDay, endOfDay);
        if (withdrawnToday == null) {
            withdrawnToday = BigDecimal.ZERO;
        }

        BigDecimal remainingLimit = account.getDailyLimit().subtract(withdrawnToday);

        if (amount.compareTo(account.getBalance()) > 0) {
            throw new BusinessException("Insufficient funds", HttpStatus.CONFLICT);
        }

        if (amount.compareTo(remainingLimit) > 0) {
            throw new BusinessException("Daily withdrawal limit exceeded", HttpStatus.CONFLICT);
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);

        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getId());
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setAmount(amount);
        transaction.setOccurredAt(now);
        transaction.setBalanceAfter(newBalance);
        transactionRepository.save(transaction);

        withdrawnToday = withdrawnToday.add(amount);

        remainingLimit = account.getDailyLimit().subtract(withdrawnToday).max(BigDecimal.ZERO);

        return new AccountSnapshotResponse(
                account.getCustomerId(),
                resolveCustomerName(account.getCustomerId()),
                newBalance,
                account.getDailyLimit(),
                withdrawnToday,
                remainingLimit);
    }

    private Instant startOfDay(Instant instant) {
        return LocalDate.ofInstant(instant, ZoneOffset.UTC)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
    }

    private Instant endOfDay(Instant instant) {
        return LocalDate.ofInstant(instant, ZoneOffset.UTC)
                .plusDays(1)
                .atStartOfDay()
                .minusNanos(1)
                .toInstant(ZoneOffset.UTC);
    }

    private String resolveCustomerName(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .map(c -> c.getName())
                .orElseThrow(() -> new BusinessException("Customer not found", HttpStatus.NOT_FOUND));
    }
}
