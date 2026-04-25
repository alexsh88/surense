package com.surense.supporthub.ticket.domain;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findAllByCustomerId(UUID customerId);

    List<Ticket> findAllByCustomerAgentId(UUID agentId);

    boolean existsByIdAndCustomerId(UUID id, UUID customerId);

    boolean existsByIdAndCustomerAgentId(UUID id, UUID agentId);

    @EntityGraph(attributePaths = {"customer"})
    List<Ticket> findAll(Specification<Ticket> spec);

    @EntityGraph(attributePaths = {"customer"})
    Optional<Ticket> findById(UUID id);
}
