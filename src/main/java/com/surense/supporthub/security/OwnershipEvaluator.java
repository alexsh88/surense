package com.surense.supporthub.security;

import com.surense.supporthub.ticket.domain.TicketRepository;
import com.surense.supporthub.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("ownership")
@RequiredArgsConstructor
public class OwnershipEvaluator {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public boolean canAccessCustomer(Authentication auth, UUID customerId) {
        AppUserPrincipal p = (AppUserPrincipal) auth.getPrincipal();
        return switch (p.role()) {
            case ADMIN -> true;
            case AGENT -> userRepository.existsByIdAndAgentId(customerId, p.userId());
            case CUSTOMER -> customerId.equals(p.userId());
        };
    }

    public boolean canAccessTicket(Authentication auth, UUID ticketId) {
        AppUserPrincipal p = (AppUserPrincipal) auth.getPrincipal();
        return switch (p.role()) {
            case ADMIN -> true;
            case AGENT -> ticketRepository.existsByIdAndCustomerAgentId(ticketId, p.userId());
            case CUSTOMER -> ticketRepository.existsByIdAndCustomerId(ticketId, p.userId());
        };
    }

}
