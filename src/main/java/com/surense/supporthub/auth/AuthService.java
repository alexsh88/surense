package com.surense.supporthub.auth;

import com.surense.supporthub.auth.dto.LoginRequest;
import com.surense.supporthub.auth.dto.LogoutRequest;
import com.surense.supporthub.auth.dto.RefreshRequest;
import com.surense.supporthub.auth.dto.TokenResponse;
import com.surense.supporthub.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public TokenResponse login(LoginRequest request, String ip, String userAgent) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
        log.info("Login [userId={}, role={}, ip={}]", principal.userId(), principal.role(), ip);
        String accessToken = jwtService.generateAccessToken(principal);
        String rawRefreshToken = refreshTokenService.issueNewFamily(principal.userId(), ip, userAgent);
        return new TokenResponse(accessToken, "Bearer", jwtService.getAccessTokenExpirySeconds(), rawRefreshToken);
    }

    public TokenResponse refresh(RefreshRequest request, String ip, String userAgent) {
        return refreshTokenService.rotate(request.refreshToken(), ip, userAgent);
    }

    public void logout(LogoutRequest request, String ip, String userAgent) {
        log.info("Logout [ip={}]", ip);
        refreshTokenService.revokeByToken(request.refreshToken(), ip, userAgent);
    }
}
