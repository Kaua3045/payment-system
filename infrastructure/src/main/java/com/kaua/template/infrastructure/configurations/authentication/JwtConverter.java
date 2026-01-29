package com.kaua.template.infrastructure.configurations.authentication;

import com.kaua.template.domain.exceptions.InternalErrorException;
import com.kaua.template.infrastructure.constants.Constants;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

@Component
public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AuthoritiesConverter authoritiesConverter;

    public JwtConverter(final AuthoritiesConverter authoritiesConverter) {
        this.authoritiesConverter = Objects.requireNonNull(authoritiesConverter);
    }

    @Override
    public AbstractAuthenticationToken convert(@NonNull final Jwt jwt) {
        final var type = jwt.getClaimAsString(Constants.JWT_CLAIM_TYPE);

        return switch (type) {
            case Constants.USER_TYPE -> new UserAuthentication(
                    jwt,
                    extractPrincipal(jwt),
                    extractAuthorities(jwt)
            );
            case Constants.SERVICE_TYPE -> new ServiceAuthentication(
                    jwt,
                    new AuthenticatedService(
                            jwt.getClaimAsString(JwtClaimNames.SUB)
                    ),
                    extractAuthorities(jwt)
            );
            default -> throw InternalErrorException.with("Invalid JWT type claim " + type);
        };
    }

    private AuthenticatedUser extractPrincipal(final Jwt jwt) {
        return new AuthenticatedUser(
                jwt.getClaimAsString(JwtClaimNames.SUB)
        );
    }

    private Collection<? extends GrantedAuthority> extractAuthorities(final Jwt jwt) {
        return this.authoritiesConverter.convert(jwt);
    }
}
