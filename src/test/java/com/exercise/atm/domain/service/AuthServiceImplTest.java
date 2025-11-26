package com.exercise.atm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.exercise.atm.api.error.BusinessException;
import com.exercise.atm.config.security.JwtProvider;
import com.exercise.atm.domain.entity.Customer;
import com.exercise.atm.domain.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    private Clock clock;

    private AuthServiceImpl authService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2024-01-01T10:00:00Z"), ZoneOffset.UTC);
        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCardNumber("4111111111111111");
        customer.setPinHash("hash");
        customer.setName("Alice");
        customer.setFailedAttempts(0);
        customer.setLockedUntil(null);

        authService = new AuthServiceImpl(customerRepository, passwordEncoder, jwtProvider, clock);
        authService.init();
    }

    @Test
    void loginSuccessResetsFailedAttempts() {
        when(customerRepository.findByCardNumber(customer.getCardNumber())).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);
        when(jwtProvider.generateToken(customer.getId(), customer.getName())).thenReturn("token");

        LoginResult result = authService.login(customer.getCardNumber(), "1234");

        assertThat(result.customerId()).isEqualTo(customer.getId());
        assertThat(result.accessToken()).isEqualTo("token");
        assertThat(result.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void exceededFailedAttemptsLocksAccount() {
        customer.setFailedAttempts(2);
        when(customerRepository.findByCardNumber(customer.getCardNumber())).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(customer.getCardNumber(), "bad"))
                .isInstanceOf(BusinessException.class)
                .extracting("status", "message")
                .containsExactly(HttpStatus.LOCKED, "Account Locked, try again later");
    }

    @Test
    void alreadyLockedAccountThrowsLocked() {
        customer.setLockedUntil(Instant.now(clock).plus(Duration.ofMinutes(5)));
        when(customerRepository.findByCardNumber(customer.getCardNumber())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> authService.login(customer.getCardNumber(), "1234"))
                .isInstanceOf(BusinessException.class)
                .extracting("status", "message")
                .containsExactly(HttpStatus.LOCKED, "Account Locked, try again later");
    }
}
