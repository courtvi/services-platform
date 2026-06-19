package org.gestion.commande.router;

import org.gestion.commande.handler.CommandeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class CommandeRouter {

    private static final String PATH_ID = "/{id}";

    @Bean
    public RouterFunction<ServerResponse> commandeRoutes(CommandeHandler handler) {
        return RouterFunctions.route()
                .nest(path("/api/commandes"), builder -> builder
                        .POST("",        handler::createCommande)
                        .GET("",         handler::getCommandes)
                        .GET(PATH_ID,    handler::getCommandeById)
                        .GET("/{id}/detail",   handler::getCommandeAvecLignes)
                        .PUT(PATH_ID,    handler::updateCommande)
                        .DELETE(PATH_ID, handler::annulerCommande)
                        .PATCH("/{id}/en-cours", handler::passerEnCours)
                        .GET("/debug", request ->
                                ReactiveSecurityContextHolder.getContext()
                                        .map(ctx -> ctx.getAuthentication())
                                        .flatMap(auth ->
                                                ServerResponse.ok().bodyValue(auth)
                                        )
                        )

                )
                .build();
    }
}