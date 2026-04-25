package com.surense.supporthub.auth;

import com.surense.supporthub.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
class LogoutControllerTest {

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
    void logout_validToken_returns204() throws Exception {
        doNothing().when(authService).logout(any(), any(), any());

        mvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20"}
                                """))
                .andExpect(status().isNoContent());

        verify(authService).logout(any(), any(), any());
    }

    @Test
    void logout_missingToken_returns400() throws Exception {
        mvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_alwaysReturns204_evenForUnknownToken() throws Exception {
        doNothing().when(authService).logout(any(), any(), any());

        mvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"}
                                """))
                .andExpect(status().isNoContent());
    }
}
