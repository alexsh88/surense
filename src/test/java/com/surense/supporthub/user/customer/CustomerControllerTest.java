package com.surense.supporthub.user.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surense.supporthub.security.AppUserPrincipal;
import com.surense.supporthub.security.SecurityConfig;
import com.surense.supporthub.user.customer.dto.CreateCustomerRequest;
import com.surense.supporthub.user.customer.dto.CustomerResponse;
import com.surense.supporthub.user.customer.dto.UpdateCustomerRequest;
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
        controllers = CustomerController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
class CustomerControllerTest {

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
    @MockBean CustomerService customerService;

    private static final UUID AGENT_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Test
    void postCustomers_asAgent_returns201() throws Exception {
        CustomerResponse resp = new CustomerResponse(CUSTOMER_ID, "carol", "Carol", AGENT_ID, true, 0L);
        when(customerService.createForAgent(any(), any())).thenReturn(resp);

        AppUserPrincipal principal = new AppUserPrincipal(AGENT_ID, "alice", null, Role.AGENT, null, true);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_AGENT")));

        mockMvc.perform(post("/api/v1/customers")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCustomerRequest("carol", "Password1!", "Carol", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()));
    }

    @Test
    void postCustomers_blankUsername_returns400() throws Exception {
        AppUserPrincipal principal = new AppUserPrincipal(AGENT_ID, "alice", null, Role.AGENT, null, true);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_AGENT")));

        mockMvc.perform(post("/api/v1/customers")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCustomerRequest("", "Password1!", "Carol", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCustomers_returns200WithList() throws Exception {
        CustomerResponse resp = new CustomerResponse(CUSTOMER_ID, "carol", "Carol", AGENT_ID, true, 0L);
        when(customerService.findAll(any())).thenReturn(List.of(resp));

        AppUserPrincipal principal = new AppUserPrincipal(AGENT_ID, "alice", null, Role.AGENT, null, true);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_AGENT")));

        mockMvc.perform(get("/api/v1/customers").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("carol"));
    }

    @Test
    void getCustomersId_returns200() throws Exception {
        CustomerResponse resp = new CustomerResponse(CUSTOMER_ID, "carol", "Carol", AGENT_ID, true, 0L);
        when(customerService.findById(any())).thenReturn(resp);

        mockMvc.perform(get("/api/v1/customers/" + CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()));
    }

    @Test
    void patchCustomersId_validRequest_returns200() throws Exception {
        CustomerResponse resp = new CustomerResponse(CUSTOMER_ID, "carol", "Updated", AGENT_ID, true, 1L);
        when(customerService.updateAnyCustomer(any(), any())).thenReturn(resp);

        mockMvc.perform(patch("/api/v1/customers/" + CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCustomerRequest("Updated", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated"));
    }
}
