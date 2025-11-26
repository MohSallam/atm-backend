package com.exercise.atm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.exercise.atm.api.dto.AccountSnapshotResponse;
import com.exercise.atm.api.error.BusinessException;
import com.exercise.atm.domain.entity.Account;
import com.exercise.atm.domain.entity.Customer;
import com.exercise.atm.domain.entity.Transaction;
import com.exercise.atm.domain.entity.TransactionType;
import com.exercise.atm.domain.repository.AccountRepository;
import com.exercise.atm.domain.repository.CustomerRepository;
import com.exercise.atm.domain.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

        @Mock
        private AccountRepository accountRepository;

        @Mock
        private TransactionRepository transactionRepository;

        @Mock
        private CustomerRepository customerRepository;

        private Clock clock;

        private AccountServiceImpl accountService;

        private UUID customerId;
        private UUID accountId;
        private Account account;
        private Instant startOfDay;
        private Instant endOfDay;

        @BeforeEach
        void setUp() {
                clock = Clock.fixed(Instant.parse("2024-01-01T10:00:00Z"), ZoneOffset.UTC);

                startOfDay = LocalDate.ofInstant(clock.instant(), clock.getZone())
                                .atStartOfDay()
                                .toInstant(clock.getZone().getRules().getOffset(clock.instant()));

                endOfDay = startOfDay.plus(1, ChronoUnit.DAYS).minusNanos(1);

                customerId = UUID.randomUUID();
                accountId = UUID.randomUUID();

                account = new Account();
                account.setId(accountId);
                account.setCustomerId(customerId);
                account.setBalance(new BigDecimal("1000.00"));
                account.setDailyLimit(new BigDecimal("500.00"));

                accountService = new AccountServiceImpl(accountRepository, transactionRepository, customerRepository,
                                clock);
        }

        @Test
        void getSnapshot_returnsCurrentState() {
                when(accountRepository.findByCustomerId(customerId)).thenReturn(Optional.of(account));
                when(customerRepository.findById(customerId))
                                .thenReturn(Optional.of(
                                                new Customer(null, null, null, "Mike Albert", 0, null, null, null)));
                when(transactionRepository.sumWithdrawnToday(accountId, startOfDay, endOfDay))
                                .thenReturn(new BigDecimal("100.00"));

                AccountSnapshotResponse snapshot = accountService.getSnapshot(customerId);

                assertThat(snapshot.customerId()).isEqualTo(customerId);
                assertThat(snapshot.customerName()).isEqualTo("Mike Albert");
                assertThat(snapshot.balance()).isEqualByComparingTo("1000.00");
                assertThat(snapshot.dailyLimit()).isEqualByComparingTo("500.00");
                assertThat(snapshot.withdrawnToday()).isEqualByComparingTo("100.00");
        }

        @Test
        void deposit_increasesBalanceAndRecordsTransaction() {
                when(accountRepository.findOneByCustomerId(customerId)).thenReturn(Optional.of(account));
                when(customerRepository.findById(customerId))
                                .thenReturn(Optional.of(new com.exercise.atm.domain.entity.Customer(null, null, null,
                                                "Mike Albert", 0, null, null, null)));
                when(transactionRepository.sumWithdrawnToday(accountId, startOfDay, endOfDay))
                                .thenReturn(BigDecimal.ZERO);

                AccountSnapshotResponse response = accountService.deposit(customerId, new BigDecimal("200.00"));

                assertThat(response.balance()).isEqualByComparingTo("1200.00");
                ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
                verify(transactionRepository).save(txCaptor.capture());
                assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.DEPOSIT);
                assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo("200.00");
                assertThat(txCaptor.getValue().getBalanceAfter()).isEqualByComparingTo("1200.00");
        }

        @Test
        void withdraw_whenInsufficientFunds_throwsBusinessException() {
                account.setBalance(new BigDecimal("50.00"));
                when(accountRepository.findOneByCustomerId(customerId)).thenReturn(Optional.of(account));
                when(transactionRepository.sumWithdrawnToday(accountId, startOfDay, endOfDay))
                                .thenReturn(BigDecimal.ZERO);

                assertThatThrownBy(() -> accountService.withdraw(customerId, new BigDecimal("100.00")))
                                .isInstanceOf(BusinessException.class)
                                .extracting("status", "message")
                                .containsExactly(HttpStatus.CONFLICT, "Insufficient funds");
        }

        @Test
    void withdraw_whenExceedsDailyLimit_throwsBusinessException() {
        when(accountRepository.findOneByCustomerId(customerId)).thenReturn(Optional.of(account));
        when(transactionRepository.sumWithdrawnToday(accountId, startOfDay, endOfDay))
                .thenReturn(new BigDecimal("400.00"));

        assertThatThrownBy(() -> accountService.withdraw(customerId, new BigDecimal("200.00")))
                .isInstanceOf(BusinessException.class)
                .extracting("status", "message")
                .containsExactly(HttpStatus.CONFLICT, "Daily withdrawal limit exceeded");
    }

    @Test
    void getSnapshot_whenAccountMissing_throwsNotFound() {
        when(accountRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getSnapshot(customerId))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deposit_whenAccountMissing_throwsNotFound() {
        when(accountRepository.findOneByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.deposit(customerId, new BigDecimal("10.00")))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void withdraw_whenAccountMissing_throwsNotFound() {
        when(accountRepository.findOneByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.withdraw(customerId, new BigDecimal("10.00")))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void snapshot_whenCustomerMissing_throwsNotFound() {
        when(accountRepository.findByCustomerId(customerId)).thenReturn(Optional.of(account));
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getSnapshot(customerId))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
