package com.surense.supporthub.user.customer.dto;

import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @Size(max = 120) String fullName,
        Boolean active
) {}
