package com.exercise.atm.api.dto;

import java.util.UUID;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, UUID customerId, String customerName) {}
