package com.surense.supporthub.ticket.dto;

import com.surense.supporthub.ticket.domain.TicketPriority;
import com.surense.supporthub.ticket.domain.TicketStatus;

import java.util.UUID;

public record TicketResponse(
        UUID id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        UUID customerId,
        long version
) {}
