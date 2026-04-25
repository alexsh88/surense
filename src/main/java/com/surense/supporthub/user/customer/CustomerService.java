package com.surense.supporthub.user.customer;

import com.surense.supporthub.common.exception.ConflictException;
import com.surense.supporthub.common.exception.ResourceNotFoundException;
import com.surense.supporthub.security.AppUserPrincipal;
import com.surense.supporthub.user.customer.dto.CreateCustomerRequest;
import com.surense.supporthub.user.customer.dto.CustomerResponse;
import com.surense.supporthub.user.customer.dto.UpdateCustomerRequest;
import com.surense.supporthub.user.domain.Role;
import com.surense.supporthub.user.domain.User;
import com.surense.supporthub.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final UserRepository userRepository;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;

    // AGENT: agentId pulled from principal — never trusted from body
    @Transactional
    @PreAuthorize("hasRole('AGENT')")
    public CustomerResponse createForAgent(CreateCustomerRequest req, AppUserPrincipal principal) {
        return create(req, principal.userId());
    }

    // ADMIN: agentId must be supplied in the request body
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse createForAdmin(CreateCustomerRequest req) {
        if (req.agentId() == null) {
            throw new IllegalArgumentException("agentId is required when creating a customer as ADMIN.");
        }
        return create(req, req.agentId());
    }

    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public List<CustomerResponse> findAll(AppUserPrincipal principal) {
        if (principal.role() == Role.ADMIN) {
            return userRepository.findAllByRole(Role.CUSTOMER).stream().map(customerMapper::toResponse).toList();
        }
        return userRepository.findAllByAgentId(principal.userId()).stream().map(customerMapper::toResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.canAccessCustomer(authentication, #customerId)")
    public CustomerResponse findById(UUID customerId) {
        return customerMapper.toResponse(findCustomerById(customerId));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse getOwnProfile(AppUserPrincipal principal) {
        return customerMapper.toResponse(findCustomerById(principal.userId()));
    }

    @Transactional
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse updateOwnProfile(UpdateCustomerRequest req, AppUserPrincipal principal) {
        User customer = findCustomerById(principal.userId());
        customerMapper.updateEntity(req, customer);
        return customerMapper.toResponse(customer);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse updateAnyCustomer(UUID customerId, UpdateCustomerRequest req) {
        User customer = findCustomerById(customerId);
        customerMapper.updateEntity(req, customer);
        return customerMapper.toResponse(customer);
    }

    private CustomerResponse create(CreateCustomerRequest req, UUID agentId) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ConflictException("An account with that username already exists.");
        }
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + agentId));
        User customer = customerMapper.toEntity(req);
        customer.setRole(Role.CUSTOMER);
        customer.setAgent(agent);
        customer.setPasswordHash(passwordEncoder.encode(req.password()));
        return customerMapper.toResponse(userRepository.save(customer));
    }

    private User findCustomerById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }
}
