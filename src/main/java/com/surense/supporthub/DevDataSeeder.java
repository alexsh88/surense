package com.surense.supporthub;

import com.surense.supporthub.ticket.domain.Ticket;
import com.surense.supporthub.ticket.domain.TicketPriority;
import com.surense.supporthub.ticket.domain.TicketRepository;
import com.surense.supporthub.user.domain.Role;
import com.surense.supporthub.user.domain.User;
import com.surense.supporthub.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "Password1!";

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User agent1 = seedUser("alice", "Alice Agent", Role.AGENT, null);
        User agent2 = seedUser("bob", "Bob Agent", Role.AGENT, null);

        User customer1 = seedUser("carol", "Carol Customer", Role.CUSTOMER, agent1);
        User customer2 = seedUser("dave", "Dave Customer", Role.CUSTOMER, agent1);
        User customer3 = seedUser("eve", "Eve Customer", Role.CUSTOMER, agent2);
        User customer4 = seedUser("frank", "Frank Customer", Role.CUSTOMER, agent2);

        if (ticketRepository.count() == 0) {
            ticketRepository.save(makeTicket("Login issue", "Cannot log in to the portal.", TicketPriority.HIGH, customer1));
            ticketRepository.save(makeTicket("Billing question", "Invoice appears incorrect.", TicketPriority.MEDIUM, customer1));
            ticketRepository.save(makeTicket("Feature request", "Would like dark mode.", TicketPriority.LOW, customer2));
            ticketRepository.save(makeTicket("Password reset", "Reset link not received.", TicketPriority.HIGH, customer3));
            ticketRepository.save(makeTicket("Account upgrade", "Need to upgrade plan.", TicketPriority.MEDIUM, customer3));
            ticketRepository.save(makeTicket("Data export", "Export button is broken.", TicketPriority.MEDIUM, customer4));
        }
    }

    private User seedUser(String username, String fullName, Role role, User agent) {
        return userRepository.findByUsername(username).orElseGet(() ->
                userRepository.save(User.builder()
                        .username(username)
                        .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                        .fullName(fullName)
                        .role(role)
                        .agent(agent)
                        .active(true)
                        .build()));
    }

    private static Ticket makeTicket(String title, String description, TicketPriority priority, User customer) {
        return Ticket.builder()
                .title(title)
                .description(description)
                .priority(priority)
                .customer(customer)
                .build();
    }
}
