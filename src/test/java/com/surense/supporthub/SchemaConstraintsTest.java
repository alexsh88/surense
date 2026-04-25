package com.surense.supporthub;

import com.surense.supporthub.auth.domain.RefreshToken;
import com.surense.supporthub.auth.domain.RefreshTokenRepository;
import com.surense.supporthub.common.audit.AuditorAwareImpl;
import com.surense.supporthub.common.config.JpaAuditingConfig;
import com.surense.supporthub.user.domain.Role;
import com.surense.supporthub.user.domain.User;
import com.surense.supporthub.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class, FlywayAutoConfiguration.class})
@ActiveProfiles("test")
class SchemaConstraintsTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    @Test
    void uniqueUsernameConstraintFires() {
        userRepository.saveAndFlush(adminUser("dupuser"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(adminUser("dupuser")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void customerWithoutAgentViolatesCheckConstraint() {
        var customer = User.builder()
            .username("orphan")
            .passwordHash("hash")
            .fullName("Orphan Customer")
            .role(Role.CUSTOMER)
            // agent deliberately omitted
            .build();

        // MySQL CHECK violations translate to JpaSystemException, not DataIntegrityViolationException.
        // Both are DataAccessException subtypes — asserting on the common parent is sufficient.
        assertThatThrownBy(() -> userRepository.saveAndFlush(customer))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("chk_customer_has_agent");
    }

    @Test
    void tokenHashMustBeUnique() {
        var agent = userRepository.saveAndFlush(agentUser());
        var customer = userRepository.saveAndFlush(customerOf(agent));

        var t1 = token(customer.getId(), "aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899");
        var t2 = token(customer.getId(), "aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899");

        refreshTokenRepository.saveAndFlush(t1);
        assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(t2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User adminUser(String username) {
        return User.builder()
            .username(username)
            .passwordHash("hash")
            .fullName("Admin")
            .role(Role.ADMIN)
            .build();
    }

    private User agentUser() {
        return User.builder()
            .username("agent-" + UUID.randomUUID())
            .passwordHash("hash")
            .fullName("Agent")
            .role(Role.AGENT)
            .build();
    }

    private User customerOf(User agent) {
        return User.builder()
            .username("cust-" + UUID.randomUUID())
            .passwordHash("hash")
            .fullName("Customer")
            .role(Role.CUSTOMER)
            .agent(agent)
            .build();
    }

    private RefreshToken token(UUID userId, String hash) {
        return RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(1209600))
            .build();
    }
}
