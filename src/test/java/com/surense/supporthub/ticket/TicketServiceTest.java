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
import com.surense.supporthub.user.domain.Role;
import com.surense.supporthub.user.domain.User;
import com.surense.supporthub.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock UserRepository userRepository;
    @Spy TicketMapper ticketMapper = new TicketMapperImpl();
    @InjectMocks TicketService ticketService;

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID TICKET_ID = UUID.randomUUID();

    @Test
    void createTicket_success() {
        User customer = User.builder().username("carol").passwordHash("h")
                .fullName("Carol").role(Role.CUSTOMER).active(true).build();
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        Ticket saved = Ticket.builder().title("Login issue").description("Can't login")
                .priority(TicketPriority.HIGH).customer(customer).build();
        when(ticketRepository.save(any())).thenReturn(saved);

        AppUserPrincipal principal = new AppUserPrincipal(CUSTOMER_ID, "carol", null, Role.CUSTOMER, null, true);
        TicketResponse resp = ticketService.createTicket(
                new CreateTicketRequest("Login issue", "Can't login", TicketPriority.HIGH), principal);

        assertThat(resp.title()).isEqualTo("Login issue");
        assertThat(resp.priority()).isEqualTo(TicketPriority.HIGH);
    }

    @Test
    void createTicket_customerNotFound_throws() {
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());
        AppUserPrincipal principal = new AppUserPrincipal(CUSTOMER_ID, "carol", null, Role.CUSTOMER, null, true);

        assertThatThrownBy(() -> ticketService.createTicket(
                new CreateTicketRequest("Title", "Desc", null), principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_asCustomer_scopedToOwnTickets() {
        User customer = User.builder().username("carol").passwordHash("h")
                .fullName("Carol").role(Role.CUSTOMER).active(true).build();
        Ticket ticket = Ticket.builder().title("My ticket").description("Desc")
                .priority(TicketPriority.MEDIUM).customer(customer).build();
        when(ticketRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(ticket));

        AppUserPrincipal principal = new AppUserPrincipal(CUSTOMER_ID, "carol", null, Role.CUSTOMER, null, true);
        var result = ticketService.findAll(principal, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("My ticket");
    }

    @Test
    void findById_notFound_throws() {
        when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.findById(TICKET_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateTicket_updatesStatus() {
        User customer = User.builder().username("carol").passwordHash("h")
                .fullName("Carol").role(Role.CUSTOMER).active(true).build();
        Ticket ticket = Ticket.builder().title("T").description("D")
                .priority(TicketPriority.MEDIUM).customer(customer).build();
        when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));

        TicketResponse resp = ticketService.updateTicket(TICKET_ID,
                new UpdateTicketRequest(null, null, TicketStatus.RESOLVED, null));

        assertThat(resp.status()).isEqualTo(TicketStatus.RESOLVED);
    }
}
