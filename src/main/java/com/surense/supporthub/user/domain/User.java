package com.surense.supporthub.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.surense.supporthub.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(
    name = "users",
    indexes = @Index(name = "idx_users_agent_role", columnList = "agent_id, role")
)
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends Auditable {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agent;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Version
    private long version;

    @Override
    public String toString() {
        return "User{id=" + id + ", role=" + role + ", active=" + active + ", version=" + version + "}";
    }
}
