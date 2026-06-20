package org.gestion.commande.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationConfigTest {

    @Test
    void routeProperties_shouldGetAndSetValues() {
        ApplicationConfig.RouteProperties props = new ApplicationConfig.RouteProperties();

        props.setBasePath("/api/commandes");
        props.setPathId("/{id}");

        assertThat(props.getBasePath()).isEqualTo("/api/commandes");
        assertThat(props.getPathId()).isEqualTo("/{id}");
    }
}