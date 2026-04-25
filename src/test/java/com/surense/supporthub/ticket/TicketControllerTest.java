package com.surense.supporthub.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surense.supporthub.security.AppUserPrincipal;
import com.surense.supporthub.security.SecurityConfig;
import com.surense.supporthub.ticket.domain.TicketPriority;
import com.surense.supporthub.ticket.domain.TicketStatus;
import com.surense.supporthub.ticket.dto.CreateTicketRequest;
import com.surense.supporthub.ticket.dto.TicketResponse;
import com.surense.supporthub.ticket.dto.UpdateTicketRequest;
import com.surense.supporthub.user.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TicketController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
class TicketControllerTest {

    @TestConfiguration
    static class PermitAll {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TicketService ticketService;

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID TICKET_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken customerAuth() {
        AppUserPrincipal p = new AppUserPrincipal(CUSTOMER_ID, "c@example.com", null, Role.CUSTOMER, null, true);
        return new UsernamePasswordAuthenticationToken(p, null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    void postTickets_validRequest_returns201() throws Exception {
        TicketResponse resp = new TicketResponse(TICKET_ID, "Login issue", "Can't login",
                TicketStatus.OPEN, TicketPriority.HIGH, CUSTOMER_ID, 0L);
        when(ticketService.createTicket(any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/tickets")
                        .with(authentication(customerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTicketRequest("Login issue", "Can't login", TicketPriority.HIGH))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TICKET_ID.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void postTickets_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .with(authentication(customerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTicketRequest("", "Desc", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTickets_returns200WithList() throws Exception {
        TicketResponse resp = new TicketResponse(TICKET_ID, "T", "D",
                TicketStatus.OPEN, TicketPriority.MEDIUM, CUSTOMER_ID, 0L);
        when(ticketService.findAll(any(), any(), any(), any()))
                .thenReturn(List.of(resp));

        mockMvc.perform(get("/api/v1/tickets").with(authentication(customerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("T"));
    }

    @Test
    void getTicketsId_returns200() throws Exception {
        TicketResponse resp = new TicketResponse(TICKET_ID, "T", "D",
                TicketStatus.OPEN, TicketPriority.MEDIUM, CUSTOMER_ID, 0L);
        when(ticketService.findById(any())).thenReturn(resp);

        mockMvc.perform(get("/api/v1/tickets/" + TICKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TICKET_ID.toString()));
    }

    @Test
    void patchTicketsId_validRequest_returns200() throws Exception {
        TicketResponse resp = new TicketResponse(TICKET_ID, "T", "D",
                TicketStatus.RESOLVED, TicketPriority.MEDIUM, CUSTOMER_ID, 1L);
        when(ticketService.updateTicket(any(), any())).thenReturn(resp);

        mockMvc.perform(patch("/api/v1/tickets/" + TICKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateTicketRequest(null, null, TicketStatus.RESOLVED, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }
}
