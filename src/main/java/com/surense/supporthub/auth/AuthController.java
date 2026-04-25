package com.surense.supporthub.auth;

import com.surense.supporthub.auth.dto.LoginRequest;
import com.surense.supporthub.auth.dto.LogoutRequest;
import com.surense.supporthub.auth.dto.RefreshRequest;
import com.surense.supporthub.auth.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest http) {
        return ResponseEntity.ok(authService.login(request, http.getRemoteAddr(),
                http.getHeader("User-Agent")));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                                 HttpServletRequest http) {
        return ResponseEntity.ok(authService.refresh(request, http.getRemoteAddr(),
                http.getHeader("User-Agent")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request,
                                       HttpServletRequest http) {
        authService.logout(request, http.getRemoteAddr(), http.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }
}
