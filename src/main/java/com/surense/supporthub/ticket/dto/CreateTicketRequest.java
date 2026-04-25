package com.surense.supporthub.ticket.dto;

import com.surense.supporthub.ticket.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        TicketPriority priority
) {}
