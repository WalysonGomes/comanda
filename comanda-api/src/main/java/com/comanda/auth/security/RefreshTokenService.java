package com.comanda.auth.security;

import com.comanda.auth.InvalidRefreshTokenException;
import com.comanda.auth.domain.User;
import com.comanda.auth.domain.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and rotates the refresh-token half of a session (D4). The current refresh token's hash
 * is the only thing stored per user, so a rotation simply overwrites it; presenting anything
 * other than that exact (unexpired) hash is treated as reuse of an already-rotated token and
 * clears the stored hash too, forcing a fresh login rather than leaving a stale session valid.
 */
@Component
public class RefreshTokenService {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    public RefreshTokenService(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    public record Session(User user, String accessToken, String refreshToken) {
    }

    @Transactional
    public Session issueNewSession(User user) {
        String accessToken = tokenService.issueAccessToken(user);
        String refreshToken = tokenService.issueRefreshToken(user);
        user.setRefreshToken(hash(refreshToken), OffsetDateTime.now().plus(tokenService.refreshTtl()));
        userRepository.save(user);
        return new Session(user, accessToken, refreshToken);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Session rotate(String presentedRefreshToken) {
        Long userId = tokenService.validateRefreshTokenAndGetUserId(presentedRefreshToken)
                .orElseThrow(InvalidRefreshTokenException::new);
        User user = userRepository.findById(userId).orElseThrow(InvalidRefreshTokenException::new);

        String presentedHash = hash(presentedRefreshToken);
        boolean valid = presentedHash.equals(user.getRefreshTokenHash())
                && user.getRefreshTokenExpiresAt() != null
                && user.getRefreshTokenExpiresAt().isAfter(OffsetDateTime.now());

        if (!valid) {
            user.clearRefreshToken();
            userRepository.save(user);
            throw new InvalidRefreshTokenException();
        }

        return issueNewSession(user);
    }

    public Duration refreshTtl() {
        return tokenService.refreshTtl();
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
