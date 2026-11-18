package org.gestion.produit.router;

import org.gestion.produit.handler.FicheBoutiqueHandler;
import org.gestion.produit.handler.ProduitHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/** Chemin /api/products : c'est celui qu'utilisent l'api-gateway et les scripts k6. */
@Configuration(proxyBeanMethods = false)
public class ProduitRouter {

    @Bean
    public RouterFunction<ServerResponse> produitRoutes(ProduitHandler handler, FicheBoutiqueHandler ficheHandler) {
        return RouterFunctions.route()
                .nest(path("/api/products"), builder -> builder
                        .GET("", handler::getAllProduits)
                        .POST("", handler::createProduit)
                        .GET("/{id}", handler::getProduitById)
                        .PUT("/{id}", handler::updateProduit)
                        .DELETE("/{id}", handler::deleteProduit)
                        // Catalogue d'une boutique (produit + fiche boutique)
                        .GET("/boutique/{boutiqueId}", ficheHandler::getCatalogue)
                        .GET("/boutique/{boutiqueId}/{produitId}", ficheHandler::getProduitBoutique))
                .build();
    }
}
