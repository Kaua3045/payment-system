# 🔐 Autenticação indices

- [🔐 Autenticação indices](#-autenticação-indices)
  - [📌 Estrutura do Código](#-estrutura-do-código)
    - [📌 AuthenticatedUser](#-authenticateduser)
    - [📌 UserAuthentication](#-userauthentication)
    - [📌 JwtConverter](#-jwtconverter)
    - [📌 Configuração de Segurança](#-configuração-de-segurança)
    - [📌 Observação](#-observação)
  - [🚀 Melhorias Sugeridas](#-melhorias-sugeridas)
    - [📌 E modificar o `JwtConverter` para utilizá-la:](#-e-modificar-o-jwtconverter-para-utilizá-la)

## 📌 Estrutura do Código
Esta parte implementa a autenticação baseada em JWT 
utilizando Spring Oauth2 Resource Server com Spring Security. 
Ele fornece configuração para um resource server e autenticação personalizada.
Mas também pode ser utilizada sem a configuração de um resource server.
Gerando um token JWT e verificando a autenticação do usuário.

### 📌 AuthenticatedUser
```java
public record AuthenticatedUser(String id) {} 
```

### 📌 UserAuthentication
```java
import org.springframework.security.authentication.AbstractAuthenticationToken;
    import org.springframework.security.core.GrantedAuthority;
    import org.springframework.security.oauth2.jwt.Jwt;
    
    import java.util.Collection;
    
    public class UserAuthentication extends AbstractAuthenticationToken {
    
        private final Jwt jwt;
        private final AuthenticatedUser user;
    
        public UserAuthentication(
                final Jwt jwt,
                final AuthenticatedUser user,
                final Collection<? extends GrantedAuthority> authorities
        ) {
            super(authorities);
            this.jwt = jwt;
            this.user = user;
        }
    
        @Override
        public Object getCredentials() {
            return jwt;
        }
    
        @Override
        public Object getPrincipal() {
            return user;
        }
    
        @Override
        public boolean isAuthenticated() {
            return true;
        }
    }
```

### 📌 JwtConverter
```java
    import org.springframework.core.convert.converter.Converter;
    import org.springframework.lang.NonNull;
    import org.springframework.security.authentication.AbstractAuthenticationToken;
    import org.springframework.security.oauth2.jwt.Jwt;
    import org.springframework.security.oauth2.jwt.JwtClaimNames;
    import org.springframework.stereotype.Component;
    
    import java.util.Collections;
    
    @Component
    public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    
        @Override
        public AbstractAuthenticationToken convert(@NonNull final Jwt jwt) {
            return new UserAuthentication(
                    jwt,
                    extractPrincipal(jwt),
                    Collections.emptyList()
            );
        }
    
        private AuthenticatedUser extractPrincipal(final Jwt jwt) {
            return new AuthenticatedUser(
                    jwt.getClaimAsString(JwtClaimNames.SUB)
            );
        }
    }
```

### 📌 Configuração de Segurança
```java
@Bean
public JwtDecoder jwtDecoder() {
    // ⚠️ Esse bean só é necessário se não estiver configurando como Resource Server
    RSAKeyGenerator rsaKeyGenerator = new RSAKeyGenerator(2048);
    try {
        RSAKey rsaKey = rsaKeyGenerator.generate();
        return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
    } catch (JOSEException e) {
        throw new RuntimeException(e);
    }
}

@Bean
public JwtEncoder jwtEncoder() {
    // ⚠️ Esse bean só é necessário se não estiver configurando como Resource Server
    RSAKeyGenerator rsaKeyGenerator = new RSAKeyGenerator(2048);
    RSAKey jwk = null;
    try {
        RSAKey rsaKey = rsaKeyGenerator.generate();
        jwk = new RSAKey.Builder(rsaKey.toRSAPublicKey())
                .privateKey(rsaKey.toPrivateKey()).build();
    } catch (JOSEException e) {
        throw new RuntimeException(e);
    }

    var jkws = new ImmutableJWKSet<>(new JWKSet(jwk));
    return new NimbusJwtEncoder(jkws);
}
```

### 📌 Observação
Caso este projeto seja usado apenas 
como um **Resource Server**
(ou seja, apenas verificando tokens JWT), 
**não é necessário definir os beans** 
`JwtDecoder` e `JwtEncoder`. 
Em vez disso, adicione a 
seguinte configuração no 
`application.yml`:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${auth-server-host}/oauth2/jwks
          issuer-uri: ${auth-server-host}
```

## 🚀 Melhorias Sugeridas
Se desejar extrair autoridades dos tokens JWT e usá-las na autenticação, você pode adicionar a seguinte classe:
```java
public class AuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(@NonNull final Jwt jwt) {
        final var resourceAuthorities = extractAuthorities(jwt);

        if (resourceAuthorities.isEmpty()) {
            return Collections.emptySet();
        }

        return resourceAuthorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    private List<String> extractAuthorities(final Jwt jwt) {
        return jwt.getClaimAsStringList("authorities") == null
                ? Collections.emptyList()
                : jwt.getClaimAsStringList("authorities");
    }
}
```

### 📌 E modificar o `JwtConverter` para utilizá-la:
```java
@Component
public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AuthoritiesConverter authoritiesConverter;

    public JwtConverter() {
        this.authoritiesConverter = new AuthoritiesConverter();
    }

    @Override
    public AbstractAuthenticationToken convert(@NonNull final Jwt jwt) {
        return new UserAuthentication(
                jwt,
                extractPrincipal(jwt),
                extractAuthorities(jwt)
        );
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
```