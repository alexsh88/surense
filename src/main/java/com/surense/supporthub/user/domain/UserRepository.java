package com.surense.supporthub.user.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"agent"})
    List<User> findAllByRole(Role role);

    @EntityGraph(attributePaths = {"agent"})
    List<User> findAllByAgentId(UUID agentId);

    Optional<User> findByIdAndAgentId(UUID id, UUID agentId);

    boolean existsByIdAndAgentId(UUID id, UUID agentId);
}
