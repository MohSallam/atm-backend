package com.exercise.atm.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record AmountRequest(
        @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount) {
}
