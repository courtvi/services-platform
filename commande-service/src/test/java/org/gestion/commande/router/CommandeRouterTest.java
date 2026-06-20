package org.gestion.commande.router;

import org.gestion.commande.config.ApplicationConfig;
import org.gestion.commande.handler.CommandeHandler;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CommandeRouterTest {

    @Test
    void commandeRoutes_shouldBuildRouterFunction() {
        ApplicationConfig.RouteProperties props = new ApplicationConfig.RouteProperties();
        props.setBasePath("/api/commandes");
        props.setPathId("/{id}");

        CommandeRouter router = new CommandeRouter(props);
        CommandeHandler handler = mock(CommandeHandler.class);

        RouterFunction<ServerResponse> routerFunction = router.commandeRoutes(handler);

        assertThat(routerFunction).isNotNull();
    }
}