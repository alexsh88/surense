package com.surense.supporthub.ticket;

import com.surense.supporthub.security.AppUserPrincipal;
import com.surense.supporthub.ticket.domain.TicketPriority;
import com.surense.supporthub.ticket.domain.TicketStatus;
import com.surense.supporthub.ticket.dto.CreateTicketRequest;
import com.surense.supporthub.ticket.dto.TicketResponse;
import com.surense.supporthub.ticket.dto.UpdateTicketRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest req,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(req, principal));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> listAll(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ticketService.findAll(principal, customerId, status, priority));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketRequest req) {
        return ResponseEntity.ok(ticketService.updateTicket(id, req));
    }
}
