package com.exercise.atm.domain.service;

import java.util.UUID;

public record LoginResult(UUID customerId, String customerName, String accessToken, long expiresInSeconds) {}
