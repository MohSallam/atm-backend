package com.exercise.atm.api.controller;

import com.exercise.atm.api.dto.AccountSnapshotResponse;
import com.exercise.atm.api.dto.AmountRequest;
import com.exercise.atm.domain.service.AccountService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public AccountSnapshotResponse getSnapshot(@AuthenticationPrincipal UUID customerId) {
        return accountService.getSnapshot(customerId);
    }

    @PostMapping("/deposit")
    public AccountSnapshotResponse deposit(
            @AuthenticationPrincipal UUID customerId, @Valid @RequestBody AmountRequest request) {
        return accountService.deposit(customerId, request.amount());
    }

    @PostMapping("/withdraw")
    public AccountSnapshotResponse withdraw(
            @AuthenticationPrincipal UUID customerId, @Valid @RequestBody AmountRequest request) {
        return accountService.withdraw(customerId, request.amount());
    }
}
