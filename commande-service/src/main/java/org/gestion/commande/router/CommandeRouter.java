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

    @Bean
    public RouterFunction<ServerResponse> commandeRoutes(CommandeHandler handler) {
        return RouterFunctions.route()
                .nest(path("/api/commandes"), builder -> builder
                        .POST("",        handler::createCommande)
                        .GET("",         handler::getCommandes)
                        .GET("/{id}",    handler::getCommandeById)
                        .GET("/{id}/detail",   handler::getCommandeAvecLignes)
                        .PUT("/{id}",    handler::updateCommande)
                        .DELETE("/{id}", handler::annulerCommande)
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