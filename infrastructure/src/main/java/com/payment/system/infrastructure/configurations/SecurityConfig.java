package com.payment.system.infrastructure.configurations;

import com.payment.system.infrastructure.configurations.authentication.JwtConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private final JwtConverter jwtConverter;

    public SecurityConfig(final JwtConverter jwtConverter) {
        this.jwtConverter = Objects.requireNonNull(jwtConverter);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize ->
                        authorize.requestMatchers("/v1/hello/test").permitAll()
                                .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(j -> j.jwtAuthenticationConverter(jwtConverter)))
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

//    @Bean
//    public JwtDecoder jwtDecoder() {
//        // TODO: Change this to a real public key
//        RSAKeyGenerator rsaKeyGenerator = new RSAKeyGenerator(2048);
//        try {
//            RSAKey rsaKey = rsaKeyGenerator.generate();
//            return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
//        } catch (JOSEException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Bean
//    public JwtEncoder jwtEncoder() {
//        // TODO: Change this to get real public and private keys
//        RSAKeyGenerator rsaKeyGenerator = new RSAKeyGenerator(2048);
//        RSAKey jwk = null;
//        try {
//            RSAKey rsaKey = rsaKeyGenerator.generate();
//            jwk = new RSAKey.Builder(rsaKey.toRSAPublicKey())
//                    .privateKey(rsaKey.toPrivateKey()).build();
//        } catch (JOSEException e) {
//            throw new RuntimeException(e);
//        }
//
//        var jkws = new ImmutableJWKSet<>(new JWKSet(jwk));
//
//        return new NimbusJwtEncoder(jkws);
//    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // Permite acesso apenas a este domínio
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
