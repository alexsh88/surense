package com.surense.supporthub.user.agent;

import com.surense.supporthub.common.exception.ConflictException;
import com.surense.supporthub.common.exception.ResourceNotFoundException;
import com.surense.supporthub.security.AppUserPrincipal;
import com.surense.supporthub.user.agent.dto.AgentResponse;
import com.surense.supporthub.user.agent.dto.CreateAgentRequest;
import com.surense.supporthub.user.agent.dto.UpdateAgentRequest;
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
public class AgentService {

    private final UserRepository userRepository;
    private final AgentMapper agentMapper;
    private final PasswordEncoder passwordEncoder;

    @PreAuthorize("hasRole('AGENT')")
    public AgentResponse getOwnProfile(AppUserPrincipal principal) {
        return agentMapper.toResponse(findAgentById(principal.userId()));
    }

    @Transactional
    @PreAuthorize("hasRole('AGENT')")
    public AgentResponse updateOwnProfile(UpdateAgentRequest req, AppUserPrincipal principal) {
        User agent = findAgentById(principal.userId());
        agentMapper.updateEntity(req, agent);
        return agentMapper.toResponse(agent);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AgentResponse createAgent(CreateAgentRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ConflictException("An account with that username already exists.");
        }
        User agent = agentMapper.toEntity(req);
        agent.setRole(Role.AGENT);
        agent.setPasswordHash(passwordEncoder.encode(req.password()));
        return agentMapper.toResponse(userRepository.save(agent));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AgentResponse updateAgent(UUID agentId, UpdateAgentRequest req) {
        User agent = findAgentById(agentId);
        agentMapper.updateEntity(req, agent);
        return agentMapper.toResponse(agent);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<AgentResponse> listAll() {
        return userRepository.findAllByRole(Role.AGENT).stream().map(agentMapper::toResponse).toList();
    }

    private User findAgentById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + id));
    }
}
