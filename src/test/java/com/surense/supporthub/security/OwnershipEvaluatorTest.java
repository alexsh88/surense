package com.surense.supporthub.security;

import com.surense.supporthub.user.domain.Role;
import com.surense.supporthub.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnershipEvaluatorTest {

    @Mock UserRepository userRepository;
    @InjectMocks OwnershipEvaluator evaluator;

    private static final UUID AGENT_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID OTHER_CUSTOMER_ID = UUID.randomUUID();

    @Test
    void admin_canAccessAnyCustomer() {
        Authentication auth = agentAuth(AGENT_ID, Role.ADMIN);
        assertThat(evaluator.canAccessCustomer(auth, CUSTOMER_ID)).isTrue();
    }

    @Test
    void agent_canAccessOwnCustomer() {
        when(userRepository.existsByIdAndAgentId(CUSTOMER_ID, AGENT_ID)).thenReturn(true);
        Authentication auth = agentAuth(AGENT_ID, Role.AGENT);
        assertThat(evaluator.canAccessCustomer(auth, CUSTOMER_ID)).isTrue();
    }

    @Test
    void agent_cannotAccessOtherAgentsCustomer() {
        when(userRepository.existsByIdAndAgentId(OTHER_CUSTOMER_ID, AGENT_ID)).thenReturn(false);
        Authentication auth = agentAuth(AGENT_ID, Role.AGENT);
        assertThat(evaluator.canAccessCustomer(auth, OTHER_CUSTOMER_ID)).isFalse();
    }

    @Test
    void customer_canAccessOwnProfile() {
        Authentication auth = agentAuth(CUSTOMER_ID, Role.CUSTOMER);
        assertThat(evaluator.canAccessCustomer(auth, CUSTOMER_ID)).isTrue();
    }

    @Test
    void customer_cannotAccessOtherCustomer() {
        Authentication auth = agentAuth(CUSTOMER_ID, Role.CUSTOMER);
        assertThat(evaluator.canAccessCustomer(auth, OTHER_CUSTOMER_ID)).isFalse();
    }

    private static Authentication agentAuth(UUID userId, Role role) {
        AppUserPrincipal principal = new AppUserPrincipal(userId, "test@example.com", null, role, null, true);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }
}
