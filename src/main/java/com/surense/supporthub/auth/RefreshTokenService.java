package com.surense.supporthub.auth;

import com.surense.supporthub.auth.domain.RefreshToken;
import com.surense.supporthub.auth.domain.RefreshTokenRepository;
import com.surense.supporthub.auth.dto.TokenResponse;
import com.surense.supporthub.common.exception.InvalidRefreshTokenException;
import com.surense.supporthub.security.AppUserPrincipal;
import com.surense.supporthub.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-token-expiry-seconds}")
    private long refreshTokenExpirySeconds;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String issueNewFamily(UUID userId, String ip, String userAgent) {
        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        Instant now = Instant.now();
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(sha256Hex(raw))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(refreshTokenExpirySeconds))
                .ipAddress(ip)
                .userAgent(userAgent)
                .build());
        return HexFormat.of().formatHex(raw);
    }

    @Transactional
    public TokenResponse rotate(String rawToken, String ip, String userAgent) {
        byte[] rawBytes = HexFormat.of().parseHex(rawToken);
        Optional<RefreshToken> opt = refreshTokenRepository.findByTokenHashForUpdate(sha256Hex(rawBytes));

        if (opt.isEmpty()) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken stored = opt.get();
        Instant now = Instant.now();

        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(now)) {
            throw new InvalidRefreshTokenException();
        }

        byte[] newRaw = new byte[32];
        secureRandom.nextBytes(newRaw);
        RefreshToken next = RefreshToken.builder()
                .userId(stored.getUserId())
                .tokenHash(sha256Hex(newRaw))
                .issuedAt(now)
                .expiresAt(stored.getExpiresAt())
                .ipAddress(ip)
                .userAgent(userAgent)
                .build();
        refreshTokenRepository.save(next);

        AppUserPrincipal principal = userRepository.findById(stored.getUserId())
                .map(AppUserPrincipal::from)
                .orElseThrow(InvalidRefreshTokenException::new);

        return new TokenResponse(
                jwtService.generateAccessToken(principal),
                "Bearer",
                jwtService.getAccessTokenExpirySeconds(),
                HexFormat.of().formatHex(newRaw));
    }

    @Transactional
    public void revokeByToken(String rawToken, String ip, String userAgent) {
        byte[] rawBytes = HexFormat.of().parseHex(rawToken);
        refreshTokenRepository.findByTokenHashForUpdate(sha256Hex(rawBytes))
                .filter(t -> t.getRevokedAt() == null)
                .ifPresent(t -> t.setRevokedAt(Instant.now()));
    }

    String sha256Hex(byte[] input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
