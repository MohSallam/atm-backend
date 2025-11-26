package com.exercise.atm.domain.service;

import com.exercise.atm.api.dto.AccountSnapshotResponse;
import java.math.BigDecimal;
import java.util.UUID;

public interface AccountService {

    /**
     * Returns the current account snapshot for the authenticated customer (balance, limits, totals).
     *
     * @param customerId authenticated customer id from the JWT
     * @return {@link com.exercise.atm.api.dto.AccountSnapshotResponse} for the current account state
     * @throws com.exercise.atm.api.error.BusinessException when the account is not found
     */
    AccountSnapshotResponse getSnapshot(UUID customerId);

    /**
     * Adds funds to the customer's account and returns the updated snapshot.
     *
     * @param customerId authenticated customer id from the JWT
     * @param amount amount to deposit (positive)
     * @return updated snapshot after deposit
     * @throws com.exercise.atm.api.error.BusinessException when the account is not found
     */
    AccountSnapshotResponse deposit(UUID customerId, BigDecimal amount);

    /**
     * Withdraws funds while enforcing balance and daily limit constraints; returns the updated snapshot.
     *
     * @param customerId authenticated customer id from the JWT
     * @param amount amount to withdraw (positive)
     * @return updated snapshot after withdrawal
     * @throws com.exercise.atm.api.error.BusinessException when account is missing, funds are insufficient, or daily
     *     limit is exceeded
     */
    AccountSnapshotResponse withdraw(UUID customerId, BigDecimal amount);
}
