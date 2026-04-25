package com.surense.supporthub.user.customer.dto;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String username,
        String fullName,
        UUID agentId,
        boolean active,
        long version
) {}
