package com.surense.supporthub.user.agent.dto;

import jakarta.validation.constraints.Size;

public record UpdateAgentRequest(
        @Size(max = 120) String fullName,
        Boolean active
) {}
