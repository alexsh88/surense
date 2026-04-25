package com.surense.supporthub.auth;

import com.surense.supporthub.auth.dto.TokenResponse;
import com.surense.supporthub.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
class LoginControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    AuthService authService;

    @org.springframework.boot.test.context.TestConfiguration
    static class NoSecurity {
        @org.springframework.context.annotation.Bean
        WebSecurityCustomizer webSecurityCustomizer() {
            return web -> web.ignoring().requestMatchers("/**");
        }
    }

    @Test
    void login_happyPath_returns200WithTokens() throws Exception {
        when(authService.login(any(), any(), any()))
                .thenReturn(new TokenResponse("acc.tok", "Bearer", 900L, "ref.tok"));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user","password":"Secret@1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("acc.tok"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(authService.login(any(), any(), any())).thenThrow(new BadCredentialsException("bad"));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_blankUsername_returns400() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":"Secret@1"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_missingPassword_returns400() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
