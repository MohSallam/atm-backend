package com.exercise.atm.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountSnapshotResponse(
        UUID customerId,
        String customerName,
        BigDecimal balance,
        BigDecimal dailyLimit,
        BigDecimal withdrawnToday,
        BigDecimal remainingDailyLimit) {}
