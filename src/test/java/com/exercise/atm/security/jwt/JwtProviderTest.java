package com.exercise.atm.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.exercise.atm.config.security.JwtProvider;

@SpringBootTest
@ActiveProfiles("test")
class JwtProviderTest {

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void generatesValidTokenAndExtractsCustomerId() {
        UUID customerId = UUID.randomUUID();
        String token = jwtProvider.generateToken(customerId, "Test User");

        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.extractCustomerId(token)).isEqualTo(customerId);
    }
}
