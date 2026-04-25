package com.surense.supporthub.auth;

import com.surense.supporthub.auth.domain.RefreshToken;
import com.surense.supporthub.auth.domain.RefreshTokenRepository;
import com.surense.supporthub.auth.dto.TokenResponse;
import com.surense.supporthub.common.exception.InvalidRefreshTokenException;
import com.surense.supporthub.user.domain.Role;
import com.surense.supporthub.user.domain.User;
import com.surense.supporthub.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;
    @InjectMocks RefreshTokenService service;

    private static final String IP = "127.0.0.1";
    private static final String UA = "TestAgent/1.0";

    private byte[] rawBytes;
    private String rawHex;
    private String hash;

    @BeforeEach
    void setUp() {
        rawBytes = new byte[32];
        for (int i = 0; i < rawBytes.length; i++) rawBytes[i] = (byte) i;
        rawHex = HexFormat.of().formatHex(rawBytes);
        hash = service.sha256Hex(rawBytes);
    }

    @Test
    void rotate_unknownToken_throwsInvalidRefreshToken() {
        when(refreshTokenRepository.findByTokenHashForUpdate(hash)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate(rawHex, IP, UA))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotate_revokedToken_throwsInvalidRefreshToken() {
        RefreshToken stored = storedToken(UUID.randomUUID(), Instant.now().plusSeconds(86400));
        stored.setRevokedAt(Instant.now().minusSeconds(30));

        when(refreshTokenRepository.findByTokenHashForUpdate(hash)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.rotate(rawHex, IP, UA))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotate_expiredToken_throwsInvalidRefreshToken() {
        RefreshToken stored = storedToken(UUID.randomUUID(), Instant.now().minusSeconds(1));

        when(refreshTokenRepository.findByTokenHashForUpdate(hash)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.rotate(rawHex, IP, UA))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotate_happyPath_savesNewToken_returnsResponse() {
        UUID userId = UUID.randomUUID();
        Instant expiry = Instant.now().plusSeconds(86400);
        RefreshToken stored = storedToken(userId, expiry);

        when(refreshTokenRepository.findByTokenHashForUpdate(hash)).thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.of(stubUser(userId)));
        when(jwtService.generateAccessToken(any())).thenReturn("new.access.token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);

        TokenResponse response = service.rotate(rawHex, IP, UA);

        assertThat(response.accessToken()).isEqualTo("new.access.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.refreshToken()).isNotBlank().isNotEqualTo(rawHex);
        assertThat(stored.getRevokedAt()).isNull();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void rotate_userDeletedAfterTokenIssued_throwsInvalidRefreshToken() {
        UUID userId = UUID.randomUUID();
        RefreshToken stored = storedToken(userId, Instant.now().plusSeconds(86400));

        when(refreshTokenRepository.findByTokenHashForUpdate(hash)).thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate(rawHex, IP, UA))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    // ---- helpers ----

    private RefreshToken storedToken(UUID userId, Instant expiresAt) {
        return RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash)
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(expiresAt)
                .build();
    }

    private User stubUser(UUID userId) {
        return User.builder()
                .id(userId)
                .username("testuser")
                .passwordHash("hash")
                .fullName("Test User")
                .role(Role.CUSTOMER)
                .build();
    }
}
