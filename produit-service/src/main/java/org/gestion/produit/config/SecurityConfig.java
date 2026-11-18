package org.gestion.produit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.authentication.preauth.x509.SubjectX500PrincipalExtractor;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLES_KEY = "roles";
    private static final String API_PRODUCTS = "/api/products";
    private static final String API_PRODUCTS_ALL = "/api/products/**";

    // ===================== Profil mtls (inchangé) =====================

    /** Profil mtls : seul un certificat client avec un CN autorisé passe. */
    @Bean
    @Profile("mtls")
    SecurityWebFilterChain mtlsChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .x509(x509 -> x509.principalExtractor(new SubjectX500PrincipalExtractor()))
                .authorizeExchange(ex -> ex
                        .pathMatchers("/manage/health/**").permitAll()   // sondes kubelet (port 8081, sans TLS)
                        .anyExchange().hasRole("API_CLIENT"))
                .build();
    }

    @Bean
    @Profile("mtls")
    ReactiveUserDetailsService clientCertUsers() {
        return new MapReactiveUserDetailsService(
                User.withUsername("k6-loadtest")
                        .password("{noop}n/a")
                        .roles("API_CLIENT")
                        .build(),
                User.withUsername("api-gateway")
                        .password("{noop}n/a")
                        .roles("API_CLIENT")
                        .build(),
                // services qui lisent les prix (GET /api/products/{id})
                User.withUsername("commande-service")
                        .password("{noop}n/a")
                        .roles("API_CLIENT")
                        .build(),
                User.withUsername("devis-service")
                        .password("{noop}n/a")
                        .roles("API_CLIENT")
                        .build());
    }

    // ============ Sans mtls : JWT Keycloak (avant : tout ouvert) ============

    /** Lecture du catalogue publique, écriture réservée au rôle ADMIN. */
    @Bean
    @Profile("!mtls")
    SecurityWebFilterChain jwtChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder,
                                    ReactiveJwtAuthenticationConverter keycloakJwtConverter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/manage/**").permitAll()
                        .pathMatchers(HttpMethod.GET, API_PRODUCTS, API_PRODUCTS_ALL).permitAll()
                        .pathMatchers(API_PRODUCTS, API_PRODUCTS_ALL).hasRole(ROLE_ADMIN)
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(keycloakJwtConverter)))
                .build();
    }

    @Bean
    @Profile("!mtls")
    ReactiveJwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(issuerUri),
                new JwtTimestampValidator());
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    @Profile("!mtls")
    ReactiveJwtAuthenticationConverter keycloakJwtConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> Flux.fromIterable(extraireRoles(jwt)));
        return converter;
    }

    /** Rôles lus dans resource_access.haller, ou realm_access en repli (comme commande-service). */
    @SuppressWarnings("unchecked")
    static Collection<GrantedAuthority> extraireRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null && resourceAccess.get("haller") instanceof Map<?, ?> haller
                && haller.get(ROLES_KEY) instanceof List<?> roles) {
            return versAuthorities((List<String>) roles);
        }
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get(ROLES_KEY) instanceof List<?> roles) {
            return versAuthorities((List<String>) roles);
        }
        return Collections.emptyList();
    }

    private static Collection<GrantedAuthority> versAuthorities(List<String> roles) {
        return roles.stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
