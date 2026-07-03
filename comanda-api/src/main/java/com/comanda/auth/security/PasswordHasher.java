package com.comanda.auth.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bcrypt hashing for passwords (D3). {@link #matchesDummy} runs a real comparison against a
 * fixed dummy hash so a lookup miss costs the same time as a wrong password, closing the timing
 * side-channel that would otherwise reveal whether an e-mail is registered.
 */
@Component
public class PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final String dummyHash = encoder.encode("comanda-dummy-password-for-timing-safety");

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }

    public void matchesDummy(String rawPassword) {
        encoder.matches(rawPassword, dummyHash);
    }
}
