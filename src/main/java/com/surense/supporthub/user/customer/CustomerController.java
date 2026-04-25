package com.surense.supporthub.user.customer;

import com.surense.supporthub.security.AppUserPrincipal;
import com.surense.supporthub.user.customer.dto.CreateCustomerRequest;
import com.surense.supporthub.user.customer.dto.CustomerResponse;
import com.surense.supporthub.user.customer.dto.UpdateCustomerRequest;
import com.surense.supporthub.user.domain.Role;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest req,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        CustomerResponse resp = switch (principal.role()) {
            case AGENT -> customerService.createForAgent(req, principal);
            case ADMIN -> customerService.createForAdmin(req);
            default -> throw new AccessDeniedException("Forbidden");
        };
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> listAll(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(customerService.findAll(principal));
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getOwnProfile(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(customerService.getOwnProfile(principal));
    }

    @PatchMapping("/me")
    public ResponseEntity<CustomerResponse> updateOwnProfile(
            @Valid @RequestBody UpdateCustomerRequest req,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(customerService.updateOwnProfile(req, principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest req) {
        return ResponseEntity.ok(customerService.updateAnyCustomer(id, req));
    }
}
