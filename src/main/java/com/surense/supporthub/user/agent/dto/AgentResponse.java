package com.surense.supporthub.user.agent.dto;

import java.util.UUID;

public record AgentResponse(
        UUID id,
        String username,
        String fullName,
        boolean active,
        long version
) {}
