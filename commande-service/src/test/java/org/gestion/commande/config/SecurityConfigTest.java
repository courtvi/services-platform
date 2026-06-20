package org.gestion.commande.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void extractRoles_shouldExtractFromResourceAccess() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("resource_access")).thenReturn(Map.of(
                "haller", Map.of("roles", List.of("CLIENT", "ADMIN"))
        ));

        Collection<GrantedAuthority> authorities = securityConfig.extractRolesForTest(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_CLIENT", "ROLE_ADMIN");
    }

    @Test
    void extractRoles_shouldFallbackToRealmAccess() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("resource_access")).thenReturn(null);
        when(jwt.getClaim("realm_access")).thenReturn(
                Map.of("roles", List.of("CLIENT"))
        );

        Collection<GrantedAuthority> authorities = securityConfig.extractRolesForTest(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CLIENT");
    }

    @Test
    void extractRoles_shouldReturnEmptyWhenNoClaims() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("resource_access")).thenReturn(null);
        when(jwt.getClaim("realm_access")).thenReturn(null);

        Collection<GrantedAuthority> authorities = securityConfig.extractRolesForTest(jwt);

        assertThat(authorities).isEmpty();
    }
}