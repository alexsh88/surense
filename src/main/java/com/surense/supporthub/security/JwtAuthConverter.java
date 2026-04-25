package com.surense.supporthub.security;

import com.surense.supporthub.user.domain.Role;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JwtAuthConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaimAsString("username");
        Role role = Role.valueOf(jwt.getClaimAsString("role"));
        String agentIdStr = jwt.getClaimAsString("agentId");
        UUID agentId = agentIdStr != null ? UUID.fromString(agentIdStr) : null;

        AppUserPrincipal principal = new AppUserPrincipal(userId, username, null, role, agentId, true);
        return new UsernamePasswordAuthenticationToken(principal, jwt, principal.getAuthorities());
    }
}
