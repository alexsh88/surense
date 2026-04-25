package com.surense.supporthub.ticket.dto;

import com.surense.supporthub.ticket.domain.TicketPriority;
import com.surense.supporthub.ticket.domain.TicketStatus;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @Size(max = 200) String title,
        String description,
        TicketStatus status,
        TicketPriority priority
) {}
