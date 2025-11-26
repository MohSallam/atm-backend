package com.exercise.atm.domain.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exercise.atm.api.error.BusinessException;
import com.exercise.atm.config.security.JwtProvider;
import com.exercise.atm.domain.entity.Customer;
import com.exercise.atm.domain.repository.CustomerRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final Clock clock;

    @Value("${security.auth.max-failed-attempts:3}")
    private int maxFailedAttempts;

    @Value("${security.auth.lock-duration-minutes:15}")
    private int lockDurationMinutes;

    @Value("${security.jwt.expiration-seconds:3600}")
    private long tokenExpirationSeconds = 3600;

    private Duration lockDuration;

    @PostConstruct
    void init() {
        this.lockDuration = Duration.ofMinutes(lockDurationMinutes);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResult login(String cardNumber, String pin) {
        Customer customer = customerRepository
                .findByCardNumber(cardNumber)
                .orElseThrow(() -> new BusinessException("Invalid Card", HttpStatus.UNAUTHORIZED));

        Instant now = Instant.now(clock);

        if (customer.getLockedUntil() != null) {
            if (customer.getLockedUntil().isAfter(now)) {
                throw new BusinessException("Account Locked, try again later", HttpStatus.LOCKED);
            }
            // Lock window has expired, forgive past failures
            customer.setFailedAttempts(0);
            customer.setLockedUntil(null);
            customerRepository.save(customer);
        }

        if (!passwordEncoder.matches(pin, customer.getPinHash())) {
            int failedAttempts = customer.getFailedAttempts() + 1;
            customer.setFailedAttempts(failedAttempts);
            if (failedAttempts >= maxFailedAttempts) {
                customer.setLockedUntil(now.plus(lockDuration));
                customerRepository.save(customer);
                throw new BusinessException("Account Locked, try again later", HttpStatus.LOCKED);
            }
            customerRepository.save(customer);
            throw new BusinessException("Invalid PIN", HttpStatus.UNAUTHORIZED);
        }

        customer.setFailedAttempts(0);
        customer.setLockedUntil(null);
        customerRepository.save(customer);

        String accessToken = jwtProvider.generateToken(customer.getId(), customer.getName());
        return new LoginResult(customer.getId(), customer.getName(), accessToken, tokenExpirationSeconds);
    }
}
