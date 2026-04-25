package com.surense.supporthub.user.agent;

import com.surense.supporthub.security.AppUserPrincipal;
import com.surense.supporthub.user.agent.dto.AgentResponse;
import com.surense.supporthub.user.agent.dto.CreateAgentRequest;
import com.surense.supporthub.user.agent.dto.UpdateAgentRequest;
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
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final AgentService agentService;

    @GetMapping("/me")
    public ResponseEntity<AgentResponse> getOwnProfile(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(agentService.getOwnProfile(principal));
    }

    @PatchMapping("/me")
    public ResponseEntity<AgentResponse> updateOwnProfile(
            @Valid @RequestBody UpdateAgentRequest req,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(agentService.updateOwnProfile(req, principal));
    }

    @PostMapping
    public ResponseEntity<AgentResponse> createAgent(
            @Valid @RequestBody CreateAgentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agentService.createAgent(req));
    }

    @GetMapping
    public ResponseEntity<List<AgentResponse>> listAll() {
        return ResponseEntity.ok(agentService.listAll());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AgentResponse> updateAgent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAgentRequest req) {
        return ResponseEntity.ok(agentService.updateAgent(id, req));
    }
}
