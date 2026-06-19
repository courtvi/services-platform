package org.gestion.commande.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    private static final String ROLE_CLIENT = "CLIENT";
    private static final String ROLE_ADMIN  = "ADMIN";
    private static final String API_COMMANDES  = "/api/commandes/**";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/manage/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/commandes")
                        .hasRole(ROLE_CLIENT)
                        .pathMatchers(HttpMethod.GET, API_COMMANDES)
                        .hasAnyRole(ROLE_CLIENT, ROLE_ADMIN)
                        .pathMatchers(HttpMethod.PUT, API_COMMANDES)
                        .hasRole(ROLE_CLIENT)
                        .pathMatchers(HttpMethod.DELETE, API_COMMANDES)
                        .hasRole(ROLE_CLIENT)
                        .anyExchange().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder())
                                .jwtAuthenticationConverter(keycloakJwtConverter())
                        )
                )
                .build();
    }

    // ✅ Décodeur JWT sans vérification d'audience
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .build();

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(issuerUri),
                new JwtTimestampValidator()
        );

        decoder.setJwtValidator(validator);
        return decoder;
    }

    // ✅ Convertit les rôles Keycloak → GrantedAuthority Spring Security
    @Bean
    public ReactiveJwtAuthenticationConverter keycloakJwtConverter() {
        ReactiveJwtAuthenticationConverter converter =
                new ReactiveJwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt ->
                Flux.fromIterable(extractRoles(jwt))
        );

        return converter;
    }

    // ✅ Extrait les rôles depuis resource_access.haller
    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {

        // ✅ Client roles (haller) — priorité
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null && resourceAccess.containsKey("haller")) {
            Map<String, Object> hallerAccess =
                    (Map<String, Object>) resourceAccess.get("haller");
            List<String> roles = (List<String>) hallerAccess.get("roles");
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        }

        // Fallback — Realm roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}