package com.kaua.template.infrastructure.configurations.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public class ServiceAuthentication extends AbstractAuthenticationToken {

    private final Jwt jwt;
    private final AuthenticatedService service;

    public ServiceAuthentication(
            final Jwt jwt,
            final AuthenticatedService service,
            final Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.jwt = jwt;
        this.service = service;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt;
    }

    @Override
    public Object getPrincipal() {
        return service;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }
}
