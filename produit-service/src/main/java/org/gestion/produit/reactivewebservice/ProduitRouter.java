package org.gestion.produit.reactivewebservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;



@Configuration(proxyBeanMethods = false)
public class ProduitRouter {

    @Bean
    public RouterFunction<ServerResponse> routes(ProduitHandler handler) {

        return RouterFunctions.route()
                .GET("api/produits", handler::getAllProduits)
                .POST("api/produits", handler::createProduit)
                .build();
    }
}
