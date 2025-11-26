package com.exercise.atm.api.controller;

import com.exercise.atm.api.dto.LoginRequest;
import com.exercise.atm.api.dto.LoginResponse;
import com.exercise.atm.domain.service.AuthService;
import com.exercise.atm.domain.service.LoginResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResult result = authService.login(request.cardNumber(), request.pin());
        return ResponseEntity.ok(
                new LoginResponse(
                        result.accessToken(), "Bearer", result.expiresInSeconds(), result.customerId(), result.customerName()));
    }
}
