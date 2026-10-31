package org.gestion.produit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.preauth.x509.SubjectX500PrincipalExtractor;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

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


    /** Sans le profil mtls : comportement actuel conservé (tout ouvert). */
    @Bean
    @Profile("!mtls")
    SecurityWebFilterChain openChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
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
                        .build());
    }
}