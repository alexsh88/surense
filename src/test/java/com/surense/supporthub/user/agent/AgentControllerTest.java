package com.surense.supporthub.user.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surense.supporthub.security.SecurityConfig;
import com.surense.supporthub.user.agent.dto.AgentResponse;
import com.surense.supporthub.user.agent.dto.CreateAgentRequest;
import com.surense.supporthub.user.agent.dto.UpdateAgentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@WebMvcTest(
        controllers = AgentController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
class AgentControllerTest {

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
    @MockBean AgentService agentService;

    private static final UUID AGENT_ID = UUID.randomUUID();

    @Test
    void postAgents_validRequest_returns201() throws Exception {
        AgentResponse resp = new AgentResponse(AGENT_ID, "alice", "New Agent", true, 0L);
        when(agentService.createAgent(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAgentRequest("alice", "Password1!", "New Agent"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(AGENT_ID.toString()))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void postAgents_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAgentRequest("", "Password1!", "New Agent"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postAgents_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAgentRequest("alice", "short", "New Agent"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAgents_returns200WithList() throws Exception {
        AgentResponse resp = new AgentResponse(AGENT_ID, "alice", "Alice", true, 0L);
        when(agentService.listAll()).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/v1/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    void patchAgentsId_validRequest_returns200() throws Exception {
        AgentResponse resp = new AgentResponse(AGENT_ID, "alice", "Updated Name", true, 1L);
        when(agentService.updateAgent(any(), any())).thenReturn(resp);

        mockMvc.perform(patch("/api/v1/agents/" + AGENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAgentRequest("Updated Name", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }
}
