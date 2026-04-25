package com.surense.supporthub.ticket;

import com.surense.supporthub.common.exception.ResourceNotFoundException;
import com.surense.supporthub.security.AppUserPrincipal;
import com.surense.supporthub.ticket.domain.Ticket;
import com.surense.supporthub.ticket.domain.TicketPriority;
import com.surense.supporthub.ticket.domain.TicketRepository;
import com.surense.supporthub.ticket.domain.TicketStatus;
import com.surense.supporthub.ticket.dto.CreateTicketRequest;
import com.surense.supporthub.ticket.dto.TicketResponse;
import com.surense.supporthub.ticket.dto.UpdateTicketRequest;
import com.surense.supporthub.user.domain.User;
import com.surense.supporthub.user.domain.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;

    @Transactional
    @PreAuthorize("hasRole('CUSTOMER')")
    public TicketResponse createTicket(CreateTicketRequest req, AppUserPrincipal principal) {
        User customer = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + principal.userId()));
        Ticket ticket = ticketMapper.toEntity(req);
        ticket.setCustomer(customer);
        if (req.priority() == null) {
            ticket.setPriority(TicketPriority.MEDIUM);
        }
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @PreAuthorize("isAuthenticated()")
    public List<TicketResponse> findAll(
            AppUserPrincipal principal,
            UUID customerId,
            TicketStatus status,
            TicketPriority priority) {
        Specification<Ticket> spec = scopeByRole(principal)
                .and(filterByCustomer(customerId))
                .and(filterByStatus(status))
                .and(filterByPriority(priority));
        return ticketRepository.findAll(spec).stream().map(ticketMapper::toResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.canAccessTicket(authentication, #ticketId)")
    public TicketResponse findById(UUID ticketId) {
        return ticketMapper.toResponse(loadTicket(ticketId));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or (hasRole('AGENT') and @ownership.canAccessTicket(authentication, #ticketId))")
    public TicketResponse updateTicket(UUID ticketId, UpdateTicketRequest req) {
        Ticket ticket = loadTicket(ticketId);
        ticketMapper.updateEntity(req, ticket);
        return ticketMapper.toResponse(ticket);
    }

    private Ticket loadTicket(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
    }

    private static Specification<Ticket> scopeByRole(AppUserPrincipal p) {
        return switch (p.role()) {
            case CUSTOMER -> (root, q, cb) -> cb.equal(root.get("customer").get("id"), p.userId());
            case AGENT -> (root, q, cb) -> cb.equal(root.get("customer").get("agent").get("id"), p.userId());
            case ADMIN -> (root, q, cb) -> cb.conjunction();
        };
    }

    private static Specification<Ticket> filterByCustomer(UUID customerId) {
        return (root, q, cb) -> {
            if (customerId == null) return cb.conjunction();
            return cb.equal(root.get("customer").get("id"), customerId);
        };
    }

    private static Specification<Ticket> filterByStatus(TicketStatus status) {
        return (root, q, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    private static Specification<Ticket> filterByPriority(TicketPriority priority) {
        return (root, q, cb) -> priority == null ? cb.conjunction() : cb.equal(root.get("priority"), priority);
    }
}
