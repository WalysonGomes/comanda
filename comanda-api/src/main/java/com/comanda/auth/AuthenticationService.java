package com.comanda.auth;

import com.comanda.auth.domain.User;
import com.comanda.auth.domain.UserRepository;
import com.comanda.auth.security.PasswordHasher;
import com.comanda.auth.security.RefreshTokenService;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login by e-mail/password, resistant to user enumeration (D3): a missing e-mail still runs a
 * bcrypt comparison against a dummy hash, and every failure path (no such e-mail, wrong password,
 * inactive account) throws the same {@link InvalidCredentialsException}.
 *
 * <p>Rate limiting attaches here (task 4.3 / {@code reliability-and-security}): {@link #login}
 * is the single entry point every login attempt goes through.
 */
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationService(
            UserRepository userRepository, PasswordHasher passwordHasher, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public RefreshTokenService.Session login(String email, String password) {
        if (email == null || password == null) {
            throw new InvalidCredentialsException();
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Optional<User> maybeUser = userRepository.findByEmail(normalizedEmail);

        if (maybeUser.isEmpty()) {
            passwordHasher.matchesDummy(password);
            throw new InvalidCredentialsException();
        }

        User user = maybeUser.get();
        boolean passwordMatches = passwordHasher.matches(password, user.getPasswordHash());
        if (!passwordMatches || !user.isActive()) {
            throw new InvalidCredentialsException();
        }

        return refreshTokenService.issueNewSession(user);
    }
}
