package com.exercise.atm.domain.service;

public interface AuthService {

    /**
     * Authenticates a card/PIN pair, returning a JWT and customer metadata or throwing a BusinessException on
     * failure/lockout.
     *
     * @param cardNumber the card number provided by the caller
     * @param pin the plaintext PIN provided by the caller
     * @return {@link LoginResult} containing customer id/name, bearer token, and expiry
     * @throws com.exercise.atm.api.error.BusinessException when the card is invalid, PIN is wrong, or the account is
     *     locked
     */
    LoginResult login(String cardNumber, String pin);
}
