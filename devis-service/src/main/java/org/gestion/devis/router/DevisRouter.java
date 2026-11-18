package org.gestion.devis.router;

import org.gestion.devis.handler.DevisHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;

@Configuration
public class DevisRouter {

    @Bean
    public RouterFunction<ServerResponse> devisRoutes(DevisHandler handler) {
        return RouterFunctions.route()
                .nest(path("/api/devis"), builder -> builder
                        .GET("", handler::getDevis)
                        .POST("", handler::createDevis)
                        .GET("/{id}", handler::getDevisById)
                        .POST("/{id}/versions", handler::nouvelleVersion)
                        .POST("/{id}/lignes", handler::ajouterLigne)
                        .DELETE("/{id}/lignes/{ligneId}", handler::supprimerLigne)
                        .POST("/{id}/envoyer", handler::envoyer)
                        .POST("/{id}/accepter", handler::accepter)
                        .POST("/{id}/refuser", handler::refuser))
                .build();
    }
}
